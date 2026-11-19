package com.example.util

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore

data class SyncSummary(
    val pelangganCount: Int = 0,
    val produkCount: Int = 0,
    val kurirCount: Int = 0,
    val transaksiCount: Int = 0,
    val pengeluaranCount: Int = 0,
    val kasbonLogCount: Int = 0,
    val perawatanCount: Int = 0,
    val tangkiRefillCount: Int = 0,
    val pushedCount: Int = 0,
    val message: String = ""
) {
    /**
     * Menghitung total seluruh dokumen operasional yang tersinkronisasi di database lokal.
     */
    fun totalRecordsCount(): Int =
        pelangganCount + produkCount + kurirCount + transaksiCount +
            pengeluaranCount + kasbonLogCount + perawatanCount + tangkiRefillCount
}

object FirestoreSyncHelper {
    fun getFirestore(context: Context): FirebaseFirestore {
        val dbId = context.getString(R.string.firestore_database_id)
        return FirebaseFirestore.getInstance(dbId)
    }
}
