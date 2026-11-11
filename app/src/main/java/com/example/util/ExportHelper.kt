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

    // --- Nested JSON Export ---
    fun generateNestedJson(
        pelangganList: List<Pelanggan>,
        transaksiList: List<TransaksiDetail>,
        produkList: List<Produk>,
        kurirList: List<Kurir>,
        pengeluaranList: List<Pengeluaran>
    ): String {
        val root = JSONObject()
        root.put("app", "Santoya RO POS")
        root.put("version", "2.0")
        root.put("export_timestamp", System.currentTimeMillis())
        root.put("export_date", FormatUtils.formatDate(System.currentTimeMillis()))

        // Nested Pelanggan + Riwayat Transaksi
        val pelangganArray = JSONArray()
        pelangganList.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("nama", p.nama)
            pObj.put("alamat", p.alamat)
            pObj.put("no_hp", p.no_hp)
            pObj.put("sisa_kasbon", p.sisa_kasbon)
            p.harga_khusus?.let { pObj.put("harga_khusus", it) }
            p.id_kurir_langganan?.let { pObj.put("id_kurir_langganan", it) }

            val trxArray = JSONArray()
            val custTrx = transaksiList.filter { it.transaksi.id_pelanggan == p.id }
            custTrx.forEach { t ->
                val tObj = JSONObject()
                tObj.put("id", t.transaksi.id)
                tObj.put("tanggal", t.transaksi.tanggal)
                tObj.put("tanggal_formatted", FormatUtils.formatDate(t.transaksi.tanggal))
                tObj.put("produk", t.produk?.nama_produk ?: "-")
                tObj.put("kurir", t.kurir?.nama ?: "-")
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

        // Master Produk
        val prodArray = JSONArray()
        produkList.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("nama_produk", p.nama_produk)
            obj.put("harga", p.harga)
            prodArray.put(obj)
        }
        root.put("master_produk", prodArray)

        // Master Kurir
        val kurirArray = JSONArray()
        kurirList.forEach { k ->
            val obj = JSONObject()
            obj.put("id", k.id)
            obj.put("nama", k.nama)
            obj.put("komisi_per_galon", k.komisi_per_galon)
            kurirArray.put(obj)
        }
        root.put("master_kurir", kurirArray)

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
        val sb = StringBuilder()
        sb.append("=== LAPORAN TRANSAKSI SANTOYA RO ===\n")
        sb.append("ID,Tanggal,Pelanggan,Alamat,NoHP,Produk,Jumlah,HargaSatuan,TotalHarga,UangMuka,SisaKasbon,Kurir,TotalKomisi,MetodeBayar,Status\n")
        transaksiList.forEach { t ->
            val trx = t.transaksi
            val tgl = FormatUtils.formatDate(trx.tanggal).replace(",", " ")
            val custName = (t.pelanggan?.nama ?: "Umum").replace(",", ";")
            val custAddr = (t.pelanggan?.alamat ?: "-").replace(",", ";")
            val custPhone = t.pelanggan?.no_hp ?: "-"
            val prod = (t.produk?.nama_produk ?: "Galon RO").replace(",", ";")
            val kurir = (t.kurir?.nama ?: "-").replace(",", ";")
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

        val totalGalon = transaksiList.sumOf { it.transaksi.jumlah }
        val pendapatanLunas = transaksiList.sumOf {
            if (it.transaksi.status.equals("Lunas", ignoreCase = true)) {
                it.transaksi.total_harga
            } else {
                it.transaksi.uangMuka.toDouble()
            }
        }
        val piutangKasbon = transaksiList
            .filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
            .sumOf { it.transaksi.remainingKasbonAmount() }
        val totalKomisi = transaksiList.sumOf { it.transaksi.total_komisi }
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
            putExtra(Intent.EXTRA_SUBJECT, "Backup Data Santoya RO: $filename")
            putExtra(Intent.EXTRA_TEXT, "File backup ekspor Santoya RO ($filename).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Simpan / Bagikan Backup Santoya RO")
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

    suspend fun importJsonData(
        context: Context,
        db: com.example.data.database.AppDatabase,
        jsonString: String
    ): ImportResult {
        return try {
            val trimmed = jsonString.trim()
            var pCount = 0
            var txCount = 0
            var prodCount = 0
            var kurirCount = 0
            var expCount = 0

            // Ensure product list cached
            val currentProducts = db.produkDao().getAll().first().toMutableList()
            val currentKurirs = db.kurirDao().getAll().first().toMutableList()

            // Ensure at least one product exists
            if (currentProducts.isEmpty()) {
                val defId = db.produkDao().insert(Produk(nama_produk = "Galon Air RO 19L", harga = 6000.0)).toInt()
                currentProducts.add(Produk(id = defId, nama_produk = "Galon Air RO 19L", harga = 6000.0))
            }

            suspend fun resolveProdukId(name: String, price: Double = 6000.0): Int {
                val match = currentProducts.find { it.nama_produk.equals(name, ignoreCase = true) }
                if (match != null) return match.id
                val newId = db.produkDao().insert(Produk(nama_produk = name, harga = price)).toInt()
                val newP = Produk(id = newId, nama_produk = name, harga = price)
                currentProducts.add(newP)
                prodCount++
                return newId
            }

            suspend fun resolveKurirId(name: String): Int? {
                if (name.isBlank() || name == "-") return null
                val match = currentKurirs.find { it.nama.equals(name, ignoreCase = true) }
                if (match != null) return match.id
                val newId = db.kurirDao().insert(Kurir(nama = name, komisi_per_galon = 1000.0)).toInt()
                val newK = Kurir(id = newId, nama = name, komisi_per_galon = 1000.0)
                currentKurirs.add(newK)
                kurirCount++
                return newId
            }

            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)

                // Master Produk if provided
                if (root.has("master_produk")) {
                    val prodArray = root.getJSONArray("master_produk")
                    for (i in 0 until prodArray.length()) {
                        val pObj = prodArray.getJSONObject(i)
                        val nama = pObj.optString("nama_produk", "")
                        val harga = pObj.optDouble("harga", 6000.0)
                        if (nama.isNotBlank() && currentProducts.none { it.nama_produk.equals(nama, ignoreCase = true) }) {
                            val id = db.produkDao().insert(Produk(nama_produk = nama, harga = harga)).toInt()
                            currentProducts.add(Produk(id = id, nama_produk = nama, harga = harga))
                            prodCount++
                        }
                    }
                }

                // Master Kurir if provided
                if (root.has("master_kurir")) {
                    val kArray = root.getJSONArray("master_kurir")
                    for (i in 0 until kArray.length()) {
                        val kObj = kArray.getJSONObject(i)
                        val nama = kObj.optString("nama", "")
                        val komisi = kObj.optDouble("komisi_per_galon", 1000.0)
                        if (nama.isNotBlank() && currentKurirs.none { it.nama.equals(nama, ignoreCase = true) }) {
                            val id = db.kurirDao().insert(Kurir(nama = nama, komisi_per_galon = komisi)).toInt()
                            currentKurirs.add(Kurir(id = id, nama = nama, komisi_per_galon = komisi))
                            kurirCount++
                        }
                    }
                }

                // Pelanggan Nested or array
                val custArray = when {
                    root.has("data_pelanggan_nested") -> root.getJSONArray("data_pelanggan_nested")
                    root.has("pelanggan") -> root.getJSONArray("pelanggan")
                    root.has("customers") -> root.getJSONArray("customers")
                    else -> null
                }

                if (custArray != null) {
                    for (i in 0 until custArray.length()) {
                        val cObj = custArray.getJSONObject(i)
                        val nama = cObj.optString("nama", "Pelanggan")
                        val alamat = cObj.optString("alamat", "")
                        val noHp = cObj.optString("no_hp", "")
                        val kasbon = cObj.optDouble("sisa_kasbon", 0.0)
                        val hKhusus = if (cObj.has("harga_khusus")) cObj.optDouble("harga_khusus") else null
                        val idKurir = if (cObj.has("id_kurir_langganan")) cObj.optInt("id_kurir_langganan") else null

                        val newCustId = db.pelangganDao().insert(
                            Pelanggan(nama = nama, alamat = alamat, no_hp = noHp, sisa_kasbon = kasbon, harga_khusus = hKhusus, id_kurir_langganan = idKurir)
                        ).toInt()
                        pCount++

                        // Riwayat Transaksi nested
                        val trxArray = when {
                            cObj.has("riwayat_transaksi") -> cObj.getJSONArray("riwayat_transaksi")
                            cObj.has("transaksi") -> cObj.getJSONArray("transaksi")
                            else -> null
                        }

                        if (trxArray != null) {
                            for (j in 0 until trxArray.length()) {
                                val tObj = trxArray.getJSONObject(j)
                                val tgl = tObj.optLong("tanggal", System.currentTimeMillis())
                                val prodName = tObj.optString("produk", "Galon Air RO 19L")
                                val hargaSatuan = tObj.optDouble("harga_satuan", 6000.0)
                                val prodId = resolveProdukId(prodName, hargaSatuan)

                                val kurirName = tObj.optString("kurir", "")
                                val kurirId = resolveKurirId(kurirName)

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

                                db.transaksiDao().insert(
                                    Transaksi(
                                        tanggal = tgl,
                                        id_pelanggan = newCustId,
                                        id_kurir = kurirId,
                                        id_produk = prodId,
                                        jumlah = jumlah,
                                        harga_satuan = hargaSatuan,
                                        total_harga = totalHarga,
                                        total_komisi = totalKomisi,
                                        metode_bayar = metode,
                                        status = status,
                                        uangMuka = uangMuka,
                                        sisaKasbon = sisaKasbon
                                    )
                                )
                                txCount++
                            }
                        }
                    }
                }

                // Flat Pengeluaran if present
                if (root.has("pengeluaran")) {
                    val expArray = root.getJSONArray("pengeluaran")
                    for (i in 0 until expArray.length()) {
                        val eObj = expArray.getJSONObject(i)
                        val namaItem = eObj.optString("nama_item", "Operasional")
                        val nominal = eObj.optDouble("nominal", 0.0)
                        val kat = eObj.optString("kategori", "Operasional")
                        val tgl = eObj.optLong("tanggal", System.currentTimeMillis())
                        if (nominal > 0) {
                            db.pengeluaranDao().insert(
                                Pengeluaran(tanggal = tgl, nama_item = namaItem, kategori = kat, nominal = nominal)
                            )
                            expCount++
                        }
                    }
                }
            } else if (trimmed.startsWith("[")) {
                // Root is a list of customers
                val custArray = JSONArray(trimmed)
                for (i in 0 until custArray.length()) {
                    val cObj = custArray.getJSONObject(i)
                    val nama = cObj.optString("nama", "Pelanggan")
                    val alamat = cObj.optString("alamat", "")
                    val noHp = cObj.optString("no_hp", "")
                    val kasbon = cObj.optDouble("sisa_kasbon", 0.0)
                    val hKhusus = if (cObj.has("harga_khusus")) cObj.optDouble("harga_khusus") else null
                    val idKurir = if (cObj.has("id_kurir_langganan")) cObj.optInt("id_kurir_langganan") else null

                    val newCustId = db.pelangganDao().insert(
                        Pelanggan(nama = nama, alamat = alamat, no_hp = noHp, sisa_kasbon = kasbon, harga_khusus = hKhusus, id_kurir_langganan = idKurir)
                    ).toInt()
                    pCount++

                    if (cObj.has("riwayat_transaksi")) {
                        val trxArray = cObj.getJSONArray("riwayat_transaksi")
                        for (j in 0 until trxArray.length()) {
                            val tObj = trxArray.getJSONObject(j)
                            val tgl = tObj.optLong("tanggal", System.currentTimeMillis())
                            val prodName = tObj.optString("produk", "Galon Air RO 19L")
                            val hargaSatuan = tObj.optDouble("harga_satuan", 6000.0)
                            val prodId = resolveProdukId(prodName, hargaSatuan)

                            val kurirName = tObj.optString("kurir", "")
                            val kurirId = resolveKurirId(kurirName)

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

                            db.transaksiDao().insert(
                                Transaksi(
                                    tanggal = tgl,
                                    id_pelanggan = newCustId,
                                    id_kurir = kurirId,
                                    id_produk = prodId,
                                    jumlah = jumlah,
                                    harga_satuan = hargaSatuan,
                                    total_harga = totalHarga,
                                    total_komisi = totalKomisi,
                                    metode_bayar = metode,
                                    status = status,
                                    uangMuka = uangMuka,
                                    sisaKasbon = sisaKasbon
                                )
                            )
                            txCount++
                        }
                    }
                }
            } else {
                return ImportResult(success = false, message = "Format JSON tidak valid.")
            }

            ImportResult(
                success = true,
                pelangganCount = pCount,
                transaksiCount = txCount,
                produkCount = prodCount,
                kurirCount = kurirCount,
                pengeluaranCount = expCount,
                message = "Berhasil mengimpor $pCount pelanggan dan $txCount transaksi!"
            )
        } catch (e: Exception) {
            ImportResult(success = false, message = "Gagal memproses JSON: ${e.localizedMessage}")
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
