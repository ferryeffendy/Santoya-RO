package com.example

import com.example.data.entity.KasbonLog
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.ExportHelper
import com.example.util.KaryawanScopeHelper
import com.example.util.LaporanFinanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LaporanFinanceCalculatorUnitTest {

    private val zoneId = ZoneId.of("Asia/Jakarta")

    private fun epochMillis(year: Int, month: Int, day: Int, hour: Int = 10): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun sampleCustomer(id: Int = 1, nama: String = "Budi", alamat: String = "Desa Sukamaju", sisa: Double = 0.0): Pelanggan {
        return Pelanggan(id = id, nama = nama, alamat = alamat, no_hp = "0812345", sisa_kasbon = sisa)
    }

    private fun sampleProduk(id: Int = 1, nama: String = "Galon RO 19L", harga: Double = 6000.0): Produk {
        return Produk(id = id, nama_produk = nama, harga = harga)
    }

    private fun sampleKurir(id: Int = 1, nama: String = "Andi"): Kurir {
        return Kurir(id = id, nama = nama, komisi_per_galon = 1000.0)
    }

    @Test
    fun `kasbon settled via FIFO same day or later never double counts in summary`() {
        val day1Morning = epochMillis(2026, 11, 10, 9)
        val day1Evening = epochMillis(2026, 11, 10, 17)
        val cust = sampleCustomer(id = 1, nama = "Ani")
        val prod = sampleProduk()
        val kurir = sampleKurir()

        // Nota dibuat pagi Rp 20.000 (Kasbon DP 0), dilunasi sore via FIFO sehingga uangMuka tertimpa jadi 20.000
        val tx = Transaksi(
            id = 10,
            tanggal = day1Morning,
            id_pelanggan = cust.id,
            id_kurir = kurir.id,
            id_produk = prod.id,
            jumlah = 4,
            harga_satuan = 5000.0,
            total_harga = 20_000.0,
            total_komisi = 4000.0,
            metode_bayar = "Cash",
            status = "Lunas",
            uangMuka = 20_000L,
            sisaKasbon = 0L,
            tanggalBayar = day1Evening
        )
        val log = KasbonLog(
            id = 1,
            id_pelanggan = cust.id,
            tanggal = day1Evening,
            jumlah_bayar = 20_000.0,
            sisa_kasbon_sebelum = 20_000.0,
            sisa_kasbon_sesudah = 0.0,
            metode_bayar = "Transfer",
            catatan = "Pelunasan Kasbon (Transfer) • Nota #10"
        )

        val dayStart = epochMillis(2026, 11, 10, 0)
        val dayEnd = epochMillis(2026, 11, 10, 23)
        val summary = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = listOf(TransaksiDetail(tx, cust, kurir, prod)),
            allPengeluaran = emptyList(),
            allKasbonLogs = listOf(log),
            allPelanggan = listOf(cust),
            startTime = dayStart,
            endTime = dayEnd,
            zoneId = zoneId
        )

        assertEquals(0.0, summary.penjualanTunaiLangsung, 0.01)
        assertEquals(20_000.0, summary.pelunasanKasbonPeriode, 0.01)
        assertEquals(20_000.0, summary.totalKasMasuk, 0.01)
        assertEquals(16_000.0, summary.labaBersih, 0.01)
        assertEquals(0.0, summary.totalKasTunai, 0.01)
        assertEquals(20_000.0, summary.totalKasNonTunai, 0.01)
    }

    @Test
    fun `kasbon with initial DP settled later via Transfer attributes DP to Day1 and settlement to Day2`() {
        val day1 = epochMillis(2026, 11, 1, 10)
        val day5 = epochMillis(2026, 11, 5, 14)
        val cust = sampleCustomer(id = 2, nama = "Budi")
        val prod = sampleProduk()

        // Total Rp 25.000, DP awal Rp 5.000 tunai pada Day 1, dilunasi Rp 20.000 Transfer pada Day 5 via FIFO
        val tx = Transaksi(
            id = 21,
            tanggal = day1,
            id_pelanggan = cust.id,
            id_kurir = null,
            id_produk = prod.id,
            jumlah = 5,
            harga_satuan = 5000.0,
            total_harga = 25_000.0,
            total_komisi = 0.0,
            metode_bayar = "Transfer",
            status = "Lunas",
            uangMuka = 25_000L,
            sisaKasbon = 0L,
            tanggalBayar = day5
        )
        val log = KasbonLog(
            id = 2,
            id_pelanggan = cust.id,
            tanggal = day5,
            jumlah_bayar = 20_000.0,
            sisa_kasbon_sebelum = 20_000.0,
            sisa_kasbon_sesudah = 0.0,
            metode_bayar = "Transfer",
            catatan = "Pelunasan Kasbon (Transfer) • Nota #21"
        )

        val monthSummary = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = listOf(TransaksiDetail(tx, cust, null, prod)),
            allPengeluaran = emptyList(),
            allKasbonLogs = listOf(log),
            allPelanggan = listOf(cust),
            startTime = epochMillis(2026, 11, 1, 0),
            endTime = epochMillis(2026, 11, 30, 23),
            zoneId = zoneId
        )
        assertEquals(5_000.0, monthSummary.penjualanTunaiLangsung, 0.01)
        assertEquals(20_000.0, monthSummary.pelunasanKasbonPeriode, 0.01)
        assertEquals(25_000.0, monthSummary.totalKasMasuk, 0.01)

        // Cek Day 5 saja (tanpa transaksi baru, hanya pelunasan kasbon): hasPeriodActivity wajib true
        val day5OnlySummary = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = listOf(TransaksiDetail(tx, cust, null, prod)),
            allPengeluaran = emptyList(),
            allKasbonLogs = listOf(log),
            allPelanggan = listOf(cust),
            startTime = epochMillis(2026, 11, 5, 0),
            endTime = epochMillis(2026, 11, 5, 23),
            zoneId = zoneId
        )
        assertTrue(day5OnlySummary.hasPeriodActivity)
        assertEquals(20_000.0, day5OnlySummary.totalKasMasuk, 0.01)
        assertEquals(1, day5OnlySummary.filteredKasbonLogs.size)
        assertEquals("Budi", day5OnlySummary.filteredKasbonLogs.first().namaPelanggan)
    }

    @Test
    fun `partial installment via FIFO splits original DP from KasbonLog and handles shifted cross-device nota IDs`() {
        val day1 = epochMillis(2026, 11, 2, 10)
        val day3 = epochMillis(2026, 11, 3, 12)
        val cust = sampleCustomer(id = 3, nama = "Citra", sisa = 9_000.0)
        val prod = sampleProduk()

        // Total 20.000, DP awal 5.000, cicil 6.000 di Day 3 -> uangMuka sekarang 11.000, sisa 9.000.
        // Simulasi sync lintas perangkat: local ID 99, catatan masih menyebut Nota #4 dari device asal.
        val tx = Transaksi(
            id = 99,
            tanggal = day1,
            id_pelanggan = cust.id,
            id_kurir = null,
            id_produk = prod.id,
            jumlah = 4,
            harga_satuan = 5000.0,
            total_harga = 20_000.0,
            total_komisi = 0.0,
            metode_bayar = "Bayar Sebagian",
            status = "Kasbon",
            uangMuka = 11_000L,
            sisaKasbon = 9_000L,
            tanggalBayar = null
        )
        val log = KasbonLog(
            id = 15,
            id_pelanggan = cust.id,
            tanggal = day3,
            jumlah_bayar = 6_000.0,
            sisa_kasbon_sebelum = 15_000.0,
            sisa_kasbon_sesudah = 9_000.0,
            metode_bayar = "Cash",
            catatan = "Pelunasan Kasbon (Cash) • Nota #4"
        )

        val monthSummary = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = listOf(TransaksiDetail(tx, cust, null, prod)),
            allPengeluaran = emptyList(),
            allKasbonLogs = listOf(log),
            allPelanggan = listOf(cust),
            startTime = epochMillis(2026, 11, 1, 0),
            endTime = epochMillis(2026, 11, 30, 23),
            zoneId = zoneId
        )

        assertEquals(5_000.0, monthSummary.penjualanTunaiLangsung, 0.01)
        assertEquals(6_000.0, monthSummary.pelunasanKasbonPeriode, 0.01)
        assertEquals(11_000.0, monthSummary.totalKasMasuk, 0.01)
        assertEquals(9_000.0, monthSummary.piutangKasbonPeriode, 0.01)
    }

    @Test
    fun `deleted transactions are excluded and desa ranking normalizes case and street prefixes`() {
        val day = epochMillis(2026, 11, 10, 11)
        val c1 = sampleCustomer(id = 1, nama = "C1", alamat = "Jl. Raya Sukamaju No. 12")
        val c2 = sampleCustomer(id = 2, nama = "C2", alamat = "desa sukamaju")
        val c3 = sampleCustomer(id = 3, nama = "C3", alamat = "-")
        val kurir = sampleKurir(id = 1, nama = "Dedi")

        val t1 = Transaksi(1, day, 1, 1, 1, 5, 6000.0, 30_000.0, 5000.0, "Cash", "Lunas", 0L, 0L, day)
        val t2 = Transaksi(2, day, 2, 1, 1, 3, 6000.0, 18_000.0, 3000.0, "Cash", "Lunas", 0L, 0L, day)
        val t3 = Transaksi(3, day, 3, null, 1, 2, 6000.0, 12_000.0, 0.0, "Cash", "Lunas", 0L, 0L, day)
        val deleted = Transaksi(4, day, 1, 1, 1, 99, 6000.0, 594_000.0, 99_000.0, "Cash", KaryawanScopeHelper.STATUS_DIHAPUS, 0L, 0L, day, isDeleted = true)

        val summary = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = listOf(
                TransaksiDetail(t1, c1, kurir, null),
                TransaksiDetail(t2, c2, kurir, null),
                TransaksiDetail(t3, c3, null, null),
                TransaksiDetail(deleted, c1, kurir, null)
            ),
            allPengeluaran = listOf(Pengeluaran(id = 1, tanggal = day, nama_item = "Listrik", kategori = "Listrik & Air", nominal = 10_000.0)),
            allKasbonLogs = emptyList(),
            allPelanggan = listOf(c1, c2, c3),
            startTime = epochMillis(2026, 11, 10, 0),
            endTime = epochMillis(2026, 11, 10, 23),
            zoneId = zoneId
        )

        assertEquals(10, summary.totalGalon)
        assertEquals(60_000.0, summary.totalKasMasuk, 0.01)
        assertEquals(8_000.0, summary.totalKomisiKurir, 0.01)
        assertEquals(42_000.0, summary.labaBersih, 0.01)

        val sukamajuEntry = summary.desaEntries.find { it.title == "Sukamaju" }
        assertEquals(8, sukamajuEntry?.totalGalon)
        assertEquals(2, sukamajuEntry?.totalTransaksi)

        val dediEntry = summary.kurirEntries.find { it.title == "Dedi" }
        assertEquals(8_000.0, dediEntry?.totalKomisi ?: 0.0, 0.01)
    }

    @Test
    fun `export csv and pdf html include kasbon repayments and properly escape quotes and html tags`() {
        val day1 = epochMillis(2026, 11, 1, 10)
        val day2 = epochMillis(2026, 11, 2, 15)
        val cust = sampleCustomer(id = 1, nama = "Toko \"Maju\" <Jaya>")
        val kurir = sampleKurir(id = 1, nama = "Rudi")
        val prod = sampleProduk(id = 1, nama = "Galon RO 19L")

        val tx = Transaksi(
            id = 5,
            tanggal = day1,
            id_pelanggan = cust.id,
            id_kurir = kurir.id,
            id_produk = prod.id,
            jumlah = 2,
            harga_satuan = 6000.0,
            total_harga = 12_000.0,
            total_komisi = 2000.0,
            metode_bayar = "Cash",
            status = "Lunas",
            uangMuka = 12_000L,
            sisaKasbon = 0L,
            tanggalBayar = day2
        )
        val log = KasbonLog(
            id = 9,
            id_pelanggan = cust.id,
            tanggal = day2,
            jumlah_bayar = 12_000.0,
            sisa_kasbon_sebelum = 12_000.0,
            sisa_kasbon_sesudah = 0.0,
            metode_bayar = "Cash",
            catatan = "Pelunasan Kasbon • Nota #5"
        )
        val details = listOf(TransaksiDetail(tx, cust, kurir, prod))
        val start = epochMillis(2026, 11, 1, 0)
        val end = epochMillis(2026, 11, 2, 23)

        val summary = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = details,
            allPengeluaran = emptyList(),
            allKasbonLogs = listOf(log),
            allPelanggan = listOf(cust),
            startTime = start,
            endTime = end,
            zoneId = zoneId
        )

        val csv = ExportHelper.generateCsv(
            transaksiList = details,
            pengeluaranList = emptyList(),
            kasbonLogList = listOf(log),
            pelangganList = listOf(cust),
            startTime = start,
            endTime = end,
            periodeLabel = "01 Nov 2026 - 02 Nov 2026"
        )
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("Total Kas Masuk (Pendapatan Riil),12000"))
        assertTrue(csv.contains("=== PELUNASAN & CICILAN KASBON SANTOYA RO ==="))
        assertTrue(csv.contains("\"Toko \"\"Maju\"\" <Jaya>\""))

        val html = ExportHelper.generateLaporanHtml(
            summary = summary,
            allTransaksi = details,
            allKasbonLogs = listOf(log),
            periodeLabel = "01 Nov 2026 - 02 Nov 2026",
            isSummaryMode = true
        )
        assertTrue(html.contains("Toko &quot;Maju&quot; &lt;Jaya&gt;"))
        assertTrue(html.contains("Ringkasan Arus Kas Harian (2 Hari)"))
        assertFalse(html.contains("<Jaya>"))
    }

    @Test
    fun `buildExportFileName sanitizes invalid characters and formats descriptive filename`() {
        val fileNamePdf = ExportHelper.buildExportFileName(
            prefix = "Laporan/Santoya:RO",
            periodeLabel = "01 Nov 2026 – 16 Nov 2026",
            extension = ".pdf",
            timestampMillis = epochMillis(2026, 11, 16, 12)
        )
        assertTrue(fileNamePdf.endsWith(".pdf"))
        assertFalse(fileNamePdf.contains("/"))
        assertFalse(fileNamePdf.contains(":"))
        assertFalse(fileNamePdf.contains("–"))
        assertTrue(fileNamePdf.contains("20261116"))
        assertTrue(fileNamePdf.startsWith("Laporan_Santoya_RO_01_Nov_2026_16_Nov_2026_"))

        val fileNameCsv = ExportHelper.buildExportFileName(
            prefix = "Laporan_Santoya",
            periodeLabel = "Bulan Ini",
            extension = "csv",
            timestampMillis = epochMillis(2026, 11, 16, 12)
        )
        assertEquals("Laporan_Santoya_Bulan_Ini_20261116.csv", fileNameCsv)
    }

    @Test
    fun `export csv and pdf include aging, desa ranking, product breakdown, and official letterhead`() {
        val now = epochMillis(2026, 11, 16, 10)
        val dayTx = epochMillis(2026, 11, 10, 9)
        val cust1 = sampleCustomer(id = 1, nama = "Warung Bu Siti", alamat = "Desa Karanganyar", sisa = 15_000.0)
        val cust2 = sampleCustomer(id = 2, nama = "Pak Joko", alamat = "Desa Sukamaju", sisa = 0.0)
        val kurir = sampleKurir(id = 1, nama = "Budi")
        val prod1 = sampleProduk(id = 1, nama = "Galon RO 19L", harga = 5000.0)
        val prod2 = sampleProduk(id = 2, nama = "Botol 600ml", harga = 3000.0)

        // Nota 1: Kasbon Rp 15.000 (usia 6 hari -> bucket 0-7 hari)
        val tx1 = Transaksi(
            id = 101,
            tanggal = dayTx,
            id_pelanggan = cust1.id,
            id_kurir = kurir.id,
            id_produk = prod1.id,
            jumlah = 3,
            harga_satuan = 5000.0,
            total_harga = 15_000.0,
            total_komisi = 3000.0,
            metode_bayar = "Cash",
            status = "Kasbon",
            uangMuka = 0L,
            sisaKasbon = 15_000L
        )
        // Nota 2: Lunas Tunai Rp 30.000
        val tx2 = Transaksi(
            id = 102,
            tanggal = dayTx,
            id_pelanggan = cust2.id,
            id_kurir = kurir.id,
            id_produk = prod2.id,
            jumlah = 10,
            harga_satuan = 3000.0,
            total_harga = 30_000.0,
            total_komisi = 5000.0,
            metode_bayar = "Cash",
            status = "Lunas",
            uangMuka = 30_000L,
            sisaKasbon = 0L
        )

        val txList = listOf(
            TransaksiDetail(tx1, cust1, kurir, prod1),
            TransaksiDetail(tx2, cust2, kurir, prod2)
        )
        val pelangganList = listOf(cust1, cust2)

        val aging = LaporanFinanceCalculator.calculateKasbonAging(
            allTransaksi = txList,
            allPelanggan = pelangganList,
            nowMillis = now,
            zoneId = zoneId
        )
        assertEquals(1, aging.bucket0To7Count)
        assertEquals(15_000.0, aging.bucket0To7Amount, 0.01)

        val summary = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = txList,
            allPengeluaran = emptyList(),
            allKasbonLogs = emptyList(),
            allPelanggan = pelangganList,
            startTime = epochMillis(2026, 11, 1, 0),
            endTime = epochMillis(2026, 11, 30, 23),
            zoneId = zoneId
        )

        val csv = ExportHelper.generateCsv(
            transaksiList = txList,
            pengeluaranList = emptyList(),
            kasbonLogList = emptyList(),
            pelangganList = pelangganList,
            startTime = epochMillis(2026, 11, 1, 0),
            endTime = epochMillis(2026, 11, 30, 23),
            periodeLabel = "Bulan Ini",
            kasbonAgingSummary = aging
        )

        assertTrue(csv.contains("=== ANALISIS UMUR PIUTANG KASBON ==="))
        assertTrue(csv.contains("Lancar (0-7 Hari),1,15000"))
        assertTrue(csv.contains("=== PERINGKAT PENJUALAN PER WILAYAH ==="))
        assertTrue(csv.contains("Sukamaju,10,1,30000"))
        assertTrue(csv.contains("Karanganyar,3,1,15000"))
        assertTrue(csv.contains("=== REKAP PENJUALAN PER PRODUK ==="))
        assertTrue(csv.contains("Galon RO 19L,3,1,15000"))
        assertTrue(csv.contains("Botol 600ml,10,1,30000"))

        val html = ExportHelper.generateLaporanHtml(
            summary = summary,
            allTransaksi = txList,
            allKasbonLogs = emptyList(),
            periodeLabel = "Bulan Ini",
            isSummaryMode = false,
            kasbonAgingSummary = aging
        )

        assertTrue(html.contains("<div class='kop-brand'>DEPOT AIR MINUM</div>"))
        assertTrue(html.contains("Kesehatan &amp; Umur Piutang Kasbon"))
        assertTrue(html.contains("Lancar (0–7 Hari)"))
        assertTrue(html.contains("Rekapitulasi Penjualan per Produk"))
        assertTrue(html.contains("Peringkat Penjualan per Wilayah"))
        assertTrue(html.contains("Admin / Kasir"))
        assertTrue(html.contains("Pemilik DEPOT AIR MINUM"))
    }
}
