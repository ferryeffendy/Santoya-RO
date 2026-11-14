package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.companion.kzpmrw.R
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.TransaksiDetail
import com.example.ui.components.HydroBentoCard
import com.example.ui.components.HydroCustomerDropIcon
import com.example.ui.components.HydroExpenseFlowIcon
import com.example.ui.components.HydroGalonIcon
import com.example.ui.components.HydroGradientBadge
import com.example.ui.components.HydroReceiptNoteIcon
import com.example.ui.components.HydroReportChartIcon
import com.example.ui.components.RealtimeSyncStatusPill
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.components.SantoyaLoadingSkeleton
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.HeroGlowAmber
import com.example.ui.theme.HeroGlowAmberSoft
import com.example.ui.theme.HeroGlowCyan
import com.example.ui.theme.HeroGlowCyanSoft
import com.example.ui.theme.HeroGlowEmerald
import com.example.ui.theme.HeroGlowEmeraldSoft
import com.example.ui.theme.HeroGlowRose
import com.example.ui.theme.HeroGlowRoseSoft
import com.example.ui.theme.SantoyaDewMint
import com.example.ui.theme.SantoyaLeafGreen
import com.example.ui.theme.SantoyaMistBlue
import com.example.ui.theme.SantoyaNavy
import com.example.ui.theme.SantoyaNavyBorder
import com.example.ui.theme.SantoyaNavyPanel
import com.example.ui.theme.SantoyaWaterBlue
import com.example.ui.theme.pressClickEffect
import com.example.ui.theme.santoyaColors
import com.example.util.CrmOrderStatus
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.SantoyaReceiptHelper
import com.example.util.SyncConnectionStatus
import com.example.viewmodel.ActiveReceiptDialog
import com.example.viewmodel.LaporanPeriode
import com.example.viewmodel.PelangganUiModel
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SantoyaViewModel,
    transaksiList: List<TransaksiDetail>? = null,
    pelangganList: List<Pelanggan>? = null,
    pengeluaranList: List<Pengeluaran>? = null,
    pelangganUiModels: List<PelangganUiModel>? = null,
    onNavigateToTransaksi: () -> Unit = { viewModel.navigateTo(SantoyaScreen.TRANSAKSI) },
    onNavigateToPelanggan: () -> Unit = { viewModel.navigateTo(SantoyaScreen.PELANGGAN) },
    onNavigateToProduk: () -> Unit = { viewModel.navigateTo(SantoyaScreen.PRODUK) },
    onNavigateToKurir: () -> Unit = { viewModel.navigateTo(SantoyaScreen.KURIR) },
    onNavigateToPengeluaran: () -> Unit = { viewModel.navigateTo(SantoyaScreen.PENGELUARAN) },
    onNavigateToLaporan: () -> Unit = { viewModel.navigateTo(SantoyaScreen.LAPORAN) },
    onNavigateToSettings: () -> Unit = { viewModel.navigateTo(SantoyaScreen.SETTINGS) },
    onSignOut: (() -> Unit)? = null,
    isKaryawan: Boolean = false,
    activeKaryawan: Kurir? = null,
    modifier: Modifier = Modifier
) {
    val scopedTransactionsFromVm by viewModel.scopedTransaksi.collectAsStateWithLifecycle()
    val scopedPelangganFromVm by viewModel.scopedPelanggan.collectAsStateWithLifecycle()
    val scopedPelangganUiModelsFromVm by viewModel.scopedPelangganUiModels.collectAsStateWithLifecycle()
    val allPengeluaranFromVm by viewModel.allPengeluaran.collectAsStateWithLifecycle()
    val activeKaryawanFromVm by viewModel.activeKaryawan.collectAsStateWithLifecycle()
    val syncState by viewModel.realtimeSyncState.collectAsStateWithLifecycle()
    val isInitialDataLoaded by viewModel.isInitialDataLoaded.collectAsStateWithLifecycle()
    var showKaryawanSignOutDialog by remember { mutableStateOf(false) }

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
        effectiveTransaksiList.filter {
            it.transaksi.tanggal in todayStart..todayEnd
        }
    }

    val todayPengeluaran = remember(effectivePengeluaranList, todayStart, todayEnd) {
        effectivePengeluaranList.filter {
            it.tanggal in todayStart..todayEnd
        }
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

    // 1. Pendapatan Tunai Hari Ini (termasuk Uang Muka/DP dari transaksi Bayar Sebagian)
    val pendapatanLunasHariIni = remember(todayTransactions) {
        todayTransactions.sumOf {
            if (it.transaksi.status.equals("Lunas", ignoreCase = true)) {
                it.transaksi.total_harga
            } else {
                it.transaksi.uangMuka.toDouble()
            }
        }
    }

    // 2. Laba Bersih Hari Ini (Pendapatan Tunai - Pengeluaran Operasional - Komisi Kurir)
    val totalKomisiHariIni = remember(todayTransactions) {
        todayTransactions.sumOf { it.transaksi.total_komisi }
    }
    val totalPengeluaranHariIni = remember(todayPengeluaran) {
        todayPengeluaran.sumOf { it.nominal }
    }
    val labaBersihHariIni = pendapatanLunasHariIni - totalPengeluaranHariIni - totalKomisiHariIni

    // 3. Total Piutang / Kasbon Tersinkronisasi
    val effectiveUiModels = remember(pelangganUiModels, scopedPelangganUiModelsFromVm, effectivePelangganList, effectiveTransaksiList) {
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

    // 4. Total Galon Hari Ini
    val totalGalonHariIni = remember(todayTransactions) {
        todayTransactions.sumOf { it.transaksi.jumlah }
    }

    // 5. Estimasi Galon Bulan Ini (Proyeksi: Total Galon Bulan Ini / Tgl Hari Ini * Total Hari Bulan Ini)
    val totalGalonBulanIni = remember(monthTransactions) {
        monthTransactions.sumOf { it.transaksi.jumlah }
    }
    val currentDayOfMonth = FormatUtils.getCurrentDayOfMonth().coerceAtLeast(1)
    val totalDaysInMonth = FormatUtils.getDaysInCurrentMonth()
    val estimasiGalonBulanIni = ((totalGalonBulanIni.toDouble() / currentDayOfMonth.toDouble()) * totalDaysInMonth).roundToInt()

    // 6. CRM Jadwal Order Pelanggan (Due Soon / Overdue)
    val crmJadwalCustomers = remember(effectiveUiModels) {
        effectiveUiModels.filter {
            it.crmPrediction.status == CrmOrderStatus.OVERDUE ||
                it.crmPrediction.status == CrmOrderStatus.DUE_SOON
        }
    }

    // 7. Daftar Transaksi Terbaru (Utamakan Hari Ini, Fallback ke 4 Transaksi Terakhir Lintas Hari)
    val recentTransactionsToShow = remember(todayTransactions, effectiveTransaksiList) {
        if (todayTransactions.isNotEmpty()) {
            todayTransactions.take(4)
        } else {
            effectiveTransaksiList.take(4)
        }
    }
    val isShowingTodayTransactions = todayTransactions.isNotEmpty()

    Scaffold(
        modifier = modifier.testTag("dashboard_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_santoya),
                                contentDescription = "Logo Santoya",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "SANTOYA RO",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Kasir & Antar Galon",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            RealtimeSyncStatusPill(
                                syncState = syncState,
                                modifier = Modifier.testTag("dashboard_sync_pill")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (!isKaryawan) {
                                IconButton(
                                    onClick = onNavigateToSettings,
                                    modifier = Modifier.testTag("dashboard_settings_button")
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Pengaturan",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }
                            } else if (onSignOut != null) {
                                IconButton(
                                    onClick = { showKaryawanSignOutDialog = true },
                                    modifier = Modifier.testTag("dashboard_karyawan_sign_out_button")
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Logout,
                                                contentDescription = "Keluar Akun",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (syncState.status == SyncConnectionStatus.REVOKED) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Akses akun Anda sedang dimatikan pemilik. Hubungi pemilik untuk mengaktifkannya lagi.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // 1. Solid Deep Hydro Navy Hero Card (Zero Gradient, Zero Glassmorphism, Organic Logo Curve)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = SantoyaNavy),
                    border = BorderStroke(1.dp, SantoyaNavyBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SantoyaNavy)
                            .drawBehind {
                                // Minimalist organic water droplet & leaf vein line art in top-right corner
                                val w = size.width
                                val h = size.height
                                val curve1 = Path().apply {
                                    moveTo(w * 0.72f, 0f)
                                    cubicTo(w * 0.76f, h * 0.28f, w * 0.88f, h * 0.36f, w, h * 0.38f)
                                }
                                drawPath(
                                    path = curve1,
                                    color = SantoyaWaterBlue.copy(alpha = 0.18f),
                                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                                )
                                val curve2 = Path().apply {
                                    moveTo(w * 0.82f, 0f)
                                    quadraticTo(w * 0.86f, h * 0.22f, w, h * 0.26f)
                                }
                                drawPath(
                                    path = curve2,
                                    color = SantoyaLeafGreen.copy(alpha = 0.22f),
                                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Top Section: Primary Revenue Hero
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressClickEffect(onClick = {
                                        if (isKaryawan) {
                                            viewModel.setTransaksiTab(1)
                                            onNavigateToTransaksi()
                                        } else {
                                            viewModel.setLaporanPeriode(LaporanPeriode.HARI_INI)
                                            onNavigateToLaporan()
                                        }
                                    }),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = HeroGlowCyanSoft,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isKaryawan) "SETORAN TUNAI HARI INI" else "PENDAPATAN TUNAI HARI INI",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.7.sp,
                                            color = Color.White.copy(alpha = 0.78f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    SantoyaAdaptiveCurrencyText(
                                        amount = if (isKaryawan) karyawanMetrics.setoranTunaiHariIni else pendapatanLunasHariIni,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        minFontSize = 18.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = SantoyaNavyPanel,
                                        border = BorderStroke(1.dp, SantoyaNavyBorder)
                                    ) {
                                        Text(
                                            text = FormatUtils.formatDateUi(System.currentTimeMillis()),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                    if (isKaryawan && effectiveActiveKaryawan != null) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = SantoyaLeafGreen,
                                            border = BorderStroke(1.dp, HeroGlowEmeraldSoft)
                                        ) {
                                            Text(
                                                text = effectiveActiveKaryawan.nama,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (!isKaryawan) {
                                // Middle Section (Pemilik): Solid Sub-Panels (Laba Bersih & Total Piutang)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Laba Bersih Hari Ini
                                    val isPositiveProfit = labaBersihHariIni >= 0.0
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .pressClickEffect(onClick = {
                                                viewModel.setLaporanPeriode(LaporanPeriode.HARI_INI)
                                                onNavigateToLaporan()
                                            }),
                                        shape = RoundedCornerShape(16.dp),
                                        color = SantoyaNavyPanel,
                                        border = BorderStroke(1.dp, SantoyaNavyBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isPositiveProfit) SantoyaLeafGreen else HeroGlowRose,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "LABA BERSIH",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.78f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            SantoyaAdaptiveCurrencyText(
                                                amount = labaBersihHariIni,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = if (isPositiveProfit) HeroGlowEmeraldSoft else HeroGlowRoseSoft,
                                                minFontSize = 11.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = "Kas masuk dikurangi biaya & komisi",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Total Piutang / Kasbon
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .pressClickEffect(onClick = {
                                                viewModel.setPelangganTab(1)
                                                onNavigateToPelanggan()
                                            }),
                                        shape = RoundedCornerShape(16.dp),
                                        color = SantoyaNavyPanel,
                                        border = BorderStroke(1.dp, SantoyaNavyBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = HeroGlowAmber,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.HourglassEmpty,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "TOTAL PIUTANG",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.78f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            SantoyaAdaptiveCurrencyText(
                                                amount = totalPiutangAktif,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = HeroGlowAmberSoft,
                                                minFontSize = 11.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = "Cek daftar kasbon →",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Bottom Section (Pemilik): Solid Production Volume & Monthly Projection Bar
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = SantoyaNavyPanel,
                                    border = BorderStroke(1.dp, SantoyaNavyBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Total Galon Hari Ini
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .pressClickEffect(onClick = {
                                                    viewModel.setTransaksiTab(0)
                                                    onNavigateToTransaksi()
                                                }),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = SantoyaWaterBlue,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.WaterDrop,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "GALON HARI INI",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.72f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text(
                                                        text = FormatUtils.formatNumber(totalGalonHariIni),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Galon",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White.copy(alpha = 0.75f),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Solid Vertical Divider
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .width(1.dp)
                                                .height(34.dp)
                                                .background(SantoyaNavyBorder)
                                        )

                                        // Estimasi Galon Bulan Ini
                                        Row(
                                            modifier = Modifier
                                                .weight(1.15f)
                                                .pressClickEffect(onClick = {
                                                    viewModel.setLaporanPeriode(LaporanPeriode.BULAN_INI)
                                                    onNavigateToLaporan()
                                                }),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = SantoyaLeafGreen,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.AutoGraph,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "ESTIMASI BULAN INI",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.72f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text(
                                                        text = FormatUtils.formatNumber(estimasiGalonBulanIni),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Black,
                                                        color = HeroGlowEmeraldSoft,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Galon",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White.copy(alpha = 0.75f),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Middle Section (Karyawan): Rekap Galon & Komisi Hari Ini vs Bulan Ini
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("card_komisi_hari_ini")
                                            .pressClickEffect(onClick = {
                                                viewModel.setTransaksiTab(1)
                                                onNavigateToTransaksi()
                                            }),
                                        shape = RoundedCornerShape(16.dp),
                                        color = SantoyaNavyPanel,
                                        border = BorderStroke(1.dp, SantoyaNavyBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = HeroGlowEmerald,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.WaterDrop,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "KOMISI HARI INI",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.78f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            SantoyaAdaptiveCurrencyText(
                                                amount = karyawanMetrics.komisiHariIni,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = HeroGlowEmeraldSoft,
                                                minFontSize = 11.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = "${karyawanMetrics.galonHariIni} Galon • ${karyawanMetrics.pesananHariIni} Order",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White.copy(alpha = 0.75f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("card_komisi_bulan_ini")
                                            .pressClickEffect(onClick = {
                                                viewModel.setTransaksiTab(1)
                                                onNavigateToTransaksi()
                                            }),
                                        shape = RoundedCornerShape(16.dp),
                                        color = SantoyaNavyPanel,
                                        border = BorderStroke(1.dp, SantoyaNavyBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = HeroGlowCyan,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.AutoGraph,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "KOMISI BULAN INI",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.78f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            SantoyaAdaptiveCurrencyText(
                                                amount = karyawanMetrics.komisiBulanIni,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = HeroGlowCyanSoft,
                                                minFontSize = 11.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = "${karyawanMetrics.galonBulanIni} Galon • ${karyawanMetrics.pesananBulanIni} Order",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White.copy(alpha = 0.75f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Bottom Section (Karyawan): Piutang Pelanggan Saya & Total Pelanggan Saya
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = SantoyaNavyPanel,
                                    border = BorderStroke(1.dp, SantoyaNavyBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .pressClickEffect(onClick = {
                                                    viewModel.setPelangganTab(1)
                                                    onNavigateToPelanggan()
                                                }),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = HeroGlowAmber,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.HourglassEmpty,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "PIUTANG PELANGGAN",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.72f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                SantoyaAdaptiveCurrencyText(
                                                    amount = totalPiutangAktif,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = HeroGlowAmberSoft,
                                                    minFontSize = 11.sp,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .width(1.dp)
                                                .height(34.dp)
                                                .background(SantoyaNavyBorder)
                                        )

                                        Row(
                                            modifier = Modifier
                                                .weight(0.95f)
                                                .pressClickEffect(onClick = {
                                                    viewModel.setPelangganTab(0)
                                                    onNavigateToPelanggan()
                                                }),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = HeroGlowCyan,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.People,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "PELANGGAN SAYA",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.72f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text(
                                                        text = "${effectivePelangganList.size}",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Orang",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White.copy(alpha = 0.75f),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(bottom = 2.dp)
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

            // 2. Smart CRM Order Alert Banner
            if (crmJadwalCustomers.isNotEmpty()) {
                item {
                    val overdueCount = crmJadwalCustomers.count { it.crmPrediction.status == CrmOrderStatus.OVERDUE }
                    val santoyaColors = MaterialTheme.santoyaColors
                    HydroBentoCard(
                        accentColor = santoyaColors.warning,
                        cornerRadius = 18.dp,
                        tintAlpha = 0.14f,
                        onClick = {
                            viewModel.setPelangganTab(2) // Tab Jadwal
                            onNavigateToPelanggan()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HydroGradientBadge(
                                    gradientColors = listOf(AccentAmber, santoyaColors.warning),
                                    size = 38.dp,
                                    cornerRadius = 12.dp
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${crmJadwalCustomers.size} Pelanggan Perlu Pengingat",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (overdueCount > 0) {
                                            "$overdueCount terlambat • Ketuk untuk kirim pengingat"
                                        } else {
                                            "Ada jadwal isi ulang galon hari ini"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = santoyaColors.warning,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = santoyaColors.warningContainer,
                                border = BorderStroke(1.dp, santoyaColors.warning.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Lihat Jadwal",
                                    tint = santoyaColors.warning,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Menu Navigasi Utama — Bento Editorial Asimetris (1 Kartu Lebar + 2 Kartu Vertikal Asimetris + 1 Kartu Penuh)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Menu Utama",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Akses Cepat Depot",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // BARIS 1: KARTU LEBAR UTAMA — TRANSAKSI (Mist Water Blue Surface + Water Blue & Leaf Line Art)
                    EditorialPrimaryWideBentoCard(
                        title = "Transaksi",
                        subtitle = "Catat penjualan galon, pengantaran & pembayaran",
                        badgeTag = "+ Nota Baru",
                        surfaceTint = SantoyaMistBlue,
                        accentColor = SantoyaWaterBlue,
                        testTag = "nav_transaksi_button",
                        onClick = {
                            viewModel.setTransaksiTab(0)
                            onNavigateToTransaksi()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HydroGalonIcon(
                            tint = SantoyaWaterBlue,
                            leafAccent = SantoyaLeafGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    if (!isKaryawan) {
                        // BARIS 2: 2 KARTU HORIZONTAL BERDAMPINGAN (Ikon di Kiri, Teks di Kanan)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EditorialCompactHorizontalBentoCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                title = "Pelanggan",
                                subtitle = "${effectivePelangganList.size} Profil & Kasbon",
                                iconContainerColor = SantoyaDewMint,
                                accentColor = SantoyaLeafGreen,
                                testTag = "nav_pelanggan_button",
                                onClick = {
                                    viewModel.setPelangganTab(0)
                                    onNavigateToPelanggan()
                                }
                            ) {
                                HydroCustomerDropIcon(
                                    tint = SantoyaLeafGreen,
                                    waterAccent = SantoyaWaterBlue,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            EditorialCompactHorizontalBentoCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                title = "Pengeluaran",
                                subtitle = "Catat biaya depot",
                                iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                                accentColor = MaterialTheme.colorScheme.error,
                                testTag = "nav_pengeluaran_button",
                                onClick = onNavigateToPengeluaran
                            ) {
                                HydroExpenseFlowIcon(
                                    tint = MaterialTheme.colorScheme.error,
                                    leafAccent = SantoyaWaterBlue,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }

                        // BARIS 3: 1 KARTU PENUH — LAPORAN & ANALITIK
                        EditorialFullBannerBentoCard(
                            title = "Laporan",
                            subtitle = "Evaluasi omzet, komisi kurir & laba bersih",
                            badgeTag = "Analitik",
                            iconContainerColor = SantoyaMistBlue,
                            accentColor = SantoyaNavy,
                            testTag = "nav_laporan_button",
                            onClick = onNavigateToLaporan,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HydroReportChartIcon(
                                tint = SantoyaNavy,
                                leafAccent = SantoyaLeafGreen,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    } else {
                        // Mode Karyawan: Kartu Pelanggan Penuh di bawah Transaksi
                        EditorialFullBannerBentoCard(
                            title = "Pelanggan",
                            subtitle = "${effectivePelangganList.size} Profil pelanggan & catatan kasbon",
                            badgeTag = "Data Pelanggan",
                            iconContainerColor = SantoyaDewMint,
                            accentColor = SantoyaLeafGreen,
                            testTag = "nav_pelanggan_button",
                            onClick = {
                                viewModel.setPelangganTab(0)
                                onNavigateToPelanggan()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HydroCustomerDropIcon(
                                tint = SantoyaLeafGreen,
                                waterAccent = SantoyaWaterBlue,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }
            }

            // 4. Aktivitas Transaksi Terbaru
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isShowingTodayTransactions) {
                            "Transaksi Hari Ini"
                        } else {
                            "Transaksi Terakhir"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (effectiveTransaksiList.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.pressClickEffect(onClick = {
                                viewModel.setTransaksiTab(1)
                                onNavigateToTransaksi()
                            })
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lihat Semua",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!isInitialDataLoaded && recentTransactionsToShow.isEmpty()) {
                item {
                    SantoyaLoadingSkeleton(
                        itemCount = 2,
                        message = "Memuat aktivitas terbaru..."
                    )
                }
            } else if (recentTransactionsToShow.isEmpty()) {
                item {
                    SantoyaEmptyState(
                        illustrationRes = R.drawable.img_empty_transaksi,
                        title = "Belum ada transaksi",
                        subtitle = if (isKaryawan) {
                            "Belum ada penjualan. Catat orderan pertama hari ini."
                        } else {
                            "Belum ada penjualan. Yuk catat transaksi pertama."
                        },
                        compact = true,
                        primaryActionLabel = "Buat Transaksi",
                        primaryActionIcon = Icons.Default.Add,
                        onPrimaryAction = {
                            viewModel.setTransaksiTab(0)
                            onNavigateToTransaksi()
                        }
                    )
                }
            } else {
                items(recentTransactionsToShow, key = { it.transaksi.id }) { detail ->
                    val isKasbon = detail.transaksi.status.equals("Kasbon", ignoreCase = true)
                    val metodeDisplay = if (isKasbon) {
                        "Belum dibayar"
                    } else {
                        val m = detail.transaksi.metode_bayar.trim()
                        if (m.isBlank() || m == "-") "Cash" else m
                    }
                    val subInfoParts = mutableListOf("${detail.transaksi.jumlah} Galon", metodeDisplay)
                    if (detail.kurir != null) {
                        subInfoParts.add(detail.kurir.nama)
                    }
                    if (!isShowingTodayTransactions) {
                        subInfoParts.add(FormatUtils.formatDateUi(detail.transaksi.tanggal))
                    }

                    val canQuickOrder = detail.pelanggan != null &&
                        !KaryawanScopeHelper.isSoftDeletedPelanggan(detail.pelanggan)
                    val santoyaColors = MaterialTheme.santoyaColors
                    val statusAccent = if (isKasbon) santoyaColors.warning else SantoyaLeafGreen

                    HydroBentoCard(
                        accentColor = statusAccent,
                        cornerRadius = 16.dp,
                        tintAlpha = 0.05f,
                        showWaterRipple = false,
                        onClick = if (canQuickOrder) {
                            {
                                viewModel.startQuickOrder(
                                    pelangganId = detail.pelanggan!!.id,
                                    produkId = detail.transaksi.id_produk,
                                    jumlah = detail.transaksi.jumlah
                                )
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.5.dp)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(statusAccent)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isKasbon) santoyaColors.warningContainer else SantoyaMistBlue,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isKasbon) santoyaColors.warning.copy(alpha = 0.35f) else SantoyaWaterBlue.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        HydroGalonIcon(
                                            tint = if (isKasbon) santoyaColors.warning else SantoyaWaterBlue,
                                            leafAccent = if (isKasbon) santoyaColors.warning else SantoyaLeafGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = detail.pelanggan?.nama ?: "Pelanggan Umum",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isKasbon) santoyaColors.warningContainer else SantoyaDewMint
                                        ) {
                                            Text(
                                                text = detail.transaksi.status.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isKasbon) santoyaColors.onWarningContainer else SantoyaLeafGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subInfoParts.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isKasbon) santoyaColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (canQuickOrder) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Replay,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Ketuk untuk Order Lagi (${detail.transaksi.jumlah} galon)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SantoyaAdaptiveCurrencyText(
                                    amount = detail.transaksi.total_harga,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    minFontSize = 11.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.widthIn(max = 118.dp)
                                )
                                IconButton(
                                    onClick = {
                                        val custKasbon = effectiveKasbonByCustomer[detail.transaksi.id_pelanggan]
                                            ?: effectivePelangganList.find { it.id == detail.transaksi.id_pelanggan }?.sisa_kasbon
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
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SantoyaMistBlue,
                                        border = BorderStroke(1.dp, SantoyaWaterBlue.copy(alpha = 0.25f)),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            HydroReceiptNoteIcon(
                                                tint = SantoyaWaterBlue,
                                                modifier = Modifier.size(17.dp)
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

    if (showKaryawanSignOutDialog && onSignOut != null) {
        AlertDialog(
            onDismissRequest = { showKaryawanSignOutDialog = false },
            title = { Text("Keluar Akun Karyawan", fontWeight = FontWeight.Bold) },
            text = { Text("Keluar dari akun Google ini sekarang?") },
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
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKaryawanSignOutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

/**
 * 1. KARTU LEBAR UTAMA (Full-Width Primary Editorial Bento)
 * Menggunakan permukaan Biru Muda Sejuk (#E8F3FC) solid dengan aksen Biru Air & Hijau Daun.
 */
@Composable
private fun EditorialPrimaryWideBentoCard(
    title: String,
    subtitle: String,
    badgeTag: String,
    surfaceTint: Color,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = surfaceTint,
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.42f)),
        shadowElevation = 0.dp,
        modifier = modifier
            .testTag(testTag)
            .pressClickEffect(pressedScale = 0.98f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val leafArc = Path().apply {
                        moveTo(w * 0.80f, h)
                        quadraticTo(w * 0.88f, h * 0.45f, w, h * 0.25f)
                    }
                    drawPath(
                        path = leafArc,
                        color = SantoyaLeafGreen.copy(alpha = 0.16f),
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        iconContent()
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SantoyaNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SantoyaNavy.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = accentColor
            ) {
                Text(
                    text = badgeTag,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * 2. KARTU HORIZONTAL 2 KOLOM (Pelanggan & Pengeluaran)
 * Tata letak sejajar horizontal (ikon di kiri, judul & subjudul di kanan) selaras dengan kartu Laporan.
 */
@Composable
private fun EditorialCompactHorizontalBentoCard(
    title: String,
    subtitle: String,
    iconContainerColor: Color,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = 18.dp,
        tintAlpha = 0.04f,
        showWaterRipple = true,
        onClick = onClick,
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconContainerColor,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    iconContent()
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 3. KARTU PENUH EDITORIAL (Full-Width Bottom Banner Bento — Laporan)
 * Tata letak horizontal bersih dengan ikon Line Art di kiri, deskripsi di tengah, dan lencana/panah di kanan.
 */
@Composable
private fun EditorialFullBannerBentoCard(
    title: String,
    subtitle: String,
    badgeTag: String,
    iconContainerColor: Color,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = 18.dp,
        tintAlpha = 0.05f,
        showWaterRipple = true,
        onClick = onClick,
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = iconContainerColor,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        iconContent()
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SantoyaDewMint,
                            border = BorderStroke(1.dp, SantoyaLeafGreen.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = badgeTag,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SantoyaLeafGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = iconContainerColor,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
