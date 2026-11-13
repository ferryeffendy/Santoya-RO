package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.santoyaColors

import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import com.example.util.RealtimeSyncState
import com.example.util.SyncConnectionStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * Real-time Cloud Firestore Sync Status Pill shared across Dashboard & Settings.
 */
@Composable
fun RealtimeSyncStatusPill(
    syncState: RealtimeSyncState,
    modifier: Modifier = Modifier
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
            Icons.Default.CloudDone,
            "Terhubung",
            santoyaColors.success,
            santoyaColors.successContainer.copy(alpha = 0.55f),
            santoyaColors.success.copy(alpha = 0.35f)
        )
        SyncConnectionStatus.SYNCING, SyncConnectionStatus.CONNECTING -> Tuple5(
            Icons.Default.CloudSync,
            if (syncState.pendingCount > 0) "Sinkron (${syncState.pendingCount})" else "Menyinkronkan",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        SyncConnectionStatus.OFFLINE -> Tuple5(
            Icons.Default.CloudOff,
            if (syncState.pendingCount > 0) "Offline (${syncState.pendingCount})" else "Mode Offline",
            santoyaColors.warning,
            santoyaColors.warningContainer.copy(alpha = 0.6f),
            santoyaColors.warning.copy(alpha = 0.45f)
        )
        SyncConnectionStatus.REVOKED -> Tuple5(
            Icons.Default.Block,
            "Akses Nonaktif",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        )
        SyncConnectionStatus.ERROR -> Tuple5(
            Icons.Default.CloudOff,
            "Perlu Cek Koneksi",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        )
    }

    val isRotating = syncState.status == SyncConnectionStatus.SYNCING ||
        syncState.status == SyncConnectionStatus.CONNECTING

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
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
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1
            )
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
        shape = RoundedCornerShape(20.dp),
        color = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else santoyaColors.successContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else santoyaColors.success.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CheckCircle,
                contentDescription = if (isConnected) "Sinkronisasi Cloud Aktif" else "Tersimpan di HP",
                tint = if (isConnected) MaterialTheme.colorScheme.primary else santoyaColors.success,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isConnected) "Terhubung" else "Tersimpan di HP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
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
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    accentColor: Color,
    onAccentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
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
                shape = RoundedCornerShape(8.dp),
                color = accentColor
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = onAccentColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    maxLines = 1
                )
            }
        }
    }
}
