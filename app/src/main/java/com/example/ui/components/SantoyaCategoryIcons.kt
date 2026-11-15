package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SantoyaLeafGreen
import com.example.ui.theme.SantoyaWaterBlue
import com.example.ui.theme.santoyaColors
import com.example.util.CategoryIconResolver
import com.example.util.PengeluaranIconType
import com.example.util.PerawatanIconType
import com.example.util.ProdukIconType

/**
 * Mengembalikan warna aksen semantik dari token tema Santoya untuk kategori pengeluaran tertentu.
 */
@Composable
fun resolvePengeluaranCategoryColor(
    kategori: String,
    namaItem: String = ""
): Color {
    val santoyaColors = MaterialTheme.santoyaColors
    return when (CategoryIconResolver.resolvePengeluaranIconType(kategori, namaItem)) {
        PengeluaranIconType.LISTRIK_AIR -> santoyaColors.info
        PengeluaranIconType.FILTER_MEMBRAN -> MaterialTheme.colorScheme.primary
        PengeluaranIconType.TUTUP_TISU_GALON -> santoyaColors.success
        PengeluaranIconType.BENSIN_TRANSPORT -> santoyaColors.warning
        PengeluaranIconType.PERAWATAN -> MaterialTheme.colorScheme.tertiary
        PengeluaranIconType.OPERASIONAL -> MaterialTheme.colorScheme.secondary
        PengeluaranIconType.LAINNYA -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 * Menampilkan ikon line-art kustom Santoya RO sesuai kategori pengeluaran.
 */
@Composable
fun PengeluaranCategoryIcon(
    kategori: String,
    tint: Color,
    modifier: Modifier = Modifier,
    namaItem: String = "",
    accentColor: Color? = null
) {
    when (CategoryIconResolver.resolvePengeluaranIconType(kategori, namaItem)) {
        PengeluaranIconType.LISTRIK_AIR -> HydroElectricityWaterIcon(
            tint = tint,
            modifier = modifier,
            waterAccent = accentColor
        )
        PengeluaranIconType.FILTER_MEMBRAN -> HydroFilterCartridgeIcon(
            tint = tint,
            modifier = modifier,
            waterAccent = accentColor
        )
        PengeluaranIconType.TUTUP_TISU_GALON -> HydroGalonCapSealIcon(
            tint = tint,
            modifier = modifier,
            leafAccent = accentColor
        )
        PengeluaranIconType.BENSIN_TRANSPORT -> HydroFuelPumpIcon(
            tint = tint,
            modifier = modifier,
            dropletAccent = accentColor
        )
        PengeluaranIconType.PERAWATAN -> HydroMaintenanceWrenchIcon(
            tint = tint,
            modifier = modifier,
            waterAccent = accentColor
        )
        PengeluaranIconType.OPERASIONAL -> HydroOperationalBoardIcon(
            tint = tint,
            modifier = modifier,
            accentColor = accentColor
        )
        PengeluaranIconType.LAINNYA -> HydroExpenseFlowIcon(
            tint = tint,
            modifier = modifier,
            leafAccent = accentColor
        )
    }
}

/**
 * Menampilkan ikon line-art kustom Santoya RO sesuai komponen jadwal/riwayat perawatan mesin RO.
 */
@Composable
fun PerawatanComponentIcon(
    syncId: String,
    nama: String,
    tint: Color,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    when (CategoryIconResolver.resolvePerawatanIconType(syncId = syncId, nama = nama)) {
        PerawatanIconType.CEK_TDS -> HydroTdsMeterIcon(
            tint = tint,
            modifier = modifier,
            waterAccent = accentColor
        )
        PerawatanIconType.FILTER_RO -> HydroFilterCartridgeIcon(
            tint = tint,
            modifier = modifier,
            waterAccent = accentColor
        )
        PerawatanIconType.CUCI_TANGKI -> HydroWaterTankWaveIcon(
            tint = tint,
            modifier = modifier,
            waterAccent = accentColor
        )
        PerawatanIconType.MEMBRAN_RO -> HydroMembraneLayerIcon(
            tint = tint,
            modifier = modifier,
            waterAccent = accentColor
        )
        PerawatanIconType.LAMPU_UV -> HydroUvLampIcon(
            tint = tint,
            modifier = modifier,
            rayAccent = accentColor
        )
        PerawatanIconType.SERVICE_POMPA -> HydroWaterPumpIcon(
            tint = tint,
            modifier = modifier,
            waterAccent = accentColor
        )
        PerawatanIconType.PERAWATAN_UMUM -> HydroMaintenanceWrenchIcon(
            tint = tint,
            modifier = modifier,
            waterAccent = accentColor
        )
    }
}

/**
 * Menampilkan ikon line-art kustom Santoya RO sesuai jenis produk depot air minum.
 */
@Composable
fun ProdukCategoryIcon(
    namaProduk: String,
    tint: Color,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    when (CategoryIconResolver.resolveProdukIconType(namaProduk)) {
        ProdukIconType.GALON_ISI_ULANG -> HydroGalonIcon(
            tint = tint,
            modifier = modifier,
            leafAccent = accentColor
        )
        ProdukIconType.GALON_BARU_TUKAR -> HydroGalonNewSparkIcon(
            tint = tint,
            modifier = modifier,
            leafAccent = accentColor
        )
        ProdukIconType.TUTUP_TISU_SEGEL -> HydroGalonCapSealIcon(
            tint = tint,
            modifier = modifier,
            leafAccent = accentColor
        )
        ProdukIconType.BOTOL_GELAS_KEMASAN -> HydroBottlePackIcon(
            tint = tint,
            modifier = modifier,
            waterAccent = accentColor
        )
    }
}

@Composable
private fun resolveSecondaryColor(
    tint: Color,
    explicitAccent: Color?,
    defaultAccent: Color
): Color {
    if (explicitAccent != null) return explicitAccent
    val santoyaColors = MaterialTheme.santoyaColors
    return if (tint == santoyaColors.onHero || tint.luminance() > 0.85f) {
        santoyaColors.heroLeafTint
    } else {
        defaultAccent
    }
}

/**
 * Kategori Listrik & Air — Simbol petir listrik berpadu tetes air RO.
 */
@Composable
fun HydroElectricityWaterIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    waterAccent: Color? = null
) {
    val dropColor = resolveSecondaryColor(tint, waterAccent, SantoyaWaterBlue)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Bolt di sisi kiri-tengah
        val boltPath = Path().apply {
            moveTo(w * 0.48f, h * 0.12f)
            lineTo(w * 0.22f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.50f)
            lineTo(w * 0.32f, h * 0.88f)
            lineTo(w * 0.60f, h * 0.44f)
            lineTo(w * 0.41f, h * 0.44f)
            close()
        }
        drawPath(
            path = boltPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Tetes air bersih di kanan bawah
        val dropPath = Path().apply {
            moveTo(w * 0.74f, h * 0.44f)
            cubicTo(w * 0.74f, h * 0.44f, w * 0.89f, h * 0.63f, w * 0.89f, h * 0.74f)
            cubicTo(w * 0.89f, h * 0.83f, w * 0.82f, h * 0.89f, w * 0.74f, h * 0.89f)
            cubicTo(w * 0.66f, h * 0.89f, w * 0.59f, h * 0.83f, w * 0.59f, h * 0.74f)
            cubicTo(w * 0.59f, h * 0.63f, w * 0.74f, h * 0.44f, w * 0.74f, h * 0.44f)
            close()
        }
        drawPath(
            path = dropPath,
            color = dropColor,
            style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Kategori Filter & Membran / Filter RO — Tabung kartrid filter dengan sekat gelombang air.
 */
@Composable
fun HydroFilterCartridgeIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    waterAccent: Color? = null
) {
    val accent = resolveSecondaryColor(tint, waterAccent, SantoyaWaterBlue)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Tutup kepala housing filter
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.24f, h * 0.12f),
            size = Size(w * 0.52f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.07f, w * 0.07f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Tabung silinder kartrid bawah
        val bodyPath = Path().apply {
            moveTo(w * 0.30f, h * 0.28f)
            lineTo(w * 0.30f, h * 0.78f)
            cubicTo(w * 0.30f, h * 0.86f, w * 0.36f, h * 0.90f, w * 0.50f, h * 0.90f)
            cubicTo(w * 0.64f, h * 0.90f, w * 0.70f, h * 0.86f, w * 0.70f, h * 0.78f)
            lineTo(w * 0.70f, h * 0.28f)
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Pori/lapisan filtrasi di dalam tabung
        drawLine(
            color = accent,
            start = Offset(w * 0.39f, h * 0.46f),
            end = Offset(w * 0.61f, h * 0.46f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(w * 0.39f, h * 0.60f),
            end = Offset(w * 0.61f, h * 0.60f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(w * 0.39f, h * 0.74f),
            end = Offset(w * 0.61f, h * 0.74f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Kategori Tutup & Tisu Galon — Tutup ulir galon dengan cincin segel serta lembar tisu higienis.
 */
@Composable
fun HydroGalonCapSealIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    leafAccent: Color? = null
) {
    val accent = resolveSecondaryColor(tint, leafAccent, SantoyaLeafGreen)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Badan tutup galon (bentuk kubah pendek berundak)
        val capPath = Path().apply {
            moveTo(w * 0.20f, h * 0.56f)
            lineTo(w * 0.20f, h * 0.30f)
            cubicTo(w * 0.20f, h * 0.20f, w * 0.28f, h * 0.16f, w * 0.50f, h * 0.16f)
            cubicTo(w * 0.72f, h * 0.16f, w * 0.80f, h * 0.20f, w * 0.80f, h * 0.30f)
            lineTo(w * 0.80f, h * 0.56f)
            close()
        }
        drawPath(
            path = capPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Garis rib vertikal tutup galon
        drawLine(
            color = tint,
            start = Offset(w * 0.38f, h * 0.28f),
            end = Offset(w * 0.38f, h * 0.45f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.26f),
            end = Offset(w * 0.50f, h * 0.45f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.62f, h * 0.28f),
            end = Offset(w * 0.62f, h * 0.45f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )

        // Cincin segel bawah & lembar tisu higienis bergelombang
        drawRoundRect(
            color = accent,
            topLeft = Offset(w * 0.15f, h * 0.66f),
            size = Size(w * 0.70f, h * 0.18f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = stroke * 0.92f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Kategori Bensin & Transport — Dispenser pompa SPBU minimalis dengan selang nozzle.
 */
@Composable
fun HydroFuelPumpIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    dropletAccent: Color? = null
) {
    val accent = resolveSecondaryColor(tint, dropletAccent, SantoyaLeafGreen)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Badan dispenser pompa
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.14f),
            size = Size(w * 0.46f, h * 0.74f),
            cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Layar meter liter pompa
        drawRoundRect(
            color = accent,
            topLeft = Offset(w * 0.27f, h * 0.24f),
            size = Size(w * 0.28f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
            style = Stroke(width = stroke * 0.88f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Alas dasar pompa
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, h * 0.88f),
            end = Offset(w * 0.70f, h * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Selang & nozzle SPBU di sisi kanan
        val hosePath = Path().apply {
            moveTo(w * 0.64f, h * 0.44f)
            cubicTo(w * 0.78f, h * 0.44f, w * 0.84f, h * 0.56f, w * 0.84f, h * 0.70f)
            cubicTo(w * 0.84f, h * 0.80f, w * 0.75f, h * 0.82f, w * 0.75f, h * 0.70f)
            lineTo(w * 0.75f, h * 0.32f)
            lineTo(w * 0.84f, h * 0.24f)
        }
        drawPath(
            path = hosePath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Kategori Perawatan / Perawatan Umum — Kunci pas servis organik dengan tetes air RO.
 */
@Composable
fun HydroMaintenanceWrenchIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    waterAccent: Color? = null
) {
    val accent = resolveSecondaryColor(tint, waterAccent, SantoyaWaterBlue)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Kunci pas diagonal
        val wrenchPath = Path().apply {
            moveTo(w * 0.24f, h * 0.82f)
            lineTo(w * 0.54f, h * 0.52f)
            cubicTo(w * 0.50f, h * 0.36f, w * 0.56f, h * 0.20f, w * 0.70f, h * 0.16f)
            lineTo(w * 0.72f, h * 0.28f)
            lineTo(w * 0.82f, h * 0.28f)
            lineTo(w * 0.85f, h * 0.38f)
            cubicTo(w * 0.77f, h * 0.52f, w * 0.64f, h * 0.56f, w * 0.58f, h * 0.55f)
            lineTo(w * 0.29f, h * 0.85f)
            cubicTo(w * 0.26f, h * 0.88f, w * 0.20f, h * 0.85f, w * 0.24f, h * 0.82f)
            close()
        }
        drawPath(
            path = wrenchPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Tetes air kecil sebagai penanda perawatan mesin air
        val dropPath = Path().apply {
            moveTo(w * 0.25f, h * 0.20f)
            cubicTo(w * 0.25f, h * 0.20f, w * 0.35f, h * 0.34f, w * 0.35f, h * 0.42f)
            cubicTo(w * 0.35f, h * 0.48f, w * 0.30f, h * 0.52f, w * 0.25f, h * 0.52f)
            cubicTo(w * 0.20f, h * 0.52f, w * 0.15f, h * 0.48f, w * 0.15f, h * 0.42f)
            cubicTo(w * 0.15f, h * 0.34f, w * 0.25f, h * 0.20f, w * 0.25f, h * 0.20f)
            close()
        }
        drawPath(
            path = dropPath,
            color = accent,
            style = Stroke(width = stroke * 0.88f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Kategori Operasional — Papan catatan kas/operasional harian depot dengan daftar centang & aksen.
 */
@Composable
fun HydroOperationalBoardIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    val accent = resolveSecondaryColor(tint, accentColor, SantoyaLeafGreen)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Klip atas papan
        drawRoundRect(
            color = accent,
            topLeft = Offset(w * 0.36f, h * 0.10f),
            size = Size(w * 0.28f, h * 0.14f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
            style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Bingkai papan operasional
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.20f, h * 0.20f),
            size = Size(w * 0.60f, h * 0.70f),
            cornerRadius = CornerRadius(w * 0.11f, w * 0.11f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Baris catatan operasional
        drawLine(
            color = tint,
            start = Offset(w * 0.33f, h * 0.44f),
            end = Offset(w * 0.67f, h * 0.44f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.33f, h * 0.59f),
            end = Offset(w * 0.67f, h * 0.59f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(w * 0.33f, h * 0.74f),
            end = Offset(w * 0.55f, h * 0.74f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Perawatan Cek TDS — Alat ukur TDS digital portabel dengan layar & ujung uji air.
 */
@Composable
fun HydroTdsMeterIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    waterAccent: Color? = null
) {
    val accent = resolveSecondaryColor(tint, waterAccent, SantoyaWaterBlue)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Bodi TDS meter
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.32f, h * 0.10f),
            size = Size(w * 0.36f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Layar baca ppm
        drawRoundRect(
            color = accent,
            topLeft = Offset(w * 0.39f, h * 0.21f),
            size = Size(w * 0.22f, h * 0.17f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
            style = Stroke(width = stroke * 0.85f)
        )

        // Dua pin elektroda TDS di bawah
        drawLine(
            color = tint,
            start = Offset(w * 0.43f, h * 0.66f),
            end = Offset(w * 0.43f, h * 0.79f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.57f, h * 0.66f),
            end = Offset(w * 0.57f, h * 0.79f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )

        // Gelombang permukaan air yang diukur
        val waterWave = Path().apply {
            moveTo(w * 0.18f, h * 0.87f)
            cubicTo(w * 0.34f, h * 0.80f, w * 0.50f, h * 0.94f, w * 0.68f, h * 0.86f)
            quadraticTo(w * 0.76f, h * 0.82f, w * 0.82f, h * 0.87f)
        }
        drawPath(
            path = waterWave,
            color = accent,
            style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Perawatan Cuci Tangki — Tandon air bulat dengan penutup atas & gelombang pembersihan di dalam.
 */
@Composable
fun HydroWaterTankWaveIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    waterAccent: Color? = null
) {
    val accent = resolveSecondaryColor(tint, waterAccent, SantoyaWaterBlue)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Tutup lubang atas tandon
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.36f, h * 0.10f),
            size = Size(w * 0.28f, h * 0.11f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Badan tangki air depot
        val tankPath = Path().apply {
            moveTo(w * 0.28f, h * 0.21f)
            lineTo(w * 0.72f, h * 0.21f)
            quadraticTo(w * 0.82f, h * 0.26f, w * 0.82f, h * 0.38f)
            lineTo(w * 0.82f, h * 0.78f)
            cubicTo(w * 0.82f, h * 0.87f, w * 0.76f, h * 0.90f, w * 0.68f, h * 0.90f)
            lineTo(w * 0.32f, h * 0.90f)
            cubicTo(w * 0.24f, h * 0.90f, w * 0.18f, h * 0.87f, w * 0.18f, h * 0.78f)
            lineTo(w * 0.18f, h * 0.38f)
            quadraticTo(w * 0.18f, h * 0.26f, w * 0.28f, h * 0.21f)
            close()
        }
        drawPath(
            path = tankPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Gelombang air bersih di dalam tangki
        val wave = Path().apply {
            moveTo(w * 0.26f, h * 0.56f)
            cubicTo(w * 0.38f, h * 0.47f, w * 0.50f, h * 0.65f, w * 0.64f, h * 0.55f)
            quadraticTo(w * 0.70f, h * 0.50f, w * 0.74f, h * 0.54f)
        }
        drawPath(
            path = wave,
            color = accent,
            style = Stroke(width = stroke * 0.92f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Perawatan Membran RO — Housing membran horizontal/miring dengan lapisan semipermeabel RO.
 */
@Composable
fun HydroMembraneLayerIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    waterAccent: Color? = null
) {
    val accent = resolveSecondaryColor(tint, waterAccent, SantoyaWaterBlue)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Tabung housing membran utama
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.16f, h * 0.28f),
            size = Size(w * 0.68f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Port inlet kiri & outlet permeat kanan
        drawLine(
            color = tint,
            start = Offset(w * 0.08f, h * 0.50f),
            end = Offset(w * 0.16f, h * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(w * 0.84f, h * 0.41f),
            end = Offset(w * 0.92f, h * 0.41f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.84f, h * 0.59f),
            end = Offset(w * 0.92f, h * 0.59f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Tiga garis lapisan gulungan membran semipermeabel
        drawLine(
            color = accent,
            start = Offset(w * 0.34f, h * 0.36f),
            end = Offset(w * 0.34f, h * 0.64f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(w * 0.50f, h * 0.36f),
            end = Offset(w * 0.50f, h * 0.64f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(w * 0.66f, h * 0.36f),
            end = Offset(w * 0.66f, h * 0.64f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Perawatan Lampu UV — Tabung sterilisasi ultraviolet dengan pancaran sinar bersih.
 */
@Composable
fun HydroUvLampIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    rayAccent: Color? = null
) {
    val accent = resolveSecondaryColor(tint, rayAccent, SantoyaWaterBlue)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Tabung lampu UV di tengah
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.39f, h * 0.14f),
            size = Size(w * 0.22f, h * 0.72f),
            cornerRadius = CornerRadius(w * 0.11f, w * 0.11f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Tutup soket atas & bawah
        drawLine(
            color = tint,
            start = Offset(w * 0.42f, h * 0.25f),
            end = Offset(w * 0.58f, h * 0.25f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.42f, h * 0.75f),
            end = Offset(w * 0.58f, h * 0.75f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )

        // Pancaran sinar sterilisasi kiri & kanan
        drawLine(
            color = accent,
            start = Offset(w * 0.16f, h * 0.34f),
            end = Offset(w * 0.28f, h * 0.38f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(w * 0.13f, h * 0.50f),
            end = Offset(w * 0.28f, h * 0.50f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(w * 0.16f, h * 0.66f),
            end = Offset(w * 0.28f, h * 0.62f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = accent,
            start = Offset(w * 0.84f, h * 0.34f),
            end = Offset(w * 0.72f, h * 0.38f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(w * 0.87f, h * 0.50f),
            end = Offset(w * 0.72f, h * 0.50f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(w * 0.84f, h * 0.66f),
            end = Offset(w * 0.72f, h * 0.62f),
            strokeWidth = stroke * 0.88f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Perawatan Service Pompa — Rumah pompa booster air dengan pipa outlet atas & rotor impeller.
 */
@Composable
fun HydroWaterPumpIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    waterAccent: Color? = null
) {
    val accent = resolveSecondaryColor(tint, waterAccent, SantoyaWaterBlue)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Rumah keong pompa bulat
        drawCircle(
            color = tint,
            radius = w * 0.26f,
            center = Offset(w * 0.45f, h * 0.56f),
            style = Stroke(width = stroke)
        )

        // Poros/impeller di tengah pompa
        drawCircle(
            color = accent,
            radius = w * 0.09f,
            center = Offset(w * 0.45f, h * 0.56f),
            style = Stroke(width = stroke * 0.9f)
        )

        // Pipa keluar vertikal ke atas
        drawLine(
            color = tint,
            start = Offset(w * 0.45f, h * 0.30f),
            end = Offset(w * 0.45f, h * 0.14f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.36f, h * 0.14f),
            end = Offset(w * 0.54f, h * 0.14f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Bodi motor pompa di samping kanan
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.71f, h * 0.41f),
            size = Size(w * 0.17f, h * 0.30f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
            style = Stroke(width = stroke * 0.92f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Kaki penyangga pompa
        drawLine(
            color = tint,
            start = Offset(w * 0.26f, h * 0.86f),
            end = Offset(w * 0.74f, h * 0.86f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Produk Galon Baru / Tukar Tabung — Siluet galon 19L dengan tanda plus di bahu kanan.
 */
@Composable
fun HydroGalonNewSparkIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    leafAccent: Color? = null
) {
    val plusColor = resolveSecondaryColor(tint, leafAccent, SantoyaLeafGreen)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Tutup & leher galon
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.34f, h * 0.10f),
            size = Size(w * 0.24f, h * 0.11f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Bodi galon
        val bodyPath = Path().apply {
            moveTo(w * 0.37f, h * 0.21f)
            lineTo(w * 0.37f, h * 0.26f)
            cubicTo(w * 0.20f, h * 0.30f, w * 0.16f, h * 0.39f, w * 0.16f, h * 0.49f)
            lineTo(w * 0.16f, h * 0.78f)
            cubicTo(w * 0.16f, h * 0.88f, w * 0.24f, h * 0.91f, w * 0.34f, h * 0.91f)
            lineTo(w * 0.58f, h * 0.91f)
            cubicTo(w * 0.68f, h * 0.91f, w * 0.76f, h * 0.88f, w * 0.76f, h * 0.78f)
            lineTo(w * 0.76f, h * 0.52f)
            cubicTo(w * 0.76f, h * 0.41f, w * 0.71f, h * 0.31f, w * 0.55f, h * 0.26f)
            lineTo(w * 0.55f, h * 0.21f)
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Gelombang air
        val wavePath = Path().apply {
            moveTo(w * 0.24f, h * 0.64f)
            cubicTo(w * 0.35f, h * 0.56f, w * 0.46f, h * 0.72f, w * 0.57f, h * 0.63f)
            quadraticTo(w * 0.64f, h * 0.58f, w * 0.68f, h * 0.62f)
        }
        drawPath(
            path = wavePath,
            color = tint,
            style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round)
        )

        // Tanda plus (+) galon baru di kanan atas
        drawLine(
            color = plusColor,
            start = Offset(w * 0.80f, h * 0.16f),
            end = Offset(w * 0.80f, h * 0.34f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = plusColor,
            start = Offset(w * 0.71f, h * 0.25f),
            end = Offset(w * 0.89f, h * 0.25f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Produk Kemasan Botol / Gelas — Botol minum ramping dengan tutup ulir & pita gelombang air.
 */
@Composable
fun HydroBottlePackIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    waterAccent: Color? = null
) {
    val accent = resolveSecondaryColor(tint, waterAccent, SantoyaWaterBlue)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx().coerceAtMost(size.minDimension * 0.095f)

        // Tutup botol atas
        drawRoundRect(
            color = accent,
            topLeft = Offset(w * 0.40f, h * 0.10f),
            size = Size(w * 0.20f, h * 0.11f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
            style = Stroke(width = stroke * 0.92f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Siluet botol air minum
        val bottlePath = Path().apply {
            moveTo(w * 0.42f, h * 0.21f)
            lineTo(w * 0.42f, h * 0.28f)
            cubicTo(w * 0.33f, h * 0.33f, w * 0.31f, h * 0.40f, w * 0.31f, h * 0.48f)
            lineTo(w * 0.31f, h * 0.82f)
            cubicTo(w * 0.31f, h * 0.88f, w * 0.35f, h * 0.91f, w * 0.42f, h * 0.91f)
            lineTo(w * 0.58f, h * 0.91f)
            cubicTo(w * 0.65f, h * 0.91f, w * 0.69f, h * 0.88f, w * 0.69f, h * 0.82f)
            lineTo(w * 0.69f, h * 0.48f)
            cubicTo(w * 0.69f, h * 0.40f, w * 0.67f, h * 0.33f, w * 0.58f, h * 0.28f)
            lineTo(w * 0.58f, h * 0.21f)
        }
        drawPath(
            path = bottlePath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Label gelombang air di tengah botol
        val labelWave = Path().apply {
            moveTo(w * 0.36f, h * 0.62f)
            cubicTo(w * 0.44f, h * 0.56f, w * 0.56f, h * 0.68f, w * 0.64f, h * 0.60f)
        }
        drawPath(
            path = labelWave,
            color = accent,
            style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round)
        )
    }
}
