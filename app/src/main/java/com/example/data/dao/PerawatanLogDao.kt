package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.PerawatanLog
import kotlinx.coroutines.flow.Flow

@Dao
interface PerawatanLogDao {
    @Query("SELECT * FROM perawatan_log ORDER BY tanggalSelesai DESC, id DESC")
    fun getAll(): Flow<List<PerawatanLog>>

    @Query("SELECT * FROM perawatan_log ORDER BY tanggalSelesai DESC, id DESC")
    suspend fun getAllOnce(): List<PerawatanLog>

    @Query("SELECT * FROM perawatan_log WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): PerawatanLog?

    @Query("SELECT * FROM perawatan_log WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): PerawatanLog?

    @Query("SELECT * FROM perawatan_log WHERE perawatanSyncId = :perawatanSyncId ORDER BY tanggalSelesai DESC, id DESC")
    suspend fun getByPerawatanSyncId(perawatanSyncId: String): List<PerawatanLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: PerawatanLog): Long

    @Update
    suspend fun update(log: PerawatanLog)

    @Delete
    suspend fun delete(log: PerawatanLog)

    @Query("DELETE FROM perawatan_log WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM perawatan_log")
    suspend fun deleteAll()
}
