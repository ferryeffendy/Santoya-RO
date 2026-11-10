package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Pelanggan
import kotlinx.coroutines.flow.Flow

@Dao
interface PelangganDao {
    @Query("SELECT * FROM pelanggan ORDER BY nama ASC")
    fun getAll(): Flow<List<Pelanggan>>

    @Query("SELECT * FROM pelanggan WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Pelanggan?

    @Query("""
        SELECT p.* FROM pelanggan p
        WHERE COALESCE(p.sisa_kasbon, 0.0) > 0 
           OR p.id IN (SELECT DISTINCT id_pelanggan FROM transaksi WHERE LOWER(status) = 'kasbon')
        ORDER BY p.sisa_kasbon DESC, p.nama ASC
    """)
    fun getWithKasbon(): Flow<List<Pelanggan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pelanggan: Pelanggan): Long

    @Update
    suspend fun update(pelanggan: Pelanggan)

    @Delete
    suspend fun delete(pelanggan: Pelanggan)

    @Query("UPDATE pelanggan SET sisa_kasbon = CASE WHEN (COALESCE(sisa_kasbon, 0.0) + :delta) < 0 THEN 0.0 ELSE (COALESCE(sisa_kasbon, 0.0) + :delta) END WHERE id = :id")
    suspend fun updateKasbon(id: Int, delta: Double)

    @Query("UPDATE pelanggan SET sisa_kasbon = :newBalance WHERE id = :id")
    suspend fun setKasbon(id: Int, newBalance: Double)
}
