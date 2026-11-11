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
    val allKasbonTransaksiWithDetail: Flow<List<TransaksiDetail>> = db.transaksiDao().getAllKasbonWithDetail()
    fun getTransaksiByDateRange(start: Long, end: Long) = db.transaksiDao().getByDateRangeWithDetail(start, end)
    fun getTotalPendapatanByDateRange(start: Long, end: Long) = db.transaksiDao().getTotalPendapatanByDateRange(start, end)
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
        status: String,
        uangMuka: Long = 0L,
        sisaKasbon: Long = 0L
    ): Long {
        val totalHarga = jumlah * hargaSatuan
        val kurir = idKurir?.let { db.kurirDao().getById(it) }
        val totalKomisi = if (kurir != null) jumlah * kurir.komisi_per_galon else 0.0
        val isKasbon = status.equals("Kasbon", ignoreCase = true)

        val finalUangMuka = if (isKasbon) uangMuka.coerceAtLeast(0L) else 0L
        val finalSisaKasbon = if (isKasbon) {
            if (sisaKasbon > 0L || finalUangMuka > 0L) {
                sisaKasbon.coerceAtLeast(0L)
            } else {
                totalHarga.toLong().coerceAtLeast(0L)
            }
        } else {
            0L
        }

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
            status = status,
            uangMuka = finalUangMuka,
            sisaKasbon = finalSisaKasbon
        )
        val newId = db.transaksiDao().insert(transaksi)

        // If Kasbon, update Pelanggan sisa_kasbon using finalSisaKasbon (not full totalHarga when DP is paid)
        if (isKasbon && finalSisaKasbon > 0L) {
            db.pelangganDao().updateKasbon(idPelanggan, finalSisaKasbon.toDouble())
        }
        return newId
    }

    suspend fun markTransaksiLunas(transaksiId: Int) {
        val detail = db.transaksiDao().getDetailById(transaksiId) ?: return
        if (detail.transaksi.status.equals("Kasbon", ignoreCase = true)) {
            val remainingDebt = detail.transaksi.remainingKasbonAmount()
            db.transaksiDao().markAsLunas(transaksiId)
            // Kurangi sisa kasbon pelanggan sebesar sisaKasbon aktif pada nota tersebut
            val delta = -remainingDebt
            db.pelangganDao().updateKasbon(detail.transaksi.id_pelanggan, delta)
        }
    }

    suspend fun deleteTransaksi(transaksi: Transaksi) {
        // If Kasbon, subtract remaining kasbon from customer kasbon
        if (transaksi.status.equals("Kasbon", ignoreCase = true)) {
            db.pelangganDao().updateKasbon(transaksi.id_pelanggan, -transaksi.remainingKasbonAmount())
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
        status: String,
        uangMuka: Long = 0L
    ) {
        val oldDetail = db.transaksiDao().getDetailById(id)
        if (oldDetail != null) {
            val old = oldDetail.transaksi
            if (old.status.equals("Kasbon", ignoreCase = true)) {
                db.pelangganDao().updateKasbon(old.id_pelanggan, -old.remainingKasbonAmount())
            }
        }

        val totalHarga = jumlah * hargaSatuan
        val kurir = idKurir?.let { db.kurirDao().getById(it) }
        val totalKomisi = if (kurir != null) jumlah * kurir.komisi_per_galon else 0.0

        val existing = oldDetail?.transaksi
        val productId = idProduk ?: existing?.id_produk ?: 1
        val isKasbon = status.equals("Kasbon", ignoreCase = true)
        val isBayarSebagian = metodeBayar.equals("Bayar Sebagian", ignoreCase = true)

        val finalUangMuka = if (isKasbon && isBayarSebagian) {
            uangMuka.coerceIn(0L, totalHarga.toLong())
        } else {
            0L
        }
        val finalSisaKasbon = if (isKasbon) {
            (totalHarga.toLong() - finalUangMuka).coerceAtLeast(0L)
        } else {
            0L
        }

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
            status = status,
            uangMuka = finalUangMuka,
            sisaKasbon = finalSisaKasbon
        )
        db.transaksiDao().update(updated)

        if (isKasbon && finalSisaKasbon > 0L) {
            db.pelangganDao().updateKasbon(idPelanggan, finalSisaKasbon.toDouble())
        }
    }

    suspend fun bayarKasbonPelanggan(pelangganId: Int, nominalBayar: Double, metodeBayar: String = "Cash") {
        val pelanggan = db.pelangganDao().getById(pelangganId) ?: return
        val unpaidList = db.transaksiDao().getUnpaidByPelanggan(pelangganId)
        val currentTotalKasbon = if (unpaidList.isNotEmpty()) {
            unpaidList.sumOf { it.remainingKasbonAmount() }
        } else {
            pelanggan.sisa_kasbon
        }
        val newBalance = (currentTotalKasbon - nominalBayar).coerceAtLeast(0.0)
        db.pelangganDao().setKasbon(pelangganId, newBalance)

        // Kurangi sisaKasbon dari nota terlama ke terbaru tanpa merusak total_harga asli nota
        var remainingPayment = nominalBayar.toLong()
        for (item in unpaidList) {
            if (remainingPayment <= 0L) break
            val itemSisa = item.remainingKasbonAmount().toLong()
            if (remainingPayment >= itemSisa) {
                val paidTrx = item.copy(
                    status = "Lunas",
                    uangMuka = item.total_harga.toLong(),
                    sisaKasbon = 0L,
                    metode_bayar = if (item.metode_bayar.isBlank() || item.metode_bayar == "-") metodeBayar else item.metode_bayar
                )
                db.transaksiDao().update(paidTrx)
                remainingPayment -= itemSisa
            } else {
                val newSisaKasbon = (itemSisa - remainingPayment).coerceAtLeast(0L)
                val newUangMuka = (item.total_harga.toLong() - newSisaKasbon).coerceAtLeast(0L)
                val updatedTrx = item.copy(
                    uangMuka = newUangMuka,
                    sisaKasbon = newSisaKasbon,
                    metode_bayar = if (item.metode_bayar.isBlank() || item.metode_bayar == "-") "Bayar Sebagian" else item.metode_bayar
                )
                db.transaksiDao().update(updatedTrx)
                remainingPayment = 0L
                break
            }
        }
    }


}


