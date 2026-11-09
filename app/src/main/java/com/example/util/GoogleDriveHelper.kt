package com.example.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.File as JavaFile
import java.io.FileOutputStream

object GoogleDriveHelper {
    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun getSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun signInWithGoogle(activity: Activity, requestCode: Int = 9001) {
        val client = getSignInClient(activity)
        activity.startActivityForResult(client.signInIntent, requestCode)
    }

    private fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val accountObj = account.account ?: throw Exception("Belum login Google")
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = accountObj
        }
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        return Drive.Builder(transport, jsonFactory, credential)
            .setApplicationName("Santoya RO")
            .build()
    }

    fun uploadDatabase(context: Context, dbFile: JavaFile, account: GoogleSignInAccount): Boolean {
        val driveService = getDriveService(context, account)
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = 'santoya_ro_database.db'")
            .setFields("files(id, name)")
            .execute()

        val existingFiles = result.files
        val mediaContent = FileContent("application/x-sqlite3", dbFile)

        if (existingFiles != null && existingFiles.isNotEmpty()) {
            val fileId = existingFiles[0].id
            val fileMetadata = File().setName("santoya_ro_database.db")
            driveService.files().update(fileId, fileMetadata, mediaContent).execute()
        } else {
            val fileMetadata = File().apply {
                name = "santoya_ro_database.db"
                parents = listOf("appDataFolder")
            }
            driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
        }
        return true
    }

    fun downloadLatestBackup(context: Context, account: GoogleSignInAccount, destFile: JavaFile): Boolean {
        val driveService = getDriveService(context, account)
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = 'santoya_ro_database.db'")
            .setFields("files(id, name)")
            .execute()

        val files = result.files
        if (files != null && files.isNotEmpty()) {
            val fileId = files[0].id
            FileOutputStream(destFile).use { outputStream ->
                driveService.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream)
            }
            return true
        } else {
            throw Exception("File backup tidak ditemukan di Drive")
        }
    }
}
