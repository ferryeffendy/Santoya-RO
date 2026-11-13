package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.Produk
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream

object ExportHelper {

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
        pengeluaranList: List<Pengeluaran>
    ): String {
        val root = JSONObject()
        root.put("app", "Santoya RO")
        root.put("version", "2.0")
        root.put("export_timestamp", System.currentTimeMillis())
        root.put("export_date", FormatUtils.formatDate(System.currentTimeMillis()))

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

        // Master Kurir (diekspor lengkap dengan id, nama, komisi_per_galon, email, akses_aktif)
        val kurirArray = JSONArray()
        effectiveKurirList.forEach { k ->
            val obj = JSONObject()
            obj.put("id", k.id)
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
                tObj.put("id_pelanggan", t.transaksi.id_pelanggan)
                tObj.put("id_produk", t.transaksi.id_produk)
                if (t.transaksi.id_kurir != null) {
                    tObj.put("id_kurir", t.transaksi.id_kurir)
                } else {
                    tObj.put("id_kurir", JSONObject.NULL)
                }
                tObj.put("tanggal", t.transaksi.tanggal)
                tObj.put("tanggal_formatted", FormatUtils.formatDate(t.transaksi.tanggal))
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
            obj.put("tanggal", e.tanggal)
            obj.put("kategori", e.kategori)
            obj.put("nama_item", e.nama_item)
            obj.put("nominal", e.nominal)
            expArray.put(obj)
        }
        root.put("pengeluaran", expArray)

        return root.toString(2)
    }

    // --- CSV Export ---
    fun generateCsv(
        transaksiList: List<TransaksiDetail>,
        pengeluaranList: List<Pengeluaran>
    ): String {
        val activeTransaksiList = transaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
        val sb = StringBuilder()
        sb.append("=== LAPORAN TRANSAKSI SANTOYA RO ===\n")
        sb.append("ID,Tanggal,Pelanggan,Alamat,NoHP,Produk,Jumlah,HargaSatuan,TotalHarga,UangMuka,SisaKasbon,Kurir,TotalKomisi,MetodeBayar,Status\n")
        activeTransaksiList.forEach { t ->
            val trx = t.transaksi
            val tgl = FormatUtils.formatDate(trx.tanggal).replace(",", " ")
            val custName = (t.pelanggan?.nama ?: "Umum").replace(",", ";")
            val custAddr = KaryawanScopeHelper.cleanPelangganAlamat(t.pelanggan?.alamat ?: "-").replace(",", ";")
            val custPhone = t.pelanggan?.no_hp ?: "-"
            val prod = (t.produk?.nama_produk ?: "Galon RO").replace(",", ";")
            val kurir = (t.kurir?.nama?.takeIf { !isGenericFakeKurirName(it) } ?: "Tanpa Kurir").replace(",", ";")
            val sisaKasbonVal = trx.remainingKasbonAmount().toLong()

            sb.append("${trx.id},\"$tgl\",\"$custName\",\"$custAddr\",\"$custPhone\",\"$prod\",${trx.jumlah},${trx.harga_satuan},${trx.total_harga},${trx.uangMuka},$sisaKasbonVal,\"$kurir\",${trx.total_komisi},${trx.metode_bayar},${trx.status}\n")
        }

        sb.append("\n=== LAPORAN PENGELUARAN SANTOYA RO ===\n")
        sb.append("ID,Tanggal,Kategori,NamaItem,Nominal\n")
        pengeluaranList.forEach { e ->
            val tgl = FormatUtils.formatDate(e.tanggal).replace(",", " ")
            val item = e.nama_item.replace(",", ";")
            sb.append("${e.id},\"$tgl\",${e.kategori},\"$item\",${e.nominal}\n")
        }

        val totalGalon = activeTransaksiList.sumOf { it.transaksi.jumlah }
        val pendapatanLunas = activeTransaksiList.sumOf {
            if (it.transaksi.status.equals("Lunas", ignoreCase = true)) {
                it.transaksi.total_harga
            } else {
                it.transaksi.uangMuka.toDouble()
            }
        }
        val piutangKasbon = activeTransaksiList
            .filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
            .sumOf { it.transaksi.remainingKasbonAmount() }
        val totalKomisi = activeTransaksiList.sumOf { it.transaksi.total_komisi }
        val totalPengeluaran = pengeluaranList.sumOf { it.nominal }
        val labaBersih = pendapatanLunas - totalKomisi - totalPengeluaran

        sb.append("\n=== RINGKASAN LABA RUGI SANTOYA RO ===\n")
        sb.append("Komponen,Nilai\n")
        sb.append("Total Galon Terjual,$totalGalon\n")
        sb.append("Pendapatan Tunai Masuk (Lunas + DP),$pendapatanLunas\n")
        sb.append("Piutang Kasbon Aktif,$piutangKasbon\n")
        sb.append("Total Komisi Kurir,$totalKomisi\n")
        sb.append("Total Pengeluaran Operasional,$totalPengeluaran\n")
        sb.append("Laba Bersih (Net Profit),$labaBersih\n")

        return sb.toString()
    }

    fun shareExportFile(context: Context, filename: String, content: String, mimeType: String) {
        val backupDir = File(context.cacheDir, "backups")
        backupDir.mkdirs()
        val file = File(backupDir, filename)
        val fos = FileOutputStream(file)
        fos.write(content.toByteArray(Charsets.UTF_8))
        fos.flush()
        fos.close()

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Cadangan Data Santoya RO: $filename")
            putExtra(Intent.EXTRA_TEXT, "Cadangan data Santoya RO ($filename)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Simpan / Bagikan Cadangan")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().use { it.readText() }
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
        pengeluaranList: List<Pengeluaran>
    ) {
        val json = generateNestedJson(pelangganList, transaksiList, produkList, kurirList, pengeluaranList)
        val file = File(context.filesDir, "auto_backup_before_import.json")
        file.writeText(json)
    }

    fun hasAutoBackup(context: Context): Boolean {
        val file = File(context.filesDir, "auto_backup_before_import.json")
        return file.exists() && file.length() > 0
    }

    fun readAutoBackupJson(context: Context): String? {
        val file = File(context.filesDir, "auto_backup_before_import.json")
        return if (file.exists()) file.readText() else null
    }

    suspend fun importJsonData(
        context: Context,
        db: com.example.data.database.AppDatabase,
        jsonString: String
    ): ImportResult {
        return try {
            val oldP = db.pelangganDao().getAll().first()
            val oldTx = db.transaksiDao().getAllWithDetail().first()
            val oldPr = db.produkDao().getAll().first()
            val oldK = db.kurirDao().getAll().first()
            val oldExp = db.pengeluaranDao().getAll().first()
            if (oldP.isNotEmpty() || oldTx.isNotEmpty()) {
                createAutoBackupBeforeImport(context, oldP, oldTx, oldPr, oldK, oldExp)
            }
            val trimmed = jsonString.trim()
            var pCount = 0
            var txCount = 0
            var prodCount = 0
            var kurirCount = 0
            var expCount = 0

            val currentProducts = db.produkDao().getAll().first().toMutableList()
            val allDbKurirs = db.kurirDao().getAll().first().toMutableList()

            // Jika di database terdapat kurir palsu ("Kurir 1", "Kurir 2") akibat bug impor sebelumnya,
            // bersihkan dari daftar aktif apabila sudah ada kurir bernama asli.
            val realDbKurirs = allDbKurirs.filter { !isGenericFakeKurirName(it.nama) }
            val fakeDbKurirs = allDbKurirs.filter { isGenericFakeKurirName(it.nama) }
            if (realDbKurirs.isNotEmpty() && fakeDbKurirs.isNotEmpty()) {
                fakeDbKurirs.forEach { fake ->
                    db.kurirDao().delete(fake)
                    allDbKurirs.remove(fake)
                }
            }
            val currentKurirs = allDbKurirs

            if (currentProducts.isEmpty()) {
                val defId = db.produkDao().insert(Produk(nama_produk = "Galon Air RO 19L", harga = 6000.0)).toInt()
                currentProducts.add(Produk(id = defId, nama_produk = "Galon Air RO 19L", harga = 6000.0))
            }

            val jsonProdukIdMap = mutableMapOf<Int, Int>()
            val jsonKurirIdMap = mutableMapOf<Int, Int>()
            val existingCustomers = oldP.toMutableList()
            val existingTransactions = oldTx.map { it.transaksi }.toMutableList()
            val existingExpenses = oldExp.toMutableList()
            val importNow = System.currentTimeMillis()

            suspend fun upsertImportedPelanggan(
                oldId: Int,
                nama: String,
                alamat: String,
                noHp: String,
                kasbon: Double,
                hKhusus: Double?,
                idKurir: Int?
            ): Int {
                val cleanNama = nama.trim().ifBlank { "Pelanggan" }
                val cleanAlamat = alamat.trim()
                val cleanHp = noHp.trim()
                val match = (if (oldId > 0) existingCustomers.find { it.id == oldId && it.nama.equals(cleanNama, ignoreCase = true) } else null)
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
                        sisa_kasbon = kasbon,
                        harga_khusus = hKhusus ?: match.harga_khusus,
                        id_kurir_langganan = idKurir ?: match.id_kurir_langganan,
                        updatedAt = importNow
                    )
                    db.pelangganDao().update(updated)
                    val idx = existingCustomers.indexOfFirst { it.id == match.id }
                    if (idx >= 0) existingCustomers[idx] = updated
                    pCount++
                    return match.id
                }
                val toInsert = if (oldId > 0 && existingCustomers.none { it.id == oldId }) {
                    Pelanggan(
                        id = oldId,
                        nama = cleanNama,
                        alamat = cleanAlamat,
                        no_hp = cleanHp,
                        sisa_kasbon = kasbon,
                        harga_khusus = hKhusus,
                        id_kurir_langganan = idKurir,
                        updatedAt = importNow
                    )
                } else {
                    Pelanggan(
                        nama = cleanNama,
                        alamat = cleanAlamat,
                        no_hp = cleanHp,
                        sisa_kasbon = kasbon,
                        harga_khusus = hKhusus,
                        id_kurir_langganan = idKurir,
                        updatedAt = importNow
                    )
                }
                val newId = db.pelangganDao().insert(toInsert).toInt()
                existingCustomers.add(toInsert.copy(id = newId))
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
                sisaKasbon: Long
            ) {
                val match = (if (oldTxId > 0) existingTransactions.find { it.id == oldTxId && it.tanggal == tgl } else null)
                    ?: existingTransactions.find {
                        it.id_pelanggan == custId &&
                            it.tanggal == tgl &&
                            it.jumlah == jumlah &&
                            it.total_harga == totalHarga
                    }
                if (match != null) {
                    val updated = match.copy(
                        tanggal = tgl,
                        id_pelanggan = custId,
                        id_kurir = kurirId,
                        id_produk = prodId,
                        jumlah = jumlah,
                        harga_satuan = hargaSatuan,
                        total_harga = totalHarga,
                        total_komisi = totalKomisi,
                        metode_bayar = metode,
                        status = status,
                        uangMuka = uangMuka,
                        sisaKasbon = sisaKasbon,
                        updatedAt = importNow
                    )
                    db.transaksiDao().update(updated)
                    val idx = existingTransactions.indexOfFirst { it.id == match.id }
                    if (idx >= 0) existingTransactions[idx] = updated
                    txCount++
                    return
                }
                val toInsert = if (oldTxId > 0 && existingTransactions.none { it.id == oldTxId }) {
                    Transaksi(
                        id = oldTxId,
                        tanggal = tgl,
                        id_pelanggan = custId,
                        id_kurir = kurirId,
                        id_produk = prodId,
                        jumlah = jumlah,
                        harga_satuan = hargaSatuan,
                        total_harga = totalHarga,
                        total_komisi = totalKomisi,
                        metode_bayar = metode,
                        status = status,
                        uangMuka = uangMuka,
                        sisaKasbon = sisaKasbon,
                        updatedAt = importNow
                    )
                } else {
                    Transaksi(
                        tanggal = tgl,
                        id_pelanggan = custId,
                        id_kurir = kurirId,
                        id_produk = prodId,
                        jumlah = jumlah,
                        harga_satuan = hargaSatuan,
                        total_harga = totalHarga,
                        total_komisi = totalKomisi,
                        metode_bayar = metode,
                        status = status,
                        uangMuka = uangMuka,
                        sisaKasbon = sisaKasbon,
                        updatedAt = importNow
                    )
                }
                val newId = db.transaksiDao().insert(toInsert).toInt()
                existingTransactions.add(toInsert.copy(id = newId))
                txCount++
            }

            suspend fun upsertRealProduk(oldId: Int, name: String, price: Double = 6000.0): Int {
                val cleanName = name.trim().ifBlank { "Galon Air RO 19L" }
                val matchByName = currentProducts.find { it.nama_produk.equals(cleanName, ignoreCase = true) }
                if (matchByName != null) {
                    if (price > 0.0 && matchByName.harga != price) {
                        val updated = matchByName.copy(harga = price, updatedAt = System.currentTimeMillis())
                        db.produkDao().update(updated)
                        val idx = currentProducts.indexOfFirst { it.id == matchByName.id }
                        if (idx >= 0) currentProducts[idx] = updated
                    }
                    if (oldId > 0) jsonProdukIdMap[oldId] = matchByName.id
                    return matchByName.id
                }
                val matchById = if (oldId > 0) currentProducts.find { it.id == oldId } else null
                if (matchById != null) {
                    val updated = matchById.copy(
                        nama_produk = cleanName,
                        harga = if (price > 0.0) price else matchById.harga,
                        updatedAt = System.currentTimeMillis()
                    )
                    db.produkDao().update(updated)
                    val idx = currentProducts.indexOfFirst { it.id == matchById.id }
                    if (idx >= 0) currentProducts[idx] = updated
                    jsonProdukIdMap[oldId] = matchById.id
                    return matchById.id
                }
                val toInsert = if (oldId > 0) {
                    Produk(id = oldId, nama_produk = cleanName, harga = price)
                } else {
                    Produk(nama_produk = cleanName, harga = price)
                }
                val newId = db.produkDao().insert(toInsert).toInt()
                val newP = Produk(id = newId, nama_produk = cleanName, harga = price)
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
                aksesAktif: Boolean = true
            ): Int? {
                val cleanName = name.trim()
                val cleanEmail = email.trim().lowercase()
                if (isGenericFakeKurirName(cleanName)) {
                    // Jangan pernah membuat nama palsu seperti "Kurir 1" atau "Kurir 2".
                    // Petakan ke kurir asli berdasarkan ID jika sudah ada di database.
                    if (oldId > 0) {
                        jsonKurirIdMap[oldId]?.let { return it }
                        val existingById = currentKurirs.find { it.id == oldId && !isGenericFakeKurirName(it.nama) }
                        if (existingById != null) {
                            if (komisi > 0.0 && existingById.komisi_per_galon != komisi) {
                                val updated = existingById.copy(
                                    komisi_per_galon = komisi,
                                    updatedAt = System.currentTimeMillis()
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
                            !isGenericFakeKurirName(it.nama) && it.id !in usedTargetIds
                        }
                        if (fallbackReal != null) {
                            jsonKurirIdMap[oldId] = fallbackReal.id
                            return fallbackReal.id
                        }
                    }
                    return null
                }

                // Nama kurir asli (misal: "Mustopa", "Radi")
                val matchByName = currentKurirs.find { it.nama.equals(cleanName, ignoreCase = true) }
                if (matchByName != null) {
                    val effectiveEmail = if (cleanEmail.isNotBlank()) cleanEmail else matchByName.email
                    val effectiveAktif = if (cleanEmail.isNotBlank()) aksesAktif else matchByName.akses_aktif
                    if ((komisi > 0.0 && matchByName.komisi_per_galon != komisi) ||
                        effectiveEmail != matchByName.email ||
                        effectiveAktif != matchByName.akses_aktif ||
                        matchByName.skema_komisi != "TETAP" ||
                        matchByName.aturan_komisi.isNotEmpty()
                    ) {
                        val updated = matchByName.copy(
                            komisi_per_galon = if (komisi > 0.0) komisi else matchByName.komisi_per_galon,
                            email = effectiveEmail,
                            akses_aktif = effectiveAktif,
                            skema_komisi = "TETAP",
                            aturan_komisi = "",
                            updatedAt = System.currentTimeMillis()
                        )
                        db.kurirDao().update(updated)
                        val idx = currentKurirs.indexOfFirst { it.id == matchByName.id }
                        if (idx >= 0) currentKurirs[idx] = updated
                    }
                    if (oldId > 0) jsonKurirIdMap[oldId] = matchByName.id
                    return matchByName.id
                }

                if (oldId > 0) {
                    val existingById = currentKurirs.find { it.id == oldId }
                    if (existingById != null) {
                        // Update in-place agar ID tetap sama dan tidak memicu ForeignKey.SET_NULL
                        val updated = existingById.copy(
                            nama = cleanName,
                            komisi_per_galon = if (komisi > 0.0) komisi else existingById.komisi_per_galon,
                            email = if (cleanEmail.isNotBlank()) cleanEmail else existingById.email,
                            akses_aktif = if (cleanEmail.isNotBlank()) aksesAktif else existingById.akses_aktif,
                            skema_komisi = "TETAP",
                            aturan_komisi = "",
                            updatedAt = System.currentTimeMillis()
                        )
                        db.kurirDao().update(updated)
                        val idx = currentKurirs.indexOfFirst { it.id == oldId }
                        if (idx >= 0) currentKurirs[idx] = updated
                        jsonKurirIdMap[oldId] = oldId
                        kurirCount++
                        return oldId
                    } else {
                        val newId = db.kurirDao().insert(
                            Kurir(
                                id = oldId,
                                nama = cleanName,
                                komisi_per_galon = if (komisi > 0.0) komisi else 1000.0,
                                email = cleanEmail,
                                akses_aktif = aksesAktif,
                                skema_komisi = "TETAP",
                                aturan_komisi = ""
                            )
                        ).toInt()
                        val newK = Kurir(
                            id = newId,
                            nama = cleanName,
                            komisi_per_galon = if (komisi > 0.0) komisi else 1000.0,
                            email = cleanEmail,
                            akses_aktif = aksesAktif,
                            skema_komisi = "TETAP",
                            aturan_komisi = ""
                        )
                        currentKurirs.add(newK)
                        jsonKurirIdMap[oldId] = newId
                        kurirCount++
                        return newId
                    }
                }

                val newId = db.kurirDao().insert(
                    Kurir(
                        nama = cleanName,
                        komisi_per_galon = if (komisi > 0.0) komisi else 1000.0,
                        email = cleanEmail,
                        akses_aktif = aksesAktif,
                        skema_komisi = "TETAP",
                        aturan_komisi = ""
                    )
                ).toInt()
                val newK = Kurir(
                    id = newId,
                    nama = cleanName,
                    komisi_per_galon = if (komisi > 0.0) komisi else 1000.0,
                    email = cleanEmail,
                    akses_aktif = aksesAktif,
                    skema_komisi = "TETAP",
                    aturan_komisi = ""
                )
                currentKurirs.add(newK)
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

            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)

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
                        val nama = kObj.optString("nama", kObj.optString("nama_kurir", "")).trim()
                        val komisi = kObj.optDouble("komisi_per_galon", kObj.optDouble("komisi", 1000.0))
                        val email = kObj.optString("email", "").trim().lowercase()
                        val aksesAktif = kObj.optBoolean("akses_aktif", true)
                        upsertRealKurir(oldId, nama, komisi, email, aksesAktif)
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
                        val nama = pObj.optString("nama_produk", pObj.optString("nama", "")).trim()
                        val harga = pObj.optDouble("harga", 6000.0)
                        if (nama.isNotBlank()) {
                            upsertRealProduk(oldId, nama, harga)
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
                        val newCustId = upsertImportedPelanggan(
                            oldId = oldCustId,
                            nama = nama,
                            alamat = alamat,
                            noHp = noHp,
                            kasbon = kasbon,
                            hKhusus = hKhusus,
                            idKurir = idKurir
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
                                val tgl = tObj.optLong("tanggal", System.currentTimeMillis())
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
                                    sisaKasbon = sisaKasbon
                                )
                            }
                        }
                    }
                }

                // Flat Pengeluaran if present
                if (root.has("pengeluaran")) {
                    val expArray = root.getJSONArray("pengeluaran")
                    for (i in 0 until expArray.length()) {
                        val eObj = expArray.getJSONObject(i)
                        val oldExpId = extractKurirIdFromObj(eObj, "id", "id_pengeluaran")
                        val namaItem = eObj.optString("nama_item", "Operasional")
                        val nominal = eObj.optDouble("nominal", 0.0)
                        val kat = eObj.optString("kategori", "Operasional")
                        val tgl = eObj.optLong("tanggal", System.currentTimeMillis())
                        if (nominal > 0) {
                            val matchExp = (if (oldExpId > 0) existingExpenses.find { it.id == oldExpId && it.tanggal == tgl } else null)
                                ?: existingExpenses.find {
                                    it.tanggal == tgl &&
                                        it.nama_item.equals(namaItem.trim(), ignoreCase = true) &&
                                        it.nominal == nominal
                                }
                            if (matchExp != null) {
                                val updatedExp = matchExp.copy(
                                    tanggal = tgl,
                                    nama_item = namaItem.trim(),
                                    kategori = kat,
                                    nominal = nominal,
                                    updatedAt = importNow
                                )
                                db.pengeluaranDao().insert(updatedExp)
                                expCount++
                            } else {
                                val toInsertExp = if (oldExpId > 0 && existingExpenses.none { it.id == oldExpId }) {
                                    Pengeluaran(id = oldExpId, tanggal = tgl, nama_item = namaItem.trim(), kategori = kat, nominal = nominal, updatedAt = importNow)
                                } else {
                                    Pengeluaran(tanggal = tgl, nama_item = namaItem.trim(), kategori = kat, nominal = nominal, updatedAt = importNow)
                                }
                                val newExpId = db.pengeluaranDao().insert(toInsertExp).toInt()
                                existingExpenses.add(toInsertExp.copy(id = newExpId))
                                expCount++
                            }
                        }
                    }
                }
            } else if (trimmed.startsWith("[")) {
                val custArray = JSONArray(trimmed)
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
                    val newCustId = upsertImportedPelanggan(
                        oldId = oldCustId,
                        nama = nama,
                        alamat = alamat,
                        noHp = noHp,
                        kasbon = kasbon,
                        hKhusus = hKhusus,
                        idKurir = idKurir
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
                            val tgl = tObj.optLong("tanggal", System.currentTimeMillis())
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
                                sisaKasbon = sisaKasbon
                            )
                        }
                    }
                }
            } else {
                return ImportResult(success = false, message = "Format JSON tidak valid")
            }

            ImportResult(
                success = true,
                pelangganCount = pCount,
                transaksiCount = txCount,
                produkCount = prodCount,
                kurirCount = kurirCount,
                pengeluaranCount = expCount,
                message = "Impor selesai: $pCount pelanggan dan $txCount transaksi"
            )
        } catch (e: Exception) {
            ImportResult(success = false, message = "Gagal memproses JSON: ${e.localizedMessage ?: "Format tidak dikenali"}")
        }
    }
}

data class ImportResult(
    val success: Boolean,
    val pelangganCount: Int = 0,
    val transaksiCount: Int = 0,
    val produkCount: Int = 0,
    val kurirCount: Int = 0,
    val pengeluaranCount: Int = 0,
    val message: String = ""
)
