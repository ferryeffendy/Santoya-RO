package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.entity.DepotProfile
import com.example.data.entity.Pelanggan
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.santoyaColors
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
                tanggal = FormatUtils.formatDateTimeTable(trx.tanggal),
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
                metodeBayar = FormatUtils.formatMetodeBayarLabel(trx.metode_bayar),
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
                tanggal = FormatUtils.formatDateTimeTable(System.currentTimeMillis()),
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
            line.startsWith("Metode Bayar:") -> {
                metodeBayar = FormatUtils.formatMetodeBayarLabel(line.substringAfter("Metode Bayar:").trim())
            }
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

/**
 * Dialog pratinjau nota dan struk transaksi dengan opsi cetak Bluetooth serta bagikan gambar/teks WhatsApp.
 * Secara otomatis menggunakan kop dinamis dari [depotProfile] dan menampilkan pengisian cepat bila nama depot masih kosong.
 */
@Composable
fun ReceiptDialog(
    dialogData: ActiveReceiptDialog,
    depotProfile: DepotProfile = DepotProfile(),
    isOwner: Boolean = false,
    onQuickSaveDepotProfile: ((String, String, String, (DepotProfile) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var printStatus by remember { mutableStateOf<String?>(null) }
    val parsedReceipt = remember(dialogData) { extractReceiptUi(dialogData) }
    var pendingQuickAction by remember(dialogData) { mutableStateOf<((String, DepotProfile) -> Unit)?>(null) }
    var pendingBluetoothReceiptText by remember(dialogData) { mutableStateOf<String?>(null) }

    val startBluetoothPrint: (String) -> Unit = { receiptText ->
        SantoyaReceiptHelper.printViaBluetooth(
            context = context,
            receiptText = receiptText
        ) { status ->
            printStatus = status
        }
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val textToPrint = pendingBluetoothReceiptText ?: dialogData.receiptText
        pendingBluetoothReceiptText = null
        if (granted) {
            startBluetoothPrint(textToPrint)
        } else {
            printStatus = context.getString(R.string.receipt_bluetooth_denied)
        }
    }
    val launchBluetoothPrint: (String) -> Unit = { receiptText ->
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingBluetoothReceiptText = receiptText
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            startBluetoothPrint(receiptText)
        }
    }
    val executeOrPromptQuickFill: ((String, DepotProfile) -> Unit) -> Unit = { action ->
        if (isOwner && !depotProfile.hasConfiguredDepotName() && onQuickSaveDepotProfile != null) {
            pendingQuickAction = action
        } else {
            action(dialogData.receiptText, depotProfile)
        }
    }

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
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.receipt_preview_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_close)
                    )
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
                ReceiptPreviewCard(
                    receipt = parsedReceipt,
                    depotProfile = depotProfile
                )

                printStatus?.let { status ->
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        executeOrPromptQuickFill { textToPrint, _ ->
                            launchBluetoothPrint(textToPrint)
                        }
                    },
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_print_bluetooth")
                ) {
                    Icon(imageVector = Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.receipt_btn_print),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                FilledTonalButton(
                    onClick = {
                        executeOrPromptQuickFill { textToShare, activeProfile ->
                            SantoyaReceiptHelper.shareReceiptImage(
                                context = context,
                                receiptText = textToShare,
                                phoneNumber = dialogData.customerPhone,
                                depotName = activeProfile.effectiveDepotName()
                            )
                        }
                    },
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.santoyaColors.successContainer,
                        contentColor = MaterialTheme.santoyaColors.onSuccessContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_share_wa_image")
                ) {
                    Icon(imageVector = Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.receipt_btn_share_image),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        executeOrPromptQuickFill { textToShare, activeProfile ->
                            SantoyaReceiptHelper.shareReceiptText(
                                context = context,
                                text = textToShare,
                                phoneNumber = dialogData.customerPhone,
                                depotName = activeProfile.effectiveDepotName()
                            )
                        }
                    },
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_share_wa_text")
                ) {
                    Icon(imageVector = Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.receipt_btn_share_text),
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

    if (pendingQuickAction != null) {
        DepotQuickFillDialog(
            initialProfile = depotProfile,
            onDismiss = { pendingQuickAction = null },
            onSaveAndContinue = { name, addr, phone ->
                val savedAction = pendingQuickAction
                pendingQuickAction = null
                onQuickSaveDepotProfile?.invoke(name, addr, phone) { updated ->
                    val rebuiltText = dialogData.receiptBuilder?.invoke(updated) ?: dialogData.receiptText
                    savedAction?.invoke(rebuiltText, updated)
                }
            },
            onSkip = {
                val skippedAction = pendingQuickAction
                pendingQuickAction = null
                skippedAction?.invoke(dialogData.receiptText, depotProfile)
            }
        )
    }
}

@Composable
private fun ReceiptPreviewCard(
    receipt: ParsedReceiptUi,
    depotProfile: DepotProfile
) {
    val isLunas = receipt.statusNota.contains("LUNAS", ignoreCase = true)
    val santoyaColors = MaterialTheme.santoyaColors

    Card(
        shape = RoundedCornerShape(SantoyaRadius.md),
        colors = CardDefaults.cardColors(containerColor = santoyaColors.receiptSurface),
        border = BorderStroke(1.dp, santoyaColors.receiptDivider),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    HydroDropletSparkIcon(
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val cleanDepotName = remember(depotProfile.depotName) { depotProfile.effectiveDepotName().uppercase() }
            val cleanAddress = remember(depotProfile.address) { depotProfile.address.trim() }
            val cleanPhone = remember(depotProfile.phoneNumber) { depotProfile.phoneNumber.trim() }
            val cleanFooter = remember(depotProfile.receiptFooter) { depotProfile.receiptFooter.trim() }

            Text(
                text = cleanDepotName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = santoyaColors.receiptOnSurface,
                textAlign = TextAlign.Center
            )
            if (cleanAddress.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cleanAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = santoyaColors.receiptMuted,
                    textAlign = TextAlign.Center
                )
            }
            if (cleanPhone.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.depot_profile_preview_phone_prefix, cleanPhone),
                    style = MaterialTheme.typography.bodySmall,
                    color = santoyaColors.receiptMuted,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = receipt.subtitle1,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = santoyaColors.receiptMuted,
                textAlign = TextAlign.Center
            )
            Text(
                text = receipt.subtitle2,
                style = MaterialTheme.typography.bodySmall,
                color = santoyaColors.receiptMuted,
                textAlign = TextAlign.Center
            )

            ReceiptDashedDivider(modifier = Modifier.padding(vertical = 14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (!receipt.noNota.isNullOrBlank()) {
                    ReceiptInfoRow(label = stringResource(R.string.receipt_label_no_nota), value = receipt.noNota)
                }
                if (!receipt.tanggal.isNullOrBlank()) {
                    ReceiptInfoRow(label = stringResource(R.string.receipt_label_tgl), value = receipt.tanggal)
                }
                if (!receipt.pelanggan.isNullOrBlank()) {
                    ReceiptInfoRow(label = stringResource(R.string.receipt_label_pelanggan), value = receipt.pelanggan)
                }
                if (!receipt.alamat.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = stringResource(R.string.receipt_label_alamat),
                            style = MaterialTheme.typography.bodyMedium,
                            color = santoyaColors.receiptMuted
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = receipt.alamat,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = santoyaColors.receiptOnSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (!receipt.kurir.isNullOrBlank()) {
                    ReceiptInfoRow(label = stringResource(R.string.receipt_label_kurir), value = receipt.kurir)
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
                                fontWeight = FontWeight.SemiBold,
                                color = santoyaColors.receiptOnSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.qtyPrice,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                    color = santoyaColors.receiptMuted
                                )
                                Text(
                                    text = item.subtotal,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold,
                                    color = santoyaColors.receiptOnSurface
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
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        RoundedCornerShape(SantoyaRadius.sm)
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = receipt.totalLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = santoyaColors.receiptOnSurface,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = receipt.totalValue,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
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
                    ReceiptInfoRow(label = stringResource(R.string.receipt_label_metode), value = receipt.metodeBayar)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.receipt_label_status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = santoyaColors.receiptMuted
                    )
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.xs),
                        color = if (isLunas) santoyaColors.successContainer else santoyaColors.warningContainer
                    ) {
                        Text(
                            text = receipt.statusNota,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLunas) santoyaColors.onSuccessContainer else santoyaColors.onWarningContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!receipt.sisaKasbon.isNullOrBlank()) {
                    ReceiptInfoRow(label = stringResource(R.string.receipt_label_sisa_kasbon), value = receipt.sisaKasbon)
                }
            }

            ReceiptDashedDivider(modifier = Modifier.padding(top = 14.dp, bottom = 10.dp))

            Spacer(modifier = Modifier.height(2.dp))

            val footerDisplay = depotProfile.receiptFooter.trim().ifBlank {
                stringResource(R.string.receipt_footer_thanks)
            }
            Text(
                text = footerDisplay,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = santoyaColors.receiptMuted,
                textAlign = TextAlign.Center
            )
            if (!depotProfile.isProUser()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = DepotProfile.WATERMARK_TEXT,
                    style = MaterialTheme.typography.labelSmall,
                    color = santoyaColors.receiptMuted.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ReceiptDashedDivider(modifier: Modifier = Modifier) {
    val dividerColor = MaterialTheme.santoyaColors.receiptDivider
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = dividerColor,
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
    val santoyaColors = MaterialTheme.santoyaColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = santoyaColors.receiptMuted,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = santoyaColors.receiptOnSurface,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
