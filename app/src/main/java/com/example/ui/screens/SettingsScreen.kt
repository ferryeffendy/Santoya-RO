package com.example.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.companion.kzpmrw.BuildConfig
import com.aistudio.companion.kzpmrw.R
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.Produk
import com.example.data.entity.TransaksiDetail
import com.example.ui.components.HydroCourierWaveIcon
import com.example.ui.components.HydroGalonIcon
import com.example.ui.components.ImportJsonDialog
import com.example.ui.components.RealtimeSyncStatusPill
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.SantoyaSpacing
import com.example.ui.theme.pressClickEffect
import com.example.ui.theme.santoyaColors
import com.example.util.ExportHelper
import com.example.util.FormatUtils
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
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showRestoreCloudDialog by remember { mutableStateOf(false) }
    var showCloudDiagnosticDialog by remember { mutableStateOf(false) }

    val isSyncingCloud by viewModel.isSyncingCloud.collectAsStateWithLifecycle()
    val lastSyncSummary by viewModel.lastSyncSummary.collectAsStateWithLifecycle()
    val syncState by viewModel.realtimeSyncState.collectAsStateWithLifecycle()
    val perawatanEvaluations by viewModel.allPerawatanEvaluations.collectAsStateWithLifecycle()
    val perawatanReminders by viewModel.perawatanReminders.collectAsStateWithLifecycle()
    val currentUser = remember { Firebase.auth.currentUser }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SantoyaSpacing.pageHorizontal),
        contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        // 1. Profil Pemilik
        item {
            SettingsSectionTitle("Profil Pemilik")
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
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Pemilik Santoya RO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUser?.email?.takeIf { it.isNotBlank() } ?: "Akun Utama Depot",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { showSignOutDialog = true },
                            modifier = Modifier
                                .size(SantoyaSize.touchTarget)
                                .testTag("btn_sign_out")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = "Keluar Akun",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = MaterialTheme.santoyaColors.success.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(color = MaterialTheme.santoyaColors.success, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Akses Pemilik Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.santoyaColors.success
                            )
                        }
                    }
                }
            }
        }

        // 2. Kelola Depot (Master Data dinaikkan ke posisi ke-2)
        item {
            SettingsSectionTitle("Kelola Depot")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(SantoyaRadius.lg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsMenuRow(
                        title = "Produk & Harga",
                        subtitle = "${produkList.size} produk • Atur jenis & harga jual galon",
                        iconColor = MaterialTheme.santoyaColors.success,
                        onClick = { viewModel.navigateTo(SantoyaScreen.PRODUK) },
                        modifier = Modifier.testTag("menu_master_produk"),
                        iconContent = {
                            HydroGalonIcon(
                                tint = MaterialTheme.santoyaColors.success,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsMenuRow(
                        title = "Karyawan & Komisi",
                        subtitle = "${kurirList.size} karyawan • Atur akun kurir & komisi antar",
                        iconColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { viewModel.navigateTo(SantoyaScreen.KURIR) },
                        modifier = Modifier.testTag("menu_kelola_kurir"),
                        iconContent = {
                            HydroCourierWaveIcon(
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    val perawatanSubtitle = if (perawatanReminders.isNotEmpty()) {
                        "${perawatanEvaluations.size} jadwal • ${perawatanReminders.size} perlu perhatian"
                    } else {
                        "${perawatanEvaluations.size} jadwal • Atur servis & ganti filter RO"
                    }
                    SettingsMenuRow(
                        title = "Jadwal Perawatan RO",
                        subtitle = perawatanSubtitle,
                        icon = Icons.Outlined.Build,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.navigateTo(SantoyaScreen.PERAWATAN) },
                        modifier = Modifier.testTag("menu_jadwal_perawatan")
                    )
                }
            }
        }

        // 3. Sinkronisasi Antar Perangkat (Gabungan Cloud + Offline-First)
        item {
            SettingsSectionTitle("Sinkronisasi Antar Perangkat")
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
                                text = "Sinkronisasi Otomatis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Data otomatis tersinkron di semua HP",
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
                                    tint = MaterialTheme.santoyaColors.success,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tetap jalan tanpa sinyal • Tersimpan di HP",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (syncState.pendingCount > 0) {
                                Text(
                                    text = "${syncState.pendingCount} transaksi menunggu koneksi untuk dikirim",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.santoyaColors.warning
                                )
                            }

                            val lastSyncedAt = syncState.lastSyncedAt
                            if (lastSyncedAt != null) {
                                Text(
                                    text = "Terakhir diperbarui: ${FormatUtils.formatDate(lastSyncedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Semua perubahan langsung tersimpan ke Cloud.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            lastSyncSummary?.let { summary ->
                                Text(
                                    text = "Rekap Cloud: ${summary.pelangganCount} pelanggan • ${summary.transaksiCount} transaksi",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!syncState.errorMessage.isNullOrBlank()) {
                                Text(
                                    text = syncState.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Penggunaan Firestore Sesi Ini",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(
                                    onClick = { showCloudDiagnosticDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("btn_open_cloud_diagnostic")
                                ) {
                                    Text(
                                        text = "Detail Ukur",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(SantoyaRadius.sm),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "Reads Sesi Ini",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${syncState.sessionReads} Dokumen",
                                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(SantoyaRadius.sm),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "Writes Sesi Ini",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "${syncState.sessionWrites} Dokumen",
                                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Kuota gratis Spark: 50.000 reads & 20.000 writes/hari. Cache lokal SQLite menyerap pembacaan layar kasir (0 reads cloud).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                            modifier = Modifier
                                .weight(1f)
                                .height(SantoyaSize.touchTarget)
                                .testTag("btn_sync_to_cloud")
                        ) {
                            if (isSyncingCloud) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menyimpan...", style = MaterialTheme.typography.titleSmall)
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cadangkan",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

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
                            Icon(
                                imageVector = Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ambil Data",
                                style = MaterialTheme.typography.titleSmall,
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
            SettingsSectionTitle("Laporan & File Cadangan")
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
                                    pengeluaranList = viewModel.allPengeluaran.value
                                )
                                ExportHelper.shareExportFile(
                                    context = context,
                                    filename = "santoya_backup_nested_${System.currentTimeMillis()}.json",
                                    content = json,
                                    mimeType = "application/json"
                                )
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
                                "File Cadangan",
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
                                    pengeluaranList = viewModel.allPengeluaran.value
                                )
                                ExportHelper.shareExportFile(
                                    context = context,
                                    filename = "santoya_laporan_${System.currentTimeMillis()}.csv",
                                    content = csv,
                                    mimeType = "text/csv"
                                )
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
                                "Laporan Excel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

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
                            "Pulihkan dari File Cadangan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (ExportHelper.hasAutoBackup(context)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val autoJson = ExportHelper.readAutoBackupJson(context)
                                if (!autoJson.isNullOrBlank()) {
                                    viewModel.importJsonData(autoJson)
                                }
                            },
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
                                "Pulihkan Cadangan Otomatis HP",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Simpan Laporan Excel (CSV) untuk pembukuan atau File Cadangan untuk arsip HP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 5. Tentang Aplikasi
        item {
            SettingsSectionTitle("Tentang Aplikasi")
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
                                text = "Santoya RO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Aplikasi Kasir Depot Air Minum",
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
                            label = "Versi Aplikasi",
                            value = "v${BuildConfig.VERSION_NAME}"
                        )
                        SystemInfoRow(
                            label = "Penyimpanan Data",
                            value = "HP & Cloud Otomatis"
                        )
                        SystemInfoRow(
                            label = "Keamanan Data",
                            value = "Cloud Terenkripsi"
                        )
                        SystemInfoRow(
                            label = "Status Operasional",
                            value = "Terhubung",
                            valueColor = MaterialTheme.santoyaColors.success,
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
                Text("Keluar dari Akun Pemilik?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Anda harus login lagi pakai akun Google untuk membuka data depot.")
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
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false },
                    modifier = Modifier.testTag("btn_cancel_sign_out")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showRestoreCloudDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreCloudDialog = false },
            title = {
                Text("Ambil Data dari Cloud?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Data pelanggan, produk, kurir, dan transaksi dari Cloud akan digabungkan ke HP ini. Lanjutkan?")
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
                    Text("Ambil Data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreCloudDialog = false },
                    modifier = Modifier.testTag("btn_cancel_restore_cloud")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showCloudDiagnosticDialog) {
        val dbId = context.getString(R.string.firestore_database_id)
        AlertDialog(
            onDismissRequest = { showCloudDiagnosticDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pengukuran Reads Firestore", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Penggunaan Sesi Aplikasi:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text("• Dokumen dibaca (Reads): ${syncState.sessionReads}", style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"))
                            Text("• Dokumen ditulis (Writes): ${syncState.sessionWrites}", style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"))
                            Text("• Kuota Gratis Spark: 50.000 reads & 20.000 writes / hari", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.santoyaColors.success, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Text(
                        text = "Cara Cek Reads Aktual di Firebase Console:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "1. Buka console.firebase.google.com dengan browser.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "2. Pilih project: gen-lang-client-0963336772",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "3. Masuk menu Build > Firestore Database, lalu klik tab Usage (Penggunaan).",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Surface(
                            shape = RoundedCornerShape(SantoyaRadius.xs),
                            color = MaterialTheme.santoyaColors.warning.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.santoyaColors.warning.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "PENTING: Di pojok kiri atas halaman Firestore, ubah dropdown database dari (default) ke database khusus depot:\n$dbId\n(Jika tetap di default, grafik akan selalu 0 karena data tersimpan di database khusus).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Text(
                        text = "Mengapa Sangat Hemat?\nAplikasi Santoya RO menggunakan arsitektur Offline-First (Room SQLite lokal). Saat Anda membuka daftar transaksi, riwayat kasir, daftar pelanggan, kasbon, dan laporan laba bersih, semua dibaca langsung dari memori HP lokal (0 reads). Firestore hanya membaca saat inisialisasi listener atau sinkronisasi perubahan antar-HP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCloudDiagnosticDialog = false },
                    modifier = Modifier.testTag("btn_close_cloud_diagnostic")
                ) {
                    Text("Tutup")
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
