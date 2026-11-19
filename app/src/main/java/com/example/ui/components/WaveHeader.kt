package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.HeroGlowCyan
import com.example.ui.theme.HeroGlowCyanSoft
import com.example.ui.theme.HeroGlowEmeraldSoft
import com.example.ui.theme.PrimaryContainerDark
import com.example.ui.theme.SantoyaNavy
import com.example.ui.theme.SantoyaNavyPanel
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaWaterBlue
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.santoyaColors
import com.example.util.RealtimeSyncState
import kotlin.math.PI
import kotlin.math.sin

/**
 * Header beranda animatif berbasis Jetpack Compose Canvas API yang menampilkan
 * efek gelombang air biru gradasi multi-lapis untuk memperkuat identitas Hydro-Fintech Santoya RO.
 */
@Composable
fun WaveHeader(
    title: String,
    subtitle: String,
    syncState: RealtimeSyncState,
    isKaryawan: Boolean,
    onSyncClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignOutClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDark = santoyaColors.isDark

    val infiniteTransition = rememberInfiniteTransition(label = "wave_header_transition")
    val phaseBack by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_header_phase_back"
    )
    val phaseMid by infiniteTransition.animateFloat(
        initialValue = (2f * PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_header_phase_mid"
    )
    val phaseFront by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = (2f * PI).toFloat() + 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_header_phase_front"
    )

    // Pre-alokasi objek Path agar frame animasi 60fps bebas alokasi memori berulang
    val backWavePath = remember { Path() }
    val midWavePath = remember { Path() }
    val frontWavePath = remember { Path() }
    val crestStrokePath = remember { Path() }

    val baseGradientColors = remember(isDark) {
        if (isDark) {
            listOf(SurfaceDark, PrimaryContainerDark, SantoyaNavyPanel)
        } else {
            listOf(SantoyaNavyPanel, SantoyaNavy, SantoyaWaterBlue)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_wave_header")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            // 1. Latar biru gradasi diagonal Hydro-Fintech
            drawRect(
                brush = Brush.linearGradient(
                    colors = baseGradientColors,
                    start = Offset.Zero,
                    end = Offset(w, h)
                )
            )

            // Cahaya riak halus di kanan atas untuk kedalaman permukaan air
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        HeroGlowCyanSoft.copy(alpha = 0.16f),
                        HeroGlowCyan.copy(alpha = 0.0f)
                    ),
                    center = Offset(w * 0.88f, h * 0.15f),
                    radius = w * 0.36f
                ),
                center = Offset(w * 0.88f, h * 0.15f),
                radius = w * 0.36f
            )

            val stepPx = 12f

            // 2. Gelombang Lapis Belakang (Biru Cyan Gradasi)
            backWavePath.reset()
            val backBaseY = h * 0.68f
            val backAmp = 7.dp.toPx()
            backWavePath.moveTo(0f, h)
            backWavePath.lineTo(0f, backBaseY)
            var xBack = 0f
            while (xBack <= w + stepPx) {
                val angle = (xBack / w) * 2.0f * PI.toFloat() + phaseBack
                val y = backBaseY + sin(angle) * backAmp
                backWavePath.lineTo(xBack, y)
                xBack += stepPx
            }
            backWavePath.lineTo(w, h)
            backWavePath.close()
            drawPath(
                path = backWavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        HeroGlowCyanSoft.copy(alpha = 0.28f),
                        SantoyaWaterBlue.copy(alpha = 0.14f)
                    ),
                    startY = backBaseY - backAmp,
                    endY = h
                )
            )

            // 3. Gelombang Lapis Tengah (Aqua -> Sentuhan Hijau Daun Santoya)
            midWavePath.reset()
            val midBaseY = h * 0.76f
            val midAmp = 5.5.dp.toPx()
            midWavePath.moveTo(0f, h)
            midWavePath.lineTo(0f, midBaseY)
            var xMid = 0f
            while (xMid <= w + stepPx) {
                val angle = (xMid / w) * 2.5f * PI.toFloat() + phaseMid
                val y = midBaseY + sin(angle) * midAmp
                midWavePath.lineTo(xMid, y)
                xMid += stepPx
            }
            midWavePath.lineTo(w, h)
            midWavePath.close()
            drawPath(
                path = midWavePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        HeroGlowCyan.copy(alpha = 0.34f),
                        HeroGlowCyanSoft.copy(alpha = 0.26f),
                        HeroGlowEmeraldSoft.copy(alpha = 0.22f)
                    )
                )
            )

            // 4. Gelombang Penutup Latar Belakang (Memadukan header ke konten dashboard)
            frontWavePath.reset()
            crestStrokePath.reset()
            val frontBaseY = h * 0.86f
            val frontAmp = 4.5.dp.toPx()
            frontWavePath.moveTo(0f, h)
            var xFront = 0f
            var isFirst = true
            while (xFront <= w + stepPx) {
                val angle = (xFront / w) * 2.15f * PI.toFloat() + phaseFront
                val y = frontBaseY + sin(angle) * frontAmp
                if (isFirst) {
                    frontWavePath.lineTo(xFront, y)
                    crestStrokePath.moveTo(xFront, y)
                    isFirst = false
                } else {
                    frontWavePath.lineTo(xFront, y)
                    crestStrokePath.lineTo(xFront, y)
                }
                xFront += stepPx
            }
            frontWavePath.lineTo(w, h)
            frontWavePath.close()

            drawPath(
                path = crestStrokePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        HeroGlowCyanSoft.copy(alpha = 0.65f),
                        HeroGlowEmeraldSoft.copy(alpha = 0.50f),
                        HeroGlowCyanSoft.copy(alpha = 0.65f)
                    )
                ),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = frontWavePath,
                color = backgroundColor
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isCompact) 12.dp else 16.dp,
                    end = if (isCompact) 8.dp else 12.dp,
                    top = if (isCompact) 10.dp else 12.dp,
                    bottom = if (isCompact) 22.dp else 26.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, HeroGlowCyanSoft.copy(alpha = 0.6f)),
                    modifier = Modifier.size(if (isCompact) 38.dp else 42.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_santoya),
                            contentDescription = stringResource(R.string.dashboard_logo_cd),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(if (isCompact) 26.dp else 28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = santoyaColors.onHero,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = santoyaColors.onHero.copy(alpha = 0.86f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Wadah terang di atas gelombang biru agar kontras status pill tetap optimal
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.xl),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, HeroGlowCyanSoft.copy(alpha = 0.45f))
                ) {
                    RealtimeSyncStatusPill(
                        syncState = syncState,
                        compact = true,
                        onClick = onSyncClick,
                        modifier = Modifier.testTag("dashboard_sync_pill")
                    )
                }

                if (!isKaryawan) {
                    SantoyaTopBarIconButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.dashboard_settings_cd),
                        onClick = onSettingsClick,
                        tint = santoyaColors.onHero,
                        containerColor = SantoyaNavyPanel.copy(alpha = 0.68f),
                        borderColor = HeroGlowCyanSoft.copy(alpha = 0.42f),
                        modifier = Modifier.testTag("dashboard_settings_button")
                    )
                } else if (onSignOutClick != null) {
                    SantoyaTopBarIconButton(
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = stringResource(R.string.dashboard_sign_out_cd),
                        onClick = onSignOutClick,
                        tint = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                        modifier = Modifier.testTag("dashboard_karyawan_sign_out_button")
                    )
                }
            }
        }
    }
}
