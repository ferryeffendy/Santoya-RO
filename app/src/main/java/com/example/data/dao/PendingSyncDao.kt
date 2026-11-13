package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {
    @Query("SELECT COUNT(*) FROM pending_sync")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM pending_sync ORDER BY updatedAt ASC")
    suspend fun getAllPending(): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync WHERE collectionName = :collectionName AND syncId = :syncId LIMIT 1")
    suspend fun getByKey(collectionName: String, syncId: String): PendingSyncEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: PendingSyncEntity)

    @Query("DELETE FROM pending_sync WHERE collectionName = :collectionName AND syncId = :syncId")
    suspend fun remove(collectionName: String, syncId: String)

    @Query("DELETE FROM pending_sync")
    suspend fun deleteAll()
}
