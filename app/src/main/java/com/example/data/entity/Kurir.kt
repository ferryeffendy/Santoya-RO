package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "kurir",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class Kurir(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String = "",
    val komisi_per_galon: Double = 0.0,
    val email: String = "",
    val password: String = "",
    val akses_aktif: Boolean = true,
    val skema_komisi: String = "TETAP",
    val aturan_komisi: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val syncId: String = UUID.randomUUID().toString()
)
