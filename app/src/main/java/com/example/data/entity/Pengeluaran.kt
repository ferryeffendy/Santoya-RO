package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pengeluaran")
data class Pengeluaran(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: Long,
    val nama_item: String,
    val kategori: String, // Operasional, Bahan Baku, Lainnya
    val nominal: Double
)
