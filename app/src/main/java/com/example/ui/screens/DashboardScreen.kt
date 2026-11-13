package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.companion.kzpmrw.R
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Pengeluaran
import com.example.data.entity.TransaksiDetail
import com.example.ui.components.RealtimeSyncStatusPill
import com.example.ui.theme.HeroGlassBase
import com.example.ui.theme.HeroGlowAmber
import com.example.ui.theme.HeroGlowAmberSoft
import com.example.ui.theme.HeroGlowCyan
import com.example.ui.theme.HeroGlowCyanSoft
import com.example.ui.theme.HeroGlowEmerald
import com.example.ui.theme.HeroGlowEmeraldSoft
import com.example.ui.theme.HeroGlowRose
import com.example.ui.theme.HeroGlowRoseSoft
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientMid
import com.example.ui.theme.HeroGradientStart
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
        topBar = {
            Column {
                TopAppBar(
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
                                    text = "Manajemen Depot Air Minum",
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
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Pengaturan",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
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
                                        color = MaterialTheme.colorScheme.surfaceVariant,
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
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (syncState.status == SyncConnectionStatus.REVOKED) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
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
                                text = "Akses akun karyawan sedang dinonaktifkan oleh Pemilik Depot. Hubungi pemilik untuk mengaktifkan kembali sinkronisasi.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            // 1. World-Class Fintech Executive Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        HeroGradientStart,
                                        HeroGradientMid,
                                        HeroGradientEnd
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(1000f, 900f)
                                )
                            )
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            HeroGlowCyan.copy(alpha = 0.22f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.92f, size.height * 0.12f),
                                        radius = size.width * 0.55f
                                    )
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            HeroGlowEmerald.copy(alpha = 0.18f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.1f, size.height * 0.95f),
                                        radius = size.width * 0.5f
                                    )
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
                                            letterSpacing = 0.8.sp,
                                            color = Color.White.copy(alpha = 0.72f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = FormatUtils.formatRupiah(
                                            if (isKaryawan) karyawanMetrics.setoranTunaiHariIni else pendapatanLunasHariIni
                                        ),
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = FormatUtils.formatDateShort(System.currentTimeMillis()),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                    if (isKaryawan && effectiveActiveKaryawan != null) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = HeroGlowCyan.copy(alpha = 0.22f),
                                            border = BorderStroke(1.dp, HeroGlowCyanSoft.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = effectiveActiveKaryawan.nama,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = HeroGlowCyanSoft,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (!isKaryawan) {
                                // Middle Section (Pemilik): Nested Glassmorphic Financial Bento (Laba Bersih & Total Piutang)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Laba Bersih Hari Ini (Clickable -> Laporan Hari Ini)
                                    val isPositiveProfit = labaBersihHariIni >= 0.0
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .pressClickEffect(onClick = {
                                                viewModel.setLaporanPeriode(LaporanPeriode.HARI_INI)
                                                onNavigateToLaporan()
                                            }),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isPositiveProfit) HeroGlowEmerald.copy(alpha = 0.25f) else HeroGlowRose.copy(alpha = 0.25f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                                                            contentDescription = null,
                                                            tint = if (isPositiveProfit) HeroGlowEmeraldSoft else HeroGlowRoseSoft,
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
                                                    color = Color.White.copy(alpha = 0.68f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = FormatUtils.formatRupiah(labaBersihHariIni),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = if (isPositiveProfit) HeroGlowEmeraldSoft else HeroGlowRoseSoft,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Tunai - Biaya & Komisi",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.55f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Total Piutang / Kasbon (Clickable -> Pelanggan Tab Kasbon)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .pressClickEffect(onClick = {
                                                viewModel.setPelangganTab(1)
                                                onNavigateToPelanggan()
                                            }),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = HeroGlowAmber.copy(alpha = 0.25f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.HourglassEmpty,
                                                            contentDescription = null,
                                                            tint = HeroGlowAmberSoft,
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
                                                    color = Color.White.copy(alpha = 0.68f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = FormatUtils.formatRupiah(totalPiutangAktif),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = HeroGlowAmberSoft,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Lihat kasbon →",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.55f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Bottom Section (Pemilik): Production Volume & Monthly Projection Glass Bar
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = HeroGlassBase.copy(alpha = 0.42f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Total Galon Hari Ini (Clickable -> Transaksi)
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
                                                color = HeroGlowCyan.copy(alpha = 0.2f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.WaterDrop,
                                                        contentDescription = null,
                                                        tint = HeroGlowCyanSoft,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "GALON HARI INI",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    maxLines = 1
                                                )
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text(
                                                        text = "$totalGalonHariIni",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Galon",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Subtle Vertical Divider
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .width(1.dp)
                                                .height(34.dp)
                                                .background(Color.White.copy(alpha = 0.16f))
                                        )

                                        // Estimasi Galon Bulan Ini (Clickable -> Laporan Bulan Ini)
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
                                                color = HeroGlowEmeraldSoft.copy(alpha = 0.18f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.AutoGraph,
                                                        contentDescription = null,
                                                        tint = HeroGlowEmeraldSoft,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "ESTIMASI BULAN INI",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text(
                                                        text = "$estimasiGalonBulanIni",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Black,
                                                        color = HeroGlowEmeraldSoft
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Galon",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White.copy(alpha = 0.7f),
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
                                        color = Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = HeroGlowEmerald.copy(alpha = 0.25f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.WaterDrop,
                                                            contentDescription = null,
                                                            tint = HeroGlowEmeraldSoft,
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
                                                    color = Color.White.copy(alpha = 0.72f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = FormatUtils.formatRupiah(karyawanMetrics.komisiHariIni),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = HeroGlowEmeraldSoft,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${karyawanMetrics.galonHariIni} Galon • ${karyawanMetrics.pesananHariIni} Order",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White.copy(alpha = 0.72f),
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
                                        color = Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = HeroGlowCyan.copy(alpha = 0.25f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.AutoGraph,
                                                            contentDescription = null,
                                                            tint = HeroGlowCyanSoft,
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
                                                    color = Color.White.copy(alpha = 0.72f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = FormatUtils.formatRupiah(karyawanMetrics.komisiBulanIni),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = HeroGlowCyanSoft,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${karyawanMetrics.galonBulanIni} Galon • ${karyawanMetrics.pesananBulanIni} Order",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White.copy(alpha = 0.72f),
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
                                    color = HeroGlassBase.copy(alpha = 0.42f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
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
                                                color = HeroGlowAmber.copy(alpha = 0.22f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.HourglassEmpty,
                                                        contentDescription = null,
                                                        tint = HeroGlowAmberSoft,
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
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = FormatUtils.formatRupiah(totalPiutangAktif),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = HeroGlowAmberSoft,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .width(1.dp)
                                                .height(34.dp)
                                                .background(Color.White.copy(alpha = 0.16f))
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
                                                color = HeroGlowCyan.copy(alpha = 0.2f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.People,
                                                        contentDescription = null,
                                                        tint = HeroGlowCyanSoft,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "PELANGGAN SAYA",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text(
                                                        text = "${effectivePelangganList.size}",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Orang",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White.copy(alpha = 0.7f),
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

            // 2. Smart CRM Order Alert Banner (Jika ada pelanggan yang masuk jadwal order / terlambat)
            if (crmJadwalCustomers.isNotEmpty()) {
                item {
                    val overdueCount = crmJadwalCustomers.count { it.crmPrediction.status == CrmOrderStatus.OVERDUE }
                    val santoyaColors = MaterialTheme.santoyaColors
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = santoyaColors.warningContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, santoyaColors.warning.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressClickEffect(onClick = {
                                viewModel.setPelangganTab(2) // Tab Jadwal
                                onNavigateToPelanggan()
                            })
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = santoyaColors.warning.copy(alpha = 0.18f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = santoyaColors.warning,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${crmJadwalCustomers.size} Pelanggan Perlu Pengingat",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = santoyaColors.onWarningContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (overdueCount > 0) {
                                            "$overdueCount lewat jadwal • Ketuk untuk kirim pengingat"
                                        } else {
                                            "Jadwal isi ulang hari ini • Ketuk untuk lihat"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = santoyaColors.warning,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Lihat Jadwal",
                                tint = santoyaColors.warning,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 3. Menu Navigasi Utama (Tactile Bento Cards)
            item {
                Text(
                    text = "Menu Utama",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MenuCard(
                            modifier = Modifier.weight(1f),
                            title = "Transaksi",
                            subtitle = "Penjualan Galon",
                            icon = Icons.Default.ShoppingCart,
                            accentColor = MaterialTheme.colorScheme.primary,
                            testTag = "nav_transaksi_button",
                            onClick = {
                                viewModel.setTransaksiTab(0)
                                onNavigateToTransaksi()
                            }
                        )
                        MenuCard(
                            modifier = Modifier.weight(1f),
                            title = "Pelanggan",
                            subtitle = "Kasbon & Profil",
                            icon = Icons.Default.People,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            testTag = "nav_pelanggan_button",
                            onClick = {
                                viewModel.setPelangganTab(0)
                                onNavigateToPelanggan()
                            }
                        )
                    }
                    if (!isKaryawan) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MenuCard(
                                modifier = Modifier.weight(1f),
                                title = "Pengeluaran",
                                subtitle = "Catat Kas Keluar",
                                icon = Icons.Default.MoneyOff,
                                accentColor = MaterialTheme.colorScheme.error,
                                testTag = "nav_pengeluaran_button",
                                onClick = onNavigateToPengeluaran
                            )
                            MenuCard(
                                modifier = Modifier.weight(1f),
                                title = "Laporan",
                                subtitle = "Omzet & Laba Bersih",
                                icon = Icons.Default.Assessment,
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                testTag = "nav_laporan_button",
                                onClick = onNavigateToLaporan
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
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.pressClickEffect(onClick = {
                                viewModel.setTransaksiTab(1)
                                onNavigateToTransaksi()
                            })
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lihat Semua",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
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

            if (recentTransactionsToShow.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Belum ada transaksi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isKaryawan) {
                                    "Catat penjualan pertama untuk memantau setoran dan komisi."
                                } else {
                                    "Catat penjualan pertama untuk memantau omzet dan laba bersih."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Button(
                                onClick = {
                                    viewModel.setTransaksiTab(0)
                                    onNavigateToTransaksi()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Buat Transaksi",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                items(recentTransactionsToShow, key = { it.transaksi.id }) { detail ->
                    val isKasbon = detail.transaksi.status.equals("Kasbon", ignoreCase = true)
                    val metodeDisplay = if (isKasbon) {
                        "Belum Dibayar"
                    } else {
                        val m = detail.transaksi.metode_bayar.trim()
                        if (m.isBlank() || m == "-") "Cash" else m
                    }
                    val subInfoParts = mutableListOf("${detail.transaksi.jumlah} Galon", metodeDisplay)
                    if (detail.kurir != null) {
                        subInfoParts.add(detail.kurir.nama)
                    }
                    if (!isShowingTodayTransactions) {
                        subInfoParts.add(FormatUtils.formatDateShort(detail.transaksi.tanggal))
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                    val santoyaColors = MaterialTheme.santoyaColors
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isKasbon) santoyaColors.warningContainer else santoyaColors.successContainer
                                    ) {
                                        Text(
                                            text = detail.transaksi.status.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isKasbon) santoyaColors.onWarningContainer else santoyaColors.onSuccessContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subInfoParts.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isKasbon) MaterialTheme.santoyaColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = FormatUtils.formatRupiah(detail.transaksi.total_harga),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
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
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = "Cetak Struk",
                                        tint = MaterialTheme.colorScheme.primary,
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

    if (showKaryawanSignOutDialog && onSignOut != null) {
        AlertDialog(
            onDismissRequest = { showKaryawanSignOutDialog = false },
            title = { Text("Keluar Akun Karyawan", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin keluar dari akun Google ini?") },
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

@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(82.dp)
            .testTag(testTag)
            .pressClickEffect(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    }
}


