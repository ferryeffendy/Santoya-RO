package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Pelanggan
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.util.FormatUtils
import com.example.util.SantoyaReceiptHelper
import com.example.viewmodel.ActiveReceiptDialog

private data class ReceiptLineItemUi(
    val name: String,
    val qtyPrice: String,
    val subtotal: String
)

private data class ParsedReceiptUi(
    val subtitle1: String = "DEPOT AIR MINUM RO",
    val subtitle2: String = "Segar • Murni • Higienis",
    val noNota: String? = null,
    val tanggal: String? = null,
    val pelanggan: String? = null,
    val alamat: String? = null,
    val kurir: String? = null,
    val items: List<ReceiptLineItemUi> = emptyList(),
    val totalLabel: String = "TOTAL BELANJA",
    val totalValue: String = "Rp 0",
    val metodeBayar: String? = null,
    val statusNota: String = "LUNAS",
    val sisaKasbon: String? = null
)

private fun extractReceiptUi(dialogData: ActiveReceiptDialog): ParsedReceiptUi {
    val lines = dialogData.receiptText.lines().map { it.trim() }
    val parsedSisaKasbon = lines.firstOrNull { it.startsWith("Sisa Kasbon:") }
        ?.substringAfter("Sisa Kasbon:")?.trim()

    when (val rawData = dialogData.data) {
        is TransaksiDetail -> {
            val trx = rawData.transaksi
            val cust = rawData.pelanggan
            val kurir = rawData.kurir
            val produkName = rawData.produk?.nama_produk ?: "Galon RO"
            val qtyPrice = "${trx.jumlah} x ${FormatUtils.formatRupiah(trx.harga_satuan)}"
            val totalStr = FormatUtils.formatRupiah(trx.total_harga)
            return ParsedReceiptUi(
                noNota = "#${trx.id}",
                tanggal = FormatUtils.formatDate(trx.tanggal),
                pelanggan = cust?.nama ?: "Umum",
                alamat = cust?.alamat?.takeIf { it.isNotBlank() },
                kurir = kurir?.nama?.takeIf { it.isNotBlank() },
                items = listOf(
                    ReceiptLineItemUi(
                        name = produkName,
                        qtyPrice = qtyPrice,
                        subtotal = totalStr
                    )
                ),
                totalLabel = "TOTAL BELANJA",
                totalValue = totalStr,
                metodeBayar = trx.metode_bayar,
                statusNota = trx.status.uppercase(),
                sisaKasbon = parsedSisaKasbon
            )
        }
        is Triple<*, *, *> -> {
            val cust = rawData.first as? Pelanggan
            val nominal = (rawData.second as? Double) ?: 0.0
            val sisa = (rawData.third as? Double) ?: 0.0
            val nominalStr = FormatUtils.formatRupiah(nominal)
            return ParsedReceiptUi(
                subtitle1 = "BUKTI PELUNASAN KASBON",
                subtitle2 = "Segar • Murni • Higienis",
                tanggal = FormatUtils.formatDate(System.currentTimeMillis()),
                pelanggan = cust?.nama ?: "Pelanggan",
                alamat = cust?.alamat?.takeIf { it.isNotBlank() },
                items = listOf(
                    ReceiptLineItemUi(
                        name = "Pelunasan Kasbon",
                        qtyPrice = "1 x $nominalStr",
                        subtotal = nominalStr
                    )
                ),
                totalLabel = "TOTAL BAYAR",
                totalValue = nominalStr,
                statusNota = if (sisa <= 0.0) "LUNAS" else "KASBON",
                sisaKasbon = FormatUtils.formatRupiah(sisa)
            )
        }
        is Pair<*, *> -> {
            val cust = rawData.first as? Pelanggan
            val unpaidList = (rawData.second as? List<*>)?.filterIsInstance<Transaksi>().orEmpty()
            val mappedItems = unpaidList.map { trx ->
                ReceiptLineItemUi(
                    name = "Nota #${trx.id} (${FormatUtils.formatDateShort(trx.tanggal)})",
                    qtyPrice = "${trx.jumlah} x ${FormatUtils.formatRupiah(trx.harga_satuan)}",
                    subtotal = FormatUtils.formatRupiah(trx.total_harga)
                )
            }
            return ParsedReceiptUi(
                subtitle1 = "RINCIAN TAGIHAN KASBON",
                subtitle2 = "Segar • Murni • Higienis",
                tanggal = FormatUtils.formatDateShort(System.currentTimeMillis()),
                pelanggan = cust?.nama ?: "Pelanggan",
                alamat = cust?.alamat?.takeIf { it.isNotBlank() },
                items = mappedItems,
                totalLabel = "TOTAL TAGIHAN",
                totalValue = FormatUtils.formatRupiah(cust?.sisa_kasbon ?: 0.0),
                statusNota = "KASBON"
            )
        }
    }

    var noNota: String? = null
    var tanggal: String? = null
    var pelanggan: String? = null
    var alamat: String? = null
    var kurir: String? = null
    var totalLabel = "TOTAL BELANJA"
    var totalValue = "Rp 0"
    var metodeBayar: String? = null
    var statusNota = "LUNAS"
    val items = mutableListOf<ReceiptLineItemUi>()

    var dashSection = 0
    val itemBuffer = mutableListOf<String>()

    for (line in lines) {
        if (line.isEmpty()) continue
        if (line.all { it == '-' }) {
            dashSection++
            continue
        }
        if (line.all { it == '=' || it == '.' }) continue

        when {
            line.startsWith("No. Nota:") -> noNota = line.substringAfter("No. Nota:").trim()
            line.startsWith("Tgl:") -> tanggal = line.substringAfter("Tgl:").trim()
            line.startsWith("Tgl Cetak:") -> tanggal = line.substringAfter("Tgl Cetak:").trim()
            line.startsWith("Pelanggan:") -> pelanggan = line.substringAfter("Pelanggan:").trim()
            line.startsWith("Alamat:") -> alamat = line.substringAfter("Alamat:").trim()
            line.startsWith("Kurir:") -> kurir = line.substringAfter("Kurir:").trim()
            line.startsWith("TOTAL BELANJA:") -> {
                totalLabel = "TOTAL BELANJA"
                totalValue = line.substringAfter("TOTAL BELANJA:").trim()
            }
            line.startsWith("TOTAL TAGIHAN:") -> {
                totalLabel = "TOTAL TAGIHAN"
                totalValue = line.substringAfter("TOTAL TAGIHAN:").trim()
            }
            line.startsWith("Nominal Bayar:") -> {
                totalLabel = "TOTAL BAYAR"
                totalValue = line.substringAfter("Nominal Bayar:").trim()
            }
            line.startsWith("Metode Bayar:") -> metodeBayar = line.substringAfter("Metode Bayar:").trim()
            line.startsWith("Status Nota:") -> statusNota = line.substringAfter("Status Nota:").trim()
            line.startsWith("Status:") -> statusNota = line.substringAfter("Status:").trim()
            dashSection == 1 -> itemBuffer.add(line)
        }
    }

    if (itemBuffer.size >= 2) {
        val name = itemBuffer[0]
        val secondLine = itemBuffer[1]
        val parts = secondLine.split(Regex("\\s{2,}"))
        val qtyPrice = parts.firstOrNull()?.trim() ?: secondLine
        val subtotal = if (parts.size > 1) parts.last().trim() else totalValue
        items.add(ReceiptLineItemUi(name = name, qtyPrice = qtyPrice, subtotal = subtotal))
    }

    return ParsedReceiptUi(
        noNota = noNota,
        tanggal = tanggal,
        pelanggan = pelanggan,
        alamat = alamat,
        kurir = kurir,
        items = items,
        totalLabel = totalLabel,
        totalValue = totalValue,
        metodeBayar = metodeBayar,
        statusNota = statusNota,
        sisaKasbon = parsedSisaKasbon
    )
}

@Composable
fun ReceiptDialog(
    dialogData: ActiveReceiptDialog,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var printStatus by remember { mutableStateOf<String?>(null) }
    val parsedReceipt = remember(dialogData) { extractReceiptUi(dialogData) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dialogData.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Pratinjau Nota Digital & Cetak Thermal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReceiptPreviewCard(receipt = parsedReceipt)

                if (printStatus != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = printStatus!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        SantoyaReceiptHelper.printViaBluetooth(
                            context = context,
                            receiptText = dialogData.receiptText
                        ) { status ->
                            printStatus = status
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_print_bluetooth")
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cetak Printer Thermal (ESC/POS)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                FilledTonalButton(
                    onClick = {
                        SantoyaReceiptHelper.shareReceiptImage(
                            context = context,
                            receiptText = dialogData.receiptText,
                            phoneNumber = dialogData.customerPhone
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                        contentColor = Color(0xFF047857)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_share_wa_image")
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kirim Gambar Struk ke WhatsApp",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        SantoyaReceiptHelper.shareReceiptText(
                            context = context,
                            text = dialogData.receiptText,
                            phoneNumber = dialogData.customerPhone
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_share_wa_text")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bagikan Teks Nota",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ReceiptPreviewCard(receipt: ParsedReceiptUi) {
    val isLunas = receipt.statusNota.contains("LUNAS", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("receipt_preview_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFE6F0FF),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color(0xFF0052CC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SANTOYA RO",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = receipt.subtitle1,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center
            )
            Text(
                text = receipt.subtitle2,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            ReceiptDashedDivider(modifier = Modifier.padding(vertical = 14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (!receipt.noNota.isNullOrBlank()) {
                    ReceiptInfoRow(label = "No. Nota", value = receipt.noNota)
                }
                if (!receipt.tanggal.isNullOrBlank()) {
                    ReceiptInfoRow(label = "Tgl", value = receipt.tanggal)
                }
                if (!receipt.pelanggan.isNullOrBlank()) {
                    ReceiptInfoRow(label = "Pelanggan", value = receipt.pelanggan)
                }
                if (!receipt.alamat.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "Alamat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = receipt.alamat,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (!receipt.kurir.isNullOrBlank()) {
                    ReceiptInfoRow(label = "Kurir", value = receipt.kurir)
                }
            }

            if (receipt.items.isNotEmpty()) {
                ReceiptDashedDivider(modifier = Modifier.padding(vertical = 14.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    receipt.items.forEach { item ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.qtyPrice,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = item.subtotal,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            ReceiptDashedDivider(modifier = Modifier.padding(vertical = 14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F7FB), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = receipt.totalLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = receipt.totalValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0052CC),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (!receipt.metodeBayar.isNullOrBlank()) {
                    ReceiptInfoRow(label = "Metode Bayar", value = receipt.metodeBayar)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status Nota",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isLunas) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = receipt.statusNota,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isLunas) Color(0xFF047857) else Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!receipt.sisaKasbon.isNullOrBlank()) {
                    ReceiptInfoRow(label = "Sisa Kasbon", value = receipt.sisaKasbon)
                }
            }

            ReceiptDashedDivider(modifier = Modifier.padding(top = 14.dp, bottom = 10.dp))

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Terima Kasih Atas Kunjungan Anda",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Santoya RO Siap Antar",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReceiptDashedDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
    }
}

@Composable
private fun ReceiptInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}
