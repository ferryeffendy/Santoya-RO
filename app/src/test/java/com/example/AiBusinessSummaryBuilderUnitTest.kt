package com.example

import com.example.data.entity.AiBusinessInsight
import com.example.data.entity.DepotProfile
import com.example.data.entity.Transaksi
import com.example.util.AiBusinessSummaryBuilder
import com.example.util.AiKasbonLogRecord
import com.example.util.AiPelangganRecord
import com.example.util.AiPengeluaranRecord
import com.example.util.AiPerawatanRecord
import com.example.util.AiProdukRecord
import com.example.util.AiTangkiRefillRecord
import com.example.util.AiTransaksiRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiBusinessSummaryBuilderUnitTest {

    @Test
    fun buildSummary_aggregates30DaysOnlyAndExcludesDeletedRecords() {
        val now = 1_750_000_000_000L
        val twoDaysAgo = now - (2L * 86_400_000L)
        val fortyDaysAgo = now - (40L * 86_400_000L)

        val transaksiList = listOf(
            AiTransaksiRecord(
                tanggal = twoDaysAgo,
                jumlah = 10,
                totalHarga = 60_000.0,
                status = "Lunas",
                uangMuka = 0L,
                sisaKasbon = 0L,
                waterType = Transaksi.WATER_TYPE_RO,
                produkName = "Galon RO 19L",
                isDeleted = false,
                id = 1,
                pelangganId = 1,
                totalKomisi = 10_000.0,
                isDelivery = true
            ),
            AiTransaksiRecord(
                tanggal = twoDaysAgo,
                jumlah = 5,
                totalHarga = 35_000.0,
                status = "Kasbon",
                uangMuka = 15_000L,
                sisaKasbon = 20_000L,
                waterType = Transaksi.WATER_TYPE_MINERAL,
                produkName = "Galon Mineral",
                isDeleted = false,
                id = 2,
                pelangganId = 2,
                totalKomisi = 0.0,
                isDelivery = false
            ),
            // Out of 30d window
            AiTransaksiRecord(
                tanggal = fortyDaysAgo,
                jumlah = 20,
                totalHarga = 120_000.0,
                status = "Lunas",
                uangMuka = 0L,
                sisaKasbon = 0L,
                waterType = Transaksi.WATER_TYPE_RO,
                produkName = "Galon RO 19L",
                isDeleted = false,
                id = 3,
                pelangganId = 1
            ),
            // Soft-deleted should be excluded
            AiTransaksiRecord(
                tanggal = twoDaysAgo,
                jumlah = 99,
                totalHarga = 500_000.0,
                status = "Dihapus",
                uangMuka = 0L,
                sisaKasbon = 0L,
                waterType = Transaksi.WATER_TYPE_RO,
                produkName = "Galon RO 19L",
                isDeleted = true,
                id = 4,
                pelangganId = 1
            )
        )

        val pelangganList = listOf(
            AiPelangganRecord(id = 1, sisaKasbon = 0.0, isDeleted = false),
            AiPelangganRecord(id = 2, sisaKasbon = 20_000.0, isDeleted = false),
            AiPelangganRecord(id = 3, sisaKasbon = 50_000.0, isDeleted = true)
        )

        val produkList = listOf(
            AiProdukRecord(namaProduk = "Galon RO 19L", harga = 6_000.0, isDeleted = false),
            AiProdukRecord(namaProduk = "Galon Mineral", harga = 7_000.0, isDeleted = false)
        )

        val pengeluaranList = listOf(
            AiPengeluaranRecord(tanggal = twoDaysAgo, kategori = "Filter", nominal = 25_000.0, isDeleted = false),
            AiPengeluaranRecord(tanggal = fortyDaysAgo, kategori = "Listrik", nominal = 100_000.0, isDeleted = false),
            AiPengeluaranRecord(tanggal = twoDaysAgo, kategori = "Lainnya", nominal = 90_000.0, isDeleted = true)
        )

        val summary = AiBusinessSummaryBuilder.buildSummary(
            transaksiList = transaksiList,
            pelangganList = pelangganList,
            produkList = produkList,
            pengeluaranList = pengeluaranList,
            depotType = DepotProfile.TYPE_BOTH,
            tankCapacityLiters = 5_000.0,
            currentTankStockLiters = 1_900.0,
            nowMillis = now
        )

        assertEquals(95_000.0, summary.totalOmzet30d, 0.01)
        assertEquals(75_000.0, summary.totalKasMasuk30d, 0.01)
        assertEquals(10_000.0, summary.totalKomisiKurir, 0.01)
        assertEquals(25_000.0, summary.totalPengeluaran30d, 0.01)
        assertEquals(40_000.0, summary.labaBersih, 0.01) // 75k kasMasuk - 25k pengeluaran - 10k komisi
        assertEquals(15, summary.totalGalon30d)
        assertEquals(10, summary.galonRo30d)
        assertEquals(5, summary.galonMineral30d)
        assertEquals(2, summary.totalTransaksi30d)
        assertEquals(10, summary.deliveryGalonCount)
        assertEquals(5, summary.pickupGalonCount)
        assertEquals(2, summary.activeCustomerCount)
        assertEquals(2, summary.transactingCustomerCount)
        assertEquals(1, summary.kasbonCustomerCount)
        assertEquals(20_000.0, summary.totalPiutang, 0.01)
        assertEquals("Galon RO 19L", summary.topProducts.first().first)
        assertEquals(10, summary.topProducts.first().second)
        assertTrue(summary.hasSufficientData)
    }

    @Test
    fun buildSummary_reconcilesKasbonRepaymentWithoutDoubleCountingDp_andTracksGrowthAndAging() {
        val now = 1_750_000_000_000L
        val twoDaysAgo = now - (2L * 86_400_000L)
        val tenDaysAgo = now - (10L * 86_400_000L)
        val thirtyFiveDaysAgo = now - (35L * 86_400_000L)

        // Window 7 hari: 2 hari lalu (omzet 80k, galon 10) vs window sebelumnya 7..14 hari lalu (10 hari lalu: omzet 40k, galon 5)
        val transaksiList = listOf(
            AiTransaksiRecord(
                id = 1,
                pelangganId = 1,
                tanggal = twoDaysAgo,
                jumlah = 10,
                totalHarga = 80_000.0,
                status = "Kasbon",
                uangMuka = 30_000L,
                sisaKasbon = 20_000L,
                waterType = Transaksi.WATER_TYPE_RO,
                produkName = "Galon RO 19L",
                isDeleted = false,
                totalKomisi = 10_000.0
            ),
            AiTransaksiRecord(
                id = 2,
                pelangganId = 1,
                tanggal = tenDaysAgo,
                jumlah = 5,
                totalHarga = 40_000.0,
                status = "Lunas",
                uangMuka = 0L,
                sisaKasbon = 0L,
                waterType = Transaksi.WATER_TYPE_RO,
                produkName = "Galon RO 19L",
                isDeleted = false,
                totalKomisi = 5_000.0
            ),
            // Nota lama > 30 hari belum lunas untuk uji Kasbon Aging > 30 hari
            AiTransaksiRecord(
                id = 3,
                pelangganId = 2,
                tanggal = thirtyFiveDaysAgo,
                jumlah = 4,
                totalHarga = 35_000.0,
                status = "Kasbon",
                uangMuka = 0L,
                sisaKasbon = 35_000L,
                waterType = Transaksi.WATER_TYPE_MINERAL,
                produkName = "Galon Mineral",
                isDeleted = false
            )
        )

        val kasbonLogs = listOf(
            // Auto-DP mirror log should NOT be double-counted on top of uangMuka 30k
            AiKasbonLogRecord(
                id = 1,
                pelangganId = 1,
                tanggal = twoDaysAgo,
                jumlahBayar = 30_000.0,
                catatan = "Uang Muka / DP"
            ),
            // Real installment repayment IS added to kasMasuk
            AiKasbonLogRecord(
                id = 2,
                pelangganId = 1,
                tanggal = now - 86_400_000L,
                jumlahBayar = 30_000.0,
                catatan = "Cicilan kasbon pelanggan"
            )
        )

        val pelangganList = listOf(
            AiPelangganRecord(id = 1, sisaKasbon = 20_000.0, isDeleted = false),
            AiPelangganRecord(id = 2, sisaKasbon = 35_000.0, isDeleted = false)
        )

        val perawatanList = listOf(
            AiPerawatanRecord(nama = "Ganti Membran RO", jadwalBerikutnya = now - 86_400_000L, intervalHari = 180),
            AiPerawatanRecord(nama = "Cuci Filter", jadwalBerikutnya = now + (2L * 86_400_000L), intervalHari = 14)
        )

        val tangkiRefills = listOf(
            AiTangkiRefillRecord(tanggal = twoDaysAgo, volumeLiter = 1_000.0, isDeleted = false)
        )

        val summary = AiBusinessSummaryBuilder.buildSummary(
            transaksiList = transaksiList,
            pelangganList = pelangganList,
            produkList = emptyList(),
            pengeluaranList = listOf(
                AiPengeluaranRecord(
                    tanggal = twoDaysAgo,
                    kategori = "Listrik",
                    nominal = 15_000.0,
                    isDeleted = false
                )
            ),
            depotType = DepotProfile.TYPE_BOTH,
            tankCapacityLiters = 5_000.0,
            currentTankStockLiters = 1_900.0,
            nowMillis = now,
            periodDays = 7,
            kasbonLogs = kasbonLogs,
            perawatanList = perawatanList,
            tangkiRefillList = tangkiRefills
        )

        assertEquals(7, summary.periodDays)
        assertEquals(80_000.0, summary.totalOmzet30d, 0.01)
        // 30k DP + 30k pelunasan kasbon = 60k (NO 90k double-counting!)
        assertEquals(60_000.0, summary.totalKasMasuk30d, 0.01)
        assertEquals(15_000.0, summary.totalPengeluaran30d, 0.01)
        assertEquals(10_000.0, summary.totalKomisiKurir, 0.01)
        assertEquals(35_000.0, summary.labaBersih, 0.01)
        assertEquals(100.0, summary.omzetGrowthPercent, 0.5)
        assertEquals(100.0, summary.galonGrowthPercent, 0.5)
        assertEquals(35_000.0, summary.overduePiutangOver30d, 0.01)
        assertEquals(1, summary.perawatanOverdueCount)
        assertEquals(1, summary.perawatanDueSoonCount)
        assertTrue(summary.waterLossPercent != null)
        assertEquals(55_000.0, summary.totalPiutang, 0.01)
    }

    @Test
    fun buildSummary_emptyOperationalData_flagsInsufficientData() {
        val summary = AiBusinessSummaryBuilder.buildSummary(
            transaksiList = emptyList(),
            pelangganList = listOf(AiPelangganRecord(id = 1, sisaKasbon = 0.0, isDeleted = false)),
            produkList = listOf(AiProdukRecord(namaProduk = "Galon RO 19L", harga = 6_000.0, isDeleted = false)),
            pengeluaranList = emptyList(),
            depotType = DepotProfile.TYPE_RO,
            tankCapacityLiters = 5_000.0,
            currentTankStockLiters = 0.0
        )

        assertFalse(summary.hasSufficientData)
    }

    @Test
    fun buildPrompt_containsRequiredAnalysisInstructionAndNoPii() {
        val summary = AiBusinessSummaryBuilder.buildSummary(
            transaksiList = listOf(
                AiTransaksiRecord(
                    tanggal = 1_750_000_000_000L,
                    jumlah = 5,
                    totalHarga = 30_000.0,
                    status = "Lunas",
                    uangMuka = 0L,
                    sisaKasbon = 0L,
                    waterType = Transaksi.WATER_TYPE_MINERAL,
                    produkName = "Galon Mineral",
                    isDeleted = false
                )
            ),
            pelangganList = listOf(AiPelangganRecord(sisaKasbon = 12_000.0, isDeleted = false)),
            produkList = listOf(AiProdukRecord(namaProduk = "Galon RO 19L", harga = 6_000.0, isDeleted = false)),
            pengeluaranList = emptyList(),
            depotType = DepotProfile.TYPE_MINERAL,
            tankCapacityLiters = 3_000.0,
            currentTankStockLiters = 1_140.0,
            nowMillis = 1_750_000_000_000L
        )

        val prompt = AiBusinessSummaryBuilder.buildPrompt(summary)
        assertTrue(
            prompt.contains("Anda adalah analis bisnis depot air. Berikan 3 insight singkat dan saran stok berdasarkan data penjualan berikut:")
        )
        assertTrue(prompt.contains("1.140 L"))
        assertFalse(prompt.contains("0812"))
    }

    @Test
    fun parseGeminiResponse_parsesAndNormalizesExtendedStructuredJson() {
        val longBullet = "A".repeat(600)
        val json = """
            {
              "healthStatus": "WASPADA",
              "insights": [
                "- Penjualan RO stabil di atas 60%.",
                "$longBullet",
                "Piutang kasbon masih terkendali."
              ],
              "stockAdvice": "Tambah stok air mineral 2.000 L sebelum akhir pekan.",
              "priorityActionTitle": "Tagih Kasbon Mendesak",
              "priorityActionTarget": "KASBON"
            }
        """.trimIndent()

        val parsed = AiBusinessSummaryBuilder.parseGeminiResponse(json)
        assertEquals(AiBusinessInsight.HEALTH_WASPADA, parsed.healthStatus)
        assertEquals(3, parsed.insights.size)
        assertEquals("Penjualan RO stabil di atas 60%.", parsed.insights[0])
        assertEquals(AiBusinessSummaryBuilder.MAX_INSIGHT_LENGTH, parsed.insights[1].length)
        assertEquals("Tambah stok air mineral 2.000 L sebelum akhir pekan.", parsed.stockAdvice)
        assertEquals("Tagih Kasbon Mendesak", parsed.priorityActionTitle)
        assertEquals(AiBusinessInsight.TARGET_KASBON, parsed.priorityActionTarget)

        val shareable = AiBusinessSummaryBuilder.buildShareableText(
            depotName = "Santoya RO",
            insight = AiBusinessInsight(
                periodDays = 30,
                healthStatus = parsed.healthStatus,
                insights = parsed.insights,
                stockAdvice = parsed.stockAdvice,
                totalOmzet = 95_000.0,
                labaBersih = 40_000.0,
                totalGalon = 15
            )
        )
        assertTrue(shareable.contains("Santoya RO"))
        assertTrue(shareable.contains("Saran Stok"))
    }
}
