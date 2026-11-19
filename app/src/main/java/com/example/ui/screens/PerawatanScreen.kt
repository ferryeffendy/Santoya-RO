package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import com.example.ui.components.CompletePerawatanBottomSheet
import com.example.ui.components.DeletePerawatanLogDialog
import com.example.ui.components.DeletePerawatanScheduleDialog
import com.example.ui.components.EditPerawatanLogBottomSheet
import com.example.ui.components.PerawatanFormBottomSheet
import com.example.ui.components.PerawatanHistoryCard
import com.example.ui.components.PerawatanHistorySummaryBanner
import com.example.ui.components.PerawatanScheduleCard
import com.example.ui.components.PerawatanSummaryBanner
import com.example.ui.components.ReschedulePerawatanBottomSheet
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.components.SantoyaEmptyType
import com.example.ui.components.SantoyaFilterPill
import com.example.ui.components.SantoyaSearchBar
import com.example.ui.components.rememberIsImeVisible
import com.example.util.PerawatanHistoryPeriodFilter
import com.example.util.PerawatanScheduleFilter
import com.example.util.PerawatanScheduler
import com.example.viewmodel.SantoyaViewModel

private enum class PerawatanTabKind {
    JADWAL,
    RIWAYAT
}

/**
 * Halaman Jadwal & Riwayat Perawatan Mesin RO yang ringkas khusus Pemilik Depot,
 * dilengkapi pencarian, filter status/periode, tunda jadwal, pemulihan templat standar,
 * dan edit riwayat terintegrasi Pengeluaran.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerawatanScreen(
    viewModel: SantoyaViewModel,
    modifier: Modifier = Modifier
) {
    val evaluations by viewModel.allPerawatanEvaluations.collectAsStateWithLifecycle()
    val logs by viewModel.allPerawatanLogs.collectAsStateWithLifecycle()
    val isImeVisible = rememberIsImeVisible()

    val availableTabs = remember {
        listOf(
            PerawatanTabKind.JADWAL,
            PerawatanTabKind.RIWAYAT
        )
    }

    var activeTabKind by rememberSaveable { mutableStateOf(PerawatanTabKind.JADWAL) }
    val selectedTabIndex = availableTabs.indexOf(activeTabKind).coerceAtLeast(0)

    var jadwalFilter by rememberSaveable { mutableStateOf(PerawatanScheduleFilter.SEMUA) }
    var jadwalQuery by rememberSaveable { mutableStateOf("") }

    var riwayatPeriod by rememberSaveable { mutableStateOf(PerawatanHistoryPeriodFilter.SEMUA) }
    var riwayatQuery by rememberSaveable { mutableStateOf("") }

    var showAddEditSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PerawatanItem?>(null) }
    var itemToComplete by remember { mutableStateOf<PerawatanItem?>(null) }
    var itemToSnooze by remember { mutableStateOf<PerawatanItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PerawatanItem?>(null) }
    var logToEdit by remember { mutableStateOf<PerawatanLog?>(null) }
    var logToDelete by remember { mutableStateOf<PerawatanLog?>(null) }

    val scheduleStats = remember(evaluations) {
        PerawatanScheduler.summarizeScheduleStats(evaluations)
    }
    val missingTemplateCount = remember(evaluations) {
        PerawatanScheduler.findMissingDefaultTemplates(evaluations.map { it.item }).size
    }
    val componentMetricsMap = remember(logs) {
        PerawatanScheduler.buildComponentMetricsMap(logs)
    }
    val filteredEvaluations = remember(evaluations, jadwalFilter, jadwalQuery) {
        PerawatanScheduler.filterEvaluations(evaluations, jadwalFilter, jadwalQuery)
    }

    val filteredLogs = remember(logs, riwayatPeriod, riwayatQuery) {
        PerawatanScheduler.filterHistoryLogs(logs, riwayatPeriod, riwayatQuery)
    }
    val historySummary = remember(filteredLogs) {
        PerawatanScheduler.summarizeHistory(filteredLogs)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("perawatan_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = activeTabKind == PerawatanTabKind.JADWAL && !isImeVisible,
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
                        contentDescription = stringResource(R.string.perawatan_fab_add_cd)
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
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                availableTabs.forEach { tabKind ->
                    val isSelected = activeTabKind == tabKind
                    when (tabKind) {
                        PerawatanTabKind.JADWAL -> {
                            Tab(
                                selected = isSelected,
                                onClick = { activeTabKind = PerawatanTabKind.JADWAL },
                                text = {
                                    Text(
                                        text = stringResource(R.string.perawatan_tab_jadwal, evaluations.size),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
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
                        }
                        PerawatanTabKind.RIWAYAT -> {
                            Tab(
                                selected = isSelected,
                                onClick = { activeTabKind = PerawatanTabKind.RIWAYAT },
                                text = {
                                    Text(
                                        text = stringResource(R.string.perawatan_tab_riwayat, logs.size),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
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
                        }
                    }
                }
            }

            when (activeTabKind) {
                PerawatanTabKind.JADWAL -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = if (maxWidth >= 600.dp) 2 else 1
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PerawatanSummaryBanner(
                                    stats = scheduleStats,
                                    missingTemplateCount = missingTemplateCount,
                                    onRestoreDefaults = { viewModel.restoreMissingDefaultPerawatan() }
                                )
                            }

                            if (evaluations.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    SantoyaSearchBar(
                                        query = jadwalQuery,
                                        onQueryChange = { jadwalQuery = it },
                                        placeholder = stringResource(R.string.perawatan_search_jadwal_hint),
                                        testTag = "search_jadwal_perawatan",
                                        resultCount = if (jadwalQuery.isNotBlank()) filteredEvaluations.size else null
                                    )
                                }

                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    val filters = listOf(
                                        PerawatanScheduleFilter.SEMUA to stringResource(
                                            R.string.perawatan_filter_all,
                                            scheduleStats.totalCount
                                        ),
                                        PerawatanScheduleFilter.PERLU_TINDAKAN to stringResource(
                                            R.string.perawatan_filter_due,
                                            scheduleStats.dueActionCount
                                        ),
                                        PerawatanScheduleFilter.TERLAMBAT to stringResource(
                                            R.string.perawatan_filter_overdue,
                                            scheduleStats.overdueCount
                                        ),
                                        PerawatanScheduleFilter.AMAN to stringResource(
                                            R.string.perawatan_filter_safe,
                                            scheduleStats.safeCount
                                        )
                                    )
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        filters.forEach { (filterKey, label) ->
                                            item(key = filterKey.name) {
                                                val isSelected = jadwalFilter == filterKey
                                                SantoyaFilterPill(
                                                    label = label,
                                                    selected = isSelected,
                                                    onClick = { jadwalFilter = filterKey },
                                                    leadingIcon = if (isSelected) Icons.Outlined.Check else null,
                                                    modifier = Modifier.testTag("filter_jadwal_${filterKey.name.lowercase()}")
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            when {
                                evaluations.isEmpty() -> {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        SantoyaEmptyState(
                                            emptyType = SantoyaEmptyType.LAPORAN,
                                            title = stringResource(R.string.perawatan_empty_jadwal_title),
                                            subtitle = stringResource(R.string.perawatan_empty_jadwal_subtitle),
                                            primaryActionLabel = stringResource(R.string.perawatan_empty_jadwal_cta),
                                            primaryActionIcon = Icons.Outlined.Add,
                                            onPrimaryAction = {
                                                itemToEdit = null
                                                showAddEditSheet = true
                                            },
                                            secondaryActionLabel = if (missingTemplateCount > 0) {
                                                stringResource(R.string.perawatan_empty_jadwal_restore_cta)
                                            } else {
                                                null
                                            },
                                            secondaryActionIcon = Icons.Outlined.AutoFixHigh,
                                            onSecondaryAction = if (missingTemplateCount > 0) {
                                                { viewModel.restoreMissingDefaultPerawatan() }
                                            } else {
                                                null
                                            },
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                                filteredEvaluations.isEmpty() -> {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        SantoyaEmptyState(
                                            emptyType = SantoyaEmptyType.LAPORAN,
                                            title = stringResource(R.string.perawatan_empty_filter_title),
                                            subtitle = stringResource(R.string.perawatan_empty_filter_subtitle),
                                            primaryActionLabel = stringResource(R.string.action_reset),
                                            primaryActionIcon = Icons.Outlined.RestartAlt,
                                            onPrimaryAction = {
                                                jadwalFilter = PerawatanScheduleFilter.SEMUA
                                                jadwalQuery = ""
                                            },
                                            compact = true,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                                else -> {
                                    items(filteredEvaluations, key = { it.item.syncId }, contentType = { "perawatan_schedule_card" }) { eval ->
                                        PerawatanScheduleCard(
                                            evaluation = eval,
                                            metrics = componentMetricsMap[eval.item.syncId],
                                            searchQuery = jadwalQuery,
                                            onComplete = { itemToComplete = eval.item },
                                            onSnooze = { itemToSnooze = eval.item },
                                            onEdit = {
                                                itemToEdit = eval.item
                                                showAddEditSheet = true
                                            },
                                            onDelete = { itemToDelete = eval.item }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                PerawatanTabKind.RIWAYAT -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = if (maxWidth >= 600.dp) 2 else 1
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PerawatanHistorySummaryBanner(summary = historySummary)
                            }

                            if (logs.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    SantoyaSearchBar(
                                        query = riwayatQuery,
                                        onQueryChange = { riwayatQuery = it },
                                        placeholder = stringResource(R.string.perawatan_search_riwayat_hint),
                                        testTag = "search_riwayat_perawatan",
                                        resultCount = if (riwayatQuery.isNotBlank()) filteredLogs.size else null
                                    )
                                }

                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val periods = listOf(
                                            PerawatanHistoryPeriodFilter.SEMUA to R.string.perawatan_period_all,
                                            PerawatanHistoryPeriodFilter.BULAN_INI to R.string.perawatan_period_month,
                                            PerawatanHistoryPeriodFilter.SEMBILAN_PULUH_HARI to R.string.perawatan_period_90d,
                                            PerawatanHistoryPeriodFilter.TAHUN_INI to R.string.perawatan_period_year
                                        )
                                        periods.forEach { (periodKey, labelRes) ->
                                            item(key = periodKey.name) {
                                                val isSelected = riwayatPeriod == periodKey
                                                SantoyaFilterPill(
                                                    label = stringResource(labelRes),
                                                    selected = isSelected,
                                                    onClick = { riwayatPeriod = periodKey },
                                                    leadingIcon = if (isSelected) Icons.Outlined.Check else null,
                                                    modifier = Modifier.testTag("filter_riwayat_${periodKey.name.lowercase()}")
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            when {
                                logs.isEmpty() -> {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        SantoyaEmptyState(
                                            emptyType = SantoyaEmptyType.LAPORAN,
                                            title = stringResource(R.string.perawatan_empty_riwayat_title),
                                            subtitle = stringResource(R.string.perawatan_empty_riwayat_subtitle),
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                                filteredLogs.isEmpty() -> {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        SantoyaEmptyState(
                                            emptyType = SantoyaEmptyType.LAPORAN,
                                            title = stringResource(R.string.perawatan_empty_filter_title),
                                            subtitle = stringResource(R.string.perawatan_empty_filter_subtitle),
                                            primaryActionLabel = stringResource(R.string.action_reset),
                                            primaryActionIcon = Icons.Outlined.RestartAlt,
                                            onPrimaryAction = {
                                                riwayatPeriod = PerawatanHistoryPeriodFilter.SEMUA
                                                riwayatQuery = ""
                                            },
                                            compact = true,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                                else -> {
                                    items(filteredLogs, key = { it.syncId }, contentType = { "perawatan_history_card" }) { log ->
                                        PerawatanHistoryCard(
                                            log = log,
                                            searchQuery = riwayatQuery,
                                            onEdit = { logToEdit = log },
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

    itemToSnooze?.let { target ->
        ReschedulePerawatanBottomSheet(
            item = target,
            onDismiss = { itemToSnooze = null },
            onConfirm = { newDueMillis ->
                viewModel.reschedulePerawatan(item = target, newDueMillis = newDueMillis)
                itemToSnooze = null
            }
        )
    }

    logToEdit?.let { targetLog ->
        EditPerawatanLogBottomSheet(
            log = targetLog,
            onDismiss = { logToEdit = null },
            onConfirm = { tanggalSelesaiBaru, biayaBaru, catatanBaru ->
                viewModel.updatePerawatanLog(
                    log = targetLog,
                    tanggalSelesaiBaru = tanggalSelesaiBaru,
                    biayaBaru = biayaBaru,
                    catatanBaru = catatanBaru
                )
                logToEdit = null
            }
        )
    }

    itemToDelete?.let { target ->
        DeletePerawatanScheduleDialog(
            item = target,
            onDismiss = { itemToDelete = null },
            onConfirmDelete = {
                viewModel.deletePerawatan(target)
                itemToDelete = null
            }
        )
    }

    logToDelete?.let { targetLog ->
        DeletePerawatanLogDialog(
            log = targetLog,
            onDismiss = { logToDelete = null },
            onConfirmDelete = {
                viewModel.deletePerawatanLog(targetLog)
                logToDelete = null
            }
        )
    }
}
