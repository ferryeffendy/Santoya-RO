package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import com.example.data.entity.Kurir
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.entity.Pelanggan
import com.example.data.entity.TransaksiDetail
import com.example.util.CrmOrderPrediction
import com.example.util.CrmOrderStatus
import com.example.util.FormatUtils
import com.example.util.SantoyaReceiptHelper
import com.example.viewmodel.ActiveReceiptDialog
import com.example.viewmodel.KasbonSortOption
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PelangganScreen(
    viewModel: SantoyaViewModel,
    pelangganUiModels: List<com.example.viewmodel.PelangganUiModel> = emptyList(),
    transaksiList: List<TransaksiDetail> = emptyList(),
    onBack: () -> Unit = { viewModel.navigateTo(SantoyaScreen.DASHBOARD) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kurirList by viewModel.allKurir.collectAsState()
    val vmPelangganList by viewModel.allPelanggan.collectAsState()
    val vmTransaksiList by viewModel.allTransaksi.collectAsState()

    val effectiveTransaksiList = if (transaksiList.isNotEmpty()) transaksiList else vmTransaksiList
    val effectivePelangganUiModels = remember(pelangganUiModels, vmPelangganList, effectiveTransaksiList) {
        if (pelangganUiModels.isNotEmpty()) {
            viewModel.getPelangganUiModels(
                pelangganList = pelangganUiModels.map { it.pelanggan },
                transaksiList = effectiveTransaksiList
            )
        } else {
            viewModel.getPelangganUiModels(
                pelangganList = vmPelangganList,
                transaksiList = effectiveTransaksiList
            )
        }
    }

    val selectedTab by viewModel.pelangganTab.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPelanggan by remember { mutableStateOf<Pelanggan?>(null) }
    var payingKasbonPelanggan by remember { mutableStateOf<Pelanggan?>(null) }
    var pelangganToDelete by remember { mutableStateOf<Pelanggan?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            val jadwalList = remember(effectivePelangganUiModels) {
                effectivePelangganUiModels
                    .filter {
                        it.crmPrediction.status == CrmOrderStatus.OVERDUE ||
                            it.crmPrediction.status == CrmOrderStatus.DUE_SOON
                    }
                    .sortedBy { it.crmPrediction.daysUntilExpected ?: Int.MAX_VALUE }
            }

            val kasbonCount = remember(effectivePelangganUiModels) {
                effectivePelangganUiModels.count { it.pelanggan.sisa_kasbon > 0 }
            }

            val tabItems = listOf(
                Pair("Semua", null),
                Pair("Kasbon", if (kasbonCount > 0) kasbonCount else null),
                Pair("Ranking", null),
                Pair("Pasif", null),
                Pair("Jadwal", if (jadwalList.isNotEmpty()) jadwalList.size else null)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabItems.forEachIndexed { idx, (label, badgeCount) ->
                    val isSelected = selectedTab == idx
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setPelangganTab(idx) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (badgeCount != null) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (idx == 1) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    }
                                ) {
                                    Text(
                                        text = badgeCount.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (idx == 1) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
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

            when (selectedTab) {
                0 -> SemuaPelangganTab(
                    viewModel = viewModel,
                    pelangganUiModels = effectivePelangganUiModels,
                    transaksiList = effectiveTransaksiList,
                    kurirList = kurirList,
                    onEdit = { editingPelanggan = it },
                    onBayarKasbon = { payingKasbonPelanggan = it },
                    onDelete = { pelangganToDelete = it }
                )
                1 -> {
                    val pelangganList = remember(effectivePelangganUiModels) {
                        effectivePelangganUiModels.map { it.pelanggan }
                    }
                    KasbonPelangganTab(
                        viewModel = viewModel,
                        pelangganList = pelangganList,
                        transaksiList = effectiveTransaksiList,
                        onBayarKasbon = { payingKasbonPelanggan = it }
                    )
                }
                2 -> {
                    val pelangganList = remember(effectivePelangganUiModels) {
                        effectivePelangganUiModels.map { it.pelanggan }
                    }
                    TopRankingTab(
                        viewModel = viewModel,
                        pelangganList = pelangganList,
                        transaksiList = effectiveTransaksiList
                    )
                }
                3 -> {
                    val pelangganList = remember(effectivePelangganUiModels) {
                        effectivePelangganUiModels.map { it.pelanggan }
                    }
                    PasifPelangganTab(
                        viewModel = viewModel,
                        pelangganList = pelangganList,
                        transaksiList = effectiveTransaksiList
                    )
                }
                4 -> JadwalPelangganTab(
                    viewModel = viewModel,
                    jadwalUiModels = jadwalList,
                    transaksiList = effectiveTransaksiList,
                    kurirList = kurirList,
                    onEdit = { editingPelanggan = it },
                    onBayarKasbon = { payingKasbonPelanggan = it },
                    onDelete = { pelangganToDelete = it }
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("tambah_pelanggan_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Pelanggan")
        }
    }

    if (showAddDialog) {
        PelangganFormDialog(
            title = "Tambah Pelanggan Baru",
            pelanggan = null,
            kurirList = kurirList,
            onDismiss = { showAddDialog = false },
            onConfirm = { nama, alamat, noHp, hargaKhusus, idKurirLangganan ->
                viewModel.addPelanggan(
                    nama = nama,
                    alamat = alamat,
                    noHp = noHp,
                    hargaKhusus = hargaKhusus,
                    idKurirLangganan = idKurirLangganan
                )
                showAddDialog = false
            }
        )
    }

    if (editingPelanggan != null) {
        val target = editingPelanggan!!
        PelangganFormDialog(
            title = "Edit Data Pelanggan",
            pelanggan = target,
            kurirList = kurirList,
            onDismiss = { editingPelanggan = null },
            onConfirm = { nama, alamat, noHp, hargaKhusus, idKurirLangganan ->
                viewModel.updatePelanggan(
                    target.copy(
                        nama = nama,
                        alamat = alamat,
                        no_hp = noHp,
                        harga_khusus = hargaKhusus,
                        id_kurir_langganan = idKurirLangganan
                    )
                )
                editingPelanggan = null
            }
        )
    }

    if (payingKasbonPelanggan != null) {
        val target = payingKasbonPelanggan!!
        val unpaidTargetList = remember(effectiveTransaksiList, target.id) {
            effectiveTransaksiList.filter {
                it.transaksi.id_pelanggan == target.id &&
                    it.transaksi.status.equals("Kasbon", ignoreCase = true)
            }
        }
        PelunasanKasbonDialog(
            pelanggan = target,
            unpaidList = unpaidTargetList,
            onDismiss = { payingKasbonPelanggan = null },
            onConfirmBayar = { nominal, metodeBayar ->
                viewModel.bayarKasbon(target.id, nominal, metodeBayar)
                val sisa = (target.sisa_kasbon - nominal).coerceAtLeast(0.0)
                val receipt = SantoyaReceiptHelper.generatePelunasanReceipt(target, nominal, sisa)
                viewModel.showReceiptDialog(
                    ActiveReceiptDialog(
                        title = "Bukti Pelunasan Kasbon",
                        receiptText = receipt,
                        customerPhone = target.no_hp,
                        data = Triple(target, nominal, sisa)
                    )
                )
                payingKasbonPelanggan = null
            }
        )
    }

    if (pelangganToDelete != null) {
        val target = pelangganToDelete!!
        AlertDialog(
            onDismissRequest = { pelangganToDelete = null },
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
                    text = "Konfirmasi Hapus Pelanggan",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus data pelanggan ini? Tindakan ini tidak dapat dibatalkan.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = target.nama,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (target.no_hp.isNotBlank()) {
                                Text(
                                    text = "No. HP: ${target.no_hp}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (target.alamat.isNotBlank()) {
                                Text(
                                    text = "Alamat: ${target.alamat}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (target.sisa_kasbon > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PERHATIAN: Pelanggan ini masih memiliki sisa kasbon sebesar ${FormatUtils.formatRupiah(target.sisa_kasbon)}! Pastikan sisa kasbon diselesaikan sebelum menghapus.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Catatan: Riwayat penjualan sebelumnya yang terkait dengan pelanggan ini tetap aman tercatat dalam laporan pembukuan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePelanggan(target)
                        pelangganToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ya, Hapus Pelanggan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pelangganToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SemuaPelangganTab(
    viewModel: SantoyaViewModel,
    pelangganUiModels: List<com.example.viewmodel.PelangganUiModel>,
    transaksiList: List<TransaksiDetail>,
    kurirList: List<Kurir> = emptyList(),
    onEdit: (Pelanggan) -> Unit,
    onBayarKasbon: (Pelanggan) -> Unit,
    onDelete: (Pelanggan) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, pelangganUiModels) {
        viewModel.getFilteredPelangganUiModels(searchQuery, pelangganUiModels)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Cari nama, alamat, atau no HP...",
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
                        modifier = Modifier.size(36.dp)
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
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("input_search_pelanggan")
        )

        if (filtered.isEmpty()) {
            EmptyListState(
                icon = Icons.Default.People,
                title = if (searchQuery.isBlank()) "Belum ada pelanggan" else "Pelanggan tidak ditemukan",
                subtitle = if (searchQuery.isBlank()) {
                    "Tekan tombol + untuk menambahkan pelanggan baru."
                } else {
                    "Tidak ada pelanggan dengan kata kunci \"${searchQuery.trim()}\"."
                },
                onResetSearch = if (searchQuery.isNotBlank()) {
                    {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }
                } else null
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.pelanggan.id }) { item ->
                    PelangganItemCard(
                        pelangganUiModel = item,
                        kurirList = kurirList,
                        onEdit = { onEdit(item.pelanggan) },
                        onDelete = { onDelete(item.pelanggan) },
                        onBayarKasbon = { onBayarKasbon(item.pelanggan) },
                        onCall = {
                            if (item.pelanggan.no_hp.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.pelanggan.no_hp}"))
                                context.startActivity(intent)
                            }
                        },
                        onPrintKasbon = if (item.pelanggan.sisa_kasbon > 0) {
                            {
                                val unpaid = transaksiList.filter {
                                    it.transaksi.id_pelanggan == item.pelanggan.id &&
                                        it.transaksi.status.equals("Kasbon", ignoreCase = true)
                                }.map { it.transaksi }
                                val receipt = SantoyaReceiptHelper.generateTagihanKasbon(item.pelanggan, unpaid)
                                viewModel.showReceiptDialog(
                                    ActiveReceiptDialog(
                                        title = "Tagihan Kasbon - ${item.pelanggan.nama}",
                                        receiptText = receipt,
                                        customerPhone = item.pelanggan.no_hp,
                                        data = Pair(item.pelanggan, unpaid)
                                    )
                                )
                            }
                        } else null,
                        onShareKasbon = if (item.pelanggan.sisa_kasbon > 0) {
                            {
                                val unpaid = transaksiList.filter {
                                    it.transaksi.id_pelanggan == item.pelanggan.id &&
                                        it.transaksi.status.equals("Kasbon", ignoreCase = true)
                                }.map { it.transaksi }
                                val receipt = SantoyaReceiptHelper.generateTagihanKasbon(item.pelanggan, unpaid)
                                SantoyaReceiptHelper.shareReceiptText(context, receipt, item.pelanggan.no_hp)
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun JadwalPelangganTab(
    viewModel: SantoyaViewModel,
    jadwalUiModels: List<com.example.viewmodel.PelangganUiModel>,
    transaksiList: List<TransaksiDetail>,
    kurirList: List<Kurir> = emptyList(),
    onEdit: (Pelanggan) -> Unit,
    onBayarKasbon: (Pelanggan) -> Unit,
    onDelete: (Pelanggan) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, jadwalUiModels) {
        viewModel.getFilteredPelangganUiModels(searchQuery, jadwalUiModels)
    }

    if (jadwalUiModels.isEmpty()) {
        EmptyListState(
            icon = Icons.Outlined.Schedule,
            title = "Belum ada pelanggan yang jatuh tempo",
            subtitle = "Pelanggan yang waktunya order atau terlambat akan muncul secara otomatis di sini."
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Cari nama, alamat, atau no HP jatuh tempo...",
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
                            modifier = Modifier.size(36.dp)
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
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("input_search_jadwal_pelanggan")
            )

            if (filtered.isEmpty()) {
                EmptyListState(
                    icon = Icons.Outlined.Schedule,
                    title = "Pelanggan tidak ditemukan",
                    subtitle = "Tidak ada pelanggan jatuh tempo dengan kata kunci \"${searchQuery.trim()}\".",
                    onResetSearch = {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.pelanggan.id }) { item ->
                        PelangganItemCard(
                            pelangganUiModel = item,
                            kurirList = kurirList,
                            onEdit = { onEdit(item.pelanggan) },
                            onDelete = { onDelete(item.pelanggan) },
                            onBayarKasbon = { onBayarKasbon(item.pelanggan) },
                            onCall = {
                                if (item.pelanggan.no_hp.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.pelanggan.no_hp}"))
                                    context.startActivity(intent)
                                }
                            },
                            onPrintKasbon = if (item.pelanggan.sisa_kasbon > 0) {
                                {
                                    val unpaid = transaksiList.filter {
                                        it.transaksi.id_pelanggan == item.pelanggan.id &&
                                            it.transaksi.status.equals("Kasbon", ignoreCase = true)
                                    }.map { it.transaksi }
                                    val receipt = SantoyaReceiptHelper.generateTagihanKasbon(item.pelanggan, unpaid)
                                    viewModel.showReceiptDialog(
                                        ActiveReceiptDialog(
                                            title = "Tagihan Kasbon - ${item.pelanggan.nama}",
                                            receiptText = receipt,
                                            customerPhone = item.pelanggan.no_hp,
                                            data = Pair(item.pelanggan, unpaid)
                                        )
                                    )
                                }
                            } else null,
                            onShareKasbon = if (item.pelanggan.sisa_kasbon > 0) {
                                {
                                    val unpaid = transaksiList.filter {
                                        it.transaksi.id_pelanggan == item.pelanggan.id &&
                                            it.transaksi.status.equals("Kasbon", ignoreCase = true)
                                    }.map { it.transaksi }
                                    val receipt = SantoyaReceiptHelper.generateTagihanKasbon(item.pelanggan, unpaid)
                                    SantoyaReceiptHelper.shareReceiptText(context, receipt, item.pelanggan.no_hp)
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PelangganItemCard(
    pelangganUiModel: com.example.viewmodel.PelangganUiModel,
    kurirList: List<Kurir> = emptyList(),
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBayarKasbon: () -> Unit,
    onCall: () -> Unit,
    onPrintKasbon: (() -> Unit)? = null,
    onShareKasbon: (() -> Unit)? = null
) {
    val pelanggan = pelangganUiModel.pelanggan
    val hasKasbon = pelanggan.sisa_kasbon > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = pelanggan.nama.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pelanggan.nama,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (pelanggan.no_hp.isNotBlank()) pelanggan.no_hp else "Tanpa No. HP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pelanggan.no_hp.isNotBlank()) {
                        IconButton(onClick = onCall, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Telepon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (pelanggan.alamat.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Alamat: ${pelanggan.alamat}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            CrmOrderPredictionBadge(
                prediction = pelangganUiModel.crmPrediction,
                pelanggan = pelanggan
            )

            if (pelanggan.harga_khusus != null || pelanggan.id_kurir_langganan != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (pelanggan.harga_khusus != null && pelanggan.harga_khusus > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Harga Khusus: ${FormatUtils.formatRupiah(pelanggan.harga_khusus)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pelanggan.id_kurir_langganan != null) {
                        val kName = kurirList.find { it.id == pelanggan.id_kurir_langganan }?.nama ?: "Kurir #${pelanggan.id_kurir_langganan}"
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Kurir: $kName",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = if (hasKasbon) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasKasbon) "Sisa Kasbon:" else "Status Tagihan:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (hasKasbon) FormatUtils.formatRupiah(pelanggan.sisa_kasbon) else "Lunas / Rp 0",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasKasbon) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    )
                }
            }

            if (hasKasbon) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onPrintKasbon != null || onShareKasbon != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (onPrintKasbon != null) {
                                IconButton(
                                    onClick = onPrintKasbon,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = "Cetak Tagihan",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (onShareKasbon != null) {
                                IconButton(
                                    onClick = onShareKasbon,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Bagi WA",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = onBayarKasbon,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Form Pelunasan Kasbon",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrmOrderPredictionBadge(
    prediction: CrmOrderPrediction,
    pelanggan: Pelanggan
) {
    val context = LocalContext.current
    val hasValidPhone = pelanggan.no_hp.isNotBlank()
    val isActionable = hasValidPhone &&
        (prediction.status == CrmOrderStatus.DUE_SOON || prediction.status == CrmOrderStatus.OVERDUE)

    val containerColor = when (prediction.status) {
        CrmOrderStatus.ACTIVE -> Color(0xFF10B981).copy(alpha = 0.16f)
        CrmOrderStatus.DUE_SOON -> Color(0xFFF59E0B).copy(alpha = 0.18f)
        CrmOrderStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer
        CrmOrderStatus.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val contentColor = when (prediction.status) {
        CrmOrderStatus.ACTIVE -> Color(0xFF047857)
        CrmOrderStatus.DUE_SOON -> Color(0xFFB45309)
        CrmOrderStatus.OVERDUE -> MaterialTheme.colorScheme.error
        CrmOrderStatus.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.outline
    }

    val borderColor = when (prediction.status) {
        CrmOrderStatus.ACTIVE -> Color(0xFF10B981).copy(alpha = 0.45f)
        CrmOrderStatus.DUE_SOON -> Color(0xFFF59E0B).copy(alpha = 0.65f)
        CrmOrderStatus.OVERDUE -> MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
        CrmOrderStatus.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.outlineVariant
    }

    val icon = when (prediction.status) {
        CrmOrderStatus.DUE_SOON, CrmOrderStatus.OVERDUE -> Icons.Outlined.NotificationsActive
        CrmOrderStatus.ACTIVE, CrmOrderStatus.INSUFFICIENT_DATA -> Icons.Outlined.Schedule
    }

    val badgeShape = RoundedCornerShape(8.dp)
    val clickModifier = if (isActionable) {
        Modifier.clickable {
            val cleanPhone = pelanggan.no_hp.replace("[^0-9]".toRegex(), "")
            val formattedPhone = if (cleanPhone.startsWith("0")) {
                "62" + cleanPhone.substring(1)
            } else {
                cleanPhone
            }
            val followUpMessage = "Halo Kak ${pelanggan.nama}, dari Santoya RO ingin menanyakan apakah stok galon air minumnya sudah perlu diisi ulang hari ini? Terima kasih!"
            try {
                val waUri = Uri.parse(
                    "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(followUpMessage)}"
                )
                val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                context.startActivity(waIntent)
            } catch (_: Exception) {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pelanggan.no_hp}"))
                context.startActivity(dialIntent)
            }
        }
    } else {
        Modifier
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = badgeShape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(badgeShape)
                .then(clickModifier)
                .testTag("crm_badge_${pelanggan.id}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = prediction.badgeText,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = prediction.badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isActionable) {
                    Text(
                        text = "• Hubungi",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        prediction.averageIntervalDays?.let { avgDays ->
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Siklus ±$avgDays hari",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun KasbonPelangganTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>,
    onBayarKasbon: (Pelanggan) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var sortOption by remember { mutableStateOf(KasbonSortOption.TERBANYAK) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedCustomerIds by remember { mutableStateOf(setOf<Int>()) }

    val sortedKasbon = remember(pelangganList, sortOption, searchQuery, transaksiList) {
        viewModel.getSortedKasbonPelanggan(pelangganList, sortOption, searchQuery, transaksiList)
    }
    val totalPiutang = remember(sortedKasbon) {
        sortedKasbon.sumOf { it.sisa_kasbon }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Cari nama, alamat, no HP, atau nominal...",
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
                        modifier = Modifier.size(36.dp)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("input_search_kasbon"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Urutkan:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            KasbonSortOption.values().forEach { option ->
                FilterChip(
                    selected = sortOption == option,
                    onClick = { sortOption = option },
                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }, maxLines = 1) },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        if (sortedKasbon.isEmpty()) {
            EmptyListState(
                icon = Icons.Default.HourglassBottom,
                title = if (searchQuery.isBlank()) "Tidak ada tagihan kasbon" else "Pelanggan tidak ditemukan",
                subtitle = if (searchQuery.isBlank()) {
                    "Semua kasbon telah dilunasi dengan baik."
                } else {
                    "Tidak ada data kasbon dengan kata kunci \"${searchQuery.trim()}\"."
                },
                onResetSearch = if (searchQuery.isNotBlank()) {
                    {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }
                } else null
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Total Piutang Kasbon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text(
                                        text = FormatUtils.formatRupiah(totalPiutang),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${sortedKasbon.size} Pelanggan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                items(sortedKasbon, key = { it.id }) { cust ->
                    val unpaid = remember(transaksiList, cust.id) {
                        transaksiList.filter {
                            it.transaksi.id_pelanggan == cust.id &&
                                it.transaksi.status.equals("Kasbon", ignoreCase = true)
                        }.sortedByDescending { it.transaksi.tanggal }
                    }
                    val isExpanded = expandedCustomerIds.contains(cust.id)
                    val displayedUnpaid = if (isExpanded) unpaid else unpaid.take(3)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cust.nama,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = cust.alamat.ifBlank { "Alamat belum diisi" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = FormatUtils.formatRupiah(cust.sisa_kasbon),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            if (unpaid.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "Rincian ${unpaid.size} Transaksi Kasbon:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        displayedUnpaid.forEach { t ->
                                            val noteSisaKasbon = t.transaksi.remainingKasbonAmount()
                                            val dpPaid = t.transaksi.uangMuka
                                            val isPartiallyPaid = dpPaid > 0L || (t.transaksi.total_harga > noteSisaKasbon && t.transaksi.jumlah > 0)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = if (dpPaid > 0L) {
                                                        "• ${FormatUtils.formatDateShort(t.transaksi.tanggal)} (${t.transaksi.jumlah} gln • DP ${FormatUtils.formatRupiah(dpPaid.toDouble())})"
                                                    } else if (isPartiallyPaid) {
                                                        "• ${FormatUtils.formatDateShort(t.transaksi.tanggal)} (${t.transaksi.jumlah} gln • Sisa Cicilan)"
                                                    } else {
                                                        "• ${FormatUtils.formatDateShort(t.transaksi.tanggal)} (${t.transaksi.jumlah} galon)"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = FormatUtils.formatRupiah(noteSisaKasbon),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        if (unpaid.size > 3) {
                                            Text(
                                                text = if (isExpanded) {
                                                    "▲ Sembunyikan rincian nota"
                                                } else {
                                                    "▼ Lihat semua ${unpaid.size} nota kasbon (+${unpaid.size - 3} lainnya)"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .clickable {
                                                        expandedCustomerIds = if (isExpanded) {
                                                            expandedCustomerIds - cust.id
                                                        } else {
                                                            expandedCustomerIds + cust.id
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val receipt = SantoyaReceiptHelper.generateTagihanKasbon(cust, unpaid.map { it.transaksi })
                                            viewModel.showReceiptDialog(
                                                ActiveReceiptDialog(
                                                    title = "Tagihan Kasbon - ${cust.nama}",
                                                    receiptText = receipt,
                                                    customerPhone = cust.no_hp,
                                                    data = Pair(cust, unpaid.map { it.transaksi })
                                                )
                                            )
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "Cetak Tagihan",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val receipt = SantoyaReceiptHelper.generateTagihanKasbon(cust, unpaid.map { it.transaksi })
                                            SantoyaReceiptHelper.shareReceiptText(context, receipt, cust.no_hp)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Bagi WA",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onBayarKasbon(cust) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pelunasan Kasbon", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopRankingTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    val ranking = remember(pelangganList, transaksiList) {
        viewModel.getTopRankingPelanggan(pelangganList, transaksiList)
    }
    val customerSpendStats = remember(transaksiList) {
        transaksiList
            .filter { it.transaksi.jumlah > 0 }
            .groupBy { it.transaksi.id_pelanggan }
            .mapValues { (_, list) ->
                Pair(list.size, list.sumOf { it.transaksi.total_harga })
            }
    }
    val filteredRanking = remember(ranking, searchQuery) {
        if (searchQuery.isBlank()) {
            ranking.mapIndexed { idx, pair -> Triple(idx + 1, pair.first, pair.second) }
        } else {
            val q = searchQuery.lowercase().trim()
            val qDigits = q.filter { it.isDigit() }
            ranking.mapIndexedNotNull { idx, pair ->
                val cust = pair.first
                val hpDigits = cust.no_hp.filter { it.isDigit() }
                val matchPhone = cust.no_hp.lowercase().contains(q) ||
                    (qDigits.isNotEmpty() && hpDigits.contains(qDigits))
                val matchGalon = "${pair.second} galon".contains(q) || pair.second.toString() == q
                if (cust.nama.lowercase().contains(q) ||
                    cust.alamat.lowercase().contains(q) ||
                    matchPhone ||
                    matchGalon
                ) {
                    Triple(idx + 1, cust, pair.second)
                } else null
            }
        }
    }

    if (ranking.isEmpty()) {
        EmptyListState(
            icon = Icons.Default.EmojiEvents,
            title = "Belum Ada Ranking Penjualan",
            subtitle = "Ranking akan terhitung otomatis saat transaksi penjualan disimpan."
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Cari nama, alamat, atau no HP di ranking...",
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
                            modifier = Modifier.size(36.dp)
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
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("input_search_ranking")
            )

            if (filteredRanking.isEmpty()) {
                EmptyListState(
                    icon = Icons.Default.EmojiEvents,
                    title = "Pelanggan tidak ditemukan",
                    subtitle = "Tidak ada pelanggan di ranking dengan kata kunci \"${searchQuery.trim()}\".",
                    onResetSearch = {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRanking, key = { it.second.id }) { triple ->
                        val rank = triple.first
                        val cust = triple.second
                        val galonCount = triple.third
                        val stats = customerSpendStats[cust.id] ?: Pair(0, 0.0)

                        val badgeBgColor = when (rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = badgeBgColor,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "#$rank",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rank <= 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cust.nama,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = cust.alamat.ifBlank { "Pelanggan Santoya" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${stats.first}x Order • ${FormatUtils.formatRupiah(stats.second)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "$galonCount Galon",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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

@Composable
private fun PasifPelangganTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedDaysFilter by remember { mutableStateOf(7L) }
    var searchQuery by remember { mutableStateOf("") }
    val pasifList = remember(pelangganList, transaksiList, selectedDaysFilter) {
        viewModel.getPasifPelanggan(pelangganList, transaksiList, selectedDaysFilter)
    }
    val lastOrderDateMap = remember(transaksiList) {
        transaksiList
            .groupBy { it.transaksi.id_pelanggan }
            .mapValues { (_, list) -> list.maxOfOrNull { it.transaksi.tanggal } }
    }
    val filteredPasif = remember(pasifList, searchQuery) {
        if (searchQuery.isBlank()) pasifList
        else {
            val q = searchQuery.lowercase().trim()
            val qDigits = q.filter { it.isDigit() }
            pasifList.filter { (cust, _) ->
                val hpDigits = cust.no_hp.filter { it.isDigit() }
                val matchPhone = cust.no_hp.lowercase().contains(q) ||
                    (qDigits.isNotEmpty() && hpDigits.contains(qDigits))
                cust.nama.lowercase().contains(q) ||
                    cust.alamat.lowercase().contains(q) ||
                    matchPhone
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Cari nama, alamat, atau no HP pasif...",
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
                        modifier = Modifier.size(36.dp)
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
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("input_search_pasif")
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Rentang:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                listOf(7L to "> 7 Hari", 14L to "> 14 Hari", 30L to "> 30 Hari", 60L to "> 60 Hari").forEach { pair ->
                    FilterChip(
                        selected = selectedDaysFilter == pair.first,
                        onClick = { selectedDaysFilter = pair.first },
                        label = { Text(pair.second, maxLines = 1) },
                        modifier = Modifier.height(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }
        }

        if (filteredPasif.isEmpty()) {
            EmptyListState(
                icon = Icons.Default.PersonOff,
                title = if (searchQuery.isBlank()) "Semua Pelanggan Aktif" else "Pelanggan tidak ditemukan",
                subtitle = if (searchQuery.isBlank()) {
                    "Tidak ada pelanggan pasif dalam rentang waktu $selectedDaysFilter hari terakhir."
                } else {
                    "Tidak ada pelanggan pasif dengan kata kunci \"${searchQuery.trim()}\"."
                },
                onResetSearch = if (searchQuery.isNotBlank()) {
                    {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }
                } else null
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPasif, key = { it.first.id }) { pair ->
                    val cust = pair.first
                    val days = pair.second
                    val lastOrderMillis = lastOrderDateMap[cust.id]

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PersonOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cust.nama,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (cust.alamat.isNotBlank()) {
                                        Text(
                                            text = cust.alamat,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = if (days >= 999L || lastOrderMillis == null) {
                                            "Belum pernah memesan"
                                        } else {
                                            "$days hari belum memesan (Terakhir: ${FormatUtils.formatDateShort(lastOrderMillis)})"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (cust.no_hp.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val cleanPhone = cust.no_hp.replace("[^0-9]".toRegex(), "")
                                        val formattedPhone = if (cleanPhone.startsWith("0")) {
                                            "62" + cleanPhone.substring(1)
                                        } else {
                                            cleanPhone
                                        }
                                        val followUpMessage = "Halo Kak ${cust.nama}, kami dari Santoya RO. Apakah persediaan galon air minum RO Kakak sudah habis dan ingin kami antar hari ini? Terima kasih!"
                                        try {
                                            val waUri = Uri.parse(
                                                "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(followUpMessage)}"
                                            )
                                            val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                                            context.startActivity(waIntent)
                                        } catch (_: Exception) {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cust.no_hp}"))
                                            context.startActivity(dialIntent)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hubungi", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PelangganFormDialog(
    title: String,
    pelanggan: Pelanggan?,
    kurirList: List<Kurir> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (nama: String, alamat: String, noHp: String, hargaKhusus: Double?, idKurirLangganan: Int?) -> Unit
) {
    var nama by remember { mutableStateOf(pelanggan?.nama ?: "") }
    var alamat by remember { mutableStateOf(pelanggan?.alamat ?: "") }
    var noHp by remember { mutableStateOf(pelanggan?.no_hp ?: "") }
    var hargaKhususStr by remember { mutableStateOf(pelanggan?.harga_khusus?.toLong()?.toString() ?: "") }
    var selectedKurirId by remember { mutableStateOf<Int?>(pelanggan?.id_kurir_langganan) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
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
                    value = nama,
                    onValueChange = {
                        nama = it
                        errorMessage = null
                    },
                    label = { Text("Nama Lengkap *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text("Alamat / Desa / Blok") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = noHp,
                    onValueChange = { noHp = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("No. HP / WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "Otomatisasi Transaksi (Opsional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                OutlinedTextField(
                    value = hargaKhususStr,
                    onValueChange = { hargaKhususStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Harga Khusus Galon (Rp)") },
                    placeholder = { Text("Kosongkan jika harga umum") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "Kurir Langganan",
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
                        selected = selectedKurirId == null,
                        onClick = { selectedKurirId = null },
                        label = { Text("Tanpa Kurir", maxLines = 1) },
                        modifier = Modifier.height(32.dp)
                    )
                    kurirList.forEach { kurir ->
                        FilterChip(
                            selected = selectedKurirId == kurir.id,
                            onClick = { selectedKurirId = kurir.id },
                            label = { Text(kurir.nama, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isBlank()) {
                        errorMessage = "Nama pelanggan wajib diisi!"
                        return@Button
                    }
                    val hKhusus = hargaKhususStr.toDoubleOrNull()
                    onConfirm(nama.trim(), alamat.trim(), noHp.trim(), hKhusus, selectedKurirId)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun PelunasanKasbonDialog(
    pelanggan: Pelanggan,
    unpaidList: List<TransaksiDetail> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmBayar: (nominal: Double, metodeBayar: String) -> Unit
) {
    var nominalStr by remember { mutableStateOf(pelanggan.sisa_kasbon.toLong().toString()) }
    var selectedMetode by remember { mutableStateOf("Cash") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val inputNominal = nominalStr.toDoubleOrNull() ?: 0.0
    val sisaSetelahBayar = (pelanggan.sisa_kasbon - inputNominal).coerceAtLeast(0.0)
    val isLunasPenuh = inputNominal >= pelanggan.sisa_kasbon && pelanggan.sisa_kasbon > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pelunasan Kasbon", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pelanggan: ${pelanggan.nama}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Total Kasbon Saat Ini: ${FormatUtils.formatRupiah(pelanggan.sisa_kasbon)}",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (unpaidList.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Daftar Nota Belum Lunas (${unpaidList.size} Nota):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            unpaidList.take(4).forEach { t ->
                                val noteSisaKasbon = t.transaksi.remainingKasbonAmount()
                                val dpPaid = t.transaksi.uangMuka
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (dpPaid > 0L) {
                                            "• ${FormatUtils.formatDateShort(t.transaksi.tanggal)} (${t.transaksi.jumlah} gln • DP ${FormatUtils.formatRupiah(dpPaid.toDouble())})"
                                        } else {
                                            "• ${FormatUtils.formatDateShort(t.transaksi.tanggal)} (${t.transaksi.jumlah} gln)"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = FormatUtils.formatRupiah(noteSisaKasbon),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (unpaidList.size > 4) {
                                Text(
                                    text = "+ ${unpaidList.size - 4} nota lainnya",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = nominalStr,
                    onValueChange = {
                        nominalStr = it.filter { c -> c.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Nominal Bayar (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { nominalStr = pelanggan.sisa_kasbon.toLong().toString() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lunas Penuh", maxLines = 1)
                    }
                    if (pelanggan.sisa_kasbon > 10000) {
                        OutlinedButton(
                            onClick = { nominalStr = (pelanggan.sisa_kasbon / 2).toLong().toString() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bayar 50%", maxLines = 1)
                        }
                    }
                }

                Text(
                    text = "Metode Pelunasan",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cash", "Transfer").forEach { metode ->
                        FilterChip(
                            selected = selectedMetode == metode,
                            onClick = { selectedMetode = metode },
                            label = { Text(metode, maxLines = 1) },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLunasPenuh) Color(0xFF16A34A).copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sisa Tagihan Setelah Bayar:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isLunasPenuh) "LUNAS (Rp 0)" else FormatUtils.formatRupiah(sisaSetelahBayar),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLunasPenuh) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nominal = nominalStr.toDoubleOrNull()
                    if (nominal == null || nominal <= 0) {
                        errorMessage = "Nominal pembayaran tidak valid!"
                        return@Button
                    }
                    if (nominal > pelanggan.sisa_kasbon) {
                        errorMessage = "Nominal melebihi total kasbon!"
                        return@Button
                    }
                    onConfirmBayar(nominal, selectedMetode)
                }
            ) {
                Text("Konfirmasi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun EmptyListState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onResetSearch: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (onResetSearch != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onResetSearch,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Pencarian")
                }
            }
        }
    }
}
