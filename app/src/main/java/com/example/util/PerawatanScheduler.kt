package com.example.util

import com.example.data.entity.PerawatanItem
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class PerawatanStatus {
    TERLAMBAT,
    HARI_INI,
    MENDEKATI,
    AMAN
}

data class PerawatanEvaluation(
    val item: PerawatanItem,
    val status: PerawatanStatus,
    val daysRemaining: Int,
    val isReminderDue: Boolean
)

/**
 * Utilitas perhitungan jadwal, pengingat H-3 / hari ini / terlambat, serta data awal perawatan mesin RO.
 */
object PerawatanScheduler {

    const val REMINDER_WINDOW_DAYS = 3
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
     * Menghitung tanggal jadwal berikutnya berdasarkan tanggal selesai (awal hari lokal) ditambah interval hari.
     */
    fun calculateNextDueDateMillis(
        completionDateMillis: Long,
        intervalHari: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val safeInterval = intervalHari.coerceAtLeast(1).toLong()
        val baseDate = Instant.ofEpochMilli(completionDateMillis).atZone(zone).toLocalDate()
        val nextDate = baseDate.plusDays(safeInterval)
        return nextDate.atStartOfDay(zone).toInstant().toEpochMilli()
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
