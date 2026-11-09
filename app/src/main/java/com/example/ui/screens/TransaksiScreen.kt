package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.TransaksiDetail
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
                text = { Text("Catat Penjualan", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("tab_catat_penjualan")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { viewModel.setTransaksiTab(1) },
                text = { Text("Riwayat Transaksi (${transaksiList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
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
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Baris Tanggal Ramping
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clickable { datePickerDialog.show() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Ubah Tgl",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
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
                    label = { Text("Pilih / Cari Pelanggan *", fontSize = 11.sp) },
                    placeholder = { Text("Ketik nama, desa, atau no hp...", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
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
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
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
                                    fontSize = 12.sp,
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
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                fontSize = 12.sp,
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
                                                    fontSize = 10.sp,
                                                    color = if (cust.sisa_kasbon > 0) Color(0xFFF59E0B) else Color.Gray,
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
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
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
                                fontSize = 10.5.sp,
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
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sisa kasbon: ${FormatUtils.formatRupiah(selectedPelanggan.sisa_kasbon)}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 4. Katalog Produk (Grid 2 Kolom proporsional, tinggi 44.dp)
            val chunkedProduk = produkList.chunked(2)
            chunkedProduk.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pair.forEach { prod ->
                        val isSelected = prod.id == selectedProdukId
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { viewModel.setTxProdukId(prod.id, prod.harga) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = prod.nama_produk,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(prod.harga),
                                    fontSize = 12.sp,
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

            // 5. Baris Horizontal: Harga Satuan & Counter Galon (Mantap & Proporsional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (hargaSatuan > 0) hargaSatuan.toLong().toString() else "",
                    onValueChange = { input ->
                        val num = input.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                        viewModel.setTxHargaSatuan(num)
                    },
                    label = { Text("Harga Satuan (Rp) *", fontSize = 11.sp) },
                    prefix = { Text("Rp ", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("input_harga_satuan")
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(54.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
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
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(36.dp),
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Kurir Pengantar",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        label = { Text("Ambil Sendiri", maxLines = 1, fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp)
                    )
                    kurirList.forEach { k ->
                        FilterChip(
                            selected = selectedKurirId == k.id,
                            onClick = { viewModel.setTxKurirId(k.id) },
                            label = { Text(k.nama, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

            // 7. Pilihan Pembayaran & Status (Berdampingan lega, tidak terpotong)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Metode Pembayaran",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Cash", "COD", "Transfer").forEach { m ->
                            FilterChip(
                                selected = metodeBayar.equals(m, ignoreCase = true),
                                onClick = { viewModel.setTxMetodeBayar(m) },
                                label = { Text(m, maxLines = 1, fontSize = 10.5.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Status Nota",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = status.equals("Lunas", ignoreCase = true),
                            onClick = { viewModel.setTxStatus("Lunas") },
                            label = { Text("Lunas", maxLines = 1, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        )
                        FilterChip(
                            selected = status.equals("Kasbon", ignoreCase = true),
                            onClick = { viewModel.setTxStatus("Kasbon") },
                            label = { Text("Kasbon", maxLines = 1, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        )
                    }
                }
            }
        }

        // 8. Docked Bottom Action Bar
        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
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
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatRupiah(totalHarga),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (status.equals("Kasbon", ignoreCase = true)) Color(0xFFB45309) else MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        if (selectedKurir != null) {
                            val komisiTotal = selectedKurir.komisi_per_galon * jumlah
                            Text(
                                text = "Komisi Kurir (${selectedKurir.nama}): ${FormatUtils.formatRupiah(komisiTotal)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (status.equals("Kasbon", ignoreCase = true)) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = status.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (status.equals("Kasbon", ignoreCase = true)) Color(0xFFB45309) else Color(0xFF047857),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                            customerPhone = savedDetail.pelanggan?.no_hp
                                        )
                                    )
                                }
                                searchQuery = ""
                                isDropdownExpanded = false
                                viewModel.onPelangganSelected(null)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_simpan_dan_cetak")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak Struk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.submitTransaksi {
                                searchQuery = ""
                                isDropdownExpanded = false
                                viewModel.onPelangganSelected(null)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status.equals("Kasbon", ignoreCase = true)) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_simpan_transaksi")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan Transaksi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            title = {
                Text(
                    text = "Tambah Pelanggan Baru",
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
                        label = { Text("Nama Pelanggan *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = alamatBaru,
                        onValueChange = { alamatBaru = it },
                        label = { Text("Alamat / Desa / Blok") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = noHpBaru,
                        onValueChange = { noHpBaru = it.filter { c -> c.isDigit() || c == '+' } },
                        label = { Text("No. HP / WhatsApp") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = hargaKhususBaru,
                        onValueChange = { hargaKhususBaru = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Khusus Galon (Rp) (Opsional)") },
                        placeholder = { Text("Kosongkan jika harga normal") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text(
                        text = "Kurir Langganan (Opsional)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
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
                            label = { Text("Tanpa Kurir", maxLines = 1) },
                            modifier = Modifier.height(32.dp)
                        )
                        kurirList.forEach { kurir ->
                            FilterChip(
                                selected = selectedKurirBaruId == kurir.id,
                                onClick = { selectedKurirBaruId = kurir.id },
                                label = { Text(kurir.nama, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                    Text("Simpan Pelanggan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text("Batal")
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
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nota, pelanggan, atau status...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_transaksi")
            )
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ada riwayat transaksi yang cocok.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(filtered, key = { it.transaksi.id }) { detail ->
                val isKasbon = detail.transaksi.status.equals("Kasbon", ignoreCase = true)
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${detail.transaksi.id}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = FormatUtils.formatDate(detail.transaksi.tanggal),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isKasbon) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = detail.transaksi.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKasbon) Color(0xFFB45309) else Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detail.pelanggan?.nama ?: "Pelanggan Umum",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${detail.produk?.nama_produk ?: "Air RO"} • ${detail.transaksi.jumlah} Galon",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (detail.kurir != null) {
                                    Text(
                                        text = "Kurir: ${detail.kurir.nama}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = FormatUtils.formatRupiah(detail.transaksi.total_harga),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = detail.transaksi.metode_bayar,
                                    fontSize = 11.sp,
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
                            if (isKasbon) {
                                TextButton(
                                    onClick = { viewModel.markTransaksiLunas(detail.transaksi.id) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Lunas", fontSize = 11.sp)
                                }
                            }

                            IconButton(
                                onClick = { editingTransaksi = detail },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
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
                                            customerPhone = detail.pelanggan?.no_hp
                                        )
                                    )
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Print, contentDescription = "Cetak", modifier = Modifier.size(16.dp))
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
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Bagikan", modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = { viewModel.deleteTransaksi(detail.transaksi) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
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
                        Text("Produk:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            produkList.forEach { prod ->
                                FilterChip(
                                    selected = editProdukId == prod.id,
                                    onClick = {
                                        editProdukId = prod.id
                                        editHarga = prod.harga.toInt().toString()
                                    },
                                    label = { Text(prod.nama_produk, fontSize = 11.sp, maxLines = 1) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editJumlah,
                        onValueChange = { editJumlah = it.filter { c -> c.isDigit() } },
                        label = { Text("Jumlah Galon") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = editHarga,
                        onValueChange = { editHarga = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Satuan (Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("Metode Pembayaran:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "COD", "Transfer").forEach { metode ->
                            FilterChip(
                                selected = editMetode.equals(metode, ignoreCase = true),
                                onClick = { editMetode = metode },
                                label = { Text(metode) }
                            )
                        }
                    }

                    Text("Status Pembayaran:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Lunas", "Kasbon").forEach { st ->
                            FilterChip(
                                selected = editStatus.equals(st, ignoreCase = true),
                                onClick = { editStatus = st },
                                label = { Text(st) }
                            )
                        }
                    }

                    Text("Kurir Pengantar:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = editKurirId == null,
                            onClick = { editKurirId = null },
                            label = { Text("Ambil Sendiri") }
                        )
                        kurirList.forEach { kurir ->
                            FilterChip(
                                selected = editKurirId == kurir.id,
                                onClick = { editKurirId = kurir.id },
                                label = { Text(kurir.nama) }
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
                    Text("Simpan Perubahan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingTransaksi = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
