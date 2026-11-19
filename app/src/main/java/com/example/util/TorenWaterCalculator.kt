package com.example.util

import com.example.data.entity.Transaksi
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

typealias WaterLossEvaluation = TorenWaterCalculator.WaterLossEvaluation

/**
 * Utilitas murni untuk perhitungan stok toren air mineral, pengurangan galon,
 * delta stok saat edit/hapus transaksi, dan evaluasi persentase susut air.
 */
object TorenWaterCalculator {

    const val LITERS_PER_GALLON: Double = 19.0
    const val WARNING_THRESHOLD_LITERS: Double = 500.0
    const val WARNING_THRESHOLD_GALLONS: Int = 25
    const val CRITICAL_THRESHOLD_LITERS: Double = 100.0

    /**
     * Mengonversi volume liter menjadi estimasi jumlah galon standar (19L).
     */
    fun calculateGallonsFromLiters(liters: Double): Double {
        if (liters <= 0.0) return 0.0
        return liters / LITERS_PER_GALLON
    }

    /**
     * Mengembalikan estimasi pembulatan galon utuh untuk tampilan ringkas.
     */
    fun calculateEstimatedGallonsInt(liters: Double): Int {
        if (liters <= 0.0) return 0
        return floor(liters / LITERS_PER_GALLON).toInt().coerceAtLeast(0)
    }

    fun litersToEstimatedGallons(liters: Double): Int = calculateEstimatedGallonsInt(liters)

    fun estimateRemainingGallons(liters: Double): Int = calculateEstimatedGallonsInt(liters)

    /**
     * Mengecek apakah sisa air toren berada di bawah ambang peringatan (< 500 Liter atau < 25 galon).
     */
    fun isLowStock(stockLiters: Double): Boolean {
        return stockLiters < WARNING_THRESHOLD_LITERS ||
            calculateEstimatedGallonsInt(stockLiters) < WARNING_THRESHOLD_GALLONS
    }

    /**
     * Menghitung total liter dari jumlah galon terjual.
     */
    fun calculateLitersFromGallons(gallons: Int): Double {
        if (gallons <= 0) return 0.0
        return gallons * LITERS_PER_GALLON
    }

    /**
     * Menghitung rasio visual progress bar terhadap kapasitas toren (dibatasi 0f..1f).
     */
    fun calculateProgressRatio(currentStockLiters: Double, capacityLiters: Double): Float {
        val safeCap = capacityLiters.coerceAtLeast(1.0)
        val ratio = (currentStockLiters / safeCap).toFloat()
        return ratio.coerceIn(0f, 1f)
    }

    /**
     * Menghitung perubahan (delta) stok air toren ketika transaksi dibuat, diedit, atau dihapus.
     * Nilai positif berarti air bertambah (dikembalikan ke toren), nilai negatif berarti air berkurang.
     */
    fun calculateDeltaStockForTransaksi(
        oldWaterType: String?,
        oldJumlah: Int,
        isOldDeleted: Boolean,
        newWaterType: String?,
        newJumlah: Int,
        isNewDeleted: Boolean
    ): Double {
        val oldConsumed = if (!isOldDeleted && oldWaterType.equals(Transaksi.WATER_TYPE_MINERAL, ignoreCase = true)) {
            oldJumlah.coerceAtLeast(0) * LITERS_PER_GALLON
        } else {
            0.0
        }

        val newConsumed = if (!isNewDeleted && newWaterType.equals(Transaksi.WATER_TYPE_MINERAL, ignoreCase = true)) {
            newJumlah.coerceAtLeast(0) * LITERS_PER_GALLON
        } else {
            0.0
        }

        return oldConsumed - newConsumed
    }

    /**
     * Overload praktis untuk menghitung selisih stok toren antara entitas [oldTx] dan [newTx].
     */
    fun calculateDeltaStockForTransaksi(oldTx: Transaksi?, newTx: Transaksi?): Double {
        return calculateDeltaStockForTransaksi(
            oldWaterType = oldTx?.waterType,
            oldJumlah = oldTx?.jumlah ?: 0,
            isOldDeleted = oldTx == null || KaryawanScopeHelper.isDeletedTransaksi(oldTx),
            newWaterType = newTx?.waterType,
            newJumlah = newTx?.jumlah ?: 0,
            isNewDeleted = newTx == null || KaryawanScopeHelper.isDeletedTransaksi(newTx)
        )
    }

    data class WaterLossEvaluation(
        val startingStockLiters: Double,
        val totalRefillLiters: Double,
        val currentStockLiters: Double,
        val totalWaterUsedLiters: Double,
        val totalWaterSoldLiters: Double,
        val soldGallons: Int,
        val lossLiters: Double,
        val lossPercent: Double
    ) {
        val isLossPositive: Boolean get() = lossLiters > 0.0
        val totalAirMasukLiter: Double get() = totalWaterUsedLiters
        val totalGalonMineralTerjual: Int get() = soldGallons
        val estimasiAirTerjualLiter: Double get() = totalWaterSoldLiters
        val selisihSusutLiter: Double get() = lossLiters
        val susutPersen: Double get() = lossPercent
    }

    /**
     * Mengevaluasi pemakaian air toren dan persentase susut air:
     * Total Air Terpakai = (Stok Awal + Total Beli Air Tangki) - Sisa Air Aktual di Toren
     * Air Terjual = Total Galon Mineral Terjual * 19 Liter
     * Air Susut = Total Air Terpakai - Air Terjual
     * Persentase Susut = (Air Susut / Total Air Terpakai) * 100%
     */
    fun evaluateWaterLoss(
        startingStockLiters: Double,
        totalRefillLiters: Double,
        currentStockLiters: Double,
        soldMineralGallons: Int
    ): WaterLossEvaluation {
        val safeStart = startingStockLiters.coerceAtLeast(0.0)
        val safeRefill = totalRefillLiters.coerceAtLeast(0.0)
        val safeCurrent = currentStockLiters.coerceAtLeast(0.0)
        val safeGallons = soldMineralGallons.coerceAtLeast(0)

        val totalWaterUsed = (safeStart + safeRefill - safeCurrent).coerceAtLeast(0.0)
        val totalWaterSold = safeGallons * LITERS_PER_GALLON
        val lossLiters = totalWaterUsed - totalWaterSold
        val lossPercent = if (totalWaterUsed > 0.0) {
            ((lossLiters / totalWaterUsed) * 100.0).coerceIn(-100.0, 100.0)
        } else {
            0.0
        }

        return WaterLossEvaluation(
            startingStockLiters = safeStart,
            totalRefillLiters = safeRefill,
            currentStockLiters = safeCurrent,
            totalWaterUsedLiters = totalWaterUsed,
            totalWaterSoldLiters = totalWaterSold,
            soldGallons = safeGallons,
            lossLiters = lossLiters,
            lossPercent = lossPercent
        )
    }

    /**
     * Mem-parsing teks input volume atau kapasitas menjadi Double secara aman (mendukung koma dan titik).
     */
    fun parseInputToLiters(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val clean = raw.trim()
            .replace(" ", "")
            .replace("L", "", ignoreCase = true)
            .replace("liter", "", ignoreCase = true)
            .replace(".", "")
            .replace(",", ".")
        return clean.toDoubleOrNull()?.takeIf { it >= 0.0 }
    }

    /**
     * Format tampilan liter dengan pemisah ribuan standar Indonesia (contoh: 5.000 L).
     */
    fun formatLiters(liters: Double): String {
        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val df = if (liters % 1.0 == 0.0) {
            DecimalFormat("#,##0", symbols)
        } else {
            DecimalFormat("#,##0.#", symbols)
        }
        return "${df.format(liters.coerceAtLeast(0.0))} L"
    }

    /**
     * Format angka desimal persentase susut air (contoh: "8,5%").
     */
    fun formatPercent(percent: Double): String {
        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
            decimalSeparator = ','
        }
        val df = DecimalFormat("0.#", symbols)
        return "${df.format(percent)}%"
    }
}
