package com.example

import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import com.example.util.PerawatanFormError
import com.example.util.PerawatanHistoryPeriodFilter
import com.example.util.PerawatanScheduleFilter
import com.example.util.PerawatanScheduler
import com.example.util.PerawatanStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PerawatanSchedulerUnitTest {

    private val jakartaZone = ZoneId.of("Asia/Jakarta")

    private fun epochOf(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(jakartaZone)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `default templates match 7 RO maintenance items with unique deterministic syncIds`() {
        val baseMillis = epochOf(2026, 3, 1)
        val defaults = PerawatanScheduler.buildDefaultItems(baseMillis, jakartaZone)

        assertEquals(7, defaults.size)
        val byName = defaults.associateBy { it.nama }
        assertEquals(7, byName["Cek TDS"]?.intervalHari)
        assertEquals(30, byName["Filter Sedimen"]?.intervalHari)
        assertEquals(30, byName["Cuci Tangki"]?.intervalHari)
        assertEquals(60, byName["Filter Karbon"]?.intervalHari)
        assertEquals(180, byName["Membran RO"]?.intervalHari)
        assertEquals(180, byName["Lampu UV"]?.intervalHari)
        assertEquals(180, byName["Service Pompa"]?.intervalHari)

        assertEquals(7, defaults.map { it.syncId }.toSet().size)
        assertTrue(defaults.all { it.syncId.startsWith("default_perawatan_") })
        assertTrue(defaults.all { it.updatedAt == 1L })
    }

    @Test
    fun `calculateNextDueDateMillis adds interval days across month boundary`() {
        val completionDate = epochOf(2026, 1, 25)
        val nextDue = PerawatanScheduler.calculateNextDueDateMillis(completionDate, 7, jakartaZone)
        assertEquals(epochOf(2026, 2, 1), nextDue)

        val next30 = PerawatanScheduler.calculateNextDueDateMillis(epochOf(2026, 2, 10), 30, jakartaZone)
        assertEquals(epochOf(2026, 3, 12), next30)
    }

    @Test
    fun `evaluate and filterDashboardReminders only include terlambat, hari ini, and H-3`() {
        val today = epochOf(2026, 5, 10)

        val overdue = PerawatanItem(
            id = 1,
            nama = "Cek TDS",
            intervalHari = 7,
            jadwalBerikutnya = epochOf(2026, 5, 8)
        )
        val dueToday = PerawatanItem(
            id = 2,
            nama = "Cuci Tangki",
            intervalHari = 30,
            jadwalBerikutnya = epochOf(2026, 5, 10)
        )
        val dueIn2Days = PerawatanItem(
            id = 3,
            nama = "Filter Sedimen",
            intervalHari = 30,
            jadwalBerikutnya = epochOf(2026, 5, 12)
        )
        val dueIn3Days = PerawatanItem(
            id = 4,
            nama = "Filter Karbon",
            intervalHari = 60,
            jadwalBerikutnya = epochOf(2026, 5, 13)
        )
        val dueIn4Days = PerawatanItem(
            id = 5,
            nama = "Membran RO",
            intervalHari = 180,
            jadwalBerikutnya = epochOf(2026, 5, 14)
        )

        val evaluated = PerawatanScheduler.evaluateAndSort(
            items = listOf(dueIn4Days, dueIn3Days, overdue, dueToday, dueIn2Days),
            todayMillis = today,
            zone = jakartaZone
        )

        assertEquals(listOf("Cek TDS", "Cuci Tangki", "Filter Sedimen", "Filter Karbon", "Membran RO"), evaluated.map { it.item.nama })
        assertEquals(PerawatanStatus.TERLAMBAT, evaluated[0].status)
        assertEquals(-2, evaluated[0].daysRemaining)
        assertEquals("Terlambat 2 hari", PerawatanScheduler.formatStatusBadge(evaluated[0]))

        assertEquals(PerawatanStatus.HARI_INI, evaluated[1].status)
        assertEquals(0, evaluated[1].daysRemaining)
        assertEquals("Jatuh tempo hari ini", PerawatanScheduler.formatStatusBadge(evaluated[1]))

        assertEquals(PerawatanStatus.MENDEKATI, evaluated[2].status)
        assertEquals(2, evaluated[2].daysRemaining)
        assertEquals("H-2 lagi", PerawatanScheduler.formatStatusBadge(evaluated[2]))

        assertEquals(PerawatanStatus.MENDEKATI, evaluated[3].status)
        assertEquals(3, evaluated[3].daysRemaining)

        assertEquals(PerawatanStatus.AMAN, evaluated[4].status)
        assertFalse(evaluated[4].isReminderDue)

        val reminders = PerawatanScheduler.filterDashboardReminders(evaluated)
        assertEquals(listOf("Cek TDS", "Cuci Tangki", "Filter Sedimen", "Filter Karbon"), reminders.map { it.item.nama })
    }

    @Test
    fun `calculateSnoozedDueDateMillis anchors overdue items to today and future items to current due date`() {
        val today = epochOf(2026, 5, 10)
        val overdueDue = epochOf(2026, 5, 4)
        val futureDue = epochOf(2026, 5, 15)

        val snoozedOverdue = PerawatanScheduler.calculateSnoozedDueDateMillis(
            currentDueMillis = overdueDue,
            snoozeDays = 3,
            todayMillis = today,
            zone = jakartaZone
        )
        assertEquals(epochOf(2026, 5, 13), snoozedOverdue)

        val snoozedFuture = PerawatanScheduler.calculateSnoozedDueDateMillis(
            currentDueMillis = futureDue,
            snoozeDays = 7,
            todayMillis = today,
            zone = jakartaZone
        )
        assertEquals(epochOf(2026, 5, 22), snoozedFuture)
    }

    @Test
    fun `summarizeScheduleStats and filterEvaluations compute counts and search results`() {
        val today = epochOf(2026, 6, 10)
        val items = listOf(
            PerawatanItem(id = 1, syncId = "s1", nama = "Cek TDS", intervalHari = 7, jadwalBerikutnya = epochOf(2026, 6, 8)),
            PerawatanItem(id = 2, syncId = "s2", nama = "Cuci Tangki", intervalHari = 30, jadwalBerikutnya = epochOf(2026, 6, 10)),
            PerawatanItem(id = 3, syncId = "s3", nama = "Filter Sedimen", intervalHari = 30, jadwalBerikutnya = epochOf(2026, 6, 12)),
            PerawatanItem(id = 4, syncId = "s4", nama = "Membran RO", intervalHari = 180, jadwalBerikutnya = epochOf(2026, 7, 1))
        )
        val evaluations = PerawatanScheduler.evaluateAndSort(items, todayMillis = today, zone = jakartaZone)
        val stats = PerawatanScheduler.summarizeScheduleStats(evaluations)

        assertEquals(4, stats.totalCount)
        assertEquals(1, stats.overdueCount)
        assertEquals(1, stats.dueTodayCount)
        assertEquals(1, stats.soonCount)
        assertEquals(1, stats.safeCount)
        assertEquals(3, stats.dueActionCount)
        assertEquals(0.25f, stats.complianceRatio, 0.001f)

        assertEquals(4, PerawatanScheduler.filterEvaluations(evaluations, PerawatanScheduleFilter.SEMUA, "").size)
        assertEquals(3, PerawatanScheduler.filterEvaluations(evaluations, PerawatanScheduleFilter.PERLU_TINDAKAN, "").size)
        assertEquals(1, PerawatanScheduler.filterEvaluations(evaluations, PerawatanScheduleFilter.TERLAMBAT, "").size)
        assertEquals(1, PerawatanScheduler.filterEvaluations(evaluations, PerawatanScheduleFilter.AMAN, "").size)

        val searched = PerawatanScheduler.filterEvaluations(evaluations, PerawatanScheduleFilter.SEMUA, "sedimen")
        assertEquals(listOf("Filter Sedimen"), searched.map { it.item.nama })
    }

    @Test
    fun `filterHistoryLogs, summarizeHistory, and buildComponentMetricsMap aggregate accurately`() {
        val now = epochOf(2026, 6, 15)
        val logs = listOf(
            PerawatanLog(
                id = 1,
                perawatanSyncId = "default_perawatan_filter_sedimen",
                namaPerawatan = "Filter Sedimen",
                tanggalSelesai = epochOf(2026, 6, 12),
                biaya = 75000.0,
                catatan = "Ganti cartridge 10 inch",
                syncId = "log1"
            ),
            PerawatanLog(
                id = 2,
                perawatanSyncId = "default_perawatan_cek_tds",
                namaPerawatan = "Cek TDS",
                tanggalSelesai = epochOf(2026, 6, 1),
                biaya = 0.0,
                catatan = "TDS normal 11 ppm",
                syncId = "log2"
            ),
            PerawatanLog(
                id = 3,
                perawatanSyncId = "default_perawatan_membran_ro",
                namaPerawatan = "Membran RO",
                tanggalSelesai = epochOf(2026, 4, 20),
                biaya = 350000.0,
                catatan = "Penggantian membran 2000 GPD",
                syncId = "log3"
            ),
            PerawatanLog(
                id = 4,
                perawatanSyncId = "default_perawatan_filter_sedimen",
                namaPerawatan = "Filter Sedimen",
                tanggalSelesai = epochOf(2025, 11, 10),
                biaya = 50000.0,
                catatan = "Servis akhir tahun lalu",
                syncId = "log4"
            )
        )

        val thisMonth = PerawatanScheduler.filterHistoryLogs(logs, PerawatanHistoryPeriodFilter.BULAN_INI, "", now, jakartaZone)
        assertEquals(2, thisMonth.size)

        val last90Days = PerawatanScheduler.filterHistoryLogs(logs, PerawatanHistoryPeriodFilter.SEMBILAN_PULUH_HARI, "", now, jakartaZone)
        assertEquals(3, last90Days.size)

        val thisYear = PerawatanScheduler.filterHistoryLogs(logs, PerawatanHistoryPeriodFilter.TAHUN_INI, "", now, jakartaZone)
        assertEquals(3, thisYear.size)

        val queryByNote = PerawatanScheduler.filterHistoryLogs(logs, PerawatanHistoryPeriodFilter.SEMUA, "cartridge", now, jakartaZone)
        assertEquals(listOf("log1"), queryByNote.map { it.syncId })

        val summary = PerawatanScheduler.summarizeHistory(logs)
        assertEquals(4, summary.totalLogs)
        assertEquals(475000.0, summary.totalBiaya, 0.01)
        assertEquals(3, summary.paidLogsCount)
        assertEquals(1, summary.freeLogsCount)
        assertEquals(475000.0 / 3.0, summary.averageBiaya, 0.01)
        assertEquals("Membran RO", summary.topComponentName)
        assertEquals(350000.0, summary.topComponentBiaya, 0.01)

        val metricsMap = PerawatanScheduler.buildComponentMetricsMap(logs)
        val sedimenMetrics = metricsMap["default_perawatan_filter_sedimen"]
        assertEquals(2, sedimenMetrics?.logCount)
        assertEquals(125000.0, sedimenMetrics?.totalBiaya ?: 0.0, 0.01)
        assertEquals("Ganti cartridge 10 inch", sedimenMetrics?.lastNote)
    }

    @Test
    fun `findMissingDefaultTemplates and buildMissingDefaultItems detect missing standard items`() {
        val existing = listOf(
            PerawatanItem(id = 1, syncId = "default_perawatan_cek_tds", nama = "Cek TDS", intervalHari = 7, jadwalBerikutnya = epochOf(2026, 6, 1)),
            PerawatanItem(id = 2, syncId = "custom_uuid", nama = "cuci tangki", intervalHari = 30, jadwalBerikutnya = epochOf(2026, 6, 1))
        )
        val missing = PerawatanScheduler.findMissingDefaultTemplates(existing)
        assertEquals(5, missing.size)
        assertFalse(missing.any { it.nama == "Cek TDS" })
        assertFalse(missing.any { it.nama == "Cuci Tangki" })

        val built = PerawatanScheduler.buildMissingDefaultItems(existing, epochOf(2026, 6, 1), jakartaZone)
        assertEquals(5, built.size)
        assertTrue(built.all { it.updatedAt == epochOf(2026, 6, 1) })
    }

    @Test
    fun `validateScheduleInput enforces Firestore bounds`() {
        val emptyErr = PerawatanScheduler.validateScheduleInput("   ", "30")
        assertFalse(emptyErr.isValid)
        assertEquals(PerawatanFormError.NAME_REQUIRED, emptyErr.error)

        val longName = "A".repeat(PerawatanScheduler.MAX_SCHEDULE_NAME_LENGTH + 1)
        val longErr = PerawatanScheduler.validateScheduleInput(longName, "30")
        assertFalse(longErr.isValid)
        assertEquals(PerawatanFormError.NAME_TOO_LONG, longErr.error)

        val zeroIntervalErr = PerawatanScheduler.validateScheduleInput("Filter Karbon", "0")
        assertFalse(zeroIntervalErr.isValid)
        assertEquals(PerawatanFormError.INTERVAL_INVALID, zeroIntervalErr.error)

        val maxIntervalErr = PerawatanScheduler.validateScheduleInput("Filter Karbon", "3651")
        assertFalse(maxIntervalErr.isValid)
        assertEquals(PerawatanFormError.INTERVAL_INVALID, maxIntervalErr.error)

        val valid = PerawatanScheduler.validateScheduleInput("  Filter Karbon  ", "60")
        assertTrue(valid.isValid)
        assertNull(valid.error)
        assertEquals("Filter Karbon", valid.cleanName)
        assertEquals(60, valid.intervalHari)
    }
}
