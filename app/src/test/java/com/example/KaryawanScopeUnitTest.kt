package com.example

import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.DepotRole
import com.example.util.KaryawanScopeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KaryawanScopeUnitTest {

    private fun sampleKurir(
        id: Int,
        nama: String,
        email: String,
        rate: Double = 1_000.0,
        aksesAktif: Boolean = true
    ) = Kurir(
        id = id,
        nama = nama,
        komisi_per_galon = rate,
        email = email,
        akses_aktif = aksesAktif
    )

    private fun samplePelanggan(
        id: Int,
        nama: String,
        idKurirLangganan: Int?
    ) = Pelanggan(
        id = id,
        nama = nama,
        alamat = "Jl. Santoya $id",
        no_hp = "0812000$id",
        id_kurir_langganan = idKurirLangganan
    )

    private fun sampleTx(
        id: Int,
        tanggal: Long,
        pelanggan: Pelanggan,
        kurir: Kurir?,
        jumlah: Int,
        hargaSatuan: Double = 5_000.0,
        status: String = "Lunas",
        uangMuka: Long = 0L
    ): TransaksiDetail {
        val total = hargaSatuan * jumlah
        val komisi = (kurir?.komisi_per_galon ?: 0.0) * jumlah
        return TransaksiDetail(
            transaksi = Transaksi(
                id = id,
                tanggal = tanggal,
                id_pelanggan = pelanggan.id,
                id_kurir = kurir?.id,
                id_produk = 1,
                jumlah = jumlah,
                harga_satuan = hargaSatuan,
                total_harga = total,
                total_komisi = komisi,
                metode_bayar = if (status == "Lunas") "Cash" else "Kasbon",
                status = status,
                uangMuka = uangMuka,
                sisaKasbon = if (status == "Kasbon") (total.toLong() - uangMuka).coerceAtLeast(0L) else 0L
            ),
            pelanggan = pelanggan,
            kurir = kurir,
            produk = Produk(id = 1, nama_produk = "Galon RO 19L", harga = hargaSatuan)
        )
    }

    @Test
    fun `normalizeEmail and resolveActiveKaryawan handle bare usernames and active preference`() {
        assertEquals("", KaryawanScopeHelper.normalizeEmail(null))
        assertEquals("", KaryawanScopeHelper.normalizeEmail("   "))
        assertEquals("fitri@gmail.com", KaryawanScopeHelper.normalizeEmail("  Fitri "))
        assertEquals("budi@santoya.id", KaryawanScopeHelper.normalizeEmail("BUDI@santoya.id "))

        val inactiveFitri = sampleKurir(id = 1, nama = "Fitri Lama", email = "fitri", aksesAktif = false)
        val activeFitri = sampleKurir(id = 2, nama = "Fitri Aktif", email = "Fitri@gmail.com", aksesAktif = true)
        val kurirBudi = sampleKurir(id = 3, nama = "Budi", email = "budi", aksesAktif = true)

        val resolved = KaryawanScopeHelper.resolveActiveKaryawan(
            userEmail = "fitri@gmail.com",
            kurirList = listOf(inactiveFitri, activeFitri, kurirBudi)
        )
        assertEquals(2, resolved?.id)

        val fallbackInactive = KaryawanScopeHelper.resolveActiveKaryawan(
            userEmail = "fitri@gmail.com",
            kurirList = listOf(inactiveFitri, kurirBudi)
        )
        assertEquals(1, fallbackInactive?.id)

        assertNull(KaryawanScopeHelper.resolveActiveKaryawan("owner@gmail.com", listOf(activeFitri, kurirBudi)))
    }

    @Test
    fun `isKaryawanRole detects STAFF sync role and active courier emails accurately`() {
        val activeFitri = sampleKurir(id = 1, nama = "Fitri", email = "fitri", aksesAktif = true)
        val inactiveRudi = sampleKurir(id = 2, nama = "Rudi", email = "rudi@gmail.com", aksesAktif = false)
        val couriers = listOf(activeFitri, inactiveRudi)

        assertTrue(KaryawanScopeHelper.isKaryawanRole(DepotRole.STAFF, "any@gmail.com", couriers))
        assertTrue(KaryawanScopeHelper.isKaryawanRole(DepotRole.OWNER, "FITRI@GMAIL.COM", couriers))
        assertFalse(KaryawanScopeHelper.isKaryawanRole(DepotRole.OWNER, "rudi@gmail.com", couriers))
        assertFalse(KaryawanScopeHelper.isKaryawanRole(DepotRole.OWNER, "pemilik@gmail.com", couriers))
    }

    @Test
    fun `filterPelangganForRole isolates customers per Karyawan and gives Owner full list`() {
        val fitri = sampleKurir(id = 1, nama = "Fitri", email = "fitri")
        val p1 = samplePelanggan(1, "Warung A", idKurirLangganan = 1)
        val p2 = samplePelanggan(2, "Warung B", idKurirLangganan = 2)
        val p3 = samplePelanggan(3, "Warung Umum", idKurirLangganan = null)
        val allCustomers = listOf(p1, p2, p3)

        assertEquals(listOf(p1), KaryawanScopeHelper.filterPelangganForRole(allCustomers, isKaryawan = true, activeKurir = fitri))
        assertEquals(emptyList<Pelanggan>(), KaryawanScopeHelper.filterPelangganForRole(allCustomers, isKaryawan = true, activeKurir = null))
        assertEquals(allCustomers, KaryawanScopeHelper.filterPelangganForRole(allCustomers, isKaryawan = false, activeKurir = null))
    }

    @Test
    fun `filterTransaksiForRole and filterCustomerHistoryForRole enforce courier isolation`() {
        val fitri = sampleKurir(id = 1, nama = "Fitri", email = "fitri")
        val budi = sampleKurir(id = 2, nama = "Budi", email = "budi")
        val pFitri = samplePelanggan(10, "Bu Ani", idKurirLangganan = 1)
        val pBudi = samplePelanggan(20, "Pak Joko", idKurirLangganan = 2)
        val now = System.currentTimeMillis()

        val txFitriDelivered = sampleTx(1, now, pFitri, fitri, jumlah = 4)
        val txBudiDeliveredToFitriCust = sampleTx(2, now, pFitri, budi, jumlah = 2)
        val txBudiDeliveredToBudiCust = sampleTx(3, now, pBudi, budi, jumlah = 5)
        val allTxs = listOf(txFitriDelivered, txBudiDeliveredToFitriCust, txBudiDeliveredToBudiCust)

        // Direct transactions for Karyawan Fitri: only delivered by Fitri
        val fitriTxs = KaryawanScopeHelper.filterTransaksiForRole(allTxs, isKaryawan = true, activeKurir = fitri)
        assertEquals(listOf(txFitriDelivered), fitriTxs)

        // Customer history for Karyawan Fitri includes deliveries by Fitri + history of Fitri's owned customers
        val fitriCustomerHistory = KaryawanScopeHelper.filterCustomerHistoryForRole(
            transaksiList = allTxs,
            isKaryawan = true,
            activeKurir = fitri,
            ownedPelangganIds = setOf(pFitri.id)
        )
        assertEquals(listOf(txFitriDelivered, txBudiDeliveredToFitriCust), fitriCustomerHistory)

        // Owner sees everything
        assertEquals(allTxs, KaryawanScopeHelper.filterTransaksiForRole(allTxs, isKaryawan = false, activeKurir = null))
    }

    @Test
    fun `summarizeKaryawanDashboardMetrics calculates today cash handover plus today and month gallons and commissions`() {
        val fitri = sampleKurir(id = 1, nama = "Fitri", email = "fitri", rate = 1_000.0)
        val budi = sampleKurir(id = 2, nama = "Budi", email = "budi", rate = 1_500.0)
        val pFitri = samplePelanggan(10, "Bu Ani", idKurirLangganan = 1)

        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 15, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = nowCal.timeInMillis

        val earlierThisMonth = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 3, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val lastMonth = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 28, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val txs = listOf(
            // Today, Fitri, Lunas: 5 galon * 5000 = 25000 cash, 5000 komisi
            sampleTx(1, nowMillis, pFitri, fitri, jumlah = 5, hargaSatuan = 5_000.0, status = "Lunas"),
            // Today, Fitri, Kasbon w/ DP 10000: 4 galon, 10000 cash handover, 4000 komisi
            sampleTx(2, nowMillis - 3_600_000L, pFitri, fitri, jumlah = 4, hargaSatuan = 5_000.0, status = "Kasbon", uangMuka = 10_000L),
            // Earlier this month, Fitri, Lunas: 6 galon, 6000 komisi (not today)
            sampleTx(3, earlierThisMonth, pFitri, fitri, jumlah = 6, hargaSatuan = 5_000.0, status = "Lunas"),
            // Last month, Fitri, Lunas: 10 galon (ignored)
            sampleTx(4, lastMonth, pFitri, fitri, jumlah = 10, hargaSatuan = 5_000.0, status = "Lunas"),
            // Today, Budi (other courier - ignored)
            sampleTx(5, nowMillis, pFitri, budi, jumlah = 8, hargaSatuan = 5_000.0, status = "Lunas")
        )

        val metrics = KaryawanScopeHelper.summarizeKaryawanDashboardMetrics(
            transaksiList = txs,
            activeKurir = fitri,
            nowMillis = nowMillis
        )

        assertEquals(35_000.0, metrics.setoranTunaiHariIni, 0.01)
        assertEquals(2, metrics.pesananHariIni)
        assertEquals(9, metrics.galonHariIni)
        assertEquals(9_000.0, metrics.komisiHariIni, 0.01)
        assertEquals(3, metrics.pesananBulanIni)
        assertEquals(15, metrics.galonBulanIni)
        assertEquals(15_000.0, metrics.komisiBulanIni, 0.01)
    }

    @Test
    fun `soft delete transaksi metadata encodes employee name within 40 chars and restores payment states accurately`() {
        val cashTx = Transaksi(
            id = 1,
            tanggal = 1_000L,
            id_pelanggan = 1,
            id_kurir = 1,
            id_produk = 1,
            jumlah = 3,
            harga_satuan = 6_000.0,
            total_harga = 18_000.0,
            total_komisi = 3_000.0,
            metode_bayar = "Cash",
            status = "Lunas"
        )
        val transferTx = cashTx.copy(metode_bayar = "Transfer")
        val fullKasbonTx = cashTx.copy(metode_bayar = "-", status = "Kasbon", sisaKasbon = 18_000L)
        val dpKasbonTx = cashTx.copy(
            metode_bayar = "Bayar Sebagian",
            status = "Kasbon",
            uangMuka = 8_000L,
            sisaKasbon = 10_000L
        )

        val encodedCash = KaryawanScopeHelper.encodeDeletedTransaksiMetadata(cashTx, "Fitri Rahmawati Depot Santoya Panjang")
        assertTrue(encodedCash.length <= 40)
        val deletedCash = cashTx.copy(status = KaryawanScopeHelper.STATUS_DIHAPUS, metode_bayar = encodedCash)
        assertTrue(KaryawanScopeHelper.isDeletedTransaksi(deletedCash))
        assertTrue(KaryawanScopeHelper.extractDeletedByTransaksi(deletedCash)?.startsWith("Fitri") == true)
        val restoredCash = KaryawanScopeHelper.decodeRestoredTransaksiPayment(deletedCash)
        assertEquals("Lunas", restoredCash.status)
        assertEquals("Cash", restoredCash.metodeBayar)
        assertEquals(0L, restoredCash.uangMuka)
        assertEquals(0L, restoredCash.sisaKasbon)

        val encodedTransfer = KaryawanScopeHelper.encodeDeletedTransaksiMetadata(transferTx, "Budi")
        val restoredTransfer = KaryawanScopeHelper.decodeRestoredTransaksiPayment(
            transferTx.copy(status = KaryawanScopeHelper.STATUS_DIHAPUS, metode_bayar = encodedTransfer)
        )
        assertEquals("Lunas", restoredTransfer.status)
        assertEquals("Transfer", restoredTransfer.metodeBayar)

        val encodedFullKasbon = KaryawanScopeHelper.encodeDeletedTransaksiMetadata(fullKasbonTx, "Budi")
        val restoredFullKasbon = KaryawanScopeHelper.decodeRestoredTransaksiPayment(
            fullKasbonTx.copy(status = KaryawanScopeHelper.STATUS_DIHAPUS, metode_bayar = encodedFullKasbon)
        )
        assertEquals("Kasbon", restoredFullKasbon.status)
        assertEquals("-", restoredFullKasbon.metodeBayar)
        assertEquals(0L, restoredFullKasbon.uangMuka)
        assertEquals(18_000L, restoredFullKasbon.sisaKasbon)

        val encodedDp = KaryawanScopeHelper.encodeDeletedTransaksiMetadata(dpKasbonTx, "Budi")
        assertTrue(encodedDp.length <= 40)
        val restoredDp = KaryawanScopeHelper.decodeRestoredTransaksiPayment(
            dpKasbonTx.copy(status = KaryawanScopeHelper.STATUS_DIHAPUS, metode_bayar = encodedDp)
        )
        assertEquals("Kasbon", restoredDp.status)
        assertEquals("Bayar Sebagian", restoredDp.metodeBayar)
        assertEquals(8_000L, restoredDp.uangMuka)
        assertEquals(10_000L, restoredDp.sisaKasbon)
    }

    @Test
    fun `soft delete pelanggan helpers encode clean restore alamat and exclude from karyawan scope and metrics`() {
        val fitri = sampleKurir(id = 1, nama = "Fitri", email = "fitri", rate = 1_000.0)
        val activePelanggan = samplePelanggan(10, "Warung Maju", idKurirLangganan = 1)
        val softDeletedAlamat = KaryawanScopeHelper.formatDeletedPelangganAlamat(activePelanggan.alamat, "Fitri")
        assertTrue(softDeletedAlamat.length <= 300)
        val deletedPelanggan = activePelanggan.copy(id = 11, nama = "Warung Tutup", alamat = softDeletedAlamat)

        assertFalse(KaryawanScopeHelper.isSoftDeletedPelanggan(activePelanggan))
        assertTrue(KaryawanScopeHelper.isSoftDeletedPelanggan(deletedPelanggan))
        assertEquals("Fitri", KaryawanScopeHelper.extractDeletedByPelanggan(deletedPelanggan))
        assertEquals(activePelanggan.alamat, KaryawanScopeHelper.cleanPelangganAlamat(deletedPelanggan.alamat))

        // Karyawan sees only active pelanggan; Owner still sees deleted pelanggan for audit
        val karyawanCustomers = KaryawanScopeHelper.filterPelangganForRole(
            pelangganList = listOf(activePelanggan, deletedPelanggan),
            isKaryawan = true,
            activeKurir = fitri
        )
        assertEquals(listOf(activePelanggan), karyawanCustomers)
        val ownerCustomers = KaryawanScopeHelper.filterPelangganForRole(
            pelangganList = listOf(activePelanggan, deletedPelanggan),
            isKaryawan = false,
            activeKurir = null
        )
        assertEquals(2, ownerCustomers.size)

        // Soft-deleted transactions are excluded from Karyawan list, customer history, and dashboard metrics
        val now = System.currentTimeMillis()
        val activeTx = sampleTx(1, now, activePelanggan, fitri, jumlah = 3, hargaSatuan = 5_000.0)
        val deletedTxRaw = sampleTx(2, now, activePelanggan, fitri, jumlah = 10, hargaSatuan = 5_000.0)
        val deletedTx = deletedTxRaw.copy(
            transaksi = deletedTxRaw.transaksi.copy(
                status = KaryawanScopeHelper.STATUS_DIHAPUS,
                metode_bayar = KaryawanScopeHelper.encodeDeletedTransaksiMetadata(deletedTxRaw.transaksi, "Fitri"),
                total_komisi = 0.0
            )
        )
        assertEquals(
            listOf(activeTx),
            KaryawanScopeHelper.filterTransaksiForRole(listOf(activeTx, deletedTx), isKaryawan = true, activeKurir = fitri)
        )
        assertEquals(
            listOf(activeTx),
            KaryawanScopeHelper.filterCustomerHistoryForRole(
                transaksiList = listOf(activeTx, deletedTx),
                isKaryawan = true,
                activeKurir = fitri,
                ownedPelangganIds = setOf(activePelanggan.id)
            )
        )
        val metrics = KaryawanScopeHelper.summarizeKaryawanDashboardMetrics(listOf(activeTx, deletedTx), fitri, now)
        assertEquals(15_000.0, metrics.setoranTunaiHariIni, 0.01)
        assertEquals(1, metrics.pesananHariIni)
        assertEquals(3, metrics.galonHariIni)
        assertEquals(3_000.0, metrics.komisiHariIni, 0.01)
    }

    @Test
    fun `resolveAuthoritativeUnitPrice enforces custom owner price or product price`() {
        val produk = Produk(id = 1, nama_produk = "Air RO 19L", harga = 6_000.0)
        val regularCust = samplePelanggan(1, "Reguler", idKurirLangganan = 1)
        val customCust = regularCust.copy(harga_khusus = 5_000.0)
        val zeroCustomCust = regularCust.copy(harga_khusus = 0.0)

        assertEquals(6_000.0, KaryawanScopeHelper.resolveAuthoritativeUnitPrice(regularCust, produk), 0.01)
        assertEquals(5_000.0, KaryawanScopeHelper.resolveAuthoritativeUnitPrice(customCust, produk), 0.01)
        assertEquals(6_000.0, KaryawanScopeHelper.resolveAuthoritativeUnitPrice(zeroCustomCust, produk), 0.01)
        assertEquals(7_000.0, KaryawanScopeHelper.resolveAuthoritativeUnitPrice(null, null, fallbackPrice = 7_000.0), 0.01)
    }
}
