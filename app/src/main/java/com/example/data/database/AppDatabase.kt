package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.KurirDao
import com.example.data.dao.PelangganDao
import com.example.data.dao.PengeluaranDao
import com.example.data.dao.ProdukDao
import com.example.data.dao.TransaksiDao
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
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
        Transaksi::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produkDao(): ProdukDao
    abstract fun kurirDao(): KurirDao
    abstract fun pelangganDao(): PelangganDao
    abstract fun pengeluaranDao(): PengeluaranDao
    abstract fun transaksiDao(): TransaksiDao

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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "santoya_ro_database"
                )
                    .addMigrations(MIGRATION_4_5)
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
                val database = getInstance(context)
                populateInitialData(database)
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // Default Products
            val p1 = db.produkDao().insert(Produk(nama_produk = "Galon Air RO 19L", harga = 6000.0))
            val p2 = db.produkDao().insert(Produk(nama_produk = "Galon Biasa / Mineral", harga = 5000.0))
            val p3 = db.produkDao().insert(Produk(nama_produk = "Tutup Galon & Tisu Higienis", harga = 1000.0))

            // Default Kurir
            val k1 = db.kurirDao().insert(Kurir(nama = "Budi Santoso", komisi_per_galon = 1000.0))
            val k2 = db.kurirDao().insert(Kurir(nama = "Agus Rahmat", komisi_per_galon = 1000.0))

            // Default Pelanggan
            val cust1 = db.pelangganDao().insert(
                Pelanggan(nama = "Bu Hj. Aminah", alamat = "RT 02 / RW 01 Desa Sukajaya", no_hp = "081234567890", sisa_kasbon = 12000.0)
            )
            val cust2 = db.pelangganDao().insert(
                Pelanggan(nama = "Pak RT Wahyu", alamat = "Blok B No. 14 Perum Indah", no_hp = "085678901234", sisa_kasbon = 0.0)
            )
            val cust3 = db.pelangganDao().insert(
                Pelanggan(nama = "Warung Bu Siti", alamat = "Jl. Raya Santoya No. 8", no_hp = "087788990011", sisa_kasbon = 24000.0)
            )
            val cust4 = db.pelangganDao().insert(
                Pelanggan(nama = "Keluarga Hendra", alamat = "Gang Mawar 3 No. 5", no_hp = "089912345678", sisa_kasbon = 0.0)
            )

            val now = System.currentTimeMillis()
            val dayMillis = 86400000L

            // Sample Transactions
            db.transaksiDao().insert(
                Transaksi(
                    tanggal = now - (3600000L * 2),
                    id_pelanggan = cust2.toInt(),
                    id_kurir = k1.toInt(),
                    id_produk = p1.toInt(),
                    jumlah = 3,
                    harga_satuan = 6000.0,
                    total_harga = 18000.0,
                    total_komisi = 3000.0,
                    metode_bayar = "Cash",
                    status = "Lunas",
                    uangMuka = 0L,
                    sisaKasbon = 0L
                )
            )
            db.transaksiDao().insert(
                Transaksi(
                    tanggal = now - (3600000L * 4),
                    id_pelanggan = cust1.toInt(),
                    id_kurir = k1.toInt(),
                    id_produk = p1.toInt(),
                    jumlah = 2,
                    harga_satuan = 6000.0,
                    total_harga = 12000.0,
                    total_komisi = 2000.0,
                    metode_bayar = "-",
                    status = "Kasbon",
                    uangMuka = 0L,
                    sisaKasbon = 12000L
                )
            )
            db.transaksiDao().insert(
                Transaksi(
                    tanggal = now - dayMillis,
                    id_pelanggan = cust3.toInt(),
                    id_kurir = k2.toInt(),
                    id_produk = p1.toInt(),
                    jumlah = 4,
                    harga_satuan = 6000.0,
                    total_harga = 24000.0,
                    total_komisi = 4000.0,
                    metode_bayar = "-",
                    status = "Kasbon",
                    uangMuka = 0L,
                    sisaKasbon = 24000L
                )
            )
            db.transaksiDao().insert(
                Transaksi(
                    tanggal = now - (dayMillis * 2),
                    id_pelanggan = cust4.toInt(),
                    id_kurir = k1.toInt(),
                    id_produk = p2.toInt(),
                    jumlah = 5,
                    harga_satuan = 5000.0,
                    total_harga = 25000.0,
                    total_komisi = 5000.0,
                    metode_bayar = "Cash",
                    status = "Lunas"
                )
            )

            // Sample Expenses
            db.pengeluaranDao().insert(
                Pengeluaran(
                    tanggal = now - (3600000L * 5),
                    nama_item = "Filter Spun Sediment & CTO",
                    kategori = "Bahan Baku",
                    nominal = 45000.0
                )
            )
            db.pengeluaranDao().insert(
                Pengeluaran(
                    tanggal = now - dayMillis,
                    nama_item = "Listrik Pompa & Ozon Generator",
                    kategori = "Operasional",
                    nominal = 35000.0
                )
            )
        }
    }
}
