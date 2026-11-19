package com.example.util

import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class PerawatanStatus {
    TERLAMBAT,
    HARI_INI,
    MENDEKATI,
    AMAN
}

enum class PerawatanScheduleFilter {
    SEMUA,
    PERLU_TINDAKAN,
    TERLAMBAT,
    AMAN
}

enum class PerawatanHistoryPeriodFilter {
    SEMUA,
    BULAN_INI,
    SEMBILAN_PULUH_HARI,
    TAHUN_INI
}

enum class PerawatanFormError {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    INTERVAL_INVALID
}

data class PerawatanEvaluation(
    val item: PerawatanItem,
    val status: PerawatanStatus,
    val daysRemaining: Int,
    val isReminderDue: Boolean
)

data class PerawatanScheduleStats(
    val totalCount: Int,
    val overdueCount: Int,
    val dueTodayCount: Int,
    val soonCount: Int,
    val safeCount: Int,
    val dueActionCount: Int,
    val complianceRatio: Float
)

data class PerawatanComponentMetrics(
    val logCount: Int,
    val totalBiaya: Double,
    val lastCost: Double,
    val lastNote: String
)

data class PerawatanHistorySummary(
    val totalLogs: Int,
    val totalBiaya: Double,
    val paidLogsCount: Int,
    val freeLogsCount: Int,
    val averageBiaya: Double,
    val topComponentName: String?,
    val topComponentBiaya: Double
)

data class PerawatanFormValidation(
    val error: PerawatanFormError?,
    val cleanName: String,
    val intervalHari: Int?
) {
    val isValid: Boolean get() = error == null && intervalHari != null
}

/**
 * Utilitas perhitungan jadwal, pengingat H-3 / hari ini / terlambat, penundaan jadwal,
 * filter/pencarian, ringkasan riwayat, serta data awal perawatan mesin RO.
 */
object PerawatanScheduler {

    const val REMINDER_WINDOW_DAYS = 3
    const val MAX_SCHEDULE_NAME_LENGTH = 120
    const val MIN_INTERVAL_DAYS = 1
    const val MAX_INTERVAL_DAYS = 3650
    const val KATEGORI_PENGELUARAN_PERAWATAN = "Perawatan"

    data class DefaultTemplate(
        val syncId: String,
        val nama: String,
        val intervalHari: Int
    )

    val DEFAULT_TEMPLATES: List<DefaultTemplate> = listOf(
        DefaultTemplate("default_perawatan_cek_tds", "Cek TDS", 7),
        DefaultTemplate("default_perawatan_filter_sedimen", "Filter Sedimen", 30),
        DefaultTemplate("default_perawatan_cuci_tangki", "Cuci Tangki", 30),
        DefaultTemplate("default_perawatan_filter_karbon", "Filter Karbon", 60),
        DefaultTemplate("default_perawatan_membran_ro", "Membran RO", 180),
        DefaultTemplate("default_perawatan_lampu_uv", "Lampu UV", 180),
        DefaultTemplate("default_perawatan_service_pompa", "Service Pompa", 180)
    )

    /**
     * Menghasilkan daftar awal perawatan mesin RO dengan syncId deterministik dan updatedAt = 1L
     * agar aman terhadap sinkronisasi lintas perangkat.
     */
    fun buildDefaultItems(
        baseTimeMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PerawatanItem> {
        return DEFAULT_TEMPLATES.map { tpl ->
            PerawatanItem(
                nama = tpl.nama,
                intervalHari = tpl.intervalHari,
                terakhirSelesai = null,
                jadwalBerikutnya = calculateNextDueDateMillis(baseTimeMillis, tpl.intervalHari, zone),
                updatedAt = 1L,
                syncId = tpl.syncId
            )
        }
    }

    /**
     * Mencari templat standar yang belum ada pada daftar jadwal aktif berdasarkan syncId maupun nama.
     */
    fun findMissingDefaultTemplates(existingItems: List<PerawatanItem>): List<DefaultTemplate> {
        val existingSyncIds = existingItems.map { it.syncId.trim() }.toSet()
        val existingNames = existingItems.map { it.nama.trim().lowercase() }.toSet()
        return DEFAULT_TEMPLATES.filter { tpl ->
            tpl.syncId !in existingSyncIds && tpl.nama.trim().lowercase() !in existingNames
        }
    }

    /**
     * Membangun item perawatan dari templat standar yang belum ada dengan stempel waktu sinkronisasi terbaru.
     */
    fun buildMissingDefaultItems(
        existingItems: List<PerawatanItem>,
        baseTimeMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PerawatanItem> {
        return findMissingDefaultTemplates(existingItems).map { tpl ->
            PerawatanItem(
                nama = tpl.nama,
                intervalHari = tpl.intervalHari,
                terakhirSelesai = null,
                jadwalBerikutnya = calculateNextDueDateMillis(baseTimeMillis, tpl.intervalHari, zone),
                updatedAt = baseTimeMillis,
                syncId = tpl.syncId
            )
        }
    }

    /**
     * Menghitung tanggal jadwal berikutnya berdasarkan tanggal selesai (awal hari lokal) ditambah interval hari.
     */
    fun calculateNextDueDateMillis(
        completionDateMillis: Long,
        intervalHari: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val safeInterval = intervalHari.coerceIn(MIN_INTERVAL_DAYS, MAX_INTERVAL_DAYS).toLong()
        val baseDate = Instant.ofEpochMilli(completionDateMillis).atZone(zone).toLocalDate()
        val nextDate = baseDate.plusDays(safeInterval)
        return nextDate.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /**
     * Menghitung tanggal jatuh tempo baru saat jadwal ditunda (snooze).
     * Basis penundaan mengambil tanggal yang lebih akhir antara hari ini dan jadwal saat ini,
     * sehingga item yang terlambat tetap tertunda ke depan sejumlah [snoozeDays] dari hari ini.
     */
    fun calculateSnoozedDueDateMillis(
        currentDueMillis: Long,
        snoozeDays: Int,
        todayMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val safeDays = snoozeDays.coerceIn(MIN_INTERVAL_DAYS, MAX_INTERVAL_DAYS).toLong()
        val todayDate = Instant.ofEpochMilli(todayMillis).atZone(zone).toLocalDate()
        val currentDueDate = Instant.ofEpochMilli(currentDueMillis).atZone(zone).toLocalDate()
        val baseDate = if (currentDueDate.isAfter(todayDate)) currentDueDate else todayDate
        return baseDate.plusDays(safeDays).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /**
     * Mengevaluasi status keterlambatan dan pengingat H-3 relatif terhadap tanggal hari ini.
     */
    fun evaluate(
        item: PerawatanItem,
        todayMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): PerawatanEvaluation {
        val todayDate = Instant.ofEpochMilli(todayMillis).atZone(zone).toLocalDate()
        val dueDate = Instant.ofEpochMilli(item.jadwalBerikutnya).atZone(zone).toLocalDate()
        val daysRemaining = ChronoUnit.DAYS.between(todayDate, dueDate).toInt()
        val status = when {
            daysRemaining < 0 -> PerawatanStatus.TERLAMBAT
            daysRemaining == 0 -> PerawatanStatus.HARI_INI
            daysRemaining <= REMINDER_WINDOW_DAYS -> PerawatanStatus.MENDEKATI
            else -> PerawatanStatus.AMAN
        }
        return PerawatanEvaluation(
            item = item,
            status = status,
            daysRemaining = daysRemaining,
            isReminderDue = daysRemaining <= REMINDER_WINDOW_DAYS
        )
    }

    /**
     * Mengurutkan evaluasi jadwal perawatan dari yang paling mendesak (terlambat -> hari ini -> H-3 -> terjadwal).
     */
    fun evaluateAndSort(
        items: List<PerawatanItem>,
        todayMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PerawatanEvaluation> {
        return items
            .map { evaluate(it, todayMillis, zone) }
            .sortedWith(compareBy<PerawatanEvaluation> { it.daysRemaining }.thenBy { it.item.nama.lowercase() })
    }

    /**
     * Merangkum metrik kesehatan seluruh jadwal perawatan aktif untuk banner ringkasan dan chip filter.
     */
    fun summarizeScheduleStats(evaluations: List<PerawatanEvaluation>): PerawatanScheduleStats {
        var overdue = 0
        var dueToday = 0
        var soon = 0
        var safe = 0
        for (eval in evaluations) {
            when (eval.status) {
                PerawatanStatus.TERLAMBAT -> overdue++
                PerawatanStatus.HARI_INI -> dueToday++
                PerawatanStatus.MENDEKATI -> soon++
                PerawatanStatus.AMAN -> safe++
            }
        }
        val total = evaluations.size
        val dueAction = overdue + dueToday + soon
        val ratio = if (total > 0) safe.toFloat() / total.toFloat() else 1f
        return PerawatanScheduleStats(
            totalCount = total,
            overdueCount = overdue,
            dueTodayCount = dueToday,
            soonCount = soon,
            safeCount = safe,
            dueActionCount = dueAction,
            complianceRatio = ratio.coerceIn(0f, 1f)
        )
    }

    /**
     * Memfilter daftar evaluasi jadwal berdasarkan filter status dan kata kunci pencarian.
     */
    fun filterEvaluations(
        evaluations: List<PerawatanEvaluation>,
        filter: PerawatanScheduleFilter,
        query: String
    ): List<PerawatanEvaluation> {
        val statusFiltered = when (filter) {
            PerawatanScheduleFilter.SEMUA -> evaluations
            PerawatanScheduleFilter.PERLU_TINDAKAN -> evaluations.filter { it.isReminderDue }
            PerawatanScheduleFilter.TERLAMBAT -> evaluations.filter { it.status == PerawatanStatus.TERLAMBAT }
            PerawatanScheduleFilter.AMAN -> evaluations.filter { it.status == PerawatanStatus.AMAN }
        }
        if (query.isBlank()) return statusFiltered
        return SearchMatcher.rankAndFilter(statusFiltered, query) { eval ->
            listOf(
                SearchField(eval.item.nama, weight = 24, isPrimaryName = true),
                SearchField(formatStatusBadge(eval), weight = 12),
                SearchField("${eval.item.intervalHari} hari", weight = 8),
                SearchField(FormatUtils.formatDateUi(eval.item.jadwalBerikutnya), weight = 6)
            )
        }
    }

    /**
     * Memfilter daftar riwayat perawatan berdasarkan periode waktu dan kata kunci pencarian.
     */
    fun filterHistoryLogs(
        logs: List<PerawatanLog>,
        period: PerawatanHistoryPeriodFilter,
        query: String,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PerawatanLog> {
        val startMillis = when (period) {
            PerawatanHistoryPeriodFilter.SEMUA -> Long.MIN_VALUE
            PerawatanHistoryPeriodFilter.BULAN_INI -> {
                val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
                nowDate.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
            }
            PerawatanHistoryPeriodFilter.SEMBILAN_PULUH_HARI -> {
                val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
                nowDate.minusDays(89).atStartOfDay(zone).toInstant().toEpochMilli()
            }
            PerawatanHistoryPeriodFilter.TAHUN_INI -> {
                val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
                LocalDate.of(nowDate.year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
            }
        }
        val periodFiltered = if (period == PerawatanHistoryPeriodFilter.SEMUA) {
            logs
        } else {
            logs.filter { it.tanggalSelesai >= startMillis }
        }
        if (query.isBlank()) return periodFiltered
        return SearchMatcher.rankAndFilter(periodFiltered, query) { log ->
            listOf(
                SearchField(log.namaPerawatan, weight = 24, isPrimaryName = true),
                SearchField(log.catatan, weight = 14),
                SearchField(FormatUtils.formatRupiah(log.biaya), weight = 8),
                SearchField(log.biaya.toLong().toString(), weight = 8),
                SearchField(FormatUtils.formatDateUi(log.tanggalSelesai), weight = 6),
                SearchField(FormatUtils.formatDateTable(log.tanggalSelesai), weight = 6)
            )
        }
    }

    /**
     * Merangkum total biaya, jumlah servis berbayar/gratis, rata-rata biaya, dan komponen berbiaya terbesar.
     */
    fun summarizeHistory(logs: List<PerawatanLog>): PerawatanHistorySummary {
        if (logs.isEmpty()) {
            return PerawatanHistorySummary(
                totalLogs = 0,
                totalBiaya = 0.0,
                paidLogsCount = 0,
                freeLogsCount = 0,
                averageBiaya = 0.0,
                topComponentName = null,
                topComponentBiaya = 0.0
            )
        }
        var totalBiaya = 0.0
        var paidCount = 0
        var freeCount = 0
        val costByComponent = linkedMapOf<String, Double>()
        for (log in logs) {
            val safeCost = log.biaya.coerceAtLeast(0.0)
            totalBiaya += safeCost
            if (safeCost > 0.0) {
                paidCount++
                val nameKey = log.namaPerawatan.trim().ifBlank { "Perawatan" }
                costByComponent[nameKey] = (costByComponent[nameKey] ?: 0.0) + safeCost
            } else {
                freeCount++
            }
        }
        val topEntry = costByComponent.maxByOrNull { it.value }
        val avgBiaya = if (paidCount > 0) totalBiaya / paidCount else 0.0
        return PerawatanHistorySummary(
            totalLogs = logs.size,
            totalBiaya = totalBiaya,
            paidLogsCount = paidCount,
            freeLogsCount = freeCount,
            averageBiaya = avgBiaya,
            topComponentName = topEntry?.key,
            topComponentBiaya = topEntry?.value ?: 0.0
        )
    }

    /**
     * Membangun peta metrik riwayat per komponen berdasarkan `perawatanSyncId` untuk ditampilkan di kartu jadwal.
     */
    fun buildComponentMetricsMap(logs: List<PerawatanLog>): Map<String, PerawatanComponentMetrics> {
        if (logs.isEmpty()) return emptyMap()
        return logs
            .filter { it.perawatanSyncId.isNotBlank() }
            .groupBy { it.perawatanSyncId }
            .mapValues { (_, groupLogs) ->
                val sorted = groupLogs.sortedWith(
                    compareByDescending<PerawatanLog> { it.tanggalSelesai }.thenByDescending { it.id }
                )
                val latest = sorted.first()
                PerawatanComponentMetrics(
                    logCount = sorted.size,
                    totalBiaya = sorted.sumOf { it.biaya.coerceAtLeast(0.0) },
                    lastCost = latest.biaya.coerceAtLeast(0.0),
                    lastNote = latest.catatan.trim()
                )
            }
    }

    /**
     * Memvalidasi input form tambah/ubah jadwal perawatan agar selaras dengan batas aturan Firestore.
     */
    fun validateScheduleInput(nama: String, intervalRaw: String): PerawatanFormValidation {
        val cleanName = nama.trim()
        val parsedInterval = intervalRaw.trim().toIntOrNull()
        val error = when {
            cleanName.isBlank() -> PerawatanFormError.NAME_REQUIRED
            cleanName.length > MAX_SCHEDULE_NAME_LENGTH -> PerawatanFormError.NAME_TOO_LONG
            parsedInterval == null || parsedInterval < MIN_INTERVAL_DAYS || parsedInterval > MAX_INTERVAL_DAYS -> {
                PerawatanFormError.INTERVAL_INVALID
            }
            else -> null
        }
        return PerawatanFormValidation(
            error = error,
            cleanName = cleanName,
            intervalHari = parsedInterval
        )
    }

    /**
     * Memfilter item pengingat yang tampil di Dashboard pemilik (terlambat, hari ini, dan H-3).
     */
    fun filterDashboardReminders(evaluations: List<PerawatanEvaluation>): List<PerawatanEvaluation> {
        return evaluations.filter { it.isReminderDue }
    }

    /**
     * Menghasilkan label status ringkas untuk UI kartu jadwal dan pengingat.
     */
    fun formatStatusBadge(evaluation: PerawatanEvaluation): String {
        return when (evaluation.status) {
            PerawatanStatus.TERLAMBAT -> "Terlambat ${-evaluation.daysRemaining} hari"
            PerawatanStatus.HARI_INI -> "Jatuh tempo hari ini"
            PerawatanStatus.MENDEKATI -> "H-${evaluation.daysRemaining} lagi"
            PerawatanStatus.AMAN -> "${evaluation.daysRemaining} hari lagi"
        }
    }
}
