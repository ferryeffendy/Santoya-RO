package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Produk
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdukDao {
    @Query("SELECT * FROM produk ORDER BY nama_produk ASC")
    fun getAll(): Flow<List<Produk>>

    @Query("SELECT * FROM produk ORDER BY nama_produk ASC")
    suspend fun getAllOnce(): List<Produk>

    @Query("SELECT * FROM produk WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Produk?

    @Query("SELECT * FROM produk WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): Produk?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(produk: Produk): Long

    @Update
    suspend fun update(produk: Produk)

    @Delete
    suspend fun delete(produk: Produk)

    @Query("DELETE FROM produk WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("SELECT * FROM produk WHERE LOWER(nama_produk) = LOWER(:nama) LIMIT 1")
    suspend fun getByName(nama: String): Produk?

    @Query("SELECT COUNT(*) FROM produk")
    suspend fun count(): Int

    @Query("DELETE FROM produk")
    suspend fun deleteAll()
}
