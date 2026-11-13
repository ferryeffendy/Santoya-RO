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
import com.example.data.dao.ProdukDao
import com.example.data.dao.TransaksiDao
import com.example.data.entity.Kurir
import com.example.data.dao.KasbonLogDao
import com.example.data.entity.KasbonLog
import com.example.data.entity.Pelanggan
import com.example.data.entity.PendingSyncEntity
import com.example.data.entity.Pengeluaran
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
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
        PendingSyncEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produkDao(): ProdukDao
    abstract fun kurirDao(): KurirDao
    abstract fun pelangganDao(): PelangganDao
    abstract fun pengeluaranDao(): PengeluaranDao
    abstract fun transaksiDao(): TransaksiDao
    abstract fun kasbonLogDao(): KasbonLogDao
    abstract fun pendingSyncDao(): PendingSyncDao

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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "santoya_ro_database"
                )
                    .addMigrations(
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                kotlinx.coroutines.delay(500)
                var retries = 0
                while (INSTANCE == null && retries < 50) {
                    kotlinx.coroutines.delay(100)
                    retries++
                }
                INSTANCE?.let { populateInitialData(it) }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            if (db.pelangganDao().count() > 0 || db.produkDao().count() > 0) {
                return
            }
            db.produkDao().insert(
                Produk(
                    nama_produk = "Galon Air RO 19L",
                    harga = 6000.0,
                    updatedAt = 1L,
                    syncId = "default_galon_ro_19l"
                )
            )
        }
    }
}
