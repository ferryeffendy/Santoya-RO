package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.ui.res.stringResource
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
import com.example.ui.navigation.AppNavigation
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.RevokedAccessDialog
import com.example.ui.screens.RoleConfirmationDialog
import com.example.ui.screens.SessionPreparingDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.util.GoogleSignInHelper
import com.example.util.OfflineSessionManager
import com.example.util.SyncConnectionStatus
import com.example.util.configureAppCheck
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
                var isOfflineSessionActive by remember {
                    mutableStateOf(
                        auth.currentUser == null && viewModel.isPersistedOfflineModeActive()
                    )
                }

                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { a ->
                        currentUser = a.currentUser
                        if (a.currentUser != null) {
                            isOfflineSessionActive = false
                        }
                    }
                    auth.addAuthStateListener(listener)
                    onDispose {
                        auth.removeAuthStateListener(listener)
                    }
                }

                if (currentUser == null && !isOfflineSessionActive) {
                    LaunchedEffect(Unit) {
                        viewModel.stopRealtimeSync()
                    }
                    val offlineSummary by viewModel.offlineLocalSummary.collectAsStateWithLifecycle()
                    val lastSession = remember(currentUser, isOfflineSessionActive) {
                        viewModel.getLastSessionInfo()
                    }
                    AuthScreen(
                        onAuthSuccess = {
                            isOfflineSessionActive = false
                            currentUser = Firebase.auth.currentUser
                        },
                        onEnterOfflineMode = {
                            isOfflineSessionActive = true
                            viewModel.enterOfflineLocalMode()
                        },
                        offlineSummary = offlineSummary,
                        lastSession = lastSession
                    )
                } else {
                    val isOfflineMode = currentUser == null && isOfflineSessionActive
                    val activeSessionKey = currentUser?.uid ?: OfflineSessionManager.OFFLINE_LOCAL_UID
                    key(activeSessionKey) {
                        val activeUser = currentUser
                        LaunchedEffect(activeSessionKey, activeUser?.email, activeUser?.displayName) {
                            if (activeUser != null) {
                                viewModel.startRealtimeSync(
                                    uid = activeUser.uid,
                                    email = activeUser.email,
                                    displayName = activeUser.displayName
                                )
                            } else if (isOfflineSessionActive) {
                                viewModel.enterOfflineLocalMode()
                            }
                        }
                        DisposableEffect(activeSessionKey) {
                            onDispose {
                                viewModel.stopRealtimeSync()
                            }
                        }

                        val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                        val receiptDialogData by viewModel.receiptDialog.collectAsStateWithLifecycle()
                        val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
                        val isKaryawan by viewModel.isKaryawan.collectAsStateWithLifecycle()
                        val isBusyAction by viewModel.isBusyAction.collectAsStateWithLifecycle()
                        val busyActionMessage by viewModel.busyActionMessage.collectAsStateWithLifecycle()
                        val syncState by viewModel.realtimeSyncState.collectAsStateWithLifecycle()
                        val depotProfile by viewModel.depotProfile.collectAsStateWithLifecycle()
                        var showSyncInfoSheet by remember { mutableStateOf(false) }

                        if (syncState.isSwitchingOwner) {
                            SessionPreparingDialog()
                        }

                        // Kunci seluruh UI jika akses karyawan dicabut pemilik.
                        if (!isOfflineMode && syncState.status == SyncConnectionStatus.REVOKED) {
                            RevokedAccessDialog(
                                ownerEmail = syncState.ownerEmail,
                                onSignOut = {
                                    coroutineScope.launch {
                                        viewModel.clearLocalDataForLogout()
                                        try {
                                            val credentialManager = CredentialManager.create(this@MainActivity)
                                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                        } catch (e: Exception) {
                                            FirebaseCrashlytics.getInstance().recordException(e)
                                        }
                                        Firebase.auth.signOut()
                                    }
                                }
                            )
                        }

                        // Tampilkan gerbang konfirmasi peran bagi akun baru yang belum terdaftar di depot mana pun.
                        if (!isOfflineMode && syncState.status == SyncConnectionStatus.AWAITING_ROLE_CONFIRMATION) {
                            RoleConfirmationDialog(
                                userEmail = activeUser?.email.orEmpty(),
                                onRecheckStaffLink = { onDone ->
                                    viewModel.recheckStaffConnection(onDone)
                                },
                                onConfirmAsOwner = {
                                    viewModel.confirmAsNewOwner()
                                },
                                onSignOut = {
                                    coroutineScope.launch {
                                        viewModel.clearLocalDataForLogout()
                                        try {
                                            val credentialManager = CredentialManager.create(this@MainActivity)
                                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                        } catch (e: Exception) {
                                            FirebaseCrashlytics.getInstance().recordException(e)
                                        }
                                        Firebase.auth.signOut()
                                    }
                                }
                            )
                        }

                        val snackbarHostState = remember { SnackbarHostState() }

                        LaunchedEffect(snackbarMessage) {
                            snackbarMessage?.let { msg ->
                                snackbarHostState.showSnackbar(msg)
                                viewModel.clearSnackbar()
                            }
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
                                                            SantoyaScreen.DASHBOARD -> stringResource(R.string.nav_title_dashboard)
                                                            SantoyaScreen.TRANSAKSI -> stringResource(R.string.nav_title_transaksi)
                                                            SantoyaScreen.PELANGGAN -> stringResource(R.string.nav_title_pelanggan)
                                                            SantoyaScreen.KURIR -> stringResource(R.string.nav_title_kurir)
                                                            SantoyaScreen.PRODUK -> stringResource(R.string.nav_title_produk)
                                                            SantoyaScreen.PENGELUARAN -> stringResource(R.string.nav_title_pengeluaran)
                                                            SantoyaScreen.LAPORAN -> stringResource(R.string.nav_title_laporan)
                                                            SantoyaScreen.SETTINGS -> stringResource(R.string.nav_title_settings)
                                                            SantoyaScreen.PERAWATAN -> stringResource(R.string.nav_title_perawatan)
                                                        },
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = when (currentScreen) {
                                                            SantoyaScreen.DASHBOARD -> stringResource(R.string.nav_sub_dashboard)
                                                            SantoyaScreen.TRANSAKSI -> stringResource(R.string.nav_sub_transaksi)
                                                            SantoyaScreen.PELANGGAN -> stringResource(R.string.nav_sub_pelanggan)
                                                            SantoyaScreen.KURIR -> stringResource(R.string.nav_sub_kurir)
                                                            SantoyaScreen.PRODUK -> stringResource(R.string.nav_sub_produk)
                                                            SantoyaScreen.PENGELUARAN -> stringResource(R.string.nav_sub_pengeluaran)
                                                            SantoyaScreen.LAPORAN -> stringResource(R.string.nav_sub_laporan)
                                                            SantoyaScreen.SETTINGS -> stringResource(R.string.nav_sub_settings)
                                                            SantoyaScreen.PERAWATAN -> stringResource(R.string.nav_sub_perawatan)
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
                                                    contentDescription = stringResource(R.string.nav_back_home_cd),
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
                                                            contentDescription = stringResource(R.string.dashboard_settings_cd),
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
                                    AppNavigation(
                                        viewModel = viewModel,
                                        auth = auth,
                                        isOfflineMode = isOfflineMode,
                                        onExitOfflineToAuth = {
                                            viewModel.exitOfflineSessionKeepLocalData()
                                            isOfflineSessionActive = false
                                        },
                                        onGoogleLinked = {
                                            isOfflineSessionActive = false
                                            currentUser = Firebase.auth.currentUser
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        receiptDialogData?.let { activeReceipt ->
                            ReceiptDialog(
                                dialogData = activeReceipt,
                                depotProfile = depotProfile,
                                isOwner = !isKaryawan && depotProfile.isOwnerRole(),
                                onQuickSaveDepotProfile = { name, addr, phone, onDone ->
                                    viewModel.quickSaveDepotProfile(name, addr, phone, onDone)
                                },
                                onDismiss = { viewModel.dismissReceiptDialog() }
                            )
                        }

                        if (showSyncInfoSheet) {
                            val activeUser = currentUser
                            val canSwitchToStaffWaiting = !isOfflineMode && activeUser != null
                            SantoyaSyncInfoBottomSheet(
                                syncState = syncState,
                                onDismiss = { showSyncInfoSheet = false },
                                onSyncNow = {
                                    activeUser?.uid?.let { uid ->
                                        viewModel.syncToCloud(this@MainActivity, uid)
                                    }
                                },
                                onForcePushAll = if (!isOfflineMode && activeUser != null) {
                                    {
                                        activeUser.uid.let { uid ->
                                            viewModel.forcePushAllLocalToCloud(this@MainActivity, uid)
                                        }
                                    }
                                } else null,
                                onNavigateToSettings = if (!isKaryawan) {
                                    {
                                        viewModel.navigateTo(SantoyaScreen.SETTINGS)
                                        showSyncInfoSheet = false
                                    }
                                } else null,
                                onSwitchToStaffWaiting = if (canSwitchToStaffWaiting) {
                                    {
                                        showSyncInfoSheet = false
                                        viewModel.switchToStaffWaitingMode()
                                    }
                                } else null,
                                isOfflineLocalMode = isOfflineMode,
                                onConnectGoogleAccount = if (isOfflineMode) {
                                    {
                                        val credentialManager = CredentialManager.create(this@MainActivity)
                                        GoogleSignInHelper.signIn(
                                            context = this@MainActivity,
                                            credentialManager = credentialManager,
                                            coroutineScope = coroutineScope,
                                            onLoadingChange = {},
                                            onSuccess = {
                                                isOfflineSessionActive = false
                                                currentUser = Firebase.auth.currentUser
                                            },
                                            onError = { err -> viewModel.showSnackbar(err) }
                                        )
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
