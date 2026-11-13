package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.room.withTransaction
import com.aistudio.companion.kzpmrw.R
import com.example.data.database.AppDatabase
import com.example.data.entity.KasbonLog
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.PendingSyncEntity
import com.example.data.entity.Pengeluaran
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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
    val errorMessage: String? = null
)

object SyncCollections {
    const val PRODUK = "produk"
    const val KURIR = "kurir"
    const val PELANGGAN = "pelanggan"
    const val TRANSAKSI = "transaksi"
    const val PENGELUARAN = "pengeluaran"
    const val KASBON_LOGS = "kasbon_logs"
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

    private var activeUid: String? = null
    private var activeEmail: String = ""
    private var activeDepotId: String = ""
    private var activeRole: DepotRole = DepotRole.OWNER
    private var hasSeededInitialOwnerPush: Boolean = false

    private var staffLinkListener: ListenerRegistration? = null
    private val collectionListeners = mutableListOf<ListenerRegistration>()
    private var pendingObserverJob: Job? = null
    private var flushJob: Job? = null
    private var autoRetryJob: Job? = null
    private var autoRetryCount: Int = 0
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val cachedSnapshots = mutableMapOf<String, List<DocumentSnapshot>>()
    private val receivedInitialCollections = mutableSetOf<String>()

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
        _state.value = RealtimeSyncState(
            status = if (initialOnline) SyncConnectionStatus.CONNECTING else SyncConnectionStatus.OFFLINE,
            isOnline = initialOnline
        )

        registerNetworkCallback()
        observePendingQueue()

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
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, staffLinkPath)
                    // Fallback gracefully to personal owner depot only if lookup is denied
                    switchToDepot(
                        depotId = "depot_$uid",
                        role = DepotRole.OWNER,
                        ownerEmail = cleanEmail
                    )
                    return@addSnapshotListener
                }

                val targetDepotId = snapshot?.getString("depotId").orEmpty()
                val ownerUid = snapshot?.getString("ownerUid").orEmpty()
                val ownerEmail = snapshot?.getString("ownerEmail").orEmpty()
                val isActive = snapshot?.getBoolean("active") == true

                if (snapshot != null && snapshot.exists() && targetDepotId.isNotBlank() && ownerUid != uid) {
                    if (isActive) {
                        switchToDepot(
                            depotId = targetDepotId,
                            role = DepotRole.STAFF,
                            ownerEmail = ownerEmail
                        )
                    } else {
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
                    }
                } else {
                    switchToDepot(
                        depotId = "depot_$uid",
                        role = DepotRole.OWNER,
                        ownerEmail = cleanEmail
                    )
                }
            }
    }

    fun stop() {
        staffLinkListener?.remove()
        staffLinkListener = null
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
        _state.value = RealtimeSyncState()
    }

    fun scheduleFlush(delayMs: Long = 250L) {
        if (activeUid == null || activeDepotId.isBlank()) return
        if (_state.value.status == SyncConnectionStatus.REVOKED) return
        flushJob?.cancel()
        flushJob = scope.launch {
            delay(delayMs)
            flushPendingQueueInternal()
        }
    }

    private fun scheduleAutoRetry() {
        if (activeUid == null || activeDepotId.isBlank()) return
        if (_state.value.status == SyncConnectionStatus.REVOKED) return
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

    suspend fun forceSyncNow(): Result<SyncSummary> {
        val uid = activeUid ?: return Result.failure(IllegalStateException("Belum masuk akun Google"))
        val depotId = activeDepotId.ifBlank { "depot_$uid" }
        val firestore = firestoreProvider()
        return try {
            _state.update { it.copy(status = SyncConnectionStatus.SYNCING, errorMessage = null) }
            if (activeRole == DepotRole.OWNER) {
                enqueueMissingLocalRecordsForOwner()
                ensureDepotAndStaffLinks(firestore, depotId, uid, activeEmail)
            }
            flushPendingQueueInternal()

            val depotRef = firestore.collection("depots").document(depotId)
            val cols = buildList {
                add(SyncCollections.PRODUK)
                add(SyncCollections.KURIR)
                add(SyncCollections.PELANGGAN)
                add(SyncCollections.TRANSAKSI)
                add(SyncCollections.KASBON_LOGS)
                if (activeRole == DepotRole.OWNER) add(SyncCollections.PENGELUARAN)
            }

            withTimeout(15_000L) {
                for (col in cols) {
                    val snap = depotRef.collection(col).get().await()
                    applyMutex.withLock {
                        cachedSnapshots[col] = snap.documents
                        receivedInitialCollections.add(col)
                    }
                }
                applyAllSnapshotsInOrder()
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
            if (role == DepotRole.STAFF && previousDepot != depotId) {
                db.withTransaction {
                    db.pengeluaranDao().deleteAll()
                    if (previousDepot != null) {
                        db.transaksiDao().deleteAll()
                        db.kasbonLogDao().deleteAll()
                        db.pelangganDao().deleteAll()
                        db.kurirDao().deleteAll()
                        db.produkDao().deleteAll()
                        db.pendingSyncDao().deleteAll()
                    }
                }
            }
            prefs.edit().putString(KEY_LAST_DEPOT_ID, depotId).apply()

            val firestore = firestoreProvider()
            val uid = activeUid ?: return@launch

            if (role == DepotRole.OWNER) {
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
            }
        }

        collections.forEach { colName ->
            val path = "depots/$depotId/$colName"
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
                        scope.launch {
                            autoRetryCount = 0
                            applyMutex.withLock {
                                cachedSnapshots[colName] = snapshot.documents
                                receivedInitialCollections.add(colName)
                            }
                            applyAllSnapshotsInOrder()

                            if (role == DepotRole.OWNER && !hasSeededInitialOwnerPush &&
                                receivedInitialCollections.containsAll(collections)
                            ) {
                                hasSeededInitialOwnerPush = true
                                enqueueMissingLocalRecordsForOwner()
                                flushPendingQueueInternal()
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
                        it.status == SyncConnectionStatus.REVOKED -> SyncConnectionStatus.REVOKED
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

                // 6. Pengeluaran (Owner only)
                if (activeRole == DepotRole.OWNER) {
                    cachedSnapshots[SyncCollections.PENGELUARAN]?.forEach { doc ->
                        applyPengeluaranDoc(doc, pendingMap["${SyncCollections.PENGELUARAN}:${doc.id}"])
                    }
                }
            }
        }
    }

    private suspend fun applyProdukDoc(doc: DocumentSnapshot, pending: PendingSyncEntity?) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: 0L
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
        } else if (cloudUpdatedAt > local.updatedAt) {
            db.produkDao().update(
                local.copy(
                    nama_produk = nama,
                    harga = harga,
                    updatedAt = cloudUpdatedAt
                )
            )
        }
    }

    private suspend fun applyKurirDoc(doc: DocumentSnapshot, pending: PendingSyncEntity?) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: 0L
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isDeleted = doc.getBoolean("isDeleted") == true
        val local = db.kurirDao().getBySyncId(syncId)

        if (isDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                db.transaksiDao().clearKurirFromTransaksi(local.id, cloudUpdatedAt)
                db.kurirDao().deleteBySyncId(syncId)
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
        } else if (cloudUpdatedAt > local.updatedAt) {
            db.kurirDao().update(
                local.copy(
                    nama = nama,
                    komisi_per_galon = komisi,
                    email = email,
                    akses_aktif = aksesAktif,
                    skema_komisi = skema,
                    aturan_komisi = aturan,
                    updatedAt = cloudUpdatedAt
                )
            )
        }
    }

    private suspend fun applyPelangganDoc(
        doc: DocumentSnapshot,
        kurirSyncToId: Map<String, Int>,
        pending: PendingSyncEntity?
    ) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: 0L
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isDeleted = doc.getBoolean("isDeleted") == true
        val local = db.pelangganDao().getBySyncId(syncId)

        if (isDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                db.pelangganDao().deleteBySyncId(syncId)
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
                    syncId = syncId
                )
            )
        } else if (cloudUpdatedAt > local.updatedAt) {
            db.pelangganDao().update(
                local.copy(
                    nama = nama,
                    alamat = alamat,
                    no_hp = noHp,
                    sisa_kasbon = sisaKasbon,
                    harga_khusus = hargaKhusus,
                    id_kurir_langganan = idKurirLangganan,
                    updatedAt = cloudUpdatedAt
                )
            )
        }
    }

    private suspend fun applyTransaksiDoc(
        doc: DocumentSnapshot,
        pelangganSyncToId: Map<String, Int>,
        kurirSyncToId: Map<String, Int>,
        produkSyncToId: Map<String, Int>,
        pending: PendingSyncEntity?
    ) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: 0L
        if (pending != null && pending.updatedAt >= cloudUpdatedAt) return

        val isDeleted = doc.getBoolean("isDeleted") == true
        val local = db.transaksiDao().getBySyncId(syncId)

        if (isDeleted) {
            if (local != null && cloudUpdatedAt >= local.updatedAt) {
                db.transaksiDao().deleteBySyncId(syncId)
            }
            return
        }

        val pelangganSyncId = doc.getString("pelangganSyncId") ?: return
        val idPelanggan = pelangganSyncToId[pelangganSyncId] ?: return
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
                    updatedAt = cloudUpdatedAt,
                    syncId = syncId
                )
            )
        } else if (cloudUpdatedAt > local.updatedAt) {
            db.transaksiDao().update(
                local.copy(
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
                    updatedAt = cloudUpdatedAt
                )
            )
        }
    }

    private suspend fun applyKasbonLogDoc(
        doc: DocumentSnapshot,
        pelangganSyncToId: Map<String, Int>,
        pending: PendingSyncEntity?
    ) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: 0L
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
        val idPelanggan = pelangganSyncToId[pelangganSyncId] ?: return
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
        } else if (cloudUpdatedAt > local.updatedAt) {
            db.kasbonLogDao().update(
                local.copy(
                    id_pelanggan = idPelanggan,
                    tanggal = tanggal,
                    jumlah_bayar = jumlahBayar,
                    sisa_kasbon_sebelum = sebelum,
                    sisa_kasbon_sesudah = sesudah,
                    metode_bayar = metode,
                    catatan = catatan,
                    updatedAt = cloudUpdatedAt
                )
            )
        }
    }

    private suspend fun applyPengeluaranDoc(doc: DocumentSnapshot, pending: PendingSyncEntity?) {
        val syncId = doc.getString("syncId") ?: doc.id
        val cloudUpdatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: 0L
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
        } else if (cloudUpdatedAt > local.updatedAt) {
            db.pengeluaranDao().update(
                local.copy(
                    tanggal = tanggal,
                    nama_item = namaItem,
                    kategori = kategori,
                    nominal = nominal,
                    updatedAt = cloudUpdatedAt
                )
            )
        }
    }

    private suspend fun flushPendingQueueInternal() {
        val uid = activeUid ?: return
        val depotId = activeDepotId.ifBlank { return }
        if (_state.value.status == SyncConnectionStatus.REVOKED) return
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
                SyncCollections.PENGELUARAN to 5
            )
            val ordered = allPending.sortedWith(
                compareBy<PendingSyncEntity> { orderPriority[it.collectionName] ?: 99 }
                    .thenBy { it.updatedAt }
            )

            var touchedKurir = false

            for (item in ordered) {
                val isOwnerOnlyCollection = item.collectionName == SyncCollections.PENGELUARAN ||
                    item.collectionName == SyncCollections.PRODUK ||
                    item.collectionName == SyncCollections.KURIR
                if (activeRole == DepotRole.STAFF && isOwnerOnlyCollection) {
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

                    withTimeout(10_000L) {
                        depotRef.collection(item.collectionName)
                            .document(item.syncId)
                            .set(payload)
                            .await()
                    }
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
                        "isDeleted" to false,
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
                        "isDeleted" to false,
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
                        "updatedAt" to tx.updatedAt,
                        "isDeleted" to false,
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
            else -> null
        }
    }

    private suspend fun ensureDepotAndStaffLinks(
        firestore: FirebaseFirestore,
        depotId: String,
        ownerUid: String,
        ownerEmail: String
    ) {
        val rawKurirs = db.kurirDao().getAllOnce()
        val kurirs = rawKurirs.map { k ->
            val rawEm = k.email.trim().lowercase()
            if (rawEm.isNotBlank() && !rawEm.contains("@")) {
                val upgraded = "$rawEm@gmail.com"
                val updatedKurir = k.copy(email = upgraded)
                db.kurirDao().update(updatedKurir)
                updatedKurir
            } else {
                k
            }
        }

        val validKurirs = kurirs.filter {
            val em = it.email.trim().lowercase()
            em.isNotBlank() && em.contains("@") && em != ownerEmail && isValidEmailKey(em)
        }
        val activeMemberEmails = validKurirs
            .filter { it.akses_aktif }
            .map { it.email.trim().lowercase() }
            .distinct()
            .take(30)

        val depotPath = "depots/$depotId"
        val depotRef = firestore.document(depotPath)
        val existingDepot = try {
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
            depotRef.set(createPayload).await()
        } else {
            val currentMembers = (existingDepot.get("memberEmails") as? List<*>)
                ?.mapNotNull { it as? String }
                .orEmpty()
            val currentOwnerEmail = existingDepot.getString("ownerEmail").orEmpty()
            if (currentMembers != activeMemberEmails || currentOwnerEmail != ownerEmail) {
                val createdAtVal = existingDepot.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
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
        }

        // Sync /staff_links/{emailKey} non-fatally so depot sync always succeeds
        try {
            val existingLinksSnap = firestore.collection("staff_links")
                .whereEqualTo("ownerUid", ownerUid)
                .get()
                .await()
            val existingByEmail = existingLinksSnap.documents.associateBy { it.id }
            val desiredByEmail = validKurirs.associateBy { it.email.trim().lowercase() }

            for ((emailKey, doc) in existingByEmail) {
                if (!desiredByEmail.containsKey(emailKey)) {
                    try {
                        doc.reference.delete().await()
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
                    } else {
                        val curActive = existing.getBoolean("active") ?: true
                        val curOwnerEmail = existing.getString("ownerEmail").orEmpty()
                        if (curActive != kurir.akses_aktif || curOwnerEmail != ownerEmail) {
                            val linkCreatedAt = existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
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
                        status = if (it.status == SyncConnectionStatus.REVOKED) {
                            SyncConnectionStatus.REVOKED
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
                        status = if (it.status == SyncConnectionStatus.REVOKED) {
                            SyncConnectionStatus.REVOKED
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
        private val EMAIL_KEY_REGEX = Regex("^[a-zA-Z0-9_.+\\-@]+$")
    }
}
