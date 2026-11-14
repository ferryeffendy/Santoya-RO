package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.data.entity.Kurir
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restore
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
import com.example.data.entity.Pelanggan
import com.example.data.entity.TransaksiDetail
import com.example.ui.components.HydroBentoCard
import com.example.ui.components.HydroCustomerDropIcon
import com.example.ui.components.HydroExpenseFlowIcon
import com.example.ui.components.HydroGalonIcon
import com.example.ui.components.HydroGradientBadge
import com.example.ui.components.HydroReceiptNoteIcon
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.components.SantoyaFormBottomSheet
import com.example.ui.components.SantoyaLoadingSkeleton
import com.example.ui.components.bringIntoViewOnFocus
import com.example.ui.components.rememberIsImeVisible
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.santoyaColors
import com.example.util.CrmOrderPrediction
import com.example.util.CrmOrderStatus
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.SantoyaReceiptHelper
import com.example.viewmodel.ActiveReceiptDialog
import com.example.viewmodel.KasbonSortOption
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PelangganScreen(
    viewModel: SantoyaViewModel,
    pelangganUiModels: List<com.example.viewmodel.PelangganUiModel>? = null,
    transaksiList: List<TransaksiDetail>? = null,
    isKaryawan: Boolean? = null,
    activeKaryawan: Kurir? = null,
    onBack: () -> Unit = { viewModel.navigateTo(SantoyaScreen.DASHBOARD) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kurirList by viewModel.allKurir.collectAsState()
    val vmPelangganUiModels by viewModel.scopedPelangganUiModels.collectAsState()
    val vmTransaksiList by viewModel.scopedPelangganTransaksi.collectAsState()
    val vmIsKaryawan by viewModel.isKaryawan.collectAsState()
    val vmActiveKaryawan by viewModel.activeKaryawan.collectAsState()

    val effectiveIsKaryawan = isKaryawan ?: vmIsKaryawan
    val effectiveActiveKaryawan = activeKaryawan ?: vmActiveKaryawan
    val effectiveTransaksiList = transaksiList ?: vmTransaksiList
    val effectivePelangganUiModels = pelangganUiModels ?: vmPelangganUiModels

    val selectedTab by viewModel.pelangganTab.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = selectedTab.coerceIn(0, 4), pageCount = { 5 })

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (selectedTab != pagerState.currentPage) {
            viewModel.setPelangganTab(pagerState.currentPage)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingPelanggan by remember { mutableStateOf<Pelanggan?>(null) }
    var payingKasbonPelanggan by remember { mutableStateOf<Pelanggan?>(null) }
    var pelangganToDelete by remember { mutableStateOf<Pelanggan?>(null) }
    val isImeVisible = rememberIsImeVisible()

    val latestTxByCustomer = remember(effectiveTransaksiList) {
        effectiveTransaksiList
            .filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) && it.transaksi.id_pelanggan != null }
            .groupBy { it.transaksi.id_pelanggan!! }
            .mapValues { (_, list) -> list.maxByOrNull { it.transaksi.tanggal }?.transaksi }
    }
    val triggerQuickOrder: (Pelanggan) -> Unit = { cust ->
        val lastTx = latestTxByCustomer[cust.id]
        viewModel.startQuickOrder(
            pelangganId = cust.id,
            produkId = lastTx?.id_produk,
            jumlah = lastTx?.jumlah ?: 1
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val activePelangganUiModels = remember(effectivePelangganUiModels) {
                effectivePelangganUiModels.filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it.pelanggan) }
            }
            val jadwalList = remember(activePelangganUiModels) {
                activePelangganUiModels
                    .filter {
                        it.crmPrediction.status == CrmOrderStatus.OVERDUE ||
                            it.crmPrediction.status == CrmOrderStatus.DUE_SOON
                    }
                    .sortedBy { it.crmPrediction.daysUntilExpected ?: Int.MAX_VALUE }
            }

            val kasbonCount = remember(activePelangganUiModels) {
                activePelangganUiModels.count { it.pelanggan.sisa_kasbon > 0 }
            }

            val tabItems = listOf(
                Pair("Semua", null),
                Pair("Kasbon", if (kasbonCount > 0) kasbonCount else null),
                Pair("Jadwal", if (jadwalList.isNotEmpty()) jadwalList.size else null),
                Pair("Pasif", null),
                Pair("Ranking", null)
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompactTabs = maxWidth < 380.dp
                val tabsContent: @Composable () -> Unit = {
                    tabItems.forEachIndexed { idx, (label, badgeCount) ->
                        val isSelected = pagerState.currentPage == idx
                        Tab(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(idx)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isCompactTabs) 6.dp else 2.dp,
                                    vertical = 12.dp
                                ),
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

                if (isCompactTabs) {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 12.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        tabs = tabsContent
                    )
                } else {
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        tabs = tabsContent
                    )
                }
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
                    when (page) {
                        0 -> SemuaPelangganTab(
                            viewModel = viewModel,
                            pelangganUiModels = effectivePelangganUiModels,
                            transaksiList = effectiveTransaksiList,
                            kurirList = kurirList,
                            isKaryawan = effectiveIsKaryawan,
                            onEdit = { editingPelanggan = it },
                            onBayarKasbon = { payingKasbonPelanggan = it },
                            onDelete = { pelangganToDelete = it },
                            onRestore = { viewModel.restorePelanggan(it) },
                            onQuickOrder = triggerQuickOrder
                        )
                        1 -> {
                            val pelangganList = remember(activePelangganUiModels) {
                                activePelangganUiModels.map { it.pelanggan }
                            }
                            KasbonPelangganTab(
                                viewModel = viewModel,
                                pelangganList = pelangganList,
                                transaksiList = effectiveTransaksiList,
                                onBayarKasbon = { payingKasbonPelanggan = it }
                            )
                        }
                        2 -> JadwalPelangganTab(
                            viewModel = viewModel,
                            jadwalUiModels = jadwalList,
                            transaksiList = effectiveTransaksiList,
                            kurirList = kurirList,
                            onEdit = { editingPelanggan = it },
                            onBayarKasbon = { payingKasbonPelanggan = it },
                            onDelete = { pelangganToDelete = it },
                            onQuickOrder = triggerQuickOrder
                        )
                        3 -> {
                            val pelangganList = remember(activePelangganUiModels) {
                                activePelangganUiModels.map { it.pelanggan }
                            }
                            PasifPelangganTab(
                                viewModel = viewModel,
                                pelangganList = pelangganList,
                                transaksiList = effectiveTransaksiList,
                                onQuickOrder = triggerQuickOrder
                            )
                        }
                        4 -> {
                            val pelangganList = remember(activePelangganUiModels) {
                                activePelangganUiModels.map { it.pelanggan }
                            }
                            TopRankingTab(
                                viewModel = viewModel,
                                pelangganList = pelangganList,
                                transaksiList = effectiveTransaksiList
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isImeVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("tambah_pelanggan_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Pelanggan")
            }
        }
    }

    if (showAddDialog) {
        PelangganFormDialog(
            title = "Tambah Pelanggan",
            pelanggan = null,
            kurirList = kurirList,
            isKaryawan = effectiveIsKaryawan,
            activeKaryawan = effectiveActiveKaryawan,
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
            title = "Edit Pelanggan",
            pelanggan = target,
            kurirList = kurirList,
            isKaryawan = effectiveIsKaryawan,
            activeKaryawan = effectiveActiveKaryawan,
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
            effectiveTransaksiList
                .filter {
                    it.transaksi.id_pelanggan == target.id &&
                        it.transaksi.status.equals("Kasbon", ignoreCase = true)
                }
                .sortedWith(compareBy<TransaksiDetail> { it.transaksi.tanggal }.thenBy { it.transaksi.id })
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
        val isAuditDeleted = KaryawanScopeHelper.isSoftDeletedPelanggan(target)
        val deletedBy = KaryawanScopeHelper.extractDeletedByPelanggan(target) ?: "Karyawan"
        val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(target.alamat)
        val hasBlockingKasbon = effectiveIsKaryawan && !isAuditDeleted && target.sisa_kasbon > 0.0
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
                    text = if (isAuditDeleted) "Hapus Permanen Pelanggan?" else "Hapus Pelanggan?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = when {
                            isAuditDeleted -> {
                                "Data ini dihapus oleh $deletedBy. Hapus permanen agar hilang dari HP dan Cloud?"
                            }
                            effectiveIsKaryawan -> {
                                "Pelanggan dihapus dari daftar Anda dan dicatat 'Dihapus oleh ${effectiveActiveKaryawan?.nama ?: "Karyawan"}' untuk laporan Pemilik."
                            }
                            else -> {
                                "Data pelanggan akan dihapus permanen."
                            }
                        },
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
                            if (cleanAlamat.isNotBlank()) {
                                Text(
                                    text = "Alamat: $cleanAlamat",
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
                                    text = if (effectiveIsKaryawan) {
                                        "Masih ada kasbon ${FormatUtils.formatRupiah(target.sisa_kasbon)}. Lunasi dulu sebelum menghapus."
                                    } else {
                                        "Pelanggan ini masih punya kasbon ${FormatUtils.formatRupiah(target.sisa_kasbon)}. Tetap ingin menghapus?"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else if (!isAuditDeleted) {
                        Text(
                            text = "Catatan transaksi lama tetap aman di laporan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isAuditDeleted) {
                            viewModel.permanentDeletePelanggan(target)
                        } else {
                            viewModel.deletePelanggan(target)
                        }
                        pelangganToDelete = null
                    },
                    enabled = !hasBlockingKasbon,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = if (isAuditDeleted) Icons.Default.DeleteForever else Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isAuditDeleted) "Hapus Permanen" else "Hapus")
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
    isKaryawan: Boolean = false,
    onEdit: (Pelanggan) -> Unit,
    onBayarKasbon: (Pelanggan) -> Unit,
    onDelete: (Pelanggan) -> Unit,
    onRestore: (Pelanggan) -> Unit = {},
    onQuickOrder: (Pelanggan) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(0) } // 0 = Semua, 1 = Aktif, 2 = Dihapus

    val activeModels = remember(pelangganUiModels) {
        pelangganUiModels.filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it.pelanggan) }
    }
    val deletedModels = remember(pelangganUiModels) {
        pelangganUiModels.filter { KaryawanScopeHelper.isSoftDeletedPelanggan(it.pelanggan) }
    }

    LaunchedEffect(isKaryawan, deletedModels.size) {
        if (filterMode == 2 && (isKaryawan || deletedModels.isEmpty())) {
            filterMode = 0
        }
    }

    val baseModels = remember(pelangganUiModels, activeModels, deletedModels, filterMode, isKaryawan) {
        if (isKaryawan) {
            activeModels
        } else {
            when (filterMode) {
                1 -> activeModels
                2 -> deletedModels
                else -> pelangganUiModels
            }
        }
    }

    val isInitialDataLoaded by viewModel.isInitialDataLoaded.collectAsState()
    val filtered = remember(searchQuery, baseModels) {
        viewModel.getFilteredPelangganUiModels(searchQuery, baseModels)
    }
    val unpaidTxByCustomer = remember(transaksiList) {
        transaksiList
            .asSequence()
            .filter {
                !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) &&
                    it.transaksi.status.equals("Kasbon", ignoreCase = true) &&
                    it.transaksi.id_pelanggan != null
            }
            .groupBy({ it.transaksi.id_pelanggan!! }, { it.transaksi })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Cari nama, alamat, atau No. HP...",
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("input_search_pelanggan")
        )

        if (!isKaryawan && deletedModels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterMode == 0,
                    onClick = { filterMode = 0 },
                    label = { Text("Semua (${pelangganUiModels.size})", maxLines = 1) },
                    modifier = Modifier.height(32.dp)
                )
                FilterChip(
                    selected = filterMode == 1,
                    onClick = { filterMode = 1 },
                    label = { Text("Aktif (${activeModels.size})", maxLines = 1) },
                    modifier = Modifier.height(32.dp)
                )
                FilterChip(
                    selected = filterMode == 2,
                    onClick = { filterMode = 2 },
                    label = { Text("Dihapus (${deletedModels.size})", maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        if (!isInitialDataLoaded && pelangganUiModels.isEmpty()) {
            SantoyaLoadingSkeleton(
                itemCount = 4,
                message = "Memuat daftar pelanggan..."
            )
        } else if (filtered.isEmpty()) {
            EmptyListState(
                icon = Icons.Default.People,
                title = if (searchQuery.isBlank()) "Belum ada pelanggan" else "Pelanggan tidak ditemukan",
                subtitle = if (searchQuery.isBlank()) {
                    "Ketuk tombol + untuk tambah pelanggan baru."
                } else {
                    "Data untuk \"${searchQuery.trim()}\" tidak ditemukan."
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.pelanggan.id }) { item ->
                    val isSoftDeleted = KaryawanScopeHelper.isSoftDeletedPelanggan(item.pelanggan)
                    val deletedBy = KaryawanScopeHelper.extractDeletedByPelanggan(item.pelanggan)
                    PelangganItemCard(
                        pelangganUiModel = item,
                        kurirList = kurirList,
                        isSoftDeleted = isSoftDeleted,
                        deletedBy = deletedBy,
                        onEdit = { onEdit(item.pelanggan) },
                        onDelete = { onDelete(item.pelanggan) },
                        onRestore = { onRestore(item.pelanggan) },
                        onBayarKasbon = { onBayarKasbon(item.pelanggan) },
                        onQuickOrder = if (!isSoftDeleted) {
                            { onQuickOrder(item.pelanggan) }
                        } else null,
                        onCall = {
                            if (item.pelanggan.no_hp.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.pelanggan.no_hp}"))
                                context.startActivity(intent)
                            }
                        },
                        onPrintKasbon = if (!isSoftDeleted && item.pelanggan.sisa_kasbon > 0) {
                            {
                                val unpaid = unpaidTxByCustomer[item.pelanggan.id] ?: emptyList()
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
                        onShareKasbon = if (!isSoftDeleted && item.pelanggan.sisa_kasbon > 0) {
                            {
                                val unpaid = unpaidTxByCustomer[item.pelanggan.id] ?: emptyList()
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
    onDelete: (Pelanggan) -> Unit,
    onQuickOrder: (Pelanggan) -> Unit = {}
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
            title = "Tidak ada jadwal pengingat",
            subtitle = "Daftar pelanggan yang waktunya isi ulang akan muncul di sini."
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Cari pelanggan yang jatuh tempo...",
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("input_search_jadwal_pelanggan")
            )

            if (filtered.isEmpty()) {
                EmptyListState(
                    icon = Icons.Outlined.Schedule,
                    title = "Pelanggan tidak ditemukan",
                    subtitle = "Data untuk \"${searchQuery.trim()}\" tidak ditemukan.",
                    onResetSearch = {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.pelanggan.id }) { item ->
                        PelangganItemCard(
                            pelangganUiModel = item,
                            kurirList = kurirList,
                            onEdit = { onEdit(item.pelanggan) },
                            onDelete = { onDelete(item.pelanggan) },
                            onBayarKasbon = { onBayarKasbon(item.pelanggan) },
                            onQuickOrder = { onQuickOrder(item.pelanggan) },
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
    isSoftDeleted: Boolean = false,
    deletedBy: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)? = null,
    onBayarKasbon: () -> Unit,
    onQuickOrder: (() -> Unit)? = null,
    onCall: () -> Unit,
    onPrintKasbon: (() -> Unit)? = null,
    onShareKasbon: (() -> Unit)? = null
) {
    val pelanggan = pelangganUiModel.pelanggan
    val hasKasbon = pelanggan.sisa_kasbon > 0
    val cleanAlamat = remember(pelanggan.alamat) {
        KaryawanScopeHelper.cleanPelangganAlamat(pelanggan.alamat)
    }

    val santoyaColors = MaterialTheme.santoyaColors
    val cardAccent = when {
        isSoftDeleted -> MaterialTheme.colorScheme.error
        hasKasbon -> santoyaColors.warning
        else -> MaterialTheme.colorScheme.primary
    }

    HydroBentoCard(
        accentColor = cardAccent,
        cornerRadius = 18.dp,
        tintAlpha = if (isSoftDeleted) 0.12f else 0.06f,
        showWaterRipple = !isSoftDeleted,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initials = remember(pelanggan.nama) {
                        val parts = pelanggan.nama.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                        when {
                            parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                            parts.size == 1 -> parts[0].take(2).uppercase()
                            else -> "P"
                        }
                    }
                    val hash = remember(pelanggan.nama) { kotlin.math.abs(pelanggan.nama.hashCode()) }
                    val avatarGradients = listOf(
                        listOf(MaterialTheme.colorScheme.primary, AccentCyan),
                        listOf(santoyaColors.success, AccentEmerald),
                        listOf(santoyaColors.warning, AccentAmber),
                        listOf(santoyaColors.info, MaterialTheme.colorScheme.primary),
                        listOf(MaterialTheme.colorScheme.secondary, AccentCyan)
                    )
                    val selectedGradient = if (isSoftDeleted) {
                        listOf(MaterialTheme.colorScheme.error, AccentRose)
                    } else {
                        avatarGradients[hash % avatarGradients.size]
                    }

                    HydroGradientBadge(
                        gradientColors = selectedGradient,
                        size = 42.dp,
                        cornerRadius = 14.dp,
                        elevation = 3.dp
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
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

                if (isSoftDeleted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.widthIn(max = 160.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (pelanggan.no_hp.isNotBlank()) {
                            IconButton(onClick = onCall, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Telepon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
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

            if (cleanAlamat.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Alamat: $cleanAlamat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!isSoftDeleted) {
                Spacer(modifier = Modifier.height(8.dp))
                CrmOrderPredictionBadge(
                    prediction = pelangganUiModel.crmPrediction,
                    pelanggan = pelanggan
                )
            }

            if (pelanggan.harga_khusus != null || pelanggan.id_kurir_langganan != null || kurirList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pelanggan.harga_khusus != null && pelanggan.harga_khusus > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f, fill = false)
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
                        val kName = kurirList.find { it.id == pelanggan.id_kurir_langganan }?.nama?.takeIf { it.isNotBlank() } ?: "Tanpa Kurir"
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f, fill = false)
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
                    } else if (kurirList.isNotEmpty() && !isSoftDeleted) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.santoyaColors.warningContainer.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = "Belum ada kurir langganan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.santoyaColors.onWarningContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (isSoftDeleted) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRestore != null) {
                        OutlinedButton(
                            onClick = onRestore,
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
                    }
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
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
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (hasKasbon) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(
                            start = 10.dp,
                            end = if (hasKasbon && (onPrintKasbon != null || onShareKasbon != null)) 2.dp else 10.dp,
                            top = if (hasKasbon && (onPrintKasbon != null || onShareKasbon != null)) 2.dp else 6.dp,
                            bottom = if (hasKasbon && (onPrintKasbon != null || onShareKasbon != null)) 2.dp else 6.dp
                        ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasKasbon && (onPrintKasbon != null || onShareKasbon != null)) {
                            Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                                Text(
                                    text = "Sisa Kasbon",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                SantoyaAdaptiveCurrencyText(
                                    amount = pelanggan.sisa_kasbon,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.santoyaColors.warning,
                                    minFontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (onPrintKasbon != null) {
                                    IconButton(
                                        onClick = onPrintKasbon,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "Cetak Tagihan",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (onShareKasbon != null) {
                                    IconButton(
                                        onClick = onShareKasbon,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Bagi WA",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = if (hasKasbon) "Sisa Kasbon:" else "Status Tagihan:",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (hasKasbon) {
                                SantoyaAdaptiveCurrencyText(
                                    amount = pelanggan.sisa_kasbon,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.santoyaColors.warning,
                                    minFontSize = 11.sp,
                                    textAlign = TextAlign.End
                                )
                            } else {
                                Text(
                                    text = "Lunas / Rp 0",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.santoyaColors.success,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (hasKasbon || onQuickOrder != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onQuickOrder != null) {
                            OutlinedButton(
                                onClick = onQuickOrder,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pesan Lagi",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (hasKasbon) {
                            Button(
                                onClick = onBayarKasbon,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bayar Kasbon",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
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

    val santoyaColors = MaterialTheme.santoyaColors
    val containerColor = when (prediction.status) {
        CrmOrderStatus.ACTIVE -> santoyaColors.successContainer.copy(alpha = 0.7f)
        CrmOrderStatus.DUE_SOON -> santoyaColors.warningContainer.copy(alpha = 0.7f)
        CrmOrderStatus.OVERDUE -> santoyaColors.dangerContainer
        CrmOrderStatus.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val contentColor = when (prediction.status) {
        CrmOrderStatus.ACTIVE -> santoyaColors.onSuccessContainer
        CrmOrderStatus.DUE_SOON -> santoyaColors.onWarningContainer
        CrmOrderStatus.OVERDUE -> santoyaColors.onDangerContainer
        CrmOrderStatus.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = when (prediction.status) {
        CrmOrderStatus.ACTIVE -> santoyaColors.success.copy(alpha = 0.45f)
        CrmOrderStatus.DUE_SOON -> santoyaColors.warning.copy(alpha = 0.55f)
        CrmOrderStatus.OVERDUE -> santoyaColors.danger.copy(alpha = 0.55f)
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
            val followUpMessage = "Halo Kak ${pelanggan.nama}, dari Santoya RO. Galonnya sudah perlu diisi ulang hari ini?"
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

    val isInitialDataLoaded by viewModel.isInitialDataLoaded.collectAsState()
    val sortedKasbon = remember(pelangganList, sortOption, searchQuery, transaksiList) {
        viewModel.getSortedKasbonPelanggan(pelangganList, sortOption, searchQuery, transaksiList)
    }
    val totalPiutang = remember(sortedKasbon) {
        sortedKasbon.sumOf { it.sisa_kasbon }
    }
    val unpaidByCustomer = remember(transaksiList) {
        transaksiList
            .filter {
                !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) &&
                    it.transaksi.status.equals("Kasbon", ignoreCase = true) &&
                    it.transaksi.id_pelanggan != null
            }
            .groupBy { it.transaksi.id_pelanggan!! }
            .mapValues { (_, list) -> list.sortedByDescending { it.transaksi.tanggal } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Cari nama, No. HP, atau nominal kasbon...",
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
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
                .padding(horizontal = 16.dp, vertical = 2.dp),
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

        if (!isInitialDataLoaded && pelangganList.isEmpty()) {
            SantoyaLoadingSkeleton(
                itemCount = 3,
                message = "Memuat data tagihan kasbon..."
            )
        } else if (sortedKasbon.isEmpty()) {
            EmptyListState(
                illustrationRes = if (searchQuery.isBlank()) R.drawable.img_empty_kasbon else R.drawable.img_empty_pelanggan,
                icon = Icons.Default.HourglassBottom,
                title = if (searchQuery.isBlank()) "Tidak ada tagihan kasbon" else "Pelanggan tidak ditemukan",
                subtitle = if (searchQuery.isBlank()) {
                    "Semua kasbon sudah lunas."
                } else {
                    "Data untuk \"${searchQuery.trim()}\" tidak ditemukan."
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    HydroBentoCard(
                        accentColor = MaterialTheme.colorScheme.error,
                        cornerRadius = 18.dp,
                        tintAlpha = 0.11f,
                        showWaterRipple = true,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                HydroGradientBadge(
                                    gradientColors = listOf(MaterialTheme.colorScheme.error, AccentRose),
                                    size = 38.dp,
                                    cornerRadius = 12.dp,
                                    elevation = 3.dp
                                ) {
                                    HydroExpenseFlowIcon(
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Total Piutang Kasbon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    SantoyaAdaptiveCurrencyText(
                                        amount = totalPiutang,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.error,
                                        minFontSize = 11.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = "${sortedKasbon.size} Pelanggan",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                items(sortedKasbon, key = { it.id }) { cust ->
                    val unpaid = unpaidByCustomer[cust.id] ?: emptyList()
                    val isExpanded = expandedCustomerIds.contains(cust.id)
                    val displayedUnpaid = if (isExpanded) unpaid else unpaid.take(3)

                    HydroBentoCard(
                        accentColor = MaterialTheme.colorScheme.error,
                        cornerRadius = 18.dp,
                        tintAlpha = 0.06f,
                        showWaterRipple = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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
                                SantoyaAdaptiveCurrencyText(
                                    amount = cust.sisa_kasbon,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    minFontSize = 11.sp,
                                    textAlign = TextAlign.End
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
                                                        "• ${FormatUtils.formatDateUi(t.transaksi.tanggal)} (${t.transaksi.jumlah} gln • DP ${FormatUtils.formatRupiah(dpPaid.toDouble())})"
                                                    } else if (isPartiallyPaid) {
                                                        "• ${FormatUtils.formatDateUi(t.transaksi.tanggal)} (${t.transaksi.jumlah} gln • Sisa Cicilan)"
                                                    } else {
                                                        "• ${FormatUtils.formatDateUi(t.transaksi.tanggal)} (${t.transaksi.jumlah} galon)"
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
                                                    "Sembunyikan rincian"
                                                } else {
                                                    "Lihat semua ${unpaid.size} nota (${unpaid.size - 3} lainnya)"
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
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
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
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "Cetak Tagihan",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val receipt = SantoyaReceiptHelper.generateTagihanKasbon(cust, unpaid.map { it.transaksi })
                                            SantoyaReceiptHelper.shareReceiptText(context, receipt, cust.no_hp)
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Bagi WA",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onBayarKasbon(cust) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bayar Kasbon", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
            subtitle = "Peringkat terisi otomatis setelah ada penjualan."
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Cari nama atau No. HP...",
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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("input_search_ranking")
            )

            if (filteredRanking.isEmpty()) {
                EmptyListState(
                    icon = Icons.Default.EmojiEvents,
                    title = "Pelanggan tidak ditemukan",
                    subtitle = "Data untuk \"${searchQuery.trim()}\" tidak ditemukan.",
                    onResetSearch = {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRanking, key = { it.second.id }) { triple ->
                        val rank = triple.first
                        val cust = triple.second
                        val galonCount = triple.third
                        val stats = customerSpendStats[cust.id] ?: Pair(0, 0.0)

                        val santoyaColors = MaterialTheme.santoyaColors
                        val rankColor = when (rank) {
                            1 -> santoyaColors.rankGold
                            2 -> santoyaColors.rankSilver
                            3 -> santoyaColors.rankBronze
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val rankGradient = when (rank) {
                            1 -> listOf(santoyaColors.rankGold, AccentAmber)
                            2 -> listOf(santoyaColors.rankSilver, Color(0xFF94A3B8))
                            3 -> listOf(santoyaColors.rankBronze, AccentAmber)
                            else -> listOf(MaterialTheme.colorScheme.primary, AccentCyan)
                        }

                        HydroBentoCard(
                            accentColor = rankColor,
                            cornerRadius = 18.dp,
                            tintAlpha = if (rank <= 3) 0.10f else 0.05f,
                            showWaterRipple = rank <= 3,
                            modifier = Modifier.fillMaxWidth()
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
                                    if (rank <= 3) {
                                        HydroGradientBadge(
                                            gradientColors = rankGradient,
                                            size = 38.dp,
                                            cornerRadius = 12.dp,
                                            elevation = 3.dp
                                        ) {
                                            Text(
                                                text = "#$rank",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#$rank",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
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
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        HydroGalonIcon(
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "$galonCount Galon",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            softWrap = false
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

@Composable
private fun PasifPelangganTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>,
    onQuickOrder: (Pelanggan) -> Unit = {}
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
                    text = "Cari pelanggan lama...",
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
                .padding(horizontal = 16.dp, vertical = 6.dp)
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
                    .padding(horizontal = 16.dp, vertical = 6.dp),
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
                    "Semua pelanggan aktif pesan dalam $selectedDaysFilter hari terakhir."
                } else {
                    "Data untuk \"${searchQuery.trim()}\" tidak ditemukan."
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPasif, key = { it.first.id }) { pair ->
                    val cust = pair.first
                    val days = pair.second
                    val lastOrderMillis = lastOrderDateMap[cust.id]

                    HydroBentoCard(
                        accentColor = MaterialTheme.colorScheme.error,
                        cornerRadius = 18.dp,
                        tintAlpha = 0.06f,
                        showWaterRipple = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HydroGradientBadge(
                                    gradientColors = listOf(MaterialTheme.colorScheme.error, AccentRose),
                                    size = 38.dp,
                                    cornerRadius = 12.dp,
                                    elevation = 2.dp
                                ) {
                                    HydroCustomerDropIcon(
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
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
                                            "$days hari belum memesan (Terakhir: ${FormatUtils.formatDateUi(lastOrderMillis)})"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onQuickOrder(cust) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pesan Lagi",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                if (cust.no_hp.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            val cleanPhone = cust.no_hp.replace("[^0-9]".toRegex(), "")
                                            val formattedPhone = if (cleanPhone.startsWith("0")) {
                                                "62" + cleanPhone.substring(1)
                                            } else {
                                                cleanPhone
                                            }
                                            val followUpMessage = "Halo Kak ${cust.nama}, dari Santoya RO. Galonnya sudah perlu diisi ulang hari ini?"
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
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Hubungi",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
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

@Composable
private fun PelangganFormDialog(
    title: String,
    pelanggan: Pelanggan?,
    kurirList: List<Kurir> = emptyList(),
    isKaryawan: Boolean = false,
    activeKaryawan: Kurir? = null,
    onDismiss: () -> Unit,
    onConfirm: (nama: String, alamat: String, noHp: String, hargaKhusus: Double?, idKurirLangganan: Int?) -> Unit
) {
    var nama by remember { mutableStateOf(pelanggan?.nama ?: "") }
    var alamat by remember { mutableStateOf(KaryawanScopeHelper.cleanPelangganAlamat(pelanggan?.alamat ?: "")) }
    var noHp by remember { mutableStateOf(pelanggan?.no_hp ?: "") }
    var hargaKhususStr by remember { mutableStateOf(pelanggan?.harga_khusus?.toLong()?.toString() ?: "") }
    val lockedKurirId = if (isKaryawan && activeKaryawan != null) activeKaryawan.id else null
    var selectedKurirId by remember(lockedKurirId, pelanggan?.id) {
        mutableStateOf<Int?>(lockedKurirId ?: pelanggan?.id_kurir_langganan)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val namaFocus = remember { FocusRequester() }
    val alamatFocus = remember { FocusRequester() }
    val hpFocus = remember { FocusRequester() }
    val hargaFocus = remember { FocusRequester() }

    val submitForm = {
        if (nama.isBlank()) {
            errorMessage = "Nama pelanggan wajib diisi"
        } else {
            val hKhusus = hargaKhususStr.toDoubleOrNull()
            focusManager.clearFocus()
            keyboardController?.hide()
            onConfirm(nama.trim(), alamat.trim(), noHp.trim(), hKhusus, selectedKurirId)
        }
    }

    SantoyaFormBottomSheet(
        title = title,
        subtitle = "Data pelanggan & langganan Santoya RO",
        headerIcon = Icons.Default.People,
        onDismiss = onDismiss,
        confirmLabel = "Simpan",
        onConfirm = submitForm
    ) {
        OutlinedTextField(
            value = nama,
            onValueChange = {
                nama = it
                errorMessage = null
            },
            label = { Text("Nama Lengkap *") },
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
            value = alamat,
            onValueChange = { alamat = it },
            label = { Text("Alamat / Desa / Blok") },
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
            value = noHp,
            onValueChange = { noHp = it.filter { c -> c.isDigit() || c == '+' } },
            label = { Text("No. HP / WhatsApp") },
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
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(hpFocus)
                .bringIntoViewOnFocus(),
            shape = RoundedCornerShape(12.dp)
        )

        Text(
            text = "Pengaturan Langganan (Opsional)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (isKaryawan) {
            val lockedHargaKhusus = pelanggan?.harga_khusus?.takeIf { it > 0.0 }
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
                            text = if (lockedHargaKhusus != null) {
                                "Harga Khusus: ${FormatUtils.formatRupiah(lockedHargaKhusus)}"
                            } else {
                                "Tarif: Harga Normal"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Harga diatur oleh Pemilik.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = hargaKhususStr,
                onValueChange = { hargaKhususStr = it.filter { c -> c.isDigit() } },
                label = { Text("Harga Khusus Galon (Rp)") },
                placeholder = { Text("Kosongkan jika harga umum") },
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
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(hargaFocus)
                    .bringIntoViewOnFocus(),
                shape = RoundedCornerShape(12.dp)
            )
            Text(
                text = "Kosongkan jika pakai harga galon normal.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Kurir Langganan",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (isKaryawan && activeKaryawan != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Otomatis: ${activeKaryawan.nama}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Pelanggan ini tercatat sebagai langganan Anda.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        } else {
            Text(
                text = "Kurir ini otomatis terpilih saat transaksi & muncul di jadwal HP-nya.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    modifier = Modifier.height(34.dp)
                )
                kurirList.forEach { kurir ->
                    FilterChip(
                        selected = selectedKurirId == kurir.id,
                        onClick = { selectedKurirId = kurir.id },
                        label = { Text(kurir.nama, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.height(34.dp)
                    )
                }
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

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputNominal = nominalStr.toDoubleOrNull() ?: 0.0
    val sisaSetelahBayar = (pelanggan.sisa_kasbon - inputNominal).coerceAtLeast(0.0)
    val isLunasPenuh = inputNominal >= pelanggan.sisa_kasbon && pelanggan.sisa_kasbon > 0

    val submitPelunasan = {
        val nominal = nominalStr.toDoubleOrNull()
        when {
            nominal == null || nominal <= 0 -> {
                errorMessage = "Nominal pembayaran tidak valid"
            }
            nominal > pelanggan.sisa_kasbon -> {
                errorMessage = "Nominal melebihi total kasbon"
            }
            else -> {
                focusManager.clearFocus()
                keyboardController?.hide()
                onConfirmBayar(nominal, selectedMetode)
            }
        }
    }

    SantoyaFormBottomSheet(
        title = "Pelunasan Kasbon",
        subtitle = "Pelanggan: ${pelanggan.nama}",
        headerIcon = Icons.Default.Payments,
        headerAccent = MaterialTheme.colorScheme.error,
        onDismiss = onDismiss,
        confirmLabel = "Konfirmasi",
        onConfirm = submitPelunasan
    ) {
        Text(
            text = "Total Kasbon Saat Ini: ${FormatUtils.formatRupiah(pelanggan.sisa_kasbon)}",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )

        if (unpaidList.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Nota Belum Lunas (${unpaidList.size}):",
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
                                    "• ${FormatUtils.formatDateUi(t.transaksi.tanggal)} (${t.transaksi.jumlah} gln • DP ${FormatUtils.formatRupiah(dpPaid.toDouble())})"
                                } else {
                                    "• ${FormatUtils.formatDateUi(t.transaksi.tanggal)} (${t.transaksi.jumlah} gln)"
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

        val quickPresets = remember(pelanggan.sisa_kasbon) {
            val total = pelanggan.sisa_kasbon.toLong()
            listOfNotNull(
                "Lunas Penuh" to total,
                (total / 2).takeIf { it > 0 && total > 10_000 }?.let { "50%" to it },
                10_000L.takeIf { it < total }?.let { "10rb" to it },
                20_000L.takeIf { it < total }?.let { "20rb" to it }
            ).distinctBy { it.second }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPresets.forEach { (label, amount) ->
                val isSelected = nominalStr == amount.toString()
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        nominalStr = amount.toString()
                        errorMessage = null
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.heightIn(min = 38.dp)
                )
            }
        }

        OutlinedTextField(
            value = nominalStr,
            onValueChange = {
                nominalStr = it.filter { c -> c.isDigit() }
                errorMessage = null
            },
            label = { Text("Nominal Bayar (Rp) *") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { submitPelunasan() }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus(),
            shape = RoundedCornerShape(12.dp)
        )

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
                    label = {
                        Text(
                            text = metode,
                            maxLines = 1,
                            fontWeight = if (selectedMetode == metode) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                )
            }
        }

        val santoyaColors = MaterialTheme.santoyaColors
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isLunasPenuh) santoyaColors.successContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sisa setelah bayar:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isLunasPenuh) "LUNAS" else FormatUtils.formatRupiah(sisaSetelahBayar),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLunasPenuh) santoyaColors.onSuccessContainer else santoyaColors.danger
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
}

@Composable
private fun EmptyListState(
    @DrawableRes illustrationRes: Int = R.drawable.img_empty_pelanggan,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    title: String,
    subtitle: String,
    onResetSearch: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        SantoyaEmptyState(
            illustrationRes = illustrationRes,
            title = title,
            subtitle = subtitle,
            secondaryActionLabel = if (onResetSearch != null) "Reset Pencarian" else null,
            secondaryActionIcon = if (onResetSearch != null) Icons.Default.Clear else null,
            onSecondaryAction = onResetSearch
        )
    }
}
