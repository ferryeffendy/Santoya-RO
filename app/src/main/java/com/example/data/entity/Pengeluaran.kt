package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "pengeluaran",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class Pengeluaran(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: Long = 0L,
    val nama_item: String = "",
    val kategori: String = "",
    val nominal: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncId: String = UUID.randomUUID().toString()
)
