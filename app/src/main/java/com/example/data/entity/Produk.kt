package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.SyncableEntity
import java.util.UUID

@Entity(
    tableName = "produk",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class Produk(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama_produk: String = "",
    val harga: Double = 0.0,
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncId: String = UUID.randomUUID().toString()
) : SyncableEntity
