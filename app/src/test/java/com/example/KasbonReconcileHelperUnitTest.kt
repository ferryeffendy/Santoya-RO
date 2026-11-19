package com.example

import com.example.data.entity.KasbonLog
import com.example.data.entity.Pelanggan
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.KasbonReconcileHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KasbonReconcileHelperUnitTest {

    private fun makeTxDetail(
        id: Int,
        pelanggan: Pelanggan,
        totalHarga: Double,
        status: String,
        uangMuka: Long = 0L,
        sisaKasbon: Long = 0L,
        tanggal: Long = 1_000L,
        tanggalBayar: Long? = null,
        isDeleted: Boolean = false
    ): TransaksiDetail {
        return TransaksiDetail(
            transaksi = Transaksi(
                id = id,
                tanggal = tanggal,
                id_pelanggan = pelanggan.id,
                id_kurir = null,
                id_produk = null,
                jumlah = 2,
                harga_satuan = totalHarga / 2.0,
                total_harga = totalHarga,
                total_komisi = 0.0,
                metode_bayar = if (uangMuka > 0L) "Bayar Sebagian" else "Cash",
                status = status,
                uangMuka = uangMuka,
                sisaKasbon = sisaKasbon,
                tanggalBayar = tanggalBayar,
                isDeleted = isDeleted
            ),
            pelanggan = pelanggan,
            kurir = null,
            produk = null
        )
    }

    @Test
    fun detectDiscrepancies_returnsEmptyWhenCustomerKasbonMatchesUnpaidNotes() {
        val pelanggan = Pelanggan(id = 1, nama = "Pak Budi", sisa_kasbon = 12_000.0)
        val tx = makeTxDetail(
            id = 10,
            pelanggan = pelanggan,
            totalHarga = 18_000.0,
            status = "Kasbon",
            uangMuka = 6_000L,
            sisaKasbon = 12_000L
        )

        val result = KasbonReconcileHelper.detectDiscrepancies(
            pelangganList = listOf(pelanggan),
            transaksiList = listOf(tx),
            kasbonLogs = emptyList()
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun detectDiscrepancies_flagsWhenUnpaidNotesExceedRecordedBalance() {
        val pelanggan = Pelanggan(id = 2, nama = "Ibu Ani", sisa_kasbon = 6_000.0)
        val tx = makeTxDetail(
            id = 11,
            pelanggan = pelanggan,
            totalHarga = 18_000.0,
            status = "Kasbon"
        )

        val result = KasbonReconcileHelper.detectDiscrepancies(
            pelangganList = listOf(pelanggan),
            transaksiList = listOf(tx),
            kasbonLogs = emptyList()
        )
        assertEquals(1, result.size)
        assertEquals(2, result.first().pelangganId)
        assertEquals(6_000.0, result.first().saldoTercatat, 0.001)
        assertEquals(18_000.0, result.first().totalNotaBelumLunas, 0.001)
        assertEquals(12_000.0, result.first().selisih, 0.001)
    }

    @Test
    fun detectDiscrepancies_flagsOverstatedRecordedBalanceWhenSettledHistoryExists() {
        val pelanggan = Pelanggan(id = 3, nama = "Warung Maju", sisa_kasbon = 20_000.0)
        val log = KasbonLog(
            id = 1,
            id_pelanggan = 3,
            jumlah_bayar = 10_000.0,
            sisa_kasbon_sebelum = 30_000.0,
            sisa_kasbon_sesudah = 20_000.0
        )

        val result = KasbonReconcileHelper.detectDiscrepancies(
            pelangganList = listOf(pelanggan),
            transaksiList = emptyList(),
            kasbonLogs = listOf(log)
        )
        assertEquals(1, result.size)
        assertEquals(20_000.0, result.first().selisih, 0.001)
    }

    @Test
    fun detectDiscrepancies_ignoresSoftDeletedTransactionsAndCoercesNegativeKasbon() {
        val pelanggan = Pelanggan(id = 4, nama = "Pak Dedi", sisa_kasbon = -5_000.0)
        val deletedTx = makeTxDetail(
            id = 12,
            pelanggan = pelanggan,
            totalHarga = 12_000.0,
            status = "Kasbon",
            isDeleted = true
        )

        val result = KasbonReconcileHelper.detectDiscrepancies(
            pelangganList = listOf(pelanggan),
            transaksiList = listOf(deletedTx),
            kasbonLogs = emptyList()
        )
        assertTrue(result.isEmpty())
    }
}
