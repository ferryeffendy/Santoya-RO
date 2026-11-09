package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.util.GoogleDriveHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val account = GoogleDriveHelper.getLastSignedInAccount(context) ?: throw Exception("Belum login Google")

            val dbFile = context.getDatabasePath("santoya_ro_database")
            val walFile = File(dbFile.parent, "santoya_ro_database-wal")
            val shmFile = File(dbFile.parent, "santoya_ro_database-shm")

            if (!dbFile.exists()) throw Exception("Database lokal tidak ditemukan")

            val cacheDb = File(context.cacheDir, "santoya_ro_database.db")
            val cacheWal = File(context.cacheDir, "santoya_ro_database.db-wal")
            val cacheShm = File(context.cacheDir, "santoya_ro_database.db-shm")

            copyFile(dbFile, cacheDb)
            if (walFile.exists()) copyFile(walFile, cacheWal)
            if (shmFile.exists()) copyFile(shmFile, cacheShm)

            val success = GoogleDriveHelper.uploadDatabase(context, cacheDb, account)

            cacheDb.delete()
            if (cacheWal.exists()) cacheWal.delete()
            if (cacheShm.exists()) cacheShm.delete()

            if (success) Result.success() else throw Exception("Gagal mengunggah database")
        } catch (e: Exception) {
            Log.e("SantoyaWorker", "Backup Error: ${e.message}", e)
            Result.failure(workDataOf("error_message" to (e.message ?: "Terjadi kesalahan saat backup")))
        }
    }

    private fun copyFile(src: File, dst: File) {
        FileInputStream(src).use { inStream ->
            FileOutputStream(dst).use { outStream ->
                inStream.channel.use { inChannel ->
                    outStream.channel.use { outChannel ->
                        inChannel.transferTo(0, inChannel.size(), outChannel)
                    }
                }
            }
        }
    }
}
