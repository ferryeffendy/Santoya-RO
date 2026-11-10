package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.data.repository.SantoyaRepository
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

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

    private val repository: SantoyaRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = SantoyaRepository(db)
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

    // --- Transaksi Form State ---
    private val _transaksiTab = MutableStateFlow(0) // 0 = Catat, 1 = Riwayat
    val transaksiTab: StateFlow<Int> = _transaksiTab.asStateFlow()

    fun setTransaksiTab(tab: Int) {
        _transaksiTab.value = tab
    }

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

    private val _txMetodeBayar = MutableStateFlow("Cash") // Cash, COD, Transfer
    val txMetodeBayar: StateFlow<String> = _txMetodeBayar.asStateFlow()

    private val _txStatus = MutableStateFlow("Lunas") // Lunas, Kasbon
    val txStatus: StateFlow<String> = _txStatus.asStateFlow()

    fun setTxTanggal(time: Long) { _txTanggal.value = time }
    fun setTxPelangganId(id: Int?) {
        _txPelangganId.value = id
        if (id != null) {
            val cust = allPelanggan.value.find { it.id == id }
            if (cust != null) {
                if (cust.harga_khusus != null && cust.harga_khusus > 0.0) {
                    _txHargaSatuan.value = cust.harga_khusus
                }
                if (cust.id_kurir_langganan != null) {
                    _txKurirId.value = cust.id_kurir_langganan
                }
            }
        }
    }
    fun onPelangganSelected(id: Int?) = setTxPelangganId(id)
    fun setTxProdukId(id: Int?, defaultPrice: Double? = null) {
        _txProdukId.value = id
        if (defaultPrice != null) {
            _txHargaSatuan.value = defaultPrice
        }
    }
    fun setTxKurirId(id: Int?) { _txKurirId.value = id }
    fun setTxJumlah(jumlah: Int) { _txJumlah.value = jumlah.coerceAtLeast(1) }
    fun incrementTxJumlah() { _txJumlah.update { it + 1 } }
    fun decrementTxJumlah() { _txJumlah.update { (it - 1).coerceAtLeast(1) } }
    fun setTxHargaSatuan(harga: Double) { _txHargaSatuan.value = harga.coerceAtLeast(0.0) }
    fun setTxMetodeBayar(metode: String) {
        _txMetodeBayar.value = metode
        if (metode.isNotBlank()) {
            _txStatus.value = "Lunas"
        }
    }

    fun setTxStatus(status: String) {
        _txStatus.value = status
        if (status.equals("Kasbon", ignoreCase = true)) {
            _txMetodeBayar.value = ""
        } else if (status.equals("Lunas", ignoreCase = true) && _txMetodeBayar.value.isBlank()) {
            _txMetodeBayar.value = "Cash"
        }
    }

    // --- Feedback / Dialog States ---
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _receiptDialog = MutableStateFlow<ActiveReceiptDialog?>(null)
    val receiptDialog: StateFlow<ActiveReceiptDialog?> = _receiptDialog.asStateFlow()

    fun showReceiptDialog(dialog: ActiveReceiptDialog) {
        _receiptDialog.value = dialog
    }

    fun dismissReceiptDialog() {
        _receiptDialog.value = null
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // --- Submit Transaksi ---
    fun submitTransaksi(onSuccess: (Long) -> Unit) {
        val pelangganId = _txPelangganId.value
        val produkId = _txProdukId.value
        if (pelangganId == null) {
            showSnackbar("Pilih pelanggan terlebih dahulu.")
            return
        }
        if (produkId == null) {
            showSnackbar("Pilih produk terlebih dahulu.")
            return
        }

        val statusVal = _txStatus.value
        val metodeVal = if (statusVal.equals("Kasbon", ignoreCase = true)) {
            "-"
        } else {
            _txMetodeBayar.value.ifBlank { "Cash" }
        }

        viewModelScope.launch {
            val newId = repository.recordTransaksi(
                tanggal = _txTanggal.value,
                idPelanggan = pelangganId,
                idKurir = _txKurirId.value,
                idProduk = produkId,
                jumlah = _txJumlah.value,
                hargaSatuan = _txHargaSatuan.value,
                metodeBayar = metodeVal,
                status = statusVal
            )
            showSnackbar("Transaksi #${newId} berhasil dicatat!")
            // Reset to 1 galon & reset date & default Lunas/Cash
            _txJumlah.value = 1
            _txTanggal.value = System.currentTimeMillis()
            _txStatus.value = "Lunas"
            _txMetodeBayar.value = "Cash"
            onSuccess(newId)
        }
    }

    fun markTransaksiLunas(transaksiId: Int) {
        viewModelScope.launch {
            repository.markTransaksiLunas(transaksiId)
            showSnackbar("Transaksi #${transaksiId} ditandai LUNAS.")
        }
    }

    fun deleteTransaksi(transaksi: Transaksi) {
        viewModelScope.launch {
            repository.deleteTransaksi(transaksi)
            showSnackbar("Transaksi #${transaksi.id} dihapus.")
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
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateTransaksi(
                id = id,
                tanggal = tanggal,
                idPelanggan = idPelanggan,
                idKurir = idKurir,
                idProduk = idProduk,
                jumlah = jumlah,
                hargaSatuan = hargaSatuan,
                metodeBayar = metodeBayar,
                status = status
            )
            showSnackbar("Transaksi #$id berhasil diperbarui!")
            onSuccess()
        }
    }

    // --- Pelanggan Actions ---
    fun addPelanggan(
        nama: String,
        alamat: String,
        noHp: String,
        initialKasbon: Double = 0.0,
        hargaKhusus: Double? = null,
        idKurirLangganan: Int? = null
    ) {
        if (nama.isBlank()) return
        viewModelScope.launch {
            val id = repository.insertPelanggan(
                Pelanggan(
                    nama = nama.trim(),
                    alamat = alamat.trim(),
                    no_hp = noHp.trim(),
                    sisa_kasbon = initialKasbon,
                    harga_khusus = hargaKhusus,
                    id_kurir_langganan = idKurirLangganan
                )
            )
            _txPelangganId.value = id.toInt()
            if (hargaKhusus != null && hargaKhusus > 0.0) {
                _txHargaSatuan.value = hargaKhusus
            }
            if (idKurirLangganan != null) {
                _txKurirId.value = idKurirLangganan
            }
            showSnackbar("Pelanggan ${nama.trim()} berhasil ditambahkan!")
        }
    }

    fun updatePelanggan(pelanggan: Pelanggan) {
        viewModelScope.launch {
            repository.updatePelanggan(pelanggan)
            showSnackbar("Data ${pelanggan.nama} diperbarui.")
        }
    }

    fun deletePelanggan(pelanggan: Pelanggan) {
        viewModelScope.launch {
            repository.deletePelanggan(pelanggan)
            showSnackbar("Pelanggan ${pelanggan.nama} dihapus.")
        }
    }

    fun bayarKasbon(pelangganId: Int, nominalBayar: Double) {
        if (nominalBayar <= 0) return
        viewModelScope.launch {
            repository.bayarKasbonPelanggan(pelangganId, nominalBayar)
            showSnackbar("Pelunasan sebesar ${FormatUtils.formatRupiah(nominalBayar)} berhasil dicatat.")
        }
    }

    // --- Produk Actions ---
    fun addProduk(nama: String, harga: Double) {
        if (nama.isBlank() || harga <= 0) return
        viewModelScope.launch {
            repository.insertProduk(Produk(nama_produk = nama.trim(), harga = harga))
            showSnackbar("Produk $nama berhasil ditambahkan!")
        }
    }

    fun updateProduk(produk: Produk) {
        viewModelScope.launch {
            repository.updateProduk(produk)
            showSnackbar("Produk ${produk.nama_produk} diperbarui.")
        }
    }

    fun deleteProduk(produk: Produk) {
        viewModelScope.launch {
            try {
                if (_txProdukId.value == produk.id) {
                    _txProdukId.value = null
                }
                repository.deleteProduk(produk)
                showSnackbar("Produk ${produk.nama_produk} berhasil dihapus.")
            } catch (e: Exception) {
                showSnackbar("Gagal menghapus produk: ${e.localizedMessage ?: "Terjadi kesalahan database"}")
            }
        }
    }

    // --- Kurir Actions ---
    fun addKurir(nama: String, komisi: Double) {
        if (nama.isBlank()) return
        viewModelScope.launch {
            repository.insertKurir(Kurir(nama = nama.trim(), komisi_per_galon = komisi))
            showSnackbar("Kurir $nama berhasil ditambahkan!")
        }
    }

    fun updateKurir(kurir: Kurir) {
        viewModelScope.launch {
            repository.updateKurir(kurir)
            showSnackbar("Kurir ${kurir.nama} diperbarui.")
        }
    }

    fun deleteKurir(kurir: Kurir) {
        viewModelScope.launch {
            repository.deleteKurir(kurir)
            showSnackbar("Kurir ${kurir.nama} dihapus.")
        }
    }

    // --- Pengeluaran Actions ---
    fun addPengeluaran(item: String, kategori: String, nominal: Double, tanggal: Long = System.currentTimeMillis()) {
        if (item.isBlank() || nominal <= 0) return
        viewModelScope.launch {
            repository.insertPengeluaran(
                Pengeluaran(
                    tanggal = tanggal,
                    nama_item = item.trim(),
                    kategori = kategori,
                    nominal = nominal
                )
            )
            showSnackbar("Pengeluaran $item berhasil dicatat.")
        }
    }

    fun deletePengeluaran(pengeluaran: Pengeluaran) {
        viewModelScope.launch {
            repository.deletePengeluaran(pengeluaran)
            showSnackbar("Pengeluaran dihapus.")
        }
    }

    // --- Pelanggan Tab Filtering & Sorting ---
    fun getFilteredPelangganUiModels(query: String, list: List<PelangganUiModel>): List<PelangganUiModel> {
        if (query.isBlank()) return list
        val q = query.lowercase().trim()
        return list.filter {
            it.pelanggan.nama.lowercase().contains(q) ||
            it.pelanggan.alamat.lowercase().contains(q) ||
            it.pelanggan.no_hp.contains(q)
        }
    }

    fun getFilteredPelanggan(query: String, list: List<Pelanggan>): List<Pelanggan> {
        if (query.isBlank()) return list
        val q = query.lowercase().trim()
        return list.filter {
            it.nama.lowercase().contains(q) ||
            it.alamat.lowercase().contains(q) ||
            it.no_hp.contains(q)
        }
    }

    fun getSortedKasbonPelanggan(
        list: List<Pelanggan>,
        sortOption: KasbonSortOption,
        query: String = "",
        transaksiList: List<TransaksiDetail> = emptyList()
    ): List<Pelanggan> {
        // Map to store latest and earliest kasbon transaction dates per customer
        val kasbonDatesMap = transaksiList
            .filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
            .groupBy { it.transaksi.id_pelanggan }
            .mapValues { (_, txs) ->
                val dates = txs.map { it.transaksi.tanggal }
                Pair(dates.minOrNull() ?: Long.MAX_VALUE, dates.maxOrNull() ?: Long.MIN_VALUE)
            }

        val processedList = list.map { p ->
            val unpaid = transaksiList.filter {
                it.transaksi.id_pelanggan == p.id &&
                    it.transaksi.status.equals("Kasbon", ignoreCase = true)
            }
            val unpaidTotal = unpaid.sumOf { it.transaksi.total_harga }
            val effectiveKasbon = if (unpaidTotal > 0) unpaidTotal else p.sisa_kasbon
            
            // Store dates in a wrapper or just use them during sort if we keep p as Pelanggan
            if (effectiveKasbon != p.sisa_kasbon) p.copy(sisa_kasbon = effectiveKasbon) else p
        }

        val filtered = processedList.filter { it.sisa_kasbon > 0 }
            .filter {
                if (query.isBlank()) true else {
                    val q = query.lowercase().trim()
                    it.nama.lowercase().contains(q) || it.alamat.lowercase().contains(q) || it.no_hp.contains(q)
                }
            }

        return when (sortOption) {
            KasbonSortOption.TERBANYAK -> filtered.sortedByDescending { it.sisa_kasbon }
            KasbonSortOption.TERLAMA -> filtered.sortedBy { p -> kasbonDatesMap[p.id]?.first ?: Long.MAX_VALUE }
            KasbonSortOption.TERBARU -> filtered.sortedByDescending { p -> kasbonDatesMap[p.id]?.second ?: Long.MIN_VALUE }
        }
    }

    fun getTopRankingPelanggan(
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>
    ): List<Pair<Pelanggan, Int>> {
        val map = mutableMapOf<Int, Int>()
        transaksiList.forEach { t ->
            val custId = t.transaksi.id_pelanggan
            map[custId] = (map[custId] ?: 0) + t.transaksi.jumlah
        }
        return pelangganList.map { p ->
            Pair(p, map[p.id] ?: 0)
        }.sortedByDescending { it.second }
    }

    fun getPasifPelanggan(
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>,
        minDays: Long
    ): List<Pair<Pelanggan, Long>> {
        val lastTxMap = mutableMapOf<Int, Long>()
        transaksiList.forEach { t ->
            val custId = t.transaksi.id_pelanggan
            val existing = lastTxMap[custId] ?: 0L
            if (t.transaksi.tanggal > existing) {
                lastTxMap[custId] = t.transaksi.tanggal
            }
        }
        val now = System.currentTimeMillis()
        return pelangganList.mapNotNull { p ->
            val lastDate = lastTxMap[p.id]
            val days = if (lastDate != null) FormatUtils.daysSince(lastDate) else 999L
            if (days >= minDays) Pair(p, days) else null
        }.sortedByDescending { it.second }
    }

    fun importJsonData(jsonString: String, onFinished: ((com.example.util.ImportResult) -> Unit)? = null) {
        viewModelScope.launch {
            val db = AppDatabase.getInstance(getApplication())
            val result = com.example.util.ExportHelper.importJsonData(getApplication(), db, jsonString)
            showSnackbar(result.message)
            onFinished?.invoke(result)
        }
    }

    fun getPelangganUiModels(
        pelangganList: List<Pelanggan>,
        transaksiList: List<com.example.data.entity.TransaksiDetail>
    ): List<PelangganUiModel> {
        return pelangganList.map { p ->
            PelangganUiModel(
                pelanggan = p,
                estimatedIntervalDays = com.example.util.FormatUtils.calculateEstimatedIntervalDays(p.id, transaksiList)
            )
        }
    }
}

data class PelangganUiModel(
    val pelanggan: Pelanggan,
    val estimatedIntervalDays: Int?
)
