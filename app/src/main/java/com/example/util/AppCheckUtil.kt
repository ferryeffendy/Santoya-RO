package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

// App Check contract keys. These strings are defined by the Firebase SDK and
// the runtime harness, so do not rename them.
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"
private val UUID_TOKEN_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

/**
 * Memvalidasi apakah string token debug App Check memenuhi format UUID standar 36 karakter.
 */
internal fun isValidAppCheckDebugToken(token: String?): Boolean {
    val clean = token?.trim().orEmpty()
    return clean.length == 36 && UUID_TOKEN_REGEX.matches(clean)
}

/**
 * Menginisialisasi Firebase App Check sesuai konfigurasi dan lingkungan:
 * - Jika launch intent menyertakan token debug (misal dari harness AI Studio / emulator), simpan secara sinkron via commit() dan gunakan DebugAppCheckProviderFactory.
 * - Jika tidak ada token debug intent, cek recaptcha_site_key dari konfigurasi proyek Firebase untuk validasi reCAPTCHA Enterprise otomatis pada perangkat nyata.
 * - Untuk release tanpa site key, gunakan PlayIntegrityAppCheckProviderFactory.
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        // 1. Ensure FirebaseApp is initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val persistenceKey = try {
            FirebaseApp.getInstance().persistenceKey
        } catch (_: Exception) {
            "default"
        }
        val prefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )

        // 2. Baca token debug dari launch intent (jika ada)
        val debugTokenExtra = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()
        if (!debugTokenExtra.isNullOrEmpty()) {
            // CRITICAL: MUST use commit() instead of apply() so the secret is written
            // synchronously before DebugAppCheckProviderFactory reads it.
            prefs.edit()
                .putString(DEBUG_SECRET_KEY, debugTokenExtra)
                .commit()
            Log.d(TAG, "App Check debug secret configured from intent successfully.")
        }

        val hasStoredDebugSecret = !prefs.getString(DEBUG_SECRET_KEY, null).isNullOrBlank()

        // 3. Resolusi recaptcha_site_key secara aman
        val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
        val siteKey = if (resId != 0) context.getString(resId).trim() else ""

        if (BuildConfig.DEBUG) {
            if (!debugTokenExtra.isNullOrEmpty() || hasStoredDebugSecret) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed.")
            } else if (siteKey.isNotBlank()) {
                // Di perangkat fisik nyata (tanpa intent debug token), pasang reCAPTCHA Enterprise
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                )
                Log.d(TAG, "RecaptchaAppCheckProviderFactory installed for debug build on device.")
            } else {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed as fallback.")
            }
        } else {
            if (siteKey.isNotBlank()) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                )
                Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
            } else {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "PlayIntegrityAppCheckProviderFactory installed.")
            }
        }
    } catch (e: Exception) {
        // Log explicitly to keep App Check attestation failures visible
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
