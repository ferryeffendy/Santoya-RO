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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.DepotProfile
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.TransaksiDetail
import com.example.ui.components.CompactPelangganSearchBar
import com.example.ui.components.CrmOrderPredictionPill
import com.example.ui.components.DeletePelangganConfirmDialog
import com.example.ui.components.DepotQuickFillDialog
import com.example.ui.components.HighlightedSearchText
import com.example.ui.components.HydroBentoCard
import com.example.ui.components.HydroCustomerDropIcon
import com.example.ui.components.HydroExpenseFlowIcon
import com.example.ui.components.HydroGalonIcon
import com.example.ui.components.HydroGradientBadge
import com.example.ui.components.PelangganDetailBottomSheet
import com.example.ui.components.PelangganEmptyListState
import com.example.ui.components.PelangganFilterSortBarContainer
import com.example.ui.components.PelangganFormDialog
import com.example.ui.components.PelangganItemCard
import com.example.ui.components.PelangganSortOptionUi
import com.example.ui.components.PelangganSortRow
import com.example.ui.components.PelunasanKasbonDialog
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaAdaptiveTabPillRow
import com.example.ui.components.SantoyaEmptyType
import com.example.ui.components.SantoyaFilterPill
import com.example.ui.components.SantoyaLoadingSkeleton
import com.example.ui.components.SantoyaTabPillItem
import com.example.ui.components.rememberContactPhonePicker
import com.example.ui.components.rememberIsImeVisible
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRose
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.santoyaColors
import com.example.util.CrmFollowUpContext
import com.example.util.CrmFollowUpHelper
import com.example.util.CrmJadwalFilter
import com.example.util.CrmJadwalSort
import com.example.util.CrmOrderStatus
import com.example.util.CrmPasifSegment
import com.example.util.CrmPasifSort
import com.example.util.CrmRankingPeriod
import com.example.util.CrmRankingSort
import com.example.util.CustomerSpendStats
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.PelangganAuditHelper
import com.example.util.SantoyaReceiptHelper
import com.example.util.SearchField
import com.example.util.SearchMatcher
import com.example.util.SemuaPelangganSort
import com.example.viewmodel.ActiveReceiptDialog
import com.example.viewmodel.KasbonSortOption
import com.example.viewmodel.PelangganUiModel
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/**
 * Layar manajemen Pelanggan dengan 5 tab (Semua, Kasbon, Jadwal, Pasif, Ranking),
 * bottom sheet Detail Pelanggan 1-ketuk, dan dialog pelunasan kasbon dengan perhitungan kembalian.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PelangganScreen(
    viewModel: SantoyaViewModel,
    pelangganUiModels: List<PelangganUiModel>? = null,
    transaksiList: List<TransaksiDetail>? = null,
    isKaryawan: Boolean? = null,
    activeKaryawan: Kurir? = null,
    onBack: () -> Unit = { viewModel.navigateTo(SantoyaScreen.DASHBOARD) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kurirList by viewModel.allKurir.collectAsStateWithLifecycle()
    val vmPelangganUiModels by viewModel.scopedPelangganUiModels.collectAsStateWithLifecycle()
    val vmTransaksiList by viewModel.scopedPelangganTransaksi.collectAsStateWithLifecycle()
    val vmIsKaryawan by viewModel.isKaryawan.collectAsStateWithLifecycle()
    val vmActiveKaryawan by viewModel.activeKaryawan.collectAsStateWithLifecycle()
    val isInitialDataLoaded by viewModel.isInitialDataLoaded.collectAsStateWithLifecycle()
    val crmContactedTimestamps by viewModel.crmContactedTimestamps.collectAsStateWithLifecycle()
    val depotProfile by viewModel.depotProfile.collectAsStateWithLifecycle()

    val effectiveIsKaryawan = isKaryawan ?: vmIsKaryawan
    val effectiveActiveKaryawan = activeKaryawan ?: vmActiveKaryawan
    val isOwnerProfile = !effectiveIsKaryawan && depotProfile.isOwnerRole()
    var quickFillPendingAction by remember { mutableStateOf<((DepotProfile) -> Unit)?>(null) }
    val effectiveTransaksiList = transaksiList ?: vmTransaksiList
    val effectivePelangganUiModels = pelangganUiModels ?: vmPelangganUiModels

    val selectedTab by viewModel.pelangganTab.collectAsStateWithLifecycle()
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
    var detailPelangganId by remember { mutableStateOf<Int?>(null) }
    var pendingContactCustomerId by rememberSaveable { mutableStateOf<Int?>(null) }
    var duplicatePhoneConfirm by remember { mutableStateOf<Triple<Pelanggan, String, Pelanggan>?>(null) }
    val isImeVisible = rememberIsImeVisible()

    val activePelangganUiModels = remember(effectivePelangganUiModels) {
        effectivePelangganUiModels.filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it.pelanggan) }
    }
    val activePelangganList = remember(activePelangganUiModels) {
        activePelangganUiModels.map { it.pelanggan }
    }

    val launchContactPicker = rememberContactPhonePicker(
        onPhonePicked = { normalizedPhone, _ ->
            val targetId = pendingContactCustomerId
            pendingContactCustomerId = null
            val target = targetId?.let { id ->
                activePelangganList.find { it.id == id }
                    ?: effectivePelangganUiModels.find { it.pelanggan.id == id }?.pelanggan
            }
            if (target != null && !KaryawanScopeHelper.isSoftDeletedPelanggan(target)) {
                val duplicateOwner = PelangganAuditHelper.findDuplicatePhoneOwner(
                    noHp = normalizedPhone,
                    existingList = activePelangganList,
                    excludeId = target.id
                )
                if (duplicateOwner != null) {
                    duplicatePhoneConfirm = Triple(target, normalizedPhone, duplicateOwner)
                } else {
                    viewModel.updatePelangganNoHp(target, normalizedPhone)
                }
            }
        },
        onError = { errMsg ->
            pendingContactCustomerId = null
            viewModel.showSnackbar(errMsg)
        }
    )

    val startContactPickFor: (Pelanggan) -> Unit = { pelanggan ->
        pendingContactCustomerId = pelanggan.id
        launchContactPicker()
    }
    val customerSpendStats = remember(effectiveTransaksiList) {
        PelangganAuditHelper.buildCustomerSpendStats(effectiveTransaksiList)
    }
    val jadwalList = remember(activePelangganUiModels) {
        activePelangganUiModels
            .filter {
                it.crmPrediction.status == CrmOrderStatus.OVERDUE ||
                    it.crmPrediction.status == CrmOrderStatus.DUE_SOON
            }
            .sortedWith(
                compareByDescending<PelangganUiModel> { it.crmPrediction.priorityScore }
                    .thenBy { it.crmPrediction.daysUntilExpected ?: Int.MAX_VALUE }
            )
    }
    val kasbonCount = remember(activePelangganUiModels) {
        activePelangganUiModels.count { it.pelanggan.sisa_kasbon > 0 }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                            contactedTimestamps = crmContactedTimestamps,
                            isKaryawan = effectiveIsKaryawan,
                            isInitialDataLoaded = isInitialDataLoaded,
                            depotProfile = depotProfile,
                            isOwner = isOwnerProfile,
                            onRequireQuickFill = { quickFillPendingAction = it },
                            onOpenDetail = { detailPelangganId = it.id },
                            onEdit = { editingPelanggan = it },
                            onBayarKasbon = { payingKasbonPelanggan = it },
                            onDelete = { pelangganToDelete = it },
                            onRestore = { viewModel.restorePelanggan(it) },
                            onMarkContacted = { viewModel.markCrmCustomerContacted(it) },
                            onAddContactPhone = startContactPickFor
                        )
                        1 -> KasbonPelangganTab(
                            viewModel = viewModel,
                            pelangganList = activePelangganList,
                            transaksiList = effectiveTransaksiList,
                            isInitialDataLoaded = isInitialDataLoaded,
                            depotProfile = depotProfile,
                            isOwner = isOwnerProfile,
                            onRequireQuickFill = { quickFillPendingAction = it },
                            onOpenDetail = { detailPelangganId = it.id },
                            onBayarKasbon = { payingKasbonPelanggan = it },
                            onAddContactPhone = startContactPickFor
                        )
                        2 -> JadwalPelangganTab(
                            viewModel = viewModel,
                            jadwalUiModels = jadwalList,
                            kurirList = kurirList,
                            contactedTimestamps = crmContactedTimestamps,
                            onOpenDetail = { detailPelangganId = it.id },
                            onQuickOrder = { viewModel.openTransaksiForPelanggan(it.id) },
                            onMarkContacted = { viewModel.markCrmCustomerContacted(it) },
                            onClearContacted = { viewModel.clearCrmCustomerContacted(it) },
                            onAddContactPhone = startContactPickFor
                        )
                        3 -> PasifPelangganTab(
                            pelangganList = activePelangganList,
                            transaksiList = effectiveTransaksiList,
                            contactedTimestamps = crmContactedTimestamps,
                            onOpenDetail = { detailPelangganId = it.id },
                            onQuickOrder = { viewModel.openTransaksiForPelanggan(it.id) },
                            onMarkContacted = { viewModel.markCrmCustomerContacted(it) },
                            onClearContacted = { viewModel.clearCrmCustomerContacted(it) },
                            onAddContactPhone = startContactPickFor
                        )
                        4 -> TopRankingTab(
                            pelangganList = activePelangganList,
                            transaksiList = effectiveTransaksiList,
                            onOpenDetail = { detailPelangganId = it.id },
                            onAddContactPhone = startContactPickFor
                        )
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
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.pelanggan_add_cd)
                )
            }
        }
    }

    detailPelangganId?.let { targetId ->
        val detailUiModel = remember(effectivePelangganUiModels, targetId) {
            effectivePelangganUiModels.find { it.pelanggan.id == targetId }
        }
        LaunchedEffect(detailUiModel) {
            if (detailUiModel == null) {
                detailPelangganId = null
            }
        }
        if (detailUiModel != null) {
            val detailPelanggan = detailUiModel.pelanggan
            val customerTransactions = remember(effectiveTransaksiList, targetId) {
                effectiveTransaksiList.filter {
                    it.transaksi.id_pelanggan == targetId &&
                        !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi)
                }
            }
            val kasbonLogs by remember(targetId) {
                viewModel.getKasbonLogsByPelanggan(targetId)
            }.collectAsStateWithLifecycle(initialValue = emptyList())

            PelangganDetailBottomSheet(
                pelangganUiModel = detailUiModel,
                customerTransactions = customerTransactions,
                kasbonLogs = kasbonLogs,
                stats = customerSpendStats[targetId],
                kurirList = kurirList,
                onDismiss = { detailPelangganId = null },
                onPesanGalon = {
                    detailPelangganId = null
                    viewModel.openTransaksiForPelanggan(targetId)
                },
                onBayarKasbon = {
                    detailPelangganId = null
                    payingKasbonPelanggan = detailPelanggan
                },
                onEdit = {
                    detailPelangganId = null
                    editingPelanggan = detailPelanggan
                },
                onPrintTagihan = {
                    val unpaid = PelangganAuditHelper.filterActiveUnpaidKasbonNotes(
                        pelangganId = targetId,
                        transaksiList = effectiveTransaksiList
                    ).map { it.transaksi }
                    val receipt = SantoyaReceiptHelper.generateTagihanKasbon(detailPelanggan, unpaid, depotProfile)
                    viewModel.showReceiptDialog(
                        ActiveReceiptDialog(
                            title = "Tagihan Kasbon - ${detailPelanggan.nama}",
                            receiptText = receipt,
                            customerPhone = detailPelanggan.no_hp,
                            data = Pair(detailPelanggan, unpaid),
                            receiptBuilder = { profile ->
                                SantoyaReceiptHelper.generateTagihanKasbon(detailPelanggan, unpaid, profile)
                            }
                        )
                    )
                },
                onShareTagihan = {
                    val unpaid = PelangganAuditHelper.filterActiveUnpaidKasbonNotes(
                        pelangganId = targetId,
                        transaksiList = effectiveTransaksiList
                    ).map { it.transaksi }
                    val action: (DepotProfile) -> Unit = { activeProfile ->
                        val receipt = SantoyaReceiptHelper.generateTagihanKasbon(detailPelanggan, unpaid, activeProfile)
                        SantoyaReceiptHelper.shareReceiptText(
                            context = context,
                            text = receipt,
                            phoneNumber = detailPelanggan.no_hp,
                            depotName = activeProfile.effectiveDepotName()
                        )
                    }
                    if (isOwnerProfile && !depotProfile.hasConfiguredDepotName()) {
                        quickFillPendingAction = action
                    } else {
                        action(depotProfile)
                    }
                },
                onAddContactPhone = { startContactPickFor(detailPelanggan) },
                lastContactedMillis = crmContactedTimestamps[targetId],
                onMarkContacted = { viewModel.markCrmCustomerContacted(it) },
                onClearContacted = { viewModel.clearCrmCustomerContacted(it) }
            )
        }
    }

    if (showAddDialog) {
        PelangganFormDialog(
            title = stringResource(R.string.pelanggan_form_add_title),
            pelanggan = null,
            existingPelanggan = activePelangganList,
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
            title = stringResource(R.string.pelanggan_form_edit_title),
            pelanggan = target,
            existingPelanggan = activePelangganList,
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
            PelangganAuditHelper.filterActiveUnpaidKasbonNotes(target.id, effectiveTransaksiList)
        }
        PelunasanKasbonDialog(
            pelanggan = target,
            unpaidList = unpaidTargetList,
            onDismiss = { payingKasbonPelanggan = null },
            onConfirmBayar = { breakdown, metodeBayar ->
                viewModel.bayarKasbon(target.id, breakdown.receivedAmount, metodeBayar)
                val receipt = SantoyaReceiptHelper.generatePelunasanReceipt(
                    pelanggan = target,
                    nominalBayar = breakdown.effectivePaid,
                    sisaKasbon = breakdown.remainingAfterPay,
                    uangDiterima = breakdown.receivedAmount,
                    kembalian = breakdown.kembalian,
                    depotProfile = depotProfile
                )
                viewModel.showReceiptDialog(
                    ActiveReceiptDialog(
                        title = "Bukti Pelunasan Kasbon",
                        receiptText = receipt,
                        customerPhone = target.no_hp,
                        data = Triple(target, breakdown.effectivePaid, breakdown.remainingAfterPay),
                        receiptBuilder = { profile ->
                            SantoyaReceiptHelper.generatePelunasanReceipt(
                                pelanggan = target,
                                nominalBayar = breakdown.effectivePaid,
                                sisaKasbon = breakdown.remainingAfterPay,
                                uangDiterima = breakdown.receivedAmount,
                                kembalian = breakdown.kembalian,
                                depotProfile = profile
                            )
                        }
                    )
                )
                payingKasbonPelanggan = null
            }
        )
    }

    pelangganToDelete?.let { target ->
        DeletePelangganConfirmDialog(
            pelanggan = target,
            isKaryawan = effectiveIsKaryawan,
            activeKaryawanNama = effectiveActiveKaryawan?.nama,
            onDismiss = { pelangganToDelete = null },
            onConfirm = { isPermanentDelete ->
                if (isPermanentDelete) {
                    viewModel.permanentDeletePelanggan(target)
                } else {
                    viewModel.deletePelanggan(target)
                }
                pelangganToDelete = null
            }
        )
    }

    duplicatePhoneConfirm?.let { (target, normalizedPhone, existingOwner) ->
        AlertDialog(
            onDismissRequest = { duplicatePhoneConfirm = null },
            title = { Text(stringResource(R.string.pelanggan_contact_duplicate_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.pelanggan_contact_duplicate_body,
                        normalizedPhone,
                        existingOwner.nama,
                        target.nama
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePelangganNoHp(target, normalizedPhone)
                        duplicatePhoneConfirm = null
                    },
                    modifier = Modifier.testTag("btn_confirm_duplicate_phone")
                ) {
                    Text(stringResource(R.string.pelanggan_contact_duplicate_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { duplicatePhoneConfirm = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (quickFillPendingAction != null) {
        DepotQuickFillDialog(
            initialProfile = depotProfile,
            onDismiss = { quickFillPendingAction = null },
            onSaveAndContinue = { name, addr, phone ->
                val action = quickFillPendingAction
                quickFillPendingAction = null
                viewModel.quickSaveDepotProfile(name, addr, phone) { updated ->
                    action?.invoke(updated)
                }
            },
            onSkip = {
                val action = quickFillPendingAction
                quickFillPendingAction = null
                action?.invoke(depotProfile)
            }
        )
    }
}

@Composable
private fun SemuaPelangganTab(
    viewModel: SantoyaViewModel,
    pelangganUiModels: List<PelangganUiModel>,
    transaksiList: List<TransaksiDetail>,
    kurirList: List<Kurir> = emptyList(),
    contactedTimestamps: Map<Int, Long> = emptyMap(),
    isKaryawan: Boolean = false,
    isInitialDataLoaded: Boolean = true,
    depotProfile: DepotProfile = DepotProfile(),
    isOwner: Boolean = false,
    onRequireQuickFill: ((DepotProfile) -> Unit) -> Unit = {},
    onOpenDetail: (Pelanggan) -> Unit,
    onEdit: (Pelanggan) -> Unit,
    onBayarKasbon: (Pelanggan) -> Unit,
    onDelete: (Pelanggan) -> Unit,
    onRestore: (Pelanggan) -> Unit = {},
    onMarkContacted: (Int) -> Unit = {},
    onAddContactPhone: (Pelanggan) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(0) } // 0 = Semua, 1 = Aktif, 2 = Dihapus
    var selectedSort by remember { mutableStateOf(SemuaPelangganSort.NAMA_AZ) }

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

    val sortedBase = remember(baseModels, selectedSort) {
        PelangganAuditHelper.sortPelangganUiModels(baseModels, selectedSort)
    }
    val filtered = remember(searchQuery, sortedBase, kurirList) {
        viewModel.getFilteredPelangganUiModels(searchQuery, sortedBase, kurirList)
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

        PelangganFilterSortBarContainer {
            if (!isKaryawan && deletedModels.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SantoyaFilterPill(
                        label = stringResource(R.string.pelanggan_semua_filter_all, pelangganUiModels.size),
                        selected = filterMode == 0,
                        onClick = { filterMode = 0 },
                        centerContent = true,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_semua_all")
                    )
                    SantoyaFilterPill(
                        label = stringResource(R.string.pelanggan_semua_filter_active, activeModels.size),
                        selected = filterMode == 1,
                        onClick = { filterMode = 1 },
                        centerContent = true,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_semua_active")
                    )
                    SantoyaFilterPill(
                        label = stringResource(R.string.pelanggan_semua_filter_deleted, deletedModels.size),
                        selected = filterMode == 2,
                        onClick = { filterMode = 2 },
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedContentColor = MaterialTheme.colorScheme.error,
                        selectedBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                        centerContent = true,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_semua_deleted")
                    )
                }
            }

            PelangganSortRow(
                options = listOf(
                    PelangganSortOptionUi(
                        label = stringResource(R.string.pelanggan_semua_sort_nama),
                        selected = selectedSort == SemuaPelangganSort.NAMA_AZ,
                        testTag = "sort_semua_nama",
                        onClick = { selectedSort = SemuaPelangganSort.NAMA_AZ }
                    ),
                    PelangganSortOptionUi(
                        label = stringResource(R.string.pelanggan_semua_sort_kasbon),
                        selected = selectedSort == SemuaPelangganSort.KASBON_TERTINGGI,
                        testTag = "sort_semua_kasbon",
                        onClick = { selectedSort = SemuaPelangganSort.KASBON_TERTINGGI }
                    ),
                    PelangganSortOptionUi(
                        label = stringResource(R.string.pelanggan_semua_sort_terbaru),
                        selected = selectedSort == SemuaPelangganSort.TERBARU,
                        testTag = "sort_semua_terbaru",
                        onClick = { selectedSort = SemuaPelangganSort.TERBARU }
                    )
                )
            )
        }

        if (!isInitialDataLoaded && pelangganUiModels.isEmpty()) {
            SantoyaLoadingSkeleton(
                itemCount = 4,
                message = "Memuat daftar pelanggan..."
            )
        } else if (filtered.isEmpty()) {
            PelangganEmptyListState(
                icon = Icons.Outlined.People,
                title = if (searchQuery.isBlank()) "Belum ada pelanggan" else "Pelanggan tidak ditemukan",
                subtitle = if (searchQuery.isBlank()) {
                    "Ketuk tombol + untuk tambah pelanggan baru."
                } else {
                    stringResource(R.string.pelanggan_empty_search_subtitle, searchQuery.trim())
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
                        val isContacted = CrmFollowUpHelper.isContactedInCurrentCycle(
                            lastContactedMillis = contactedTimestamps[item.pelanggan.id],
                            lastOrderMillis = item.crmPrediction.lastOrderMillis
                        )
                        PelangganItemCard(
                            pelangganUiModel = item,
                            kurirList = kurirList,
                            searchQuery = searchQuery,
                            isSoftDeleted = isSoftDeleted,
                            deletedBy = deletedBy,
                            onClick = {
                                if (!isSoftDeleted) {
                                    onOpenDetail(item.pelanggan)
                                }
                            },
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
                                    val receipt = SantoyaReceiptHelper.generateTagihanKasbon(item.pelanggan, unpaid, depotProfile)
                                    viewModel.showReceiptDialog(
                                        ActiveReceiptDialog(
                                            title = "Tagihan Kasbon - ${item.pelanggan.nama}",
                                            receiptText = receipt,
                                            customerPhone = item.pelanggan.no_hp,
                                            data = Pair(item.pelanggan, unpaid),
                                            receiptBuilder = { profile ->
                                                SantoyaReceiptHelper.generateTagihanKasbon(item.pelanggan, unpaid, profile)
                                            }
                                        )
                                    )
                                }
                            } else null,
                            onShareKasbon = if (!isSoftDeleted && item.pelanggan.sisa_kasbon > 0) {
                                {
                                    val unpaid = unpaidTxByCustomer[item.pelanggan.id] ?: emptyList()
                                    val action: (DepotProfile) -> Unit = { activeProfile ->
                                        val receipt = SantoyaReceiptHelper.generateTagihanKasbon(item.pelanggan, unpaid, activeProfile)
                                        SantoyaReceiptHelper.shareReceiptText(
                                            context = context,
                                            text = receipt,
                                            phoneNumber = item.pelanggan.no_hp,
                                            depotName = activeProfile.effectiveDepotName()
                                        )
                                    }
                                    if (isOwner && !depotProfile.hasConfiguredDepotName()) {
                                        onRequireQuickFill(action)
                                    } else {
                                        action(depotProfile)
                                    }
                                }
                            } else null,
                            isContacted = isContacted,
                            onMarkContacted = onMarkContacted,
                            onAddContactPhone = { onAddContactPhone(item.pelanggan) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JadwalPelangganTab(
    viewModel: SantoyaViewModel,
    jadwalUiModels: List<PelangganUiModel>,
    kurirList: List<Kurir> = emptyList(),
    contactedTimestamps: Map<Int, Long> = emptyMap(),
    onOpenDetail: (Pelanggan) -> Unit,
    onQuickOrder: (Pelanggan) -> Unit,
    onMarkContacted: (Int) -> Unit,
    onClearContacted: (Int) -> Unit,
    onAddContactPhone: (Pelanggan) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val santoyaColors = MaterialTheme.santoyaColors
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CrmJadwalFilter.SEMUA) }

    val overdueCount = remember(jadwalUiModels) {
        jadwalUiModels.count { it.crmPrediction.status == CrmOrderStatus.OVERDUE }
    }
    val dueSoonCount = remember(jadwalUiModels) {
        jadwalUiModels.count { it.crmPrediction.status == CrmOrderStatus.DUE_SOON }
    }

    val filteredBySearch = remember(searchQuery, jadwalUiModels, kurirList) {
        viewModel.getFilteredPelangganUiModels(searchQuery, jadwalUiModels, kurirList)
    }
    val displayedModels = remember(filteredBySearch, selectedFilter, contactedTimestamps) {
        CrmFollowUpHelper.filterAndSortJadwal(
            jadwalUiModels = filteredBySearch,
            filter = selectedFilter,
            sort = CrmJadwalSort.PRIORITAS,
            contactedTimestamps = contactedTimestamps
        )
    }

    if (jadwalUiModels.isEmpty()) {
        PelangganEmptyListState(
            icon = Icons.Outlined.Schedule,
            title = stringResource(R.string.crm_jadwal_empty_title),
            subtitle = stringResource(R.string.crm_jadwal_empty_subtitle)
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            CompactPelangganSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Cari pelanggan yang jatuh tempo...",
                testTag = "input_search_jadwal_pelanggan",
                resultCount = if (searchQuery.isNotBlank()) displayedModels.size else null
            )

            // Filter Status (Single Row Ramping & Hemat Ruang)
            PelangganFilterSortBarContainer {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SantoyaFilterPill(
                        label = stringResource(R.string.crm_jadwal_filter_semua, jadwalUiModels.size),
                        selected = selectedFilter == CrmJadwalFilter.SEMUA,
                        onClick = { selectedFilter = CrmJadwalFilter.SEMUA },
                        centerContent = true,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_jadwal_semua")
                    )
                    SantoyaFilterPill(
                        label = stringResource(R.string.crm_jadwal_filter_terlambat, overdueCount),
                        selected = selectedFilter == CrmJadwalFilter.TERLAMBAT,
                        onClick = { selectedFilter = CrmJadwalFilter.TERLAMBAT },
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedContentColor = MaterialTheme.colorScheme.onErrorContainer,
                        selectedBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                        centerContent = true,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_jadwal_terlambat")
                    )
                    SantoyaFilterPill(
                        label = stringResource(R.string.crm_jadwal_filter_hari_ini, dueSoonCount),
                        selected = selectedFilter == CrmJadwalFilter.HARI_INI_BESOK,
                        onClick = { selectedFilter = CrmJadwalFilter.HARI_INI_BESOK },
                        selectedContainerColor = santoyaColors.warningContainer,
                        selectedContentColor = santoyaColors.onWarningContainer,
                        selectedBorderColor = santoyaColors.warning.copy(alpha = 0.45f),
                        centerContent = true,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_jadwal_hari_ini")
                    )
                }
            }

            if (displayedModels.isEmpty()) {
                val hasActiveSearch = searchQuery.isNotBlank()
                PelangganEmptyListState(
                    icon = Icons.Outlined.Schedule,
                    title = if (hasActiveSearch) {
                        "Pelanggan tidak ditemukan"
                    } else {
                        stringResource(R.string.crm_jadwal_empty_filter_title)
                    },
                    subtitle = if (hasActiveSearch) {
                        stringResource(R.string.pelanggan_empty_search_subtitle, searchQuery.trim())
                    } else {
                        stringResource(R.string.crm_jadwal_empty_filter_subtitle)
                    },
                    onResetSearch = {
                        searchQuery = ""
                        selectedFilter = CrmJadwalFilter.SEMUA
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
                        items(displayedModels, key = { it.pelanggan.id }) { item ->
                            val cust = item.pelanggan
                            val prediction = item.crmPrediction
                            val lastContactedMillis = contactedTimestamps[cust.id]
                            val isContacted = CrmFollowUpHelper.isContactedInCurrentCycle(
                                lastContactedMillis = lastContactedMillis,
                                lastOrderMillis = prediction.lastOrderMillis
                            )
                            val cardAccent = when {
                                isContacted -> santoyaColors.success
                                prediction.status == CrmOrderStatus.OVERDUE -> MaterialTheme.colorScheme.error
                                else -> santoyaColors.warning
                            }
                            val cleanAddr = KaryawanScopeHelper.cleanPelangganAlamat(cust.alamat)
                            val kurirName = cust.id_kurir_langganan?.let { kid ->
                                kurirList.find { it.id == kid }?.nama?.takeIf { it.isNotBlank() }
                            }

                            HydroBentoCard(
                                accentColor = cardAccent,
                                cornerRadius = SantoyaRadius.lg,
                                tintAlpha = if (isContacted) 0.04f else 0.07f,
                                showWaterRipple = !isContacted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clip(RoundedCornerShape(SantoyaRadius.lg))
                                    .clickable { onOpenDetail(cust) }
                                    .animateContentSize()
                                    .testTag("jadwal_card_${cust.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
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
                                            if (cleanAddr.isNotBlank()) {
                                                HighlightedSearchText(
                                                    text = cleanAddr,
                                                    query = searchQuery,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else if (cust.no_hp.isNotBlank()) {
                                                HighlightedSearchText(
                                                    text = cust.no_hp,
                                                    query = searchQuery,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (kurirName != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(SantoyaRadius.xs),
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = kurirName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        CrmOrderPredictionPill(
                                            prediction = prediction,
                                            pelanggan = cust,
                                            isContacted = isContacted,
                                            onFollowUpClick = {
                                                val followUpCtx = CrmFollowUpHelper.resolveFollowUpContext(prediction)
                                                val msg = CrmFollowUpHelper.buildContextualWhatsAppMessage(
                                                    customerName = cust.nama,
                                                    prediction = prediction,
                                                    followUpContext = followUpCtx
                                                )
                                                CrmFollowUpHelper.launchWhatsAppFollowUp(
                                                    context = context,
                                                    pelanggan = cust,
                                                    message = msg,
                                                    onMarkContacted = { onMarkContacted(cust.id) }
                                                )
                                            }
                                        )
                                        val avgDays = prediction.averageIntervalDays
                                        val avgGal = prediction.avgGallonsPerOrder
                                        if (avgDays != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = if (avgGal != null) {
                                                        stringResource(R.string.crm_jadwal_cycle_galon, avgDays, avgGal)
                                                    } else {
                                                        stringResource(R.string.crm_jadwal_cycle_only, avgDays)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        if (isContacted && lastContactedMillis != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = santoyaColors.successContainer.copy(alpha = 0.85f),
                                                border = BorderStroke(1.dp, santoyaColors.success.copy(alpha = 0.4f)),
                                                onClick = { onClearContacted(cust.id) },
                                                modifier = Modifier.testTag("badge_jadwal_contacted_${cust.id}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.CheckCircle,
                                                        contentDescription = null,
                                                        tint = santoyaColors.onSuccessContainer,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = stringResource(
                                                            R.string.crm_contacted_badge,
                                                            FormatUtils.formatTimeOnly(lastContactedMillis)
                                                        ),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                        fontWeight = FontWeight.Bold,
                                                        color = santoyaColors.onSuccessContainer
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Outlined.Close,
                                                        contentDescription = stringResource(R.string.crm_reset_contacted_cd),
                                                        tint = santoyaColors.onSuccessContainer,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Baris Aksi Cepat 1-Ketuk: WA Pengingat & Pesan Galon
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (cust.no_hp.isNotBlank()) {
                                            val waAction = {
                                                val followUpCtx = CrmFollowUpHelper.resolveFollowUpContext(prediction)
                                                val msg = CrmFollowUpHelper.buildContextualWhatsAppMessage(
                                                    customerName = cust.nama,
                                                    prediction = prediction,
                                                    followUpContext = followUpCtx
                                                )
                                                CrmFollowUpHelper.launchWhatsAppFollowUp(
                                                    context = context,
                                                    pelanggan = cust,
                                                    message = msg,
                                                    onMarkContacted = { onMarkContacted(cust.id) }
                                                )
                                            }
                                            if (isContacted) {
                                                OutlinedButton(
                                                    onClick = waAction,
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = SantoyaSize.touchTarget)
                                                        .testTag("btn_jadwal_wa_${cust.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Share,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = stringResource(R.string.crm_jadwal_btn_wa_again),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            } else {
                                                Button(
                                                    onClick = waAction,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (prediction.status == CrmOrderStatus.OVERDUE) {
                                                            MaterialTheme.colorScheme.error
                                                        } else {
                                                            santoyaColors.warning
                                                        },
                                                        contentColor = santoyaColors.onHero
                                                    ),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = SantoyaSize.touchTarget)
                                                        .testTag("btn_jadwal_wa_${cust.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Share,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = stringResource(R.string.crm_jadwal_btn_wa),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = { onAddContactPhone(cust) },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = SantoyaSize.touchTarget)
                                                    .testTag("btn_jadwal_add_contact_${cust.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ContactPhone,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.pelanggan_add_contact_short),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { onQuickOrder(cust) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .weight(0.85f)
                                                .heightIn(min = SantoyaSize.touchTarget)
                                                .testTag("btn_jadwal_pesan_${cust.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AddShoppingCart,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.crm_jadwal_btn_order),
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
}

@Composable
private fun KasbonPelangganTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>,
    isInitialDataLoaded: Boolean = true,
    depotProfile: DepotProfile = DepotProfile(),
    isOwner: Boolean = false,
    onRequireQuickFill: ((DepotProfile) -> Unit) -> Unit = {},
    onOpenDetail: (Pelanggan) -> Unit,
    onBayarKasbon: (Pelanggan) -> Unit,
    onAddContactPhone: (Pelanggan) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var sortOption by remember { mutableStateOf(KasbonSortOption.TERBANYAK) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedCustomerIds by remember { mutableStateOf(setOf<Int>()) }

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

        PelangganFilterSortBarContainer {
            PelangganSortRow(
                options = listOf(
                    PelangganSortOptionUi(
                        label = stringResource(R.string.pelanggan_kasbon_sort_terbanyak),
                        selected = sortOption == KasbonSortOption.TERBANYAK,
                        testTag = "sort_kasbon_terbanyak",
                        onClick = { sortOption = KasbonSortOption.TERBANYAK }
                    ),
                    PelangganSortOptionUi(
                        label = stringResource(R.string.pelanggan_kasbon_sort_terlama),
                        selected = sortOption == KasbonSortOption.TERLAMA,
                        testTag = "sort_kasbon_terlama",
                        onClick = { sortOption = KasbonSortOption.TERLAMA }
                    ),
                    PelangganSortOptionUi(
                        label = stringResource(R.string.pelanggan_kasbon_sort_terbaru),
                        selected = sortOption == KasbonSortOption.TERBARU,
                        testTag = "sort_kasbon_terbaru",
                        onClick = { sortOption = KasbonSortOption.TERBARU }
                    )
                )
            )
        }

        if (!isInitialDataLoaded && pelangganList.isEmpty()) {
            SantoyaLoadingSkeleton(
                itemCount = 3,
                message = "Memuat data tagihan kasbon..."
            )
        } else if (sortedKasbon.isEmpty()) {
            PelangganEmptyListState(
                emptyType = if (searchQuery.isBlank()) SantoyaEmptyType.KASBON else SantoyaEmptyType.PELANGGAN,
                icon = Icons.Outlined.HourglassEmpty,
                title = if (searchQuery.isBlank()) "Tidak ada tagihan kasbon" else "Pelanggan tidak ditemukan",
                subtitle = if (searchQuery.isBlank()) {
                    "Semua kasbon sudah lunas."
                } else {
                    stringResource(R.string.pelanggan_empty_search_subtitle, searchQuery.trim())
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
                                .clip(RoundedCornerShape(SantoyaRadius.lg))
                                .clickable { onOpenDetail(cust) }
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
                                                            .padding(horizontal = 6.dp, vertical = 4.dp)
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val unpaidTxList = unpaid.map { it.transaksi }
                                                val receipt = SantoyaReceiptHelper.generateTagihanKasbon(cust, unpaidTxList, depotProfile)
                                                viewModel.showReceiptDialog(
                                                    ActiveReceiptDialog(
                                                        title = "Tagihan Kasbon - ${cust.nama}",
                                                        receiptText = receipt,
                                                        customerPhone = cust.no_hp,
                                                        data = Pair(cust, unpaidTxList),
                                                        receiptBuilder = { profile ->
                                                            SantoyaReceiptHelper.generateTagihanKasbon(cust, unpaidTxList, profile)
                                                        }
                                                    )
                                                )
                                            },
                                            modifier = Modifier.size(SantoyaSize.touchTarget)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Print,
                                                contentDescription = stringResource(R.string.pelanggan_kasbon_print_cd),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val unpaidTxList = unpaid.map { it.transaksi }
                                                val action: (DepotProfile) -> Unit = { activeProfile ->
                                                    val receipt = SantoyaReceiptHelper.generateTagihanKasbon(cust, unpaidTxList, activeProfile)
                                                    SantoyaReceiptHelper.shareReceiptText(
                                                        context = context,
                                                        text = receipt,
                                                        phoneNumber = cust.no_hp,
                                                        depotName = activeProfile.effectiveDepotName()
                                                    )
                                                }
                                                if (isOwner && !depotProfile.hasConfiguredDepotName()) {
                                                    onRequireQuickFill(action)
                                                } else {
                                                    action(depotProfile)
                                                }
                                            },
                                            modifier = Modifier.size(SantoyaSize.touchTarget)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Share,
                                                contentDescription = stringResource(R.string.pelanggan_kasbon_share_cd),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    if (cust.no_hp.isBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        ) {
                                            IconButton(
                                                onClick = { onAddContactPhone(cust) },
                                                modifier = Modifier
                                                    .size(SantoyaSize.touchTarget)
                                                    .testTag("btn_kasbon_add_contact_${cust.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ContactPhone,
                                                    contentDescription = stringResource(R.string.pelanggan_add_contact_cd),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = { onBayarKasbon(cust) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = SantoyaSize.touchTarget)
                                    ) {
                                        Icon(imageVector = Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.pelanggan_kasbon_pay_btn),
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
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>,
    onOpenDetail: (Pelanggan) -> Unit,
    onAddContactPhone: (Pelanggan) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(CrmRankingPeriod.SEMUA) }
    var selectedSort by remember { mutableStateOf(CrmRankingSort.GALON) }

    val ranking = remember(pelangganList, transaksiList, selectedPeriod, selectedSort) {
        CrmFollowUpHelper.buildRankedCustomerEntries(
            pelangganList = pelangganList,
            transaksiList = transaksiList,
            period = selectedPeriod,
            sort = selectedSort
        )
    }
    val filteredRanking = remember(ranking, searchQuery) {
        if (searchQuery.isBlank()) {
            ranking
        } else {
            SearchMatcher.rankAndFilter(ranking, searchQuery) { entry ->
                val cust = entry.pelanggan
                val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(cust.alamat)
                listOf(
                    SearchField(cust.nama, weight = 24, isPrimaryName = true),
                    SearchField(cleanAlamat, weight = 10),
                    SearchField(cust.no_hp, weight = 14, isPhone = true),
                    SearchField("${entry.totalGalon} galon", weight = 6),
                    SearchField("${entry.totalGalon} gln", weight = 6)
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CompactPelangganSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Cari nama atau No. HP...",
            testTag = "input_search_ranking",
            resultCount = if (searchQuery.isNotBlank()) filteredRanking.size else null
        )

        PelangganFilterSortBarContainer {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SantoyaFilterPill(
                    label = stringResource(R.string.crm_ranking_period_semua),
                    selected = selectedPeriod == CrmRankingPeriod.SEMUA,
                    onClick = { selectedPeriod = CrmRankingPeriod.SEMUA },
                    centerContent = true,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("ranking_period_semua")
                )
                SantoyaFilterPill(
                    label = stringResource(R.string.crm_ranking_period_bulan_ini),
                    selected = selectedPeriod == CrmRankingPeriod.BULAN_INI,
                    onClick = { selectedPeriod = CrmRankingPeriod.BULAN_INI },
                    centerContent = true,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ranking_period_bulan_ini")
                )
                SantoyaFilterPill(
                    label = stringResource(R.string.crm_ranking_period_30_hari),
                    selected = selectedPeriod == CrmRankingPeriod.TIGA_PULUH_HARI,
                    onClick = { selectedPeriod = CrmRankingPeriod.TIGA_PULUH_HARI },
                    centerContent = true,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("ranking_period_30_hari")
                )
            }

            PelangganSortRow(
                options = listOf(
                    PelangganSortOptionUi(
                        label = stringResource(R.string.crm_ranking_sort_galon),
                        selected = selectedSort == CrmRankingSort.GALON,
                        testTag = "ranking_sort_galon",
                        onClick = { selectedSort = CrmRankingSort.GALON }
                    ),
                    PelangganSortOptionUi(
                        label = stringResource(R.string.crm_ranking_sort_omzet),
                        selected = selectedSort == CrmRankingSort.OMZET,
                        testTag = "ranking_sort_omzet",
                        onClick = { selectedSort = CrmRankingSort.OMZET }
                    )
                )
            )
        }

        if (ranking.isEmpty()) {
            PelangganEmptyListState(
                icon = Icons.Outlined.EmojiEvents,
                title = "Belum Ada Ranking Penjualan",
                subtitle = "Peringkat terisi otomatis setelah ada penjualan pada periode terpilih.",
                onResetSearch = if (selectedPeriod != CrmRankingPeriod.SEMUA) {
                    { selectedPeriod = CrmRankingPeriod.SEMUA }
                } else null
            )
        } else if (filteredRanking.isEmpty()) {
            PelangganEmptyListState(
                icon = Icons.Outlined.EmojiEvents,
                title = "Pelanggan tidak ditemukan",
                subtitle = stringResource(R.string.pelanggan_empty_search_subtitle, searchQuery.trim()),
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
                    items(filteredRanking, key = { it.pelanggan.id }) { entry ->
                        val rank = entry.rank
                        val cust = entry.pelanggan
                        val galonCount = entry.totalGalon

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
                                .clip(RoundedCornerShape(SantoyaRadius.lg))
                                .clickable { onOpenDetail(cust) }
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
                                                color = santoyaColors.onHero,
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${entry.totalOrder}x Order • ${FormatUtils.formatRupiah(entry.totalOmzet)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            entry.averageIntervalDays?.let { avgDays ->
                                                Surface(
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.crm_ranking_cycle_badge, avgDays),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontFeatureSettings = "tnum"),
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
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
                                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                if (cust.no_hp.isBlank()) {
                                    IconButton(
                                        onClick = { onAddContactPhone(cust) },
                                        modifier = Modifier
                                            .size(SantoyaSize.touchTarget)
                                            .testTag("btn_ranking_add_contact_${cust.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContactPhone,
                                            contentDescription = stringResource(R.string.pelanggan_add_contact_cd),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
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
private fun PasifPelangganTab(
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>,
    contactedTimestamps: Map<Int, Long> = emptyMap(),
    onOpenDetail: (Pelanggan) -> Unit,
    onQuickOrder: (Pelanggan) -> Unit,
    onMarkContacted: (Int) -> Unit,
    onClearContacted: (Int) -> Unit,
    onAddContactPhone: (Pelanggan) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val santoyaColors = MaterialTheme.santoyaColors
    var selectedDaysFilter by remember { mutableLongStateOf(7L) }
    var searchQuery by remember { mutableStateOf("") }

    val pasifList = remember(pelangganList, transaksiList, selectedDaysFilter) {
        CrmFollowUpHelper.buildPassiveCustomerEntries(
            pelangganList = pelangganList,
            transaksiList = transaksiList,
            minDays = selectedDaysFilter,
            segment = CrmPasifSegment.SEMUA,
            sort = CrmPasifSort.TERLAMA_PASIF
        )
    }
    val filteredPasif = remember(pasifList, searchQuery) {
        if (searchQuery.isBlank()) pasifList
        else {
            SearchMatcher.rankAndFilter(pasifList, searchQuery) { entry ->
                val cust = entry.pelanggan
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

        // Filter Rentang Hari Pasif (Single Row Ramping & Hemat Ruang)
        PelangganFilterSortBarContainer {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
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
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pasif_days_$days")
                        )
                    }
                }
            }
        }

        if (filteredPasif.isEmpty()) {
            val hasFilters = searchQuery.isNotBlank()
            PelangganEmptyListState(
                icon = Icons.Outlined.PersonOff,
                title = if (searchQuery.isBlank()) "Semua Pelanggan Aktif" else "Pelanggan tidak ditemukan",
                subtitle = if (searchQuery.isBlank()) {
                    "Tidak ada pelanggan pasif pada rentang > $selectedDaysFilter hari."
                } else {
                    stringResource(R.string.pelanggan_empty_search_subtitle, searchQuery.trim())
                },
                onResetSearch = if (hasFilters) {
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
                    items(filteredPasif, key = { it.pelanggan.id }) { entry ->
                        val cust = entry.pelanggan
                        val days = entry.daysPassive
                        val lastOrderMillis = entry.lastOrderMillis
                        val lastContactedMillis = contactedTimestamps[cust.id]
                        val isContacted = CrmFollowUpHelper.isContactedInCurrentCycle(
                            lastContactedMillis = lastContactedMillis,
                            lastOrderMillis = lastOrderMillis
                        )

                        HydroBentoCard(
                            accentColor = if (isContacted) santoyaColors.success else MaterialTheme.colorScheme.error,
                            cornerRadius = 16.dp,
                            tintAlpha = 0.05f,
                            showWaterRipple = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenDetail(cust) }
                                .animateContentSize()
                                .testTag("pasif_card_${cust.id}")
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
                                        gradientColors = if (isContacted) {
                                            listOf(santoyaColors.success, AccentCyan)
                                        } else {
                                            listOf(MaterialTheme.colorScheme.error, AccentRose)
                                        },
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
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            color = if (isContacted) santoyaColors.success else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (entry.totalOrder > 0 || isContacted) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (entry.totalOrder > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.crm_pasif_stats_summary,
                                                        entry.totalOrder,
                                                        entry.totalGalon
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        if (isContacted && lastContactedMillis != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = santoyaColors.successContainer.copy(alpha = 0.85f),
                                                border = BorderStroke(1.dp, santoyaColors.success.copy(alpha = 0.4f)),
                                                onClick = { onClearContacted(cust.id) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.CheckCircle,
                                                        contentDescription = null,
                                                        tint = santoyaColors.onSuccessContainer,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = stringResource(
                                                            R.string.crm_contacted_badge,
                                                            FormatUtils.formatTimeOnly(lastContactedMillis)
                                                        ),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                        fontWeight = FontWeight.Bold,
                                                        color = santoyaColors.onSuccessContainer
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Outlined.Close,
                                                        contentDescription = stringResource(R.string.crm_reset_contacted_cd),
                                                        tint = santoyaColors.onSuccessContainer,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (cust.no_hp.isNotBlank()) {
                                        val waAction = {
                                            val msg = CrmFollowUpHelper.buildContextualWhatsAppMessage(
                                                customerName = cust.nama,
                                                prediction = entry.prediction,
                                                followUpContext = CrmFollowUpContext.PASIF,
                                                passiveDays = days
                                            )
                                            CrmFollowUpHelper.launchWhatsAppFollowUp(
                                                context = context,
                                                pelanggan = cust,
                                                message = msg,
                                                onMarkContacted = { onMarkContacted(cust.id) }
                                            )
                                        }
                                        if (isContacted) {
                                            OutlinedButton(
                                                onClick = waAction,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = SantoyaSize.touchTarget)
                                                    .testTag("btn_pasif_wa_${cust.id}")
                                            ) {
                                                Icon(imageVector = Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.crm_jadwal_btn_wa_again),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = waAction,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = SantoyaSize.touchTarget)
                                                    .testTag("btn_pasif_wa_${cust.id}")
                                            ) {
                                                Icon(imageVector = Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.crm_pasif_btn_wa),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { onAddContactPhone(cust) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = SantoyaSize.touchTarget)
                                                .testTag("btn_pasif_add_contact_${cust.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContactPhone,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.pelanggan_add_contact_short),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { onQuickOrder(cust) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(0.85f)
                                            .heightIn(min = SantoyaSize.touchTarget)
                                            .testTag("btn_pasif_pesan_${cust.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AddShoppingCart,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.crm_jadwal_btn_order),
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
