package com.example

import com.example.data.entity.PerawatanItem
import com.example.util.PerawatanScheduler
import com.example.util.PerawatanStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
