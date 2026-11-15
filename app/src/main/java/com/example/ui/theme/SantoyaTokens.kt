package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Jarak (spacing) standar Santoya RO.
 *
 * Gunakan nilai di sini agar margin dan jarak antar elemen konsisten di semua layar.
 * Skala mengikuti kelipatan 4dp supaya rapi di berbagai ukuran layar Android.
 */
object SantoyaSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** Margin kiri-kanan standar untuk isi halaman. */
    val pageHorizontal = 16.dp

    /** Jarak aman di bawah daftar agar tidak tertutup tombol melayang / navigasi. */
    val listBottom = 96.dp
}

/**
 * Sudut membulat standar. Hindari angka acak seperti 9dp, 11dp, 13dp, atau 15dp.
 */
object SantoyaRadius {
    val xs = 6.dp
    val sm = 10.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val pill = 50.dp
}

/**
 * Ukuran yang berkaitan dengan kenyamanan menyentuh layar.
 */
object SantoyaSize {
    /** Ukuran minimum area sentuh yang disarankan (aksesibilitas). */
    val touchTarget = 48.dp

    /** Lingkaran visual di dalam area sentuh 48dp. */
    val iconButtonVisual = 40.dp

    /** Lingkaran tombol aksi standar di TopAppBar (di dalam area sentuh 48dp). */
    val topBarCircle = 38.dp

    /** Ukuran ikon di dalam tombol. */
    val iconGlyph = 20.dp
}

/**
 * Bentuk bawaan Material 3 untuk Santoya RO.
 * Dengan ini komponen yang tidak menyebut bentuk sendiri otomatis senada.
 */
val SantoyaShapes = Shapes(
    extraSmall = RoundedCornerShape(SantoyaRadius.xs),
    small = RoundedCornerShape(SantoyaRadius.sm),
    medium = RoundedCornerShape(SantoyaRadius.md),
    large = RoundedCornerShape(SantoyaRadius.lg),
    extraLarge = RoundedCornerShape(SantoyaRadius.xxl)
)
