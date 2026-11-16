package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.SantoyaSpacing
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.RealtimeSyncState
import com.example.util.SyncConnectionStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Real-time Cloud Firestore Sync Status Pill shared across Dashboard, TopAppBar, & Settings.
 */
@Composable
fun RealtimeSyncStatusPill(
    syncState: RealtimeSyncState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_icon_spin"
    )

    val (icon, label, tint, bgColor, borderColor) = when (syncState.status) {
        SyncConnectionStatus.LIVE -> Tuple5(
            Icons.Outlined.CloudDone,
            "Terhubung",
            santoyaColors.success,
            santoyaColors.successContainer.copy(alpha = 0.55f),
            santoyaColors.success.copy(alpha = 0.35f)
        )
        SyncConnectionStatus.SYNCING, SyncConnectionStatus.CONNECTING -> Tuple5(
            Icons.Outlined.CloudSync,
            if (syncState.pendingCount > 0) "Sinkron (${syncState.pendingCount})" else "Sinkron...",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        SyncConnectionStatus.OFFLINE -> Tuple5(
            Icons.Outlined.CloudOff,
            if (syncState.pendingCount > 0) "Offline (${syncState.pendingCount})" else "Mode Offline",
            santoyaColors.warning,
            santoyaColors.warningContainer.copy(alpha = 0.6f),
            santoyaColors.warning.copy(alpha = 0.45f)
        )
        SyncConnectionStatus.REVOKED, SyncConnectionStatus.AWAITING_ROLE_CONFIRMATION -> Tuple5(
            Icons.Outlined.Block,
            "Dibatasi",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        )
        SyncConnectionStatus.ERROR -> Tuple5(
            Icons.Outlined.CloudOff,
            "Cek Koneksi",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        )
    }

    val isRotating = syncState.status == SyncConnectionStatus.SYNCING ||
        syncState.status == SyncConnectionStatus.CONNECTING
    val shape = RoundedCornerShape(SantoyaRadius.xl)
    val clickModifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        shape = shape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.then(clickModifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier
                    .size(15.dp)
                    .graphicsLayer {
                        if (isRotating) rotationZ = rotation
                    }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1
            )
        }
    }
}

/**
 * Bottom sheet displaying detailed cloud sync status and manual sync trigger.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SantoyaSyncInfoBottomSheet(
    syncState: RealtimeSyncState,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null,
    onSwitchToStaffWaiting: (() -> Unit)? = null
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val isSyncing = syncState.status == SyncConnectionStatus.SYNCING ||
        syncState.status == SyncConnectionStatus.CONNECTING

    val (statusTitle, statusDesc, statusColor, statusBg, statusIcon) = when (syncState.status) {
        SyncConnectionStatus.LIVE -> Tuple5(
            "Terhubung Real-Time",
            "Database terhubung ke Firebase Firestore. Semua data dan perubahan otomatis tersinkron.",
            santoyaColors.success,
            santoyaColors.successContainer.copy(alpha = 0.5f),
            Icons.Outlined.CloudDone
        )
        SyncConnectionStatus.SYNCING, SyncConnectionStatus.CONNECTING -> Tuple5(
            "Sedang Menyinkronkan...",
            "Mengirim dan menerima pembaruan data antara HP dan Cloud Firestore.",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            Icons.Outlined.CloudSync
        )
        SyncConnectionStatus.OFFLINE -> Tuple5(
            "Mode Offline",
            "Tidak ada koneksi internet. Data transaksi tetap tersimpan aman di HP dan akan otomatis dikirim saat online.",
            santoyaColors.warning,
            santoyaColors.warningContainer.copy(alpha = 0.5f),
            Icons.Outlined.CloudOff
        )
        SyncConnectionStatus.REVOKED, SyncConnectionStatus.AWAITING_ROLE_CONFIRMATION -> Tuple5(
            "Akses Dibatasi",
            "Akun ini belum memiliki izin akses depot atau dinonaktifkan oleh Pemilik.",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            Icons.Outlined.Block
        )
        SyncConnectionStatus.ERROR -> Tuple5(
            "Gagal Terhubung",
            syncState.errorMessage ?: "Periksa koneksi data seluler atau WiFi perangkat Anda.",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            Icons.Outlined.CloudOff
        )
    }

    SantoyaFormBottomSheet(
        title = "Status Sinkronisasi Cloud",
        subtitle = "Google Firebase Firestore",
        headerIcon = statusIcon,
        headerAccent = statusColor,
        confirmLabel = if (isSyncing) "Menyinkronkan..." else "Sinkronkan Sekarang",
        confirmEnabled = !isSyncing,
        confirmContainerColor = MaterialTheme.colorScheme.primary,
        dismissLabel = "Tutup",
        onDismiss = onDismiss,
        onConfirm = onSyncNow
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = statusBg,
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = statusDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HydroBentoCard(
            accentColor = MaterialTheme.colorScheme.primary,
            cornerRadius = SantoyaRadius.lg,
            tintAlpha = 0.04f,
            showWaterRipple = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Antrean Data Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (syncState.pendingCount > 0) "${syncState.pendingCount} data tertunda" else "Semua data aman (0)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (syncState.pendingCount > 0) santoyaColors.warning else santoyaColors.success
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terakhir Sinkron",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = syncState.lastSyncedAt?.let { FormatUtils.formatDateTime(it) } ?: "Belum pernah",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (syncState.ownerEmail.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Email Depot",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = syncState.ownerEmail,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (onNavigateToSettings != null) {
            OutlinedButton(
                onClick = onNavigateToSettings,
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buka Pengaturan Lengkap",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (onSwitchToStaffWaiting != null) {
            OutlinedButton(
                onClick = onSwitchToStaffWaiting,
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_switch_to_staff_waiting")
            ) {
                Text(
                    text = stringResource(R.string.sync_sheet_switch_to_staff_btn),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}



private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)

/**
 * Unified Local Storage & Cloud Status Pill shared by Dashboard & sub-screen TopAppBars.
 */
@Composable
fun LocalStorageStatusPill(
    modifier: Modifier = Modifier,
    isCloudActive: Boolean = Firebase.auth.currentUser != null
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val isConnected = isCloudActive
    Surface(
        shape = RoundedCornerShape(SantoyaRadius.xl),
        color = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else santoyaColors.successContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else santoyaColors.success.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Outlined.CloudDone else Icons.Outlined.CheckCircle,
                contentDescription = if (isConnected) "Sinkronisasi Cloud Aktif" else "Lokal HP",
                tint = if (isConnected) MaterialTheme.colorScheme.primary else santoyaColors.success,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isConnected) "Terhubung" else "Lokal HP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else santoyaColors.onSuccessContainer
            )
        }
    }
}

/**
 * Unified Section Header Banner for Master Screens (Produk, Kurir, etc.)
 * replacing fragmented container colors with a cohesive Fintech surface + accent pill.
 */
@Composable
fun SantoyaSectionBanner(
    icon: ImageVector? = null,
    title: String,
    subtitle: String,
    badgeText: String,
    accentColor: Color,
    onAccentColor: Color,
    modifier: Modifier = Modifier,
    iconContent: (@Composable () -> Unit)? = null
) {
    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.09f,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SantoyaSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HydroGradientBadge(
                gradientColors = listOf(
                    accentColor,
                    accentColor.copy(alpha = 0.78f)
                ),
                size = 44.dp,
                cornerRadius = SantoyaRadius.md
            ) {
                if (iconContent != null) {
                    iconContent()
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = onAccentColor,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(SantoyaRadius.sm),
                color = accentColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.30f))
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Returns true whenever the soft keyboard (IME) is currently visible.
 */
@Composable
fun rememberIsImeVisible(): Boolean {
    val density = LocalDensity.current
    return WindowInsets.ime.getBottom(density) > 0
}

/**
 * Automatically scrolls the focused text field cleanly above the soft keyboard.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bringIntoViewOnFocus(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    this
        .bringIntoViewRequester(requester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                scope.launch {
                    delay(180L)
                    requester.bringIntoView()
                }
            }
        }
}

/**
 * Adaptive ModalBottomSheet designed for Santoya RO forms:
 * - Follows IME height smoothly with navigationBarsPadding + imePadding
 * - Scrollable content body that never gets clipped
 * - Sticky bottom action footer with 48dp touch targets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SantoyaFormBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    subtitle: String? = null,
    headerIcon: ImageVector? = null,
    headerAccent: Color = MaterialTheme.colorScheme.primary,
    dismissLabel: String = "Batal",
    confirmEnabled: Boolean = true,
    confirmContainerColor: Color = MaterialTheme.colorScheme.primary,
    confirmTestTag: String? = null,
    headerIconContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (headerIconContent != null || headerIcon != null) {
                        HydroGradientBadge(
                            gradientColors = listOf(headerAccent, AccentCyan),
                            size = 38.dp,
                            cornerRadius = 12.dp,
                            elevation = 2.dp
                        ) {
                            if (headerIconContent != null) {
                                headerIconContent()
                            } else if (headerIcon != null) {
                                Icon(
                                    imageVector = headerIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.santoyaColors.onHero,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
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
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = dismissLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                val confirmModifier = Modifier
                    .weight(1.35f)
                    .heightIn(min = 48.dp)
                    .let { if (confirmTestTag != null) it.testTag(confirmTestTag) else it }
                Button(
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    colors = ButtonDefaults.buttonColors(containerColor = confirmContainerColor),
                    modifier = confirmModifier
                ) {
                    Text(
                        text = confirmLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Unified Empty State Card with borderless Light/Dark-adaptive flat vector illustrations and contextual actions.
 */
@Composable
fun SantoyaEmptyState(
    emptyType: SantoyaEmptyType,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    compact: Boolean = false,
    primaryActionLabel: String? = null,
    primaryActionIcon: ImageVector? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    secondaryActionIcon: ImageVector? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = 20.dp,
        tintAlpha = 0.05f,
        showWaterRipple = false,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = if (compact) 18.dp else 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SantoyaEmptyIllustration(
                type = emptyType,
                accentColor = accentColor,
                compact = compact,
                description = title
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if ((primaryActionLabel != null && onPrimaryAction != null) ||
                (secondaryActionLabel != null && onSecondaryAction != null)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (secondaryActionLabel != null && onSecondaryAction != null) {
                        OutlinedButton(
                            onClick = onSecondaryAction,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            if (secondaryActionIcon != null) {
                                Icon(
                                    imageVector = secondaryActionIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = secondaryActionLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (primaryActionLabel != null && onPrimaryAction != null) {
                        Button(
                            onClick = onPrimaryAction,
                            shape = RoundedCornerShape(SantoyaRadius.md),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            if (primaryActionIcon != null) {
                                Icon(
                                    imageVector = primaryActionIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = primaryActionLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Adaptive Currency Text component that automatically downscales font size in constrained
 * layouts (e.g. Bento cards, financial summaries, narrow dialog rows) so that large Rupiah
 * figures (tens or hundreds of millions) NEVER truncate with ellipsis or cut off.
 */
@Composable
fun SantoyaAdaptiveCurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    customText: String? = null,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    minFontSize: TextUnit = 11.sp,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1
) {
    val text = remember(amount, customText) { customText ?: FormatUtils.formatRupiah(amount) }
    val baseFontSize = if (style.fontSize.isSp && style.fontSize.value > 0f) style.fontSize else 18.sp
    var scaledFontSize by remember(text, baseFontSize, minFontSize) {
        mutableStateOf(baseFontSize)
    }
    var lastMaxWidthPx by remember { mutableIntStateOf(0) }

    Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            fontSize = scaledFontSize,
            fontWeight = fontWeight ?: style.fontWeight ?: FontWeight.Bold,
            color = color,
            textAlign = textAlign,
            fontFeatureSettings = "tnum"
        ),
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result ->
            val currentMaxWidth = result.layoutInput.constraints.maxWidth
            if (lastMaxWidthPx in 1 until currentMaxWidth && scaledFontSize < baseFontSize) {
                lastMaxWidthPx = currentMaxWidth
                scaledFontSize = baseFontSize
            } else {
                lastMaxWidthPx = currentMaxWidth
                if ((result.didOverflowWidth || result.hasVisualOverflow) &&
                    scaledFontSize.value > minFontSize.value + 0.1f
                ) {
                    val nextSize = (scaledFontSize.value * 0.88f).coerceAtLeast(minFontSize.value)
                    if (nextSize < scaledFontSize.value) {
                        scaledFontSize = nextSize.sp
                    }
                }
            }
        }
    )
}

/**
 * Smooth pulsing skeleton placeholder displayed while Room / Cloud initial data is loading,
 * preventing abrupt flicker to Empty State on startup or account switch.
 */
@Composable
fun SantoyaLoadingSkeleton(
    itemCount: Int = 4,
    message: String = "Memuat & menyinkronkan data...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        repeat(itemCount) {
            HydroBentoCard(
                accentColor = MaterialTheme.colorScheme.primary,
                cornerRadius = SantoyaRadius.lg,
                tintAlpha = 0.04f,
                showWaterRipple = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(42.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulseAlpha),
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .height(14.dp)
                        ) {}
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulseAlpha * 0.8f),
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .height(11.dp)
                        ) {}
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha * 0.75f),
                        modifier = Modifier
                            .width(68.dp)
                            .height(24.dp)
                    ) {}
                }
            }
        }
    }
}

/**
 * Unified top status banner for active background/mutation operations (save, sync, import).
 */
@Composable
fun SantoyaBusyStatusBanner(
    isBusy: Boolean,
    message: String?,
    modifier: Modifier = Modifier
) {
    if (!isBusy || message.isNullOrBlank()) return

    val infiniteTransition = rememberInfiniteTransition(label = "busy_banner_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "busy_icon_rotation"
    )

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(15.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Tombol aksi standar di TopAppBar Santoya RO dengan area sentuh 48dp,
 * lingkaran visual 38dp, dan garis tepi halus 1dp.
 */
@Composable
fun SantoyaTopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(SantoyaSize.touchTarget)
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.size(SantoyaSize.topBarCircle)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Tombol ikon standar Santoya RO.
 *
 * Area sentuh selalu 48dp (nyaman untuk jempol), sedangkan lingkaran warnanya
 * boleh lebih kecil agar tampilan tetap ringkas dan rapi.
 */
@Composable
fun SantoyaIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    enabled: Boolean = true,
    visualSize: Dp = SantoyaSize.iconButtonVisual,
    glyphSize: Dp = SantoyaSize.iconGlyph
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(SantoyaSize.touchTarget)
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            modifier = Modifier.size(visualSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(glyphSize)
                )
            }
        }
    }
}

/**
 * Compact adaptive filter pill without Material 3 FilterChip's 48.dp minimum vertical
 * layout bloat, ensuring multi-row FlowRows and single-row filter bars stay tight and
 * balanced across all Android screen widths.
 *
 * Area sentuh tetap 48dp meskipun bentuk visual pilnya lebih pendek.
 */
@Composable
fun SantoyaFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingIconContent: (@Composable () -> Unit)? = null,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedBorderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surface,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedBorderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    centerContent: Boolean = false
) {
    val shape = RoundedCornerShape(10.dp)
    val containerColor = if (selected) selectedContainerColor else unselectedContainerColor
    val contentColor = if (selected) selectedContentColor else unselectedContentColor
    val borderColor = if (selected) selectedBorderColor else unselectedBorderColor

    Box(
        modifier = modifier
            .heightIn(min = SantoyaSize.touchTarget)
            .clip(shape)
            .semantics {
                this.selected = selected
                this.role = Role.RadioButton
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.heightIn(min = 34.dp)
        ) {
            Row(
                modifier = Modifier.padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (centerContent) Arrangement.Center else Arrangement.Start
            ) {
                if (leadingIconContent != null) {
                    leadingIconContent()
                    Spacer(modifier = Modifier.width(5.dp))
                } else if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Data item for [SantoyaAdaptiveTabPillRow].
 */
@Immutable
data class SantoyaTabPillItem(
    val label: String,
    val badgeCount: Int? = null,
    val isUrgentBadge: Boolean = false,
    val testTag: String? = null
)

/**
 * Full-width single-row capsule pill tab bar styled consistently with [SantoyaFilterPill].
 * Dynamically scales spacing, proportional widths, and label typography so every tab fits
 * cleanly on one row across all phone screen widths without horizontal scrolling or clipping.
 */
@Composable
fun SantoyaAdaptiveTabPillRow(
    items: List<SantoyaTabPillItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val fontScale = density.fontScale

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                val availableWidth = maxWidth
                val horizontalPadding = when {
                    availableWidth < 340.dp -> 6.dp
                    availableWidth < 400.dp -> 10.dp
                    else -> 12.dp
                }
                val itemGap = when {
                    items.size <= 2 -> 8.dp
                    availableWidth < 340.dp -> 3.dp
                    availableWidth < 400.dp -> 5.dp
                    else -> 8.dp
                }
                val innerHorizontalPadding = when {
                    items.size <= 2 -> 12.dp
                    availableWidth < 340.dp -> 3.dp
                    availableWidth < 400.dp -> 5.dp
                    else -> 8.dp
                }
                val baseLabelFontSize = when {
                    items.size <= 2 && availableWidth >= 360.dp -> 13.sp
                    items.size <= 2 -> 12.sp
                    availableWidth < 330.dp -> 9.5.sp
                    availableWidth < 365.dp -> 10.5.sp
                    availableWidth < 400.dp -> 11.5.sp
                    else -> 12.5.sp
                }
                val baseBadgeFontSize = when {
                    items.size <= 2 -> 10.5.sp
                    availableWidth < 330.dp -> 8.sp
                    availableWidth < 365.dp -> 9.sp
                    availableWidth < 400.dp -> 9.5.sp
                    else -> 10.5.sp
                }
                val badgeGap = if (items.size >= 4 && availableWidth < 400.dp) 3.dp else 5.dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(itemGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { idx, item ->
                        val isSelected = selectedIndex == idx
                        val itemWeight = remember(items.size, item.label, item.badgeCount) {
                            if (items.size <= 2) {
                                1f
                            } else {
                                val badgeWeight = item.badgeCount?.toString()?.length?.let { len ->
                                    (len * 0.16f) + 0.28f
                                } ?: 0f
                                ((item.label.length * 0.145f) + 0.20f + badgeWeight).coerceAtLeast(0.88f)
                            }
                        }

                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            animationSpec = tween(180),
                            label = "tab_pill_bg"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(180),
                            label = "tab_pill_fg"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                            },
                            animationSpec = tween(180),
                            label = "tab_pill_border"
                        )

                        val initialFontSize = remember(baseLabelFontSize, fontScale, items.size) {
                            if (items.size >= 4 && fontScale > 1.15f) {
                                (baseLabelFontSize.value / (fontScale / 1.1f)).coerceAtLeast(8f).sp
                            } else {
                                baseLabelFontSize
                            }
                        }
                        var labelFontSize by remember(
                            item.label,
                            item.badgeCount,
                            availableWidth,
                            initialFontSize
                        ) {
                            mutableStateOf(initialFontSize)
                        }
                        val effectiveBadgeFontSize = remember(labelFontSize, baseLabelFontSize, baseBadgeFontSize) {
                            val ratio = (labelFontSize.value / baseLabelFontSize.value).coerceIn(0.7f, 1f)
                            (baseBadgeFontSize.value * ratio).coerceAtLeast(7.5f).sp
                        }

                        val shape = RoundedCornerShape(50)
                        val interactionSource = remember { MutableInteractionSource() }
                        val tagModifier = if (item.testTag != null) {
                            Modifier.testTag(item.testTag)
                        } else {
                            Modifier
                        }

                        Box(
                            modifier = Modifier
                                .weight(itemWeight)
                                .heightIn(min = 48.dp)
                                .then(tagModifier)
                                .semantics {
                                    this.selected = isSelected
                                    this.role = Role.Tab
                                }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onSelect(idx) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = shape,
                                color = containerColor,
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 36.dp)
                                    .clip(shape)
                                    .indication(interactionSource, LocalIndication.current)
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = innerHorizontalPadding,
                                        vertical = 6.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = labelFontSize,
                                            lineHeight = labelFontSize
                                        ),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = contentColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip,
                                        onTextLayout = { result ->
                                            if (result.hasVisualOverflow && labelFontSize.value > 8f) {
                                                labelFontSize = (labelFontSize.value - 0.5f)
                                                    .coerceAtLeast(8f)
                                                    .sp
                                            }
                                        }
                                    )
                                    if (item.badgeCount != null) {
                                        Spacer(modifier = Modifier.width(badgeGap))
                                        val badgeBg = when {
                                            item.isUrgentBadge -> MaterialTheme.colorScheme.errorContainer
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        }
                                        val badgeFg = when {
                                            item.isUrgentBadge -> MaterialTheme.colorScheme.error
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = badgeBg
                                        ) {
                                            Text(
                                                text = item.badgeCount.toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = effectiveBadgeFontSize,
                                                    lineHeight = effectiveBadgeFontSize
                                                ),
                                                fontWeight = FontWeight.Bold,
                                                color = badgeFg,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(
                                                    horizontal = if (items.size >= 4 && availableWidth < 365.dp) 4.dp else 5.dp,
                                                    vertical = 1.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        }
    }
}




