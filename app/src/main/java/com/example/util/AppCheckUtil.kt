package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.aistudio.companion.kzpmrw.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

// App Check contract keys. These strings are defined by the Firebase SDK and
// the runtime harness, so do not rename them.
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"
private const val DEBUG_FACTORY_CLASS = "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"

/**
 * Menginisialisasi Firebase App Check sesuai jenis build:
 * - Debug: memuat DebugAppCheckProviderFactory secara reflektif (agar library debug tidak bocor ke APK release).
 * - Release: memasang PlayIntegrityAppCheckProviderFactory sebagai provider utama Android (dengan fallback Recaptcha bila dikonfigurasi).
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        // 1. Ensure FirebaseApp is initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            // 2. Debug: Read debug token from Intent extra
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // CRITICAL: MUST use commit() instead of apply() so the secret is written
                // synchronously before DebugAppCheckProviderFactory reads it.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            val debugFactory = Class.forName(DEBUG_FACTORY_CLASS)
                .getMethod("getInstance")
                .invoke(null) as AppCheckProviderFactory
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(debugFactory)
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            // 3. Release: Prefer Play Integrity on Android; allow explicit recaptcha_site_key override if provided
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            val siteKey = if (resId != 0) context.getString(resId).trim() else ""
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
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
