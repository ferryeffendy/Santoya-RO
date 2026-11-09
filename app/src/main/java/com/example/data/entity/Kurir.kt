package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kurir")
data class Kurir(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val komisi_per_galon: Double
)
