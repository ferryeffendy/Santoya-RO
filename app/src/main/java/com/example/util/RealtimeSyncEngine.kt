package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.room.withTransaction
import com.example.BuildConfig
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.entity.DepotProfile
import com.example.data.entity.KasbonLog
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.PendingSyncEntity
import com.example.data.entity.Pengeluaran
import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import com.example.data.entity.Produk
import com.example.data.entity.TangkiRefill
import com.example.data.entity.Transaksi
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

enum class DepotRole {
    OWNER,
    STAFF
}

enum class SyncConnectionStatus {
    OFFLINE,
    CONNECTING,
    SYNCING,
    LIVE,
    REVOKED,
    AWAITING_ROLE_CONFIRMATION,
    ERROR
}

data class RealtimeSyncState(
    val status: SyncConnectionStatus = SyncConnectionStatus.OFFLINE,
    val role: DepotRole = DepotRole.OWNER,
    val depotId: String = "",
    val ownerEmail: String = "",
    val pendingCount: Int = 0,
    val pendingByCollection: Map<String, Int> = emptyMap(),
    val isOnline: Boolean = true,
    val lastSyncedAt: Long? = null,
    val errorMessage: String? = null,
    val isSwitchingOwner: Boolean = false,
    val syncStageLabel: String? = null,
    val syncProgressFraction: Float? = null,
    val activeListenerCount: Int = 0,
    val expectedListenerCount: Int = 0,
    val autoRetryAttempt: Int = 0,
    val maxAutoRetryAttempts: Int = 5,
    val isAutoRetryScheduled: Boolean = false,
    val sessionReads: Int = 0,
    val sessionWrites: Int = 0
)

object SyncCollections {
    const val PRODUK = "produk"
    const val KURIR = "kurir"
    const val PELANGGAN = "pelanggan"
    const val TRANSAKSI = "transaksi"
    const val PENGELUARAN = "pengeluaran"
    const val KASBON_LOGS = "kasbon_logs"
    const val PERAWATAN_JADWAL = "perawatan_jadwal"
    const val PERAWATAN_LOG = "perawatan_log"
    const val TANGKI_REFILL = "tangki_refill"
    const val PROFIL = "profil"

    val ORDERED_DISPLAY_KEYS: List<String> = listOf(
        TRANSAKSI,
        PELANGGAN,
        KASBON_LOGS,
        PRODUK,
        KURIR,
        PENGELUARAN,
        PERAWATAN_JADWAL,
        PERAWATAN_LOG,
        TANGKI_REFILL,
        PROFIL
    )
}

object SyncOperations {
    const val UPSERT = "UPSERT"
    const val DELETE = "DELETE"
}

class RealtimeSyncEngine(
    private val context: Context,
    private val db: AppDatabase,
    private val firestoreProvider: () -> FirebaseFirestore = {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    },
    val profileStore: DepotProfileStore = DepotProfileStore(context)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val applyMutex = Mutex()
    private val flushMutex = Mutex()
    private val staffLinkSyncMutex = Mutex()
    private val profileSyncMutex = Mutex()

    val offlineSessionManager: OfflineSessionManager = OfflineSessionManager(context)

    private val _state = MutableStateFlow(RealtimeSyncState())
    val state: StateFlow<RealtimeSyncState> = _state.asStateFlow()

    private val _offlineMergeMessage = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val offlineMergeMessage: SharedFlow<String> = _offlineMergeMessage.asSharedFlow()

    /**
     * Mengembalikan true apabila mesin sinkronisasi sedang berjalan dalam Mode Offline lokal tanpa akun Google.
     */
    fun isOfflineLocalSession(): Boolean = activeUid == OfflineSessionManager.OFFLINE_LOCAL_UID

    private fun recordReads(count: Int) {
        // No-op: fitur metrik sesi Firestore telah dihapus dari antarmuka Pengaturan
    }

    private fun recordWrites(count: Int) {
        // No-op: fitur metrik sesi Firestore telah dihapus dari antarmuka Pengaturan
    }

    @Volatile
    private var activeUid: String? = null
    @Volatile
    private var activeEmail: String = ""
    @Volatile
    private var activeDisplayName: String = ""
    @Volatile
    private var activeDepotId: String = ""
    @Volatile
    private var activeRole: DepotRole = DepotRole.OWNER
    @Volatile
    private var hasSeededInitialOwnerPush: Boolean = false
    @Volatile
    private var lastDepotMirroredTier: String? = null

    private var staffLinkListener: ListenerRegistration? = null
    private var userProfileListener: ListenerRegistration? = null
    private var depotDocListener: ListenerRegistration? = null
    private var lastStaffLinkSnapshot: DocumentSnapshot? = null
    private val collectionListeners = ConcurrentHashMap<String, ListenerRegistration>()
    private val failedCollections = ConcurrentHashMap.newKeySet<String>()
    private val permissionDeniedAttempts = ConcurrentHashMap<String, Int>()
    private val attachGeneration = AtomicInteger(0)
    private var pendingObserverJob: Job? = null
    private var roleResolveJob: Job? = null
    private var switchDepotJob: Job? = null
    private var userTouchJob: Job? = null
    private var profileSyncJob: Job? = null
    private var flushJob: Job? = null
    private var autoRetryJob: Job? = null
    private var autoRetryCount: Int = 0
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val cachedSnapshots = mutableMapOf<String, List<DocumentSnapshot>>()
    private val receivedInitialCollections = mutableSetOf<String>()

    private fun isTerminalBlockingStatus(status: SyncConnectionStatus): Boolean {
        return status == SyncConnectionStatus.REVOKED ||
            status == SyncConnectionStatus.AWAITING_ROLE_CONFIRMATION
    }

    private fun keyConfirmedOwner(uid: String): String = "${KEY_CONFIRMED_OWNER_PREFIX}$uid"
    private fun keyLastRole(uid: String): String = "${KEY_LAST_ROLE_PREFIX}$uid"
    private fun keyLastDepot(uid: String): String = "${KEY_LAST_DEPOT_UID_PREFIX}$uid"
    private fun keyLastOwnerEmail(uid: String): String = "${KEY_LAST_OWNER_EMAIL_PREFIX}$uid"

    /**
     * Memulai Mode Offline lokal tanpa akun Google sehingga seluruh operasional kasir, pelanggan,
     * dan pembukuan tetap berjalan penuh di Room serta ditandai siap digabung saat login Google nanti.
     */
    fun startOfflineLocal(displayName: String? = null) {
        val offlineUid = OfflineSessionManager.OFFLINE_LOCAL_UID
        if (activeUid == offlineUid && activeDepotId == OfflineSessionManager.OFFLINE_LOCAL_DEPOT_ID) {
            offlineSessionManager.setOfflineModeActive(true)
            return
        }
        stop()

        activeUid = offlineUid
        activeEmail = ""
        activeDisplayName = displayName?.trim().orEmpty().ifBlank { "Pemilik Offline" }.take(120)
        activeDepotId = OfflineSessionManager.OFFLINE_LOCAL_DEPOT_ID
        activeRole = DepotRole.OWNER
        hasSeededInitialOwnerPush = false

        val appVersion = BuildConfig.VERSION_NAME.take(40)
        profileStore.loadForUser(
            uid = offlineUid,
            email = "",
            googleDisplayName = activeDisplayName,
            appVersion = appVersion,
            defaultRole = DepotProfile.ROLE_OWNER
        )

        prefs.edit()
            .putString(KEY_LAST_OWNER_UID, offlineUid)
            .putString(KEY_LAST_DEPOT_ID, OfflineSessionManager.OFFLINE_LOCAL_DEPOT_ID)
            .apply()
        offlineSessionManager.setOfflineModeActive(true)
        offlineSessionManager.setHasPendingOfflineMerge(true)
        offlineSessionManager.saveLastSession(
            uid = offlineUid,
            email = "",
            displayName = activeDisplayName,
            isOfflineLocal = true,
            role = DepotRole.OWNER
        )

        val initialOnline = checkIsOnline()
        _state.update {
            it.copy(
                status = SyncConnectionStatus.OFFLINE,
                role = DepotRole.OWNER,
                depotId = OfflineSessionManager.OFFLINE_LOCAL_DEPOT_ID,
                ownerEmail = "",
                isOnline = initialOnline,
                errorMessage = null
            )
        }

        registerNetworkCallback()
        observePendingQueue()

        scope.launch {
            validateAndPruneOfflineQueue()
            if (activeUid != offlineUid) return@launch
            try {
                if (db.perawatanDao().count() == 0) {
                    db.perawatanDao().insertAllIgnore(PerawatanScheduler.buildDefaultItems())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Gagal menyiapkan jadwal perawatan default offline: ${e.message}")
            }
        }
    }

    /**
     * Memulai sinkronisasi real-time depot sekaligus memuat profil offline dan mencatat pendaftar
     * ke `users/{uid}` di latar belakang tanpa menghambat pengguna masuk ke aplikasi.
     * Jika terdapat sesi Mode Offline sebelumnya, data lokal dipertahankan untuk digabungkan ke akun Google.
     */
    fun start(uid: String, email: String?, displayName: String? = null) {
        val cleanEmail = email?.trim()?.lowercase().orEmpty()
        val cleanDisplayName = displayName?.trim().orEmpty().take(120)
        if (activeUid == uid && activeEmail == cleanEmail && staffLinkListener != null) {
            if (cleanDisplayName.isNotBlank() && activeDisplayName != cleanDisplayName) {
                activeDisplayName = cleanDisplayName
            }
            return
        }
        stop()

        offlineSessionManager.setOfflineModeActive(false)
        activeUid = uid
        activeEmail = cleanEmail
        activeDisplayName = cleanDisplayName
        hasSeededInitialOwnerPush = false

        val appVersion = BuildConfig.VERSION_NAME.take(40)
        val previousOwnerUid = prefs.getString(KEY_LAST_OWNER_UID, null)
        val shouldPreserveOfflineMerge = offlineSessionManager.shouldPreserveLocalDataOnGoogleSignIn(
            newUid = uid,
            previousOwnerUid = previousOwnerUid
        )

        if (shouldPreserveOfflineMerge) {
            profileStore.mergeFromOfflineProfile(
                offlineUid = OfflineSessionManager.OFFLINE_LOCAL_UID,
                targetUid = uid,
                email = cleanEmail,
                googleDisplayName = cleanDisplayName,
                appVersion = appVersion
            )
        } else {
            profileStore.loadForUser(
                uid = uid,
                email = cleanEmail,
                googleDisplayName = cleanDisplayName,
                appVersion = appVersion
            )
        }

        val initialOnline = checkIsOnline()
        _state.update {
            it.copy(
                status = if (initialOnline) SyncConnectionStatus.CONNECTING else SyncConnectionStatus.OFFLINE,
                isOnline = initialOnline
            )
        }

        registerNetworkCallback()
        observePendingQueue()
        attachUserProfileListener(uid)
        userTouchJob?.cancel()
        userTouchJob = scope.launch {
            touchUserProfileDocument(
                uid = uid,
                email = cleanEmail,
                googleDisplayName = cleanDisplayName,
                appVersion = appVersion
            )
        }

        scope.launch {
            validateAndPruneOfflineQueue()
            if (activeUid != uid) return@launch

            if (shouldPreserveOfflineMerge) {
                prefs.edit().putString(KEY_LAST_OWNER_UID, uid).apply()
                continueStart(uid, cleanEmail)
            } else if (!previousOwnerUid.isNullOrBlank() && previousOwnerUid != uid) {
                // Jangan pernah mempercayai data lokal saat pemilik akun berganti.
                // Bersihkan dulu, baru lanjut memuat depot milik pemilik baru.
                _state.update { it.copy(isSwitchingOwner = true) }
                val wiped = try {
                    clearLocalDataInternal()
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal menghapus data pemilik sebelumnya", e)
                    false
                }
                if (activeUid != uid) return@launch
                if (!wiped) {
                    // Jangan lanjut memakai data lama: lebih aman menolak sesi.
                    _state.update {
                        it.copy(
                            isSwitchingOwner = false,
                            status = SyncConnectionStatus.ERROR,
                            errorMessage = "Gagal membersihkan data akun sebelumnya. Coba login ulang."
                        )
                    }
                    return@launch
                }
                prefs.edit().putString(KEY_LAST_OWNER_UID, uid).apply()
                _state.update { it.copy(isSwitchingOwner = false) }
                continueStart(uid, cleanEmail)
            } else {
                if (previousOwnerUid.isNullOrBlank()) {
                    prefs.edit().putString(KEY_LAST_OWNER_UID, uid).apply()
                }
                continueStart(uid, cleanEmail)
            }
        }
    }

    internal suspend fun validateAndPruneOfflineQueue() {
        try {
            val pendingSyncDao = db.pendingSyncDao()
            val queueSize = pendingSyncDao.getQueueSize()
            if (queueSize > 1000) {
                pendingSyncDao.pruneStaleEntries(
                    System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                )
                logCrashlytics("Pruned stale sync queue: was $queueSize")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gagal memvalidasi antrean sinkronisasi offline: ${e.message}")
        }
    }

    private fun logCrashlytics(message: String) {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseCrashlytics.getInstance().log(message)
            }
        } catch (_: IllegalStateException) {
            // Abaikan saat FirebaseApp belum diinisialisasi pada unit test
        }
    }

    private fun unwrapFirestoreException(t: Throwable): FirebaseFirestoreException? {
        return generateSequence(t) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private fun isNonRetryableFirestoreError(e: Throwable): Boolean {
        val code = unwrapFirestoreException(e)?.code ?: return false
        return code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            code == FirebaseFirestoreException.Code.INVALID_ARGUMENT ||
            code == FirebaseFirestoreException.Code.FAILED_PRECONDITION ||
            code == FirebaseFirestoreException.Code.OUT_OF_RANGE ||
            code == FirebaseFirestoreException.Code.UNIMPLEMENTED ||
            code == FirebaseFirestoreException.Code.DATA_LOSS ||
            code == FirebaseFirestoreException.Code.ALREADY_EXISTS ||
            code == FirebaseFirestoreException.Code.NOT_FOUND
    }

    internal suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000L,
        block: suspend () -> T
    ): T {
        var attempt = 0
        var currentDelay = initialDelay
        while (true) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isNonRetryableFirestoreError(e) || ++attempt >= maxAttempts) throw e
                delay(currentDelay)
                currentDelay *= 2
            }
        }
    }

    private fun continueStart(uid: String, cleanEmail: String) {
        if (activeUid != uid) return

        if (cleanEmail.isBlank() || !isValidEmailKey(cleanEmail)) {
            switchToDepot(
                depotId = "depot_$uid",
                role = DepotRole.OWNER,
                ownerEmail = cleanEmail
            )
            return
        }

        val firestore = firestoreProvider()
        val staffLinkPath = "staff_links/$cleanEmail"
        staffLinkListener = firestore.document(staffLinkPath)
            .addSnapshotListener { snapshot, error ->
                recordReads(1)
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, staffLinkPath)
                    roleResolveJob?.cancel()
                    roleResolveJob = scope.launch {
                        resolveAndApplyDepotRole(firestore, uid, cleanEmail, null)
                    }
                    return@addSnapshotListener
                }

                lastStaffLinkSnapshot = snapshot
                roleResolveJob?.cancel()
                roleResolveJob = scope.launch {
                    resolveAndApplyDepotRole(firestore, uid, cleanEmail, snapshot)
                }
            }
    }

    /**
     * Menentukan depot dan peran otoritatif bagi akun yang sedang login:
     * 1. Memeriksa dokumen `depots` yang mendaftarkan `cleanEmail` di dalam `memberEmails` serta menyaring
     *    depot bayangan (depot milik sesama karyawan yang email pemiliknya juga terdaftar sebagai anggota).
     * 2. Memeriksa dokumen `staff_links/{cleanEmail}`.
     * 3. Jika belum terdaftar sebagai karyawan di depot mana pun dan belum memiliki depot pemilik sah,
     *    menampilkan gerbang `AWAITING_ROLE_CONFIRMATION` agar karyawan tidak otomatis menjadi pemilik bayangan.
     */
    private suspend fun resolveAndApplyDepotRole(
        firestore: FirebaseFirestore,
        uid: String,
        cleanEmail: String,
        snapshot: DocumentSnapshot?
    ): Boolean {
        if (activeUid != uid) return false

        if (KaryawanScopeHelper.isPrimaryOwnerEmail(cleanEmail)) {
            saveResolvedRoleCache(uid, "depot_$uid", DepotRole.OWNER, cleanEmail)
            switchToDepot(
                depotId = "depot_$uid",
                role = DepotRole.OWNER,
                ownerEmail = cleanEmail
            )
            return true
        }

        val isDesignatedStaff = KaryawanScopeHelper.isDesignatedStaffEmail(cleanEmail)
        if (isDesignatedStaff) {
            prefs.edit().remove(keyConfirmedOwner(uid)).apply()
            cleanupShadowStaffLinks(firestore, uid, cleanEmail)
        }

        val targetDepotId = snapshot?.getString("depotId").orEmpty()
        val linkOwnerUid = snapshot?.getString("ownerUid").orEmpty()
        val linkOwnerEmail = snapshot?.getString("ownerEmail").orEmpty()
        val isActive = snapshot?.getBoolean("active") == true
        val hasExternalStaffLink = snapshot != null &&
            snapshot.exists() &&
            targetDepotId.isNotBlank() &&
            linkOwnerUid != uid &&
            !KaryawanScopeHelper.isDesignatedStaffEmail(linkOwnerEmail)

        // Hindari query depots berulang apabila sudah tersambung sebagai staf di depot yang sama & masih aktif.
        if (hasExternalStaffLink &&
            isActive &&
            activeRole == DepotRole.STAFF &&
            activeDepotId == targetDepotId &&
            collectionListeners.isNotEmpty()
        ) {
            saveResolvedRoleCache(uid, targetDepotId, DepotRole.STAFF, linkOwnerEmail)
            return true
        }

        val isOnlineNow = checkIsOnline()
        val memberDepots = if (cleanEmail.isNotBlank() && isOnlineNow) {
            try {
                val snap = withTimeout(6_000L) {
                    firestore.collection("depots")
                        .whereArrayContains("memberEmails", cleanEmail)
                        .get()
                        .await()
                }
                recordReads(snap.documents.size.coerceAtLeast(1))
                snap.documents.filter { doc ->
                    val dOwnerUid = doc.getString("ownerUid").orEmpty()
                    dOwnerUid.isNotBlank() && dOwnerUid != uid
                }
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val allStaffEmailsAcrossDepots = memberDepots
            .flatMap { doc ->
                (doc.get("memberEmails") as? List<*>)
                    ?.mapNotNull { (it as? String)?.trim()?.lowercase() }
                    .orEmpty()
            }
            .toSet()

        val validMemberDepots = memberDepots.filter { doc ->
            val docOwnerEmail = doc.getString("ownerEmail")?.trim()?.lowercase().orEmpty()
            !KaryawanScopeHelper.isDesignatedStaffEmail(docOwnerEmail)
        }

        val nonShadowDepots = validMemberDepots.filter { doc ->
            val docOwnerEmail = doc.getString("ownerEmail")?.trim()?.lowercase().orEmpty()
            docOwnerEmail.isBlank() || docOwnerEmail !in allStaffEmailsAcrossDepots
        }.ifEmpty { validMemberDepots }

        val authoritativeDepotDoc = nonShadowDepots.firstOrNull { doc ->
            KaryawanScopeHelper.isPrimaryOwnerEmail(doc.getString("ownerEmail"))
        } ?: nonShadowDepots.maxByOrNull { doc ->
            doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
        }

        if (activeUid != uid) return false

        if (authoritativeDepotDoc != null) {
            val authDepotId = authoritativeDepotDoc.getString("depotId").orEmpty()
                .ifBlank { authoritativeDepotDoc.id }
            val authOwnerEmail = authoritativeDepotDoc.getString("ownerEmail").orEmpty()

            if (hasExternalStaffLink && targetDepotId == authDepotId && !isActive) {
                transitionToRevoked(uid, authDepotId, authOwnerEmail)
                return true
            }

            prefs.edit().remove(keyConfirmedOwner(uid)).apply()
            cleanupShadowStaffLinks(firestore, uid, cleanEmail)
            saveResolvedRoleCache(uid, authDepotId, DepotRole.STAFF, authOwnerEmail)
            switchToDepot(
                depotId = authDepotId,
                role = DepotRole.STAFF,
                ownerEmail = authOwnerEmail
            )
            return true
        }

        if (hasExternalStaffLink) {
            if (isActive) {
                prefs.edit().remove(keyConfirmedOwner(uid)).apply()
                cleanupShadowStaffLinks(firestore, uid, cleanEmail)
                saveResolvedRoleCache(uid, targetDepotId, DepotRole.STAFF, linkOwnerEmail)
                switchToDepot(
                    depotId = targetDepotId,
                    role = DepotRole.STAFF,
                    ownerEmail = linkOwnerEmail
                )
            } else {
                transitionToRevoked(uid, targetDepotId, linkOwnerEmail)
            }
            return true
        }

        if (!isDesignatedStaff) {
            val isConfirmedOwner = prefs.getBoolean(keyConfirmedOwner(uid), false)
            if (isConfirmedOwner) {
                saveResolvedRoleCache(uid, "depot_$uid", DepotRole.OWNER, cleanEmail)
                switchToDepot(
                    depotId = "depot_$uid",
                    role = DepotRole.OWNER,
                    ownerEmail = cleanEmail
                )
                return true
            }

            val ownDepotExists = if (isOnlineNow) {
                try {
                    val ownDoc = withTimeout(5_000L) {
                        firestore.collection("depots").document("depot_$uid").get().await()
                    }
                    recordReads(1)
                    ownDoc.exists()
                } catch (_: Exception) {
                    prefs.getString(KEY_LAST_DEPOT_ID, null) == "depot_$uid"
                }
            } else {
                prefs.getString(KEY_LAST_DEPOT_ID, null) == "depot_$uid"
            }

            if (ownDepotExists) {
                saveResolvedRoleCache(uid, "depot_$uid", DepotRole.OWNER, cleanEmail)
                switchToDepot(
                    depotId = "depot_$uid",
                    role = DepotRole.OWNER,
                    ownerEmail = cleanEmail
                )
                return true
            }
        }

        // Fallback offline untuk mode Karyawan agar tidak terkunci AWAITING_ROLE_CONFIRMATION
        // ketika perangkat dibuka offline sebelum cache snapshot staff_links tersedia.
        if (!isOnlineNow) {
            val cachedDepot = prefs.getString(keyLastDepot(uid), null).orEmpty()
            val cachedRoleName = prefs.getString(keyLastRole(uid), null)
            val cachedOwnerEmail = prefs.getString(keyLastOwnerEmail(uid), null).orEmpty()
            if (cachedDepot.isNotBlank() &&
                cachedRoleName == DepotRole.STAFF.name &&
                cachedDepot != "depot_$uid"
            ) {
                switchToDepot(
                    depotId = cachedDepot,
                    role = DepotRole.STAFF,
                    ownerEmail = cachedOwnerEmail
                )
                return true
            }
        }

        detachCollectionListeners()
        activeDepotId = ""
        activeRole = DepotRole.STAFF
        _state.update {
            it.copy(
                status = SyncConnectionStatus.AWAITING_ROLE_CONFIRMATION,
                role = DepotRole.STAFF,
                depotId = "",
                ownerEmail = "",
                errorMessage = null
            )
        }
        return false
    }

    private fun saveResolvedRoleCache(
        uid: String,
        depotId: String,
        role: DepotRole,
        ownerEmail: String
    ) {
        val editor = prefs.edit()
            .putString(keyLastDepot(uid), depotId)
            .putString(keyLastRole(uid), role.name)
            .putString(keyLastOwnerEmail(uid), ownerEmail)
        if (role == DepotRole.OWNER) {
            editor.putBoolean(keyConfirmedOwner(uid), true)
        } else {
            editor.remove(keyConfirmedOwner(uid))
        }
        editor.apply()
        offlineSessionManager.saveLastSession(
            uid = uid,
            email = activeEmail,
            displayName = activeDisplayName,
            isOfflineLocal = false,
            role = role
        )
    }

    private fun clearResolvedRoleCache(uid: String) {
        prefs.edit()
            .remove(keyConfirmedOwner(uid))
            .remove(keyLastDepot(uid))
            .remove(keyLastRole(uid))
            .remove(keyLastOwnerEmail(uid))
            .apply()
    }

    private fun transitionToRevoked(uid: String, targetDepotId: String, ownerEmail: String) {
        val wasRevoked = _state.value.status == SyncConnectionStatus.REVOKED
        clearResolvedRoleCache(uid)
        detachCollectionListeners()
        activeDepotId = ""
        activeRole = DepotRole.STAFF
        _state.update {
            it.copy(
                status = SyncConnectionStatus.REVOKED,
                role = DepotRole.STAFF,
                depotId = targetDepotId,
                ownerEmail = ownerEmail,
                errorMessage = "Akses akun karyawan dinonaktifkan oleh pemilik"
            )
        }
        if (!wasRevoked) {
            // Data depot tidak boleh tetap bisa dibaca setelah akses dicabut.
            scope.launch { clearLocalDataInternal() }
        }
    }

    private suspend fun cleanupShadowStaffLinks(
        firestore: FirebaseFirestore,
        uid: String,
        userEmail: String = activeEmail
    ) {
        if (!checkIsOnline()) return
        try {
            val snap = withTimeout(5_000L) {
                firestore.collection("staff_links")
                    .whereEqualTo("ownerUid", uid)
                    .get()
                    .await()
            }
            recordReads(snap.documents.size.coerceAtLeast(1))
            for (doc in snap.documents) {
                try {
                    withRetry { doc.reference.delete().await() }
                    recordWrites(1)
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }

        if (KaryawanScopeHelper.isDesignatedStaffEmail(userEmail)) {
            try {
                withTimeout(15_000L) {
                    withRetry {
                        firestore.collection("depots").document("depot_$uid").delete().await()
                    }
                    recordWrites(1)
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Dipanggil saat pengguna mengonfirmasi secara eksplisit bahwa akun ini adalah Pemilik Depot baru.
     */
    fun confirmAsNewOwner() {
        val uid = activeUid ?: return
        if (KaryawanScopeHelper.isDesignatedStaffEmail(activeEmail)) {
            scope.launch { recheckStaffConnection() }
            return
        }
        prefs.edit().putBoolean(keyConfirmedOwner(uid), true).apply()
        switchToDepot(
            depotId = "depot_$uid",
            role = DepotRole.OWNER,
            ownerEmail = activeEmail
        )
    }

    /**
     * Memeriksa ulang apakah akun karyawan sudah didaftarkan oleh Pemilik Depot di Cloud.
     */
    suspend fun recheckStaffConnection(): Boolean {
        val uid = activeUid ?: return false
        val cleanEmail = activeEmail
        val firestore = firestoreProvider()
        val freshLinkSnap = if (cleanEmail.isNotBlank() && isValidEmailKey(cleanEmail) && checkIsOnline()) {
            try {
                val doc = withTimeout(5_000L) {
                    firestore.collection("staff_links").document(cleanEmail).get().await()
                }
                recordReads(1)
                lastStaffLinkSnapshot = doc
                doc
            } catch (_: Exception) {
                lastStaffLinkSnapshot
            }
        } else {
            lastStaffLinkSnapshot
        }
        return resolveAndApplyDepotRole(firestore, uid, cleanEmail, freshLinkSnap)
    }

    /**
     * Mengubah akun yang sebelumnya terlanjur masuk sebagai Pemilik bayangan menjadi mode Karyawan,
     * menghapus klaim `staff_links` bayangan di Cloud, membersihkan data lokal, dan mencari depot Pemilik asli.
     */
    suspend fun switchToStaffWaitingMode(): Boolean {
        val uid = activeUid ?: return false
        clearResolvedRoleCache(uid)
        prefs.edit().remove(KEY_LAST_DEPOT_ID).apply()
        detachCollectionListeners()
        activeDepotId = ""
        activeRole = DepotRole.STAFF
        val firestore = firestoreProvider()
        cleanupShadowStaffLinks(firestore, uid)
        clearLocalDataInternal()
        val linked = recheckStaffConnection()
        if (!linked) {
            _state.update {
                it.copy(
                    status = SyncConnectionStatus.AWAITING_ROLE_CONFIRMATION,
                    role = DepotRole.STAFF,
                    depotId = "",
                    ownerEmail = "",
                    errorMessage = null
                )
            }
        }
        return linked
    }

    /**
     * Memaksa pembaruan dokumen depot (`memberEmails`) dan klaim ulang `staff_links`
     * segera setelah Pemilik menambah, mengubah, atau menghapus data kurir.
     */
    suspend fun syncStaffLinksNow() {
        val uid = activeUid ?: return
        if (activeRole != DepotRole.OWNER) return
        val depotId = activeDepotId.ifBlank { "depot_$uid" }
        if (!checkIsOnline()) {
            scheduleFlush(delayMs = 150L)
            return
        }
        val firestore = firestoreProvider()
        try {
            withTimeout(10_000L) {
                ensureDepotAndStaffLinks(firestore, depotId, uid, activeEmail)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Immediate staff_links sync deferred: ${e.message}")
        }
        scheduleFlush(delayMs = 100L)
    }

    fun stop() {
        attachGeneration.incrementAndGet()
        staffLinkListener?.remove()
        staffLinkListener = null
        userProfileListener?.remove()
        userProfileListener = null
        depotDocListener?.remove()
        depotDocListener = null
        lastStaffLinkSnapshot = null
        roleResolveJob?.cancel()
        roleResolveJob = null
        switchDepotJob?.cancel()
        switchDepotJob = null
        userTouchJob?.cancel()
        userTouchJob = null
        profileSyncJob?.cancel()
        profileSyncJob = null
        detachCollectionListeners()
        pendingObserverJob?.cancel()
        pendingObserverJob = null
        flushJob?.cancel()
        flushJob = null
        autoRetryJob?.cancel()
        autoRetryJob = null
        autoRetryCount = 0
        permissionDeniedAttempts.clear()
        unregisterNetworkCallback()
        activeUid = null
        activeDisplayName = ""
        activeDepotId = ""
        hasSeededInitialOwnerPush = false
        lastDepotMirroredTier = null
        cachedSnapshots.clear()
        receivedInitialCollections.clear()
        profileStore.clearActiveSession()
        _state.value = RealtimeSyncState(
            sessionReads = _state.value.sessionReads,
            sessionWrites = _state.value.sessionWrites
        )
    }

    /**
     * Menghapus seluruh data lokal. Dipakai saat logout dan saat pemilik akun berganti,
     * supaya data depot lama tidak pernah tercampur dengan depot baru.
     */
    suspend fun clearLocalData() {
        clearLocalDataInternal()
        offlineSessionManager.clearAll()
        profileStore.clearProfile(OfflineSessionManager.OFFLINE_LOCAL_UID)
        prefs.edit()
            .remove(KEY_LAST_OWNER_UID)
            .remove(KEY_LAST_DEPOT_ID)
            .apply()
        profileStore.clearActiveSession()
        _state.value = RealtimeSyncState(
            sessionReads = _state.value.sessionReads,
            sessionWrites = _state.value.sessionWrites
        )
    }

    private suspend fun clearLocalDataInternal() {
        db.withTransaction {
            db.tangkiRefillDao().deleteAll()
            db.perawatanLogDao().deleteAll()
            db.perawatanDao().deleteAll()
            db.pengeluaranDao().deleteAll()
            db.transaksiDao().deleteAll()
            db.kasbonLogDao().deleteAll()
            db.pelangganDao().deleteAll()
            db.kurirDao().deleteAll()
            db.produkDao().deleteAll()
            db.pendingSyncDao().deleteAll()
        }
        cachedSnapshots.clear()
        receivedInitialCollections.clear()
        clearAllDeltaCursors()
    }

    fun scheduleFlush(delayMs: Long = 250L) {
        if (activeUid == null || activeDepotId.isBlank() || isOfflineLocalSession()) return
        if (isTerminalBlockingStatus(_state.value.status)) return
        flushJob?.cancel()
        flushJob = scope.launch {
            delay(delayMs)
            flushPendingQueueInternal()
        }
    }

    /**
     * Memicu pengiriman antrean tertunda secara instan (misal saat pengguna menekan tombol Coba Sekarang).
     */
    fun retryPendingQueueNow() {
        autoRetryCount = 0
        autoRetryJob?.cancel()
        _state.update { it.copy(autoRetryAttempt = 0, isAutoRetryScheduled = false, errorMessage = null) }
        scheduleFlush(delayMs = 50L)
    }

    private fun expectedCollectionsForRole(role: DepotRole): List<String> = buildList {
        add(SyncCollections.PRODUK)
        add(SyncCollections.KURIR)
        add(SyncCollections.PELANGGAN)
        add(SyncCollections.TRANSAKSI)
        add(SyncCollections.KASBON_LOGS)
        if (role == DepotRole.OWNER) {
            add(SyncCollections.PENGELUARAN)
            add(SyncCollections.PERAWATAN_JADWAL)
            add(SyncCollections.PERAWATAN_LOG)
            add(SyncCollections.TANGKI_REFILL)
        }
    }

    private fun collectionDisplayLabel(colName: String): String {
        return when (colName) {
            SyncCollections.PELANGGAN -> context.getString(R.string.sync_col_pelanggan)
            SyncCollections.TRANSAKSI -> context.getString(R.string.sync_col_transaksi)
            SyncCollections.KASBON_LOGS -> context.getString(R.string.sync_col_kasbon_logs)
            SyncCollections.PRODUK -> context.getString(R.string.sync_col_produk)
            SyncCollections.KURIR -> context.getString(R.string.sync_col_kurir)
            SyncCollections.PENGELUARAN -> context.getString(R.string.sync_col_pengeluaran)
            SyncCollections.PERAWATAN_JADWAL -> context.getString(R.string.sync_col_perawatan_jadwal)
            SyncCollections.PERAWATAN_LOG -> context.getString(R.string.sync_col_perawatan_log)
            SyncCollections.TANGKI_REFILL -> context.getString(R.string.sync_col_tangki_refill)
            SyncCollections.PROFIL -> context.getString(R.string.sync_col_profil)
            else -> colName
        }
    }

    private fun updateListenerHealthState() {
        val expected = if (activeDepotId.isBlank() || isOfflineLocalSession()) {
            0
        } else {
            expectedCollectionsForRole(activeRole).size
        }
        val active = collectionListeners.size
        _state.update {
            it.copy(
                activeListenerCount = active,
                expectedListenerCount = expected
            )
        }
    }

    private fun scheduleAutoRetry() {
        if (activeUid == null || activeDepotId.isBlank() || isOfflineLocalSession()) return
        if (isTerminalBlockingStatus(_state.value.status)) return
        val maxAttempts = _state.value.maxAutoRetryAttempts
        if (autoRetryCount >= maxAttempts || autoRetryJob?.isActive == true) return
        autoRetryCount++
        // Exponential backoff (1.5s, 3s, 6s, 12s, 24s) + deterministik jitter ringan
        val backoffMultiplier = 1L shl (autoRetryCount - 1).coerceIn(0, 4)
        val jitterMs = (autoRetryCount * 250L)
        val delayMs = (1_500L * backoffMultiplier + jitterMs).coerceAtMost(25_000L)
        _state.update {
            it.copy(
                autoRetryAttempt = autoRetryCount,
                isAutoRetryScheduled = true
            )
        }
        autoRetryJob = scope.launch {
            try {
                delay(delayMs)
                val uid = activeUid ?: return@launch
                val depotId = activeDepotId.ifBlank { return@launch }
                if (!checkIsOnline()) return@launch
                val firestore = firestoreProvider()
                if (activeRole == DepotRole.OWNER) {
                    try {
                        withTimeout(10_000L) {
                            ensureDepotAndStaffLinks(firestore, depotId, uid, activeEmail)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Auto-retry depot init failed: ${e.message}")
                    }
                }
                val expected = expectedCollectionsForRole(activeRole)
                val needsAttachAll = collectionListeners.isEmpty()
                if (needsAttachAll) {
                    attachDepotListeners(firestore, depotId, activeRole)
                } else if (failedCollections.isNotEmpty()) {
                    val toRetry = failedCollections.filter { it in expected }
                    if (toRetry.isNotEmpty()) {
                        failedCollections.removeAll(toRetry.toSet())
                        attachMissingCollectionListeners(firestore, depotId, activeRole, toRetry)
                    }
                }
                flushPendingQueueInternal()
            } finally {
                _state.update { it.copy(isAutoRetryScheduled = false) }
            }
        }
    }

    suspend fun forceSyncNow(
        pushLocalFirst: Boolean = true,
        forceReenqueueAll: Boolean = false
    ): Result<SyncSummary> {
        val uid = activeUid ?: return Result.failure(IllegalStateException("Belum masuk akun Google"))
        val depotId = activeDepotId.ifBlank { "depot_$uid" }
        val firestore = firestoreProvider()
        autoRetryCount = 0
        autoRetryJob?.cancel()
        return try {
            _state.update {
                it.copy(
                    status = SyncConnectionStatus.SYNCING,
                    errorMessage = null,
                    autoRetryAttempt = 0,
                    isAutoRetryScheduled = false,
                    syncStageLabel = context.getString(R.string.sync_stage_verifying_access),
                    syncProgressFraction = 0.08f
                )
            }
            if (activeRole == DepotRole.OWNER) {
                ensureDepotAndStaffLinks(firestore, depotId, uid, activeEmail)
            }
            if (forceReenqueueAll && pushLocalFirst) {
                _state.update {
                    it.copy(
                        syncStageLabel = context.getString(R.string.sync_stage_reenqueue_local),
                        syncProgressFraction = 0.16f
                    )
                }
                enqueueAllRoleScopedLocalRecords(uid, activeRole)
            }
            // Mode "Ambil Data" tidak boleh mengirim antrean lokal sebelum cloud
            // selesai diterapkan. Pada mode pushLocalFirst, hanya antrean tertunda yang dikirim
            // terlebih dahulu (atau seluruh record lokal bila forceReenqueueAll = true).
            val initialPendingToPush = db.pendingSyncDao().getQueueSize()
            if (pushLocalFirst) {
                flushPendingQueueInternal()
            }

            val depotRef = firestore.collection("depots").document(depotId)
            val cols = expectedCollectionsForRole(activeRole)

            withTimeout(30_000L) {
                val fullSnapshots = mutableMapOf<String, List<DocumentSnapshot>>()
                for ((idx, col) in cols.withIndex()) {
                    val stepFraction = 0.25f + (0.55f * ((idx + 1).toFloat() / cols.size.coerceAtLeast(1)))
                    _state.update {
                        it.copy(
                            syncStageLabel = context.getString(
                                R.string.sync_stage_pulling_collection,
                                collectionDisplayLabel(col),
                                idx + 1,
                                cols.size
                            ),
                            syncProgressFraction = stepFraction
                        )
                    }
                    val snap = depotRef.collection(col).get().await()
                    recordReads(snap.documents.size)
                    fullSnapshots[col] = snap.documents
                }
                _state.update {
                    it.copy(
                        syncStageLabel = context.getString(R.string.sync_stage_reconciling),
                        syncProgressFraction = 0.88f
                    )
                }
                applySnapshotsInOrder(
                    snapshotsByCollection = fullSnapshots,
                    seedDefaultPerawatanIfEmpty = activeRole == DepotRole.OWNER,
                    reconcileHardDeletes = !pushLocalFirst
                )
                if (activeRole == DepotRole.OWNER && pushLocalFirst) {
                    enqueueMissingLocalRecordsForOwner(fullSnapshots, null)
                    flushPendingQueueInternal()
                }
                for (col in cols) {
                    advanceDeltaCursor(
                        depotId = depotId,
                        colName = col,
                        docs = fullSnapshots[col].orEmpty(),
                        isFromCache = false,
                        markInitializedIfEmpty = true
                    )
                }
            }

            if (failedCollections.isNotEmpty() || collectionListeners.size < cols.size) {
                val missing = cols.filter { !collectionListeners.containsKey(it) }
                if (missing.isNotEmpty()) {
                    failedCollections.removeAll(missing.toSet())
                    attachMissingCollectionListeners(firestore, depotId, activeRole, missing)
                }
            }
            updateListenerHealthState()

            val pCount = db.pelangganDao().count()
            val prCount = db.produkDao().count()
            val kCount = db.kurirDao().count()
            val txCount = db.transaksiDao().getAllOnce().size
            val expCount = db.pengeluaranDao().getAllOnce().size
            val kasbonLogCount = db.kasbonLogDao().getAllOnce().size
            val perawatanCount = db.perawatanDao().count() + db.perawatanLogDao().getAllOnce().size
            val tangkiCount = db.tangkiRefillDao().getAllOnce().size

            val remainingPending = db.pendingSyncDao().getQueueSize()
            _state.update {
                it.copy(
                    status = when {
                        !it.isOnline -> SyncConnectionStatus.OFFLINE
                        remainingPending > 0 -> SyncConnectionStatus.SYNCING
                        else -> SyncConnectionStatus.LIVE
                    },
                    pendingCount = remainingPending,
                    lastSyncedAt = System.currentTimeMillis(),
                    errorMessage = null,
                    syncStageLabel = null,
                    syncProgressFraction = null,
                    activeListenerCount = collectionListeners.size,
                    expectedListenerCount = cols.size
                )
            }
            Result.success(
                SyncSummary(
                    pelangganCount = pCount,
                    produkCount = prCount,
                    kurirCount = kCount,
                    transaksiCount = txCount,
                    pengeluaranCount = expCount,
                    kasbonLogCount = kasbonLogCount,
                    perawatanCount = perawatanCount,
                    tangkiRefillCount = tangkiCount,
                    pushedCount = initialPendingToPush,
                    message = "Data online berhasil diperbarui"
                )
            )
        } catch (e: CancellationException) {
            _state.update { it.copy(syncStageLabel = null, syncProgressFraction = null) }
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "depots/$depotId")
            val isOnline = checkIsOnline()
            _state.update {
                it.copy(
                    status = if (!isOnline) SyncConnectionStatus.OFFLINE else SyncConnectionStatus.ERROR,
                    isOnline = isOnline,
                    errorMessage = friendlyError(e),
                    syncStageLabel = null,
                    syncProgressFraction = null
                )
            }
            if (isOnline) {
                scheduleAutoRetry()
            }
            Result.failure(Exception(friendlyError(e), e))
        }
    }

    private fun switchToDepot(
        depotId: String,
        role: DepotRole,
        ownerEmail: String
    ) {
        if (activeDepotId == depotId &&
            activeRole == role &&
            collectionListeners.isNotEmpty() &&
            failedCollections.isEmpty()
        ) {
            return
        }
        switchDepotJob?.cancel()
        profileSyncJob?.cancel()
        depotDocListener?.remove()
        depotDocListener = null
        detachCollectionListeners()
        cachedSnapshots.clear()
        receivedInitialCollections.clear()
        hasSeededInitialOwnerPush = false

        val previousDepot = prefs.getString(KEY_LAST_DEPOT_ID, null)
        val previousRole = activeRole
        activeDepotId = depotId
        activeRole = role
        activeUid?.let { uid ->
            val roleStr = if (role == DepotRole.STAFF) DepotProfile.ROLE_STAFF else DepotProfile.ROLE_OWNER
            profileStore.updateRole(uid, roleStr)
        }

        _state.update {
            it.copy(
                status = if (it.isOnline) SyncConnectionStatus.SYNCING else SyncConnectionStatus.OFFLINE,
                role = role,
                depotId = depotId,
                ownerEmail = ownerEmail,
                errorMessage = null
            )
        }

        switchDepotJob = scope.launch {
            val sessionUid = activeUid ?: return@launch
            val isMergingFromOffline = offlineSessionManager.hasPendingOfflineMerge() &&
                sessionUid != OfflineSessionManager.OFFLINE_LOCAL_UID
            // Setiap perpindahan depot wajib mengirim antrean yang masih valid milik depot lama
            // terlebih dahulu (best-effort), lalu menghapus data depot lama agar tidak tercampur,
            // KECUALI saat sedang menggabungkan data Mode Offline ke akun Google baru.
            if (isMergingFromOffline) {
                if (role == DepotRole.OWNER) {
                    val hadLocalData = hasAnyLocalCoreData() || db.pendingSyncDao().getQueueSize() > 0
                    clearDeltaCursorsForDepot(depotId)
                    previousDepot?.let { clearDeltaCursorsForDepot(it) }
                    enqueueAllLocalRecordsForOfflineMerge()
                    offlineSessionManager.setHasPendingOfflineMerge(false)
                    profileStore.clearProfile(OfflineSessionManager.OFFLINE_LOCAL_UID)
                    if (hadLocalData) {
                        _offlineMergeMessage.tryEmit(
                            context.getString(R.string.offline_merge_success)
                        )
                    }
                } else {
                    // Karyawan tidak diizinkan oleh Security Rules menulis data milik pemilik
                    sanitizeOwnerOnlyDataForStaffMerge()
                    offlineSessionManager.setHasPendingOfflineMerge(false)
                    profileStore.clearProfile(OfflineSessionManager.OFFLINE_LOCAL_UID)
                    _offlineMergeMessage.tryEmit(
                        context.getString(R.string.offline_merge_staff_notice)
                    )
                }
            } else if (previousDepot != depotId) {
                val lastOwnerUid = prefs.getString(KEY_LAST_OWNER_UID, null)
                if (previousDepot != null &&
                    previousDepot != OfflineSessionManager.OFFLINE_LOCAL_DEPOT_ID &&
                    lastOwnerUid == sessionUid &&
                    !KaryawanScopeHelper.isDesignatedStaffEmail(activeEmail) &&
                    checkIsOnline()
                ) {
                    try {
                        withTimeout(5_000L) {
                            flushPendingQueueInternal(
                                targetDepotId = previousDepot,
                                targetRole = previousRole
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Best-effort flush before switching depot skipped: ${e.message}")
                    }
                }
                if (activeUid != sessionUid || activeDepotId != depotId) return@launch
                if (previousDepot != null || role == DepotRole.STAFF) {
                    db.withTransaction {
                        db.tangkiRefillDao().deleteAll()
                        db.perawatanLogDao().deleteAll()
                        db.perawatanDao().deleteAll()
                        db.pengeluaranDao().deleteAll()
                        db.transaksiDao().deleteAll()
                        db.kasbonLogDao().deleteAll()
                        db.pelangganDao().deleteAll()
                        db.kurirDao().deleteAll()
                        db.produkDao().deleteAll()
                        db.pendingSyncDao().deleteAll()
                    }
                    cachedSnapshots.clear()
                    receivedInitialCollections.clear()
                    clearDeltaCursorsForDepot(depotId)
                    previousDepot?.let { clearDeltaCursorsForDepot(it) }
                }
            } else if (role == DepotRole.STAFF) {
                // Karyawan tidak boleh memiliki data pengeluaran maupun jadwal/riwayat perawatan/tangki pemilik di cache lokal.
                db.withTransaction {
                    db.tangkiRefillDao().deleteAll()
                    db.pengeluaranDao().deleteAll()
                    db.perawatanLogDao().deleteAll()
                    db.perawatanDao().deleteAll()
                }
            }
            if (!hasAnyLocalCoreData()) {
                clearDeltaCursorsForDepot(depotId)
            }
            if (activeUid != sessionUid || activeDepotId != depotId) return@launch
            prefs.edit()
                .putString(KEY_LAST_DEPOT_ID, depotId)
                .putString(KEY_LAST_OWNER_UID, sessionUid)
                .apply()

            val firestore = firestoreProvider()
            val uid = sessionUid

            syncResolvedUserRoleInBackground(firestore, uid, role)

            if (role == DepotRole.OWNER) {
                try {
                    ensureDesignatedStaffKurirsForOwner(activeEmail)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Local designated staff seed skipped: ${e.message}")
                }
                if (activeUid != sessionUid || activeDepotId != depotId) return@launch
                try {
                    withTimeout(10_000L) {
                        ensureDepotAndStaffLinks(firestore, depotId, uid, activeEmail)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Deferred depot metadata sync: ${e.message}")
                }
            }

            if (activeUid != sessionUid || activeDepotId != depotId) return@launch
            attachDepotDocListener(firestore, depotId, uid, role)
            attachDepotListeners(firestore, depotId, role)
            if (role == DepotRole.OWNER && profileStore.hasPendingSync(uid) && checkIsOnline()) {
                try {
                    pushProfileToCloudInternal(firestore, uid, depotId, profileStore.currentProfile())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Pending profile flush deferred: ${e.message}")
                }
            }
            flushPendingQueueInternal()
        }
    }

    private fun attachDepotListeners(
        firestore: FirebaseFirestore,
        depotId: String,
        role: DepotRole
    ) {
        val collections = expectedCollectionsForRole(role)
        val generation = attachGeneration.incrementAndGet()
        val depotRef = firestore.collection("depots").document(depotId)
        val bootstrapCollections = mutableSetOf<String>()
        var initialBatchDispatched = false
        val pendingCursorAdvance = mutableMapOf<String, List<DocumentSnapshot>>()

        collections.forEach { colName ->
            val path = "depots/$depotId/$colName"
            val cursor = getDeltaCursor(depotId, colName)
            if (cursor == null) {
                bootstrapCollections.add(colName)
            }
            val query = if (cursor != null) {
                depotRef.collection(colName).whereGreaterThan("serverUpdatedAt", cursor)
            } else {
                depotRef.collection(colName)
            }
            var isInitialSnapshotForCol = true
            val registration = query.addSnapshotListener { snapshot, error ->
                if (attachGeneration.get() != generation || activeDepotId != depotId) return@addSnapshotListener

                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    collectionListeners.remove(colName)?.remove()
                    failedCollections.add(colName)
                    val isOnline = checkIsOnline()
                    _state.update {
                        it.copy(
                            status = if (!isOnline) SyncConnectionStatus.OFFLINE else SyncConnectionStatus.ERROR,
                            isOnline = isOnline,
                            errorMessage = friendlyError(error)
                        )
                    }
                    scope.launch {
                        if (attachGeneration.get() != generation || activeDepotId != depotId) return@launch
                        var batchToApply: Map<String, List<DocumentSnapshot>>? = null
                        var shouldFlushBatch = false
                        applyMutex.withLock {
                            if (attachGeneration.get() != generation || activeDepotId != depotId) return@withLock
                            if (isInitialSnapshotForCol) {
                                isInitialSnapshotForCol = false
                                receivedInitialCollections.add(colName)
                            }
                            if (!initialBatchDispatched && receivedInitialCollections.containsAll(collections)) {
                                initialBatchDispatched = true
                                shouldFlushBatch = true
                                batchToApply = cachedSnapshots.toMap()
                                cachedSnapshots.clear()
                            }
                        }
                        val batch = batchToApply
                        if (shouldFlushBatch && !batch.isNullOrEmpty()) {
                            applySnapshotsInOrder(
                                snapshotsByCollection = batch,
                                seedDefaultPerawatanIfEmpty = role == DepotRole.OWNER &&
                                    SyncCollections.PERAWATAN_JADWAL in bootstrapCollections
                            )
                        }
                    }
                    if (isOnline) {
                        scheduleAutoRetry()
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val isFromCache = snapshot.metadata.isFromCache
                    val isInitialForCollection = isInitialSnapshotForCol
                    if (isInitialForCollection) {
                        isInitialSnapshotForCol = false
                    }
                    val readsCount = when {
                        isFromCache -> 0
                        isInitialForCollection -> snapshot.documents.size
                        else -> snapshot.documentChanges.size
                    }
                    recordReads(readsCount)

                    scope.launch {
                        if (attachGeneration.get() != generation || activeDepotId != depotId) return@launch
                        autoRetryCount = 0
                        failedCollections.remove(colName)
                        var batchToApply: Map<String, List<DocumentSnapshot>>? = null
                        var allInitialArrived = false
                        var bufferedBeforeBatch = false

                        applyMutex.withLock {
                            if (attachGeneration.get() != generation || activeDepotId != depotId) return@withLock
                            if (!initialBatchDispatched) {
                                // Pastikan tidak ada applyChangedDocuments() sebelum seluruh batch awal
                                // selesai diterapkan sesuai urutan ketergantungan (Produk -> Kurir -> Pelanggan -> Transaksi).
                                cachedSnapshots[colName] = snapshot.documents
                                if (!isFromCache) {
                                    pendingCursorAdvance[colName] = snapshot.documents
                                }
                                receivedInitialCollections.add(colName)
                                if (receivedInitialCollections.containsAll(collections)) {
                                    initialBatchDispatched = true
                                    allInitialArrived = true
                                    batchToApply = cachedSnapshots.toMap()
                                    cachedSnapshots.clear()
                                } else {
                                    bufferedBeforeBatch = true
                                }
                            }
                        }

                        if (bufferedBeforeBatch) {
                            return@launch
                        }

                        if (allInitialArrived) {
                            val resolvedBatch = batchToApply.orEmpty()
                            applySnapshotsInOrder(
                                snapshotsByCollection = resolvedBatch,
                                seedDefaultPerawatanIfEmpty = role == DepotRole.OWNER &&
                                    SyncCollections.PERAWATAN_JADWAL in bootstrapCollections
                            )
                            if (attachGeneration.get() != generation || activeDepotId != depotId) return@launch
                            pendingCursorAdvance.forEach { (readyCol, readyDocs) ->
                                advanceDeltaCursor(
                                    depotId = depotId,
                                    colName = readyCol,
                                    docs = readyDocs,
                                    isFromCache = false,
                                    markInitializedIfEmpty = (getDeltaCursor(depotId, readyCol) == null)
                                )
                            }
                            pendingCursorAdvance.clear()

                            if (role == DepotRole.OWNER && !hasSeededInitialOwnerPush) {
                                hasSeededInitialOwnerPush = true
                                if (bootstrapCollections.isNotEmpty()) {
                                    enqueueMissingLocalRecordsForOwner(resolvedBatch, bootstrapCollections)
                                }
                                flushPendingQueueInternal()
                            }
                        } else {
                            applyChangedDocuments(colName, snapshot.documentChanges)
                            if (!isFromCache) {
                                advanceDeltaCursor(
                                    depotId = depotId,
                                    colName = colName,
                                    docs = snapshot.documents,
                                    isFromCache = false,
                                    markInitializedIfEmpty = (cursor == null)
                                )
                            }
                        }

                        _state.update {
                            val onlineNow = checkIsOnline()
                            it.copy(
                                status = when {
                                    isTerminalBlockingStatus(it.status) -> it.status
                                    !onlineNow -> SyncConnectionStatus.OFFLINE
                                    it.pendingCount > 0 -> SyncConnectionStatus.SYNCING
                                    else -> SyncConnectionStatus.LIVE
                                },
                                isOnline = onlineNow,
                                lastSyncedAt = System.currentTimeMillis(),
                                errorMessage = null,
                                activeListenerCount = collectionListeners.size,
                                expectedListenerCount = collections.size
                            )
                        }
                    }
                }
            }
            collectionListeners[colName]?.remove()
            collectionListeners[colName] = registration
        }
        updateListenerHealthState()
    }

    private fun attachMissingCollectionListeners(
        firestore: FirebaseFirestore,
        depotId: String,
        role: DepotRole,
        targetCollections: List<String>
    ) {
        val generation = attachGeneration.get()
        val depotRef = firestore.collection("depots").document(depotId)
        val expectedColsCount = expectedCollectionsForRole(role).size
        targetCollections.forEach { colName ->
            if (collectionListeners.containsKey(colName)) return@forEach
            val path = "depots/$depotId/$colName"
            val cursor = getDeltaCursor(depotId, colName)
            val query = if (cursor != null) {
                depotRef.collection(colName).whereGreaterThan("serverUpdatedAt", cursor)
            } else {
                depotRef.collection(colName)
            }
            var isInitialForRecovery = true
            val registration = query.addSnapshotListener { snapshot, error ->
                if (attachGeneration.get() != generation || activeDepotId != depotId) return@addSnapshotListener
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    collectionListeners.remove(colName)?.remove()
                    failedCollections.add(colName)
                    updateListenerHealthState()
                    if (checkIsOnline()) scheduleAutoRetry()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val isFromCache = snapshot.metadata.isFromCache
                    val readsCount = when {
                        isFromCache -> 0
                        isInitialForRecovery -> snapshot.documents.size
                        else -> snapshot.documentChanges.size
                    }
                    recordReads(readsCount)
                    scope.launch {
                        if (attachGeneration.get() != generation || activeDepotId != depotId) return@launch
                        failedCollections.remove(colName)
                        if (isInitialForRecovery) {
                            isInitialForRecovery = false
                            applySnapshotsInOrder(
                                snapshotsByCollection = mapOf(colName to snapshot.documents),
                                seedDefaultPerawatanIfEmpty = false
                            )
                        } else {
                            applyChangedDocuments(colName, snapshot.documentChanges)
                        }
                        if (!isFromCache) {
                            advanceDeltaCursor(
                                depotId = depotId,
                                colName = colName,
                                docs = snapshot.documents,
                                isFromCache = false,
                                markInitializedIfEmpty = (cursor == null)
                            )
                        }
                        _state.update {
                            val onlineNow = checkIsOnline()
                            it.copy(
                                status = when {
                                    isTerminalBlockingStatus(it.status) -> it.status
                                    !onlineNow -> SyncConnectionStatus.OFFLINE
                                    it.pendingCount > 0 -> SyncConnectionStatus.SYNCING
                                    else -> SyncConnectionStatus.LIVE
                                },
                                isOnline = onlineNow,
                                lastSyncedAt = System.currentTimeMillis(),
                                errorMessage = null,
                                activeListenerCount = collectionListeners.size,
                                expectedListenerCount = expectedColsCount
                            )
                        }
                    }
                }
            }
            collectionListeners[colName]?.remove()
            collectionListeners[colName] = registration
        }
        updateListenerHealthState()
    }

    private fun detachCollectionListeners() {
        attachGeneration.incrementAndGet()
        collectionListeners.values.forEach { it.remove() }
        collectionListeners.clear()
        failedCollections.clear()
        updateListenerHealthState()
    }

    private fun observePendingQueue() {
        pendingObserverJob?.cancel()
        pendingObserverJob = scope.launch {
            kotlinx.coroutines.flow.combine(
                db.pendingSyncDao().observeAllPending(),
                profileStore.pendingSyncFlow
            ) { pendingItems, hasProfilePending ->
                val breakdown = LinkedHashMap<String, Int>()
                for (item in pendingItems) {
                    breakdown[item.collectionName] = (breakdown[item.collectionName] ?: 0) + 1
                }
                if (hasProfilePending && activeRole == DepotRole.OWNER) {
                    breakdown[SyncCollections.PROFIL] = 1
                }
                val totalCount = pendingItems.size + (if (hasProfilePending && activeRole == DepotRole.OWNER) 1 else 0)
                totalCount to breakdown.toMap()
            }.collect { (count, breakdown) ->
                val isOnline = checkIsOnline()
                val isOfflineLocal = isOfflineLocalSession()
                val expectedCols = if (activeDepotId.isBlank() || isOfflineLocal) 0 else expectedCollectionsForRole(activeRole).size
                _state.update {
                    val nextStatus = when {
                        isOfflineLocal -> SyncConnectionStatus.OFFLINE
                        isTerminalBlockingStatus(it.status) -> it.status
                        !isOnline -> SyncConnectionStatus.OFFLINE
                        count > 0 -> SyncConnectionStatus.SYNCING
                        activeDepotId.isNotBlank() && collectionListeners.isNotEmpty() -> SyncConnectionStatus.LIVE
                        else -> it.status
                    }
                    it.copy(
                        pendingCount = count,
                        pendingByCollection = breakdown,
                        isOnline = isOnline,
                        status = nextStatus,
                        activeListenerCount = collectionListeners.size,
                        expectedListenerCount = expectedCols
                    )
                }
            }
        }
    }

    /**
     * Memasukkan kembali seluruh data lokal yang diizinkan untuk peran aktif (OWNER atau STAFF)
     * ke antrean `pending_sync` agar dikirim ulang ke Cloud (Force Push All).
     */
    private suspend fun enqueueAllRoleScopedLocalRecords(uid: String, role: DepotRole) {
        if (role == DepotRole.OWNER) {
            profileStore.markPendingSync(uid, true)
            enqueueAllLocalRecordsForOfflineMerge()
        } else {
            val allKurirs = db.kurirDao().getAllOnce()
            val staffKurir = KaryawanScopeHelper.resolveActiveKaryawan(activeEmail, allKurirs) ?: return
            val allPelanggan = db.pelangganDao().getAllOnce()
            val allTransaksi = db.transaksiDao().getAllOnce()

            val staffCustomerIds = mutableSetOf<Int>()
            allPelanggan.forEach { p ->
                val isPlaceholder = p.isDeleted && p.updatedAt == 0L && p.nama == "Pelanggan Dihapus"
                if (!isPlaceholder && p.id_kurir_langganan == staffKurir.id) {
                    staffCustomerIds.add(p.id)
                    db.pendingSyncDao().enqueue(
                        PendingSyncEntity(
                            collectionName = SyncCollections.PELANGGAN,
                            syncId = p.syncId,
                            operation = SyncOperations.UPSERT,
                            updatedAt = p.updatedAt
                        )
                    )
                }
            }

            allTransaksi.forEach { tx ->
                if (tx.id_kurir == staffKurir.id || tx.id_pelanggan in staffCustomerIds) {
                    staffCustomerIds.add(tx.id_pelanggan)
                    db.pendingSyncDao().enqueue(
                        PendingSyncEntity(
                            collectionName = SyncCollections.TRANSAKSI,
                            syncId = tx.syncId,
                            operation = SyncOperations.UPSERT,
                            updatedAt = tx.updatedAt
                        )
                    )
                }
            }

            db.kasbonLogDao().getAllOnce().forEach { log ->
                if (log.id_pelanggan in staffCustomerIds) {
                    db.pendingSyncDao().enqueue(
                        PendingSyncEntity(
                            collectionName = SyncCollections.KASBON_LOGS,
                            syncId = log.syncId,
                            operation = SyncOperations.UPSERT,
                            updatedAt = log.updatedAt
                        )
                    )
                }
            }
        }
    }

    private suspend fun enqueueAllLocalRecordsForOfflineMerge() {
        val pendingKeys = db.pendingSyncDao().getAllPending()
            .map { "${it.collectionName}:${it.syncId}" }
            .toSet()

        suspend fun enqueueIfAbsent(col: String, syncId: String, updatedAt: Long) {
            if ("$col:$syncId" in pendingKeys) return
            db.pendingSyncDao().enqueue(
                PendingSyncEntity(
                    collectionName = col,
                    syncId = syncId,
                    operation = SyncOperations.UPSERT,
                    updatedAt = updatedAt
                )
            )
        }

        db.produkDao().getAllOnce().forEach { enqueueIfAbsent(SyncCollections.PRODUK, it.syncId, it.updatedAt) }
        db.kurirDao().getAllOnce().forEach { enqueueIfAbsent(SyncCollections.KURIR, it.syncId, it.updatedAt) }
        db.pelangganDao().getAllOnce().forEach {
            val isPlaceholder = it.isDeleted && it.updatedAt == 0L && it.nama == "Pelanggan Dihapus"
            if (!isPlaceholder) {
                enqueueIfAbsent(SyncCollections.PELANGGAN, it.syncId, it.updatedAt)
            }
        }
        db.transaksiDao().getAllOnce().forEach { enqueueIfAbsent(SyncCollections.TRANSAKSI, it.syncId, it.updatedAt) }
        db.pengeluaranDao().getAllOnce().forEach { enqueueIfAbsent(SyncCollections.PENGELUARAN, it.syncId, it.updatedAt) }
        db.kasbonLogDao().getAllOnce().forEach { enqueueIfAbsent(SyncCollections.KASBON_LOGS, it.syncId, it.updatedAt) }
        db.perawatanDao().getAllOnce().forEach { enqueueIfAbsent(SyncCollections.PERAWATAN_JADWAL, it.syncId, it.updatedAt) }
        db.perawatanLogDao().getAllOnce().forEach { enqueueIfAbsent(SyncCollections.PERAWATAN_LOG, it.syncId, it.updatedAt) }
        db.tangkiRefillDao().getAllOnce().forEach { enqueueIfAbsent(SyncCollections.TANGKI_REFILL, it.syncId, it.updatedAt) }
    }

    private suspend fun sanitizeOwnerOnlyDataForStaffMerge() {
        val ownerOnlyCols = setOf(
            SyncCollections.PRODUK,
            SyncCollections.KURIR,
            SyncCollections.PENGELUARAN,
            SyncCollections.PERAWATAN_JADWAL,
            SyncCollections.PERAWATAN_LOG,
            SyncCollections.TANGKI_REFILL
        )
        db.withTransaction {
            db.tangkiRefillDao().deleteAll()
            db.perawatanLogDao().deleteAll()
            db.perawatanDao().deleteAll()
            db.pengeluaranDao().deleteAll()
            db.kurirDao().deleteAll()
            db.produkDao().deleteAll()
            db.pendingSyncDao().getAllPending()
                .filter { it.collectionName in ownerOnlyCols }
                .forEach { db.pendingSyncDao().remove(it.collectionName, it.syncId) }
        }
    }

    private suspend fun enqueueMissingLocalRecordsForOwner(
        sourceSnapshots: Map<String, List<DocumentSnapshot>>,
        allowedCollections: Set<String>?
    ) {
        // Jaring pengaman: jangan pernah mengunggah data lokal jika pemilik sesi
        // tidak sama dengan pemilik yang tercatat di perangkat ini.
        val recordedOwnerUid = prefs.getString(KEY_LAST_OWNER_UID, null)
        if (recordedOwnerUid.isNullOrBlank() || recordedOwnerUid != activeUid) return

        val pendingKeys = db.pendingSyncDao().getAllPending()
            .map { "${it.collectionName}:${it.syncId}" }
            .toSet()

        fun shouldProcess(col: String): Boolean = allowedCollections == null || col in allowedCollections

        suspend fun checkAndEnqueue(col: String, syncId: String, localUpdatedAt: Long) {
            if ("$col:$syncId" in pendingKeys) return
            val cloudDoc = sourceSnapshots[col]?.firstOrNull { it.id == syncId }
            val cloudUpdatedAt = (cloudDoc?.get("updatedAt") as? Number)?.toLong() ?: -1L
            val cloudDeleted = cloudDoc?.getBoolean("isDeleted") == true
            if (cloudDoc == null || (!cloudDeleted && localUpdatedAt > cloudUpdatedAt)) {
                db.pendingSyncDao().enqueue(
                    PendingSyncEntity(
                        collectionName = col,
                        syncId = syncId,
                        operation = SyncOperations.UPSERT,
                        updatedAt = localUpdatedAt
                    )
                )
            }
        }

        if (shouldProcess(SyncCollections.PRODUK)) {
            db.produkDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.PRODUK, it.syncId, it.updatedAt) }
        }
        if (shouldProcess(SyncCollections.KURIR)) {
            db.kurirDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.KURIR, it.syncId, it.updatedAt) }
        }
        if (shouldProcess(SyncCollections.PELANGGAN)) {
            db.pelangganDao().getAllOnce().forEach {
                val isPlaceholder = it.isDeleted && it.updatedAt == 0L && it.nama == "Pelanggan Dihapus"
                if (!isPlaceholder) {
                    checkAndEnqueue(SyncCollections.PELANGGAN, it.syncId, it.updatedAt)
                }
            }
        }
        if (shouldProcess(SyncCollections.TRANSAKSI)) {
            db.transaksiDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.TRANSAKSI, it.syncId, it.updatedAt) }
        }
        if (shouldProcess(SyncCollections.PENGELUARAN)) {
            db.pengeluaranDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.PENGELUARAN, it.syncId, it.updatedAt) }
        }
        if (shouldProcess(SyncCollections.KASBON_LOGS)) {
            db.kasbonLogDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.KASBON_LOGS, it.syncId, it.updatedAt) }
        }
        if (shouldProcess(SyncCollections.PERAWATAN_JADWAL)) {
            db.perawatanDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.PERAWATAN_JADWAL, it.syncId, it.updatedAt) }
        }
        if (shouldProcess(SyncCollections.PERAWATAN_LOG)) {
            db.perawatanLogDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.PERAWATAN_LOG, it.syncId, it.updatedAt) }
        }
        if (shouldProcess(SyncCollections.TANGKI_REFILL)) {
            db.tangkiRefillDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.TANGKI_REFILL, it.syncId, it.updatedAt) }
        }
    }

    private suspend fun applySnapshotsInOrder(
        snapshotsByCollection: Map<String, List<DocumentSnapshot>>,
        seedDefaultPerawatanIfEmpty: Boolean,
        reconcileHardDeletes: Boolean = false
    ) {
        val hasAnyDocuments = snapshotsByCollection.values.any { it.isNotEmpty() }
        if (!hasAnyDocuments && !seedDefaultPerawatanIfEmpty && !reconcileHardDeletes) return

        applyMutex.withLock {
            val pendingMap = db.pendingSyncDao().getAllPending()
                .associateBy { "${it.collectionName}:${it.syncId}" }

            db.withTransaction {
                if (hasAnyDocuments) {
                    // 1. Produk
                    snapshotsByCollection[SyncCollections.PRODUK]?.forEach { doc ->
                        applyProdukDoc(doc, pendingMap["${SyncCollections.PRODUK}:${doc.id}"])
                    }

                    // 2. Kurir (also ensures depot/staff links stay clean if needed)
                    snapshotsByCollection[SyncCollections.KURIR]?.forEach { doc ->
                        applyKurirDoc(doc, pendingMap["${SyncCollections.KURIR}:${doc.id}"])
                    }

                    // 3. Pelanggan (depends on Kurir syncId -> local id)
                    val kurirSyncToId = db.kurirDao().getAllOnce().associate { it.syncId to it.id }
                    snapshotsByCollection[SyncCollections.PELANGGAN]?.forEach { doc ->
                        applyPelangganDoc(doc, kurirSyncToId, pendingMap["${SyncCollections.PELANGGAN}:${doc.id}"])
                    }

                    // 4. Transaksi (depends on Pelanggan, Kurir, Produk)
                    val pelangganSyncToId = db.pelangganDao().getAllOnce().associate { it.syncId to it.id }
                    val produkSyncToId = db.produkDao().getAllOnce().associate { it.syncId to it.id }
                    snapshotsByCollection[SyncCollections.TRANSAKSI]?.forEach { doc ->
                        applyTransaksiDoc(
                            doc = doc,
                            pelangganSyncToId = pelangganSyncToId,
                            kurirSyncToId = kurirSyncToId,
                            produkSyncToId = produkSyncToId,
                            pending = pendingMap["${SyncCollections.TRANSAKSI}:${doc.id}"]
                        )
                    }

                    // 5. Kasbon Logs
                    snapshotsByCollection[SyncCollections.KASBON_LOGS]?.forEach { doc ->
                        applyKasbonLogDoc(
                            doc = doc,
                            pelangganSyncToId = pelangganSyncToId,
                            pending = pendingMap["${SyncCollections.KASBON_LOGS}:${doc.id}"]
                        )
                    }

                    // 6. Pengeluaran & Perawatan RO (Owner only)
                    if (activeRole == DepotRole.OWNER) {
                        snapshotsByCollection[SyncCollections.PENGELUARAN]?.forEach { doc ->
                            applyPengeluaranDoc(doc, pendingMap["${SyncCollections.PENGELUARAN}:${doc.id}"])
                        }
                        snapshotsByCollection[SyncCollections.PERAWATAN_JADWAL]?.forEach { doc ->
                            applyPerawatanJadwalDoc(doc, pendingMap["${SyncCollections.PERAWATAN_JADWAL}:${doc.id}"])
                        }
                        snapshotsByCollection[SyncCollections.PERAWATAN_LOG]?.forEach { doc ->
                            applyPerawatanLogDoc(doc, pendingMap["${SyncCollections.PERAWATAN_LOG}:${doc.id}"])
                        }
                        snapshotsByCollection[SyncCollections.TANGKI_REFILL]?.forEach { doc ->
                            applyTangkiRefillDoc(doc, pendingMap["${SyncCollections.TANGKI_REFILL}:${doc.id}"])
                        }
                    }
                }

                // Jika mode restore penuh ("Ambil Data dari Cloud"), hapus baris lokal yang tidak lagi ada di Cloud
                // (mis. dokumen yang dihapus fisik langsung di Firestore) selama tidak sedang dalam antrean pending_sync.
                if (reconcileHardDeletes) {
                    reconcileMissingCloudRecords(snapshotsByCollection, pendingMap)
                }

                if (activeRole == DepotRole.OWNER &&
                    seedDefaultPerawatanIfEmpty &&
                    snapshotsByCollection[SyncCollections.PERAWATAN_JADWAL].isNullOrEmpty() &&
                    db.perawatanDao().count() == 0
                ) {
                    db.perawatanDao().insertAllIgnore(PerawatanScheduler.buildDefaultItems())
                }
            }
        }
    }

    private suspend fun reconcileMissingCloudRecords(
        snapshotsByCollection: Map<String, List<DocumentSnapshot>>,
        pendingMap: Map<String, PendingSyncEntity>
    ) {
        suspend fun pruneCol(col: String, localSyncIds: List<String>) {
            val cloudIds = snapshotsByCollection[col]?.map { it.id }?.toSet() ?: return
            for (syncId in localSyncIds) {
                if (syncId !in cloudIds && !pendingMap.containsKey("$col:$syncId")) {
                    deleteLocalBySyncId(col, syncId)
                }
            }
        }

        pruneCol(SyncCollections.TRANSAKSI, db.transaksiDao().getAllOnce().map { it.syncId })
        pruneCol(SyncCollections.KASBON_LOGS, db.kasbonLogDao().getAllOnce().map { it.syncId })
        pruneCol(SyncCollections.PELANGGAN, db.pelangganDao().getAllOnce().map { it.syncId })
        pruneCol(SyncCollections.KURIR, db.kurirDao().getAllOnce().map { it.syncId })
        pruneCol(SyncCollections.PRODUK, db.produkDao().getAllOnce().map { it.syncId })
        if (activeRole == DepotRole.OWNER) {
            pruneCol(SyncCollections.PENGELUARAN, db.pengeluaranDao().getAllOnce().map { it.syncId })
            pruneCol(SyncCollections.PERAWATAN_JADWAL, db.perawatanDao().getAllOnce().map { it.syncId })
            pruneCol(SyncCollections.PERAWATAN_LOG, db.perawatanLogDao().getAllOnce().map { it.syncId })
            pruneCol(SyncCollections.TANGKI_REFILL, db.tangkiRefillDao().getAllOnce().map { it.syncId })
        }
    }

    /**
     * Terapkan hanya dokumen yang berubah, bukan seluruh koleksi. Ini menjaga
     * biaya sinkronisasi tetap proporsional saat data depot sudah besar.
     */
    private suspend fun applyChangedDocuments(colName: String, changes: List<DocumentChange>) {
        if (changes.isEmpty()) return
        applyMutex.withLock {
            val pendingMap = db.pendingSyncDao().getAllPending()
                .associateBy { "${it.collectionName}:${it.syncId}" }
            val pelangganSyncToId = if (colName == SyncCollections.PELANGGAN ||
                colName == SyncCollections.TRANSAKSI ||
                colName == SyncCollections.KASBON_LOGS
            ) {
                db.pelangganDao().getAllOnce().associate { it.syncId to it.id }
            } else {
                emptyMap()
            }
            val kurirSyncToId = if (colName == SyncCollections.PELANGGAN ||
                colName == SyncCollections.TRANSAKSI
            ) {
                db.kurirDao().getAllOnce().associate { it.syncId to it.id }
            } else {
                emptyMap()
            }
            val produkSyncToId = if (colName == SyncCollections.TRANSAKSI) {
                db.produkDao().getAllOnce().associate { it.syncId to it.id }
            } else {
                emptyMap()
            }

            db.withTransaction {
                changes.forEach { change ->
                    val doc = change.document
                    val pending = pendingMap["$colName:${doc.id}"]
                    if (change.type == DocumentChange.Type.REMOVED) {
                        deleteLocalBySyncId(colName, doc.id)
                    } else {
                        when (colName) {
                            SyncCollections.PRODUK -> applyProdukDoc(doc, pending)
                            SyncCollections.KURIR -> applyKurirDoc(doc, pending)
                            SyncCollections.PELANGGAN -> applyPelangganDoc(doc, kurirSyncToId, pending)
                            SyncCollections.TRANSAKSI -> applyTransaksiDoc(
                                doc = doc,
                                pelangganSyncToId = pelangganSyncToId,
                                kurirSyncToId = kurirSyncToId,
                                produkSyncToId = produkSyncToId,
                                pending = pending
                            )
                            SyncCollections.KASBON_LOGS -> applyKasbonLogDoc(
                                doc = doc,
                                pelangganSyncToId = pelangganSyncToId,
                                pending = pending
                            )
                            SyncCollections.PENGELUARAN -> {
                                if (activeRole == DepotRole.OWNER) applyPengeluaranDoc(doc, pending)
                            }
                            SyncCollections.PERAWATAN_JADWAL -> {
                                if (activeRole == DepotRole.OWNER) applyPerawatanJadwalDoc(doc, pending)
                            }
                            SyncCollections.PERAWATAN_LOG -> {
                                if (activeRole == DepotRole.OWNER) applyPerawatanLogDoc(doc, pending)
                            }
                            SyncCollections.TANGKI_REFILL -> {
                                if (activeRole == DepotRole.OWNER) applyTangkiRefillDoc(doc, pending)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun deleteLocalBySyncId(colName: String, syncId: String) {
        when (colName) {
            SyncCollections.PRODUK -> db.produkDao().deleteBySyncId(syncId)
            SyncCollections.KURIR -> {
                val local = db.kurirDao().getBySyncId(syncId)
                if (local != null) {
                    db.kurirDao().update(
                        local.copy(
                            akses_aktif = false,
                            isDeleted = true,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            SyncCollections.PELANGGAN -> {
                val local = db.pelangganDao().getBySyncId(syncId)
                if (local != null) {
                    if (db.transaksiDao().countByPelanggan(local.id) > 0) {
                        if (!KaryawanScopeHelper.isSoftDeletedPelanggan(local)) {
                            db.pelangganDao().update(
                                local.copy(
                                    alamat = KaryawanScopeHelper.formatDeletedPelangganAlamat(local.alamat, "Pemilik"),
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    } else {
                        db.pelangganDao().deleteBySyncId(syncId)
                    }
                }
            }
            SyncCollections.TRANSAKSI -> db.transaksiDao().deleteBySyncId(syncId)
            SyncCollections.KASBON_LOGS -> db.kasbonLogDao().deleteBySyncId(syncId)
            SyncCollections.PENGELUARAN -> db.pengeluaranDao().deleteBySyncId(syncId)
            SyncCollections.PERAWATAN_JADWAL -> db.perawatanDao().deleteBySyncId(syncId)
            SyncCollections.PERAWATAN_LOG -> db.perawatanLogDao().deleteBySyncId(syncId)
            SyncCollections.TANGKI_REFILL -> db.tangkiRefillDao().deleteBySyncId(syncId)
        }
    }

    private fun extractCloudTimestamp(doc: DocumentSnapshot): Long {
        val serverTs = try {
            doc.getTimestamp("serverUpdatedAt")?.toDate()?.time
        } catch (_: Exception) {
            null
        }
        return serverTs ?: (doc.get("updatedAt") as? Number)?.toLong() ?: 0L
    }

    private suspend fun applyProdukDoc(doc: DocumentSnapshot, pending: PendingSyncEntity?) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = extractCloudTimestamp(doc)
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isDeleted = doc.getBoolean("isDeleted") == true
        val local = db.produkDao().getBySyncId(syncId)

        if (isDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                db.transaksiDao().clearProdukFromTransaksi(local.id, cloudUpdatedAt)
                db.produkDao().deleteBySyncId(syncId)
            }
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.PRODUK, syncId)
            return
        }

        val nama = doc.getString("nama_produk")?.takeIf { it.isNotBlank() } ?: return
        val harga = (doc.get("harga") as? Number)?.toDouble() ?: 6000.0

        if (local == null) {
            db.produkDao().insert(
                Produk(
                    nama_produk = nama,
                    harga = harga,
                    updatedAt = cloudUpdatedAt,
                    syncId = syncId
                )
            )
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.PRODUK, syncId)
        } else {
            val remote = local.copy(
                nama_produk = nama,
                harga = harga,
                updatedAt = cloudUpdatedAt
            )
            val resolved = SyncConflictResolver.resolve(local, remote)
            if (resolved !== local) {
                db.produkDao().update(resolved)
                if (pending != null) db.pendingSyncDao().remove(SyncCollections.PRODUK, syncId)
            }
        }
    }

    private suspend fun applyKurirDoc(doc: DocumentSnapshot, pending: PendingSyncEntity?) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = extractCloudTimestamp(doc)
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isDeleted = doc.getBoolean("isDeleted") == true
        val local = db.kurirDao().getBySyncId(syncId)

        if (isDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                // Jangan hapus baris atau mengosongkan id_kurir di transaksi lama:
                // komisi historis harus tetap bisa dihitung.
                db.kurirDao().update(
                    local.copy(
                        akses_aktif = false,
                        isDeleted = true,
                        updatedAt = cloudUpdatedAt
                    )
                )
            }
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.KURIR, syncId)
            return
        }

        val nama = doc.getString("nama")?.takeIf { it.isNotBlank() } ?: return
        val komisi = (doc.get("komisi_per_galon") as? Number)?.toDouble() ?: 0.0
        val email = doc.getString("email") ?: ""
        val aksesAktif = doc.getBoolean("akses_aktif") ?: true
        val skema = doc.getString("skema_komisi") ?: "TETAP"
        val aturan = doc.getString("aturan_komisi") ?: ""

        if (local == null) {
            val normalizedIncomingEmail = KaryawanScopeHelper.normalizeEmail(email)
            val duplicateSeeded = if (normalizedIncomingEmail.isNotBlank()) {
                val seededSyncId = KaryawanScopeHelper.defaultStaffSyncId(normalizedIncomingEmail)
                if (seededSyncId != syncId) db.kurirDao().getBySyncId(seededSyncId) else null
            } else {
                null
            }
            if (duplicateSeeded != null) {
                db.pendingSyncDao().remove(SyncCollections.KURIR, duplicateSeeded.syncId)
                db.kurirDao().deleteBySyncId(duplicateSeeded.syncId)
            }
            db.kurirDao().insert(
                Kurir(
                    nama = nama,
                    komisi_per_galon = komisi,
                    email = email,
                    password = "",
                    akses_aktif = aksesAktif,
                    skema_komisi = skema,
                    aturan_komisi = aturan,
                    updatedAt = cloudUpdatedAt,
                    syncId = syncId
                )
            )
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.KURIR, syncId)
        } else {
            val remote = local.copy(
                nama = nama,
                komisi_per_galon = komisi,
                email = email,
                akses_aktif = aksesAktif,
                skema_komisi = skema,
                aturan_komisi = aturan,
                updatedAt = cloudUpdatedAt,
                isDeleted = false
            )
            val resolved = SyncConflictResolver.resolve(local, remote)
            if (resolved !== local) {
                db.kurirDao().update(resolved)
                if (pending != null) db.pendingSyncDao().remove(SyncCollections.KURIR, syncId)
            }
        }
    }

    private suspend fun applyPelangganDoc(
        doc: DocumentSnapshot,
        kurirSyncToId: Map<String, Int>,
        pending: PendingSyncEntity?
    ) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = extractCloudTimestamp(doc)
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isDeleted = doc.getBoolean("isDeleted") == true
        val deletedBy = doc.getString("deletedBy")
        val local = db.pelangganDao().getBySyncId(syncId)

        if (isDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                if (db.transaksiDao().countByPelanggan(local.id) > 0) {
                    // Pelanggan dihapus di perangkat lain, tetapi transaksi lama
                    // masih merujuk ke baris ini. Tandai soft-delete lokal, jangan hapus.
                    if (!local.isDeleted) {
                        val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(local.alamat)
                        db.pelangganDao().update(
                            local.copy(
                                alamat = cleanAlamat,
                                isDeleted = true,
                                deletedBy = deletedBy ?: "Pemilik",
                                updatedAt = cloudUpdatedAt
                            )
                        )
                    }
                } else {
                    db.pelangganDao().deleteBySyncId(syncId)
                }
            }
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.PELANGGAN, syncId)
            return
        }

        val nama = doc.getString("nama")?.takeIf { it.isNotBlank() } ?: return
        val alamat = doc.getString("alamat") ?: ""
        val noHp = doc.getString("no_hp") ?: ""
        val sisaKasbon = (doc.get("sisa_kasbon") as? Number)?.toDouble() ?: 0.0
        val hargaKhusus = (doc.get("harga_khusus") as? Number)?.toDouble()?.takeIf { it > 0.0 }
        val kurirSyncId = doc.getString("kurirSyncId")
        val idKurirLangganan = kurirSyncId?.let { kurirSyncToId[it] }

        if (local == null) {
            db.pelangganDao().insert(
                Pelanggan(
                    nama = nama,
                    alamat = alamat,
                    no_hp = noHp,
                    sisa_kasbon = sisaKasbon,
                    harga_khusus = hargaKhusus,
                    id_kurir_langganan = idKurirLangganan,
                    updatedAt = cloudUpdatedAt,
                    syncId = syncId,
                    isDeleted = false,
                    deletedBy = null
                )
            )
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.PELANGGAN, syncId)
        } else {
            val remote = local.copy(
                nama = nama,
                alamat = alamat,
                no_hp = noHp,
                sisa_kasbon = sisaKasbon,
                harga_khusus = hargaKhusus,
                id_kurir_langganan = idKurirLangganan,
                updatedAt = cloudUpdatedAt,
                isDeleted = false,
                deletedBy = null
            )
            val resolved = SyncConflictResolver.resolve(local, remote)
            if (resolved !== local) {
                db.pelangganDao().update(resolved)
                if (pending != null) db.pendingSyncDao().remove(SyncCollections.PELANGGAN, syncId)
            }
        }
    }

    /**
     * Pelanggan bisa sudah dihapus permanen di perangkat lain. Agar riwayat
     * transaksi/kasbon tidak hilang, buat baris placeholder lokal dengan syncId
     * yang sama. Placeholder ditandai soft-delete dan tidak pernah diunggah.
     */
    private suspend fun ensurePlaceholderPelanggan(pelangganSyncId: String): Int {
        db.pelangganDao().getBySyncId(pelangganSyncId)?.let { return it.id }
        return db.pelangganDao().insert(
            Pelanggan(
                nama = "Pelanggan Dihapus",
                alamat = "",
                no_hp = "",
                sisa_kasbon = 0.0,
                updatedAt = 0L,
                syncId = pelangganSyncId,
                isDeleted = true,
                deletedBy = "Pemilik"
            )
        ).toInt()
    }

    private suspend fun applyTransaksiDoc(
        doc: DocumentSnapshot,
        pelangganSyncToId: Map<String, Int>,
        kurirSyncToId: Map<String, Int>,
        produkSyncToId: Map<String, Int>,
        pending: PendingSyncEntity?
    ) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = extractCloudTimestamp(doc)
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isHardDeleted = doc.getBoolean("_hardDeleted") == true ||
            (doc.getBoolean("isDeleted") == true && doc.getString("pelangganSyncId") == "deleted")
        val local = db.transaksiDao().getBySyncId(syncId)

        if (isHardDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                db.transaksiDao().deleteBySyncId(syncId)
            }
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.TRANSAKSI, syncId)
            return
        }

        val pelangganSyncId = doc.getString("pelangganSyncId") ?: return
        val idPelanggan = pelangganSyncToId[pelangganSyncId] ?: ensurePlaceholderPelanggan(pelangganSyncId)
        val kurirSyncId = doc.getString("kurirSyncId")
        val idKurir = kurirSyncId?.let { kurirSyncToId[it] }
        val produkSyncId = doc.getString("produkSyncId")
        val idProduk = produkSyncId?.let { produkSyncToId[it] }

        val tanggal = (doc.get("tanggal") as? Number)?.toLong() ?: System.currentTimeMillis()
        val jumlah = ((doc.get("jumlah") as? Number)?.toInt() ?: 1).coerceAtLeast(1)
        val hargaSatuan = (doc.get("harga_satuan") as? Number)?.toDouble() ?: 0.0
        val totalHarga = (doc.get("total_harga") as? Number)?.toDouble() ?: (jumlah * hargaSatuan)
        val totalKomisi = (doc.get("total_komisi") as? Number)?.toDouble() ?: 0.0
        val metode = doc.getString("metode_bayar") ?: "Cash"
        val status = doc.getString("status") ?: "Lunas"
        val uangMuka = (doc.get("uangMuka") as? Number)?.toLong() ?: 0L
        val sisaKasbon = (doc.get("sisaKasbon") as? Number)?.toLong() ?: 0L
        val tanggalBayar = (doc.get("tanggalBayar") as? Number)?.toLong()
        val isSoftDeleted = doc.getBoolean("isDeleted") == true || status.equals("Dihapus", ignoreCase = true)
        val deletedBy = doc.getString("deletedBy")
        val rawWaterType = doc.getString("waterType")?.trim()?.uppercase()
        val waterType = if (rawWaterType == Transaksi.WATER_TYPE_MINERAL) Transaksi.WATER_TYPE_MINERAL else Transaksi.WATER_TYPE_RO

        if (local == null) {
            db.transaksiDao().insert(
                Transaksi(
                    tanggal = tanggal,
                    id_pelanggan = idPelanggan,
                    id_kurir = idKurir,
                    id_produk = idProduk,
                    jumlah = jumlah,
                    harga_satuan = hargaSatuan,
                    total_harga = totalHarga,
                    total_komisi = totalKomisi,
                    metode_bayar = metode,
                    status = status,
                    uangMuka = uangMuka,
                    sisaKasbon = sisaKasbon,
                    tanggalBayar = tanggalBayar,
                    waterType = waterType,
                    updatedAt = cloudUpdatedAt,
                    syncId = syncId,
                    isDeleted = isSoftDeleted,
                    deletedBy = deletedBy
                )
            )
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.TRANSAKSI, syncId)
        } else {
            val remote = local.copy(
                tanggal = tanggal,
                id_pelanggan = idPelanggan,
                id_kurir = idKurir,
                id_produk = idProduk,
                jumlah = jumlah,
                harga_satuan = hargaSatuan,
                total_harga = totalHarga,
                total_komisi = totalKomisi,
                metode_bayar = metode,
                status = status,
                uangMuka = uangMuka,
                sisaKasbon = sisaKasbon,
                tanggalBayar = tanggalBayar ?: local.tanggalBayar,
                waterType = if (rawWaterType != null) waterType else local.waterType,
                updatedAt = cloudUpdatedAt,
                isDeleted = isSoftDeleted,
                deletedBy = deletedBy
            )
            val resolved = SyncConflictResolver.resolve(local, remote)
            if (resolved !== local) {
                db.transaksiDao().update(resolved)
                if (pending != null) db.pendingSyncDao().remove(SyncCollections.TRANSAKSI, syncId)
            }
        }
    }

    private suspend fun applyKasbonLogDoc(
        doc: DocumentSnapshot,
        pelangganSyncToId: Map<String, Int>,
        pending: PendingSyncEntity?
    ) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = extractCloudTimestamp(doc)
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isDeleted = doc.getBoolean("isDeleted") == true
        val local = db.kasbonLogDao().getBySyncId(syncId)

        if (isDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                db.kasbonLogDao().deleteBySyncId(syncId)
            }
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.KASBON_LOGS, syncId)
            return
        }

        val pelangganSyncId = doc.getString("pelangganSyncId") ?: return
        val idPelanggan = pelangganSyncToId[pelangganSyncId] ?: ensurePlaceholderPelanggan(pelangganSyncId)
        val tanggal = (doc.get("tanggal") as? Number)?.toLong() ?: System.currentTimeMillis()
        val jumlahBayar = (doc.get("jumlah_bayar") as? Number)?.toDouble() ?: 0.0
        val sebelum = (doc.get("sisa_kasbon_sebelum") as? Number)?.toDouble() ?: 0.0
        val sesudah = (doc.get("sisa_kasbon_sesudah") as? Number)?.toDouble() ?: 0.0
        val metode = doc.getString("metode_bayar") ?: "Cash"
        val catatan = doc.getString("catatan") ?: ""

        if (local == null) {
            db.kasbonLogDao().insert(
                KasbonLog(
                    id_pelanggan = idPelanggan,
                    tanggal = tanggal,
                    jumlah_bayar = jumlahBayar,
                    sisa_kasbon_sebelum = sebelum,
                    sisa_kasbon_sesudah = sesudah,
                    metode_bayar = metode,
                    catatan = catatan,
                    updatedAt = cloudUpdatedAt,
                    syncId = syncId
                )
            )
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.KASBON_LOGS, syncId)
        } else {
            val remote = local.copy(
                id_pelanggan = idPelanggan,
                tanggal = tanggal,
                jumlah_bayar = jumlahBayar,
                sisa_kasbon_sebelum = sebelum,
                sisa_kasbon_sesudah = sesudah,
                metode_bayar = metode,
                catatan = catatan,
                updatedAt = cloudUpdatedAt
            )
            val resolved = SyncConflictResolver.resolve(local, remote)
            if (resolved !== local) {
                db.kasbonLogDao().update(resolved)
                if (pending != null) db.pendingSyncDao().remove(SyncCollections.KASBON_LOGS, syncId)
            }
        }
    }

    private suspend fun applyPengeluaranDoc(doc: DocumentSnapshot, pending: PendingSyncEntity?) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = extractCloudTimestamp(doc)
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isDeleted = doc.getBoolean("isDeleted") == true
        val local = db.pengeluaranDao().getBySyncId(syncId)

        if (isDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                db.pengeluaranDao().deleteBySyncId(syncId)
            }
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.PENGELUARAN, syncId)
            return
        }

        val tanggal = (doc.get("tanggal") as? Number)?.toLong() ?: System.currentTimeMillis()
        val namaItem = doc.getString("nama_item")?.takeIf { it.isNotBlank() } ?: return
        val kategori = doc.getString("kategori") ?: "Operasional"
        val nominal = (doc.get("nominal") as? Number)?.toDouble() ?: 0.0

        if (local == null) {
            db.pengeluaranDao().insert(
                Pengeluaran(
                    tanggal = tanggal,
                    nama_item = namaItem,
                    kategori = kategori,
                    nominal = nominal,
                    updatedAt = cloudUpdatedAt,
                    syncId = syncId
                )
            )
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.PENGELUARAN, syncId)
        } else {
            val remote = local.copy(
                tanggal = tanggal,
                nama_item = namaItem,
                kategori = kategori,
                nominal = nominal,
                updatedAt = cloudUpdatedAt
            )
            val resolved = SyncConflictResolver.resolve(local, remote)
            if (resolved !== local) {
                db.pengeluaranDao().update(resolved)
                if (pending != null) db.pendingSyncDao().remove(SyncCollections.PENGELUARAN, syncId)
            }
        }
    }

    private suspend fun applyPerawatanJadwalDoc(doc: DocumentSnapshot, pending: PendingSyncEntity?) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = extractCloudTimestamp(doc)
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isDeleted = doc.getBoolean("isDeleted") == true
        val local = db.perawatanDao().getBySyncId(syncId)

        if (isDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                db.perawatanDao().deleteBySyncId(syncId)
            }
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.PERAWATAN_JADWAL, syncId)
            return
        }

        val nama = doc.getString("nama")?.takeIf { it.isNotBlank() } ?: return
        val intervalHari = ((doc.get("intervalHari") as? Number)?.toInt() ?: 30).coerceIn(1, 3650)
        val terakhirSelesai = (doc.get("terakhirSelesai") as? Number)?.toLong()
        val jadwalBerikutnya = (doc.get("jadwalBerikutnya") as? Number)?.toLong()
            ?: PerawatanScheduler.calculateNextDueDateMillis(System.currentTimeMillis(), intervalHari)

        if (local == null) {
            db.perawatanDao().insert(
                PerawatanItem(
                    nama = nama,
                    intervalHari = intervalHari,
                    terakhirSelesai = terakhirSelesai,
                    jadwalBerikutnya = jadwalBerikutnya,
                    updatedAt = cloudUpdatedAt,
                    syncId = syncId
                )
            )
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.PERAWATAN_JADWAL, syncId)
        } else {
            val remote = local.copy(
                nama = nama,
                intervalHari = intervalHari,
                terakhirSelesai = terakhirSelesai,
                jadwalBerikutnya = jadwalBerikutnya,
                updatedAt = cloudUpdatedAt
            )
            val resolved = SyncConflictResolver.resolve(local, remote)
            if (resolved !== local) {
                db.perawatanDao().update(resolved)
                if (pending != null) db.pendingSyncDao().remove(SyncCollections.PERAWATAN_JADWAL, syncId)
            }
        }
    }

    private suspend fun applyPerawatanLogDoc(doc: DocumentSnapshot, pending: PendingSyncEntity?) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = extractCloudTimestamp(doc)
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isDeleted = doc.getBoolean("isDeleted") == true
        val local = db.perawatanLogDao().getBySyncId(syncId)

        if (isDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                db.perawatanLogDao().deleteBySyncId(syncId)
            }
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.PERAWATAN_LOG, syncId)
            return
        }

        val perawatanSyncId = doc.getString("perawatanSyncId")?.takeIf { it.isNotBlank() } ?: return
        val namaPerawatan = doc.getString("namaPerawatan")?.takeIf { it.isNotBlank() } ?: return
        val tanggalSelesai = (doc.get("tanggalSelesai") as? Number)?.toLong() ?: 0L
        val biaya = ((doc.get("biaya") as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
        val catatan = doc.getString("catatan") ?: ""
        val pengeluaranSyncId = doc.getString("pengeluaranSyncId")

        if (local == null) {
            db.perawatanLogDao().insert(
                PerawatanLog(
                    perawatanSyncId = perawatanSyncId,
                    namaPerawatan = namaPerawatan,
                    tanggalSelesai = tanggalSelesai,
                    biaya = biaya,
                    catatan = catatan,
                    pengeluaranSyncId = pengeluaranSyncId,
                    updatedAt = cloudUpdatedAt,
                    syncId = syncId
                )
            )
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.PERAWATAN_LOG, syncId)
        } else {
            val remote = local.copy(
                perawatanSyncId = perawatanSyncId,
                namaPerawatan = namaPerawatan,
                tanggalSelesai = tanggalSelesai,
                biaya = biaya,
                catatan = catatan,
                pengeluaranSyncId = pengeluaranSyncId,
                updatedAt = cloudUpdatedAt
            )
            val resolved = SyncConflictResolver.resolve(local, remote)
            if (resolved !== local) {
                db.perawatanLogDao().update(resolved)
                if (pending != null) db.pendingSyncDao().remove(SyncCollections.PERAWATAN_LOG, syncId)
            }
        }
    }

    private suspend fun applyTangkiRefillDoc(doc: DocumentSnapshot, pending: PendingSyncEntity?) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = extractCloudTimestamp(doc)
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isDeleted = doc.getBoolean("isDeleted") == true
        val local = db.tangkiRefillDao().getBySyncId(syncId)

        if (isDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                db.tangkiRefillDao().deleteBySyncId(syncId)
            }
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.TANGKI_REFILL, syncId)
            return
        }

        val tanggal = (doc.get("tanggal") as? Number)?.toLong() ?: System.currentTimeMillis()
        val volumeLiter = ((doc.get("volumeLiter") as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
        val totalHarga = ((doc.get("totalHarga") as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
        val namaPemasok = doc.getString("namaPemasok") ?: ""
        val catatan = doc.getString("catatan") ?: ""
        val pengeluaranSyncId = doc.getString("pengeluaranSyncId")
        val linkedExpenseId = if (pengeluaranSyncId != null) {
            db.pengeluaranDao().getBySyncId(pengeluaranSyncId)?.id
        } else {
            (doc.get("linkedExpenseId") as? Number)?.toInt()
        }
        val deletedBy = doc.getString("deletedBy")

        if (local == null) {
            db.tangkiRefillDao().insert(
                TangkiRefill(
                    tanggal = tanggal,
                    volumeLiter = volumeLiter,
                    totalHarga = totalHarga,
                    namaPemasok = namaPemasok,
                    catatan = catatan,
                    linkedExpenseId = linkedExpenseId,
                    pengeluaranSyncId = pengeluaranSyncId,
                    updatedAt = cloudUpdatedAt,
                    syncId = syncId,
                    isDeleted = false,
                    deletedBy = deletedBy
                )
            )
            if (pending != null) db.pendingSyncDao().remove(SyncCollections.TANGKI_REFILL, syncId)
        } else {
            val remote = local.copy(
                tanggal = tanggal,
                volumeLiter = volumeLiter,
                totalHarga = totalHarga,
                namaPemasok = namaPemasok,
                catatan = catatan,
                linkedExpenseId = linkedExpenseId ?: local.linkedExpenseId,
                pengeluaranSyncId = pengeluaranSyncId ?: local.pengeluaranSyncId,
                updatedAt = cloudUpdatedAt,
                isDeleted = false,
                deletedBy = deletedBy
            )
            val resolved = SyncConflictResolver.resolve(local, remote)
            if (resolved !== local) {
                db.tangkiRefillDao().update(resolved)
                if (pending != null) db.pendingSyncDao().remove(SyncCollections.TANGKI_REFILL, syncId)
            }
        }
    }

    private suspend fun flushPendingQueueInternal(
        targetDepotId: String = activeDepotId,
        targetRole: DepotRole = activeRole
    ) {
        val uid = activeUid ?: return
        if (uid == OfflineSessionManager.OFFLINE_LOCAL_UID) return
        val depotId = targetDepotId.ifBlank { return }
        if (isTerminalBlockingStatus(_state.value.status)) return
        if (!checkIsOnline()) {
            _state.update { it.copy(isOnline = false, status = SyncConnectionStatus.OFFLINE) }
            return
        }

        flushMutex.withLock {
            if (targetRole == DepotRole.OWNER && profileStore.hasPendingSync(uid)) {
                try {
                    pushProfileToCloudInternal(
                        firestore = firestoreProvider(),
                        uid = uid,
                        depotId = depotId,
                        profile = profileStore.currentProfile()
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Pending profile flush failed: ${e.message}")
                }
            }

            val allPending = db.pendingSyncDao().getAllPending()
            if (allPending.isEmpty()) {
                _state.update {
                    it.copy(
                        isOnline = true,
                        status = if (collectionListeners.isNotEmpty()) SyncConnectionStatus.LIVE else it.status
                    )
                }
                return
            }

            _state.update { it.copy(status = SyncConnectionStatus.SYNCING, errorMessage = null) }
            val firestore = firestoreProvider()
            val depotRef = firestore.collection("depots").document(depotId)

            val orderPriority = mapOf(
                SyncCollections.PRODUK to 0,
                SyncCollections.KURIR to 1,
                SyncCollections.PELANGGAN to 2,
                SyncCollections.TRANSAKSI to 3,
                SyncCollections.KASBON_LOGS to 4,
                SyncCollections.PENGELUARAN to 5,
                SyncCollections.PERAWATAN_JADWAL to 6,
                SyncCollections.PERAWATAN_LOG to 7,
                SyncCollections.TANGKI_REFILL to 8
            )
            val ordered = allPending.sortedWith(
                compareBy<PendingSyncEntity> { orderPriority[it.collectionName] ?: 99 }
                    .thenBy { it.updatedAt }
            )

            var touchedKurir = false
            var anyWriteSucceeded = false
            var hitTransientError = false
            var lastError: Exception? = null
            var lastPermissionError: Exception? = null
            val skippedPermissionItems = mutableListOf<String>()

            val startedAsActiveDepot = (targetDepotId == activeDepotId)
            val totalToFlush = ordered.size.coerceAtLeast(1)

            for ((idx, item) in ordered.withIndex()) {
                if (activeUid != uid || (startedAsActiveDepot && activeDepotId != depotId)) {
                    return
                }
                val isOwnerOnlyCollection = item.collectionName == SyncCollections.PENGELUARAN ||
                    item.collectionName == SyncCollections.PERAWATAN_JADWAL ||
                    item.collectionName == SyncCollections.PERAWATAN_LOG ||
                    item.collectionName == SyncCollections.TANGKI_REFILL ||
                    item.collectionName == SyncCollections.PRODUK ||
                    item.collectionName == SyncCollections.KURIR
                if (targetRole == DepotRole.STAFF && isOwnerOnlyCollection) {
                    db.pendingSyncDao().remove(item.collectionName, item.syncId)
                    continue
                }

                if (startedAsActiveDepot) {
                    val colLabel = collectionDisplayLabel(item.collectionName)
                    val progress = ((idx + 1).toFloat() / totalToFlush.toFloat()).coerceIn(0.05f, 1.0f)
                    _state.update {
                        it.copy(
                            syncStageLabel = context.getString(
                                R.string.sync_stage_flushing_item,
                                colLabel,
                                idx + 1,
                                totalToFlush
                            ),
                            syncProgressFraction = progress
                        )
                    }
                }

                val itemKey = "${item.collectionName}:${item.syncId}"
                val path = "depots/$depotId/${item.collectionName}/${item.syncId}"
                try {
                    val payload = buildDocumentPayload(item)
                    if (payload == null) {
                        db.pendingSyncDao().remove(item.collectionName, item.syncId)
                        permissionDeniedAttempts.remove(itemKey)
                        continue
                    }

                    withTimeout(20_000L) {
                        withRetry {
                            depotRef.collection(item.collectionName)
                                .document(item.syncId)
                                .set(payload)
                                .await()
                        }
                    }
                    recordWrites(1)
                    db.pendingSyncDao().remove(item.collectionName, item.syncId)
                    permissionDeniedAttempts.remove(itemKey)
                    anyWriteSucceeded = true

                    if (item.collectionName == SyncCollections.KURIR) {
                        touchedKurir = true
                    }
                } catch (e: CancellationException) {
                    _state.update { it.copy(syncStageLabel = null, syncProgressFraction = null) }
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, path)
                    val code = unwrapFirestoreException(e)?.code
                    if (code == FirebaseFirestoreException.Code.INVALID_ARGUMENT ||
                        code == FirebaseFirestoreException.Code.OUT_OF_RANGE ||
                        code == FirebaseFirestoreException.Code.DATA_LOSS
                    ) {
                        // Data cacat permanen tidak boleh menyumbat item antrean valid lainnya.
                        logCrashlytics("Dropped invalid sync payload $path: ${e.message}")
                        db.pendingSyncDao().remove(item.collectionName, item.syncId)
                        permissionDeniedAttempts.remove(itemKey)
                        lastError = e
                        continue
                    } else if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    ) {
                        lastPermissionError = e
                        lastError = e
                        skippedPermissionItems.add(itemKey)
                        // Lanjutkan item berikutnya agar satu dokumen bermasalah tidak memblokir seluruh antrean.
                        continue
                    } else {
                        lastError = e
                        hitTransientError = true
                        break
                    }
                }
            }

            if (anyWriteSucceeded && skippedPermissionItems.isNotEmpty()) {
                // Sebagian item berhasil ditulis -> akses pengguna masih aktif, artinya item yang gagal
                // tersebut melanggar validasi rule atau tidak diizinkan untuk peran ini.
                for (badKey in skippedPermissionItems) {
                    val attempts = (permissionDeniedAttempts[badKey] ?: 0) + 1
                    permissionDeniedAttempts[badKey] = attempts
                    if (attempts >= 3) {
                        val parts = badKey.split(":", limit = 2)
                        if (parts.size == 2) {
                            logCrashlytics("Dropped blocked sync item after $attempts PERMISSION_DENIED attempts: $badKey")
                            db.pendingSyncDao().remove(parts[0], parts[1])
                        }
                        permissionDeniedAttempts.remove(badKey)
                    }
                }
            } else if (!anyWriteSucceeded && lastPermissionError != null && targetRole == DepotRole.STAFF) {
                // Semua write staf ditolak -> periksa apakah akses staf dicabut oleh pemilik.
                scope.launch { recheckStaffConnection() }
            }

            if (touchedKurir && targetRole == DepotRole.OWNER && activeDepotId == depotId) {
                try {
                    withTimeout(10_000L) {
                        ensureDepotAndStaffLinks(firestore, depotId, uid, activeEmail)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, "depots/$depotId")
                }
            }

            if (hitTransientError) {
                val err = lastError ?: IllegalStateException("Sync transient error")
                val online = checkIsOnline()
                _state.update {
                    it.copy(
                        isOnline = online,
                        status = if (!online) SyncConnectionStatus.OFFLINE else SyncConnectionStatus.ERROR,
                        errorMessage = friendlyError(err),
                        syncStageLabel = null,
                        syncProgressFraction = null
                    )
                }
                if (online) {
                    scheduleAutoRetry()
                }
                return
            }

            val blockingError = lastError
            if (!anyWriteSucceeded && blockingError != null) {
                val online = checkIsOnline()
                _state.update {
                    it.copy(
                        isOnline = online,
                        status = if (!online) SyncConnectionStatus.OFFLINE else SyncConnectionStatus.ERROR,
                        errorMessage = friendlyError(blockingError),
                        syncStageLabel = null,
                        syncProgressFraction = null
                    )
                }
                if (online && skippedPermissionItems.isNotEmpty()) {
                    scheduleAutoRetry()
                }
                return
            }

            autoRetryCount = 0
            val remaining = db.pendingSyncDao().getQueueSize()
            _state.update {
                it.copy(
                    isOnline = true,
                    pendingCount = remaining,
                    status = when {
                        isTerminalBlockingStatus(it.status) -> it.status
                        remaining == 0 -> SyncConnectionStatus.LIVE
                        else -> SyncConnectionStatus.SYNCING
                    },
                    lastSyncedAt = System.currentTimeMillis(),
                    errorMessage = lastError?.let { err -> friendlyError(err) },
                    syncStageLabel = null,
                    syncProgressFraction = null,
                    autoRetryAttempt = 0,
                    isAutoRetryScheduled = false
                )
            }
        }
    }

    private suspend fun buildDocumentPayload(item: PendingSyncEntity): Map<String, Any?>? {
        val syncId = item.syncId
        val isDelete = item.operation == SyncOperations.DELETE
        return when (item.collectionName) {
            SyncCollections.PRODUK -> {
                if (isDelete) {
                    mapOf(
                        "syncId" to syncId,
                        "nama_produk" to "(Dihapus)",
                        "harga" to 0.0,
                        "updatedAt" to item.updatedAt,
                        "isDeleted" to true,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    val p = db.produkDao().getBySyncId(syncId) ?: return null
                    mapOf(
                        "syncId" to p.syncId,
                        "nama_produk" to p.nama_produk.trim().take(120).ifBlank { "Produk" },
                        "harga" to p.harga.coerceAtLeast(0.0),
                        "updatedAt" to p.updatedAt,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                }
            }
            SyncCollections.KURIR -> {
                if (isDelete) {
                    mapOf(
                        "syncId" to syncId,
                        "nama" to "(Dihapus)",
                        "komisi_per_galon" to 0.0,
                        "email" to "",
                        "akses_aktif" to false,
                        "skema_komisi" to "TETAP",
                        "aturan_komisi" to "",
                        "updatedAt" to item.updatedAt,
                        "isDeleted" to true,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    val k = db.kurirDao().getBySyncId(syncId) ?: return null
                    mapOf(
                        "syncId" to k.syncId,
                        "nama" to k.nama.trim().take(120).ifBlank { "Karyawan" },
                        "komisi_per_galon" to k.komisi_per_galon.coerceAtLeast(0.0),
                        "email" to k.email.trim().lowercase().take(160),
                        "akses_aktif" to k.akses_aktif,
                        "skema_komisi" to k.skema_komisi.take(40),
                        "aturan_komisi" to k.aturan_komisi.take(200),
                        "updatedAt" to k.updatedAt,
                        "isDeleted" to k.isDeleted,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                }
            }
            SyncCollections.PELANGGAN -> {
                if (isDelete) {
                    mapOf(
                        "syncId" to syncId,
                        "nama" to "(Dihapus)",
                        "alamat" to "",
                        "no_hp" to "",
                        "sisa_kasbon" to 0.0,
                        "harga_khusus" to null,
                        "kurirSyncId" to null,
                        "updatedAt" to item.updatedAt,
                        "isDeleted" to true,
                        "_hardDeleted" to true,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    val p = db.pelangganDao().getBySyncId(syncId) ?: return null
                    if (p.isDeleted && p.updatedAt == 0L && p.nama == "Pelanggan Dihapus") {
                        return null
                    }
                    val kurirSyncId = p.id_kurir_langganan?.let { db.kurirDao().getById(it)?.syncId }?.take(128)
                    mapOf(
                        "syncId" to p.syncId,
                        "nama" to p.nama.trim().take(120).ifBlank { "Pelanggan" },
                        "alamat" to p.alamat.trim().take(300),
                        "no_hp" to p.no_hp.trim().take(40),
                        "sisa_kasbon" to p.sisa_kasbon.coerceAtLeast(0.0),
                        "harga_khusus" to p.harga_khusus?.takeIf { it >= 0.0 },
                        "kurirSyncId" to kurirSyncId,
                        "updatedAt" to p.updatedAt,
                        "isDeleted" to p.isDeleted,
                        "deletedBy" to p.deletedBy?.trim()?.take(120),
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                }
            }
            SyncCollections.TRANSAKSI -> {
                if (isDelete) {
                    mapOf(
                        "syncId" to syncId,
                        "tanggal" to 0L,
                        "pelangganSyncId" to "deleted",
                        "kurirSyncId" to null,
                        "produkSyncId" to null,
                        "jumlah" to 1,
                        "harga_satuan" to 0.0,
                        "total_harga" to 0.0,
                        "total_komisi" to 0.0,
                        "metode_bayar" to "Cash",
                        "status" to "Lunas",
                        "uangMuka" to 0L,
                        "sisaKasbon" to 0L,
                        "updatedAt" to item.updatedAt,
                        "isDeleted" to true,
                        "_hardDeleted" to true,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    val tx = db.transaksiDao().getBySyncId(syncId) ?: return null
                    val pelangganSyncId = db.pelangganDao().getById(tx.id_pelanggan)?.syncId
                        ?.takeIf { it.isNotBlank() }?.take(128)
                        ?: return null
                    val kurirSyncId = tx.id_kurir?.let { db.kurirDao().getById(it)?.syncId }?.take(128)
                    val produkSyncId = tx.id_produk?.let { db.produkDao().getById(it)?.syncId }?.take(128)
                    val safeJumlah = tx.jumlah.coerceAtLeast(1)
                    val safeHarga = tx.harga_satuan.coerceAtLeast(0.0)
                    val safeTotalHarga = if (kotlin.math.abs(tx.total_harga - (safeJumlah * safeHarga)) <= 0.01) {
                        tx.total_harga.coerceAtLeast(0.0)
                    } else {
                        safeJumlah * safeHarga
                    }
                    val safeStatus = when {
                        tx.status.equals("Kasbon", ignoreCase = true) -> "Kasbon"
                        tx.status.equals("Lunas", ignoreCase = true) -> "Lunas"
                        tx.status.equals(KaryawanScopeHelper.STATUS_DIHAPUS, ignoreCase = true) || tx.isDeleted ->
                            KaryawanScopeHelper.STATUS_DIHAPUS
                        else -> tx.status.trim().take(40).ifBlank { "Lunas" }
                    }
                    val safeUangMuka = tx.uangMuka.coerceIn(0L, safeTotalHarga.toLong().coerceAtLeast(0L))
                    val safeSisaKasbon: Number = when (safeStatus) {
                        "Kasbon" -> {
                            val rawSisa = (safeTotalHarga - safeUangMuka.toDouble()).coerceAtLeast(0.0)
                            if (kotlin.math.abs(rawSisa - rawSisa.toLong()) > 0.001) rawSisa else rawSisa.toLong()
                        }
                        "Lunas" -> 0L
                        else -> tx.sisaKasbon.coerceAtLeast(0L)
                    }
                    mapOf(
                        "syncId" to tx.syncId,
                        "tanggal" to tx.tanggal.coerceAtLeast(0L),
                        "pelangganSyncId" to pelangganSyncId,
                        "kurirSyncId" to kurirSyncId,
                        "produkSyncId" to produkSyncId,
                        "jumlah" to safeJumlah,
                        "harga_satuan" to safeHarga,
                        "total_harga" to safeTotalHarga,
                        "total_komisi" to tx.total_komisi.coerceAtLeast(0.0),
                        "metode_bayar" to tx.metode_bayar.take(40),
                        "status" to safeStatus,
                        "uangMuka" to safeUangMuka,
                        "sisaKasbon" to safeSisaKasbon,
                        "tanggalBayar" to tx.tanggalBayar?.coerceAtLeast(0L),
                        "waterType" to (if (tx.waterType == Transaksi.WATER_TYPE_MINERAL) Transaksi.WATER_TYPE_MINERAL else Transaksi.WATER_TYPE_RO),
                        "updatedAt" to tx.updatedAt.coerceAtLeast(0L),
                        "isDeleted" to (tx.isDeleted || safeStatus == KaryawanScopeHelper.STATUS_DIHAPUS),
                        "deletedBy" to tx.deletedBy?.trim()?.take(120),
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                }
            }
            SyncCollections.PENGELUARAN -> {
                if (isDelete) {
                    mapOf(
                        "syncId" to syncId,
                        "tanggal" to 0L,
                        "nama_item" to "(Dihapus)",
                        "kategori" to "Operasional",
                        "nominal" to 0.0,
                        "updatedAt" to item.updatedAt,
                        "isDeleted" to true,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    val e = db.pengeluaranDao().getBySyncId(syncId) ?: return null
                    mapOf(
                        "syncId" to e.syncId,
                        "tanggal" to e.tanggal.coerceAtLeast(0L),
                        "nama_item" to e.nama_item.trim().take(200).ifBlank { "Pengeluaran" },
                        "kategori" to e.kategori.take(80),
                        "nominal" to e.nominal.coerceAtLeast(0.0),
                        "updatedAt" to e.updatedAt,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                }
            }
            SyncCollections.KASBON_LOGS -> {
                if (isDelete) {
                    mapOf(
                        "syncId" to syncId,
                        "pelangganSyncId" to "deleted",
                        "tanggal" to 0L,
                        "jumlah_bayar" to 0.0,
                        "sisa_kasbon_sebelum" to 0.0,
                        "sisa_kasbon_sesudah" to 0.0,
                        "metode_bayar" to "Cash",
                        "catatan" to "Dihapus",
                        "updatedAt" to item.updatedAt,
                        "isDeleted" to true,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    val log = db.kasbonLogDao().getBySyncId(syncId) ?: return null
                    val expectedSesudah = log.sisa_kasbon_sebelum - log.jumlah_bayar
                    if (log.jumlah_bayar < 0.0 ||
                        log.sisa_kasbon_sebelum < 0.0 ||
                        log.sisa_kasbon_sesudah < 0.0 ||
                        kotlin.math.abs(log.sisa_kasbon_sesudah - expectedSesudah) > 0.01
                    ) {
                        return null
                    }
                    val pelangganSyncId = db.pelangganDao().getById(log.id_pelanggan)?.syncId
                        ?.takeIf { it.isNotBlank() }?.take(128)
                        ?: return null
                    mapOf(
                        "syncId" to log.syncId,
                        "pelangganSyncId" to pelangganSyncId,
                        "tanggal" to log.tanggal.coerceAtLeast(0L),
                        "jumlah_bayar" to log.jumlah_bayar,
                        "sisa_kasbon_sebelum" to log.sisa_kasbon_sebelum,
                        "sisa_kasbon_sesudah" to log.sisa_kasbon_sesudah,
                        "metode_bayar" to log.metode_bayar.take(40),
                        "catatan" to log.catatan.take(300),
                        "updatedAt" to log.updatedAt.coerceAtLeast(0L),
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                }
            }
            SyncCollections.PERAWATAN_JADWAL -> {
                if (isDelete) {
                    mapOf(
                        "syncId" to syncId,
                        "nama" to "(Dihapus)",
                        "intervalHari" to 30,
                        "terakhirSelesai" to null,
                        "jadwalBerikutnya" to 0L,
                        "updatedAt" to item.updatedAt,
                        "isDeleted" to true,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    val itemPerawatan = db.perawatanDao().getBySyncId(syncId) ?: return null
                    mapOf(
                        "syncId" to itemPerawatan.syncId,
                        "nama" to itemPerawatan.nama.trim().take(120).ifBlank { "Perawatan RO" },
                        "intervalHari" to itemPerawatan.intervalHari.coerceIn(1, 3650),
                        "terakhirSelesai" to itemPerawatan.terakhirSelesai?.coerceAtLeast(0L),
                        "jadwalBerikutnya" to itemPerawatan.jadwalBerikutnya.coerceAtLeast(0L),
                        "updatedAt" to itemPerawatan.updatedAt,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                }
            }
            SyncCollections.PERAWATAN_LOG -> {
                if (isDelete) {
                    mapOf(
                        "syncId" to syncId,
                        "perawatanSyncId" to "deleted",
                        "namaPerawatan" to "(Dihapus)",
                        "tanggalSelesai" to 0L,
                        "biaya" to 0.0,
                        "catatan" to "",
                        "pengeluaranSyncId" to null,
                        "updatedAt" to item.updatedAt,
                        "isDeleted" to true,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    val log = db.perawatanLogDao().getBySyncId(syncId) ?: return null
                    mapOf(
                        "syncId" to log.syncId,
                        "perawatanSyncId" to log.perawatanSyncId.take(128).ifBlank { "unknown" },
                        "namaPerawatan" to log.namaPerawatan.trim().take(120).ifBlank { "Perawatan RO" },
                        "tanggalSelesai" to log.tanggalSelesai.coerceAtLeast(0L),
                        "biaya" to log.biaya.coerceAtLeast(0.0),
                        "catatan" to log.catatan.trim().take(500),
                        "pengeluaranSyncId" to log.pengeluaranSyncId?.take(128),
                        "updatedAt" to log.updatedAt,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                }
            }
            SyncCollections.TANGKI_REFILL -> {
                if (isDelete) {
                    mapOf(
                        "syncId" to syncId,
                        "tanggal" to 0L,
                        "volumeLiter" to 0.0,
                        "totalHarga" to 0.0,
                        "namaPemasok" to "",
                        "catatan" to "",
                        "linkedExpenseId" to null,
                        "pengeluaranSyncId" to null,
                        "updatedAt" to item.updatedAt,
                        "isDeleted" to true,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    val r = db.tangkiRefillDao().getBySyncId(syncId) ?: return null
                    mapOf(
                        "syncId" to r.syncId,
                        "tanggal" to r.tanggal.coerceAtLeast(0L),
                        "volumeLiter" to r.volumeLiter.coerceAtLeast(0.0),
                        "totalHarga" to r.totalHarga.coerceAtLeast(0.0),
                        "namaPemasok" to r.namaPemasok.trim().take(120),
                        "catatan" to r.catatan.trim().take(500),
                        "linkedExpenseId" to r.linkedExpenseId,
                        "pengeluaranSyncId" to r.pengeluaranSyncId?.take(128),
                        "updatedAt" to r.updatedAt,
                        "isDeleted" to r.isDeleted,
                        "deletedBy" to r.deletedBy?.trim()?.take(120),
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                }
            }
            else -> null
        }
    }

    /**
     * Memastikan ketiga email karyawan resmi terdaftar otomatis di tabel `kurir` lokal dan antrean sinkronisasi
     * ketika pemilik utama (`ferryeffendy@gmail.com`) aktif, tanpa menimpa nama atau komisi yang sudah diubah pemilik.
     */
    internal suspend fun ensureDesignatedStaffKurirsForOwner(ownerEmail: String) {
        if (!KaryawanScopeHelper.isPrimaryOwnerEmail(ownerEmail)) return
        val existingKurirs = db.kurirDao().getAllOnce()
        val seedTimestamp = 1_000L

        for (staffEmail in KaryawanScopeHelper.DESIGNATED_STAFF_EMAILS) {
            val normalizedTarget = KaryawanScopeHelper.normalizeEmail(staffEmail)
            if (normalizedTarget.isBlank()) continue

            val matchedByEmail = existingKurirs.firstOrNull {
                !it.isDeleted && KaryawanScopeHelper.normalizeEmail(it.email) == normalizedTarget
            }
            if (matchedByEmail != null) {
                continue
            }

            val defaultSyncId = KaryawanScopeHelper.defaultStaffSyncId(normalizedTarget)
            val existingBySyncId = db.kurirDao().getBySyncId(defaultSyncId)
            val softDeletedByEmail = existingKurirs.firstOrNull {
                it.isDeleted && KaryawanScopeHelper.normalizeEmail(it.email) == normalizedTarget
            }
            val targetExisting = existingBySyncId ?: softDeletedByEmail

            if (targetExisting == null) {
                val newKurir = Kurir(
                    nama = KaryawanScopeHelper.defaultStaffDisplayName(normalizedTarget),
                    komisi_per_galon = KaryawanScopeHelper.DEFAULT_STAFF_COMMISSION_PER_GALON,
                    email = normalizedTarget,
                    password = "",
                    akses_aktif = true,
                    skema_komisi = "TETAP",
                    aturan_komisi = "",
                    updatedAt = seedTimestamp,
                    syncId = defaultSyncId,
                    isDeleted = false
                )
                db.kurirDao().insert(newKurir)
                db.pendingSyncDao().enqueue(
                    PendingSyncEntity(
                        collectionName = SyncCollections.KURIR,
                        syncId = defaultSyncId,
                        operation = SyncOperations.UPSERT,
                        updatedAt = seedTimestamp
                    )
                )
            } else if (targetExisting.isDeleted) {
                val restoredTimestamp = System.currentTimeMillis()
                db.kurirDao().update(
                    targetExisting.copy(
                        email = normalizedTarget,
                        akses_aktif = true,
                        isDeleted = false,
                        updatedAt = restoredTimestamp
                    )
                )
                db.pendingSyncDao().enqueue(
                    PendingSyncEntity(
                        collectionName = SyncCollections.KURIR,
                        syncId = targetExisting.syncId,
                        operation = SyncOperations.UPSERT,
                        updatedAt = restoredTimestamp
                    )
                )
            }
        }
    }

    private suspend fun ensureDepotAndStaffLinks(
        firestore: FirebaseFirestore,
        depotId: String,
        ownerUid: String,
        ownerEmail: String
    ) = staffLinkSyncMutex.withLock {
        ensureDesignatedStaffKurirsForOwner(ownerEmail)
        val rawKurirs = db.kurirDao().getAllOnce().filter { !it.isDeleted }
        val validKurirs = rawKurirs.mapNotNull { k ->
            val em = KaryawanScopeHelper.normalizeEmail(k.email)
            if (em.isNotBlank() && em != ownerEmail && isValidEmailKey(em)) {
                if (k.email != em) k.copy(email = em) else k
            } else {
                null
            }
        }
        val activeMemberEmails = validKurirs
            .filter { it.akses_aktif }
            .map { it.email }
            .distinct()
            .take(30)

        val depotPath = "depots/$depotId"
        val depotRef = firestore.document(depotPath)
        val existingDepot = try {
            recordReads(1)
            depotRef.get().await()
        } catch (_: Exception) {
            null
        }
        val currentProfile = profileStore.currentProfile()
        if (existingDepot == null || !existingDepot.exists()) {
            val createPayload = mapOf(
                "depotId" to depotId,
                "ownerUid" to ownerUid,
                "ownerEmail" to ownerEmail.take(160),
                "name" to currentProfile.effectiveDepotName().take(100),
                "depotName" to currentProfile.depotName.take(100),
                "ownerName" to currentProfile.effectiveOwnerName().take(120),
                "phoneNumber" to currentProfile.phoneNumber.take(40),
                "address" to currentProfile.address.take(300),
                "receiptFooter" to currentProfile.receiptFooter.take(200),
                "subscriptionTier" to currentProfile.subscriptionTier,
                "memberEmails" to activeMemberEmails,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            withRetry { depotRef.set(createPayload).await() }
            recordWrites(1)
        } else {
            val currentMembers = (existingDepot.get("memberEmails") as? List<*>)
                ?.mapNotNull { it as? String }
                .orEmpty()
            val currentOwnerEmail = existingDepot.getString("ownerEmail").orEmpty()
            val missingProfileFields = existingDepot.getString("depotName") == null
            if (currentMembers != activeMemberEmails || currentOwnerEmail != ownerEmail || missingProfileFields) {
                val mergePayload = mutableMapOf<String, Any?>(
                    "depotId" to depotId,
                    "ownerUid" to ownerUid,
                    "ownerEmail" to ownerEmail.take(160),
                    "memberEmails" to activeMemberEmails,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (existingDepot.getTimestamp("createdAt") == null) {
                    mergePayload["createdAt"] = FieldValue.serverTimestamp()
                }
                if (missingProfileFields) {
                    mergePayload["name"] = currentProfile.effectiveDepotName().take(100)
                    mergePayload["depotName"] = currentProfile.depotName.take(100)
                    mergePayload["ownerName"] = currentProfile.effectiveOwnerName().take(120)
                    mergePayload["phoneNumber"] = currentProfile.phoneNumber.take(40)
                    mergePayload["address"] = currentProfile.address.take(300)
                    mergePayload["receiptFooter"] = currentProfile.receiptFooter.take(200)
                    mergePayload["subscriptionTier"] = currentProfile.subscriptionTier
                }
                withRetry {
                    depotRef.set(mergePayload, SetOptions.merge()).await()
                }
                recordWrites(1)
            }
        }

        // Sync /staff_links/{emailKey} non-fatally so depot sync always succeeds
        try {
            val existingLinksSnap = firestore.collection("staff_links")
                .whereEqualTo("ownerUid", ownerUid)
                .get()
                .await()
            recordReads(existingLinksSnap.documents.size.coerceAtLeast(1))
            val existingByEmail = existingLinksSnap.documents.associateBy { it.id }
            val desiredByEmail = validKurirs.associateBy { it.email.trim().lowercase() }

            for ((emailKey, doc) in existingByEmail) {
                if (!desiredByEmail.containsKey(emailKey)) {
                    try {
                        withRetry { doc.reference.delete().await() }
                        recordWrites(1)
                    } catch (e: Exception) {
                        Log.w(TAG, "Skip deleting staff_link $emailKey: ${e.message}")
                    }
                }
            }

            for ((emailKey, kurir) in desiredByEmail) {
                val existing = existingByEmail[emailKey]
                val linkRef = firestore.collection("staff_links").document(emailKey)
                try {
                    if (existing == null) {
                        if (kurir.akses_aktif) {
                            withRetry {
                                linkRef.set(
                                    mapOf(
                                        "depotId" to depotId,
                                        "ownerUid" to ownerUid,
                                        "ownerEmail" to ownerEmail.take(160),
                                        "staffEmail" to emailKey,
                                        "active" to true,
                                        "createdAt" to FieldValue.serverTimestamp(),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                ).await()
                            }
                            recordWrites(1)
                        }
                    } else {
                        val curActive = existing.getBoolean("active") ?: true
                        val curOwnerEmail = existing.getString("ownerEmail").orEmpty()
                        val curDepotId = existing.getString("depotId").orEmpty()
                        val curOwnerUid = existing.getString("ownerUid").orEmpty()
                        if (curActive != kurir.akses_aktif ||
                            curOwnerEmail != ownerEmail ||
                            curDepotId != depotId ||
                            curOwnerUid != ownerUid
                        ) {
                            val linkCreatedAt = existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
                            withRetry {
                                linkRef.set(
                                    mapOf(
                                        "depotId" to depotId,
                                        "ownerUid" to ownerUid,
                                        "ownerEmail" to ownerEmail.take(160),
                                        "staffEmail" to emailKey,
                                        "active" to kurir.akses_aktif,
                                        "createdAt" to linkCreatedAt,
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                ).await()
                            }
                            recordWrites(1)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Skip updating staff_link $emailKey: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Staff links query/sync deferred: ${e.message}")
        }
    }

    /**
     * Mencatat atau memperbarui dokumen pendaftar di `users/{uid}` dengan struktur datar untuk monitoring.
     * Menjaga `registeredAt` dan `subscriptionTier` agar tidak tertimpa saat pembaruan sesi berikutnya.
     */
    private suspend fun touchUserProfileDocument(
        uid: String,
        email: String,
        googleDisplayName: String,
        appVersion: String
    ) {
        if (uid.isBlank() || activeUid != uid || !checkIsOnline()) return
        try {
            val firestore = firestoreProvider()
            val userRef = firestore.collection("users").document(uid)
            val existing = withTimeout(8_000L) {
                recordReads(1)
                userRef.get().await()
            }
            if (activeUid != uid) return

            val local = profileStore.currentProfile()
            val safeEmail = email.trim().lowercase().take(160)
            val safeGoogleName = googleDisplayName.trim().take(120)
            val defaultRole = if (KaryawanScopeHelper.isDesignatedStaffEmail(safeEmail)) {
                DepotProfile.ROLE_STAFF
            } else {
                local.role
            }

            if (!existing.exists()) {
                val initialOwnerName = local.ownerName.ifBlank { safeGoogleName }.take(120)
                val createPayload = mapOf(
                    "uid" to uid,
                    "email" to safeEmail,
                    "googleDisplayName" to safeGoogleName,
                    "ownerName" to initialOwnerName,
                    "depotName" to local.depotName.trim().take(100),
                    "phoneNumber" to local.phoneNumber.trim().take(40),
                    "address" to local.address.trim().take(300),
                    "receiptFooter" to local.receiptFooter.trim().take(200),
                    "role" to defaultRole,
                    "subscriptionTier" to DepotProfile.TIER_FREE,
                    "registeredAt" to FieldValue.serverTimestamp(),
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "appVersion" to appVersion.take(40)
                )
                withTimeout(10_000L) {
                    withRetry { userRef.set(createPayload, SetOptions.merge()).await() }
                }
                recordWrites(1)
            } else {
                val mergePayload = mutableMapOf<String, Any?>(
                    "uid" to uid,
                    "email" to safeEmail,
                    "googleDisplayName" to safeGoogleName,
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "appVersion" to appVersion.take(40)
                )
                val existingOwner = existing.getString("ownerName").orEmpty().trim()
                if (existingOwner.isBlank()) {
                    val fallbackOwner = local.ownerName.ifBlank { safeGoogleName }.take(120)
                    mergePayload["ownerName"] = fallbackOwner
                }
                if (existing.get("depotName") == null) mergePayload["depotName"] = local.depotName.trim().take(100)
                if (existing.get("phoneNumber") == null) mergePayload["phoneNumber"] = local.phoneNumber.trim().take(40)
                if (existing.get("address") == null) mergePayload["address"] = local.address.trim().take(300)
                if (existing.get("receiptFooter") == null) mergePayload["receiptFooter"] = local.receiptFooter.trim().take(200)
                if (existing.get("role") == null) mergePayload["role"] = defaultRole
                if (existing.getTimestamp("registeredAt") == null) {
                    mergePayload["registeredAt"] = FieldValue.serverTimestamp()
                }
                if (existing.get("subscriptionTier") == null) {
                    mergePayload["subscriptionTier"] = DepotProfile.TIER_FREE
                }
                withTimeout(10_000L) {
                    withRetry { userRef.set(mergePayload, SetOptions.merge()).await() }
                }
                recordWrites(1)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Deferred users/$uid touch: ${e.message}")
        }
    }

    private fun attachUserProfileListener(uid: String) {
        userProfileListener?.remove()
        val firestore = firestoreProvider()
        userProfileListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "users/$uid")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists() && activeUid == uid) {
                    val data = snapshot.data ?: return@addSnapshotListener
                    val updated = profileStore.applyRemoteUserProfile(uid, data)
                    if (activeRole == DepotRole.OWNER &&
                        activeDepotId.isNotBlank() &&
                        !snapshot.metadata.hasPendingWrites() &&
                        lastDepotMirroredTier != null &&
                        lastDepotMirroredTier != updated.subscriptionTier &&
                        checkIsOnline()
                    ) {
                        val targetDepot = activeDepotId
                        profileSyncJob?.cancel()
                        profileSyncJob = scope.launch {
                            runCatching {
                                mirrorTierToDepot(firestore, targetDepot, updated.subscriptionTier)
                            }
                        }
                    }
                }
            }
    }

    private suspend fun mirrorTierToDepot(
        firestore: FirebaseFirestore,
        depotId: String,
        tier: String
    ) {
        val depotRef = firestore.collection("depots").document(depotId)
        val snap = withTimeout(5_000L) { depotRef.get().await() }
        if (!snap.exists()) return
        withTimeout(8_000L) {
            withRetry {
                depotRef.set(
                    mapOf(
                        "subscriptionTier" to tier,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }
        }
        lastDepotMirroredTier = tier
        recordWrites(1)
    }

    private fun syncResolvedUserRoleInBackground(
        firestore: FirebaseFirestore,
        uid: String,
        role: DepotRole
    ) {
        if (!checkIsOnline() || uid.isBlank()) return
        val roleStr = if (role == DepotRole.STAFF) DepotProfile.ROLE_STAFF else DepotProfile.ROLE_OWNER
        scope.launch {
            try {
                val userRef = firestore.collection("users").document(uid)
                val snap = withTimeout(6_000L) { userRef.get().await() }
                if (activeUid != uid || !snap.exists()) return@launch
                if (snap.getString("role") != roleStr) {
                    withTimeout(8_000L) {
                        withRetry {
                            userRef.set(
                                mapOf(
                                    "role" to roleStr,
                                    "lastActiveAt" to FieldValue.serverTimestamp()
                                ),
                                SetOptions.merge()
                            ).await()
                        }
                    }
                    recordWrites(1)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Role sync on users/$uid deferred: ${e.message}")
            }
        }
    }

    private fun attachDepotDocListener(
        firestore: FirebaseFirestore,
        depotId: String,
        uid: String,
        role: DepotRole
    ) {
        depotDocListener?.remove()
        val roleStr = if (role == DepotRole.STAFF) DepotProfile.ROLE_STAFF else DepotProfile.ROLE_OWNER
        depotDocListener = firestore.collection("depots").document(depotId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "depots/$depotId")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists() && activeUid == uid && activeDepotId == depotId) {
                    val data = snapshot.data ?: return@addSnapshotListener
                    lastDepotMirroredTier = (data["subscriptionTier"] as? String)
                        ?.trim()
                        ?.uppercase()
                        ?.let { if (it == DepotProfile.TIER_PRO) DepotProfile.TIER_PRO else DepotProfile.TIER_FREE }
                    profileStore.applyRemoteDepotProfile(uid, roleStr, data)
                }
            }
    }

    private suspend fun pushProfileToCloudInternal(
        firestore: FirebaseFirestore,
        uid: String,
        depotId: String,
        profile: DepotProfile
    ) = profileSyncMutex.withLock {
        if (uid.isBlank() || activeUid != uid || !checkIsOnline()) return@withLock

        val userRef = firestore.collection("users").document(uid)
        val userSnap = withTimeout(8_000L) {
            recordReads(1)
            userRef.get().await()
        }
        val cleanOwner = profile.effectiveOwnerName().take(120)
        val cleanDepot = profile.depotName.trim().take(100)
        val cleanPhone = profile.phoneNumber.trim().take(40)
        val cleanAddress = profile.address.trim().take(300)
        val cleanFooter = profile.receiptFooter.trim().take(200)
        val roleStr = if (activeRole == DepotRole.STAFF) DepotProfile.ROLE_STAFF else DepotProfile.ROLE_OWNER

        if (!userSnap.exists()) {
            val createPayload = mapOf(
                "uid" to uid,
                "email" to activeEmail.take(160),
                "googleDisplayName" to activeDisplayName.take(120),
                "ownerName" to cleanOwner,
                "depotName" to cleanDepot,
                "phoneNumber" to cleanPhone,
                "address" to cleanAddress,
                "receiptFooter" to cleanFooter,
                "role" to roleStr,
                "subscriptionTier" to DepotProfile.TIER_FREE,
                "depotType" to profile.depotType,
                "tankCapacityLiters" to profile.tankCapacityLiters,
                "currentTankStockLiters" to profile.currentTankStockLiters,
                "registeredAt" to FieldValue.serverTimestamp(),
                "lastActiveAt" to FieldValue.serverTimestamp(),
                "appVersion" to BuildConfig.VERSION_NAME.take(40)
            )
            withTimeout(10_000L) {
                withRetry { userRef.set(createPayload, SetOptions.merge()).await() }
            }
            recordWrites(1)
        } else {
            val updatePayload = mutableMapOf<String, Any?>(
                "ownerName" to cleanOwner,
                "depotName" to cleanDepot,
                "phoneNumber" to cleanPhone,
                "address" to cleanAddress,
                "receiptFooter" to cleanFooter,
                "role" to roleStr,
                "depotType" to profile.depotType,
                "tankCapacityLiters" to profile.tankCapacityLiters,
                "currentTankStockLiters" to profile.currentTankStockLiters,
                "lastActiveAt" to FieldValue.serverTimestamp(),
                "appVersion" to BuildConfig.VERSION_NAME.take(40)
            )
            if (userSnap.getTimestamp("registeredAt") == null) {
                updatePayload["registeredAt"] = FieldValue.serverTimestamp()
            }
            if (userSnap.get("subscriptionTier") == null) {
                updatePayload["subscriptionTier"] = DepotProfile.TIER_FREE
            }
            withTimeout(10_000L) {
                withRetry { userRef.set(updatePayload, SetOptions.merge()).await() }
            }
            recordWrites(1)
        }

        if (activeRole == DepotRole.OWNER && depotId.isNotBlank()) {
            val depotRef = firestore.collection("depots").document(depotId)
            val depotSnap = withTimeout(8_000L) {
                recordReads(1)
                depotRef.get().await()
            }
            if (!depotSnap.exists()) {
                ensureDepotAndStaffLinks(firestore, depotId, uid, activeEmail)
            } else {
                val effectiveTier = (userSnap.getString("subscriptionTier") ?: profile.subscriptionTier)
                    .trim()
                    .uppercase()
                    .let { if (it == DepotProfile.TIER_PRO) DepotProfile.TIER_PRO else DepotProfile.TIER_FREE }
                val depotPayload = mutableMapOf<String, Any?>(
                    "name" to profile.effectiveDepotName().take(100),
                    "depotName" to cleanDepot,
                    "ownerName" to cleanOwner,
                    "phoneNumber" to cleanPhone,
                    "address" to cleanAddress,
                    "receiptFooter" to cleanFooter,
                    "subscriptionTier" to effectiveTier,
                    "depotType" to profile.depotType,
                    "tankCapacityLiters" to profile.tankCapacityLiters,
                    "currentTankStockLiters" to profile.currentTankStockLiters,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (depotSnap.getTimestamp("createdAt") == null) {
                    depotPayload["createdAt"] = FieldValue.serverTimestamp()
                }
                withTimeout(10_000L) {
                    withRetry { depotRef.set(depotPayload, SetOptions.merge()).await() }
                }
                lastDepotMirroredTier = effectiveTier
                recordWrites(1)
            }
        }
        profileStore.markPendingSync(uid, false)
    }

    /**
     * Menyimpan perubahan profil pemilik & depot secara offline-first lalu menyinkronkannya ke Cloud
     * (`users/{uid}` dan `depots/{depotId}`) agar perangkat karyawan otomatis memakai identitas terbaru.
     */
    suspend fun saveDepotProfile(
        ownerName: String,
        depotName: String,
        phoneNumber: String,
        address: String,
        receiptFooter: String,
        depotType: String? = null,
        tankCapacityLiters: Double? = null
    ): Result<DepotProfile> {
        val uid = activeUid ?: return Result.failure(IllegalStateException("Belum masuk akun Google"))
        if (activeRole != DepotRole.OWNER) {
            return Result.failure(IllegalStateException("Hanya pemilik depot yang dapat mengubah profil"))
        }
        val current = profileStore.currentProfile()
        val saved = profileStore.saveOwnerEdit(
            uid = uid,
            ownerName = ownerName,
            depotName = depotName,
            phoneNumber = phoneNumber,
            address = address,
            receiptFooter = receiptFooter,
            depotType = depotType ?: current.depotType,
            tankCapacityLiters = tankCapacityLiters ?: current.tankCapacityLiters
        )
        val targetDepotId = activeDepotId.ifBlank { "depot_$uid" }
        if (!isOfflineLocalSession() && checkIsOnline()) {
            try {
                withTimeout(10_000L) {
                    pushProfileToCloudInternal(
                        firestore = firestoreProvider(),
                        uid = uid,
                        depotId = targetDepotId,
                        profile = saved
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Profile cloud sync deferred (saved offline): ${e.message}")
            }
        }
        return Result.success(profileStore.currentProfile())
    }

    /**
     * Menyesuaikan stok air toren di Firestore secara atomik menggunakan FieldValue.increment
     * agar perubahan stok multi-perangkat (kurir menjual galon mineral) tidak saling menimpa.
     */
    suspend fun adjustTankStockCloud(deltaLiters: Double) {
        if (isOfflineLocalSession()) return
        val depotId = activeDepotId
        if (depotId.isBlank() || !checkIsOnline()) return
        try {
            val firestore = firestoreProvider()
            val updates = mapOf(
                "currentTankStockLiters" to FieldValue.increment(deltaLiters),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("depots").document(depotId).update(updates).await()
            recordWrites(1)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal update stok toren di cloud: ${e.message}")
        }
    }

    /**
     * Mengatur ulang stok dan kapasitas toren di Firestore (kuras toren atau penyesuaian stok manual).
     */
    suspend fun setTankStockCloud(newStock: Double, newCapacity: Double? = null) {
        if (isOfflineLocalSession()) return
        val depotId = activeDepotId
        if (depotId.isBlank() || !checkIsOnline()) return
        try {
            val firestore = firestoreProvider()
            val updates = mutableMapOf<String, Any>(
                "currentTankStockLiters" to newStock.coerceAtLeast(0.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newCapacity != null) {
                updates["tankCapacityLiters"] = newCapacity.coerceAtLeast(100.0)
            }
            firestore.collection("depots").document(depotId).update(updates).await()
            recordWrites(1)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal set stok toren di cloud: ${e.message}")
        }
    }

    private fun registerNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                autoRetryCount = 0
                if (isOfflineLocalSession()) {
                    _state.update {
                        it.copy(
                            isOnline = true,
                            status = SyncConnectionStatus.OFFLINE
                        )
                    }
                    return
                }
                _state.update {
                    it.copy(
                        isOnline = true,
                        status = if (isTerminalBlockingStatus(it.status)) {
                            it.status
                        } else if (it.pendingCount > 0) {
                            SyncConnectionStatus.SYNCING
                        } else {
                            SyncConnectionStatus.LIVE
                        }
                    )
                }
                if (failedCollections.isNotEmpty() && activeUid != null && activeDepotId.isNotBlank()) {
                    scheduleAutoRetry()
                } else {
                    scheduleFlush(delayMs = 150L)
                }
                val uidNow = activeUid
                if (uidNow != null && activeRole == DepotRole.OWNER && activeDepotId.isNotBlank() && profileStore.hasPendingSync(uidNow)) {
                    profileSyncJob?.cancel()
                    profileSyncJob = scope.launch {
                        runCatching {
                            pushProfileToCloudInternal(
                                firestore = firestoreProvider(),
                                uid = uidNow,
                                depotId = activeDepotId,
                                profile = profileStore.currentProfile()
                            )
                        }
                    }
                }
            }

            override fun onLost(network: Network) {
                val stillOnline = checkIsOnline()
                _state.update {
                    it.copy(
                        isOnline = stillOnline,
                        status = if (isOfflineLocalSession()) {
                            SyncConnectionStatus.OFFLINE
                        } else if (isTerminalBlockingStatus(it.status)) {
                            it.status
                        } else if (!stillOnline) {
                            SyncConnectionStatus.OFFLINE
                        } else {
                            it.status
                        }
                    )
                }
            }
        }
        networkCallback = callback
        try {
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(req, callback)
        } catch (_: Exception) {
            networkCallback = null
        }
    }

    private fun unregisterNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        networkCallback?.let { cb ->
            try {
                cm.unregisterNetworkCallback(cb)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
    }

    private fun checkIsOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isValidEmailKey(email: String): Boolean {
        return email.length in 3..160 && EMAIL_KEY_REGEX.matches(email)
    }

    private fun friendlyError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Akses ditolak. Hubungi pemilik depot."
            msg.contains("UNAVAILABLE", ignoreCase = true) ||
                msg.contains("timed out", ignoreCase = true) ||
                msg.contains("network", ignoreCase = true) ->
                "Koneksi internet terputus. Coba beberapa saat lagi."
            else -> "Gagal menyimpan data online. Cek koneksi internet."
        }
    }

    internal fun getDeltaCursor(depotId: String, colName: String): Timestamp? {
        if (depotId.isBlank() || colName.isBlank()) return null
        val sec = prefs.getLong(deltaCursorSecKey(depotId, colName), -1L)
        if (sec < 0L) return null
        val ns = prefs.getInt(deltaCursorNsKey(depotId, colName), 0)
        return Timestamp(sec, ns)
    }

    internal fun advanceDeltaCursor(
        depotId: String,
        colName: String,
        docs: List<DocumentSnapshot>,
        isFromCache: Boolean,
        markInitializedIfEmpty: Boolean = false
    ) {
        if (isFromCache || depotId.isBlank() || colName.isBlank()) return
        var maxTs = getDeltaCursor(depotId, colName)
        var changed = false

        for (doc in docs) {
            if (doc.metadata.hasPendingWrites()) continue
            val serverTs = doc.getTimestamp("serverUpdatedAt") ?: continue
            if (maxTs == null || compareTimestamp(serverTs, maxTs) > 0) {
                maxTs = serverTs
                changed = true
            }
        }

        if (!changed && markInitializedIfEmpty && maxTs == null) {
            maxTs = Timestamp(0L, 1)
            changed = true
        }

        if (changed && maxTs != null) {
            prefs.edit()
                .putLong(deltaCursorSecKey(depotId, colName), maxTs.seconds)
                .putInt(deltaCursorNsKey(depotId, colName), maxTs.nanoseconds)
                .apply()
        }
    }

    internal fun clearDeltaCursorsForDepot(depotId: String) {
        if (depotId.isBlank()) return
        val editor = prefs.edit()
        val secPrefix = "${KEY_DELTA_SEC_PREFIX}${depotId}_"
        val nsPrefix = "${KEY_DELTA_NS_PREFIX}${depotId}_"
        prefs.all.keys.forEach { key ->
            if (key.startsWith(secPrefix) || key.startsWith(nsPrefix)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    internal fun clearAllDeltaCursors() {
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.startsWith(KEY_DELTA_SEC_PREFIX) || key.startsWith(KEY_DELTA_NS_PREFIX)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    private suspend fun hasAnyLocalCoreData(): Boolean {
        return db.produkDao().count() > 0 ||
            db.pelangganDao().count() > 0 ||
            db.transaksiDao().getAllOnce().isNotEmpty()
    }

    private fun deltaCursorSecKey(depotId: String, colName: String): String =
        "${KEY_DELTA_SEC_PREFIX}${depotId}_$colName"

    private fun deltaCursorNsKey(depotId: String, colName: String): String =
        "${KEY_DELTA_NS_PREFIX}${depotId}_$colName"

    private fun compareTimestamp(a: Timestamp, b: Timestamp): Int {
        val secCmp = a.seconds.compareTo(b.seconds)
        return if (secCmp != 0) secCmp else a.nanoseconds.compareTo(b.nanoseconds)
    }

    companion object {
        private const val TAG = "RealtimeSyncEngine"
        private const val PREFS_NAME = "santoya_sync_prefs"
        private const val KEY_LAST_DEPOT_ID = "last_depot_id"
        private const val KEY_LAST_OWNER_UID = "last_owner_uid"
        private const val KEY_CONFIRMED_OWNER_PREFIX = "confirmed_owner_"
        private const val KEY_LAST_ROLE_PREFIX = "last_role_"
        private const val KEY_LAST_DEPOT_UID_PREFIX = "last_depot_uid_"
        private const val KEY_LAST_OWNER_EMAIL_PREFIX = "last_owner_email_"
        private const val KEY_DELTA_SEC_PREFIX = "delta_sec_"
        private const val KEY_DELTA_NS_PREFIX = "delta_ns_"
        private val EMAIL_KEY_REGEX = Regex("^[a-zA-Z0-9_.+\\-@]+$")
    }
}
