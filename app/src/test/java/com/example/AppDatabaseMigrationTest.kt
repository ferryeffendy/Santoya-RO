package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.SantoyaRepository
import com.example.util.PerawatanScheduler
import com.example.util.SyncCollections
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppDatabaseMigrationTest {

    private lateinit var context: Context
    private lateinit var dbName: String
    private var roomDb: AppDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbName = "test_santoya_migration_${UUID.randomUUID()}.db"
    }

    @After
    fun tearDown() {
        roomDb?.close()
        context.deleteDatabase(dbName)
    }

    private fun createV13DatabaseWithExistingData() {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        val sqlite = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqlite.use { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `produk` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `nama_produk` TEXT NOT NULL,
                    `harga` REAL NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncId` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_produk_syncId` ON `produk` (`syncId`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `kurir` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `nama` TEXT NOT NULL,
                    `komisi_per_galon` REAL NOT NULL,
                    `email` TEXT NOT NULL,
                    `password` TEXT NOT NULL,
                    `akses_aktif` INTEGER NOT NULL,
                    `skema_komisi` TEXT NOT NULL,
                    `aturan_komisi` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncId` TEXT NOT NULL,
                    `isDeleted` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_kurir_syncId` ON `kurir` (`syncId`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pelanggan` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `nama` TEXT NOT NULL,
                    `alamat` TEXT NOT NULL,
                    `no_hp` TEXT NOT NULL,
                    `sisa_kasbon` REAL NOT NULL,
                    `harga_khusus` REAL,
                    `id_kurir_langganan` INTEGER,
                    `updatedAt` INTEGER NOT NULL,
                    `syncId` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pelanggan_syncId` ON `pelanggan` (`syncId`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pengeluaran` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tanggal` INTEGER NOT NULL,
                    `nama_item` TEXT NOT NULL,
                    `kategori` TEXT NOT NULL,
                    `nominal` REAL NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncId` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pengeluaran_syncId` ON `pengeluaran` (`syncId`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transaksi` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tanggal` INTEGER NOT NULL,
                    `id_pelanggan` INTEGER NOT NULL,
                    `id_kurir` INTEGER,
                    `id_produk` INTEGER,
                    `jumlah` INTEGER NOT NULL,
                    `harga_satuan` REAL NOT NULL,
                    `total_harga` REAL NOT NULL,
                    `total_komisi` REAL NOT NULL,
                    `metode_bayar` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `uangMuka` INTEGER NOT NULL,
                    `sisaKasbon` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncId` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaksi_id_pelanggan` ON `transaksi` (`id_pelanggan`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaksi_id_kurir` ON `transaksi` (`id_kurir`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaksi_id_produk` ON `transaksi` (`id_produk`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaksi_tanggal` ON `transaksi` (`tanggal`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transaksi_syncId` ON `transaksi` (`syncId`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `kasbon_log` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `id_pelanggan` INTEGER NOT NULL,
                    `tanggal` INTEGER NOT NULL,
                    `jumlah_bayar` REAL NOT NULL,
                    `sisa_kasbon_sebelum` REAL NOT NULL,
                    `sisa_kasbon_sesudah` REAL NOT NULL,
                    `metode_bayar` TEXT NOT NULL,
                    `catatan` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncId` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_kasbon_log_syncId` ON `kasbon_log` (`syncId`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pending_sync` (
                    `collectionName` TEXT NOT NULL,
                    `syncId` TEXT NOT NULL,
                    `operation` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`collectionName`, `syncId`)
                )
                """.trimIndent()
            )

            // Seed existing depot data from version 13
            db.execSQL("INSERT INTO `produk` VALUES (1, 'Galon Air RO 19L', 6000.0, 1700000000000, 'prod_existing_1')")
            db.execSQL("INSERT INTO `pelanggan` VALUES (1, 'Bu Rina', 'Jl. Melati No. 5', '08123456789', 12000.0, NULL, NULL, 1700000000000, 'pel_existing_1')")
            db.execSQL("INSERT INTO `pengeluaran` VALUES (1, 1700000000000, 'Tutup Galon', 'Tutup & Tisu Galon', 45000.0, 1700000000000, 'exp_existing_1')")
            db.execSQL("INSERT INTO `transaksi` VALUES (1, 1700000000000, 1, NULL, 1, 2, 6000.0, 12000.0, 0.0, 'Cash', 'Lunas', 0, 0, 1700000000000, 'tx_existing_1')")
            db.version = 13
        }
    }

    @Test
    fun `migration 13 to 14 preserves all existing data and seeds 7 default perawatan items`() = runBlocking {
        createV13DatabaseWithExistingData()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18
            )
            .allowMainThreadQueries()
            .build()
        roomDb = db

        val products = db.produkDao().getAllOnce()
        val customers = db.pelangganDao().getAllOnce()
        val expenses = db.pengeluaranDao().getAllOnce()
        val transactions = db.transaksiDao().getAllOnce()

        assertTrue(products.isNotEmpty())
        assertTrue(products.any { it.nama_produk == "Galon Air RO 19L" })
        assertEquals(1, customers.size)
        assertEquals("Bu Rina", customers.first().nama)
        assertEquals(1, expenses.size)
        assertEquals("Tutup Galon", expenses.first().nama_item)
        assertEquals(1, transactions.size)
        assertEquals("tx_existing_1", transactions.first().syncId)

        val seededPerawatan = db.perawatanDao().getAllOnce()
        assertEquals(7, seededPerawatan.size)
        assertEquals(
            setOf(
                "Cek TDS",
                "Filter Sedimen",
                "Cuci Tangki",
                "Filter Karbon",
                "Membran RO",
                "Lampu UV",
                "Service Pompa"
            ),
            seededPerawatan.map { it.nama }.toSet()
        )
    }

    @Test
    fun `completePerawatan saves history, updates next due date, and creates Pengeluaran Perawatan when biaya positive`() = runBlocking {
        createV13DatabaseWithExistingData()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18
            )
            .allowMainThreadQueries()
            .build()
        roomDb = db
        val repo = SantoyaRepository(db)

        val filterSedimen = db.perawatanDao().getBySyncId("default_perawatan_filter_sedimen")
        requireNotNull(filterSedimen)

        val completionDate = LocalDate.of(2026, 6, 1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val saved = repo.completePerawatan(
            item = filterSedimen,
            tanggalSelesai = completionDate,
            biaya = 75000.0,
            catatan = "Ganti cartridge 5 micron"
        )
        assertTrue(saved)

        val updatedItem = db.perawatanDao().getBySyncId("default_perawatan_filter_sedimen")
        requireNotNull(updatedItem)
        assertEquals(completionDate, updatedItem.terakhirSelesai)
        assertEquals(
            PerawatanScheduler.calculateNextDueDateMillis(completionDate, 30),
            updatedItem.jadwalBerikutnya
        )

        val logs = db.perawatanLogDao().getAllOnce()
        assertEquals(1, logs.size)
        assertEquals("Filter Sedimen", logs.first().namaPerawatan)
        assertEquals(75000.0, logs.first().biaya, 0.01)
        assertEquals("Ganti cartridge 5 micron", logs.first().catatan)
        requireNotNull(logs.first().pengeluaranSyncId)

        val expenses = db.pengeluaranDao().getAllOnce()
        assertEquals(2, expenses.size)
        val perawatanExpense = expenses.first { it.syncId == logs.first().pengeluaranSyncId }
        assertEquals("Perawatan", perawatanExpense.kategori)
        assertEquals("Perawatan: Filter Sedimen", perawatanExpense.nama_item)
        assertEquals(75000.0, perawatanExpense.nominal, 0.01)
        assertEquals(completionDate, perawatanExpense.tanggal)

        val pending = db.pendingSyncDao().getAllPending()
        assertTrue(pending.any { it.collectionName == SyncCollections.PERAWATAN_JADWAL && it.syncId == updatedItem.syncId })
        assertTrue(pending.any { it.collectionName == SyncCollections.PERAWATAN_LOG && it.syncId == logs.first().syncId })
        assertTrue(pending.any { it.collectionName == SyncCollections.PENGELUARAN && it.syncId == perawatanExpense.syncId })

        // Test zero-cost completion skips creating an expense row
        val cekTds = db.perawatanDao().getBySyncId("default_perawatan_cek_tds")
        requireNotNull(cekTds)
        assertTrue(repo.completePerawatan(cekTds, completionDate, 0.0, "TDS 12 ppm"))
        val logsAfterZeroCost = db.perawatanLogDao().getAllOnce()
        assertEquals(2, logsAfterZeroCost.size)
        val tdsLog = logsAfterZeroCost.first { it.perawatanSyncId == cekTds.syncId }
        assertNull(tdsLog.pengeluaranSyncId)
        assertEquals(2, db.pengeluaranDao().getAllOnce().size)
    }

    @Test
    fun `migration 15 to 16 adds isDeleted and deletedBy and migrates legacy encoded records`() = runBlocking {
        createV13DatabaseWithExistingData()

        val dbFile = context.getDatabasePath(dbName)
        val sqlite = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqlite.use { db ->
            db.execSQL("INSERT INTO `pelanggan` VALUES (2, 'Pak Ahmad', '[DIHAPUS:Budi] Gang Mawar No. 3', '0811', 0.0, NULL, NULL, 1000, 'pel_del_2')")
            db.execSQL("INSERT INTO `pelanggan` VALUES (3, 'Bu Sari', 'Jl. Kenanga 12', '0812', 0.0, NULL, NULL, 1000, 'pel_act_3')")
            db.execSQL("INSERT INTO `transaksi` VALUES (2, 1000, 2, NULL, 1, 2, 6000.0, 12000.0, 0.0, 'DEL:Fitri|C', 'Dihapus', 0, 0, 1000, 'tx_del_2')")
            db.execSQL("INSERT INTO `transaksi` VALUES (3, 1000, 3, NULL, 1, 1, 6000.0, 6000.0, 0.0, 'Cash', 'Lunas', 0, 0, 1000, 'tx_act_3')")
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18
            )
            .allowMainThreadQueries()
            .build()
        roomDb = db

        val pDel = db.pelangganDao().getById(2)
        val pAct = db.pelangganDao().getById(3)
        requireNotNull(pDel)
        requireNotNull(pAct)
        assertTrue(pDel.isDeleted)
        assertEquals("Budi", pDel.deletedBy)
        assertEquals("Gang Mawar No. 3", pDel.alamat)

        assertFalse(pAct.isDeleted)
        assertNull(pAct.deletedBy)
        assertEquals("Jl. Kenanga 12", pAct.alamat)

        val txDel = db.transaksiDao().getAllOnce().first { it.id == 2 }
        val txAct = db.transaksiDao().getAllOnce().first { it.id == 3 }
        assertTrue(txDel.isDeleted)
        assertEquals("Fitri", txDel.deletedBy)
        assertFalse(txAct.isDeleted)
        assertNull(txAct.deletedBy)

        // Verify v17 -> v18 performance indexes were created
        val indexNames = mutableSetOf<String>()
        db.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'index'"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                indexNames.add(cursor.getString(0))
            }
        }
        assertTrue(indexNames.contains("index_pelanggan_id_kurir_langganan"))
        assertTrue(indexNames.contains("index_pelanggan_isDeleted"))
        assertTrue(indexNames.contains("index_kasbon_log_id_pelanggan"))
        assertTrue(indexNames.contains("index_kasbon_log_tanggal"))
        assertTrue(indexNames.contains("index_pengeluaran_tanggal"))
        assertTrue(indexNames.contains("index_transaksi_status"))
        assertTrue(indexNames.contains("index_transaksi_id_pelanggan_status_isDeleted"))
    }

    @Test
    fun `repository guards owner deletePelanggan on active kasbon, restores kasbon transaksi, unassigns deleted kurir, and reconciles two way kasbon`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        roomDb = db
        val repo = SantoyaRepository(db)

        val kurirId = repo.insertKurir(
            com.example.data.entity.Kurir(
                nama = "Kurir Anton",
                komisi_per_galon = 1000.0,
                email = "anton@gmail.com",
                akses_aktif = true
            )
        ).toInt()
        val produkId = repo.insertProduk(
            com.example.data.entity.Produk(
                nama_produk = "Galon RO 19L",
                harga = 6000.0
            )
        ).toInt()
        val custId = repo.insertPelanggan(
            com.example.data.entity.Pelanggan(
                nama = "Warung Sejahtera",
                alamat = "Desa Makmur",
                no_hp = "0812345678",
                sisa_kasbon = 0.0,
                id_kurir_langganan = kurirId
            )
        ).toInt()

        // 1. Catat transaksi Kasbon penuh (3 galon x 6000 = 18.000)
        val txId = repo.recordTransaksi(
            tanggal = 1_700_000_000_000L,
            idPelanggan = custId,
            idKurir = kurirId,
            idProduk = produkId,
            jumlah = 3,
            hargaSatuan = 6000.0,
            metodeBayar = "-",
            status = "Kasbon",
            uangMuka = 0L,
            sisaKasbon = 18_000L
        ).toInt()

        val custWithDebt = requireNotNull(repo.getPelangganById(custId))
        assertEquals(18_000.0, custWithDebt.sisa_kasbon, 0.01)

        // 2. Owner deletePelanggan wajib ditolak selama kasbon masih aktif
        assertFalse(repo.deletePelanggan(custWithDebt))
        assertFalse(requireNotNull(repo.getPelangganById(custId)).isDeleted)

        // 3. Soft-delete transaksi Kasbon lalu pulihkan kembali (termasuk jika sisaKasbon di-nolkan sebelum hapus)
        val rawTx = requireNotNull(repo.getTransaksiDetailById(txId)).transaksi.copy(sisaKasbon = 0L)
        db.transaksiDao().update(rawTx)
        repo.deleteTransaksi(rawTx)
        assertEquals(0.0, requireNotNull(repo.getPelangganById(custId)).sisa_kasbon, 0.01)

        val deletedTx = requireNotNull(repo.getTransaksiDetailById(txId)).transaksi
        assertTrue(deletedTx.isDeleted)
        assertEquals(18_000L, deletedTx.sisaKasbon)

        repo.restoreTransaksi(deletedTx)
        val restoredTx = requireNotNull(repo.getTransaksiDetailById(txId)).transaksi
        assertFalse(restoredTx.isDeleted)
        assertEquals("Kasbon", restoredTx.status)
        assertEquals("-", restoredTx.metode_bayar)
        assertEquals(18_000L, restoredTx.sisaKasbon)
        assertEquals(18_000.0, requireNotNull(repo.getPelangganById(custId)).sisa_kasbon, 0.01)

        // 4. Cicil Rp 8.000 via bayarKasbonPelanggan -> sisa 10.000, lalu edit jumlah galon dari 3 jadi 4 (total 24.000)
        repo.bayarKasbonPelanggan(custId, 8_000.0, "Cash")
        assertEquals(10_000.0, requireNotNull(repo.getPelangganById(custId)).sisa_kasbon, 0.01)

        repo.updateTransaksi(
            id = txId,
            tanggal = 1_700_000_000_000L,
            idPelanggan = custId,
            idKurir = kurirId,
            idProduk = produkId,
            jumlah = 4,
            hargaSatuan = 6000.0,
            metodeBayar = "-",
            status = "Kasbon",
            uangMuka = 0L
        )
        val editedTx = requireNotNull(repo.getTransaksiDetailById(txId)).transaksi
        assertEquals("Kasbon", editedTx.status)
        assertEquals(8_000L, editedTx.uangMuka)
        assertEquals(16_000L, editedTx.sisaKasbon)
        assertEquals(16_000.0, requireNotNull(repo.getPelangganById(custId)).sisa_kasbon, 0.01)

        // 5. Rekonsiliasi dua arah: jika seluruh nota sudah lunas namun sisa_kasbon pelanggan tertinggal > 0
        repo.markTransaksiLunas(txId, "Transfer")
        assertEquals(0.0, requireNotNull(repo.getPelangganById(custId)).sisa_kasbon, 0.01)
        db.pelangganDao().setKasbon(custId, 12_000.0, System.currentTimeMillis())
        assertEquals(12_000.0, requireNotNull(repo.getPelangganById(custId)).sisa_kasbon, 0.01)

        val reconciledCount = repo.reconcileKasbonDiscrepancies()
        assertEquals(1, reconciledCount)
        assertEquals(0.0, requireNotNull(repo.getPelangganById(custId)).sisa_kasbon, 0.01)

        // 6. Hapus kurir -> melepaskan id_kurir_langganan pelanggan
        val kurirEntity = requireNotNull(repo.getKurirById(kurirId))
        repo.deleteKurir(kurirEntity)
        val custAfterKurirDel = requireNotNull(repo.getPelangganById(custId))
        assertNull(custAfterKurirDel.id_kurir_langganan)

        // 7. Setelah kasbon 0, owner deletePelanggan berhasil dan mengisi isDeleted = true & deletedBy = "Pemilik"
        assertTrue(repo.deletePelanggan(custAfterKurirDel))
        val softDeletedCust = requireNotNull(repo.getPelangganById(custId))
        assertTrue(softDeletedCust.isDeleted)
        assertEquals("Pemilik", softDeletedCust.deletedBy)
    }
}
