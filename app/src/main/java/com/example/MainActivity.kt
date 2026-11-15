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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.RealtimeSyncStatusPill
import com.example.ui.components.SantoyaBusyStatusBanner
import com.example.ui.components.SantoyaSyncInfoBottomSheet
import com.example.ui.components.SantoyaTopBarIconButton
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.KurirScreen
import com.example.ui.screens.LaporanScreen
import com.example.ui.screens.PelangganScreen
import com.example.ui.screens.PengeluaranScreen
import com.example.ui.screens.PerawatanScreen
import com.example.ui.screens.ProdukScreen
import com.example.ui.screens.RevokedAccessDialog
import com.example.ui.screens.SessionPreparingDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransaksiScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.SyncConnectionStatus
import com.example.util.configureAppCheck
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SantoyaViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val coroutineScope = rememberCoroutineScope()
                val auth = remember { Firebase.auth }
                var currentUser by remember { mutableStateOf(auth.currentUser) }

                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { a ->
                        currentUser = a.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose {
                        auth.removeAuthStateListener(listener)
                    }
                }

                if (currentUser == null) {
                    LaunchedEffect(Unit) {
                        viewModel.stopRealtimeSync()
                    }
                    AuthScreen(
                        onAuthSuccess = {
                            currentUser = Firebase.auth.currentUser
                        }
                    )
                } else {
                    key(currentUser?.uid) {
                        val activeUser = currentUser
                        LaunchedEffect(activeUser?.uid, activeUser?.email) {
                            if (activeUser != null) {
                                viewModel.startRealtimeSync(activeUser.uid, activeUser.email)
                            }
                        }
                        DisposableEffect(activeUser?.uid) {
                            onDispose {
                                viewModel.stopRealtimeSync()
                            }
                        }

                        val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                        val receiptDialogData by viewModel.receiptDialog.collectAsStateWithLifecycle()
                        val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
                        val isKaryawan by viewModel.isKaryawan.collectAsStateWithLifecycle()
                        val activeKaryawan by viewModel.activeKaryawan.collectAsStateWithLifecycle()
                        val isBusyAction by viewModel.isBusyAction.collectAsStateWithLifecycle()
                        val busyActionMessage by viewModel.busyActionMessage.collectAsStateWithLifecycle()
                        val syncState by viewModel.realtimeSyncState.collectAsStateWithLifecycle()
                        var showSyncInfoSheet by remember { mutableStateOf(false) }

                        if (syncState.isSwitchingOwner) {
                            SessionPreparingDialog()
                        }

                        // Kunci seluruh UI jika akses karyawan dicabut pemilik.
                        if (syncState.status == SyncConnectionStatus.REVOKED) {
                            RevokedAccessDialog(
                                ownerEmail = syncState.ownerEmail,
                                onSignOut = {
                                    coroutineScope.launch {
                                        viewModel.clearLocalDataForLogout()
                                        try {
                                            val credentialManager = CredentialManager.create(this@MainActivity)
                                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                        } catch (_: Exception) {
                                        }
                                        Firebase.auth.signOut()
                                    }
                                }
                            )
                        }

                        LaunchedEffect(currentScreen, isKaryawan) {
                            if (isKaryawan && (
                                    currentScreen == SantoyaScreen.LAPORAN ||
                                        currentScreen == SantoyaScreen.PENGELUARAN ||
                                        currentScreen == SantoyaScreen.SETTINGS ||
                                        currentScreen == SantoyaScreen.KURIR ||
                                        currentScreen == SantoyaScreen.PRODUK ||
                                        currentScreen == SantoyaScreen.PERAWATAN
                                    )
                            ) {
                                viewModel.navigateTo(SantoyaScreen.DASHBOARD)
                            }
                        }

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
                                                            SantoyaScreen.TRANSAKSI -> "Transaksi"
                                                            SantoyaScreen.PELANGGAN -> "Pelanggan"
                                                            SantoyaScreen.KURIR -> "Karyawan & Komisi"
                                                            SantoyaScreen.PRODUK -> "Produk & Harga"
                                                            SantoyaScreen.PENGELUARAN -> "Pengeluaran"
                                                            SantoyaScreen.LAPORAN -> "Laporan Keuangan"
                                                            SantoyaScreen.SETTINGS -> "Pengaturan"
                                                            SantoyaScreen.PERAWATAN -> "Jadwal Perawatan RO"
                                                        },
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = when (currentScreen) {
                                                            SantoyaScreen.DASHBOARD -> "Beranda"
                                                            SantoyaScreen.TRANSAKSI -> "Kasir & daftar nota"
                                                            SantoyaScreen.PELANGGAN -> "Data pelanggan & kasbon"
                                                            SantoyaScreen.KURIR -> "Atur kurir & komisi per galon"
                                                            SantoyaScreen.PRODUK -> "Atur harga galon"
                                                            SantoyaScreen.PENGELUARAN -> "Catat belanja & biaya depot"
                                                            SantoyaScreen.LAPORAN -> "Omzet, laba bersih & ekspor"
                                                            SantoyaScreen.SETTINGS -> "Akun, sinkron & cadangan data"
                                                            SantoyaScreen.PERAWATAN -> "Jadwal servis & riwayat perawatan"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },
                                            navigationIcon = {
                                                SantoyaTopBarIconButton(
                                                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                                    contentDescription = "Kembali ke Beranda",
                                                    onClick = { viewModel.navigateTo(SantoyaScreen.DASHBOARD) },
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier
                                                        .padding(start = 4.dp)
                                                        .testTag("btn_back_to_dashboard")
                                                )
                                            },
                                            actions = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                ) {
                                                    RealtimeSyncStatusPill(
                                                        syncState = syncState,
                                                        onClick = { showSyncInfoSheet = true },
                                                        modifier = Modifier.testTag("top_app_bar_sync_pill")
                                                    )
                                                    if (currentScreen != SantoyaScreen.SETTINGS && !isKaryawan) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        SantoyaTopBarIconButton(
                                                            icon = Icons.Outlined.Settings,
                                                            contentDescription = "Pengaturan",
                                                            onClick = { viewModel.navigateTo(SantoyaScreen.SETTINGS) },
                                                            modifier = Modifier.testTag("btn_top_settings")
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
                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            },
                            snackbarHost = { SnackbarHost(snackbarHostState) }
                        ) { innerPadding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .consumeWindowInsets(innerPadding)
                            ) {
                                SantoyaBusyStatusBanner(
                                    isBusy = isBusyAction,
                                    message = busyActionMessage
                                )
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Crossfade(
                                        targetState = currentScreen,
                                        animationSpec = tween(durationMillis = 220),
                                        label = "screen_crossfade"
                                    ) { screen ->
                                        when (screen) {
                                            SantoyaScreen.DASHBOARD -> {
                                                val scopedTransaksi by viewModel.scopedTransaksi.collectAsStateWithLifecycle()
                                                val scopedPelanggan by viewModel.scopedPelanggan.collectAsStateWithLifecycle()
                                                val pengeluaranList by viewModel.allPengeluaran.collectAsStateWithLifecycle()
                                                val scopedPelangganUiModels by viewModel.scopedPelangganUiModels.collectAsStateWithLifecycle()
                                                DashboardScreen(
                                                    viewModel = viewModel,
                                                    transaksiList = scopedTransaksi,
                                                    pelangganList = scopedPelanggan,
                                                    pengeluaranList = if (isKaryawan) emptyList() else pengeluaranList,
                                                    pelangganUiModels = scopedPelangganUiModels,
                                                    onSignOut = {
                                                        coroutineScope.launch {
                                                            viewModel.clearLocalDataForLogout()
                                                            try {
                                                                val credentialManager = CredentialManager.create(this@MainActivity)
                                                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                                            } catch (_: Exception) {
                                                            }
                                                            Firebase.auth.signOut()
                                                        }
                                                    },
                                                    isKaryawan = isKaryawan,
                                                    activeKaryawan = activeKaryawan
                                                )
                                            }
                                            SantoyaScreen.TRANSAKSI -> {
                                                val scopedTransaksi by viewModel.scopedTransaksi.collectAsStateWithLifecycle()
                                                val scopedPelanggan by viewModel.scopedPelanggan.collectAsStateWithLifecycle()
                                                val produkList by viewModel.allProduk.collectAsStateWithLifecycle()
                                                val kurirList by viewModel.allKurir.collectAsStateWithLifecycle()
                                                TransaksiScreen(
                                                    viewModel = viewModel,
                                                    transaksiList = scopedTransaksi,
                                                    pelangganList = scopedPelanggan,
                                                    produkList = produkList,
                                                    kurirList = kurirList,
                                                    isKaryawan = isKaryawan,
                                                    activeKaryawan = activeKaryawan
                                                )
                                            }
                                            SantoyaScreen.PELANGGAN -> {
                                                val scopedPelangganUiModels by viewModel.scopedPelangganUiModels.collectAsStateWithLifecycle()
                                                val scopedPelangganTransaksi by viewModel.scopedPelangganTransaksi.collectAsStateWithLifecycle()
                                                PelangganScreen(
                                                    viewModel = viewModel,
                                                    pelangganUiModels = scopedPelangganUiModels,
                                                    transaksiList = scopedPelangganTransaksi,
                                                    isKaryawan = isKaryawan,
                                                    activeKaryawan = activeKaryawan
                                                )
                                            }
                                            SantoyaScreen.KURIR -> {
                                                val kurirList by viewModel.allKurir.collectAsStateWithLifecycle()
                                                val transaksiList by viewModel.allTransaksi.collectAsStateWithLifecycle()
                                                KurirScreen(
                                                    viewModel = viewModel,
                                                    kurirList = kurirList,
                                                    transaksiList = transaksiList
                                                )
                                            }
                                            SantoyaScreen.PRODUK -> {
                                                val produkList by viewModel.allProduk.collectAsStateWithLifecycle()
                                                ProdukScreen(
                                                    viewModel = viewModel,
                                                    produkList = produkList
                                                )
                                            }
                                            SantoyaScreen.PENGELUARAN -> {
                                                val pengeluaranList by viewModel.allPengeluaran.collectAsStateWithLifecycle()
                                                PengeluaranScreen(
                                                    viewModel = viewModel,
                                                    pengeluaranList = pengeluaranList
                                                )
                                            }
                                            SantoyaScreen.LAPORAN -> {
                                                val transaksiList by viewModel.allTransaksi.collectAsStateWithLifecycle()
                                                val pengeluaranList by viewModel.allPengeluaran.collectAsStateWithLifecycle()
                                                LaporanScreen(
                                                    viewModel = viewModel,
                                                    transaksiList = transaksiList,
                                                    pengeluaranList = pengeluaranList
                                                )
                                            }
                                            SantoyaScreen.SETTINGS -> {
                                                val pelangganList by viewModel.allPelanggan.collectAsStateWithLifecycle()
                                                val transaksiList by viewModel.allTransaksi.collectAsStateWithLifecycle()
                                                val produkList by viewModel.allProduk.collectAsStateWithLifecycle()
                                                val kurirList by viewModel.allKurir.collectAsStateWithLifecycle()
                                                SettingsScreen(
                                                    viewModel = viewModel,
                                                    pelangganList = pelangganList,
                                                    transaksiList = transaksiList,
                                                    produkList = produkList,
                                                    kurirList = kurirList,
                                                    onSignOut = {
                                                        coroutineScope.launch {
                                                            viewModel.clearLocalDataForLogout()
                                                            try {
                                                                val credentialManager = CredentialManager.create(this@MainActivity)
                                                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                                            } catch (e: Exception) {
                                                                // ignore
                                                            }
                                                            Firebase.auth.signOut()
                                                        }
                                                    }
                                                )
                                            }
                                            SantoyaScreen.PERAWATAN -> {
                                                if (!isKaryawan) {
                                                    PerawatanScreen(viewModel = viewModel)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        receiptDialogData?.let { activeReceipt ->
                            ReceiptDialog(
                                dialogData = activeReceipt,
                                onDismiss = { viewModel.dismissReceiptDialog() }
                            )
                        }

                        if (showSyncInfoSheet) {
                            val activeUser = currentUser
                            SantoyaSyncInfoBottomSheet(
                                syncState = syncState,
                                onDismiss = { showSyncInfoSheet = false },
                                onSyncNow = {
                                    activeUser?.uid?.let { uid ->
                                        viewModel.syncToCloud(this@MainActivity, uid)
                                    }
                                },
                                onNavigateToSettings = if (!isKaryawan) {
                                    {
                                        viewModel.navigateTo(SantoyaScreen.SETTINGS)
                                        showSyncInfoSheet = false
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
