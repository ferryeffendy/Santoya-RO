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
    private const val TX_DEL_PREFIX = "DEL:"
    private const val PELANGGAN_DEL_PREFIX = "[DIHAPUS:"

    fun normalizeEmail(raw: String?): String {
        val clean = raw?.trim()?.lowercase().orEmpty()
        if (clean.isBlank()) return ""
        return if (!clean.contains("@")) "$clean@gmail.com" else clean
    }

    fun resolveActiveKaryawan(userEmail: String?, kurirList: List<Kurir>): Kurir? {
        val normalizedUser = normalizeEmail(userEmail)
        if (normalizedUser.isBlank()) return null
        return kurirList.firstOrNull {
            it.akses_aktif && normalizeEmail(it.email) == normalizedUser
        } ?: kurirList.firstOrNull {
            normalizeEmail(it.email) == normalizedUser
        }
    }

    fun isKaryawanRole(
        syncRole: DepotRole,
        userEmail: String?,
        kurirList: List<Kurir>
    ): Boolean {
        if (syncRole == DepotRole.STAFF) return true
        val normalizedUser = normalizeEmail(userEmail)
        if (normalizedUser.isBlank()) return false
        return kurirList.any {
            it.akses_aktif && normalizeEmail(it.email) == normalizedUser
        }
    }

    fun isDeletedTransaksi(transaksi: Transaksi): Boolean {
        return transaksi.status.equals(STATUS_DIHAPUS, ignoreCase = true)
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
        return pelanggan.alamat.trimStart().startsWith(PELANGGAN_DEL_PREFIX)
    }

    fun extractDeletedByPelanggan(pelanggan: Pelanggan): String? {
        if (!isSoftDeletedPelanggan(pelanggan)) return null
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

        val setoranTunaiHariIni = todayTxs.sumOf {
            if (it.transaksi.status.equals("Lunas", ignoreCase = true)) {
                it.transaksi.total_harga
            } else {
                it.transaksi.uangMuka.toDouble()
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
