package com.example.util

import android.content.Context

/**
 * Ringkasan jumlah data lokal yang tersimpan di perangkat saat Mode Offline.
 */
data class OfflineLocalSummary(
    val pelangganCount: Int = 0,
    val transaksiCount: Int = 0,
    val produkCount: Int = 0,
    val pendingCount: Int = 0
) {
    val hasAnyData: Boolean
        get() = pelangganCount > 0 || transaksiCount > 0 || produkCount > 0 || pendingCount > 0
}

/**
 * Metadata sesi terakhir yang digunakan di perangkat untuk mendukung fitur lanjut sesi terakhir.
 */
data class LastSessionInfo(
    val uid: String,
    val email: String,
    val displayName: String,
    val isOfflineLocal: Boolean,
    val role: DepotRole,
    val lastActiveAt: Long
)

/**
 * Pengelola status sesi Offline Login, deteksi sesi terakhir, dan flag penggabungan data lokal
 * ke akun Google saat pengguna melakukan login online di kemudian hari.
 */
class OfflineSessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Mengembalikan true jika aplikasi sedang berjalan dalam Mode Offline tanpa akun Google.
     */
    fun isOfflineModeActive(): Boolean = prefs.getBoolean(KEY_OFFLINE_MODE_ACTIVE, false)

    /**
     * Mengatur status aktif Mode Offline di penyimpanan lokal.
     */
    fun setOfflineModeActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE_MODE_ACTIVE, active).apply()
    }

    /**
     * Mengembalikan true jika terdapat data Mode Offline yang belum digabungkan ke akun Google.
     */
    fun hasPendingOfflineMerge(): Boolean = prefs.getBoolean(KEY_PENDING_OFFLINE_MERGE, false)

    /**
     * Menandai apakah data lokal Mode Offline perlu digabungkan saat login Google berikutnya.
     */
    fun setHasPendingOfflineMerge(pending: Boolean) {
        prefs.edit().putBoolean(KEY_PENDING_OFFLINE_MERGE, pending).apply()
    }

    /**
     * Menyimpan metadata sesi terakhir agar layar masuk dapat menampilkan informasi lanjut sesi.
     */
    fun saveLastSession(
        uid: String,
        email: String,
        displayName: String,
        isOfflineLocal: Boolean,
        role: DepotRole
    ) {
        if (uid.isBlank()) return
        prefs.edit()
            .putString(KEY_LAST_UID, uid)
            .putString(KEY_LAST_EMAIL, email.trim())
            .putString(KEY_LAST_DISPLAY_NAME, displayName.trim())
            .putBoolean(KEY_LAST_IS_OFFLINE, isOfflineLocal)
            .putString(KEY_LAST_ROLE, role.name)
            .putLong(KEY_LAST_ACTIVE_AT, System.currentTimeMillis())
            .apply()
    }

    /**
     * Mengambil metadata sesi terakhir yang pernah aktif, atau null jika belum ada.
     */
    fun getLastSession(): LastSessionInfo? {
        val uid = prefs.getString(KEY_LAST_UID, null)?.takeIf { it.isNotBlank() } ?: return null
        val email = prefs.getString(KEY_LAST_EMAIL, "").orEmpty()
        val displayName = prefs.getString(KEY_LAST_DISPLAY_NAME, "").orEmpty()
        val isOffline = prefs.getBoolean(KEY_LAST_IS_OFFLINE, false)
        val roleRaw = prefs.getString(KEY_LAST_ROLE, DepotRole.OWNER.name)
        val role = DepotRole.entries.firstOrNull { it.name == roleRaw } ?: DepotRole.OWNER
        val lastActiveAt = prefs.getLong(KEY_LAST_ACTIVE_AT, 0L)
        return LastSessionInfo(
            uid = uid,
            email = email,
            displayName = displayName,
            isOfflineLocal = isOffline,
            role = role,
            lastActiveAt = lastActiveAt
        )
    }

    /**
     * Menentukan apakah data Room lokal wajib dipertahankan dan digabung saat login Google baru,
     * bukan dihapus oleh mekanisme isolasi pergantian pemilik akun.
     */
    fun shouldPreserveLocalDataOnGoogleSignIn(
        newUid: String,
        previousOwnerUid: String?
    ): Boolean {
        if (newUid.isBlank() || newUid == OFFLINE_LOCAL_UID) return false
        if (!hasPendingOfflineMerge()) return false
        return previousOwnerUid.isNullOrBlank() ||
            previousOwnerUid == OFFLINE_LOCAL_UID ||
            previousOwnerUid == newUid
    }

    /**
     * Menghapus seluruh state sesi offline dan riwayat sesi terakhir (dipanggil saat logout penuh / reset).
     */
    fun clearAll() {
        prefs.edit()
            .remove(KEY_OFFLINE_MODE_ACTIVE)
            .remove(KEY_PENDING_OFFLINE_MERGE)
            .remove(KEY_LAST_UID)
            .remove(KEY_LAST_EMAIL)
            .remove(KEY_LAST_DISPLAY_NAME)
            .remove(KEY_LAST_IS_OFFLINE)
            .remove(KEY_LAST_ROLE)
            .remove(KEY_LAST_ACTIVE_AT)
            .apply()
    }

    companion object {
        const val OFFLINE_LOCAL_UID = "offline_local"
        const val OFFLINE_LOCAL_DEPOT_ID = "depot_offline_local"

        private const val PREFS_NAME = "santoya_offline_session_prefs"
        private const val KEY_OFFLINE_MODE_ACTIVE = "offline_mode_active"
        private const val KEY_PENDING_OFFLINE_MERGE = "pending_offline_merge"
        private const val KEY_LAST_UID = "last_uid"
        private const val KEY_LAST_EMAIL = "last_email"
        private const val KEY_LAST_DISPLAY_NAME = "last_display_name"
        private const val KEY_LAST_IS_OFFLINE = "last_is_offline"
        private const val KEY_LAST_ROLE = "last_role"
        private const val KEY_LAST_ACTIVE_AT = "last_active_at"
    }
}
