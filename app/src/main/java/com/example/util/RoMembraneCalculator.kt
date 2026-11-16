package com.example.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Status kesehatan membran RO berdasarkan hasil evaluasi Rejeksi TDS dan Penurunan Flow.
 */
enum class RoMembraneStatus {
    SEHAT,
    WASPADA,
    KRITIS
}

/**
 * Klasifikasi akar penyebab penurunan performa membran RO (TDS vs Flow vs Gabungan).
 */
enum class RoDiagnosisCause {
    OPTIMAL,
    FLOW_DEGRADED,
    TDS_DEGRADED,
    BOTH_DEGRADED
}

/**
 * Pengelompokan ukuran fisik membran RO (4040, 4021, 8040, Rumahan).
 */
enum class RoMembraneCategory {
    SIZE_4040,
    SIZE_4021,
    SIZE_8040,
    RUMAHAN;

    companion object {
        /**
         * Mengonversi nama string (termasuk nilai legacy) menjadi enum kategori ukuran membran.
         */
        fun fromLegacyOrName(name: String?): RoMembraneCategory {
            if (name == null) return SIZE_4040
            return when (name) {
                "SIZE_4040", "DEPOT_4021_4040" -> SIZE_4040
                "SIZE_4021" -> SIZE_4021
                "SIZE_8040", "INDUSTRI_8040" -> SIZE_8040
                "RUMAHAN", "RUMAHAN_KAFE" -> RUMAHAN
                else -> entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SIZE_4040
            }
        }
    }
}

/**
 * Jenis kesalahan validasi input pada Kalkulator Kesehatan Membran RO.
 */
enum class RoValidationFieldError {
    REQUIRED,
    MUST_BE_GREATER_THAN_ZERO,
    CANNOT_BE_NEGATIVE,
    INVALID_NUMBER,
    TDS_JADI_EXCEEDS_BAKU,
    PAIR_REQUIRED_WITH_FLOW,
    PAIR_REQUIRED_WITH_GPD
}

/**
 * Preset kapasitas membran RO populer di Indonesia lengkap dengan seri, patokan PSI, batas TDS baku, dan referensi merek.
 */
data class RoMembranePreset(
    val id: String,
    val category: RoMembraneCategory,
    val gpdPerUnit: Int,
    val seriesCode: String,
    val psiHint: String,
    val maxFeedTdsHint: String,
    val maxFeedTdsPpm: Int = 1000,
    val typeName: String? = null,
    val shortTypeLabel: String = typeName ?: seriesCode,
    val brandReference: String? = null
)

/**
 * Hasil perhitungan diagnostik kesehatan membran RO.
 */
data class RoMembraneResult(
    val tdsBakuPpm: Double,
    val tdsJadiPpm: Double,
    val rejectionPercent: Double,
    val parallelCount: Int,
    val gpdPerUnit: Double?,
    val totalGpd: Double?,
    val standardFlowLpm: Double?,
    val actualFlowLpm: Double?,
    val flowDropPercent: Double?,
    val presetTypeName: String?,
    val presetPsiHint: String? = null,
    val presetMaxFeedTdsPpm: Int? = null,
    val status: RoMembraneStatus,
    val tdsStatus: RoMembraneStatus = status,
    val flowStatus: RoMembraneStatus? = null,
    val diagnosisCause: RoDiagnosisCause = RoDiagnosisCause.OPTIMAL,
    val isFeedTdsAbovePresetLimit: Boolean = false,
    val isTdsJadiHigherThanBaku: Boolean = false,
    val isFlowAboveStandard: Boolean = false
)

/**
 * Keluaran validasi dan perhitungan Kalkulator Kesehatan Membran RO.
 */
sealed class RoMembraneEvaluationOutcome {
    data class Success(val result: RoMembraneResult) : RoMembraneEvaluationOutcome()
    data class ValidationError(
        val tdsBakuError: RoValidationFieldError? = null,
        val tdsJadiError: RoValidationFieldError? = null,
        val gpdError: RoValidationFieldError? = null,
        val flowAktualError: RoValidationFieldError? = null
    ) : RoMembraneEvaluationOutcome()
}

/**
 * Utilitas logika murni untuk menghitung kesehatan membran Reverse Osmosis (RO) depot air minum.
 * Mendukung konfigurasi membran tunggal maupun multi-membran paralel (mis. 2x 4040 ULP/LP)
 * serta preset populer di Indonesia (4040, 4021, 8040, Rumahan).
 */
object RoMembraneCalculator {

    const val LITERS_PER_GALLON = 3.78541
    const val MINUTES_PER_DAY = 1440.0
    const val MIN_PARALLEL_MEMBRANES = 1
    const val MAX_PARALLEL_MEMBRANES = 20
    const val PURE_PERMEATE_TDS_MAX_PPM = 10.0
    const val FLOW_DROP_WARNING_PERCENT = 15.0
    const val FLOW_DROP_CRITICAL_PERCENT = 25.0
    private const val THRESHOLD_EPSILON = 1e-6

    val POPULAR_PRESETS: List<RoMembranePreset> = listOf(
        // Ukuran 1: Membran 4040 (Standar Utama Depot Air Minum AMIU) — 5 Tipe
        RoMembranePreset(
            id = "4040_ulp",
            category = RoMembraneCategory.SIZE_4040,
            gpdPerUnit = 2400,
            seriesCode = "ULP21-4040 / RE4040-UL",
            psiHint = "150 PSI",
            maxFeedTdsHint = "< 2.000 ppm (Standar utama depot air minum)",
            maxFeedTdsPpm = 2000,
            typeName = "4040 ULP",
            shortTypeLabel = "ULP",
            brandReference = "Vontron ULP21 / CSM RE4040-UL / LG Chem ES"
        ),
        RoMembranePreset(
            id = "4040_lp",
            category = RoMembraneCategory.SIZE_4040,
            gpdPerUnit = 2800,
            seriesCode = "LP21-4040 / TW30-4040",
            psiHint = "225 PSI",
            maxFeedTdsHint = "s.d. 8.000 ppm (Air sumur mineral tinggi / payau)",
            maxFeedTdsPpm = 8000,
            typeName = "4040 LP",
            shortTypeLabel = "LP",
            brandReference = "Vontron LP21 / DuPont FilmTec TW30 / LG Chem R"
        ),
        RoMembranePreset(
            id = "4040_xlp",
            category = RoMembraneCategory.SIZE_4040,
            gpdPerUnit = 2000,
            seriesCode = "XLP11-4040",
            psiHint = "70–100 PSI",
            maxFeedTdsHint = "< 1.000 ppm (Extra Low Pressure hemat daya)",
            maxFeedTdsPpm = 1000,
            typeName = "4040 XLP",
            shortTypeLabel = "XLP",
            brandReference = "Vontron XLP11 / LG Chem UES (Pompa hemat watt)"
        ),
        RoMembranePreset(
            id = "4040_xle",
            category = RoMembraneCategory.SIZE_4040,
            gpdPerUnit = 2600,
            seriesCode = "FilmTec XLE-4040 / XLE PRO",
            psiHint = "100–125 PSI",
            maxFeedTdsHint = "< 1.000 ppm (High-flow hemat energi)",
            maxFeedTdsPpm = 1000,
            typeName = "4040 XLE",
            shortTypeLabel = "XLE",
            brandReference = "DuPont FilmTec XLE-4040 / XLE PRO"
        ),
        RoMembranePreset(
            id = "4040_taptec",
            category = RoMembraneCategory.SIZE_4040,
            gpdPerUnit = 2800,
            seriesCode = "TapTec LC HF-4040",
            psiHint = "150 PSI",
            maxFeedTdsHint = "< 1.500 ppm (High-flow khusus depot AMIU)",
            maxFeedTdsPpm = 1500,
            typeName = "4040 TapTec",
            shortTypeLabel = "TapTec",
            brandReference = "DuPont TapTec LC HF-4040 (High-Flow Drinking Water)"
        ),

        // Ukuran 2: Membran 4021 (Depot Skala Ringkas) — 2 Tipe
        RoMembranePreset(
            id = "4021_ulp",
            category = RoMembraneCategory.SIZE_4021,
            gpdPerUnit = 950,
            seriesCode = "ULP21-4021 / CSM RE4021",
            psiHint = "150 PSI",
            maxFeedTdsHint = "< 1.500 ppm (Depot skala ringkas)",
            maxFeedTdsPpm = 1500,
            typeName = "4021 ULP",
            shortTypeLabel = "ULP",
            brandReference = "CSM RE4021-UL / Vontron ULP21-4021"
        ),
        RoMembranePreset(
            id = "4021_lp",
            category = RoMembraneCategory.SIZE_4021,
            gpdPerUnit = 1000,
            seriesCode = "LP21-4021",
            psiHint = "225 PSI",
            maxFeedTdsHint = "s.d. 5.000 ppm (Air tanah mineral tinggi)",
            maxFeedTdsPpm = 5000,
            typeName = "4021 LP",
            shortTypeLabel = "LP",
            brandReference = "Vontron LP21-4021"
        ),

        // Ukuran 3: Membran 8040 (Industri / AMDK Pabrik) — 4 Tipe
        RoMembranePreset(
            id = "8040_lp",
            category = RoMembraneCategory.SIZE_8040,
            gpdPerUnit = 9600,
            seriesCode = "LP21-8040 / BW30-8040",
            psiHint = "225 PSI",
            maxFeedTdsHint = "s.d. 8.000 ppm (Industri air payau)",
            maxFeedTdsPpm = 8000,
            typeName = "8040 LP",
            shortTypeLabel = "LP",
            brandReference = "Vontron LP21-8040 / FilmTec BW30-8040"
        ),
        RoMembranePreset(
            id = "8040_ulp22",
            category = RoMembraneCategory.SIZE_8040,
            gpdPerUnit = 10500,
            seriesCode = "ULP22-8040",
            psiHint = "150 PSI",
            maxFeedTdsHint = "< 2.000 ppm (AMDK / pabrik skala besar)",
            maxFeedTdsPpm = 2000,
            typeName = "8040 ULP 22",
            shortTypeLabel = "ULP 22",
            brandReference = "Vontron ULP22-8040"
        ),
        RoMembranePreset(
            id = "8040_ulp32",
            category = RoMembraneCategory.SIZE_8040,
            gpdPerUnit = 12100,
            seriesCode = "ULP32-8040",
            psiHint = "150–225 PSI",
            maxFeedTdsHint = "< 2.000 ppm (Kapasitas produksi tinggi)",
            maxFeedTdsPpm = 2000,
            typeName = "8040 ULP 32",
            shortTypeLabel = "ULP 32",
            brandReference = "Vontron ULP32-8040"
        ),
        RoMembranePreset(
            id = "8040_xlp",
            category = RoMembraneCategory.SIZE_8040,
            gpdPerUnit = 12800,
            seriesCode = "XLP12-8040",
            psiHint = "60–100 PSI",
            maxFeedTdsHint = "< 1.000 ppm (Industri ekstra hemat energi)",
            maxFeedTdsPpm = 1000,
            typeName = "8040 XLP",
            shortTypeLabel = "XLP",
            brandReference = "Vontron XLP12-8040"
        ),

        // Ukuran 4: Membran Rumahan & Kafe (1812 / 2012 / 3012 / 3013) — 10 Tipe
        RoMembranePreset(
            id = "gpd_50",
            category = RoMembraneCategory.RUMAHAN,
            gpdPerUnit = 50,
            seriesCode = "1812-50",
            psiHint = "60 PSI",
            maxFeedTdsHint = "< 500 ppm (RO undersink rumah)",
            maxFeedTdsPpm = 500,
            typeName = "1812-50",
            shortTypeLabel = "50 GPD",
            brandReference = "FilmTec / Vontron 50 GPD"
        ),
        RoMembranePreset(
            id = "gpd_75",
            category = RoMembraneCategory.RUMAHAN,
            gpdPerUnit = 75,
            seriesCode = "1812-75",
            psiHint = "60 PSI",
            maxFeedTdsHint = "< 500 ppm (RO undersink standar)",
            maxFeedTdsPpm = 500,
            typeName = "1812-75",
            shortTypeLabel = "75 GPD",
            brandReference = "FilmTec / Vontron 75 GPD"
        ),
        RoMembranePreset(
            id = "gpd_100",
            category = RoMembraneCategory.RUMAHAN,
            gpdPerUnit = 100,
            seriesCode = "2012-100",
            psiHint = "60 PSI",
            maxFeedTdsHint = "< 500 ppm (RO rumah/kantor kecil)",
            maxFeedTdsPpm = 500,
            typeName = "2012-100",
            shortTypeLabel = "100 GPD",
            brandReference = "FilmTec / CSM 100 GPD"
        ),
        RoMembranePreset(
            id = "gpd_200",
            category = RoMembraneCategory.RUMAHAN,
            gpdPerUnit = 200,
            seriesCode = "2812/3012-200",
            psiHint = "70–80 PSI",
            maxFeedTdsHint = "< 1.000 ppm (RO kafe/kios kecil)",
            maxFeedTdsPpm = 1000,
            typeName = "3012-200",
            shortTypeLabel = "200 GPD",
            brandReference = "Vontron / CSM 200 GPD"
        ),
        RoMembranePreset(
            id = "gpd_300",
            category = RoMembraneCategory.RUMAHAN,
            gpdPerUnit = 300,
            seriesCode = "3012-300",
            psiHint = "80–100 PSI",
            maxFeedTdsHint = "< 1.000 ppm (RO komersial mini)",
            maxFeedTdsPpm = 1000,
            typeName = "3012-300",
            shortTypeLabel = "300 GPD",
            brandReference = "Vontron 300 GPD"
        ),
        RoMembranePreset(
            id = "gpd_400",
            category = RoMembraneCategory.RUMAHAN,
            gpdPerUnit = 400,
            seriesCode = "ULP3013-400",
            psiHint = "100 PSI",
            maxFeedTdsHint = "< 1.000 ppm (Direct-flow kafe)",
            maxFeedTdsPpm = 1000,
            typeName = "3013-400",
            shortTypeLabel = "400 GPD",
            brandReference = "Vontron ULP3013-400"
        ),
        RoMembranePreset(
            id = "gpd_500",
            category = RoMembraneCategory.RUMAHAN,
            gpdPerUnit = 500,
            seriesCode = "3013-500",
            psiHint = "100 PSI",
            maxFeedTdsHint = "< 1.000 ppm (Komersial ringan)",
            maxFeedTdsPpm = 1000,
            typeName = "3013-500",
            shortTypeLabel = "500 GPD",
            brandReference = "CSM / Vontron 500 GPD"
        ),
        RoMembranePreset(
            id = "gpd_600",
            category = RoMembraneCategory.RUMAHAN,
            gpdPerUnit = 600,
            seriesCode = "ULP3013-600",
            psiHint = "100 PSI",
            maxFeedTdsHint = "< 1.000 ppm (Direct-flow kafe padat)",
            maxFeedTdsPpm = 1000,
            typeName = "3013-600",
            shortTypeLabel = "600 GPD",
            brandReference = "Vontron ULP3013-600"
        ),
        RoMembranePreset(
            id = "gpd_800",
            category = RoMembraneCategory.RUMAHAN,
            gpdPerUnit = 800,
            seriesCode = "ULP3013-800",
            psiHint = "100 PSI",
            maxFeedTdsHint = "< 1.000 ppm (Direct-flow besar)",
            maxFeedTdsPpm = 1000,
            typeName = "3013-800",
            shortTypeLabel = "800 GPD",
            brandReference = "Vontron ULP3013-800"
        ),
        RoMembranePreset(
            id = "gpd_1000",
            category = RoMembraneCategory.RUMAHAN,
            gpdPerUnit = 1000,
            seriesCode = "3013/3213-1000",
            psiHint = "100 PSI",
            maxFeedTdsHint = "< 1.000 ppm (RO depot pemula)",
            maxFeedTdsPpm = 1000,
            typeName = "3213-1000",
            shortTypeLabel = "1000 GPD",
            brandReference = "Vontron 3213-1000"
        )
    )

    /**
     * Mengembalikan daftar preset membran yang termasuk ke dalam kategori tertentu.
     */
    fun presetsForCategory(category: RoMembraneCategory): List<RoMembranePreset> {
        return POPULAR_PRESETS.filter { it.category == category }
    }

    /**
     * Mencari preset berdasarkan ID uniknya dengan normalisasi ID versi legacy.
     */
    fun findPresetById(id: String?): RoMembranePreset? {
        if (id.isNullOrBlank()) return null
        val normalizedId = when (id) {
            "4040_ulp_lp" -> "4040_ulp"
            "4040_lp21" -> "4040_lp"
            "8040_lp21" -> "8040_lp"
            "8040_xlp12" -> "8040_xlp"
            else -> id
        }
        return POPULAR_PRESETS.firstOrNull { it.id == normalizedId }
    }

    /**
     * Mem-parsing teks angka pengguna (mendukung pemisah desimal titik maupun koma).
     */
    fun parseDecimalInput(raw: String): Double? {
        val normalized = raw.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val parsed = normalized.toDoubleOrNull() ?: return null
        return if (parsed.isFinite()) parsed else null
    }

    /**
     * Mengonversi total kapasitas membran dari GPD (Gallon Per Day) menjadi LPM (Liter Per Menit).
     */
    fun convertGpdToLpm(totalGpd: Double): Double {
        if (!totalGpd.isFinite() || totalGpd <= 0.0) return 0.0
        return (totalGpd * LITERS_PER_GALLON) / MINUTES_PER_DAY
    }

    /**
     * Menghitung debit aktual dalam LPM dari volume wadah (Liter) dan durasi stopwatch (Detik).
     */
    fun calculateFlowLpmFromStopwatch(volumeLiters: Double, elapsedSeconds: Double): Double? {
        if (!volumeLiters.isFinite() || !elapsedSeconds.isFinite()) return null
        if (volumeLiters <= 0.0 || elapsedSeconds <= 0.0) return null
        val lpm = (volumeLiters / elapsedSeconds) * 60.0
        return if (lpm.isFinite()) lpm else null
    }

    /**
     * Memvalidasi seluruh input mentah pengguna lalu menjalankan perhitungan diagnosis membran RO.
     * Jika GPD sudah terisi (mis. dari preset tersimpan) tetapi Flow Aktual dikosongkan, sistem
     * otomatis menjalankan Mode Cek TDS Saja tanpa error blokir.
     */
    fun evaluateInputs(
        tdsBakuRaw: String,
        tdsJadiRaw: String,
        gpdPerUnitRaw: String,
        actualFlowLpmRaw: String,
        parallelCount: Int = 1,
        presetTypeName: String? = null,
        presetPsiHint: String? = null,
        presetMaxFeedTdsPpm: Int? = null
    ): RoMembraneEvaluationOutcome {
        val cleanBaku = tdsBakuRaw.trim()
        val cleanJadi = tdsJadiRaw.trim()
        val cleanGpd = gpdPerUnitRaw.trim()
        val cleanFlow = actualFlowLpmRaw.trim()

        val tdsBaku = parseDecimalInput(cleanBaku)
        val tdsJadi = parseDecimalInput(cleanJadi)

        val tdsBakuErr = when {
            cleanBaku.isEmpty() -> RoValidationFieldError.REQUIRED
            tdsBaku == null -> RoValidationFieldError.INVALID_NUMBER
            tdsBaku <= 0.0 -> RoValidationFieldError.MUST_BE_GREATER_THAN_ZERO
            else -> null
        }

        val tdsJadiErr = when {
            cleanJadi.isEmpty() -> RoValidationFieldError.REQUIRED
            tdsJadi == null -> RoValidationFieldError.INVALID_NUMBER
            tdsJadi < 0.0 -> RoValidationFieldError.CANNOT_BE_NEGATIVE
            tdsBaku != null && tdsBaku > 0.0 && tdsJadi > tdsBaku + THRESHOLD_EPSILON ->
                RoValidationFieldError.TDS_JADI_EXCEEDS_BAKU
            else -> null
        }

        val hasGpd = cleanGpd.isNotEmpty()
        val hasFlow = cleanFlow.isNotEmpty()
        val parsedGpd = if (hasGpd) parseDecimalInput(cleanGpd) else null
        val parsedFlow = if (hasFlow) parseDecimalInput(cleanFlow) else null

        val gpdErr = when {
            !hasGpd && hasFlow -> RoValidationFieldError.PAIR_REQUIRED_WITH_FLOW
            hasGpd && parsedGpd == null -> RoValidationFieldError.INVALID_NUMBER
            hasGpd && parsedGpd != null && parsedGpd <= 0.0 -> RoValidationFieldError.MUST_BE_GREATER_THAN_ZERO
            else -> null
        }

        val flowErr = when {
            hasFlow && parsedFlow == null -> RoValidationFieldError.INVALID_NUMBER
            hasFlow && parsedFlow != null && parsedFlow < 0.0 -> RoValidationFieldError.CANNOT_BE_NEGATIVE
            else -> null
        }

        if (tdsBakuErr != null || tdsJadiErr != null || gpdErr != null || flowErr != null) {
            return RoMembraneEvaluationOutcome.ValidationError(
                tdsBakuError = tdsBakuErr,
                tdsJadiError = tdsJadiErr,
                gpdError = gpdErr,
                flowAktualError = flowErr
            )
        }

        val validTdsBaku = requireNotNull(tdsBaku)
        val validTdsJadi = requireNotNull(tdsJadi)
        val validGpd = if (hasGpd && hasFlow) parsedGpd else null
        val validFlow = if (hasGpd && hasFlow) parsedFlow else null

        return RoMembraneEvaluationOutcome.Success(
            evaluate(
                tdsBakuPpm = validTdsBaku,
                tdsJadiPpm = validTdsJadi,
                gpdPerUnit = validGpd,
                actualFlowLpm = validFlow,
                parallelCount = parallelCount,
                presetTypeName = presetTypeName,
                presetPsiHint = presetPsiHint,
                presetMaxFeedTdsPpm = presetMaxFeedTdsPpm
            )
        )
    }

    /**
     * Menghitung Rejeksi TDS, Penurunan Flow (jika GPD & Flow Aktual tersedia), matriks akar penyebab,
     * serta pengecekan batas TDS air baku terhadap spesifikasi preset.
     */
    fun evaluate(
        tdsBakuPpm: Double,
        tdsJadiPpm: Double,
        gpdPerUnit: Double? = null,
        actualFlowLpm: Double? = null,
        parallelCount: Int = 1,
        presetTypeName: String? = null,
        presetPsiHint: String? = null,
        presetMaxFeedTdsPpm: Int? = null
    ): RoMembraneResult {
        require(tdsBakuPpm > 0.0) { "TDS Air Baku harus lebih besar dari 0" }
        require(tdsJadiPpm >= 0.0) { "TDS Air Jadi tidak boleh negatif" }

        val safeParallelCount = parallelCount.coerceIn(MIN_PARALLEL_MEMBRANES, MAX_PARALLEL_MEMBRANES)
        val rejectionPercent = ((tdsBakuPpm - tdsJadiPpm) / tdsBakuPpm) * 100.0

        val includeFlow = gpdPerUnit != null && gpdPerUnit > 0.0 && actualFlowLpm != null && actualFlowLpm >= 0.0
        val totalGpd = if (includeFlow) gpdPerUnit * safeParallelCount else null
        val standardFlowLpm = totalGpd?.let { convertGpdToLpm(it) }
        val flowDropPercent = if (standardFlowLpm != null && standardFlowLpm > 0.0 && actualFlowLpm != null) {
            ((standardFlowLpm - actualFlowLpm) / standardFlowLpm) * 100.0
        } else {
            null
        }

        // Evaluasi mandiri status Rejeksi TDS (dengan kompensasi TDS Air Jadi <= 10 ppm jika rejeksi >= 90%)
        val tdsStatus = when {
            rejectionPercent < 90.0 - THRESHOLD_EPSILON -> RoMembraneStatus.KRITIS
            rejectionPercent > 96.0 + THRESHOLD_EPSILON ||
                tdsJadiPpm <= PURE_PERMEATE_TDS_MAX_PPM + THRESHOLD_EPSILON -> RoMembraneStatus.SEHAT
            else -> RoMembraneStatus.WASPADA
        }

        // Evaluasi mandiri status Penurunan Flow terhadap GPD label (< 15% Sehat, 15-25% Waspada, > 25% Kritis)
        val flowStatus = if (flowDropPercent != null) {
            when {
                flowDropPercent > FLOW_DROP_CRITICAL_PERCENT + THRESHOLD_EPSILON -> RoMembraneStatus.KRITIS
                flowDropPercent >= FLOW_DROP_WARNING_PERCENT - THRESHOLD_EPSILON -> RoMembraneStatus.WASPADA
                else -> RoMembraneStatus.SEHAT
            }
        } else {
            null
        }

        // Status akhir mengambil kondisi terburuk (worst-case) antara TDS dan Flow
        val status = when {
            tdsStatus == RoMembraneStatus.KRITIS || flowStatus == RoMembraneStatus.KRITIS ->
                RoMembraneStatus.KRITIS
            tdsStatus == RoMembraneStatus.WASPADA || flowStatus == RoMembraneStatus.WASPADA ->
                RoMembraneStatus.WASPADA
            else -> RoMembraneStatus.SEHAT
        }

        val isTdsDegraded = tdsStatus != RoMembraneStatus.SEHAT
        val isFlowDegraded = flowStatus != null && flowStatus != RoMembraneStatus.SEHAT
        val diagnosisCause = when {
            isTdsDegraded && isFlowDegraded -> RoDiagnosisCause.BOTH_DEGRADED
            isFlowDegraded -> RoDiagnosisCause.FLOW_DEGRADED
            isTdsDegraded -> RoDiagnosisCause.TDS_DEGRADED
            else -> RoDiagnosisCause.OPTIMAL
        }

        val isFeedTdsAbovePresetLimit = presetMaxFeedTdsPpm != null &&
            tdsBakuPpm > presetMaxFeedTdsPpm.toDouble() + THRESHOLD_EPSILON

        return RoMembraneResult(
            tdsBakuPpm = tdsBakuPpm,
            tdsJadiPpm = tdsJadiPpm,
            rejectionPercent = rejectionPercent,
            parallelCount = safeParallelCount,
            gpdPerUnit = if (includeFlow) gpdPerUnit else null,
            totalGpd = totalGpd,
            standardFlowLpm = standardFlowLpm,
            actualFlowLpm = if (includeFlow) actualFlowLpm else null,
            flowDropPercent = flowDropPercent,
            presetTypeName = presetTypeName,
            presetPsiHint = presetPsiHint,
            presetMaxFeedTdsPpm = presetMaxFeedTdsPpm,
            status = status,
            tdsStatus = tdsStatus,
            flowStatus = flowStatus,
            diagnosisCause = diagnosisCause,
            isFeedTdsAbovePresetLimit = isFeedTdsAbovePresetLimit,
            isTdsJadiHigherThanBaku = tdsJadiPpm > tdsBakuPpm,
            isFlowAboveStandard = flowDropPercent != null && flowDropPercent < 0.0
        )
    }

    /**
     * Memformat nilai desimal memakai koma lokal Indonesia (mis. 10,52 LPM atau 150 ppm).
     */
    fun formatDecimalId(value: Double, decimals: Int = 2): String {
        val localeId = Locale.forLanguageTag("id-ID")
        val roundedInt = value.roundToInt().toDouble()
        return if (decimals == 0 || abs(value - roundedInt) < 0.0001 && decimals == 1) {
            String.format(localeId, "%,.0f", value)
        } else {
            String.format(localeId, "%,.${decimals}f", value)
        }
    }

    /**
     * Menyusun catatan terstruktur untuk disimpan ke Riwayat Perawatan RO.
     */
    fun buildHistoryNote(
        result: RoMembraneResult,
        statusLabel: String,
        diagnosisText: String
    ): String {
        val tdsBakuText = formatDecimalId(result.tdsBakuPpm, decimals = 1)
        val tdsJadiText = formatDecimalId(result.tdsJadiPpm, decimals = 1)
        val rejectionText = FormatUtils.formatPercent(result.rejectionPercent, decimals = 1)

        val parts = mutableListOf(
            "Status: $statusLabel ($diagnosisText)",
            "Rejeksi TDS: $rejectionText (Baku $tdsBakuText ppm -> Jadi $tdsJadiText ppm)"
        )

        val gpdUnit = result.gpdPerUnit
        val totalGpd = result.totalGpd
        val stdLpm = result.standardFlowLpm
        val actualLpm = result.actualFlowLpm
        val dropPct = result.flowDropPercent
        if (gpdUnit != null && totalGpd != null && stdLpm != null && actualLpm != null && dropPct != null) {
            val typeInfo = result.presetTypeName?.let { "$it • " } ?: ""
            val psiInfo = result.presetPsiHint?.let { " ($it)" } ?: ""
            val configText = "${result.parallelCount}x $typeInfo${formatDecimalId(gpdUnit, 0)} GPD$psiInfo"
            val flowInfo = "Penurunan Flow: ${FormatUtils.formatPercent(dropPct, 1)} " +
                "(Std ${formatDecimalId(stdLpm, 2)} vs Aktual ${formatDecimalId(actualLpm, 2)} LPM)"
            parts.add("Konfigurasi: $configText • Total ${formatDecimalId(totalGpd, 0)} GPD")
            parts.add(flowInfo)
        } else {
            val presetLabel = result.presetTypeName?.let { " ($it)" } ?: ""
            parts.add("Mode: Cek Rejeksi TDS saja$presetLabel")
        }

        if (result.isFeedTdsAbovePresetLimit && result.presetMaxFeedTdsPpm != null) {
            val maxTdsFormatted = formatDecimalId(result.presetMaxFeedTdsPpm.toDouble(), 0)
            parts.add("Peringatan Spek: TDS Air Baku ($tdsBakuText ppm) melebihi anjuran membran (Maks. $maxTdsFormatted ppm)")
        }

        return parts.joinToString("\n")
    }
}
