package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SantoyaRepository(private val db: AppDatabase) {

    // --- Produk ---
    val allProduk: Flow<List<Produk>> = db.produkDao().getAll()
    suspend fun insertProduk(produk: Produk) = db.produkDao().insert(produk)
    suspend fun updateProduk(produk: Produk) = db.produkDao().update(produk)
    suspend fun deleteProduk(produk: Produk) {
        db.transaksiDao().clearProdukFromTransaksi(produk.id)
        db.produkDao().delete(produk)
    }
    suspend fun getProdukById(id: Int) = db.produkDao().getById(id)

    // --- Kurir ---
    val allKurir: Flow<List<Kurir>> = db.kurirDao().getAll()
    suspend fun insertKurir(kurir: Kurir) = db.kurirDao().insert(kurir)
    suspend fun updateKurir(kurir: Kurir) = db.kurirDao().update(kurir)
    suspend fun deleteKurir(kurir: Kurir) = db.kurirDao().delete(kurir)
    suspend fun getKurirById(id: Int) = db.kurirDao().getById(id)

    // --- Pelanggan ---
    val allPelanggan: Flow<List<Pelanggan>> = db.pelangganDao().getAll()
    val pelangganKasbon: Flow<List<Pelanggan>> = db.pelangganDao().getWithKasbon()
    suspend fun insertPelanggan(pelanggan: Pelanggan) = db.pelangganDao().insert(pelanggan)
    suspend fun updatePelanggan(pelanggan: Pelanggan) = db.pelangganDao().update(pelanggan)
    suspend fun deletePelanggan(pelanggan: Pelanggan) = db.pelangganDao().delete(pelanggan)
    suspend fun getPelangganById(id: Int) = db.pelangganDao().getById(id)

    // --- Pengeluaran ---
    val allPengeluaran: Flow<List<Pengeluaran>> = db.pengeluaranDao().getAll()
    fun getPengeluaranByRange(start: Long, end: Long) = db.pengeluaranDao().getByRange(start, end)
    suspend fun insertPengeluaran(pengeluaran: Pengeluaran) = db.pengeluaranDao().insert(pengeluaran)
    suspend fun updatePengeluaran(pengeluaran: Pengeluaran) = db.pengeluaranDao().update(pengeluaran)
    suspend fun deletePengeluaran(pengeluaran: Pengeluaran) = db.pengeluaranDao().delete(pengeluaran)

    // --- Transaksi ---
    val allTransaksiWithDetail: Flow<List<TransaksiDetail>> = db.transaksiDao().getAllWithDetail()
    fun getTransaksiByDateRange(start: Long, end: Long) = db.transaksiDao().getByDateRangeWithDetail(start, end)
    fun getTransaksiByPelanggan(pelangganId: Int) = db.transaksiDao().getByPelangganWithDetail(pelangganId)
    suspend fun getTransaksiDetailById(id: Int) = db.transaksiDao().getDetailById(id)

    suspend fun recordTransaksi(
        tanggal: Long,
        idPelanggan: Int,
        idKurir: Int?,
        idProduk: Int?,
        jumlah: Int,
        hargaSatuan: Double,
        metodeBayar: String,
        status: String
    ): Long {
        val totalHarga = jumlah * hargaSatuan
        val kurir = idKurir?.let { db.kurirDao().getById(it) }
        val totalKomisi = if (kurir != null) jumlah * kurir.komisi_per_galon else 0.0

        val transaksi = Transaksi(
            tanggal = tanggal,
            id_pelanggan = idPelanggan,
            id_kurir = idKurir,
            id_produk = idProduk,
            jumlah = jumlah,
            harga_satuan = hargaSatuan,
            total_harga = totalHarga,
            total_komisi = totalKomisi,
            metode_bayar = metodeBayar,
            status = status
        )
        val newId = db.transaksiDao().insert(transaksi)

        // If Kasbon, update Pelanggan sisa_kasbon
        if (status.equals("Kasbon", ignoreCase = true)) {
            db.pelangganDao().updateKasbon(idPelanggan, totalHarga)
        }
        return newId
    }

    suspend fun markTransaksiLunas(transaksiId: Int) {
        val detail = db.transaksiDao().getDetailById(transaksiId) ?: return
        if (detail.transaksi.status.equals("Kasbon", ignoreCase = true)) {
            db.transaksiDao().markAsLunas(transaksiId)
            // Kurangi sisa kasbon pelanggan
            val delta = -detail.transaksi.total_harga
            db.pelangganDao().updateKasbon(detail.transaksi.id_pelanggan, delta)
        }
    }

    suspend fun deleteTransaksi(transaksi: Transaksi) {
        // If Kasbon, subtract from customer kasbon
        if (transaksi.status.equals("Kasbon", ignoreCase = true)) {
            db.pelangganDao().updateKasbon(transaksi.id_pelanggan, -transaksi.total_harga)
        }
        db.transaksiDao().delete(transaksi)
    }

    suspend fun updateTransaksi(
        id: Int,
        tanggal: Long,
        idPelanggan: Int,
        idKurir: Int?,
        idProduk: Int?,
        jumlah: Int,
        hargaSatuan: Double,
        metodeBayar: String,
        status: String
    ) {
        val oldDetail = db.transaksiDao().getDetailById(id)
        if (oldDetail != null) {
            val old = oldDetail.transaksi
            if (old.status.equals("Kasbon", ignoreCase = true)) {
                db.pelangganDao().updateKasbon(old.id_pelanggan, -old.total_harga)
            }
        }

        val totalHarga = jumlah * hargaSatuan
        val kurir = idKurir?.let { db.kurirDao().getById(it) }
        val totalKomisi = if (kurir != null) jumlah * kurir.komisi_per_galon else 0.0

        val existing = db.transaksiDao().getDetailById(id)?.transaksi
        val productId = idProduk ?: existing?.id_produk ?: 1

        val updated = Transaksi(
            id = id,
            tanggal = tanggal,
            id_pelanggan = idPelanggan,
            id_kurir = idKurir,
            id_produk = productId,
            jumlah = jumlah,
            harga_satuan = hargaSatuan,
            total_harga = totalHarga,
            total_komisi = totalKomisi,
            metode_bayar = metodeBayar,
            status = status
        )
        db.transaksiDao().update(updated)

        if (status.equals("Kasbon", ignoreCase = true)) {
            db.pelangganDao().updateKasbon(idPelanggan, totalHarga)
        }
    }

    suspend fun bayarKasbonPelanggan(pelangganId: Int, nominalBayar: Double) {
        val pelanggan = db.pelangganDao().getById(pelangganId) ?: return
        val newBalance = (pelanggan.sisa_kasbon - nominalBayar).coerceAtLeast(0.0)
        db.pelangganDao().setKasbon(pelangganId, newBalance)

        // Update unpaid transactions if fully paid or partially paid
        val unpaidList = db.transaksiDao().getUnpaidByPelanggan(pelangganId)
        var remainingPayment = nominalBayar
        for (item in unpaidList) {
            if (remainingPayment >= item.total_harga) {
                db.transaksiDao().markAsLunas(item.id)
                remainingPayment -= item.total_harga
            } else {
                break
            }
        }
    }
}
