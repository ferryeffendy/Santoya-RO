package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

                BackHandler(enabled = currentScreen != SantoyaScreen.DASHBOARD) {
                    viewModel.navigateTo(SantoyaScreen.DASHBOARD)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (currentScreen != SantoyaScreen.DASHBOARD) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = when (currentScreen) {
                                            SantoyaScreen.DASHBOARD -> "Santoya RO"
                                            SantoyaScreen.TRANSAKSI -> "Transaksi Penjualan"
                                            SantoyaScreen.PELANGGAN -> "Kelola Pelanggan"
                                            SantoyaScreen.KURIR -> "Kelola Kurir & Komisi"
                                            SantoyaScreen.PRODUK -> "Master Produk & Harga"
                                            SantoyaScreen.PENGELUARAN -> "Catat Kas Keluar"
                                            SantoyaScreen.LAPORAN -> "Laporan Laba / Rugi"
                                            SantoyaScreen.SETTINGS -> "Pengaturan"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { viewModel.navigateTo(SantoyaScreen.DASHBOARD) },
                                        modifier = Modifier.testTag("btn_back_to_dashboard")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Kembali ke Beranda"
                                        )
                                    }
                                },
                                actions = {
                                    if (currentScreen != SantoyaScreen.SETTINGS) {
                                        IconButton(
                                            onClick = { viewModel.navigateTo(SantoyaScreen.SETTINGS) },
                                            modifier = Modifier.testTag("btn_top_settings")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Pengaturan",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.testTag("main_top_app_bar")
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        Crossfade(
                            targetState = currentScreen,
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
                                    pelangganList = pelangganList,
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

                // Modal Receipt Dialog for Printing / Sharing
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
