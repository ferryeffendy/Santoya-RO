package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.entity.DepotProfile
import com.example.data.entity.Pelanggan
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

object SantoyaReceiptHelper {

    private const val LINE_WIDTH = 32
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // --- Pure ASCII Line Formatting Helpers (32 columns) ---
    fun centerText(text: String, width: Int = LINE_WIDTH): String {
        val clean = text.trim()
        if (clean.length >= width) return clean.substring(0, width)
        val totalPad = width - clean.length
        val leftPad = totalPad / 2
        val rightPad = totalPad - leftPad
        return " ".repeat(leftPad) + clean + " ".repeat(rightPad)
    }

    /**
     * Membungkus teks panjang berdasarkan batas kata agar pas di kolom kertas thermal [width]
     * dan meratakan setiap barisnya ke tengah.
     */
    fun centerWrappedText(text: String, width: Int = LINE_WIDTH): List<String> {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        if (clean.isEmpty()) return emptyList()
        if (clean.length <= width) return listOf(centerText(clean, width))

        val lines = mutableListOf<String>()
        val words = clean.split(" ")
        val current = StringBuilder()

        for (word in words) {
            if (word.length > width) {
                if (current.isNotEmpty()) {
                    lines.add(centerText(current.toString(), width))
                    current.clear()
                }
                var index = 0
                while (index < word.length) {
                    val end = (index + width).coerceAtMost(word.length)
                    lines.add(centerText(word.substring(index, end), width))
                    index = end
                }
            } else if (current.isEmpty()) {
                current.append(word)
            } else if (current.length + 1 + word.length <= width) {
                current.append(' ').append(word)
            } else {
                lines.add(centerText(current.toString(), width))
                current.clear()
                current.append(word)
            }
        }
        if (current.isNotEmpty()) {
            lines.add(centerText(current.toString(), width))
        }
        return lines
    }

    fun leftRightText(left: String, right: String, width: Int = LINE_WIDTH): String {
        val cleanRight = right.trim()
        val cleanLeft = left.trimEnd()
        if (cleanRight.length >= width) {
            return cleanRight.takeLast(width)
        }
        val maxLeftLen = (width - cleanRight.length - 1).coerceAtLeast(0)
        val safeLeft = if (cleanLeft.length > maxLeftLen) cleanLeft.take(maxLeftLen) else cleanLeft
        val spaces = (width - safeLeft.length - cleanRight.length).coerceAtLeast(1)
        return safeLeft + " ".repeat(spaces) + cleanRight
    }

    private fun divider(char: Char = '-', width: Int = LINE_WIDTH): String =
        char.toString().repeat(width)

    private fun appendReceiptHeader(
        sb: StringBuilder,
        depotProfile: DepotProfile,
        subtitle: String
    ) {
        val depotTitle = depotProfile.effectiveDepotName().uppercase()
        centerWrappedText(depotTitle).forEach { line ->
            sb.append(line).append("\n")
        }
        val cleanAddress = depotProfile.address.trim()
        if (cleanAddress.isNotEmpty()) {
            centerWrappedText(cleanAddress).forEach { line ->
                sb.append(line).append("\n")
            }
        }
        val cleanPhone = depotProfile.phoneNumber.trim()
        if (cleanPhone.isNotEmpty()) {
            centerWrappedText("WA/Telp: $cleanPhone").forEach { line ->
                sb.append(line).append("\n")
            }
        }
        centerWrappedText(subtitle).forEach { line ->
            sb.append(line).append("\n")
        }
    }

    private fun appendReceiptFooter(
        sb: StringBuilder,
        depotProfile: DepotProfile,
        isProUser: Boolean
    ) {
        val cleanFooter = depotProfile.receiptFooter.trim()
        if (cleanFooter.isNotEmpty()) {
            centerWrappedText(cleanFooter).forEach { line ->
                sb.append(line).append("\n")
            }
        } else {
            sb.append(centerText("Terima Kasih")).append("\n")
        }
        if (!isProUser) {
            sb.append(centerText(DepotProfile.WATERMARK_TEXT)).append("\n")
        }
    }

    // --- Struk Transaksi Penjualan ---
    /**
     * Membuat teks struk transaksi penjualan 32-kolom dengan kop dinamis dari [depotProfile]
     * dan watermark TOYA otomatis jika [isProUser] bernilai false.
     */
    fun generateSaleReceipt(
        detail: TransaksiDetail,
        customerKasbon: Double,
        depotProfile: DepotProfile = DepotProfile(),
        isProUser: Boolean = depotProfile.isProUser()
    ): String {
        val sb = StringBuilder()
        appendReceiptHeader(sb, depotProfile, "NOTA PENJUALAN")
        sb.append(divider('-')).append("\n")

        // Details
        val date = FormatUtils.formatDateShort(detail.transaksi.tanggal)
        sb.append(leftRightText("Tgl:", date)).append("\n")
        sb.append(leftRightText("Pelanggan:", detail.pelanggan?.nama ?: "Pelanggan Umum")).append("\n")
        val pelangganAlamat = KaryawanScopeHelper.cleanPelangganAlamat(detail.pelanggan?.alamat.orEmpty())
        if (pelangganAlamat.isNotBlank()) {
            sb.append(leftRightText("Alamat:", pelangganAlamat)).append("\n")
        }
        if (detail.kurir != null) {
            sb.append(leftRightText("Kurir:", detail.kurir.nama)).append("\n")
        }
        sb.append(divider('-')).append("\n")

        // Item
        val defaultItemLabel = if (detail.transaksi.waterType == Transaksi.WATER_TYPE_MINERAL ||
            depotProfile.depotType == DepotProfile.TYPE_MINERAL
        ) {
            "Galon Air Mineral"
        } else {
            "Galon RO"
        }
        val rawProdukNama = detail.produk?.nama_produk?.trim().takeUnless { it.isNullOrBlank() } ?: defaultItemLabel
        val formattedProdukNama = if (depotProfile.isBothType()) {
            val isMineralTx = detail.transaksi.waterType == Transaksi.WATER_TYPE_MINERAL
            when {
                isMineralTx && !rawProdukNama.contains("mineral", ignoreCase = true) ->
                    "Galon Air Mineral"
                !isMineralTx && !rawProdukNama.contains("ro", ignoreCase = true) ->
                    "Galon Air RO"
                else -> rawProdukNama
            }
        } else {
            rawProdukNama
        }
        val produkNama = formattedProdukNama.take(LINE_WIDTH)
        sb.append(produkNama).append("\n")
        val qtyPrice = "${detail.transaksi.jumlah} x ${FormatUtils.formatRupiah(detail.transaksi.harga_satuan)}"
        val subtotal = FormatUtils.formatRupiah(detail.transaksi.total_harga)
        sb.append(leftRightText(qtyPrice, subtotal)).append("\n")
        sb.append(divider('-')).append("\n")

        // Total Belanja
        sb.append(centerText("TOTAL: ${FormatUtils.formatRupiah(detail.transaksi.total_harga).uppercase()}")).append("\n")
        sb.append(divider('-')).append("\n")

        // Payment Info
        val isKasbon = detail.transaksi.status.equals("Kasbon", ignoreCase = true)
        val dpPaid = detail.transaksi.uangMuka
        val metodeText = when {
            isKasbon && dpPaid > 0L -> "Bayar Sebagian"
            isKasbon -> "- (Kasbon)"
            else -> FormatUtils.formatMetodeBayarLabel(detail.transaksi.metode_bayar)
        }
        sb.append(leftRightText("Metode:", metodeText)).append("\n")
        sb.append(leftRightText("Status:", detail.transaksi.status.uppercase())).append("\n")
        if (isKasbon && dpPaid > 0L) {
            sb.append(leftRightText("Uang Muka:", FormatUtils.formatRupiah(dpPaid.toDouble()))).append("\n")
            sb.append(leftRightText("Sisa Nota:", FormatUtils.formatRupiah(detail.transaksi.remainingKasbonAmount()))).append("\n")
        }

        if (customerKasbon > 0) {
            sb.append(leftRightText("Sisa Kasbon Aktif:", FormatUtils.formatRupiah(customerKasbon))).append("\n")
        }

        sb.append(divider('-')).append("\n")
        appendReceiptFooter(sb, depotProfile, isProUser)
        sb.append("\n\n\n")
        return sb.toString()
    }

    // --- Nota Pelunasan Kasbon ---
    /**
     * Membuat teks struk bukti pelunasan kasbon dengan kop dinamis dari [depotProfile]
     * dan watermark TOYA otomatis jika [isProUser] bernilai false.
     */
    fun generatePelunasanReceipt(
        pelanggan: Pelanggan,
        nominalBayar: Double,
        sisaKasbon: Double,
        uangDiterima: Double? = null,
        kembalian: Double = 0.0,
        depotProfile: DepotProfile = DepotProfile(),
        isProUser: Boolean = depotProfile.isProUser()
    ): String {
        val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(pelanggan.alamat)
        val sb = StringBuilder()
        appendReceiptHeader(sb, depotProfile, "BUKTI PELUNASAN KASBON")
        sb.append(divider('-')).append("\n")

        sb.append(leftRightText("Tgl:", FormatUtils.formatDateShort(System.currentTimeMillis()))).append("\n")
        sb.append(leftRightText("Pelanggan:", pelanggan.nama)).append("\n")
        if (cleanAlamat.isNotBlank()) {
            sb.append(leftRightText("Alamat:", cleanAlamat)).append("\n")
        }
        sb.append(divider('-')).append("\n")

        sb.append(leftRightText("Nominal Bayar:", FormatUtils.formatRupiah(nominalBayar))).append("\n")
        if (uangDiterima != null && kembalian > 0.0) {
            sb.append(leftRightText("Uang Diterima:", FormatUtils.formatRupiah(uangDiterima))).append("\n")
            sb.append(leftRightText("Kembalian:", FormatUtils.formatRupiah(kembalian))).append("\n")
        }
        sb.append(leftRightText("Status:", if (sisaKasbon <= 0) "LUNAS" else "CICILAN")).append("\n")
        sb.append(leftRightText("Sisa Kasbon:", FormatUtils.formatRupiah(sisaKasbon))).append("\n")
        sb.append(divider('-')).append("\n")

        sb.append(centerText("TOTAL: ${FormatUtils.formatRupiah(nominalBayar).uppercase()}")).append("\n")
        sb.append(divider('-')).append("\n")

        appendReceiptFooter(sb, depotProfile, isProUser)
        sb.append("\n\n\n")
        return sb.toString()
    }

    // --- Nota Tagihan Kasbon ---
    /**
     * Membuat teks nota rincian tagihan kasbon dengan kop dinamis dari [depotProfile]
     * dan watermark TOYA otomatis jika [isProUser] bernilai false.
     */
    fun generateTagihanKasbon(
        pelanggan: Pelanggan,
        unpaidList: List<Transaksi>,
        depotProfile: DepotProfile = DepotProfile(),
        isProUser: Boolean = depotProfile.isProUser()
    ): String {
        val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(pelanggan.alamat)
        val sb = StringBuilder()
        appendReceiptHeader(sb, depotProfile, "RINCIAN TAGIHAN KASBON")
        sb.append(divider('-')).append("\n")

        sb.append(leftRightText("Pelanggan:", pelanggan.nama)).append("\n")
        if (cleanAlamat.isNotBlank()) {
            sb.append(leftRightText("Alamat:", cleanAlamat)).append("\n")
        }
        sb.append(leftRightText("Tgl Cetak:", FormatUtils.formatDateShort(System.currentTimeMillis()))).append("\n")
        sb.append(divider('-')).append("\n")

        unpaidList.forEachIndexed { i, trx ->
            val leftLabel = "${i + 1}. ${FormatUtils.formatDateShort(trx.tanggal)} (${trx.jumlah} gln)"
            sb.append(leftRightText(leftLabel, FormatUtils.formatRupiah(trx.remainingKasbonAmount()))).append("\n")
        }

        sb.append(divider('-')).append("\n")
        sb.append(centerText("TOTAL: ${FormatUtils.formatRupiah(pelanggan.sisa_kasbon).uppercase()}")).append("\n")
        sb.append(divider('-')).append("\n")
        appendReceiptFooter(sb, depotProfile, isProUser)
        sb.append("\n\n\n")
        return sb.toString()
    }

    // --- Convert Receipt Text to Crisp Bitmap Image ---
    fun renderReceiptBitmap(receiptText: String): Bitmap {
        val rawLines = receiptText.trimEnd('\n').lines()
        val fontSize = 26f
        val lineHeight = 38f
        val horizontalPadding = 32f
        val verticalPadding = 36f

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = fontSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = fontSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.parseColor("#1B6FC0")
            textSize = fontSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val watermarkPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sampleWidth = paint.measureText("M".repeat(LINE_WIDTH))
        val width = (sampleWidth + (horizontalPadding * 2)).toInt().coerceAtLeast(576)
        val startX = (width - sampleWidth) / 2f
        val height = ((rawLines.size * lineHeight) + (verticalPadding * 2) + 20f).toInt().coerceAtLeast(320)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val barPaint = Paint().apply { color = Color.parseColor("#1B6FC0") }
        canvas.drawRect(0f, 0f, width.toFloat(), 12f, barPaint)

        var y = verticalPadding + lineHeight
        rawLines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            val activePaint = when {
                index == 0 -> headerPaint
                trimmed == DepotProfile.WATERMARK_TEXT -> watermarkPaint
                trimmed.startsWith("TOTAL:") -> boldPaint
                else -> paint
            }
            if (trimmed == DepotProfile.WATERMARK_TEXT) {
                val wmWidth = activePaint.measureText(trimmed)
                val wmX = ((width - wmWidth) / 2f).coerceAtLeast(horizontalPadding)
                canvas.drawText(trimmed, wmX, y, activePaint)
            } else {
                canvas.drawText(line, startX, y, activePaint)
            }
            y += lineHeight
        }

        return bitmap
    }

    // --- Share to WhatsApp / Apps ---
    fun shareReceiptText(
        context: Context,
        text: String,
        phoneNumber: String? = null,
        depotName: String = DepotProfile.DEFAULT_DEPOT_NAME
    ) {
        val safeDepot = depotName.trim().ifBlank { DepotProfile.DEFAULT_DEPOT_NAME }
        val cleanPhone = phoneNumber?.replace("[^0-9]".toRegex(), "")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (!cleanPhone.isNullOrBlank()) {
                val formattedPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
                putExtra("jid", "$formattedPhone@s.whatsapp.net")
            }
        }
        val chooser = Intent.createChooser(intent, "Bagikan Struk $safeDepot")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private const val RECEIPT_CACHE_TTL_MILLIS = 24L * 60L * 60L * 1000L

    private fun pruneStaleReceiptFiles(dir: File, nowMillis: Long = System.currentTimeMillis()) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isFile && (nowMillis - f.lastModified()) > RECEIPT_CACHE_TTL_MILLIS) {
                runCatching { f.delete() }
            }
        }
    }

    fun shareReceiptImage(
        context: Context,
        receiptText: String,
        phoneNumber: String? = null,
        depotName: String = DepotProfile.DEFAULT_DEPOT_NAME
    ) {
        val safeDepot = depotName.trim().ifBlank { DepotProfile.DEFAULT_DEPOT_NAME }
        try {
            val bitmap = renderReceiptBitmap(receiptText)
            val cachePath = File(context.cacheDir, "receipts")
            cachePath.mkdirs()
            pruneStaleReceiptFiles(cachePath)
            val file = File(cachePath, "struk_nota_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                clipData = ClipData.newRawUri("receipt", contentUri)
                putExtra(Intent.EXTRA_TEXT, "Struk $safeDepot")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (!phoneNumber.isNullOrBlank()) {
                    val clean = phoneNumber.replace("[^0-9]".toRegex(), "")
                    val formatted = if (clean.startsWith("0")) "62" + clean.substring(1) else clean
                    putExtra("jid", "$formatted@s.whatsapp.net")
                }
            }
            val chooser = Intent.createChooser(intent, "Kirim Gambar Struk $safeDepot")
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.w("SantoyaReceiptHelper", "Gagal membagikan gambar struk, beralih ke teks biasa: ${e.message}")
            shareReceiptText(context, receiptText, phoneNumber, safeDepot)
        }
    }

    // --- Pure Text Thermal Bluetooth Printing Helper ---
    @SuppressLint("MissingPermission")
    fun printViaBluetooth(
        context: Context,
        receiptText: String,
        targetDeviceAddress: String? = null,
        onStatus: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            onStatus("Izin Bluetooth belum diberikan. Aktifkan izin Bluetooth di Pengaturan aplikasi.")
            return
        }

        val adapter = try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (e: SecurityException) {
            onStatus("Izin Bluetooth ditolak oleh sistem.")
            return
        }
        if (adapter == null) {
            onStatus("Bluetooth tidak tersedia di perangkat ini")
            return
        }
        if (!adapter.isEnabled) {
            onStatus("Aktifkan Bluetooth terlebih dahulu")
            return
        }

        val bondedDevices = try {
            adapter.bondedDevices
        } catch (e: SecurityException) {
            onStatus("Izin Bluetooth ditolak oleh sistem.")
            return
        }
        if (bondedDevices.isNullOrEmpty()) {
            onStatus("Printer Bluetooth belum tersambung. Sambungkan dulu di Pengaturan HP.")
            return
        }

        val device: BluetoothDevice = if (!targetDeviceAddress.isNullOrBlank()) {
            bondedDevices.firstOrNull { it.address == targetDeviceAddress } ?: bondedDevices.first()
        } else {
            bondedDevices.first()
        }

        Thread {
            try {
                onStatus("Menghubungkan ke ${device.name ?: device.address}...")
                val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                val out: OutputStream = socket.outputStream

                // Send pure ASCII text bytes directly (already formatted with 32-char padding & 3 trailing newlines)
                out.write(receiptText.toByteArray(Charsets.US_ASCII))
                out.flush()
                socket.close()

                onStatus("Struk dicetak ke ${device.name ?: "printer"}")
            } catch (e: Exception) {
                onStatus("Gagal terhubung ke printer. Pastikan printer menyala dan sudah dipasangkan.")
            }
        }.start()
    }
}
