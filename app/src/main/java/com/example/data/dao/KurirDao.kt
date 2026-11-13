package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Kurir
import kotlinx.coroutines.flow.Flow

@Dao
interface KurirDao {
    @Query("SELECT * FROM kurir ORDER BY nama ASC")
    fun getAll(): Flow<List<Kurir>>

    @Query("SELECT * FROM kurir ORDER BY nama ASC")
    suspend fun getAllOnce(): List<Kurir>

    @Query("SELECT * FROM kurir WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Kurir?

    @Query("SELECT * FROM kurir WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): Kurir?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(kurir: Kurir): Long

    @Update
    suspend fun update(kurir: Kurir)

    @Delete
    suspend fun delete(kurir: Kurir)

    @Query("DELETE FROM kurir WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("SELECT * FROM kurir WHERE LOWER(nama) = LOWER(:nama) LIMIT 1")
    suspend fun getByName(nama: String): Kurir?

    @Query("SELECT COUNT(*) FROM kurir")
    suspend fun count(): Int

    @Query("DELETE FROM kurir")
    suspend fun deleteAll()
}
