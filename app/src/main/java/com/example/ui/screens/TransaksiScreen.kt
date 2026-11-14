package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.companion.kzpmrw.R
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.TransaksiDetail
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.example.ui.components.HydroBentoCard
import com.example.ui.components.HydroGalonIcon
import com.example.ui.components.HydroGradientBadge
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.components.SantoyaFormBottomSheet
import com.example.ui.components.SantoyaLoadingSkeleton
import com.example.ui.components.bringIntoViewOnFocus
import com.example.ui.components.rememberIsImeVisible
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.pressClickEffect
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.KomisiCalculator
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
    isKaryawan: Boolean? = null,
    activeKaryawan: Kurir? = null,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.transaksiTab.collectAsState()
    val vmIsKaryawan by viewModel.isKaryawan.collectAsState()
    val vmActiveKaryawan by viewModel.activeKaryawan.collectAsState()
    val effectiveIsKaryawan = isKaryawan ?: vmIsKaryawan
    val effectiveActiveKaryawan = activeKaryawan ?: vmActiveKaryawan
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = selectedTab.coerceIn(0, 1), pageCount = { 2 })

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (selectedTab != pagerState.currentPage) {
            viewModel.setTransaksiTab(pagerState.currentPage)
        }
    }

    val activePelangganList = remember(pelangganList) {
        pelangganList.filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) }
    }

    val recentCustomers = remember(transaksiList, activePelangganList) {
        val byId = activePelangganList.associateBy { it.id }
        val seen = LinkedHashSet<Int>()
        val result = mutableListOf<Pelanggan>()
        transaksiList
            .asSequence()
            .filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
            .sortedByDescending { it.transaksi.tanggal }
            .forEach { detail ->
                val pId = detail.transaksi.id_pelanggan
                if (pId != null && seen.add(pId)) {
                    byId[pId]?.let { result.add(it) }
                }
            }
        if (result.size < 6) {
            activePelangganList.forEach { cust ->
                if (result.size < 6 && seen.add(cust.id)) {
                    result.add(cust)
                }
            }
        }
        result.take(6)
    }

    val activeTransaksiCount = remember(transaksiList) {
        transaksiList.count { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
    }

    val effectiveKasbonByCustomer = remember(activePelangganList, transaksiList) {
        val unpaidByCustomer = transaksiList
            .filter {
                !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) &&
                    it.transaksi.status.equals("Kasbon", ignoreCase = true)
            }
            .groupBy { it.transaksi.id_pelanggan }
        activePelangganList.associate { p ->
            val unpaidNotes = unpaidByCustomer[p.id] ?: emptyList()
            val unpaidSum = unpaidNotes.sumOf { it.transaksi.remainingKasbonAmount() }
            val effectiveKasbon = if (unpaidNotes.isNotEmpty()) unpaidSum else p.sisa_kasbon
            p.id to effectiveKasbon
        }
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                text = {
                    Text(
                        "Catat Penjualan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (pagerState.currentPage == 0) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                },
                modifier = Modifier.testTag("tab_catat_penjualan")
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                text = {
                    Text(
                        "Riwayat ($activeTransaksiCount)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (pagerState.currentPage == 1) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                },
                modifier = Modifier.testTag("tab_riwayat_penjualan")
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val pageAlpha = (1f - (pageOffset.absoluteValue * 0.35f)).coerceIn(0.5f, 1f)
            val pageScale = (1f - (pageOffset.absoluteValue * 0.04f)).coerceIn(0.96f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = pageAlpha
                        scaleX = pageScale
                        scaleY = pageScale
                    }
            ) {
                if (page == 0) {
                    CatatPenjualanTab(
                        viewModel = viewModel,
                        pelangganList = activePelangganList,
                        recentCustomers = recentCustomers,
                        produkList = produkList,
                        kurirList = kurirList,
                        effectiveKasbonByCustomer = effectiveKasbonByCustomer,
                        isKaryawan = effectiveIsKaryawan,
                        activeKaryawan = effectiveActiveKaryawan
                    )
                } else {
                    RiwayatTransaksiTab(
                        viewModel = viewModel,
                        transaksiList = transaksiList,
                        pelangganList = activePelangganList,
                        kurirList = kurirList,
                        produkList = produkList,
                        effectiveKasbonByCustomer = effectiveKasbonByCustomer,
                        isKaryawan = effectiveIsKaryawan,
                        activeKaryawan = effectiveActiveKaryawan
                    )
                }
            }
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
    effectiveKasbonByCustomer: Map<Int, Double> = emptyMap(),
    isKaryawan: Boolean = false,
    activeKaryawan: Kurir? = null,
    recentCustomers: List<Pelanggan> = emptyList()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = rememberIsImeVisible()
    val tanggal by viewModel.txTanggal.collectAsState()
    val selectedPelangganId by viewModel.txPelangganId.collectAsState()
    val selectedProdukId by viewModel.txProdukId.collectAsState()
    val selectedKurirId by viewModel.txKurirId.collectAsState()
    val jumlah by viewModel.txJumlah.collectAsState()
    val hargaSatuan by viewModel.txHargaSatuan.collectAsState()
    val metodeBayar by viewModel.txMetodeBayar.collectAsState()
    val status by viewModel.txStatus.collectAsState()
    val uangMuka by viewModel.txUangMuka.collectAsState()
    val isBusyAction by viewModel.isBusyAction.collectAsState()
    val formScrollState = rememberScrollState()

    LaunchedEffect(isKaryawan, activeKaryawan?.id, selectedKurirId) {
        if (isKaryawan && activeKaryawan != null && selectedKurirId != activeKaryawan.id) {
            viewModel.setTxKurirId(activeKaryawan.id)
        }
    }

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
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = if (isImeVisible) 116.dp else 148.dp
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Baris Tanggal Transaksi (Default Hari Ini + Dukungan Backdate Nota Kurir)
            HydroBentoCard(
                accentColor = MaterialTheme.colorScheme.primary,
                cornerRadius = 14.dp,
                tintAlpha = 0.06f,
                showWaterRipple = false,
                onClick = { datePickerDialog.show() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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
                        HydroGradientBadge(
                            gradientColors = listOf(MaterialTheme.colorScheme.primary, AccentCyan),
                            size = 28.dp,
                            cornerRadius = 9.dp,
                            elevation = 2.dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isToday) {
                                "Hari Ini • ${FormatUtils.formatDateUi(tanggal)}"
                            } else {
                                "Tanggal: ${FormatUtils.formatDateUi(tanggal)}"
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
                            val santoyaColors = MaterialTheme.santoyaColors
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = santoyaColors.warningContainer,
                                modifier = Modifier.clickable {
                                    viewModel.setTxTanggal(System.currentTimeMillis())
                                }
                            ) {
                                Text(
                                    text = "Hari Ini",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = santoyaColors.onWarningContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "Ubah",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 2. Quick-Customer 1-Ketuk + Autocomplete Pemilihan Pelanggan
            if (recentCustomers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cepat:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    recentCustomers.forEach { cust ->
                        val isChipSelected = cust.id == selectedPelangganId
                        FilterChip(
                            selected = isChipSelected,
                            onClick = {
                                searchQuery = cust.nama
                                isDropdownExpanded = false
                                viewModel.onPelangganSelected(cust.id)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                            label = {
                                Text(
                                    text = cust.nama,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

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
                            "Pelanggan *",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    placeholder = {
                        Text(
                            "Cari nama, alamat, atau no. HP...",
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
                        .bringIntoViewOnFocus()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                        .testTag("input_cari_pelanggan")
                )

                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.heightIn(max = 220.dp)
                ) {
                    if (filteredPelanggan.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "\"$searchQuery\" belum ada • Tambah pelanggan baru",
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
                                                    color = if (custEffectiveKasbon > 0) MaterialTheme.santoyaColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                val kName = kurirList.find { it.id == selectedPelanggan.id_kurir_langganan }?.nama?.takeIf { it.isNotBlank() } ?: "Tanpa Kurir"
                                autoDetails.add("Kurir: $kName")
                            }
                            Text(
                                text = "Terpasang otomatis: ${autoDetails.joinToString(" • ")}",
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
                    val santoyaColors = MaterialTheme.santoyaColors
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = santoyaColors.warningContainer.copy(alpha = 0.7f),
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
                                tint = santoyaColors.warning,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sisa kasbon aktif: ${FormatUtils.formatRupiah(selectedEffectiveKasbon)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = santoyaColors.onWarningContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 4. Katalog Produk (Hydro Bento Product Tiles dengan Ikon Galon & Badge Gradien)
            val chunkedProduk = produkList.chunked(2)
            chunkedProduk.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { prod ->
                        val isSelected = prod.id == selectedProdukId
                        val tileAccent = MaterialTheme.colorScheme.primary
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .pressClickEffect {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.setTxProdukId(prod.id, prod.harga)
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 3.dp else 0.5.dp,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) tileAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isSelected) {
                                            tileAccent.copy(alpha = 0.10f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    HydroGradientBadge(
                                        gradientColors = listOf(tileAccent, AccentCyan),
                                        size = 34.dp,
                                        cornerRadius = 10.dp,
                                        elevation = 2.dp
                                    ) {
                                        HydroGalonIcon(
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            HydroGalonIcon(
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(9.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = prod.nama_produk,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = FormatUtils.formatRupiah(prod.harga),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) tileAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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
                            if (!isKaryawan) {
                                val num = input.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                                viewModel.setTxHargaSatuan(num)
                            }
                        },
                        readOnly = isKaryawan,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isKaryawan) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        ),
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
                        trailingIcon = if (isKaryawan) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Harga dikunci Pemilik",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        supportingText = if (isKaryawan) {
                            {
                                Text(
                                    text = "Harga dikunci Pemilik",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isKaryawan) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            unfocusedContainerColor = if (isKaryawan) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1.3f)
                            .bringIntoViewOnFocus()
                            .testTag("input_harga_satuan")
                    )

                    HydroBentoCard(
                        accentColor = MaterialTheme.colorScheme.primary,
                        cornerRadius = 14.dp,
                        tintAlpha = 0.07f,
                        showWaterRipple = false,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(64.dp)
                            .padding(top = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                HydroGalonIcon(
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Jumlah Galon",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                if (isKaryawan && activeKaryawan != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = activeKaryawan.nama,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Otomatis Akun Anda",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
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
            }

            // 7. Pilihan Pembayaran Terpadu (Cash, Transfer, DP Sebagian, Kasbon Penuh)
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pembayaran",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val santoyaColors = MaterialTheme.santoyaColors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isCashSelected = isLunas && !isBayarSebagian && metodeBayar.equals("Cash", ignoreCase = true)
                    FilterChip(
                        selected = isCashSelected,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setTxMetodeBayar("Cash")
                        },
                        label = {
                            Text(
                                text = "Cash (Lunas)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isCashSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .testTag("chip_metode_cash")
                    )

                    val isTransferSelected = isLunas && !isBayarSebagian && metodeBayar.equals("Transfer", ignoreCase = true)
                    FilterChip(
                        selected = isTransferSelected,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setTxMetodeBayar("Transfer")
                        },
                        label = {
                            Text(
                                text = "Transfer (Lunas)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isTransferSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .testTag("chip_metode_transfer")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isBayarSebagian,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setTxMetodeBayar("Bayar Sebagian")
                        },
                        label = {
                            Text(
                                text = "DP Sebagian",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isBayarSebagian) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = santoyaColors.warningContainer,
                            selectedLabelColor = santoyaColors.onWarningContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .testTag("chip_metode_bayar_sebagian")
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
                                text = "Kasbon Penuh",
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
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .testTag("chip_metode_kasbon")
                    )
                }

                if (isBayarSebagian) {
                    var isDpFieldFocused by remember { mutableStateOf(false) }
                    LaunchedEffect(isDpFieldFocused) {
                        if (isDpFieldFocused) {
                            formScrollState.animateScrollTo(formScrollState.maxValue)
                        }
                    }

                    val dpPresets = remember(totalBelanjaLong) {
                        if (totalBelanjaLong <= 1L) emptyList()
                        else listOfNotNull(
                            ((totalBelanjaLong * 25) / 100).takeIf { it in 1L until totalBelanjaLong }?.let { "25%" to it },
                            ((totalBelanjaLong * 50) / 100).takeIf { it in 1L until totalBelanjaLong }?.let { "50%" to it },
                            10_000L.takeIf { it in 1L until totalBelanjaLong }?.let { "10rb" to it },
                            20_000L.takeIf { it in 1L until totalBelanjaLong }?.let { "20rb" to it }
                        ).distinctBy { it.second }
                    }

                    if (dpPresets.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DP Cepat:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            dpPresets.forEach { (label, presetAmount) ->
                                val isPresetSelected = uangMuka == presetAmount
                                FilterChip(
                                    selected = isPresetSelected,
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        viewModel.setTxUangMuka(presetAmount)
                                    },
                                    label = {
                                        Text(
                                            text = "$label (${FormatUtils.formatRupiah(presetAmount.toDouble())})",
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.height(32.dp)
                                )
                            }
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
                                "Uang Muka / DP (Rp) *",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        placeholder = {
                            Text(
                                "Tulis nominal DP",
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
                                    modifier = Modifier.size(48.dp)
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
                            .bringIntoViewOnFocus()
                            .onFocusChanged { isDpFieldFocused = it.isFocused }
                            .testTag("input_uang_muka")
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDpExceedingTotal) {
                            santoyaColors.dangerContainer
                        } else {
                            santoyaColors.warningContainer.copy(alpha = 0.7f)
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
                                        santoyaColors.danger
                                    } else {
                                        santoyaColors.onWarningContainer
                                    },
                                    modifier = Modifier.testTag("txt_sisa_kasbon_realtime")
                                )
                                if (uangMuka > 0L && !isDpExceedingTotal) {
                                    Text(
                                        text = "DP: ${FormatUtils.formatRupiah(uangMuka.toDouble())}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = santoyaColors.success
                                    )
                                }
                            }
                            if (isDpExceedingTotal) {
                                Text(
                                    text = "Uang muka melebihi total belanja (${FormatUtils.formatRupiah(totalHarga)})",
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
                    .padding(horizontal = 16.dp, vertical = if (isImeVisible) 8.dp else 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Total (${jumlah} Galon): ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                            SantoyaAdaptiveCurrencyText(
                                amount = totalHarga,
                                style = if (isImeVisible) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                minFontSize = 12.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        AnimatedVisibility(visible = !isImeVisible && selectedKurir != null) {
                            selectedKurir?.let { kurir ->
                                val komisiBreakdown = KomisiCalculator.calculateTransactionCommission(
                                    kurir = kurir,
                                    jumlah = jumlah
                                )
                                Text(
                                    text = "Komisi (${kurir.nama}): ${FormatUtils.formatRupiah(komisiBreakdown.totalKomisi)} (${FormatUtils.formatRupiah(komisiBreakdown.perGalonRate)}/gal)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val isBottomKasbon = status.equals("Kasbon", ignoreCase = true) || metodeBayar.equals("Bayar Sebagian", ignoreCase = true)
                    val santoyaColors = MaterialTheme.santoyaColors
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isBottomKasbon) santoyaColors.warningContainer else santoyaColors.successContainer
                    ) {
                        Text(
                            text = when {
                                metodeBayar.equals("Bayar Sebagian", ignoreCase = true) -> "KASBON (DP)"
                                isBottomKasbon -> "KASBON"
                                else -> "LUNAS"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isBottomKasbon) santoyaColors.onWarningContainer else santoyaColors.onSuccessContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isImeVisible) 6.dp else 8.dp))

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
                        enabled = !isBusyAction,
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
                            if (isBusyAction) "Memproses..." else "Cetak Struk",
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
                        enabled = !isBusyAction,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status.equals("Kasbon", ignoreCase = true)) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            contentColor = if (status.equals("Kasbon", ignoreCase = true)) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_simpan_transaksi")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isBusyAction) "Menyimpan..." else "Simpan",
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
        val applyJumlah = {
            val parsed = jumlahInput.toIntOrNull() ?: 1
            viewModel.setTxJumlah(parsed.coerceAtLeast(1))
            focusManager.clearFocus()
            keyboardController?.hide()
            showEditJumlahDialog = false
        }
        SantoyaFormBottomSheet(
            title = "Atur Jumlah Galon",
            subtitle = "Pilih cepat atau ketik jumlah manual",
            headerIcon = Icons.Default.Edit,
            onDismiss = { showEditJumlahDialog = false },
            confirmLabel = "Terapkan",
            onConfirm = applyJumlah
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(1, 2, 3, 5, 10, 15, 20).forEach { preset ->
                    val isSelected = jumlahInput == preset.toString()
                    FilterChip(
                        selected = isSelected,
                        onClick = { jumlahInput = preset.toString() },
                        label = {
                            Text(
                                text = "$preset Galon",
                                maxLines = 1,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { applyJumlah() }),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
            )
            Text(
                text = "Isi jumlah galon (minimal 1).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showAddCustomerDialog) {
        val initialNama = if (pelangganList.none { it.nama.equals(searchQuery.trim(), ignoreCase = true) }) searchQuery.trim() else ""
        var namaBaru by remember { mutableStateOf(initialNama) }
        var alamatBaru by remember { mutableStateOf("") }
        var noHpBaru by remember { mutableStateOf("") }
        var hargaKhususBaru by remember { mutableStateOf("") }
        val lockedKurirBaruId = if (isKaryawan && activeKaryawan != null) activeKaryawan.id else null
        var selectedKurirBaruId by remember(lockedKurirBaruId) { mutableStateOf<Int?>(lockedKurirBaruId) }

        val namaFocus = remember { FocusRequester() }
        val alamatFocus = remember { FocusRequester() }
        val hpFocus = remember { FocusRequester() }
        val hargaFocus = remember { FocusRequester() }

        SantoyaFormBottomSheet(
            title = "Tambah Pelanggan Baru",
            subtitle = "Otomatis terpilih di transaksi ini",
            headerIcon = Icons.Default.PersonAdd,
            onDismiss = { showAddCustomerDialog = false },
            confirmLabel = "Simpan",
            confirmEnabled = namaBaru.isNotBlank(),
            onConfirm = {
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
            }
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { alamatFocus.requestFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(namaFocus)
                    .bringIntoViewOnFocus(),
                shape = RoundedCornerShape(12.dp)
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { hpFocus.requestFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(alamatFocus)
                    .bringIntoViewOnFocus(),
                shape = RoundedCornerShape(12.dp)
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = if (isKaryawan) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { if (!isKaryawan) hargaFocus.requestFocus() },
                    onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(hpFocus)
                    .bringIntoViewOnFocus(),
                shape = RoundedCornerShape(12.dp)
            )
            if (isKaryawan) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Tarif Harga Terkunci",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Harga mengikuti tarif dari Pemilik.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
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
                            "Isi jika pakai harga khusus",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
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
                        .focusRequester(hargaFocus)
                        .bringIntoViewOnFocus(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Text(
                text = if (isKaryawan && activeKaryawan != null) "Kurir Langganan" else "Kurir Langganan (Opsional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isKaryawan && activeKaryawan != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Otomatis: ${activeKaryawan.nama}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Pelanggan ini otomatis jadi langganan Anda.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            } else {
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
                        modifier = Modifier.height(34.dp)
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
                            modifier = Modifier.height(34.dp)
                        )
                    }
                }
            }
        }
    }
}

private enum class RiwayatFilterOption(val label: String) {
    SEMUA("Semua"),
    HARI_INI("Hari Ini"),
    LUNAS("Lunas"),
    KASBON("Kasbon"),
    DIHAPUS("Dihapus")
}

@Composable
fun RiwayatTransaksiTab(
    viewModel: SantoyaViewModel,
    transaksiList: List<TransaksiDetail>,
    pelangganList: List<Pelanggan>,
    kurirList: List<Kurir> = emptyList(),
    produkList: List<Produk> = emptyList(),
    effectiveKasbonByCustomer: Map<Int, Double> = emptyMap(),
    isKaryawan: Boolean = false,
    activeKaryawan: Kurir? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isInitialDataLoaded by viewModel.isInitialDataLoaded.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(RiwayatFilterOption.SEMUA) }
    var showAllHistory by remember { mutableStateOf(false) }
    var editingTransaksi by remember { mutableStateOf<TransaksiDetail?>(null) }
    var deletingTransaksi by remember { mutableStateOf<TransaksiDetail?>(null) }
    var markingLunasTransaksi by remember { mutableStateOf<TransaksiDetail?>(null) }

    val nowMillis = remember(transaksiList) { System.currentTimeMillis() }

    val activeTransactions = remember(transaksiList) {
        transaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
    }
    val deletedTransactions = remember(transaksiList) {
        transaksiList.filter { KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
    }

    LaunchedEffect(isKaryawan, deletedTransactions.size) {
        if (selectedFilter == RiwayatFilterOption.DIHAPUS && (isKaryawan || deletedTransactions.isEmpty())) {
            selectedFilter = RiwayatFilterOption.SEMUA
        }
    }

    val countHariIni = remember(activeTransactions, nowMillis) {
        activeTransactions.count { isSameDay(it.transaksi.tanggal, nowMillis) }
    }
    val countLunas = remember(activeTransactions) {
        activeTransactions.count { it.transaksi.status.equals("Lunas", ignoreCase = true) }
    }
    val countKasbon = remember(activeTransactions) {
        activeTransactions.count { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
    }
    val countDihapus = deletedTransactions.size

    val filtered = remember(searchQuery, selectedFilter, transaksiList, activeTransactions, deletedTransactions, isKaryawan, nowMillis) {
        val byStatusOrDate = when (selectedFilter) {
            RiwayatFilterOption.SEMUA -> if (isKaryawan) activeTransactions else transaksiList
            RiwayatFilterOption.HARI_INI -> activeTransactions.filter { isSameDay(it.transaksi.tanggal, nowMillis) }
            RiwayatFilterOption.LUNAS -> activeTransactions.filter { it.transaksi.status.equals("Lunas", ignoreCase = true) }
            RiwayatFilterOption.KASBON -> activeTransactions.filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
            RiwayatFilterOption.DIHAPUS -> if (isKaryawan) emptyList() else deletedTransactions
        }
        if (searchQuery.isBlank()) {
            byStatusOrDate
        } else {
            val q = searchQuery.lowercase().trim()
            val qDigits = q.filter { it.isDigit() }
            byStatusOrDate.filter { detail ->
                val isDeletedTx = KaryawanScopeHelper.isDeletedTransaksi(detail.transaksi)
                val deletedBy = KaryawanScopeHelper.extractDeletedByTransaksi(detail.transaksi)?.lowercase().orEmpty()
                val isKasbonTx = !isDeletedTx && detail.transaksi.status.equals("Kasbon", ignoreCase = true)
                val formattedTotal = FormatUtils.formatRupiah(detail.transaksi.total_harga).lowercase()
                val rawTotalStr = detail.transaksi.total_harga.toLong().toString()
                val formattedDateFull = FormatUtils.formatDate(detail.transaksi.tanggal).lowercase()
                val formattedDateShort = FormatUtils.formatDateShort(detail.transaksi.tanggal).lowercase()
                val galonLabel = "${detail.transaksi.jumlah} galon"
                val kurirLabel = detail.kurir?.nama?.lowercase() ?: "ambil sendiri"
                val metodeLabel = when {
                    isDeletedTx -> "dihapus $deletedBy"
                    isKasbonTx -> "belum dibayar"
                    else -> detail.transaksi.metode_bayar.lowercase()
                }
                val hpDigits = detail.pelanggan?.no_hp?.filter { it.isDigit() } ?: ""

                (detail.pelanggan?.nama?.lowercase()?.contains(q) == true) ||
                    (detail.pelanggan?.alamat?.let { KaryawanScopeHelper.cleanPelangganAlamat(it).lowercase().contains(q) } == true) ||
                    (detail.pelanggan?.no_hp?.lowercase()?.contains(q) == true) ||
                    (qDigits.isNotEmpty() && hpDigits.contains(qDigits)) ||
                    kurirLabel.contains(q) ||
                    (detail.produk?.nama_produk?.lowercase()?.contains(q) == true) ||
                    metodeLabel.contains(q) ||
                    detail.transaksi.status.lowercase().contains(q) ||
                    (isDeletedTx && ("dihapus".contains(q) || deletedBy.contains(q))) ||
                    formattedTotal.contains(q) ||
                    (qDigits.isNotEmpty() && rawTotalStr.contains(qDigits)) ||
                    galonLabel.contains(q) ||
                    detail.transaksi.jumlah.toString() == q ||
                    formattedDateFull.contains(q) ||
                    formattedDateShort.contains(q)
            }
        }
    }

    val activeFiltered = remember(filtered) {
        filtered.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
    }
    val totalGalonFiltered = remember(activeFiltered) {
        activeFiltered.sumOf { it.transaksi.jumlah }
    }
    val totalLunasFiltered = remember(activeFiltered) {
        activeFiltered.sumOf {
            if (it.transaksi.status.equals("Lunas", ignoreCase = true)) {
                it.transaksi.total_harga
            } else {
                it.transaksi.uangMuka.toDouble()
            }
        }
    }
    val totalKasbonFiltered = remember(activeFiltered) {
        activeFiltered.filter { it.transaksi.status.equals("Kasbon", ignoreCase = true) }
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
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
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
                        "Cari pelanggan, kurir, atau nominal...",
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
                            modifier = Modifier.size(48.dp)
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
                val visibleFilters = remember(isKaryawan, countDihapus) {
                    RiwayatFilterOption.entries.filter { option ->
                        if (isKaryawan) {
                            option != RiwayatFilterOption.DIHAPUS
                        } else {
                            option != RiwayatFilterOption.DIHAPUS || countDihapus > 0
                        }
                    }
                }
                visibleFilters.forEach { option ->
                    val isSelected = selectedFilter == option
                    val count = when (option) {
                        RiwayatFilterOption.SEMUA -> activeTransactions.size
                        RiwayatFilterOption.HARI_INI -> countHariIni
                        RiwayatFilterOption.LUNAS -> countLunas
                        RiwayatFilterOption.KASBON -> countKasbon
                        RiwayatFilterOption.DIHAPUS -> countDihapus
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
                                RiwayatFilterOption.KASBON -> MaterialTheme.santoyaColors.warningContainer
                                RiwayatFilterOption.LUNAS -> MaterialTheme.santoyaColors.successContainer
                                RiwayatFilterOption.DIHAPUS -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            selectedLabelColor = when (option) {
                                RiwayatFilterOption.KASBON -> MaterialTheme.santoyaColors.onWarningContainer
                                RiwayatFilterOption.LUNAS -> MaterialTheme.santoyaColors.onSuccessContainer
                                RiwayatFilterOption.DIHAPUS -> MaterialTheme.colorScheme.error
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
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3. Mini Summary Banner (Rekap Hasil Filter / Pencarian Aktif)
            if (filtered.isNotEmpty()) {
                item {
                    val santoyaColors = MaterialTheme.santoyaColors
                    if (selectedFilter == RiwayatFilterOption.DIHAPUS) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filtered.size} Nota Dihapus Karyawan",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Tidak dihitung ke omzet & kasbon",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else {
                        HydroBentoCard(
                            accentColor = MaterialTheme.colorScheme.primary,
                            cornerRadius = 18.dp,
                            tintAlpha = 0.09f,
                            showWaterRipple = true,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        HydroGradientBadge(
                                            gradientColors = listOf(MaterialTheme.colorScheme.primary, AccentCyan),
                                            size = 32.dp,
                                            cornerRadius = 10.dp,
                                            elevation = 2.dp
                                        ) {
                                            HydroGalonIcon(
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${activeFiltered.size} Transaksi • $totalGalonFiltered Galon",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    SantoyaAdaptiveCurrencyText(
                                        amount = totalNominalFiltered,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        minFontSize = 12.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = santoyaColors.successContainer.copy(alpha = 0.75f),
                                        border = BorderStroke(1.dp, santoyaColors.success.copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Lunas",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = santoyaColors.onSuccessContainer
                                            )
                                            SantoyaAdaptiveCurrencyText(
                                                amount = totalLunasFiltered,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = santoyaColors.onSuccessContainer,
                                                minFontSize = 9.sp,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = santoyaColors.warningContainer.copy(alpha = 0.75f),
                                        border = BorderStroke(1.dp, santoyaColors.warning.copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Kasbon",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = santoyaColors.onWarningContainer
                                            )
                                            SantoyaAdaptiveCurrencyText(
                                                amount = totalKasbonFiltered,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = santoyaColors.onWarningContainer,
                                                minFontSize = 9.sp,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isInitialDataLoaded && transaksiList.isEmpty()) {
                item {
                    SantoyaLoadingSkeleton(
                        itemCount = 4,
                        message = "Memuat riwayat transaksi..."
                    )
                }
            } else if (filtered.isEmpty()) {
                item {
                    val hasActiveFilter = searchQuery.isNotBlank() || selectedFilter != RiwayatFilterOption.SEMUA
                    SantoyaEmptyState(
                        illustrationRes = R.drawable.img_empty_transaksi,
                        title = if (searchQuery.isNotBlank()) "Transaksi Tidak Ditemukan" else "Belum Ada Riwayat",
                        subtitle = if (searchQuery.isNotBlank()) {
                            "Nota untuk \"$searchQuery\" tidak ditemukan."
                        } else {
                            "Belum ada transaksi ${selectedFilter.label.lowercase()}."
                        },
                        primaryActionLabel = if (!hasActiveFilter) "Catat Transaksi" else null,
                        primaryActionIcon = if (!hasActiveFilter) Icons.Default.Add else null,
                        onPrimaryAction = if (!hasActiveFilter) {
                            { viewModel.setTransaksiTab(0) }
                        } else null,
                        secondaryActionLabel = if (hasActiveFilter) "Reset Filter" else null,
                        secondaryActionIcon = if (hasActiveFilter) Icons.Default.Clear else null,
                        onSecondaryAction = if (hasActiveFilter) {
                            {
                                searchQuery = ""
                                selectedFilter = RiwayatFilterOption.SEMUA
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        } else null,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
            items(displayedHistory, key = { it.transaksi.id }) { detail ->
                val isDeletedTx = KaryawanScopeHelper.isDeletedTransaksi(detail.transaksi)
                val deletedBy = KaryawanScopeHelper.extractDeletedByTransaksi(detail.transaksi)
                val isKasbon = !isDeletedTx && detail.transaksi.status.equals("Kasbon", ignoreCase = true)
                val santoyaColors = MaterialTheme.santoyaColors
                val cardAccent = when {
                    isDeletedTx -> MaterialTheme.colorScheme.error
                    isKasbon -> santoyaColors.warning
                    else -> santoyaColors.success
                }
                HydroBentoCard(
                    accentColor = cardAccent,
                    cornerRadius = 18.dp,
                    tintAlpha = if (isDeletedTx) 0.10f else 0.06f,
                    showWaterRipple = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(cardAccent)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = FormatUtils.formatDate(detail.transaksi.tanggal),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isDeletedTx) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.widthIn(max = 175.dp)
                                ) {
                                    Text(
                                        text = "DIHAPUS OLEH ${(deletedBy ?: "Karyawan").uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isKasbon) santoyaColors.warningContainer else santoyaColors.successContainer
                                ) {
                                    Text(
                                        text = detail.transaksi.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isKasbon) santoyaColors.onWarningContainer else santoyaColors.onSuccessContainer,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HydroGradientBadge(
                                    gradientColors = when {
                                        isDeletedTx -> listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error.copy(alpha = 0.75f))
                                        isKasbon -> listOf(AccentAmber, santoyaColors.warning)
                                        else -> listOf(MaterialTheme.colorScheme.primary, AccentCyan)
                                    },
                                    size = 38.dp,
                                    cornerRadius = 12.dp,
                                    elevation = 2.dp
                                ) {
                                    HydroGalonIcon(
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
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
                                val resolvedKurirName = detail.kurir?.nama?.takeIf { it.isNotBlank() }
                                    ?: detail.transaksi.id_kurir?.let { kId ->
                                        kurirList.find { it.id == kId }?.nama?.takeIf { it.isNotBlank() }
                                    }
                                Text(
                                    text = "Kurir: ${resolvedKurirName ?: "Tanpa Kurir"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (resolvedKurirName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.widthIn(max = 142.dp)
                            ) {
                                SantoyaAdaptiveCurrencyText(
                                    amount = detail.transaksi.total_harga,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        textDecoration = if (isDeletedTx) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    fontWeight = FontWeight.Black,
                                    color = if (isDeletedTx) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    minFontSize = 11.sp,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = when {
                                        isDeletedTx -> "Tidak dihitung ke omzet"
                                        isKasbon && detail.transaksi.uangMuka > 0L -> {
                                            "DP: ${FormatUtils.formatRupiah(detail.transaksi.uangMuka.toDouble())} • Sisa: ${FormatUtils.formatRupiah(detail.transaksi.remainingKasbonAmount())}"
                                        }
                                        isKasbon -> "Belum Dibayar"
                                        else -> detail.transaksi.metode_bayar.ifBlank { "Cash" }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = when {
                                        isDeletedTx -> MaterialTheme.colorScheme.error
                                        isKasbon -> Color(0xFFB45309)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (isDeletedTx) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.restoreTransaksi(detail.transaksi) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = "Pulihkan",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pulihkan",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(
                                    onClick = { deletingTransaksi = detail },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Hapus Permanen",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Hapus Permanen",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
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
                                                color = Color(0xFF047857),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { editingTransaksi = detail },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(19.dp)
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
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "Cetak",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(19.dp)
                                        )
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
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Bagikan",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { deletingTransaksi = detail },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
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
                                "Tampilkan 30 terbaru saja"
                            } else {
                                "Lihat semua (${filtered.size} transaksi)"
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
                    text = "Tandai Nota Lunas?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tandai sisa kasbon ${target.pelanggan?.nama ?: "Pelanggan Umum"} sebesar ${FormatUtils.formatRupiah(target.transaksi.remainingKasbonAmount())} (${target.transaksi.jumlah} galon) sebagai Lunas?",
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
                        "Tandai Lunas",
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
        val isAuditDeleted = KaryawanScopeHelper.isDeletedTransaksi(target.transaksi)
        val deletedBy = KaryawanScopeHelper.extractDeletedByTransaksi(target.transaksi) ?: "Karyawan"
        AlertDialog(
            onDismissRequest = { deletingTransaksi = null },
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = if (isAuditDeleted) "Hapus Permanen Transaksi?" else "Hapus Transaksi?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = when {
                        isAuditDeleted -> {
                            "Nota ${target.pelanggan?.nama ?: "Pelanggan Umum"} (${FormatUtils.formatRupiah(target.transaksi.total_harga)}) yang dihapus $deletedBy akan dihapus permanen dari HP dan Cloud."
                        }
                        isKaryawan -> {
                            "Nota ${target.pelanggan?.nama ?: "Pelanggan Umum"} (${FormatUtils.formatRupiah(target.transaksi.total_harga)}) akan disembunyikan dari daftar Anda dan dicatat 'Dihapus oleh ${activeKaryawan?.nama ?: "Karyawan"}' untuk laporan Pemilik."
                        }
                        else -> {
                            "Nota ${target.pelanggan?.nama ?: "Pelanggan Umum"} (${FormatUtils.formatRupiah(target.transaksi.total_harga)}) akan dihapus. Sisa kasbon pelanggan ikut diperbarui."
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isAuditDeleted) {
                            viewModel.permanentDeleteTransaksi(target.transaksi)
                        } else {
                            viewModel.deleteTransaksi(target.transaksi)
                        }
                        deletingTransaksi = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isAuditDeleted) "Hapus Permanen" else "Hapus",
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
        val lockedEditKurirId = if (isKaryawan && activeKaryawan != null) activeKaryawan.id else null
        var editKurirId by remember(detail, lockedEditKurirId) {
            mutableStateOf(lockedEditKurirId ?: detail.transaksi.id_kurir)
        }
        var editProdukId by remember(detail) { mutableStateOf(detail.transaksi.id_produk) }

        val parsedJumlah = editJumlah.toIntOrNull() ?: 0
        val parsedHarga = editHarga.toDoubleOrNull() ?: 0.0
        val previewTotalHarga = parsedJumlah * parsedHarga
        val isEditBayarSebagian = editMetode.equals("Bayar Sebagian", ignoreCase = true)
        val parsedEditDp = editUangMuka.toLongOrNull() ?: 0L
        val isValidEdit = parsedJumlah > 0 && parsedHarga > 0.0 &&
            (!isEditBayarSebagian || (parsedEditDp > 0L && parsedEditDp <= previewTotalHarga.toLong()))

        val jumlahFocus = remember { FocusRequester() }
        val hargaFocus = remember { FocusRequester() }

        SantoyaFormBottomSheet(
            title = "Edit Transaksi",
            subtitle = "Pelanggan: ${detail.pelanggan?.nama ?: "Umum"}",
            headerIcon = Icons.Default.Edit,
            onDismiss = { editingTransaksi = null },
            confirmLabel = "Simpan Perubahan",
            confirmEnabled = isValidEdit,
            onConfirm = {
                if (isValidEdit) {
                    val finalStatus = if (isEditBayarSebagian) "Kasbon" else editStatus
                    val finalMetode = when {
                        isEditBayarSebagian -> "Bayar Sebagian"
                        finalStatus.equals("Kasbon", ignoreCase = true) -> "-"
                        else -> editMetode.ifBlank { "Cash" }
                    }
                    focusManager.clearFocus()
                    keyboardController?.hide()
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
            }
        ) {
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
                                editHarga = if (isKaryawan) {
                                    KaryawanScopeHelper.resolveAuthoritativeUnitPrice(
                                        pelanggan = detail.pelanggan,
                                        produk = prod,
                                        fallbackPrice = prod.harga
                                    ).toInt().toString()
                                } else {
                                    prod.harga.toInt().toString()
                                }
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (isKaryawan) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { if (!isKaryawan) hargaFocus.requestFocus() },
                    onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(jumlahFocus)
                    .bringIntoViewOnFocus(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = editHarga,
                onValueChange = { if (!isKaryawan) editHarga = it.filter { c -> c.isDigit() } },
                readOnly = isKaryawan,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isKaryawan) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                ),
                label = {
                    Text(
                        "Harga Satuan (Rp) *",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = if (isKaryawan) {
                    {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Harga dikunci Pemilik",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                supportingText = if (isKaryawan) {
                    {
                        Text(
                            text = "Harga dikunci oleh Pemilik",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isKaryawan) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    unfocusedContainerColor = if (isKaryawan) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                isError = parsedHarga <= 0.0,
                singleLine = true,
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
                    .focusRequester(hargaFocus)
                    .bringIntoViewOnFocus(),
                shape = RoundedCornerShape(12.dp)
            )

            val editSelectedKurir = kurirList.find { it.id == editKurirId }
            val editKomisiBreakdown = KomisiCalculator.calculateTransactionCommission(
                kurir = editSelectedKurir,
                jumlah = parsedJumlah
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
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
                            text = "Total Baru:",
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
                    if (editSelectedKurir != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Komisi ${editSelectedKurir.nama} (${FormatUtils.formatRupiah(editKomisiBreakdown.perGalonRate)}/gal):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = FormatUtils.formatRupiah(editKomisiBreakdown.totalKomisi),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Text(
                "Pembayaran:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val isEditKasbon = editStatus.equals("Kasbon", ignoreCase = true)
            val isEditLunas = editStatus.equals("Lunas", ignoreCase = true)
            val isEditKasbonPenuh = isEditKasbon && !isEditBayarSebagian
            val santoyaColors = MaterialTheme.santoyaColors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isCash = isEditLunas && !isEditBayarSebagian && editMetode.equals("Cash", ignoreCase = true)
                FilterChip(
                    selected = isCash,
                    onClick = {
                        editMetode = "Cash"
                        editStatus = "Lunas"
                        editUangMuka = ""
                    },
                    label = {
                        Text(
                            "Cash (Lunas)",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isCash) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                )
                val isTransfer = isEditLunas && !isEditBayarSebagian && editMetode.equals("Transfer", ignoreCase = true)
                FilterChip(
                    selected = isTransfer,
                    onClick = {
                        editMetode = "Transfer"
                        editStatus = "Lunas"
                        editUangMuka = ""
                    },
                    label = {
                        Text(
                            "Transfer (Lunas)",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isTransfer) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isEditBayarSebagian,
                    onClick = {
                        editMetode = "Bayar Sebagian"
                        editStatus = "Kasbon"
                    },
                    label = {
                        Text(
                            "DP Sebagian",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isEditBayarSebagian) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = santoyaColors.warningContainer,
                        selectedLabelColor = santoyaColors.onWarningContainer
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                )
                FilterChip(
                    selected = isEditKasbonPenuh,
                    onClick = {
                        editStatus = "Kasbon"
                        editMetode = ""
                        editUangMuka = ""
                    },
                    label = {
                        Text(
                            "Kasbon Penuh",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isEditKasbonPenuh) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                )
            }

            if (isEditBayarSebagian) {
                val previewTotalLong = previewTotalHarga.toLong()
                val editDpPresets = remember(previewTotalLong) {
                    if (previewTotalLong <= 1L) emptyList()
                    else listOfNotNull(
                        ((previewTotalLong * 25) / 100).takeIf { it in 1L until previewTotalLong }?.let { "25%" to it },
                        ((previewTotalLong * 50) / 100).takeIf { it in 1L until previewTotalLong }?.let { "50%" to it },
                        10_000L.takeIf { it in 1L until previewTotalLong }?.let { "10rb" to it },
                        20_000L.takeIf { it in 1L until previewTotalLong }?.let { "20rb" to it }
                    ).distinctBy { it.second }
                }
                if (editDpPresets.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        editDpPresets.forEach { (label, presetAmount) ->
                            val isSelected = parsedEditDp == presetAmount
                            FilterChip(
                                selected = isSelected,
                                onClick = { editUangMuka = presetAmount.toString() },
                                label = {
                                    Text(
                                        text = "$label (${FormatUtils.formatRupiah(presetAmount.toDouble())})",
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                val editSisaPreview = (previewTotalLong - parsedEditDp).coerceAtLeast(0L)
                OutlinedTextField(
                    value = editUangMuka,
                    onValueChange = { editUangMuka = it.filter { c -> c.isDigit() } },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    label = { Text("Uang Muka / DP (Rp) *") },
                    isError = parsedEditDp <= 0L || parsedEditDp > previewTotalLong,
                    singleLine = true,
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
                        .bringIntoViewOnFocus(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "Sisa Kasbon: ${FormatUtils.formatRupiah(editSisaPreview.toDouble())}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309)
                )
            }

            Text(
                "Kurir Pengantar:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isKaryawan && activeKaryawan != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activeKaryawan.nama,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Otomatis Akun Anda",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
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
        }
    }
}
