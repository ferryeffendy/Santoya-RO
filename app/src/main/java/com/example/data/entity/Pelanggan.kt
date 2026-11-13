package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "pelanggan",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class Pelanggan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String = "",
    val alamat: String = "",
    val no_hp: String = "",
    val sisa_kasbon: Double = 0.0,
    val harga_khusus: Double? = null,
    val id_kurir_langganan: Int? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncId: String = UUID.randomUUID().toString()
)
