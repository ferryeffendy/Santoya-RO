package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.entity.KasbonLog
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.PendingSyncEntity
import com.example.data.entity.Pengeluaran
import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import com.example.data.entity.Produk
import com.example.data.entity.TangkiRefill
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.KomisiCalculator
import com.example.util.PerawatanScheduler
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
    private fun monotonicNow(vararg baseTimestamps: Long): Long {
        val current = System.currentTimeMillis()
        val highestBase = baseTimestamps.maxOrNull() ?: 0L
        return if (current > highestBase) current else highestBase + 1L
    }

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
        db.withTransaction {
            val existing = db.produkDao().getById(produk.id)
            val now = monotonicNow(produk.updatedAt, existing?.updatedAt ?: 0L)
            val entity = produk.copy(updatedAt = now)
            db.produkDao().update(entity)
            enqueueUpsert(SyncCollections.PRODUK, entity.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    suspend fun deleteProduk(produk: Produk) {
        db.withTransaction {
            val affectedTx = db.transaksiDao().getByProdukIdOnce(produk.id)
            val existing = db.produkDao().getById(produk.id)
            val maxTxTs = affectedTx.maxOfOrNull { it.updatedAt } ?: 0L
            val now = monotonicNow(produk.updatedAt, existing?.updatedAt ?: 0L, maxTxTs)
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
        @Suppress("DEPRECATION")
        val entity = kurir.copy(password = "", updatedAt = now)
        val id = db.withTransaction {
            val insertedId = db.kurirDao().insert(entity)
            enqueueUpsert(SyncCollections.KURIR, entity.syncId, now)
            insertedId
        }
        onLocalMutation?.invoke()
        return id
    }

    suspend fun updateKurir(kurir: Kurir) {
        db.withTransaction {
            val existing = db.kurirDao().getById(kurir.id)
            val now = monotonicNow(kurir.updatedAt, existing?.updatedAt ?: 0L)
            @Suppress("DEPRECATION")
            val entity = kurir.copy(password = "", updatedAt = now)
            db.kurirDao().update(entity)
            enqueueUpsert(SyncCollections.KURIR, entity.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    suspend fun deleteKurir(kurir: Kurir) {
        db.withTransaction {
            val latest = db.kurirDao().getById(kurir.id) ?: kurir
            val allCust = db.pelangganDao().getAllOnce().filter { it.id_kurir_langganan == kurir.id }
            val maxCustTs = allCust.maxOfOrNull { it.updatedAt } ?: 0L
            val now = monotonicNow(kurir.updatedAt, latest.updatedAt, maxCustTs)
            // Nonaktifkan dan sembunyikan kurir, tetapi jangan hapus barisnya
            // karena transaksi lama masih memakai id_kurir untuk komisi historis.
            val updated = latest.copy(
                akses_aktif = false,
                isDeleted = true,
                updatedAt = now
            )
            db.kurirDao().update(updated)
            enqueueUpsert(SyncCollections.KURIR, updated.syncId, now)

            // Lepaskan tautan kurir langganan pada pelanggan aktif agar tidak merujuk ke kurir nonaktif
            allCust.forEach { p ->
                val updatedCust = p.copy(id_kurir_langganan = null, updatedAt = now)
                db.pelangganDao().update(updatedCust)
                enqueueUpsert(SyncCollections.PELANGGAN, updatedCust.syncId, now)
            }
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
        db.withTransaction {
            val existing = db.pelangganDao().getById(pelanggan.id)
            val now = monotonicNow(pelanggan.updatedAt, existing?.updatedAt ?: 0L)
            val entity = pelanggan.copy(updatedAt = now)
            db.pelangganDao().update(entity)
            enqueueUpsert(SyncCollections.PELANGGAN, entity.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    suspend fun deletePelanggan(pelanggan: Pelanggan): Boolean {
        var softDeleted = false
        db.withTransaction {
            val latest = db.pelangganDao().getById(pelanggan.id) ?: pelanggan
            val unpaidTxs = db.transaksiDao().getUnpaidByPelanggan(latest.id)
                .filter { !KaryawanScopeHelper.isDeletedTransaksi(it) }
            if (latest.sisa_kasbon > 0.0 || unpaidTxs.isNotEmpty()) {
                return@withTransaction
            }
            if (KaryawanScopeHelper.isSoftDeletedPelanggan(latest)) {
                softDeleted = true
                return@withTransaction
            }
            val now = monotonicNow(pelanggan.updatedAt, latest.updatedAt)
            val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(latest.alamat)
            val updated = latest.copy(
                alamat = cleanAlamat,
                isDeleted = true,
                deletedBy = "Pemilik",
                updatedAt = now
            )
            db.pelangganDao().update(updated)
            enqueueUpsert(SyncCollections.PELANGGAN, updated.syncId, now)
            softDeleted = true
        }
        if (softDeleted) onLocalMutation?.invoke()
        return softDeleted
    }

    /**
     * Hapus permanen hanya dipakai dari aksi eksplisit "Hapus Permanen".
     * Riwayat transaksi tetap dipertahankan lewat placeholder di perangkat lain.
     */
    suspend fun permanentlyDeletePelanggan(pelanggan: Pelanggan) {
        var deleted = false
        db.withTransaction {
            val latest = db.pelangganDao().getById(pelanggan.id) ?: pelanggan
            val unpaidTxs = db.transaksiDao().getUnpaidByPelanggan(latest.id)
                .filter { !KaryawanScopeHelper.isDeletedTransaksi(it) }
            if (latest.sisa_kasbon > 0.0 || unpaidTxs.isNotEmpty()) {
                return@withTransaction
            }
            val now = monotonicNow(pelanggan.updatedAt, latest.updatedAt)
            if (db.transaksiDao().countByPelanggan(latest.id) > 0) {
                // Tetap simpan baris lokal (tersembunyi) agar relasi transaksi lama
                // tidak menggantung. Cloud menerima tombstone.
                if (!KaryawanScopeHelper.isSoftDeletedPelanggan(latest)) {
                    val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(latest.alamat)
                    db.pelangganDao().update(
                        latest.copy(
                            alamat = cleanAlamat,
                            isDeleted = true,
                            deletedBy = "Pemilik",
                            updatedAt = now
                        )
                    )
                }
            } else {
                db.pelangganDao().delete(latest)
            }
            enqueueDelete(SyncCollections.PELANGGAN, latest.syncId, now)
            deleted = true
        }
        if (deleted) onLocalMutation?.invoke()
    }

    suspend fun softDeletePelangganByKaryawan(pelanggan: Pelanggan, karyawanName: String): Boolean {
        var softDeleted = false
        db.withTransaction {
            val latest = db.pelangganDao().getById(pelanggan.id) ?: pelanggan
            val unpaidTxs = db.transaksiDao().getUnpaidByPelanggan(latest.id)
                .filter { !KaryawanScopeHelper.isDeletedTransaksi(it) }
            if (latest.sisa_kasbon > 0.0 || unpaidTxs.isNotEmpty()) {
                return@withTransaction
            }
            if (KaryawanScopeHelper.isSoftDeletedPelanggan(latest)) {
                softDeleted = true
                return@withTransaction
            }
            val now = monotonicNow(pelanggan.updatedAt, latest.updatedAt)
            val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(latest.alamat)
            val updated = latest.copy(
                alamat = cleanAlamat,
                isDeleted = true,
                deletedBy = karyawanName.trim().ifBlank { "Karyawan" },
                updatedAt = now
            )
            db.pelangganDao().update(updated)
            enqueueUpsert(SyncCollections.PELANGGAN, updated.syncId, now)
            softDeleted = true
        }
        if (softDeleted) onLocalMutation?.invoke()
        return softDeleted
    }

    suspend fun restorePelanggan(pelanggan: Pelanggan) {
        var restored = false
        db.withTransaction {
            val latest = db.pelangganDao().getById(pelanggan.id) ?: pelanggan
            if (!KaryawanScopeHelper.isSoftDeletedPelanggan(latest)) return@withTransaction
            val now = monotonicNow(pelanggan.updatedAt, latest.updatedAt)
            val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(latest.alamat)
            val updated = latest.copy(
                alamat = cleanAlamat,
                isDeleted = false,
                deletedBy = null,
                updatedAt = now
            )
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
            val txNow = monotonicNow(now, tx.updatedAt)
            val noteRemaining = tx.remainingKasbonAmount().toLong().coerceAtLeast(0L)
            if (noteRemaining <= 0L) {
                val fixedTx = tx.copy(
                    status = "Lunas",
                    metode_bayar = tx.metode_bayar.takeIf { it.isNotBlank() && it != "-" } ?: cleanMetode,
                    uangMuka = tx.uangMuka.coerceIn(0L, tx.total_harga.toLong().coerceAtLeast(0L)),
                    sisaKasbon = 0L,
                    tanggalBayar = tx.tanggalBayar ?: txNow,
                    updatedAt = txNow
                )
                db.transaksiDao().update(fixedTx)
                enqueueUpsert(SyncCollections.TRANSAKSI, fixedTx.syncId, txNow)
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
                    tanggalBayar = txNow,
                    updatedAt = txNow
                )
                db.transaksiDao().update(settledTx)
                enqueueUpsert(SyncCollections.TRANSAKSI, settledTx.syncId, txNow)
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
                    tanggalBayar = null,
                    updatedAt = txNow
                )
                db.transaksiDao().update(partialTx)
                enqueueUpsert(SyncCollections.TRANSAKSI, partialTx.syncId, txNow)
                affectedNoteIds.add(tx.id)
            }
        }
        return affectedNoteIds
    }

    suspend fun reconcileKasbonDiscrepancies(): Int = withContext(Dispatchers.IO) {
        var reconciledCustomers = 0
        db.withTransaction {
            val allCustomers = db.pelangganDao().getAllOnce()
            val allTxs = db.transaksiDao().getAllOnce()
                .filter { !KaryawanScopeHelper.isDeletedTransaksi(it) }
            val allUnpaidNotes = allTxs.filter { it.status.equals("Kasbon", ignoreCase = true) }
            val unpaidByCustomer = allUnpaidNotes.groupBy { it.id_pelanggan }
            val allTxsByCustomer = allTxs.groupBy { it.id_pelanggan }
            val allLogsByCustomer = db.kasbonLogDao().getAllOnce().groupBy { it.id_pelanggan }

            for (pelanggan in allCustomers) {
                val custUnpaid = unpaidByCustomer[pelanggan.id].orEmpty()
                val unpaidTotal = custUnpaid.sumOf { it.remainingKasbonAmount() }
                val targetKasbon = pelanggan.sisa_kasbon.coerceAtLeast(0.0)

                if (unpaidTotal > targetKasbon && custUnpaid.isNotEmpty()) {
                    val alreadyPaidAmount = (unpaidTotal - targetKasbon).toLong().coerceAtLeast(0L)
                    val latestPaymentMethod = allLogsByCustomer[pelanggan.id]
                        ?.filter { it.jumlah_bayar > 0.0 }
                        ?.maxByOrNull { it.tanggal }
                        ?.metode_bayar
                        ?.takeIf { it.isNotBlank() && !it.equals("Kasbon", ignoreCase = true) }
                        ?: "Cash"
                    val now = monotonicNow(
                        pelanggan.updatedAt,
                        custUnpaid.maxOfOrNull { it.updatedAt } ?: 0L
                    )

                    val settledIds = allocatePaymentToUnpaidNotesFifo(
                        unpaidNotes = custUnpaid,
                        amountToSettle = alreadyPaidAmount.coerceAtLeast(1L),
                        metodeBayar = latestPaymentMethod,
                        now = now
                    )
                    if (settledIds.isNotEmpty()) {
                        reconciledCustomers++
                    }
                } else if (targetKasbon > unpaidTotal) {
                    val hasKasbonPaymentHistory = !allLogsByCustomer[pelanggan.id].isNullOrEmpty()
                    val hasSettledNotes = allTxsByCustomer[pelanggan.id].orEmpty().any {
                        it.status.equals("Lunas", ignoreCase = true) && it.tanggalBayar != null && it.tanggalBayar != it.tanggal
                    }
                    if (hasKasbonPaymentHistory || hasSettledNotes || custUnpaid.isNotEmpty()) {
                        val now = monotonicNow(
                            pelanggan.updatedAt,
                            allTxsByCustomer[pelanggan.id]?.maxOfOrNull { it.updatedAt } ?: 0L
                        )
                        val updatedPelanggan = pelanggan.copy(
                            sisa_kasbon = unpaidTotal.coerceAtLeast(0.0),
                            updatedAt = now
                        )
                        db.pelangganDao().update(updatedPelanggan)
                        enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)
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
        val cleanMetode = metodeBayar.trim().ifBlank { "Cash" }
        var changed = false
        db.withTransaction {
            val pelanggan = db.pelangganDao().getById(pelangganId) ?: return@withTransaction
            val unpaidNotes = db.transaksiDao().getUnpaidByPelanggan(pelangganId)
                .filter { !KaryawanScopeHelper.isDeletedTransaksi(it) }
            val now = monotonicNow(
                pelanggan.updatedAt,
                unpaidNotes.maxOfOrNull { it.updatedAt } ?: 0L
            )
            val unpaidTotal = unpaidNotes.sumOf { it.remainingKasbonAmount() }

            val currentKasbon = if (pelanggan.sisa_kasbon > 0.0) {
                pelanggan.sisa_kasbon
            } else {
                unpaidTotal.coerceAtLeast(0.0)
            }
            val effectiveBayar = nominalBayar.coerceAtMost(currentKasbon)
            val kembalian = (nominalBayar - currentKasbon).coerceAtLeast(0.0)
            val newKasbon = (currentKasbon - effectiveBayar).coerceAtLeast(0.0)

            val priorDiscrepancy = (unpaidTotal - currentKasbon).toLong().coerceAtLeast(0L)
            val totalToAllocateOnNotes = priorDiscrepancy + effectiveBayar.toLong().coerceAtLeast(0L)
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
            val kembalianSummary = if (kembalian > 0.0) {
                " (Diterima ${FormatUtils.formatRupiah(nominalBayar)}, Kembalian ${FormatUtils.formatRupiah(kembalian)})"
            } else {
                ""
            }
            val log = KasbonLog(
                id_pelanggan = pelangganId,
                tanggal = System.currentTimeMillis(),
                jumlah_bayar = effectiveBayar,
                sisa_kasbon_sebelum = currentKasbon,
                sisa_kasbon_sesudah = newKasbon,
                metode_bayar = cleanMetode,
                catatan = "Pelunasan Kasbon ($cleanMetode)$kembalianSummary$noteSummary",
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
        db.withTransaction {
            val existing = db.pengeluaranDao().getById(pengeluaran.id)
            val now = monotonicNow(pengeluaran.updatedAt, existing?.updatedAt ?: 0L)
            val entity = pengeluaran.copy(updatedAt = now)
            db.pengeluaranDao().update(entity)
            enqueueUpsert(SyncCollections.PENGELUARAN, entity.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    suspend fun deletePengeluaran(pengeluaran: Pengeluaran) {
        db.withTransaction {
            val existing = db.pengeluaranDao().getById(pengeluaran.id)
            val now = monotonicNow(pengeluaran.updatedAt, existing?.updatedAt ?: 0L)
            db.pengeluaranDao().delete(pengeluaran)
            enqueueDelete(SyncCollections.PENGELUARAN, pengeluaran.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    // --- Riwayat Pembelian Air Tangki & Toren (Depot Mineral / Keduanya) ---
    val allTangkiRefill: Flow<List<TangkiRefill>> = db.tangkiRefillDao().getAll()

    suspend fun recordTangkiRefill(refill: TangkiRefill): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entity = refill.copy(updatedAt = now)
        val insertedId = db.withTransaction {
            val id = db.tangkiRefillDao().insert(entity)
            enqueueUpsert(SyncCollections.TANGKI_REFILL, entity.syncId, now)
            id
        }
        onLocalMutation?.invoke()
        insertedId
    }

    suspend fun deleteTangkiRefill(refill: TangkiRefill) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            db.tangkiRefillDao().delete(refill)
            enqueueDelete(SyncCollections.TANGKI_REFILL, refill.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    // --- Jadwal & Riwayat Perawatan RO (Khusus Pemilik) ---
    val allPerawatan: Flow<List<PerawatanItem>> = db.perawatanDao().getAll()
    val allPerawatanLogs: Flow<List<PerawatanLog>> = db.perawatanLogDao().getAll()

    suspend fun insertPerawatan(item: PerawatanItem): Long {
        val now = System.currentTimeMillis()
        val safeInterval = item.intervalHari.coerceIn(
            PerawatanScheduler.MIN_INTERVAL_DAYS,
            PerawatanScheduler.MAX_INTERVAL_DAYS
        )
        val nextDue = if (item.jadwalBerikutnya > 0L) {
            item.jadwalBerikutnya
        } else {
            PerawatanScheduler.calculateNextDueDateMillis(now, safeInterval)
        }
        val entity = item.copy(
            nama = item.nama.trim().take(PerawatanScheduler.MAX_SCHEDULE_NAME_LENGTH),
            intervalHari = safeInterval,
            jadwalBerikutnya = nextDue,
            updatedAt = now
        )
        val id = db.withTransaction {
            val insertedId = db.perawatanDao().insert(entity)
            enqueueUpsert(SyncCollections.PERAWATAN_JADWAL, entity.syncId, now)
            insertedId
        }
        onLocalMutation?.invoke()
        return id
    }

    suspend fun updatePerawatan(item: PerawatanItem) {
        val safeInterval = item.intervalHari.coerceIn(
            PerawatanScheduler.MIN_INTERVAL_DAYS,
            PerawatanScheduler.MAX_INTERVAL_DAYS
        )
        db.withTransaction {
            val existing = db.perawatanDao().getById(item.id)
            val now = monotonicNow(item.updatedAt, existing?.updatedAt ?: 0L)
            val entity = item.copy(
                nama = item.nama.trim().take(PerawatanScheduler.MAX_SCHEDULE_NAME_LENGTH),
                intervalHari = safeInterval,
                updatedAt = now
            )
            db.perawatanDao().update(entity)
            enqueueUpsert(SyncCollections.PERAWATAN_JADWAL, entity.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    /**
     * Menggeser atau menunda tanggal jatuh tempo jadwal perawatan tanpa mengubah intervalnya.
     */
    suspend fun reschedulePerawatan(
        item: PerawatanItem,
        newDueMillis: Long
    ): Boolean {
        val safeDueMillis = newDueMillis.coerceAtLeast(0L)
        var updated = false
        db.withTransaction {
            val latest = db.perawatanDao().getById(item.id)
                ?: db.perawatanDao().getBySyncId(item.syncId)
                ?: return@withTransaction
            val now = monotonicNow(item.updatedAt, latest.updatedAt)
            val entity = latest.copy(
                jadwalBerikutnya = safeDueMillis,
                updatedAt = now
            )
            db.perawatanDao().update(entity)
            enqueueUpsert(SyncCollections.PERAWATAN_JADWAL, entity.syncId, now)
            updated = true
        }
        if (updated) onLocalMutation?.invoke()
        return updated
    }

    /**
     * Memulihkan templat standar perawatan mesin RO yang belum ada di database lokal tanpa menimpa jadwal existing.
     */
    suspend fun restoreMissingDefaultPerawatan(): Int {
        val now = System.currentTimeMillis()
        var insertedCount = 0
        db.withTransaction {
            val existing = db.perawatanDao().getAllOnce()
            val missing = PerawatanScheduler.buildMissingDefaultItems(
                existingItems = existing,
                baseTimeMillis = now
            )
            for (item in missing) {
                db.perawatanDao().insert(item)
                enqueueUpsert(SyncCollections.PERAWATAN_JADWAL, item.syncId, now)
                insertedCount++
            }
        }
        if (insertedCount > 0) onLocalMutation?.invoke()
        return insertedCount
    }

    suspend fun deletePerawatan(item: PerawatanItem) {
        db.withTransaction {
            val existing = db.perawatanDao().getById(item.id)
            val now = monotonicNow(item.updatedAt, existing?.updatedAt ?: 0L)
            db.perawatanDao().delete(item)
            enqueueDelete(SyncCollections.PERAWATAN_JADWAL, item.syncId, now)
        }
        onLocalMutation?.invoke()
    }

    /**
     * Menandai jadwal perawatan selesai, menghitung jadwal berikutnya dari tanggal selesai + interval,
     * menyimpan riwayat perawatan, dan otomatis mencatat Pengeluaran kategori "Perawatan" jika biaya > 0.
     */
    suspend fun completePerawatan(
        item: PerawatanItem,
        tanggalSelesai: Long,
        biaya: Double,
        catatan: String
    ): Boolean {
        val cleanBiaya = biaya.coerceAtLeast(0.0)
        val cleanCatatan = catatan.trim().take(500)
        var saved = false

        db.withTransaction {
            val latest = db.perawatanDao().getById(item.id)
                ?: db.perawatanDao().getBySyncId(item.syncId)
                ?: return@withTransaction
            val now = monotonicNow(item.updatedAt, latest.updatedAt)

            val nextDue = PerawatanScheduler.calculateNextDueDateMillis(
                completionDateMillis = tanggalSelesai,
                intervalHari = latest.intervalHari
            )

            var pengeluaranSyncId: String? = null
            if (cleanBiaya > 0.0) {
                val expense = Pengeluaran(
                    tanggal = tanggalSelesai,
                    nama_item = "Perawatan: ${latest.nama}".take(200),
                    kategori = PerawatanScheduler.KATEGORI_PENGELUARAN_PERAWATAN,
                    nominal = cleanBiaya,
                    updatedAt = now
                )
                db.pengeluaranDao().insert(expense)
                enqueueUpsert(SyncCollections.PENGELUARAN, expense.syncId, now)
                pengeluaranSyncId = expense.syncId
            }

            val updatedItem = latest.copy(
                terakhirSelesai = tanggalSelesai,
                jadwalBerikutnya = nextDue,
                updatedAt = now
            )
            db.perawatanDao().update(updatedItem)
            enqueueUpsert(SyncCollections.PERAWATAN_JADWAL, updatedItem.syncId, now)

            val log = PerawatanLog(
                perawatanSyncId = updatedItem.syncId,
                namaPerawatan = updatedItem.nama,
                tanggalSelesai = tanggalSelesai,
                biaya = cleanBiaya,
                catatan = cleanCatatan,
                pengeluaranSyncId = pengeluaranSyncId,
                updatedAt = now
            )
            db.perawatanLogDao().insert(log)
            enqueueUpsert(SyncCollections.PERAWATAN_LOG, log.syncId, now)
            saved = true
        }
        if (saved) onLocalMutation?.invoke()
        return saved
    }

    /**
     * Memperbarui catatan riwayat perawatan beserta sinkronisasi otomatis ke catatan Pengeluaran terkait
     * (membuat baru, memperbarui nominal/tanggal, atau menghapus pengeluaran bila biaya diubah ke Rp 0)
     * serta mengevaluasi ulang tanggal jadwal induk bila tanggal selesai riwayat terbaru berubah.
     */
    suspend fun updatePerawatanLog(
        log: PerawatanLog,
        tanggalSelesaiBaru: Long,
        biayaBaru: Double,
        catatanBaru: String
    ): Boolean {
        val cleanBiaya = biayaBaru.coerceAtLeast(0.0)
        val cleanCatatan = catatanBaru.trim().take(500)
        val cleanDate = tanggalSelesaiBaru.coerceAtLeast(0L)
        var saved = false

        db.withTransaction {
            val existing = db.perawatanLogDao().getBySyncId(log.syncId)
                ?: db.perawatanLogDao().getById(log.id)
                ?: return@withTransaction
            val expSyncId = existing.pengeluaranSyncId?.trim().orEmpty()
            val linkedExpense = if (expSyncId.isNotEmpty()) {
                db.pengeluaranDao().getBySyncId(expSyncId)
            } else {
                null
            }
            val scheduleSyncId = existing.perawatanSyncId.trim()
            val parentItem = if (scheduleSyncId.isNotEmpty() && scheduleSyncId != "standalone") {
                db.perawatanDao().getBySyncId(scheduleSyncId)
            } else {
                null
            }

            val now = monotonicNow(
                log.updatedAt,
                existing.updatedAt,
                linkedExpense?.updatedAt ?: 0L,
                parentItem?.updatedAt ?: 0L
            )

            val updatedPengeluaranSyncId: String? = when {
                cleanBiaya > 0.0 && linkedExpense != null -> {
                    val updatedExpense = linkedExpense.copy(
                        tanggal = cleanDate,
                        nama_item = "Perawatan: ${existing.namaPerawatan}".take(200),
                        kategori = PerawatanScheduler.KATEGORI_PENGELUARAN_PERAWATAN,
                        nominal = cleanBiaya,
                        updatedAt = now
                    )
                    db.pengeluaranDao().update(updatedExpense)
                    enqueueUpsert(SyncCollections.PENGELUARAN, updatedExpense.syncId, now)
                    updatedExpense.syncId
                }
                cleanBiaya > 0.0 -> {
                    val newExpense = Pengeluaran(
                        tanggal = cleanDate,
                        nama_item = "Perawatan: ${existing.namaPerawatan}".take(200),
                        kategori = PerawatanScheduler.KATEGORI_PENGELUARAN_PERAWATAN,
                        nominal = cleanBiaya,
                        updatedAt = now
                    )
                    db.pengeluaranDao().insert(newExpense)
                    enqueueUpsert(SyncCollections.PENGELUARAN, newExpense.syncId, now)
                    newExpense.syncId
                }
                expSyncId.isNotEmpty() -> {
                    if (linkedExpense != null) {
                        db.pengeluaranDao().delete(linkedExpense)
                    } else {
                        db.pengeluaranDao().deleteBySyncId(expSyncId)
                    }
                    enqueueDelete(SyncCollections.PENGELUARAN, expSyncId, now)
                    null
                }
                else -> null
            }

            val updatedLog = existing.copy(
                tanggalSelesai = cleanDate,
                biaya = cleanBiaya,
                catatan = cleanCatatan,
                pengeluaranSyncId = updatedPengeluaranSyncId,
                updatedAt = now
            )
            db.perawatanLogDao().update(updatedLog)
            enqueueUpsert(SyncCollections.PERAWATAN_LOG, updatedLog.syncId, now)

            if (parentItem != null && existing.tanggalSelesai != cleanDate) {
                val allScheduleLogs = db.perawatanLogDao().getByPerawatanSyncId(scheduleSyncId)
                    .map { if (it.syncId == updatedLog.syncId) updatedLog else it }
                val latestAfterEdit = allScheduleLogs.maxByOrNull { it.tanggalSelesai }
                val wasLatestCompleted = parentItem.terakhirSelesai == existing.tanggalSelesai
                val becameLatestCompleted = latestAfterEdit?.syncId == updatedLog.syncId
                if (wasLatestCompleted || becameLatestCompleted) {
                    val latestDoneMillis = latestAfterEdit?.tanggalSelesai ?: cleanDate
                    val nextDue = PerawatanScheduler.calculateNextDueDateMillis(
                        completionDateMillis = latestDoneMillis,
                        intervalHari = parentItem.intervalHari
                    )
                    val updatedParent = parentItem.copy(
                        terakhirSelesai = latestDoneMillis,
                        jadwalBerikutnya = nextDue,
                        updatedAt = now
                    )
                    db.perawatanDao().update(updatedParent)
                    enqueueUpsert(SyncCollections.PERAWATAN_JADWAL, updatedParent.syncId, now)
                }
            }
            saved = true
        }
        if (saved) onLocalMutation?.invoke()
        return saved
    }

    /**
     * Menghapus riwayat perawatan RO, ikut menghapus catatan Pengeluaran otomatis yang terkait
     * (`pengeluaranSyncId`), serta mengembalikan tanggal `terakhirSelesai` dan `jadwalBerikutnya`
     * pada jadwal perawatan induk apabila riwayat yang dihapus merupakan pencatatan terbaru.
     */
    suspend fun deletePerawatanLog(log: PerawatanLog) {
        db.withTransaction {
            val existing = db.perawatanLogDao().getBySyncId(log.syncId)
            val targetLog = existing ?: log
            val expSyncId = targetLog.pengeluaranSyncId?.trim().orEmpty()
            val linkedExpense = if (expSyncId.isNotEmpty()) {
                db.pengeluaranDao().getBySyncId(expSyncId)
            } else {
                null
            }
            val scheduleSyncId = targetLog.perawatanSyncId.trim()
            val parentItem = if (scheduleSyncId.isNotEmpty() && scheduleSyncId != "standalone") {
                db.perawatanDao().getBySyncId(scheduleSyncId)
            } else {
                null
            }

            val now = monotonicNow(
                log.updatedAt,
                existing?.updatedAt ?: 0L,
                linkedExpense?.updatedAt ?: 0L,
                parentItem?.updatedAt ?: 0L
            )

            db.perawatanLogDao().delete(targetLog)
            enqueueDelete(SyncCollections.PERAWATAN_LOG, targetLog.syncId, now)

            if (expSyncId.isNotEmpty()) {
                if (linkedExpense != null) {
                    db.pengeluaranDao().delete(linkedExpense)
                } else {
                    db.pengeluaranDao().deleteBySyncId(expSyncId)
                }
                enqueueDelete(SyncCollections.PENGELUARAN, expSyncId, now)
            }

            if (parentItem != null && parentItem.terakhirSelesai == targetLog.tanggalSelesai) {
                val remainingLogs = db.perawatanLogDao().getByPerawatanSyncId(scheduleSyncId)
                    .filter { it.syncId != targetLog.syncId }
                val prevLatest = remainingLogs.maxByOrNull { it.tanggalSelesai }
                val revertedTerakhir = prevLatest?.tanggalSelesai
                val revertedNextDue = if (revertedTerakhir != null) {
                    PerawatanScheduler.calculateNextDueDateMillis(
                        completionDateMillis = revertedTerakhir,
                        intervalHari = parentItem.intervalHari
                    )
                } else {
                    PerawatanScheduler.calculateNextDueDateMillis(
                        completionDateMillis = now,
                        intervalHari = parentItem.intervalHari
                    )
                }
                val updatedParent = parentItem.copy(
                    terakhirSelesai = revertedTerakhir,
                    jadwalBerikutnya = revertedNextDue,
                    updatedAt = now
                )
                db.perawatanDao().update(updatedParent)
                enqueueUpsert(SyncCollections.PERAWATAN_JADWAL, updatedParent.syncId, now)
            }
        }
        onLocalMutation?.invoke()
    }

    /**
     * Menyimpan catatan pemeriksaan atau perawatan mandiri (seperti cek kesehatan membran RO atau pencucian CIP)
     * ke riwayat perawatan, serta otomatis mencatat Pengeluaran kategori "Perawatan" jika [biaya] > 0.
     */
    suspend fun recordStandalonePerawatanLog(
        namaPerawatan: String,
        tanggalSelesai: Long,
        catatan: String,
        biaya: Double = 0.0
    ): Boolean {
        val cleanName = namaPerawatan.trim().take(PerawatanScheduler.MAX_SCHEDULE_NAME_LENGTH)
        if (cleanName.isEmpty()) return false
        val cleanBiaya = biaya.coerceAtLeast(0.0)
        val now = System.currentTimeMillis()
        db.withTransaction {
            var pengeluaranSyncId: String? = null
            if (cleanBiaya > 0.0) {
                val expense = Pengeluaran(
                    tanggal = tanggalSelesai,
                    nama_item = "Perawatan: $cleanName".take(200),
                    kategori = PerawatanScheduler.KATEGORI_PENGELUARAN_PERAWATAN,
                    nominal = cleanBiaya,
                    updatedAt = now
                )
                db.pengeluaranDao().insert(expense)
                enqueueUpsert(SyncCollections.PENGELUARAN, expense.syncId, now)
                pengeluaranSyncId = expense.syncId
            }
            val log = PerawatanLog(
                perawatanSyncId = "standalone",
                namaPerawatan = cleanName,
                tanggalSelesai = tanggalSelesai,
                biaya = cleanBiaya,
                catatan = catatan.trim().take(500),
                pengeluaranSyncId = pengeluaranSyncId,
                updatedAt = now
            )
            db.perawatanLogDao().insert(log)
            enqueueUpsert(SyncCollections.PERAWATAN_LOG, log.syncId, now)
        }
        onLocalMutation?.invoke()
        return true
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
        sisaKasbon: Long,
        waterType: String = Transaksi.WATER_TYPE_RO
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val safeJumlah = jumlah.coerceAtLeast(1)
        val safeHarga = hargaSatuan.coerceAtLeast(0.0)
        val safeUangMukaInput = uangMuka.coerceAtLeast(0L)
        val totalBelanja = (safeJumlah * safeHarga)
        val kurir = idKurir?.let { db.kurirDao().getById(it) }
        val breakdown = KomisiCalculator.calculateTransactionCommission(kurir, safeJumlah)
        val totalKomisi = breakdown.totalKomisi.coerceAtLeast(0.0)

        val txId = db.withTransaction {
            val isBayarSebagian = metodeBayar.equals("Bayar Sebagian", ignoreCase = true)
            val isFullPaidByDp = isBayarSebagian && safeUangMukaInput >= totalBelanja.toLong()
            val statusVal = when {
                isFullPaidByDp -> "Lunas"
                isBayarSebagian -> "Kasbon"
                else -> status
            }
            val metodeVal = when {
                isFullPaidByDp -> "Cash"
                isBayarSebagian -> "Bayar Sebagian"
                statusVal.equals("Kasbon", ignoreCase = true) -> "-"
                else -> metodeBayar.ifBlank { "Cash" }
            }
            val uangMukaVal = when {
                isFullPaidByDp -> totalBelanja.toLong().coerceAtLeast(0L)
                isBayarSebagian -> safeUangMukaInput.coerceIn(0L, totalBelanja.toLong().coerceAtLeast(0L))
                else -> 0L
            }
            val sisaKasbonVal = when {
                isFullPaidByDp -> 0L
                isBayarSebagian -> (totalBelanja.toLong() - uangMukaVal).coerceAtLeast(0L)
                statusVal.equals("Kasbon", ignoreCase = true) -> totalBelanja.toLong().coerceAtLeast(0L)
                else -> 0L
            }
            val tanggalBayarVal = if (statusVal.equals("Lunas", ignoreCase = true)) tanggal else null

            val transaksi = Transaksi(
                tanggal = tanggal,
                id_pelanggan = idPelanggan,
                id_kurir = idKurir,
                id_produk = idProduk,
                jumlah = safeJumlah,
                harga_satuan = safeHarga,
                total_harga = totalBelanja,
                total_komisi = totalKomisi,
                metode_bayar = metodeVal,
                status = statusVal,
                uangMuka = uangMukaVal,
                sisaKasbon = sisaKasbonVal,
                tanggalBayar = tanggalBayarVal,
                waterType = waterType,
                updatedAt = now
            )
            val insertedTxId = db.transaksiDao().insert(transaksi)
            enqueueUpsert(SyncCollections.TRANSAKSI, transaksi.syncId, now)

            if (statusVal.equals("Kasbon", ignoreCase = true) && sisaKasbonVal > 0) {
                val pelanggan = db.pelangganDao().getById(idPelanggan)
                if (pelanggan != null) {
                    val pelangganNow = monotonicNow(now, pelanggan.updatedAt)
                    val oldKasbon = pelanggan.sisa_kasbon.coerceAtLeast(0.0)
                    val newKasbon = oldKasbon + sisaKasbonVal.toDouble()
                    val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = pelangganNow)
                    db.pelangganDao().update(updatedPelanggan)
                    enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, pelangganNow)
                }
            }
            insertedTxId
        }
        onLocalMutation?.invoke()
        txId
    }

    /**
     * Memastikan entitas pelanggan umum ("Umum / Tunai") tersedia untuk transaksi kasir cepat / walk-in.
     */
    suspend fun ensureGeneralCustomer(): Pelanggan {
        val now = System.currentTimeMillis()
        var createdDefaultCustomer = false
        val defaultPelanggan = db.pelangganDao().getBySyncId("pelanggan_umum_tunai")
            ?: db.withTransaction {
                val entity = Pelanggan(
                    nama = "Umum / Tunai",
                    alamat = "-",
                    no_hp = "-",
                    updatedAt = now,
                    syncId = "pelanggan_umum_tunai"
                )
                val insertedId = db.pelangganDao().insert(entity)
                enqueueUpsert(SyncCollections.PELANGGAN, entity.syncId, now)
                createdDefaultCustomer = true
                entity.copy(id = insertedId.toInt())
            }

        if (createdDefaultCustomer) {
            onLocalMutation?.invoke()
        }
        return defaultPelanggan
    }

    suspend fun createFastCashTransaksi(
        idProduk: Int,
        jumlah: Int,
        idKurir: Int? = null,
        waterType: String = Transaksi.WATER_TYPE_RO
    ) {
        val produk = db.produkDao().getById(idProduk) ?: return
        val now = System.currentTimeMillis()
        val defaultPelanggan = ensureGeneralCustomer()

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
            sisaKasbon = 0L,
            waterType = waterType
        )
    }

    suspend fun markTransaksiLunas(transaksiId: Int, metodeBayar: String = "Cash") {
        val cleanMetode = metodeBayar.trim().ifBlank { "Cash" }
        var changed = false
        db.withTransaction {
            val detail = db.transaksiDao().getDetailById(transaksiId) ?: return@withTransaction
            val tx = detail.transaksi
            if (tx.status.equals("Lunas", ignoreCase = true)) return@withTransaction

            val sisa = tx.remainingKasbonAmount()
            val pelanggan = db.pelangganDao().getById(tx.id_pelanggan)
            val now = monotonicNow(tx.updatedAt, pelanggan?.updatedAt ?: 0L)
            if (pelanggan != null && sisa > 0) {
                val oldKasbon = pelanggan.sisa_kasbon.coerceAtLeast(0.0)
                val newKasbon = (oldKasbon - sisa).coerceAtLeast(0.0)
                val loggedBayar = (oldKasbon - newKasbon).coerceAtLeast(0.0)
                val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
                db.pelangganDao().update(updatedPelanggan)
                enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)

                if (loggedBayar > 0.0) {
                    val log = KasbonLog(
                        id_pelanggan = pelanggan.id,
                        tanggal = System.currentTimeMillis(),
                        jumlah_bayar = loggedBayar,
                        sisa_kasbon_sebelum = oldKasbon,
                        sisa_kasbon_sesudah = newKasbon,
                        metode_bayar = cleanMetode,
                        catatan = "Pelunasan Nota #${tx.id} ($cleanMetode)",
                        updatedAt = now
                    )
                    db.kasbonLogDao().insert(log)
                    enqueueUpsert(SyncCollections.KASBON_LOGS, log.syncId, now)
                }
            }

            val finalMetode = if (tx.metode_bayar.equals("Kasbon", ignoreCase = true) || tx.metode_bayar == "-") {
                cleanMetode
            } else {
                tx.metode_bayar
            }

            val updatedTx = tx.copy(
                status = "Lunas",
                metode_bayar = finalMetode,
                uangMuka = tx.uangMuka.coerceIn(0L, tx.total_harga.toLong().coerceAtLeast(0L)),
                sisaKasbon = 0L,
                tanggalBayar = now,
                updatedAt = now
            )
            db.transaksiDao().update(updatedTx)
            enqueueUpsert(SyncCollections.TRANSAKSI, updatedTx.syncId, now)
            changed = true
        }
        if (changed) onLocalMutation?.invoke()
    }

    suspend fun softDeleteTransaksiByKaryawan(transaksi: Transaksi, karyawanName: String) {
        var changed = false
        db.withTransaction {
            val latest = db.transaksiDao().getDetailById(transaksi.id)?.transaksi ?: transaksi
            if (KaryawanScopeHelper.isDeletedTransaksi(latest)) return@withTransaction

            val pelanggan = db.pelangganDao().getById(latest.id_pelanggan)
            val now = monotonicNow(transaksi.updatedAt, latest.updatedAt, pelanggan?.updatedAt ?: 0L)

            val preservedSisa = if (latest.status.equals("Kasbon", ignoreCase = true)) {
                val deduct = latest.remainingKasbonAmount()
                if (deduct > 0.0 && pelanggan != null) {
                    val newKasbon = (pelanggan.sisa_kasbon - deduct).coerceAtLeast(0.0)
                    val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
                    db.pelangganDao().update(updatedPelanggan)
                    enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)
                }
                deduct.toLong().coerceAtLeast(0L)
            } else {
                latest.sisaKasbon
            }

            val softDeletedTx = latest.copy(
                status = KaryawanScopeHelper.STATUS_DIHAPUS,
                sisaKasbon = preservedSisa,
                isDeleted = true,
                deletedBy = karyawanName.trim().ifBlank { "Karyawan" },
                total_komisi = 0.0,
                updatedAt = now
            )
            db.transaksiDao().update(softDeletedTx)
            enqueueUpsert(SyncCollections.TRANSAKSI, softDeletedTx.syncId, now)
            changed = true
        }
        if (changed) onLocalMutation?.invoke()
    }

    suspend fun restoreTransaksi(transaksi: Transaksi) {
        var changed = false
        db.withTransaction {
            val latest = db.transaksiDao().getDetailById(transaksi.id)?.transaksi ?: transaksi
            if (!KaryawanScopeHelper.isDeletedTransaksi(latest)) return@withTransaction

            val pelanggan = db.pelangganDao().getById(latest.id_pelanggan)
            val now = monotonicNow(transaksi.updatedAt, latest.updatedAt, pelanggan?.updatedAt ?: 0L)

            val kurir = latest.id_kurir?.let { db.kurirDao().getById(it) }
            val breakdown = KomisiCalculator.calculateTransactionCommission(kurir, latest.jumlah)

            val (restoredStatus, restoredMetode, restoredUangMuka, restoredSisa) = if (latest.metode_bayar.startsWith("DEL:")) {
                val decoded = KaryawanScopeHelper.decodeRestoredTransaksiPayment(latest)
                listOf(decoded.status, decoded.metodeBayar, decoded.uangMuka, decoded.sisaKasbon)
            } else {
                val sisa = latest.sisaKasbon
                val dp = latest.uangMuka
                val total = latest.total_harga.toLong().coerceAtLeast(0L)
                val isKasbon = sisa > 0L ||
                    latest.metode_bayar == "-" ||
                    latest.metode_bayar.equals("Kasbon", ignoreCase = true) ||
                    (latest.metode_bayar.equals("Bayar Sebagian", ignoreCase = true) && dp < total)
                val st = if (isKasbon) "Kasbon" else "Lunas"
                val finalSisa = if (isKasbon && sisa == 0L) (total - dp).coerceAtLeast(0L) else sisa
                val finalMetode = when {
                    isKasbon && dp > 0L -> "Bayar Sebagian"
                    isKasbon -> "-"
                    latest.metode_bayar.isBlank() || latest.metode_bayar == "-" -> "Cash"
                    else -> latest.metode_bayar
                }
                listOf(st, finalMetode, dp, finalSisa)
            }

            val statusStr = restoredStatus as String
            val metodeStr = restoredMetode as String
            val dpLong = restoredUangMuka as Long
            val sisaLong = restoredSisa as Long

            val restoredTx = latest.copy(
                status = statusStr,
                metode_bayar = metodeStr,
                uangMuka = dpLong,
                sisaKasbon = sisaLong,
                isDeleted = false,
                deletedBy = null,
                total_komisi = breakdown.totalKomisi,
                updatedAt = now
            )
            db.transaksiDao().update(restoredTx)
            enqueueUpsert(SyncCollections.TRANSAKSI, restoredTx.syncId, now)

            if (statusStr.equals("Kasbon", ignoreCase = true) && sisaLong > 0L && pelanggan != null) {
                val newKasbon = pelanggan.sisa_kasbon + sisaLong.toDouble()
                val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
                db.pelangganDao().update(updatedPelanggan)
                enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)
            }
            changed = true
        }
        if (changed) onLocalMutation?.invoke()
    }

    suspend fun deleteTransaksi(transaksi: Transaksi, deletedByName: String = "Pemilik") {
        var changed = false
        db.withTransaction {
            val latest = db.transaksiDao().getDetailById(transaksi.id)?.transaksi ?: transaksi
            if (KaryawanScopeHelper.isDeletedTransaksi(latest)) return@withTransaction

            val pelanggan = db.pelangganDao().getById(latest.id_pelanggan)
            val now = monotonicNow(transaksi.updatedAt, latest.updatedAt, pelanggan?.updatedAt ?: 0L)

            val preservedSisa = if (latest.status.equals("Kasbon", ignoreCase = true)) {
                val deduct = latest.remainingKasbonAmount()
                if (deduct > 0.0 && pelanggan != null) {
                    val newKasbon = (pelanggan.sisa_kasbon - deduct).coerceAtLeast(0.0)
                    val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
                    db.pelangganDao().update(updatedPelanggan)
                    enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)
                }
                deduct.toLong().coerceAtLeast(0L)
            } else {
                latest.sisaKasbon
            }

            val softDeletedTx = latest.copy(
                status = KaryawanScopeHelper.STATUS_DIHAPUS,
                sisaKasbon = preservedSisa,
                isDeleted = true,
                deletedBy = deletedByName.trim().ifBlank { "Pemilik" },
                total_komisi = 0.0,
                updatedAt = now
            )
            db.transaksiDao().update(softDeletedTx)
            enqueueUpsert(SyncCollections.TRANSAKSI, softDeletedTx.syncId, now)
            changed = true
        }
        if (changed) onLocalMutation?.invoke()
    }

    suspend fun permanentDeleteTransaksi(transaksi: Transaksi) {
        db.withTransaction {
            val latest = db.transaksiDao().getDetailById(transaksi.id)?.transaksi ?: transaksi
            val pelanggan = db.pelangganDao().getById(latest.id_pelanggan)
            val now = monotonicNow(transaksi.updatedAt, latest.updatedAt, pelanggan?.updatedAt ?: 0L)
            if (!KaryawanScopeHelper.isDeletedTransaksi(latest) && latest.status.equals("Kasbon", ignoreCase = true)) {
                val deduct = latest.remainingKasbonAmount()
                if (deduct > 0.0 && pelanggan != null) {
                    val newKasbon = (pelanggan.sisa_kasbon - deduct).coerceAtLeast(0.0)
                    val updatedPelanggan = pelanggan.copy(sisa_kasbon = newKasbon, updatedAt = now)
                    db.pelangganDao().update(updatedPelanggan)
                    enqueueUpsert(SyncCollections.PELANGGAN, updatedPelanggan.syncId, now)
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
        uangMuka: Long,
        waterType: String? = null
    ) {
        var changed = false
        db.withTransaction {
            val oldDetail = db.transaksiDao().getDetailById(id) ?: return@withTransaction
            val oldTx = oldDetail.transaksi
            val oldPelanggan = db.pelangganDao().getById(oldTx.id_pelanggan)
            val targetPelanggan = if (idPelanggan == oldTx.id_pelanggan) {
                oldPelanggan
            } else {
                db.pelangganDao().getById(idPelanggan)
            }
            val now = monotonicNow(
                oldTx.updatedAt,
                oldPelanggan?.updatedAt ?: 0L,
                targetPelanggan?.updatedAt ?: 0L
            )

            val oldDeduct = oldTx.remainingKasbonAmount()
            if (oldDeduct > 0.0 && oldTx.status.equals("Kasbon", ignoreCase = true) && oldPelanggan != null) {
                val restoredKasbon = (oldPelanggan.sisa_kasbon - oldDeduct).coerceAtLeast(0.0)
                val updatedOldPelanggan = oldPelanggan.copy(sisa_kasbon = restoredKasbon, updatedAt = now)
                db.pelangganDao().update(updatedOldPelanggan)
                enqueueUpsert(SyncCollections.PELANGGAN, updatedOldPelanggan.syncId, now)
            }

            val safeJumlah = jumlah.coerceAtLeast(1)
            val safeHarga = hargaSatuan.coerceAtLeast(0.0)
            val safeUangMukaInput = uangMuka.coerceAtLeast(0L)
            val totalBelanja = (safeJumlah * safeHarga)
            val kurir = idKurir?.let { db.kurirDao().getById(it) }
            val breakdown = KomisiCalculator.calculateTransactionCommission(kurir, safeJumlah)
            val totalKomisi = breakdown.totalKomisi.coerceAtLeast(0.0)
            val nonDpInstallmentCredit = if (oldTx.status.equals("Kasbon", ignoreCase = true)) {
                (oldTx.total_harga.toLong() - oldTx.uangMuka - oldDeduct.toLong()).coerceAtLeast(0L)
            } else {
                0L
            }
            val hasReferencedKasbonLog = if (oldTx.status.equals("Kasbon", ignoreCase = true) && oldTx.uangMuka > 0L) {
                val refRegex = Regex("(?:Nota\\s+#|,\\s*#)${oldTx.id}(?!\\d)", RegexOption.IGNORE_CASE)
                db.kasbonLogDao().getAllOnce().any { log ->
                    log.id_pelanggan == oldTx.id_pelanggan &&
                        log.jumlah_bayar > 0.0 &&
                        refRegex.containsMatchIn(log.catatan)
                }
            } else {
                false
            }
            val alreadyPaidViaInstallment = if (oldTx.status.equals("Kasbon", ignoreCase = true)) {
                if (hasReferencedKasbonLog) {
                    (oldTx.total_harga.toLong() - oldDeduct.toLong()).coerceAtLeast(0L)
                } else {
                    nonDpInstallmentCredit
                }
            } else {
                0L
            }
            val isBayarSebagian = metodeBayar.equals("Bayar Sebagian", ignoreCase = true)
            val effectivePaid = when {
                isBayarSebagian -> safeUangMukaInput
                status.equals("Kasbon", ignoreCase = true) && safeUangMukaInput == 0L && alreadyPaidViaInstallment > 0L -> alreadyPaidViaInstallment
                else -> 0L
            }
            val isFullPaidByDp = (isBayarSebagian || (status.equals("Kasbon", ignoreCase = true) && effectivePaid > 0L)) &&
                effectivePaid >= totalBelanja.toLong()
            val statusVal = when {
                isFullPaidByDp -> "Lunas"
                isBayarSebagian -> "Kasbon"
                else -> status
            }
            val finalUangMuka = when {
                isFullPaidByDp -> totalBelanja.toLong().coerceAtLeast(0L)
                isBayarSebagian || (statusVal.equals("Kasbon", ignoreCase = true) && effectivePaid > 0L) -> {
                    effectivePaid.coerceIn(0L, totalBelanja.toLong().coerceAtLeast(0L))
                }
                else -> 0L
            }
            val sisaKasbonVal: Long = when {
                isFullPaidByDp -> 0L
                statusVal.equals("Kasbon", ignoreCase = true) -> (totalBelanja.toLong() - finalUangMuka).coerceAtLeast(0L)
                else -> 0L
            }
            val finalMetode = when {
                isFullPaidByDp -> "Cash"
                isBayarSebagian || (statusVal.equals("Kasbon", ignoreCase = true) && finalUangMuka > 0L) -> "Bayar Sebagian"
                statusVal.equals("Kasbon", ignoreCase = true) -> "-"
                else -> metodeBayar.ifBlank { "Cash" }
            }
            val tanggalBayarVal: Long? = when {
                statusVal.equals("Lunas", ignoreCase = true) -> oldTx.tanggalBayar ?: now
                else -> null
            }

            val updatedTx = oldTx.copy(
                tanggal = tanggal,
                id_pelanggan = idPelanggan,
                id_kurir = idKurir,
                id_produk = idProduk,
                jumlah = safeJumlah,
                harga_satuan = safeHarga,
                total_harga = totalBelanja,
                total_komisi = totalKomisi,
                metode_bayar = finalMetode,
                status = statusVal,
                uangMuka = finalUangMuka,
                sisaKasbon = sisaKasbonVal,
                tanggalBayar = tanggalBayarVal,
                waterType = waterType ?: oldTx.waterType,
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
            val validPelangganIds = mutableSetOf<Int>()
            db.pelangganDao().getAllOnce().forEach { p ->
                val isEmptyPlaceholder = p.isDeleted && p.updatedAt == 0L
                if (!isEmptyPlaceholder) {
                    validPelangganIds.add(p.id)
                    enqueueUpsert(SyncCollections.PELANGGAN, p.syncId, p.updatedAt)
                }
            }
            db.transaksiDao().getAllOnce().forEach { tx ->
                if (tx.id_pelanggan in validPelangganIds) {
                    enqueueUpsert(SyncCollections.TRANSAKSI, tx.syncId, tx.updatedAt)
                }
            }
            db.pengeluaranDao().getAllOnce().forEach {
                enqueueUpsert(SyncCollections.PENGELUARAN, it.syncId, it.updatedAt)
            }
            db.kasbonLogDao().getAllOnce().forEach { log ->
                if (log.id_pelanggan in validPelangganIds) {
                    enqueueUpsert(SyncCollections.KASBON_LOGS, log.syncId, log.updatedAt)
                }
            }
            db.perawatanDao().getAllOnce().forEach {
                enqueueUpsert(SyncCollections.PERAWATAN_JADWAL, it.syncId, it.updatedAt)
            }
            db.perawatanLogDao().getAllOnce().forEach {
                enqueueUpsert(SyncCollections.PERAWATAN_LOG, it.syncId, it.updatedAt)
            }
            db.tangkiRefillDao().getAllOnce().forEach {
                enqueueUpsert(SyncCollections.TANGKI_REFILL, it.syncId, it.updatedAt)
            }
        }
        onLocalMutation?.invoke()
    }

    suspend fun getTransaksiDetailById(id: Int): TransaksiDetail? = db.transaksiDao().getDetailById(id)

    fun getTotalPendapatanByDateRange(start: Long, end: Long): Flow<Double?> =
        db.transaksiDao().getTotalPendapatanByDateRange(start, end)

    fun getKasbonLogsByPelanggan(pelangganId: Int): Flow<List<KasbonLog>> =
        db.kasbonLogDao().getByPelangganId(pelangganId)

    val allKasbonLogs: Flow<List<KasbonLog>> = db.kasbonLogDao().getAll()

    suspend fun deleteAllLocalData() {
        db.withTransaction {
            db.transaksiDao().deleteAll()
            db.pelangganDao().deleteAll()
            db.kurirDao().deleteAll()
            db.produkDao().deleteAll()
            db.pengeluaranDao().deleteAll()
            db.kasbonLogDao().deleteAll()
            db.perawatanLogDao().deleteAll()
            db.perawatanDao().deleteAll()
            db.pendingSyncDao().deleteAll()
        }
    }

    suspend fun isLocalDatabaseEmpty(): Boolean = withContext(Dispatchers.IO) {
        db.produkDao().count() == 0
    }
}
