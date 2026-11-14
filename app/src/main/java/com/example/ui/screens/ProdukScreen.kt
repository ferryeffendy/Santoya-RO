package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.companion.kzpmrw.R
import com.example.data.entity.Produk
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.components.SantoyaFormBottomSheet
import com.example.ui.components.SantoyaSectionBanner
import com.example.ui.components.bringIntoViewOnFocus
import com.example.ui.components.rememberIsImeVisible
import com.example.util.FormatUtils
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdukScreen(
    viewModel: SantoyaViewModel,
    produkList: List<Produk> = emptyList(),
    onBack: () -> Unit = { viewModel.navigateTo(SantoyaScreen.DASHBOARD) },
    modifier: Modifier = Modifier
) {
    val vmList by viewModel.allProduk.collectAsState()
    val displayList = if (produkList.isNotEmpty()) produkList else vmList

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduk by remember { mutableStateOf<Produk?>(null) }
    var produkToDelete by remember { mutableStateOf<Produk?>(null) }

    val isImeVisible = rememberIsImeVisible()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("produk_screen"),
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
                    modifier = Modifier.testTag("fab_add_produk")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Produk"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SantoyaSectionBanner(
                    icon = Icons.Default.Sell,
                    title = "Produk & Harga",
                    subtitle = "Atur produk & harga jual",
                    badgeText = "${displayList.size} Produk",
                    accentColor = MaterialTheme.colorScheme.primary,
                    onAccentColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            if (displayList.isEmpty()) {
                item {
                    SantoyaEmptyState(
                        illustrationRes = R.drawable.img_empty_depot,
                        title = "Belum Ada Produk",
                        subtitle = "Tambahkan daftar galon atau layanan isi ulang agar siap digunakan di kasir.",
                        primaryActionLabel = "Tambah Produk",
                        primaryActionIcon = Icons.Default.Add,
                        onPrimaryAction = { showAddDialog = true },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                items(displayList, key = { it.id }) { produk ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = produk.nama_produk,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    SantoyaAdaptiveCurrencyText(
                                        amount = produk.harga,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        minFontSize = 11.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { editingProduk = produk },
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Produk",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { produkToDelete = produk },
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Produk",
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

    if (showAddDialog || editingProduk != null) {
        val target = editingProduk
        var nama by remember(target) { mutableStateOf(target?.nama_produk ?: "") }
        var harga by remember(target) { mutableStateOf(target?.harga?.toLong()?.toString() ?: "6000") }
        var errorMessage by remember(target) { mutableStateOf<String?>(null) }
        val hargaFocusRequester = remember { FocusRequester() }
        val sheetFocusManager = LocalFocusManager.current

        val submitProduk = {
            val trimmedNama = nama.trim()
            val hVal = harga.toDoubleOrNull() ?: 0.0
            when {
                trimmedNama.isBlank() -> errorMessage = "Nama produk wajib diisi"
                hVal <= 0.0 -> errorMessage = "Harga harus lebih dari 0"
                else -> {
                    if (target == null) {
                        viewModel.addProduk(trimmedNama, hVal)
                        showAddDialog = false
                    } else {
                        viewModel.updateProduk(target.copy(nama_produk = trimmedNama, harga = hVal))
                        editingProduk = null
                    }
                }
            }
        }

        SantoyaFormBottomSheet(
            title = if (target == null) "Tambah Produk" else "Edit Produk",
            subtitle = "Atur nama dan harga jual per galon",
            headerIcon = Icons.Default.WaterDrop,
            confirmLabel = if (target == null) "Simpan" else "Perbarui",
            onDismiss = {
                showAddDialog = false
                editingProduk = null
            },
            onConfirm = { submitProduk() }
        ) {
            OutlinedTextField(
                value = nama,
                onValueChange = {
                    nama = it
                    errorMessage = null
                },
                label = { Text("Nama Produk *") },
                placeholder = { Text("Contoh: Galon RO 19L") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { hargaFocusRequester.requestFocus() }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = harga,
                onValueChange = {
                    harga = it.filter { c -> c.isDigit() }
                    errorMessage = null
                },
                label = { Text("Harga Satuan (Rp) *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        sheetFocusManager.clearFocus()
                        submitProduk()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(hargaFocusRequester)
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

    if (produkToDelete != null) {
        val target = produkToDelete!!
        AlertDialog(
            onDismissRequest = { produkToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Peringatan",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Hapus Produk?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "\"${target.nama_produk}\" akan dihapus dari katalog.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = target.nama_produk,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Harga: ${FormatUtils.formatRupiah(target.harga)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "Catatan transaksi lama tetap aman di laporan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduk(target)
                        produkToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { produkToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
