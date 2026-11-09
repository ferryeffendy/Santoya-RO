package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Pengeluaran
import kotlinx.coroutines.flow.Flow

@Dao
interface PengeluaranDao {
    @Query("SELECT * FROM pengeluaran ORDER BY tanggal DESC")
    fun getAll(): Flow<List<Pengeluaran>>

    @Query("SELECT * FROM pengeluaran WHERE tanggal >= :startTime AND tanggal <= :endTime ORDER BY tanggal DESC")
    fun getByRange(startTime: Long, endTime: Long): Flow<List<Pengeluaran>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pengeluaran: Pengeluaran): Long

    @Update
    suspend fun update(pengeluaran: Pengeluaran)

    @Delete
    suspend fun delete(pengeluaran: Pengeluaran)
}
