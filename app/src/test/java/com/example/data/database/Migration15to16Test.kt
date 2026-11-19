package com.example.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration15to16Test {

    private val testDbName = "migration-test-15-16"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private fun getMigratedRoomDatabase(): AppDatabase {
        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            testDbName
        )
            .addMigrations(
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18
            )
            .allowMainThreadQueries()
            .build()
        helper.closeWhenFinished(database)
        return database
    }

    @Test
    fun migrate15To16_parsesValidPelangganMarker_preservesCleanRow_andSafelySkipsMalformed() = runBlocking {
        helper.createDatabase(testDbName, 15).use { db ->
            db.execSQL(
                """
                INSERT INTO `pelanggan` (`id`, `nama`, `alamat`, `no_hp`, `sisa_kasbon`, `harga_khusus`, `id_kurir_langganan`, `updatedAt`, `syncId`)
                VALUES (1, 'Pak Budi', '[DIHAPUS:admin@depot.com] Alamat asli', '081111', 0.0, NULL, NULL, 1000, 'pel_1')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `pelanggan` (`id`, `nama`, `alamat`, `no_hp`, `sisa_kasbon`, `harga_khusus`, `id_kurir_langganan`, `updatedAt`, `syncId`)
                VALUES (2, 'Bu Sari', 'Jl. Melati No. 5', '082222', 0.0, NULL, NULL, 1000, 'pel_2')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `pelanggan` (`id`, `nama`, `alamat`, `no_hp`, `sisa_kasbon`, `harga_khusus`, `id_kurir_langganan`, `updatedAt`, `syncId`)
                VALUES (3, 'Pak Rusak', '[DIHAPUS:tanpa_kurung_tutup', '083333', 0.0, NULL, NULL, 1000, 'pel_3')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `pelanggan` (`id`, `nama`, `alamat`, `no_hp`, `sisa_kasbon`, `harga_khusus`, `id_kurir_langganan`, `updatedAt`, `syncId`)
                VALUES (4, 'Bu Kosong', '[DIHAPUS:] Gang Kenanga', '084444', 0.0, NULL, NULL, 1000, 'pel_4')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `transaksi` (`id`, `tanggal`, `id_pelanggan`, `id_kurir`, `id_produk`, `jumlah`, `harga_satuan`, `total_harga`, `total_komisi`, `metode_bayar`, `status`, `uangMuka`, `sisaKasbon`, `tanggalBayar`, `updatedAt`, `syncId`)
                VALUES (1, 1000, 1, NULL, 1, 2, 6000.0, 12000.0, 0.0, 'DEL:admin@depot.com|C', 'Dihapus', 0, 0, NULL, 1000, 'tx_1')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `transaksi` (`id`, `tanggal`, `id_pelanggan`, `id_kurir`, `id_produk`, `jumlah`, `harga_satuan`, `total_harga`, `total_komisi`, `metode_bayar`, `status`, `uangMuka`, `sisaKasbon`, `tanggalBayar`, `updatedAt`, `syncId`)
                VALUES (2, 1000, 2, NULL, 1, 1, 6000.0, 6000.0, 0.0, 'DEL:|C', 'Dihapus', 0, 0, NULL, 1000, 'tx_2')
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(testDbName, 16, true, AppDatabase.MIGRATION_15_16).close()

        val migratedDb = getMigratedRoomDatabase()

        // a) Format valid [DIHAPUS:admin@depot.com] Alamat asli
        val validPelanggan = requireNotNull(migratedDb.pelangganDao().getById(1))
        assertTrue(validPelanggan.isDeleted)
        assertEquals("admin@depot.com", validPelanggan.deletedBy)
        assertEquals("Alamat asli", validPelanggan.alamat)

        // b) Tanpa marker [DIHAPUS:] -> tidak berubah
        val cleanPelanggan = requireNotNull(migratedDb.pelangganDao().getById(2))
        assertFalse(cleanPelanggan.isDeleted)
        assertNull(cleanPelanggan.deletedBy)
        assertEquals("Jl. Melati No. 5", cleanPelanggan.alamat)

        // c) Format rusak ([DIHAPUS: tanpa ]) -> tidak crash, fallback aman
        val malformedPelanggan = requireNotNull(migratedDb.pelangganDao().getById(3))
        assertFalse(malformedPelanggan.isDeleted)
        assertNull(malformedPelanggan.deletedBy)
        assertEquals("[DIHAPUS:tanpa_kurung_tutup", malformedPelanggan.alamat)

        // c2) Format marker kosong [DIHAPUS:] -> fallback deletedBy ke 'Karyawan'
        val emptyActorPelanggan = requireNotNull(migratedDb.pelangganDao().getById(4))
        assertTrue(emptyActorPelanggan.isDeleted)
        assertEquals("Karyawan", emptyActorPelanggan.deletedBy)
        assertEquals("Gang Kenanga", emptyActorPelanggan.alamat)

        // Transaksi valid & malformed fallback ke 'Karyawan'
        val transactions = migratedDb.transaksiDao().getAllOnce()
        val validTx = transactions.first { it.id == 1 }
        assertTrue(validTx.isDeleted)
        assertEquals("admin@depot.com", validTx.deletedBy)

        val malformedTx = transactions.first { it.id == 2 }
        assertTrue(malformedTx.isDeleted)
        assertEquals("Karyawan", malformedTx.deletedBy)
    }
}
