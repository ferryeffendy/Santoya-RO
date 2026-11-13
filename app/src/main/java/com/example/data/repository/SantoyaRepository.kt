package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.entity.KasbonLog
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.PendingSyncEntity
import com.example.data.entity.Pengeluaran
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.KaryawanScopeHelper
import com.example.util.KomisiCalculator
import com.example.util.SyncCollections
import com.example.util.SyncOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SantoyaRepository(
    private val db: AppDatabase,
    private val context: Context? = null,
    var onLocalMutation: (() -> Unit)? = null
) {
    private suspend fun enqueueUpsert(collection: String, syncId: String, updatedAt: Long) {
        db.pendingSyncDao().enqueue(
            PendingSyncEntity(
                collectionName = collection,
                syncId = syncId,
                operation = SyncOperations.UPSERT,
                updatedAt = updatedAt
            )
        )
    }

    private suspend fun enqueueDelete(collection: String, syncId: String, updatedAt: Long) {
        db.pendingSyncDao().enqueue(
            PendingSyncEntity(
                collectionName = collection,
                syncId = syncId,
                operation = SyncOperations.DELETE,
                updatedAt = updatedAt
            )
        )
    }

    // --- Produk ---
    val allProduk: Flow<List<Produk>> = db.produkDao().getAll()

    suspend fun insertProduk(produk: Produk): Long {
        val now = System.currentTimeMillis()
        val entity = produk.copy(updatedAt = now)
        val id = db.withTransaction {
            val insertedId = db.produkDao().insert(entity)
            enqueueUpsert(SyncCollections.PRODUK, entity.syncId, now)
            insertedId
        }
        onLocalMutation?.invoke()
        return id
    }

    suspend fun updateProduk(produk: Produk) {
        val now = System.currentTimeMillis()
        val entity = produk.copy(updatedAt = now)
        db.withTransaction {
            db.produkDao().update(entity)
            enqueueUpsert(SyncCollections.PRODUK, entity.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    suspend fun deleteProduk(produk: Produk) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val affectedTx = db.transaksiDao().getByProdukIdOnce(produk.id)
            db.transaksiDao().clearProdukFromTransaksi(produk.id, now)
            affectedTx.forEach { tx ->
                enqueueUpsert(SyncCollections.TRANSAKSI, tx.syncId, now)
            }
            db.produkDao().delete(produk)
            enqueueDelete(SyncCollections.PRODUK, produk.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    suspend fun getProdukById(id: Int): Produk? = db.produkDao().getById(id)

    // --- Kurir ---
    val allKurir: Flow<List<Kurir>> = db.kurirDao().getAll()

    suspend fun insertKurir(kurir: Kurir): Long {
        val now = System.currentTimeMillis()
        val entity = kurir.copy(updatedAt = now)
        val id = db.withTransaction {
            val insertedId = db.kurirDao().insert(entity)
            enqueueUpsert(SyncCollections.KURIR, entity.syncId, now)
            insertedId
        }
        onLocalMutation?.invoke()
        return id
    }

    suspend fun updateKurir(kurir: Kurir) {
        val now = System.currentTimeMillis()
        val entity = kurir.copy(updatedAt = now)
        db.withTransaction {
            db.kurirDao().update(entity)
            enqueueUpsert(SyncCollections.KURIR, entity.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    suspend fun deleteKurir(kurir: Kurir) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val affectedTx = db.transaksiDao().getByKurirIdOnce(kurir.id)
            db.transaksiDao().clearKurirFromTransaksi(kurir.id, now)
            affectedTx.forEach { tx ->
                enqueueUpsert(SyncCollections.TRANSAKSI, tx.syncId, now)
            }
            db.kurirDao().delete(kurir)
            enqueueDelete(SyncCollections.KURIR, kurir.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    suspend fun getKurirById(id: Int): Kurir? = db.kurirDao().getById(id)

    // --- Pelanggan ---
    val allPelanggan: Flow<List<Pelanggan>> = db.pelangganDao().getAll()
    val pelangganKasbon: Flow<List<Pelanggan>> = db.pelangganDao().getWithKasbon()

    suspend fun insertPelanggan(pelanggan: Pelanggan): Long {
        val now = System.currentTimeMillis()
        val entity = pelanggan.copy(updatedAt = now)
        val id = db.withTransaction {
            val insertedId = db.pelangganDao().insert(entity)
            enqueueUpsert(SyncCollections.PELANGGAN, entity.syncId, now)
            insertedId
        }
        onLocalMutation?.invoke()
        return id
    }

    suspend fun updatePelanggan(pelanggan: Pelanggan) {
        val now = System.currentTimeMillis()
        val entity = pelanggan.copy(updatedAt = now)
        db.withTransaction {
            db.pelangganDao().update(entity)
            enqueueUpsert(SyncCollections.PELANGGAN, entity.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    suspend fun deletePelanggan(pelanggan: Pelanggan) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            db.pelangganDao().delete(pelanggan)
            enqueueDelete(SyncCollections.PELANGGAN, pelanggan.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    suspend fun softDeletePelangganByKaryawan(pelanggan: Pelanggan, karyawanName: String): Boolean {
        val now = System.currentTimeMillis()
        var softDeleted = false
        db.withTransaction {
            val latest = db.pelangganDao().getById(pelanggan.id) ?: pelanggan
            val unpaidTxs = db.transaksiDao().getUnpaidByPelanggan(latest.id)
            if (latest.sisa_kasbon > 0.0 || unpaidTxs.isNotEmpty()) {
                return@withTransaction
            }
            if (KaryawanScopeHelper.isSoftDeletedPelanggan(latest)) {
                softDeleted = true
                return@withTransaction
            }
            val updatedAlamat = KaryawanScopeHelper.formatDeletedPelangganAlamat(latest.alamat, karyawanName)
            val updated = latest.copy(alamat = updatedAlamat, updatedAt = now)
            db.pelangganDao().update(updated)
            enqueueUpsert(SyncCollections.PELANGGAN, updated.syncId, now)
            softDeleted = true
        }
        if (softDeleted) onLocalMutation?.invoke()
        return softDeleted
    }

    suspend fun restorePelanggan(pelanggan: Pelanggan) {
        val now = System.currentTimeMillis()
        var restored = false
        db.withTransaction {
            val latest = db.pelangganDao().getById(pelanggan.id) ?: pelanggan
            if (!KaryawanScopeHelper.isSoftDeletedPelanggan(latest)) return@withTransaction
            val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(latest.alamat)
            val updated = latest.copy(alamat = cleanAlamat, updatedAt = now)
            db.pelangganDao().update(updated)
            enqueueUpsert(SyncCollections.PELANGGAN, updated.syncId, now)
            restored = true
        }
        if (restored) onLocalMutation?.invoke()
    }

    suspend fun getPelangganById(id: Int): Pelanggan? = db.pelangganDao().getById(id)

    private suspend fun allocatePaymentToUnpaidNotesFifo(
        unpaidNotes: List<Transaksi>,
        amountToSettle: Long,
        metodeBayar: String,
        now: Long
    ): List<Int> {
        if (amountToSettle <= 0L || unpaidNotes.isEmpty()) return emptyList()
        var remainingPayment = amountToSettle
        val cleanMetode = metodeBayar.trim().ifBlank { "Cash" }
        val affectedNoteIds = mutableListOf<Int>()

        val sortedNotes = unpaidNotes
            .filter { !KaryawanScopeHelper.isDeletedTransaksi(it) }
            .sortedWith(compareBy<Transaksi> { it.tanggal }.thenBy { it.id })

        for (tx in sortedNotes) {
            val noteRemaining = tx.remainingKasbonAmount().toLong().coerceAtLeast(0L)
            if (noteRemaining <= 0L) {
                val fixedTx = tx.copy(
                    status = "Lunas",
                    metode_bayar = tx.metode_bayar.takeIf { it.isNotBlank() && it != "-" } ?: cleanMetode,
                    uangMuka = tx.total_harga.toLong().coerceAtLeast(0L),
                    sisaKasbon = 0L,
                    updatedAt = now
                )
                db.transaksiDao().update(fixedTx)
                enqueueUpsert(SyncCollections.TRANSAKSI, fixedTx.syncId, now)
                affectedNoteIds.add(tx.id)
                continue
            }

            if (remainingPayment <= 0L) break

            if (remainingPayment >= noteRemaining) {
                remainingPayment -= noteRemaining
                val settledTx = tx.copy(
                    status = "Lunas",
                    metode_bayar = cleanMetode,
                    uangMuka = tx.total_harga.toLong().coerceAtLeast(0L),
                    sisaKasbon = 0L,
                    updatedAt = now
                )
                db.transaksiDao().update(settledTx)
                enqueueUpsert(SyncCollections.TRANSAKSI, settledTx.syncId, now)
                affectedNoteIds.add(tx.id)
            } else {
                val newNoteSisa = (noteRemaining - remainingPayment).coerceAtLeast(0L)
                val newNoteDp = (tx.total_harga.toLong() - newNoteSisa).coerceAtLeast(0L)
                remainingPayment = 0L
                val partialTx = tx.copy(
                    status = "Kasbon",
                    metode_bayar = "Bayar Sebagian",
                    uangMuka = newNoteDp,
                    sisaKasbon = newNoteSisa,
                    updatedAt = now
                )
                db.transaksiDao().update(partialTx)
                enqueueUpsert(SyncCollections.TRANSAKSI, partialTx.syncId, now)
                affectedNoteIds.add(tx.id)
            }
        }
        return affectedNoteIds
    }

    suspend fun reconcileKasbonDiscrepancies(): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        var reconciledCustomers = 0
        db.withTransaction {
            val allCustomers = db.pelangganDao().getAllOnce()
            val allUnpaidNotes = db.transaksiDao().getAllUnpaidOnce()
                .filter { !KaryawanScopeHelper.isDeletedTransaksi(it) }
            if (allUnpaidNotes.isEmpty()) return@withTransaction

            val unpaidByCustomer = allUnpaidNotes.groupBy { it.id_pelanggan }
            val allLogsByCustomer = db.kasbonLogDao().getAllOnce().groupBy { it.id_pelanggan }

            for (pelanggan in allCustomers) {
                val custUnpaid = unpaidByCustomer[pelanggan.id] ?: continue
                val unpaidTotal = custUnpaid.sumOf { it.remainingKasbonAmount() }
                val targetKasbon = pelanggan.sisa_kasbon.coerceAtLeast(0.0)

                if (unpaidTotal > targetKasbon) {
                    val alreadyPaidAmount = (unpaidTotal - targetKasbon).toLong().coerceAtLeast(0L)
                    val latestPaymentMethod = allLogsByCustomer[pelanggan.id]
                        ?.filter { it.jumlah_bayar > 0.0 }
                        ?.maxByOrNull { it.tanggal }
                        ?.metode_bayar
                        ?.takeIf { it.isNotBlank() && !it.equals("Kasbon", ignoreCase = true) }
                        ?: "Cash"

                    val settledIds = allocatePaymentToUnpaidNotesFifo(
                        unpaidNotes = custUnpaid,
                        amountToSettle = alreadyPaidAmount.coerceAtLeast(1L),
                        metodeBayar = latestPaymentMethod,
                        now = now
                    )
                    if (settledIds.isNotEmpty()) {
                        reconciledCustomers++
                    }
                }
            }
        }
        if (reconciledCustomers > 0) {
            onLocalMutation?.invoke()
        }
        reconciledCustomers
    }

    suspend fun bayarKasbonPelanggan(pelangganId: Int, nominalBayar: Double, metodeBayar: String) {
        if (nominalBayar <= 0.0) return
        val now = System.currentTimeMillis()
        val cleanMetode = metodeBayar.trim().ifBlank { "Cash" }
        var changed = false
        db.withTransaction {
            val pelanggan = db.pelangganDao().getById(pelangganId) ?: return@withTransaction
            val unpaidNotes = db.transaksiDao().getUnpaidByPelanggan(pelangganId)
                .filter { !KaryawanScopeHelper.isDeletedTransaksi(it) }
            val unpaidTotal = unpaidNotes.sumOf { it.remainingKasbonAmount() }

            val currentKasbon = if (pelanggan.sisa_kasbon > 0.0) {
                pelanggan.sisa_kasbon
            } else {
                unpaidTotal.coerceAtLeast(0.0)
            }
            val newKasbon = (currentKasbon - nominalBayar).coerceAtLeast(0.0)
            val priorDiscrepancy = (unpaidTotal - currentKasbon).toLong().coerceAtLeast(0L)
            val totalToAllocateOnNotes = priorDiscrepancy + nominalBayar.toLong().coerceAtLeast(0L)
            val affectedNoteIds = allocatePaymentToUnpaidNotesFifo(
                unpaidNotes = unpaidNotes,
                amountToSettle = totalToAllocateOnNotes,
                metodeBayar = cleanMetode,
                now = now
            )

            val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
            db.pelangganDao().update(updatedPelanggan)
            enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)

            val noteSummary = if (affectedNoteIds.isNotEmpty()) {
                " • Nota " + affectedNoteIds.joinToString(", ") { "#$it" }
            } else {
                ""
            }
            val log = KasbonLog(
                id_pelanggan = pelangganId,
                tanggal = now,
                jumlah_bayar = nominalBayar,
                sisa_kasbon_sebelum = currentKasbon,
                sisa_kasbon_sesudah = newKasbon,
                metode_bayar = cleanMetode,
                catatan = "Pelunasan Kasbon ($cleanMetode)$noteSummary",
                updatedAt = now
            )
            db.kasbonLogDao().insert(log)
            enqueueUpsert(SyncCollections.KASBON_LOGS, log.syncId, now)
            changed = true
        }
        if (changed) onLocalMutation?.invoke()
    }

    // --- Pengeluaran ---
    val allPengeluaran: Flow<List<Pengeluaran>> = db.pengeluaranDao().getAll()

    suspend fun insertPengeluaran(pengeluaran: Pengeluaran): Long {
        val now = System.currentTimeMillis()
        val entity = pengeluaran.copy(updatedAt = now)
        val id = db.withTransaction {
            val insertedId = db.pengeluaranDao().insert(entity)
            enqueueUpsert(SyncCollections.PENGELUARAN, entity.syncId, now)
            insertedId
        }
        onLocalMutation?.invoke()
        return id
    }

    suspend fun updatePengeluaran(pengeluaran: Pengeluaran) {
        val now = System.currentTimeMillis()
        val entity = pengeluaran.copy(updatedAt = now)
        db.withTransaction {
            db.pengeluaranDao().update(entity)
            enqueueUpsert(SyncCollections.PENGELUARAN, entity.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    suspend fun deletePengeluaran(pengeluaran: Pengeluaran) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            db.pengeluaranDao().delete(pengeluaran)
            enqueueDelete(SyncCollections.PENGELUARAN, pengeluaran.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    // --- Transaksi ---
    val allTransaksiWithDetail: Flow<List<TransaksiDetail>> = db.transaksiDao().getAllWithDetail()
    val allKasbonTransaksiWithDetail: Flow<List<TransaksiDetail>> = db.transaksiDao().getAllKasbonWithDetail()

    suspend fun recordTransaksi(
        tanggal: Long,
        idPelanggan: Int,
        idKurir: Int?,
        idProduk: Int?,
        jumlah: Int,
        hargaSatuan: Double,
        metodeBayar: String,
        status: String,
        uangMuka: Long,
        sisaKasbon: Long
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val totalBelanja = (jumlah * hargaSatuan)
        val kurir = idKurir?.let { db.kurirDao().getById(it) }
        val breakdown = KomisiCalculator.calculateTransactionCommission(kurir, jumlah)
        val totalKomisi = breakdown.totalKomisi

        val txId = db.withTransaction {
            val transaksi = Transaksi(
                tanggal = tanggal,
                id_pelanggan = idPelanggan,
                id_kurir = idKurir,
                id_produk = idProduk,
                jumlah = jumlah,
                harga_satuan = hargaSatuan,
                total_harga = totalBelanja,
                total_komisi = totalKomisi,
                metode_bayar = metodeBayar,
                status = status,
                uangMuka = uangMuka,
                sisaKasbon = sisaKasbon,
                updatedAt = now
            )
            val insertedTxId = db.transaksiDao().insert(transaksi)
            enqueueUpsert(SyncCollections.TRANSAKSI, transaksi.syncId, now)

            if (status.equals("Kasbon", ignoreCase = true) && sisaKasbon > 0) {
                val pelanggan = db.pelangganDao().getById(idPelanggan)
                if (pelanggan != null) {
                    val oldKasbon = pelanggan.sisa_kasbon
                    val newKasbon = oldKasbon + sisaKasbon
                    val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
                    db.pelangganDao().update(updatedPelanggan)
                    enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)

                    val log = KasbonLog(
                        id_pelanggan = idPelanggan,
                        tanggal = tanggal,
                        jumlah_bayar = -sisaKasbon.toDouble(),
                        sisa_kasbon_sebelum = oldKasbon,
                        sisa_kasbon_sesudah = newKasbon,
                        metode_bayar = "Kasbon",
                        catatan = "Pembelian Kasbon (Nota #$insertedTxId)",
                        updatedAt = now
                    )
                    db.kasbonLogDao().insert(log)
                    enqueueUpsert(SyncCollections.KASBON_LOGS, log.syncId, now)
                }
            }
            insertedTxId
        }
        onLocalMutation?.invoke()
        txId
    }

    suspend fun createFastCashTransaksi(idProduk: Int, jumlah: Int, idKurir: Int? = null) {
        val produk = db.produkDao().getById(idProduk) ?: return
        val now = System.currentTimeMillis()
        var createdDefaultCustomer = false
        val defaultPelanggan = db.pelangganDao().getBySyncId("pelanggan_umum_tunai")
            ?: db.pelangganDao().getById(1)
            ?: db.withTransaction {
                val entity = Pelanggan(
                    id = 1,
                    nama = "Umum / Tunai",
                    alamat = "-",
                    no_hp = "-",
                    updatedAt = now,
                    syncId = "pelanggan_umum_tunai"
                )
                db.pelangganDao().insert(entity)
                enqueueUpsert(SyncCollections.PELANGGAN, entity.syncId, now)
                createdDefaultCustomer = true
                db.pelangganDao().getBySyncId("pelanggan_umum_tunai")!!
            }

        if (createdDefaultCustomer) {
            onLocalMutation?.invoke()
        }

        recordTransaksi(
            tanggal = now,
            idPelanggan = defaultPelanggan.id,
            idKurir = idKurir,
            idProduk = produk.id,
            jumlah = jumlah,
            hargaSatuan = produk.harga,
            metodeBayar = "Cash",
            status = "Lunas",
            uangMuka = 0L,
            sisaKasbon = 0L
        )
    }

    suspend fun markTransaksiLunas(transaksiId: Int) {
        val now = System.currentTimeMillis()
        var changed = false
        db.withTransaction {
            val detail = db.transaksiDao().getDetailById(transaksiId) ?: return@withTransaction
            val tx = detail.transaksi
            if (tx.status.equals("Lunas", ignoreCase = true)) return@withTransaction

            val sisa = tx.remainingKasbonAmount()
            val pelanggan = db.pelangganDao().getById(tx.id_pelanggan)
            if (pelanggan != null && sisa > 0) {
                val oldKasbon = pelanggan.sisa_kasbon
                val newKasbon = (oldKasbon - sisa).coerceAtLeast(0.0)
                val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
                db.pelangganDao().update(updatedPelanggan)
                enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)

                val log = KasbonLog(
                    id_pelanggan = pelanggan.id,
                    tanggal = now,
                    jumlah_bayar = sisa,
                    sisa_kasbon_sebelum = oldKasbon,
                    sisa_kasbon_sesudah = newKasbon,
                    metode_bayar = "Cash",
                    catatan = "Pelunasan Nota #${tx.id}",
                    updatedAt = now
                )
                db.kasbonLogDao().insert(log)
                enqueueUpsert(SyncCollections.KASBON_LOGS, log.syncId, now)
            }

            val updatedTx = tx.copy(
                status = "Lunas",
                metode_bayar = "Cash",
                uangMuka = tx.total_harga.toLong(),
                sisaKasbon = 0L,
                updatedAt = now
            )
            db.transaksiDao().update(updatedTx)
            enqueueUpsert(SyncCollections.TRANSAKSI, updatedTx.syncId, now)
            changed = true
        }
        if (changed) onLocalMutation?.invoke()
    }

    suspend fun softDeleteTransaksiByKaryawan(transaksi: Transaksi, karyawanName: String) {
        val now = System.currentTimeMillis()
        var changed = false
        db.withTransaction {
            val latest = db.transaksiDao().getDetailById(transaksi.id)?.transaksi ?: transaksi
            if (KaryawanScopeHelper.isDeletedTransaksi(latest)) return@withTransaction

            if (latest.status.equals("Kasbon", ignoreCase = true)) {
                val deduct = latest.remainingKasbonAmount()
                if (deduct > 0.0) {
                    val pelanggan = db.pelangganDao().getById(latest.id_pelanggan)
                    if (pelanggan != null) {
                        val newKasbon = (pelanggan.sisa_kasbon - deduct).coerceAtLeast(0.0)
                        val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
                        db.pelangganDao().update(updatedPelanggan)
                        enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)
                    }
                }
            }

            val encodedMetode = KaryawanScopeHelper.encodeDeletedTransaksiMetadata(latest, karyawanName)
            val softDeletedTx = latest.copy(
                status = KaryawanScopeHelper.STATUS_DIHAPUS,
                metode_bayar = encodedMetode,
                total_komisi = 0.0,
                uangMuka = 0L,
                sisaKasbon = 0L,
                updatedAt = now
            )
            db.transaksiDao().update(softDeletedTx)
            enqueueUpsert(SyncCollections.TRANSAKSI, softDeletedTx.syncId, now)
            changed = true
        }
        if (changed) onLocalMutation?.invoke()
    }

    suspend fun restoreTransaksi(transaksi: Transaksi) {
        val now = System.currentTimeMillis()
        var changed = false
        db.withTransaction {
            val latest = db.transaksiDao().getDetailById(transaksi.id)?.transaksi ?: transaksi
            if (!KaryawanScopeHelper.isDeletedTransaksi(latest)) return@withTransaction

            val restoredPayment = KaryawanScopeHelper.decodeRestoredTransaksiPayment(latest)
            val kurir = latest.id_kurir?.let { db.kurirDao().getById(it) }
            val breakdown = KomisiCalculator.calculateTransactionCommission(kurir, latest.jumlah)

            val restoredTx = latest.copy(
                status = restoredPayment.status,
                metode_bayar = restoredPayment.metodeBayar,
                uangMuka = restoredPayment.uangMuka,
                sisaKasbon = restoredPayment.sisaKasbon,
                total_komisi = breakdown.totalKomisi,
                updatedAt = now
            )
            db.transaksiDao().update(restoredTx)
            enqueueUpsert(SyncCollections.TRANSAKSI, restoredTx.syncId, now)

            if (restoredPayment.status.equals("Kasbon", ignoreCase = true) && restoredPayment.sisaKasbon > 0L) {
                val pelanggan = db.pelangganDao().getById(latest.id_pelanggan)
                if (pelanggan != null) {
                    val newKasbon = pelanggan.sisa_kasbon + restoredPayment.sisaKasbon.toDouble()
                    val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
                    db.pelangganDao().update(updatedPelanggan)
                    enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)
                }
            }
            changed = true
        }
        if (changed) onLocalMutation?.invoke()
    }

    suspend fun deleteTransaksi(transaksi: Transaksi) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val latest = db.transaksiDao().getDetailById(transaksi.id)?.transaksi ?: transaksi
            if (!KaryawanScopeHelper.isDeletedTransaksi(latest) && latest.status.equals("Kasbon", ignoreCase = true)) {
                val deduct = latest.remainingKasbonAmount()
                if (deduct > 0.0) {
                    val pelanggan = db.pelangganDao().getById(latest.id_pelanggan)
                    if (pelanggan != null) {
                        val newKasbon = (pelanggan.sisa_kasbon - deduct).coerceAtLeast(0.0)
                        val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
                        db.pelangganDao().update(updatedPelanggan)
                        enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)
                    }
                }
            }
            db.transaksiDao().delete(latest)
            enqueueDelete(SyncCollections.TRANSAKSI, latest.syncId, now)
        }
        onLocalMutation?.invoke()
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
        uangMuka: Long
    ) {
        val now = System.currentTimeMillis()
        var changed = false
        db.withTransaction {
            val oldDetail = db.transaksiDao().getDetailById(id) ?: return@withTransaction
            val oldTx = oldDetail.transaksi

            val oldDeduct = oldTx.remainingKasbonAmount()
            if (oldDeduct > 0.0 && oldTx.status.equals("Kasbon", ignoreCase = true)) {
                val oldPelanggan = db.pelangganDao().getById(oldTx.id_pelanggan)
                if (oldPelanggan != null) {
                    val restoredKasbon = (oldPelanggan.sisa_kasbon - oldDeduct).coerceAtLeast(0.0)
                    val updatedOldPelanggan = oldPelanggan.copy(sisa_kasbon = restoredKasbon, updatedAt = now)
                    db.pelangganDao().update(updatedOldPelanggan)
                    enqueueUpsert(SyncCollections.PELANGGAN, updatedOldPelanggan.syncId, now)
                }
            }

            val totalBelanja = (jumlah * hargaSatuan)
            val kurir = idKurir?.let { db.kurirDao().getById(it) }
            val breakdown = KomisiCalculator.calculateTransactionCommission(kurir, jumlah)
            val totalKomisi = breakdown.totalKomisi
            val isBayarSebagian = metodeBayar.equals("Bayar Sebagian", ignoreCase = true)
            val statusVal = if (isBayarSebagian) "Kasbon" else status
            val sisaKasbonVal: Long = when {
                isBayarSebagian -> ((totalBelanja - uangMuka.toDouble()).toLong()).coerceAtLeast(0L)
                statusVal.equals("Kasbon", ignoreCase = true) -> totalBelanja.toLong().coerceAtLeast(0L)
                else -> 0L
            }

            val updatedTx = oldTx.copy(
                tanggal = tanggal,
                id_pelanggan = idPelanggan,
                id_kurir = idKurir,
                id_produk = idProduk,
                jumlah = jumlah,
                harga_satuan = hargaSatuan,
                total_harga = totalBelanja,
                total_komisi = totalKomisi,
                metode_bayar = metodeBayar,
                status = statusVal,
                uangMuka = if (isBayarSebagian) uangMuka else 0L,
                sisaKasbon = sisaKasbonVal,
                updatedAt = now
            )
            db.transaksiDao().update(updatedTx)
            enqueueUpsert(SyncCollections.TRANSAKSI, updatedTx.syncId, now)

            if (statusVal.equals("Kasbon", ignoreCase = true) && sisaKasbonVal > 0L) {
                val newPelanggan = db.pelangganDao().getById(idPelanggan)
                if (newPelanggan != null) {
                    val newKasbon = newPelanggan.sisa_kasbon + sisaKasbonVal.toDouble()
                    val updatedNewPelanggan = newPelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
                    db.pelangganDao().update(updatedNewPelanggan)
                    enqueueUpsert(SyncCollections.PELANGGAN, updatedNewPelanggan.syncId, now)
                }
            }
            changed = true
        }
        if (changed) onLocalMutation?.invoke()
    }

    suspend fun enqueueAllLocalDataForSync() {
        db.withTransaction {
            db.produkDao().getAllOnce().forEach {
                enqueueUpsert(SyncCollections.PRODUK, it.syncId, it.updatedAt)
            }
            db.kurirDao().getAllOnce().forEach {
                enqueueUpsert(SyncCollections.KURIR, it.syncId, it.updatedAt)
            }
            db.pelangganDao().getAllOnce().forEach {
                enqueueUpsert(SyncCollections.PELANGGAN, it.syncId, it.updatedAt)
            }
            db.transaksiDao().getAllOnce().forEach {
                enqueueUpsert(SyncCollections.TRANSAKSI, it.syncId, it.updatedAt)
            }
            db.pengeluaranDao().getAllOnce().forEach {
                enqueueUpsert(SyncCollections.PENGELUARAN, it.syncId, it.updatedAt)
            }
            db.kasbonLogDao().getAllOnce().forEach {
                enqueueUpsert(SyncCollections.KASBON_LOGS, it.syncId, it.updatedAt)
            }
        }
        onLocalMutation?.invoke()
    }

    suspend fun getTransaksiDetailById(id: Int): TransaksiDetail? = db.transaksiDao().getDetailById(id)

    fun getTotalPendapatanByDateRange(start: Long, end: Long): Flow<Double?> =
        db.transaksiDao().getTotalPendapatanByDateRange(start, end)

    fun getKasbonLogsByPelanggan(pelangganId: Int): Flow<List<KasbonLog>> =
        db.kasbonLogDao().getByPelangganId(pelangganId)

    suspend fun deleteAllLocalData() {
        db.withTransaction {
            db.transaksiDao().deleteAll()
            db.pelangganDao().deleteAll()
            db.kurirDao().deleteAll()
            db.produkDao().deleteAll()
            db.pengeluaranDao().deleteAll()
            db.kasbonLogDao().deleteAll()
            db.pendingSyncDao().deleteAll()
        }
    }

    suspend fun isLocalDatabaseEmpty(): Boolean = withContext(Dispatchers.IO) {
        db.produkDao().count() == 0
    }
}
