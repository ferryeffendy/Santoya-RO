package com.example.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.companion.kzpmrw.R
import com.example.data.entity.Transaksi
import com.example.data.entity.TransaksiDetail
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.SantoyaReceiptHelper
import com.example.viewmodel.ActiveReceiptDialog
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel

data class TransaksiItemUi(
    val transaksi: Transaksi,
    val namaPelanggan: String,
    val namaKurir: String? = null,
    val namaProduk: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    viewModel: SantoyaViewModel,
    transaksiList: List<TransaksiDetail>? = null,
    onBack: () -> Unit = { viewModel.navigateTo(SantoyaScreen.TRANSAKSI) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scopedTransactionsFromVm by viewModel.scopedTransaksi.collectAsStateWithLifecycle()
    val rawTransaksiList = transaksiList ?: scopedTransactionsFromVm
    val effectiveTransaksiList = remember(rawTransaksiList) {
        rawTransaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var pendingReceiptText by remember { mutableStateOf("") }
    var transaksiToDelete by remember { mutableStateOf<Transaksi?>(null) }

    @SuppressLint("MissingPermission")
    fun printReceipt(receiptText: String) {
        val adapter = try { BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
        val devices = try {
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }

        if (devices.isEmpty()) {
            Toast.makeText(
                context,
                "Printer Bluetooth belum tersambung. Sambungkan dulu di Pengaturan HP.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (devices.size == 1) {
            val device = devices.first()
            val deviceName = try { device.name ?: device.address } catch (_: SecurityException) { device.address }
            Toast.makeText(context, "Mencetak ke $deviceName...", Toast.LENGTH_SHORT).show()
            SantoyaReceiptHelper.printViaBluetooth(
                context = context,
                receiptText = receiptText,
                targetDeviceAddress = device.address
            ) { status ->
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
            }
        } else {
            pairedDevices = devices
            pendingReceiptText = receiptText
            showPrinterDialog = true
        }
    }

    val filteredList = remember(effectiveTransaksiList, searchQuery) {
        if (searchQuery.isBlank()) {
            effectiveTransaksiList
        } else {
            val query = searchQuery.trim().lowercase()
            effectiveTransaksiList.filter { item ->
                (item.pelanggan?.nama?.lowercase()?.contains(query) == true) ||
                    (item.produk?.nama_produk?.lowercase()?.contains(query) == true) ||
                    (item.kurir?.nama?.lowercase()?.contains(query) == true) ||
                    item.transaksi.status.lowercase().contains(query) ||
                    item.transaksi.metode_bayar.lowercase().contains(query) ||
                    item.transaksi.id.toString() == query
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("riwayat_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Riwayat Transaksi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${filteredList.size} transaksi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("riwayat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("input_search_riwayat"),
                placeholder = { Text("Cari nota, pelanggan, kurir, produk...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Bersihkan",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SantoyaEmptyState(
                        illustrationRes = R.drawable.img_empty_transaksi,
                        title = if (searchQuery.isBlank()) "Belum ada transaksi" else "Transaksi tidak ditemukan",
                        subtitle = if (searchQuery.isBlank()) {
                            "Penjualan yang tercatat akan muncul di sini."
                        } else {
                            "Coba gunakan kata kunci pencarian lain."
                        },
                        secondaryActionLabel = if (searchQuery.isNotBlank()) "Reset Pencarian" else null,
                        secondaryActionIcon = if (searchQuery.isNotBlank()) Icons.Default.Clear else null,
                        onSecondaryAction = if (searchQuery.isNotBlank()) {
                            { searchQuery = "" }
                        } else null
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.transaksi.id }) { item ->
                        val trx = item.transaksi
                        val custName = item.pelanggan?.nama ?: "Pelanggan Umum"
                        val prodName = item.produk?.nama_produk ?: "Galon Air RO"
                        val kurirName = item.kurir?.nama?.takeIf { it.isNotBlank() } ?: "Tanpa Kurir"
                        val receiptText = remember(item) {
                            SantoyaReceiptHelper.generateSaleReceipt(
                                detail = item,
                                customerKasbon = item.pelanggan?.sisa_kasbon ?: 0.0
                            )
                        }

                        RiwayatTransaksiCard(
                            noNota = "#TRX-${trx.id}",
                            tanggal = FormatUtils.formatDate(trx.tanggal),
                            namaPelanggan = custName,
                            namaProduk = prodName,
                            namaKurir = kurirName,
                            jumlah = trx.jumlah,
                            hargaSatuan = trx.harga_satuan,
                            totalHarga = trx.total_harga,
                            metodeBayar = trx.metode_bayar,
                            status = trx.status,
                            onPrint = { printReceipt(receiptText) },
                            onShare = {
                                SantoyaReceiptHelper.shareReceiptImage(
                                    context = context,
                                    receiptText = receiptText,
                                    phoneNumber = item.pelanggan?.no_hp
                                )
                            },
                            onTandaiLunas = {
                                viewModel.markTransaksiLunas(trx.id)
                                Toast.makeText(context, "Transaksi #TRX-${trx.id} ditandai Lunas", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                transaksiToDelete = trx
                            }
                        )
                    }
                }
            }
        }
    }

    if (transaksiToDelete != null) {
        val trx = transaksiToDelete!!
        AlertDialog(
            onDismissRequest = { transaksiToDelete = null },
            title = { Text("Hapus Transaksi?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Transaksi #TRX-${trx.id} sebesar ${FormatUtils.formatRupiah(trx.total_harga)} akan dihapus.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaksi(trx)
                        transaksiToDelete = null
                        Toast.makeText(context, "Transaksi dihapus", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transaksiToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showPrinterDialog) {
        AlertDialog(
            onDismissRequest = { showPrinterDialog = false },
            title = { Text("Pilih Printer Bluetooth", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pairedDevices.forEach { device ->
                        @SuppressLint("MissingPermission")
                        val deviceName = try { device.name ?: device.address } catch (_: SecurityException) { device.address }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPrinterDialog = false
                                    Toast.makeText(context, "Mencetak ke $deviceName...", Toast.LENGTH_SHORT).show()
                                    SantoyaReceiptHelper.printViaBluetooth(
                                        context = context,
                                        receiptText = pendingReceiptText,
                                        targetDeviceAddress = device.address
                                    ) { status ->
                                        Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = deviceName,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPrinterDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun RiwayatTransaksiCard(
    noNota: String,
    tanggal: String,
    namaPelanggan: String,
    namaProduk: String,
    namaKurir: String,
    jumlah: Int,
    hargaSatuan: Double,
    totalHarga: Double,
    metodeBayar: String,
    status: String,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onTandaiLunas: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLunas = status.equals("Lunas", ignoreCase = true)
    val santoyaColors = MaterialTheme.santoyaColors

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLunas) santoyaColors.successContainer
                                else santoyaColors.warningContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLunas) Icons.Default.CheckCircle else Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = if (isLunas) santoyaColors.success else santoyaColors.warning,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = namaPelanggan,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tanggal,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrint, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Cetak Struk",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Bagikan Struk",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Transaksi",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$namaProduk x $jumlah (${FormatUtils.formatRupiah(hargaSatuan)})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$metodeBayar • Kurir: $namaKurir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatRupiah(totalHarga),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isLunas) santoyaColors.successContainer else santoyaColors.warningContainer
                    ) {
                        Text(
                            text = status.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLunas) santoyaColors.onSuccessContainer else santoyaColors.onWarningContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (!isLunas) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onTandaiLunas,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tandai Lunas", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
