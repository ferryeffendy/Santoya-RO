package com.example

import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.CrmOrderPrediction
import com.example.util.KaryawanScopeHelper
import com.example.util.PelangganAuditHelper
import com.example.util.SantoyaReceiptHelper
import com.example.util.SemuaPelangganSort
import com.example.viewmodel.PelangganUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PelangganAuditUnitTest {

    private val zoneId = ZoneId.of("Asia/Jakarta")

    private fun epochMillis(year: Int, month: Int, day: Int, hour: Int = 10): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun customer(
        id: Int,
        nama: String,
        alamat: String = "Desa Sukamaju",
        noHp: String = "08123456789",
        sisaKasbon: Double = 0.0
    ) = Pelanggan(
        id = id,
        nama = nama,
        alamat = alamat,
        no_hp = noHp,
        sisa_kasbon = sisaKasbon
    )

    @Test
    fun `calculateKasbonPaymentBreakdown handles exact, partial, overpayment kembalian, and zero debt`() {
        val exact = PelangganAuditHelper.calculateKasbonPaymentBreakdown(
            sisaKasbon = 25_000.0,
            inputNominal = 25_000.0
        )
        assertEquals(25_000.0, exact.effectivePaid, 0.01)
        assertEquals(25_000.0, exact.receivedAmount, 0.01)
        assertEquals(0.0, exact.kembalian, 0.01)
        assertEquals(0.0, exact.remainingAfterPay, 0.01)
        assertTrue(exact.isLunas)

        val partial = PelangganAuditHelper.calculateKasbonPaymentBreakdown(
            sisaKasbon = 30_000.0,
            inputNominal = 10_000.0
        )
        assertEquals(10_000.0, partial.effectivePaid, 0.01)
        assertEquals(10_000.0, partial.receivedAmount, 0.01)
        assertEquals(0.0, partial.kembalian, 0.01)
        assertEquals(20_000.0, partial.remainingAfterPay, 0.01)
        assertFalse(partial.isLunas)

        val overpaid = PelangganAuditHelper.calculateKasbonPaymentBreakdown(
            sisaKasbon = 18_000.0,
            inputNominal = 50_000.0
        )
        assertEquals(18_000.0, overpaid.effectivePaid, 0.01)
        assertEquals(50_000.0, overpaid.receivedAmount, 0.01)
        assertEquals(32_000.0, overpaid.kembalian, 0.01)
        assertEquals(0.0, overpaid.remainingAfterPay, 0.01)
        assertTrue(overpaid.isLunas)

        val zeroDebt = PelangganAuditHelper.calculateKasbonPaymentBreakdown(
            sisaKasbon = 0.0,
            inputNominal = 10_000.0
        )
        assertEquals(0.0, zeroDebt.effectivePaid, 0.01)
        assertEquals(10_000.0, zeroDebt.kembalian, 0.01)
        assertFalse(zeroDebt.isLunas)
    }

    @Test
    fun `generatePelunasanReceipt shows effective payment, received cash, kembalian, and clean address`() {
        val cust = customer(
            id = 1,
            nama = "Bu Sari",
            alamat = "[DIHAPUS:Andi] Jl. Melati No. 5",
            sisaKasbon = 18_000.0
        )
        val breakdown = PelangganAuditHelper.calculateKasbonPaymentBreakdown(
            sisaKasbon = cust.sisa_kasbon,
            inputNominal = 50_000.0
        )

        val receipt = SantoyaReceiptHelper.generatePelunasanReceipt(
            pelanggan = cust,
            nominalBayar = breakdown.effectivePaid,
            sisaKasbon = breakdown.remainingAfterPay,
            uangDiterima = breakdown.receivedAmount,
            kembalian = breakdown.kembalian
        )

        assertFalse(receipt.contains("[DIHAPUS:"))
        assertTrue(receipt.contains("Jl. Melati No. 5"))
        assertTrue(receipt.contains("Nominal Bayar:"))
        assertTrue(receipt.contains("18.000"))
        assertTrue(receipt.contains("Uang Diterima:"))
        assertTrue(receipt.contains("50.000"))
        assertTrue(receipt.contains("Kembalian:"))
        assertTrue(receipt.contains("32.000"))
        assertTrue(receipt.contains("LUNAS"))
        assertTrue(receipt.contains("TOTAL: RP 18.000"))
    }

    @Test
    fun `buildCustomerSpendStats and filterActiveUnpaidKasbonNotes exclude soft deleted transactions`() {
        val cust = customer(id = 7, nama = "Pak Eko", sisaKasbon = 12_000.0)
        val kurir = Kurir(id = 1, nama = "Budi")
        val produk = Produk(id = 1, nama_produk = "Galon RO 19L", harga = 6000.0)

        val day1 = epochMillis(2026, 11, 1, 9)
        val day2 = epochMillis(2026, 11, 5, 14)
        val day3 = epochMillis(2026, 11, 10, 16)

        val activeCash = Transaksi(
            id = 10, tanggal = day1, id_pelanggan = 7, id_kurir = 1, id_produk = 1,
            jumlah = 3, harga_satuan = 6000.0, total_harga = 18_000.0, total_komisi = 3000.0,
            metode_bayar = "Cash", status = "Lunas"
        )
        val activeKasbon = Transaksi(
            id = 11, tanggal = day2, id_pelanggan = 7, id_kurir = 1, id_produk = 1,
            jumlah = 2, harga_satuan = 6000.0, total_harga = 12_000.0, total_komisi = 2000.0,
            metode_bayar = "-", status = "Kasbon", sisaKasbon = 12_000L
        )
        val deletedByFlag = Transaksi(
            id = 12, tanggal = day3, id_pelanggan = 7, id_kurir = 1, id_produk = 1,
            jumlah = 9, harga_satuan = 6000.0, total_harga = 54_000.0, total_komisi = 9000.0,
            metode_bayar = "-", status = "Kasbon", sisaKasbon = 54_000L, isDeleted = true
        )
        val deletedByStatus = Transaksi(
            id = 13, tanggal = day3, id_pelanggan = 7, id_kurir = 1, id_produk = 1,
            jumlah = 4, harga_satuan = 6000.0, total_harga = 24_000.0, total_komisi = 4000.0,
            metode_bayar = "-", status = KaryawanScopeHelper.STATUS_DIHAPUS, isDeleted = false
        )

        val details = listOf(
            TransaksiDetail(activeCash, cust, kurir, produk),
            TransaksiDetail(activeKasbon, cust, kurir, produk),
            TransaksiDetail(deletedByFlag, cust, kurir, produk),
            TransaksiDetail(deletedByStatus, cust, kurir, produk)
        )

        val stats = PelangganAuditHelper.buildCustomerSpendStats(details)[7]
        assertNotNull(stats)
        assertEquals(5, stats?.totalGalon)
        assertEquals(2, stats?.totalOrder)
        assertEquals(30_000.0, stats?.totalOmzet ?: 0.0, 0.01)
        assertEquals(day2, stats?.lastOrderMillis)

        val unpaidNotes = PelangganAuditHelper.filterActiveUnpaidKasbonNotes(7, details)
        assertEquals(1, unpaidNotes.size)
        assertEquals(11, unpaidNotes.first().transaksi.id)
    }

    @Test
    fun `findPotentialDuplicatePelanggan matches normalized phone and case insensitive name while ignoring self and deleted`() {
        val active1 = customer(id = 1, nama = "Warung Berkah", noHp = "0812-3456-7890")
        val deleted = customer(
            id = 2,
            nama = "Toko Lama",
            alamat = "[DIHAPUS:Kurir] Blok B",
            noHp = "085611112222"
        )
        val existing = listOf(active1, deleted)

        val phoneMatch = PelangganAuditHelper.findPotentialDuplicatePelanggan(
            nama = "Pemilik Baru",
            noHp = "+62 812 3456 7890",
            existingList = existing
        )
        assertEquals(1, phoneMatch?.id)

        val nameMatch = PelangganAuditHelper.findPotentialDuplicatePelanggan(
            nama = "  warung berkah ",
            noHp = "",
            existingList = existing
        )
        assertEquals(1, nameMatch?.id)

        val selfIgnored = PelangganAuditHelper.findPotentialDuplicatePelanggan(
            nama = "Warung Berkah",
            noHp = "081234567890",
            existingList = existing,
            excludeId = 1
        )
        assertNull(selfIgnored)

        val deletedIgnored = PelangganAuditHelper.findPotentialDuplicatePelanggan(
            nama = "Toko Lama",
            noHp = "085611112222",
            existingList = existing
        )
        assertNull(deletedIgnored)
    }

    @Test
    fun `normalizeSpecialPrice treats zero or negative as null and keeps positive price`() {
        assertNull(PelangganAuditHelper.normalizeSpecialPrice(null))
        assertNull(PelangganAuditHelper.normalizeSpecialPrice(0.0))
        assertNull(PelangganAuditHelper.normalizeSpecialPrice(-5000.0))
        assertEquals(4500.0, PelangganAuditHelper.normalizeSpecialPrice(4500.0) ?: 0.0, 0.01)
    }

    @Test
    fun `formatLocalPhoneNumber normalizes country prefixes punctuation and rejects invalid inputs`() {
        assertEquals("081234567890", PelangganAuditHelper.formatLocalPhoneNumber("+62 812-3456-7890"))
        assertEquals("081234567890", PelangganAuditHelper.formatLocalPhoneNumber("6281234567890"))
        assertEquals("081234567890", PelangganAuditHelper.formatLocalPhoneNumber("0812 3456 7890"))
        assertEquals("0215551234", PelangganAuditHelper.formatLocalPhoneNumber("(021) 555-1234"))
        assertEquals("", PelangganAuditHelper.formatLocalPhoneNumber(null))
        assertEquals("", PelangganAuditHelper.formatLocalPhoneNumber("   "))
        assertEquals("", PelangganAuditHelper.formatLocalPhoneNumber("---"))
        assertEquals("", PelangganAuditHelper.formatLocalPhoneNumber("12345"))
    }

    @Test
    fun `findDuplicatePhoneOwner ignores self and soft deleted customers while matching normalized digits`() {
        val ownerA = customer(id = 1, nama = "Bu Ani", noHp = "081234567890")
        val deletedB = customer(
            id = 2,
            nama = "Pak Budi",
            alamat = "[DIHAPUS:Admin] Blok C",
            noHp = "085611112222"
        )
        val blankC = customer(id = 3, nama = "Ibu Sari", noHp = "")
        val list = listOf(ownerA, deletedB, blankC)

        val matched = PelangganAuditHelper.findDuplicatePhoneOwner(
            noHp = "+62 812-3456-7890",
            existingList = list,
            excludeId = 3
        )
        assertEquals(1, matched?.id)

        val selfIgnored = PelangganAuditHelper.findDuplicatePhoneOwner(
            noHp = "081234567890",
            existingList = list,
            excludeId = 1
        )
        assertNull(selfIgnored)

        val deletedIgnored = PelangganAuditHelper.findDuplicatePhoneOwner(
            noHp = "085611112222",
            existingList = list,
            excludeId = 3
        )
        assertNull(deletedIgnored)
    }

    @Test
    fun `sortPelangganUiModels supports name A-Z, highest kasbon, and newest order`() {
        val m1 = PelangganUiModel(
            pelanggan = customer(id = 1, nama = "Zebra", sisaKasbon = 10_000.0).copy(updatedAt = 100L),
            estimatedIntervalDays = 5,
            crmPrediction = CrmOrderPrediction(status = com.example.util.CrmOrderStatus.ACTIVE, lastOrderMillis = 1_000L)
        )
        val m2 = PelangganUiModel(
            pelanggan = customer(id = 2, nama = "Alpha", sisaKasbon = 45_000.0).copy(updatedAt = 200L),
            estimatedIntervalDays = 5,
            crmPrediction = CrmOrderPrediction(status = com.example.util.CrmOrderStatus.ACTIVE, lastOrderMillis = 5_000L)
        )
        val m3 = PelangganUiModel(
            pelanggan = customer(id = 3, nama = "Beta", sisaKasbon = 0.0).copy(updatedAt = 9_000L),
            estimatedIntervalDays = null,
            crmPrediction = CrmOrderPrediction(status = com.example.util.CrmOrderStatus.INSUFFICIENT_DATA, lastOrderMillis = null)
        )

        val byName = PelangganAuditHelper.sortPelangganUiModels(listOf(m1, m2, m3), SemuaPelangganSort.NAMA_AZ)
        assertEquals(listOf(2, 3, 1), byName.map { it.pelanggan.id })

        val byKasbon = PelangganAuditHelper.sortPelangganUiModels(listOf(m1, m2, m3), SemuaPelangganSort.KASBON_TERTINGGI)
        assertEquals(listOf(2, 1, 3), byKasbon.map { it.pelanggan.id })

        val byNewest = PelangganAuditHelper.sortPelangganUiModels(listOf(m1, m2, m3), SemuaPelangganSort.TERBARU)
        assertEquals(listOf(3, 2, 1), byNewest.map { it.pelanggan.id })
    }
}
