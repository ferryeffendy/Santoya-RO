package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.components.SantoyaFormBottomSheet
import com.example.ui.components.bringIntoViewOnFocus
import com.example.ui.components.rememberIsImeVisible
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
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
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Cari pengeluaran, kategori, atau nominal...",
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
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(48.dp)
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(cat, maxLines = 1) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (searchQuery.isBlank()) {
                                        "Total Pengeluaran ($selectedCategoryFilter)"
                                    } else {
                                        "Hasil Pencarian ($selectedCategoryFilter)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                SantoyaAdaptiveCurrencyText(
                                    amount = totalPengeluaran,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    minFontSize = 13.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "${filteredList.size} Catatan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MoneyOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredList.isNotEmpty()) {
                    item {
                            val categoryBreakdown = remember(filteredList) {
                                filteredList.groupBy { it.kategori }
                                    .mapValues { entry -> entry.value.sumOf { it.nominal } }
                                    .toList()
                                    .sortedByDescending { it.second }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Rincian per Kategori",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    categoryBreakdown.forEach { (cat, nominal) ->
                                        val pct = if (totalPengeluaran > 0) ((nominal / totalPengeluaran) * 100).toInt() else 0
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
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
                    item {
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
                        "bahan baku" -> santoyaColors.info
                        "operasional" -> santoyaColors.warning
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MoneyOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.nama_item,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = kategoriColor.copy(alpha = 0.12f),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(
                                                text = item.kategori,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = kategoriColor,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = FormatUtils.formatDateTime(item.tanggal),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SantoyaAdaptiveCurrencyText(
                                    amount = item.nominal,
                                    customText = "- ${FormatUtils.formatRupiah(item.nominal)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    minFontSize = 10.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.widthIn(max = 124.dp)
                                )
                                IconButton(
                                    onClick = { itemToDelete = item },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                kategoriOptions.forEach { kat ->
                    FilterChip(
                        selected = selectedKategori.equals(kat, ignoreCase = true),
                        onClick = { selectedKategori = kat },
                        label = { Text(kat, maxLines = 1) },
                        modifier = Modifier.height(34.dp),
                        leadingIcon = if (selectedKategori.equals(kat, ignoreCase = true)) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
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
