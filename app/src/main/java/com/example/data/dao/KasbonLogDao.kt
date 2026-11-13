package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.KasbonLog
import kotlinx.coroutines.flow.Flow

@Dao
interface KasbonLogDao {
    @Query("SELECT * FROM kasbon_log ORDER BY tanggal DESC")
    fun getAll(): Flow<List<KasbonLog>>

    @Query("SELECT * FROM kasbon_log ORDER BY tanggal DESC")
    suspend fun getAllOnce(): List<KasbonLog>

    @Query("SELECT * FROM kasbon_log WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): KasbonLog?

    @Query("SELECT * FROM kasbon_log WHERE id_pelanggan = :idPelanggan ORDER BY tanggal DESC")
    fun getByPelangganId(idPelanggan: Int): Flow<List<KasbonLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: KasbonLog): Long

    @Update
    suspend fun update(log: KasbonLog)

    @Query("DELETE FROM kasbon_log WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM kasbon_log")
    suspend fun deleteAll()
}
