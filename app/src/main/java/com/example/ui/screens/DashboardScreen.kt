package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.TransaksiDetail
import com.example.ui.components.BentoFeaturedTransaksiCard
import com.example.ui.components.DashboardDepotReminderCard
import com.example.ui.components.DashboardPerawatanReminderStrip
import com.example.ui.components.DepotProfileEditSheet
import com.example.ui.components.HydroWaveHeroCard
import com.example.ui.components.KaryawanVerticalMenuList
import com.example.ui.components.OwnerVerticalMenuList
import com.example.ui.components.SantoyaSyncInfoBottomSheet
import com.example.ui.components.WaveHeader
import com.example.ui.theme.SantoyaRadius
import com.example.util.FormatUtils
import com.example.util.GoogleSignInHelper
import com.example.util.KaryawanScopeHelper
import com.example.util.LaporanFinanceCalculator
import com.example.util.SyncConnectionStatus
import com.example.viewmodel.LaporanPeriode
import com.example.viewmodel.PelangganUiModel
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlin.math.roundToInt

/**
 * Layar Beranda utama Santoya RO:
 * - WaveHeader animatif berbasis Canvas API di bagian atas
 * - Hero Gelombang Air untuk ringkasan kas, laba bersih, piutang, dan volume galon
 * - Menu Utama vertikal rapi: 1 Kartu Transaksi Utama diikuti daftar menu vertikal seragam
 */
@Composable
fun DashboardScreen(
    viewModel: SantoyaViewModel,
    transaksiList: List<TransaksiDetail>? = null,
    pelangganList: List<Pelanggan>? = null,
    pengeluaranList: List<Pengeluaran>? = null,
    pelangganUiModels: List<PelangganUiModel>? = null,
    onNavigateToTransaksi: () -> Unit = { viewModel.navigateTo(SantoyaScreen.TRANSAKSI) },
    onNavigateToPelanggan: () -> Unit = { viewModel.navigateTo(SantoyaScreen.PELANGGAN) },
    @Suppress("UNUSED_PARAMETER") onNavigateToProduk: () -> Unit = { viewModel.navigateTo(SantoyaScreen.PRODUK) },
    @Suppress("UNUSED_PARAMETER") onNavigateToKurir: () -> Unit = { viewModel.navigateTo(SantoyaScreen.KURIR) },
    onNavigateToPengeluaran: () -> Unit = { viewModel.navigateTo(SantoyaScreen.PENGELUARAN) },
    onNavigateToLaporan: () -> Unit = { viewModel.navigateTo(SantoyaScreen.LAPORAN) },
    onNavigateToSettings: () -> Unit = { viewModel.navigateTo(SantoyaScreen.SETTINGS) },
    onNavigateToPerawatan: () -> Unit = { viewModel.navigateTo(SantoyaScreen.PERAWATAN) },
    onSignOut: (() -> Unit)? = null,
    onGoogleLinked: () -> Unit = {},
    isOfflineMode: Boolean = false,
    isKaryawan: Boolean = false,
    activeKaryawan: Kurir? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()

    val scopedTransactionsFromVm by viewModel.scopedTransaksi.collectAsStateWithLifecycle()
    val scopedPelangganFromVm by viewModel.scopedPelanggan.collectAsStateWithLifecycle()
    val allKasbonLogsFromVm by viewModel.allKasbonLogs.collectAsStateWithLifecycle()
    val scopedPelangganUiModelsFromVm by viewModel.scopedPelangganUiModels.collectAsStateWithLifecycle()
    val allPengeluaranFromVm by viewModel.allPengeluaran.collectAsStateWithLifecycle()
    val activeKaryawanFromVm by viewModel.activeKaryawan.collectAsStateWithLifecycle()
    val syncState by viewModel.realtimeSyncState.collectAsStateWithLifecycle()
    val depotProfile by viewModel.depotProfile.collectAsStateWithLifecycle()
    val perawatanReminders by viewModel.perawatanReminders.collectAsStateWithLifecycle()
    val isBusyAction by viewModel.isBusyAction.collectAsStateWithLifecycle()

    var showKaryawanSignOutDialog by remember { mutableStateOf(false) }
    var showSyncInfoSheet by remember { mutableStateOf(false) }
    var showDepotProfileSheet by remember { mutableStateOf(false) }

    val rawTransaksiList = transaksiList ?: scopedTransactionsFromVm
    val rawPelangganList = pelangganList ?: scopedPelangganFromVm
    val effectiveTransaksiList = remember(rawTransaksiList) {
        rawTransaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
    }
    val effectivePelangganList = remember(rawPelangganList) {
        rawPelangganList.filter { !KaryawanScopeHelper.isSoftDeletedPelanggan(it) }
    }
    val effectivePengeluaranList = pengeluaranList ?: if (isKaryawan) emptyList() else allPengeluaranFromVm
    val effectiveActiveKaryawan = activeKaryawan ?: activeKaryawanFromVm

    val todayStart = FormatUtils.getStartOfDay()
    val todayEnd = FormatUtils.getEndOfDay()
    val monthStart = FormatUtils.getStartOfMonth()

    val todayTransactions = remember(effectiveTransaksiList, todayStart, todayEnd) {
        effectiveTransaksiList
            .filter { it.transaksi.tanggal in todayStart..todayEnd }
            .sortedByDescending { it.transaksi.tanggal }
    }

    val monthTransactions = remember(effectiveTransaksiList, monthStart, todayEnd) {
        effectiveTransaksiList.filter {
            it.transaksi.tanggal in monthStart..todayEnd
        }
    }

    val karyawanMetrics = remember(effectiveTransaksiList, effectiveActiveKaryawan) {
        KaryawanScopeHelper.summarizeKaryawanDashboardMetrics(
            transaksiList = effectiveTransaksiList,
            activeKurir = effectiveActiveKaryawan
        )
    }

    // Ringkasan Arus Kas & Laba Bersih Hari Ini (Single Source of Truth)
    val todaySummary = remember(
        effectiveTransaksiList,
        effectivePengeluaranList,
        allKasbonLogsFromVm,
        effectivePelangganList,
        todayStart,
        todayEnd
    ) {
        LaporanFinanceCalculator.summarizePeriod(
            allTransaksi = effectiveTransaksiList,
            allPengeluaran = effectivePengeluaranList,
            allKasbonLogs = allKasbonLogsFromVm,
            allPelanggan = effectivePelangganList,
            startTime = todayStart,
            endTime = todayEnd
        )
    }
    val pendapatanLunasHariIni = todaySummary.totalKasMasuk
    val labaBersihHariIni = todaySummary.labaBersih
    val pengeluaranHariIni = todaySummary.totalPengeluaran
    val marginPercentText = remember(todaySummary) {
        if (todaySummary.totalKasMasuk > 0.0) {
            FormatUtils.formatPercent(todaySummary.marginLabaPersen, decimals = 0)
        } else {
            ""
        }
    }

    // Total Piutang / Kasbon Tersinkronisasi
    val effectiveUiModels = remember(
        pelangganUiModels,
        scopedPelangganUiModelsFromVm,
        effectivePelangganList,
        effectiveTransaksiList
    ) {
        pelangganUiModels
            ?: if (pelangganList == null && transaksiList == null) {
                scopedPelangganUiModelsFromVm
            } else {
                viewModel.getPelangganUiModels(effectivePelangganList, effectiveTransaksiList)
            }
    }
    val effectiveKasbonByCustomer = remember(effectiveUiModels) {
        effectiveUiModels.associate { it.pelanggan.id to it.pelanggan.sisa_kasbon }
    }
    val totalPiutangAktif = remember(effectiveKasbonByCustomer) {
        effectiveKasbonByCustomer.values.sum()
    }

    // Total Galon Hari Ini & Proyeksi Bulan Ini
    val totalGalonHariIni = remember(todayTransactions) {
        todayTransactions.sumOf { it.transaksi.jumlah }
    }
    val totalGalonBulanIni = remember(monthTransactions) {
        monthTransactions.sumOf { it.transaksi.jumlah }
    }
    val currentDayOfMonth = FormatUtils.getCurrentDayOfMonth().coerceAtLeast(1)
    val totalDaysInMonth = FormatUtils.getDaysInCurrentMonth()
    val estimasiGalonBulanIni = ((totalGalonBulanIni.toDouble() / currentDayOfMonth.toDouble()) * totalDaysInMonth).roundToInt()

    val customerCountText = remember(effectivePelangganList.size) {
        FormatUtils.formatNumber(effectivePelangganList.size)
    }

    val topBarTitle = remember(depotProfile) {
        if (depotProfile.hasConfiguredDepotName()) {
            depotProfile.effectiveDepotName()
        } else {
            null
        }
    }

    Scaffold(
        modifier = modifier.testTag("dashboard_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaveHeader(
                title = topBarTitle ?: stringResource(R.string.dashboard_brand_title),
                subtitle = stringResource(R.string.dashboard_brand_subtitle),
                syncState = syncState,
                isKaryawan = isKaryawan,
                onSyncClick = { showSyncInfoSheet = true },
                onSettingsClick = onNavigateToSettings,
                onSignOutClick = if (onSignOut != null) {
                    { showKaryawanSignOutDialog = true }
                } else {
                    null
                },
                isCompact = LocalDensity.current.fontScale >= 1.15f
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val density = LocalDensity.current
            val fontScale = density.fontScale
            val isCompact = maxHeight < 640.dp || maxWidth < 360.dp || fontScale >= 1.15f
            val isVeryNarrow = maxWidth < 340.dp
            val isExpandedLayout = maxWidth >= 720.dp

            val horizontalPadding = when {
                isVeryNarrow -> 10.dp
                isExpandedLayout -> 24.dp
                isCompact -> 12.dp
                else -> 16.dp
            }
            val topContentPadding = if (isCompact) 4.dp else 8.dp
            val bottomContentPadding = if (isCompact) 10.dp else 16.dp
            val menuSpacing = if (isCompact) 8.dp else 10.dp

            val hasRevokedBanner = syncState.status == SyncConnectionStatus.REVOKED
            val hasDepotReminder = !isKaryawan && depotProfile.isOwnerRole() && !depotProfile.hasConfiguredDepotName()

            // Hitung apakah seluruh Menu Utama vertikal pas satu layar tanpa scroll
            val minMenuTotalHeightDp = if (isKaryawan) {
                if (isCompact) 224.dp else 252.dp
            } else {
                if (isCompact) 292.dp else 326.dp
            }
            val navBarBottomDp = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
            var topSectionHeightPx by remember { mutableIntStateOf(0) }
            val topSectionHeightDp = with(density) { topSectionHeightPx.toDp() }
            val availableMenuHeightDp = maxHeight - navBarBottomDp - topContentPadding - bottomContentPadding - topSectionHeightDp
            val fitMenuToRemainingScreen = !isExpandedLayout &&
                topSectionHeightPx > 0 &&
                fontScale < 1.35f &&
                availableMenuHeightDp >= minMenuTotalHeightDp

            val scrollState = rememberScrollState()

            val heroSection: @Composable () -> Unit = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 10.dp)
                ) {
                    if (hasRevokedBanner) {
                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.lg),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.dashboard_revoked_banner),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    if (hasDepotReminder) {
                        DashboardDepotReminderCard(
                            onSetupClick = { showDepotProfileSheet = true }
                        )
                    }

                    HydroWaveHeroCard(
                        isKaryawan = isKaryawan,
                        isCompact = isCompact,
                        pendapatanLunasHariIni = pendapatanLunasHariIni,
                        labaBersihHariIni = labaBersihHariIni,
                        totalPiutangAktif = totalPiutangAktif,
                        totalGalonHariIni = totalGalonHariIni,
                        estimasiGalonBulanIni = estimasiGalonBulanIni,
                        karyawanMetrics = karyawanMetrics,
                        activeKaryawan = effectiveActiveKaryawan,
                        pelangganCount = effectivePelangganList.size,
                        onPrimaryHeroClick = {
                            if (isKaryawan) {
                                viewModel.setTransaksiTab(1)
                                onNavigateToTransaksi()
                            } else {
                                viewModel.setLaporanPeriode(LaporanPeriode.HARI_INI)
                                onNavigateToLaporan()
                            }
                        },
                        onNetProfitClick = {
                            viewModel.setLaporanPeriode(LaporanPeriode.HARI_INI)
                            onNavigateToLaporan()
                        },
                        onReceivablesClick = {
                            viewModel.setPelangganTab(1)
                            onNavigateToPelanggan()
                        },
                        onGalonTodayClick = {
                            viewModel.setTransaksiTab(0)
                            onNavigateToTransaksi()
                        },
                        onMonthEstimateClick = {
                            viewModel.setLaporanPeriode(LaporanPeriode.BULAN_INI)
                            onNavigateToLaporan()
                        },
                        onStaffCustomersClick = {
                            viewModel.setPelangganTab(0)
                            onNavigateToPelanggan()
                        }
                    )

                    if (!isKaryawan && perawatanReminders.isNotEmpty()) {
                        DashboardPerawatanReminderStrip(
                            reminders = perawatanReminders,
                            isCompact = isCompact,
                            onClick = onNavigateToPerawatan
                        )
                    }
                }
            }

            if (isExpandedLayout) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1080.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .navigationBarsPadding()
                        .padding(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            top = topContentPadding,
                            bottom = bottomContentPadding
                        ),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(0.95f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        heroSection()
                    }
                    Column(
                        modifier = Modifier.weight(1.05f),
                        verticalArrangement = Arrangement.spacedBy(menuSpacing)
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_menu_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        BentoFeaturedTransaksiCard(
                            todayGallons = totalGalonHariIni,
                            todayOrderCount = todayTransactions.size,
                            isCompact = isCompact,
                            onClick = {
                                viewModel.setTransaksiTab(0)
                                onNavigateToTransaksi()
                            }
                        )
                        if (!isKaryawan) {
                            OwnerVerticalMenuList(
                                customerCountText = customerCountText,
                                totalPiutang = totalPiutangAktif,
                                todayExpense = pengeluaranHariIni,
                                profitMarginPercentText = marginPercentText,
                                isCompact = isCompact,
                                fitToScreen = false,
                                onPelangganClick = {
                                    viewModel.setPelangganTab(0)
                                    onNavigateToPelanggan()
                                },
                                onPengeluaranClick = onNavigateToPengeluaran,
                                onLaporanClick = onNavigateToLaporan
                            )
                        } else {
                            KaryawanVerticalMenuList(
                                customerCountText = customerCountText,
                                totalPiutang = totalPiutangAktif,
                                galonHariIni = karyawanMetrics.galonHariIni,
                                pesananHariIni = karyawanMetrics.pesananHariIni,
                                isCompact = isCompact,
                                fitToScreen = false,
                                onPelangganClick = {
                                    viewModel.setPelangganTab(0)
                                    onNavigateToPelanggan()
                                },
                                onHistoryClick = {
                                    viewModel.setTransaksiTab(1)
                                    onNavigateToTransaksi()
                                }
                            )
                        }
                    }
                }
            } else {
                val rootColumnModifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .let { base ->
                        if (fitMenuToRemainingScreen) {
                            base.fillMaxHeight()
                        } else {
                            base.verticalScroll(scrollState)
                        }
                    }
                    .navigationBarsPadding()
                    .padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = topContentPadding,
                        bottom = bottomContentPadding
                    )

                Column(modifier = rootColumnModifier) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { size ->
                                if (topSectionHeightPx != size.height) {
                                    topSectionHeightPx = size.height
                                }
                            }
                    ) {
                        heroSection()

                        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 14.dp))

                        Text(
                            text = stringResource(R.string.dashboard_menu_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 10.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { base -> if (fitMenuToRemainingScreen) base.weight(1f) else base },
                        verticalArrangement = Arrangement.spacedBy(menuSpacing)
                    ) {
                        BentoFeaturedTransaksiCard(
                            todayGallons = totalGalonHariIni,
                            todayOrderCount = todayTransactions.size,
                            isCompact = isCompact,
                            onClick = {
                                viewModel.setTransaksiTab(0)
                                onNavigateToTransaksi()
                            },
                            modifier = if (fitMenuToRemainingScreen) {
                                Modifier.weight(if (isKaryawan) 0.36f else 0.28f)
                            } else {
                                Modifier
                            }
                        )

                        if (!isKaryawan) {
                            OwnerVerticalMenuList(
                                customerCountText = customerCountText,
                                totalPiutang = totalPiutangAktif,
                                todayExpense = pengeluaranHariIni,
                                profitMarginPercentText = marginPercentText,
                                isCompact = isCompact,
                                fitToScreen = fitMenuToRemainingScreen,
                                onPelangganClick = {
                                    viewModel.setPelangganTab(0)
                                    onNavigateToPelanggan()
                                },
                                onPengeluaranClick = onNavigateToPengeluaran,
                                onLaporanClick = onNavigateToLaporan,
                                modifier = if (fitMenuToRemainingScreen) Modifier.weight(0.72f) else Modifier
                            )
                        } else {
                            KaryawanVerticalMenuList(
                                customerCountText = customerCountText,
                                totalPiutang = totalPiutangAktif,
                                galonHariIni = karyawanMetrics.galonHariIni,
                                pesananHariIni = karyawanMetrics.pesananHariIni,
                                isCompact = isCompact,
                                fitToScreen = fitMenuToRemainingScreen,
                                onPelangganClick = {
                                    viewModel.setPelangganTab(0)
                                    onNavigateToPelanggan()
                                },
                                onHistoryClick = {
                                    viewModel.setTransaksiTab(1)
                                    onNavigateToTransaksi()
                                },
                                modifier = if (fitMenuToRemainingScreen) Modifier.weight(0.64f) else Modifier
                            )
                        }
                    }
                }
            }
        }
    }

    if (showKaryawanSignOutDialog && onSignOut != null) {
        AlertDialog(
            onDismissRequest = { showKaryawanSignOutDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dashboard_staff_sign_out_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(stringResource(R.string.dashboard_staff_sign_out_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showKaryawanSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.action_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showKaryawanSignOutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showSyncInfoSheet) {
        val currentUser = Firebase.auth.currentUser
        val currentUserId = currentUser?.uid
        val canSwitchToStaffWaiting = !isOfflineMode &&
            !KaryawanScopeHelper.isPrimaryOwnerEmail(currentUser?.email) &&
            (!isKaryawan || !KaryawanScopeHelper.isPrimaryOwnerEmail(syncState.ownerEmail))
        SantoyaSyncInfoBottomSheet(
            syncState = syncState,
            isOfflineLocalMode = isOfflineMode,
            onDismiss = { showSyncInfoSheet = false },
            onSyncNow = {
                currentUserId?.let { uid ->
                    viewModel.syncToCloud(context, uid)
                }
            },
            onConnectGoogleAccount = if (isOfflineMode) {
                {
                    GoogleSignInHelper.signIn(
                        context = context,
                        credentialManager = credentialManager,
                        coroutineScope = coroutineScope,
                        onLoadingChange = {},
                        onSuccess = onGoogleLinked,
                        onError = { msg -> viewModel.showSnackbar(msg) }
                    )
                }
            } else {
                null
            },
            onNavigateToSettings = if (!isKaryawan) {
                {
                    showSyncInfoSheet = false
                    onNavigateToSettings()
                }
            } else {
                null
            },
            onSwitchToStaffWaiting = if (canSwitchToStaffWaiting) {
                {
                    showSyncInfoSheet = false
                    viewModel.switchToStaffWaitingMode()
                }
            } else {
                null
            }
        )
    }

    if (showDepotProfileSheet) {
        val isOwnerProfile = !isKaryawan && depotProfile.isOwnerRole()
        DepotProfileEditSheet(
            profile = depotProfile,
            isOwner = isOwnerProfile,
            isSaving = isBusyAction,
            onDismiss = { showDepotProfileSheet = false },
            onSave = { owner, depot, phone, addr, footer, depotType, tankCapacity ->
                viewModel.saveDepotProfile(
                    ownerName = owner,
                    depotName = depot,
                    phoneNumber = phone,
                    address = addr,
                    receiptFooter = footer,
                    depotType = depotType,
                    tankCapacityLiters = tankCapacity
                ) { ok ->
                    if (ok) {
                        showDepotProfileSheet = false
                    }
                }
            }
        )
    }
}
