package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Helper terpusat untuk alur masuk Google menggunakan Jetpack Credential Manager
 * (`GetSignInWithGoogleOption`), dipakai bersama oleh layar login, pengaturan, dan panel sinkronisasi.
 */
object GoogleSignInHelper {

    /**
     * Memicu proses autentikasi Google interaktif dan menautkannya ke Firebase Auth.
     */
    fun signIn(
        context: Context,
        credentialManager: CredentialManager,
        coroutineScope: CoroutineScope,
        onLoadingChange: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onError(context.getString(R.string.auth_err_not_ready))
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        coroutineScope.launch {
            onLoadingChange(true)
            try {
                withTimeout(15_000L) {
                    val activity = context as? Activity
                        ?: throw IllegalStateException("Context bukan Activity")
                    val result = credentialManager.getCredential(activity, request)
                    val credential = result.credential
                    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        Firebase.auth.signInWithCredential(authCredential).await()
                        onSuccess()
                    } else {
                        onError(context.getString(R.string.auth_err_google_unreadable))
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In dibatalkan pengguna: ${e.message}", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In gagal", e)
                val msg = when {
                    e.message?.contains("network", ignoreCase = true) == true ->
                        context.getString(R.string.auth_err_network)
                    e.message?.contains("timed out", ignoreCase = true) == true ->
                        context.getString(R.string.auth_err_timeout)
                    else ->
                        context.getString(R.string.auth_err_generic)
                }
                onError(msg)
            } finally {
                onLoadingChange(false)
            }
        }
    }
}
