package com.example

import com.example.data.entity.DepotProfile
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.ExportHelper
import com.example.util.LaporanFinanceCalculator
import com.example.util.SantoyaReceiptHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class SantoyaReceiptAndProfileUnitTest {

    private val zoneId = ZoneId.of("Asia/Jakarta")

    private fun sampleMillis(): Long {
        return LocalDate.of(2026, 11, 17)
            .atTime(10, 30)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun samplePelanggan(): Pelanggan {
        return Pelanggan(
            id = 1,
            nama = "Ibu Sari",
            alamat = "Jl. Mawar No. 5",
            no_hp = "08123456789",
            sisa_kasbon = 12000.0
        )
    }

    private fun sampleDetail(): TransaksiDetail {
        val cust = samplePelanggan()
        val prod = Produk(id = 1, nama_produk = "Galon RO 19L", harga = 6000.0)
        val kurir = Kurir(id = 1, nama = "Budi", komisi_per_galon = 1000.0)
        val trx = Transaksi(
            id = 101,
            tanggal = sampleMillis(),
            id_pelanggan = cust.id,
            id_kurir = kurir.id,
            id_produk = prod.id,
            jumlah = 2,
            harga_satuan = 6000.0,
            total_harga = 12000.0,
            total_komisi = 2000.0,
            metode_bayar = "Cash",
            status = "Lunas"
        )
        return TransaksiDetail(transaksi = trx, pelanggan = cust, kurir = kurir, produk = prod)
    }

    @Test
    fun `depotProfile fallbacks and helper flags behave accurately`() {
        val blankProfile = DepotProfile(
            googleDisplayName = "Hendra Saputra",
            ownerName = "   ",
            depotName = "   ",
            role = "owner",
            subscriptionTier = "free"
        )
        assertEquals("DEPOT AIR MINUM", blankProfile.effectiveDepotName())
        assertEquals("Hendra Saputra", blankProfile.effectiveOwnerName())
        assertFalse(blankProfile.hasConfiguredDepotName())
        assertFalse(blankProfile.isProUser())
        assertTrue(blankProfile.isOwnerRole())

        val proProfile = blankProfile.copy(
            ownerName = "Hendra S.",
            depotName = "Tirta Sejuk RO",
            role = "STAFF",
            subscriptionTier = "PRO"
        )
        assertEquals("Tirta Sejuk RO", proProfile.effectiveDepotName())
        assertEquals("Hendra S.", proProfile.effectiveOwnerName())
        assertTrue(proProfile.hasConfiguredDepotName())
        assertTrue(proProfile.isProUser())
        assertFalse(proProfile.isOwnerRole())
    }

    @Test
    fun `sale receipt uses fallback depot name omits blank fields and shows watermark for free tier`() {
        val blankProfile = DepotProfile(
            depotName = "   ",
            address = "",
            phoneNumber = "",
            receiptFooter = "",
            subscriptionTier = DepotProfile.TIER_FREE
        )
        val receipt = SantoyaReceiptHelper.generateSaleReceipt(
            detail = sampleDetail(),
            customerKasbon = 0.0,
            depotProfile = blankProfile
        )

        assertTrue(receipt.contains("DEPOT AIR MINUM"))
        assertFalse(receipt.contains("SANTOYA RO", ignoreCase = true))
        assertFalse(receipt.contains("null", ignoreCase = true))
        assertFalse(receipt.contains("WA/Telp:"))
        assertTrue(receipt.contains("Terima Kasih"))
        assertTrue(receipt.contains(DepotProfile.WATERMARK_TEXT))

        val headerSegment = receipt.substringBefore("NOTA PENJUALAN")
        val headerLines = headerSegment.lines().dropLast(1)
        assertTrue("Header should not contain blank lines", headerLines.none { it.isBlank() })
    }

    @Test
    fun `sale receipt renders configured depot profile and hides watermark for pro tier`() {
        val proProfile = DepotProfile(
            ownerName = "Pak Joko",
            depotName = "Depot Berkah Tirta",
            address = "Jl. Merdeka Barat No. 88, Bandung Selatan",
            phoneNumber = "0811-2233-4455",
            receiptFooter = "Terima kasih sudah berlangganan air RO higienis kami!",
            subscriptionTier = DepotProfile.TIER_PRO
        )
        val receipt = SantoyaReceiptHelper.generateSaleReceipt(
            detail = sampleDetail(),
            customerKasbon = 12000.0,
            depotProfile = proProfile
        )

        assertTrue(receipt.contains("DEPOT BERKAH TIRTA"))
        assertTrue(receipt.contains("WA/Telp: 0811-2233-4455"))
        assertTrue(receipt.contains("Jl. Merdeka Barat No. 88,"))
        assertTrue(receipt.contains("Bandung Selatan"))
        assertTrue(receipt.contains("Terima kasih sudah berlangganan"))
        assertFalse(receipt.contains(DepotProfile.WATERMARK_TEXT))
    }

    @Test
    fun `pelunasan and tagihan kasbon receipts respect dynamic profile and isProUser parameter`() {
        val profile = DepotProfile(
            depotName = "Sumber Sehat RO",
            address = "Pasar Baru Blok C",
            phoneNumber = "0899-1122-3344",
            receiptFooter = "Simpan struk ini sebagai bukti sah",
            subscriptionTier = DepotProfile.TIER_FREE
        )
        val cust = samplePelanggan()
        val detail = sampleDetail()

        val pelunasanFree = SantoyaReceiptHelper.generatePelunasanReceipt(
            pelanggan = cust,
            nominalBayar = 12000.0,
            sisaKasbon = 0.0,
            depotProfile = profile,
            isProUser = false
        )
        assertTrue(pelunasanFree.contains("SUMBER SEHAT RO"))
        assertTrue(pelunasanFree.contains("WA/Telp: 0899-1122-3344"))
        assertTrue(pelunasanFree.contains(DepotProfile.WATERMARK_TEXT))

        val tagihanProOverride = SantoyaReceiptHelper.generateTagihanKasbon(
            pelanggan = cust,
            unpaidList = listOf(detail.transaksi),
            depotProfile = profile,
            isProUser = true
        )
        assertTrue(tagihanProOverride.contains("SUMBER SEHAT RO"))
        assertTrue(tagihanProOverride.contains("Simpan struk ini sebagai bukti"))
        assertFalse(tagihanProOverride.contains(DepotProfile.WATERMARK_TEXT))
    }

    @Test
    fun `generateLaporanHtml renders dynamic depot header owner signature and tier watermark`() {
        val detail = sampleDetail()
        val now = sampleMillis()
        val summary = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = listOf(detail),
            allPengeluaran = emptyList(),
            allKasbonLogs = emptyList(),
            allPelanggan = listOf(samplePelanggan()),
            startTime = now - 3600_000L,
            endTime = now + 3600_000L,
            zoneId = zoneId
        )

        val freeProfile = DepotProfile(
            ownerName = "Ibu Rina",
            depotName = "",
            address = "",
            phoneNumber = "",
            subscriptionTier = DepotProfile.TIER_FREE
        )
        val freeHtml = ExportHelper.generateLaporanHtml(
            summary = summary,
            allTransaksi = listOf(detail),
            allKasbonLogs = emptyList(),
            periodeLabel = "17 Nov 2026",
            isSummaryMode = false,
            depotProfile = freeProfile
        )
        assertTrue(freeHtml.contains("<div class='kop-brand'>DEPOT AIR MINUM</div>"))
        assertFalse(freeHtml.contains("<div class='kop-address'>"))
        assertTrue(freeHtml.contains("Ibu Rina"))
        assertTrue(freeHtml.contains("<div class='pdf-watermark'>${DepotProfile.WATERMARK_TEXT}</div>"))

        val proProfile = freeProfile.copy(
            depotName = "Depot Mutiara Air",
            address = "Jl. Raya Timur No. 9",
            phoneNumber = "0812-9090-1010",
            subscriptionTier = DepotProfile.TIER_PRO
        )
        val proHtml = ExportHelper.generateLaporanHtml(
            summary = summary,
            allTransaksi = listOf(detail),
            allKasbonLogs = emptyList(),
            periodeLabel = "17 Nov 2026",
            isSummaryMode = true,
            depotProfile = proProfile
        )
        assertTrue(proHtml.contains("<div class='kop-brand'>DEPOT MUTIARA AIR</div>"))
        assertTrue(proHtml.contains("Jl. Raya Timur No. 9 • WA/Telp: 0812-9090-1010"))
        assertFalse(proHtml.contains("<div class='pdf-watermark'>"))
    }

    @Test
    fun `depotProfile completeness metrics avatar initials and phone normalization work accurately`() {
        val partialProfile = DepotProfile(
            googleDisplayName = "Bapak Hendra",
            ownerName = "",
            depotName = "Santoya Tirta",
            phoneNumber = "  0812   3456 - 7890 🎉 ",
            address = "",
            receiptFooter = "",
            depotType = DepotProfile.TYPE_BOTH,
            tankCapacityLiters = 5000.0
        )

        assertEquals("ST", partialProfile.avatarInitials())
        assertEquals("0812 3456 - 7890", partialProfile.normalizedPhoneNumber())
        assertEquals(3, partialProfile.completenessCount())
        assertEquals(60, partialProfile.completenessPercent())
        assertFalse(partialProfile.isProfileFullyComplete())

        val fullProfile = partialProfile.copy(
            ownerName = "Hendra Saputra",
            address = "Jl. Raya Puncak No. 10",
            receiptFooter = "Terima kasih sudah berlangganan"
        )
        assertEquals(5, fullProfile.completenessCount())
        assertEquals(100, fullProfile.completenessPercent())
        assertTrue(fullProfile.isProfileFullyComplete())

        val summaryText = fullProfile.toShareableSummaryText()
        assertTrue(summaryText.contains("SANTOYA TIRTA"))
        assertTrue(summaryText.contains("Pemilik: Hendra Saputra"))
        assertTrue(summaryText.contains("WA/Telp: 0812 3456 - 7890"))
        assertTrue(summaryText.contains("Alamat: Jl. Raya Puncak No. 10"))
    }

    @Test
    fun `generateNestedJson exports depot_profile and extractImportedDepotProfile parses it back`() {
        val profile = DepotProfile(
            ownerName = "Pak Dedi",
            depotName = "Depot Air Segar",
            phoneNumber = "0813-9988-7766",
            address = "Jl. Melati No. 3",
            receiptFooter = "Air sehat berkualitas",
            depotType = DepotProfile.TYPE_MINERAL,
            tankCapacityLiters = 3000.0
        )
        val jsonStr = ExportHelper.generateNestedJson(
            pelangganList = listOf(samplePelanggan()),
            transaksiList = listOf(sampleDetail()),
            produkList = emptyList(),
            kurirList = emptyList(),
            pengeluaranList = emptyList(),
            depotProfile = profile
        )
        val extracted = ExportHelper.extractImportedDepotProfile(org.json.JSONObject(jsonStr))
        requireNotNull(extracted)
        assertEquals("Pak Dedi", extracted.ownerName)
        assertEquals("Depot Air Segar", extracted.depotName)
        assertEquals("0813-9988-7766", extracted.phoneNumber)
        assertEquals("Jl. Melati No. 3", extracted.address)
        assertEquals("Air sehat berkualitas", extracted.receiptFooter)
        assertEquals(DepotProfile.TYPE_MINERAL, extracted.depotType)
        assertEquals(3000.0, extracted.tankCapacityLiters ?: 0.0, 0.01)
    }
}

