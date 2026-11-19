package com.example.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pilihan mode bahan kimia pembersih membran RO (Clean-In-Place / CIP).
 */
enum class CipCleaningMode(
    val defaultConcentrationPercent: Double,
    val recommendedMinPercent: Double,
    val recommendedMaxPercent: Double,
    val maxAllowedPercent: Double,
    val unit: String,
    val targetPhRange: String,
    val maxTemperatureCelsius: Int = 35
) {
    /**
     * Mode A: Kerak Mineral / Kapur / Besi menggunakan Asam Sitrat bubuk murni (Citric Acid 100%).
     * Konsentrasi baku standar DuPont FilmTec / Hydranautics: 2.0% berat (rentang 1.5% - 2.0%).
     */
    CITRIC_ACID(
        defaultConcentrationPercent = 2.0,
        recommendedMinPercent = 1.5,
        recommendedMaxPercent = 2.0,
        maxAllowedPercent = 10.0,
        unit = "gram",
        targetPhRange = "pH 2,5 – 3,5"
    ),

    /**
     * Mode B: Lumut / Lendir Organik menggunakan Soda Api murni (NaOH flake/bubuk 100%).
     * Konsentrasi baku standar DuPont FilmTec / Hydranautics: 0.1% berat (rentang 0.05% - 0.1%, batas aman <= 0.2%).
     */
    CAUSTIC_SODA_NAOH(
        defaultConcentrationPercent = 0.1,
        recommendedMinPercent = 0.05,
        recommendedMaxPercent = 0.1,
        maxAllowedPercent = 0.2,
        unit = "gram",
        targetPhRange = "pH 11,0 – 11,5 (Maks. 12,0)"
    ),

    /**
     * Mode C: Cairan Khusus Membran kemasan pabrik (Avista RoClean, Genesys, Permaclean, dll.).
     * Dosis mengikuti label kemasan (default 3.0%, rentang umum 1.0% - 5.0%).
     */
    SPECIALTY_LIQUID(
        defaultConcentrationPercent = 3.0,
        recommendedMinPercent = 1.0,
        recommendedMaxPercent = 5.0,
        maxAllowedPercent = 20.0,
        unit = "ml",
        targetPhRange = "Sesuai label kemasan"
    )
}

/**
 * Ukuran housing membran RO beserta saran volume air sirkulasi per tabung (Liter),
 * sudah mencakup estimasi volume ruang pipa dan dasar tangki sirkulasi CIP.
 */
enum class CipHousingSize(
    val defaultLitersPerTube: Double,
    val shortCode: String
) {
    HOUSING_4040(defaultLitersPerTube = 18.0, shortCode = "4040 (4 inci)"),
    HOUSING_4021(defaultLitersPerTube = 12.0, shortCode = "4021 (4×21 inci)"),
    HOUSING_8040(defaultLitersPerTube = 45.0, shortCode = "8040 (8 inci)"),
    HOUSING_HOUSEHOLD(defaultLitersPerTube = 5.0, shortCode = "1812 / 3012 (Rumah Tangga)");

    companion object {
        /**
         * Memetakan kategori ukuran membran dari fitur Cek Kesehatan Membran ke ukuran housing CIP secara 1-ke-1.
         */
        fun fromRoMembraneCategory(category: RoMembraneCategory): CipHousingSize {
            return when (category) {
                RoMembraneCategory.SIZE_4040 -> HOUSING_4040
                RoMembraneCategory.SIZE_4021 -> HOUSING_4021
                RoMembraneCategory.SIZE_8040 -> HOUSING_8040
                RoMembraneCategory.RUMAHAN -> HOUSING_HOUSEHOLD
            }
        }
    }

    /**
     * Mengonversi ukuran housing CIP kembali ke [RoMembraneCategory] secara 1-ke-1.
     */
    fun toRoMembraneCategory(currentCategory: RoMembraneCategory = RoMembraneCategory.SIZE_4040): RoMembraneCategory {
        return when (this) {
            HOUSING_4040 -> RoMembraneCategory.SIZE_4040
            HOUSING_4021 -> RoMembraneCategory.SIZE_4021
            HOUSING_8040 -> RoMembraneCategory.SIZE_8040
            HOUSING_HOUSEHOLD -> RoMembraneCategory.RUMAHAN
        }
    }
}

/**
 * Jenis kesalahan validasi input pada Kalkulator Takaran Pencucian Membran (CIP).
 */
enum class CipValidationError {
    VOLUME_REQUIRED,
    VOLUME_MUST_BE_POSITIVE,
    VOLUME_CANNOT_BE_NEGATIVE,
    VOLUME_INVALID_NUMBER,
    CONCENTRATION_REQUIRED,
    CONCENTRATION_MUST_BE_POSITIVE,
    CONCENTRATION_CANNOT_BE_NEGATIVE,
    CONCENTRATION_INVALID_NUMBER,
    NAOH_EXCEEDS_MAX_SAFE_LIMIT,
    CONCENTRATION_EXCEEDS_MAX_LIMIT
}

/**
 * Hasil perhitungan takaran bahan kimia pencucian membran RO (CIP).
 */
data class CipDosageResult(
    val mode: CipCleaningMode,
    val housingSize: CipHousingSize,
    val tubeCount: Int,
    val suggestedWaterLiters: Double,
    val waterVolumeLiters: Double,
    val concentrationPercent: Double,
    val dosageAmount: Double,
    val dosagePerLiter: Double,
    val unit: String,
    val targetPhRange: String,
    val maxTemperatureCelsius: Int,
    val isCustomWaterVolume: Boolean,
    val isOutsideRecommendedRange: Boolean
)

/**
 * Keluaran evaluasi validasi dan perhitungan Kalkulator CIP Membran RO.
 */
sealed class CipEvaluationOutcome {
    data class Success(val result: CipDosageResult) : CipEvaluationOutcome()
    data class ValidationError(
        val volumeError: CipValidationError? = null,
        val concentrationError: CipValidationError? = null
    ) : CipEvaluationOutcome()
}

/**
 * Utilitas murni untuk menghitung takaran bahan kimia pencucian membran RO (Clean-In-Place / CIP)
 * mengacu pada standar pabrikan membran (DuPont FilmTec / Hydranautics).
 */
object CipDosageCalculator {

    const val MIN_TUBE_COUNT = 1
    const val MAX_TUBE_COUNT = 20
    const val MAX_SAFE_NAOH_PERCENT = 0.2
    private const val EPSILON = 1e-6

    /**
     * Preset konsentrasi cepat untuk masing-masing mode pembersih.
     */
    fun presetConcentrationsForMode(mode: CipCleaningMode): List<Double> {
        return when (mode) {
            CipCleaningMode.CITRIC_ACID -> listOf(1.5, 2.0)
            CipCleaningMode.CAUSTIC_SODA_NAOH -> listOf(0.05, 0.1)
            CipCleaningMode.SPECIALTY_LIQUID -> listOf(1.0, 2.0, 3.0, 5.0)
        }
    }

    /**
     * Menyimpan saran mode pembersih awal berdasarkan penyebab penurunan performa membran RO.
     */
    fun recommendModeFromDiagnosis(
        status: RoMembraneStatus,
        cause: RoDiagnosisCause,
        flowDropPercent: Double?
    ): CipCleaningMode {
        val highFlowDrop = (flowDropPercent ?: 0.0) > RoMembraneCalculator.FLOW_DROP_WARNING_PERCENT
        return when {
            status == RoMembraneStatus.KRITIS && highFlowDrop -> CipCleaningMode.SPECIALTY_LIQUID
            cause == RoDiagnosisCause.FLOW_DEGRADED || cause == RoDiagnosisCause.BOTH_DEGRADED -> CipCleaningMode.CITRIC_ACID
            cause == RoDiagnosisCause.TDS_DEGRADED -> CipCleaningMode.CITRIC_ACID
            else -> CipCleaningMode.CITRIC_ACID
        }
    }

    /**
     * Menghitung saran total volume air sirkulasi (Liter) berdasarkan ukuran housing dan jumlah tabung.
     * Jumlah tabung dikunci minimal [MIN_TUBE_COUNT] (1 tabung) dan maksimal [MAX_TUBE_COUNT].
     */
    fun suggestWaterVolumeLiters(
        housingSize: CipHousingSize,
        tubeCount: Int
    ): Double {
        val safeTubes = tubeCount.coerceIn(MIN_TUBE_COUNT, MAX_TUBE_COUNT)
        return housingSize.defaultLitersPerTube * safeTubes
    }

    /**
     * Menghitung kebutuhan berat bubuk kimia murni (gram) untuk Mode A (Asam Sitrat) dan Mode B (NaOH).
     * Rumus: Berat (gram) = Volume Air (Liter) * (Konsentrasi / 100.0) * 1000.0
     * Mengembalikan 0.0 jika input <= 0 atau tidak valid agar bebas NaN/Infinity.
     */
    fun calculateSolidWeightGrams(
        waterVolumeLiters: Double,
        concentrationPercent: Double
    ): Double {
        if (!waterVolumeLiters.isFinite() || !concentrationPercent.isFinite()) return 0.0
        if (waterVolumeLiters <= 0.0 || concentrationPercent <= 0.0) return 0.0
        val grams = waterVolumeLiters * (concentrationPercent / 100.0) * 1000.0
        return if (grams.isFinite() && grams >= 0.0) grams else 0.0
    }

    /**
     * Menghitung kebutuhan volume cairan pembersih membran (ml) untuk Mode C (Cairan Khusus Kemasan).
     * Rumus: Volume Cairan (ml) = Volume Air (Liter) * Dosis_Persen * 10.0
     * Mengembalikan 0.0 jika input <= 0 atau tidak valid agar bebas NaN/Infinity.
     */
    fun calculateLiquidVolumeMl(
        waterVolumeLiters: Double,
        dosagePercent: Double
    ): Double {
        if (!waterVolumeLiters.isFinite() || !dosagePercent.isFinite()) return 0.0
        if (waterVolumeLiters <= 0.0 || dosagePercent <= 0.0) return 0.0
        val ml = waterVolumeLiters * dosagePercent * 10.0
        return if (ml.isFinite() && ml >= 0.0) ml else 0.0
    }

    /**
     * Mengevaluasi input string pengguna (mendukung titik maupun koma) dan menghitung takaran CIP
     * beserta validasi keamanan konsentrasi (NaOH tidak boleh melebihi 0.2% berat).
     */
    fun evaluateInputs(
        mode: CipCleaningMode,
        housingSize: CipHousingSize,
        tubeCount: Int,
        waterVolumeRaw: String,
        concentrationRaw: String
    ): CipEvaluationOutcome {
        val safeTubeCount = tubeCount.coerceIn(MIN_TUBE_COUNT, MAX_TUBE_COUNT)
        val suggestedLiters = suggestWaterVolumeLiters(housingSize, safeTubeCount)

        val trimmedVolume = waterVolumeRaw.trim()
        val parsedVolume = parseDecimalInput(trimmedVolume)
        val volumeError = when {
            trimmedVolume.isEmpty() -> CipValidationError.VOLUME_REQUIRED
            parsedVolume == null -> CipValidationError.VOLUME_INVALID_NUMBER
            parsedVolume < 0.0 -> CipValidationError.VOLUME_CANNOT_BE_NEGATIVE
            parsedVolume <= 0.0 -> CipValidationError.VOLUME_MUST_BE_POSITIVE
            else -> null
        }

        val trimmedConc = concentrationRaw.trim()
        val parsedConc = parseDecimalInput(trimmedConc)
        val concentrationError = when {
            trimmedConc.isEmpty() -> CipValidationError.CONCENTRATION_REQUIRED
            parsedConc == null -> CipValidationError.CONCENTRATION_INVALID_NUMBER
            parsedConc < 0.0 -> CipValidationError.CONCENTRATION_CANNOT_BE_NEGATIVE
            parsedConc <= 0.0 -> CipValidationError.CONCENTRATION_MUST_BE_POSITIVE
            mode == CipCleaningMode.CAUSTIC_SODA_NAOH && parsedConc > MAX_SAFE_NAOH_PERCENT + EPSILON ->
                CipValidationError.NAOH_EXCEEDS_MAX_SAFE_LIMIT
            parsedConc > mode.maxAllowedPercent + EPSILON ->
                CipValidationError.CONCENTRATION_EXCEEDS_MAX_LIMIT
            else -> null
        }

        if (volumeError != null || concentrationError != null || parsedVolume == null || parsedConc == null) {
            return CipEvaluationOutcome.ValidationError(
                volumeError = volumeError,
                concentrationError = concentrationError
            )
        }

        val dosageAmount = when (mode) {
            CipCleaningMode.CITRIC_ACID,
            CipCleaningMode.CAUSTIC_SODA_NAOH -> calculateSolidWeightGrams(parsedVolume, parsedConc)
            CipCleaningMode.SPECIALTY_LIQUID -> calculateLiquidVolumeMl(parsedVolume, parsedConc)
        }
        val dosagePerLiter = when (mode) {
            CipCleaningMode.CITRIC_ACID,
            CipCleaningMode.CAUSTIC_SODA_NAOH -> calculateSolidWeightGrams(1.0, parsedConc)
            CipCleaningMode.SPECIALTY_LIQUID -> calculateLiquidVolumeMl(1.0, parsedConc)
        }

        val isOutsideRecommended = parsedConc + EPSILON < mode.recommendedMinPercent ||
            parsedConc - EPSILON > mode.recommendedMaxPercent
        val isCustomWater = abs(parsedVolume - suggestedLiters) > 0.05

        return CipEvaluationOutcome.Success(
            CipDosageResult(
                mode = mode,
                housingSize = housingSize,
                tubeCount = safeTubeCount,
                suggestedWaterLiters = suggestedLiters,
                waterVolumeLiters = parsedVolume,
                concentrationPercent = parsedConc,
                dosageAmount = dosageAmount,
                dosagePerLiter = dosagePerLiter,
                unit = mode.unit,
                targetPhRange = mode.targetPhRange,
                maxTemperatureCelsius = mode.maxTemperatureCelsius,
                isCustomWaterVolume = isCustomWater,
                isOutsideRecommendedRange = isOutsideRecommended
            )
        )
    }

    /**
     * Mem-parsing string angka dengan dukungan pemisah desimal titik (`.`) maupun koma (`,`).
     */
    fun parseDecimalInput(raw: String): Double? {
        val cleaned = raw.trim().replace(" ", "")
        if (cleaned.isEmpty()) return null
        val normalized = if (cleaned.contains(',') && cleaned.contains('.')) {
            if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                cleaned.replace(".", "").replace(',', '.')
            } else {
                cleaned.replace(",", "")
            }
        } else {
            cleaned.replace(',', '.')
        }
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    /**
     * Memformat angka desimal ke format Indonesia (koma sebagai desimal, tanpa `.0` jika bulat).
     */
    fun formatDecimalId(value: Double, maxDecimals: Int = 1): String {
        if (!value.isFinite()) return "0"
        val roundedInt = value.roundToInt()
        if (abs(value - roundedInt) < 0.005 || maxDecimals <= 0) {
            return roundedInt.toString()
        }
        val formatted = String.format(Locale.US, "%.${maxDecimals}f", value)
            .trimEnd('0')
            .trimEnd('.')
        return formatted.replace('.', ',')
    }

    /**
     * Menyusun teks ringkasan pencucian membran RO (CIP) untuk disimpan ke entitas `PerawatanLog`.
     */
    fun buildHistoryNote(
        result: CipDosageResult,
        modeShortLabel: String,
        housingLabel: String,
        extraNote: String = ""
    ): String {
        val dosageStr = "${formatDecimalId(result.dosageAmount, 1)} ${result.unit}"
        val concStr = "${formatDecimalId(result.concentrationPercent, 2)}%"
        val volStr = "${formatDecimalId(result.waterVolumeLiters, 1)} L"
        val perLiterStr = "${formatDecimalId(result.dosagePerLiter, 1)} ${result.unit}/L"
        val baseSummary = "CIP $modeShortLabel ($concStr) • Takaran: $dosageStr ($perLiterStr) • " +
            "Air Sirkulasi: $volStr • Membran: ${result.tubeCount}x $housingLabel • Target ${result.targetPhRange}"
        val cleanExtra = extraNote.trim()
        return if (cleanExtra.isNotEmpty()) {
            "$baseSummary • Catatan: $cleanExtra"
        } else {
            baseSummary
        }
    }
}
