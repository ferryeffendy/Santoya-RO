package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.TransaksiDetail
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.KasbonPaymentBreakdown
import com.example.util.KaryawanScopeHelper
import com.example.util.PelangganAuditHelper

/**
 * Bottom sheet form Tambah / Edit Pelanggan dengan validasi harga khusus
 * dan pengingat lembut anti-duplikasi data pelanggan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PelangganFormDialog(
    title: String,
    pelanggan: Pelanggan?,
    existingPelanggan: List<Pelanggan> = emptyList(),
    kurirList: List<Kurir> = emptyList(),
    isKaryawan: Boolean = false,
    activeKaryawan: Kurir? = null,
    initialNama: String = "",
    onDismiss: () -> Unit,
    onConfirm: (nama: String, alamat: String, noHp: String, hargaKhusus: Double?, idKurirLangganan: Int?) -> Unit
) {
    var nama by remember(pelanggan?.id, initialNama) {
        mutableStateOf(pelanggan?.nama ?: initialNama)
    }
    var alamat by remember(pelanggan?.id) {
        mutableStateOf(KaryawanScopeHelper.cleanPelangganAlamat(pelanggan?.alamat ?: ""))
    }
    var noHp by remember(pelanggan?.id) { mutableStateOf(pelanggan?.no_hp ?: "") }
    var hargaKhususStr by remember(pelanggan?.id) {
        mutableStateOf(
            pelanggan?.harga_khusus?.takeIf { it > 0.0 }?.toLong()?.toString() ?: ""
        )
    }
    val lockedKurirId = if (isKaryawan && activeKaryawan != null) activeKaryawan.id else null
    var selectedKurirId by remember(lockedKurirId, pelanggan?.id) {
        mutableStateOf<Int?>(lockedKurirId ?: pelanggan?.id_kurir_langganan)
    }
    val hasExistingOptionalData = remember(pelanggan?.id) {
        pelanggan != null && (
            KaryawanScopeHelper.cleanPelangganAlamat(pelanggan.alamat).isNotBlank() ||
                (pelanggan.harga_khusus != null && pelanggan.harga_khusus > 0.0) ||
                pelanggan.id_kurir_langganan != null
            )
    }
    var showOptionalFields by remember(pelanggan?.id) { mutableStateOf(hasExistingOptionalData) }
    var errorMessage by remember(pelanggan?.id) { mutableStateOf<String?>(null) }

    val duplicateCandidate = remember(nama, noHp, existingPelanggan, pelanggan?.id) {
        PelangganAuditHelper.findPotentialDuplicatePelanggan(
            nama = nama,
            noHp = noHp,
            existingList = existingPelanggan,
            excludeId = pelanggan?.id
        )
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val namaFocus = remember { FocusRequester() }
    val alamatFocus = remember { FocusRequester() }
    val hpFocus = remember { FocusRequester() }
    val hargaFocus = remember { FocusRequester() }
    val errorNameRequired = stringResource(R.string.pelanggan_form_error_name)

    val submitForm = {
        if (nama.isBlank()) {
            errorMessage = errorNameRequired
        } else {
            val hKhusus = PelangganAuditHelper.normalizeSpecialPrice(hargaKhususStr.toDoubleOrNull())
            focusManager.clearFocus()
            keyboardController?.hide()
            onConfirm(nama.trim(), alamat.trim(), noHp.trim(), hKhusus, selectedKurirId)
        }
    }

    SantoyaFormBottomSheet(
        title = title,
        subtitle = stringResource(R.string.pelanggan_form_subtitle_simple),
        headerIconContent = {
            HydroCustomerDropIcon(
                tint = MaterialTheme.santoyaColors.onHero,
                modifier = Modifier.size(21.dp)
            )
        },
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.action_save),
        confirmTestTag = "btn_save_pelanggan",
        onConfirm = submitForm
    ) {
        OutlinedTextField(
            value = nama,
            onValueChange = {
                nama = it
                errorMessage = null
            },
            label = { Text(stringResource(R.string.pelanggan_form_name_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { hpFocus.requestFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(namaFocus)
                .bringIntoViewOnFocus()
                .testTag("input_pelanggan_nama"),
            shape = RoundedCornerShape(12.dp)
        )

        if (duplicateCandidate != null) {
            val santoyaColors = MaterialTheme.santoyaColors
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = santoyaColors.warningContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, santoyaColors.warning.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = santoyaColors.onWarningContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.pelanggan_duplicate_warning, duplicateCandidate.nama),
                        style = MaterialTheme.typography.labelSmall,
                        color = santoyaColors.onWarningContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        val launchContactPicker = rememberContactPhonePicker(
            onPhonePicked = { normalizedPhone, contactName ->
                noHp = normalizedPhone
                if (nama.isBlank() && !contactName.isNullOrBlank()) {
                    nama = contactName
                }
                errorMessage = null
            },
            onError = { msg -> errorMessage = msg }
        )

        OutlinedTextField(
            value = noHp,
            onValueChange = { noHp = it.filter { c -> c.isDigit() || c == '+' } },
            label = { Text(stringResource(R.string.pelanggan_form_phone_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = if (showOptionalFields) ImeAction.Next else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    if (showOptionalFields) {
                        alamatFocus.requestFocus()
                    }
                },
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            ),
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = launchContactPicker,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_form_pick_contact")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContactPhone,
                        contentDescription = stringResource(R.string.pelanggan_add_contact_cd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(hpFocus)
                .bringIntoViewOnFocus()
                .testTag("input_pelanggan_hp"),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedButton(
            onClick = { showOptionalFields = !showOptionalFields },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_toggle_pelanggan_optional")
        ) {
            Text(
                text = if (showOptionalFields) {
                    stringResource(R.string.pelanggan_form_optional_hide)
                } else {
                    stringResource(R.string.pelanggan_form_optional_show)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        androidx.compose.animation.AnimatedVisibility(visible = showOptionalFields) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text(stringResource(R.string.pelanggan_form_address_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (isKaryawan) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { if (!isKaryawan) hargaFocus.requestFocus() },
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(alamatFocus)
                        .bringIntoViewOnFocus()
                        .testTag("input_pelanggan_alamat"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = stringResource(R.string.pelanggan_form_section_langganan),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (isKaryawan) {
                    val lockedHargaKhusus = pelanggan?.harga_khusus?.takeIf { it > 0.0 }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = if (lockedHargaKhusus != null) {
                                        "Harga Khusus: ${FormatUtils.formatRupiah(lockedHargaKhusus)}"
                                    } else {
                                        "Tarif: Harga Normal"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Harga diatur oleh Pemilik.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = hargaKhususStr,
                        onValueChange = { hargaKhususStr = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.pelanggan_form_price_label)) },
                        placeholder = { Text(stringResource(R.string.pelanggan_form_price_placeholder)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(hargaFocus)
                            .bringIntoViewOnFocus()
                            .testTag("input_pelanggan_harga"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = stringResource(R.string.pelanggan_form_price_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.pelanggan_form_kurir_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (isKaryawan && activeKaryawan != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.staff_auto_courier_prefix, activeKaryawan.nama),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.staff_auto_customer_note),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.pelanggan_form_kurir_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SantoyaFilterPill(
                            label = stringResource(R.string.pelanggan_form_kurir_none),
                            selected = selectedKurirId == null,
                            onClick = { selectedKurirId = null }
                        )
                        kurirList.forEach { kurir ->
                            SantoyaFilterPill(
                                label = kurir.nama,
                                selected = selectedKurirId == kurir.id,
                                onClick = { selectedKurirId = kurir.id }
                            )
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Bottom sheet Pelunasan Kasbon dengan rincian Masuk Kas (maksimal sebesar sisa kasbon)
 * dan Kembalian otomatis bila nominal diterima melebihi tagihan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PelunasanKasbonDialog(
    pelanggan: Pelanggan,
    unpaidList: List<TransaksiDetail> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmBayar: (breakdown: KasbonPaymentBreakdown, metodeBayar: String) -> Unit
) {
    var nominalStr by remember(pelanggan.id) { mutableStateOf(pelanggan.sisa_kasbon.toLong().toString()) }
    var selectedMetode by remember(pelanggan.id) { mutableStateOf("Cash") }
    var errorMessage by remember(pelanggan.id) { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputNominal = nominalStr.toDoubleOrNull() ?: 0.0
    val breakdown = remember(pelanggan.sisa_kasbon, inputNominal) {
        PelangganAuditHelper.calculateKasbonPaymentBreakdown(
            sisaKasbon = pelanggan.sisa_kasbon,
            inputNominal = inputNominal
        )
    }

    val submitPelunasan = {
        val nominal = nominalStr.toDoubleOrNull()
        when {
            nominal == null || nominal <= 0 -> {
                errorMessage = "Nominal pembayaran tidak valid"
            }
            else -> {
                focusManager.clearFocus()
                keyboardController?.hide()
                val finalBreakdown = PelangganAuditHelper.calculateKasbonPaymentBreakdown(
                    sisaKasbon = pelanggan.sisa_kasbon,
                    inputNominal = nominal
                )
                onConfirmBayar(finalBreakdown, selectedMetode)
            }
        }
    }

    SantoyaFormBottomSheet(
        title = stringResource(R.string.kasbon_dialog_title),
        subtitle = "Pelanggan: ${pelanggan.nama}",
        headerIcon = Icons.Outlined.Payments,
        headerAccent = MaterialTheme.colorScheme.error,
        onDismiss = onDismiss,
        confirmLabel = "Konfirmasi",
        confirmTestTag = "btn_confirm_pelunasan_kasbon",
        onConfirm = submitPelunasan
    ) {
        Text(
            text = "Total Kasbon Saat Ini: ${FormatUtils.formatRupiah(pelanggan.sisa_kasbon)}",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )

        if (unpaidList.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Nota Belum Lunas (${unpaidList.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    unpaidList.take(4).forEach { t ->
                        val noteSisaKasbon = t.transaksi.remainingKasbonAmount()
                        val dpPaid = t.transaksi.uangMuka
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (dpPaid > 0L) {
                                    stringResource(
                                        R.string.pelanggan_kasbon_note_with_uang_muka,
                                        FormatUtils.formatDateUi(t.transaksi.tanggal),
                                        t.transaksi.jumlah,
                                        FormatUtils.formatRupiah(dpPaid.toDouble())
                                    )
                                } else {
                                    "• ${FormatUtils.formatDateUi(t.transaksi.tanggal)} (${t.transaksi.jumlah} gln)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = FormatUtils.formatRupiah(noteSisaKasbon),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (unpaidList.size > 4) {
                        Text(
                            text = "+ ${unpaidList.size - 4} nota lainnya",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        val quickPresets = remember(pelanggan.sisa_kasbon) {
            val total = pelanggan.sisa_kasbon.toLong()
            listOfNotNull(
                "Lunas Penuh" to total,
                (total / 2).takeIf { it > 0 && total > 10_000 }?.let { "50%" to it },
                10_000L.takeIf { it < total }?.let { "10rb" to it },
                20_000L.takeIf { it < total }?.let { "20rb" to it }
            ).distinctBy { it.second }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPresets.forEach { (label, amount) ->
                val isSelected = nominalStr == amount.toString()
                SantoyaFilterPill(
                    label = label,
                    selected = isSelected,
                    onClick = {
                        nominalStr = amount.toString()
                        errorMessage = null
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        OutlinedTextField(
            value = nominalStr,
            onValueChange = {
                nominalStr = it.filter { c -> c.isDigit() }
                errorMessage = null
            },
            label = { Text(stringResource(R.string.pelanggan_kasbon_nominal_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { submitPelunasan() }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus()
                .testTag("input_pelunasan_nominal"),
            shape = RoundedCornerShape(12.dp)
        )

        Text(
            text = "Metode Pelunasan",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Cash", "Transfer").forEach { metode ->
                SantoyaFilterPill(
                    label = FormatUtils.formatMetodeBayarLabel(metode),
                    selected = selectedMetode == metode,
                    onClick = { selectedMetode = metode },
                    centerContent = true,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val santoyaColors = MaterialTheme.santoyaColors
        Surface(
            shape = RoundedCornerShape(SantoyaRadius.sm),
            color = if (breakdown.isLunas) {
                santoyaColors.successContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (breakdown.kembalian > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.pelanggan_kasbon_recorded_cash),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = santoyaColors.onSuccessContainer
                        )
                        Text(
                            text = FormatUtils.formatRupiah(breakdown.effectivePaid),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = santoyaColors.onSuccessContainer
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sisa setelah bayar:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (breakdown.isLunas) {
                            stringResource(R.string.pelanggan_kasbon_lunas_badge)
                        } else {
                            FormatUtils.formatRupiah(breakdown.remainingAfterPay)
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = if (breakdown.isLunas) santoyaColors.onSuccessContainer else santoyaColors.danger
                    )
                }
                if (breakdown.kembalian > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.pelanggan_kasbon_kembalian),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = santoyaColors.onSuccessContainer
                        )
                        Text(
                            text = FormatUtils.formatRupiah(breakdown.kembalian),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = santoyaColors.onSuccessContainer
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Dialog konfirmasi hapus atau hapus permanen pelanggan dengan peringatan kasbon aktif.
 */
@Composable
fun DeletePelangganConfirmDialog(
    pelanggan: Pelanggan,
    isKaryawan: Boolean,
    activeKaryawanNama: String?,
    onDismiss: () -> Unit,
    onConfirm: (isPermanentDelete: Boolean) -> Unit
) {
    val isAuditDeleted = KaryawanScopeHelper.isSoftDeletedPelanggan(pelanggan)
    val deletedBy = KaryawanScopeHelper.extractDeletedByPelanggan(pelanggan) ?: "Karyawan"
    val cleanAlamat = KaryawanScopeHelper.cleanPelangganAlamat(pelanggan.alamat)
    val hasBlockingKasbon = !isAuditDeleted && pelanggan.sisa_kasbon > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = "Peringatan",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = stringResource(
                    if (isAuditDeleted) R.string.pelanggan_delete_title_perm
                    else R.string.pelanggan_delete_title
                ),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = when {
                        isAuditDeleted -> {
                            stringResource(R.string.pelanggan_delete_msg_perm, deletedBy)
                        }
                        isKaryawan -> {
                            stringResource(
                                R.string.pelanggan_delete_msg_staff,
                                activeKaryawanNama ?: "Karyawan"
                            )
                        }
                        else -> {
                            stringResource(R.string.pelanggan_delete_msg_owner)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = pelanggan.nama,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (pelanggan.no_hp.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.pelanggan_phone_prefix, pelanggan.no_hp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (cleanAlamat.isNotBlank()) {
                            Text(
                                text = "Alamat: $cleanAlamat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (pelanggan.sisa_kasbon > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HourglassEmpty,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Masih ada kasbon ${FormatUtils.formatRupiah(pelanggan.sisa_kasbon)}. Lunasi dulu sebelum menghapus.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else if (!isAuditDeleted) {
                    Text(
                        text = "Catatan transaksi lama tetap aman di laporan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(isAuditDeleted) },
                enabled = !hasBlockingKasbon,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    imageVector = if (isAuditDeleted) Icons.Outlined.DeleteForever else Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isAuditDeleted) "Hapus Permanen" else "Hapus")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
