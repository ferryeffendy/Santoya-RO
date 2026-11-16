package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

/**
 * Menyamarkan alamat email untuk log produksi agar tidak membocorkan PII di Logcat.
 */
fun redactEmailForLog(email: String?, redact: Boolean = !BuildConfig.DEBUG): String? {
    if (email.isNullOrBlank() || !redact) return email
    val trimmed = email.trim()
    val atIndex = trimmed.indexOf('@')
    if (atIndex <= 0 || atIndex == trimmed.lastIndex) return "***"
    val localPart = trimmed.substring(0, atIndex)
    val domainPart = trimmed.substring(atIndex)
    val visiblePrefix = when {
        localPart.length >= 3 -> localPart.take(2)
        else -> localPart.take(1)
    }
    return "$visiblePrefix***$domainPart"
}

/**
 * Menyamarkan UID atau identifier akun untuk log produksi agar tetap dapat dilacak tanpa mengekspos ID penuh.
 */
fun redactUidForLog(uid: String?, redact: Boolean = !BuildConfig.DEBUG): String? {
    if (uid.isNullOrBlank() || !redact) return uid
    val trimmed = uid.trim()
    if (trimmed.length <= 6) return "***"
    return "${trimmed.take(4)}***${trimmed.takeLast(2)}"
}

/**
 * Mencatat error Firestore terstruktur dengan penyamaran PII otomatis pada build produksi.
 */
fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = try {
        Firebase.auth.currentUser
    } catch (_: Exception) {
        null
    }

    val shouldRedact = !BuildConfig.DEBUG

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", redactEmailForLog(provider.email, shouldRedact))
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", redactUidForLog(currentUser?.uid, shouldRedact))
        put("email", redactEmailForLog(currentUser?.email, shouldRedact))
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", redactUidForLog(currentUser?.tenantId, shouldRedact))
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}
