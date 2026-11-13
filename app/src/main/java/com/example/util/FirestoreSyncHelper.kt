package com.example.util

import android.content.Context
import com.aistudio.companion.kzpmrw.R
import com.google.firebase.firestore.FirebaseFirestore

data class SyncSummary(
    val pelangganCount: Int = 0,
    val produkCount: Int = 0,
    val kurirCount: Int = 0,
    val transaksiCount: Int = 0,
    val pengeluaranCount: Int = 0,
    val message: String = ""
)

object FirestoreSyncHelper {
    fun getFirestore(context: Context): FirebaseFirestore {
        val dbId = context.getString(R.string.firestore_database_id)
        return FirebaseFirestore.getInstance(dbId)
    }
}
