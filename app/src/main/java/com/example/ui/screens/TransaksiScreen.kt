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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

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

    val effectiveKasbonByCustomer = remember(pelangganList, transaksiList) {
        val unpaidByCustomer = transaksiList
            .filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
            .groupBy { it.transaksi.id_pelanggan }
        pelangganList.associate { p ->
            val unpaidNotes = unpaidByCustomer[p.id] ?: emptyList()
            val unpaidSum = unpaidNotes.sumOf { it.transaksi.remainingKasbonAmount() }
            val effectiveKasbon = if (unpaidNotes.isNotEmpty()) unpaidSum else p.sisa_kasbon
            p.id to effectiveKasbon
        }
    }

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
                kurirList = kurirList,
                effectiveKasbonByCustomer = effectiveKasbonByCustomer
            )
        } else {
            RiwayatTransaksiTab(
                viewModel = viewModel,
                transaksiList = transaksiList,
                pelangganList = pelangganList,
                kurirList = kurirList,
                produkList = produkList,
                effectiveKasbonByCustomer = effectiveKasbonByCustomer
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
    kurirList: List<Kurir>,
    effectiveKasbonByCustomer: Map<Int, Double> = emptyMap()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val tanggal by viewModel.txTanggal.collectAsState()
    val selectedPelangganId by viewModel.txPelangganId.collectAsState()
    val selectedProdukId by viewModel.txProdukId.collectAsState()
    val selectedKurirId by viewModel.txKurirId.collectAsState()
    val jumlah by viewModel.txJumlah.collectAsState()
    val hargaSatuan by viewModel.txHargaSatuan.collectAsState()
    val metodeBayar by viewModel.txMetodeBayar.collectAsState()
    val status by viewModel.txStatus.collectAsState()
    val uangMuka by viewModel.txUangMuka.collectAsState()
    val formScrollState = rememberScrollState()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showEditJumlahDialog by remember { mutableStateOf(false) }

    val selectedPelanggan = pelangganList.find { it.id == selectedPelangganId }
    val selectedKurir = kurirList.find { it.id == selectedKurirId }

    val initialCustomerName = remember(selectedPelangganId, pelangganList) {
        pelangganList.find { it.id == selectedPelangganId }?.nama ?: ""
    }
    var searchQuery by remember { mutableStateOf(initialCustomerName) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPelangganId, pelangganList) {
        val matchedName = pelangganList.find { it.id == selectedPelangganId }?.nama
        if (matchedName != null) {
            searchQuery = matchedName
        } else if (selectedPelangganId == null && pelangganList.any { it.nama.equals(searchQuery.trim(), ignoreCase = true) }) {
            searchQuery = ""
        }
    }

    val filteredPelanggan = remember(searchQuery, pelangganList, selectedPelanggan) {
        if (searchQuery.isBlank() || (selectedPelanggan != null && searchQuery.trim() == selectedPelanggan.nama.trim())) {
            pelangganList
        } else {
            val q = searchQuery.trim().lowercase()
            val qDigits = q.filter { it.isDigit() }
            pelangganList.filter {
                val hpDigits = it.no_hp.filter { c -> c.isDigit() }
                it.nama.lowercase().contains(q) ||
                    it.alamat.lowercase().contains(q) ||
                    it.no_hp.lowercase().contains(q) ||
                    (qDigits.isNotEmpty() && hpDigits.contains(qDigits))
            }
        }
    }

    if (selectedProdukId == null && produkList.isNotEmpty()) {
        val first = produkList.first()
        viewModel.setTxProdukId(first.id, first.harga)
    }

    val totalHarga = jumlah * hargaSatuan
    val isToday = remember(tanggal) { isSameDay(tanggal, System.currentTimeMillis()) }

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
                .verticalScroll(formScrollState)
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Baris Tanggal Transaksi (Default Hari Ini + Dukungan Backdate Nota Kurir)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .pressClickEffect { datePickerDialog.show() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isToday) {
                                "Hari Ini • ${FormatUtils.formatDate(tanggal)}"
                            } else {
                                "Tgl Nota: ${FormatUtils.formatDate(tanggal)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!isToday) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                                modifier = Modifier.clickable {
                                    viewModel.setTxTanggal(System.currentTimeMillis())
                                }
                            ) {
                                Text(
                                    text = "Hari Ini",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    maxLines = 1
                                )
                            }
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (searchQuery.isNotBlank() || selectedPelangganId != null) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        isDropdownExpanded = true
                                        viewModel.onPelangganSelected(null)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Hapus Pilihan",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    isDropdownExpanded = false
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    showAddCustomerDialog = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = "Tambah Pelanggan Baru",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            isDropdownExpanded = false
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
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
                            val custEffectiveKasbon = effectiveKasbonByCustomer[cust.id] ?: cust.sisa_kasbon
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
                                            if (custEffectiveKasbon > 0) subDetails.add("Kasbon: ${FormatUtils.formatRupiah(custEffectiveKasbon)}")
                                            if (cust.harga_khusus != null && cust.harga_khusus > 0) subDetails.add("Khusus: ${FormatUtils.formatRupiah(cust.harga_khusus)}")
                                            if (cust.id_kurir_langganan != null) {
                                                val kName = kurirList.find { it.id == cust.id_kurir_langganan }?.nama
                                                if (kName != null) subDetails.add("Kurir: $kName")
                                            }
                                            if (subDetails.isNotEmpty()) {
                                                Text(
                                                    subDetails.joinToString(" • "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (custEffectiveKasbon > 0) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            )
                        }
                    }
                }
            }

            // 3. Banner Info Profil Otomatis & Kasbon Ramping
            if (selectedPelanggan != null) {
                val selectedEffectiveKasbon = effectiveKasbonByCustomer[selectedPelanggan.id] ?: selectedPelanggan.sisa_kasbon
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

                if (selectedEffectiveKasbon > 0) {
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
                                text = "Sisa kasbon aktif: ${FormatUtils.formatRupiah(selectedEffectiveKasbon)}",
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

            // 4. Katalog Produk (Grid 2 Kolom dengan susunan vertikal agar nama produk tidak terpotong)
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
                                .height(56.dp)
                                .pressClickEffect {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.setTxProdukId(prod.id, prod.harga)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = prod.nama_produk,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(prod.harga),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 5. Baris Horizontal: Harga Satuan & Counter Galon + Quick-Qty Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            .height(60.dp)
                            .padding(top = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Jumlah Galon (Ketuk Angka)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        viewModel.decrementTxJumlah()
                                    },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("btn_minus_galon")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            showEditJumlahDialog = true
                                        }
                                        .testTag("btn_ketik_jumlah_galon")
                                ) {
                                    Text(
                                        text = "$jumlah",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.animateContentSize(),
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        viewModel.incrementTxJumlah()
                                    },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("btn_plus_galon")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Quick-Quantity Chips untuk input cepat 1 ketukan (Eceran & Grosir/Warung)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 2, 3, 5, 10, 15, 20).forEach { presetQty ->
                        val isQtySelected = jumlah == presetQty
                        FilterChip(
                            selected = isQtySelected,
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.setTxJumlah(presetQty)
                            },
                            label = {
                                Text(
                                    text = "$presetQty Galon",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isQtySelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.height(30.dp)
                        )
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
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setTxKurirId(null)
                        },
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
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.setTxKurirId(k.id)
                            },
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

            // 7. Pilihan Status Nota & Metode Pembayaran (Termasuk Bayar Sebagian / Uang Muka)
            val isKasbon = status.equals("Kasbon", ignoreCase = true)
            val isLunas = status.equals("Lunas", ignoreCase = true)
            val isBayarSebagian = metodeBayar.equals("Bayar Sebagian", ignoreCase = true)
            val isKasbonPenuh = isKasbon && !isBayarSebagian
            val totalBelanjaLong = totalHarga.toLong()
            val sisaKasbonPreview = (totalBelanjaLong - uangMuka).coerceAtLeast(0L)
            val isDpExceedingTotal = isBayarSebagian && uangMuka > totalBelanjaLong

            LaunchedEffect(isBayarSebagian) {
                if (isBayarSebagian) {
                    formScrollState.animateScrollTo(formScrollState.maxValue)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Status Nota & Metode Pembayaran",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isLunas && !isBayarSebagian,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setTxStatus("Lunas")
                        },
                        label = {
                            Text(
                                text = "Lunas",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isLunas && !isBayarSebagian) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    )
                    FilterChip(
                        selected = isKasbonPenuh,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setTxStatus("Kasbon")
                        },
                        label = {
                            Text(
                                text = "Kasbon (Belum Bayar)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isKasbonPenuh) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .weight(1.25f)
                            .height(36.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cash", "Transfer", "Bayar Sebagian").forEach { m ->
                        val isChipBayarSebagian = m.equals("Bayar Sebagian", ignoreCase = true)
                        val isSelected = if (isChipBayarSebagian) {
                            isBayarSebagian
                        } else {
                            !isKasbon && metodeBayar.equals(m, ignoreCase = true)
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.setTxMetodeBayar(m)
                            },
                            label = {
                                Text(
                                    text = m,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = if (isChipBayarSebagian) {
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.22f),
                                    selectedLabelColor = Color(0xFFB45309)
                                )
                            } else {
                                FilterChipDefaults.filterChipColors()
                            },
                            modifier = Modifier
                                .weight(if (isChipBayarSebagian) 1.35f else 1f)
                                .height(36.dp)
                                .testTag("chip_metode_${m.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                if (isBayarSebagian) {
                    var isDpFieldFocused by remember { mutableStateOf(false) }
                    LaunchedEffect(isDpFieldFocused) {
                        if (isDpFieldFocused) {
                            formScrollState.animateScrollTo(formScrollState.maxValue)
                        }
                    }

                    OutlinedTextField(
                        value = if (uangMuka > 0L) uangMuka.toString() else "",
                        onValueChange = { input ->
                            val cleanDigits = input.filter { it.isDigit() }.take(10)
                            val parsed = cleanDigits.toLongOrNull() ?: 0L
                            viewModel.setTxUangMuka(parsed)
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        label = {
                            Text(
                                "Nominal Uang Muka / DP (Rp) *",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        placeholder = {
                            Text(
                                "Masukkan nominal uang muka...",
                                style = MaterialTheme.typography.bodyMedium,
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
                        trailingIcon = {
                            if (uangMuka > 0L) {
                                IconButton(
                                    onClick = { viewModel.setTxUangMuka(0L) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Kosongkan Uang Muka",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        isError = isDpExceedingTotal,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isDpFieldFocused = it.isFocused }
                            .testTag("input_uang_muka")
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDpExceedingTotal) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            Color(0xFFF59E0B).copy(alpha = 0.16f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sisa Kasbon: ${FormatUtils.formatRupiah(sisaKasbonPreview.toDouble())}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDpExceedingTotal) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        Color(0xFFB45309)
                                    },
                                    modifier = Modifier.testTag("txt_sisa_kasbon_realtime")
                                )
                                if (uangMuka > 0L && !isDpExceedingTotal) {
                                    Text(
                                        text = "DP: ${FormatUtils.formatRupiah(uangMuka.toDouble())}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                            if (isDpExceedingTotal) {
                                Text(
                                    text = "Uang Muka tidak boleh melebihi Total Belanja (${FormatUtils.formatRupiah(totalHarga)})!",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Total (${jumlah} Galon): ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = FormatUtils.formatRupiah(totalHarga),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

                    Spacer(modifier = Modifier.width(8.dp))

                    val isBottomKasbon = status.equals("Kasbon", ignoreCase = true) || metodeBayar.equals("Bayar Sebagian", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isBottomKasbon) Color(0xFFF59E0B).copy(alpha = 0.18f) else Color(0xFF10B981).copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = when {
                                metodeBayar.equals("Bayar Sebagian", ignoreCase = true) -> "KASBON (DP)"
                                isBottomKasbon -> "DICATAT KASBON"
                                else -> "LUNAS"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isBottomKasbon) Color(0xFFB45309) else Color(0xFF047857),
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
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            val prevKasbon = selectedPelangganId?.let { effectiveKasbonByCustomer[it] } ?: 0.0
                            viewModel.submitTransaksi { savedDetail ->
                                if (savedDetail != null) {
                                    val newKasbonTotal = if (savedDetail.transaksi.status.equals("Kasbon", ignoreCase = true)) {
                                        prevKasbon + savedDetail.transaksi.remainingKasbonAmount()
                                    } else {
                                        prevKasbon
                                    }
                                    val text = SantoyaReceiptHelper.generateSaleReceipt(savedDetail, newKasbonTotal)
                                    viewModel.showReceiptDialog(
                                        ActiveReceiptDialog(
                                            title = "Struk Transaksi Penjualan",
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
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.submitTransaksi { _ ->
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

    if (showEditJumlahDialog) {
        var jumlahInput by remember(jumlah) { mutableStateOf(jumlah.toString()) }
        AlertDialog(
            onDismissRequest = { showEditJumlahDialog = false },
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Ketik Jumlah Galon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = jumlahInput,
                        onValueChange = { text ->
                            jumlahInput = text.filter { it.isDigit() }.take(4)
                        },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        ),
                        label = {
                            Text(
                                "Jumlah Galon *",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Masukkan jumlah pembelian galon (minimal 1 galon).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = jumlahInput.toIntOrNull() ?: 1
                        viewModel.setTxJumlah(parsed.coerceAtLeast(1))
                        showEditJumlahDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Terapkan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditJumlahDialog = false }) {
                    Text(
                        "Batal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
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
                            focusManager.clearFocus()
                            keyboardController?.hide()
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

private enum class RiwayatFilterOption(val label: String) {
    SEMUA("Semua"),
    HARI_INI("Hari Ini"),
    LUNAS("Lunas"),
    KASBON("Kasbon")
}

@Composable
fun RiwayatTransaksiTab(
    viewModel: SantoyaViewModel,
    transaksiList: List<TransaksiDetail>,
    pelangganList: List<Pelanggan>,
    kurirList: List<Kurir> = emptyList(),
    produkList: List<Produk> = emptyList(),
    effectiveKasbonByCustomer: Map<Int, Double> = emptyMap()
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(RiwayatFilterOption.SEMUA) }
    var showAllHistory by remember { mutableStateOf(false) }
    var editingTransaksi by remember { mutableStateOf<TransaksiDetail?>(null) }
    var deletingTransaksi by remember { mutableStateOf<TransaksiDetail?>(null) }
    var markingLunasTransaksi by remember { mutableStateOf<TransaksiDetail?>(null) }

    val nowMillis = remember(transaksiList) { System.currentTimeMillis() }

    val countHariIni = remember(transaksiList, nowMillis) {
        transaksiList.count { isSameDay(it.transaksi.tanggal, nowMillis) }
    }
    val countLunas = remember(transaksiList) {
        transaksiList.count { it.transaksi.status.equals("Lunas", ignoreCase = true) }
    }
    val countKasbon = remember(transaksiList) {
        transaksiList.count { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
    }

    val filtered = remember(searchQuery, selectedFilter, transaksiList, nowMillis) {
        val byStatusOrDate = when (selectedFilter) {
            RiwayatFilterOption.SEMUA -> transaksiList
            RiwayatFilterOption.HARI_INI -> transaksiList.filter { isSameDay(it.transaksi.tanggal, nowMillis) }
            RiwayatFilterOption.LUNAS -> transaksiList.filter { it.transaksi.status.equals("Lunas", ignoreCase = true) }
            RiwayatFilterOption.KASBON -> transaksiList.filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
        }
        if (searchQuery.isBlank()) {
            byStatusOrDate
        } else {
            val q = searchQuery.lowercase().trim()
            val qDigits = q.filter { it.isDigit() }
            byStatusOrDate.filter { detail ->
                val isKasbonTx = detail.transaksi.status.equals("Kasbon", ignoreCase = true)
                val formattedTotal = FormatUtils.formatRupiah(detail.transaksi.total_harga).lowercase()
                val rawTotalStr = detail.transaksi.total_harga.toLong().toString()
                val formattedDateFull = FormatUtils.formatDate(detail.transaksi.tanggal).lowercase()
                val formattedDateShort = FormatUtils.formatDateShort(detail.transaksi.tanggal).lowercase()
                val galonLabel = "${detail.transaksi.jumlah} galon"
                val kurirLabel = detail.kurir?.nama?.lowercase() ?: "ambil sendiri"
                val metodeLabel = if (isKasbonTx) "belum dibayar" else detail.transaksi.metode_bayar.lowercase()
                val hpDigits = detail.pelanggan?.no_hp?.filter { it.isDigit() } ?: ""

                (detail.pelanggan?.nama?.lowercase()?.contains(q) == true) ||
                    (detail.pelanggan?.alamat?.lowercase()?.contains(q) == true) ||
                    (detail.pelanggan?.no_hp?.lowercase()?.contains(q) == true) ||
                    (qDigits.isNotEmpty() && hpDigits.contains(qDigits)) ||
                    kurirLabel.contains(q) ||
                    (detail.produk?.nama_produk?.lowercase()?.contains(q) == true) ||
                    metodeLabel.contains(q) ||
                    detail.transaksi.status.lowercase().contains(q) ||
                    formattedTotal.contains(q) ||
                    (qDigits.isNotEmpty() && rawTotalStr.contains(qDigits)) ||
                    galonLabel.contains(q) ||
                    detail.transaksi.jumlah.toString() == q ||
                    formattedDateFull.contains(q) ||
                    formattedDateShort.contains(q)
            }
        }
    }

    val totalGalonFiltered = remember(filtered) {
        filtered.sumOf { it.transaksi.jumlah }
    }
    val totalLunasFiltered = remember(filtered) {
        filtered.sumOf {
            if (it.transaksi.status.equals("Lunas", ignoreCase = true)) {
                it.transaksi.total_harga
            } else {
                it.transaksi.uangMuka.toDouble()
            }
        }
    }
    val totalKasbonFiltered = remember(filtered) {
        filtered.filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
            .sumOf { it.transaksi.remainingKasbonAmount() }
    }
    val totalNominalFiltered = totalLunasFiltered + totalKasbonFiltered

    val displayedHistory = remember(filtered, showAllHistory) {
        if (showAllHistory) filtered else filtered.take(30)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Search Bar & Filter Chips di bagian atas Riwayat Transaksi
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    showAllHistory = false
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = {
                    Text(
                        "Cari nama, nominal, galon, tanggal, kurir...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Bersihkan Pencarian",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_transaksi")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RiwayatFilterOption.entries.forEach { option ->
                    val isSelected = selectedFilter == option
                    val count = when (option) {
                        RiwayatFilterOption.SEMUA -> transaksiList.size
                        RiwayatFilterOption.HARI_INI -> countHariIni
                        RiwayatFilterOption.LUNAS -> countLunas
                        RiwayatFilterOption.KASBON -> countKasbon
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            selectedFilter = option
                            showAllHistory = false
                        },
                        label = {
                            Text(
                                text = "${option.label} ($count)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (option) {
                                RiwayatFilterOption.KASBON -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                RiwayatFilterOption.LUNAS -> Color(0xFF10B981).copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            selectedLabelColor = when (option) {
                                RiwayatFilterOption.KASBON -> Color(0xFFB45309)
                                RiwayatFilterOption.LUNAS -> Color(0xFF047857)
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        ),
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3. Mini Summary Banner (Rekap Hasil Filter / Pencarian Aktif)
            if (filtered.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filtered.size} Transaksi • $totalGalonFiltered Galon",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(totalNominalFiltered),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.14f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Lunas",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF047857)
                                        )
                                        Text(
                                            text = FormatUtils.formatRupiah(totalLunasFiltered),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Kasbon",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFB45309)
                                        )
                                        Text(
                                            text = FormatUtils.formatRupiah(totalKasbonFiltered),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Transaksi Tidak Ditemukan" else "Belum Ada Riwayat Transaksi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    "Tidak ada nota yang cocok dengan kata kunci \"$searchQuery\"."
                                } else {
                                    "Belum ada transaksi penjualan pada filter ${selectedFilter.label}."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isNotBlank() || selectedFilter != RiwayatFilterOption.SEMUA) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedFilter = RiwayatFilterOption.SEMUA
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reset Pencarian & Filter",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
            items(displayedHistory, key = { it.transaksi.id }) { detail ->
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
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

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = FormatUtils.formatRupiah(detail.transaksi.total_harga),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = when {
                                        isKasbon && detail.transaksi.uangMuka > 0L -> {
                                            "DP: ${FormatUtils.formatRupiah(detail.transaksi.uangMuka.toDouble())} • Sisa: ${FormatUtils.formatRupiah(detail.transaksi.remainingKasbonAmount())}"
                                        }
                                        isKasbon -> "Belum Dibayar"
                                        else -> detail.transaksi.metode_bayar.ifBlank { "Cash" }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isKasbon) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isKasbon) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        markingLunasTransaksi = detail
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Tandai Lunas",
                                            tint = Color(0xFF047857),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Lunasi Nota",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { editingTransaksi = detail },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val custKasbon = effectiveKasbonByCustomer[detail.transaksi.id_pelanggan]
                                            ?: pelangganList.find { it.id == detail.transaksi.id_pelanggan }?.sisa_kasbon
                                            ?: 0.0
                                        val text = SantoyaReceiptHelper.generateSaleReceipt(detail, custKasbon)
                                        viewModel.showReceiptDialog(
                                            ActiveReceiptDialog(
                                                title = "Struk Transaksi Penjualan",
                                                receiptText = text,
                                                customerPhone = detail.pelanggan?.no_hp,
                                                data = detail
                                            )
                                        )
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Print, contentDescription = "Cetak", modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val custKasbon = effectiveKasbonByCustomer[detail.transaksi.id_pelanggan]
                                            ?: pelangganList.find { it.id == detail.transaksi.id_pelanggan }?.sisa_kasbon
                                            ?: 0.0
                                        val text = SantoyaReceiptHelper.generateSaleReceipt(detail, custKasbon)
                                        SantoyaReceiptHelper.shareReceiptText(
                                            context = viewModel.getApplication(),
                                            text = text,
                                            phoneNumber = detail.pelanggan?.no_hp
                                        )
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Bagikan", modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = { deletingTransaksi = detail },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filtered.size > 30) {
                item {
                    OutlinedButton(
                        onClick = { showAllHistory = !showAllHistory },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (showAllHistory) {
                                "Tampilkan Lebih Sedikit (30 Terbaru)"
                            } else {
                                "Tampilkan Semua Riwayat (${filtered.size} Transaksi)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        }
    }

    if (markingLunasTransaksi != null) {
        val target = markingLunasTransaksi!!
        AlertDialog(
            onDismissRequest = { markingLunasTransaksi = null },
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Lunasi Nota Kasbon?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tandai sisa kasbon ${target.pelanggan?.nama ?: "Pelanggan Umum"} sebesar ${FormatUtils.formatRupiah(target.transaksi.remainingKasbonAmount())} (${target.transaksi.jumlah} Galon) sebagai LUNAS?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markTransaksiLunas(target.transaksi.id)
                        markingLunasTransaksi = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Ya, Tandai Lunas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { markingLunasTransaksi = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Batal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    if (deletingTransaksi != null) {
        val target = deletingTransaksi!!
        AlertDialog(
            onDismissRequest = { deletingTransaksi = null },
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Hapus Transaksi?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus transaksi ${target.pelanggan?.nama ?: "Pelanggan Umum"} sebesar ${FormatUtils.formatRupiah(target.transaksi.total_harga)}? Sisa kasbon pelanggan akan disesuaikan secara otomatis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaksi(target.transaksi)
                        deletingTransaksi = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Ya, Hapus",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deletingTransaksi = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Batal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    if (editingTransaksi != null) {
        val detail = editingTransaksi!!
        var editJumlah by remember(detail) { mutableStateOf(detail.transaksi.jumlah.toString()) }
        var editHarga by remember(detail) { mutableStateOf(detail.transaksi.harga_satuan.toInt().toString()) }
        var editMetode by remember(detail) { mutableStateOf(detail.transaksi.metode_bayar) }
        var editStatus by remember(detail) { mutableStateOf(detail.transaksi.status) }
        var editUangMuka by remember(detail) { mutableStateOf(if (detail.transaksi.uangMuka > 0L) detail.transaksi.uangMuka.toString() else "") }
        var editKurirId by remember(detail) { mutableStateOf(detail.transaksi.id_kurir) }
        var editProdukId by remember(detail) { mutableStateOf(detail.transaksi.id_produk) }

        val parsedJumlah = editJumlah.toIntOrNull() ?: 0
        val parsedHarga = editHarga.toDoubleOrNull() ?: 0.0
        val previewTotalHarga = parsedJumlah * parsedHarga
        val isEditBayarSebagian = editMetode.equals("Bayar Sebagian", ignoreCase = true)
        val parsedEditDp = editUangMuka.toLongOrNull() ?: 0L
        val isValidEdit = parsedJumlah > 0 && parsedHarga > 0.0 &&
            (!isEditBayarSebagian || (parsedEditDp > 0L && parsedEditDp <= previewTotalHarga.toLong()))

        AlertDialog(
            onDismissRequest = { editingTransaksi = null },
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Edit Transaksi Penjualan",
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
                                "Jumlah Galon *",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        isError = parsedJumlah <= 0,
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
                                "Harga Satuan (Rp) *",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        isError = parsedHarga <= 0.0,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Live Preview Total Harga Baru
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Harga Baru:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = FormatUtils.formatRupiah(previewTotalHarga),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        "Metode Pembayaran:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isEditKasbon = editStatus.equals("Kasbon", ignoreCase = true)
                        listOf("Cash", "Transfer", "Bayar Sebagian").forEach { metode ->
                            val isSelected = if (metode.equals("Bayar Sebagian", ignoreCase = true)) {
                                isEditBayarSebagian
                            } else {
                                !isEditKasbon && editMetode.equals(metode, ignoreCase = true)
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    editMetode = metode
                                    if (metode.equals("Bayar Sebagian", ignoreCase = true)) {
                                        editStatus = "Kasbon"
                                    } else {
                                        editStatus = "Lunas"
                                        editUangMuka = ""
                                    }
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

                    if (isEditBayarSebagian) {
                        val editSisaPreview = (previewTotalHarga.toLong() - parsedEditDp).coerceAtLeast(0L)
                        OutlinedTextField(
                            value = editUangMuka,
                            onValueChange = { editUangMuka = it.filter { c -> c.isDigit() } },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            label = { Text("Uang Muka / DP (Rp) *") },
                            isError = parsedEditDp <= 0L || parsedEditDp > previewTotalHarga.toLong(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Text(
                            text = "Sisa Kasbon: ${FormatUtils.formatRupiah(editSisaPreview.toDouble())}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }

                    Text(
                        "Status Pembayaran:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Lunas", "Kasbon").forEach { st ->
                            val isSelected = if (st.equals("Lunas", ignoreCase = true)) {
                                editStatus.equals("Lunas", ignoreCase = true) && !isEditBayarSebagian
                            } else {
                                editStatus.equals("Kasbon", ignoreCase = true) && !isEditBayarSebagian
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    editStatus = st
                                    editUangMuka = ""
                                    if (st.equals("Kasbon", ignoreCase = true)) {
                                        editMetode = ""
                                    } else if (st.equals("Lunas", ignoreCase = true) && (editMetode.isBlank() || isEditBayarSebagian)) {
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
                        if (isValidEdit) {
                            val finalStatus = if (isEditBayarSebagian) "Kasbon" else editStatus
                            val finalMetode = when {
                                isEditBayarSebagian -> "Bayar Sebagian"
                                finalStatus.equals("Kasbon", ignoreCase = true) -> "-"
                                else -> editMetode.ifBlank { "Cash" }
                            }
                            viewModel.updateTransaksi(
                                id = detail.transaksi.id,
                                tanggal = detail.transaksi.tanggal,
                                idPelanggan = detail.transaksi.id_pelanggan,
                                idKurir = editKurirId,
                                idProduk = editProdukId,
                                jumlah = parsedJumlah,
                                hargaSatuan = parsedHarga,
                                metodeBayar = finalMetode,
                                status = finalStatus,
                                uangMuka = if (isEditBayarSebagian) parsedEditDp else 0L,
                                onSuccess = { editingTransaksi = null }
                            )
                        }
                    },
                    enabled = isValidEdit,
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
