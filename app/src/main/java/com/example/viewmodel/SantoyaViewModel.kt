package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.KasbonLog
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.data.repository.SantoyaRepository
import com.example.util.DepotRole
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.PerawatanEvaluation
import com.example.util.PerawatanScheduler
import com.example.util.RealtimeSyncEngine
import com.example.util.RealtimeSyncState
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
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
    val data: Any? = null
)

data class KasbonDiscrepancy(
    val pelangganId: Int,
    val namaPelanggan: String,
    val saldoTercatat: Double,
    val totalNotaBelumLunas: Double,
    val selisih: Double
)

class SantoyaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val syncEngine = RealtimeSyncEngine(application, db)
    private val repository = SantoyaRepository(db, application) {
        syncEngine.scheduleFlush()
    }

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
            showSnackbar("Akses Anda dicabut. Transaksi baru tidak diizinkan.")
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

    // --- Cloud Firestore Real-Time Sync State ---
    val realtimeSyncState: StateFlow<RealtimeSyncState> = syncEngine.state

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private val _activeSyncPushMode = MutableStateFlow<Boolean?>(null)
    val activeSyncPushMode: StateFlow<Boolean?> = _activeSyncPushMode.asStateFlow()

    private val _cloudSyncError = MutableStateFlow<String?>(null)
    val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow<SyncSummary?>(null)
    val lastSyncSummary: StateFlow<SyncSummary?> = _lastSyncSummary.asStateFlow()

    private val _currentUserEmail = MutableStateFlow(
        try {
            Firebase.auth.currentUser?.email
        } catch (_: Exception) {
            null
        }
    )

    val isKaryawan: StateFlow<Boolean> = combine(
        realtimeSyncState,
        allKurir,
        _currentUserEmail
    ) { sync, kurirs, email ->
        KaryawanScopeHelper.isKaryawanRole(sync.role, email, sync.ownerEmail, kurirs)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    private val _roMembraneUiState = MutableStateFlow(loadSavedRoMembraneState())
    val roMembraneUiState: StateFlow<RoMembraneUiState> = _roMembraneUiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            combine(
                repository.allPelanggan,
                repository.allProduk,
                repository.allTransaksiWithDetail
            ) { _, _, _ -> true }.first()
            _isInitialDataLoaded.value = true
        }
    }

    val kasbonDiscrepancies: StateFlow<List<KasbonDiscrepancy>> = combine(allPelanggan, allTransaksi) { pelangganList, transaksiList ->
        val activeUnpaidByCust = transaksiList.asSequence()
            .filter {
                !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) &&
                    it.transaksi.status.equals("Kasbon", ignoreCase = true)
            }
            .groupBy { it.transaksi.id_pelanggan }
        pelangganList.mapNotNull { p ->
            val unpaidSum = activeUnpaidByCust[p.id]?.sumOf { it.transaksi.remainingKasbonAmount() } ?: 0.0
            val targetKasbon = p.sisa_kasbon.coerceAtLeast(0.0)
            if (unpaidSum > targetKasbon) {
                KasbonDiscrepancy(
                    pelangganId = p.id,
                    namaPelanggan = p.nama,
                    saldoTercatat = targetKasbon,
                    totalNotaBelumLunas = unpaidSum,
                    selisih = unpaidSum - targetKasbon
                )
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun reconcileKasbonDiscrepancies(onFinished: ((Int) -> Unit)? = null) {
        launchGuardedAction("Menyelaraskan nota kasbon...") {
            val count = repository.reconcileKasbonDiscrepancies()
            if (count > 0) {
                showSnackbar("Berhasil menyelaraskan nota kasbon ($count pelanggan)")
            } else {
                showSnackbar("Semua saldo kasbon dan nota sudah selaras")
            }
            onFinished?.invoke(count)
        }
    }

    fun startRealtimeSync(uid: String, email: String?) {
        _currentUserEmail.value = email ?: try {
            Firebase.auth.currentUser?.email
        } catch (_: Exception) {
            null
        }
        syncEngine.start(uid, email)
    }

    fun stopRealtimeSync() {
        _currentUserEmail.value = null
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
                showSnackbar("Belum ditemukan tautan depot untuk $email. Pastikan Pemilik sudah menyimpan email Anda.")
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
        _currentUserEmail.value = null
        syncEngine.stop()
        try {
            syncEngine.clearLocalData()
        } catch (e: Exception) {
            showSnackbar("Gagal menghapus data lokal: ${e.localizedMessage ?: "Kesalahan database"}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncEngine.stop()
    }

    fun isUserKaryawan(): Boolean {
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
        val activeCust = _txPelangganId.value?.let { custId ->
            allPelanggan.value.find { it.id == custId }
        }
        val prod = allProduk.value.find { it.id == id }
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
                val karyawanKurirId = if (isUserKaryawan()) resolveActiveKaryawanImmediate()?.id else null
                repository.createFastCashTransaksi(idProduk, jumlah, karyawanKurirId)
                showSnackbar("Terjual $jumlah galon (Cash)")
            } catch (e: Exception) {
                showSnackbar("Gagal mencatat transaksi: ${e.message ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun getKasbonLogsByPelanggan(pelangganId: Int): Flow<List<KasbonLog>> =
        repository.getKasbonLogsByPelanggan(pelangganId)

    fun submitTransaksi(onSuccess: (TransaksiDetail?) -> Unit) {
        if (_isBusyAction.value) return
        val pelangganId = _txPelangganId.value
        val produkId = _txProdukId.value
        if (pelangganId == null) {
            showSnackbar("Pilih pelanggan terlebih dahulu")
            return
        }
        if (produkId == null) {
            showSnackbar("Pilih produk terlebih dahulu")
            return
        }

        val karyawanMode = isUserKaryawan()
        val activeCust = allPelanggan.value.find { it.id == pelangganId }
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

        launchGuardedAction("Menyimpan transaksi...") {
            val newId = repository.recordTransaksi(
                tanggal = _txTanggal.value,
                idPelanggan = pelangganId,
                idKurir = effectiveKurirId,
                idProduk = produkId,
                jumlah = _txJumlah.value,
                hargaSatuan = effectiveHargaSatuan,
                metodeBayar = metodeVal,
                status = statusVal,
                uangMuka = uangMukaVal,
                sisaKasbon = sisaKasbonVal
            )
            val savedDetail = repository.getTransaksiDetailById(newId.toInt())
            showSnackbar("Transaksi berhasil disimpan")
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
            showSnackbar("Transaksi ditandai lunas ($metodeBayar)")
        }
    }

    fun deleteTransaksi(transaksi: Transaksi) {
        launchGuardedAction("Menghapus transaksi...") {
            if (isUserKaryawan()) {
                val karyawanName = resolveActiveKaryawanImmediate()?.nama ?: "Karyawan"
                repository.softDeleteTransaksiByKaryawan(transaksi, karyawanName)
                showSnackbar("Transaksi dihapus (dicatat oleh $karyawanName)")
            } else {
                repository.deleteTransaksi(transaksi)
                showSnackbar("Transaksi dihapus")
            }
        }
    }

    fun restoreTransaksi(transaksi: Transaksi) {
        launchGuardedAction("Memulihkan transaksi...") {
            repository.restoreTransaksi(transaksi)
            showSnackbar("Transaksi berhasil dipulihkan")
        }
    }

    fun permanentDeleteTransaksi(transaksi: Transaksi) {
        if (isUserKaryawan()) {
            showSnackbar("Hanya pemilik depot yang dapat menghapus permanen.")
            return
        }
        launchGuardedAction("Menghapus permanen...") {
            repository.deleteTransaksi(transaksi)
            showSnackbar("Transaksi dihapus permanen")
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
        launchGuardedAction("Memperbarui transaksi...") {
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
                uangMuka = uangMuka
            )
            showSnackbar("Transaksi diperbarui")
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
        val effectiveHargaKhusus = if (karyawanMode) null else hargaKhusus
        launchGuardedAction("Menambahkan pelanggan...") {
            val id = repository.insertPelanggan(
                Pelanggan(
                    nama = nama.trim(),
                    alamat = alamat.trim(),
                    no_hp = noHp.trim(),
                    sisa_kasbon = initialKasbon,
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
            allPelanggan.value.find { it.id == pelanggan.id }?.harga_khusus
        } else {
            pelanggan.harga_khusus
        }
        val effectivePelanggan = if (karyawanMode) {
            pelanggan.copy(
                id_kurir_langganan = karyawanKurirId ?: pelanggan.id_kurir_langganan,
                harga_khusus = existingHargaKhusus
            )
        } else {
            pelanggan
        }
        launchGuardedAction("Memperbarui pelanggan...") {
            repository.updatePelanggan(effectivePelanggan)
            showSnackbar("Data ${effectivePelanggan.nama} diperbarui")
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
                repository.deletePelanggan(pelanggan)
                showSnackbar("Pelanggan ${pelanggan.nama} dihapus")
            }
        }
    }

    fun restorePelanggan(pelanggan: Pelanggan) {
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

    fun bayarKasbon(pelangganId: Int, nominalBayar: Double, metodeBayar: String = "Cash") {
        if (nominalBayar <= 0) return
        launchGuardedAction("Menyimpan pembayaran kasbon...") {
            repository.bayarKasbonPelanggan(pelangganId, nominalBayar, metodeBayar)
            showSnackbar("Pembayaran ${FormatUtils.formatRupiah(nominalBayar)} ($metodeBayar) tersimpan")
        }
    }

    fun addProduk(nama: String, harga: Double) {
        if (nama.isBlank() || harga <= 0) return
        launchGuardedAction("Menambahkan produk...") {
            repository.insertProduk(Produk(nama_produk = nama.trim(), harga = harga))
            showSnackbar("Produk $nama ditambahkan")
        }
    }

    fun updateProduk(produk: Produk) {
        launchGuardedAction("Memperbarui produk...") {
            repository.updateProduk(produk)
            showSnackbar("Produk ${produk.nama_produk} diperbarui")
        }
    }

    fun deleteProduk(produk: Produk) {
        launchGuardedAction("Menghapus produk...") {
            try {
                if (_txProdukId.value == produk.id) {
                    _txProdukId.value = null
                }
                repository.deleteProduk(produk)
                showSnackbar("Produk ${produk.nama_produk} dihapus")
            } catch (e: Exception) {
                showSnackbar("Gagal menghapus produk: ${e.localizedMessage ?: "Kesalahan database"}")
            }
        }
    }

    fun addKurir(
        nama: String,
        komisi: Double,
        email: String = "",
        aksesAktif: Boolean = true
    ) {
        val trimmedNama = nama.trim()
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
        if (isOwnerEmail(kurir.email)) {
            showSnackbar("Email pemilik depot tidak bisa didaftarkan sebagai karyawan.")
            return
        }
        launchGuardedAction("Memperbarui data kurir...") {
            repository.updateKurir(kurir.copy(email = kurir.email.trim().lowercase()))
            syncEngine.syncStaffLinksNow()
            showSnackbar("Data kurir ${kurir.nama} diperbarui")
        }
    }

    fun deleteKurir(kurir: Kurir) {
        launchGuardedAction("Menghapus kurir...") {
            repository.deleteKurir(kurir)
            syncEngine.syncStaffLinksNow()
            showSnackbar("Kurir ${kurir.nama} dihapus")
        }
    }

    fun addPengeluaran(namaItem: String, kategori: String, nominal: Double, tanggal: Long = System.currentTimeMillis()) {
        if (namaItem.isBlank() || nominal <= 0) return
        launchGuardedAction("Mencatat pengeluaran...") {
            repository.insertPengeluaran(
                Pengeluaran(
                    tanggal = tanggal,
                    nama_item = namaItem.trim(),
                    kategori = kategori,
                    nominal = nominal
                )
            )
            showSnackbar("Pengeluaran dicatat")
        }
    }

    fun updatePengeluaran(pengeluaran: Pengeluaran) {
        launchGuardedAction("Memperbarui pengeluaran...") {
            repository.updatePengeluaran(pengeluaran)
            showSnackbar("Pengeluaran diperbarui")
        }
    }

    fun deletePengeluaran(pengeluaran: Pengeluaran) {
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
        val cleanName = nama.trim()
        if (cleanName.isBlank() || intervalHari < 1) return
        val safeInterval = intervalHari.coerceIn(1, 3650)
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
        val cleanName = namaBaru.trim()
        if (cleanName.isBlank() || intervalHariBaru < 1) return
        val safeInterval = intervalHariBaru.coerceIn(1, 3650)
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
                catatan = catatan
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

    fun importJsonData(jsonString: String, onFinished: ((com.example.util.ImportResult) -> Unit)? = null) {
        launchGuardedAction("Mengimpor data cadangan...") {
            val result = com.example.util.ExportHelper.importJsonData(getApplication(), db, jsonString)
            if (result.success) {
                repository.reconcileKasbonDiscrepancies()
                repository.enqueueAllLocalDataForSync()
            }
            showSnackbar(result.message)
            onFinished?.invoke(result)
        }
    }

    fun syncToCloud(context: Context, userId: String) {
        runCloudSync(context, userId, pushLocalFirst = true)
    }

    fun restoreFromCloud(context: Context, userId: String) {
        runCloudSync(context, userId, pushLocalFirst = false)
    }

    fun clearCloudSyncError() {
        _cloudSyncError.value = null
    }

    private fun runCloudSync(context: Context, userId: String, pushLocalFirst: Boolean) {
        if (_isSyncingCloud.value) return
        _isSyncingCloud.value = true
        _activeSyncPushMode.value = pushLocalFirst
        _cloudSyncError.value = null
        _isBusyAction.value = true
        _busyActionMessage.value = if (pushLocalFirst) {
            "Menyinkronkan data ke Cloud..."
        } else {
            "Mengambil data dari Cloud..."
        }
        viewModelScope.launch {
            try {
                val res = syncEngine.forceSyncNow(pushLocalFirst = pushLocalFirst)
                res.onSuccess { summary ->
                    _lastSyncSummary.value = summary
                    _cloudSyncError.value = null
                    val message = if (pushLocalFirst) {
                        "Sinkronisasi selesai (${summary.pelangganCount} pelanggan, ${summary.transaksiCount} transaksi)"
                    } else {
                        "Data Cloud diterapkan (${summary.pelangganCount} pelanggan, ${summary.transaksiCount} transaksi)"
                    }
                    showSnackbar(message)
                }.onFailure { err ->
                    val errText = "Gagal sinkronisasi: ${err.message ?: "Periksa koneksi internet"}"
                    _cloudSyncError.value = errText
                    showSnackbar(errText)
                }
            } catch (e: Exception) {
                val errText = "Koneksi gagal: ${e.message ?: "Periksa jaringan"}"
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
            for (detail in customerTxs) {
                if (detail.transaksi.status.equals("Kasbon", ignoreCase = true)) {
                    unpaidTotal += detail.transaksi.remainingKasbonAmount()
                }
            }
            val effectiveKasbon = if (unpaidTotal > 0.0) unpaidTotal else p.sisa_kasbon
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
        val activeTx = transaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
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
        val activeTx = transaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
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

    fun deleteAllData() {
        viewModelScope.launch {
            try {
                repository.deleteAllLocalData()
                showSnackbar("Seluruh data lokal dihapus")
            } catch (e: Exception) {
                showSnackbar("Gagal menghapus data: ${e.message ?: "Terjadi kesalahan"}")
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
