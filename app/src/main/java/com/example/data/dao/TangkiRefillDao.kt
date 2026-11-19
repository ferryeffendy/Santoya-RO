package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.TangkiRefill
import kotlinx.coroutines.flow.Flow

@Dao
interface TangkiRefillDao {
    @Query("SELECT * FROM tangki_refill WHERE isDeleted = 0 ORDER BY tanggal DESC")
    fun getAll(): Flow<List<TangkiRefill>>

    @Query("SELECT * FROM tangki_refill ORDER BY tanggal DESC")
    fun getAllIncludingDeleted(): Flow<List<TangkiRefill>>

    @Query("SELECT * FROM tangki_refill WHERE isDeleted = 0 ORDER BY tanggal DESC")
    suspend fun getAllOnce(): List<TangkiRefill>

    @Query("SELECT * FROM tangki_refill WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): TangkiRefill?

    @Query("SELECT * FROM tangki_refill WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): TangkiRefill?

    @Query("SELECT * FROM tangki_refill WHERE linkedExpenseId = :expenseId AND isDeleted = 0 LIMIT 1")
    suspend fun getByExpenseId(expenseId: Int): TangkiRefill?

    @Query("SELECT * FROM tangki_refill WHERE pengeluaranSyncId = :expenseSyncId AND isDeleted = 0 LIMIT 1")
    suspend fun getByExpenseSyncId(expenseSyncId: String): TangkiRefill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(refill: TangkiRefill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TangkiRefill>)

    @Update
    suspend fun update(refill: TangkiRefill)

    @Delete
    suspend fun delete(refill: TangkiRefill)

    @Query("UPDATE tangki_refill SET isDeleted = 1, deletedBy = :deletedBy, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Int, deletedBy: String?, now: Long)

    @Query("DELETE FROM tangki_refill WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM tangki_refill")
    suspend fun deleteAll()
}
