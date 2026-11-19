package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.TransaksiDetail
import com.example.ui.components.DepotProfileEditSheet
import com.example.ui.components.DepotProfileSummaryCard
import com.example.ui.components.ImportJsonDialog
import com.example.ui.components.RealtimeSyncStatusPill
import com.example.ui.components.SyncActiveStageProgressBox
import com.example.ui.components.SyncQueueBreakdownChips
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.SantoyaSpacing
import com.example.ui.theme.pressClickEffect
import com.example.ui.theme.santoyaColors
import com.example.util.ExportHelper
import com.example.util.FormatUtils
import com.example.util.GoogleSignInHelper
import com.example.util.SyncConnectionStatus
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun SettingsScreen(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>,
    produkList: List<Produk>,
    kurirList: List<Kurir>,
    isOfflineMode: Boolean = false,
    onExitOfflineToAuth: () -> Unit = {},
    onGoogleLinked: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    var showImportDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showExitOfflineDialog by remember { mutableStateOf(false) }
    var showResetOfflineDialog by remember { mutableStateOf(false) }
    var isLinkingGoogle by remember { mutableStateOf(false) }
    var showRestoreCloudDialog by remember { mutableStateOf(false) }
    var showForcePushCloudDialog by remember { mutableStateOf(false) }
    var showRestoreAutoBackupDialog by remember { mutableStateOf(false) }

    val isKaryawan by viewModel.isKaryawan.collectAsStateWithLifecycle()
    val depotProfile by viewModel.depotProfile.collectAsStateWithLifecycle()
    val hasPendingProfileSync by viewModel.hasPendingProfileSync.collectAsStateWithLifecycle()
    val isBusyAction by viewModel.isBusyAction.collectAsStateWithLifecycle()
    val isSyncingCloud by viewModel.isSyncingCloud.collectAsStateWithLifecycle()
    val activeSyncPushMode by viewModel.activeSyncPushMode.collectAsStateWithLifecycle()
    val cloudSyncError by viewModel.cloudSyncError.collectAsStateWithLifecycle()
    val lastSyncSummary by viewModel.lastSyncSummary.collectAsStateWithLifecycle()
    val syncState by viewModel.realtimeSyncState.collectAsStateWithLifecycle()
    val perawatanEvaluations by viewModel.allPerawatanEvaluations.collectAsStateWithLifecycle()
    val currentUser = remember(isOfflineMode) { if (isOfflineMode) null else Firebase.auth.currentUser }
    var showDepotProfileSheet by remember { mutableStateOf(false) }
    val isOwnerProfile = !isKaryawan && depotProfile.isOwnerRole()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SantoyaSpacing.pageHorizontal),
        contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        // 0. Profil Pemilik & Depot (untuk kop struk Bluetooth, nota WhatsApp, dan laporan PDF)
        item {
            SettingsSectionTitle(stringResource(R.string.depot_profile_section_title))
            Spacer(modifier = Modifier.height(8.dp))
            DepotProfileSummaryCard(
                profile = depotProfile,
                isOwner = isOwnerProfile,
                onClick = { showDepotProfileSheet = true },
                hasPendingSync = hasPendingProfileSync,
                isOfflineMode = isOfflineMode,
                onCopyProfile = { summaryText ->
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    if (clipboard != null) {
                        val clip = android.content.ClipData.newPlainText("Profil Depot", summaryText)
                        clipboard.setPrimaryClip(clip)
                        viewModel.showSnackbar(context.getString(R.string.depot_profile_copied_snackbar))
                    }
                }
            )
        }

        // 1. Profil Pengguna (Pemilik / Karyawan)
        item {
            val sectionTitle = if (isKaryawan) {
                stringResource(R.string.settings_section_profile_staff)
            } else {
                stringResource(R.string.settings_section_profile_owner)
            }
            SettingsSectionTitle(sectionTitle)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(SantoyaRadius.lg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isOfflineMode -> MaterialTheme.santoyaColors.warningContainer
                                isKaryawan -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountCircle,
                                    contentDescription = null,
                                    tint = when {
                                        isOfflineMode -> MaterialTheme.santoyaColors.warning
                                        isKaryawan -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val defaultName = when {
                                isOfflineMode -> stringResource(R.string.offline_profile_title)
                                isKaryawan -> stringResource(R.string.settings_profile_default_karyawan)
                                else -> stringResource(R.string.settings_profile_default_owner)
                            }
                            val defaultAccount = when {
                                isOfflineMode -> stringResource(R.string.offline_profile_subtitle)
                                isKaryawan -> stringResource(R.string.settings_profile_account_karyawan)
                                else -> stringResource(R.string.settings_profile_account_owner)
                            }
                            Text(
                                text = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: defaultName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUser?.email?.takeIf { it.isNotBlank() } ?: defaultAccount,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isOfflineMode) {
                                    showExitOfflineDialog = true
                                } else {
                                    showSignOutDialog = true
                                }
                            },
                            modifier = Modifier
                                .size(SantoyaSize.touchTarget)
                                .testTag("btn_sign_out")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = stringResource(R.string.settings_sign_out_cd),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val badgeText = when {
                        isOfflineMode -> stringResource(R.string.offline_mode_badge)
                        isKaryawan -> stringResource(R.string.settings_profile_badge_karyawan)
                        else -> stringResource(R.string.settings_profile_badge_owner)
                    }
                    val badgeColor = when {
                        isOfflineMode -> MaterialTheme.santoyaColors.warning
                        isKaryawan -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.santoyaColors.success
                    }
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(color = badgeColor, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                R.string.settings_profile_stats,
                                pelangganList.size,
                                produkList.size,
                                transaksiList.size
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 1B. Master Data Operasional Depot (Produk & Harga, Kurir & Komisi, Perawatan Mesin)
        if (!isKaryawan) {
            item {
                SettingsSectionTitle(stringResource(R.string.settings_section_depot))
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(SantoyaRadius.lg),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsMenuRow(
                            title = stringResource(R.string.settings_menu_produk_title),
                            subtitle = stringResource(R.string.settings_menu_produk_subtitle, produkList.size),
                            iconColor = MaterialTheme.santoyaColors.success,
                            onClick = { viewModel.navigateTo(SantoyaScreen.PRODUK) },
                            modifier = Modifier.testTag("settings_nav_produk_button"),
                            iconContent = {
                                com.example.ui.components.HydroGalonIcon(
                                    tint = MaterialTheme.santoyaColors.success,
                                    leafAccent = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        SettingsMenuRow(
                            title = stringResource(R.string.settings_menu_kurir_title),
                            subtitle = stringResource(R.string.settings_menu_kurir_subtitle, kurirList.size),
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = { viewModel.navigateTo(SantoyaScreen.KURIR) },
                            modifier = Modifier.testTag("settings_nav_kurir_button"),
                            iconContent = {
                                com.example.ui.components.HydroCourierWaveIcon(
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        val dueCount = perawatanEvaluations.count { it.isReminderDue }
                        SettingsMenuRow(
                            title = stringResource(R.string.settings_menu_perawatan_title),
                            subtitle = if (dueCount > 0) {
                                stringResource(R.string.settings_menu_perawatan_subtitle_alert, perawatanEvaluations.size, dueCount)
                            } else {
                                stringResource(R.string.settings_menu_perawatan_subtitle_normal, perawatanEvaluations.size)
                            },
                            iconColor = MaterialTheme.santoyaColors.warning,
                            onClick = { viewModel.navigateTo(SantoyaScreen.PERAWATAN) },
                            modifier = Modifier.testTag("settings_nav_perawatan_button"),
                            iconContent = {
                                com.example.ui.components.HydroMaintenanceWrenchIcon(
                                    tint = MaterialTheme.santoyaColors.warning,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // 2. Sinkronisasi Antar Perangkat (Gabungan Cloud + Offline-First)
        item {
            SettingsSectionTitle(stringResource(R.string.settings_section_sync))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(SantoyaRadius.lg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.md),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_sync_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isOfflineMode) {
                                    stringResource(R.string.offline_profile_subtitle)
                                } else {
                                    stringResource(R.string.settings_sync_subtitle)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        RealtimeSyncStatusPill(syncState = syncState)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = if (isOfflineMode) MaterialTheme.santoyaColors.warning else MaterialTheme.santoyaColors.success,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOfflineMode) {
                                        stringResource(R.string.offline_mode_badge)
                                    } else {
                                        stringResource(R.string.settings_sync_offline_note)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (isOfflineMode) {
                                Text(
                                    text = stringResource(R.string.offline_connect_google_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (syncState.pendingCount > 0) {
                                    Text(
                                        text = stringResource(R.string.offline_sync_sheet_merge_ready, syncState.pendingCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.santoyaColors.warning
                                    )
                                }
                                if (syncState.pendingByCollection.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SyncQueueBreakdownChips(pendingByCollection = syncState.pendingByCollection)
                                }
                            } else {
                                if (syncState.pendingCount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_sync_pending_count, syncState.pendingCount),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.santoyaColors.warning,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (!isSyncingCloud) {
                                            TextButton(
                                                onClick = { viewModel.retryPendingSyncNow() },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier
                                                    .heightIn(min = 32.dp)
                                                    .testTag("btn_retry_pending_queue_now")
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.sync_btn_retry_queue_now),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                if (syncState.pendingByCollection.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    SyncQueueBreakdownChips(pendingByCollection = syncState.pendingByCollection)
                                    Spacer(modifier = Modifier.height(2.dp))
                                }

                                if (syncState.isAutoRetryScheduled && syncState.autoRetryAttempt > 0) {
                                    Text(
                                        text = stringResource(
                                            R.string.sync_auto_retry_scheduled_note,
                                            syncState.autoRetryAttempt,
                                            syncState.maxAutoRetryAttempts
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.santoyaColors.warning
                                    )
                                }

                                if (syncState.expectedListenerCount > 0) {
                                    val isHealthy = syncState.activeListenerCount >= syncState.expectedListenerCount
                                    val healthColor = if (isHealthy) {
                                        MaterialTheme.santoyaColors.success
                                    } else {
                                        MaterialTheme.santoyaColors.warning
                                    }
                                    val healthNote = if (isHealthy) {
                                        stringResource(R.string.sync_channel_health_healthy)
                                    } else {
                                        stringResource(R.string.sync_channel_health_degraded)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(color = healthColor, shape = CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${
                                                stringResource(
                                                    R.string.sync_channel_health_format,
                                                    syncState.activeListenerCount,
                                                    syncState.expectedListenerCount
                                                )
                                            } • $healthNote",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                val lastSyncedAt = syncState.lastSyncedAt
                                if (lastSyncedAt != null) {
                                    Text(
                                        text = stringResource(R.string.settings_sync_last_synced, FormatUtils.formatDateTime(lastSyncedAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.settings_sync_realtime_note),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                lastSyncSummary?.let { summary ->
                                    Text(
                                        text = if (isKaryawan) {
                                            stringResource(
                                                R.string.settings_sync_summary_recap,
                                                summary.pelangganCount,
                                                summary.transaksiCount
                                            )
                                        } else {
                                            stringResource(
                                                R.string.settings_sync_summary_recap_full,
                                                summary.pelangganCount,
                                                summary.transaksiCount,
                                                summary.produkCount,
                                                summary.kurirCount,
                                                summary.pengeluaranCount
                                            )
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    val activeStageLabel = syncState.syncStageLabel?.takeIf { it.isNotBlank() }
                        ?: if (isSyncingCloud) {
                            if (activeSyncPushMode == false) {
                                stringResource(R.string.settings_sync_pull_progress)
                            } else {
                                stringResource(R.string.settings_sync_push_progress)
                            }
                        } else {
                            null
                        }

                    AnimatedVisibility(visible = isSyncingCloud || activeStageLabel != null) {
                        SyncActiveStageProgressBox(
                            stageLabel = activeStageLabel ?: stringResource(R.string.settings_sync_push_progress),
                            progressFraction = syncState.syncProgressFraction,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .testTag("banner_sync_loading")
                        )
                    }

                    val effectiveSyncError = cloudSyncError?.takeIf { it.isNotBlank() }
                        ?: syncState.errorMessage?.takeIf { it.isNotBlank() }
                    AnimatedVisibility(
                        visible = !isSyncingCloud &&
                            (effectiveSyncError != null || syncState.status == SyncConnectionStatus.ERROR)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.sm),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .testTag("banner_sync_error")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_sync_error_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = effectiveSyncError ?: stringResource(R.string.settings_sync_error_default),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                if (currentUser != null) {
                                    TextButton(
                                        onClick = {
                                            viewModel.clearCloudSyncError()
                                            viewModel.syncToCloud(context, currentUser.uid)
                                        },
                                        modifier = Modifier.heightIn(min = SantoyaSize.touchTarget)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.action_retry),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isOfflineMode) {
                        Button(
                            onClick = {
                                GoogleSignInHelper.signIn(
                                    context = context,
                                    credentialManager = credentialManager,
                                    coroutineScope = coroutineScope,
                                    onLoadingChange = { isLinkingGoogle = it },
                                    onSuccess = onGoogleLinked,
                                    onError = { msg -> viewModel.showSnackbar(msg) }
                                )
                            },
                            enabled = !isLinkingGoogle,
                            shape = RoundedCornerShape(SantoyaRadius.md),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(SantoyaSize.touchTarget)
                                .testTag("btn_connect_google_offline")
                        ) {
                            if (isLinkingGoogle) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.offline_connect_google_btn),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showExitOfflineDialog = true },
                                enabled = !isLinkingGoogle,
                                shape = RoundedCornerShape(SantoyaRadius.md),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(SantoyaSize.touchTarget)
                                    .testTag("btn_exit_offline_to_auth")
                            ) {
                                Text(
                                    text = stringResource(R.string.offline_exit_to_login_btn),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = { showResetOfflineDialog = true },
                                enabled = !isLinkingGoogle,
                                shape = RoundedCornerShape(SantoyaRadius.md),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(SantoyaSize.touchTarget)
                                    .testTag("btn_reset_offline_data")
                            ) {
                                Text(
                                    text = stringResource(R.string.offline_reset_data_btn),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val uid = currentUser?.uid
                                    if (!uid.isNullOrBlank()) {
                                        viewModel.syncToCloud(context, uid)
                                    }
                                },
                                enabled = !isSyncingCloud && currentUser != null,
                                shape = RoundedCornerShape(SantoyaRadius.md),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = if (isKaryawan) {
                                    Modifier
                                        .fillMaxWidth()
                                        .height(SantoyaSize.touchTarget)
                                        .testTag("btn_sync_to_cloud")
                                } else {
                                    Modifier
                                        .weight(1f)
                                        .height(SantoyaSize.touchTarget)
                                        .testTag("btn_sync_to_cloud")
                                }
                            ) {
                                if (isSyncingCloud && activeSyncPushMode != false) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.settings_sync_btn_backing_up),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.settings_sync_btn_backup),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!isKaryawan) {
                                OutlinedButton(
                                    onClick = { showRestoreCloudDialog = true },
                                    enabled = !isSyncingCloud && currentUser != null,
                                    shape = RoundedCornerShape(SantoyaRadius.md),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(SantoyaSize.touchTarget)
                                        .testTag("btn_restore_from_cloud")
                                ) {
                                    if (isSyncingCloud && activeSyncPushMode == false) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.settings_sync_btn_restoring),
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.CloudDownload,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.settings_sync_btn_restore),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showForcePushCloudDialog = true },
                            enabled = !isSyncingCloud && currentUser != null,
                            shape = RoundedCornerShape(SantoyaRadius.md),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(SantoyaSize.touchTarget)
                                .testTag("btn_force_push_all_cloud")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sync_btn_force_push_all),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Laporan & File Cadangan
        item {
            SettingsSectionTitle(stringResource(R.string.settings_section_backup))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(SantoyaRadius.lg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val json = ExportHelper.generateNestedJson(
                                    pelangganList = pelangganList,
                                    transaksiList = transaksiList,
                                    produkList = produkList.ifEmpty { viewModel.allProduk.value },
                                    kurirList = kurirList.ifEmpty { viewModel.allKurir.value },
                                    pengeluaranList = viewModel.allPengeluaran.value,
                                    kasbonLogList = viewModel.allKasbonLogs.value,
                                    perawatanList = perawatanEvaluations.map { it.item },
                                    perawatanLogList = viewModel.allPerawatanLogs.value,
                                    depotProfile = depotProfile
                                )
                                ExportHelper.shareExportFile(
                                    context = context,
                                    filename = "santoya_backup_nested_${System.currentTimeMillis()}.json",
                                    content = json,
                                    mimeType = "application/json"
                                )
                                viewModel.showSnackbar(context.getString(R.string.settings_export_json_success))
                            },
                            shape = RoundedCornerShape(SantoyaRadius.md),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(SantoyaSize.touchTarget)
                                .testTag("btn_export_json")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.settings_backup_btn_json),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val csv = ExportHelper.generateCsv(
                                    transaksiList = transaksiList,
                                    pengeluaranList = viewModel.allPengeluaran.value,
                                    kasbonLogList = viewModel.allKasbonLogs.value,
                                    pelangganList = pelangganList
                                )
                                ExportHelper.shareExportFile(
                                    context = context,
                                    filename = "santoya_laporan_${System.currentTimeMillis()}.csv",
                                    content = csv,
                                    mimeType = "text/csv"
                                )
                                viewModel.showSnackbar(context.getString(R.string.settings_export_csv_success))
                            },
                            shape = RoundedCornerShape(SantoyaRadius.md),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(SantoyaSize.touchTarget)
                                .testTag("btn_export_csv")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.settings_backup_btn_excel),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!isKaryawan) {
                        Spacer(modifier = Modifier.height(10.dp))

                        FilledTonalButton(
                            onClick = { showImportDialog = true },
                            shape = RoundedCornerShape(SantoyaRadius.md),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(SantoyaSize.touchTarget)
                                .testTag("btn_import_json")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_backup_btn_restore_file),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (ExportHelper.hasAutoBackup(context)) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showRestoreAutoBackupDialog = true },
                                shape = RoundedCornerShape(SantoyaRadius.md),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(SantoyaSize.touchTarget)
                                    .testTag("btn_restore_auto_backup")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.settings_backup_btn_restore_auto),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.settings_backup_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 5. Tentang Aplikasi
        item {
            SettingsSectionTitle(stringResource(R.string.settings_section_about))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(SantoyaRadius.lg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.md),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.settings_about_app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_about_app_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SystemInfoRow(
                            label = stringResource(R.string.settings_about_version_label),
                            value = "v${BuildConfig.VERSION_NAME}"
                        )
                        SystemInfoRow(
                            label = stringResource(R.string.settings_about_storage_label),
                            value = stringResource(R.string.settings_about_storage_value)
                        )
                        SystemInfoRow(
                            label = stringResource(R.string.settings_about_security_label),
                            value = stringResource(R.string.settings_about_security_value)
                        )
                        SystemInfoRow(
                            label = stringResource(R.string.settings_about_status_label),
                            value = if (isOfflineMode) {
                                stringResource(R.string.offline_mode_badge)
                            } else {
                                stringResource(R.string.settings_about_status_connected)
                            },
                            valueColor = if (isOfflineMode) {
                                MaterialTheme.santoyaColors.warning
                            } else {
                                MaterialTheme.santoyaColors.success
                            },
                            showStatusDot = true
                        )
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        ImportJsonDialog(
            onDismiss = { showImportDialog = false },
            onImportJson = { jsonStr ->
                viewModel.importJsonData(jsonStr)
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(stringResource(R.string.settings_sign_out_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(stringResource(R.string.settings_sign_out_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("btn_confirm_sign_out")
                ) {
                    Text(stringResource(R.string.action_sign_out))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false },
                    modifier = Modifier.testTag("btn_cancel_sign_out")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showExitOfflineDialog) {
        AlertDialog(
            onDismissRequest = { showExitOfflineDialog = false },
            title = {
                Text(stringResource(R.string.offline_exit_dialog_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(stringResource(R.string.offline_exit_dialog_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitOfflineDialog = false
                        onExitOfflineToAuth()
                    },
                    modifier = Modifier.testTag("btn_confirm_exit_offline")
                ) {
                    Text(stringResource(R.string.offline_exit_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitOfflineDialog = false },
                    modifier = Modifier.testTag("btn_cancel_exit_offline")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showResetOfflineDialog) {
        AlertDialog(
            onDismissRequest = { showResetOfflineDialog = false },
            title = {
                Text(stringResource(R.string.offline_reset_dialog_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(stringResource(R.string.offline_reset_dialog_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetOfflineDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("btn_confirm_reset_offline")
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetOfflineDialog = false },
                    modifier = Modifier.testTag("btn_cancel_reset_offline")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showRestoreCloudDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreCloudDialog = false },
            title = {
                Text(stringResource(R.string.settings_sync_restore_dialog_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(stringResource(R.string.settings_sync_restore_dialog_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreCloudDialog = false
                        val uid = currentUser?.uid
                        if (!uid.isNullOrBlank()) {
                            viewModel.restoreFromCloud(context, uid)
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_restore_cloud")
                ) {
                    Text(stringResource(R.string.settings_sync_btn_restore))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreCloudDialog = false },
                    modifier = Modifier.testTag("btn_cancel_restore_cloud")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showForcePushCloudDialog) {
        AlertDialog(
            onDismissRequest = { showForcePushCloudDialog = false },
            title = {
                Text(stringResource(R.string.sync_force_push_dialog_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(stringResource(R.string.sync_force_push_dialog_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForcePushCloudDialog = false
                        val uid = currentUser?.uid
                        if (!uid.isNullOrBlank()) {
                            viewModel.forcePushAllLocalToCloud(context, uid)
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_force_push_cloud")
                ) {
                    Text(stringResource(R.string.sync_force_push_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForcePushCloudDialog = false },
                    modifier = Modifier.testTag("btn_cancel_force_push_cloud")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showRestoreAutoBackupDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreAutoBackupDialog = false },
            title = {
                Text(stringResource(R.string.settings_backup_dialog_auto_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(stringResource(R.string.settings_backup_dialog_auto_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreAutoBackupDialog = false
                        val autoJson = ExportHelper.readAutoBackupJson(context)
                        if (autoJson != null) {
                            viewModel.importJsonData(autoJson)
                        } else {
                            viewModel.showSnackbar(context.getString(R.string.import_dialog_read_error))
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_restore_auto_backup")
                ) {
                    Text(stringResource(R.string.settings_backup_dialog_auto_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreAutoBackupDialog = false },
                    modifier = Modifier.testTag("btn_cancel_restore_auto_backup")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDepotProfileSheet) {
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

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SystemInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    showStatusDot: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showStatusDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(color = valueColor, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SettingsMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SantoyaSize.touchTarget)
            .pressClickEffect(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(SantoyaRadius.md),
            color = iconColor.copy(alpha = 0.14f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (iconContent != null) {
                    iconContent()
                } else if (icon != null) {
                    Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(15.dp)
        )
    }
}
