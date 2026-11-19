package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.KurirDao
import com.example.data.dao.PelangganDao
import com.example.data.dao.PendingSyncDao
import com.example.data.dao.PengeluaranDao
import com.example.data.dao.PerawatanDao
import com.example.data.dao.PerawatanLogDao
import com.example.data.dao.ProdukDao
import com.example.data.dao.TangkiRefillDao
import com.example.data.dao.TransaksiDao
import com.example.data.entity.Kurir
import com.example.data.dao.KasbonLogDao
import com.example.data.entity.KasbonLog
import com.example.data.entity.Pelanggan
import com.example.data.entity.PendingSyncEntity
import com.example.data.entity.Pengeluaran
import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import com.example.data.entity.Produk
import com.example.data.entity.TangkiRefill
import com.example.data.entity.Transaksi
import com.example.util.PerawatanScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Produk::class,
        Kurir::class,
        Pelanggan::class,
        Pengeluaran::class,
        Transaksi::class,
        KasbonLog::class,
        PendingSyncEntity::class,
        PerawatanItem::class,
        PerawatanLog::class,
        TangkiRefill::class
    ],
    version = 18,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produkDao(): ProdukDao
    abstract fun kurirDao(): KurirDao
    abstract fun pelangganDao(): PelangganDao
    abstract fun pengeluaranDao(): PengeluaranDao
    abstract fun transaksiDao(): TransaksiDao
    abstract fun kasbonLogDao(): KasbonLogDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun perawatanDao(): PerawatanDao
    abstract fun perawatanLogDao(): PerawatanLogDao
    abstract fun tangkiRefillDao(): TangkiRefillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transaksi ADD COLUMN uangMuka INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transaksi ADD COLUMN sisaKasbon INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE transaksi SET sisaKasbon = CAST(total_harga AS INTEGER) WHERE LOWER(status) = 'kasbon' AND sisaKasbon = 0"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pelanggan ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transaksi ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE produk ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE kurir ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pengeluaran ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                val now = System.currentTimeMillis()
                db.execSQL("UPDATE pelanggan SET updatedAt = $now WHERE updatedAt = 0")
                db.execSQL("UPDATE transaksi SET updatedAt = $now WHERE updatedAt = 0")
                db.execSQL("UPDATE produk SET updatedAt = $now WHERE updatedAt = 0")
                db.execSQL("UPDATE kurir SET updatedAt = $now WHERE updatedAt = 0")
                db.execSQL("UPDATE pengeluaran SET updatedAt = $now WHERE updatedAt = 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaksi_new` (
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
                        `uangMuka` INTEGER NOT NULL DEFAULT 0,
                        `sisaKasbon` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT OR IGNORE INTO `transaksi_new` SELECT `id`, `tanggal`, `id_pelanggan`, `id_kurir`, `id_produk`, `jumlah`, `harga_satuan`, `total_harga`, `total_komisi`, `metode_bayar`, `status`, `uangMuka`, `sisaKasbon`, `updatedAt` FROM `transaksi`")
                db.execSQL("DROP TABLE `transaksi`")
                db.execSQL("ALTER TABLE `transaksi_new` RENAME TO `transaksi`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaksi_id_pelanggan` ON `transaksi` (`id_pelanggan`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaksi_id_kurir` ON `transaksi` (`id_kurir`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaksi_id_produk` ON `transaksi` (`id_produk`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaksi_tanggal` ON `transaksi` (`tanggal`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `kurir` ADD COLUMN `email` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `kurir` ADD COLUMN `akses_aktif` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `kurir` ADD COLUMN `skema_komisi` TEXT NOT NULL DEFAULT 'TETAP'")
                db.execSQL("ALTER TABLE `kurir` ADD COLUMN `aturan_komisi` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `kurir` ADD COLUMN `password` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val tables = listOf("produk", "kurir", "pelanggan", "pengeluaran", "transaksi", "kasbon_log")
                tables.forEach { table ->
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
                    db.execSQL("UPDATE `$table` SET `syncId` = 'legacy_${table}_' || `id` WHERE `syncId` = ''")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_syncId` ON `$table` (`syncId`)")
                }
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
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `kurir` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `perawatan_item` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nama` TEXT NOT NULL,
                        `intervalHari` INTEGER NOT NULL,
                        `terakhirSelesai` INTEGER,
                        `jadwalBerikutnya` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `syncId` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_perawatan_item_syncId` ON `perawatan_item` (`syncId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `perawatan_log` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `perawatanSyncId` TEXT NOT NULL,
                        `namaPerawatan` TEXT NOT NULL,
                        `tanggalSelesai` INTEGER NOT NULL,
                        `biaya` REAL NOT NULL,
                        `catatan` TEXT NOT NULL,
                        `pengeluaranSyncId` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        `syncId` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_perawatan_log_syncId` ON `perawatan_log` (`syncId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_perawatan_log_perawatanSyncId` ON `perawatan_log` (`perawatanSyncId`)")

                val now = System.currentTimeMillis()
                PerawatanScheduler.buildDefaultItems(now).forEach { item ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO `perawatan_item` (`nama`, `intervalHari`, `terakhirSelesai`, `jadwalBerikutnya`, `updatedAt`, `syncId`) VALUES (?, ?, NULL, ?, ?, ?)",
                        arrayOf<Any>(item.nama, item.intervalHari, item.jadwalBerikutnya, item.updatedAt, item.syncId)
                    )
                }
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transaksi` ADD COLUMN `tanggalBayar` INTEGER")
                db.execSQL("UPDATE `transaksi` SET `tanggalBayar` = `tanggal` WHERE LOWER(`status`) = 'lunas'")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pelanggan` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `pelanggan` ADD COLUMN `deletedBy` TEXT DEFAULT NULL")
                db.execSQL(
                    """
                    UPDATE `pelanggan`
                    SET `isDeleted` = 1,
                        `deletedBy` = CASE
                            WHEN INSTR(`alamat`, '[DIHAPUS:') > 0
                                 AND INSTR(`alamat`, ']') > INSTR(`alamat`, '[DIHAPUS:') + 9
                                 AND TRIM(SUBSTR(`alamat`, INSTR(`alamat`, '[DIHAPUS:') + 9, INSTR(`alamat`, ']') - (INSTR(`alamat`, '[DIHAPUS:') + 9))) != '' THEN
                                TRIM(SUBSTR(`alamat`, INSTR(`alamat`, '[DIHAPUS:') + 9, INSTR(`alamat`, ']') - (INSTR(`alamat`, '[DIHAPUS:') + 9)))
                            ELSE 'Karyawan'
                        END,
                        `alamat` = CASE
                            WHEN INSTR(`alamat`, ']') > INSTR(`alamat`, '[DIHAPUS:') THEN
                                TRIM(SUBSTR(`alamat`, INSTR(`alamat`, ']') + 1))
                            ELSE `alamat`
                        END
                    WHERE INSTR(`alamat`, '[DIHAPUS:') > 0
                      AND INSTR(`alamat`, ']') > INSTR(`alamat`, '[DIHAPUS:')
                    """.trimIndent()
                )

                db.execSQL("ALTER TABLE `transaksi` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `transaksi` ADD COLUMN `deletedBy` TEXT DEFAULT NULL")
                db.execSQL(
                    """
                    UPDATE `transaksi`
                    SET `isDeleted` = 1,
                        `deletedBy` = CASE
                            WHEN `metode_bayar` LIKE 'DEL:%|%'
                                 AND INSTR(`metode_bayar`, 'DEL:') = 1
                                 AND INSTR(`metode_bayar`, '|') > 5
                                 AND TRIM(SUBSTR(`metode_bayar`, 5, INSTR(`metode_bayar`, '|') - 5)) != '' THEN
                                TRIM(SUBSTR(`metode_bayar`, 5, INSTR(`metode_bayar`, '|') - 5))
                            WHEN `metode_bayar` LIKE 'DEL:%'
                                 AND INSTR(`metode_bayar`, 'DEL:') = 1
                                 AND LENGTH(`metode_bayar`) > 4
                                 AND INSTR(`metode_bayar`, '|') = 0
                                 AND TRIM(SUBSTR(`metode_bayar`, 5)) != '' THEN
                                TRIM(SUBSTR(`metode_bayar`, 5))
                            ELSE 'Karyawan'
                        END
                    WHERE LOWER(`status`) = 'dihapus'
                      AND (
                          (`metode_bayar` LIKE 'DEL:%|%' AND INSTR(`metode_bayar`, '|') > 5)
                          OR (`metode_bayar` LIKE 'DEL:%' AND LENGTH(`metode_bayar`) > 4)
                      )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transaksi` ADD COLUMN `waterType` TEXT NOT NULL DEFAULT 'RO'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tangki_refill` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tanggal` INTEGER NOT NULL,
                        `volumeLiter` REAL NOT NULL,
                        `totalHarga` REAL NOT NULL,
                        `namaPemasok` TEXT NOT NULL DEFAULT '',
                        `catatan` TEXT NOT NULL DEFAULT '',
                        `linkedExpenseId` INTEGER,
                        `pengeluaranSyncId` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        `syncId` TEXT NOT NULL,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0,
                        `deletedBy` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tangki_refill_tanggal` ON `tangki_refill` (`tanggal`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tangki_refill_syncId` ON `tangki_refill` (`syncId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tangki_refill_linkedExpenseId` ON `tangki_refill` (`linkedExpenseId`)")
                db.execSQL(
                    "INSERT OR IGNORE INTO `produk` (`id`, `nama_produk`, `harga`, `updatedAt`, `syncId`) VALUES (2, 'Galon Air Mineral 19L', 5000.0, 1, 'default_galon_mineral_19l')"
                )
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pelanggan_id_kurir_langganan` ON `pelanggan` (`id_kurir_langganan`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pelanggan_isDeleted` ON `pelanggan` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_kasbon_log_id_pelanggan` ON `kasbon_log` (`id_pelanggan`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_kasbon_log_tanggal` ON `kasbon_log` (`tanggal`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pengeluaran_tanggal` ON `pengeluaran` (`tanggal`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaksi_status` ON `transaksi` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaksi_id_pelanggan_status_isDeleted` ON `transaksi` (`id_pelanggan`, `status`, `isDeleted`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "santoya_ro_database"
                )
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .addMigrations(
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18
                    )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.execSQL(
                "INSERT OR IGNORE INTO `produk` (`id`, `nama_produk`, `harga`, `updatedAt`, `syncId`) VALUES (1, 'Galon Air RO 19L', 6000.0, 1, 'default_galon_ro_19l')"
            )
            db.execSQL(
                "INSERT OR IGNORE INTO `produk` (`id`, `nama_produk`, `harga`, `updatedAt`, `syncId`) VALUES (2, 'Galon Air Mineral 19L', 5000.0, 1, 'default_galon_mineral_19l')"
            )
            val now = System.currentTimeMillis()
            PerawatanScheduler.buildDefaultItems(now).forEach { item ->
                db.execSQL(
                    "INSERT OR IGNORE INTO `perawatan_item` (`nama`, `intervalHari`, `terakhirSelesai`, `jadwalBerikutnya`, `updatedAt`, `syncId`) VALUES (?, ?, NULL, ?, ?, ?)",
                    arrayOf<Any>(item.nama, item.intervalHari, item.jadwalBerikutnya, item.updatedAt, item.syncId)
                )
            }
        }
    }
}
