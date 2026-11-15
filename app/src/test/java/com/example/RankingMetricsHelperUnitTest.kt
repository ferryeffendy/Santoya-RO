package com.example

import com.example.util.RankingMetricsHelper
import com.example.util.RankingRawEntry
import com.example.util.RankingSortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingMetricsHelperUnitTest {

    private val sampleEntries = listOf(
        RankingRawEntry(
            title = "Rancadaka",
            totalGalon = 767,
            totalTransaksi = 30,
            totalPendapatan = 2_800_000.0
        ),
        RankingRawEntry(
            title = "Gempol",
            totalGalon = 854,
            totalTransaksi = 62,
            totalPendapatan = 3_400_000.0
        ),
        RankingRawEntry(
            title = "Genteng",
            totalGalon = 501,
            totalTransaksi = 77,
            totalPendapatan = 3_800_000.0
        )
    )

    @Test
    fun `buildRankedEntries sorts descending by galon in GALON mode with proportional progress`() {
        val ranked = RankingMetricsHelper.buildRankedEntries(
            entries = sampleEntries,
            sortMode = RankingSortMode.GALON
        )

        assertEquals(listOf("Gempol", "Rancadaka", "Genteng"), ranked.map { it.title })
        assertEquals(1f, ranked[0].progressFraction, 0.0001f)
        assertEquals(767f / 854f, ranked[1].progressFraction, 0.0001f)
        assertEquals(501f / 854f, ranked[2].progressFraction, 0.0001f)

        // Total omzet = 3.4M + 2.8M + 3.8M = 10.0M
        assertEquals(34.0, ranked[0].omzetSharePercent, 0.01)
        assertEquals(28.0, ranked[1].omzetSharePercent, 0.01)
        assertEquals(38.0, ranked[2].omzetSharePercent, 0.01)
    }

    @Test
    fun `buildRankedEntries sorts descending by pendapatan in OMZET mode with proportional progress`() {
        val ranked = RankingMetricsHelper.buildRankedEntries(
            entries = sampleEntries,
            sortMode = RankingSortMode.OMZET
        )

        assertEquals(listOf("Genteng", "Gempol", "Rancadaka"), ranked.map { it.title })
        assertEquals(1f, ranked[0].progressFraction, 0.0001f)
        assertEquals(3_400_000f / 3_800_000f, ranked[1].progressFraction, 0.0001f)
        assertEquals(2_800_000f / 3_800_000f, ranked[2].progressFraction, 0.0001f)
    }

    @Test
    fun `buildRankedEntries applies deterministic tie breakers for equal primary metric`() {
        val tiedEntries = listOf(
            RankingRawEntry("Kurir Z", totalGalon = 100, totalTransaksi = 10, totalPendapatan = 500_000.0),
            RankingRawEntry("Kurir B", totalGalon = 100, totalTransaksi = 12, totalPendapatan = 700_000.0),
            RankingRawEntry("Kurir A", totalGalon = 100, totalTransaksi = 12, totalPendapatan = 700_000.0)
        )

        val rankedByGalon = RankingMetricsHelper.buildRankedEntries(
            entries = tiedEntries,
            sortMode = RankingSortMode.GALON
        )
        assertEquals(listOf("Kurir A", "Kurir B", "Kurir Z"), rankedByGalon.map { it.title })

        val tiedOmzet = listOf(
            RankingRawEntry("Desa C", totalGalon = 20, totalTransaksi = 4, totalPendapatan = 500_000.0),
            RankingRawEntry("Desa A", totalGalon = 40, totalTransaksi = 5, totalPendapatan = 500_000.0),
            RankingRawEntry("Desa B", totalGalon = 40, totalTransaksi = 5, totalPendapatan = 500_000.0)
        )

        val rankedByOmzet = RankingMetricsHelper.buildRankedEntries(
            entries = tiedOmzet,
            sortMode = RankingSortMode.OMZET
        )
        assertEquals(listOf("Desa A", "Desa B", "Desa C"), rankedByOmzet.map { it.title })
    }

    @Test
    fun `buildRankedEntries handles empty list and zero totals safely`() {
        assertTrue(
            RankingMetricsHelper.buildRankedEntries(emptyList(), RankingSortMode.GALON).isEmpty()
        )

        val zeroEntries = listOf(
            RankingRawEntry("Desa Kosong", totalGalon = 0, totalTransaksi = 0, totalPendapatan = 0.0)
        )
        val rankedZero = RankingMetricsHelper.buildRankedEntries(zeroEntries, RankingSortMode.OMZET)
        assertEquals(1, rankedZero.size)
        assertEquals(0.0, rankedZero[0].omzetSharePercent, 0.0001)
        assertEquals(0f, rankedZero[0].progressFraction, 0.0001f)
    }

    @Test
    fun `formatOmzetSharePercent formats edge and Indonesian decimal values cleanly`() {
        assertEquals("0% omzet", RankingMetricsHelper.formatOmzetSharePercent(0.0))
        assertEquals("0% omzet", RankingMetricsHelper.formatOmzetSharePercent(-5.0))
        assertEquals("<0,1% omzet", RankingMetricsHelper.formatOmzetSharePercent(0.04))
        assertEquals("24,5% omzet", RankingMetricsHelper.formatOmzetSharePercent(24.5))
        assertEquals("100% omzet", RankingMetricsHelper.formatOmzetSharePercent(99.98))
        assertEquals("100% omzet", RankingMetricsHelper.formatOmzetSharePercent(120.0))
    }
}
