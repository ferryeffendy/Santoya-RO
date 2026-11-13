package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "produk",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class Produk(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama_produk: String = "",
    val harga: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncId: String = UUID.randomUUID().toString()
)
