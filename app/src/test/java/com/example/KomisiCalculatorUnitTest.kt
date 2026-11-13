package com.example

import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.KomisiCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KomisiCalculatorUnitTest {

    private fun flatKurir(id: Int = 1, rate: Double = 1_000.0): Kurir = Kurir(
        id = id,
        nama = "Andi",
        komisi_per_galon = rate
    )

    private fun txDetail(
        id: Int,
        kurir: Kurir,
        jumlah: Int,
        hargaSatuan: Double = 5_000.0
    ): TransaksiDetail {
        val breakdown = KomisiCalculator.calculateTransactionCommission(kurir, jumlah)
        return TransaksiDetail(
            transaksi = Transaksi(
                id = id,
                tanggal = System.currentTimeMillis(),
                id_pelanggan = 1,
                id_kurir = kurir.id,
                id_produk = 1,
                jumlah = jumlah,
                harga_satuan = hargaSatuan,
                total_harga = hargaSatuan * jumlah,
                total_komisi = breakdown.totalKomisi,
                metode_bayar = "Cash",
                status = "Lunas"
            ),
            pelanggan = Pelanggan(id = 1, nama = "Warung A", alamat = "Blok A", no_hp = "08123"),
            kurir = kurir,
            produk = Produk(id = 1, nama_produk = "RO 19L", harga = hargaSatuan)
        )
    }

    @Test
    fun `fixed commission calculates accurately regardless of selling price`() {
        val kurir = flatKurir(rate = 1_200.0)

        val lowPrice = KomisiCalculator.calculateTransactionCommission(kurir, jumlah = 10)
        assertEquals(1_200.0, lowPrice.perGalonRate, 0.01)
        assertEquals(12_000.0, lowPrice.totalKomisi, 0.01)

        val zeroQty = KomisiCalculator.calculateTransactionCommission(kurir, jumlah = 0)
        assertEquals(0.0, zeroQty.totalKomisi, 0.01)

        val noCourier = KomisiCalculator.calculateTransactionCommission(null, jumlah = 5)
        assertEquals(0.0, noCourier.perGalonRate, 0.01)
        assertEquals(0.0, noCourier.totalKomisi, 0.01)

        val negativeRate = KomisiCalculator.calculateTransactionCommission(flatKurir(rate = -500.0), jumlah = 5)
        assertEquals(0.0, negativeRate.perGalonRate, 0.01)
        assertEquals(0.0, negativeRate.totalKomisi, 0.01)
    }

    @Test
    fun `summarizeCourierCompensation aggregates total gallons, orders, and fixed commission`() {
        val kurir = flatKurir(rate = 1_000.0)
        val txs = listOf(
            txDetail(1, kurir, jumlah = 20, hargaSatuan = 4_000.0),
            txDetail(2, kurir, jumlah = 35, hargaSatuan = 5_000.0),
            txDetail(3, kurir, jumlah = 10, hargaSatuan = 6_000.0)
        )

        val summary = KomisiCalculator.summarizeCourierCompensation(kurir, txs)
        assertEquals(65, summary.totalGallons)
        assertEquals(3, summary.totalOrders)
        assertEquals(65_000.0, summary.totalKomisi, 0.01)
    }
}
