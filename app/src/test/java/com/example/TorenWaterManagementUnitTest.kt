package com.example

import com.example.data.entity.DepotProfile
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.TangkiRefill
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.LaporanFinanceCalculator
import com.example.util.SantoyaReceiptHelper
import com.example.util.TorenWaterCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test komprehensif untuk manajemen stok air toren, riwayat tangki, pengurangan otomatis 19L/galon,
 * penyesuaian stok saat edit/hapus transaksi, pemisahan laporan & struk mode campuran (BOTH),
 * serta rumus evaluasi susut air.
 */
class TorenWaterManagementUnitTest {

    @Test
    fun `progress ratio clamped between 0f and 1f and gallon estimate uses 19L`() {
        assertEquals(0.5f, TorenWaterCalculator.calculateProgressRatio(2500.0, 5000.0), 0.0001f)
        // Melebihi kapasitas toren tetap dibatasi di 1.0f
        assertEquals(1.0f, TorenWaterCalculator.calculateProgressRatio(7500.0, 5000.0), 0.0001f)
        // Stok minus tetap dibatasi di 0.0f
        assertEquals(0.0f, TorenWaterCalculator.calculateProgressRatio(-190.0, 5000.0), 0.0001f)

        // Estimasi galon = floor(sisaLiter / 19)
        assertEquals(263, TorenWaterCalculator.estimateRemainingGallons(5000.0))
        assertEquals(10, TorenWaterCalculator.estimateRemainingGallons(190.0))
        assertEquals(0, TorenWaterCalculator.estimateRemainingGallons(-38.0))
    }

    @Test
    fun `low stock warning triggers below 500 liters or 25 gallons`() {
        assertFalse(TorenWaterCalculator.isLowStock(500.0))
        assertTrue(TorenWaterCalculator.isLowStock(499.0))
        assertTrue(TorenWaterCalculator.isLowStock(400.0))
        assertTrue(TorenWaterCalculator.isLowStock(0.0))
        assertTrue(TorenWaterCalculator.isLowStock(-57.0))
    }

    @Test
    fun `delta stock on create deducts 19L per gallon only for MINERAL water`() {
        // Transaksi RO tidak mengurangi stok toren air mineral
        assertEquals(
            0.0,
            TorenWaterCalculator.calculateDeltaStockForTransaksi(
                oldWaterType = null,
                oldJumlah = 0,
                isOldDeleted = true,
                newWaterType = Transaksi.WATER_TYPE_RO,
                newJumlah = 10,
                isNewDeleted = false
            ),
            0.001
        )
        // Transaksi MINERAL mengurangi jumlahGalon * 19 Liter
        assertEquals(
            -190.0,
            TorenWaterCalculator.calculateDeltaStockForTransaksi(
                oldWaterType = null,
                oldJumlah = 0,
                isOldDeleted = true,
                newWaterType = Transaksi.WATER_TYPE_MINERAL,
                newJumlah = 10,
                isNewDeleted = false
            ),
            0.001
        )
    }

    @Test
    fun `delta stock on delete and restore reverses and reapplies mineral deduction`() {
        val txMineral = Transaksi(
            id = 1,
            tanggal = 1_000L,
            id_pelanggan = 1,
            id_kurir = null,
            id_produk = 2,
            jumlah = 5,
            harga_satuan = 5000.0,
            total_harga = 25000.0,
            total_komisi = 0.0,
            metode_bayar = "Cash",
            status = "Lunas",
            waterType = Transaksi.WATER_TYPE_MINERAL
        )
        val txRo = txMineral.copy(waterType = Transaksi.WATER_TYPE_RO)

        // Hapus transaksi MINERAL 5 galon -> kembalikan +95 Liter
        assertEquals(95.0, TorenWaterCalculator.calculateDeltaStockForTransaksi(txMineral, null), 0.001)
        // Hapus transaksi RO -> 0 Liter
        assertEquals(0.0, TorenWaterCalculator.calculateDeltaStockForTransaksi(txRo, null), 0.001)

        // Pulihkan transaksi MINERAL 5 galon -> kurangi lagi -95 Liter
        assertEquals(-95.0, TorenWaterCalculator.calculateDeltaStockForTransaksi(null, txMineral), 0.001)
    }

    @Test
    fun `delta stock on edit handles quantity change and waterType switching accurately`() {
        // 1. Edit jumlah galon MINERAL dari 4 galon (76L) menjadi 6 galon (114L) -> selisih -38L
        assertEquals(
            -38.0,
            TorenWaterCalculator.calculateDeltaStockForTransaksi(
                oldWaterType = Transaksi.WATER_TYPE_MINERAL,
                oldJumlah = 4,
                isOldDeleted = false,
                newWaterType = Transaksi.WATER_TYPE_MINERAL,
                newJumlah = 6,
                isNewDeleted = false
            ),
            0.001
        )

        // 2. Edit jenis air dari RO (5 galon) menjadi MINERAL (5 galon) -> kurangi -95L
        assertEquals(
            -95.0,
            TorenWaterCalculator.calculateDeltaStockForTransaksi(
                oldWaterType = Transaksi.WATER_TYPE_RO,
                oldJumlah = 5,
                isOldDeleted = false,
                newWaterType = Transaksi.WATER_TYPE_MINERAL,
                newJumlah = 5,
                isNewDeleted = false
            ),
            0.001
        )

        // 3. Edit jenis air dari MINERAL (5 galon) menjadi RO (5 galon) -> kembalikan +95L
        assertEquals(
            95.0,
            TorenWaterCalculator.calculateDeltaStockForTransaksi(
                oldWaterType = Transaksi.WATER_TYPE_MINERAL,
                oldJumlah = 5,
                isOldDeleted = false,
                newWaterType = Transaksi.WATER_TYPE_RO,
                newJumlah = 5,
                isNewDeleted = false
            ),
            0.001
        )
    }

    @Test
    fun `water loss evaluation formula calculates used sold shrinkage and percentage safely`() {
        val refills = listOf(
            TangkiRefill(id = 1, tanggal = 1_000L, volumeLiter = 7000.0, totalHarga = 300_000.0),
            TangkiRefill(id = 2, tanggal = 2_000L, volumeLiter = 5000.0, totalHarga = 250_000.0)
        )
        val totalRefill = refills.sumOf { it.volumeLiter }

        // Stok awal 0 L + Beli 12.000 L - Sisa aktual 4.000 L = 8.000 L terpakai
        // Air mineral terjual = 400 galon * 19 = 7.600 L
        // Air susut = 8.000 - 7.600 = 400 L (5.0%)
        val eval = TorenWaterCalculator.evaluateWaterLoss(
            startingStockLiters = 0.0,
            totalRefillLiters = totalRefill,
            currentStockLiters = 4000.0,
            soldMineralGallons = 400
        )

        assertEquals(12000.0, eval.totalRefillLiters, 0.001)
        assertEquals(8000.0, eval.totalWaterUsedLiters, 0.001)
        assertEquals(400, eval.soldGallons)
        assertEquals(7600.0, eval.totalWaterSoldLiters, 0.001)
        assertEquals(400.0, eval.lossLiters, 0.001)
        assertEquals(5.0, eval.lossPercent, 0.001)

        // Zero division safety ketika totalAirTerpakai <= 0
        val emptyEval = TorenWaterCalculator.evaluateWaterLoss(
            startingStockLiters = 0.0,
            totalRefillLiters = 0.0,
            currentStockLiters = 5000.0,
            soldMineralGallons = 0
        )
        assertEquals(0.0, emptyEval.totalWaterUsedLiters, 0.001)
        assertEquals(0.0, emptyEval.lossPercent, 0.001)
    }

    @Test
    fun `mixed mode BOTH separates RO and Mineral gallons in reports and receipts`() {
        val prodRo = Produk(id = 1, nama_produk = "Galon Air RO 19L", harga = 6000.0)
        val prodMin = Produk(id = 2, nama_produk = "Galon Air Mineral 19L", harga = 5000.0)
        val cust = Pelanggan(id = 1, nama = "Budi", alamat = "Sukamaju", no_hp = "08123")

        val txRo = TransaksiDetail(
            transaksi = Transaksi(
                id = 1,
                tanggal = 10_000L,
                id_pelanggan = 1,
                id_kurir = null,
                id_produk = 1,
                jumlah = 12,
                harga_satuan = 6000.0,
                total_harga = 72000.0,
                total_komisi = 0.0,
                metode_bayar = "Cash",
                status = "Lunas",
                waterType = Transaksi.WATER_TYPE_RO
            ),
            pelanggan = cust,
            kurir = null,
            produk = prodRo
        )
        val txMin = TransaksiDetail(
            transaksi = Transaksi(
                id = 2,
                tanggal = 12_000L,
                id_pelanggan = 1,
                id_kurir = null,
                id_produk = 2,
                jumlah = 8,
                harga_satuan = 5000.0,
                total_harga = 40000.0,
                total_komisi = 0.0,
                metode_bayar = "Cash",
                status = "Lunas",
                waterType = Transaksi.WATER_TYPE_MINERAL
            ),
            pelanggan = cust,
            kurir = null,
            produk = prodMin
        )

        val summary = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = listOf(txRo, txMin),
            allPengeluaran = emptyList(),
            allKasbonLogs = emptyList(),
            allPelanggan = listOf(cust),
            startTime = 0L,
            endTime = 20_000L
        )

        assertEquals(20, summary.totalGalon)
        assertEquals(12, summary.totalGalonRo)
        assertEquals(8, summary.totalGalonMineral)

        val bothProfile = DepotProfile(depotName = "Berkah Water", depotType = DepotProfile.TYPE_BOTH)
        val receiptMin = SantoyaReceiptHelper.generateSaleReceipt(txMin, 0.0, bothProfile)
        val receiptRo = SantoyaReceiptHelper.generateSaleReceipt(txRo, 0.0, bothProfile)

        assertTrue(receiptMin.contains("Galon Air Mineral"))
        assertTrue(receiptRo.contains("Galon Air RO"))
    }
}
