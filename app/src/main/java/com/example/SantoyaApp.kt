package com.example

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Custom [Application] class untuk inisialisasi Firebase Crashlytics.
 */
class SantoyaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }
    }
}
