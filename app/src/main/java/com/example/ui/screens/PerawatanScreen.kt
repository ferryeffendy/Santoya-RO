package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.companion.kzpmrw.R
import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import com.example.ui.components.HydroBentoCard
import com.example.ui.components.HydroGradientBadge
import com.example.ui.components.HydroMaintenanceWrenchIcon
import com.example.ui.components.PerawatanComponentIcon
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.components.SantoyaEmptyType
import com.example.ui.components.SantoyaFilterPill
import com.example.ui.components.SantoyaFormBottomSheet
import com.example.ui.components.bringIntoViewOnFocus
import com.example.ui.components.rememberIsImeVisible
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.PerawatanEvaluation
import com.example.util.PerawatanScheduler
import com.example.util.PerawatanStatus
import com.example.viewmodel.SantoyaViewModel
import java.util.Calendar

/**
 * Halaman Jadwal & Riwayat Perawatan Mesin RO (khusus Pemilik Depot).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PerawatanScreen(
    viewModel: SantoyaViewModel,
    modifier: Modifier = Modifier
) {
    val evaluations by viewModel.allPerawatanEvaluations.collectAsStateWithLifecycle()
    val logs by viewModel.allPerawatanLogs.collectAsStateWithLifecycle()
    val isImeVisible = rememberIsImeVisible()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddEditSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PerawatanItem?>(null) }
    var itemToComplete by remember { mutableStateOf<PerawatanItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PerawatanItem?>(null) }
    var logToDelete by remember { mutableStateOf<PerawatanLog?>(null) }

    val overdueCount = remember(evaluations) {
        evaluations.count { it.status == PerawatanStatus.TERLAMBAT }
    }
    val todayAndSoonCount = remember(evaluations) {
        evaluations.count {
            it.status == PerawatanStatus.HARI_INI || it.status == PerawatanStatus.MENDEKATI
        }
    }
    val totalBiayaPerawatan = remember(logs) {
        logs.sumOf { it.biaya }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("perawatan_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedTab == 0 && !isImeVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        itemToEdit = null
                        showAddEditSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("tambah_perawatan_fab")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Tambah Jadwal Perawatan"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = stringResource(R.string.perawatan_tab_jadwal, evaluations.size),
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_jadwal_perawatan")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = stringResource(R.string.perawatan_tab_riwayat, logs.size),
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_riwayat_perawatan")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = stringResource(R.string.perawatan_tab_cek_membran),
                            fontWeight = if (selectedTab == 2) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_cek_membran_perawatan")
                )
            }

            if (selectedTab == 2) {
                MembranCalculatorTab(viewModel = viewModel)
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = if (maxWidth >= 600.dp) 2 else 1

                if (selectedTab == 0) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PerawatanSummaryBanner(
                                totalJadwal = evaluations.size,
                                overdueCount = overdueCount,
                                todayAndSoonCount = todayAndSoonCount
                            )
                        }

                        if (evaluations.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SantoyaEmptyState(
                                    emptyType = SantoyaEmptyType.LAPORAN,
                                    title = "Belum Ada Jadwal Perawatan",
                                    subtitle = "Tambahkan jadwal cek TDS, ganti filter, atau servis pompa mesin RO depot Anda.",
                                    primaryActionLabel = "Tambah Jadwal",
                                    primaryActionIcon = Icons.Outlined.Add,
                                    onPrimaryAction = {
                                        itemToEdit = null
                                        showAddEditSheet = true
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        } else {
                            items(evaluations, key = { it.item.syncId }) { eval ->
                                PerawatanScheduleCard(
                                    evaluation = eval,
                                    onComplete = { itemToComplete = eval.item },
                                    onEdit = {
                                        itemToEdit = eval.item
                                        showAddEditSheet = true
                                    },
                                    onDelete = { itemToDelete = eval.item }
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PerawatanHistorySummaryBanner(
                                totalLogs = logs.size,
                                totalBiaya = totalBiayaPerawatan
                            )
                        }

                        if (logs.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SantoyaEmptyState(
                                    emptyType = SantoyaEmptyType.LAPORAN,
                                    title = "Belum Ada Riwayat Perawatan",
                                    subtitle = "Tandai salah satu jadwal perawatan selesai untuk menyimpan riwayat dan mencatat biaya ke Pengeluaran.",
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        } else {
                            items(logs, key = { it.syncId }) { log ->
                                PerawatanHistoryCard(
                                    log = log,
                                    onDelete = { logToDelete = log }
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (showAddEditSheet) {
        PerawatanFormBottomSheet(
            initialItem = itemToEdit,
            onDismiss = { showAddEditSheet = false },
            onSave = { nama, intervalHari, jadwalBerikutnya ->
                val editing = itemToEdit
                if (editing == null) {
                    viewModel.addPerawatan(nama = nama, intervalHari = intervalHari, jadwalPertama = jadwalBerikutnya)
                } else {
                    viewModel.updatePerawatan(
                        item = editing,
                        namaBaru = nama,
                        intervalHariBaru = intervalHari,
                        jadwalBerikutnyaBaru = jadwalBerikutnya
                    )
                }
                showAddEditSheet = false
            }
        )
    }

    itemToComplete?.let { target ->
        CompletePerawatanBottomSheet(
            item = target,
            onDismiss = { itemToComplete = null },
            onConfirm = { tanggalSelesai, biaya, catatan ->
                viewModel.completePerawatan(
                    item = target,
                    tanggalSelesai = tanggalSelesai,
                    biaya = biaya,
                    catatan = catatan
                )
                itemToComplete = null
            }
        )
    }

    itemToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Hapus Jadwal Perawatan?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Hapus jadwal \"${target.nama}\" (interval ${target.intervalHari} hari)? Riwayat perawatan dan pengeluaran yang sudah tercatat tidak ikut terhapus.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePerawatan(target)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(SantoyaRadius.sm)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    logToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = {
                Text(
                    text = "Hapus Riwayat Perawatan?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Hapus catatan perawatan \"${log.namaPerawatan}\" tanggal ${FormatUtils.formatDateUi(log.tanggalSelesai)}?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePerawatanLog(log)
                        logToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(SantoyaRadius.sm)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun PerawatanSummaryBanner(
    totalJadwal: Int,
    overdueCount: Int,
    todayAndSoonCount: Int
) {
    val santoyaColors = MaterialTheme.santoyaColors
    val accentColor = when {
        overdueCount > 0 -> MaterialTheme.colorScheme.error
        todayAndSoonCount > 0 -> santoyaColors.warning
        else -> MaterialTheme.colorScheme.primary
    }

    HydroBentoCard(
        accentColor = accentColor,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.10f,
        showWaterRipple = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                HydroGradientBadge(
                    gradientColors = listOf(accentColor, AccentCyan),
                    size = 40.dp,
                    cornerRadius = SantoyaRadius.md,
                    elevation = 0.dp
                ) {
                    if (overdueCount > 0) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.santoyaColors.onHero,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        HydroMaintenanceWrenchIcon(
                            tint = MaterialTheme.santoyaColors.onHero,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            overdueCount > 0 -> "$overdueCount Perawatan Terlambat"
                            todayAndSoonCount > 0 -> "$todayAndSoonCount Perawatan Mendekati Jadwal"
                            else -> "Semua Mesin RO Terjadwal Aman"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$totalJadwal item aktif • $todayAndSoonCount jatuh tempo / H-3",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PerawatanHistorySummaryBanner(
    totalLogs: Int,
    totalBiaya: Double
) {
    HydroBentoCard(
        accentColor = MaterialTheme.colorScheme.tertiary,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.09f,
        showWaterRipple = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                HydroGradientBadge(
                    gradientColors = listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary),
                    size = 40.dp,
                    cornerRadius = SantoyaRadius.md,
                    elevation = 0.dp
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.santoyaColors.onHero,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Biaya Perawatan RO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SantoyaAdaptiveCurrencyText(
                        amount = totalBiaya,
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        minFontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(SantoyaRadius.sm),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = "$totalLogs Riwayat",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerawatanScheduleCard(
    evaluation: PerawatanEvaluation,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val item = evaluation.item
    val santoyaColors = MaterialTheme.santoyaColors
    val (statusColor, statusContainer, statusOnContainer) = when (evaluation.status) {
        PerawatanStatus.TERLAMBAT -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        PerawatanStatus.HARI_INI -> Triple(
            santoyaColors.warning,
            santoyaColors.warningContainer,
            santoyaColors.onWarningContainer
        )
        PerawatanStatus.MENDEKATI -> Triple(
            santoyaColors.warning,
            santoyaColors.warningContainer.copy(alpha = 0.78f),
            santoyaColors.onWarningContainer
        )
        PerawatanStatus.AMAN -> Triple(
            santoyaColors.success,
            santoyaColors.successContainer,
            santoyaColors.onSuccessContainer
        )
    }

    HydroBentoCard(
        accentColor = statusColor,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = if (evaluation.isReminderDue) 0.08f else 0.04f,
        showWaterRipple = false,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("perawatan_card_${item.syncId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        color = statusColor.copy(alpha = 0.14f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PerawatanComponentIcon(
                                syncId = item.syncId,
                                nama = item.nama,
                                tint = statusColor,
                                accentColor = statusColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.nama,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Interval setiap ${item.intervalHari} hari",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .sizeIn(minWidth = SantoyaSize.touchTarget, minHeight = SantoyaSize.touchTarget)
                            .testTag("btn_edit_perawatan_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Ubah Jadwal ${item.nama}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .sizeIn(minWidth = SantoyaSize.touchTarget, minHeight = SantoyaSize.touchTarget)
                            .testTag("btn_hapus_perawatan_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Hapus Jadwal ${item.nama}",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    color = statusContainer
                ) {
                    Text(
                        text = PerawatanScheduler.formatStatusBadge(evaluation),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusOnContainer,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.sm),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Berikutnya: ${FormatUtils.formatDateUi(item.jadwalBerikutnya)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = if (item.terakhirSelesai != null) {
                    "Terakhir selesai: ${FormatUtils.formatDateUi(item.terakhirSelesai)}"
                } else {
                    "Terakhir selesai: Belum pernah dicatat"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onComplete,
                shape = RoundedCornerShape(SantoyaRadius.md),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (evaluation.isReminderDue) statusColor else MaterialTheme.colorScheme.primary,
                    contentColor = if (evaluation.isReminderDue) santoyaColors.onHero else MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SantoyaSize.touchTarget)
                    .testTag("btn_selesai_perawatan_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tandai Selesai",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerawatanHistoryCard(
    log: PerawatanLog,
    onDelete: () -> Unit
) {
    val hasExpense = log.biaya > 0.0
    val accent = if (hasExpense) MaterialTheme.colorScheme.tertiary else MaterialTheme.santoyaColors.success

    HydroBentoCard(
        accentColor = accent,
        cornerRadius = SantoyaRadius.lg,
        tintAlpha = 0.04f,
        showWaterRipple = false,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("perawatan_log_card_${log.syncId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.sm),
                        color = accent.copy(alpha = 0.14f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PerawatanComponentIcon(
                                syncId = "",
                                nama = log.namaPerawatan,
                                tint = accent,
                                accentColor = accent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = log.namaPerawatan,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Selesai ${FormatUtils.formatDateUi(log.tanggalSelesai)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (hasExpense) FormatUtils.formatRupiah(log.biaya) else "Tanpa Biaya",
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = if (hasExpense) MaterialTheme.colorScheme.error else MaterialTheme.santoyaColors.success
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .sizeIn(minWidth = SantoyaSize.touchTarget, minHeight = SantoyaSize.touchTarget)
                            .testTag("btn_hapus_log_perawatan_${log.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Hapus Riwayat",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (hasExpense) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.xs),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Tercatat di Pengeluaran (Perawatan)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            if (log.catatan.isNotBlank()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = log.catatan,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerawatanFormBottomSheet(
    initialItem: PerawatanItem?,
    onDismiss: () -> Unit,
    onSave: (nama: String, intervalHari: Int, jadwalBerikutnya: Long) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val intervalFocusRequester = remember { FocusRequester() }

    var nama by remember(initialItem) { mutableStateOf(initialItem?.nama ?: "") }
    var intervalStr by remember(initialItem) {
        mutableStateOf((initialItem?.intervalHari ?: 30).toString())
    }
    var customDueMillis by remember(initialItem) {
        mutableLongStateOf(
            initialItem?.jadwalBerikutnya ?: PerawatanScheduler.calculateNextDueDateMillis(
                completionDateMillis = System.currentTimeMillis(),
                intervalHari = 30
            )
        )
    }
    var hasManuallyPickedDate by remember(initialItem) { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetIntervals = remember { listOf(7, 14, 30, 60, 90, 180) }

    val submit = {
        val cleanName = nama.trim()
        val intervalVal = intervalStr.toIntOrNull()
        when {
            cleanName.isBlank() -> errorMessage = "Nama perawatan wajib diisi"
            intervalVal == null || intervalVal < 1 || intervalVal > 3650 -> {
                errorMessage = "Interval harus antara 1 sampai 3650 hari"
            }
            else -> onSave(cleanName, intervalVal, customDueMillis)
        }
    }

    SantoyaFormBottomSheet(
        title = if (initialItem == null) "Tambah Jadwal Perawatan" else "Ubah Jadwal Perawatan",
        subtitle = "Atur komponen mesin RO dan interval pengingat",
        headerAccent = MaterialTheme.colorScheme.primary,
        headerIconContent = {
            PerawatanComponentIcon(
                syncId = initialItem?.syncId.orEmpty(),
                nama = nama,
                tint = MaterialTheme.santoyaColors.onHero,
                modifier = Modifier.size(21.dp)
            )
        },
        confirmLabel = if (initialItem == null) "Tambah" else "Simpan",
        confirmContainerColor = MaterialTheme.colorScheme.primary,
        onDismiss = onDismiss,
        onConfirm = { submit() }
    ) {
        OutlinedTextField(
            value = nama,
            onValueChange = {
                nama = it
                errorMessage = null
            },
            label = { Text("Nama Perawatan *") },
            placeholder = { Text("Contoh: Filter Sedimen, Cek TDS") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { intervalFocusRequester.requestFocus() }
            ),
            singleLine = true,
            shape = RoundedCornerShape(SantoyaRadius.md),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus()
                .testTag("input_nama_perawatan")
        )

        OutlinedTextField(
            value = intervalStr,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(4)
                intervalStr = digits
                errorMessage = null
                val parsed = digits.toIntOrNull()
                if (!hasManuallyPickedDate && parsed != null && parsed >= 1) {
                    val base = initialItem?.terakhirSelesai ?: System.currentTimeMillis()
                    customDueMillis = PerawatanScheduler.calculateNextDueDateMillis(base, parsed)
                }
            },
            label = { Text("Interval (Hari) *") },
            placeholder = { Text("Contoh: 30") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            singleLine = true,
            shape = RoundedCornerShape(SantoyaRadius.md),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(intervalFocusRequester)
                .bringIntoViewOnFocus()
                .testTag("input_interval_perawatan")
        )

        Text(
            text = "Pilihan Cepat Interval",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            presetIntervals.forEach { days ->
                val isSelected = intervalStr.toIntOrNull() == days
                SantoyaFilterPill(
                    label = "$days Hari",
                    selected = isSelected,
                    onClick = {
                        intervalStr = days.toString()
                        errorMessage = null
                        if (!hasManuallyPickedDate) {
                            val base = initialItem?.terakhirSelesai ?: System.currentTimeMillis()
                            customDueMillis = PerawatanScheduler.calculateNextDueDateMillis(base, days)
                        }
                    },
                    leadingIcon = if (isSelected) Icons.Outlined.Check else null
                )
            }
        }

        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = customDueMillis }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val pickedCal = Calendar.getInstance().apply {
                            set(y, m, d, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        customDueMillis = pickedCal.timeInMillis
                        hasManuallyPickedDate = true
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            shape = RoundedCornerShape(SantoyaRadius.md),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SantoyaSize.touchTarget)
                .testTag("btn_pilih_jadwal_berikutnya")
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Jadwal Berikutnya: ${FormatUtils.formatDateUi(customDueMillis)}")
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CompletePerawatanBottomSheet(
    item: PerawatanItem,
    onDismiss: () -> Unit,
    onConfirm: (tanggalSelesai: Long, biaya: Double, catatan: String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val catatanFocusRequester = remember { FocusRequester() }

    var tanggalSelesai by remember(item) { mutableLongStateOf(System.currentTimeMillis()) }
    var biayaStr by remember(item) { mutableStateOf("") }
    var catatan by remember(item) { mutableStateOf("") }

    val predictedNextDue = remember(tanggalSelesai, item.intervalHari) {
        PerawatanScheduler.calculateNextDueDateMillis(tanggalSelesai, item.intervalHari)
    }

    SantoyaFormBottomSheet(
        title = "Selesaikan Perawatan",
        subtitle = "${item.nama} • Interval ${item.intervalHari} hari",
        headerAccent = MaterialTheme.santoyaColors.success,
        headerIconContent = {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.santoyaColors.onHero,
                modifier = Modifier.size(20.dp)
            )
        },
        confirmLabel = "Simpan Selesai",
        confirmContainerColor = MaterialTheme.santoyaColors.success,
        onDismiss = onDismiss,
        onConfirm = {
            val biaya = biayaStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
            onConfirm(tanggalSelesai, biaya, catatan.trim())
        }
    ) {
        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = tanggalSelesai }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val pickedCal = Calendar.getInstance().apply {
                            set(y, m, d, 8, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        tanggalSelesai = pickedCal.timeInMillis
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            shape = RoundedCornerShape(SantoyaRadius.md),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SantoyaSize.touchTarget)
                .testTag("btn_pilih_tanggal_selesai")
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tanggal Selesai: ${FormatUtils.formatDateUi(tanggalSelesai)}")
        }

        OutlinedTextField(
            value = biayaStr,
            onValueChange = { raw ->
                biayaStr = raw.filter { it.isDigit() }
            },
            label = { Text("Biaya Perawatan (Rp)") },
            placeholder = { Text("0 jika tanpa biaya") },
            supportingText = {
                Text("Jika > Rp 0, otomatis masuk Pengeluaran kategori 'Perawatan'.")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { catatanFocusRequester.requestFocus() }
            ),
            singleLine = true,
            shape = RoundedCornerShape(SantoyaRadius.md),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus()
                .testTag("input_biaya_perawatan")
        )

        OutlinedTextField(
            value = catatan,
            onValueChange = { catatan = it.take(500) },
            label = { Text("Catatan (Opsional)") },
            placeholder = { Text("Contoh: TDS turun ke 12 ppm, ganti cartridge baru") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(SantoyaRadius.md),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(catatanFocusRequester)
                .bringIntoViewOnFocus()
                .testTag("input_catatan_perawatan")
        )

        Surface(
            shape = RoundedCornerShape(SantoyaRadius.md),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Jadwal berikutnya: ${FormatUtils.formatDateUi(predictedNextDue)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
