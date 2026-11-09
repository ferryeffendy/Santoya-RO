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

    // --- Line Formatting Helpers (32 columns) ---
    private fun center(text: String): String {
        if (text.length >= LINE_WIDTH) return text.substring(0, LINE_WIDTH)
        val pad = (LINE_WIDTH - text.length) / 2
        return " ".repeat(pad) + text
    }

    private fun twoColumns(left: String, right: String): String {
        val totalLen = left.length + right.length
        return if (totalLen >= LINE_WIDTH) {
            val maxLeft = LINE_WIDTH - right.length - 1
            val truncatedLeft = if (maxLeft > 0) left.take(maxLeft) else ""
            val spaces = (LINE_WIDTH - truncatedLeft.length - right.length).coerceAtLeast(1)
            truncatedLeft + " ".repeat(spaces) + right
        } else {
            val spaces = LINE_WIDTH - totalLen
            left + " ".repeat(spaces) + right
        }
    }

    private fun divider(char: Char = '-'): String = char.toString().repeat(LINE_WIDTH)

    // --- Struk Transaksi Penjualan ---
    fun generateSaleReceipt(detail: TransaksiDetail, customerKasbon: Double): String {
        val sb = StringBuilder()
        sb.append(center("SANTOYA RO")).append("\n")
        sb.append(center("DEPOT AIR MINUM RO")).append("\n")
        sb.append(center("Segar - Murni - Higienis")).append("\n")
        sb.append(divider('=')).append("\n")

        sb.append(twoColumns("No. Nota:", "#${detail.transaksi.id}")).append("\n")
        sb.append(twoColumns("Tgl:", FormatUtils.formatDate(detail.transaksi.tanggal))).append("\n")
        sb.append(twoColumns("Pelanggan:", detail.pelanggan?.nama ?: "Umum")).append("\n")
        if (!detail.pelanggan?.alamat.isNullOrBlank()) {
            sb.append(twoColumns("Alamat:", detail.pelanggan!!.alamat)).append("\n")
        }
        if (detail.kurir != null) {
            sb.append(twoColumns("Kurir:", detail.kurir.nama)).append("\n")
        }
        sb.append(divider('-')).append("\n")

        val produkNama = detail.produk?.nama_produk ?: "Galon Air RO"
        sb.append(produkNama).append("\n")
        val qtyPrice = "${detail.transaksi.jumlah} x ${FormatUtils.formatRupiah(detail.transaksi.harga_satuan)}"
        val subtotal = FormatUtils.formatRupiah(detail.transaksi.total_harga)
        sb.append(twoColumns(" $qtyPrice", subtotal)).append("\n")

        sb.append(divider('-')).append("\n")
        sb.append(twoColumns("TOTAL BELANJA:", FormatUtils.formatRupiah(detail.transaksi.total_harga))).append("\n")
        sb.append(twoColumns("Metode Bayar:", detail.transaksi.metode_bayar)).append("\n")
        sb.append(twoColumns("Status Nota:", detail.transaksi.status.uppercase())).append("\n")

        if (customerKasbon > 0) {
            sb.append(divider('.')).append("\n")
            sb.append(twoColumns("Sisa Kasbon:", FormatUtils.formatRupiah(customerKasbon))).append("\n")
        }

        sb.append(divider('=')).append("\n")
        sb.append(center("Terima Kasih Atas")).append("\n")
        sb.append(center("Kunjungan Anda!")).append("\n")
        sb.append(center("Santoya RO Siap Antar")).append("\n")
        sb.append("\n\n")
        return sb.toString()
    }

    // --- Nota Pelunasan Kasbon ---
    fun generatePelunasanReceipt(
        pelanggan: Pelanggan,
        nominalBayar: Double,
        sisaKasbon: Double
    ): String {
        val sb = StringBuilder()
        sb.append(center("SANTOYA RO")).append("\n")
        sb.append(center("BUKTI PELUNASAN KASBON")).append("\n")
        sb.append(divider('=')).append("\n")

        sb.append(twoColumns("Tgl:", FormatUtils.formatDate(System.currentTimeMillis()))).append("\n")
        sb.append(twoColumns("Pelanggan:", pelanggan.nama)).append("\n")
        if (pelanggan.alamat.isNotBlank()) {
            sb.append(twoColumns("Alamat:", pelanggan.alamat)).append("\n")
        }
        sb.append(divider('-')).append("\n")

        sb.append(twoColumns("Nominal Bayar:", FormatUtils.formatRupiah(nominalBayar))).append("\n")
        sb.append(twoColumns("Status:", if (sisaKasbon <= 0) "LUNAS LENGKAP" else "CICILAN")).append("\n")
        sb.append(twoColumns("Sisa Kasbon:", FormatUtils.formatRupiah(sisaKasbon))).append("\n")

        sb.append(divider('=')).append("\n")
        sb.append(center("Terima Kasih!")).append("\n")
        sb.append(center("Pembayaran Telah Diterima")).append("\n")
        sb.append("\n\n")
        return sb.toString()
    }

    // --- Nota Tagihan Kasbon ---
    fun generateTagihanKasbon(
        pelanggan: Pelanggan,
        unpaidList: List<Transaksi>
    ): String {
        val sb = StringBuilder()
        sb.append(center("SANTOYA RO")).append("\n")
        sb.append(center("RINCIAN TAGIHAN KASBON")).append("\n")
        sb.append(divider('=')).append("\n")

        sb.append(twoColumns("Pelanggan:", pelanggan.nama)).append("\n")
        sb.append(twoColumns("No. HP:", pelanggan.no_hp.ifBlank { "-" })).append("\n")
        sb.append(twoColumns("Tgl Cetak:", FormatUtils.formatDateShort(System.currentTimeMillis()))).append("\n")
        sb.append(divider('-')).append("\n")

        sb.append("Daftar Belum Lunas:\n")
        unpaidList.forEachIndexed { i, trx ->
            val tgl = FormatUtils.formatDateShort(trx.tanggal)
            val nominal = FormatUtils.formatRupiah(trx.total_harga)
            sb.append(twoColumns("${i + 1}. $tgl (${trx.jumlah} galon)", nominal)).append("\n")
        }

        sb.append(divider('-')).append("\n")
        sb.append(twoColumns("TOTAL TAGIHAN:", FormatUtils.formatRupiah(pelanggan.sisa_kasbon))).append("\n")
        sb.append(divider('=')).append("\n")
        sb.append(center("Mohon konfirmasi jika")).append("\n")
        sb.append(center("sudah melakukan transfer.")).append("\n")
        sb.append(center("Depot Santoya RO")).append("\n")
        sb.append("\n\n")
        return sb.toString()
    }

    // --- Convert Receipt Text to Crisp Bitmap Image ---
    fun renderReceiptBitmap(receiptText: String): Bitmap {
        val lines = receiptText.lines()
        val fontSize = 28f
        val lineHeight = 38f
        val padding = 32f

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = fontSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.parseColor("#0284C7")
            textSize = fontSize + 4f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val width = 576 // 58mm printer approx width in px
        val height = ((lines.size * lineHeight) + (padding * 2)).toInt().coerceAtLeast(300)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Draw top decorative bar
        val barPaint = Paint().apply { color = Color.parseColor("#0284C7") }
        canvas.drawRect(0f, 0f, width.toFloat(), 12f, barPaint)

        var y = padding + lineHeight
        lines.forEachIndexed { index, line ->
            val p = if (index < 2 && line.contains("SANTOYA")) headerPaint else paint
            canvas.drawText(line, padding, y, p)
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
                putExtra(Intent.EXTRA_TEXT, receiptText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (!phoneNumber.isNullOrBlank()) {
                    val clean = phoneNumber.replace("[^0-9]".toRegex(), "")
                    val formatted = if (clean.startsWith("0")) "62" + clean.substring(1) else clean
                    putExtra("jid", "$formatted@s.whatsapp.net")
                }
            }
            val chooser = Intent.createChooser(intent, "Kirim Gambar Struk Santoya")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback to text share
            shareReceiptText(context, receiptText, phoneNumber)
        }
    }

    // --- ESC/POS Thermal Bluetooth Printing Helper ---
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

                // Initialize ESC/POS
                out.write(byteArrayOf(0x1B, 0x40)) // ESC @ (Init)
                out.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left align

                // Write text bytes
                out.write(receiptText.toByteArray(charset("GBK")))

                // Feed 3 lines & cut if supported
                out.write(byteArrayOf(0x1B, 0x64, 0x03)) // ESC d 3
                out.flush()
                socket.close()

                onStatus("Berhasil mencetak struk ke ${device.name}!")
            } catch (e: Exception) {
                onStatus("Gagal mencetak: ${e.localizedMessage ?: "Koneksi terputus"}. Anda dapat membagikan via WhatsApp.")
            }
        }.start()
    }
}
