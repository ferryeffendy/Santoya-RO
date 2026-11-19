package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.entity.DepotProfile
import com.example.data.entity.KasbonLog
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object ExportHelper {

    internal const val MAX_IMPORT_FILE_BYTES: Int = 10 * 1024 * 1024 // 10 MB
    private const val BACKUP_CACHE_TTL_MILLIS: Long = 24L * 60L * 60L * 1000L // 24 jam
    private const val AUTO_BACKUP_FILE_NAME = "auto_backup_before_import.json"
    private val VALID_SYNC_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")

    /**
     * Memvalidasi dan menormalisasi `syncId` agar mematuhi aturan `isValidId` di `firestore.rules`.
     * Mengembalikan `null` bila string kosong atau mengandung karakter di luar `[a-zA-Z0-9_-]`.
     */
    internal fun sanitizeSyncId(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true)) return null
        return if (VALID_SYNC_ID_REGEX.matches(trimmed)) trimmed else null
    }

    /**
     * Menormalisasi email kurir dari berkas impor JSON dan menolak email pemilik depot aktif
     * agar tidak terjadi kebingungan hak akses (role confusion).
     */
    internal fun sanitizeImportedCourierEmail(raw: String?, ownerEmail: String? = null): String {
        val normalized = KaryawanScopeHelper.normalizeEmail(raw)
        if (normalized.isBlank()) return ""
        val normalizedOwner = KaryawanScopeHelper.normalizeEmail(ownerEmail)
        if (normalizedOwner.isNotBlank() && normalized == normalizedOwner) {
            return ""
        }
        return normalized
    }

    private fun pruneStaleBackupFiles(dir: File, nowMillis: Long = System.currentTimeMillis()) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isFile && (nowMillis - f.lastModified()) > BACKUP_CACHE_TTL_MILLIS) {
                runCatching { f.delete() }
            }
        }
    }

    private fun isGenericFakeKurirName(name: String?): Boolean {
        if (name.isNullOrBlank()) return true
        val trimmed = name.trim()
        if (trimmed == "-" ||
            trimmed.equals("null", ignoreCase = true) ||
            trimmed.equals("Tanpa Kurir", ignoreCase = true) ||
            trimmed.equals("Ambil Sendiri", ignoreCase = true) ||
            trimmed.equals("Kurir", ignoreCase = true)
        ) {
            return true
        }
        return Regex("^kurir\\s*#?\\d+$", RegexOption.IGNORE_CASE).matches(trimmed)
    }

    private fun isDefaultSeedKurirName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val trimmed = name.trim()
        return trimmed.equals("Budi Santoso", ignoreCase = true) ||
            trimmed.equals("Agus Rahmat", ignoreCase = true)
    }

    // --- Nested JSON Export ---
    fun generateNestedJson(
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>,
        produkList: List<Produk>,
        kurirList: List<Kurir>,
        pengeluaranList: List<Pengeluaran>,
        kasbonLogList: List<KasbonLog> = emptyList(),
        perawatanList: List<PerawatanItem> = emptyList(),
        perawatanLogList: List<PerawatanLog> = emptyList(),
        depotProfile: DepotProfile? = null
    ): String {
        val root = JSONObject()
        root.put("app", "Santoya RO")
        root.put("version", "2.1")
        root.put("export_timestamp", System.currentTimeMillis())
        root.put("export_date", FormatUtils.formatDate(System.currentTimeMillis()))

        depotProfile?.let { prof ->
            val profObj = JSONObject()
            profObj.put("ownerName", prof.effectiveOwnerName())
            profObj.put("depotName", prof.depotName.trim())
            profObj.put("phoneNumber", prof.normalizedPhoneNumber())
            profObj.put("address", prof.address.trim())
            profObj.put("receiptFooter", prof.receiptFooter.trim())
            profObj.put("depotType", prof.depotType)
            profObj.put("tankCapacityLiters", prof.tankCapacityLiters)
            root.put("depot_profile", profObj)
        }

        // Gabungkan kurirList & produkList dengan relasi yang ada di transaksiList agar master data selalu lengkap
        val effectiveKurirMap = linkedMapOf<Int, Kurir>()
        kurirList.forEach { k -> effectiveKurirMap[k.id] = k }
        transaksiList.mapNotNull { it.kurir }.forEach { k ->
            if (!effectiveKurirMap.containsKey(k.id)) {
                effectiveKurirMap[k.id] = k
            }
        }
        val effectiveKurirList = effectiveKurirMap.values.toList()

        val effectiveProdukMap = linkedMapOf<Int, Produk>()
        produkList.forEach { p -> effectiveProdukMap[p.id] = p }
        transaksiList.mapNotNull { it.produk }.forEach { p ->
            if (!effectiveProdukMap.containsKey(p.id)) {
                effectiveProdukMap[p.id] = p
            }
        }
        val effectiveProdukList = effectiveProdukMap.values.toList()

        // Master Kurir (diekspor lengkap dengan id, syncId, nama, komisi_per_galon, email, akses_aktif)
        val kurirArray = JSONArray()
        effectiveKurirList.forEach { k ->
            val obj = JSONObject()
            obj.put("id", k.id)
            obj.put("syncId", k.syncId)
            obj.put("nama", k.nama)
            obj.put("komisi_per_galon", k.komisi_per_galon)
            obj.put("email", k.email)
            obj.put("akses_aktif", k.akses_aktif)
            kurirArray.put(obj)
        }
        root.put("master_kurir", kurirArray)

        // Master Produk
        val prodArray = JSONArray()
        effectiveProdukList.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("syncId", p.syncId)
            obj.put("nama_produk", p.nama_produk)
            obj.put("harga", p.harga)
            prodArray.put(obj)
        }
        root.put("master_produk", prodArray)

        val activePelangganList = pelangganList.filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) }
        val activeTransaksiList = transaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }

        // Nested Pelanggan + Riwayat Transaksi
        val pelangganArray = JSONArray()
        activePelangganList.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("syncId", p.syncId)
            pObj.put("nama", p.nama)
            pObj.put("alamat", KaryawanScopeHelper.cleanPelangganAlamat(p.alamat))
            pObj.put("no_hp", p.no_hp)
            pObj.put("sisa_kasbon", p.sisa_kasbon)
            p.harga_khusus?.let { pObj.put("harga_khusus", it) }
            p.id_kurir_langganan?.let { kId ->
                pObj.put("id_kurir_langganan", kId)
                val kName = effectiveKurirMap[kId]?.nama?.takeIf { !isGenericFakeKurirName(it) }
                if (!kName.isNullOrBlank()) {
                    pObj.put("kurir_langganan_nama", kName)
                }
            }

            val trxArray = JSONArray()
            val custTrx = activeTransaksiList.filter { it.transaksi.id_pelanggan == p.id }
            custTrx.forEach { t ->
                val tObj = JSONObject()
                val resolvedProdName = effectiveProdukMap[t.transaksi.id_produk]?.nama_produk
                    ?: t.produk?.nama_produk
                    ?: "-"
                val resolvedKurirName = t.transaksi.id_kurir?.let { kId ->
                    effectiveKurirMap[kId]?.nama ?: t.kurir?.nama
                }?.takeIf { !isGenericFakeKurirName(it) } ?: "-"

                tObj.put("id", t.transaksi.id)
                tObj.put("syncId", t.transaksi.syncId)
                tObj.put("id_pelanggan", t.transaksi.id_pelanggan)
                tObj.put("id_produk", t.transaksi.id_produk)
                if (t.transaksi.id_kurir != null) {
                    tObj.put("id_kurir", t.transaksi.id_kurir)
                } else {
                    tObj.put("id_kurir", JSONObject.NULL)
                }
                tObj.put("tanggal", t.transaksi.tanggal)
                tObj.put("tanggal_formatted", FormatUtils.formatDate(t.transaksi.tanggal))
                if (t.transaksi.tanggalBayar != null) {
                    tObj.put("tanggalBayar", t.transaksi.tanggalBayar)
                } else {
                    tObj.put("tanggalBayar", JSONObject.NULL)
                }
                tObj.put("produk", resolvedProdName)
                tObj.put("kurir", resolvedKurirName)
                tObj.put("jumlah", t.transaksi.jumlah)
                tObj.put("harga_satuan", t.transaksi.harga_satuan)
                tObj.put("total_harga", t.transaksi.total_harga)
                tObj.put("total_komisi", t.transaksi.total_komisi)
                tObj.put("metode_bayar", t.transaksi.metode_bayar)
                tObj.put("status", t.transaksi.status)
                tObj.put("uangMuka", t.transaksi.uangMuka)
                tObj.put("sisaKasbon", t.transaksi.sisaKasbon)
                tObj.put("waterType", t.transaksi.waterType)
                trxArray.put(tObj)
            }
            pObj.put("riwayat_transaksi", trxArray)
            pelangganArray.put(pObj)
        }
        root.put("data_pelanggan_nested", pelangganArray)

        // Pengeluaran
        val expArray = JSONArray()
        pengeluaranList.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("syncId", e.syncId)
            obj.put("tanggal", e.tanggal)
            obj.put("kategori", e.kategori)
            obj.put("nama_item", e.nama_item)
            obj.put("nominal", e.nominal)
            expArray.put(obj)
        }
        root.put("pengeluaran", expArray)

        // Riwayat Pelunasan & Cicilan Kasbon
        val pelangganById = pelangganList.associateBy { it.id }
        val kasbonLogArray = JSONArray()
        kasbonLogList.forEach { log ->
            val obj = JSONObject()
            obj.put("id", log.id)
            obj.put("syncId", log.syncId)
            obj.put("id_pelanggan", log.id_pelanggan)
            val cust = pelangganById[log.id_pelanggan]
            if (cust != null) {
                obj.put("pelangganSyncId", cust.syncId)
                obj.put("nama_pelanggan", cust.nama)
            }
            obj.put("tanggal", log.tanggal)
            obj.put("jumlah_bayar", log.jumlah_bayar)
            obj.put("sisa_kasbon_sebelum", log.sisa_kasbon_sebelum)
            obj.put("sisa_kasbon_sesudah", log.sisa_kasbon_sesudah)
            obj.put("metode_bayar", log.metode_bayar)
            obj.put("catatan", log.catatan)
            kasbonLogArray.put(obj)
        }
        root.put("kasbon_logs", kasbonLogArray)

        // Jadwal Perawatan RO
        val perawatanArray = JSONArray()
        perawatanList.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("syncId", item.syncId)
            obj.put("nama", item.nama)
            obj.put("intervalHari", item.intervalHari)
            if (item.terakhirSelesai != null) {
                obj.put("terakhirSelesai", item.terakhirSelesai)
            } else {
                obj.put("terakhirSelesai", JSONObject.NULL)
            }
            obj.put("jadwalBerikutnya", item.jadwalBerikutnya)
            perawatanArray.put(obj)
        }
        root.put("perawatan_jadwal", perawatanArray)

        // Riwayat Perawatan RO
        val perawatanLogArray = JSONArray()
        perawatanLogList.forEach { log ->
            val obj = JSONObject()
            obj.put("id", log.id)
            obj.put("syncId", log.syncId)
            obj.put("perawatanSyncId", log.perawatanSyncId)
            obj.put("namaPerawatan", log.namaPerawatan)
            obj.put("tanggalSelesai", log.tanggalSelesai)
            obj.put("biaya", log.biaya)
            obj.put("catatan", log.catatan)
            if (!log.pengeluaranSyncId.isNullOrBlank()) {
                obj.put("pengeluaranSyncId", log.pengeluaranSyncId)
            } else {
                obj.put("pengeluaranSyncId", JSONObject.NULL)
            }
            perawatanLogArray.put(obj)
        }
        root.put("perawatan_log", perawatanLogArray)

        return root.toString(2)
    }

    // --- File Naming & Sanitization ---
    /**
     * Membersihkan karakter string agar aman digunakan sebagai nama berkas di semua platform OS.
     */
    fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return cleaned.ifBlank { "Laporan" }
    }

    /**
     * Membangun nama berkas ekspor deskriptif yang memuat label periode dan stempel tanggal cetak.
     * Contoh: Laporan_Santoya_Bulan_Ini_20261116.csv
     */
    fun buildExportFileName(
        prefix: String = "Laporan_Santoya",
        periodeLabel: String,
        extension: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): String {
        val cleanPrefix = sanitizeFileName(prefix)
        val cleanPeriod = sanitizeFileName(periodeLabel)
        val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestampMillis))
        val ext = extension.removePrefix(".")
        return "${cleanPrefix}_${cleanPeriod}_$dateStamp.$ext"
    }

    // --- CSV Export ---
    /**
     * Menghasilkan konten CSV laporan keuangan lengkap dengan UTF-8 BOM, escaping RFC-4180,
     * ringkasan laba rugi & arus kas, umur piutang kasbon, peringkat wilayah/desa, rincian transaksi,
     * pelunasan kasbon, pengeluaran, komisi kurir, dan rekap produk.
     */
    fun generateCsv(
        transaksiList: List<TransaksiDetail>,
        pengeluaranList: List<Pengeluaran>,
        kasbonLogList: List<KasbonLog> = emptyList(),
        pelangganList: List<Pelanggan> = emptyList(),
        startTime: Long = 0L,
        endTime: Long = Long.MAX_VALUE,
        periodeLabel: String = "Semua Waktu",
        kasbonAgingSummary: KasbonAgingSummary? = null,
        rankedDesaEntries: List<RankingEntryUi> = emptyList()
    ): String {
        val summary = LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = transaksiList,
            allPengeluaran = pengeluaranList,
            allKasbonLogs = kasbonLogList,
            allPelanggan = pelangganList,
            startTime = startTime,
            endTime = endTime
        )
        val esc: (String?) -> String = LaporanFinanceCalculator::escapeCsvField

        val sb = StringBuilder()
        // UTF-8 BOM agar Excel Windows/Android membaca karakter Indonesia dengan benar
        sb.append('\uFEFF')

        sb.append("=== RINGKASAN LABA RUGI & ARUS KAS SANTOYA RO ===\n")
        sb.append("Periode,${esc(periodeLabel)}\n")
        sb.append("Komponen,Nilai\n")
        sb.append("Total Galon Terjual,${summary.totalGalon}\n")
        if (summary.totalGalonMineral > 0 && summary.totalGalonRo > 0) {
            sb.append("  - Galon Air RO,${summary.totalGalonRo}\n")
            sb.append("  - Galon Air Mineral,${summary.totalGalonMineral}\n")
        }
        sb.append("Total Omzet Nota Penjualan,${summary.totalOmzetPenjualan.toLong()}\n")
        sb.append("Kas Masuk dari Penjualan Langsung & DP,${summary.penjualanTunaiLangsung.toLong()}\n")
        sb.append("Kas Masuk dari Pelunasan Kasbon,${summary.pelunasanKasbonPeriode.toLong()}\n")
        sb.append("Total Kas Masuk (Pendapatan Riil),${summary.totalKasMasuk.toLong()}\n")
        sb.append("  - Rincian Tunai (Laci Kas),${summary.totalKasTunai.toLong()}\n")
        sb.append("  - Rincian Transfer / Non-Tunai,${summary.totalKasNonTunai.toLong()}\n")
        sb.append("Piutang Kasbon Baru Periode Ini,${summary.piutangKasbonPeriode.toLong()}\n")
        sb.append("Total Komisi Kurir,${summary.totalKomisiKurir.toLong()}\n")
        sb.append("Total Pengeluaran Operasional,${summary.totalPengeluaran.toLong()}\n")
        sb.append("Laba Bersih Kas,${summary.labaBersih.toLong()}\n")
        sb.append("Margin Laba (%),${esc(FormatUtils.formatPercent(summary.marginLabaPersen))}\n")

        val effectiveAging = kasbonAgingSummary ?: if (pelangganList.isNotEmpty()) {
            LaporanFinanceCalculator.calculateKasbonAging(
                allTransaksi = transaksiList,
                allPelanggan = pelangganList,
                nowMillis = if (endTime < Long.MAX_VALUE) endTime else System.currentTimeMillis()
            )
        } else null

        if (effectiveAging != null) {
            sb.append("\n=== ANALISIS UMUR PIUTANG KASBON ===\n")
            sb.append("Kategori,JumlahNota,TotalNominal\n")
            sb.append("Lancar (0-7 Hari),${effectiveAging.bucket0To7Count},${effectiveAging.bucket0To7Amount.toLong()}\n")
            sb.append("Waspada (8-30 Hari),${effectiveAging.bucket8To30Count},${effectiveAging.bucket8To30Amount.toLong()}\n")
            sb.append("Kritis (>30 Hari),${effectiveAging.bucketOver30Count},${effectiveAging.bucketOver30Amount.toLong()}\n")
            sb.append("Total Piutang Aktif,${effectiveAging.totalActiveNotes},${effectiveAging.totalActiveReceivable.toLong()}\n")
        }

        val effectiveDesaList = if (rankedDesaEntries.isNotEmpty()) {
            rankedDesaEntries
        } else if (summary.desaEntries.isNotEmpty()) {
            RankingMetricsHelper.buildRankedEntries(summary.desaEntries, RankingSortMode.GALON)
        } else {
            emptyList()
        }

        if (effectiveDesaList.isNotEmpty()) {
            sb.append("\n=== PERINGKAT PENJUALAN PER WILAYAH ===\n")
            sb.append("Wilayah,TotalGalon,JumlahTransaksi,TotalOmzet,PorsiOmzet\n")
            effectiveDesaList.forEach { d ->
                sb.append(
                    listOf(
                        esc(d.title),
                        d.totalGalon.toString(),
                        d.totalTransaksi.toString(),
                        d.totalPendapatan.toLong().toString(),
                        esc(FormatUtils.formatPercent(d.omzetSharePercent))
                    ).joinToString(",")
                ).append('\n')
            }
        }

        sb.append("\n=== LAPORAN TRANSAKSI SANTOYA RO ===\n")
        sb.append("ID,Tanggal,Pelanggan,Alamat,NoHP,Produk,Jumlah,HargaSatuan,TotalHarga,UangMuka,SisaKasbon,Kurir,TotalKomisi,MetodeBayar,Status\n")
        summary.filteredTx.forEach { t ->
            val trx = t.transaksi
            val tgl = FormatUtils.formatDateTimeTable(trx.tanggal)
            val custName = t.pelanggan?.nama?.trim()?.ifBlank { "Umum" } ?: "Umum"
            val custAddr = KaryawanScopeHelper.cleanPelangganAlamat(t.pelanggan?.alamat ?: "-").ifBlank { "-" }
            val custPhone = t.pelanggan?.no_hp ?: "-"
            val prod = t.produk?.nama_produk ?: "Galon RO"
            val kurir = t.kurir?.nama?.takeIf { !isGenericFakeKurirName(it) } ?: "Ambil Sendiri"
            val sisaKasbonVal = trx.remainingKasbonAmount().toLong()

            sb.append(
                listOf(
                    trx.id.toString(),
                    esc(tgl),
                    esc(custName),
                    esc(custAddr),
                    esc(custPhone),
                    esc(prod),
                    trx.jumlah.toString(),
                    trx.harga_satuan.toLong().toString(),
                    trx.total_harga.toLong().toString(),
                    trx.uangMuka.toString(),
                    sisaKasbonVal.toString(),
                    esc(kurir),
                    trx.total_komisi.toLong().toString(),
                    esc(FormatUtils.formatMetodeBayarLabel(trx.metode_bayar)),
                    esc(trx.status)
                ).joinToString(",")
            ).append('\n')
        }

        sb.append("\n=== PELUNASAN & CICILAN KASBON SANTOYA RO ===\n")
        sb.append("ID,Tanggal,Pelanggan,MetodeBayar,JumlahBayar,SisaSebelum,SisaSesudah,Catatan\n")
        summary.filteredKasbonLogs.forEach { item ->
            val log = item.log
            sb.append(
                listOf(
                    log.id.toString(),
                    esc(FormatUtils.formatDateTimeTable(log.tanggal)),
                    esc(item.namaPelanggan),
                    esc(FormatUtils.formatMetodeBayarLabel(log.metode_bayar)),
                    log.jumlah_bayar.toLong().toString(),
                    log.sisa_kasbon_sebelum.toLong().toString(),
                    log.sisa_kasbon_sesudah.toLong().toString(),
                    esc(log.catatan)
                ).joinToString(",")
            ).append('\n')
        }

        sb.append("\n=== LAPORAN PENGELUARAN SANTOYA RO ===\n")
        sb.append("ID,Tanggal,Kategori,NamaItem,Nominal\n")
        summary.filteredExp.forEach { e ->
            sb.append(
                listOf(
                    e.id.toString(),
                    esc(FormatUtils.formatDateTimeTable(e.tanggal)),
                    esc(e.kategori),
                    esc(e.nama_item),
                    e.nominal.toLong().toString()
                ).joinToString(",")
            ).append('\n')
        }

        if (summary.kurirEntries.isNotEmpty()) {
            sb.append("\n=== REKAP KOMISI & PENGANTARAN KURIR ===\n")
            sb.append("Kurir,TotalGalon,JumlahKirim,TotalOmzet,TotalKomisi\n")
            summary.kurirEntries
                .sortedByDescending { it.totalGalon }
                .forEach { k ->
                    sb.append(
                        listOf(
                            esc(k.title),
                            k.totalGalon.toString(),
                            k.totalTransaksi.toString(),
                            k.totalPendapatan.toLong().toString(),
                            k.totalKomisi.toLong().toString()
                        ).joinToString(",")
                    ).append('\n')
                }
        }

        if (summary.productBreakdown.isNotEmpty()) {
            sb.append("\n=== REKAP PENJUALAN PER PRODUK ===\n")
            sb.append("Produk,TotalGalon,JumlahTransaksi,TotalOmzet,PorsiOmzet\n")
            summary.productBreakdown.forEach { p ->
                sb.append(
                    listOf(
                        esc(p.namaProduk),
                        p.totalGalon.toString(),
                        p.totalTransaksi.toString(),
                        p.totalOmzet.toLong().toString(),
                        esc(FormatUtils.formatPercent(p.sharePercent))
                    ).joinToString(",")
                ).append('\n')
            }
        }

        return sb.toString()
    }

    /**
     * Menghasilkan dokumen HTML siap cetak PDF (A4) untuk [LaporanScreen] yang konsisten
     * dengan perhitungan arus kas riil, mencakup kop resmi dari [depotProfile], analisis umur piutang kasbon,
     * rekapitulasi produk & wilayah/desa, transaksi/harian, pelunasan kasbon, pengeluaran,
     * rekap komisi kurir, dan lembar tanda tangan pengesahan.
     */
    fun generateLaporanHtml(
        summary: LaporanPeriodSummary,
        allTransaksi: List<TransaksiDetail>,
        allKasbonLogs: List<KasbonLog>,
        periodeLabel: String,
        isSummaryMode: Boolean,
        kasbonAgingSummary: KasbonAgingSummary? = null,
        rankedDesaEntries: List<RankingEntryUi> = emptyList(),
        depotProfile: DepotProfile = DepotProfile(),
        isProUser: Boolean = depotProfile.isProUser()
    ): String {
        val esc: (String?) -> String = LaporanFinanceCalculator::escapeHtml
        val cleanDepotName = depotProfile.effectiveDepotName()
        val cleanAddress = depotProfile.address.trim()
        val cleanPhone = depotProfile.phoneNumber.trim()
        val contactParts = buildList {
            if (cleanAddress.isNotEmpty()) add(cleanAddress)
            if (cleanPhone.isNotEmpty()) add("WA/Telp: $cleanPhone")
        }
        val ownerSignatureName = depotProfile.effectiveOwnerName().ifBlank { "Pemilik $cleanDepotName" }

        return buildString {
            append("<html><head><meta charset='UTF-8'/><style>")
            append("body { font-family: Arial, sans-serif; padding: 24px; color: #1E293B; font-size: 11.5px; line-height: 1.45; }")
            append(".kop-container { text-align: center; margin-bottom: 18px; border-bottom: 2px solid #0284C7; padding-bottom: 12px; }")
            append(".kop-brand { font-size: 22px; font-weight: bold; color: #0369A1; letter-spacing: 1.5px; margin-bottom: 2px; }")
            append(".kop-tagline { font-size: 11px; color: #475569; font-weight: 500; }")
            append(".kop-address { font-size: 10px; color: #64748B; margin-top: 2px; }")
            append(".report-title-bar { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 14px; border-bottom: 1px solid #E2E8F0; padding-bottom: 6px; }")
            append(".report-title { font-size: 15px; font-weight: bold; color: #0F172A; letter-spacing: 0.5px; }")
            append(".report-meta { font-size: 11px; color: #475569; }")
            append("h3 { color: #0F172A; font-size: 13px; margin-top: 18px; margin-bottom: 6px; border-bottom: 1px solid #CBD5E1; padding-bottom: 4px; }")
            append(".grid { display: flex; gap: 12px; margin-bottom: 12px; }")
            append(".card { flex: 1; background: #F8FAFC; border: 1px solid #E2E8F0; padding: 12px; border-radius: 8px; page-break-inside: avoid; }")
            append(".highlight { background: #EFF6FF; border-color: #BFDBFE; }")
            append(".card-title { font-weight: bold; font-size: 12px; color: #0F172A; margin-bottom: 6px; display: block; }")
            append(".metric-row { display: flex; justify-content: space-between; margin: 4px 0; }")
            append(".muted { color: #64748B; font-size: 10.5px; }")
            append("table { width: 100%; border-collapse: collapse; margin-top: 6px; font-size: 10.5px; }")
            append("th, td { border: 1px solid #CBD5E1; padding: 5px 6px; text-align: left; vertical-align: top; }")
            append("th { background: #F1F5F9; font-weight: bold; color: #0F172A; }")
            append("tr { page-break-inside: avoid; }")
            append(".right { text-align: right; white-space: nowrap; }")
            append(".total-row { background: #F8FAFC; font-weight: bold; }")
            append(".signature-container { display: flex; justify-content: space-between; margin-top: 36px; page-break-inside: avoid; }")
            append(".signature-box { width: 38%; text-align: center; }")
            append(".signature-role { font-size: 11px; color: #475569; font-weight: 500; }")
            append(".signature-space { height: 48px; }")
            append(".signature-line { border-bottom: 1px solid #1E293B; margin: 0 16px 4px 16px; }")
            append(".signature-name { font-size: 11px; font-weight: bold; color: #0F172A; }")
            append(".pdf-watermark { margin-top: 24px; text-align: center; font-size: 10px; color: #94A3B8; }")
            append("</style></head><body>")

            // 1. Kop Resmi Laporan
            append("<div class='kop-container'>")
            append("<div class='kop-brand'>${esc(cleanDepotName.uppercase())}</div>")
            append("<div class='kop-tagline'>Laporan Penjualan &amp; Keuangan Usaha Air Minum</div>")
            if (contactParts.isNotEmpty()) {
                append("<div class='kop-address'>${esc(contactParts.joinToString(" • "))}</div>")
            }
            append("</div>")

            // 2. Bar Judul Laporan & Periode
            append("<div class='report-title-bar'>")
            append("<div class='report-title'>LAPORAN ARUS KAS &amp; LABA BERSIH</div>")
            append("<div class='report-meta'>Periode: <b>${esc(periodeLabel)}</b> &bull; Dicetak: ${esc(FormatUtils.formatDateTimeUi(System.currentTimeMillis()))}</div>")
            append("</div>")

            // 3. Grid Finansial: Arus Kas & Rekonsiliasi
            append("<div class='grid'>")
            append("<div class='card highlight'>")
            append("<span class='card-title'>Ringkasan Arus Kas &amp; Laba Bersih</span>")
            append("<div class='metric-row'><span>Total Volume Terjual</span><b>${summary.totalGalon} Galon</b></div>")
            if (depotProfile.isBothType() || (summary.totalGalonRo > 0 && summary.totalGalonMineral > 0)) {
                append("<div class='metric-row'><span class='muted'>Rincian Jenis Air</span><span class='muted'>Air RO: ${summary.totalGalonRo} Galon &bull; Air Mineral: ${summary.totalGalonMineral} Galon</span></div>")
            }
            append("<div class='metric-row'><span>Total Omzet Nota</span><span>${esc(FormatUtils.formatRupiah(summary.totalOmzetPenjualan))}</span></div>")
            append("<div class='metric-row'><span>Kas Penjualan Langsung &amp; DP</span><span>${esc(FormatUtils.formatRupiah(summary.penjualanTunaiLangsung))}</span></div>")
            append("<div class='metric-row'><span>Pelunasan Kasbon Masuk</span><span>${esc(FormatUtils.formatRupiah(summary.pelunasanKasbonPeriode))}</span></div>")
            append("<div class='metric-row'><b>Total Kas Masuk Riil</b><b>${esc(FormatUtils.formatRupiah(summary.totalKasMasuk))}</b></div>")
            append("<div class='metric-row'><span>Piutang Kasbon Baru</span><span>${esc(FormatUtils.formatRupiah(summary.piutangKasbonPeriode))}</span></div>")
            append("<hr style='border:none;border-top:1px solid #CBD5E1;margin:6px 0;'/>")
            append("<div class='metric-row'><span>Komisi Kurir</span><span>- ${esc(FormatUtils.formatRupiah(summary.totalKomisiKurir))}</span></div>")
            append("<div class='metric-row'><span>Pengeluaran Operasional</span><span>- ${esc(FormatUtils.formatRupiah(summary.totalPengeluaran))}</span></div>")
            append("<div class='metric-row'><b>${if (summary.labaBersih >= 0) "Laba Bersih" else "Rugi Bersih"} (Margin ${esc(FormatUtils.formatPercent(summary.marginLabaPersen))})</b><b>${esc(FormatUtils.formatRupiah(summary.labaBersih))}</b></div>")
            append("</div>")

            append("<div class='card'>")
            append("<span class='card-title'>Rekonsiliasi Kas Masuk</span>")
            append("<div class='metric-row'><span>Kas Tunai (Penjualan)</span><span>${esc(FormatUtils.formatRupiah(summary.kasTunaiDariJual))}</span></div>")
            append("<div class='metric-row'><span>Kas Tunai (Pelunasan)</span><span>${esc(FormatUtils.formatRupiah(summary.kasTunaiDariKasbon))}</span></div>")
            append("<div class='metric-row'><b>Total Tunai (Laci Kas)</b><b>${esc(FormatUtils.formatRupiah(summary.totalKasTunai))}</b></div>")
            append("<hr style='border:none;border-top:1px solid #E2E8F0;margin:6px 0;'/>")
            append("<div class='metric-row'><span>Transfer / Non-Tunai (Penjualan)</span><span>${esc(FormatUtils.formatRupiah(summary.kasTransferDariJual))}</span></div>")
            append("<div class='metric-row'><span>Transfer / Non-Tunai (Pelunasan)</span><span>${esc(FormatUtils.formatRupiah(summary.kasTransferDariKasbon))}</span></div>")
            append("<div class='metric-row'><b>Total Transfer / QRIS (Rekening)</b><b>${esc(FormatUtils.formatRupiah(summary.totalKasNonTunai))}</b></div>")
            append("</div>")
            append("</div>")

            // 4. Analisis Umur Piutang Kasbon (Aging)
            val effectiveAging = kasbonAgingSummary ?: if (allTransaksi.isNotEmpty()) {
                val pelangganList = allTransaksi.mapNotNull { it.pelanggan }.distinctBy { it.id }
                if (pelangganList.isNotEmpty()) {
                    LaporanFinanceCalculator.calculateKasbonAging(
                        allTransaksi = allTransaksi,
                        allPelanggan = pelangganList,
                        nowMillis = System.currentTimeMillis()
                    )
                } else null
            } else null

            if (effectiveAging != null) {
                append("<div class='card' style='margin-bottom:14px;'>")
                append("<span class='card-title'>Kesehatan &amp; Umur Piutang Kasbon</span>")
                append("<div class='grid' style='margin-bottom:0;'>")
                append("<div style='flex:1;'><div class='muted'>Lancar (0–7 Hari)</div><b>${esc(FormatUtils.formatRupiah(effectiveAging.bucket0To7Amount))}</b> <span class='muted'>(${effectiveAging.bucket0To7Count} nota)</span></div>")
                append("<div style='flex:1;'><div class='muted'>Waspada (8–30 Hari)</div><b style='color:#D97706;'>${esc(FormatUtils.formatRupiah(effectiveAging.bucket8To30Amount))}</b> <span class='muted'>(${effectiveAging.bucket8To30Count} nota)</span></div>")
                append("<div style='flex:1;'><div class='muted'>Kritis (&gt;30 Hari)</div><b style='color:#DC2626;'>${esc(FormatUtils.formatRupiah(effectiveAging.bucketOver30Amount))}</b> <span class='muted'>(${effectiveAging.bucketOver30Count} nota)</span></div>")
                append("<div style='flex:1;'><div class='muted'>Total Piutang Aktif</div><b>${esc(FormatUtils.formatRupiah(effectiveAging.totalActiveReceivable))}</b> <span class='muted'>(${effectiveAging.totalActiveNotes} nota)</span></div>")
                append("</div>")
                append("</div>")
            }

            // 5. Rekap Penjualan per Produk
            if (summary.productBreakdown.isNotEmpty()) {
                append("<h3>Rekapitulasi Penjualan per Produk</h3>")
                append("<table><thead><tr><th>Produk</th><th class='right'>Galon</th><th class='right'>Transaksi</th><th class='right'>Omzet Nota</th><th class='right'>Porsi Omzet</th></tr></thead><tbody>")
                summary.productBreakdown.forEach { p ->
                    append("<tr>")
                    append("<td>${esc(p.namaProduk)}</td>")
                    append("<td class='right'>${p.totalGalon}</td>")
                    append("<td class='right'>${p.totalTransaksi}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatRupiah(p.totalOmzet))}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatPercent(p.sharePercent))}</td>")
                    append("</tr>")
                }
                append("</tbody></table>")
            }

            // 6. Rekap Penjualan per Wilayah / Desa
            val effectiveDesaList = if (rankedDesaEntries.isNotEmpty()) {
                rankedDesaEntries
            } else if (summary.desaEntries.isNotEmpty()) {
                RankingMetricsHelper.buildRankedEntries(summary.desaEntries, RankingSortMode.GALON)
            } else {
                emptyList()
            }
            if (effectiveDesaList.isNotEmpty()) {
                append("<h3>Peringkat Penjualan per Wilayah</h3>")
                append("<table><thead><tr><th>No</th><th>Wilayah</th><th class='right'>Total Galon</th><th class='right'>Jumlah Transaksi</th><th class='right'>Omzet Nota</th><th class='right'>Kontribusi</th></tr></thead><tbody>")
                effectiveDesaList.forEachIndexed { idx, d ->
                    append("<tr>")
                    append("<td>${idx + 1}</td>")
                    append("<td>${esc(d.title)}</td>")
                    append("<td class='right'>${d.totalGalon}</td>")
                    append("<td class='right'>${d.totalTransaksi}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatRupiah(d.totalPendapatan))}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatPercent(d.omzetSharePercent))}</td>")
                    append("</tr>")
                }
                append("</tbody></table>")
            }

            // 7. Tabel Transaksi (Ringkas Harian atau Rincian Penuh)
            if (isSummaryMode) {
                val dailyRows = LaporanFinanceCalculator.buildDailySummaryRows(
                    summary = summary,
                    allTransaksi = allTransaksi,
                    allKasbonLogs = allKasbonLogs
                )
                append("<h3>Ringkasan Arus Kas Harian (${dailyRows.size} Hari)</h3>")
                append("<table><thead><tr><th>Tanggal</th><th class='right'>Galon</th><th class='right'>Omzet Nota</th><th class='right'>Pelunasan Kasbon</th><th class='right'>Total Kas Masuk</th><th class='right'>Beban &amp; Komisi</th><th class='right'>Laba Harian</th></tr></thead><tbody>")
                dailyRows.forEach { row ->
                    append("<tr>")
                    append("<td>${esc(row.fullDateLabel)}</td>")
                    append("<td class='right'>${row.totalGalon}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatRupiah(row.totalOmzet))}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatRupiah(row.kasbonMasuk))}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatRupiah(row.kasMasuk))}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatRupiah(row.bebanKeluar))}</td>")
                    append("<td class='right'><b>${esc(FormatUtils.formatRupiah(row.labaBersih))}</b></td>")
                    append("</tr>")
                }
                append("<tr class='total-row'>")
                append("<td>Total Periode</td>")
                append("<td class='right'>${summary.totalGalon}</td>")
                append("<td class='right'>${esc(FormatUtils.formatRupiah(summary.totalOmzetPenjualan))}</td>")
                append("<td class='right'>${esc(FormatUtils.formatRupiah(summary.pelunasanKasbonPeriode))}</td>")
                append("<td class='right'>${esc(FormatUtils.formatRupiah(summary.totalKasMasuk))}</td>")
                append("<td class='right'>${esc(FormatUtils.formatRupiah(summary.totalBebanKeluar))}</td>")
                append("<td class='right'>${esc(FormatUtils.formatRupiah(summary.labaBersih))}</td>")
                append("</tr>")
                append("</tbody></table>")
            } else {
                append("<h3>Daftar Transaksi Penjualan (${summary.filteredTx.size})</h3>")
                if (summary.filteredTx.isEmpty()) {
                    append("<p class='muted'>Tidak ada transaksi pada periode ini.</p>")
                } else {
                    append("<table><thead><tr><th>No</th><th>Tanggal</th><th>Pelanggan</th><th>Produk</th><th class='right'>Jml</th><th class='right'>Total</th><th>Metode &amp; Status</th></tr></thead><tbody>")
                    summary.filteredTx.forEachIndexed { idx, item ->
                        val trx = item.transaksi
                        val custName = item.pelanggan?.nama?.trim()?.ifBlank { "Umum" } ?: "Umum"
                        val prodName = item.produk?.nama_produk ?: "Galon RO"
                        val statusDetail = if (trx.status.equals("Kasbon", ignoreCase = true) && trx.uangMuka > 0L) {
                            "Kasbon (Terbayar: ${FormatUtils.formatRupiah(trx.uangMuka.toDouble())} • Sisa: ${FormatUtils.formatRupiah(trx.remainingKasbonAmount())})"
                        } else {
                            "${FormatUtils.formatMetodeBayarLabel(trx.metode_bayar)} • ${trx.status}"
                        }
                        append("<tr>")
                        append("<td>${idx + 1}</td>")
                        append("<td>${esc(FormatUtils.formatDateTimeTable(trx.tanggal))}</td>")
                        append("<td>${esc(custName)}</td>")
                        append("<td>${esc(prodName)}</td>")
                        append("<td class='right'>${trx.jumlah}</td>")
                        append("<td class='right'>${esc(FormatUtils.formatRupiah(trx.total_harga))}</td>")
                        append("<td>${esc(statusDetail)}</td>")
                        append("</tr>")
                    }
                    append("</tbody></table>")
                }
            }

            // 8. Riwayat Pelunasan Kasbon
            if (summary.filteredKasbonLogs.isNotEmpty()) {
                append("<h3>Riwayat Pelunasan &amp; Cicilan Kasbon (${summary.filteredKasbonLogs.size})</h3>")
                append("<table><thead><tr><th>No</th><th>Tanggal</th><th>Pelanggan</th><th>Metode</th><th class='right'>Nominal Bayar</th><th>Catatan</th></tr></thead><tbody>")
                summary.filteredKasbonLogs.forEachIndexed { idx, item ->
                    append("<tr>")
                    append("<td>${idx + 1}</td>")
                    append("<td>${esc(FormatUtils.formatDateTimeTable(item.log.tanggal))}</td>")
                    append("<td>${esc(item.namaPelanggan)}</td>")
                    append("<td>${esc(FormatUtils.formatMetodeBayarLabel(item.log.metode_bayar))}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatRupiah(item.log.jumlah_bayar))}</td>")
                    append("<td>${esc(item.log.catatan)}</td>")
                    append("</tr>")
                }
                append("</tbody></table>")
            }

            // 9. Rincian Pengeluaran Operasional
            if (summary.filteredExp.isNotEmpty()) {
                append("<h3>Rincian Pengeluaran Operasional (${summary.filteredExp.size})</h3>")
                append("<table><thead><tr><th>No</th><th>Tanggal</th><th>Kategori</th><th>Item Pengeluaran</th><th class='right'>Nominal</th></tr></thead><tbody>")
                summary.filteredExp.forEachIndexed { idx, exp ->
                    append("<tr>")
                    append("<td>${idx + 1}</td>")
                    append("<td>${esc(FormatUtils.formatDateShort(exp.tanggal))}</td>")
                    append("<td>${esc(exp.kategori)}</td>")
                    append("<td>${esc(exp.nama_item)}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatRupiah(exp.nominal))}</td>")
                    append("</tr>")
                }
                append("</tbody></table>")
            }

            // 10. Rekap Komisi Kurir
            if (summary.kurirEntries.isNotEmpty()) {
                append("<h3>Rekapitulasi Komisi &amp; Pengiriman Kurir</h3>")
                append("<table><thead><tr><th>Kurir</th><th class='right'>Galon</th><th class='right'>Jumlah Kirim</th><th class='right'>Omzet</th><th class='right'>Total Komisi</th></tr></thead><tbody>")
                summary.kurirEntries.sortedByDescending { it.totalGalon }.forEach { k ->
                    append("<tr>")
                    append("<td>${esc(k.title)}</td>")
                    append("<td class='right'>${k.totalGalon}</td>")
                    append("<td class='right'>${k.totalTransaksi}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatRupiah(k.totalPendapatan))}</td>")
                    append("<td class='right'>${esc(FormatUtils.formatRupiah(k.totalKomisi))}</td>")
                    append("</tr>")
                }
                append("</tbody></table>")
            }

            // 11. Lembar Validasi Tanda Tangan
            append("<div class='signature-container'>")
            append("<div class='signature-box'>")
            append("<div class='signature-role'>Dibuat Oleh:</div>")
            append("<div class='signature-space'></div>")
            append("<div class='signature-line'></div>")
            append("<div class='signature-name'>Admin / Kasir</div>")
            append("</div>")
            append("<div class='signature-box'>")
            append("<div class='signature-role'>Disetujui Oleh:</div>")
            append("<div class='signature-space'></div>")
            append("<div class='signature-line'></div>")
            append("<div class='signature-name'>${esc(ownerSignatureName)}</div>")
            append("</div>")
            append("</div>")

            if (!isProUser) {
                append("<div class='pdf-watermark'>${esc(DepotProfile.WATERMARK_TEXT)}</div>")
            }

            append("</body></html>")
        }
    }

    fun shareExportFile(context: Context, filename: String, content: String, mimeType: String) {
        val backupDir = File(context.cacheDir, "backups")
        backupDir.mkdirs()
        pruneStaleBackupFiles(backupDir)
        val safeFilename = File(filename).name.ifBlank { "Laporan_Santoya.txt" }
        val file = File(backupDir, safeFilename)
        FileOutputStream(file).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
            fos.flush()
        }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Cadangan Data Santoya RO: $safeFilename")
            putExtra(Intent.EXTRA_TEXT, "Cadangan data Santoya RO ($safeFilename)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Simpan / Bagikan Cadangan")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Membaca isi teks dari [uri] dengan batas ukuran maksimum [maxBytes] (default 10 MB)
     * untuk mencegah OutOfMemoryError / Denial-of-Service dari berkas eksternal berukuran besar.
     */
    fun readTextFromUri(
        context: Context,
        uri: Uri,
        maxBytes: Int = MAX_IMPORT_FILE_BYTES
    ): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var totalRead = 0
                while (true) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    totalRead += read
                    if (totalRead > maxBytes) {
                        return null
                    }
                    out.write(buffer, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            }
        } catch (e: Exception) {
            null
        }
    }

    fun createAutoBackupBeforeImport(
        context: Context,
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>,
        produkList: List<Produk>,
        kurirList: List<Kurir>,
        pengeluaranList: List<Pengeluaran>,
        kasbonLogList: List<KasbonLog> = emptyList(),
        perawatanList: List<PerawatanItem> = emptyList(),
        perawatanLogList: List<PerawatanLog> = emptyList(),
        depotProfile: DepotProfile? = null
    ) {
        val json = generateNestedJson(
            pelangganList = pelangganList,
            transaksiList = transaksiList,
            produkList = produkList,
            kurirList = kurirList,
            pengeluaranList = pengeluaranList,
            kasbonLogList = kasbonLogList,
            perawatanList = perawatanList,
            perawatanLogList = perawatanLogList,
            depotProfile = depotProfile
        )
        context.openFileOutput(AUTO_BACKUP_FILE_NAME, Context.MODE_PRIVATE).use { fos ->
            fos.write(json.toByteArray(Charsets.UTF_8))
            fos.flush()
        }
    }

    /**
     * Membuat snapshot cadangan otomatis (`AUTO_BACKUP_FILE_NAME`) langsung dari isi database Room saat ini
     * sebelum pembersihan data lokal (logout atau pindah depot) ketika masih ada data lokal/antrean tertunda.
     */
    suspend fun createAutoBackupFromDatabase(
        context: Context,
        db: AppDatabase,
        depotProfile: DepotProfile? = null
    ): Boolean {
        return try {
            val pelangganList = db.pelangganDao().getAllOnce()
            val produkList = db.produkDao().getAllOnce()
            val kurirList = db.kurirDao().getAllOnce()
            val rawTransaksi = db.transaksiDao().getAllOnce()
            val pengeluaranList = db.pengeluaranDao().getAllOnce()
            val kasbonLogList = db.kasbonLogDao().getAllOnce()
            val perawatanList = db.perawatanDao().getAllOnce()
            val perawatanLogList = db.perawatanLogDao().getAllOnce()

            if (pelangganList.isEmpty() && rawTransaksi.isEmpty() && pengeluaranList.isEmpty()) {
                return false
            }

            val pelangganById = pelangganList.associateBy { it.id }
            val produkById = produkList.associateBy { it.id }
            val kurirById = kurirList.associateBy { it.id }

            val fallbackPelanggan = Pelanggan(id = 0, nama = "Pelanggan", alamat = "", no_hp = "")
            val fallbackProduk = Produk(id = 1, nama_produk = "Galon Air RO 19L", harga = 6000.0)

            val transaksiDetails = rawTransaksi.map { tx ->
                TransaksiDetail(
                    transaksi = tx,
                    pelanggan = pelangganById[tx.id_pelanggan] ?: fallbackPelanggan,
                    kurir = tx.id_kurir?.let { kurirById[it] },
                    produk = tx.id_produk?.let { produkById[it] } ?: fallbackProduk
                )
            }

            createAutoBackupBeforeImport(
                context = context,
                pelangganList = pelangganList,
                transaksiList = transaksiDetails,
                produkList = produkList,
                kurirList = kurirList,
                pengeluaranList = pengeluaranList,
                kasbonLogList = kasbonLogList,
                perawatanList = perawatanList,
                perawatanLogList = perawatanLogList,
                depotProfile = depotProfile
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasAutoBackup(context: Context): Boolean {
        val file = File(context.filesDir, AUTO_BACKUP_FILE_NAME)
        return file.exists() && file.length() in 1..MAX_IMPORT_FILE_BYTES.toLong()
    }

    fun readAutoBackupJson(context: Context): String? {
        val file = File(context.filesDir, AUTO_BACKUP_FILE_NAME)
        if (!file.exists() || file.length() > MAX_IMPORT_FILE_BYTES.toLong()) return null
        return file.readText(Charsets.UTF_8)
    }

    private data class NormalizedTxMath(
        val jumlah: Int,
        val hargaSatuan: Double,
        val totalHarga: Double,
        val totalKomisi: Double,
        val metode: String,
        val status: String,
        val uangMuka: Long,
        val sisaKasbon: Long,
        val tanggalBayar: Long?
    )

    private fun normalizeImportedTransaksiMath(
        tgl: Long,
        rawJumlah: Int,
        rawHargaSatuan: Double,
        rawTotalHarga: Double,
        rawTotalKomisi: Double,
        rawMetode: String,
        rawStatus: String,
        rawUangMuka: Long,
        rawSisaKasbon: Long,
        rawTanggalBayar: Long? = null
    ): NormalizedTxMath {
        val safeJumlah = rawJumlah.coerceAtLeast(1)
        val safeHarga = if (rawHargaSatuan.isFinite()) rawHargaSatuan.coerceAtLeast(0.0) else 0.0
        val expectedTotal = safeJumlah * safeHarga
        val safeTotal = if (rawTotalHarga.isFinite() && abs(rawTotalHarga - expectedTotal) <= 0.01) {
            rawTotalHarga.coerceAtLeast(0.0)
        } else {
            expectedTotal
        }
        val maxTotalLong = safeTotal.roundToLong().coerceAtLeast(0L)
        val safeKomisi = if (rawTotalKomisi.isFinite()) rawTotalKomisi.coerceAtLeast(0.0) else 0.0
        val canonicalStatus = when {
            rawStatus.equals(KaryawanScopeHelper.STATUS_DIHAPUS, ignoreCase = true) -> KaryawanScopeHelper.STATUS_DIHAPUS
            rawStatus.equals("Kasbon", ignoreCase = true) -> "Kasbon"
            else -> "Lunas"
        }
        val clampedDp = rawUangMuka.coerceIn(0L, maxTotalLong)
        val computedSisa = when (canonicalStatus) {
            "Kasbon" -> (safeTotal - clampedDp.toDouble()).roundToLong().coerceAtLeast(0L)
            else -> 0L
        }
        val finalStatus = if (canonicalStatus == "Kasbon" && computedSisa == 0L && rawSisaKasbon <= 0L) {
            "Lunas"
        } else {
            canonicalStatus
        }
        val finalSisa = if (finalStatus == "Kasbon") computedSisa else 0L
        val cleanMetode = rawMetode.trim().ifBlank {
            if (finalStatus == "Kasbon") "-" else "Cash"
        }.take(40)
        val tanggalBayar = if (finalStatus == "Lunas") {
            rawTanggalBayar?.takeIf { it > 0L } ?: tgl
        } else {
            null
        }
        return NormalizedTxMath(
            jumlah = safeJumlah,
            hargaSatuan = safeHarga,
            totalHarga = safeTotal,
            totalKomisi = safeKomisi,
            metode = cleanMetode,
            status = finalStatus,
            uangMuka = clampedDp,
            sisaKasbon = finalSisa,
            tanggalBayar = tanggalBayar
        )
    }

    private fun monotonicImportTime(existingUpdatedAt: Long, importNow: Long): Long {
        return if (importNow > existingUpdatedAt) importNow else existingUpdatedAt + 1L
    }

    suspend fun importJsonData(
        context: Context,
        db: com.example.data.database.AppDatabase,
        jsonString: String
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            if (jsonString.toByteArray(Charsets.UTF_8).size > MAX_IMPORT_FILE_BYTES) {
                return@withContext ImportResult(
                    success = false,
                    message = "Ukuran file cadangan terlalu besar (maksimal 10 MB)"
                )
            }
            val trimmed = jsonString.trim()
            val parsedRoot: JSONObject? = if (trimmed.startsWith("{")) JSONObject(trimmed) else null
            val parsedArray: JSONArray? = if (trimmed.startsWith("[")) JSONArray(trimmed) else null
            if (parsedRoot == null && parsedArray == null) {
                return@withContext ImportResult(success = false, message = "Format file cadangan tidak sesuai")
            }

            val oldP = db.pelangganDao().getAll().first()
            val oldTx = db.transaksiDao().getAllWithDetail().first()
            val oldPr = db.produkDao().getAll().first()
            val oldK = db.kurirDao().getAll().first()
            val oldExp = db.pengeluaranDao().getAll().first()
            val oldKasbonLogs = db.kasbonLogDao().getAllOnce()
            val oldPerawatan = db.perawatanDao().getAllOnce()
            val oldPerawatanLogs = db.perawatanLogDao().getAllOnce()
            if (oldP.isNotEmpty() || oldTx.isNotEmpty() || oldPerawatan.isNotEmpty()) {
                createAutoBackupBeforeImport(
                    context = context,
                    pelangganList = oldP,
                    transaksiList = oldTx,
                    produkList = oldPr,
                    kurirList = oldK,
                    pengeluaranList = oldExp,
                    kasbonLogList = oldKasbonLogs,
                    perawatanList = oldPerawatan,
                    perawatanLogList = oldPerawatanLogs
                )
            }

            var pCount = 0
            var txCount = 0
            var prodCount = 0
            var kurirCount = 0
            var expCount = 0
            var kasbonLogCount = 0
            var perawatanCount = 0
            var perawatanLogCount = 0

            db.withTransaction {
                val importNow = System.currentTimeMillis()
                val currentProducts = db.produkDao().getAll().first().toMutableList()
                val allDbKurirs = db.kurirDao().getAll().first().toMutableList()

                // Jika di database terdapat kurir placeholder ("Kurir 1", "Kurir 2") akibat bug impor sebelumnya,
                // nonaktifkan secara soft-delete apabila sudah ada kurir bernama asli agar ikut tersinkron ke Cloud.
                val realDbKurirs = allDbKurirs.filter { !it.isDeleted && !isGenericFakeKurirName(it.nama) }
                val fakeDbKurirs = allDbKurirs.filter { !it.isDeleted && isGenericFakeKurirName(it.nama) }
                if (realDbKurirs.isNotEmpty() && fakeDbKurirs.isNotEmpty()) {
                    fakeDbKurirs.forEach { fake ->
                        val softDeletedFake = fake.copy(
                            akses_aktif = false,
                            isDeleted = true,
                            updatedAt = monotonicImportTime(fake.updatedAt, importNow)
                        )
                        db.kurirDao().update(softDeletedFake)
                        val idx = allDbKurirs.indexOfFirst { it.id == fake.id }
                        if (idx >= 0) allDbKurirs[idx] = softDeletedFake
                    }
                }
                val currentKurirs = allDbKurirs

                if (currentProducts.isEmpty()) {
                    val defProd = Produk(nama_produk = "Galon Air RO 19L", harga = 6000.0, updatedAt = importNow)
                    val defId = db.produkDao().insert(defProd).toInt()
                    currentProducts.add(defProd.copy(id = defId))
                }

                val jsonProdukIdMap = mutableMapOf<Int, Int>()
                val jsonKurirIdMap = mutableMapOf<Int, Int>()
                val jsonPelangganIdMap = mutableMapOf<Int, Int>()
                val pelangganSyncIdMap = mutableMapOf<String, Int>()
                val existingCustomers = db.pelangganDao().getAll().first().toMutableList()
                val existingTransactions = db.transaksiDao().getAllOnce().toMutableList()
                val existingExpenses = db.pengeluaranDao().getAll().first().toMutableList()
                val existingKasbonLogs = db.kasbonLogDao().getAllOnce().toMutableList()
                val existingPerawatan = db.perawatanDao().getAllOnce().toMutableList()
                val existingPerawatanLogs = db.perawatanLogDao().getAllOnce().toMutableList()

                existingCustomers.forEach { c ->
                    pelangganSyncIdMap[c.syncId] = c.id
                }

                suspend fun upsertImportedPelanggan(
                    oldId: Int,
                    nama: String,
                    alamat: String,
                    noHp: String,
                    kasbon: Double,
                    hKhusus: Double?,
                    idKurir: Int?,
                    rawSyncId: String? = null
                ): Int {
                    val cleanNama = nama.trim().ifBlank { "Pelanggan" }.take(120)
                    val cleanAlamat = alamat.trim().take(300)
                    val cleanHp = noHp.trim().take(40)
                    val cleanSyncId = sanitizeSyncId(rawSyncId)
                    val safeKasbon = if (kasbon.isFinite()) kasbon.coerceAtLeast(0.0) else 0.0
                    val safeHKhusus = hKhusus?.takeIf { it.isFinite() && it >= 0.0 }
                    val match = (if (cleanSyncId != null) existingCustomers.find { it.syncId == cleanSyncId } else null)
                        ?: (if (oldId > 0) existingCustomers.find { it.id == oldId && it.nama.equals(cleanNama, ignoreCase = true) } else null)
                        ?: existingCustomers.find {
                            it.nama.equals(cleanNama, ignoreCase = true) &&
                                it.alamat.trim().equals(cleanAlamat, ignoreCase = true) &&
                                it.no_hp.trim().equals(cleanHp, ignoreCase = true)
                        }
                    if (match != null) {
                        val updated = match.copy(
                            nama = cleanNama,
                            alamat = cleanAlamat,
                            no_hp = cleanHp,
                            sisa_kasbon = safeKasbon,
                            harga_khusus = safeHKhusus ?: match.harga_khusus,
                            id_kurir_langganan = idKurir ?: match.id_kurir_langganan,
                            isDeleted = false,
                            deletedBy = null,
                            updatedAt = monotonicImportTime(match.updatedAt, importNow)
                        )
                        db.pelangganDao().update(updated)
                        val idx = existingCustomers.indexOfFirst { it.id == match.id }
                        if (idx >= 0) existingCustomers[idx] = updated
                        if (oldId > 0) jsonPelangganIdMap[oldId] = match.id
                        pelangganSyncIdMap[updated.syncId] = match.id
                        if (cleanSyncId != null) pelangganSyncIdMap[cleanSyncId] = match.id
                        pCount++
                        return match.id
                    }
                    val chosenSyncId = if (cleanSyncId != null && existingCustomers.none { it.syncId == cleanSyncId }) {
                        cleanSyncId
                    } else {
                        java.util.UUID.randomUUID().toString()
                    }
                    val toInsert = if (oldId > 0 && existingCustomers.none { it.id == oldId }) {
                        Pelanggan(
                            id = oldId,
                            nama = cleanNama,
                            alamat = cleanAlamat,
                            no_hp = cleanHp,
                            sisa_kasbon = safeKasbon,
                            harga_khusus = safeHKhusus,
                            id_kurir_langganan = idKurir,
                            updatedAt = importNow,
                            syncId = chosenSyncId
                        )
                    } else {
                        Pelanggan(
                            nama = cleanNama,
                            alamat = cleanAlamat,
                            no_hp = cleanHp,
                            sisa_kasbon = safeKasbon,
                            harga_khusus = safeHKhusus,
                            id_kurir_langganan = idKurir,
                            updatedAt = importNow,
                            syncId = chosenSyncId
                        )
                    }
                    val newId = db.pelangganDao().insert(toInsert).toInt()
                    val insertedCustomer = toInsert.copy(id = newId)
                    existingCustomers.add(insertedCustomer)
                    if (oldId > 0) jsonPelangganIdMap[oldId] = newId
                    pelangganSyncIdMap[insertedCustomer.syncId] = newId
                    if (cleanSyncId != null) pelangganSyncIdMap[cleanSyncId] = newId
                    pCount++
                    return newId
                }

                suspend fun upsertImportedTransaksi(
                    oldTxId: Int,
                    tgl: Long,
                    custId: Int,
                    kurirId: Int?,
                    prodId: Int,
                    jumlah: Int,
                    hargaSatuan: Double,
                    totalHarga: Double,
                    totalKomisi: Double,
                    metode: String,
                    status: String,
                    uangMuka: Long,
                    sisaKasbon: Long,
                    rawTanggalBayar: Long? = null,
                    rawSyncId: String? = null,
                    rawWaterType: String? = null
                ) {
                    val cleanSyncId = sanitizeSyncId(rawSyncId)
                    val cleanWaterType = if (rawWaterType.equals(Transaksi.WATER_TYPE_MINERAL, ignoreCase = true)) {
                        Transaksi.WATER_TYPE_MINERAL
                    } else {
                        Transaksi.WATER_TYPE_RO
                    }
                    val normalized = normalizeImportedTransaksiMath(
                        tgl = tgl,
                        rawJumlah = jumlah,
                        rawHargaSatuan = hargaSatuan,
                        rawTotalHarga = totalHarga,
                        rawTotalKomisi = totalKomisi,
                        rawMetode = metode,
                        rawStatus = status,
                        rawUangMuka = uangMuka,
                        rawSisaKasbon = sisaKasbon,
                        rawTanggalBayar = rawTanggalBayar
                    )
                    val match = (if (cleanSyncId != null) existingTransactions.find { it.syncId == cleanSyncId } else null)
                        ?: (if (oldTxId > 0) existingTransactions.find { it.id == oldTxId && it.tanggal == tgl } else null)
                        ?: existingTransactions.find {
                            it.id_pelanggan == custId &&
                                it.tanggal == tgl &&
                                it.jumlah == normalized.jumlah &&
                                abs(it.total_harga - normalized.totalHarga) <= 0.01
                        }
                    if (match != null) {
                        val updated = match.copy(
                            tanggal = tgl,
                            id_pelanggan = custId,
                            id_kurir = kurirId,
                            id_produk = prodId,
                            jumlah = normalized.jumlah,
                            harga_satuan = normalized.hargaSatuan,
                            total_harga = normalized.totalHarga,
                            total_komisi = normalized.totalKomisi,
                            metode_bayar = normalized.metode,
                            status = normalized.status,
                            uangMuka = normalized.uangMuka,
                            sisaKasbon = normalized.sisaKasbon,
                            waterType = cleanWaterType,
                            tanggalBayar = rawTanggalBayar?.takeIf { it > 0L }
                                ?: if (normalized.status == "Lunas") (match.tanggalBayar ?: normalized.tanggalBayar) else null,
                            isDeleted = normalized.status == KaryawanScopeHelper.STATUS_DIHAPUS,
                            updatedAt = monotonicImportTime(match.updatedAt, importNow)
                        )
                        db.transaksiDao().update(updated)
                        val idx = existingTransactions.indexOfFirst { it.id == match.id }
                        if (idx >= 0) existingTransactions[idx] = updated
                        txCount++
                        return
                    }
                    val chosenSyncId = if (cleanSyncId != null && existingTransactions.none { it.syncId == cleanSyncId }) {
                        cleanSyncId
                    } else {
                        java.util.UUID.randomUUID().toString()
                    }
                    val toInsert = if (oldTxId > 0 && existingTransactions.none { it.id == oldTxId }) {
                        Transaksi(
                            id = oldTxId,
                            tanggal = tgl,
                            id_pelanggan = custId,
                            id_kurir = kurirId,
                            id_produk = prodId,
                            jumlah = normalized.jumlah,
                            harga_satuan = normalized.hargaSatuan,
                            total_harga = normalized.totalHarga,
                            total_komisi = normalized.totalKomisi,
                            metode_bayar = normalized.metode,
                            status = normalized.status,
                            uangMuka = normalized.uangMuka,
                            sisaKasbon = normalized.sisaKasbon,
                            waterType = cleanWaterType,
                            tanggalBayar = normalized.tanggalBayar,
                            isDeleted = normalized.status == KaryawanScopeHelper.STATUS_DIHAPUS,
                            updatedAt = importNow,
                            syncId = chosenSyncId
                        )
                    } else {
                        Transaksi(
                            tanggal = tgl,
                            id_pelanggan = custId,
                            id_kurir = kurirId,
                            id_produk = prodId,
                            jumlah = normalized.jumlah,
                            harga_satuan = normalized.hargaSatuan,
                            total_harga = normalized.totalHarga,
                            total_komisi = normalized.totalKomisi,
                            metode_bayar = normalized.metode,
                            status = normalized.status,
                            uangMuka = normalized.uangMuka,
                            sisaKasbon = normalized.sisaKasbon,
                            waterType = cleanWaterType,
                            tanggalBayar = normalized.tanggalBayar,
                            isDeleted = normalized.status == KaryawanScopeHelper.STATUS_DIHAPUS,
                            updatedAt = importNow,
                            syncId = chosenSyncId
                        )
                    }
                    val newId = db.transaksiDao().insert(toInsert).toInt()
                    existingTransactions.add(toInsert.copy(id = newId))
                    txCount++
                }

                suspend fun upsertRealProduk(
                    oldId: Int,
                    name: String,
                    price: Double = 6000.0,
                    rawSyncId: String? = null
                ): Int {
                    val cleanName = name.trim().ifBlank { "Galon Air RO 19L" }.take(120)
                    val cleanSyncId = sanitizeSyncId(rawSyncId)
                    val safePrice = if (price.isFinite() && price >= 0.0) price else 6000.0
                    val matchBySyncOrName = (if (cleanSyncId != null) currentProducts.find { it.syncId == cleanSyncId } else null)
                        ?: currentProducts.find { it.nama_produk.equals(cleanName, ignoreCase = true) }
                    if (matchBySyncOrName != null) {
                        if (safePrice > 0.0 && (matchBySyncOrName.harga != safePrice || !matchBySyncOrName.nama_produk.equals(cleanName, ignoreCase = true))) {
                            val updated = matchBySyncOrName.copy(
                                nama_produk = cleanName,
                                harga = safePrice,
                                updatedAt = monotonicImportTime(matchBySyncOrName.updatedAt, importNow)
                            )
                            db.produkDao().update(updated)
                            val idx = currentProducts.indexOfFirst { it.id == matchBySyncOrName.id }
                            if (idx >= 0) currentProducts[idx] = updated
                        }
                        if (oldId > 0) jsonProdukIdMap[oldId] = matchBySyncOrName.id
                        return matchBySyncOrName.id
                    }
                    val matchById = if (oldId > 0) currentProducts.find { it.id == oldId } else null
                    if (matchById != null) {
                        val updated = matchById.copy(
                            nama_produk = cleanName,
                            harga = if (safePrice > 0.0) safePrice else matchById.harga,
                            updatedAt = monotonicImportTime(matchById.updatedAt, importNow)
                        )
                        db.produkDao().update(updated)
                        val idx = currentProducts.indexOfFirst { it.id == matchById.id }
                        if (idx >= 0) currentProducts[idx] = updated
                        jsonProdukIdMap[oldId] = matchById.id
                        return matchById.id
                    }
                    val chosenSyncId = if (cleanSyncId != null && currentProducts.none { it.syncId == cleanSyncId }) {
                        cleanSyncId
                    } else {
                        java.util.UUID.randomUUID().toString()
                    }
                    val toInsert = if (oldId > 0 && currentProducts.none { it.id == oldId }) {
                        Produk(id = oldId, nama_produk = cleanName, harga = safePrice, updatedAt = importNow, syncId = chosenSyncId)
                    } else {
                        Produk(nama_produk = cleanName, harga = safePrice, updatedAt = importNow, syncId = chosenSyncId)
                    }
                    val newId = db.produkDao().insert(toInsert).toInt()
                    val newP = toInsert.copy(id = newId)
                    currentProducts.add(newP)
                    if (oldId > 0) jsonProdukIdMap[oldId] = newId
                    prodCount++
                    return newId
                }

                suspend fun upsertRealKurir(
                    oldId: Int,
                    name: String,
                    komisi: Double = 1000.0,
                    email: String = "",
                    aksesAktif: Boolean = true,
                    rawSyncId: String? = null
                ): Int? {
                    val cleanName = name.trim().take(120)
                    val cleanEmail = sanitizeImportedCourierEmail(email)
                    val cleanSyncId = sanitizeSyncId(rawSyncId)
                    val safeKomisi = if (komisi.isFinite() && komisi >= 0.0) komisi else 1000.0
                    if (isGenericFakeKurirName(cleanName)) {
                        // Jangan pernah membuat nama palsu seperti "Kurir 1" atau "Kurir 2".
                        // Petakan ke kurir asli berdasarkan ID jika sudah ada di database.
                        if (oldId > 0) {
                            jsonKurirIdMap[oldId]?.let { return it }
                            val existingById = currentKurirs.find {
                                it.id == oldId && !it.isDeleted && !isGenericFakeKurirName(it.nama)
                            }
                            if (existingById != null) {
                                if (safeKomisi > 0.0 && existingById.komisi_per_galon != safeKomisi) {
                                    val updated = existingById.copy(
                                        komisi_per_galon = safeKomisi,
                                        updatedAt = monotonicImportTime(existingById.updatedAt, importNow)
                                    )
                                    db.kurirDao().update(updated)
                                    val idx = currentKurirs.indexOfFirst { it.id == existingById.id }
                                    if (idx >= 0) currentKurirs[idx] = updated
                                }
                                jsonKurirIdMap[oldId] = existingById.id
                                return existingById.id
                            }
                            val usedTargetIds = jsonKurirIdMap.values.toSet()
                            val fallbackReal = currentKurirs.firstOrNull {
                                !it.isDeleted && !isGenericFakeKurirName(it.nama) && it.id !in usedTargetIds
                            }
                            if (fallbackReal != null) {
                                jsonKurirIdMap[oldId] = fallbackReal.id
                                return fallbackReal.id
                            }
                        }
                        return null
                    }

                    // Nama kurir asli (misal: "Mustopa", "Radi")
                    val matchBySyncOrName = (if (cleanSyncId != null) currentKurirs.find { it.syncId == cleanSyncId } else null)
                        ?: currentKurirs.find { it.nama.equals(cleanName, ignoreCase = true) }
                    if (matchBySyncOrName != null) {
                        val effectiveEmail = if (cleanEmail.isNotBlank()) cleanEmail else matchBySyncOrName.email
                        val effectiveAktif = if (cleanEmail.isNotBlank()) aksesAktif else matchBySyncOrName.akses_aktif
                        if (matchBySyncOrName.isDeleted ||
                            (safeKomisi > 0.0 && matchBySyncOrName.komisi_per_galon != safeKomisi) ||
                            effectiveEmail != matchBySyncOrName.email ||
                            effectiveAktif != matchBySyncOrName.akses_aktif ||
                            matchBySyncOrName.skema_komisi != "TETAP" ||
                            matchBySyncOrName.aturan_komisi.isNotEmpty()
                        ) {
                            val updated = matchBySyncOrName.copy(
                                nama = cleanName,
                                komisi_per_galon = if (safeKomisi > 0.0) safeKomisi else matchBySyncOrName.komisi_per_galon,
                                email = effectiveEmail,
                                akses_aktif = effectiveAktif,
                                isDeleted = false,
                                skema_komisi = "TETAP",
                                aturan_komisi = "",
                                updatedAt = monotonicImportTime(matchBySyncOrName.updatedAt, importNow)
                            )
                            db.kurirDao().update(updated)
                            val idx = currentKurirs.indexOfFirst { it.id == matchBySyncOrName.id }
                            if (idx >= 0) currentKurirs[idx] = updated
                        }
                        if (oldId > 0) jsonKurirIdMap[oldId] = matchBySyncOrName.id
                        return matchBySyncOrName.id
                    }

                    val chosenSyncId = if (cleanSyncId != null && currentKurirs.none { it.syncId == cleanSyncId }) {
                        cleanSyncId
                    } else {
                        java.util.UUID.randomUUID().toString()
                    }

                    if (oldId > 0) {
                        val existingById = currentKurirs.find { it.id == oldId }
                        if (existingById != null) {
                            // Update in-place agar ID tetap sama dan tidak memicu ForeignKey.SET_NULL
                            val updated = existingById.copy(
                                nama = cleanName,
                                komisi_per_galon = if (safeKomisi > 0.0) safeKomisi else existingById.komisi_per_galon,
                                email = if (cleanEmail.isNotBlank()) cleanEmail else existingById.email,
                                akses_aktif = if (cleanEmail.isNotBlank()) aksesAktif else existingById.akses_aktif,
                                isDeleted = false,
                                skema_komisi = "TETAP",
                                aturan_komisi = "",
                                updatedAt = monotonicImportTime(existingById.updatedAt, importNow)
                            )
                            db.kurirDao().update(updated)
                            val idx = currentKurirs.indexOfFirst { it.id == oldId }
                            if (idx >= 0) currentKurirs[idx] = updated
                            jsonKurirIdMap[oldId] = oldId
                            kurirCount++
                            return oldId
                        } else {
                            val toInsertK = Kurir(
                                id = oldId,
                                nama = cleanName,
                                komisi_per_galon = if (safeKomisi > 0.0) safeKomisi else 1000.0,
                                email = cleanEmail,
                                akses_aktif = aksesAktif,
                                skema_komisi = "TETAP",
                                aturan_komisi = "",
                                updatedAt = importNow,
                                syncId = chosenSyncId
                            )
                            val newId = db.kurirDao().insert(toInsertK).toInt()
                            currentKurirs.add(toInsertK.copy(id = newId))
                            jsonKurirIdMap[oldId] = newId
                            kurirCount++
                            return newId
                        }
                    }

                    val toInsertK = Kurir(
                        nama = cleanName,
                        komisi_per_galon = if (safeKomisi > 0.0) safeKomisi else 1000.0,
                        email = cleanEmail,
                        akses_aktif = aksesAktif,
                        skema_komisi = "TETAP",
                        aturan_komisi = "",
                        updatedAt = importNow,
                        syncId = chosenSyncId
                    )
                    val newId = db.kurirDao().insert(toInsertK).toInt()
                    currentKurirs.add(toInsertK.copy(id = newId))
                    kurirCount++
                    return newId
                }

                fun extractKurirIdFromObj(obj: JSONObject, vararg keys: String): Int {
                    for (key in keys) {
                        if (obj.has(key) && !obj.isNull(key)) {
                            val v = obj.optInt(key, -1)
                            if (v > 0) return v
                        }
                    }
                    return -1
                }

                fun extractOptionalLong(obj: JSONObject, vararg keys: String): Long? {
                    for (key in keys) {
                        if (obj.has(key) && !obj.isNull(key)) {
                            val v = obj.optLong(key, -1L)
                            if (v > 0L) return v
                        }
                    }
                    return null
                }

                fun extractOptionalString(obj: JSONObject, vararg keys: String): String? {
                    for (key in keys) {
                        if (obj.has(key) && !obj.isNull(key)) {
                            val v = obj.optString(key, "").trim()
                            if (v.isNotEmpty() && !v.equals("null", ignoreCase = true)) return v
                        }
                    }
                    return null
                }

                suspend fun resolveTransactionKurirId(tObj: JSONObject, fallbackCustomerKurirId: Int?): Int? {
                    val rawIdKurir = extractKurirIdFromObj(tObj, "id_kurir", "kurirId", "kurir_id")
                    val rawKurirName = when {
                        tObj.has("kurir_nama") && !tObj.isNull("kurir_nama") -> tObj.optString("kurir_nama", "")
                        tObj.has("nama_kurir") && !tObj.isNull("nama_kurir") -> tObj.optString("nama_kurir", "")
                        tObj.has("kurir") && !tObj.isNull("kurir") -> tObj.optString("kurir", "")
                        else -> ""
                    }.trim()
                    val jumlah = tObj.optInt("jumlah", 1).coerceAtLeast(1)
                    val totalKomisi = tObj.optDouble("total_komisi", 0.0)
                    val inferredKomisi = if (totalKomisi > 0.0) (totalKomisi / jumlah) else 1000.0

                    // 1. Jika ada nama kurir asli di transaksi, pastikan masuk ke tabel Kurir & petakan ke rawIdKurir
                    if (!isGenericFakeKurirName(rawKurirName)) {
                        return upsertRealKurir(rawIdKurir, rawKurirName, inferredKomisi)
                    }

                    // 2. Prioritaskan pemetaan Foreign Key id_kurir dari tabel Kurir yang sudah di-insert terlebih dahulu
                    if (rawIdKurir > 0) {
                        jsonKurirIdMap[rawIdKurir]?.let { return it }
                        val mapped = upsertRealKurir(rawIdKurir, rawKurirName, inferredKomisi)
                        if (mapped != null) return mapped
                    }

                    // 3. Fallback ke kurir langganan pelanggan jika ada, atau null ("Tanpa Kurir")
                    return fallbackCustomerKurirId
                }

                suspend fun resolveTransactionProdukId(tObj: JSONObject): Int {
                    val rawIdProduk = extractKurirIdFromObj(tObj, "id_produk", "produkId", "produk_id")
                    val prodName = tObj.optString("produk", "Galon Air RO 19L").trim()
                    val hargaSatuan = tObj.optDouble("harga_satuan", 6000.0)

                    if (rawIdProduk > 0) {
                        jsonProdukIdMap[rawIdProduk]?.let { return it }
                        if (prodName.isNotBlank() && prodName != "-") {
                            return upsertRealProduk(rawIdProduk, prodName, hargaSatuan)
                        }
                        val existingById = currentProducts.find { it.id == rawIdProduk }
                        if (existingById != null) {
                            jsonProdukIdMap[rawIdProduk] = existingById.id
                            return existingById.id
                        }
                    }
                    return upsertRealProduk(-1, prodName, hargaSatuan)
                }

                suspend fun preScanAndInsertKurirFromCustomers(custArray: JSONArray?) {
                    if (custArray == null) return
                    for (i in 0 until custArray.length()) {
                        val cObj = custArray.optJSONObject(i) ?: continue
                        val rawCustKurirId = extractKurirIdFromObj(cObj, "id_kurir_langganan", "kurir_langganan_id")
                        val custKurirName = when {
                            cObj.has("kurir_langganan_nama") -> cObj.optString("kurir_langganan_nama", "")
                            cObj.has("kurir_nama") -> cObj.optString("kurir_nama", "")
                            cObj.has("kurir") -> cObj.optString("kurir", "")
                            else -> ""
                        }.trim()
                        if (!isGenericFakeKurirName(custKurirName)) {
                            upsertRealKurir(rawCustKurirId, custKurirName, 1000.0)
                        }

                        val trxArray = when {
                            cObj.has("riwayat_transaksi") -> cObj.optJSONArray("riwayat_transaksi")
                            cObj.has("transaksi") -> cObj.optJSONArray("transaksi")
                            else -> null
                        }
                        if (trxArray != null) {
                            for (j in 0 until trxArray.length()) {
                                val tObj = trxArray.optJSONObject(j) ?: continue
                                val tKurirId = extractKurirIdFromObj(tObj, "id_kurir", "kurirId", "kurir_id")
                                val tKurirName = when {
                                    tObj.has("kurir_nama") -> tObj.optString("kurir_nama", "")
                                    tObj.has("nama_kurir") -> tObj.optString("nama_kurir", "")
                                    tObj.has("kurir") -> tObj.optString("kurir", "")
                                    else -> ""
                                }.trim()
                                val jumlah = tObj.optInt("jumlah", 1).coerceAtLeast(1)
                                val totalKomisi = tObj.optDouble("total_komisi", 0.0)
                                val inferredKomisi = if (totalKomisi > 0.0) (totalKomisi / jumlah) else 1000.0
                                if (!isGenericFakeKurirName(tKurirName)) {
                                    upsertRealKurir(tKurirId, tKurirName, inferredKomisi)
                                }
                            }
                        }
                    }
                }

                if (parsedRoot != null) {
                    val root = parsedRoot
                    val custArray = when {
                        root.has("data_pelanggan_nested") -> root.optJSONArray("data_pelanggan_nested")
                        root.has("pelanggan") -> root.optJSONArray("pelanggan")
                        root.has("customers") -> root.optJSONArray("customers")
                        else -> null
                    }

                    // LANGKAH 1 IMPORT: Parse & INSERT/UPDATE tabel Kurir TERLEBIH DAHULU sebelum Pelanggan & Transaksi
                    val kArray = when {
                        root.has("master_kurir") -> root.optJSONArray("master_kurir")
                        root.has("kurir") -> root.optJSONArray("kurir")
                        root.has("data_kurir") -> root.optJSONArray("data_kurir")
                        else -> null
                    }
                    if (kArray != null) {
                        for (i in 0 until kArray.length()) {
                            val kObj = kArray.optJSONObject(i) ?: continue
                            val oldId = extractKurirIdFromObj(kObj, "id", "id_kurir", "kurirId")
                            val rawSyncId = extractOptionalString(kObj, "syncId")
                            val nama = kObj.optString("nama", kObj.optString("nama_kurir", "")).trim()
                            val komisi = kObj.optDouble("komisi_per_galon", kObj.optDouble("komisi", 1000.0))
                            val email = kObj.optString("email", "").trim().lowercase()
                            val aksesAktif = kObj.optBoolean("akses_aktif", true)
                            upsertRealKurir(oldId, nama, komisi, email, aksesAktif, rawSyncId)
                        }
                    }

                    // Pre-scan nama kurir asli dari data pelanggan/transaksi sebelum insert Pelanggan & Transaksi
                    preScanAndInsertKurirFromCustomers(custArray)

                    // LANGKAH 2 IMPORT: Parse & INSERT/UPDATE tabel Produk sebelum Transaksi
                    val prodArray = when {
                        root.has("master_produk") -> root.optJSONArray("master_produk")
                        root.has("produk") -> root.optJSONArray("produk")
                        else -> null
                    }
                    if (prodArray != null) {
                        for (i in 0 until prodArray.length()) {
                            val pObj = prodArray.optJSONObject(i) ?: continue
                            val oldId = extractKurirIdFromObj(pObj, "id", "id_produk", "produkId")
                            val rawSyncId = extractOptionalString(pObj, "syncId")
                            val nama = pObj.optString("nama_produk", pObj.optString("nama", "")).trim()
                            val harga = pObj.optDouble("harga", 6000.0)
                            if (nama.isNotBlank()) {
                                upsertRealProduk(oldId, nama, harga, rawSyncId)
                            }
                        }
                    }

                    // LANGKAH 3 IMPORT: Insert Pelanggan lalu Transaksi dengan pemetaan kurirId yang akurat
                    if (custArray != null) {
                        for (i in 0 until custArray.length()) {
                            val cObj = custArray.getJSONObject(i)
                            val nama = cObj.optString("nama", "Pelanggan")
                            val alamat = cObj.optString("alamat", "")
                            val noHp = cObj.optString("no_hp", "")
                            val kasbon = cObj.optDouble("sisa_kasbon", 0.0)
                            val hKhusus = if (cObj.has("harga_khusus") && !cObj.isNull("harga_khusus")) {
                                cObj.optDouble("harga_khusus")
                            } else null
                            val rawCustKurirId = extractKurirIdFromObj(cObj, "id_kurir_langganan", "kurir_langganan_id")
                            val kLanggananName = when {
                                cObj.has("kurir_langganan_nama") -> cObj.optString("kurir_langganan_nama", "")
                                cObj.has("kurir_nama") -> cObj.optString("kurir_nama", "")
                                cObj.has("kurir") -> cObj.optString("kurir", "")
                                else -> ""
                            }.trim()

                            val idKurir = when {
                                !isGenericFakeKurirName(kLanggananName) -> upsertRealKurir(rawCustKurirId, kLanggananName, 1000.0)
                                rawCustKurirId > 0 -> jsonKurirIdMap[rawCustKurirId] ?: upsertRealKurir(rawCustKurirId, "", 1000.0)
                                else -> null
                            }

                            val oldCustId = extractKurirIdFromObj(cObj, "id", "id_pelanggan", "pelangganId")
                            val rawCustSyncId = extractOptionalString(cObj, "syncId")
                            val newCustId = upsertImportedPelanggan(
                                oldId = oldCustId,
                                nama = nama,
                                alamat = alamat,
                                noHp = noHp,
                                kasbon = kasbon,
                                hKhusus = hKhusus,
                                idKurir = idKurir,
                                rawSyncId = rawCustSyncId
                            )

                            // Riwayat Transaksi nested
                            val trxArray = when {
                                cObj.has("riwayat_transaksi") -> cObj.optJSONArray("riwayat_transaksi")
                                cObj.has("transaksi") -> cObj.optJSONArray("transaksi")
                                else -> null
                            }

                            if (trxArray != null) {
                                for (j in 0 until trxArray.length()) {
                                    val tObj = trxArray.getJSONObject(j)
                                    val oldTxId = extractKurirIdFromObj(tObj, "id", "id_transaksi", "transaksiId")
                                    val rawTxSyncId = extractOptionalString(tObj, "syncId")
                                    val tgl = tObj.optLong("tanggal", System.currentTimeMillis())
                                    val rawTanggalBayar = extractOptionalLong(tObj, "tanggalBayar", "tanggal_bayar")
                                    val hargaSatuan = tObj.optDouble("harga_satuan", 6000.0)
                                    val prodId = resolveTransactionProdukId(tObj)
                                    val kurirId = resolveTransactionKurirId(tObj, idKurir)

                                    val jumlah = tObj.optInt("jumlah", 1)
                                    val totalHarga = tObj.optDouble("total_harga", jumlah * hargaSatuan)
                                    val totalKomisi = tObj.optDouble("total_komisi", 0.0)
                                    val metode = tObj.optString("metode_bayar", "Cash")
                                    val status = tObj.optString("status", "Lunas")
                                    val uangMuka = tObj.optLong("uangMuka", 0L)
                                    val defaultSisa = if (status.equals("Kasbon", ignoreCase = true)) {
                                        (totalHarga.toLong() - uangMuka).coerceAtLeast(0L)
                                    } else 0L
                                    val sisaKasbon = tObj.optLong("sisaKasbon", defaultSisa)
                                    val rawWaterType = extractOptionalString(tObj, "waterType", "water_type")

                                    upsertImportedTransaksi(
                                        oldTxId = oldTxId,
                                        tgl = tgl,
                                        custId = newCustId,
                                        kurirId = kurirId,
                                        prodId = prodId,
                                        jumlah = jumlah,
                                        hargaSatuan = hargaSatuan,
                                        totalHarga = totalHarga,
                                        totalKomisi = totalKomisi,
                                        metode = metode,
                                        status = status,
                                        uangMuka = uangMuka,
                                        sisaKasbon = sisaKasbon,
                                        rawTanggalBayar = rawTanggalBayar,
                                        rawSyncId = rawTxSyncId,
                                        rawWaterType = rawWaterType
                                    )
                                }
                            }
                        }
                    }

                    // LANGKAH 4 IMPORT: Flat Pengeluaran if present
                    if (root.has("pengeluaran")) {
                        val expArray = root.getJSONArray("pengeluaran")
                        for (i in 0 until expArray.length()) {
                            val eObj = expArray.getJSONObject(i)
                            val oldExpId = extractKurirIdFromObj(eObj, "id", "id_pengeluaran")
                            val rawExpSyncId = sanitizeSyncId(extractOptionalString(eObj, "syncId"))
                            val namaItem = eObj.optString("nama_item", "Operasional").trim().ifBlank { "Operasional" }.take(200)
                            val rawNominal = eObj.optDouble("nominal", 0.0)
                            val nominal = if (rawNominal.isFinite()) rawNominal else 0.0
                            val kat = eObj.optString("kategori", "Operasional").trim().ifBlank { "Operasional" }.take(80)
                            val tgl = eObj.optLong("tanggal", System.currentTimeMillis()).coerceAtLeast(0L)
                            if (nominal > 0.0) {
                                val matchExp = (if (rawExpSyncId != null) existingExpenses.find { it.syncId == rawExpSyncId } else null)
                                    ?: (if (oldExpId > 0) existingExpenses.find { it.id == oldExpId && it.tanggal == tgl } else null)
                                    ?: existingExpenses.find {
                                        it.tanggal == tgl &&
                                            it.nama_item.equals(namaItem, ignoreCase = true) &&
                                            it.nominal == nominal
                                    }
                                if (matchExp != null) {
                                    val updatedExp = matchExp.copy(
                                        tanggal = tgl,
                                        nama_item = namaItem,
                                        kategori = kat,
                                        nominal = nominal,
                                        updatedAt = monotonicImportTime(matchExp.updatedAt, importNow)
                                    )
                                    db.pengeluaranDao().update(updatedExp)
                                    val idx = existingExpenses.indexOfFirst { it.id == matchExp.id }
                                    if (idx >= 0) existingExpenses[idx] = updatedExp
                                    expCount++
                                } else {
                                    val chosenSyncId = if (rawExpSyncId != null && existingExpenses.none { it.syncId == rawExpSyncId }) {
                                        rawExpSyncId
                                    } else {
                                        java.util.UUID.randomUUID().toString()
                                    }
                                    val toInsertExp = if (oldExpId > 0 && existingExpenses.none { it.id == oldExpId }) {
                                        Pengeluaran(id = oldExpId, tanggal = tgl, nama_item = namaItem, kategori = kat, nominal = nominal, updatedAt = importNow, syncId = chosenSyncId)
                                    } else {
                                        Pengeluaran(tanggal = tgl, nama_item = namaItem, kategori = kat, nominal = nominal, updatedAt = importNow, syncId = chosenSyncId)
                                    }
                                    val newExpId = db.pengeluaranDao().insert(toInsertExp).toInt()
                                    existingExpenses.add(toInsertExp.copy(id = newExpId))
                                    expCount++
                                }
                            }
                        }
                    }

                    // LANGKAH 5 IMPORT: Riwayat Pelunasan & Cicilan Kasbon (kasbon_logs)
                    val kasbonLogArray = when {
                        root.has("kasbon_logs") -> root.optJSONArray("kasbon_logs")
                        root.has("kasbon_log") -> root.optJSONArray("kasbon_log")
                        else -> null
                    }
                    if (kasbonLogArray != null) {
                        for (i in 0 until kasbonLogArray.length()) {
                            val lObj = kasbonLogArray.optJSONObject(i) ?: continue
                            val oldLogId = extractKurirIdFromObj(lObj, "id", "id_log")
                            val rawLogSyncId = sanitizeSyncId(extractOptionalString(lObj, "syncId"))
                            val rawPelangganId = extractKurirIdFromObj(lObj, "id_pelanggan", "pelangganId")
                            val rawPelangganSyncId = sanitizeSyncId(extractOptionalString(lObj, "pelangganSyncId", "pelanggan_sync_id"))
                            val rawNamaPelanggan = extractOptionalString(lObj, "nama_pelanggan", "namaPelanggan")
                            val targetPelangganId = (if (rawPelangganSyncId != null) pelangganSyncIdMap[rawPelangganSyncId] else null)
                                ?: (if (rawPelangganId > 0) jsonPelangganIdMap[rawPelangganId] else null)
                                ?: (if (!rawNamaPelanggan.isNullOrBlank()) {
                                    existingCustomers.find { it.nama.equals(rawNamaPelanggan, ignoreCase = true) }?.id
                                } else null)
                                ?: existingCustomers.find { it.id == rawPelangganId }?.id
                                ?: continue

                            val tgl = lObj.optLong("tanggal", System.currentTimeMillis()).coerceAtLeast(0L)
                            val jumlahBayar = lObj.optDouble("jumlah_bayar", 0.0).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
                            if (jumlahBayar <= 0.0) continue
                            val sisaSebelum = lObj.optDouble("sisa_kasbon_sebelum", jumlahBayar).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: jumlahBayar
                            val sisaSesudah = lObj.optDouble("sisa_kasbon_sesudah", (sisaSebelum - jumlahBayar).coerceAtLeast(0.0)).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
                            val metode = lObj.optString("metode_bayar", "Cash").trim().ifBlank { "Cash" }.take(40)
                            val catatan = lObj.optString("catatan", "").take(300)

                            val matchLog = (if (rawLogSyncId != null) existingKasbonLogs.find { it.syncId == rawLogSyncId } else null)
                                ?: (if (oldLogId > 0) existingKasbonLogs.find { it.id == oldLogId && it.tanggal == tgl } else null)
                                ?: existingKasbonLogs.find {
                                    it.id_pelanggan == targetPelangganId &&
                                        it.tanggal == tgl &&
                                        abs(it.jumlah_bayar - jumlahBayar) <= 0.01
                                }
                            if (matchLog != null) {
                                val updatedLog = matchLog.copy(
                                    id_pelanggan = targetPelangganId,
                                    tanggal = tgl,
                                    jumlah_bayar = jumlahBayar,
                                    sisa_kasbon_sebelum = sisaSebelum,
                                    sisa_kasbon_sesudah = sisaSesudah,
                                    metode_bayar = metode,
                                    catatan = catatan,
                                    updatedAt = monotonicImportTime(matchLog.updatedAt, importNow)
                                )
                                db.kasbonLogDao().update(updatedLog)
                                val idx = existingKasbonLogs.indexOfFirst { it.id == matchLog.id }
                                if (idx >= 0) existingKasbonLogs[idx] = updatedLog
                                kasbonLogCount++
                            } else {
                                val chosenSyncId = if (rawLogSyncId != null && existingKasbonLogs.none { it.syncId == rawLogSyncId }) {
                                    rawLogSyncId
                                } else {
                                    java.util.UUID.randomUUID().toString()
                                }
                                val toInsertLog = if (oldLogId > 0 && existingKasbonLogs.none { it.id == oldLogId }) {
                                    KasbonLog(
                                        id = oldLogId,
                                        id_pelanggan = targetPelangganId,
                                        tanggal = tgl,
                                        jumlah_bayar = jumlahBayar,
                                        sisa_kasbon_sebelum = sisaSebelum,
                                        sisa_kasbon_sesudah = sisaSesudah,
                                        metode_bayar = metode,
                                        catatan = catatan,
                                        updatedAt = importNow,
                                        syncId = chosenSyncId
                                    )
                                } else {
                                    KasbonLog(
                                        id_pelanggan = targetPelangganId,
                                        tanggal = tgl,
                                        jumlah_bayar = jumlahBayar,
                                        sisa_kasbon_sebelum = sisaSebelum,
                                        sisa_kasbon_sesudah = sisaSesudah,
                                        metode_bayar = metode,
                                        catatan = catatan,
                                        updatedAt = importNow,
                                        syncId = chosenSyncId
                                    )
                                }
                                val newLogId = db.kasbonLogDao().insert(toInsertLog).toInt()
                                existingKasbonLogs.add(toInsertLog.copy(id = newLogId))
                                kasbonLogCount++
                            }
                        }
                    }

                    // LANGKAH 6 IMPORT: Jadwal Perawatan RO (perawatan_jadwal)
                    val perawatanArray = root.optJSONArray("perawatan_jadwal")
                    if (perawatanArray != null) {
                        for (i in 0 until perawatanArray.length()) {
                            val pObj = perawatanArray.optJSONObject(i) ?: continue
                            val oldItemId = extractKurirIdFromObj(pObj, "id")
                            val rawSyncId = sanitizeSyncId(extractOptionalString(pObj, "syncId"))
                            val nama = pObj.optString("nama", "").trim().take(120)
                            if (nama.isBlank()) continue
                            val intervalHari = pObj.optInt("intervalHari", 30).coerceIn(1, 3650)
                            val terakhirSelesai = extractOptionalLong(pObj, "terakhirSelesai", "terakhir_selesai")
                            val jadwalBerikutnya = extractOptionalLong(pObj, "jadwalBerikutnya", "jadwal_berikutnya")
                                ?: PerawatanScheduler.calculateNextDueDateMillis(terakhirSelesai ?: importNow, intervalHari)

                            val matchItem = (if (rawSyncId != null) existingPerawatan.find { it.syncId == rawSyncId } else null)
                                ?: existingPerawatan.find { it.nama.equals(nama, ignoreCase = true) }
                            if (matchItem != null) {
                                val updatedItem = matchItem.copy(
                                    nama = nama,
                                    intervalHari = intervalHari,
                                    terakhirSelesai = terakhirSelesai ?: matchItem.terakhirSelesai,
                                    jadwalBerikutnya = jadwalBerikutnya,
                                    updatedAt = monotonicImportTime(matchItem.updatedAt, importNow)
                                )
                                db.perawatanDao().update(updatedItem)
                                val idx = existingPerawatan.indexOfFirst { it.id == matchItem.id }
                                if (idx >= 0) existingPerawatan[idx] = updatedItem
                                perawatanCount++
                            } else {
                                val chosenSyncId = if (rawSyncId != null && existingPerawatan.none { it.syncId == rawSyncId }) {
                                    rawSyncId
                                } else {
                                    java.util.UUID.randomUUID().toString()
                                }
                                val toInsertItem = if (oldItemId > 0 && existingPerawatan.none { it.id == oldItemId }) {
                                    PerawatanItem(
                                        id = oldItemId,
                                        nama = nama,
                                        intervalHari = intervalHari,
                                        terakhirSelesai = terakhirSelesai,
                                        jadwalBerikutnya = jadwalBerikutnya,
                                        updatedAt = importNow,
                                        syncId = chosenSyncId
                                    )
                                } else {
                                    PerawatanItem(
                                        nama = nama,
                                        intervalHari = intervalHari,
                                        terakhirSelesai = terakhirSelesai,
                                        jadwalBerikutnya = jadwalBerikutnya,
                                        updatedAt = importNow,
                                        syncId = chosenSyncId
                                    )
                                }
                                val newItemId = db.perawatanDao().insert(toInsertItem).toInt()
                                existingPerawatan.add(toInsertItem.copy(id = newItemId))
                                perawatanCount++
                            }
                        }
                    }

                    // LANGKAH 7 IMPORT: Riwayat Perawatan RO (perawatan_log)
                    val perawatanLogArray = root.optJSONArray("perawatan_log")
                    if (perawatanLogArray != null) {
                        for (i in 0 until perawatanLogArray.length()) {
                            val lObj = perawatanLogArray.optJSONObject(i) ?: continue
                            val oldLogId = extractKurirIdFromObj(lObj, "id")
                            val rawSyncId = sanitizeSyncId(extractOptionalString(lObj, "syncId"))
                            val perawatanSyncId = sanitizeSyncId(lObj.optString("perawatanSyncId", "").trim()) ?: ""
                            val namaPerawatan = lObj.optString("namaPerawatan", "").trim().take(120)
                            if (namaPerawatan.isBlank()) continue
                            val tanggalSelesai = lObj.optLong("tanggalSelesai", System.currentTimeMillis()).coerceAtLeast(0L)
                            val biaya = lObj.optDouble("biaya", 0.0).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
                            val catatan = lObj.optString("catatan", "").take(500)
                            val pengeluaranSyncId = sanitizeSyncId(extractOptionalString(lObj, "pengeluaranSyncId"))

                            val matchLog = (if (rawSyncId != null) existingPerawatanLogs.find { it.syncId == rawSyncId } else null)
                                ?: existingPerawatanLogs.find {
                                    it.namaPerawatan.equals(namaPerawatan, ignoreCase = true) &&
                                        it.tanggalSelesai == tanggalSelesai &&
                                        abs(it.biaya - biaya) <= 0.01
                                }
                            if (matchLog != null) {
                                val updatedLog = matchLog.copy(
                                    perawatanSyncId = perawatanSyncId.ifBlank { matchLog.perawatanSyncId },
                                    namaPerawatan = namaPerawatan,
                                    tanggalSelesai = tanggalSelesai,
                                    biaya = biaya,
                                    catatan = catatan,
                                    pengeluaranSyncId = pengeluaranSyncId ?: matchLog.pengeluaranSyncId,
                                    updatedAt = monotonicImportTime(matchLog.updatedAt, importNow)
                                )
                                db.perawatanLogDao().update(updatedLog)
                                val idx = existingPerawatanLogs.indexOfFirst { it.id == matchLog.id }
                                if (idx >= 0) existingPerawatanLogs[idx] = updatedLog
                                perawatanLogCount++
                            } else {
                                val chosenSyncId = if (rawSyncId != null && existingPerawatanLogs.none { it.syncId == rawSyncId }) {
                                    rawSyncId
                                } else {
                                    java.util.UUID.randomUUID().toString()
                                }
                                val toInsertLog = if (oldLogId > 0 && existingPerawatanLogs.none { it.id == oldLogId }) {
                                    PerawatanLog(
                                        id = oldLogId,
                                        perawatanSyncId = perawatanSyncId.ifBlank { chosenSyncId },
                                        namaPerawatan = namaPerawatan,
                                        tanggalSelesai = tanggalSelesai,
                                        biaya = biaya,
                                        catatan = catatan,
                                        pengeluaranSyncId = pengeluaranSyncId,
                                        updatedAt = importNow,
                                        syncId = chosenSyncId
                                    )
                                } else {
                                    PerawatanLog(
                                        perawatanSyncId = perawatanSyncId.ifBlank { chosenSyncId },
                                        namaPerawatan = namaPerawatan,
                                        tanggalSelesai = tanggalSelesai,
                                        biaya = biaya,
                                        catatan = catatan,
                                        pengeluaranSyncId = pengeluaranSyncId,
                                        updatedAt = importNow,
                                        syncId = chosenSyncId
                                    )
                                }
                                val newLogId = db.perawatanLogDao().insert(toInsertLog).toInt()
                                existingPerawatanLogs.add(toInsertLog.copy(id = newLogId))
                                perawatanLogCount++
                            }
                        }
                    }
                } else if (parsedArray != null) {
                    val custArray = parsedArray
                    // Pre-scan dan insert Kurir terlebih dahulu sebelum Pelanggan & Transaksi
                    preScanAndInsertKurirFromCustomers(custArray)

                    for (i in 0 until custArray.length()) {
                        val cObj = custArray.getJSONObject(i)
                        val nama = cObj.optString("nama", "Pelanggan")
                        val alamat = cObj.optString("alamat", "")
                        val noHp = cObj.optString("no_hp", "")
                        val kasbon = cObj.optDouble("sisa_kasbon", 0.0)
                        val hKhusus = if (cObj.has("harga_khusus") && !cObj.isNull("harga_khusus")) {
                            cObj.optDouble("harga_khusus")
                        } else null
                        val rawCustKurirId = extractKurirIdFromObj(cObj, "id_kurir_langganan", "kurir_langganan_id")
                        val kLanggananName = when {
                            cObj.has("kurir_langganan_nama") -> cObj.optString("kurir_langganan_nama", "")
                            cObj.has("kurir_nama") -> cObj.optString("kurir_nama", "")
                            cObj.has("kurir") -> cObj.optString("kurir", "")
                            else -> ""
                        }.trim()

                        val idKurir = when {
                            !isGenericFakeKurirName(kLanggananName) -> upsertRealKurir(rawCustKurirId, kLanggananName, 1000.0)
                            rawCustKurirId > 0 -> jsonKurirIdMap[rawCustKurirId] ?: upsertRealKurir(rawCustKurirId, "", 1000.0)
                            else -> null
                        }

                        val oldCustId = extractKurirIdFromObj(cObj, "id", "id_pelanggan", "pelangganId")
                        val rawCustSyncId = extractOptionalString(cObj, "syncId")
                        val newCustId = upsertImportedPelanggan(
                            oldId = oldCustId,
                            nama = nama,
                            alamat = alamat,
                            noHp = noHp,
                            kasbon = kasbon,
                            hKhusus = hKhusus,
                            idKurir = idKurir,
                            rawSyncId = rawCustSyncId
                        )

                        val trxArray = when {
                            cObj.has("riwayat_transaksi") -> cObj.optJSONArray("riwayat_transaksi")
                            cObj.has("transaksi") -> cObj.optJSONArray("transaksi")
                            else -> null
                        }
                        if (trxArray != null) {
                            for (j in 0 until trxArray.length()) {
                                val tObj = trxArray.getJSONObject(j)
                                val oldTxId = extractKurirIdFromObj(tObj, "id", "id_transaksi", "transaksiId")
                                val rawTxSyncId = extractOptionalString(tObj, "syncId")
                                val tgl = tObj.optLong("tanggal", System.currentTimeMillis())
                                val rawTanggalBayar = extractOptionalLong(tObj, "tanggalBayar", "tanggal_bayar")
                                val hargaSatuan = tObj.optDouble("harga_satuan", 6000.0)
                                val prodId = resolveTransactionProdukId(tObj)
                                val kurirId = resolveTransactionKurirId(tObj, idKurir)

                                val jumlah = tObj.optInt("jumlah", 1)
                                val totalHarga = tObj.optDouble("total_harga", jumlah * hargaSatuan)
                                val totalKomisi = tObj.optDouble("total_komisi", 0.0)
                                val metode = tObj.optString("metode_bayar", "Cash")
                                val status = tObj.optString("status", "Lunas")
                                val uangMuka = tObj.optLong("uangMuka", 0L)
                                val defaultSisa = if (status.equals("Kasbon", ignoreCase = true)) {
                                    (totalHarga.toLong() - uangMuka).coerceAtLeast(0L)
                                } else 0L
                                val sisaKasbon = tObj.optLong("sisaKasbon", defaultSisa)

                                upsertImportedTransaksi(
                                    oldTxId = oldTxId,
                                    tgl = tgl,
                                    custId = newCustId,
                                    kurirId = kurirId,
                                    prodId = prodId,
                                    jumlah = jumlah,
                                    hargaSatuan = hargaSatuan,
                                    totalHarga = totalHarga,
                                    totalKomisi = totalKomisi,
                                    metode = metode,
                                    status = status,
                                    uangMuka = uangMuka,
                                    sisaKasbon = sisaKasbon,
                                    rawTanggalBayar = rawTanggalBayar,
                                    rawSyncId = rawTxSyncId
                                )
                            }
                        }
                    }
                }
            }

            ImportResult(
                success = true,
                pelangganCount = pCount,
                transaksiCount = txCount,
                produkCount = prodCount,
                kurirCount = kurirCount,
                pengeluaranCount = expCount,
                kasbonLogCount = kasbonLogCount,
                perawatanCount = perawatanCount,
                perawatanLogCount = perawatanLogCount,
                message = "Impor selesai: $pCount pelanggan dan $txCount transaksi",
                importedDepotProfile = extractImportedDepotProfile(parsedRoot)
            )
        } catch (e: Exception) {
            ImportResult(success = false, message = "File cadangan gagal dibaca: format tidak sesuai")
        }
    }

    /**
     * Mengekstrak objek `depot_profile` dari berkas cadangan JSON jika tersedia.
     */
    internal fun extractImportedDepotProfile(root: JSONObject?): ImportedDepotProfileData? {
        val profObj = root?.optJSONObject("depot_profile") ?: return null
        val owner = profObj.optString("ownerName", "").trim().take(120)
        val depot = profObj.optString("depotName", "").trim().take(100)
        val phone = DepotProfile.normalizePhoneInput(profObj.optString("phoneNumber", ""))
        val address = profObj.optString("address", "").trim().take(300)
        val footer = profObj.optString("receiptFooter", "").trim().take(200)
        val depotType = profObj.optString("depotType", "").trim().uppercase().takeIf {
            it == DepotProfile.TYPE_RO || it == DepotProfile.TYPE_MINERAL || it == DepotProfile.TYPE_BOTH
        }
        val cap = profObj.optDouble("tankCapacityLiters", Double.NaN).takeIf { it.isFinite() && it >= 100.0 }
        if (owner.isEmpty() && depot.isEmpty() && phone.isEmpty() && address.isEmpty() && footer.isEmpty()) {
            return null
        }
        return ImportedDepotProfileData(
            ownerName = owner,
            depotName = depot,
            phoneNumber = phone,
            address = address,
            receiptFooter = footer,
            depotType = depotType,
            tankCapacityLiters = cap
        )
    }
}

data class ImportedDepotProfileData(
    val ownerName: String = "",
    val depotName: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val receiptFooter: String = "",
    val depotType: String? = null,
    val tankCapacityLiters: Double? = null
)

data class ImportResult(
    val success: Boolean,
    val pelangganCount: Int = 0,
    val transaksiCount: Int = 0,
    val produkCount: Int = 0,
    val kurirCount: Int = 0,
    val pengeluaranCount: Int = 0,
    val kasbonLogCount: Int = 0,
    val perawatanCount: Int = 0,
    val perawatanLogCount: Int = 0,
    val message: String = "",
    val importedDepotProfile: ImportedDepotProfileData? = null
)
