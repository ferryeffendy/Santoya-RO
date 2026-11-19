package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.entity.Pelanggan
import com.example.data.entity.TransaksiDetail
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Konteks tindak lanjut WhatsApp CRM agar nada dan isi pesan otomatis menyesuaikan kondisi pelanggan.
 */
enum class CrmFollowUpContext {
    JATUH_TEMPO,
    TERLAMBAT,
    PASIF,
    UMUM
}

/**
 * Filter status pelanggan pada tab Jadwal CRM.
 */
enum class CrmJadwalFilter {
    SEMUA,
    TERLAMBAT,
    HARI_INI_BESOK,
    BELUM_DIHUBUNGI
}

/**
 * Mode pengurutan daftar pelanggan pada tab Jadwal CRM.
 */
enum class CrmJadwalSort {
    PRIORITAS,
    TERLAMBAT_LAMA
}

/**
 * Segmen pelanggan pasif agar pelanggan lama yang berhenti pesan tidak tercampur prospek baru.
 */
enum class CrmPasifSegment {
    SEMUA,
    PERNAH_RUTIN,
    BELUM_PERNAH_ORDER
}

/**
 * Kriteria pengurutan daftar pelanggan pasif pada tab Pasif CRM.
 */
enum class CrmPasifSort {
    TERLAMA_PASIF,
    GALON_TERBANYAK,
    NAMA_AZ
}

/**
 * Filter rentang waktu untuk analisis ranking loyalitas pelanggan.
 */
enum class CrmRankingPeriod {
    SEMUA,
    BULAN_INI,
    TIGA_PULUH_HARI
}

/**
 * Kriteria pengurutan peringkat pelanggan pada tab Ranking.
 */
enum class CrmRankingSort {
    GALON,
    OMZET
}

/**
 * Entri teragregasi pelanggan pasif beserta histori pesanan dan prediksi siklusnya.
 */
data class PasifPelangganEntry(
    val pelanggan: Pelanggan,
    val daysPassive: Long,
    val lastOrderMillis: Long?,
    val totalOrder: Int,
    val totalGalon: Int,
    val prediction: CrmOrderPrediction
)

/**
 * Entri peringkat loyalitas pelanggan untuk periode dan kriteria urut terpilih.
 */
data class RankingPelangganEntry(
    val rank: Int,
    val pelanggan: Pelanggan,
    val totalGalon: Int,
    val totalOrder: Int,
    val totalOmzet: Double,
    val averageIntervalDays: Int?
)

/**
 * Utilitas murni CRM Santoya RO untuk template pesan WhatsApp kontekstual,
 * validasi status sudah dihubungi dalam siklus berjalan, segmentasi pasif, dan ranking periode.
 */
object CrmFollowUpHelper {

    const val CONTACTED_VALID_WINDOW_MILLIS: Long = 86_400_000L

    /**
     * Menentukan konteks tindak lanjut otomatis berdasarkan status prediksi order pelanggan.
     */
    fun resolveFollowUpContext(
        prediction: CrmOrderPrediction,
        isPassiveTab: Boolean = false
    ): CrmFollowUpContext {
        if (isPassiveTab) return CrmFollowUpContext.PASIF
        return when (prediction.status) {
            CrmOrderStatus.OVERDUE -> CrmFollowUpContext.TERLAMBAT
            CrmOrderStatus.DUE_SOON -> CrmFollowUpContext.JATUH_TEMPO
            CrmOrderStatus.ACTIVE, CrmOrderStatus.INSUFFICIENT_DATA -> CrmFollowUpContext.UMUM
        }
    }

    /**
     * Mengevaluasi apakah pelanggan masih berstatus "Sudah Dihubungi" untuk siklus order saat ini.
     * Status otomatis kedaluwarsa setelah 24 jam atau langsung bersih jika ada transaksi baru setelah dihubungi.
     */
    fun isContactedInCurrentCycle(
        lastContactedMillis: Long?,
        lastOrderMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (lastContactedMillis == null || lastContactedMillis <= 0L) return false
        if (lastContactedMillis > nowMillis + 60_000L) return false
        val ageMillis = nowMillis - lastContactedMillis
        if (ageMillis > CONTACTED_VALID_WINDOW_MILLIS) return false
        if (lastOrderMillis != null && lastContactedMillis <= lastOrderMillis) return false
        return true
    }

    /**
     * Menyusun pesan WhatsApp berbahasa Indonesia sesuai konteks jadwal jatuh tempo, terlambat, atau pasif.
     */
    fun buildContextualWhatsAppMessage(
        customerName: String,
        prediction: CrmOrderPrediction,
        followUpContext: CrmFollowUpContext,
        passiveDays: Long? = null
    ): String {
        val cleanName = customerName.trim().ifBlank { "Kak" }
        val avgGallons = prediction.avgGallonsPerOrder?.takeIf { it > 0 }
        val galonSuffix = if (avgGallons != null) " (biasanya $avgGallons galon)" else ""

        return when (followUpContext) {
            CrmFollowUpContext.JATUH_TEMPO -> {
                val intervalPart = prediction.averageIntervalDays?.let {
                    " Biasanya isi ulang sekitar tiap $it hari$galonSuffix."
                }.orEmpty()
                "Halo Kak $cleanName, dari Depot Santoya RO.$intervalPart Apakah hari ini perlu antar isi ulang galon? Siap kami kirim."
            }
            CrmFollowUpContext.TERLAMBAT -> {
                val overdueDays = (-(prediction.daysUntilExpected ?: -1)).coerceAtLeast(1)
                "Halo Kak $cleanName, dari Depot Santoya RO. Jadwal isi ulang rutinnya lewat $overdueDays hari$galonSuffix. Apakah stok galon sudah habis dan mau diantar hari ini?"
            }
            CrmFollowUpContext.PASIF -> {
                if (passiveDays != null && passiveDays < 999L) {
                    "Halo Kak $cleanName, apa kabar? Sudah $passiveDays hari belum pesan galon lagi di Santoya RO. Kalau butuh isi ulang air RO segar siap antar, kabari ya Kak!"
                } else {
                    "Halo Kak $cleanName, dari Depot Santoya RO. Jika butuh isi ulang air minum RO segar siap antar ke lokasi, kabari kami ya Kak!"
                }
            }
            CrmFollowUpContext.UMUM -> {
                "Halo Kak $cleanName, dari Depot Santoya RO. Ada yang bisa kami bantu untuk pengantaran galon hari ini?"
            }
        }
    }

    /**
     * Membuka WhatsApp dengan pesan kontekstual (fallback ke Dialer jika aplikasi WA tidak tersedia)
     * sekaligus memicu callback [onMarkContacted] untuk mencatat status hubungi lokal.
     */
    fun launchWhatsAppFollowUp(
        context: Context,
        pelanggan: Pelanggan,
        message: String,
        onMarkContacted: (() -> Unit)? = null
    ) {
        if (pelanggan.no_hp.isBlank()) return
        onMarkContacted?.invoke()
        val formattedPhone = PelangganAuditHelper.normalizePhoneDigits(pelanggan.no_hp)
        try {
            val waUri = Uri.parse(
                "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"
            )
            context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
        } catch (_: Exception) {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pelanggan.no_hp}")))
        }
    }

    /**
     * Menyaring dan mengurutkan daftar pelanggan pada tab Jadwal berdasarkan status tindak lanjut dan mode urut.
     */
    fun filterAndSortJadwal(
        jadwalUiModels: List<com.example.viewmodel.PelangganUiModel>,
        filter: CrmJadwalFilter,
        sort: CrmJadwalSort,
        contactedTimestamps: Map<Int, Long>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<com.example.viewmodel.PelangganUiModel> {
        val filtered = jadwalUiModels.filter { model ->
            when (filter) {
                CrmJadwalFilter.SEMUA -> true
                CrmJadwalFilter.TERLAMBAT -> model.crmPrediction.status == CrmOrderStatus.OVERDUE
                CrmJadwalFilter.HARI_INI_BESOK -> model.crmPrediction.status == CrmOrderStatus.DUE_SOON
                CrmJadwalFilter.BELUM_DIHUBUNGI -> !isContactedInCurrentCycle(
                    lastContactedMillis = contactedTimestamps[model.pelanggan.id],
                    lastOrderMillis = model.crmPrediction.lastOrderMillis,
                    nowMillis = nowMillis
                )
            }
        }

        return when (sort) {
            CrmJadwalSort.PRIORITAS -> filtered.sortedWith(
                compareByDescending<com.example.viewmodel.PelangganUiModel> { it.crmPrediction.priorityScore }
                    .thenBy { it.crmPrediction.daysUntilExpected ?: Int.MAX_VALUE }
                    .thenBy { it.pelanggan.nama.lowercase() }
            )
            CrmJadwalSort.TERLAMBAT_LAMA -> filtered.sortedWith(
                compareBy<com.example.viewmodel.PelangganUiModel> { it.crmPrediction.daysUntilExpected ?: Int.MAX_VALUE }
                    .thenByDescending { it.crmPrediction.priorityScore }
                    .thenBy { it.pelanggan.nama.lowercase() }
            )
        }
    }

    /**
     * Menyusun daftar pelanggan pasif yang dipisahkan antara pernah bertransaksi dan belum pernah order,
     * serta diurutkan sesuai kriteria [sort].
     */
    fun buildPassiveCustomerEntries(
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>,
        minDays: Long,
        segment: CrmPasifSegment = CrmPasifSegment.SEMUA,
        sort: CrmPasifSort = CrmPasifSort.TERLAMA_PASIF,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<PasifPelangganEntry> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val txByCust = HashMap<Int, MutableList<TransaksiDetail>>()
        for (detail in transaksiList) {
            val tx = detail.transaksi
            if (KaryawanScopeHelper.isDeletedTransaksi(tx) || tx.jumlah <= 0) continue
            val cid = tx.id_pelanggan ?: continue
            txByCust.getOrPut(cid) { ArrayList() }.add(detail)
        }

        val comparator: Comparator<PasifPelangganEntry> = when (sort) {
            CrmPasifSort.TERLAMA_PASIF -> compareBy<PasifPelangganEntry> { it.lastOrderMillis == null }
                .thenByDescending { it.daysPassive }
                .thenByDescending { it.totalGalon }
                .thenBy { it.pelanggan.nama.lowercase() }
            CrmPasifSort.GALON_TERBANYAK -> compareByDescending<PasifPelangganEntry> { it.totalGalon }
                .thenByDescending { it.totalOrder }
                .thenBy { it.pelanggan.nama.lowercase() }
            CrmPasifSort.NAMA_AZ -> compareBy<PasifPelangganEntry> { it.pelanggan.nama.lowercase() }
                .thenByDescending { it.daysPassive }
        }

        return pelangganList
            .asSequence()
            .filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) }
            .mapNotNull { cust ->
                val custTxs = txByCust[cust.id].orEmpty()
                val lastOrderMillis = custTxs.maxOfOrNull { it.transaksi.tanggal }
                val daysPassive = if (lastOrderMillis != null) {
                    val lastDate = Instant.ofEpochMilli(lastOrderMillis).atZone(zoneId).toLocalDate()
                    ChronoUnit.DAYS.between(lastDate, today).coerceAtLeast(0L)
                } else {
                    999L
                }
                if (daysPassive < minDays) return@mapNotNull null

                val hasOrderedBefore = lastOrderMillis != null
                when (segment) {
                    CrmPasifSegment.SEMUA -> Unit
                    CrmPasifSegment.PERNAH_RUTIN -> if (!hasOrderedBefore) return@mapNotNull null
                    CrmPasifSegment.BELUM_PERNAH_ORDER -> if (hasOrderedBefore) return@mapNotNull null
                }

                val prediction = FormatUtils.calculateCrmPredictionForCustomer(
                    customerTransaksiList = custTxs,
                    today = today,
                    zone = zoneId
                )
                PasifPelangganEntry(
                    pelanggan = cust,
                    daysPassive = daysPassive,
                    lastOrderMillis = lastOrderMillis,
                    totalOrder = custTxs.size,
                    totalGalon = custTxs.sumOf { it.transaksi.jumlah },
                    prediction = prediction
                )
            }
            .sortedWith(comparator)
            .toList()
    }

    /**
     * Menyusun peringkat loyalitas pelanggan berdasarkan rentang waktu [period] dan kriteria [sort].
     */
    fun buildRankedCustomerEntries(
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>,
        period: CrmRankingPeriod = CrmRankingPeriod.SEMUA,
        sort: CrmRankingSort = CrmRankingSort.GALON,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<RankingPelangganEntry> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val periodStartMillis = when (period) {
            CrmRankingPeriod.SEMUA -> 0L
            CrmRankingPeriod.BULAN_INI -> today.withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            CrmRankingPeriod.TIGA_PULUH_HARI -> today.minusDays(29).atStartOfDay(zoneId).toInstant().toEpochMilli()
        }

        val allTxByCust = HashMap<Int, MutableList<TransaksiDetail>>()
        val periodTxByCust = HashMap<Int, MutableList<TransaksiDetail>>()

        for (detail in transaksiList) {
            val tx = detail.transaksi
            if (KaryawanScopeHelper.isDeletedTransaksi(tx) || tx.jumlah <= 0) continue
            val cid = tx.id_pelanggan ?: continue
            allTxByCust.getOrPut(cid) { ArrayList() }.add(detail)
            if (tx.tanggal >= periodStartMillis) {
                periodTxByCust.getOrPut(cid) { ArrayList() }.add(detail)
            }
        }

        val raw = pelangganList
            .asSequence()
            .filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) }
            .mapNotNull { cust ->
                val periodTxs = periodTxByCust[cust.id].orEmpty()
                if (period != CrmRankingPeriod.SEMUA && periodTxs.isEmpty()) {
                    return@mapNotNull null
                }
                val totalGalon = periodTxs.sumOf { it.transaksi.jumlah }
                val totalOmzet = periodTxs.sumOf { it.transaksi.total_harga }
                val prediction = FormatUtils.calculateCrmPredictionForCustomer(
                    customerTransaksiList = allTxByCust[cust.id].orEmpty(),
                    today = today,
                    zone = zoneId
                )
                RankingPelangganEntry(
                    rank = 0,
                    pelanggan = cust,
                    totalGalon = totalGalon,
                    totalOrder = periodTxs.size,
                    totalOmzet = totalOmzet,
                    averageIntervalDays = prediction.averageIntervalDays
                )
            }
            .sortedWith(
                when (sort) {
                    CrmRankingSort.GALON -> compareByDescending<RankingPelangganEntry> { it.totalGalon }
                        .thenByDescending { it.totalOmzet }
                        .thenBy { it.pelanggan.nama.lowercase() }
                    CrmRankingSort.OMZET -> compareByDescending<RankingPelangganEntry> { it.totalOmzet }
                        .thenByDescending { it.totalGalon }
                        .thenBy { it.pelanggan.nama.lowercase() }
                }
            )
            .toList()

        return raw.mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }
}
