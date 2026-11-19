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
        assertTrue(mutationTriggerCount >= 3)

        repo.bayarKasbonPelanggan(pelangganId, 5_000.0, "Cash")
        val pendingAfterPay = db.pendingSyncDao().getAllPending()
        assertTrue(pendingAfterPay.any { it.collectionName == SyncCollections.KASBON_LOGS && it.operation == SyncOperations.UPSERT })
        val kasbonLogs = db.kasbonLogDao().getAllOnce()
        assertTrue(kasbonLogs.all { it.jumlah_bayar >= 0.0 && it.sisa_kasbon_sebelum >= 0.0 && it.sisa_kasbon_sesudah >= 0.0 })

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
    fun clearLocalData_createsEmergencyAutoBackupBeforeWipingWhenPendingQueueIsNotEmpty() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = com.example.util.RealtimeSyncEngine(context, db)

        val pelanggan = Pelanggan(
            nama = "Pelanggan Offline Penting",
            alamat = "Jl. Melati No. 8",
            no_hp = "081234567890",
            syncId = "pel_offline_1",
            updatedAt = 10_000L
        )
        db.pelangganDao().insert(pelanggan)
        db.pendingSyncDao().enqueue(
            com.example.data.entity.PendingSyncEntity(
                collectionName = com.example.util.SyncCollections.PELANGGAN,
                syncId = "pel_offline_1",
                operation = com.example.util.SyncOperations.UPSERT,
                updatedAt = 10_000L
            )
        )
        assertEquals(1, db.pendingSyncDao().getQueueSize())

        engine.clearLocalData()

        // Local tables are cleared for the next account session
        assertEquals(0, db.pendingSyncDao().getQueueSize())
        assertEquals(0, db.pelangganDao().getAllOnce().size)

        // Emergency auto-backup JSON was preserved automatically before wipe
        assertTrue(com.example.util.ExportHelper.hasAutoBackup(context))
        val backupJson = com.example.util.ExportHelper.readAutoBackupJson(context).orEmpty()
        assertTrue(backupJson.contains("Pelanggan Offline Penting"))
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

    @Test
    fun repositoryMutations_useMonotonicTimestampsUnderClockSkew() = runBlocking {
        val futureTimestamp = System.currentTimeMillis() + 3_600_000L
        val prodId = db.produkDao().insert(
            Produk(
                nama_produk = "Galon RO 19L",
                harga = 6000.0,
                updatedAt = futureTimestamp,
                syncId = "prod_skew"
            )
        ).toInt()
        val storedProduk = requireNotNull(db.produkDao().getById(prodId))

        repo.updateProduk(storedProduk.copy(harga = 6500.0))

        val updatedProduk = requireNotNull(db.produkDao().getById(prodId))
        assertTrue(updatedProduk.updatedAt > futureTimestamp)

        val pendingProduk = requireNotNull(db.pendingSyncDao().getByKey(SyncCollections.PRODUK, "prod_skew"))
        assertEquals(updatedProduk.updatedAt, pendingProduk.updatedAt)
    }

    @Test
    fun enqueueAllLocalDataForSync_skipsDeletedPlaceholderCustomersAndOrphans() = runBlocking {
        val realCustId = repo.insertPelanggan(
            Pelanggan(nama = "Pelanggan Aktif", alamat = "Jl. Mawar", no_hp = "0811", syncId = "cust_real")
        ).toInt()
        val placeholderId = db.pelangganDao().insert(
            Pelanggan(
                nama = "Pelanggan Dihapus",
                alamat = "-",
                no_hp = "-",
                updatedAt = 0L,
                syncId = "cust_placeholder_orphan",
                isDeleted = true
            )
        ).toInt()
        db.transaksiDao().insert(
            com.example.data.entity.Transaksi(
                tanggal = 1_000L,
                id_pelanggan = placeholderId,
                id_kurir = null,
                id_produk = null,
                jumlah = 1,
                harga_satuan = 6000.0,
                total_harga = 6000.0,
                total_komisi = 0.0,
                metode_bayar = "Cash",
                status = "Lunas",
                updatedAt = 1_000L,
                syncId = "tx_orphan"
            )
        )
        db.pendingSyncDao().deleteAll()

        repo.enqueueAllLocalDataForSync()

        val queued = db.pendingSyncDao().getAllPending()
        assertTrue(queued.any { it.collectionName == SyncCollections.PELANGGAN && it.syncId == "cust_real" })
        assertTrue(queued.none { it.collectionName == SyncCollections.PELANGGAN && it.syncId == "cust_placeholder_orphan" })
        assertTrue(queued.none { it.collectionName == SyncCollections.TRANSAKSI && it.syncId == "tx_orphan" })
        assertTrue(realCustId > 0)
    }

    @Test
    fun exportHelperImport_normalizesTransaksiMathAndSoftDeletesFakeKurir() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeId = db.kurirDao().insert(
            com.example.data.entity.Kurir(nama = "Kurir 1", komisi_per_galon = 1000.0, akses_aktif = true, syncId = "fake_kurir_1")
        ).toInt()
        db.kurirDao().insert(
            com.example.data.entity.Kurir(nama = "Mustopa", komisi_per_galon = 1000.0, akses_aktif = true, syncId = "real_kurir_mustopa")
        )

        val backupJson = """
            {
              "master_produk": [
                { "id": 1, "nama_produk": "Galon RO 19L", "harga": 6000.0 }
              ],
              "data_pelanggan_nested": [
                {
                  "id": 10,
                  "nama": "Pak Hasan",
                  "alamat": "Blok A",
                  "no_hp": "08123",
                  "sisa_kasbon": 12000.0,
                  "riwayat_transaksi": [
                    {
                      "id": 100,
                      "tanggal": 1700000000000,
                      "produk": "Galon RO 19L",
                      "jumlah": 2,
                      "harga_satuan": 6000.0,
                      "total_harga": 99999.0,
                      "total_komisi": -50.0,
                      "metode_bayar": "Bayar Sebagian",
                      "status": "Kasbon",
                      "uangMuka": 2000,
                      "sisaKasbon": 500
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = com.example.util.ExportHelper.importJsonData(context, db, backupJson)
        assertTrue(result.success)

        // Fake kurir is soft-deleted rather than hard-deleted so cloud stays consistent
        val fakeAfter = requireNotNull(db.kurirDao().getById(fakeId))
        assertTrue(fakeAfter.isDeleted)
        assertTrue(!fakeAfter.akses_aktif)

        // Transaction math is normalized to jumlah * harga_satuan and valid Kasbon remainder
        val tx = db.transaksiDao().getAllOnce().first()
        assertEquals(12_000.0, tx.total_harga, 0.01)
        assertEquals(0.0, tx.total_komisi, 0.01)
        assertEquals("Kasbon", tx.status)
        assertEquals(2_000L, tx.uangMuka)
        assertEquals(10_000L, tx.sisaKasbon)
    }

    @Test
    fun deletePerawatanLog_deletesLinkedPengeluaranAndRollsBackSchedule() = runBlocking {
        val t1 = 1_700_000_000_000L
        val t2 = t1 + 30L * 86_400_000L
        val itemId = repo.insertPerawatan(
            com.example.data.entity.PerawatanItem(
                nama = "Filter Sedimen",
                intervalHari = 30,
                syncId = "ro_item_1"
            )
        ).toInt()
        val itemInitial = requireNotNull(db.perawatanDao().getById(itemId))
        repo.completePerawatan(item = itemInitial, tanggalSelesai = t1, biaya = 20_000.0, catatan = "Servis 1")

        val itemAfterFirst = requireNotNull(db.perawatanDao().getById(itemId))
        repo.completePerawatan(item = itemAfterFirst, tanggalSelesai = t2, biaya = 25_000.0, catatan = "Servis 2")

        val logsBefore = db.perawatanLogDao().getByPerawatanSyncId("ro_item_1")
        assertEquals(2, logsBefore.size)
        val latestLog = logsBefore.first { it.tanggalSelesai == t2 }
        val expSyncId = requireNotNull(latestLog.pengeluaranSyncId)
        assertNotNull(db.pengeluaranDao().getBySyncId(expSyncId))

        db.pendingSyncDao().deleteAll()
        repo.deletePerawatanLog(latestLog)

        // Linked Pengeluaran is deleted from Room and enqueued for cloud DELETE
        assertNull(db.pengeluaranDao().getBySyncId(expSyncId))
        val pending = db.pendingSyncDao().getAllPending()
        assertTrue(pending.any { it.collectionName == SyncCollections.PERAWATAN_LOG && it.operation == SyncOperations.DELETE && it.syncId == latestLog.syncId })
        assertTrue(pending.any { it.collectionName == SyncCollections.PENGELUARAN && it.operation == SyncOperations.DELETE && it.syncId == expSyncId })

        // Parent PerawatanItem schedule rolls back to t1
        val itemAfterRollback = requireNotNull(db.perawatanDao().getById(itemId))
        assertEquals(t1, itemAfterRollback.terakhirSelesai)
        assertEquals(com.example.util.PerawatanScheduler.calculateNextDueDateMillis(t1, 30), itemAfterRollback.jadwalBerikutnya)
    }

    @Test
    fun updateTransaksi_allowsCancellingInitialDpIntoFullKasbonWhilePreservingInstallments() = runBlocking {
        val custId = repo.insertPelanggan(
            Pelanggan(nama = "Pak Budi", alamat = "Blok C", no_hp = "08111", syncId = "cust_budi")
        ).toInt()
        val prodId = repo.insertProduk(
            Produk(nama_produk = "Galon RO", harga = 6000.0, syncId = "prod_ro")
        ).toInt()

        // 1. Record Bayar Sebagian with 4,000 DP out of 12,000 total
        val txId = repo.recordTransaksi(
            tanggal = 1_700_000_000_000L,
            idPelanggan = custId,
            idKurir = null,
            idProduk = prodId,
            jumlah = 2,
            hargaSatuan = 6000.0,
            metodeBayar = "Bayar Sebagian",
            status = "Kasbon",
            uangMuka = 4_000L,
            sisaKasbon = 8_000L
        ).toInt()

        assertEquals(8_000.0, requireNotNull(db.pelangganDao().getById(custId)).sisa_kasbon, 0.01)

        // 2. Edit transaction from Bayar Sebagian to Kasbon Penuh (uangMuka = 0, metodeBayar = "-")
        val existingTx = requireNotNull(db.transaksiDao().getDetailById(txId)).transaksi
        repo.updateTransaksi(
            id = txId,
            tanggal = existingTx.tanggal,
            idPelanggan = custId,
            idKurir = null,
            idProduk = prodId,
            jumlah = 2,
            hargaSatuan = 6000.0,
            metodeBayar = "-",
            status = "Kasbon",
            uangMuka = 0L
        )

        val updatedTx = requireNotNull(db.transaksiDao().getDetailById(txId)).transaksi
        assertEquals(0L, updatedTx.uangMuka)
        assertEquals(12_000L, updatedTx.sisaKasbon)
        assertEquals("Kasbon", updatedTx.status)
        assertEquals(12_000.0, requireNotNull(db.pelangganDao().getById(custId)).sisa_kasbon, 0.01)

        // 3. Now pay 5,000 via cicilan kasbon (creates KasbonLog referencing Nota #txId)
        repo.bayarKasbonPelanggan(custId, 5_000.0, "Cash")
        val txAfterCicilan = requireNotNull(db.transaksiDao().getDetailById(txId)).transaksi
        assertEquals(7_000L, txAfterCicilan.sisaKasbon)

        // 4. Editing quantity from 2 to 3 gallons (18,000 total) preserves the 5,000 cicilan credit -> sisaKasbon = 13,000
        repo.updateTransaksi(
            id = txId,
            tanggal = txAfterCicilan.tanggal,
            idPelanggan = custId,
            idKurir = null,
            idProduk = prodId,
            jumlah = 3,
            hargaSatuan = 6000.0,
            metodeBayar = "-",
            status = "Kasbon",
            uangMuka = 0L
        )
        val txAfterQtyEdit = requireNotNull(db.transaksiDao().getDetailById(txId)).transaksi
        assertEquals(13_000L, txAfterQtyEdit.sisaKasbon)
        assertEquals(13_000.0, requireNotNull(db.pelangganDao().getById(custId)).sisa_kasbon, 0.01)
    }

    @Test
    fun customerDeleteValidation_ignoresSoftDeletedKasbonAndGuardsPermanentDelete() = runBlocking {
        val custId = repo.insertPelanggan(
            Pelanggan(nama = "Bu Sari", alamat = "Jl. Kenanga", no_hp = "08199", syncId = "cust_sari")
        ).toInt()
        val prodId = repo.insertProduk(
            Produk(nama_produk = "Galon RO", harga = 6000.0, syncId = "prod_ro_2")
        ).toInt()

        val txId = repo.recordTransaksi(
            tanggal = 1_700_000_000_000L,
            idPelanggan = custId,
            idKurir = null,
            idProduk = prodId,
            jumlah = 1,
            hargaSatuan = 6000.0,
            metodeBayar = "-",
            status = "Kasbon",
            uangMuka = 0L,
            sisaKasbon = 6_000L
        ).toInt()

        // Permanent delete is blocked while active kasbon > 0
        val custWithDebt = requireNotNull(db.pelangganDao().getById(custId))
        repo.permanentlyDeletePelanggan(custWithDebt)
        assertNotNull(db.pelangganDao().getById(custId))
        assertTrue(!requireNotNull(db.pelangganDao().getById(custId)).isDeleted)

        // Soft-delete the kasbon transaction as karyawan (reconciles sisa_kasbon to 0)
        val txEntity = requireNotNull(db.transaksiDao().getDetailById(txId)).transaksi
        repo.softDeleteTransaksiByKaryawan(txEntity, "Mustopa")

        val custAfterTxDelete = requireNotNull(db.pelangganDao().getById(custId))
        assertEquals(0.0, custAfterTxDelete.sisa_kasbon, 0.01)

        // Karyawan can now soft-delete the customer because the only kasbon note was already soft-deleted
        assertTrue(repo.softDeletePelangganByKaryawan(custAfterTxDelete, "Mustopa"))
    }

    @Test
    fun exportAndImportJson_roundTripsFullV21BackupFidelity() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prod = Produk(id = 1, nama_produk = "Galon RO", harga = 6000.0, syncId = "sync_prod_1")
        val kurir = com.example.data.entity.Kurir(id = 1, nama = "Mustopa", komisi_per_galon = 1000.0, akses_aktif = true, syncId = "sync_kurir_1")
        val pelanggan = Pelanggan(id = 1, nama = "Pak Dedi", alamat = "Blok D", no_hp = "08122", sisa_kasbon = 0.0, syncId = "sync_cust_1")
        val tx = com.example.data.entity.Transaksi(
            id = 10,
            tanggal = 1_700_000_000_000L,
            tanggalBayar = 1_700_086_400_000L,
            id_pelanggan = 1,
            id_kurir = 1,
            id_produk = 1,
            jumlah = 2,
            harga_satuan = 6000.0,
            total_harga = 12_000.0,
            total_komisi = 2000.0,
            metode_bayar = "Transfer",
            status = "Lunas",
            uangMuka = 12_000L,
            sisaKasbon = 0L,
            syncId = "sync_tx_10"
        )
        val detail = com.example.data.entity.TransaksiDetail(
            transaksi = tx,
            pelanggan = pelanggan,
            kurir = kurir,
            produk = prod
        )
        val kasbonLog = com.example.data.entity.KasbonLog(
            id = 5,
            id_pelanggan = 1,
            tanggal = 1_700_086_400_000L,
            jumlah_bayar = 12_000.0,
            sisa_kasbon_sebelum = 12_000.0,
            sisa_kasbon_sesudah = 0.0,
            metode_bayar = "Transfer",
            catatan = "Pelunasan Nota #10",
            syncId = "sync_klog_5"
        )
        val perawatanItem = com.example.data.entity.PerawatanItem(
            id = 3,
            nama = "Membran RO",
            intervalHari = 180,
            terakhirSelesai = 1_700_000_000_000L,
            jadwalBerikutnya = 1_715_552_000_000L,
            syncId = "sync_ro_item_3"
        )
        val perawatanLog = com.example.data.entity.PerawatanLog(
            id = 7,
            perawatanSyncId = "sync_ro_item_3",
            namaPerawatan = "Membran RO",
            tanggalSelesai = 1_700_000_000_000L,
            biaya = 350_000.0,
            catatan = "Ganti baru",
            pengeluaranSyncId = "sync_exp_ro_7",
            syncId = "sync_ro_log_7"
        )
        val pengeluaran = com.example.data.entity.Pengeluaran(
            id = 9,
            tanggal = 1_700_000_000_000L,
            nama_item = "Perawatan: Membran RO",
            kategori = "Perawatan",
            nominal = 350_000.0,
            syncId = "sync_exp_ro_7"
        )

        val json = com.example.util.ExportHelper.generateNestedJson(
            pelangganList = listOf(pelanggan),
            transaksiList = listOf(detail),
            produkList = listOf(prod),
            kurirList = listOf(kurir),
            pengeluaranList = listOf(pengeluaran),
            kasbonLogList = listOf(kasbonLog),
            perawatanList = listOf(perawatanItem),
            perawatanLogList = listOf(perawatanLog)
        )

        val importResult = com.example.util.ExportHelper.importJsonData(context, db, json)
        assertTrue(importResult.success)

        val importedTx = requireNotNull(db.transaksiDao().getBySyncId("sync_tx_10"))
        assertEquals(1_700_086_400_000L, importedTx.tanggalBayar)
        assertNotNull(db.kasbonLogDao().getBySyncId("sync_klog_5"))
        assertNotNull(db.perawatanDao().getBySyncId("sync_ro_item_3"))
        val importedRoLog = requireNotNull(db.perawatanLogDao().getBySyncId("sync_ro_log_7"))
        assertEquals("sync_exp_ro_7", importedRoLog.pengeluaranSyncId)
    }
}



