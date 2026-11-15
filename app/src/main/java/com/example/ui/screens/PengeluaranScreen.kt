package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.companion.kzpmrw.R
import com.example.data.entity.Pengeluaran
import com.example.ui.components.HydroBentoCard
import com.example.ui.components.HydroExpenseFlowIcon
import com.example.ui.components.HydroGradientBadge
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.components.SantoyaEmptyType
import com.example.ui.components.SantoyaFilterPill
import com.example.ui.components.SantoyaFormBottomSheet
import com.example.ui.components.bringIntoViewOnFocus
import com.example.ui.components.rememberIsImeVisible
import com.example.ui.theme.AccentRose
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PengeluaranScreen(
    viewModel: SantoyaViewModel,
    pengeluaranList: List<Pengeluaran> = emptyList(),
    onBack: () -> Unit = { viewModel.navigateTo(SantoyaScreen.DASHBOARD) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val vmList by viewModel.allPengeluaran.collectAsState()
    val displayList = if (pengeluaranList.isNotEmpty()) pengeluaranList else vmList

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }
    var selectedDateFilter by remember { mutableStateOf(PengeluaranDateFilter.SEMUA) }
    var customStartMillis by remember { mutableLongStateOf(FormatUtils.getStartOfMonth()) }
    var customEndMillis by remember { mutableLongStateOf(FormatUtils.getEndOfDay()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Pengeluaran?>(null) }

    val openCustomDateRangePicker = {
        val startCal = Calendar.getInstance().apply { timeInMillis = customStartMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedStartCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val pickedStart = pickedStartCal.timeInMillis
                val endCal = Calendar.getInstance().apply { timeInMillis = customEndMillis }
                DatePickerDialog(
                    context,
                    { _, endYear, endMonth, endDay ->
                        val pickedEndCal = Calendar.getInstance().apply {
                            set(endYear, endMonth, endDay, 23, 59, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        val pickedEnd = pickedEndCal.timeInMillis
                        customStartMillis = minOf(pickedStart, pickedEnd)
                        customEndMillis = maxOf(pickedStart, pickedEnd)
                        selectedDateFilter = PengeluaranDateFilter.RENTANG
                    },
                    endCal.get(Calendar.YEAR),
                    endCal.get(Calendar.MONTH),
                    endCal.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setTitle("Pilih Tanggal Akhir")
                    show()
                }
            },
            startCal.get(Calendar.YEAR),
            startCal.get(Calendar.MONTH),
            startCal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Pilih Tanggal Mulai")
            show()
        }
    }

    val (startTime, endTime) = remember(selectedDateFilter, customStartMillis, customEndMillis) {
        val todayEnd = FormatUtils.getEndOfDay()
        when (selectedDateFilter) {
            PengeluaranDateFilter.SEMUA -> Pair(0L, todayEnd + 86_400_000L)
            PengeluaranDateFilter.HARI_INI -> Pair(FormatUtils.getStartOfDay(), todayEnd)
            PengeluaranDateFilter.MINGGU_INI -> Pair(FormatUtils.getStartOfWeek(), todayEnd)
            PengeluaranDateFilter.BULAN_INI -> Pair(FormatUtils.getStartOfMonth(), todayEnd)
            PengeluaranDateFilter.RENTANG -> Pair(customStartMillis, customEndMillis)
        }
    }

    val periodAndSearchFilteredList = remember(selectedDateFilter, startTime, endTime, searchQuery, displayList) {
        displayList.filter { item ->
            val matchDate = selectedDateFilter == PengeluaranDateFilter.SEMUA || item.tanggal in startTime..endTime
            val q = searchQuery.lowercase().trim()
            val qDigits = q.filter { it.isDigit() }
            val matchSearch = if (q.isBlank()) {
                true
            } else {
                val nominalFormatted = FormatUtils.formatRupiah(item.nominal).lowercase()
                val nominalDigits = item.nominal.toLong().toString()
                val dateFull = FormatUtils.formatDateTime(item.tanggal).lowercase()
                val dateShort = FormatUtils.formatDateShort(item.tanggal).lowercase()
                item.nama_item.lowercase().contains(q) ||
                    item.kategori.lowercase().contains(q) ||
                    nominalFormatted.contains(q) ||
                    (qDigits.isNotEmpty() && nominalDigits.contains(qDigits)) ||
                    dateFull.contains(q) ||
                    dateShort.contains(q)
            }
            matchDate && matchSearch
        }
    }

    val categoryCounts = remember(periodAndSearchFilteredList) {
        val counts = mutableMapOf<String, Int>()
        counts["Semua"] = periodAndSearchFilteredList.size
        periodAndSearchFilteredList.forEach { item ->
            val matchedKey = when (item.kategori.lowercase()) {
                "listrik & air", "bahan baku" -> "Listrik & Air"
                "filter & membran" -> "Filter & Membran"
                "tutup & tisu galon" -> "Tutup & Tisu Galon"
                "bensin & transport" -> "Bensin & Transport"
                "perawatan" -> "Perawatan"
                "operasional" -> "Operasional"
                else -> "Lainnya"
            }
            counts[matchedKey] = (counts[matchedKey] ?: 0) + 1
        }
        counts
    }

    val filteredList = remember(selectedCategoryFilter, periodAndSearchFilteredList) {
        if (selectedCategoryFilter == "Semua") {
            periodAndSearchFilteredList
        } else {
            periodAndSearchFilteredList.filter { item ->
                item.kategori.equals(selectedCategoryFilter, ignoreCase = true) ||
                    (selectedCategoryFilter == "Listrik & Air" && item.kategori.equals("Bahan Baku", ignoreCase = true))
            }
        }
    }

    val totalPengeluaran = filteredList.sumOf { it.nominal }
    val isImeVisible = rememberIsImeVisible()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("pengeluaran_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isImeVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("tambah_pengeluaran_fab")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Catat Pengeluaran"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Cari pengeluaran, kategori, nominal...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Bersihkan pencarian",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_pengeluaran")
                )

                PengeluaranDateFilterRow(
                    selectedFilter = selectedDateFilter,
                    customStartMillis = customStartMillis,
                    customEndMillis = customEndMillis,
                    onSelectFilter = { filter ->
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        if (filter == PengeluaranDateFilter.RENTANG) {
                            openCustomDateRangePicker()
                        } else {
                            selectedDateFilter = filter
                        }
                    },
                    onOpenCustomRangePicker = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        openCustomDateRangePicker()
                    },
                    onResetCustomRange = {
                        selectedDateFilter = PengeluaranDateFilter.SEMUA
                    }
                )

                PengeluaranCategoryTwoRowGrid(
                    selectedCategory = selectedCategoryFilter,
                    categoryCounts = categoryCounts,
                    onSelectCategory = { cat ->
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        selectedCategoryFilter = cat
                    }
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = if (maxWidth >= 600.dp) 2 else 1
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        HydroBentoCard(
                            accentColor = MaterialTheme.colorScheme.error,
                            cornerRadius = 16.dp,
                            tintAlpha = 0.11f,
                            showWaterRipple = true,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    HydroGradientBadge(
                                        gradientColors = listOf(MaterialTheme.colorScheme.error, AccentRose),
                                        size = 36.dp,
                                        cornerRadius = 11.dp,
                                        elevation = 2.dp
                                    ) {
                                        HydroExpenseFlowIcon(
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val periodSummaryLabel = when (selectedDateFilter) {
                                            PengeluaranDateFilter.SEMUA -> "Semua Periode"
                                            PengeluaranDateFilter.HARI_INI -> "Hari Ini"
                                            PengeluaranDateFilter.MINGGU_INI -> "Minggu Ini"
                                            PengeluaranDateFilter.BULAN_INI -> "Bulan Ini"
                                            PengeluaranDateFilter.RENTANG -> {
                                                val s = FormatUtils.formatDateUi(customStartMillis)
                                                val e = FormatUtils.formatDateUi(customEndMillis)
                                                if (s == e) s else "$s - $e"
                                            }
                                        }
                                        Text(
                                            text = if (searchQuery.isBlank()) {
                                                "Total ($selectedCategoryFilter • $periodSummaryLabel)"
                                            } else {
                                                "Hasil Cari ($selectedCategoryFilter • $periodSummaryLabel)"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        SantoyaAdaptiveCurrencyText(
                                            amount = totalPengeluaran,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.error,
                                            minFontSize = 12.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = "${filteredList.size} Catatan",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (filteredList.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val categoryBreakdown = remember(filteredList) {
                                filteredList.groupBy { it.kategori }
                                    .mapValues { entry -> entry.value.sumOf { it.nominal } }
                                    .toList()
                                    .sortedByDescending { it.second }
                            }

                            HydroBentoCard(
                                accentColor = MaterialTheme.colorScheme.primary,
                                cornerRadius = 16.dp,
                                tintAlpha = 0.04f,
                                showWaterRipple = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Rincian per Kategori",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                    categoryBreakdown.forEach { (cat, nominal) ->
                                        val pct = if (totalPengeluaran > 0) ((nominal / totalPengeluaran) * 100).toInt() else 0
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$cat ($pct%)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            SantoyaAdaptiveCurrencyText(
                                                amount = nominal,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error,
                                                minFontSize = 10.sp,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (filteredList.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val isUnfilteredEmpty = searchQuery.isBlank() &&
                                selectedCategoryFilter == "Semua" &&
                                selectedDateFilter == PengeluaranDateFilter.SEMUA
                            SantoyaEmptyState(
                                emptyType = SantoyaEmptyType.LAPORAN,
                                title = if (isUnfilteredEmpty) "Belum Ada Pengeluaran" else "Pengeluaran Tidak Ditemukan",
                                subtitle = if (isUnfilteredEmpty) {
                                    "Catat biaya listrik, tutup galon, atau BBM antar agar laba bersih tercatat akurat."
                                } else {
                                    "Tidak ada catatan pengeluaran untuk periode dan kategori yang dipilih."
                                },
                                primaryActionLabel = if (isUnfilteredEmpty) "Catat Pengeluaran" else null,
                                primaryActionIcon = if (isUnfilteredEmpty) Icons.Outlined.Add else null,
                                onPrimaryAction = if (isUnfilteredEmpty) {
                                    { showAddDialog = true }
                                } else null,
                                secondaryActionLabel = if (!isUnfilteredEmpty) "Reset Filter" else null,
                                secondaryActionIcon = if (!isUnfilteredEmpty) Icons.Outlined.Clear else null,
                                onSecondaryAction = if (!isUnfilteredEmpty) {
                                    {
                                        searchQuery = ""
                                        selectedCategoryFilter = "Semua"
                                        selectedDateFilter = PengeluaranDateFilter.SEMUA
                                        focusManager.clearFocus()
                                    }
                                } else null,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        items(filteredList, key = { it.id }) { item ->
                            val santoyaColors = MaterialTheme.santoyaColors
                            val kategoriColor = when (item.kategori.lowercase()) {
                                "listrik & air", "bahan baku" -> santoyaColors.info
                                "filter & membran" -> MaterialTheme.colorScheme.primary
                                "tutup & tisu galon" -> santoyaColors.success
                                "bensin & transport" -> santoyaColors.warning
                                "perawatan" -> MaterialTheme.colorScheme.tertiary
                                "operasional" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            HydroBentoCard(
                                accentColor = kategoriColor,
                                cornerRadius = 16.dp,
                                tintAlpha = 0.05f,
                                showWaterRipple = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(11.dp),
                                                color = kategoriColor.copy(alpha = 0.14f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    HydroExpenseFlowIcon(
                                                        tint = kategoriColor,
                                                        leafAccent = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.nama_item,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            SantoyaAdaptiveCurrencyText(
                                                amount = item.nominal,
                                                customText = "- ${FormatUtils.formatRupiah(item.nominal)}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.error,
                                                minFontSize = 11.sp,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.wrapContentWidth()
                                            )
                                            IconButton(
                                                onClick = { itemToDelete = item },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Hapus",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = kategoriColor.copy(alpha = 0.14f),
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        ) {
                                            Text(
                                                text = item.kategori,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = kategoriColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Text(
                                            text = FormatUtils.formatDateTime(item.tanggal),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var namaItem by remember { mutableStateOf("") }
        var selectedKategori by remember { mutableStateOf("Operasional") }
        var nominalStr by remember { mutableStateOf("") }
        var tanggalMillis by remember { mutableStateOf(System.currentTimeMillis()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val kategoriOptions = listOf("Listrik & Air", "Filter & Membran", "Tutup & Tisu Galon", "Bensin & Transport", "Perawatan", "Operasional", "Lainnya")
        val nominalFocusRequester = remember { FocusRequester() }
        val sheetFocusManager = LocalFocusManager.current

        val submitExpense = {
            val trimmed = namaItem.trim()
            val nominal = nominalStr.toDoubleOrNull()
            when {
                trimmed.isBlank() -> errorMessage = "Nama pengeluaran wajib diisi"
                nominal == null || nominal <= 0 -> errorMessage = "Nominal tidak valid"
                else -> {
                    viewModel.addPengeluaran(trimmed, selectedKategori, nominal, tanggalMillis)
                    showAddDialog = false
                }
            }
        }

        SantoyaFormBottomSheet(
            title = "Catat Pengeluaran",
            subtitle = "Biaya operasional depot & antar",
            headerAccent = MaterialTheme.colorScheme.error,
            headerIconContent = {
                HydroExpenseFlowIcon(
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            },
            confirmLabel = "Simpan",
            confirmContainerColor = MaterialTheme.colorScheme.primary,
            onDismiss = { showAddDialog = false },
            onConfirm = { submitExpense() }
        ) {
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = tanggalMillis }
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val newCal = Calendar.getInstance().apply { set(y, m, d) }
                            tanggalMillis = newCal.timeInMillis
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(FormatUtils.formatDate(tanggalMillis))
            }

            OutlinedTextField(
                value = namaItem,
                onValueChange = {
                    namaItem = it
                    errorMessage = null
                },
                label = { Text("Nama Pengeluaran *") },
                placeholder = { Text("Contoh: Tutup galon, Listrik") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { nominalFocusRequester.requestFocus() }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                text = "Kategori",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                kategoriOptions.forEach { kat ->
                    val isSelected = selectedKategori.equals(kat, ignoreCase = true)
                    SantoyaFilterPill(
                        label = kat,
                        selected = isSelected,
                        onClick = { selectedKategori = kat },
                        leadingIcon = if (isSelected) Icons.Outlined.Check else null
                    )
                }
            }

            OutlinedTextField(
                value = nominalStr,
                onValueChange = {
                    nominalStr = it.filter { char -> char.isDigit() }
                    errorMessage = null
                },
                label = { Text("Nominal (Rp) *") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        sheetFocusManager.clearFocus()
                        submitExpense()
                    }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nominalFocusRequester)
                    .bringIntoViewOnFocus(),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Hapus Pengeluaran?",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Text(
                    text = "Hapus catatan \"${item.nama_item}\" (${FormatUtils.formatRupiah(item.nominal)})?",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePengeluaran(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

private enum class PengeluaranDateFilter(val label: String) {
    SEMUA("Semua"),
    HARI_INI("Hari Ini"),
    MINGGU_INI("Minggu Ini"),
    BULAN_INI("Bulan Ini"),
    RENTANG("Rentang")
}

/**
 * Full-width single-row quick period filter pills ("Semua", "Hari Ini", "Minggu Ini",
 * "Bulan Ini", and "Rentang" date picker) with an active custom date range banner when selected.
 */
@Composable
private fun PengeluaranDateFilterRow(
    selectedFilter: PengeluaranDateFilter,
    customStartMillis: Long,
    customEndMillis: Long,
    onSelectFilter: (PengeluaranDateFilter) -> Unit,
    onOpenCustomRangePicker: () -> Unit,
    onResetCustomRange: () -> Unit
) {
    val density = LocalDensity.current
    val fontScale = density.fontScale

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth
            val gap = when {
                availableWidth < 340.dp -> 3.dp
                availableWidth < 400.dp -> 5.dp
                else -> 6.dp
            }
            val innerHorizontal = when {
                availableWidth < 340.dp -> 4.dp
                availableWidth < 400.dp -> 6.dp
                else -> 8.dp
            }
            val baseFontSize = when {
                availableWidth < 335.dp -> 9.5.sp
                availableWidth < 370.dp -> 10.5.sp
                availableWidth < 400.dp -> 11.sp
                else -> 12.sp
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PengeluaranDateFilter.entries.forEach { option ->
                    val isSelected = selectedFilter == option
                    val itemWeight = remember(option) {
                        when (option) {
                            PengeluaranDateFilter.SEMUA -> 0.88f
                            PengeluaranDateFilter.HARI_INI -> 0.96f
                            PengeluaranDateFilter.MINGGU_INI -> 1.14f
                            PengeluaranDateFilter.BULAN_INI -> 1.04f
                            PengeluaranDateFilter.RENTANG -> 1.15f
                        }
                    }

                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        animationSpec = tween(160),
                        label = "exp_date_bg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(160),
                        label = "exp_date_fg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                        },
                        animationSpec = tween(160),
                        label = "exp_date_border"
                    )

                    val initialFont = remember(baseFontSize, fontScale) {
                        if (fontScale > 1.15f) {
                            (baseFontSize.value / (fontScale / 1.1f)).coerceAtLeast(8f).sp
                        } else {
                            baseFontSize
                        }
                    }
                    var labelFontSize by remember(option, availableWidth, initialFont) {
                        mutableStateOf(initialFont)
                    }

                    val shape = RoundedCornerShape(50)
                    Surface(
                        shape = shape,
                        color = containerColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .weight(itemWeight)
                            .heightIn(min = 34.dp)
                            .clip(shape)
                            .semantics {
                                this.selected = isSelected
                                this.role = Role.RadioButton
                            }
                            .clickable { onSelectFilter(option) }
                            .testTag("filter_date_${option.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = innerHorizontal, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (option == PengeluaranDateFilter.RENTANG) {
                                Icon(
                                    imageVector = Icons.Outlined.DateRange,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor,
                                    modifier = Modifier.size(if (availableWidth < 360.dp) 12.dp else 13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = labelFontSize,
                                    lineHeight = labelFontSize
                                ),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                onTextLayout = { result ->
                                    if (result.hasVisualOverflow && labelFontSize.value > 8f) {
                                        labelFontSize = (labelFontSize.value - 0.5f).coerceAtLeast(8f).sp
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = selectedFilter == PengeluaranDateFilter.RENTANG) {
            val startStr = FormatUtils.formatDateUi(customStartMillis)
            val endStr = FormatUtils.formatDateUi(customEndMillis)
            val rangeLabel = if (startStr == endStr) startStr else "$startStr – $endStr"

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenCustomRangePicker() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rentang: $rangeLabel (Ketuk untuk ubah)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onResetCustomRange() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Reset tanggal",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Balanced 2-row full-width category pill grid (Row 1: 4 items, Row 2: 3 items) so every
 * category fits edge-to-edge without any dangling 3rd row, complete with semantic color dot
 * and note count badge.
 */
@Composable
private fun PengeluaranCategoryTwoRowGrid(
    selectedCategory: String,
    categoryCounts: Map<String, Int>,
    onSelectCategory: (String) -> Unit
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val density = LocalDensity.current
    val fontScale = density.fontScale

    val row1Categories = remember {
        listOf("Semua", "Listrik & Air", "Filter & Membran", "Operasional")
    }
    val row2Categories = remember {
        listOf("Tutup & Tisu Galon", "Bensin & Transport", "Perawatan", "Lainnya")
    }

    @Composable
    fun resolveCategoryAccent(category: String): Color {
        return when (category.lowercase()) {
            "semua" -> MaterialTheme.colorScheme.primary
            "listrik & air" -> santoyaColors.info
            "filter & membran" -> MaterialTheme.colorScheme.primary
            "tutup & tisu galon" -> santoyaColors.success
            "bensin & transport" -> santoyaColors.warning
            "perawatan" -> MaterialTheme.colorScheme.tertiary
            "operasional" -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val gap = when {
            availableWidth < 340.dp -> 4.dp
            availableWidth < 400.dp -> 5.dp
            else -> 6.dp
        }
        val innerHorizontal = when {
            availableWidth < 340.dp -> 4.dp
            availableWidth < 400.dp -> 6.dp
            else -> 8.dp
        }
        val baseLabelFont = when {
            availableWidth < 335.dp -> 9.5.sp
            availableWidth < 370.dp -> 10.2.sp
            availableWidth < 400.dp -> 11.sp
            else -> 11.8.sp
        }
        val baseBadgeFont = when {
            availableWidth < 335.dp -> 8.sp
            availableWidth < 370.dp -> 8.5.sp
            availableWidth < 400.dp -> 9.sp
            else -> 9.5.sp
        }
        val dotSize = if (availableWidth < 360.dp) 5.5.dp else 6.5.dp

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            listOf(row1Categories, row2Categories).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowItems.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val count = categoryCounts[cat] ?: 0
                        val accentColor = resolveCategoryAccent(cat)
                        val itemWeight = remember(cat, count) {
                            val badgeExtra = if (count > 0) (count.toString().length * 0.14f) + 0.18f else 0f
                            ((cat.length * 0.12f) + 0.35f + badgeExtra).coerceAtLeast(0.75f)
                        }

                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (cat == "Semua") {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    accentColor.copy(alpha = 0.16f)
                                }
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            animationSpec = tween(160),
                            label = "cat_pill_bg"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (cat == "Semua") {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    accentColor
                                }
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(160),
                            label = "cat_pill_fg"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                accentColor.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                            },
                            animationSpec = tween(160),
                            label = "cat_pill_border"
                        )

                        val initialFont = remember(baseLabelFont, fontScale) {
                            if (fontScale > 1.15f) {
                                (baseLabelFont.value / (fontScale / 1.1f)).coerceAtLeast(8f).sp
                            } else {
                                baseLabelFont
                            }
                        }
                        var labelFontSize by remember(cat, count, availableWidth, initialFont) {
                            mutableStateOf(initialFont)
                        }
                        val effectiveBadgeFont: TextUnit = remember(labelFontSize, baseLabelFont, baseBadgeFont) {
                            val ratio = (labelFontSize.value / baseLabelFont.value).coerceIn(0.75f, 1f)
                            (baseBadgeFont.value * ratio).coerceAtLeast(7.5f).sp
                        }

                        val shape = RoundedCornerShape(10.dp)
                        Surface(
                            shape = shape,
                            color = containerColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .weight(itemWeight)
                                .heightIn(min = 34.dp)
                                .clip(shape)
                                .semantics {
                                    this.selected = isSelected
                                    this.role = Role.RadioButton
                                }
                                .clickable { onSelectCategory(cat) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = innerHorizontal, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(dotSize)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) accentColor else accentColor.copy(alpha = 0.7f)
                                        )
                                )
                                Spacer(modifier = Modifier.width(if (availableWidth < 360.dp) 3.5.dp else 4.5.dp))
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = labelFontSize,
                                        lineHeight = labelFontSize
                                    ),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    onTextLayout = { result ->
                                        if (result.hasVisualOverflow && labelFontSize.value > 7.8f) {
                                            labelFontSize = (labelFontSize.value - 0.4f).coerceAtLeast(7.8f).sp
                                        }
                                    }
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(if (availableWidth < 360.dp) 3.dp else 4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isSelected) {
                                            accentColor.copy(alpha = 0.22f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                                        }
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = effectiveBadgeFont,
                                                lineHeight = effectiveBadgeFont
                                            ),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

