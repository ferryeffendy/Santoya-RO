package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.TransaksiDetail
import com.example.ui.theme.pressClickEffect
import com.example.util.FormatUtils
import com.example.util.SantoyaReceiptHelper
import com.example.viewmodel.ActiveReceiptDialog
import com.example.viewmodel.SantoyaViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaksiScreen(
    viewModel: SantoyaViewModel,
    transaksiList: List<TransaksiDetail>,
    pelangganList: List<Pelanggan>,
    produkList: List<Produk>,
    kurirList: List<Kurir>,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.transaksiTab.collectAsState()

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { viewModel.setTransaksiTab(0) },
                text = {
                    Text(
                        "Catat Penjualan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                },
                modifier = Modifier.testTag("tab_catat_penjualan")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { viewModel.setTransaksiTab(1) },
                text = {
                    Text(
                        "Riwayat Transaksi (${transaksiList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                },
                modifier = Modifier.testTag("tab_riwayat_penjualan")
            )
        }

        if (selectedTab == 0) {
            CatatPenjualanTab(
                viewModel = viewModel,
                pelangganList = pelangganList,
                produkList = produkList,
                kurirList = kurirList
            )
        } else {
            RiwayatTransaksiTab(
                viewModel = viewModel,
                transaksiList = transaksiList,
                pelangganList = pelangganList,
                kurirList = kurirList,
                produkList = produkList
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatatPenjualanTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    produkList: List<Produk>,
    kurirList: List<Kurir>
) {
    val context = LocalContext.current
    val tanggal by viewModel.txTanggal.collectAsState()
    val selectedPelangganId by viewModel.txPelangganId.collectAsState()
    val selectedProdukId by viewModel.txProdukId.collectAsState()
    val selectedKurirId by viewModel.txKurirId.collectAsState()
    val jumlah by viewModel.txJumlah.collectAsState()
    val hargaSatuan by viewModel.txHargaSatuan.collectAsState()
    val metodeBayar by viewModel.txMetodeBayar.collectAsState()
    val status by viewModel.txStatus.collectAsState()

    var showAddCustomerDialog by remember { mutableStateOf(false) }

    val selectedPelanggan = pelangganList.find { it.id == selectedPelangganId }
    val selectedKurir = kurirList.find { it.id == selectedKurirId }

    val initialCustomerName = remember(selectedPelangganId) {
        pelangganList.find { it.id == selectedPelangganId }?.nama ?: ""
    }
    var searchQuery by remember { mutableStateOf(initialCustomerName) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val filteredPelanggan = remember(searchQuery, pelangganList, selectedPelanggan) {
        if (searchQuery.isBlank() || (selectedPelanggan != null && searchQuery.trim() == selectedPelanggan.nama.trim())) {
            pelangganList
        } else {
            val q = searchQuery.trim().lowercase()
            pelangganList.filter {
                it.nama.lowercase().contains(q) ||
                    it.alamat.lowercase().contains(q) ||
                    it.no_hp.lowercase().contains(q)
            }
        }
    }

    if (selectedProdukId == null && produkList.isNotEmpty()) {
        val first = produkList.first()
        viewModel.setTxProdukId(first.id, first.harga)
    }

    val totalHarga = jumlah * hargaSatuan

    val calendar = Calendar.getInstance().apply { timeInMillis = tanggal }
    val datePickerDialog = remember(context, tanggal) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                viewModel.setTxTanggal(selectedCal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Baris Tanggal Transaksi
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .pressClickEffect { datePickerDialog.show() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tgl: ${FormatUtils.formatDate(tanggal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Ubah Tgl",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // 2. Autocomplete & Dropdown Pemilihan Pelanggan
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { text ->
                        searchQuery = text
                        isDropdownExpanded = true
                        val match = pelangganList.find { it.nama.equals(text.trim(), ignoreCase = true) }
                        if (match != null) {
                            viewModel.onPelangganSelected(match.id)
                        } else if (selectedPelanggan != null && selectedPelanggan.nama != text) {
                            viewModel.onPelangganSelected(null)
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    label = {
                        Text(
                            "Pilih / Cari Pelanggan *",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    placeholder = {
                        Text(
                            "Ketik nama, desa, atau no hp...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotBlank() || selectedPelangganId != null) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        isDropdownExpanded = true
                                        viewModel.onPelangganSelected(null)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Hapus Pilihan",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showAddCustomerDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = "Tambah Pelanggan Baru",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                        .testTag("input_cari_pelanggan")
                )

                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    if (filteredPelanggan.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Pelanggan \"$searchQuery\" tidak ditemukan. Klik untuk buat baru +",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            onClick = {
                                isDropdownExpanded = false
                                showAddCustomerDialog = true
                            }
                        )
                    } else {
                        filteredPelanggan.take(30).forEach { cust ->
                            val isSelected = cust.id == selectedPelangganId
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                cust.nama,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val subDetails = mutableListOf<String>()
                                            if (cust.alamat.isNotBlank()) subDetails.add(cust.alamat)
                                            if (cust.no_hp.isNotBlank()) subDetails.add(cust.no_hp)
                                            if (cust.sisa_kasbon > 0) subDetails.add("Kasbon: ${FormatUtils.formatRupiah(cust.sisa_kasbon)}")
                                            if (cust.harga_khusus != null && cust.harga_khusus > 0) subDetails.add("Khusus: ${FormatUtils.formatRupiah(cust.harga_khusus)}")
                                            if (cust.id_kurir_langganan != null) {
                                                val kName = kurirList.find { it.id == cust.id_kurir_langganan }?.nama
                                                if (kName != null) subDetails.add("Kurir: $kName")
                                            }
                                            if (subDetails.isNotEmpty()) {
                                                Text(
                                                    subDetails.joinToString(" • "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (cust.sisa_kasbon > 0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    searchQuery = cust.nama
                                    isDropdownExpanded = false
                                    viewModel.onPelangganSelected(cust.id)
                                }
                            )
                        }
                    }
                }
            }

            // 3. Banner Info Profil Otomatis & Kasbon Ramping
            if (selectedPelanggan != null) {
                if (selectedPelanggan.harga_khusus != null || selectedPelanggan.id_kurir_langganan != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val autoDetails = mutableListOf<String>()
                            if (selectedPelanggan.harga_khusus != null && selectedPelanggan.harga_khusus > 0) {
                                autoDetails.add("Khusus: ${FormatUtils.formatRupiah(selectedPelanggan.harga_khusus)}")
                            }
                            if (selectedPelanggan.id_kurir_langganan != null) {
                                val kName = kurirList.find { it.id == selectedPelanggan.id_kurir_langganan }?.nama ?: "Kurir"
                                autoDetails.add("Kurir: $kName")
                            }
                            Text(
                                text = "Otomatis: ${autoDetails.joinToString(" • ")}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (selectedPelanggan.sisa_kasbon > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sisa kasbon: ${FormatUtils.formatRupiah(selectedPelanggan.sisa_kasbon)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 4. Katalog Produk (Grid 2 Kolom dengan micro-interaction)
            val chunkedProduk = produkList.chunked(2)
            chunkedProduk.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { prod ->
                        val isSelected = prod.id == selectedProdukId
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .pressClickEffect { viewModel.setTxProdukId(prod.id, prod.harga) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = prod.nama_produk,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(prod.harga),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 5. Baris Horizontal: Harga Satuan & Counter Galon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (hargaSatuan > 0) hargaSatuan.toLong().toString() else "",
                    onValueChange = { input ->
                        val num = input.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                        viewModel.setTxHargaSatuan(num)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    label = {
                        Text(
                            "Harga Satuan (Rp) *",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    prefix = {
                        Text(
                            "Rp ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("input_harga_satuan")
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.decrementTxJumlah() },
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_minus_galon")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "$jumlah",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(38.dp)
                                .animateContentSize(),
                            maxLines = 1
                        )

                        OutlinedButton(
                            onClick = { viewModel.incrementTxJumlah() },
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_plus_galon")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // 6. Pilihan Kurir Pengantar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Kurir Pengantar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedKurirId == null,
                        onClick = { viewModel.setTxKurirId(null) },
                        label = {
                            Text(
                                "Ambil Sendiri",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedKurirId == null) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.height(34.dp)
                    )
                    kurirList.forEach { k ->
                        FilterChip(
                            selected = selectedKurirId == k.id,
                            onClick = { viewModel.setTxKurirId(k.id) },
                            label = {
                                Text(
                                    k.nama,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedKurirId == k.id) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.height(34.dp)
                        )
                    }
                }
            }

            // 7. Pilihan Pembayaran & Status
            val isKasbon = status.equals("Kasbon", ignoreCase = true)
            val isLunas = status.equals("Lunas", ignoreCase = true)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Metode Pembayaran",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Cash", "Transfer").forEach { m ->
                            val isSelected = !isKasbon && metodeBayar.equals(m, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTxMetodeBayar(m) },
                                label = {
                                    Text(
                                        m,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Status Nota",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = isLunas,
                            onClick = { viewModel.setTxStatus("Lunas") },
                            label = {
                                Text(
                                    "Lunas",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isLunas) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        )
                        FilterChip(
                            selected = isKasbon,
                            onClick = { viewModel.setTxStatus("Kasbon") },
                            label = {
                                Text(
                                    "Kasbon",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isKasbon) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        )
                    }
                }
            }
        }

        // 8. Docked Fintech Bottom Action Bar
        Surface(
            shadowElevation = 10.dp,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Total (${jumlah} Galon): ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatRupiah(totalHarga),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        if (selectedKurir != null) {
                            val komisiTotal = selectedKurir.komisi_per_galon * jumlah
                            Text(
                                text = "Komisi Kurir (${selectedKurir.nama}): ${FormatUtils.formatRupiah(komisiTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (status.equals("Kasbon", ignoreCase = true)) Color(0xFFF59E0B).copy(alpha = 0.18f) else Color(0xFF10B981).copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = status.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (status.equals("Kasbon", ignoreCase = true)) Color(0xFFB45309) else Color(0xFF047857),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.submitTransaksi { newId ->
                                val savedDetail = viewModel.allTransaksi.value.find { it.transaksi.id == newId.toInt() }
                                if (savedDetail != null) {
                                    val custKasbon = pelangganList.find { it.id == savedDetail.transaksi.id_pelanggan }?.sisa_kasbon ?: 0.0
                                    val text = SantoyaReceiptHelper.generateSaleReceipt(savedDetail, custKasbon)
                                    viewModel.showReceiptDialog(
                                        ActiveReceiptDialog(
                                            title = "Struk Nota #${savedDetail.transaksi.id}",
                                            receiptText = text,
                                            customerPhone = savedDetail.pelanggan?.no_hp,
                                            data = savedDetail
                                        )
                                    )
                                }
                                searchQuery = ""
                                isDropdownExpanded = false
                                viewModel.onPelangganSelected(null)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_simpan_dan_cetak")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Cetak Struk",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.submitTransaksi {
                                searchQuery = ""
                                isDropdownExpanded = false
                                viewModel.onPelangganSelected(null)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status.equals("Kasbon", ignoreCase = true)) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_simpan_transaksi")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Simpan Transaksi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        val initialNama = if (pelangganList.none { it.nama.equals(searchQuery.trim(), ignoreCase = true) }) searchQuery.trim() else ""
        var namaBaru by remember { mutableStateOf(initialNama) }
        var alamatBaru by remember { mutableStateOf("") }
        var noHpBaru by remember { mutableStateOf("") }
        var hargaKhususBaru by remember { mutableStateOf("") }
        var selectedKurirBaruId by remember { mutableStateOf<Int?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Tambah Pelanggan Baru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = namaBaru,
                        onValueChange = { namaBaru = it },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        label = {
                            Text(
                                "Nama Pelanggan *",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = alamatBaru,
                        onValueChange = { alamatBaru = it },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        label = {
                            Text(
                                "Alamat / Desa / Blok",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = noHpBaru,
                        onValueChange = { noHpBaru = it.filter { c -> c.isDigit() || c == '+' } },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        label = {
                            Text(
                                "No. HP / WhatsApp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = hargaKhususBaru,
                        onValueChange = { hargaKhususBaru = it.filter { c -> c.isDigit() } },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        label = {
                            Text(
                                "Harga Khusus Galon (Rp) (Opsional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        placeholder = {
                            Text(
                                "Kosongkan jika harga normal",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text(
                        text = "Kurir Langganan (Opsional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedKurirBaruId == null,
                            onClick = { selectedKurirBaruId = null },
                            label = {
                                Text(
                                    "Tanpa Kurir",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedKurirBaruId == null) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.height(32.dp)
                        )
                        kurirList.forEach { kurir ->
                            FilterChip(
                                selected = selectedKurirBaruId == kurir.id,
                                onClick = { selectedKurirBaruId = kurir.id },
                                label = {
                                    Text(
                                        kurir.nama,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selectedKurirBaruId == kurir.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (namaBaru.isNotBlank()) {
                            val hKhusus = hargaKhususBaru.toDoubleOrNull()
                            viewModel.addPelanggan(
                                nama = namaBaru,
                                alamat = alamatBaru,
                                noHp = noHpBaru,
                                hargaKhusus = hKhusus,
                                idKurirLangganan = selectedKurirBaruId
                            )
                            searchQuery = namaBaru.trim()
                            isDropdownExpanded = false
                            showAddCustomerDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Simpan Pelanggan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text(
                        "Batal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
fun RiwayatTransaksiTab(
    viewModel: SantoyaViewModel,
    transaksiList: List<TransaksiDetail>,
    pelangganList: List<Pelanggan>,
    kurirList: List<Kurir> = emptyList(),
    produkList: List<Produk> = emptyList()
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingTransaksi by remember { mutableStateOf<TransaksiDetail?>(null) }

    val filtered = remember(searchQuery, transaksiList) {
        if (searchQuery.isBlank()) {
            transaksiList
        } else {
            val q = searchQuery.lowercase().trim()
            transaksiList.filter {
                (it.pelanggan?.nama?.lowercase()?.contains(q) == true) ||
                    (it.produk?.nama_produk?.lowercase()?.contains(q) == true) ||
                    (it.transaksi.status.lowercase().contains(q)) ||
                    (it.transaksi.id.toString() == q)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = {
                    Text(
                        "Cari nota, pelanggan, atau status...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_transaksi")
            )
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Tidak ada riwayat transaksi yang cocok.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.transaksi.id }) { detail ->
                val isKasbon = detail.transaksi.status.equals("Kasbon", ignoreCase = true)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = FormatUtils.formatDate(detail.transaksi.tanggal),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isKasbon) Color(0xFFF59E0B).copy(alpha = 0.18f) else Color(0xFF10B981).copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = detail.transaksi.status.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKasbon) Color(0xFFB45309) else Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detail.pelanggan?.nama ?: "Pelanggan Umum",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${detail.produk?.nama_produk ?: "Air RO"} • ${detail.transaksi.jumlah} Galon",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (detail.kurir != null) {
                                    Text(
                                        text = "Kurir: ${detail.kurir.nama}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = FormatUtils.formatRupiah(detail.transaksi.total_harga),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = detail.transaksi.metode_bayar,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { editingTransaksi = detail },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val custKasbon = pelangganList.find { it.id == detail.transaksi.id_pelanggan }?.sisa_kasbon ?: 0.0
                                    val text = SantoyaReceiptHelper.generateSaleReceipt(detail, custKasbon)
                                    viewModel.showReceiptDialog(
                                        ActiveReceiptDialog(
                                            title = "Struk Nota #${detail.transaksi.id}",
                                            receiptText = text,
                                            customerPhone = detail.pelanggan?.no_hp,
                                            data = detail
                                        )
                                    )
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Print, contentDescription = "Cetak", modifier = Modifier.size(17.dp))
                            }

                            IconButton(
                                onClick = {
                                    val custKasbon = pelangganList.find { it.id == detail.transaksi.id_pelanggan }?.sisa_kasbon ?: 0.0
                                    val text = SantoyaReceiptHelper.generateSaleReceipt(detail, custKasbon)
                                    SantoyaReceiptHelper.shareReceiptText(
                                        context = viewModel.getApplication(),
                                        text = text,
                                        phoneNumber = detail.pelanggan?.no_hp
                                    )
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Bagikan", modifier = Modifier.size(17.dp))
                            }

                            IconButton(
                                onClick = { viewModel.deleteTransaksi(detail.transaksi) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingTransaksi != null) {
        val detail = editingTransaksi!!
        var editJumlah by remember(detail) { mutableStateOf(detail.transaksi.jumlah.toString()) }
        var editHarga by remember(detail) { mutableStateOf(detail.transaksi.harga_satuan.toInt().toString()) }
        var editMetode by remember(detail) { mutableStateOf(detail.transaksi.metode_bayar) }
        var editStatus by remember(detail) { mutableStateOf(detail.transaksi.status) }
        var editKurirId by remember(detail) { mutableStateOf(detail.transaksi.id_kurir) }
        var editProdukId by remember(detail) { mutableStateOf(detail.transaksi.id_produk) }

        AlertDialog(
            onDismissRequest = { editingTransaksi = null },
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Edit Transaksi #${detail.transaksi.id}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pelanggan: ${detail.pelanggan?.nama ?: "Umum"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (produkList.isNotEmpty()) {
                        Text(
                            "Produk:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            produkList.forEach { prod ->
                                val isSelected = editProdukId == prod.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        editProdukId = prod.id
                                        editHarga = prod.harga.toInt().toString()
                                    },
                                    label = {
                                        Text(
                                            prod.nama_produk,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editJumlah,
                        onValueChange = { editJumlah = it.filter { c -> c.isDigit() } },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        label = {
                            Text(
                                "Jumlah Galon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = editHarga,
                        onValueChange = { editHarga = it.filter { c -> c.isDigit() } },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        label = {
                            Text(
                                "Harga Satuan (Rp)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text(
                        "Metode Pembayaran:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isEditKasbon = editStatus.equals("Kasbon", ignoreCase = true)
                        listOf("Cash", "Transfer").forEach { metode ->
                            val isSelected = !isEditKasbon && editMetode.equals(metode, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    editMetode = metode
                                    editStatus = "Lunas"
                                },
                                label = {
                                    Text(
                                        metode,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Text(
                        "Status Pembayaran:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Lunas", "Kasbon").forEach { st ->
                            val isSelected = editStatus.equals(st, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    editStatus = st
                                    if (st.equals("Kasbon", ignoreCase = true)) {
                                        editMetode = ""
                                    } else if (st.equals("Lunas", ignoreCase = true) && editMetode.isBlank()) {
                                        editMetode = "Cash"
                                    }
                                },
                                label = {
                                    Text(
                                        st,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Text(
                        "Kurir Pengantar:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = editKurirId == null,
                            onClick = { editKurirId = null },
                            label = {
                                Text(
                                    "Ambil Sendiri",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (editKurirId == null) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        kurirList.forEach { kurir ->
                            val isSelected = editKurirId == kurir.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { editKurirId = kurir.id },
                                label = {
                                    Text(
                                        kurir.nama,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val jml = editJumlah.toIntOrNull() ?: 1
                        val hrg = editHarga.toDoubleOrNull() ?: 0.0
                        viewModel.updateTransaksi(
                            id = detail.transaksi.id,
                            tanggal = detail.transaksi.tanggal,
                            idPelanggan = detail.transaksi.id_pelanggan,
                            idKurir = editKurirId,
                            idProduk = editProdukId,
                            jumlah = jml,
                            hargaSatuan = hrg,
                            metodeBayar = editMetode,
                            status = editStatus,
                            onSuccess = { editingTransaksi = null }
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Simpan Perubahan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingTransaksi = null }) {
                    Text(
                        "Batal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
