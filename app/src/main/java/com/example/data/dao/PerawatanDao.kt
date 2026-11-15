package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.PerawatanItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PerawatanDao {
    @Query("SELECT * FROM perawatan_item ORDER BY jadwalBerikutnya ASC, nama ASC")
    fun getAll(): Flow<List<PerawatanItem>>

    @Query("SELECT * FROM perawatan_item ORDER BY jadwalBerikutnya ASC, nama ASC")
    suspend fun getAllOnce(): List<PerawatanItem>

    @Query("SELECT * FROM perawatan_item WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): PerawatanItem?

    @Query("SELECT * FROM perawatan_item WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): PerawatanItem?

    @Query("SELECT COUNT(*) FROM perawatan_item")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PerawatanItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<PerawatanItem>)

    @Update
    suspend fun update(item: PerawatanItem)

    @Delete
    suspend fun delete(item: PerawatanItem)

    @Query("DELETE FROM perawatan_item WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM perawatan_item")
    suspend fun deleteAll()
}
