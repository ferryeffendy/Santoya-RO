package com.example.util

import com.example.data.entity.Kurir
import com.example.data.entity.TransaksiDetail

data class TransactionCommissionBreakdown(
    val perGalonRate: Double,
    val totalKomisi: Double
)

data class CourierCompensationSummary(
    val kurir: Kurir?,
    val totalGallons: Int = 0,
    val totalOrders: Int = 0,
    val totalKomisi: Double = 0.0
)

object KomisiCalculator {

    fun calculateTransactionCommission(
        kurir: Kurir?,
        jumlah: Int
    ): TransactionCommissionBreakdown {
        if (kurir == null || jumlah <= 0) {
            return TransactionCommissionBreakdown(
                perGalonRate = 0.0,
                totalKomisi = 0.0
            )
        }
        val rate = kurir.komisi_per_galon.coerceAtLeast(0.0)
        return TransactionCommissionBreakdown(
            perGalonRate = rate,
            totalKomisi = rate * jumlah
        )
    }

    fun summarizeCourierCompensation(
        kurir: Kurir?,
        transactions: List<TransaksiDetail>
    ): CourierCompensationSummary {
        val totalGallons = transactions.sumOf { it.transaksi.jumlah }
        val totalOrders = transactions.size
        val totalKomisi = transactions.sumOf { it.transaksi.total_komisi }
        return CourierCompensationSummary(
            kurir = kurir,
            totalGallons = totalGallons,
            totalOrders = totalOrders,
            totalKomisi = totalKomisi
        )
    }

    fun formatCompactRuleSummary(kurir: Kurir): String {
        return "Komisi ${FormatUtils.formatRupiah(kurir.komisi_per_galon.coerceAtLeast(0.0))} / galon"
    }
}
