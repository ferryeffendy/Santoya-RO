package com.example.util

import androidx.compose.runtime.Immutable
import com.example.data.entity.KasbonLog
import com.example.data.entity.Pelanggan
import com.example.data.entity.TransaksiDetail
import kotlin.math.abs

/**
 * Representasi selisih antara saldo kasbon tercatat pada profil pelanggan dengan total nota kasbon aktif.
 */
@Immutable
data class KasbonDiscrepancy(
    val pelangganId: Int,
    val namaPelanggan: String,
    val saldoTercatat: Double,
    val totalNotaBelumLunas: Double,
    val selisih: Double
)

/**
 * Helper murni untuk mendeteksi ketidakcocokan dua arah antara saldo kasbon pelanggan
 * dan daftar nota kasbon aktif (berjalan di background dispatcher).
 */
object KasbonReconcileHelper {

    /**
     * Menghitung daftar pelanggan yang memiliki selisih antara [Pelanggan.sisa_kasbon]
     * dan akumulasi nota berstatus "Kasbon" yang belum dihapus.
     */
    fun detectDiscrepancies(
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>,
        kasbonLogs: List<KasbonLog>
    ): List<KasbonDiscrepancy> {
        if (pelangganList.isEmpty()) return emptyList()

        val activeTxByCust = transaksiList.asSequence()
            .filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
            .groupBy { it.transaksi.id_pelanggan }
        val custIdsWithLogs = kasbonLogs.asSequence()
            .filter { it.jumlah_bayar > 0.0 }
            .map { it.id_pelanggan }
            .toSet()

        return pelangganList.mapNotNull { p ->
            val custTxs = activeTxByCust[p.id].orEmpty()
            val unpaidSum = custTxs
                .filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
                .sumOf { it.transaksi.remainingKasbonAmount() }
            val targetKasbon = p.sisa_kasbon.coerceAtLeast(0.0)
            val hasSettledHistory = p.id in custIdsWithLogs || custTxs.any {
                it.transaksi.status.equals("Lunas", ignoreCase = true) &&
                    it.transaksi.tanggalBayar != null &&
                    it.transaksi.tanggalBayar != it.transaksi.tanggal
            }

            if (unpaidSum > targetKasbon || (targetKasbon > unpaidSum && (hasSettledHistory || unpaidSum > 0.0))) {
                KasbonDiscrepancy(
                    pelangganId = p.id,
                    namaPelanggan = p.nama,
                    saldoTercatat = targetKasbon,
                    totalNotaBelumLunas = unpaidSum,
                    selisih = abs(unpaidSum - targetKasbon)
                )
            } else {
                null
            }
        }
    }
}
