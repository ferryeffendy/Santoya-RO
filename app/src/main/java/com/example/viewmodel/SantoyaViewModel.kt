package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.data.database.AppDatabase
import com.example.data.entity.AiBusinessInsight
import com.example.data.entity.DepotProfile
import com.example.data.entity.KasbonLog
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import com.example.data.entity.Produk
import com.example.data.entity.TangkiRefill
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.data.repository.AiAnalyticsRepository
import com.example.data.repository.InsufficientAiDataException
import com.example.data.repository.SantoyaRepository
import com.example.util.DepotRole
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.LastSessionInfo
import com.example.util.OfflineLocalSummary
import com.example.util.OfflineSessionManager
import com.example.util.OperationType
import com.example.util.PelangganAuditHelper
import com.example.util.PerawatanEvaluation
import com.example.util.PerawatanScheduler
import com.example.util.RealtimeSyncEngine
import com.example.util.RealtimeSyncState
import com.example.util.TorenWaterCalculator
import com.example.util.CipCleaningMode
import com.example.util.CipDosageCalculator
import com.example.util.CipDosageResult
import com.example.util.CipEvaluationOutcome
import com.example.util.CipHousingSize
import com.example.util.CipValidationError
import com.example.util.RoMembraneCalculator
import com.example.util.RoMembraneCategory
import com.example.util.RoMembraneEvaluationOutcome
import com.example.util.RoMembranePreset
import com.example.util.RoMembraneResult
import com.example.util.RoValidationFieldError
import com.example.util.SearchField
import com.example.util.SearchMatcher
import com.example.util.SyncConnectionStatus
import com.example.util.SyncSummary
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

enum class SantoyaScreen {
    DASHBOARD,
    TRANSAKSI,
    PELANGGAN,
    KURIR,
    PRODUK,
    PENGELUARAN,
    LAPORAN,
    SETTINGS,
    PERAWATAN
}

enum class KasbonSortOption {
    TERBANYAK,
    TERLAMA,
    TERBARU
}

/**
 * State tampilan untuk tab Kalkulator Kesehatan Membran RO (khusus pemilik depot).
 */
data class RoMembraneUiState(
    val tdsBakuInput: String = "",
    val tdsJadiInput: String = "",
    val selectedCategory: RoMembraneCategory = RoMembraneCategory.SIZE_4040,
    val parallelCount: Int = RoMembraneCalculator.MIN_PARALLEL_MEMBRANES,
    val selectedPresetId: String? = "4040_ulp",
    val gpdPerUnitInput: String = "2400",
    val actualFlowLpmInput: String = "",
    val tdsBakuError: RoValidationFieldError? = null,
    val tdsJadiError: RoValidationFieldError? = null,
    val gpdError: RoValidationFieldError? = null,
    val flowAktualError: RoValidationFieldError? = null,
    val result: RoMembraneResult? = null,
    val isSavedToHistory: Boolean = false
)

/**
 * State tampilan untuk sub-tab Kalkulator Takaran Pencucian Membran RO (CIP).
 */
data class CipCalculatorUiState(
    val selectedMode: CipCleaningMode = CipCleaningMode.CITRIC_ACID,
    val selectedHousing: CipHousingSize = CipHousingSize.HOUSING_4040,
    val tubeCount: Int = CipDosageCalculator.MIN_TUBE_COUNT,
    val waterVolumeInput: String = "18",
    val isCustomWaterVolume: Boolean = false,
    val concentrationInput: String = "2,0",
    val biayaInput: String = "",
    val extraNoteInput: String = "",
    val volumeError: CipValidationError? = null,
    val concentrationError: CipValidationError? = null,
    val result: CipDosageResult? = null,
    val isSavedToHistory: Boolean = false,
    val recommendedFromDiagnosis: CipCleaningMode? = null
)

enum class LaporanPeriode(val label: String) {
    HARI_INI("Hari Ini"),
    MINGGU_INI("Minggu Ini"),
    BULAN_INI("Bulan Ini"),
    TAHUN_INI("Tahun Ini"),
    SEMUA("Semua")
}

data class TodaySummary(
    val galonTerjual: Int = 0,
    val pendapatanLunas: Double = 0.0,
    val piutangKasbon: Double = 0.0,
    val jumlahPesanan: Int = 0
)

data class ActiveReceiptDialog(
    val title: String,
    val receiptText: String,
    val customerPhone: String? = null,
    val data: Any? = null,
    val receiptBuilder: ((DepotProfile) -> String)? = null
)

data class KasbonDiscrepancy(
    val pelangganId: Int,
    val namaPelanggan: String,
    val saldoTercatat: Double,
    val totalNotaBelumLunas: Double,
    val selisih: Double
)

/**
 * State tampilan untuk fitur AI Business Insights khusus Pemilik Depot.
 */
sealed interface AiAnalyticsUiState {
    data class Idle(
        val selectedPeriodDays: Int = AiBusinessInsight.DEFAULT_PERIOD_DAYS
    ) : AiAnalyticsUiState

    data class Loading(
        val cachedInsight: AiBusinessInsight? = null,
        val selectedPeriodDays: Int = AiBusinessInsight.DEFAULT_PERIOD_DAYS
    ) : AiAnalyticsUiState

    data class Success(
        val insight: AiBusinessInsight,
        val isSavedOffline: Boolean = false
    ) : AiAnalyticsUiState

    data class Error(
        val message: String,
        val cachedInsight: AiBusinessInsight? = null,
        val selectedPeriodDays: Int = AiBusinessInsight.DEFAULT_PERIOD_DAYS
    ) : AiAnalyticsUiState
}

class SantoyaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val transaksiDao = db.transaksiDao()
    private val pelangganDao = db.pelangganDao()
    private val syncEngine = RealtimeSyncEngine(application, db)
    private val repository = SantoyaRepository(db, application) {
        syncEngine.scheduleFlush()
    }
    private val aiAnalyticsRepository by lazy { AiAnalyticsRepository(getApplication()) }

    val transaksiPagingFlow: Flow<PagingData<Transaksi>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        transaksiDao.getTransaksiPaging()
    }.flow.cachedIn(viewModelScope)

    val pelangganPagingFlow: Flow<PagingData<Pelanggan>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        pelangganDao.getPelangganPaging()
    }.flow.cachedIn(viewModelScope)

    // --- Centralized Loading & Anti-Double-Tap Busy States ---
    private val _isInitialDataLoaded = MutableStateFlow(false)
    val isInitialDataLoaded: StateFlow<Boolean> = _isInitialDataLoaded.asStateFlow()

    private val _isBusyAction = MutableStateFlow(false)
    val isBusyAction: StateFlow<Boolean> = _isBusyAction.asStateFlow()

    private val _busyActionMessage = MutableStateFlow<String?>(null)
    val busyActionMessage: StateFlow<String?> = _busyActionMessage.asStateFlow()

    private val busyActionGuard = AtomicBoolean(false)

    private inline fun launchGuardedAction(
        message: String,
        crossinline block: suspend () -> Unit
    ) {
        if (realtimeSyncState.value.status == SyncConnectionStatus.REVOKED) {
            showSnackbar("Akses akun dinonaktifkan. Belum bisa membuat nota baru.")
            return
        }
        if (realtimeSyncState.value.status == SyncConnectionStatus.AWAITING_ROLE_CONFIRMATION) {
            showSnackbar("Konfirmasi peran akun terlebih dahulu.")
            return
        }
        if (!busyActionGuard.compareAndSet(false, true)) return
        _isBusyAction.value = true
        _busyActionMessage.value = message
        viewModelScope.launch {
            try {
                block()
            } finally {
                _busyActionMessage.value = null
                _isBusyAction.value = false
                busyActionGuard.set(false)
            }
        }
    }

    // --- Navigation ---
    private val _currentScreen = MutableStateFlow(SantoyaScreen.DASHBOARD)
    val currentScreen: StateFlow<SantoyaScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: SantoyaScreen) {
        if (screen == SantoyaScreen.PERAWATAN && isUserKaryawan()) {
            showSnackbar("Jadwal Perawatan hanya dapat diakses pemilik depot.")
            return
        }
        _currentScreen.value = screen
    }

    // --- Master Data StateFlows ---
    val allProduk: StateFlow<List<Produk>> = repository.allProduk
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKurir: StateFlow<List<Kurir>> = repository.allKurir
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPelanggan: StateFlow<List<Pelanggan>> = repository.allPelanggan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pelangganKasbon: StateFlow<List<Pelanggan>> = repository.pelangganKasbon
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPengeluaran: StateFlow<List<Pengeluaran>> = repository.allPengeluaran
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransaksi: StateFlow<List<TransaksiDetail>> = repository.allTransaksiWithDetail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKasbonLogs: StateFlow<List<KasbonLog>> = repository.allKasbonLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKasbonTransaksi: StateFlow<List<TransaksiDetail>> = repository.allKasbonTransaksiWithDetail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTangkiRefill: StateFlow<List<TangkiRefill>> = repository.allTangkiRefill
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Cloud Firestore Real-Time Sync State & Depot Profile ---
    val realtimeSyncState: StateFlow<RealtimeSyncState> = syncEngine.state
    val depotProfile: StateFlow<DepotProfile> = syncEngine.profileStore.profileFlow
    val hasPendingProfileSync: StateFlow<Boolean> = syncEngine.profileStore.pendingSyncFlow

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private val _activeSyncPushMode = MutableStateFlow<Boolean?>(null)
    val activeSyncPushMode: StateFlow<Boolean?> = _activeSyncPushMode.asStateFlow()

    private val _cloudSyncError = MutableStateFlow<String?>(null)
    val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow<SyncSummary?>(null)
    val lastSyncSummary: StateFlow<SyncSummary?> = _lastSyncSummary.asStateFlow()

    private val _activeAuthUid = MutableStateFlow(
        try {
            Firebase.auth.currentUser?.uid
                ?: if (syncEngine.offlineSessionManager.isOfflineModeActive()) {
                    OfflineSessionManager.OFFLINE_LOCAL_UID
                } else {
                    null
                }
        } catch (_: Exception) {
            null
        }
    )

    private val _currentUserEmail = MutableStateFlow(
        try {
            Firebase.auth.currentUser?.email
        } catch (_: Exception) {
            null
        }
    )

    val isOfflineLocalSession: StateFlow<Boolean> = _activeAuthUid
        .map { it == OfflineSessionManager.OFFLINE_LOCAL_UID }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            syncEngine.offlineSessionManager.isOfflineModeActive()
        )

    val offlineLocalSummary: StateFlow<OfflineLocalSummary> = combine(
        allPelanggan,
        allTransaksi,
        allProduk,
        realtimeSyncState
    ) { pelangganList, transaksiList, produkList, sync ->
        val activePelanggan = pelangganList.count { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) }
        val activeTransaksi = transaksiList.count { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
        OfflineLocalSummary(
            pelangganCount = activePelanggan,
            transaksiCount = activeTransaksi,
            produkCount = produkList.size,
            pendingCount = sync.pendingCount
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OfflineLocalSummary())

    val isKaryawan: StateFlow<Boolean> = combine(
        realtimeSyncState,
        allKurir,
        _currentUserEmail,
        isOfflineLocalSession
    ) { sync, kurirs, email, offlineLocal ->
        if (offlineLocal) {
            false
        } else {
            KaryawanScopeHelper.isKaryawanRole(sync.role, email, sync.ownerEmail, kurirs)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- AI Business Insights (Khusus Pemilik Depot) ---
    private val _isAnalyzingAi = MutableStateFlow(false)
    private val _aiAnalyticsError = MutableStateFlow<String?>(null)
    private val _selectedAiPeriodDays = MutableStateFlow(AiBusinessInsight.DEFAULT_PERIOD_DAYS)
    val selectedAiPeriodDays: StateFlow<Int> = _selectedAiPeriodDays.asStateFlow()
    private val _localAiInsightFallback = MutableStateFlow<Pair<AiBusinessInsight, Boolean>?>(null)
    private val _showAiInsightsSheet = MutableStateFlow(false)
    val showAiInsightsSheet: StateFlow<Boolean> = _showAiInsightsSheet.asStateFlow()
    private val aiAnalysisGuard = AtomicBoolean(false)
    private var aiAnalysisJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val latestAiInsight: StateFlow<AiBusinessInsight?> = combine(
        _activeAuthUid,
        isKaryawan
    ) { uid, karyawan ->
        if (uid.isNullOrBlank() || uid == OfflineSessionManager.OFFLINE_LOCAL_UID || karyawan) null else uid
    }.flatMapLatest { ownerUid ->
        if (ownerUid == null) {
            flowOf(null)
        } else {
            aiAnalyticsRepository.observeLatestInsight(ownerUid)
                .catch { err ->
                    if (err is CancellationException) {
                        val fsCause = generateSequence(err as Throwable) { it.cause }
                            .firstOrNull { it is FirebaseFirestoreException }
                        if (fsCause == null) throw err
                    }
                    handleFirestoreError(
                        err,
                        OperationType.GET,
                        "ai_insights/${AiBusinessInsight.latestDocId(ownerUid)}"
                    )
                    _aiAnalyticsError.value = mapAiAnalyticsError(err)
                    emit(null)
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val aiAnalyticsUiState: StateFlow<AiAnalyticsUiState> = combine(
        latestAiInsight,
        _localAiInsightFallback,
        _isAnalyzingAi,
        _aiAnalyticsError,
        _selectedAiPeriodDays
    ) { cloudInsight, localFallback, isLoading, errorMsg, periodDays ->
        val (effectiveInsight, isSavedOffline) = when {
            cloudInsight != null && localFallback == null -> cloudInsight to false
            cloudInsight == null && localFallback != null -> localFallback.first to localFallback.second
            cloudInsight != null && localFallback != null -> {
                val cloudTs = cloudInsight.timestamp?.toDate()?.time ?: 0L
                val localTs = localFallback.first.timestamp?.toDate()?.time ?: 0L
                if (!localFallback.second && cloudTs >= localTs) {
                    cloudInsight to false
                } else if (localFallback.second && cloudInsight.periodDays == localFallback.first.periodDays && cloudTs > localTs) {
                    cloudInsight to false
                } else {
                    localFallback.first to localFallback.second
                }
            }
            else -> null to false
        }

        when {
            isLoading -> AiAnalyticsUiState.Loading(
                cachedInsight = effectiveInsight,
                selectedPeriodDays = periodDays
            )
            errorMsg != null -> AiAnalyticsUiState.Error(
                message = errorMsg,
                cachedInsight = effectiveInsight,
                selectedPeriodDays = periodDays
            )
            effectiveInsight != null -> AiAnalyticsUiState.Success(
                insight = effectiveInsight,
                isSavedOffline = isSavedOffline
            )
            else -> AiAnalyticsUiState.Idle(selectedPeriodDays = periodDays)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiAnalyticsUiState.Idle())

    val activeKaryawan: StateFlow<Kurir?> = combine(
        allKurir,
        _currentUserEmail
    ) { kurirs, email ->
        KaryawanScopeHelper.resolveActiveKaryawan(email, kurirs)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val scopedPelanggan: StateFlow<List<Pelanggan>> = combine(
        allPelanggan,
        isKaryawan,
        activeKaryawan
    ) { pelangganList, karyawan, kurir ->
        KaryawanScopeHelper.filterPelangganForRole(pelangganList, karyawan, kurir)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scopedTransaksi: StateFlow<List<TransaksiDetail>> = combine(
        allTransaksi,
        isKaryawan,
        activeKaryawan
    ) { transaksiList, karyawan, kurir ->
        KaryawanScopeHelper.filterTransaksiForRole(transaksiList, karyawan, kurir)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scopedPelangganTransaksi: StateFlow<List<TransaksiDetail>> = combine(
        allTransaksi,
        isKaryawan,
        activeKaryawan,
        scopedPelanggan
    ) { transaksiList, karyawan, kurir, ownedPelanggan ->
        val ownedIds = ownedPelanggan.map { it.id }.toSet()
        KaryawanScopeHelper.filterCustomerHistoryForRole(transaksiList, karyawan, kurir, ownedIds)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scopedPelangganUiModels: StateFlow<List<PelangganUiModel>> = combine(
        scopedPelanggan,
        scopedPelangganTransaksi
    ) { pelangganList, transaksiList ->
        getPelangganUiModels(pelangganList, transaksiList)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Jadwal & Riwayat Perawatan RO (Khusus Pemilik) ---
    val allPerawatanEvaluations: StateFlow<List<PerawatanEvaluation>> = combine(
        repository.allPerawatan,
        isKaryawan
    ) { items, karyawan ->
        if (karyawan) emptyList() else PerawatanScheduler.evaluateAndSort(items)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val perawatanReminders: StateFlow<List<PerawatanEvaluation>> = combine(
        allPerawatanEvaluations,
        isKaryawan
    ) { evaluations, karyawan ->
        if (karyawan) emptyList() else PerawatanScheduler.filterDashboardReminders(evaluations)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPerawatanLogs: StateFlow<List<PerawatanLog>> = combine(
        repository.allPerawatanLogs,
        isKaryawan
    ) { logs, karyawan ->
        if (karyawan) emptyList() else logs
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val roCalcPrefs by lazy {
        getApplication<Application>().getSharedPreferences("santoya_ro_calc_prefs", Context.MODE_PRIVATE)
    }

    private val crmReminderPrefs by lazy {
        getApplication<Application>().getSharedPreferences("santoya_crm_reminder_prefs", Context.MODE_PRIVATE)
    }

    private val _crmContactedTimestamps = MutableStateFlow(loadCrmContactedTimestamps())
    val crmContactedTimestamps: StateFlow<Map<Int, Long>> = _crmContactedTimestamps.asStateFlow()

    private val _roMembraneSubTab = MutableStateFlow(0)
    val roMembraneSubTab: StateFlow<Int> = _roMembraneSubTab.asStateFlow()

    private val _roMembraneUiState = MutableStateFlow(loadSavedRoMembraneState())
    val roMembraneUiState: StateFlow<RoMembraneUiState> = _roMembraneUiState.asStateFlow()

    private val _cipCalculatorUiState = MutableStateFlow(buildInitialCipState(_roMembraneUiState.value))
    val cipCalculatorUiState: StateFlow<CipCalculatorUiState> = _cipCalculatorUiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            combine(
                repository.allPelanggan,
                repository.allProduk,
                repository.allTransaksiWithDetail
            ) { _, _, _ -> true }.first()
            _isInitialDataLoaded.value = true
        }
        viewModelScope.launch {
            syncEngine.offlineMergeMessage.collect { message ->
                showSnackbar(message)
            }
        }
    }

    val kasbonDiscrepancies: StateFlow<List<KasbonDiscrepancy>> = combine(
        allPelanggan,
        allTransaksi,
        allKasbonLogs
    ) { pelangganList, transaksiList, kasbonLogs ->
        val activeTxByCust = transaksiList.asSequence()
            .filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
            .groupBy { it.transaksi.id_pelanggan }
        val custIdsWithLogs = kasbonLogs.asSequence()
            .filter { it.jumlah_bayar > 0.0 }
            .map { it.id_pelanggan }
            .toSet()

        pelangganList.mapNotNull { p ->
            val custTxs = activeTxByCust[p.id].orEmpty()
            val unpaidSum = custTxs
                .filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
                .sumOf { it.transaksi.remainingKasbonAmount() }
            val targetKasbon = p.sisa_kasbon.coerceAtLeast(0.0)
            val hasSettledHistory = p.id in custIdsWithLogs || custTxs.any {
                it.transaksi.status.equals("Lunas", ignoreCase = true) &&
                    it.transaksi.tanggalBayar != null &&
                    it.transaksi.tanggalBayar != it.transaksi.tanggal
            }

            if (unpaidSum > targetKasbon || (targetKasbon > unpaidSum && (hasSettledHistory || unpaidSum > 0.0))) {
                KasbonDiscrepancy(
                    pelangganId = p.id,
                    namaPelanggan = p.nama,
                    saldoTercatat = targetKasbon,
                    totalNotaBelumLunas = unpaidSum,
                    selisih = kotlin.math.abs(unpaidSum - targetKasbon)
                )
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun reconcileKasbonDiscrepancies(onFinished: ((Int) -> Unit)? = null) {
        launchGuardedAction("Merapikan nota kasbon...") {
            val count = repository.reconcileKasbonDiscrepancies()
            if (count > 0) {
                showSnackbar("Nota kasbon berhasil dirapikan ($count pelanggan)")
            } else {
                showSnackbar("Semua saldo kasbon dan nota sudah sesuai")
            }
            onFinished?.invoke(count)
        }
    }

    fun startRealtimeSync(uid: String, email: String?, displayName: String? = null) {
        val resolvedUser = try {
            Firebase.auth.currentUser
        } catch (_: Exception) {
            null
        }
        _activeAuthUid.value = uid
        _currentUserEmail.value = email ?: resolvedUser?.email
        val resolvedDisplayName = displayName?.takeIf { it.isNotBlank() } ?: resolvedUser?.displayName
        syncEngine.start(uid, email, resolvedDisplayName)
    }

    /**
     * Mengembalikan true apabila Mode Offline lokal tersimpan aktif di perangkat.
     */
    fun isPersistedOfflineModeActive(): Boolean = syncEngine.offlineSessionManager.isOfflineModeActive()

    /**
     * Mengembalikan metadata sesi terakhir untuk ditampilkan di layar masuk.
     */
    fun getLastSessionInfo(): LastSessionInfo? = syncEngine.offlineSessionManager.getLastSession()

    /**
     * Mengaktifkan Mode Offline lokal sebagai Pemilik depot tanpa autentikasi Google.
     */
    fun enterOfflineLocalMode(displayName: String? = null) {
        _activeAuthUid.value = OfflineSessionManager.OFFLINE_LOCAL_UID
        _currentUserEmail.value = null
        syncEngine.startOfflineLocal(displayName)
    }

    /**
     * Keluar dari sesi Mode Offline kembali ke layar masuk tanpa menghapus data lokal yang tersimpan.
     */
    fun exitOfflineSessionKeepLocalData() {
        syncEngine.offlineSessionManager.setOfflineModeActive(false)
        _currentScreen.value = SantoyaScreen.DASHBOARD
        stopRealtimeSync()
    }

    /**
     * Membuka bottom sheet detail AI Business Insights (khusus Pemilik Depot).
     */
    fun openAiInsightsSheet() {
        val app = getApplication<Application>()
        if (isUserKaryawan() || !depotProfile.value.isOwnerRole()) {
            showSnackbar(app.getString(com.example.R.string.ai_insights_owner_only_error))
            return
        }
        _showAiInsightsSheet.value = true
    }

    /**
     * Menutup bottom sheet detail AI Business Insights.
     */
    fun dismissAiInsightsSheet() {
        _showAiInsightsSheet.value = false
    }

    /**
     * Mengubah rentang evaluasi analisis AI (7, 30, atau 90 hari) dan opsional langsung menganalisis ulang.
     */
    fun selectAiPeriodDays(days: Int, autoAnalyze: Boolean = true) {
        val sanitized = com.example.util.AiBusinessSummaryBuilder.sanitizePeriodDays(days)
        val changed = _selectedAiPeriodDays.value != sanitized
        _selectedAiPeriodDays.value = sanitized
        _aiAnalyticsError.value = null
        if (autoAnalyze && (changed || aiAnalyticsUiState.value !is AiAnalyticsUiState.Success)) {
            analyzeBusinessWithAi(sanitized)
        }
    }

    /**
     * Menghitung ringkasan data penjualan, kasbon, laba bersih, stok toren, dan perawatan RO dari cache lokal/Cloud,
     * memanggil Gemini melalui Firebase AI Logic, lalu menyimpan insight terbaru ke `/ai_insights/latest_{uid}`.
     */
    fun analyzeBusinessWithAi(periodDays: Int = _selectedAiPeriodDays.value) {
        val app = getApplication<Application>()
        if (isUserKaryawan() || !depotProfile.value.isOwnerRole()) {
            showSnackbar(app.getString(com.example.R.string.ai_insights_owner_only_error))
            return
        }
        val uid = _activeAuthUid.value ?: try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        }
        if (uid.isNullOrBlank() || uid == OfflineSessionManager.OFFLINE_LOCAL_UID) {
            val msg = app.getString(com.example.R.string.ai_insights_auth_required_error)
            _aiAnalyticsError.value = msg
            showSnackbar(msg)
            return
        }
        if (!aiAnalysisGuard.compareAndSet(false, true)) return

        val safePeriodDays = com.example.util.AiBusinessSummaryBuilder.sanitizePeriodDays(periodDays)
        _selectedAiPeriodDays.value = safePeriodDays
        _aiAnalyticsError.value = null
        _isAnalyzingAi.value = true
        aiAnalysisJob = viewModelScope.launch {
            try {
                val result = aiAnalyticsRepository.analyzeAndSaveLatestInsight(
                    uid = uid,
                    fallbackDepotProfile = depotProfile.value,
                    periodDays = safePeriodDays
                )
                result.onSuccess { outcome ->
                    _aiAnalyticsError.value = null
                    _localAiInsightFallback.value = outcome.insight to !outcome.savedToCloud
                    if (!outcome.savedToCloud) {
                        showSnackbar(app.getString(com.example.R.string.ai_insights_offline_saved_badge))
                    }
                }.onFailure { err ->
                    android.util.Log.e("SantoyaVM", "AI analytics failed: ${err.message}", err)
                    val errMsg = mapAiAnalyticsError(err)
                    _aiAnalyticsError.value = errMsg
                    showSnackbar(errMsg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SantoyaVM", "AI analytics unexpected exception: ${e.message}", e)
                val errMsg = mapAiAnalyticsError(e)
                _aiAnalyticsError.value = errMsg
                showSnackbar(errMsg)
            } finally {
                _isAnalyzingAi.value = false
                aiAnalysisGuard.set(false)
            }
        }
    }

    private fun mapAiAnalyticsError(throwable: Throwable?): String {
        val app = getApplication<Application>()
        if (throwable is InsufficientAiDataException) {
            return throwable.message?.takeIf { it.isNotBlank() }
                ?: app.getString(com.example.R.string.ai_insights_empty_data_error)
        }
        if (throwable is TimeoutCancellationException) {
            return app.getString(com.example.R.string.ai_insights_timeout_error)
        }
        val raw = generateSequence(throwable) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" | ")
        return when {
            raw.contains("timed out", ignoreCase = true) ||
                raw.contains("timeout", ignoreCase = true) ->
                app.getString(com.example.R.string.ai_insights_timeout_error)
            raw.contains("App Check", ignoreCase = true) ||
                raw.contains("AppCheck", ignoreCase = true) ||
                raw.contains("403", ignoreCase = true) ||
                raw.contains("attestation", ignoreCase = true) ->
                app.getString(com.example.R.string.ai_insights_appcheck_error)
            raw.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                raw.contains("quota", ignoreCase = true) ||
                raw.contains("429", ignoreCase = true) ->
                app.getString(com.example.R.string.ai_insights_quota_error)
            raw.contains("PERMISSION_DENIED", ignoreCase = true) ->
                app.getString(com.example.R.string.error_permission_denied)
            raw.contains("UNAVAILABLE", ignoreCase = true) ||
                raw.contains("network", ignoreCase = true) ||
                raw.contains("UnknownHost", ignoreCase = true) ||
                raw.contains("ConnectException", ignoreCase = true) ->
                app.getString(com.example.R.string.error_network_disconnected)
            else -> app.getString(com.example.R.string.ai_insights_generic_error)
        }
    }

    /**
     * Menyimpan perubahan profil pemilik dan identitas depot ke penyimpanan lokal dan Cloud.
     */
    fun saveDepotProfile(
        ownerName: String,
        depotName: String,
        phoneNumber: String,
        address: String,
        receiptFooter: String,
        depotType: String? = null,
        tankCapacityLiters: Double? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (isUserKaryawan()) {
            showSnackbar(getApplication<Application>().getString(com.example.R.string.depot_profile_owner_only_error))
            onComplete?.invoke(false)
            return
        }
        val cleanOwner = ownerName.trim()
        if (cleanOwner.isEmpty()) {
            showSnackbar(getApplication<Application>().getString(com.example.R.string.depot_profile_owner_required))
            onComplete?.invoke(false)
            return
        }
        launchGuardedAction(getApplication<Application>().getString(com.example.R.string.depot_profile_saving)) {
            val res = syncEngine.saveDepotProfile(
                ownerName = cleanOwner,
                depotName = depotName.trim(),
                phoneNumber = phoneNumber.trim(),
                address = address.trim(),
                receiptFooter = receiptFooter.trim(),
                depotType = depotType,
                tankCapacityLiters = tankCapacityLiters
            )
            res.onSuccess { updated ->
                _receiptDialog.value?.let { activeDialog ->
                    activeDialog.receiptBuilder?.let { builder ->
                        _receiptDialog.value = activeDialog.copy(receiptText = builder(updated))
                    }
                }
                showSnackbar(getApplication<Application>().getString(com.example.R.string.depot_profile_save_success))
                onComplete?.invoke(true)
            }.onFailure {
                showSnackbar(getApplication<Application>().getString(com.example.R.string.depot_profile_save_error))
                onComplete?.invoke(false)
            }
        }
    }

    /**
     * Mencatat pembelian air tangki curah untuk pengisian toren air mineral,
     * otomatis menambah sisa air toren, dan opsional mencatat ke pengeluaran.
     */
    fun catatBeliAirTangki(
        volumeLiter: Double,
        totalHarga: Double,
        namaPemasok: String = "",
        catatan: String = "",
        tanggal: Long = System.currentTimeMillis(),
        catatKePengeluaran: Boolean = true
    ) {
        if (_isBusyAction.value) return
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mencatat pembelian tangki.")
            return
        }
        if (volumeLiter <= 0.0) {
            showSnackbar("Volume air harus lebih dari 0 Liter")
            return
        }
        val safeVolume = volumeLiter.coerceAtLeast(0.0)
        val safeHarga = totalHarga.coerceAtLeast(0.0)

        launchGuardedAction("Menyimpan pengisian tangki...") {
            try {
                var linkedExpenseId: Int? = null
                var pengeluaranSyncId: String? = null

                if (catatKePengeluaran && safeHarga > 0.0) {
                    val namaItemExp = if (namaPemasok.isNotBlank()) {
                        "Beli Air Tangki ($namaPemasok - ${FormatUtils.formatNumber(safeVolume)} L)"
                    } else {
                        "Beli Air Tangki (${FormatUtils.formatNumber(safeVolume)} L)"
                    }
                    val expenseEntity = Pengeluaran(
                        nama_item = namaItemExp,
                        kategori = "Beli Air Tangki",
                        nominal = safeHarga,
                        tanggal = tanggal
                    )
                    val insertedExpId = repository.insertPengeluaran(expenseEntity)
                    linkedExpenseId = insertedExpId.toInt()
                    pengeluaranSyncId = expenseEntity.syncId
                }

                val now = System.currentTimeMillis()
                val refillEntity = TangkiRefill(
                    tanggal = tanggal,
                    volumeLiter = safeVolume,
                    totalHarga = safeHarga,
                    namaPemasok = namaPemasok.trim(),
                    catatan = catatan.trim(),
                    linkedExpenseId = linkedExpenseId,
                    pengeluaranSyncId = pengeluaranSyncId,
                    updatedAt = now
                )
                repository.recordTangkiRefill(refillEntity)

                // Tambahkan sisa air toren secara lokal dan di cloud
                val currentUid = depotProfile.value.uid
                syncEngine.profileStore.adjustTankStock(currentUid, safeVolume)
                syncEngine.adjustTankStockCloud(safeVolume)
                syncEngine.scheduleFlush()

                showSnackbar("Stok bertambah ${FormatUtils.formatNumber(safeVolume)} L")
            } catch (e: Exception) {
                showSnackbar("Gagal menyimpan pengisian tangki")
            }
        }
    }

    /**
     * Menghapus riwayat pembelian air tangki, mengembalikan sisa air toren,
     * serta otomatis menghapus entri pengeluaran yang terhubung.
     */
    fun hapusBeliAirTangki(refill: TangkiRefill) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menghapus riwayat tangki.")
            return
        }
        launchGuardedAction("Menghapus riwayat tangki...") {
            try {
                // Kurangi stok toren kembali sebesar volume refill
                val currentUid = depotProfile.value.uid
                syncEngine.profileStore.adjustTankStock(currentUid, -refill.volumeLiter)
                syncEngine.adjustTankStockCloud(-refill.volumeLiter)

                // Hapus pengeluaran terkait jika ada
                if (refill.linkedExpenseId != null) {
                    val exp = db.pengeluaranDao().getById(refill.linkedExpenseId)
                    if (exp != null) {
                        repository.deletePengeluaran(exp)
                    }
                } else if (!refill.pengeluaranSyncId.isNullOrBlank()) {
                    val exp = db.pengeluaranDao().getBySyncId(refill.pengeluaranSyncId)
                    if (exp != null) {
                        repository.deletePengeluaran(exp)
                    }
                }

                repository.deleteTangkiRefill(refill)
                syncEngine.scheduleFlush()

                showSnackbar("Riwayat tangki dihapus & stok disesuaikan")
            } catch (e: Exception) {
                showSnackbar("Gagal menghapus riwayat tangki")
            }
        }
    }

    /**
     * Menyesuaikan sisa air saat ini dan kapasitas total toren tanpa tercatat sebagai pembelian atau pengeluaran.
     */
    fun sesuaikanStokToren(newStock: Double, newCapacity: Double? = null) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menyesuaikan stok toren.")
            return
        }
        viewModelScope.launch {
            val currentUid = depotProfile.value.uid
            val updated = syncEngine.profileStore.setTankStock(currentUid, newStock, newCapacity)
            syncEngine.setTankStockCloud(newStock, newCapacity)
            showSnackbar("Stok toren disesuaikan: ${FormatUtils.formatNumber(updated.currentTankStockLiters)} L")
        }
    }

    /**
     * Menguras toren (reset stok menjadi 0 Liter) tanpa tercatat sebagai transaksi atau pengeluaran.
     */
    fun kurasToren() {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menguras toren.")
            return
        }
        viewModelScope.launch {
            val currentUid = depotProfile.value.uid
            syncEngine.profileStore.kurasToren(currentUid)
            syncEngine.setTankStockCloud(0.0)
            showSnackbar("Toren telah dikuras (Stok 0 L)")
        }
    }

    /**
     * Menyimpan cepat Nama, Nomor WA/Telp, & Alamat Depot saat mencetak/membagikan nota atau ekspor PDF
     * tanpa menunda alur kerja pengguna.
     */
    fun quickSaveDepotProfile(
        depotName: String,
        address: String,
        phoneNumber: String = depotProfile.value.phoneNumber,
        onComplete: (DepotProfile) -> Unit
    ) {
        val current = depotProfile.value
        val cleanDepotName = depotName.trim()
        val cleanAddress = address.trim()
        val cleanPhone = DepotProfile.normalizePhoneInput(phoneNumber).ifBlank { current.phoneNumber }
        if (isUserKaryawan()) {
            onComplete(current)
            return
        }
        val uid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: current.uid

        val immediateUpdated = syncEngine.profileStore.saveOwnerEdit(
            uid = uid,
            ownerName = current.effectiveOwnerName(),
            depotName = cleanDepotName,
            phoneNumber = cleanPhone,
            address = cleanAddress,
            receiptFooter = current.receiptFooter
        )
        _receiptDialog.value?.let { activeDialog ->
            activeDialog.receiptBuilder?.let { builder ->
                _receiptDialog.value = activeDialog.copy(receiptText = builder(immediateUpdated))
            }
        }
        onComplete(immediateUpdated)

        viewModelScope.launch {
            syncEngine.saveDepotProfile(
                ownerName = immediateUpdated.effectiveOwnerName(),
                depotName = cleanDepotName,
                phoneNumber = cleanPhone,
                address = cleanAddress,
                receiptFooter = immediateUpdated.receiptFooter
            )
        }
        showSnackbar(getApplication<Application>().getString(com.example.R.string.depot_profile_save_success))
    }

    fun stopRealtimeSync() {
        _activeAuthUid.value = null
        _currentUserEmail.value = null
        _isAnalyzingAi.value = false
        _aiAnalyticsError.value = null
        _localAiInsightFallback.value = null
        _showAiInsightsSheet.value = false
        _selectedAiPeriodDays.value = AiBusinessInsight.DEFAULT_PERIOD_DAYS
        aiAnalysisJob?.cancel()
        aiAnalysisJob = null
        aiAnalysisGuard.set(false)
        syncEngine.stop()
    }

    /**
     * Mengonfirmasi bahwa akun yang sedang login adalah Pemilik Depot baru.
     */
    fun confirmAsNewOwner() {
        syncEngine.confirmAsNewOwner()
        showSnackbar("Depot baru siap digunakan")
    }

    /**
     * Memeriksa ulang apakah email karyawan sudah ditautkan ke depot Pemilik di Cloud.
     */
    fun recheckStaffConnection(onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val connected = syncEngine.recheckStaffConnection()
            if (connected) {
                showSnackbar("Berhasil terhubung ke depot pemilik")
            } else {
                val email = _currentUserEmail.value.orEmpty()
                showSnackbar("Belum ditemukan tautan depot untuk $email. Pastikan Pemilik sudah menyimpan email tersebut.")
            }
            onResult?.invoke(connected)
        }
    }

    /**
     * Mengalihkan akun dari mode Pemilik bayangan ke mode Karyawan, membersihkan klaim tautan bayangan,
     * dan menghubungkan ulang ke depot Pemilik asli.
     */
    fun switchToStaffWaitingMode() {
        viewModelScope.launch {
            val connected = syncEngine.switchToStaffWaitingMode()
            if (connected) {
                showSnackbar("Berhasil berpindah ke depot pemilik")
            } else {
                showSnackbar("Mode karyawan aktif. Menunggu tautan dari HP Pemilik.")
            }
        }
    }

    /**
     * Dipanggil sebelum Firebase.signOut(). Semua data depot di perangkat ini
     * dihapus agar akun berikutnya tidak melihat data pemilik sebelumnya.
     */
    suspend fun clearLocalDataForLogout() {
        _activeAuthUid.value = null
        _currentUserEmail.value = null
        _isAnalyzingAi.value = false
        _aiAnalyticsError.value = null
        _localAiInsightFallback.value = null
        _showAiInsightsSheet.value = false
        _selectedAiPeriodDays.value = AiBusinessInsight.DEFAULT_PERIOD_DAYS
        aiAnalysisJob?.cancel()
        aiAnalysisJob = null
        aiAnalysisGuard.set(false)
        syncEngine.stop()
        try {
            syncEngine.clearLocalData()
        } catch (_: Exception) {
            showSnackbar(getApplication<Application>().getString(com.example.R.string.error_delete_local))
        }
    }

    override fun onCleared() {
        super.onCleared()
        aiAnalysisJob?.cancel()
        syncEngine.stop()
    }

    fun isUserKaryawan(): Boolean {
        if (_activeAuthUid.value == OfflineSessionManager.OFFLINE_LOCAL_UID) {
            return false
        }
        val email = _currentUserEmail.value ?: try {
            Firebase.auth.currentUser?.email
        } catch (_: Exception) {
            null
        }
        return KaryawanScopeHelper.isKaryawanRole(
            syncRole = realtimeSyncState.value.role,
            userEmail = email,
            ownerEmail = realtimeSyncState.value.ownerEmail,
            kurirList = allKurir.value
        )
    }

    private fun resolveActiveKaryawanImmediate(): Kurir? {
        val email = _currentUserEmail.value ?: try {
            Firebase.auth.currentUser?.email
        } catch (_: Exception) {
            null
        }
        return KaryawanScopeHelper.resolveActiveKaryawan(email, allKurir.value)
    }

    private fun isOwnerEmail(email: String): Boolean {
        val normalized = KaryawanScopeHelper.normalizeEmail(email)
        if (normalized.isBlank()) return false
        val ownerEmail = realtimeSyncState.value.ownerEmail.ifBlank {
            _currentUserEmail.value ?: Firebase.auth.currentUser?.email.orEmpty()
        }
        return KaryawanScopeHelper.normalizeEmail(ownerEmail) == normalized
    }

    // --- Transaksi & Screen Tab State ---
    private val _transaksiTab = MutableStateFlow(0)
    val transaksiTab: StateFlow<Int> = _transaksiTab.asStateFlow()
    fun setTransaksiTab(tab: Int) { _transaksiTab.value = tab }

    private val _pelangganTab = MutableStateFlow(0)
    val pelangganTab: StateFlow<Int> = _pelangganTab.asStateFlow()
    fun setPelangganTab(tab: Int) { _pelangganTab.value = tab }

    private val _laporanPeriode = MutableStateFlow(LaporanPeriode.BULAN_INI)
    val laporanPeriode: StateFlow<LaporanPeriode> = _laporanPeriode.asStateFlow()
    fun setLaporanPeriode(periode: LaporanPeriode) { _laporanPeriode.value = periode }

    fun getTotalPendapatanByDateRange(start: Long, end: Long) =
        repository.getTotalPendapatanByDateRange(start, end)

    // --- Transaction Form States ---
    private val _txTanggal = MutableStateFlow(System.currentTimeMillis())
    val txTanggal: StateFlow<Long> = _txTanggal.asStateFlow()

    private val _txPelangganId = MutableStateFlow<Int?>(null)
    val txPelangganId: StateFlow<Int?> = _txPelangganId.asStateFlow()

    private val _txProdukId = MutableStateFlow<Int?>(null)
    val txProdukId: StateFlow<Int?> = _txProdukId.asStateFlow()

    private val _txKurirId = MutableStateFlow<Int?>(null)
    val txKurirId: StateFlow<Int?> = _txKurirId.asStateFlow()

    private val _txJumlah = MutableStateFlow(1)
    val txJumlah: StateFlow<Int> = _txJumlah.asStateFlow()

    private val _txHargaSatuan = MutableStateFlow(6000.0)
    val txHargaSatuan: StateFlow<Double> = _txHargaSatuan.asStateFlow()

    private val _txMetodeBayar = MutableStateFlow("Cash")
    val txMetodeBayar: StateFlow<String> = _txMetodeBayar.asStateFlow()

    private val _txStatus = MutableStateFlow("Lunas")
    val txStatus: StateFlow<String> = _txStatus.asStateFlow()

    private val _txUangMuka = MutableStateFlow(0L)
    val txUangMuka: StateFlow<Long> = _txUangMuka.asStateFlow()

    private val _txWaterType = MutableStateFlow(Transaksi.WATER_TYPE_RO)
    val txWaterType: StateFlow<String> = _txWaterType.asStateFlow()

    fun setTxWaterType(type: String) {
        _txWaterType.value = if (type == Transaksi.WATER_TYPE_MINERAL) Transaksi.WATER_TYPE_MINERAL else Transaksi.WATER_TYPE_RO
    }

    fun setTxTanggal(time: Long) { _txTanggal.value = time }
    fun setTxPelangganId(id: Int?) {
        _txPelangganId.value = id
        val selectedProd = allProduk.value.find { it.id == _txProdukId.value } ?: allProduk.value.firstOrNull()
        val defaultPrice = selectedProd?.harga ?: 6000.0
        val karyawanKurirId = if (isUserKaryawan()) resolveActiveKaryawanImmediate()?.id else null
        if (id != null) {
            val cust = allPelanggan.value.find { it.id == id }
            if (cust != null) {
                _txHargaSatuan.value = KaryawanScopeHelper.resolveAuthoritativeUnitPrice(
                    pelanggan = cust,
                    produk = selectedProd,
                    fallbackPrice = defaultPrice
                )
                _txKurirId.value = karyawanKurirId ?: cust.id_kurir_langganan
            }
        } else {
            _txHargaSatuan.value = defaultPrice
            _txKurirId.value = karyawanKurirId
        }
    }
    fun onPelangganSelected(id: Int?) = setTxPelangganId(id)
    fun setTxProdukId(id: Int?, defaultPrice: Double? = null) {
        _txProdukId.value = id
        val prod = allProduk.value.find { it.id == id }
        if (prod != null) {
            if (prod.nama_produk.contains("mineral", ignoreCase = true)) {
                _txWaterType.value = Transaksi.WATER_TYPE_MINERAL
            } else if (prod.nama_produk.contains("ro", ignoreCase = true)) {
                _txWaterType.value = Transaksi.WATER_TYPE_RO
            }
        }
        val activeCust = _txPelangganId.value?.let { custId ->
            allPelanggan.value.find { it.id == custId }
        }
        val fallback = if (isUserKaryawan()) {
            prod?.harga ?: defaultPrice ?: 6000.0
        } else {
            defaultPrice ?: prod?.harga ?: 6000.0
        }
        _txHargaSatuan.value = KaryawanScopeHelper.resolveAuthoritativeUnitPrice(
            pelanggan = activeCust,
            produk = prod,
            fallbackPrice = fallback
        )
    }
    fun setTxKurirId(id: Int?) {
        val karyawanKurirId = if (isUserKaryawan()) resolveActiveKaryawanImmediate()?.id else null
        _txKurirId.value = karyawanKurirId ?: id
    }
    fun setTxJumlah(jumlah: Int) { _txJumlah.value = jumlah.coerceAtLeast(1) }
    fun incrementTxJumlah() { _txJumlah.update { it + 1 } }
    fun decrementTxJumlah() { _txJumlah.update { (it - 1).coerceAtLeast(1) } }
    fun setTxHargaSatuan(harga: Double) {
        if (isUserKaryawan()) {
            val activeCust = _txPelangganId.value?.let { cid -> allPelanggan.value.find { it.id == cid } }
            val prod = _txProdukId.value?.let { pid -> allProduk.value.find { it.id == pid } } ?: allProduk.value.firstOrNull()
            _txHargaSatuan.value = KaryawanScopeHelper.resolveAuthoritativeUnitPrice(
                pelanggan = activeCust,
                produk = prod,
                fallbackPrice = _txHargaSatuan.value.takeIf { it > 0.0 } ?: 6000.0
            )
            return
        }
        _txHargaSatuan.value = harga.coerceAtLeast(0.0)
    }
    fun setTxUangMuka(nominal: Long) { _txUangMuka.value = nominal.coerceAtLeast(0L) }

    fun setTxMetodeBayar(metode: String) {
        _txMetodeBayar.value = metode
        if (metode.equals("Bayar Sebagian", ignoreCase = true)) {
            _txStatus.value = "Kasbon"
        } else if (metode.isNotBlank()) {
            _txStatus.value = "Lunas"
            _txUangMuka.value = 0L
        }
    }

    fun setTxStatus(status: String) {
        _txStatus.value = status
        if (status.equals("Kasbon", ignoreCase = true)) {
            _txMetodeBayar.value = ""
            _txUangMuka.value = 0L
        } else if (status.equals("Lunas", ignoreCase = true)) {
            if (_txMetodeBayar.value.isBlank() || _txMetodeBayar.value.equals("Bayar Sebagian", ignoreCase = true)) {
                _txMetodeBayar.value = "Cash"
            }
            _txUangMuka.value = 0L
        }
    }

    fun startQuickOrder(
        pelangganId: Int,
        produkId: Int? = null,
        jumlah: Int = 1
    ) {
        _txTanggal.value = System.currentTimeMillis()
        if (produkId != null && allProduk.value.any { it.id == produkId }) {
            _txProdukId.value = produkId
        } else if (_txProdukId.value == null) {
            _txProdukId.value = allProduk.value.firstOrNull()?.id
        }
        setTxPelangganId(pelangganId)
        setTxJumlah(jumlah.coerceAtLeast(1))
        setTxMetodeBayar("Cash")
        setTransaksiTab(0)
        navigateTo(SantoyaScreen.TRANSAKSI)
    }

    // --- Feedback / Dialog States ---
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _receiptDialog = MutableStateFlow<ActiveReceiptDialog?>(null)
    val receiptDialog: StateFlow<ActiveReceiptDialog?> = _receiptDialog.asStateFlow()

    fun showReceiptDialog(dialog: ActiveReceiptDialog) { _receiptDialog.value = dialog }
    fun dismissReceiptDialog() { _receiptDialog.value = null }
    fun showSnackbar(message: String) { _snackbarMessage.value = message }
    fun clearSnackbar() { _snackbarMessage.value = null }

    fun createFastCashTransaksi(idProduk: Int, jumlah: Int = 1) {
        launchGuardedAction("Mencatat penjualan cepat...") {
            try {
                val prod = allProduk.value.find { it.id == idProduk }
                val currentDepotType = depotProfile.value.depotType
                val effectiveWaterType = when {
                    currentDepotType == DepotProfile.TYPE_MINERAL -> Transaksi.WATER_TYPE_MINERAL
                    prod?.nama_produk?.contains("mineral", ignoreCase = true) == true -> Transaksi.WATER_TYPE_MINERAL
                    currentDepotType == DepotProfile.TYPE_BOTH -> _txWaterType.value
                    else -> Transaksi.WATER_TYPE_RO
                }
                val karyawanKurirId = if (isUserKaryawan()) resolveActiveKaryawanImmediate()?.id else null
                repository.createFastCashTransaksi(idProduk, jumlah, karyawanKurirId, effectiveWaterType)
                if (effectiveWaterType == Transaksi.WATER_TYPE_MINERAL) {
                    val delta = -(jumlah * TorenWaterCalculator.LITERS_PER_GALLON)
                    val currentUid = depotProfile.value.uid
                    syncEngine.profileStore.adjustTankStock(currentUid, delta)
                    syncEngine.adjustTankStockCloud(delta)
                }
                showSnackbar("Terjual $jumlah galon (Tunai)")
            } catch (_: Exception) {
                showSnackbar(getApplication<Application>().getString(com.example.R.string.error_save_local))
            }
        }
    }

    fun getKasbonLogsByPelanggan(pelangganId: Int): Flow<List<KasbonLog>> =
        repository.getKasbonLogsByPelanggan(pelangganId)

    fun submitTransaksi(onSuccess: (TransaksiDetail?) -> Unit) {
        if (_isBusyAction.value) return
        val pelangganId = _txPelangganId.value
        val produkId = _txProdukId.value
        if (produkId == null) {
            showSnackbar("Pilih produk terlebih dahulu")
            return
        }

        val karyawanMode = isUserKaryawan()
        val activeCust = pelangganId?.let { id -> allPelanggan.value.find { it.id == id } }
        val activeProd = allProduk.value.find { it.id == produkId } ?: allProduk.value.firstOrNull()
        val effectiveHargaSatuan = if (karyawanMode) {
            KaryawanScopeHelper.resolveAuthoritativeUnitPrice(
                pelanggan = activeCust,
                produk = activeProd,
                fallbackPrice = _txHargaSatuan.value.takeIf { it > 0.0 } ?: 6000.0
            )
        } else {
            _txHargaSatuan.value
        }

        val currentDepotType = depotProfile.value.depotType
        val effectiveWaterType = when {
            currentDepotType == DepotProfile.TYPE_MINERAL -> Transaksi.WATER_TYPE_MINERAL
            activeProd?.nama_produk?.contains("mineral", ignoreCase = true) == true -> Transaksi.WATER_TYPE_MINERAL
            currentDepotType == DepotProfile.TYPE_BOTH -> _txWaterType.value
            else -> Transaksi.WATER_TYPE_RO
        }

        val totalBelanjaLong = (_txJumlah.value * effectiveHargaSatuan).toLong()
        val isBayarSebagian = _txMetodeBayar.value.equals("Bayar Sebagian", ignoreCase = true)
        val dpInput = _txUangMuka.value

        if (isBayarSebagian) {
            if (dpInput <= 0L) {
                showSnackbar("Uang muka tidak boleh Rp 0")
                return
            }
            if (dpInput > totalBelanjaLong) {
                showSnackbar("Uang muka melebihi total belanja")
                return
            }
        }

        val isFullPaidByDp = isBayarSebagian && dpInput >= totalBelanjaLong
        val statusVal = when {
            isFullPaidByDp -> "Lunas"
            isBayarSebagian -> "Kasbon"
            else -> _txStatus.value
        }
        if (pelangganId == null && statusVal.equals("Kasbon", ignoreCase = true)) {
            showSnackbar("Pilih pelanggan terlebih dahulu")
            return
        }
        val metodeVal = when {
            isFullPaidByDp -> "Cash"
            isBayarSebagian -> "Bayar Sebagian"
            statusVal.equals("Kasbon", ignoreCase = true) -> "-"
            else -> _txMetodeBayar.value.ifBlank { "Cash" }
        }
        val uangMukaVal = when {
            isFullPaidByDp -> totalBelanjaLong
            isBayarSebagian -> dpInput
            else -> 0L
        }
        val sisaKasbonVal = when {
            isFullPaidByDp -> 0L
            isBayarSebagian -> (totalBelanjaLong - dpInput).coerceAtLeast(0L)
            statusVal.equals("Kasbon", ignoreCase = true) -> totalBelanjaLong.coerceAtLeast(0L)
            else -> 0L
        }

        val karyawanKurirId = if (karyawanMode) resolveActiveKaryawanImmediate()?.id else null
        val effectiveKurirId = karyawanKurirId ?: _txKurirId.value

        launchGuardedAction("Menyimpan nota...") {
            val resolvedPelangganId = pelangganId ?: repository.ensureGeneralCustomer().id
            val newId = repository.recordTransaksi(
                tanggal = _txTanggal.value,
                idPelanggan = resolvedPelangganId,
                idKurir = effectiveKurirId,
                idProduk = produkId,
                jumlah = _txJumlah.value,
                hargaSatuan = effectiveHargaSatuan,
                metodeBayar = metodeVal,
                status = statusVal,
                uangMuka = uangMukaVal,
                sisaKasbon = sisaKasbonVal,
                waterType = effectiveWaterType
            )
            if (effectiveWaterType == Transaksi.WATER_TYPE_MINERAL) {
                val delta = -(_txJumlah.value * TorenWaterCalculator.LITERS_PER_GALLON)
                val currentUid = depotProfile.value.uid
                syncEngine.profileStore.adjustTankStock(currentUid, delta)
                syncEngine.adjustTankStockCloud(delta)
            }
            val savedDetail = repository.getTransaksiDetailById(newId.toInt())
            showSnackbar("Nota berhasil disimpan")
            val defaultPrice = allProduk.value.find { it.id == _txProdukId.value }?.harga
                ?: allProduk.value.firstOrNull()?.harga
                ?: 6000.0
            _txPelangganId.value = null
            _txKurirId.value = if (isUserKaryawan()) resolveActiveKaryawanImmediate()?.id else null
            _txHargaSatuan.value = defaultPrice
            _txJumlah.value = 1
            _txTanggal.value = System.currentTimeMillis()
            _txStatus.value = "Lunas"
            _txMetodeBayar.value = "Cash"
            _txUangMuka.value = 0L
            onSuccess(savedDetail)
        }
    }

    fun onSaveTransaksi(
        idPelanggan: Int,
        idProduk: Int,
        jumlah: Int,
        hargaSatuan: Double,
        metodeBayar: String,
        uangMuka: Long = 0L,
        idKurir: Int? = null,
        onSuccess: (TransaksiDetail?) -> Unit = {}
    ) {
        setTxPelangganId(idPelanggan)
        setTxProdukId(idProduk, hargaSatuan)
        if (idKurir != null) setTxKurirId(idKurir)
        setTxJumlah(jumlah)
        setTxHargaSatuan(hargaSatuan)
        setTxMetodeBayar(metodeBayar)
        setTxUangMuka(uangMuka)
        submitTransaksi(onSuccess)
    }

    fun markTransaksiLunas(transaksiId: Int, metodeBayar: String = "Cash") {
        launchGuardedAction("Menandai lunas...") {
            repository.markTransaksiLunas(transaksiId, metodeBayar)
            showSnackbar("Nota ditandai lunas (${FormatUtils.formatMetodeBayarLabel(metodeBayar)})")
        }
    }

    fun deleteTransaksi(transaksi: Transaksi) {
        launchGuardedAction("Menghapus nota...") {
            if (isUserKaryawan()) {
                val karyawanName = resolveActiveKaryawanImmediate()?.nama ?: "Karyawan"
                repository.softDeleteTransaksiByKaryawan(transaksi, karyawanName)
                showSnackbar("Nota dihapus (dicatat oleh $karyawanName)")
            } else {
                repository.deleteTransaksi(transaksi)
                showSnackbar("Nota dihapus")
            }
            val delta = TorenWaterCalculator.calculateDeltaStockForTransaksi(transaksi, null)
            if (delta != 0.0) {
                val currentUid = depotProfile.value.uid
                syncEngine.profileStore.adjustTankStock(currentUid, delta)
                syncEngine.adjustTankStockCloud(delta)
            }
        }
    }

    fun restoreTransaksi(transaksi: Transaksi) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat memulihkan nota.")
            return
        }
        launchGuardedAction("Memulihkan nota...") {
            repository.restoreTransaksi(transaksi)
            val delta = TorenWaterCalculator.calculateDeltaStockForTransaksi(null, transaksi)
            if (delta != 0.0) {
                val currentUid = depotProfile.value.uid
                syncEngine.profileStore.adjustTankStock(currentUid, delta)
                syncEngine.adjustTankStockCloud(delta)
            }
            showSnackbar("Nota berhasil dipulihkan")
        }
    }

    fun permanentDeleteTransaksi(transaksi: Transaksi) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menghapus permanen.")
            return
        }
        launchGuardedAction("Menghapus permanen...") {
            repository.permanentDeleteTransaksi(transaksi)
            showSnackbar("Nota dihapus permanen")
        }
    }

    fun updateTransaksi(
        id: Int,
        tanggal: Long,
        idPelanggan: Int,
        idKurir: Int?,
        idProduk: Int?,
        jumlah: Int,
        hargaSatuan: Double,
        metodeBayar: String,
        status: String,
        uangMuka: Long = 0L,
        waterType: String? = null,
        onSuccess: () -> Unit
    ) {
        val karyawanMode = isUserKaryawan()
        val karyawanKurirId = if (karyawanMode) resolveActiveKaryawanImmediate()?.id else null
        val effectiveKurirId = karyawanKurirId ?: idKurir
        val effectiveHargaSatuan = if (karyawanMode) {
            val cust = allPelanggan.value.find { it.id == idPelanggan }
            val prod = idProduk?.let { pid -> allProduk.value.find { it.id == pid } } ?: allProduk.value.firstOrNull()
            KaryawanScopeHelper.resolveAuthoritativeUnitPrice(
                pelanggan = cust,
                produk = prod,
                fallbackPrice = hargaSatuan.takeIf { it > 0.0 } ?: 6000.0
            )
        } else {
            hargaSatuan
        }
        launchGuardedAction("Memperbarui nota...") {
            val oldTx = db.transaksiDao().getDetailById(id)?.transaksi
            repository.updateTransaksi(
                id = id,
                tanggal = tanggal,
                idPelanggan = idPelanggan,
                idKurir = effectiveKurirId,
                idProduk = idProduk,
                jumlah = jumlah,
                hargaSatuan = effectiveHargaSatuan,
                metodeBayar = metodeBayar,
                status = status,
                uangMuka = uangMuka,
                waterType = waterType
            )
            val newTx = db.transaksiDao().getDetailById(id)?.transaksi
            val delta = TorenWaterCalculator.calculateDeltaStockForTransaksi(oldTx, newTx)
            if (delta != 0.0) {
                val currentUid = depotProfile.value.uid
                syncEngine.profileStore.adjustTankStock(currentUid, delta)
                syncEngine.adjustTankStockCloud(delta)
            }
            showSnackbar("Nota diperbarui")
            onSuccess()
        }
    }

    fun addPelanggan(
        nama: String,
        alamat: String,
        noHp: String,
        initialKasbon: Double = 0.0,
        hargaKhusus: Double? = null,
        idKurirLangganan: Int? = null
    ) {
        if (nama.isBlank()) return
        val karyawanMode = isUserKaryawan()
        val karyawanKurirId = if (karyawanMode) resolveActiveKaryawanImmediate()?.id else null
        val effectiveKurirLangganan = karyawanKurirId ?: idKurirLangganan
        val effectiveHargaKhusus = if (karyawanMode) null else PelangganAuditHelper.normalizeSpecialPrice(hargaKhusus)
        launchGuardedAction("Menambahkan pelanggan...") {
            val id = repository.insertPelanggan(
                Pelanggan(
                    nama = nama.trim(),
                    alamat = alamat.trim(),
                    no_hp = noHp.trim(),
                    sisa_kasbon = initialKasbon.coerceAtLeast(0.0),
                    harga_khusus = effectiveHargaKhusus,
                    id_kurir_langganan = effectiveKurirLangganan
                )
            )
            _txPelangganId.value = id.toInt()
            val defaultPrice = allProduk.value.find { it.id == _txProdukId.value }?.harga
                ?: allProduk.value.firstOrNull()?.harga
                ?: 6000.0
            _txHargaSatuan.value = if (effectiveHargaKhusus != null && effectiveHargaKhusus > 0.0) {
                effectiveHargaKhusus
            } else {
                defaultPrice
            }
            _txKurirId.value = effectiveKurirLangganan
            showSnackbar("Pelanggan ${nama.trim()} ditambahkan")
        }
    }

    fun updatePelanggan(pelanggan: Pelanggan) {
        val karyawanMode = isUserKaryawan()
        val karyawanKurirId = if (karyawanMode) resolveActiveKaryawanImmediate()?.id else null
        val existingHargaKhusus = if (karyawanMode) {
            PelangganAuditHelper.normalizeSpecialPrice(allPelanggan.value.find { it.id == pelanggan.id }?.harga_khusus)
        } else {
            PelangganAuditHelper.normalizeSpecialPrice(pelanggan.harga_khusus)
        }
        val effectivePelanggan = if (karyawanMode) {
            pelanggan.copy(
                id_kurir_langganan = karyawanKurirId ?: pelanggan.id_kurir_langganan,
                harga_khusus = existingHargaKhusus
            )
        } else {
            pelanggan.copy(harga_khusus = existingHargaKhusus)
        }
        launchGuardedAction("Memperbarui pelanggan...") {
            repository.updatePelanggan(effectivePelanggan)
            showSnackbar("Data ${effectivePelanggan.nama} diperbarui")
        }
    }

    /**
     * Memperbarui nomor HP pelanggan langsung dari pemilih kontak perangkat dengan mempertahankan
     * nama, alamat, harga khusus, saldo kasbon, dan penugasan kurir yang sudah ada.
     */
    fun updatePelangganNoHp(pelanggan: Pelanggan, rawPhone: String) {
        val normalized = PelangganAuditHelper.formatLocalPhoneNumber(rawPhone)
        if (normalized.isBlank()) {
            showSnackbar("Nomor HP kontak tidak valid")
            return
        }
        val latest = allPelanggan.value.find { it.id == pelanggan.id } ?: pelanggan
        if (KaryawanScopeHelper.isSoftDeletedPelanggan(latest)) return
        val updated = latest.copy(no_hp = normalized)
        launchGuardedAction("Menyimpan nomor HP...") {
            repository.updatePelanggan(updated)
            showSnackbar("Nomor HP ${updated.nama} berhasil ditambahkan")
        }
    }

    fun deletePelanggan(pelanggan: Pelanggan) {
        launchGuardedAction("Menghapus pelanggan...") {
            if (isUserKaryawan()) {
                val karyawanName = resolveActiveKaryawanImmediate()?.nama ?: "Karyawan"
                val ok = repository.softDeletePelangganByKaryawan(pelanggan, karyawanName)
                if (ok) {
                    showSnackbar("${pelanggan.nama} dihapus (dicatat oleh $karyawanName)")
                } else {
                    showSnackbar("Masih ada kasbon aktif. Lunasi dulu sebelum menghapus.")
                }
            } else {
                val ok = repository.deletePelanggan(pelanggan)
                if (ok) {
                    showSnackbar("Pelanggan ${pelanggan.nama} dihapus")
                } else {
                    showSnackbar("Masih ada kasbon aktif. Lunasi dulu sebelum menghapus.")
                }
            }
        }
    }

    fun restorePelanggan(pelanggan: Pelanggan) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat memulihkan pelanggan.")
            return
        }
        launchGuardedAction("Memulihkan pelanggan...") {
            repository.restorePelanggan(pelanggan)
            showSnackbar("Pelanggan ${pelanggan.nama} dipulihkan")
        }
    }

    fun permanentDeletePelanggan(pelanggan: Pelanggan) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menghapus permanen.")
            return
        }
        launchGuardedAction("Menghapus pelanggan permanen...") {
            repository.permanentlyDeletePelanggan(pelanggan)
            showSnackbar("Pelanggan ${pelanggan.nama} dihapus permanen")
        }
    }

    /**
     * Menyimpan pelunasan atau cicilan kasbon pelanggan dengan pencatatan nominal efektif
     * maksimal sebesar sisa kasbon dan menampilkan informasi kembalian bila berlebih.
     */
    fun bayarKasbon(pelangganId: Int, nominalBayar: Double, metodeBayar: String = "Cash") {
        if (nominalBayar <= 0) return
        val custDebt = allPelanggan.value.find { it.id == pelangganId }?.sisa_kasbon ?: nominalBayar
        val breakdown = PelangganAuditHelper.calculateKasbonPaymentBreakdown(
            sisaKasbon = if (custDebt > 0.0) custDebt else nominalBayar,
            inputNominal = nominalBayar
        )
        launchGuardedAction("Menyimpan pembayaran kasbon...") {
            repository.bayarKasbonPelanggan(pelangganId, nominalBayar, metodeBayar)
            val paidLabel = FormatUtils.formatRupiah(breakdown.effectivePaid)
            val msg = if (breakdown.kembalian > 0.0) {
                "Pembayaran $paidLabel ($metodeBayar) tersimpan • Kembalian ${FormatUtils.formatRupiah(breakdown.kembalian)}"
            } else {
                "Pembayaran $paidLabel ($metodeBayar) tersimpan"
            }
            showSnackbar(msg)
        }
    }

    /**
     * Memilih pelanggan secara langsung pada form kasir dan membuka layar Transaksi.
     */
    fun openTransaksiForPelanggan(pelangganId: Int) {
        onPelangganSelected(pelangganId)
        setTransaksiTab(0)
        navigateTo(SantoyaScreen.TRANSAKSI)
    }

    fun addProduk(nama: String, harga: Double) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mengelola produk.")
            return
        }
        if (nama.isBlank() || harga <= 0) return
        launchGuardedAction("Menambahkan produk...") {
            repository.insertProduk(Produk(nama_produk = nama.trim().take(120), harga = harga))
            showSnackbar("Produk $nama ditambahkan")
        }
    }

    fun updateProduk(produk: Produk) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mengelola produk.")
            return
        }
        launchGuardedAction("Memperbarui produk...") {
            repository.updateProduk(produk.copy(nama_produk = produk.nama_produk.trim().take(120)))
            showSnackbar("Produk ${produk.nama_produk} diperbarui")
        }
    }

    fun deleteProduk(produk: Produk) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mengelola produk.")
            return
        }
        launchGuardedAction("Menghapus produk...") {
            try {
                if (_txProdukId.value == produk.id) {
                    _txProdukId.value = null
                }
                repository.deleteProduk(produk)
                showSnackbar("Produk ${produk.nama_produk} dihapus")
            } catch (_: Exception) {
                showSnackbar(getApplication<Application>().getString(com.example.R.string.error_delete_local))
            }
        }
    }

    fun addKurir(
        nama: String,
        komisi: Double,
        email: String = "",
        aksesAktif: Boolean = true
    ) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mengelola data kurir.")
            return
        }
        val trimmedNama = nama.trim().take(120)
        if (trimmedNama.isBlank()) return
        if (isOwnerEmail(email)) {
            showSnackbar("Email pemilik depot tidak bisa didaftarkan sebagai karyawan.")
            return
        }
        launchGuardedAction("Menambahkan kurir...") {
            repository.insertKurir(
                Kurir(
                    nama = trimmedNama,
                    komisi_per_galon = komisi,
                    email = email.trim().lowercase(),
                    akses_aktif = aksesAktif,
                    skema_komisi = "TETAP",
                    aturan_komisi = ""
                )
            )
            syncEngine.syncStaffLinksNow()
            showSnackbar("Kurir $trimmedNama ditambahkan")
        }
    }

    fun updateKurir(kurir: Kurir) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mengelola data kurir.")
            return
        }
        if (isOwnerEmail(kurir.email)) {
            showSnackbar("Email pemilik depot tidak bisa didaftarkan sebagai karyawan.")
            return
        }
        launchGuardedAction("Memperbarui data kurir...") {
            repository.updateKurir(
                kurir.copy(
                    nama = kurir.nama.trim().take(120),
                    email = kurir.email.trim().lowercase()
                )
            )
            syncEngine.syncStaffLinksNow()
            showSnackbar("Data kurir ${kurir.nama} diperbarui")
        }
    }

    fun deleteKurir(kurir: Kurir) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mengelola data kurir.")
            return
        }
        launchGuardedAction("Menghapus kurir...") {
            repository.deleteKurir(kurir)
            syncEngine.syncStaffLinksNow()
            showSnackbar("Kurir ${kurir.nama} dihapus")
        }
    }

    fun addPengeluaran(namaItem: String, kategori: String, nominal: Double, tanggal: Long = System.currentTimeMillis()) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mencatat pengeluaran.")
            return
        }
        if (namaItem.isBlank() || nominal <= 0) return
        launchGuardedAction("Mencatat pengeluaran...") {
            repository.insertPengeluaran(
                Pengeluaran(
                    tanggal = tanggal,
                    nama_item = namaItem.trim().take(200),
                    kategori = kategori.trim().take(80),
                    nominal = nominal
                )
            )
            showSnackbar("Pengeluaran dicatat")
        }
    }

    fun updatePengeluaran(pengeluaran: Pengeluaran) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mengubah pengeluaran.")
            return
        }
        launchGuardedAction("Memperbarui pengeluaran...") {
            repository.updatePengeluaran(
                pengeluaran.copy(
                    nama_item = pengeluaran.nama_item.trim().take(200),
                    kategori = pengeluaran.kategori.trim().take(80)
                )
            )
            showSnackbar("Pengeluaran diperbarui")
        }
    }

    fun deletePengeluaran(pengeluaran: Pengeluaran) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menghapus pengeluaran.")
            return
        }
        launchGuardedAction("Menghapus pengeluaran...") {
            repository.deletePengeluaran(pengeluaran)
            showSnackbar("Pengeluaran dihapus")
        }
    }

    // --- Aksi Jadwal Perawatan RO (Khusus Pemilik) ---
    fun addPerawatan(nama: String, intervalHari: Int, jadwalPertama: Long? = null) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mengelola jadwal perawatan.")
            return
        }
        val cleanName = nama.trim().take(PerawatanScheduler.MAX_SCHEDULE_NAME_LENGTH)
        if (cleanName.isBlank() || intervalHari < 1) return
        val safeInterval = intervalHari.coerceIn(
            PerawatanScheduler.MIN_INTERVAL_DAYS,
            PerawatanScheduler.MAX_INTERVAL_DAYS
        )
        val nextDue = jadwalPertama ?: PerawatanScheduler.calculateNextDueDateMillis(
            completionDateMillis = System.currentTimeMillis(),
            intervalHari = safeInterval
        )
        launchGuardedAction("Menambahkan jadwal perawatan...") {
            repository.insertPerawatan(
                PerawatanItem(
                    nama = cleanName,
                    intervalHari = safeInterval,
                    terakhirSelesai = null,
                    jadwalBerikutnya = nextDue
                )
            )
            showSnackbar("Jadwal \"$cleanName\" ditambahkan")
        }
    }

    fun updatePerawatan(
        item: PerawatanItem,
        namaBaru: String,
        intervalHariBaru: Int,
        jadwalBerikutnyaBaru: Long? = null
    ) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mengubah jadwal perawatan.")
            return
        }
        val cleanName = namaBaru.trim().take(PerawatanScheduler.MAX_SCHEDULE_NAME_LENGTH)
        if (cleanName.isBlank() || intervalHariBaru < 1) return
        val safeInterval = intervalHariBaru.coerceIn(
            PerawatanScheduler.MIN_INTERVAL_DAYS,
            PerawatanScheduler.MAX_INTERVAL_DAYS
        )
        val updatedDue = jadwalBerikutnyaBaru ?: if (item.terakhirSelesai != null) {
            PerawatanScheduler.calculateNextDueDateMillis(item.terakhirSelesai, safeInterval)
        } else {
            item.jadwalBerikutnya
        }
        launchGuardedAction("Memperbarui jadwal perawatan...") {
            repository.updatePerawatan(
                item.copy(
                    nama = cleanName,
                    intervalHari = safeInterval,
                    jadwalBerikutnya = updatedDue
                )
            )
            showSnackbar("Jadwal \"$cleanName\" diperbarui")
        }
    }

    /**
     * Menunda jadwal perawatan sejumlah hari ke depan (khusus pemilik depot).
     */
    fun snoozePerawatan(item: PerawatanItem, snoozeDays: Int) {
        val newDue = PerawatanScheduler.calculateSnoozedDueDateMillis(
            currentDueMillis = item.jadwalBerikutnya,
            snoozeDays = snoozeDays
        )
        reschedulePerawatan(item = item, newDueMillis = newDue)
    }

    /**
     * Menggeser tanggal jatuh tempo jadwal perawatan ke tanggal tertentu (khusus pemilik depot).
     */
    fun reschedulePerawatan(item: PerawatanItem, newDueMillis: Long) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menggeser jadwal perawatan.")
            return
        }
        launchGuardedAction("Menggeser jadwal perawatan...") {
            val saved = repository.reschedulePerawatan(item, newDueMillis)
            if (saved) {
                showSnackbar("Jadwal \"${item.nama}\" digeser ke ${FormatUtils.formatDateUi(newDueMillis)}")
            } else {
                showSnackbar("Jadwal perawatan tidak ditemukan")
            }
        }
    }

    /**
     * Memulihkan templat standar perawatan mesin RO yang belum ada (khusus pemilik depot).
     */
    fun restoreMissingDefaultPerawatan() {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mengelola jadwal perawatan.")
            return
        }
        launchGuardedAction("Memulihkan templat standar...") {
            val restoredCount = repository.restoreMissingDefaultPerawatan()
            if (restoredCount > 0) {
                showSnackbar("$restoredCount templat jadwal standar ditambahkan")
            } else {
                showSnackbar("Semua templat standar sudah tersedia")
            }
        }
    }

    fun deletePerawatan(item: PerawatanItem) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menghapus jadwal perawatan.")
            return
        }
        launchGuardedAction("Menghapus jadwal perawatan...") {
            repository.deletePerawatan(item)
            showSnackbar("Jadwal \"${item.nama}\" dihapus")
        }
    }

    fun completePerawatan(
        item: PerawatanItem,
        tanggalSelesai: Long,
        biaya: Double,
        catatan: String
    ) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menandai perawatan selesai.")
            return
        }
        launchGuardedAction("Menyimpan penyelesaian perawatan...") {
            val saved = repository.completePerawatan(
                item = item,
                tanggalSelesai = tanggalSelesai,
                biaya = biaya,
                catatan = catatan.trim().take(500)
            )
            if (saved) {
                val msg = if (biaya > 0.0) {
                    "Perawatan \"${item.nama}\" selesai & biaya ${FormatUtils.formatRupiah(biaya)} dicatat ke Pengeluaran"
                } else {
                    "Perawatan \"${item.nama}\" ditandai selesai"
                }
                showSnackbar(msg)
            } else {
                showSnackbar("Jadwal perawatan tidak ditemukan")
            }
        }
    }

    /**
     * Memperbarui riwayat perawatan dan menyelaraskan catatan Pengeluaran terkait (khusus pemilik depot).
     */
    fun updatePerawatanLog(
        log: PerawatanLog,
        tanggalSelesaiBaru: Long,
        biayaBaru: Double,
        catatanBaru: String
    ) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat mengubah riwayat perawatan.")
            return
        }
        val cleanBiaya = biayaBaru.coerceAtLeast(0.0)
        launchGuardedAction("Memperbarui riwayat perawatan...") {
            val saved = repository.updatePerawatanLog(
                log = log,
                tanggalSelesaiBaru = tanggalSelesaiBaru,
                biayaBaru = cleanBiaya,
                catatanBaru = catatanBaru.trim().take(500)
            )
            if (saved) {
                val msg = if (cleanBiaya > 0.0) {
                    "Riwayat \"${log.namaPerawatan}\" diperbarui & diselaraskan ke Pengeluaran (${FormatUtils.formatRupiah(cleanBiaya)})"
                } else {
                    "Riwayat \"${log.namaPerawatan}\" diperbarui"
                }
                showSnackbar(msg)
            } else {
                showSnackbar("Riwayat perawatan tidak ditemukan")
            }
        }
    }

    fun deletePerawatanLog(log: PerawatanLog) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menghapus riwayat perawatan.")
            return
        }
        launchGuardedAction("Menghapus riwayat perawatan...") {
            repository.deletePerawatanLog(log)
            showSnackbar("Riwayat perawatan dihapus")
        }
    }

    /**
     * Memperbarui input TDS Air Baku (ppm) pada kalkulator membran RO.
     */
    fun updateRoTdsBaku(value: String) {
        _roMembraneUiState.update { state ->
            state.copy(
                tdsBakuInput = value,
                tdsBakuError = null,
                tdsJadiError = if (state.tdsJadiError == RoValidationFieldError.TDS_JADI_EXCEEDS_BAKU) {
                    null
                } else {
                    state.tdsJadiError
                },
                result = null,
                isSavedToHistory = false
            )
        }
    }

    /**
     * Memperbarui input TDS Air Jadi (ppm) pada kalkulator membran RO.
     */
    fun updateRoTdsJadi(value: String) {
        _roMembraneUiState.update {
            it.copy(
                tdsJadiInput = value,
                tdsJadiError = null,
                result = null,
                isSavedToHistory = false
            )
        }
    }

    /**
     * Memilih ukuran membran RO (4040, 4021, 8040, atau Rumahan).
     * Jika beralih ke 4040, preset otomatis disetel ke 4040 ULP (2.400 GPD) sebagai standar utama depot AMIU.
     */
    fun selectRoCategory(category: RoMembraneCategory) {
        _roMembraneUiState.update { state ->
            if (state.selectedCategory == category) return@update state
            val defaultPreset = if (category == RoMembraneCategory.SIZE_4040) {
                RoMembraneCalculator.findPresetById("4040_ulp")
            } else null
            val updated = state.copy(
                selectedCategory = category,
                selectedPresetId = defaultPreset?.id,
                gpdPerUnitInput = defaultPreset?.gpdPerUnit?.toString() ?: "",
                gpdError = null,
                flowAktualError = null,
                result = null,
                isSavedToHistory = false
            )
            persistRoMembraneConfig(updated)
            syncCipFromRoMembraneConfig(category = updated.selectedCategory, parallelCount = updated.parallelCount)
            updated
        }
    }

    /**
     * Mengatur jumlah membran paralel pada kalkulator kesehatan membran RO.
     */
    fun updateRoParallelCount(count: Int) {
        val clamped = count.coerceIn(
            RoMembraneCalculator.MIN_PARALLEL_MEMBRANES,
            RoMembraneCalculator.MAX_PARALLEL_MEMBRANES
        )
        _roMembraneUiState.update { state ->
            val updated = state.copy(
                parallelCount = clamped,
                result = null,
                isSavedToHistory = false
            )
            persistRoMembraneConfig(updated)
            syncCipFromRoMembraneConfig(category = updated.selectedCategory, parallelCount = updated.parallelCount)
            updated
        }
    }

    /**
     * Memilih atau membatalkan pilihan chip preset kapasitas membran populer Indonesia.
     * Saat dipilih, nilai GPD otomatis terisi ke input kalkulasi.
     */
    fun toggleRoPreset(preset: RoMembranePreset) {
        _roMembraneUiState.update { state ->
            val isDeselecting = state.selectedPresetId == preset.id
            val updated = if (isDeselecting) {
                state.copy(
                    selectedPresetId = null,
                    gpdPerUnitInput = "",
                    gpdError = null,
                    flowAktualError = if (state.flowAktualError == RoValidationFieldError.PAIR_REQUIRED_WITH_GPD) null else state.flowAktualError,
                    result = null,
                    isSavedToHistory = false
                )
            } else {
                state.copy(
                    selectedCategory = preset.category,
                    selectedPresetId = preset.id,
                    gpdPerUnitInput = preset.gpdPerUnit.toString(),
                    gpdError = null,
                    flowAktualError = null,
                    result = null,
                    isSavedToHistory = false
                )
            }
            persistRoMembraneConfig(updated)
            syncCipFromRoMembraneConfig(category = updated.selectedCategory, parallelCount = updated.parallelCount)
            updated
        }
    }

    /**
     * Memperbarui input kapasitas GPD per membran dan menyinkronkan chip preset khusus di kategori aktif.
     */
    fun updateRoGpdPerUnit(value: String) {
        _roMembraneUiState.update { state ->
            val trimmed = value.trim()
            val categoryPresets = RoMembraneCalculator.presetsForCategory(state.selectedCategory)
            val matchedPreset = categoryPresets.firstOrNull {
                it.gpdPerUnit.toString() == trimmed && it.id == state.selectedPresetId
            } ?: categoryPresets.firstOrNull {
                it.gpdPerUnit.toString() == trimmed
            }
            val updated = state.copy(
                gpdPerUnitInput = value,
                selectedPresetId = matchedPreset?.id,
                gpdError = null,
                flowAktualError = if (state.flowAktualError == RoValidationFieldError.PAIR_REQUIRED_WITH_GPD) null else state.flowAktualError,
                result = null,
                isSavedToHistory = false
            )
            persistRoMembraneConfig(updated)
            updated
        }
    }

    /**
     * Memperbarui input Flow Rate Aktual Total (LPM).
     */
    fun updateRoActualFlowLpm(value: String) {
        _roMembraneUiState.update { state ->
            state.copy(
                actualFlowLpmInput = value,
                flowAktualError = null,
                gpdError = if (state.gpdError == RoValidationFieldError.PAIR_REQUIRED_WITH_FLOW) null else state.gpdError,
                result = null,
                isSavedToHistory = false
            )
        }
    }

    /**
     * Memvalidasi input dan menghitung status kesehatan membran RO.
     */
    fun calculateRoMembraneHealth() {
        val state = _roMembraneUiState.value
        val activePreset = RoMembraneCalculator.findPresetById(state.selectedPresetId)
        when (
            val outcome = RoMembraneCalculator.evaluateInputs(
                tdsBakuRaw = state.tdsBakuInput,
                tdsJadiRaw = state.tdsJadiInput,
                gpdPerUnitRaw = state.gpdPerUnitInput,
                actualFlowLpmRaw = state.actualFlowLpmInput,
                parallelCount = state.parallelCount,
                presetTypeName = activePreset?.typeName,
                presetPsiHint = activePreset?.psiHint,
                presetMaxFeedTdsPpm = activePreset?.maxFeedTdsPpm
            )
        ) {
            is RoMembraneEvaluationOutcome.ValidationError -> {
                _roMembraneUiState.update {
                    it.copy(
                        tdsBakuError = outcome.tdsBakuError,
                        tdsJadiError = outcome.tdsJadiError,
                        gpdError = outcome.gpdError,
                        flowAktualError = outcome.flowAktualError,
                        result = null,
                        isSavedToHistory = false
                    )
                }
            }
            is RoMembraneEvaluationOutcome.Success -> {
                _roMembraneUiState.update {
                    it.copy(
                        tdsBakuError = null,
                        tdsJadiError = null,
                        gpdError = null,
                        flowAktualError = null,
                        result = outcome.result,
                        isSavedToHistory = false
                    )
                }
            }
        }
    }

    /**
     * Membersihkan angka pengukuran sementara (TDS & Flow Aktual) sambil mempertahankan konfigurasi membran.
     */
    fun resetRoMembraneCalculator() {
        _roMembraneUiState.update { state ->
            state.copy(
                tdsBakuInput = "",
                tdsJadiInput = "",
                actualFlowLpmInput = "",
                tdsBakuError = null,
                tdsJadiError = null,
                gpdError = null,
                flowAktualError = null,
                result = null,
                isSavedToHistory = false
            )
        }
    }

    private fun loadSavedRoMembraneState(): RoMembraneUiState {
        return try {
            val savedCategoryName = roCalcPrefs.getString("ro_calc_category", null)
            val savedCategory = RoMembraneCategory.fromLegacyOrName(savedCategoryName)
            val savedParallel = roCalcPrefs.getInt(
                "ro_calc_parallel_count",
                RoMembraneCalculator.MIN_PARALLEL_MEMBRANES
            ).coerceIn(
                RoMembraneCalculator.MIN_PARALLEL_MEMBRANES,
                RoMembraneCalculator.MAX_PARALLEL_MEMBRANES
            )
            val rawPresetId = roCalcPrefs.getString("ro_calc_preset_id", null)
            val matchedPreset = RoMembraneCalculator.findPresetById(rawPresetId)
                ?.takeIf { it.category == savedCategory }
            val savedPresetId = matchedPreset?.id
                ?: if (savedCategory == RoMembraneCategory.SIZE_4040 && rawPresetId == null) "4040_ulp" else null
            val savedGpd = roCalcPrefs.getString("ro_calc_gpd", "") ?: ""
            val resolvedGpd = if (savedGpd.isBlank() && savedPresetId != null) {
                RoMembraneCalculator.findPresetById(savedPresetId)?.gpdPerUnit?.toString() ?: ""
            } else if (savedGpd.isBlank() && savedCategory == RoMembraneCategory.SIZE_4040) {
                "2400"
            } else {
                savedGpd
            }
            RoMembraneUiState(
                selectedCategory = savedCategory,
                parallelCount = savedParallel,
                selectedPresetId = savedPresetId,
                gpdPerUnitInput = resolvedGpd
            )
        } catch (_: Exception) {
            RoMembraneUiState()
        }
    }

    private fun persistRoMembraneConfig(state: RoMembraneUiState) {
        try {
            roCalcPrefs.edit()
                .putString("ro_calc_category", state.selectedCategory.name)
                .putInt("ro_calc_parallel_count", state.parallelCount)
                .putString("ro_calc_preset_id", state.selectedPresetId)
                .putString("ro_calc_gpd", state.gpdPerUnitInput)
                .apply()
        } catch (_: Exception) {
            // Abaikan jika penyimpanan preferensi lokal gagal
        }
    }

    /**
     * Menyimpan hasil pengecekan kesehatan membran RO ke Riwayat Perawatan (khusus pemilik depot).
     */
    fun saveRoMembraneCheckToHistory(
        namaPerawatan: String,
        statusLabel: String,
        diagnosisText: String
    ) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menyimpan riwayat cek membran.")
            return
        }
        val currentState = _roMembraneUiState.value
        val result = currentState.result ?: return
        if (currentState.isSavedToHistory) return

        val catatan = RoMembraneCalculator.buildHistoryNote(
            result = result,
            statusLabel = statusLabel,
            diagnosisText = diagnosisText
        )
        launchGuardedAction("Menyimpan hasil cek membran...") {
            val saved = repository.recordStandalonePerawatanLog(
                namaPerawatan = namaPerawatan,
                tanggalSelesai = System.currentTimeMillis(),
                catatan = catatan
            )
            if (saved) {
                _roMembraneUiState.update { it.copy(isSavedToHistory = true) }
                showSnackbar("Hasil cek membran disimpan ke Riwayat Perawatan")
            } else {
                showSnackbar("Gagal menyimpan hasil cek membran")
            }
        }
    }

    private fun buildInitialCipState(roState: RoMembraneUiState): CipCalculatorUiState {
        val housing = CipHousingSize.fromRoMembraneCategory(roState.selectedCategory)
        val tubes = roState.parallelCount.coerceIn(
            CipDosageCalculator.MIN_TUBE_COUNT,
            CipDosageCalculator.MAX_TUBE_COUNT
        )
        val suggestedLiters = CipDosageCalculator.suggestWaterVolumeLiters(housing, tubes)
        val defaultMode = CipCleaningMode.CITRIC_ACID
        val waterStr = CipDosageCalculator.formatDecimalId(suggestedLiters, 1)
        val concStr = CipDosageCalculator.formatDecimalId(defaultMode.defaultConcentrationPercent, 2)
        val initialOutcome = CipDosageCalculator.evaluateInputs(
            mode = defaultMode,
            housingSize = housing,
            tubeCount = tubes,
            waterVolumeRaw = waterStr,
            concentrationRaw = concStr
        )
        return CipCalculatorUiState(
            selectedMode = defaultMode,
            selectedHousing = housing,
            tubeCount = tubes,
            waterVolumeInput = waterStr,
            isCustomWaterVolume = false,
            concentrationInput = concStr,
            result = (initialOutcome as? CipEvaluationOutcome.Success)?.result
        )
    }

    private fun evaluateAndApplyCipState(base: CipCalculatorUiState): CipCalculatorUiState {
        return when (
            val outcome = CipDosageCalculator.evaluateInputs(
                mode = base.selectedMode,
                housingSize = base.selectedHousing,
                tubeCount = base.tubeCount,
                waterVolumeRaw = base.waterVolumeInput,
                concentrationRaw = base.concentrationInput
            )
        ) {
            is CipEvaluationOutcome.Success -> base.copy(
                volumeError = null,
                concentrationError = null,
                result = outcome.result,
                isSavedToHistory = false
            )
            is CipEvaluationOutcome.ValidationError -> base.copy(
                volumeError = outcome.volumeError,
                concentrationError = outcome.concentrationError,
                result = null,
                isSavedToHistory = false
            )
        }
    }

    private fun syncCipFromRoMembraneConfig(
        category: RoMembraneCategory,
        parallelCount: Int
    ) {
        val mappedHousing = CipHousingSize.fromRoMembraneCategory(category)
        val safeTubes = parallelCount.coerceIn(
            CipDosageCalculator.MIN_TUBE_COUNT,
            CipDosageCalculator.MAX_TUBE_COUNT
        )
        _cipCalculatorUiState.update { state ->
            val newSuggested = CipDosageCalculator.suggestWaterVolumeLiters(mappedHousing, safeTubes)
            val newWaterInput = if (state.isCustomWaterVolume) {
                state.waterVolumeInput
            } else {
                CipDosageCalculator.formatDecimalId(newSuggested, 1)
            }
            evaluateAndApplyCipState(
                state.copy(
                    selectedHousing = mappedHousing,
                    tubeCount = safeTubes,
                    waterVolumeInput = newWaterInput
                )
            )
        }
    }

    /**
     * Mengatur indeks sub-tab aktif pada tab Cek Membran (0 = Cek Kesehatan, 1 = Kalkulator Cuci CIP).
     */
    fun setRoMembraneSubTab(index: Int) {
        _roMembraneSubTab.value = index.coerceIn(0, 1)
    }

    /**
     * Membuka sub-tab Kalkulator CIP dari kartu hasil diagnosis Cek Kesehatan Membran
     * sekaligus memilih mode pembersih yang direkomendasikan dan menampilkan banner alasan rekomendasi.
     */
    fun openCipFromDiagnosis(result: RoMembraneResult) {
        val recommendedMode = CipDosageCalculator.recommendModeFromDiagnosis(
            status = result.status,
            cause = result.diagnosisCause,
            flowDropPercent = result.flowDropPercent
        )
        val defaultConc = CipDosageCalculator.formatDecimalId(recommendedMode.defaultConcentrationPercent, 2)
        _cipCalculatorUiState.update { state ->
            evaluateAndApplyCipState(
                state.copy(
                    selectedMode = recommendedMode,
                    concentrationInput = defaultConc,
                    recommendedFromDiagnosis = recommendedMode
                )
            )
        }
        _roMembraneSubTab.value = 1
    }

    /**
     * Memilih mode bahan pembersih CIP dan mengatur konsentrasi default yang sesuai.
     */
    fun selectCipMode(mode: CipCleaningMode) {
        _cipCalculatorUiState.update { state ->
            if (state.selectedMode == mode && state.recommendedFromDiagnosis == null) return@update state
            val defaultConc = CipDosageCalculator.formatDecimalId(mode.defaultConcentrationPercent, 2)
            evaluateAndApplyCipState(
                state.copy(
                    selectedMode = mode,
                    concentrationInput = defaultConc,
                    recommendedFromDiagnosis = if (state.selectedMode == mode) state.recommendedFromDiagnosis else null
                )
            )
        }
    }

    /**
     * Memilih ukuran housing membran pada Kalkulator CIP, memperbarui saran volume air sirkulasi,
     * serta menyinkronkan kategori ukuran membran tersimpan.
     */
    fun selectCipHousing(housing: CipHousingSize) {
        _cipCalculatorUiState.update { state ->
            val newSuggested = CipDosageCalculator.suggestWaterVolumeLiters(housing, state.tubeCount)
            val newWaterStr = CipDosageCalculator.formatDecimalId(newSuggested, 1)
            evaluateAndApplyCipState(
                state.copy(
                    selectedHousing = housing,
                    waterVolumeInput = newWaterStr,
                    isCustomWaterVolume = false
                )
            )
        }
        _roMembraneUiState.update { roState ->
            val targetCategory = housing.toRoMembraneCategory(roState.selectedCategory)
            if (roState.selectedCategory == targetCategory) {
                roState
            } else {
                val defaultPreset = if (targetCategory == RoMembraneCategory.SIZE_4040) {
                    RoMembraneCalculator.findPresetById("4040_ulp")
                } else null
                val updatedRo = roState.copy(
                    selectedCategory = targetCategory,
                    selectedPresetId = defaultPreset?.id,
                    gpdPerUnitInput = defaultPreset?.gpdPerUnit?.toString() ?: roState.gpdPerUnitInput,
                    result = null,
                    isSavedToHistory = false
                )
                persistRoMembraneConfig(updatedRo)
                updatedRo
            }
        }
    }

    /**
     * Mengubah jumlah tabung membran pada Kalkulator CIP (minimal 1), menghitung ulang saran volume air,
     * dan menyinkronkan jumlah paralel ke pengaturan Cek Membran.
     */
    fun updateCipTubeCount(count: Int) {
        val clamped = count.coerceIn(
            CipDosageCalculator.MIN_TUBE_COUNT,
            CipDosageCalculator.MAX_TUBE_COUNT
        )
        _cipCalculatorUiState.update { state ->
            val newSuggested = CipDosageCalculator.suggestWaterVolumeLiters(state.selectedHousing, clamped)
            val newWaterStr = CipDosageCalculator.formatDecimalId(newSuggested, 1)
            evaluateAndApplyCipState(
                state.copy(
                    tubeCount = clamped,
                    waterVolumeInput = newWaterStr,
                    isCustomWaterVolume = false
                )
            )
        }
        _roMembraneUiState.update { roState ->
            if (roState.parallelCount == clamped) {
                roState
            } else {
                val updatedRo = roState.copy(
                    parallelCount = clamped,
                    result = null,
                    isSavedToHistory = false
                )
                persistRoMembraneConfig(updatedRo)
                updatedRo
            }
        }
    }

    /**
     * Memperbarui angka volume air sirkulasi CIP secara manual.
     */
    fun updateCipWaterVolume(value: String) {
        _cipCalculatorUiState.update { state ->
            val suggested = CipDosageCalculator.suggestWaterVolumeLiters(state.selectedHousing, state.tubeCount)
            val parsed = CipDosageCalculator.parseDecimalInput(value)
            val isCustom = parsed == null || kotlin.math.abs(parsed - suggested) > 0.05
            evaluateAndApplyCipState(
                state.copy(
                    waterVolumeInput = value,
                    isCustomWaterVolume = isCustom
                )
            )
        }
    }

    /**
     * Mengembalikan volume air sirkulasi CIP ke saran standar berdasarkan ukuran housing & jumlah tabung.
     */
    fun applyCipSuggestedWaterVolume() {
        _cipCalculatorUiState.update { state ->
            val suggested = CipDosageCalculator.suggestWaterVolumeLiters(state.selectedHousing, state.tubeCount)
            val waterStr = CipDosageCalculator.formatDecimalId(suggested, 1)
            evaluateAndApplyCipState(
                state.copy(
                    waterVolumeInput = waterStr,
                    isCustomWaterVolume = false
                )
            )
        }
    }

    /**
     * Memperbarui persentase konsentrasi/dosis bahan kimia CIP.
     */
    fun updateCipConcentration(value: String) {
        _cipCalculatorUiState.update { state ->
            evaluateAndApplyCipState(state.copy(concentrationInput = value))
        }
    }

    /**
     * Memperbarui input biaya opsional pembelian bahan kimia CIP (Rp).
     */
    fun updateCipBiayaInput(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(12)
        _cipCalculatorUiState.update { state ->
            state.copy(
                biayaInput = digitsOnly,
                isSavedToHistory = false
            )
        }
    }

    /**
     * Memperbarui catatan tambahan opsional sebelum menyimpan riwayat CIP.
     */
    fun updateCipExtraNote(value: String) {
        _cipCalculatorUiState.update { state ->
            state.copy(
                extraNoteInput = value.take(160),
                isSavedToHistory = false
            )
        }
    }

    /**
     * Memicu evaluasi dan perhitungan eksplisit takaran CIP.
     */
    fun calculateCipDosage() {
        _cipCalculatorUiState.update { state ->
            evaluateAndApplyCipState(state)
        }
    }

    /**
     * Mereset input Kalkulator CIP ke saran volume standar dan konsentrasi default mode aktif.
     */
    fun resetCipCalculator() {
        _cipCalculatorUiState.update { state ->
            val suggested = CipDosageCalculator.suggestWaterVolumeLiters(state.selectedHousing, state.tubeCount)
            val waterStr = CipDosageCalculator.formatDecimalId(suggested, 1)
            val concStr = CipDosageCalculator.formatDecimalId(state.selectedMode.defaultConcentrationPercent, 2)
            evaluateAndApplyCipState(
                state.copy(
                    waterVolumeInput = waterStr,
                    isCustomWaterVolume = false,
                    concentrationInput = concStr,
                    biayaInput = "",
                    extraNoteInput = "",
                    recommendedFromDiagnosis = null
                )
            )
        }
    }

    /**
     * Menyimpan catatan pencucian membran RO (CIP) ke Riwayat Perawatan (serta Pengeluaran jika biaya > Rp 0).
     */
    fun saveCipDosageToHistory(
        namaPerawatan: String,
        modeShortLabel: String,
        housingLabel: String
    ) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menyimpan riwayat perawatan.")
            return
        }
        val currentState = _cipCalculatorUiState.value
        val result = currentState.result ?: return
        if (currentState.isSavedToHistory) return

        val biayaNominal = currentState.biayaInput.toLongOrNull()?.toDouble()?.coerceAtLeast(0.0) ?: 0.0
        val catatan = CipDosageCalculator.buildHistoryNote(
            result = result,
            modeShortLabel = modeShortLabel,
            housingLabel = housingLabel,
            extraNote = currentState.extraNoteInput
        )
        launchGuardedAction("Menyimpan riwayat pencucian membran (CIP)...") {
            val saved = repository.recordStandalonePerawatanLog(
                namaPerawatan = namaPerawatan,
                tanggalSelesai = System.currentTimeMillis(),
                catatan = catatan,
                biaya = biayaNominal
            )
            if (saved) {
                _cipCalculatorUiState.update { it.copy(isSavedToHistory = true) }
                val msg = if (biayaNominal > 0.0) {
                    "Riwayat CIP & pengeluaran ${FormatUtils.formatRupiah(biayaNominal)} berhasil disimpan"
                } else {
                    "Riwayat pencucian membran (CIP) disimpan ke Riwayat Perawatan"
                }
                showSnackbar(msg)
            } else {
                showSnackbar("Gagal menyimpan riwayat pencucian membran")
            }
        }
    }

    fun importJsonData(jsonString: String, onFinished: ((com.example.util.ImportResult) -> Unit)? = null) {
        if (isUserKaryawan()) {
            val denied = com.example.util.ImportResult(
                success = false,
                message = "Hanya pemilik depot yang dapat mengimpor cadangan data."
            )
            showSnackbar(denied.message)
            onFinished?.invoke(denied)
            return
        }
        launchGuardedAction("Mengimpor data cadangan...") {
            val result = com.example.util.ExportHelper.importJsonData(getApplication(), db, jsonString)
            if (result.success) {
                result.importedDepotProfile?.let { importedProf ->
                    val currentUid = depotProfile.value.uid
                    val merged = syncEngine.profileStore.mergeFromBackupJson(
                        uid = currentUid,
                        ownerName = importedProf.ownerName,
                        depotName = importedProf.depotName,
                        phoneNumber = importedProf.phoneNumber,
                        address = importedProf.address,
                        receiptFooter = importedProf.receiptFooter,
                        depotType = importedProf.depotType,
                        tankCapacityLiters = importedProf.tankCapacityLiters
                    )
                    if (merged.hasConfiguredDepotName()) {
                        syncEngine.saveDepotProfile(
                            ownerName = merged.effectiveOwnerName(),
                            depotName = merged.depotName,
                            phoneNumber = merged.phoneNumber,
                            address = merged.address,
                            receiptFooter = merged.receiptFooter,
                            depotType = merged.depotType,
                            tankCapacityLiters = merged.tankCapacityLiters
                        )
                    }
                }
                repository.reconcileKasbonDiscrepancies()
                repository.enqueueAllLocalDataForSync()
            }
            showSnackbar(result.message)
            onFinished?.invoke(result)
        }
    }

    fun syncToCloud(context: Context, userId: String) {
        if (userId.isBlank() || userId == OfflineSessionManager.OFFLINE_LOCAL_UID) {
            showSnackbar(context.getString(com.example.R.string.offline_requires_google_sync))
            return
        }
        runCloudSync(context, userId, pushLocalFirst = true, forceReenqueueAll = false)
    }

    /**
     * Memasukkan ulang seluruh data lokal sesuai peran pengguna ke antrean sinkronisasi
     * dan langsung mengirimkannya ke Cloud (Force Push All).
     */
    fun forcePushAllLocalToCloud(context: Context, userId: String) {
        if (userId.isBlank() || userId == OfflineSessionManager.OFFLINE_LOCAL_UID) {
            showSnackbar(context.getString(com.example.R.string.offline_requires_google_sync))
            return
        }
        runCloudSync(context, userId, pushLocalFirst = true, forceReenqueueAll = true)
    }

    /**
     * Memicu pengiriman antrean tertunda ke Cloud secara instan tanpa menunggu jadwal auto-retry.
     */
    fun retryPendingSyncNow() {
        clearCloudSyncError()
        syncEngine.retryPendingQueueNow()
    }

    fun restoreFromCloud(context: Context, userId: String) {
        if (userId.isBlank() || userId == OfflineSessionManager.OFFLINE_LOCAL_UID) {
            showSnackbar(context.getString(com.example.R.string.offline_requires_google_sync))
            return
        }
        runCloudSync(context, userId, pushLocalFirst = false, forceReenqueueAll = false)
    }

    fun clearCloudSyncError() {
        _cloudSyncError.value = null
    }

    private fun mapOnlineError(throwable: Throwable?): String {
        val raw = throwable?.message.orEmpty()
        val app = getApplication<Application>()
        return when {
            raw.contains("PERMISSION_DENIED", ignoreCase = true) ||
                raw.contains("ditolak", ignoreCase = true) ->
                app.getString(com.example.R.string.error_permission_denied)
            raw.contains("UNAVAILABLE", ignoreCase = true) ||
                raw.contains("timed out", ignoreCase = true) ||
                raw.contains("network", ignoreCase = true) ||
                raw.contains("koneksi", ignoreCase = true) ||
                raw.contains("terputus", ignoreCase = true) ->
                app.getString(com.example.R.string.error_network_disconnected)
            else -> app.getString(com.example.R.string.error_online_sync)
        }
    }

    private fun runCloudSync(
        context: Context,
        userId: String,
        pushLocalFirst: Boolean,
        forceReenqueueAll: Boolean = false
    ) {
        if (_isSyncingCloud.value) return
        _isSyncingCloud.value = true
        _activeSyncPushMode.value = pushLocalFirst
        _cloudSyncError.value = null
        _isBusyAction.value = true
        _busyActionMessage.value = when {
            forceReenqueueAll -> context.getString(com.example.R.string.sync_force_push_progress)
            pushLocalFirst -> "Menyimpan data online..."
            else -> "Mengambil cadangan online..."
        }
        viewModelScope.launch {
            try {
                val res = syncEngine.forceSyncNow(
                    pushLocalFirst = pushLocalFirst,
                    forceReenqueueAll = forceReenqueueAll
                )
                res.onSuccess { summary ->
                    _lastSyncSummary.value = summary
                    _cloudSyncError.value = null
                    val message = when {
                        forceReenqueueAll -> context.getString(
                            com.example.R.string.sync_force_push_success_snackbar,
                            summary.pelangganCount,
                            summary.transaksiCount
                        )
                        pushLocalFirst ->
                            "Data online tersimpan (${summary.pelangganCount} pelanggan, ${summary.transaksiCount} nota)"
                        else ->
                            "Cadangan online diterapkan (${summary.pelangganCount} pelanggan, ${summary.transaksiCount} nota)"
                    }
                    showSnackbar(message)
                }.onFailure { err ->
                    val errText = mapOnlineError(err)
                    _cloudSyncError.value = errText
                    showSnackbar(errText)
                }
            } catch (e: Exception) {
                val errText = mapOnlineError(e)
                _cloudSyncError.value = errText
                showSnackbar(errText)
            } finally {
                _busyActionMessage.value = null
                _isBusyAction.value = false
                _activeSyncPushMode.value = null
                _isSyncingCloud.value = false
            }
        }
    }

    /**
     * O(P + T) indexed customer UI model computation (replaces O(P * T) nested loops).
     */
    fun getPelangganUiModels(
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>
    ): List<PelangganUiModel> {
        val txByCustomer = HashMap<Int, MutableList<TransaksiDetail>>(pelangganList.size.coerceAtLeast(16))
        for (detail in transaksiList) {
            if (!KaryawanScopeHelper.isDeletedTransaksi(detail.transaksi)) {
                val cid = detail.transaksi.id_pelanggan ?: continue
                txByCustomer.getOrPut(cid) { ArrayList() }.add(detail)
            }
        }

        return pelangganList.map { p ->
            val customerTxs = txByCustomer[p.id] ?: emptyList()
            var unpaidTotal = 0.0
            var hasSettledKasbonNote = false
            for (detail in customerTxs) {
                val tx = detail.transaksi
                if (tx.status.equals("Kasbon", ignoreCase = true)) {
                    unpaidTotal += tx.remainingKasbonAmount()
                } else if (tx.status.equals("Lunas", ignoreCase = true) &&
                    tx.tanggalBayar != null &&
                    tx.tanggalBayar != tx.tanggal
                ) {
                    hasSettledKasbonNote = true
                }
            }
            val effectiveKasbon = when {
                unpaidTotal > 0.0 -> unpaidTotal
                hasSettledKasbonNote -> 0.0
                else -> p.sisa_kasbon
            }
            val syncedPelanggan = if (effectiveKasbon != p.sisa_kasbon) {
                p.copy(sisa_kasbon = effectiveKasbon)
            } else {
                p
            }
            val prediction = FormatUtils.calculateCrmPredictionForCustomer(customerTxs)
            PelangganUiModel(
                pelanggan = syncedPelanggan,
                estimatedIntervalDays = prediction.averageIntervalDays,
                crmPrediction = prediction
            )
        }
    }

    /**
     * Menyaring dan mengurutkan daftar [PelangganUiModel] berdasarkan relevansi kata kunci [query].
     */
    fun getFilteredPelangganUiModels(
        query: String,
        list: List<PelangganUiModel>,
        kurirList: List<Kurir> = emptyList()
    ): List<PelangganUiModel> {
        if (query.isBlank()) return list
        val kurirById = if (kurirList.isEmpty()) emptyMap() else kurirList.associate { it.id to it.nama }
        return SearchMatcher.rankAndFilter(list, query) { item ->
            val p = item.pelanggan
            val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(p.alamat)
            val deletedBy = KaryawanScopeHelper.extractDeletedByPelanggan(p).orEmpty()
            val kurirName = p.id_kurir_langganan?.let { kurirById[it] }.orEmpty()
            listOf(
                SearchField(p.nama, weight = 24, isPrimaryName = true),
                SearchField(cleanAlamat, weight = 10),
                SearchField(p.no_hp, weight = 14, isPhone = true),
                SearchField(kurirName, weight = 6),
                SearchField(deletedBy, weight = 5)
            )
        }
    }

    /**
     * Menyaring dan mengurutkan daftar pelanggan untuk dropdown pemilih di layar Transaksi.
     */
    fun getFilteredPelangganForPicker(
        query: String,
        pelangganList: List<Pelanggan>,
        kurirList: List<Kurir> = emptyList()
    ): List<Pelanggan> {
        val sortedBase = pelangganList.sortedBy { it.nama.lowercase() }
        if (query.isBlank()) return sortedBase
        val kurirById = if (kurirList.isEmpty()) emptyMap() else kurirList.associate { it.id to it.nama }
        return SearchMatcher.rankAndFilter(sortedBase, query) { p ->
            val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(p.alamat)
            val kurirName = p.id_kurir_langganan?.let { kurirById[it] }.orEmpty()
            listOf(
                SearchField(p.nama, weight = 24, isPrimaryName = true),
                SearchField(cleanAlamat, weight = 10),
                SearchField(p.no_hp, weight = 14, isPhone = true),
                SearchField(kurirName, weight = 6)
            )
        }
    }

    /**
     * Menyaring dan mengurutkan daftar pelanggan kasbon sesuai [sortOption] dan relevansi [query].
     */
    fun getSortedKasbonPelanggan(
        pelangganList: List<Pelanggan>,
        sortOption: KasbonSortOption,
        query: String,
        transaksiList: List<TransaksiDetail>
    ): List<Pelanggan> {
        val activeKasbon = pelangganList
            .filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) && it.sisa_kasbon > 0 }

        val baseSorted = if (sortOption == KasbonSortOption.TERBANYAK) {
            activeKasbon.sortedByDescending { it.sisa_kasbon }
        } else {
            val minDateByCust = HashMap<Int, Long>()
            val maxDateByCust = HashMap<Int, Long>()
            for (detail in transaksiList) {
                val tx = detail.transaksi
                if (!KaryawanScopeHelper.isDeletedTransaksi(tx) && tx.status.equals("Kasbon", ignoreCase = true)) {
                    val cid = tx.id_pelanggan ?: continue
                    val currentMin = minDateByCust[cid]
                    if (currentMin == null || tx.tanggal < currentMin) {
                        minDateByCust[cid] = tx.tanggal
                    }
                    val currentMax = maxDateByCust[cid]
                    if (currentMax == null || tx.tanggal > currentMax) {
                        maxDateByCust[cid] = tx.tanggal
                    }
                }
            }
            when (sortOption) {
                KasbonSortOption.TERBANYAK -> activeKasbon.sortedByDescending { it.sisa_kasbon }
                KasbonSortOption.TERLAMA -> activeKasbon.sortedBy { p -> minDateByCust[p.id] ?: Long.MAX_VALUE }
                KasbonSortOption.TERBARU -> activeKasbon.sortedByDescending { p -> maxDateByCust[p.id] ?: 0L }
            }
        }

        if (query.isBlank()) return baseSorted

        return SearchMatcher.rankAndFilter(baseSorted, query) { p ->
            val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(p.alamat)
            val nominalFormatted = FormatUtils.formatRupiah(p.sisa_kasbon)
            val nominalRaw = p.sisa_kasbon.toLong().toString()
            listOf(
                SearchField(p.nama, weight = 24, isPrimaryName = true),
                SearchField(cleanAlamat, weight = 10),
                SearchField(p.no_hp, weight = 14, isPhone = true),
                SearchField(nominalFormatted, weight = 8),
                SearchField(nominalRaw, weight = 8)
            )
        }
    }

    fun getTopRankingPelanggan(
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>
    ): List<Pair<Pelanggan, Int>> {
        val activeTx = transaksiList.filter {
            !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) && it.transaksi.jumlah > 0
        }
        val stats = activeTx.groupBy { it.transaksi.id_pelanggan }.mapValues { (_, txs) ->
            txs.sumOf { it.transaksi.jumlah }
        }
        return pelangganList
            .filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) }
            .map { p -> Pair(p, stats[p.id] ?: 0) }
            .sortedByDescending { it.second }
    }

    fun getPasifPelanggan(
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>,
        minDays: Long
    ): List<Pair<Pelanggan, Long>> {
        val activeTx = transaksiList.filter {
            !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) && it.transaksi.jumlah > 0
        }
        val lastTxMap = activeTx.groupBy { it.transaksi.id_pelanggan }.mapValues { (_, txs) ->
            txs.maxOf { it.transaksi.tanggal }
        }
        return pelangganList
            .filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) }
            .mapNotNull { p ->
                val lastDate = lastTxMap[p.id]
                val days = if (lastDate != null) FormatUtils.daysSince(lastDate) else 999L
                if (days >= minDays) Pair(p, days) else null
            }.sortedByDescending { it.second }
    }

    /**
     * Menandai pelanggan sebagai sudah dihubungi pada perangkat ini dengan timestamp terkini.
     */
    fun markCrmCustomerContacted(pelangganId: Int, contactedAtMillis: Long = System.currentTimeMillis()) {
        val updated = _crmContactedTimestamps.value.toMutableMap()
        updated[pelangganId] = contactedAtMillis
        val pruned = pruneOldCrmContactedEntries(updated, contactedAtMillis)
        _crmContactedTimestamps.value = pruned
        saveCrmContactedTimestamps(pruned)
    }

    /**
     * Menghapus penanda sudah dihubungi untuk [pelangganId] agar kembali masuk daftar tindak lanjut.
     */
    fun clearCrmCustomerContacted(pelangganId: Int) {
        val updated = _crmContactedTimestamps.value.toMutableMap()
        if (updated.remove(pelangganId) != null) {
            _crmContactedTimestamps.value = updated
            saveCrmContactedTimestamps(updated)
        }
    }

    private fun pruneOldCrmContactedEntries(
        raw: Map<Int, Long>,
        nowMillis: Long = System.currentTimeMillis()
    ): Map<Int, Long> {
        val retentionMillis = 7L * 86_400_000L
        return raw.filterValues { ts -> ts > 0L && (nowMillis - ts) <= retentionMillis }
    }

    private fun loadCrmContactedTimestamps(): Map<Int, Long> {
        return try {
            val raw = crmReminderPrefs.getString("contacted_map_json", null).orEmpty()
            if (raw.isBlank()) return emptyMap()
            val json = org.json.JSONObject(raw)
            val result = HashMap<Int, Long>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val id = key.toIntOrNull() ?: continue
                val ts = json.optLong(key, 0L)
                if (ts > 0L) result[id] = ts
            }
            pruneOldCrmContactedEntries(result)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveCrmContactedTimestamps(map: Map<Int, Long>) {
        try {
            val json = org.json.JSONObject()
            map.forEach { (id, ts) ->
                json.put(id.toString(), ts)
            }
            crmReminderPrefs.edit().putString("contacted_map_json", json.toString()).apply()
        } catch (_: Exception) {
            // Abaikan kegagalan I/O preferensi lokal non-kritis
        }
    }

    fun deleteAllData() {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menghapus seluruh data.")
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteAllLocalData()
                showSnackbar("Seluruh data di HP dihapus")
            } catch (_: Exception) {
                showSnackbar(getApplication<Application>().getString(com.example.R.string.error_delete_local))
            }
        }
    }
}

data class PelangganUiModel(
    val pelanggan: Pelanggan,
    val estimatedIntervalDays: Int?,
    val crmPrediction: com.example.util.CrmOrderPrediction = com.example.util.CrmOrderPrediction(
        status = com.example.util.CrmOrderStatus.INSUFFICIENT_DATA
    )
)
