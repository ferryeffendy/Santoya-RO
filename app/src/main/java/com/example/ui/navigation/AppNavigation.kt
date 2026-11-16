package com.example.ui.navigation

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.KurirScreen
import com.example.ui.screens.LaporanScreen
import com.example.ui.screens.PelangganScreen
import com.example.ui.screens.PengeluaranScreen
import com.example.ui.screens.PerawatanScreen
import com.example.ui.screens.ProdukScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransaksiScreen
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Mengelola graf navigasi Jetpack Navigation Compose untuk seluruh layar utama Santoya RO,
 * termasuk sinkronisasi state layar ViewModel, pembatasan akses karyawan, dan tombol kembali sistem.
 */
@Composable
fun AppNavigation(
    viewModel: SantoyaViewModel,
    auth: FirebaseAuth,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isKaryawan by viewModel.isKaryawan.collectAsStateWithLifecycle()
    val activeKaryawan by viewModel.activeKaryawan.collectAsStateWithLifecycle()

    val restrictedForKaryawan = isKaryawan && (
        currentScreen == SantoyaScreen.LAPORAN ||
            currentScreen == SantoyaScreen.PENGELUARAN ||
            currentScreen == SantoyaScreen.SETTINGS ||
            currentScreen == SantoyaScreen.KURIR ||
            currentScreen == SantoyaScreen.PRODUK ||
            currentScreen == SantoyaScreen.PERAWATAN
        )

    LaunchedEffect(currentScreen, isKaryawan) {
        if (restrictedForKaryawan) {
            viewModel.navigateTo(SantoyaScreen.DASHBOARD)
            return@LaunchedEffect
        }
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != null && currentRoute != currentScreen.name) {
            if (currentScreen == SantoyaScreen.DASHBOARD) {
                navController.popBackStack(SantoyaScreen.DASHBOARD.name, inclusive = false)
            } else {
                navController.navigate(currentScreen.name) {
                    popUpTo(SantoyaScreen.DASHBOARD.name) {
                        saveState = false
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    val performSignOut: () -> Unit = {
        coroutineScope.launch {
            viewModel.clearLocalDataForLogout()
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("AppNavigation", "Clear credential failed", e)
            }
            auth.signOut()
        }
    }

    NavHost(
        navController = navController,
        startDestination = SantoyaScreen.DASHBOARD.name,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(durationMillis = 220)) },
        exitTransition = { fadeOut(animationSpec = tween(durationMillis = 220)) }
    ) {
        composable(SantoyaScreen.DASHBOARD.name) {
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
                onSignOut = performSignOut,
                isKaryawan = isKaryawan,
                activeKaryawan = activeKaryawan
            )
        }
        composable(SantoyaScreen.TRANSAKSI.name) {
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
        composable(SantoyaScreen.PELANGGAN.name) {
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
        composable(SantoyaScreen.KURIR.name) {
            val kurirList by viewModel.allKurir.collectAsStateWithLifecycle()
            val transaksiList by viewModel.allTransaksi.collectAsStateWithLifecycle()
            KurirScreen(
                viewModel = viewModel,
                kurirList = kurirList,
                transaksiList = transaksiList
            )
        }
        composable(SantoyaScreen.PRODUK.name) {
            val produkList by viewModel.allProduk.collectAsStateWithLifecycle()
            ProdukScreen(
                viewModel = viewModel,
                produkList = produkList
            )
        }
        composable(SantoyaScreen.PENGELUARAN.name) {
            val pengeluaranList by viewModel.allPengeluaran.collectAsStateWithLifecycle()
            PengeluaranScreen(
                viewModel = viewModel,
                pengeluaranList = pengeluaranList
            )
        }
        composable(SantoyaScreen.LAPORAN.name) {
            val transaksiList by viewModel.allTransaksi.collectAsStateWithLifecycle()
            val pengeluaranList by viewModel.allPengeluaran.collectAsStateWithLifecycle()
            LaporanScreen(
                viewModel = viewModel,
                transaksiList = transaksiList,
                pengeluaranList = pengeluaranList
            )
        }
        composable(SantoyaScreen.SETTINGS.name) {
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
                onSignOut = performSignOut
            )
        }
        composable(SantoyaScreen.PERAWATAN.name) {
            if (!isKaryawan) {
                PerawatanScreen(viewModel = viewModel)
            }
        }
    }

    BackHandler(enabled = currentScreen != SantoyaScreen.DASHBOARD) {
        viewModel.navigateTo(SantoyaScreen.DASHBOARD)
    }
}
