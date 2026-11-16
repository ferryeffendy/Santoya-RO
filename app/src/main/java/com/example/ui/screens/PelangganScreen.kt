package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.TransaksiDetail
import com.example.ui.components.HighlightedSearchText
import com.example.ui.components.HydroBentoCard
import com.example.ui.components.HydroCustomerDropIcon
import com.example.ui.components.HydroExpenseFlowIcon
import com.example.ui.components.HydroGalonIcon
import com.example.ui.components.HydroGradientBadge
import com.example.ui.components.HydroReceiptNoteIcon
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaAdaptiveTabPillRow
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.components.SantoyaEmptyType
import com.example.ui.components.SantoyaFilterPill
import com.example.ui.components.SantoyaFormBottomSheet
import com.example.ui.components.SantoyaSearchBar
import com.example.ui.components.SantoyaTabPillItem
import com.example.ui.components.SantoyaLoadingSkeleton
import com.example.ui.components.bringIntoViewOnFocus
import com.example.ui.components.rememberIsImeVisible
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.santoyaColors
import com.example.util.CrmOrderPrediction
import com.example.util.CrmOrderStatus
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.SantoyaReceiptHelper
import com.example.util.SearchField
import com.example.util.SearchMatcher
import com.example.viewmodel.ActiveReceiptDialog
import com.example.viewmodel.KasbonSortOption
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

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

            val tabItems = remember(kasbonCount, jadwalList.size) {
                listOf(
                    SantoyaTabPillItem(
                        label = "Semua",
                        testTag = "tab_pelanggan_semua"
                    ),
                    SantoyaTabPillItem(
                        label = "Kasbon",
                        badgeCount = if (kasbonCount > 0) kasbonCount else null,
                        isUrgentBadge = true,
                        testTag = "tab_pelanggan_kasbon"
                    ),
                    SantoyaTabPillItem(
                        label = "Jadwal",
                        badgeCount = if (jadwalList.isNotEmpty()) jadwalList.size else null,
                        testTag = "tab_pelanggan_jadwal"
                    ),
                    SantoyaTabPillItem(
                        label = "Pasif",
                        testTag = "tab_pelanggan_pasif"
                    ),
                    SantoyaTabPillItem(
                        label = "Ranking",
                        testTag = "tab_pelanggan_ranking"
                    )
                )
            }

            SantoyaAdaptiveTabPillRow(
                items = tabItems,
                selectedIndex = pagerState.currentPage,
                onSelect = { idx ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(idx)
                    }
                }
            )

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
                            onRestore = { viewModel.restorePelanggan(it) }
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
                            onDelete = { pelangganToDelete = it }
                        )
                        3 -> {
                            val pelangganList = remember(activePelangganUiModels) {
                                activePelangganUiModels.map { it.pelanggan }
                            }
                            PasifPelangganTab(
                                viewModel = viewModel,
                                pelangganList = pelangganList,
                                transaksiList = effectiveTransaksiList
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
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Tambah Pelanggan")
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

    editingPelanggan?.let { target ->
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

    payingKasbonPelanggan?.let { target ->
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

    pelangganToDelete?.let { target ->
        val isAuditDeleted = KaryawanScopeHelper.isSoftDeletedPelanggan(target)
        val deletedBy = KaryawanScopeHelper.extractDeletedByPelanggan(target) ?: "Karyawan"
        val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(target.alamat)
        val hasBlockingKasbon = effectiveIsKaryawan && !isAuditDeleted && target.sisa_kasbon > 0.0
        AlertDialog(
            onDismissRequest = { pelangganToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Warning,
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
                        shape = RoundedCornerShape(10.dp),
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
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HourglassEmpty,
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
                        imageVector = if (isAuditDeleted) Icons.Outlined.DeleteForever else Icons.Outlined.Delete,
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
private fun CompactPelangganSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    resultCount: Int? = null,
    modifier: Modifier = Modifier
) {
    SantoyaSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = placeholder,
        testTag = testTag,
        resultCount = resultCount,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
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
    onRestore: (Pelanggan) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
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
    val filtered = remember(searchQuery, baseModels, kurirList) {
        viewModel.getFilteredPelangganUiModels(searchQuery, baseModels, kurirList)
    }
    val unpaidTxByCustomer = remember(transaksiList) {
        transaksiList
            .asSequence()
            .filter {
                !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) &&
                    it.transaksi.status.equals("Kasbon", ignoreCase = true)
            }
            .mapNotNull { detail -> detail.transaksi.id_pelanggan?.let { pid -> pid to detail.transaksi } }
            .groupBy({ it.first }, { it.second })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CompactPelangganSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Cari nama, alamat, atau No. HP...",
            testTag = "input_search_pelanggan",
            resultCount = if (searchQuery.isNotBlank()) filtered.size else null
        )

        if (!isKaryawan && deletedModels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SantoyaFilterPill(
                    label = "Semua (${pelangganUiModels.size})",
                    selected = filterMode == 0,
                    onClick = { filterMode = 0 },
                    centerContent = true,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                )
                SantoyaFilterPill(
                    label = "Aktif (${activeModels.size})",
                    selected = filterMode == 1,
                    onClick = { filterMode = 1 },
                    centerContent = true,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                )
                SantoyaFilterPill(
                    label = "Dihapus (${deletedModels.size})",
                    selected = filterMode == 2,
                    onClick = { filterMode = 2 },
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedContentColor = MaterialTheme.colorScheme.error,
                    selectedBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                    centerContent = true,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
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
                icon = Icons.Outlined.People,
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
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = if (maxWidth >= 600.dp) 2 else 1
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 92.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.pelanggan.id }) { item ->
                        val isSoftDeleted = KaryawanScopeHelper.isSoftDeletedPelanggan(item.pelanggan)
                        val deletedBy = KaryawanScopeHelper.extractDeletedByPelanggan(item.pelanggan)
                        PelangganItemCard(
                            pelangganUiModel = item,
                            kurirList = kurirList,
                            searchQuery = searchQuery,
                            isSoftDeleted = isSoftDeleted,
                            deletedBy = deletedBy,
                            onEdit = { onEdit(item.pelanggan) },
                            onDelete = { onDelete(item.pelanggan) },
                            onRestore = { onRestore(item.pelanggan) },
                            onBayarKasbon = { onBayarKasbon(item.pelanggan) },
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
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, jadwalUiModels, kurirList) {
        viewModel.getFilteredPelangganUiModels(searchQuery, jadwalUiModels, kurirList)
    }

    if (jadwalUiModels.isEmpty()) {
        EmptyListState(
            icon = Icons.Outlined.Schedule,
            title = "Tidak ada jadwal pengingat",
            subtitle = "Daftar pelanggan yang waktunya isi ulang akan muncul di sini."
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            CompactPelangganSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Cari pelanggan yang jatuh tempo...",
                testTag = "input_search_jadwal_pelanggan",
                resultCount = if (searchQuery.isNotBlank()) filtered.size else null
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
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = if (maxWidth >= 600.dp) 2 else 1
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 92.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.pelanggan.id }) { item ->
                            PelangganItemCard(
                                pelangganUiModel = item,
                                kurirList = kurirList,
                                searchQuery = searchQuery,
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PelangganItemCard(
    pelangganUiModel: com.example.viewmodel.PelangganUiModel,
    kurirList: List<Kurir> = emptyList(),
    searchQuery: String = "",
    isSoftDeleted: Boolean = false,
    deletedBy: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)? = null,
    onBayarKasbon: () -> Unit,
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
        cornerRadius = 16.dp,
        tintAlpha = if (isSoftDeleted) 0.12f else 0.05f,
        showWaterRipple = !isSoftDeleted,
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
                        size = 36.dp,
                        cornerRadius = 11.dp,
                        elevation = 2.dp
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.santoyaColors.onHero
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        HighlightedSearchText(
                            text = pelanggan.nama,
                            query = searchQuery,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (pelanggan.no_hp.isNotBlank()) {
                            HighlightedSearchText(
                                text = pelanggan.no_hp,
                                query = searchQuery,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Tanpa No. HP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!isSoftDeleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (pelanggan.no_hp.isNotBlank()) {
                            IconButton(
                                onClick = onCall,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Call,
                                    contentDescription = "Telepon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(48.dp)
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
            }

            if (cleanAlamat.isNotBlank()) {
                HighlightedSearchText(
                    text = cleanAlamat,
                    query = searchQuery,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (isSoftDeleted) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.xs),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Dihapus oleh ${deletedBy ?: "Karyawan"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    CrmOrderPredictionPill(
                        prediction = pelangganUiModel.crmPrediction,
                        pelanggan = pelanggan
                    )
                    pelangganUiModel.crmPrediction.averageIntervalDays?.let { avgDays ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Siklus ±$avgDays hari",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (pelanggan.harga_khusus != null && pelanggan.harga_khusus > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "Khusus: ${FormatUtils.formatRupiah(pelanggan.harga_khusus)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                if (pelanggan.id_kurir_langganan != null) {
                    val kName = kurirList.find { it.id == pelanggan.id_kurir_langganan }?.nama?.takeIf { it.isNotBlank() } ?: "Tanpa Kurir"
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "Kurir: $kName",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                } else if (kurirList.isNotEmpty() && !isSoftDeleted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.santoyaColors.warningContainer.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "Tanpa kurir",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.santoyaColors.onWarningContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (isSoftDeleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRestore != null) {
                        OutlinedButton(
                            onClick = onRestore,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = "Pulihkan",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pulihkan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = "Hapus Permanen",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hapus Permanen",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (hasKasbon) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sisa Kasbon",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            SantoyaAdaptiveCurrencyText(
                                amount = pelanggan.sisa_kasbon,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.santoyaColors.warning,
                                minFontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (onPrintKasbon != null) {
                            IconButton(
                                onClick = onPrintKasbon,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Print,
                                    contentDescription = "Cetak Tagihan",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (onShareKasbon != null) {
                            IconButton(
                                onClick = onShareKasbon,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Bagi WA",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Button(
                            onClick = onBayarKasbon,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .wrapContentWidth()
                                .heightIn(min = 38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Bayar Kasbon",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status Tagihan",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Lunas • Rp 0",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.santoyaColors.success,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrmOrderPredictionPill(
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

    val badgeShape = RoundedCornerShape(6.dp)
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

    Surface(
        shape = badgeShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .clip(badgeShape)
            .then(clickModifier)
            .testTag("crm_badge_${pelanggan.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = prediction.badgeText,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isActionable) "${prediction.badgeText} • Hubungi" else prediction.badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KasbonPelangganTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>,
    onBayarKasbon: (Pelanggan) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
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
                    it.transaksi.status.equals("Kasbon", ignoreCase = true)
            }
            .mapNotNull { detail -> detail.transaksi.id_pelanggan?.let { pid -> pid to detail } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.sortedByDescending { it.transaksi.tanggal } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CompactPelangganSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Cari nama, No. HP, atau nominal kasbon...",
            testTag = "input_search_kasbon",
            resultCount = if (searchQuery.isNotBlank()) sortedKasbon.size else null
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Urut:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            KasbonSortOption.values().forEach { option ->
                SantoyaFilterPill(
                    label = option.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = sortOption == option,
                    onClick = { sortOption = option },
                    centerContent = true,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
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
                emptyType = if (searchQuery.isBlank()) SantoyaEmptyType.KASBON else SantoyaEmptyType.PELANGGAN,
                icon = Icons.Outlined.HourglassEmpty,
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
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = if (maxWidth >= 600.dp) 2 else 1
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 92.dp),
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
                                            tint = MaterialTheme.santoyaColors.onHero,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Total Piutang Kasbon",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        SantoyaAdaptiveCurrencyText(
                                            amount = totalPiutang,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                            minFontSize = 11.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(SantoyaRadius.sm),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = "${sortedKasbon.size} Pelanggan",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                            cornerRadius = SantoyaRadius.lg,
                            tintAlpha = 0.05f,
                            showWaterRipple = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .animateContentSize()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        HighlightedSearchText(
                                            text = cust.nama,
                                            query = searchQuery,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        HighlightedSearchText(
                                            text = KaryawanScopeHelper.cleanPelangganAlamat(cust.alamat)
                                                .ifBlank { cust.no_hp.ifBlank { "Tanpa keterangan" } },
                                            query = searchQuery,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        SantoyaAdaptiveCurrencyText(
                                            amount = cust.sisa_kasbon,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                            minFontSize = 11.sp,
                                            textAlign = TextAlign.End
                                        )
                                        if (unpaid.isNotEmpty()) {
                                            Text(
                                                text = "${unpaid.size} nota belum lunas",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                if (unpaid.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(SantoyaRadius.sm),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Rincian Kasbon (${unpaid.size})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.error,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (unpaid.size > 3) {
                                                    Text(
                                                        text = if (isExpanded) "Ringkas" else "+${unpaid.size - 3} lainnya",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(SantoyaRadius.xs))
                                                            .clickable {
                                                                expandedCustomerIds = if (isExpanded) {
                                                                    expandedCustomerIds - cust.id
                                                                } else {
                                                                    expandedCustomerIds + cust.id
                                                                }
                                                            }
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            displayedUnpaid.forEach { t ->
                                                val noteSisaKasbon = t.transaksi.remainingKasbonAmount()
                                                val dpPaid = t.transaksi.uangMuka
                                                val isPartiallyPaid = dpPaid > 0L || (t.transaksi.total_harga > noteSisaKasbon && t.transaksi.jumlah > 0)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = when {
                                                            dpPaid > 0L -> "${FormatUtils.formatDateUi(t.transaksi.tanggal)} • ${t.transaksi.jumlah} gln • Uang Muka ${FormatUtils.formatRupiah(dpPaid.toDouble())}"
                                                            isPartiallyPaid -> "${FormatUtils.formatDateUi(t.transaksi.tanggal)} • ${t.transaksi.jumlah} gln • Cicilan"
                                                            else -> "${FormatUtils.formatDateUi(t.transaksi.tanggal)} • ${t.transaksi.jumlah} galon"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = FormatUtils.formatRupiah(noteSisaKasbon),
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    ) {
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
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Print,
                                                contentDescription = "Cetak Tagihan",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val receipt = SantoyaReceiptHelper.generateTagihanKasbon(cust, unpaid.map { it.transaksi })
                                                SantoyaReceiptHelper.shareReceiptText(context, receipt, cust.no_hp)
                                            },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Share,
                                                contentDescription = "Bagi WA",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { onBayarKasbon(cust) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 38.dp)
                                    ) {
                                        Icon(imageVector = Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
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
    }
}

@Composable
private fun TopRankingTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>
) {
    val focusManager = LocalFocusManager.current
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
    val indexedRanking = remember(ranking) {
        ranking.mapIndexed { idx, pair -> Triple(idx + 1, pair.first, pair.second) }
    }
    val filteredRanking = remember(indexedRanking, searchQuery) {
        if (searchQuery.isBlank()) {
            indexedRanking
        } else {
            SearchMatcher.rankAndFilter(indexedRanking, searchQuery) { triple ->
                val cust = triple.second
                val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(cust.alamat)
                listOf(
                    SearchField(cust.nama, weight = 24, isPrimaryName = true),
                    SearchField(cleanAlamat, weight = 10),
                    SearchField(cust.no_hp, weight = 14, isPhone = true),
                    SearchField("${triple.third} galon", weight = 6),
                    SearchField("${triple.third} gln", weight = 6)
                )
            }
        }
    }

    if (ranking.isEmpty()) {
        EmptyListState(
            icon = Icons.Outlined.EmojiEvents,
            title = "Belum Ada Ranking Penjualan",
            subtitle = "Peringkat terisi otomatis setelah ada penjualan."
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            CompactPelangganSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Cari nama atau No. HP...",
                testTag = "input_search_ranking",
                resultCount = if (searchQuery.isNotBlank()) filteredRanking.size else null
            )

            if (filteredRanking.isEmpty()) {
                EmptyListState(
                    icon = Icons.Outlined.EmojiEvents,
                    title = "Pelanggan tidak ditemukan",
                    subtitle = "Data untuk \"${searchQuery.trim()}\" tidak ditemukan.",
                    onResetSearch = {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = if (maxWidth >= 600.dp) 2 else 1
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 92.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                2 -> listOf(santoyaColors.rankSilver, santoyaColors.rankSilver.copy(alpha = 0.78f))
                                3 -> listOf(santoyaColors.rankBronze, AccentAmber)
                                else -> listOf(MaterialTheme.colorScheme.primary, AccentCyan)
                            }

                            HydroBentoCard(
                                accentColor = rankColor,
                                cornerRadius = SantoyaRadius.lg,
                                tintAlpha = if (rank <= 3) 0.10f else 0.05f,
                                showWaterRipple = rank <= 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (rank <= 3) {
                                            HydroGradientBadge(
                                                gradientColors = rankGradient,
                                                size = 36.dp,
                                                cornerRadius = 11.dp,
                                                elevation = 2.dp
                                            ) {
                                                Text(
                                                    text = "#$rank",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.santoyaColors.onHero,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(SantoyaRadius.md),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "#$rank",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            HighlightedSearchText(
                                                text = cust.nama,
                                                query = searchQuery,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            HighlightedSearchText(
                                                text = KaryawanScopeHelper.cleanPelangganAlamat(cust.alamat).ifBlank { "Pelanggan Santoya" },
                                                query = searchQuery,
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
                                        modifier = Modifier.wrapContentWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            HydroGalonIcon(
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "$galonCount Gln",
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PasifPelangganTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
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
            SearchMatcher.rankAndFilter(pasifList, searchQuery) { (cust, _) ->
                val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(cust.alamat)
                listOf(
                    SearchField(cust.nama, weight = 24, isPrimaryName = true),
                    SearchField(cleanAlamat, weight = 10),
                    SearchField(cust.no_hp, weight = 14, isPhone = true)
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CompactPelangganSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Cari pelanggan lama...",
            testTag = "input_search_pasif",
            resultCount = if (searchQuery.isNotBlank()) filteredPasif.size else null
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val useShortUnit = maxWidth < 330.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(7L, 14L, 30L, 60L).forEach { days ->
                        val label = if (useShortUnit) "> $days Hr" else "> $days Hari"
                        SantoyaFilterPill(
                            label = label,
                            selected = selectedDaysFilter == days,
                            onClick = { selectedDaysFilter = days },
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedContentColor = MaterialTheme.colorScheme.onErrorContainer,
                            selectedBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            centerContent = true,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (filteredPasif.isEmpty()) {
            EmptyListState(
                icon = Icons.Outlined.PersonOff,
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
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = if (maxWidth >= 600.dp) 2 else 1
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 92.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPasif, key = { it.first.id }) { pair ->
                        val cust = pair.first
                        val days = pair.second
                        val lastOrderMillis = lastOrderDateMap[cust.id]

                        HydroBentoCard(
                            accentColor = MaterialTheme.colorScheme.error,
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
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HydroGradientBadge(
                                        gradientColors = listOf(MaterialTheme.colorScheme.error, AccentRose),
                                        size = 36.dp,
                                        cornerRadius = 11.dp,
                                        elevation = 2.dp
                                    ) {
                                        HydroCustomerDropIcon(
                                            tint = MaterialTheme.santoyaColors.onHero,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        HighlightedSearchText(
                                            text = cust.nama,
                                            query = searchQuery,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val cleanAddr = KaryawanScopeHelper.cleanPelangganAlamat(cust.alamat)
                                        if (cleanAddr.isNotBlank()) {
                                            HighlightedSearchText(
                                                text = cleanAddr,
                                                query = searchQuery,
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
                                                "$days hari pasif • Terakhir ${FormatUtils.formatDateUi(lastOrderMillis)}"
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
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 38.dp)
                                    ) {
                                        Icon(imageVector = Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Hubungi (${cust.no_hp})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No. HP belum tercatat",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
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
}

@OptIn(ExperimentalLayoutApi::class)
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
        headerIconContent = {
            HydroCustomerDropIcon(
                tint = MaterialTheme.santoyaColors.onHero,
                modifier = Modifier.size(21.dp)
            )
        },
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
                        imageVector = Icons.Outlined.Lock,
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

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SantoyaFilterPill(
                    label = "Tanpa Kurir",
                    selected = selectedKurirId == null,
                    onClick = { selectedKurirId = null }
                )
                kurirList.forEach { kurir ->
                    SantoyaFilterPill(
                        label = kurir.nama,
                        selected = selectedKurirId == kurir.id,
                        onClick = { selectedKurirId = kurir.id }
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

@OptIn(ExperimentalLayoutApi::class)
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
    val kembalian = (inputNominal - pelanggan.sisa_kasbon).coerceAtLeast(0.0)
    val isLunasPenuh = inputNominal >= pelanggan.sisa_kasbon && pelanggan.sisa_kasbon > 0

    val submitPelunasan = {
        val nominal = nominalStr.toDoubleOrNull()
        when {
            nominal == null || nominal <= 0 -> {
                errorMessage = "Nominal pembayaran tidak valid"
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
        headerIcon = Icons.Outlined.Payments,
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
                                    "• ${FormatUtils.formatDateUi(t.transaksi.tanggal)} (${t.transaksi.jumlah} gln • Uang Muka ${FormatUtils.formatRupiah(dpPaid.toDouble())})"
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

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPresets.forEach { (label, amount) ->
                val isSelected = nominalStr == amount.toString()
                SantoyaFilterPill(
                    label = label,
                    selected = isSelected,
                    onClick = {
                        nominalStr = amount.toString()
                        errorMessage = null
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
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
                SantoyaFilterPill(
                    label = FormatUtils.formatMetodeBayarLabel(metode),
                    selected = selectedMetode == metode,
                    onClick = { selectedMetode = metode },
                    centerContent = true,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val santoyaColors = MaterialTheme.santoyaColors
        Surface(
            shape = RoundedCornerShape(SantoyaRadius.sm),
            color = if (isLunasPenuh) santoyaColors.successContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = if (isLunasPenuh) santoyaColors.onSuccessContainer else santoyaColors.danger
                    )
                }
                if (kembalian > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kembalian:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = santoyaColors.onSuccessContainer
                        )
                        Text(
                            text = FormatUtils.formatRupiah(kembalian),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = santoyaColors.onSuccessContainer
                        )
                    }
                }
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
    emptyType: SantoyaEmptyType = SantoyaEmptyType.PELANGGAN,
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
            emptyType = emptyType,
            title = title,
            subtitle = subtitle,
            accentColor = if (emptyType == SantoyaEmptyType.KASBON) {
                MaterialTheme.santoyaColors.success
            } else {
                MaterialTheme.colorScheme.primary
            },
            secondaryActionLabel = if (onResetSearch != null) "Reset Pencarian" else null,
            secondaryActionIcon = if (onResetSearch != null) Icons.Outlined.Clear else null,
            onSecondaryAction = onResetSearch
        )
    }
}
