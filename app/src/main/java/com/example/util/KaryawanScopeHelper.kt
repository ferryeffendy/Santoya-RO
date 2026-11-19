package com.example.util

import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail

data class KaryawanDashboardMetrics(
    val setoranTunaiHariIni: Double = 0.0,
    val pesananHariIni: Int = 0,
    val galonHariIni: Int = 0,
    val komisiHariIni: Double = 0.0,
    val pesananBulanIni: Int = 0,
    val galonBulanIni: Int = 0,
    val komisiBulanIni: Double = 0.0
)

data class RestoredTransaksiPayment(
    val status: String,
    val metodeBayar: String,
    val uangMuka: Long,
    val sisaKasbon: Long
)

object KaryawanScopeHelper {

    const val STATUS_DIHAPUS = "Dihapus"
    const val DEFAULT_STAFF_COMMISSION_PER_GALON = 1000.0

    private const val TX_DEL_PREFIX = "DEL:"
    private const val PELANGGAN_DEL_PREFIX = "[DIHAPUS:"

    private val STRICT_EMAIL_REGEX = Regex("^[a-z0-9_.+\\-]+@[a-z0-9\\-]+(\\.[a-z0-9\\-]+)+$")

    /**
     * Memvalidasi apakah string merupakan alamat email penuh yang sah (wajib mengandung `@` dan domain).
     */
    fun isValidEmail(raw: String?): Boolean {
        val clean = raw?.trim()?.lowercase().orEmpty()
        return clean.isNotBlank() && clean.length <= 160 && STRICT_EMAIL_REGEX.matches(clean)
    }

    /**
     * Menormalisasi alamat email menjadi huruf kecil tanpa spasi.
     * Mengembalikan string kosong bila input bukan alamat email penuh yang valid (tanpa auto-append `@gmail.com`).
     */
    fun normalizeEmail(raw: String?): String {
        val clean = raw?.trim()?.lowercase().orEmpty()
        if (!isValidEmail(clean)) return ""
        return clean
    }

    /**
     * Menghasilkan nama tampilan sementara dari bagian lokal alamat email karyawan (mis. `Mumus8800`).
     */
    fun defaultStaffDisplayName(email: String): String {
        val localPart = email.trim().substringBefore("@").trim()
        if (localPart.isBlank()) return "Karyawan"
        return localPart.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase() else ch.toString()
        }.take(120)
    }

    /**
     * Menghasilkan `syncId` deterministik untuk entri kurir berdasarkan alamat email karyawan.
     */
    fun defaultStaffSyncId(email: String): String {
        val localPart = email.trim().lowercase().substringBefore("@")
            .replace(Regex("[^a-z0-9_\\-]"), "_")
            .trim('_')
            .ifBlank { "staff" }
        return "kurir_staff_${localPart.take(80)}"
    }

    fun resolveActiveKaryawan(userEmail: String?, kurirList: List<Kurir>): Kurir? {
        val normalizedUser = normalizeEmail(userEmail)
        if (normalizedUser.isBlank()) return null
        return kurirList.firstOrNull {
            !it.isDeleted && it.akses_aktif && normalizeEmail(it.email) == normalizedUser
        } ?: kurirList.firstOrNull {
            !it.isDeleted && normalizeEmail(it.email) == normalizedUser
        }
    }

    fun isKaryawanRole(
        syncRole: DepotRole,
        userEmail: String?,
        ownerEmail: String?,
        kurirList: List<Kurir>
    ): Boolean {
        if (syncRole == DepotRole.STAFF) return true
        val normalizedUser = normalizeEmail(userEmail)
        if (normalizedUser.isBlank()) return false

        // Pemilik depot selalu owner, walaupun emailnya juga terdaftar sebagai kurir.
        val normalizedOwner = normalizeEmail(ownerEmail)
        if (normalizedOwner.isNotBlank() && normalizedOwner == normalizedUser) {
            return false
        }

        return kurirList.any {
            !it.isDeleted && it.akses_aktif && normalizeEmail(it.email) == normalizedUser
        }
    }

    fun isDeletedTransaksi(transaksi: Transaksi): Boolean {
        return transaksi.isDeleted || transaksi.status.equals(STATUS_DIHAPUS, ignoreCase = true)
    }

    fun encodeDeletedTransaksiMetadata(transaksi: Transaksi, karyawanName: String): String {
        val safeName = karyawanName.trim()
            .replace("|", " ")
            .replace("\n", " ")
            .ifBlank { "Karyawan" }

        val origCode = when {
            transaksi.status.equals("Kasbon", ignoreCase = true) &&
                transaksi.metode_bayar.equals("Bayar Sebagian", ignoreCase = true) -> {
                "DP:${transaksi.uangMuka.coerceAtLeast(0L)}"
            }
            transaksi.status.equals("Kasbon", ignoreCase = true) -> "K"
            transaksi.metode_bayar.equals("Transfer", ignoreCase = true) -> "T"
            transaksi.metode_bayar.equals("Cash", ignoreCase = true) || transaksi.metode_bayar.isBlank() || transaksi.metode_bayar == "-" -> "C"
            else -> transaksi.metode_bayar.trim().replace("|", " ").take(10)
        }
        val maxNameLen = (40 - TX_DEL_PREFIX.length - 1 - origCode.length).coerceAtLeast(4)
        return "$TX_DEL_PREFIX${safeName.take(maxNameLen)}|$origCode".take(40)
    }

    fun extractDeletedByTransaksi(transaksi: Transaksi): String? {
        if (!isDeletedTransaksi(transaksi)) return null
        transaksi.deletedBy?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        val meta = transaksi.metode_bayar.trim()
        if (meta.startsWith(TX_DEL_PREFIX)) {
            val payload = meta.removePrefix(TX_DEL_PREFIX)
            val namePart = payload.substringBefore("|").trim()
            if (namePart.isNotBlank()) return namePart
        } else if (meta.startsWith("Dihapus:", ignoreCase = true)) {
            val namePart = meta.substringAfter(":").trim()
            if (namePart.isNotBlank()) return namePart
        }
        return "Karyawan"
    }

    fun decodeRestoredTransaksiPayment(transaksi: Transaksi): RestoredTransaksiPayment {
        val meta = transaksi.metode_bayar.trim()
        val origCode = if (meta.startsWith(TX_DEL_PREFIX) && meta.contains("|")) {
            meta.substringAfter("|").trim()
        } else {
            "C"
        }
        val totalLong = transaksi.total_harga.toLong().coerceAtLeast(0L)
        return when {
            origCode == "K" -> RestoredTransaksiPayment(
                status = "Kasbon",
                metodeBayar = "-",
                uangMuka = 0L,
                sisaKasbon = totalLong
            )
            origCode.startsWith("DP:") -> {
                val dp = origCode.removePrefix("DP:").toLongOrNull()?.coerceIn(0L, totalLong) ?: 0L
                val sisa = (totalLong - dp).coerceAtLeast(0L)
                RestoredTransaksiPayment(
                    status = "Kasbon",
                    metodeBayar = if (dp > 0L) "Bayar Sebagian" else "-",
                    uangMuka = dp,
                    sisaKasbon = sisa
                )
            }
            origCode == "T" -> RestoredTransaksiPayment(
                status = "Lunas",
                metodeBayar = "Transfer",
                uangMuka = 0L,
                sisaKasbon = 0L
            )
            origCode == "C" || origCode.isBlank() -> RestoredTransaksiPayment(
                status = "Lunas",
                metodeBayar = "Cash",
                uangMuka = 0L,
                sisaKasbon = 0L
            )
            else -> RestoredTransaksiPayment(
                status = "Lunas",
                metodeBayar = origCode,
                uangMuka = 0L,
                sisaKasbon = 0L
            )
        }
    }

    fun isSoftDeletedPelanggan(pelanggan: Pelanggan): Boolean {
        return pelanggan.isDeleted || pelanggan.alamat.trimStart().startsWith(PELANGGAN_DEL_PREFIX)
    }

    fun extractDeletedByPelanggan(pelanggan: Pelanggan): String? {
        if (!isSoftDeletedPelanggan(pelanggan)) return null
        pelanggan.deletedBy?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        val trimmed = pelanggan.alamat.trimStart()
        val endIdx = trimmed.indexOf(']')
        if (endIdx > PELANGGAN_DEL_PREFIX.length) {
            val name = trimmed.substring(PELANGGAN_DEL_PREFIX.length, endIdx).trim()
            if (name.isNotBlank()) return name
        }
        return "Karyawan"
    }

    fun cleanPelangganAlamat(alamat: String): String {
        val trimmed = alamat.trimStart()
        if (!trimmed.startsWith(PELANGGAN_DEL_PREFIX)) return alamat
        val endIdx = trimmed.indexOf(']')
        return if (endIdx >= 0) {
            trimmed.substring(endIdx + 1).trimStart()
        } else {
            alamat
        }
    }

    fun formatDeletedPelangganAlamat(alamat: String, karyawanName: String): String {
        val cleanName = karyawanName.trim()
            .replace("]", "")
            .replace("[", "")
            .replace("\n", " ")
            .ifBlank { "Karyawan" }
            .take(40)
        val cleanOrigAlamat = cleanPelangganAlamat(alamat).trim()
        val prefix = "$PELANGGAN_DEL_PREFIX$cleanName]"
        return if (cleanOrigAlamat.isBlank()) {
            prefix.take(300)
        } else {
            "$prefix $cleanOrigAlamat".take(300)
        }
    }

    fun resolveAuthoritativeUnitPrice(
        pelanggan: Pelanggan?,
        produk: Produk?,
        fallbackPrice: Double = 6000.0
    ): Double {
        val customPrice = pelanggan?.harga_khusus
        if (customPrice != null && customPrice > 0.0) return customPrice
        val productPrice = produk?.harga
        if (productPrice != null && productPrice > 0.0) return productPrice
        return fallbackPrice
    }

    fun filterPelangganForRole(
        pelangganList: List<Pelanggan>,
        isKaryawan: Boolean,
        activeKurir: Kurir?
    ): List<Pelanggan> {
        if (!isKaryawan) return pelangganList
        if (activeKurir == null) return emptyList()
        return pelangganList.filter {
            !isSoftDeletedPelanggan(it) && it.id_kurir_langganan == activeKurir.id
        }
    }

    fun filterTransaksiForRole(
        transaksiList: List<TransaksiDetail>,
        isKaryawan: Boolean,
        activeKurir: Kurir?
    ): List<TransaksiDetail> {
        if (!isKaryawan) return transaksiList
        if (activeKurir == null) return emptyList()
        return transaksiList.filter {
            !isDeletedTransaksi(it.transaksi) && it.transaksi.id_kurir == activeKurir.id
        }
    }

    fun filterCustomerHistoryForRole(
        transaksiList: List<TransaksiDetail>,
        isKaryawan: Boolean,
        activeKurir: Kurir?,
        ownedPelangganIds: Set<Int>
    ): List<TransaksiDetail> {
        if (!isKaryawan) return transaksiList.filter { !isDeletedTransaksi(it.transaksi) }
        if (activeKurir == null) return emptyList()
        return transaksiList.filter {
            !isDeletedTransaksi(it.transaksi) &&
                (it.transaksi.id_kurir == activeKurir.id || it.transaksi.id_pelanggan in ownedPelangganIds)
        }
    }

    /**
     * Menghitung ringkasan performa harian & bulanan kurir, termasuk setoran uang tunai fisik hari ini
     * (tidak menghitung pembayaran non-tunai/Transfer yang langsung masuk ke rekening pemilik).
     */
    fun summarizeKaryawanDashboardMetrics(
        transaksiList: List<TransaksiDetail>,
        activeKurir: Kurir?,
        nowMillis: Long = System.currentTimeMillis()
    ): KaryawanDashboardMetrics {
        val activeTxs = transaksiList.filter { !isDeletedTransaksi(it.transaksi) }
        val courierTxs = if (activeKurir != null) {
            activeTxs.filter { it.transaksi.id_kurir == activeKurir.id }
        } else {
            activeTxs
        }
        val todayStart = FormatUtils.getStartOfDay(nowMillis)
        val todayEnd = FormatUtils.getEndOfDay(nowMillis)
        val monthStart = FormatUtils.getStartOfMonth(nowMillis)

        val todayTxs = courierTxs.filter { it.transaksi.tanggal in todayStart..todayEnd }
        val monthTxs = courierTxs.filter { it.transaksi.tanggal in monthStart..todayEnd }

        val setoranTunaiHariIni = todayTxs.sumOf { detail ->
            val tx = detail.transaksi
            val isNonCash = LaporanFinanceCalculator.isNonCashPaymentMethod(tx.metode_bayar)
            when {
                isNonCash -> 0.0
                tx.status.equals("Lunas", ignoreCase = true) -> tx.total_harga.coerceAtLeast(0.0)
                else -> tx.uangMuka.coerceAtLeast(0L).toDouble()
            }
        }
        val todayComp = KomisiCalculator.summarizeCourierCompensation(activeKurir, todayTxs)
        val monthComp = KomisiCalculator.summarizeCourierCompensation(activeKurir, monthTxs)

        return KaryawanDashboardMetrics(
            setoranTunaiHariIni = setoranTunaiHariIni,
            pesananHariIni = todayComp.totalOrders,
            galonHariIni = todayComp.totalGallons,
            komisiHariIni = todayComp.totalKomisi,
            pesananBulanIni = monthComp.totalOrders,
            galonBulanIni = monthComp.totalGallons,
            komisiBulanIni = monthComp.totalKomisi
        )
    }
}
