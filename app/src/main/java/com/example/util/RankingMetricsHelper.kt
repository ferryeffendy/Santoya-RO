package com.example.util

import java.util.Locale

/**
 * Mode pengurutan daftar peringkat penjualan di Laporan.
 */
enum class RankingSortMode {
    GALON,
    OMZET
}

/**
 * Input mentah satu entri peringkat (desa atau kurir) sebelum dihitung porsi kontribusi dan rasio progres.
 */
data class RankingRawEntry(
    val title: String,
    val totalGalon: Int,
    val totalTransaksi: Int,
    val totalPendapatan: Double
)

/**
 * Model UI terstruktur untuk satu baris kartu Peringkat Penjualan di Laporan.
 */
data class RankingEntryUi(
    val title: String,
    val totalGalon: Int,
    val totalTransaksi: Int,
    val totalPendapatan: Double,
    val omzetSharePercent: Double,
    val progressFraction: Float
)

/**
 * Utilitas murni untuk mengurutkan peringkat penjualan (berdasarkan volume galon atau omzet)
 * serta menghitung porsi kontribusi omzet dan rasio batang progres tiap item.
 */
object RankingMetricsHelper {

    private val INDONESIAN_LOCALE: Locale = Locale.forLanguageTag("id-ID")

    /**
     * Mengurutkan daftar [entries] sesuai [sortMode] dan menghitung persentase kontribusi omzet
     * serta rasio progres terhadap peringkat teratas.
     */
    fun buildRankedEntries(
        entries: List<RankingRawEntry>,
        sortMode: RankingSortMode
    ): List<RankingEntryUi> {
        if (entries.isEmpty()) return emptyList()

        val comparator = when (sortMode) {
            RankingSortMode.GALON -> compareByDescending<RankingRawEntry> { it.totalGalon }
                .thenByDescending { it.totalPendapatan }
                .thenBy { it.title.lowercase(INDONESIAN_LOCALE) }
            RankingSortMode.OMZET -> compareByDescending<RankingRawEntry> { it.totalPendapatan }
                .thenByDescending { it.totalGalon }
                .thenBy { it.title.lowercase(INDONESIAN_LOCALE) }
        }

        val sorted = entries.sortedWith(comparator)
        val totalOmzet = sorted.sumOf { it.totalPendapatan.coerceAtLeast(0.0) }
        val maxMetric = when (sortMode) {
            RankingSortMode.GALON -> (sorted.maxOfOrNull { it.totalGalon } ?: 0).toDouble()
            RankingSortMode.OMZET -> sorted.maxOfOrNull { it.totalPendapatan } ?: 0.0
        }.coerceAtLeast(0.0)

        return sorted.map { entry ->
            val safePendapatan = entry.totalPendapatan.coerceAtLeast(0.0)
            val sharePercent = if (totalOmzet > 0.0) {
                ((safePendapatan / totalOmzet) * 100.0).coerceIn(0.0, 100.0)
            } else {
                0.0
            }
            val metricValue = when (sortMode) {
                RankingSortMode.GALON -> entry.totalGalon.coerceAtLeast(0).toDouble()
                RankingSortMode.OMZET -> safePendapatan
            }
            val progress = if (maxMetric > 0.0) {
                (metricValue / maxMetric).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
            RankingEntryUi(
                title = entry.title,
                totalGalon = entry.totalGalon,
                totalTransaksi = entry.totalTransaksi,
                totalPendapatan = entry.totalPendapatan,
                omzetSharePercent = sharePercent,
                progressFraction = progress
            )
        }
    }

    /**
     * Memformat persentase kontribusi omzet secara ringkas agar nyaman dibaca pada kartu peringkat.
     */
    fun formatOmzetSharePercent(percent: Double): String {
        val safe = percent.coerceIn(0.0, 100.0)
        return when {
            safe <= 0.0 -> "0% omzet"
            safe < 0.1 -> "<0,1% omzet"
            safe >= 99.95 -> "100% omzet"
            else -> String.format(INDONESIAN_LOCALE, "%.1f%% omzet", safe)
        }
    }
}
