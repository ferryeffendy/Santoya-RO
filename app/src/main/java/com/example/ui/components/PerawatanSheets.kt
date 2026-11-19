package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.PerawatanItem
import com.example.data.entity.PerawatanLog
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.PerawatanFormError
import com.example.util.PerawatanScheduler
import java.util.Calendar

private val COST_PRESETS = listOf(0.0, 25000.0, 50000.0, 100000.0, 150000.0)
private val SNOOZE_PRESET_DAYS = listOf(1, 3, 7, 14, 30)
private val INTERVAL_PRESET_DAYS = listOf(7, 14, 30, 60, 90, 180)

/**
 * Lembar formulir tambah atau ubah jadwal perawatan komponen mesin RO dengan templat cepat
 * dan validasi selaras batas Firestore.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerawatanFormBottomSheet(
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
    var formError by remember(initialItem) { mutableStateOf<PerawatanFormError?>(null) }

    val submit = {
        val validation = PerawatanScheduler.validateScheduleInput(nama, intervalStr)
        val validInterval = validation.intervalHari
        if (!validation.isValid || validInterval == null) {
            formError = validation.error
        } else {
            onSave(validation.cleanName, validInterval, customDueMillis)
        }
    }

    SantoyaFormBottomSheet(
        title = stringResource(
            if (initialItem == null) R.string.perawatan_form_add_title else R.string.perawatan_form_edit_title
        ),
        subtitle = stringResource(R.string.perawatan_form_subtitle),
        headerAccent = MaterialTheme.colorScheme.primary,
        headerIconContent = {
            PerawatanComponentIcon(
                syncId = initialItem?.syncId.orEmpty(),
                nama = nama,
                tint = MaterialTheme.santoyaColors.onHero,
                modifier = Modifier.size(21.dp)
            )
        },
        confirmLabel = stringResource(
            if (initialItem == null) R.string.perawatan_form_btn_add else R.string.perawatan_form_btn_save
        ),
        dismissLabel = stringResource(R.string.action_cancel),
        confirmContainerColor = MaterialTheme.colorScheme.primary,
        confirmTestTag = "btn_simpan_jadwal_perawatan",
        onDismiss = onDismiss,
        onConfirm = { submit() }
    ) {
        if (initialItem == null) {
            Text(
                text = stringResource(R.string.perawatan_form_templates_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PerawatanScheduler.DEFAULT_TEMPLATES.forEach { tpl ->
                    val isSelected = nama.trim().equals(tpl.nama, ignoreCase = true)
                    SantoyaFilterPill(
                        label = stringResource(
                            R.string.perawatan_form_template_chip,
                            tpl.nama,
                            tpl.intervalHari
                        ),
                        selected = isSelected,
                        onClick = {
                            nama = tpl.nama
                            intervalStr = tpl.intervalHari.toString()
                            formError = null
                            if (!hasManuallyPickedDate) {
                                customDueMillis = PerawatanScheduler.calculateNextDueDateMillis(
                                    completionDateMillis = System.currentTimeMillis(),
                                    intervalHari = tpl.intervalHari
                                )
                            }
                        },
                        leadingIcon = if (isSelected) Icons.Outlined.Check else null
                    )
                }
            }
        }

        OutlinedTextField(
            value = nama,
            onValueChange = {
                nama = it.take(PerawatanScheduler.MAX_SCHEDULE_NAME_LENGTH)
                formError = null
            },
            label = { Text(stringResource(R.string.perawatan_form_name_label)) },
            placeholder = { Text(stringResource(R.string.perawatan_form_name_hint)) },
            isError = formError == PerawatanFormError.NAME_REQUIRED || formError == PerawatanFormError.NAME_TOO_LONG,
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
                formError = null
                val parsed = digits.toIntOrNull()
                if (!hasManuallyPickedDate && parsed != null && parsed >= 1) {
                    val base = initialItem?.terakhirSelesai ?: System.currentTimeMillis()
                    customDueMillis = PerawatanScheduler.calculateNextDueDateMillis(base, parsed)
                }
            },
            label = { Text(stringResource(R.string.perawatan_form_interval_label)) },
            placeholder = { Text(stringResource(R.string.perawatan_form_interval_hint)) },
            isError = formError == PerawatanFormError.INTERVAL_INVALID,
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
            text = stringResource(R.string.perawatan_form_preset_intervals_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            INTERVAL_PRESET_DAYS.forEach { days ->
                val isSelected = intervalStr.toIntOrNull() == days
                SantoyaFilterPill(
                    label = stringResource(R.string.perawatan_form_preset_days, days),
                    selected = isSelected,
                    onClick = {
                        intervalStr = days.toString()
                        formError = null
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
            Text(
                text = stringResource(
                    R.string.perawatan_form_next_due_btn,
                    FormatUtils.formatDateUi(customDueMillis)
                )
            )
        }

        formError?.let { error ->
            val errRes = when (error) {
                PerawatanFormError.NAME_REQUIRED -> R.string.perawatan_err_name_required
                PerawatanFormError.NAME_TOO_LONG -> R.string.perawatan_err_name_too_long
                PerawatanFormError.INTERVAL_INVALID -> R.string.perawatan_err_interval_invalid
            }
            Text(
                text = stringResource(errRes),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Lembar penyelesaian jadwal perawatan dengan pemilih tanggal, pilihan cepat biaya,
 * pratinjau nominal Rupiah, catatan pengerjaan, dan pratinjau jatuh tempo berikutnya.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompletePerawatanBottomSheet(
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

    val parsedBiaya = biayaStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val predictedNextDue = remember(tanggalSelesai, item.intervalHari) {
        PerawatanScheduler.calculateNextDueDateMillis(tanggalSelesai, item.intervalHari)
    }

    SantoyaFormBottomSheet(
        title = stringResource(R.string.perawatan_complete_title),
        subtitle = stringResource(R.string.perawatan_complete_subtitle, item.nama, item.intervalHari),
        headerAccent = MaterialTheme.santoyaColors.success,
        headerIconContent = {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.santoyaColors.onHero,
                modifier = Modifier.size(20.dp)
            )
        },
        confirmLabel = stringResource(R.string.perawatan_complete_btn_save),
        dismissLabel = stringResource(R.string.action_cancel),
        confirmContainerColor = MaterialTheme.santoyaColors.success,
        confirmTestTag = "btn_konfirmasi_selesai_perawatan",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(tanggalSelesai, parsedBiaya, catatan.trim()) }
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
            Text(
                text = stringResource(
                    R.string.perawatan_complete_date_btn,
                    FormatUtils.formatDateUi(tanggalSelesai)
                )
            )
        }

        Text(
            text = stringResource(R.string.perawatan_complete_cost_presets_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            COST_PRESETS.forEach { preset ->
                val isSelected = if (preset == 0.0) {
                    biayaStr.isEmpty() || parsedBiaya == 0.0
                } else {
                    parsedBiaya == preset
                }
                val label = if (preset == 0.0) {
                    stringResource(R.string.perawatan_complete_cost_free)
                } else {
                    FormatUtils.formatRupiah(preset)
                }
                SantoyaFilterPill(
                    label = label,
                    selected = isSelected,
                    onClick = {
                        biayaStr = if (preset == 0.0) "" else preset.toLong().toString()
                    },
                    leadingIcon = if (isSelected) Icons.Outlined.Check else null
                )
            }
        }

        OutlinedTextField(
            value = biayaStr,
            onValueChange = { raw -> biayaStr = raw.filter { it.isDigit() }.take(9) },
            label = { Text(stringResource(R.string.perawatan_complete_cost_label)) },
            placeholder = { Text(stringResource(R.string.perawatan_complete_cost_hint)) },
            supportingText = {
                Text(
                    text = if (parsedBiaya > 0.0) {
                        stringResource(
                            R.string.perawatan_complete_cost_support_preview,
                            FormatUtils.formatRupiah(parsedBiaya)
                        )
                    } else {
                        stringResource(R.string.perawatan_complete_cost_support_empty)
                    }
                )
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
            label = { Text(stringResource(R.string.perawatan_complete_notes_label)) },
            placeholder = { Text(stringResource(R.string.perawatan_complete_notes_hint)) },
            supportingText = {
                Text(stringResource(R.string.perawatan_complete_notes_count, catatan.length))
            },
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
                    text = stringResource(
                        R.string.perawatan_complete_next_preview,
                        FormatUtils.formatDateUi(predictedNextDue)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Lembar ubah riwayat perawatan yang otomatis menyinkronkan biaya ke catatan Pengeluaran terkait.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPerawatanLogBottomSheet(
    log: PerawatanLog,
    onDismiss: () -> Unit,
    onConfirm: (tanggalSelesaiBaru: Long, biayaBaru: Double, catatanBaru: String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val catatanFocusRequester = remember { FocusRequester() }

    var tanggalSelesai by remember(log) { mutableLongStateOf(log.tanggalSelesai) }
    var biayaStr by remember(log) {
        mutableStateOf(if (log.biaya > 0.0) log.biaya.toLong().toString() else "")
    }
    var catatan by remember(log) { mutableStateOf(log.catatan) }
    val parsedBiaya = biayaStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    SantoyaFormBottomSheet(
        title = stringResource(R.string.perawatan_edit_log_title),
        subtitle = log.namaPerawatan,
        headerAccent = MaterialTheme.colorScheme.tertiary,
        headerIcon = Icons.Outlined.Edit,
        confirmLabel = stringResource(R.string.perawatan_edit_log_btn_save),
        dismissLabel = stringResource(R.string.action_cancel),
        confirmContainerColor = MaterialTheme.colorScheme.tertiary,
        confirmTestTag = "btn_simpan_edit_log_perawatan",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(tanggalSelesai, parsedBiaya, catatan.trim()) }
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
                .testTag("btn_edit_log_tanggal")
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(
                    R.string.perawatan_complete_date_btn,
                    FormatUtils.formatDateUi(tanggalSelesai)
                )
            )
        }

        Text(
            text = stringResource(R.string.perawatan_complete_cost_presets_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            COST_PRESETS.forEach { preset ->
                val isSelected = if (preset == 0.0) {
                    biayaStr.isEmpty() || parsedBiaya == 0.0
                } else {
                    parsedBiaya == preset
                }
                val label = if (preset == 0.0) {
                    stringResource(R.string.perawatan_complete_cost_free)
                } else {
                    FormatUtils.formatRupiah(preset)
                }
                SantoyaFilterPill(
                    label = label,
                    selected = isSelected,
                    onClick = {
                        biayaStr = if (preset == 0.0) "" else preset.toLong().toString()
                    },
                    leadingIcon = if (isSelected) Icons.Outlined.Check else null
                )
            }
        }

        OutlinedTextField(
            value = biayaStr,
            onValueChange = { raw -> biayaStr = raw.filter { it.isDigit() }.take(9) },
            label = { Text(stringResource(R.string.perawatan_complete_cost_label)) },
            placeholder = { Text(stringResource(R.string.perawatan_complete_cost_hint)) },
            supportingText = {
                Text(
                    text = if (parsedBiaya > 0.0) {
                        stringResource(
                            R.string.perawatan_edit_log_sync_info_paid,
                            FormatUtils.formatRupiah(parsedBiaya)
                        )
                    } else {
                        stringResource(R.string.perawatan_edit_log_sync_info_free)
                    }
                )
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
                .testTag("input_edit_log_biaya")
        )

        OutlinedTextField(
            value = catatan,
            onValueChange = { catatan = it.take(500) },
            label = { Text(stringResource(R.string.perawatan_complete_notes_label)) },
            placeholder = { Text(stringResource(R.string.perawatan_complete_notes_hint)) },
            supportingText = {
                Text(stringResource(R.string.perawatan_complete_notes_count, catatan.length))
            },
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
                .testTag("input_edit_log_catatan")
        )
    }
}

/**
 * Lembar penundaan cepat (snooze) atau pemilih tanggal khusus untuk menggeser jatuh tempo perawatan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReschedulePerawatanBottomSheet(
    item: PerawatanItem,
    onDismiss: () -> Unit,
    onConfirm: (newDueMillis: Long) -> Unit
) {
    val context = LocalContext.current
    val santoyaColors = MaterialTheme.santoyaColors
    var selectedDueMillis by remember(item) {
        mutableLongStateOf(
            PerawatanScheduler.calculateSnoozedDueDateMillis(
                currentDueMillis = item.jadwalBerikutnya,
                snoozeDays = 7
            )
        )
    }
    var selectedPresetDays by remember(item) { mutableStateOf<Int?>(7) }

    SantoyaFormBottomSheet(
        title = stringResource(R.string.perawatan_snooze_title),
        subtitle = stringResource(
            R.string.perawatan_snooze_subtitle,
            item.nama,
            FormatUtils.formatDateUi(item.jadwalBerikutnya)
        ),
        headerAccent = santoyaColors.warning,
        headerIcon = Icons.Outlined.Snooze,
        confirmLabel = stringResource(R.string.perawatan_snooze_btn_confirm),
        dismissLabel = stringResource(R.string.action_cancel),
        confirmContainerColor = MaterialTheme.colorScheme.primary,
        confirmTestTag = "btn_simpan_tunda_perawatan",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(selectedDueMillis) }
    ) {
        Text(
            text = stringResource(R.string.perawatan_snooze_presets_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SNOOZE_PRESET_DAYS.forEach { days ->
                val isSelected = selectedPresetDays == days
                SantoyaFilterPill(
                    label = stringResource(R.string.perawatan_snooze_preset_days, days),
                    selected = isSelected,
                    onClick = {
                        selectedPresetDays = days
                        selectedDueMillis = PerawatanScheduler.calculateSnoozedDueDateMillis(
                            currentDueMillis = item.jadwalBerikutnya,
                            snoozeDays = days
                        )
                    },
                    leadingIcon = if (isSelected) Icons.Outlined.Check else null,
                    modifier = Modifier.testTag("chip_tunda_${days}_hari")
                )
            }
        }

        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = selectedDueMillis }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val pickedCal = Calendar.getInstance().apply {
                            set(y, m, d, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedDueMillis = pickedCal.timeInMillis
                        selectedPresetDays = null
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
                .testTag("btn_tunda_pilih_tanggal")
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(
                    R.string.perawatan_snooze_custom_date_btn,
                    FormatUtils.formatDateUi(selectedDueMillis)
                )
            )
        }

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
                    text = stringResource(
                        R.string.perawatan_snooze_preview,
                        FormatUtils.formatDateUi(selectedDueMillis)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Dialog konfirmasi hapus jadwal perawatan.
 */
@Composable
fun DeletePerawatanScheduleDialog(
    item: PerawatanItem,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.perawatan_delete_schedule_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.perawatan_delete_schedule_msg,
                    item.nama,
                    item.intervalHari
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(SantoyaRadius.sm)
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Dialog konfirmasi hapus riwayat perawatan beserta info penghapusan catatan pengeluaran terkait.
 */
@Composable
fun DeletePerawatanLogDialog(
    log: PerawatanLog,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.perawatan_delete_log_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val formattedDate = FormatUtils.formatDateUi(log.tanggalSelesai)
            val msg = if (log.biaya > 0.0 && !log.pengeluaranSyncId.isNullOrBlank()) {
                stringResource(
                    R.string.perawatan_delete_log_msg_with_expense,
                    log.namaPerawatan,
                    formattedDate,
                    FormatUtils.formatRupiah(log.biaya)
                )
            } else {
                stringResource(
                    R.string.perawatan_delete_log_msg,
                    log.namaPerawatan,
                    formattedDate
                )
            }
            Text(text = msg, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(SantoyaRadius.sm)
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
