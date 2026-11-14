package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.companion.kzpmrw.R
import com.example.data.entity.Kurir
import com.example.data.entity.TransaksiDetail
import com.example.ui.components.SantoyaAdaptiveCurrencyText
import com.example.ui.components.SantoyaEmptyState
import com.example.ui.components.SantoyaFormBottomSheet
import com.example.ui.components.SantoyaSectionBanner
import com.example.ui.components.bringIntoViewOnFocus
import com.example.ui.components.rememberIsImeVisible
import com.example.ui.theme.santoyaColors
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.KomisiCalculator
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KurirScreen(
    viewModel: SantoyaViewModel,
    kurirList: List<Kurir> = emptyList(),
    transaksiList: List<TransaksiDetail> = emptyList(),
    onBack: () -> Unit = { viewModel.navigateTo(SantoyaScreen.DASHBOARD) },
    modifier: Modifier = Modifier
) {
    val vmKurirList by viewModel.allKurir.collectAsState()
    val vmTransaksiList by viewModel.allTransaksi.collectAsState()
    val displayKurirList = if (kurirList.isNotEmpty()) kurirList else vmKurirList
    val rawTransaksiList = if (transaksiList.isNotEmpty()) transaksiList else vmTransaksiList
    val displayTransaksiList = remember(rawTransaksiList) {
        rawTransaksiList.filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingKurir by remember { mutableStateOf<Kurir?>(null) }
    var kurirToDelete by remember { mutableStateOf<Kurir?>(null) }

    val isImeVisible = rememberIsImeVisible()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("kurir_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isImeVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("tambah_kurir_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Karyawan"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SantoyaSectionBanner(
                    icon = Icons.Default.DeliveryDining,
                    title = "Karyawan & Komisi",
                    subtitle = "Atur akun kurir & tarif komisi",
                    badgeText = "${displayKurirList.size} Karyawan",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onAccentColor = MaterialTheme.colorScheme.onSecondary
                )
            }

            if (displayKurirList.isEmpty()) {
                item {
                    SantoyaEmptyState(
                        illustrationRes = R.drawable.img_empty_depot,
                        title = "Belum Ada Karyawan",
                        subtitle = "Tambahkan akun kurir & tarif komisi per galon agar pengiriman otomatis tercatat.",
                        primaryActionLabel = "Tambah Karyawan",
                        primaryActionIcon = Icons.Default.Add,
                        onPrimaryAction = { showAddDialog = true },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                items(displayKurirList, key = { it.id }) { kurir ->
                    val kurirTx = remember(displayTransaksiList, kurir.id) {
                        displayTransaksiList.filter { it.transaksi.id_kurir == kurir.id }
                    }
                    val compSummary = remember(kurir, kurirTx) {
                        KomisiCalculator.summarizeCourierCompensation(kurir, kurirTx)
                    }
                    val ruleSummaryText = remember(kurir) {
                        KomisiCalculator.formatCompactRuleSummary(kurir)
                    }
                    val colors = MaterialTheme.santoyaColors

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kurir_card_${kurir.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val initials = remember(kurir.nama) {
                                        val parts = kurir.nama.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                                        when {
                                            parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                                            parts.size == 1 -> parts[0].take(2).uppercase()
                                            else -> "K"
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = initials,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = kurir.nama,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = ruleSummaryText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { editingKurir = kurir },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .testTag("btn_edit_kurir_${kurir.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Karyawan",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { kurirToDelete = kurir },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .testTag("btn_delete_kurir_${kurir.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus Karyawan",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Antar: ${FormatUtils.formatNumber(compSummary.totalGallons)} Galon (${compSummary.totalOrders}x)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    SantoyaAdaptiveCurrencyText(
                                        amount = compSummary.totalKomisi,
                                        customText = "Komisi: ${FormatUtils.formatRupiah(compSummary.totalKomisi)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        minFontSize = 10.sp,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }

                            if (kurir.email.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val isActive = kurir.akses_aktif
                                val displayEmail = if (kurir.email.contains("@")) kurir.email else "${kurir.email}@gmail.com"
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isActive) colors.successContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isActive) colors.success.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Email Google: $displayEmail",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isActive) colors.onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = if (isActive) "Aktif" else "Nonaktif",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) colors.success else MaterialTheme.colorScheme.error
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

    if (showAddDialog) {
        KurirFormDialog(
            title = "Tambah Karyawan",
            confirmLabel = "Simpan",
            initialKurir = null,
            existingKurirList = displayKurirList,
            onDismiss = { showAddDialog = false },
            onSubmit = { nama, komisi, emailGoogle, aksesAktif ->
                viewModel.addKurir(
                    nama = nama,
                    komisi = komisi,
                    email = emailGoogle,
                    password = "",
                    aksesAktif = aksesAktif
                )
                showAddDialog = false
            }
        )
    }

    editingKurir?.let { target ->
        KurirFormDialog(
            title = "Edit Karyawan",
            confirmLabel = "Perbarui",
            initialKurir = target,
            existingKurirList = displayKurirList,
            onDismiss = { editingKurir = null },
            onSubmit = { nama, komisi, emailGoogle, aksesAktif ->
                viewModel.updateKurir(
                    target.copy(
                        nama = nama,
                        komisi_per_galon = komisi,
                        email = emailGoogle,
                        password = "",
                        akses_aktif = if (emailGoogle.isBlank()) true else aksesAktif,
                        skema_komisi = "TETAP",
                        aturan_komisi = ""
                    )
                )
                editingKurir = null
            }
        )
    }

    kurirToDelete?.let { kurir ->
        AlertDialog(
            onDismissRequest = { kurirToDelete = null },
            title = {
                Text(
                    text = "Hapus Karyawan?",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Text(
                    text = "Hapus \"${kurir.nama}\" dari daftar? Riwayat transaksi lama tetap aman.",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteKurir(kurir)
                        kurirToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { kurirToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun KurirFormDialog(
    title: String,
    confirmLabel: String,
    initialKurir: Kurir?,
    existingKurirList: List<Kurir>,
    onDismiss: () -> Unit,
    onSubmit: (
        nama: String,
        komisi: Double,
        emailGoogle: String,
        aksesAktif: Boolean
    ) -> Unit
) {
    var nama by remember(initialKurir) { mutableStateOf(initialKurir?.nama ?: "") }
    var komisiStr by remember(initialKurir) {
        mutableStateOf(
            if (initialKurir != null) initialKurir.komisi_per_galon.toLong().toString() else "1000"
        )
    }
    var emailInput by remember(initialKurir) {
        val raw = initialKurir?.email?.trim().orEmpty()
        val display = if (raw.isNotBlank() && !raw.contains("@")) "$raw@gmail.com" else raw
        mutableStateOf(display)
    }
    var aksesAktif by remember(initialKurir) { mutableStateOf(initialKurir?.akses_aktif ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val komisiFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val validateAndSubmit = {
        val trimmedNama = nama.trim()
        val komisi = komisiStr.toDoubleOrNull()
        val rawEmail = emailInput.trim().removePrefix("@").lowercase()
        val cleanEmail = when {
            rawEmail.isBlank() -> ""
            !rawEmail.contains("@") -> "$rawEmail@gmail.com"
            else -> rawEmail
        }
        val targetId = initialKurir?.id
        val isDuplicate = cleanEmail.isNotBlank() && existingKurirList.any { existing ->
            if (existing.id == targetId) return@any false
            val exRaw = existing.email.trim().lowercase()
            val exNormalized = if (exRaw.isNotBlank() && !exRaw.contains("@")) "$exRaw@gmail.com" else exRaw
            exNormalized == cleanEmail
        }
        when {
            trimmedNama.isBlank() -> errorMessage = "Nama karyawan wajib diisi"
            komisi == null || komisi < 0.0 -> errorMessage = "Nominal komisi tidak valid"
            cleanEmail.isNotBlank() && (cleanEmail.length < 5 || !cleanEmail.contains("@") || cleanEmail.endsWith("@")) ->
                errorMessage = "Format email Google tidak valid"
            isDuplicate -> errorMessage = "Email '$cleanEmail' sudah dipakai karyawan lain"
            else -> onSubmit(trimmedNama, komisi, cleanEmail, aksesAktif)
        }
    }

    SantoyaFormBottomSheet(
        title = title,
        subtitle = "Tarif komisi & akses Google karyawan",
        headerIcon = Icons.Default.DeliveryDining,
        headerAccent = MaterialTheme.colorScheme.secondary,
        confirmLabel = confirmLabel,
        confirmTestTag = "btn_simpan_kurir",
        onDismiss = onDismiss,
        onConfirm = { validateAndSubmit() }
    ) {
        OutlinedTextField(
            value = nama,
            onValueChange = {
                nama = it
                errorMessage = null
            },
            label = { Text("Nama Karyawan *") },
            placeholder = { Text("Contoh: Fitri, Budi") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { komisiFocusRequester.requestFocus() }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus()
                .testTag("input_kurir_nama"),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = komisiStr,
            onValueChange = {
                komisiStr = it.filter { char -> char.isDigit() }
                errorMessage = null
            },
            label = { Text("Komisi per Galon (Rp) *") },
            supportingText = {
                Text("Dihitung otomatis per galon yang diantar.")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { emailFocusRequester.requestFocus() }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(komisiFocusRequester)
                .bringIntoViewOnFocus()
                .testTag("input_kurir_komisi"),
            shape = RoundedCornerShape(12.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        Text(
            text = "Akun Google Karyawan (Opsional)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = emailInput,
            onValueChange = {
                emailInput = it.replace(" ", "").lowercase()
                errorMessage = null
            },
            label = { Text("Email Google Karyawan") },
            placeholder = { Text("contoh: fitri@gmail.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            supportingText = {
                Text("Masukkan email Google kurir supaya datanya tersambung ke HP-nya.")
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester)
                .bringIntoViewOnFocus()
                .testTag(if (initialKurir == null) "input_kurir_email" else "input_edit_kurir_email"),
            shape = RoundedCornerShape(12.dp)
        )

        if (emailInput.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Izinkan Akses Depot",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Matikan jika kurir sedang libur atau berhenti.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = aksesAktif,
                    onCheckedChange = { aksesAktif = it },
                    modifier = Modifier.testTag(
                        if (initialKurir == null) "switch_kurir_akses" else "switch_edit_kurir_akses"
                    )
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
