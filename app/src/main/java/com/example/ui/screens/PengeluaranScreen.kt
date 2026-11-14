package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.unit.sp
import com.aistudio.companion.kzpmrw.R
import com.example.data.entity.Pengeluaran
import com.example.ui.components.HydroBentoCard
import com.example.ui.components.HydroExpenseFlowIcon
import com.example.ui.components.HydroGradientBadge
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaEmptyState
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
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Pengeluaran?>(null) }

    val categories = listOf("Semua", "Listrik & Air", "Filter & Membran", "Tutup & Tisu Galon", "Bensin & Transport", "Operasional", "Lainnya")

    val filteredList = remember(selectedCategoryFilter, searchQuery, displayList) {
        displayList.filter { item ->
            val matchCat = selectedCategoryFilter == "Semua" ||
                item.kategori.equals(selectedCategoryFilter, ignoreCase = true)
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
            matchCat && matchSearch
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
                        imageVector = Icons.Default.Add,
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
                            imageVector = Icons.Default.Search,
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
                                    imageVector = Icons.Default.Clear,
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

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        SantoyaFilterPill(
                            label = cat,
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = cat },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
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
                                        Text(
                                            text = if (searchQuery.isBlank()) {
                                                "Total Pengeluaran ($selectedCategoryFilter)"
                                            } else {
                                                "Hasil Pencarian ($selectedCategoryFilter)"
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
                            val isUnfilteredEmpty = searchQuery.isBlank() && selectedCategoryFilter == "Semua"
                            SantoyaEmptyState(
                                illustrationRes = R.drawable.img_empty_laporan,
                                title = if (isUnfilteredEmpty) "Belum Ada Pengeluaran" else "Pengeluaran Tidak Ditemukan",
                                subtitle = if (isUnfilteredEmpty) {
                                    "Catat biaya listrik, tutup galon, atau BBM antar agar laba bersih tercatat akurat."
                                } else {
                                    "Pengeluaran tidak ditemukan untuk filter ini."
                                },
                                primaryActionLabel = if (isUnfilteredEmpty) "Catat Pengeluaran" else null,
                                primaryActionIcon = if (isUnfilteredEmpty) Icons.Default.Add else null,
                                onPrimaryAction = if (isUnfilteredEmpty) {
                                    { showAddDialog = true }
                                } else null,
                                secondaryActionLabel = if (!isUnfilteredEmpty) "Reset Filter" else null,
                                secondaryActionIcon = if (!isUnfilteredEmpty) Icons.Default.Clear else null,
                                onSecondaryAction = if (!isUnfilteredEmpty) {
                                    {
                                        searchQuery = ""
                                        selectedCategoryFilter = "Semua"
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
                                                    Icon(
                                                        imageVector = Icons.Default.MoneyOff,
                                                        contentDescription = null,
                                                        tint = kategoriColor,
                                                        modifier = Modifier.size(18.dp)
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
                                                    imageVector = Icons.Default.Delete,
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
        val kategoriOptions = listOf("Listrik & Air", "Filter & Membran", "Tutup & Tisu Galon", "Bensin & Transport", "Operasional", "Lainnya")
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
            headerIcon = Icons.Default.MoneyOff,
            headerAccent = MaterialTheme.colorScheme.error,
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
                    imageVector = Icons.Default.CalendarToday,
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
                        leadingIcon = if (isSelected) Icons.Default.Check else null
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
