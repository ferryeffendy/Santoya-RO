package com.example.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
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

    // --- Struk Transaksi Penjualan ---
    fun generateSaleReceipt(detail: TransaksiDetail, customerKasbon: Double): String {
        val sb = StringBuilder()
        // Header
        sb.append(centerText("SANTOYA RO")).append("\n")
        sb.append(centerText("DEPOT AIR MINUM RO")).append("\n")
        sb.append(centerText("Segar - Murni - Higienis")).append("\n")
        sb.append(divider('-')).append("\n")

        // Details
        val date = FormatUtils.formatDateShort(detail.transaksi.tanggal)
        sb.append(leftRightText("Tgl:", date)).append("\n")
        sb.append(leftRightText("Pelanggan:", detail.pelanggan?.nama ?: "Umum")).append("\n")
        if (!detail.pelanggan?.alamat.isNullOrBlank()) {
            sb.append(leftRightText("Alamat:", detail.pelanggan!!.alamat)).append("\n")
        }
        if (detail.kurir != null) {
            sb.append(leftRightText("Kurir:", detail.kurir.nama)).append("\n")
        }
        sb.append(divider('-')).append("\n")

        // Item
        val produkNama = (detail.produk?.nama_produk ?: "Galon RO").take(LINE_WIDTH)
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
            else -> detail.transaksi.metode_bayar.ifBlank { "Cash" }
        }
        sb.append(leftRightText("Metode:", metodeText)).append("\n")
        sb.append(leftRightText("Status:", detail.transaksi.status.uppercase())).append("\n")
        if (isKasbon && dpPaid > 0L) {
            sb.append(leftRightText("Uang Muka:", FormatUtils.formatRupiah(dpPaid.toDouble()))).append("\n")
            sb.append(leftRightText("Sisa Nota:", FormatUtils.formatRupiah(detail.transaksi.remainingKasbonAmount()))).append("\n")
        }

        if (customerKasbon > 0) {
            sb.append(leftRightText("Sisa Kasbon:", FormatUtils.formatRupiah(customerKasbon))).append("\n")
        }

        sb.append(divider('-')).append("\n")
        sb.append(centerText("Terima Kasih")).append("\n")
        sb.append(centerText("Santoya RO Siap Antar")).append("\n")
        sb.append("\n\n\n")
        return sb.toString()
    }

    // --- Nota Pelunasan Kasbon ---
    fun generatePelunasanReceipt(pelanggan: Pelanggan, nominalBayar: Double, sisaKasbon: Double): String {
        val sb = StringBuilder()
        sb.append(centerText("SANTOYA RO")).append("\n")
        sb.append(centerText("BUKTI PELUNASAN KASBON")).append("\n")
        sb.append(centerText("Segar - Murni - Higienis")).append("\n")
        sb.append(divider('-')).append("\n")

        sb.append(leftRightText("Tgl:", FormatUtils.formatDateShort(System.currentTimeMillis()))).append("\n")
        sb.append(leftRightText("Pelanggan:", pelanggan.nama)).append("\n")
        if (pelanggan.alamat.isNotBlank()) {
            sb.append(leftRightText("Alamat:", pelanggan.alamat)).append("\n")
        }
        sb.append(divider('-')).append("\n")

        sb.append(leftRightText("Nominal Bayar:", FormatUtils.formatRupiah(nominalBayar))).append("\n")
        sb.append(leftRightText("Status:", if (sisaKasbon <= 0) "LUNAS" else "CICILAN")).append("\n")
        sb.append(leftRightText("Sisa Kasbon:", FormatUtils.formatRupiah(sisaKasbon))).append("\n")
        sb.append(divider('-')).append("\n")

        sb.append(centerText("TOTAL: ${FormatUtils.formatRupiah(nominalBayar).uppercase()}")).append("\n")
        sb.append(divider('-')).append("\n")

        sb.append(centerText("Terima Kasih")).append("\n")
        sb.append(centerText("Santoya RO Siap Antar")).append("\n")
        sb.append("\n\n\n")
        return sb.toString()
    }

    // --- Nota Tagihan Kasbon ---
    fun generateTagihanKasbon(pelanggan: Pelanggan, unpaidList: List<Transaksi>): String {
        val sb = StringBuilder()
        sb.append(centerText("SANTOYA RO")).append("\n")
        sb.append(centerText("RINCIAN TAGIHAN KASBON")).append("\n")
        sb.append(centerText("Segar - Murni - Higienis")).append("\n")
        sb.append(divider('-')).append("\n")

        sb.append(leftRightText("Pelanggan:", pelanggan.nama)).append("\n")
        if (pelanggan.alamat.isNotBlank()) {
            sb.append(leftRightText("Alamat:", pelanggan.alamat)).append("\n")
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
        sb.append(centerText("Terima Kasih")).append("\n")
        sb.append(centerText("Santoya RO Siap Antar")).append("\n")
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
            color = Color.parseColor("#0284C7")
            textSize = fontSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val sampleWidth = paint.measureText("M".repeat(LINE_WIDTH))
        val width = (sampleWidth + (horizontalPadding * 2)).toInt().coerceAtLeast(576)
        val startX = (width - sampleWidth) / 2f
        val height = ((rawLines.size * lineHeight) + (verticalPadding * 2) + 20f).toInt().coerceAtLeast(320)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val barPaint = Paint().apply { color = Color.parseColor("#0284C7") }
        canvas.drawRect(0f, 0f, width.toFloat(), 12f, barPaint)

        var y = verticalPadding + lineHeight
        rawLines.forEachIndexed { index, line ->
            val activePaint = when {
                index == 0 && line.contains("SANTOYA") -> headerPaint
                line.trim().startsWith("TOTAL:") -> boldPaint
                else -> paint
            }
            canvas.drawText(line, startX, y, activePaint)
            y += lineHeight
        }

        return bitmap
    }

    // --- Share to WhatsApp / Apps ---
    fun shareReceiptText(context: Context, text: String, phoneNumber: String? = null) {
        val cleanPhone = phoneNumber?.replace("[^0-9]".toRegex(), "")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (!cleanPhone.isNullOrBlank()) {
                val formattedPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
                putExtra("jid", "$formattedPhone@s.whatsapp.net")
            }
        }
        val chooser = Intent.createChooser(intent, "Bagikan Struk Santoya RO")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareReceiptImage(context: Context, receiptText: String, phoneNumber: String? = null) {
        try {
            val bitmap = renderReceiptBitmap(receiptText)
            val cachePath = File(context.cacheDir, "receipts")
            cachePath.mkdirs()
            val file = File(cachePath, "struk_santoya_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                clipData = android.content.ClipData.newRawUri("receipt", contentUri)
                putExtra(Intent.EXTRA_TEXT, "Struk Santoya RO")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (!phoneNumber.isNullOrBlank()) {
                    val clean = phoneNumber.replace("[^0-9]".toRegex(), "")
                    val formatted = if (clean.startsWith("0")) "62" + clean.substring(1) else clean
                    putExtra("jid", "$formatted@s.whatsapp.net")
                }
            }
            val chooser = Intent.createChooser(intent, "Kirim Gambar Struk Santoya")
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            shareReceiptText(context, receiptText, phoneNumber)
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
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            onStatus("Bluetooth tidak tersedia di perangkat ini.")
            return
        }
        if (!adapter.isEnabled) {
            onStatus("Harap aktifkan Bluetooth terlebih dahulu.")
            return
        }

        val bondedDevices = adapter.bondedDevices
        if (bondedDevices.isNullOrEmpty()) {
            onStatus("Tidak ada printer Bluetooth yang terpasang (paired). Pasangkan printer thermal terlebih dahulu.")
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

                onStatus("Berhasil mencetak struk ke ${device.name}!")
            } catch (e: Exception) {
                onStatus("Gagal mencetak: ${e.localizedMessage ?: "Koneksi terputus"}. Anda dapat membagikan via WhatsApp.")
            }
        }.start()
    }
}
