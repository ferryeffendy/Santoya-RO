package com.example

import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.CrmFollowUpContext
import com.example.util.CrmFollowUpHelper
import com.example.util.CrmJadwalFilter
import com.example.util.CrmJadwalSort
import com.example.util.CrmOrderPrediction
import com.example.util.CrmOrderStatus
import com.example.util.CrmPasifSegment
import com.example.util.CrmPasifSort
import com.example.util.CrmRankingPeriod
import com.example.util.CrmRankingSort
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.viewmodel.PelangganUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CrmFollowUpUnitTest {

    private val zoneId = ZoneId.of("Asia/Jakarta")
    private val kurir = Kurir(id = 1, nama = "Budi")
    private val produk = Produk(id = 1, nama_produk = "Galon RO 19L", harga = 6000.0)

    private fun epochMillis(date: LocalDate, hour: Int = 10): Long {
        return date.atTime(hour, 0).atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun customer(id: Int, nama: String, alamat: String = "Desa Sukamaju") = Pelanggan(
        id = id,
        nama = nama,
        alamat = alamat,
        no_hp = "08123456789"
    )

    private fun txDetail(
        txId: Int,
        pelanggan: Pelanggan,
        date: LocalDate,
        jumlah: Int = 2,
        hargaSatuan: Double = 6000.0,
        isDeleted: Boolean = false,
        status: String = "Lunas"
    ): TransaksiDetail {
        val tx = Transaksi(
            id = txId,
            tanggal = epochMillis(date),
            id_pelanggan = pelanggan.id,
            id_kurir = kurir.id,
            id_produk = produk.id,
            jumlah = jumlah,
            harga_satuan = hargaSatuan,
            total_harga = jumlah * hargaSatuan,
            total_komisi = jumlah * 1000.0,
            metode_bayar = "Tunai",
            status = status,
            isDeleted = isDeleted
        )
        return TransaksiDetail(tx, pelanggan, kurir, produk)
    }

    @Test
    fun `calculateCrmPredictionForCustomer clamps vacation outlier and ignores deleted or zero-qty transactions`() {
        val cust = customer(1, "Bu Ani")
        val today = LocalDate.of(2026, 11, 20)
        // Normal routine ~3 days, plus 1 long 30-day vacation outlier, plus deleted & 0-qty rows
        val d1 = LocalDate.of(2026, 10, 5)
        val d2 = LocalDate.of(2026, 10, 8)   // +3
        val d3 = LocalDate.of(2026, 11, 7)   // +30 (outlier, median=3 -> cap = max(7.5, 10) = 10)
        val d4 = LocalDate.of(2026, 11, 10)  // +3
        val d5 = LocalDate.of(2026, 11, 13)  // +3

        val txs = listOf(
            txDetail(1, cust, d1, jumlah = 4),
            txDetail(2, cust, d2, jumlah = 4),
            txDetail(3, cust, d3, jumlah = 4),
            txDetail(4, cust, d4, jumlah = 4),
            txDetail(5, cust, d5, jumlah = 4),
            txDetail(6, cust, LocalDate.of(2026, 11, 19), jumlah = 99, isDeleted = true),
            txDetail(7, cust, LocalDate.of(2026, 11, 19), jumlah = 0),
            txDetail(8, cust, LocalDate.of(2026, 11, 19), jumlah = 5, status = KaryawanScopeHelper.STATUS_DIHAPUS)
        )

        val prediction = FormatUtils.calculateCrmPredictionForCustomer(txs, today = today, zone = zoneId)

        // Without clamping, average interval would be ~9-10 days; with clamping + recency weights it stays <= 5 days
        assertTrue((prediction.averageIntervalDays ?: 99) <= 5)
        assertEquals(4, prediction.avgGallonsPerOrder)
        assertEquals(epochMillis(d5), prediction.lastOrderMillis)
        assertEquals(5, prediction.distinctOrderDays)
        assertTrue(prediction.isReliableCycle)
        assertEquals(CrmOrderStatus.OVERDUE, prediction.status)
        assertTrue(prediction.priorityScore > 60)
    }

    @Test
    fun `calculateCrmPredictionForCustomer weights recent intervals higher than older ones`() {
        val cust = customer(2, "Pak Budi")
        val today = LocalDate.of(2026, 11, 20)
        // Older interval 10 days, newer interval 4 days -> weighted avg nearer to 4-6 than pure 7
        val txs = listOf(
            txDetail(1, cust, LocalDate.of(2026, 11, 1), jumlah = 2),
            txDetail(2, cust, LocalDate.of(2026, 11, 11), jumlah = 2), // +10 (weight 1)
            txDetail(3, cust, LocalDate.of(2026, 11, 15), jumlah = 2)  // +4  (weight 2) -> (10 + 8)/3 = 6
        )

        val prediction = FormatUtils.calculateCrmPredictionForCustomer(txs, today = today, zone = zoneId)
        assertEquals(6, prediction.averageIntervalDays)
        assertEquals(1, prediction.daysUntilExpected) // Nov 15 + 6 = Nov 21, today Nov 20 => 1 day
        assertEquals(CrmOrderStatus.DUE_SOON, prediction.status)
    }

    @Test
    fun `priorityScore ranks higher volume overdue customer above lower volume due soon customer`() {
        val highVol = customer(1, "Warung Besar")
        val lowVol = customer(2, "Rumah Kecil")
        val today = LocalDate.of(2026, 11, 20)

        val highPred = FormatUtils.calculateCrmPredictionForCustomer(
            listOf(
                txDetail(1, highVol, LocalDate.of(2026, 11, 10), jumlah = 6),
                txDetail(2, highVol, LocalDate.of(2026, 11, 13), jumlah = 6),
                txDetail(3, highVol, LocalDate.of(2026, 11, 16), jumlah = 6) // due Nov 19 -> overdue 1d
            ),
            today = today,
            zone = zoneId
        )
        val lowPred = FormatUtils.calculateCrmPredictionForCustomer(
            listOf(
                txDetail(4, lowVol, LocalDate.of(2026, 11, 14), jumlah = 1),
                txDetail(5, lowVol, LocalDate.of(2026, 11, 17), jumlah = 1) // due Nov 20 -> due today
            ),
            today = today,
            zone = zoneId
        )

        assertTrue(highPred.priorityScore > lowPred.priorityScore)
    }

    @Test
    fun `isContactedInCurrentCycle validates 24h window and auto-clears on newer transaction`() {
        val now = 1_700_000_000_000L
        val twoHoursAgo = now - 2 * 3_600_000L
        val twentyFiveHoursAgo = now - 25 * 3_600_000L
        val fiveHoursAgoOrder = now - 5 * 3_600_000L
        val oneHourAgoOrder = now - 1 * 3_600_000L

        assertTrue(CrmFollowUpHelper.isContactedInCurrentCycle(twoHoursAgo, fiveHoursAgoOrder, now))
        assertFalse(CrmFollowUpHelper.isContactedInCurrentCycle(null, fiveHoursAgoOrder, now))
        assertFalse(CrmFollowUpHelper.isContactedInCurrentCycle(twentyFiveHoursAgo, fiveHoursAgoOrder, now))
        assertFalse(CrmFollowUpHelper.isContactedInCurrentCycle(twoHoursAgo, oneHourAgoOrder, now))
        assertFalse(CrmFollowUpHelper.isContactedInCurrentCycle(twoHoursAgo, twoHoursAgo, now))
    }

    @Test
    fun `buildContextualWhatsAppMessage creates distinct messages for due, overdue, passive, and general contexts`() {
        val duePred = CrmOrderPrediction(
            status = CrmOrderStatus.DUE_SOON,
            averageIntervalDays = 4,
            daysUntilExpected = 0,
            avgGallonsPerOrder = 3
        )
        val overduePred = CrmOrderPrediction(
            status = CrmOrderStatus.OVERDUE,
            averageIntervalDays = 5,
            daysUntilExpected = -3,
            avgGallonsPerOrder = 2
        )

        val dueMsg = CrmFollowUpHelper.buildContextualWhatsAppMessage("Siti", duePred, CrmFollowUpContext.JATUH_TEMPO)
        val overdueMsg = CrmFollowUpHelper.buildContextualWhatsAppMessage("Siti", overduePred, CrmFollowUpContext.TERLAMBAT)
        val passiveLapsedMsg = CrmFollowUpHelper.buildContextualWhatsAppMessage("Siti", overduePred, CrmFollowUpContext.PASIF, passiveDays = 21L)
        val passiveNewMsg = CrmFollowUpHelper.buildContextualWhatsAppMessage("Siti", overduePred, CrmFollowUpContext.PASIF, passiveDays = 999L)
        val generalMsg = CrmFollowUpHelper.buildContextualWhatsAppMessage("Siti", duePred, CrmFollowUpContext.UMUM)

        assertTrue(dueMsg.contains("tiap 4 hari"))
        assertTrue(dueMsg.contains("3 galon"))
        assertTrue(overdueMsg.contains("lewat 3 hari"))
        assertTrue(passiveLapsedMsg.contains("21 hari belum pesan"))
        assertFalse(passiveNewMsg.contains("999"))
        assertNotEquals(dueMsg, overdueMsg)
        assertNotEquals(overdueMsg, passiveLapsedMsg)
        assertNotEquals(dueMsg, generalMsg)
    }

    @Test
    fun `filterAndSortJadwal filters status and uncontacted and respects sort order`() {
        val now = 1_700_000_000_000L
        val c1 = PelangganUiModel(
            pelanggan = customer(1, "Alpha"),
            estimatedIntervalDays = 4,
            crmPrediction = CrmOrderPrediction(
                status = CrmOrderStatus.OVERDUE,
                daysUntilExpected = -4,
                priorityScore = 85,
                lastOrderMillis = now - 400_000_000L
            )
        )
        val c2 = PelangganUiModel(
            pelanggan = customer(2, "Beta"),
            estimatedIntervalDays = 5,
            crmPrediction = CrmOrderPrediction(
                status = CrmOrderStatus.DUE_SOON,
                daysUntilExpected = 0,
                priorityScore = 64,
                lastOrderMillis = now - 300_000_000L
            )
        )
        val c3 = PelangganUiModel(
            pelanggan = customer(3, "Gamma"),
            estimatedIntervalDays = 3,
            crmPrediction = CrmOrderPrediction(
                status = CrmOrderStatus.OVERDUE,
                daysUntilExpected = -1,
                priorityScore = 92,
                lastOrderMillis = now - 200_000_000L
            )
        )

        val contactedMap = mapOf(1 to (now - 3_600_000L))

        val overdueOnly = CrmFollowUpHelper.filterAndSortJadwal(
            listOf(c1, c2, c3),
            CrmJadwalFilter.TERLAMBAT,
            CrmJadwalSort.PRIORITAS,
            contactedMap,
            now
        )
        assertEquals(listOf(3, 1), overdueOnly.map { it.pelanggan.id })

        val dueOnly = CrmFollowUpHelper.filterAndSortJadwal(
            listOf(c1, c2, c3),
            CrmJadwalFilter.HARI_INI_BESOK,
            CrmJadwalSort.PRIORITAS,
            contactedMap,
            now
        )
        assertEquals(listOf(2), dueOnly.map { it.pelanggan.id })

        val uncontactedOnly = CrmFollowUpHelper.filterAndSortJadwal(
            listOf(c1, c2, c3),
            CrmJadwalFilter.BELUM_DIHUBUNGI,
            CrmJadwalSort.TERLAMBAT_LAMA,
            contactedMap,
            now
        )
        assertEquals(listOf(3, 2), uncontactedOnly.map { it.pelanggan.id })
    }

    @Test
    fun `buildPassiveCustomerEntries segments lapsed regular customers from never-ordered customers`() {
        val today = LocalDate.of(2026, 11, 20)
        val nowMillis = epochMillis(today, hour = 12)
        val lapsedCust = customer(1, "Lama")
        val activeCust = customer(2, "Aktif")
        val neverOrderedCust = customer(3, "Baru")
        val deletedCust = customer(4, "Hilang", alamat = "[DIHAPUS:Admin] Blok A")

        val txs = listOf(
            txDetail(1, lapsedCust, today.minusDays(20), jumlah = 3),
            txDetail(2, lapsedCust, today.minusDays(15), jumlah = 4),
            txDetail(3, activeCust, today.minusDays(2), jumlah = 2)
        )

        val allPassive = CrmFollowUpHelper.buildPassiveCustomerEntries(
            pelangganList = listOf(lapsedCust, activeCust, neverOrderedCust, deletedCust),
            transaksiList = txs,
            minDays = 7L,
            segment = CrmPasifSegment.SEMUA,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        assertEquals(listOf(1, 3), allPassive.map { it.pelanggan.id })
        assertEquals(15L, allPassive.first().daysPassive)
        assertEquals(7, allPassive.first().totalGalon)
        assertEquals(999L, allPassive.last().daysPassive)

        val pernahRutin = CrmFollowUpHelper.buildPassiveCustomerEntries(
            pelangganList = listOf(lapsedCust, activeCust, neverOrderedCust),
            transaksiList = txs,
            minDays = 7L,
            segment = CrmPasifSegment.PERNAH_RUTIN,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        assertEquals(listOf(1), pernahRutin.map { it.pelanggan.id })

        val belumPernah = CrmFollowUpHelper.buildPassiveCustomerEntries(
            pelangganList = listOf(lapsedCust, activeCust, neverOrderedCust),
            transaksiList = txs,
            minDays = 7L,
            segment = CrmPasifSegment.BELUM_PERNAH_ORDER,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        assertEquals(listOf(3), belumPernah.map { it.pelanggan.id })
    }

    @Test
    fun `buildRankedCustomerEntries supports period filter, gallon vs omzet sorting, and preserves cycle interval`() {
        val today = LocalDate.of(2026, 11, 20)
        val nowMillis = epochMillis(today, hour = 12)
        val c1 = customer(1, "Warung A") // higher gallons, lower price -> higher galon, lower omzet
        val c2 = customer(2, "Kantor B") // fewer gallons, higher price -> lower galon, higher omzet
        val c3 = customer(3, "Rumah C")  // only ordered 60 days ago

        val txs = listOf(
            txDetail(1, c1, LocalDate.of(2026, 11, 10), jumlah = 5, hargaSatuan = 4000.0),
            txDetail(2, c1, LocalDate.of(2026, 11, 15), jumlah = 5, hargaSatuan = 4000.0), // 10 galon, Rp 40.000, cycle 5d
            txDetail(3, c2, LocalDate.of(2026, 11, 12), jumlah = 6, hargaSatuan = 10000.0), // 6 galon, Rp 60.000
            txDetail(4, c3, LocalDate.of(2026, 9, 10), jumlah = 20, hargaSatuan = 6000.0)
        )

        val monthByGallon = CrmFollowUpHelper.buildRankedCustomerEntries(
            pelangganList = listOf(c1, c2, c3),
            transaksiList = txs,
            period = CrmRankingPeriod.BULAN_INI,
            sort = CrmRankingSort.GALON,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        assertEquals(listOf(1, 2), monthByGallon.map { it.pelanggan.id })
        assertEquals(1, monthByGallon.first().rank)
        assertEquals(5, monthByGallon.first().averageIntervalDays)

        val monthByOmzet = CrmFollowUpHelper.buildRankedCustomerEntries(
            pelangganList = listOf(c1, c2, c3),
            transaksiList = txs,
            period = CrmRankingPeriod.BULAN_INI,
            sort = CrmRankingSort.OMZET,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        assertEquals(listOf(2, 1), monthByOmzet.map { it.pelanggan.id })

        val allTimeByGallon = CrmFollowUpHelper.buildRankedCustomerEntries(
            pelangganList = listOf(c1, c2, c3),
            transaksiList = txs,
            period = CrmRankingPeriod.SEMUA,
            sort = CrmRankingSort.GALON,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        assertEquals(listOf(3, 1, 2), allTimeByGallon.map { it.pelanggan.id })
    }

    @Test
    fun `buildPassiveCustomerEntries supports sorting by longest passive, highest gallon, and name A-Z`() {
        val today = LocalDate.of(2026, 11, 20)
        val nowMillis = epochMillis(today, hour = 12)
        val c1 = customer(1, "Zaki") // 20 days passive, 2 galon
        val c2 = customer(2, "Ani")  // 10 days passive, 15 galon
        val c3 = customer(3, "Budi") // never ordered (0 galon, 999d)

        val txs = listOf(
            txDetail(1, c1, today.minusDays(20), jumlah = 2),
            txDetail(2, c2, today.minusDays(10), jumlah = 15)
        )

        val byLongest = CrmFollowUpHelper.buildPassiveCustomerEntries(
            pelangganList = listOf(c1, c2, c3),
            transaksiList = txs,
            minDays = 7L,
            sort = CrmPasifSort.TERLAMA_PASIF,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        assertEquals(listOf(1, 2, 3), byLongest.map { it.pelanggan.id })

        val byGallon = CrmFollowUpHelper.buildPassiveCustomerEntries(
            pelangganList = listOf(c1, c2, c3),
            transaksiList = txs,
            minDays = 7L,
            sort = CrmPasifSort.GALON_TERBANYAK,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        assertEquals(listOf(2, 1, 3), byGallon.map { it.pelanggan.id })

        val byName = CrmFollowUpHelper.buildPassiveCustomerEntries(
            pelangganList = listOf(c1, c2, c3),
            transaksiList = txs,
            minDays = 7L,
            sort = CrmPasifSort.NAMA_AZ,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        assertEquals(listOf(2, 3, 1), byName.map { it.pelanggan.id })
    }
}
