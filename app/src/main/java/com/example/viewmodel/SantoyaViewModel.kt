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
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.data.repository.SantoyaRepository
import com.example.util.DepotRole
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.RealtimeSyncEngine
import com.example.util.RealtimeSyncState
import com.example.util.SyncSummary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SantoyaScreen {
    DASHBOARD,
    TRANSAKSI,
    PELANGGAN,
    KURIR,
    PRODUK,
    PENGELUARAN,
    LAPORAN,
    SETTINGS
}

enum class KasbonSortOption {
    TERBANYAK,
    TERLAMA,
    TERBARU
}

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

class SantoyaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val syncEngine = RealtimeSyncEngine(application, db)
    private val repository = SantoyaRepository(db, application) {
        syncEngine.scheduleFlush()
    }

    // --- Navigation ---
    private val _currentScreen = MutableStateFlow(SantoyaScreen.DASHBOARD)
    val currentScreen: StateFlow<SantoyaScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: SantoyaScreen) {
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

    val allKasbonTransaksi: StateFlow<List<TransaksiDetail>> = repository.allKasbonTransaksiWithDetail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Cloud Firestore Real-Time Sync State ---
    val realtimeSyncState: StateFlow<RealtimeSyncState> = syncEngine.state

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

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
        KaryawanScopeHelper.isKaryawanRole(sync.role, email, kurirs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activeKaryawan: StateFlow<Kurir?> = combine(
        allKurir,
        _currentUserEmail
    ) { kurirs, email ->
        KaryawanScopeHelper.resolveActiveKaryawan(email, kurirs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val scopedPelanggan: StateFlow<List<Pelanggan>> = combine(
        allPelanggan,
        isKaryawan,
        activeKaryawan
    ) { pelangganList, karyawan, kurir ->
        KaryawanScopeHelper.filterPelangganForRole(pelangganList, karyawan, kurir)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scopedTransaksi: StateFlow<List<TransaksiDetail>> = combine(
        allTransaksi,
        isKaryawan,
        activeKaryawan
    ) { transaksiList, karyawan, kurir ->
        KaryawanScopeHelper.filterTransaksiForRole(transaksiList, karyawan, kurir)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scopedPelangganTransaksi: StateFlow<List<TransaksiDetail>> = combine(
        allTransaksi,
        isKaryawan,
        activeKaryawan,
        scopedPelanggan
    ) { transaksiList, karyawan, kurir, ownedPelanggan ->
        val ownedIds = ownedPelanggan.map { it.id }.toSet()
        KaryawanScopeHelper.filterCustomerHistoryForRole(transaksiList, karyawan, kurir, ownedIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scopedPelangganUiModels: StateFlow<List<PelangganUiModel>> = combine(
        scopedPelanggan,
        scopedPelangganTransaksi
    ) { pelangganList, transaksiList ->
        getPelangganUiModels(pelangganList, transaksiList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.reconcileKasbonDiscrepancies()
            combine(allPelanggan, allTransaksi) { pelangganList, transaksiList ->
                val activeUnpaidByCust = transaksiList.asSequence()
                    .filter {
                        !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) &&
                            it.transaksi.status.equals("Kasbon", ignoreCase = true)
                    }
                    .groupBy { it.transaksi.id_pelanggan }
                pelangganList.any { p ->
                    val unpaidSum = activeUnpaidByCust[p.id]?.sumOf { it.transaksi.remainingKasbonAmount() } ?: 0.0
                    unpaidSum > p.sisa_kasbon.coerceAtLeast(0.0)
                }
            }.collect { hasDiscrepancy ->
                if (hasDiscrepancy) {
                    repository.reconcileKasbonDiscrepancies()
                }
            }
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
        viewModelScope.launch {
            try {
                val karyawanKurirId = if (isUserKaryawan()) resolveActiveKaryawanImmediate()?.id else null
                repository.createFastCashTransaksi(idProduk, jumlah, karyawanKurirId)
                showSnackbar("Jual cepat tercatat ($jumlah galon • Cash)")
            } catch (e: Exception) {
                showSnackbar("Gagal mencatat transaksi: ${e.message ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun getKasbonLogsByPelanggan(pelangganId: Int): Flow<List<KasbonLog>> =
        repository.getKasbonLogsByPelanggan(pelangganId)

    fun submitTransaksi(onSuccess: (TransaksiDetail?) -> Unit) {
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

        val statusVal = if (isBayarSebagian) "Kasbon" else _txStatus.value
        val metodeVal = when {
            isBayarSebagian -> "Bayar Sebagian"
            statusVal.equals("Kasbon", ignoreCase = true) -> "-"
            else -> _txMetodeBayar.value.ifBlank { "Cash" }
        }
        val uangMukaVal = if (isBayarSebagian) dpInput else 0L
        val sisaKasbonVal = when {
            isBayarSebagian -> (totalBelanjaLong - dpInput).coerceAtLeast(0L)
            statusVal.equals("Kasbon", ignoreCase = true) -> totalBelanjaLong.coerceAtLeast(0L)
            else -> 0L
        }

        val karyawanKurirId = if (karyawanMode) resolveActiveKaryawanImmediate()?.id else null
        val effectiveKurirId = karyawanKurirId ?: _txKurirId.value

        viewModelScope.launch {
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

    fun markTransaksiLunas(transaksiId: Int) {
        viewModelScope.launch {
            repository.markTransaksiLunas(transaksiId)
            showSnackbar("Transaksi ditandai lunas")
        }
    }

    fun deleteTransaksi(transaksi: Transaksi) {
        viewModelScope.launch {
            if (isUserKaryawan()) {
                val karyawanName = resolveActiveKaryawanImmediate()?.nama ?: "Karyawan"
                repository.softDeleteTransaksiByKaryawan(transaksi, karyawanName)
                showSnackbar("Transaksi dihapus (ditandai oleh $karyawanName)")
            } else {
                repository.deleteTransaksi(transaksi)
                showSnackbar("Transaksi dihapus")
            }
        }
    }

    fun restoreTransaksi(transaksi: Transaksi) {
        viewModelScope.launch {
            repository.restoreTransaksi(transaksi)
            showSnackbar("Transaksi berhasil dipulihkan")
        }
    }

    fun permanentDeleteTransaksi(transaksi: Transaksi) {
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
            repository.updatePelanggan(effectivePelanggan)
            showSnackbar("Data ${effectivePelanggan.nama} diperbarui")
        }
    }

    fun deletePelanggan(pelanggan: Pelanggan) {
        viewModelScope.launch {
            if (isUserKaryawan()) {
                val karyawanName = resolveActiveKaryawanImmediate()?.nama ?: "Karyawan"
                val ok = repository.softDeletePelangganByKaryawan(pelanggan, karyawanName)
                if (ok) {
                    showSnackbar("Pelanggan ${pelanggan.nama} dihapus (ditandai oleh $karyawanName)")
                } else {
                    showSnackbar("Pelanggan masih memiliki kasbon aktif. Lunasi terlebih dahulu sebelum menghapus.")
                }
            } else {
                repository.deletePelanggan(pelanggan)
                showSnackbar("Pelanggan ${pelanggan.nama} dihapus")
            }
        }
    }

    fun restorePelanggan(pelanggan: Pelanggan) {
        viewModelScope.launch {
            repository.restorePelanggan(pelanggan)
            showSnackbar("Pelanggan ${pelanggan.nama} dipulihkan")
        }
    }

    fun permanentDeletePelanggan(pelanggan: Pelanggan) {
        viewModelScope.launch {
            repository.deletePelanggan(pelanggan)
            showSnackbar("Pelanggan ${pelanggan.nama} dihapus permanen")
        }
    }

    fun bayarKasbon(pelangganId: Int, nominalBayar: Double, metodeBayar: String = "Cash") {
        if (nominalBayar <= 0) return
        viewModelScope.launch {
            repository.bayarKasbonPelanggan(pelangganId, nominalBayar, metodeBayar)
            showSnackbar("Pelunasan ${FormatUtils.formatRupiah(nominalBayar)} ($metodeBayar) dicatat")
        }
    }

    fun addProduk(nama: String, harga: Double) {
        if (nama.isBlank() || harga <= 0) return
        viewModelScope.launch {
            repository.insertProduk(Produk(nama_produk = nama.trim(), harga = harga))
            showSnackbar("Produk $nama ditambahkan")
        }
    }

    fun updateProduk(produk: Produk) {
        viewModelScope.launch {
            repository.updateProduk(produk)
            showSnackbar("Produk ${produk.nama_produk} diperbarui")
        }
    }

    fun deleteProduk(produk: Produk) {
        viewModelScope.launch {
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
        password: String = "",
        aksesAktif: Boolean = true
    ) {
        val trimmedNama = nama.trim()
        if (trimmedNama.isBlank()) return
        viewModelScope.launch {
            repository.insertKurir(
                Kurir(
                    nama = trimmedNama,
                    komisi_per_galon = komisi,
                    email = email.trim(),
                    password = password.trim(),
                    akses_aktif = aksesAktif,
                    skema_komisi = "TETAP",
                    aturan_komisi = ""
                )
            )
            showSnackbar("Kurir $trimmedNama ditambahkan")
        }
    }

    fun updateKurir(kurir: Kurir) {
        viewModelScope.launch {
            repository.updateKurir(kurir)
            showSnackbar("Data kurir ${kurir.nama} diperbarui")
        }
    }

    fun deleteKurir(kurir: Kurir) {
        viewModelScope.launch {
            repository.deleteKurir(kurir)
            showSnackbar("Kurir ${kurir.nama} dihapus")
        }
    }

    fun addPengeluaran(namaItem: String, kategori: String, nominal: Double, tanggal: Long = System.currentTimeMillis()) {
        if (namaItem.isBlank() || nominal <= 0) return
        viewModelScope.launch {
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
        viewModelScope.launch {
            repository.updatePengeluaran(pengeluaran)
            showSnackbar("Pengeluaran diperbarui")
        }
    }

    fun deletePengeluaran(pengeluaran: Pengeluaran) {
        viewModelScope.launch {
            repository.deletePengeluaran(pengeluaran)
            showSnackbar("Pengeluaran dihapus")
        }
    }

    fun importJsonData(jsonString: String, onFinished: ((com.example.util.ImportResult) -> Unit)? = null) {
        viewModelScope.launch {
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
        if (_isSyncingCloud.value) return
        _isSyncingCloud.value = true
        viewModelScope.launch {
            try {
                val res = syncEngine.forceSyncNow()
                res.onSuccess { summary ->
                    _lastSyncSummary.value = summary
                    showSnackbar("Sinkronisasi real-time selesai (${summary.pelangganCount} pelanggan, ${summary.transaksiCount} transaksi)")
                }.onFailure { err ->
                    showSnackbar("Gagal sinkronisasi: ${err.message ?: "Periksa koneksi internet"}")
                }
            } catch (e: Exception) {
                showSnackbar("Koneksi gagal: ${e.message ?: "Periksa jaringan"}")
            } finally {
                _isSyncingCloud.value = false
            }
        }
    }

    fun restoreFromCloud(context: Context, userId: String) {
        syncToCloud(context, userId)
    }

    fun getPelangganUiModels(
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>
    ): List<PelangganUiModel> {
        val activeTx = transaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
        return pelangganList.map { p ->
            val unpaidTotal = activeTx.asSequence()
                .filter {
                    it.transaksi.id_pelanggan == p.id &&
                        it.transaksi.status.equals("Kasbon", ignoreCase = true)
                }
                .sumOf { it.transaksi.remainingKasbonAmount() }
            val effectiveKasbon = if (unpaidTotal > 0) unpaidTotal else p.sisa_kasbon
            val syncedPelanggan = if (effectiveKasbon != p.sisa_kasbon) {
                p.copy(sisa_kasbon = effectiveKasbon)
            } else {
                p
            }
            val prediction = FormatUtils.calculateCrmPrediction(p.id, activeTx)
            PelangganUiModel(
                pelanggan = syncedPelanggan,
                estimatedIntervalDays = prediction.averageIntervalDays,
                crmPrediction = prediction
            )
        }
    }

    fun getFilteredPelangganUiModels(query: String, list: List<PelangganUiModel>): List<PelangganUiModel> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return list
        return list.filter {
            val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(it.pelanggan.alamat)
            val deletedBy = KaryawanScopeHelper.extractDeletedByPelanggan(it.pelanggan).orEmpty()
            it.pelanggan.nama.lowercase().contains(q) ||
                cleanAlamat.lowercase().contains(q) ||
                deletedBy.lowercase().contains(q) ||
                it.pelanggan.no_hp.lowercase().contains(q)
        }
    }

    fun getSortedKasbonPelanggan(
        pelangganList: List<Pelanggan>,
        sortOption: KasbonSortOption,
        query: String,
        transaksiList: List<TransaksiDetail>
    ): List<Pelanggan> {
        val q = query.trim().lowercase()
        val qDigits = q.filter { it.isDigit() }
        val activeTx = transaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
        val filtered = pelangganList
            .filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) && it.sisa_kasbon > 0 }
            .filter { p ->
                if (q.isBlank()) true
                else {
                    val hpDigits = p.no_hp.filter { it.isDigit() }
                    val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(p.alamat)
                    p.nama.lowercase().contains(q) ||
                        cleanAlamat.lowercase().contains(q) ||
                        p.no_hp.lowercase().contains(q) ||
                        (qDigits.isNotEmpty() && hpDigits.contains(qDigits)) ||
                        p.sisa_kasbon.toLong().toString().contains(q)
                }
            }
        return when (sortOption) {
            KasbonSortOption.TERBANYAK -> filtered.sortedByDescending { it.sisa_kasbon }
            KasbonSortOption.TERLAMA -> {
                filtered.sortedBy { p ->
                    activeTx.filter { it.transaksi.id_pelanggan == p.id && it.transaksi.status.equals("Kasbon", ignoreCase = true) }
                        .minOfOrNull { it.transaksi.tanggal } ?: Long.MAX_VALUE
                }
            }
            KasbonSortOption.TERBARU -> {
                filtered.sortedByDescending { p ->
                    activeTx.filter { it.transaksi.id_pelanggan == p.id && it.transaksi.status.equals("Kasbon", ignoreCase = true) }
                        .maxOfOrNull { it.transaksi.tanggal } ?: 0L
                }
            }
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
