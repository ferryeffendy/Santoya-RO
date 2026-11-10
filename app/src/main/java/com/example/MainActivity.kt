package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.KurirScreen
import com.example.ui.screens.LaporanScreen
import com.example.ui.screens.PelangganScreen
import com.example.ui.screens.PengeluaranScreen
import com.example.ui.screens.ProdukScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransaksiScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SantoyaViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val pelangganList by viewModel.allPelanggan.collectAsState()
                val produkList by viewModel.allProduk.collectAsState()
                val kurirList by viewModel.allKurir.collectAsState()
                val pengeluaranList by viewModel.allPengeluaran.collectAsState()
                val transaksiList by viewModel.allTransaksi.collectAsState()
                val receiptDialogData by viewModel.receiptDialog.collectAsState()
                val snackbarMessage by viewModel.snackbarMessage.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(snackbarMessage) {
                    snackbarMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSnackbar()
                    }
                }

                val pelangganUiModels = remember(pelangganList, transaksiList) {
                    viewModel.getPelangganUiModels(pelangganList, transaksiList)
                }

                BackHandler(enabled = currentScreen != SantoyaScreen.DASHBOARD) {
                    viewModel.navigateTo(SantoyaScreen.DASHBOARD)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        if (currentScreen != SantoyaScreen.DASHBOARD) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                TopAppBar(
                                    title = {
                                        Column {
                                            Text(
                                                text = when (currentScreen) {
                                                    SantoyaScreen.DASHBOARD -> "Santoya RO"
                                                    SantoyaScreen.TRANSAKSI -> "Transaksi Penjualan"
                                                    SantoyaScreen.PELANGGAN -> "Kelola Pelanggan"
                                                    SantoyaScreen.KURIR -> "Kelola Kurir & Komisi"
                                                    SantoyaScreen.PRODUK -> "Master Produk & Harga"
                                                    SantoyaScreen.PENGELUARAN -> "Catat Kas Keluar"
                                                    SantoyaScreen.LAPORAN -> "Laporan Laba / Rugi"
                                                    SantoyaScreen.SETTINGS -> "Pengaturan Sistem"
                                                },
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = when (currentScreen) {
                                                    SantoyaScreen.DASHBOARD -> "Beranda Utama"
                                                    SantoyaScreen.TRANSAKSI -> "Terminal Kasir & Riwayat Nota"
                                                    SantoyaScreen.PELANGGAN -> "Database Pelanggan & Piutang Kasbon"
                                                    SantoyaScreen.KURIR -> "Manajemen Pengantaran & Tarif Komisi"
                                                    SantoyaScreen.PRODUK -> "Katalog Harga Galon & Produk"
                                                    SantoyaScreen.PENGELUARAN -> "Jurnal Biaya Operasional & Bahan Baku"
                                                    SantoyaScreen.LAPORAN -> "Analisis Finansial & Ekspor Laporan"
                                                    SantoyaScreen.SETTINGS -> "Master Data & Cadangan Database"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = { viewModel.navigateTo(SantoyaScreen.DASHBOARD) },
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .testTag("btn_back_to_dashboard")
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Kembali ke Beranda",
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    actions = {
                                        if (currentScreen != SantoyaScreen.SETTINGS) {
                                            IconButton(
                                                onClick = { viewModel.navigateTo(SantoyaScreen.SETTINGS) },
                                                modifier = Modifier
                                                    .padding(end = 4.dp)
                                                    .testTag("btn_top_settings")
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Settings,
                                                            contentDescription = "Pengaturan",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.testTag("main_top_app_bar")
                                )
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        Crossfade(
                            targetState = currentScreen,
                            animationSpec = tween(durationMillis = 220),
                            label = "screen_crossfade"
                        ) { screen ->
                            when (screen) {
                                SantoyaScreen.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    transaksiList = transaksiList,
                                    pelangganList = pelangganList
                                )
                                SantoyaScreen.TRANSAKSI -> TransaksiScreen(
                                    viewModel = viewModel,
                                    transaksiList = transaksiList,
                                    pelangganList = pelangganList,
                                    produkList = produkList,
                                    kurirList = kurirList
                                )
                                SantoyaScreen.PELANGGAN -> PelangganScreen(
                                    viewModel = viewModel,
                                    pelangganUiModels = pelangganUiModels,
                                    transaksiList = transaksiList
                                )
                                SantoyaScreen.KURIR -> KurirScreen(
                                    viewModel = viewModel,
                                    kurirList = kurirList,
                                    transaksiList = transaksiList
                                )
                                SantoyaScreen.PRODUK -> ProdukScreen(
                                    viewModel = viewModel,
                                    produkList = produkList
                                )
                                SantoyaScreen.PENGELUARAN -> PengeluaranScreen(
                                    viewModel = viewModel,
                                    pengeluaranList = pengeluaranList
                                )
                                SantoyaScreen.LAPORAN -> LaporanScreen(
                                    viewModel = viewModel,
                                    transaksiList = transaksiList,
                                    pengeluaranList = pengeluaranList
                                )
                                SantoyaScreen.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel,
                                    pelangganList = pelangganList,
                                    transaksiList = transaksiList,
                                    produkList = produkList,
                                    kurirList = kurirList
                                )
                            }
                        }
                    }
                }

                if (receiptDialogData != null) {
                    ReceiptDialog(
                        dialogData = receiptDialogData!!,
                        onDismiss = { viewModel.dismissReceiptDialog() }
                    )
                }
            }
        }
    }
}
