package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface TransaksiDao {
    @Transaction
    @Query("SELECT * FROM transaksi ORDER BY tanggal DESC")
    fun getAllWithDetail(): Flow<List<TransaksiDetail>>

    @Transaction
    @Query("SELECT * FROM transaksi WHERE tanggal >= :startTime AND tanggal <= :endTime ORDER BY tanggal DESC")
    fun getByDateRangeWithDetail(startTime: Long, endTime: Long): Flow<List<TransaksiDetail>>

    @Transaction
    @Query("SELECT * FROM transaksi WHERE LOWER(status) = 'kasbon' ORDER BY tanggal DESC")
    fun getAllKasbonWithDetail(): Flow<List<TransaksiDetail>>

    @Transaction
    @Query("SELECT * FROM transaksi WHERE id_pelanggan = :pelangganId ORDER BY tanggal DESC")
    fun getByPelangganWithDetail(pelangganId: Int): Flow<List<TransaksiDetail>>

    @Query("SELECT tanggal FROM transaksi WHERE id_pelanggan = :pelangganId ORDER BY tanggal ASC")
    suspend fun getAllDatesByPelanggan(pelangganId: Int): List<Long>

    @Transaction
    @Query("SELECT * FROM transaksi WHERE id = :id LIMIT 1")
    suspend fun getDetailById(id: Int): TransaksiDetail?

    @Query("SELECT * FROM transaksi WHERE id_pelanggan = :pelangganId AND LOWER(status) = 'kasbon' ORDER BY tanggal ASC")
    suspend fun getUnpaidByPelanggan(pelangganId: Int): List<Transaksi>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaksi: Transaksi): Long

    @Update
    suspend fun update(transaksi: Transaksi)

    @Delete
    suspend fun delete(transaksi: Transaksi)

    @Query("UPDATE transaksi SET status = 'Lunas', sisaKasbon = 0 WHERE id = :id")
    suspend fun markAsLunas(id: Int)

    @Query("UPDATE transaksi SET id_produk = NULL WHERE id_produk = :produkId")
    suspend fun clearProdukFromTransaksi(produkId: Int)

    @Query("SELECT tanggal, SUM(total_harga) as total FROM transaksi WHERE tanggal >= :startTime GROUP BY tanggal ORDER BY tanggal ASC")
    fun getDailyRevenueLast30Days(startTime: Long): Flow<List<DailyRevenue>>

    @Query(
        """
        SELECT COALESCE(SUM(
            CASE 
                WHEN LOWER(status) = 'lunas' THEN total_harga 
                ELSE CAST(uangMuka AS REAL) 
            END
        ), 0.0)
        FROM transaksi
        WHERE tanggal >= :startTime AND tanggal <= :endTime
        """
    )
    fun getTotalPendapatanByDateRange(startTime: Long, endTime: Long): Flow<Double>
}

data class DailyRevenue(
    val tanggal: Long,
    val total: Double
)
