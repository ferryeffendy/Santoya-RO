package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.database.AppDatabase
import com.example.util.GoogleDriveHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class RestoreWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val account = GoogleDriveHelper.getLastSignedInAccount(context) ?: throw Exception("Belum login Google")

            val tempFile = File(context.cacheDir, "downloaded_santoya.db")
            val success = GoogleDriveHelper.downloadLatestBackup(context, account, tempFile)
            if (!success || !tempFile.exists()) throw Exception("File backup tidak ditemukan di Drive")

            try {
                AppDatabase.getInstance(context).close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val dbFile = context.getDatabasePath("santoya_ro_database")
            val walFile = File(dbFile.parent, "santoya_ro_database-wal")
            val shmFile = File(dbFile.parent, "santoya_ro_database-shm")

            if (dbFile.exists()) dbFile.delete()
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            copyFile(tempFile, dbFile)
            tempFile.delete()
            Result.success()
        } catch (e: Exception) {
            Log.e("SantoyaWorker", "Restore Error: ${e.message}", e)
            Result.failure(workDataOf("error_message" to (e.message ?: "Terjadi kesalahan saat restore")))
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
