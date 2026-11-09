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

    @Query("SELECT * FROM produk WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Produk?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(produk: Produk): Long

    @Update
    suspend fun update(produk: Produk)

    @Delete
    suspend fun delete(produk: Produk)
}
