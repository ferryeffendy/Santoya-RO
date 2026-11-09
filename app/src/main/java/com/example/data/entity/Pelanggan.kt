package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pelanggan")
data class Pelanggan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val alamat: String,
    val no_hp: String,
    val sisa_kasbon: Double = 0.0,
    val harga_khusus: Double? = null,
    val id_kurir_langganan: Int? = null
)
