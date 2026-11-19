package com.example.util

import com.example.data.entity.Pelanggan
import com.example.data.entity.TransaksiDetail

/**
 * Ringkasan perhitungan pembayaran kasbon termasuk pembatasan nilai masuk kas
 * maksimal sebesar sisa kasbon aktif dan perhitungan uang kembalian.
 */
data class KasbonPaymentBreakdown(
    val effectivePaid: Double,
    val receivedAmount: Double,
    val kembalian: Double,
    val remainingAfterPay: Double,
    val isLunas: Boolean
)

/**
 * Statistik belanja teragregasi per pelanggan dari transaksi non-terhapus.
 */
data class CustomerSpendStats(
    val totalGalon: Int,
    val totalOrder: Int,
    val totalOmzet: Double,
    val lastOrderMillis: Long?
)

/**
 * Mode pengurutan daftar pelanggan pada tab Semua.
 */
enum class SemuaPelangganSort {
    NAMA_AZ,
    KASBON_TERTINGGI,
    TERBARU
}

/**
 * Utilitas murni untuk perhitungan akurasi data pelanggan, pembagian bayar kasbon + kembalian,
 * deteksi potensi duplikasi data pelanggan, dan agregasi riwayat transaksi aktif.
 */
object PelangganAuditHelper {

    /**
     * Mengurutkan daftar [PelangganUiModel] sesuai mode [sort] dengan tie-breaker nama alfabetis.
     */
    fun sortPelangganUiModels(
        models: List<com.example.viewmodel.PelangganUiModel>,
        sort: SemuaPelangganSort
    ): List<com.example.viewmodel.PelangganUiModel> {
        return when (sort) {
            SemuaPelangganSort.NAMA_AZ -> models.sortedBy { it.pelanggan.nama.lowercase() }
            SemuaPelangganSort.KASBON_TERTINGGI -> models.sortedWith(
                compareByDescending<com.example.viewmodel.PelangganUiModel> { it.pelanggan.sisa_kasbon }
                    .thenBy { it.pelanggan.nama.lowercase() }
            )
            SemuaPelangganSort.TERBARU -> models.sortedWith(
                compareByDescending<com.example.viewmodel.PelangganUiModel> {
                    it.crmPrediction.lastOrderMillis ?: it.pelanggan.updatedAt
                }
                    .thenByDescending { it.pelanggan.id }
                    .thenBy { it.pelanggan.nama.lowercase() }
            )
        }
    }

    /**
     * Menghitung nominal pelunasan kasbon efektif (maksimal sebesar [sisaKasbon]),
     * uang diterima, kembalian, dan sisa tagihan akhir setelah bayar.
     */
    fun calculateKasbonPaymentBreakdown(
        sisaKasbon: Double,
        inputNominal: Double
    ): KasbonPaymentBreakdown {
        val currentDebt = sisaKasbon.coerceAtLeast(0.0)
        val received = inputNominal.coerceAtLeast(0.0)
        val effectivePaid = if (currentDebt > 0.0) received.coerceAtMost(currentDebt) else 0.0
        val kembalian = (received - effectivePaid).coerceAtLeast(0.0)
        val remainingAfter = (currentDebt - effectivePaid).coerceAtLeast(0.0)
        return KasbonPaymentBreakdown(
            effectivePaid = effectivePaid,
            receivedAmount = received,
            kembalian = kembalian,
            remainingAfterPay = remainingAfter,
            isLunas = currentDebt > 0.0 && remainingAfter <= 0.0 && effectivePaid > 0.0
        )
    }

    /**
     * Menormalisasi input harga khusus pelanggan sehingga nilai 0 atau negatif
     * dianggap memakai harga galon normal (`null`).
     */
    fun normalizeSpecialPrice(rawPrice: Double?): Double? {
        return rawPrice?.takeIf { it > 0.0 }
    }

    /**
     * Menormalisasi nomor HP dari input bebas atau buku kontak perangkat (mis. `+62 812-3456-7890`)
     * menjadi format lokal Indonesia (`081234567890`). Mengembalikan string kosong jika tidak valid.
     */
    fun formatLocalPhoneNumber(rawPhone: String?): String {
        val cleaned = rawPhone.orEmpty().trim().replace(Regex("[^0-9+]"), "")
        if (cleaned.isBlank()) return ""
        val local = when {
            cleaned.startsWith("+62") -> "0" + cleaned.substring(3)
            cleaned.startsWith("62") -> "0" + cleaned.substring(2)
            else -> cleaned
        }.replace("+", "")
        return if (local.length >= 6) local else ""
    }

    /**
     * Menormalisasi nomor HP Indonesia (`08...` -> `628...`) untuk komparasi dan tautan WhatsApp.
     */
    fun normalizePhoneDigits(rawPhone: String?): String {
        val digits = rawPhone.orEmpty().replace(Regex("[^0-9]"), "")
        if (digits.isBlank()) return ""
        return if (digits.startsWith("0")) {
            "62${digits.substring(1)}"
        } else {
            digits
        }
    }

    /**
     * Mencari pelanggan aktif lain yang sudah memakai nomor HP identik (berbasis digit ternormalisasi).
     */
    fun findDuplicatePhoneOwner(
        noHp: String,
        existingList: List<Pelanggan>,
        excludeId: Int? = null
    ): Pelanggan? {
        val normalizedPhone = normalizePhoneDigits(noHp)
        if (normalizedPhone.length < 8) return null
        return existingList.firstOrNull { candidate ->
            if (excludeId != null && candidate.id == excludeId) return@firstOrNull false
            if (KaryawanScopeHelper.isSoftDeletedPelanggan(candidate)) return@firstOrNull false
            normalizePhoneDigits(candidate.no_hp) == normalizedPhone
        }
    }

    /**
     * Mendeteksi apakah terdapat pelanggan aktif lain yang memiliki nama atau nomor HP identik
     * guna membantu pemilik mencegah duplikasi data pelanggan.
     */
    fun findPotentialDuplicatePelanggan(
        nama: String,
        noHp: String,
        existingList: List<Pelanggan>,
        excludeId: Int? = null
    ): Pelanggan? {
        val normalizedName = nama.trim().lowercase()
        val normalizedPhone = normalizePhoneDigits(noHp)
        if (normalizedName.isBlank() && normalizedPhone.isBlank()) return null

        return existingList.firstOrNull { candidate ->
            if (excludeId != null && candidate.id == excludeId) return@firstOrNull false
            if (KaryawanScopeHelper.isSoftDeletedPelanggan(candidate)) return@firstOrNull false

            val candidatePhone = normalizePhoneDigits(candidate.no_hp)
            val phoneMatch = normalizedPhone.length >= 8 && candidatePhone == normalizedPhone
            val nameMatch = normalizedName.length >= 2 &&
                candidate.nama.trim().lowercase() == normalizedName
            phoneMatch || nameMatch
        }
    }

    /**
     * Mengagregasi statistik volume galon, jumlah order, total omzet, dan tanggal order terakhir
     * per pelanggan dengan mengeksklusi transaksi bertanda hapus.
     */
    fun buildCustomerSpendStats(
        transaksiList: List<TransaksiDetail>
    ): Map<Int, CustomerSpendStats> {
        val grouped = HashMap<Int, MutableList<TransaksiDetail>>()
        for (detail in transaksiList) {
            val tx = detail.transaksi
            if (KaryawanScopeHelper.isDeletedTransaksi(tx) || tx.jumlah <= 0) continue
            val cid = tx.id_pelanggan ?: continue
            grouped.getOrPut(cid) { ArrayList() }.add(detail)
        }
        return grouped.mapValues { (_, list) ->
            var galon = 0
            var omzet = 0.0
            var lastDate: Long? = null
            for (item in list) {
                galon += item.transaksi.jumlah
                omzet += item.transaksi.total_harga
                val tgl = item.transaksi.tanggal
                if (lastDate == null || tgl > lastDate) {
                    lastDate = tgl
                }
            }
            CustomerSpendStats(
                totalGalon = galon,
                totalOrder = list.size,
                totalOmzet = omzet,
                lastOrderMillis = lastDate
            )
        }
    }

    /**
     * Menyaring seluruh nota kasbon aktif (non-terhapus) milik [pelangganId]
     * terurut kronologis dari yang paling lama ke terbaru (urutan FIFO).
     */
    fun filterActiveUnpaidKasbonNotes(
        pelangganId: Int,
        transaksiList: List<TransaksiDetail>
    ): List<TransaksiDetail> {
        return transaksiList
            .filter {
                !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) &&
                    it.transaksi.id_pelanggan == pelangganId &&
                    it.transaksi.status.equals("Kasbon", ignoreCase = true)
            }
            .sortedWith(compareBy<TransaksiDetail> { it.transaksi.tanggal }.thenBy { it.transaksi.id })
    }
}
