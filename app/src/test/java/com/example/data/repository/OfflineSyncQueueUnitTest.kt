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

    @Test
    fun ensureDesignatedStaffKurirsForOwner_seedsThreeStaffEmailsWithoutOverwritingCustomOwnerEdits() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = com.example.util.RealtimeSyncEngine(context, db)

        // Pre-populate one customized courier for mumus8800@gmail.com
        val customMumus = com.example.data.entity.Kurir(
            nama = "Kang Mumus",
            komisi_per_galon = 1_500.0,
            email = "mumus8800@gmail.com",
            akses_aktif = true,
            syncId = "custom_mumus_sync",
            updatedAt = 50_000L
        )
        db.kurirDao().insert(customMumus)

        engine.ensureDesignatedStaffKurirsForOwner("ferryeffendy@gmail.com")

        val allKurirs = db.kurirDao().getAllOnce().filter { !it.isDeleted }
        assertEquals(3, allKurirs.size)

        val mumusAfter = allKurirs.first { it.email == "mumus8800@gmail.com" }
        assertEquals("Kang Mumus", mumusAfter.nama)
        assertEquals(1_500.0, mumusAfter.komisi_per_galon, 0.01)

        val radiAfter = allKurirs.first { it.email == "radijaya168@gmail.com" }
        assertEquals("Radijaya168", radiAfter.nama)
        assertEquals(1_000.0, radiAfter.komisi_per_galon, 0.01)
        assertTrue(radiAfter.akses_aktif)

        val fitriAfter = allKurirs.first { it.email == "fitriazizaheffendy@gmail.com" }
        assertEquals("Fitriazizaheffendy", fitriAfter.nama)
        assertEquals(1_000.0, fitriAfter.komisi_per_galon, 0.01)
        assertTrue(fitriAfter.akses_aktif)

        // Running again is idempotent (still 3 rows)
        engine.ensureDesignatedStaffKurirsForOwner("ferryeffendy@gmail.com")
        assertEquals(3, db.kurirDao().getAllOnce().count { !it.isDeleted })
    }

    @Test
    fun syncConflictResolver_resolvesByLatestUpdatedAt() {
        val localNewer = Produk(id = 1, nama_produk = "Galon Lokal", harga = 6000.0, updatedAt = 2_000L, syncId = "p_1")
        val remoteOlder = localNewer.copy(nama_produk = "Galon Cloud", updatedAt = 1_500L)
        assertEquals(localNewer, com.example.util.SyncConflictResolver.resolve(localNewer, remoteOlder))

        val remoteEqual = localNewer.copy(nama_produk = "Galon Sama", updatedAt = 2_000L)
        assertEquals(localNewer, com.example.util.SyncConflictResolver.resolve(localNewer, remoteEqual))

        val remoteNewer = localNewer.copy(nama_produk = "Galon Baru", updatedAt = 2_500L)
        assertEquals(remoteNewer, com.example.util.SyncConflictResolver.resolve(localNewer, remoteNewer))
    }

    @Test
    fun pendingSyncQueue_prunesStaleEntriesOnlyWhenQueueExceeds1000() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = com.example.util.RealtimeSyncEngine(context, db)
        val now = System.currentTimeMillis()
        val staleTimestamp = now - (35L * 24 * 60 * 60 * 1000)

        for (i in 1..1001) {
            db.pendingSyncDao().enqueue(
                com.example.data.entity.PendingSyncEntity(
                    collectionName = SyncCollections.PRODUK,
                    syncId = "sync_$i",
                    operation = SyncOperations.UPSERT,
                    updatedAt = if (i <= 50) staleTimestamp else now
                )
            )
        }
        assertEquals(1001, db.pendingSyncDao().getQueueSize())

        engine.validateAndPruneOfflineQueue()

        assertEquals(951, db.pendingSyncDao().getQueueSize())
    }

    @Test
    fun withRetry_retriesTransientFailuresAndReturnsOnSuccess() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = com.example.util.RealtimeSyncEngine(context, db)
        var attempts = 0

        val result = engine.withRetry(maxAttempts = 3, initialDelay = 1L) {
            attempts++
            if (attempts < 3) {
                throw IllegalStateException("Transient network error #$attempts")
            }
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(3, attempts)
    }

    @Test
    fun deleteTransaksi_performsSoftDeleteForDeltaSyncAndUpdatesKasbon() = runBlocking {
        val produkId = repo.insertProduk(Produk(nama_produk = "Galon RO", harga = 6000.0)).toInt()
        val pelangganId = repo.insertPelanggan(Pelanggan(nama = "Pak Anto", alamat = "Blok D", no_hp = "0819")).toInt()

        val txId = repo.recordTransaksi(
            tanggal = 3_000L,
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

        val savedTx = db.transaksiDao().getDetailById(txId)!!.transaksi
        repo.deleteTransaksi(savedTx)

        val softDeleted = db.transaksiDao().getBySyncId(savedTx.syncId)
        assertNotNull(softDeleted)
        assertTrue(softDeleted!!.isDeleted)
        assertEquals(com.example.util.KaryawanScopeHelper.STATUS_DIHAPUS, softDeleted.status)

        val custAfterSoftDelete = db.pelangganDao().getById(pelangganId)!!
        assertEquals(0.0, custAfterSoftDelete.sisa_kasbon, 0.01)

        val queueAfterSoftDelete = db.pendingSyncDao().getByKey(SyncCollections.TRANSAKSI, savedTx.syncId)
        assertNotNull(queueAfterSoftDelete)
        assertEquals(SyncOperations.UPSERT, queueAfterSoftDelete!!.operation)

        repo.permanentDeleteTransaksi(softDeleted)
        assertNull(db.transaksiDao().getBySyncId(savedTx.syncId))

        val queueAfterPermanentDelete = db.pendingSyncDao().getByKey(SyncCollections.TRANSAKSI, savedTx.syncId)
        assertNotNull(queueAfterPermanentDelete)
        assertEquals(SyncOperations.DELETE, queueAfterPermanentDelete!!.operation)
    }

    @Test
    fun deltaCursor_ignoresCacheAndAdvancesOrResetsCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = com.example.util.RealtimeSyncEngine(context, db)
        val depotId = "depot_test_1"

        engine.clearAllDeltaCursors()
        assertNull(engine.getDeltaCursor(depotId, SyncCollections.TRANSAKSI))

        // Cache-only empty snapshot must not initialize cursor
        engine.advanceDeltaCursor(
            depotId = depotId,
            colName = SyncCollections.TRANSAKSI,
            docs = emptyList(),
            isFromCache = true,
            markInitializedIfEmpty = true
        )
        assertNull(engine.getDeltaCursor(depotId, SyncCollections.TRANSAKSI))

        // Server empty bootstrap snapshot initializes sentinel cursor > 0 nanoseconds
        engine.advanceDeltaCursor(
            depotId = depotId,
            colName = SyncCollections.TRANSAKSI,
            docs = emptyList(),
            isFromCache = false,
            markInitializedIfEmpty = true
        )
        val initialized = engine.getDeltaCursor(depotId, SyncCollections.TRANSAKSI)
        assertNotNull(initialized)
        assertEquals(0L, initialized!!.seconds)
        assertEquals(1, initialized.nanoseconds)

        engine.clearDeltaCursorsForDepot(depotId)
        assertNull(engine.getDeltaCursor(depotId, SyncCollections.TRANSAKSI))
    }
}


