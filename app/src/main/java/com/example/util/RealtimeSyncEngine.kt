package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.room.withTransaction
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.entity.KasbonLog
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.PendingSyncEntity
import com.example.data.entity.Pengeluaran
import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

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
    val isOnline: Boolean = true,
    val lastSyncedAt: Long? = null,
    val errorMessage: String? = null,
    val isSwitchingOwner: Boolean = false,
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
    }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val applyMutex = Mutex()
    private val flushMutex = Mutex()

    private val _state = MutableStateFlow(RealtimeSyncState())
    val state: StateFlow<RealtimeSyncState> = _state.asStateFlow()

    private fun recordReads(count: Int) {
        if (count <= 0) return
        _state.update { it.copy(sessionReads = it.sessionReads + count) }
    }

    private fun recordWrites(count: Int) {
        if (count <= 0) return
        _state.update { it.copy(sessionWrites = it.sessionWrites + count) }
    }

    private var activeUid: String? = null
    private var activeEmail: String = ""
    private var activeDepotId: String = ""
    private var activeRole: DepotRole = DepotRole.OWNER
    private var hasSeededInitialOwnerPush: Boolean = false

    private var staffLinkListener: ListenerRegistration? = null
    private var lastStaffLinkSnapshot: DocumentSnapshot? = null
    private val collectionListeners = mutableListOf<ListenerRegistration>()
    private var pendingObserverJob: Job? = null
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

    fun start(uid: String, email: String?) {
        val cleanEmail = email?.trim()?.lowercase().orEmpty()
        if (activeUid == uid && activeEmail == cleanEmail && staffLinkListener != null) {
            return
        }
        stop()

        activeUid = uid
        activeEmail = cleanEmail
        hasSeededInitialOwnerPush = false

        val initialOnline = checkIsOnline()
        _state.update {
            it.copy(
                status = if (initialOnline) SyncConnectionStatus.CONNECTING else SyncConnectionStatus.OFFLINE,
                isOnline = initialOnline
            )
        }

        registerNetworkCallback()
        observePendingQueue()

        val previousOwnerUid = prefs.getString(KEY_LAST_OWNER_UID, null)
        scope.launch {
            validateAndPruneOfflineQueue()
            if (activeUid != uid) return@launch

            if (!previousOwnerUid.isNullOrBlank() && previousOwnerUid != uid) {
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
                if (++attempt >= maxAttempts) throw e
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
                    scope.launch {
                        resolveAndApplyDepotRole(firestore, uid, cleanEmail, null)
                    }
                    return@addSnapshotListener
                }

                lastStaffLinkSnapshot = snapshot
                scope.launch {
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
            prefs.edit().putBoolean(keyConfirmedOwner(uid), true).apply()
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

        val memberDepots = if (cleanEmail.isNotBlank() && checkIsOnline()) {
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

        val targetDepotId = snapshot?.getString("depotId").orEmpty()
        val linkOwnerUid = snapshot?.getString("ownerUid").orEmpty()
        val linkOwnerEmail = snapshot?.getString("ownerEmail").orEmpty()
        val isActive = snapshot?.getBoolean("active") == true
        val hasExternalStaffLink = snapshot != null &&
            snapshot.exists() &&
            targetDepotId.isNotBlank() &&
            linkOwnerUid != uid &&
            !KaryawanScopeHelper.isDesignatedStaffEmail(linkOwnerEmail)

        if (activeUid != uid) return false

        if (authoritativeDepotDoc != null) {
            val authDepotId = authoritativeDepotDoc.getString("depotId").orEmpty()
                .ifBlank { authoritativeDepotDoc.id }
            val authOwnerEmail = authoritativeDepotDoc.getString("ownerEmail").orEmpty()

            if (hasExternalStaffLink && targetDepotId == authDepotId && !isActive) {
                transitionToRevoked(authDepotId, authOwnerEmail)
                return true
            }

            prefs.edit().remove(keyConfirmedOwner(uid)).apply()
            cleanupShadowStaffLinks(firestore, uid, cleanEmail)
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
                switchToDepot(
                    depotId = targetDepotId,
                    role = DepotRole.STAFF,
                    ownerEmail = linkOwnerEmail
                )
            } else {
                transitionToRevoked(targetDepotId, linkOwnerEmail)
            }
            return true
        }

        if (!isDesignatedStaff) {
            val isConfirmedOwner = prefs.getBoolean(keyConfirmedOwner(uid), false)
            if (isConfirmedOwner) {
                switchToDepot(
                    depotId = "depot_$uid",
                    role = DepotRole.OWNER,
                    ownerEmail = cleanEmail
                )
                return true
            }

            val ownDepotExists = if (checkIsOnline()) {
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
                prefs.edit().putBoolean(keyConfirmedOwner(uid), true).apply()
                switchToDepot(
                    depotId = "depot_$uid",
                    role = DepotRole.OWNER,
                    ownerEmail = cleanEmail
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

    private fun transitionToRevoked(targetDepotId: String, ownerEmail: String) {
        val wasRevoked = _state.value.status == SyncConnectionStatus.REVOKED
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
        prefs.edit()
            .remove(keyConfirmedOwner(uid))
            .remove(KEY_LAST_DEPOT_ID)
            .apply()
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
        staffLinkListener?.remove()
        staffLinkListener = null
        lastStaffLinkSnapshot = null
        detachCollectionListeners()
        pendingObserverJob?.cancel()
        pendingObserverJob = null
        flushJob?.cancel()
        flushJob = null
        autoRetryJob?.cancel()
        autoRetryJob = null
        autoRetryCount = 0
        unregisterNetworkCallback()
        activeUid = null
        activeDepotId = ""
        hasSeededInitialOwnerPush = false
        cachedSnapshots.clear()
        receivedInitialCollections.clear()
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
        _state.value = RealtimeSyncState(
            sessionReads = _state.value.sessionReads,
            sessionWrites = _state.value.sessionWrites
        )
    }

    private suspend fun clearLocalDataInternal() {
        db.withTransaction {
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
    }

    fun scheduleFlush(delayMs: Long = 250L) {
        if (activeUid == null || activeDepotId.isBlank()) return
        if (isTerminalBlockingStatus(_state.value.status)) return
        flushJob?.cancel()
        flushJob = scope.launch {
            delay(delayMs)
            flushPendingQueueInternal()
        }
    }

    private fun scheduleAutoRetry() {
        if (activeUid == null || activeDepotId.isBlank()) return
        if (isTerminalBlockingStatus(_state.value.status)) return
        if (autoRetryCount >= 3 || autoRetryJob?.isActive == true) return
        autoRetryCount++
        val delayMs = 2_000L * autoRetryCount
        autoRetryJob = scope.launch {
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
            if (collectionListeners.isEmpty()) {
                attachDepotListeners(firestore, depotId, activeRole)
            }
            flushPendingQueueInternal()
        }
    }

    suspend fun forceSyncNow(pushLocalFirst: Boolean = true): Result<SyncSummary> {
        val uid = activeUid ?: return Result.failure(IllegalStateException("Belum masuk akun Google"))
        val depotId = activeDepotId.ifBlank { "depot_$uid" }
        val firestore = firestoreProvider()
        return try {
            _state.update { it.copy(status = SyncConnectionStatus.SYNCING, errorMessage = null) }
            if (activeRole == DepotRole.OWNER) {
                if (pushLocalFirst) enqueueMissingLocalRecordsForOwner()
                ensureDepotAndStaffLinks(firestore, depotId, uid, activeEmail)
            }
            // Mode "Ambil Data" tidak boleh mengirim antrean lokal sebelum cloud
            // selesai diterapkan. Antrean tetap disimpan untuk sinkronisasi berikutnya.
            if (pushLocalFirst) flushPendingQueueInternal()

            val depotRef = firestore.collection("depots").document(depotId)
            val cols = buildList {
                add(SyncCollections.PRODUK)
                add(SyncCollections.KURIR)
                add(SyncCollections.PELANGGAN)
                add(SyncCollections.TRANSAKSI)
                add(SyncCollections.KASBON_LOGS)
                if (activeRole == DepotRole.OWNER) {
                    add(SyncCollections.PENGELUARAN)
                    add(SyncCollections.PERAWATAN_JADWAL)
                    add(SyncCollections.PERAWATAN_LOG)
                }
            }

            withTimeout(15_000L) {
                for (col in cols) {
                    val snap = depotRef.collection(col).get().await()
                    recordReads(snap.documents.size.coerceAtLeast(1))
                    applyMutex.withLock {
                        cachedSnapshots[col] = snap.documents
                        receivedInitialCollections.add(col)
                    }
                }
                applyAllSnapshotsInOrder()
                applyMutex.withLock {
                    cachedSnapshots.clear()
                }
            }

            val pCount = db.pelangganDao().count()
            val prCount = db.produkDao().count()
            val kCount = db.kurirDao().count()
            val txCount = db.transaksiDao().getAllOnce().size
            val expCount = db.pengeluaranDao().getAllOnce().size

            _state.update {
                it.copy(
                    status = if (it.isOnline) SyncConnectionStatus.LIVE else SyncConnectionStatus.OFFLINE,
                    lastSyncedAt = System.currentTimeMillis(),
                    errorMessage = null
                )
            }
            Result.success(
                SyncSummary(
                    pelangganCount = pCount,
                    produkCount = prCount,
                    kurirCount = kCount,
                    transaksiCount = txCount,
                    pengeluaranCount = expCount,
                    message = "Sinkronisasi Real-Time selesai"
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "depots/$depotId")
            val isOnline = checkIsOnline()
            _state.update {
                it.copy(
                    status = if (!isOnline) SyncConnectionStatus.OFFLINE else SyncConnectionStatus.ERROR,
                    isOnline = isOnline,
                    errorMessage = friendlyError(e)
                )
            }
            Result.failure(Exception(friendlyError(e), e))
        }
    }

    private fun switchToDepot(
        depotId: String,
        role: DepotRole,
        ownerEmail: String
    ) {
        if (activeDepotId == depotId && activeRole == role && collectionListeners.isNotEmpty()) {
            return
        }
        detachCollectionListeners()
        cachedSnapshots.clear()
        receivedInitialCollections.clear()
        hasSeededInitialOwnerPush = false

        val previousDepot = prefs.getString(KEY_LAST_DEPOT_ID, null)
        val previousRole = activeRole
        activeDepotId = depotId
        activeRole = role

        _state.update {
            it.copy(
                status = if (it.isOnline) SyncConnectionStatus.SYNCING else SyncConnectionStatus.OFFLINE,
                role = role,
                depotId = depotId,
                ownerEmail = ownerEmail,
                errorMessage = null
            )
        }

        scope.launch {
            val sessionUid = activeUid ?: return@launch
            // Setiap perpindahan depot wajib mengirim antrean yang masih valid milik depot lama
            // terlebih dahulu (best-effort), lalu menghapus data depot lama agar tidak tercampur.
            if (previousDepot != depotId) {
                val lastOwnerUid = prefs.getString(KEY_LAST_OWNER_UID, null)
                if (previousDepot != null &&
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
                    } catch (e: Exception) {
                        Log.w(TAG, "Best-effort flush before switching depot skipped: ${e.message}")
                    }
                }
                if (previousDepot != null || role == DepotRole.STAFF) {
                    db.withTransaction {
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
                }
            } else if (role == DepotRole.STAFF) {
                // Karyawan tidak boleh memiliki data pengeluaran maupun jadwal/riwayat perawatan pemilik di cache lokal.
                db.withTransaction {
                    db.pengeluaranDao().deleteAll()
                    db.perawatanLogDao().deleteAll()
                    db.perawatanDao().deleteAll()
                }
            }
            if (activeUid != sessionUid) return@launch
            prefs.edit().putString(KEY_LAST_DEPOT_ID, depotId).apply()

            val firestore = firestoreProvider()
            val uid = sessionUid

            if (role == DepotRole.OWNER) {
                try {
                    ensureDesignatedStaffKurirsForOwner(activeEmail)
                } catch (e: Exception) {
                    Log.w(TAG, "Local designated staff seed skipped: ${e.message}")
                }
                try {
                    withTimeout(10_000L) {
                        ensureDepotAndStaffLinks(firestore, depotId, uid, activeEmail)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Deferred depot metadata sync: ${e.message}")
                }
            }

            attachDepotListeners(firestore, depotId, role)
            flushPendingQueueInternal()
        }
    }

    private fun attachDepotListeners(
        firestore: FirebaseFirestore,
        depotId: String,
        role: DepotRole
    ) {
        val depotRef = firestore.collection("depots").document(depotId)
        val collections = buildList {
            add(SyncCollections.PRODUK)
            add(SyncCollections.KURIR)
            add(SyncCollections.PELANGGAN)
            add(SyncCollections.TRANSAKSI)
            add(SyncCollections.KASBON_LOGS)
            if (role == DepotRole.OWNER) {
                add(SyncCollections.PENGELUARAN)
                add(SyncCollections.PERAWATAN_JADWAL)
                add(SyncCollections.PERAWATAN_LOG)
            }
        }

        collections.forEach { colName ->
            val path = "depots/$depotId/$colName"
            var isInitialSnapshotForCol = true
            val registration = depotRef.collection(colName)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, path)
                        val isOnline = checkIsOnline()
                        _state.update {
                            it.copy(
                                status = if (!isOnline) SyncConnectionStatus.OFFLINE else SyncConnectionStatus.ERROR,
                                isOnline = isOnline,
                                errorMessage = friendlyError(error)
                            )
                        }
                        if (isOnline) {
                            scheduleAutoRetry()
                        }
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val isInitialForCollection = isInitialSnapshotForCol
                        val readsCount = if (isInitialForCollection) {
                            isInitialSnapshotForCol = false
                            snapshot.documents.size.coerceAtLeast(1)
                        } else {
                            snapshot.documentChanges.size
                        }
                        recordReads(readsCount)
                        scope.launch {
                            autoRetryCount = 0
                            if (isInitialForCollection) {
                                applyMutex.withLock {
                                    cachedSnapshots[colName] = snapshot.documents
                                    receivedInitialCollections.add(colName)
                                }
                                applyAllSnapshotsInOrder()
                            } else {
                                applyChangedDocuments(colName, snapshot.documentChanges)
                            }

                            if (receivedInitialCollections.containsAll(collections)) {
                                if (role == DepotRole.OWNER && !hasSeededInitialOwnerPush) {
                                    hasSeededInitialOwnerPush = true
                                    enqueueMissingLocalRecordsForOwner()
                                    flushPendingQueueInternal()
                                }
                                applyMutex.withLock {
                                    if (cachedSnapshots.isNotEmpty()) {
                                        cachedSnapshots.clear()
                                    }
                                }
                            }

                            _state.update {
                                val onlineNow = checkIsOnline()
                                it.copy(
                                    status = if (!onlineNow) {
                                        SyncConnectionStatus.OFFLINE
                                    } else if (it.pendingCount > 0) {
                                        SyncConnectionStatus.SYNCING
                                    } else {
                                        SyncConnectionStatus.LIVE
                                    },
                                    isOnline = onlineNow,
                                    lastSyncedAt = System.currentTimeMillis(),
                                    errorMessage = null
                                )
                            }
                        }
                    }
                }
            collectionListeners.add(registration)
        }
    }

    private fun detachCollectionListeners() {
        collectionListeners.forEach { it.remove() }
        collectionListeners.clear()
    }

    private fun observePendingQueue() {
        pendingObserverJob?.cancel()
        pendingObserverJob = scope.launch {
            db.pendingSyncDao().observePendingCount().collect { count ->
                val isOnline = checkIsOnline()
                _state.update {
                    val nextStatus = when {
                        isTerminalBlockingStatus(it.status) -> it.status
                        !isOnline -> SyncConnectionStatus.OFFLINE
                        count > 0 -> SyncConnectionStatus.SYNCING
                        activeDepotId.isNotBlank() && collectionListeners.isNotEmpty() -> SyncConnectionStatus.LIVE
                        else -> it.status
                    }
                    it.copy(
                        pendingCount = count,
                        isOnline = isOnline,
                        status = nextStatus
                    )
                }
            }
        }
    }

    private suspend fun enqueueMissingLocalRecordsForOwner() {
        // Jaring pengaman: jangan pernah mengunggah data lokal jika pemilik sesi
        // tidak sama dengan pemilik yang tercatat di perangkat ini.
        val recordedOwnerUid = prefs.getString(KEY_LAST_OWNER_UID, null)
        if (recordedOwnerUid.isNullOrBlank() || recordedOwnerUid != activeUid) return

        val pendingKeys = db.pendingSyncDao().getAllPending()
            .map { "${it.collectionName}:${it.syncId}" }
            .toSet()

        suspend fun checkAndEnqueue(col: String, syncId: String, localUpdatedAt: Long) {
            if ("$col:$syncId" in pendingKeys) return
            val cloudDoc = cachedSnapshots[col]?.firstOrNull { it.id == syncId }
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

        db.produkDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.PRODUK, it.syncId, it.updatedAt) }
        db.kurirDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.KURIR, it.syncId, it.updatedAt) }
        db.pelangganDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.PELANGGAN, it.syncId, it.updatedAt) }
        db.transaksiDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.TRANSAKSI, it.syncId, it.updatedAt) }
        db.pengeluaranDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.PENGELUARAN, it.syncId, it.updatedAt) }
        db.kasbonLogDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.KASBON_LOGS, it.syncId, it.updatedAt) }
        db.perawatanDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.PERAWATAN_JADWAL, it.syncId, it.updatedAt) }
        db.perawatanLogDao().getAllOnce().forEach { checkAndEnqueue(SyncCollections.PERAWATAN_LOG, it.syncId, it.updatedAt) }
    }

    private suspend fun applyAllSnapshotsInOrder() {
        applyMutex.withLock {
            val pendingMap = db.pendingSyncDao().getAllPending()
                .associateBy { "${it.collectionName}:${it.syncId}" }

            db.withTransaction {
                // 1. Produk
                cachedSnapshots[SyncCollections.PRODUK]?.forEach { doc ->
                    applyProdukDoc(doc, pendingMap["${SyncCollections.PRODUK}:${doc.id}"])
                }

                // 2. Kurir (also ensures depot/staff links stay clean if needed)
                cachedSnapshots[SyncCollections.KURIR]?.forEach { doc ->
                    applyKurirDoc(doc, pendingMap["${SyncCollections.KURIR}:${doc.id}"])
                }

                // 3. Pelanggan (depends on Kurir syncId -> local id)
                val kurirSyncToId = db.kurirDao().getAllOnce().associate { it.syncId to it.id }
                cachedSnapshots[SyncCollections.PELANGGAN]?.forEach { doc ->
                    applyPelangganDoc(doc, kurirSyncToId, pendingMap["${SyncCollections.PELANGGAN}:${doc.id}"])
                }

                // 4. Transaksi (depends on Pelanggan, Kurir, Produk)
                val pelangganSyncToId = db.pelangganDao().getAllOnce().associate { it.syncId to it.id }
                val produkSyncToId = db.produkDao().getAllOnce().associate { it.syncId to it.id }
                cachedSnapshots[SyncCollections.TRANSAKSI]?.forEach { doc ->
                    applyTransaksiDoc(
                        doc = doc,
                        pelangganSyncToId = pelangganSyncToId,
                        kurirSyncToId = kurirSyncToId,
                        produkSyncToId = produkSyncToId,
                        pending = pendingMap["${SyncCollections.TRANSAKSI}:${doc.id}"]
                    )
                }

                // 5. Kasbon Logs
                cachedSnapshots[SyncCollections.KASBON_LOGS]?.forEach { doc ->
                    applyKasbonLogDoc(
                        doc = doc,
                        pelangganSyncToId = pelangganSyncToId,
                        pending = pendingMap["${SyncCollections.KASBON_LOGS}:${doc.id}"]
                    )
                }

                // 6. Pengeluaran & Perawatan RO (Owner only)
                if (activeRole == DepotRole.OWNER) {
                    cachedSnapshots[SyncCollections.PENGELUARAN]?.forEach { doc ->
                        applyPengeluaranDoc(doc, pendingMap["${SyncCollections.PENGELUARAN}:${doc.id}"])
                    }
                    cachedSnapshots[SyncCollections.PERAWATAN_JADWAL]?.forEach { doc ->
                        applyPerawatanJadwalDoc(doc, pendingMap["${SyncCollections.PERAWATAN_JADWAL}:${doc.id}"])
                    }
                    cachedSnapshots[SyncCollections.PERAWATAN_LOG]?.forEach { doc ->
                        applyPerawatanLogDoc(doc, pendingMap["${SyncCollections.PERAWATAN_LOG}:${doc.id}"])
                    }
                    if (receivedInitialCollections.contains(SyncCollections.PERAWATAN_JADWAL) &&
                        cachedSnapshots[SyncCollections.PERAWATAN_JADWAL].isNullOrEmpty() &&
                        db.perawatanDao().count() == 0
                    ) {
                        db.perawatanDao().insertAllIgnore(PerawatanScheduler.buildDefaultItems())
                    }
                }
            }
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
        } else {
            val remote = local.copy(
                nama_produk = nama,
                harga = harga,
                updatedAt = cloudUpdatedAt
            )
            val resolved = SyncConflictResolver.resolve(local, remote)
            if (resolved !== local) {
                db.produkDao().update(resolved)
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
                    updatedAt = cloudUpdatedAt,
                    syncId = syncId,
                    isDeleted = isSoftDeleted,
                    deletedBy = deletedBy
                )
            )
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
                updatedAt = cloudUpdatedAt,
                isDeleted = isSoftDeleted,
                deletedBy = deletedBy
            )
            val resolved = SyncConflictResolver.resolve(local, remote)
            if (resolved !== local) {
                db.transaksiDao().update(resolved)
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
            }
        }
    }

    private suspend fun flushPendingQueueInternal(
        targetDepotId: String = activeDepotId,
        targetRole: DepotRole = activeRole
    ) {
        val uid = activeUid ?: return
        val depotId = targetDepotId.ifBlank { return }
        if (isTerminalBlockingStatus(_state.value.status)) return
        if (!checkIsOnline()) {
            _state.update { it.copy(isOnline = false, status = SyncConnectionStatus.OFFLINE) }
            return
        }

        flushMutex.withLock {
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
                SyncCollections.PERAWATAN_LOG to 7
            )
            val ordered = allPending.sortedWith(
                compareBy<PendingSyncEntity> { orderPriority[it.collectionName] ?: 99 }
                    .thenBy { it.updatedAt }
            )

            var touchedKurir = false

            for (item in ordered) {
                val isOwnerOnlyCollection = item.collectionName == SyncCollections.PENGELUARAN ||
                    item.collectionName == SyncCollections.PERAWATAN_JADWAL ||
                    item.collectionName == SyncCollections.PERAWATAN_LOG ||
                    item.collectionName == SyncCollections.PRODUK ||
                    item.collectionName == SyncCollections.KURIR
                if (targetRole == DepotRole.STAFF && isOwnerOnlyCollection) {
                    db.pendingSyncDao().remove(item.collectionName, item.syncId)
                    continue
                }

                val path = "depots/$depotId/${item.collectionName}/${item.syncId}"
                try {
                    val payload = buildDocumentPayload(item)
                    if (payload == null) {
                        db.pendingSyncDao().remove(item.collectionName, item.syncId)
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

                    if (item.collectionName == SyncCollections.KURIR) {
                        touchedKurir = true
                    }
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, path)
                    val online = checkIsOnline()
                    _state.update {
                        it.copy(
                            isOnline = online,
                            status = if (!online) SyncConnectionStatus.OFFLINE else SyncConnectionStatus.ERROR,
                            errorMessage = friendlyError(e)
                        )
                    }
                    if (online) {
                        scheduleAutoRetry()
                    }
                    return
                }
            }

            if (touchedKurir && activeRole == DepotRole.OWNER) {
                try {
                    withTimeout(10_000L) {
                        ensureDepotAndStaffLinks(firestore, depotId, uid, activeEmail)
                    }
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, "depots/$depotId")
                }
            }

            autoRetryCount = 0
            val remaining = db.pendingSyncDao().getAllPending().size
            _state.update {
                it.copy(
                    isOnline = true,
                    pendingCount = remaining,
                    status = if (remaining == 0) SyncConnectionStatus.LIVE else SyncConnectionStatus.SYNCING,
                    lastSyncedAt = System.currentTimeMillis(),
                    errorMessage = null
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
                    val kurirSyncId = p.id_kurir_langganan?.let { db.kurirDao().getById(it)?.syncId }
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
                        "deletedBy" to p.deletedBy,
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
                    val pelangganSyncId = db.pelangganDao().getById(tx.id_pelanggan)?.syncId ?: "unknown"
                    val kurirSyncId = tx.id_kurir?.let { db.kurirDao().getById(it)?.syncId }
                    val produkSyncId = tx.id_produk?.let { db.produkDao().getById(it)?.syncId }
                    mapOf(
                        "syncId" to tx.syncId,
                        "tanggal" to tx.tanggal.coerceAtLeast(0L),
                        "pelangganSyncId" to pelangganSyncId,
                        "kurirSyncId" to kurirSyncId,
                        "produkSyncId" to produkSyncId,
                        "jumlah" to tx.jumlah.coerceAtLeast(1),
                        "harga_satuan" to tx.harga_satuan.coerceAtLeast(0.0),
                        "total_harga" to tx.total_harga.coerceAtLeast(0.0),
                        "total_komisi" to tx.total_komisi.coerceAtLeast(0.0),
                        "metode_bayar" to tx.metode_bayar.take(40),
                        "status" to tx.status.take(40),
                        "uangMuka" to tx.uangMuka.coerceAtLeast(0L),
                        "sisaKasbon" to tx.sisaKasbon.coerceAtLeast(0L),
                        "tanggalBayar" to tx.tanggalBayar,
                        "updatedAt" to tx.updatedAt,
                        "isDeleted" to tx.isDeleted,
                        "deletedBy" to tx.deletedBy,
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
                    val pelangganSyncId = db.pelangganDao().getById(log.id_pelanggan)?.syncId ?: "unknown"
                    mapOf(
                        "syncId" to log.syncId,
                        "pelangganSyncId" to pelangganSyncId,
                        "tanggal" to log.tanggal.coerceAtLeast(0L),
                        "jumlah_bayar" to log.jumlah_bayar,
                        "sisa_kasbon_sebelum" to log.sisa_kasbon_sebelum,
                        "sisa_kasbon_sesudah" to log.sisa_kasbon_sesudah,
                        "metode_bayar" to log.metode_bayar.take(40),
                        "catatan" to log.catatan.take(300),
                        "updatedAt" to log.updatedAt,
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
    ) {
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
        if (existingDepot == null || !existingDepot.exists()) {
            val createPayload = mapOf(
                "depotId" to depotId,
                "ownerUid" to ownerUid,
                "ownerEmail" to ownerEmail.take(160),
                "name" to "Santoya RO",
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
            if (currentMembers != activeMemberEmails || currentOwnerEmail != ownerEmail) {
                val createdAtVal = existingDepot.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
                withRetry {
                    depotRef.set(
                        mapOf(
                            "depotId" to depotId,
                            "ownerUid" to ownerUid,
                            "ownerEmail" to ownerEmail.take(160),
                            "name" to "Santoya RO",
                            "memberEmails" to activeMemberEmails,
                            "createdAt" to createdAtVal,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
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

    private fun registerNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
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
                scheduleFlush(delayMs = 150L)
            }

            override fun onLost(network: Network) {
                val stillOnline = checkIsOnline()
                _state.update {
                    it.copy(
                        isOnline = stillOnline,
                        status = if (isTerminalBlockingStatus(it.status)) {
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
            msg.contains("PERMISSION_DENIED", ignoreCase = true) -> "Akses Cloud ditolak"
            msg.contains("UNAVAILABLE", ignoreCase = true) ||
                msg.contains("timed out", ignoreCase = true) -> "Koneksi Cloud terganggu, mencoba ulang otomatis"
            else -> e.localizedMessage ?: "Gagal sinkronisasi Cloud"
        }
    }

    companion object {
        private const val TAG = "RealtimeSyncEngine"
        private const val PREFS_NAME = "santoya_sync_prefs"
        private const val KEY_LAST_DEPOT_ID = "last_depot_id"
        private const val KEY_LAST_OWNER_UID = "last_owner_uid"
        private const val KEY_CONFIRMED_OWNER_PREFIX = "confirmed_owner_"
        private val EMAIL_KEY_REGEX = Regex("^[a-zA-Z0-9_.+\\-@]+$")
    }
}
