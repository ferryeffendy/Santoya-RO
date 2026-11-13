package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.util.SyncCollections
import com.example.util.SyncOperations
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineSyncQueueUnitTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: SantoyaRepository
    private var mutationTriggerCount = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mutationTriggerCount = 0
        repo = SantoyaRepository(db, context) {
            mutationTriggerCount++
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun repositoryMutations_enqueuePendingSyncAndNotifyCallback() = runBlocking {
        val produk = Produk(nama_produk = "Galon RO", harga = 6000.0, syncId = "prod_1")
        val produkId = repo.insertProduk(produk).toInt()
        val pelanggan = Pelanggan(nama = "Pak Budi", alamat = "Jl. A", no_hp = "0812", syncId = "pel_1")
        val pelangganId = repo.insertPelanggan(pelanggan).toInt()

        val txId = repo.recordTransaksi(
            tanggal = 1000L,
            idPelanggan = pelangganId,
            idKurir = null,
            idProduk = produkId,
            jumlah = 2,
            hargaSatuan = 6000.0,
            metodeBayar = "-",
            status = "Kasbon",
            uangMuka = 0L,
            sisaKasbon = 12000L
        ).toInt()

        val pending = db.pendingSyncDao().getAllPending()
        assertTrue(pending.any { it.collectionName == SyncCollections.PRODUK && it.syncId == "prod_1" && it.operation == SyncOperations.UPSERT })
        assertTrue(pending.any { it.collectionName == SyncCollections.PELANGGAN && it.syncId == "pel_1" && it.operation == SyncOperations.UPSERT })
        assertTrue(pending.any { it.collectionName == SyncCollections.TRANSAKSI && it.operation == SyncOperations.UPSERT })
        assertTrue(pending.any { it.collectionName == SyncCollections.KASBON_LOGS && it.operation == SyncOperations.UPSERT })
        assertTrue(mutationTriggerCount >= 3)

        val savedTx = db.transaksiDao().getDetailById(txId)!!.transaksi
        val savedProduk = db.produkDao().getById(produkId)!!
        repo.deleteProduk(savedProduk)

        val txAfterProdukDelete = db.transaksiDao().getBySyncId(savedTx.syncId)
        assertNotNull(txAfterProdukDelete)
        assertNull(txAfterProdukDelete!!.id_produk)

        val pendingAfterDelete = db.pendingSyncDao().getAllPending()
        val prodQueue = pendingAfterDelete.first { it.collectionName == SyncCollections.PRODUK && it.syncId == "prod_1" }
        assertEquals(SyncOperations.DELETE, prodQueue.operation)
    }

    @Test
    fun bayarKasbonPelanggan_allocatesPaymentFifoAcrossNotesAndUpdatesPelanggan() = runBlocking {
        val produkId = repo.insertProduk(Produk(nama_produk = "Galon RO", harga = 6000.0)).toInt()
        val pelangganId = repo.insertPelanggan(Pelanggan(nama = "Bu Sari", alamat = "Blok B", no_hp = "0813")).toInt()

        val txOldId = repo.recordTransaksi(
            tanggal = 1_000L,
            idPelanggan = pelangganId,
            idKurir = null,
            idProduk = produkId,
            jumlah = 2,
            hargaSatuan = 6000.0,
            metodeBayar = "-",
            status = "Kasbon",
            uangMuka = 0L,
            sisaKasbon = 12_000L
        ).toInt()

        val txNewId = repo.recordTransaksi(
            tanggal = 2_000L,
            idPelanggan = pelangganId,
            idKurir = null,
            idProduk = produkId,
            jumlah = 2,
            hargaSatuan = 6000.0,
            metodeBayar = "-",
            status = "Kasbon",
            uangMuka = 0L,
            sisaKasbon = 12_000L
        ).toInt()

        // Partial payment of Rp 15.000 -> settles oldest note (12.000) to Lunas, reduces newer note by 3.000 (sisa 9.000)
        repo.bayarKasbonPelanggan(pelangganId, 15_000.0, "Transfer")

        val oldTxAfterPartial = db.transaksiDao().getDetailById(txOldId)!!.transaksi
        val newTxAfterPartial = db.transaksiDao().getDetailById(txNewId)!!.transaksi
        val custAfterPartial = db.pelangganDao().getById(pelangganId)!!

        assertEquals("Lunas", oldTxAfterPartial.status)
        assertEquals("Transfer", oldTxAfterPartial.metode_bayar)
        assertEquals(0L, oldTxAfterPartial.sisaKasbon)
        assertEquals(12_000L, oldTxAfterPartial.uangMuka)

        assertEquals("Kasbon", newTxAfterPartial.status)
        assertEquals("Bayar Sebagian", newTxAfterPartial.metode_bayar)
        assertEquals(9_000L, newTxAfterPartial.sisaKasbon)
        assertEquals(3_000L, newTxAfterPartial.uangMuka)
        assertEquals(9_000.0, custAfterPartial.sisa_kasbon, 0.01)

        // Pay remaining Rp 9.000 -> settles newer note to Lunas and clears customer kasbon
        repo.bayarKasbonPelanggan(pelangganId, 9_000.0, "Cash")

        val newTxAfterFull = db.transaksiDao().getDetailById(txNewId)!!.transaksi
        val custAfterFull = db.pelangganDao().getById(pelangganId)!!
        val remainingUnpaid = db.transaksiDao().getUnpaidByPelanggan(pelangganId)

        assertEquals("Lunas", newTxAfterFull.status)
        assertEquals(0L, newTxAfterFull.sisaKasbon)
        assertEquals(0.0, custAfterFull.sisa_kasbon, 0.01)
        assertTrue(remainingUnpaid.isEmpty())
    }

    @Test
    fun reconcileKasbonDiscrepancies_automaticallySettlesStaleNotesMatchingCurrentCustomerBalance() = runBlocking {
        val produkId = repo.insertProduk(Produk(nama_produk = "Galon RO", harga = 6000.0)).toInt()
        val pelangganId = repo.insertPelanggan(Pelanggan(nama = "Pak Joko", alamat = "Blok C", no_hp = "0815")).toInt()

        val tx1Id = repo.recordTransaksi(
            tanggal = 1_000L,
            idPelanggan = pelangganId,
            idKurir = null,
            idProduk = produkId,
            jumlah = 2,
            hargaSatuan = 6000.0,
            metodeBayar = "-",
            status = "Kasbon",
            uangMuka = 0L,
            sisaKasbon = 12_000L
        ).toInt()

        val tx2Id = repo.recordTransaksi(
            tanggal = 2_000L,
            idPelanggan = pelangganId,
            idKurir = null,
            idProduk = produkId,
            jumlah = 1,
            hargaSatuan = 6000.0,
            metodeBayar = "-",
            status = "Kasbon",
            uangMuka = 0L,
            sisaKasbon = 6_000L
        ).toInt()

        // Simulate legacy bug state where customer already paid Rp 15.000 (sisa_kasbon = 3.000),
        // but both transaction notes remained Kasbon (12.000 + 6.000 = 18.000)
        db.pelangganDao().setKasbon(pelangganId, 3_000.0)

        val reconciledCount = repo.reconcileKasbonDiscrepancies()
        assertEquals(1, reconciledCount)

        val tx1After = db.transaksiDao().getDetailById(tx1Id)!!.transaksi
        val tx2After = db.transaksiDao().getDetailById(tx2Id)!!.transaksi

        assertEquals("Lunas", tx1After.status)
        assertEquals(0L, tx1After.sisaKasbon)

        assertEquals("Kasbon", tx2After.status)
        assertEquals("Bayar Sebagian", tx2After.metode_bayar)
        assertEquals(3_000L, tx2After.sisaKasbon)
        assertEquals(3_000L, tx2After.uangMuka)
    }
}
