package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.KasbonLog
import com.example.data.entity.Kurir
import com.example.data.entity.Pelanggan
import com.example.data.entity.TransaksiDetail
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
import com.example.ui.theme.santoyaColors
import com.example.util.ContactLookupHelper
import com.example.util.ContactLookupHelper.findActivity
import com.example.util.CrmFollowUpHelper
import com.example.util.CrmOrderPrediction
import com.example.util.CrmOrderStatus
import com.example.util.CustomerSpendStats
import com.example.util.DeviceContactEntry
import com.example.util.FormatUtils
import com.example.util.KaryawanScopeHelper
import com.example.util.PelangganAuditHelper
import com.example.viewmodel.PelangganUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Peluncur pemilih nomor telepon dari Kontak perangkat menggunakan pemilih kontak sistem Android.
 * Menggunakan prinsip zero-permission tanpa memerlukan izin `READ_CONTACTS`.
 */
@Composable
fun rememberContactPhonePicker(
    onPhonePicked: (normalizedPhone: String, contactName: String?) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val noNumberMessage = stringResource(R.string.pelanggan_contact_no_number_error)
    val unavailableMessage = stringResource(R.string.pelanggan_contact_picker_unavailable)

    val systemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: run {
            onError(noNumberMessage)
            return@rememberLauncherForActivityResult
        }
        val resolved = ContactLookupHelper.resolvePickedContactUri(
            context = context,
            uri = uri
        )
        if (resolved == null) {
            onError(noNumberMessage)
        } else {
            onPhonePicked(resolved.first, resolved.second)
        }
    }

    return remember(systemPickerLauncher, unavailableMessage) {
        {
            val mimeIntent = Intent(Intent.ACTION_PICK).apply {
                type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
            }
            try {
                systemPickerLauncher.launch(mimeIntent)
            } catch (_: ActivityNotFoundException) {
                try {
                    val uriIntent = Intent(
                        Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                    )
                    systemPickerLauncher.launch(uriIntent)
                } catch (_: ActivityNotFoundException) {
                    onError(unavailableMessage)
                }
            }
        }
    }
}

/**
 * Lembar bawah pemilih kontak lengkap dalam aplikasi agar seluruh kontak HP, akun Google,
 * dan WhatsApp dapat dicari dan dipilih dalam satu ketukan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactPickerBottomSheet(
    onDismiss: () -> Unit,
    onSelectContact: (DeviceContactEntry) -> Unit,
    onOpenSystemPicker: () -> Unit
) {
    val context = LocalContext.current
    val readErrorMessage = stringResource(R.string.pelanggan_contact_picker_read_error)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var allContacts by remember { mutableStateOf<List<DeviceContactEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val loaded = withContext(Dispatchers.IO) {
                ContactLookupHelper.loadDevicePhoneContacts(context)
            }
            allContacts = loaded
        } catch (_: SecurityException) {
            errorMessage = readErrorMessage
        } finally {
            isLoading = false
        }
    }

    val filteredContacts = remember(allContacts, searchQuery) {
        ContactLookupHelper.filterContacts(allContacts, searchQuery)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    HydroGradientBadge(
                        gradientColors = listOf(MaterialTheme.colorScheme.primary, AccentCyan),
                        size = 38.dp,
                        cornerRadius = 12.dp,
                        elevation = 2.dp
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContactPhone,
                            contentDescription = null,
                            tint = MaterialTheme.santoyaColors.onHero,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.pelanggan_contact_picker_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                R.string.pelanggan_contact_picker_subtitle,
                                allContacts.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(SantoyaSize.touchTarget)
                        .testTag("btn_close_contact_picker")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_close)
                    )
                }
            }

            SantoyaSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(R.string.pelanggan_contact_picker_search_hint),
                testTag = "input_search_contact_picker",
                resultCount = if (searchQuery.isNotBlank()) filteredContacts.size else null
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 380.dp)
            ) {
                when {
                    isLoading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(30.dp))
                            Text(
                                text = stringResource(R.string.pelanggan_contact_picker_loading),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = errorMessage ?: readErrorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    filteredContacts.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.pelanggan_contact_picker_empty_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    stringResource(R.string.pelanggan_contact_picker_empty_search, searchQuery.trim())
                                } else {
                                    stringResource(R.string.pelanggan_contact_picker_empty_subtitle)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(
                                count = filteredContacts.size,
                                key = { index -> filteredContacts[index].key }
                            ) { index ->
                                val entry = filteredContacts[index]
                                val initial = entry.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                                Surface(
                                    shape = RoundedCornerShape(SantoyaRadius.md),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .clip(RoundedCornerShape(SantoyaRadius.md))
                                        .clickable { onSelectContact(entry) }
                                        .testTag("contact_picker_item_$index")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = initial,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            HighlightedSearchText(
                                                text = entry.name,
                                                query = searchQuery,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            HighlightedSearchText(
                                                text = entry.phone,
                                                query = searchQuery,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFeatureSettings = "tnum"
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        entry.label?.let { labelText ->
                                            Surface(
                                                shape = RoundedCornerShape(SantoyaRadius.xs),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                                )
                                            ) {
                                                Text(
                                                    text = labelText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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

            OutlinedButton(
                onClick = onOpenSystemPicker,
                shape = RoundedCornerShape(SantoyaRadius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SantoyaSize.touchTarget)
                    .testTag("btn_contact_picker_system_fallback")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContactPhone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.pelanggan_contact_picker_system_btn),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Kolom pencarian ringkas untuk seluruh tab pada layar Pelanggan.
 */
@Composable
fun CompactPelangganSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    resultCount: Int? = null,
    modifier: Modifier = Modifier
) {
    SantoyaSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = placeholder,
        testTag = testTag,
        resultCount = resultCount,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
    )
}

/**
 * Item konfigurasi satu pil pengurutan untuk [PelangganSortRow].
 */
data class PelangganSortOptionUi(
    val label: String,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit
)

/**
 * Wadah seragam untuk bar filter dan pengurutan di seluruh tab Pelanggan
 * agar margin, sudut, dan kontras permukaannya konsisten.
 */
@Composable
fun PelangganFilterSortBarContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(SantoyaRadius.md),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content
        )
    }
}

/**
 * Baris pil pengurutan seragam dengan ikon sortir, label "Urut", dan pil berlebar proporsional.
 */
@Composable
fun PelangganSortRow(
    options: List<PelangganSortOptionUi>,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = stringResource(R.string.pelanggan_sort_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
        options.forEach { option ->
            SantoyaFilterPill(
                label = option.label,
                selected = option.selected,
                onClick = option.onClick,
                centerContent = true,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag(option.testTag)
            )
        }
    }
}

/**
 * Kartu pelanggan yang tenang dan mudah dipindai pemilik depot.
 * Ketuk kartu untuk melihat Detail Pelanggan (riwayat transaksi dan log pelunasan kasbon).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PelangganItemCard(
    pelangganUiModel: PelangganUiModel,
    kurirList: List<Kurir> = emptyList(),
    searchQuery: String = "",
    isSoftDeleted: Boolean = false,
    deletedBy: String? = null,
    onClick: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)? = null,
    onBayarKasbon: () -> Unit,
    onCall: () -> Unit,
    onAddContactPhone: (() -> Unit)? = null,
    onPrintKasbon: (() -> Unit)? = null,
    onShareKasbon: (() -> Unit)? = null,
    isContacted: Boolean = false,
    onMarkContacted: ((Int) -> Unit)? = null
) {
    val pelanggan = pelangganUiModel.pelanggan
    val hasKasbon = pelanggan.sisa_kasbon > 0
    val cleanAlamat = remember(pelanggan.alamat) {
        KaryawanScopeHelper.cleanPelangganAlamat(pelanggan.alamat)
    }

    val santoyaColors = MaterialTheme.santoyaColors
    val cardAccent = when {
        isSoftDeleted -> MaterialTheme.colorScheme.error
        hasKasbon -> santoyaColors.warning
        else -> MaterialTheme.colorScheme.primary
    }

    HydroBentoCard(
        accentColor = cardAccent,
        cornerRadius = 16.dp,
        tintAlpha = if (isSoftDeleted) 0.12f else 0.05f,
        showWaterRipple = !isSoftDeleted,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("pelanggan_card_${pelanggan.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initials = remember(pelanggan.nama) {
                        val parts = pelanggan.nama.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                        when {
                            parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                            parts.size == 1 -> parts[0].take(2).uppercase()
                            else -> "P"
                        }
                    }
                    val hash = remember(pelanggan.nama) { kotlin.math.abs(pelanggan.nama.hashCode()) }
                    val avatarGradients = listOf(
                        listOf(MaterialTheme.colorScheme.primary, AccentCyan),
                        listOf(santoyaColors.success, AccentEmerald),
                        listOf(santoyaColors.warning, AccentAmber),
                        listOf(santoyaColors.info, MaterialTheme.colorScheme.primary),
                        listOf(MaterialTheme.colorScheme.secondary, AccentCyan)
                    )
                    val selectedGradient = if (isSoftDeleted) {
                        listOf(MaterialTheme.colorScheme.error, AccentRose)
                    } else {
                        avatarGradients[hash % avatarGradients.size]
                    }

                    HydroGradientBadge(
                        gradientColors = selectedGradient,
                        size = 36.dp,
                        cornerRadius = 11.dp,
                        elevation = 2.dp
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = santoyaColors.onHero
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        HighlightedSearchText(
                            text = pelanggan.nama,
                            query = searchQuery,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (pelanggan.no_hp.isNotBlank()) {
                            HighlightedSearchText(
                                text = pelanggan.no_hp,
                                query = searchQuery,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Tanpa No. HP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!isSoftDeleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (pelanggan.no_hp.isNotBlank()) {
                            IconButton(
                                onClick = onCall,
                                modifier = Modifier.size(SantoyaSize.touchTarget)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Call,
                                    contentDescription = stringResource(R.string.pelanggan_detail_btn_call),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (onAddContactPhone != null) {
                            IconButton(
                                onClick = onAddContactPhone,
                                modifier = Modifier
                                    .size(SantoyaSize.touchTarget)
                                    .testTag("btn_add_contact_card_${pelanggan.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContactPhone,
                                    contentDescription = stringResource(R.string.pelanggan_add_contact_cd),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(SantoyaSize.touchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.pelanggan_detail_btn_edit),
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(SantoyaSize.touchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (cleanAlamat.isNotBlank()) {
                HighlightedSearchText(
                    text = cleanAlamat,
                    query = searchQuery,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (isSoftDeleted) {
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.xs),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Dihapus oleh ${deletedBy ?: "Karyawan"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    val context = LocalContext.current
                    CrmOrderPredictionPill(
                        prediction = pelangganUiModel.crmPrediction,
                        pelanggan = pelanggan,
                        isContacted = isContacted,
                        onFollowUpClick = {
                            val followUpCtx = CrmFollowUpHelper.resolveFollowUpContext(pelangganUiModel.crmPrediction)
                            val msg = CrmFollowUpHelper.buildContextualWhatsAppMessage(
                                customerName = pelanggan.nama,
                                prediction = pelangganUiModel.crmPrediction,
                                followUpContext = followUpCtx
                            )
                            CrmFollowUpHelper.launchWhatsAppFollowUp(
                                context = context,
                                pelanggan = pelanggan,
                                message = msg,
                                onMarkContacted = { onMarkContacted?.invoke(pelanggan.id) }
                            )
                        }
                    )
                    if (isContacted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = santoyaColors.successContainer.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, santoyaColors.success.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = santoyaColors.onSuccessContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.crm_contacted_pill_short),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = santoyaColors.onSuccessContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (pelanggan.harga_khusus != null && pelanggan.harga_khusus > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "Khusus: ${FormatUtils.formatRupiah(pelanggan.harga_khusus)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                val kurirName = if (pelanggan.id_kurir_langganan != null) {
                    kurirList.find { it.id == pelanggan.id_kurir_langganan }?.nama?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
                if (kurirName != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "Kurir: $kurirName",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (isSoftDeleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRestore != null) {
                        OutlinedButton(
                            onClick = onRestore,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = SantoyaSize.touchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = "Pulihkan",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pulihkan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SantoyaSize.touchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = "Hapus Permanen",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hapus Permanen",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (hasKasbon) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.kasbon_remaining_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            SantoyaAdaptiveCurrencyText(
                                amount = pelanggan.sisa_kasbon,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = santoyaColors.warning,
                                minFontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (onPrintKasbon != null) {
                            IconButton(
                                onClick = onPrintKasbon,
                                modifier = Modifier.size(SantoyaSize.touchTarget)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Print,
                                    contentDescription = stringResource(R.string.pelanggan_kasbon_print_cd),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (onShareKasbon != null) {
                            IconButton(
                                onClick = onShareKasbon,
                                modifier = Modifier.size(SantoyaSize.touchTarget)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = stringResource(R.string.pelanggan_kasbon_share_cd),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Button(
                            onClick = onBayarKasbon,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .wrapContentWidth()
                                .heightIn(min = SantoyaSize.touchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.pelanggan_kasbon_pay_btn),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
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
                            text = "Status Tagihan",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.pelanggan_kasbon_status_clean),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = santoyaColors.success,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Badge status prediksi jadwal isi ulang galon dengan pintasan tindak lanjut WhatsApp.
 */
@Composable
fun CrmOrderPredictionPill(
    prediction: CrmOrderPrediction,
    pelanggan: Pelanggan,
    isContacted: Boolean = false,
    onFollowUpClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val hasValidPhone = pelanggan.no_hp.isNotBlank()
    val isActionable = hasValidPhone &&
        (prediction.status == CrmOrderStatus.DUE_SOON || prediction.status == CrmOrderStatus.OVERDUE)

    val santoyaColors = MaterialTheme.santoyaColors
    val containerColor = when (prediction.status) {
        CrmOrderStatus.ACTIVE -> santoyaColors.successContainer.copy(alpha = 0.7f)
        CrmOrderStatus.DUE_SOON -> santoyaColors.warningContainer.copy(alpha = 0.7f)
        CrmOrderStatus.OVERDUE -> santoyaColors.dangerContainer
        CrmOrderStatus.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val contentColor = when (prediction.status) {
        CrmOrderStatus.ACTIVE -> santoyaColors.onSuccessContainer
        CrmOrderStatus.DUE_SOON -> santoyaColors.onWarningContainer
        CrmOrderStatus.OVERDUE -> santoyaColors.onDangerContainer
        CrmOrderStatus.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = when (prediction.status) {
        CrmOrderStatus.ACTIVE -> santoyaColors.success.copy(alpha = 0.45f)
        CrmOrderStatus.DUE_SOON -> santoyaColors.warning.copy(alpha = 0.55f)
        CrmOrderStatus.OVERDUE -> santoyaColors.danger.copy(alpha = 0.55f)
        CrmOrderStatus.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.outlineVariant
    }

    val icon = when (prediction.status) {
        CrmOrderStatus.DUE_SOON, CrmOrderStatus.OVERDUE -> Icons.Outlined.NotificationsActive
        CrmOrderStatus.ACTIVE, CrmOrderStatus.INSUFFICIENT_DATA -> Icons.Outlined.Schedule
    }

    val badgeShape = RoundedCornerShape(6.dp)
    val clickModifier = if (isActionable) {
        Modifier.clickable {
            if (onFollowUpClick != null) {
                onFollowUpClick()
            } else {
                val followUpCtx = CrmFollowUpHelper.resolveFollowUpContext(prediction)
                val followUpMessage = CrmFollowUpHelper.buildContextualWhatsAppMessage(
                    customerName = pelanggan.nama,
                    prediction = prediction,
                    followUpContext = followUpCtx
                )
                CrmFollowUpHelper.launchWhatsAppFollowUp(
                    context = context,
                    pelanggan = pelanggan,
                    message = followUpMessage
                )
            }
        }
    } else {
        Modifier
    }

    Surface(
        shape = badgeShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .clip(badgeShape)
            .then(clickModifier)
            .testTag("crm_badge_${pelanggan.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = prediction.badgeText,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isActionable && !isContacted) {
                    stringResource(R.string.crm_pill_contact_suffix, prediction.badgeText)
                } else {
                    prediction.badgeText
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Bottom sheet Detail Pelanggan 1-ketuk yang menyajikan ringkasan profil,
 * 2 tab riwayat (Transaksi aktif & Pelunasan Kasbon), serta pintasan Pesan Galon dan Bayar Kasbon.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PelangganDetailBottomSheet(
    pelangganUiModel: PelangganUiModel,
    customerTransactions: List<TransaksiDetail>,
    kasbonLogs: List<KasbonLog>,
    stats: CustomerSpendStats?,
    kurirList: List<Kurir>,
    onDismiss: () -> Unit,
    onPesanGalon: () -> Unit,
    onBayarKasbon: () -> Unit,
    onEdit: () -> Unit,
    onPrintTagihan: () -> Unit,
    onShareTagihan: () -> Unit,
    onAddContactPhone: (() -> Unit)? = null,
    lastContactedMillis: Long? = null,
    onMarkContacted: (Int) -> Unit = {},
    onClearContacted: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pelanggan = pelangganUiModel.pelanggan
    val cleanAlamat = remember(pelanggan.alamat) { KaryawanScopeHelper.cleanPelangganAlamat(pelanggan.alamat) }
    val santoyaColors = MaterialTheme.santoyaColors
    var detailTabIndex by remember(pelanggan.id) { mutableIntStateOf(0) }

    val sortedTxs = remember(customerTransactions) {
        customerTransactions
            .filter { !KaryawanScopeHelper.isDeletedTransaksi(it.transaksi) }
            .sortedByDescending { it.transaksi.tanggal }
    }
    val sortedLogs = remember(kasbonLogs) {
        kasbonLogs.sortedByDescending { it.tanggal }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = SantoyaRadius.xl, topEnd = SantoyaRadius.xl)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .testTag("pelanggan_detail_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Profil Pelanggan
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val initials = remember(pelanggan.nama) {
                    val parts = pelanggan.nama.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                    when {
                        parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                        parts.size == 1 -> parts[0].take(2).uppercase()
                        else -> "P"
                    }
                }
                HydroGradientBadge(
                    gradientColors = listOf(MaterialTheme.colorScheme.primary, AccentCyan),
                    size = 46.dp,
                    cornerRadius = 14.dp,
                    elevation = 3.dp
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = santoyaColors.onHero
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pelanggan.nama,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (cleanAlamat.isNotBlank()) {
                        Text(
                            text = cleanAlamat,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val lastOrderText = stats?.lastOrderMillis?.let {
                        stringResource(R.string.pelanggan_detail_last_order, FormatUtils.formatDateUi(it))
                    } ?: stringResource(R.string.pelanggan_detail_never_ordered)
                    val cycleSuffix = pelangganUiModel.estimatedIntervalDays?.let {
                        " • " + stringResource(R.string.pelanggan_detail_cycle_days, it)
                    }.orEmpty()
                    Text(
                        text = "$lastOrderText$cycleSuffix",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(SantoyaSize.touchTarget)
                        .testTag("btn_detail_edit_pelanggan")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.pelanggan_detail_btn_edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val isContacted = CrmFollowUpHelper.isContactedInCurrentCycle(
                lastContactedMillis = lastContactedMillis,
                lastOrderMillis = pelangganUiModel.crmPrediction.lastOrderMillis
            )

            // Badge informasi tambahan (Harga khusus, Kurir, Insight Siklus, & Status Dihubungi)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CrmOrderPredictionPill(
                    prediction = pelangganUiModel.crmPrediction,
                    pelanggan = pelanggan,
                    isContacted = isContacted,
                    onFollowUpClick = {
                        val followUpCtx = CrmFollowUpHelper.resolveFollowUpContext(pelangganUiModel.crmPrediction)
                        val msg = CrmFollowUpHelper.buildContextualWhatsAppMessage(
                            customerName = pelanggan.nama,
                            prediction = pelangganUiModel.crmPrediction,
                            followUpContext = followUpCtx
                        )
                        CrmFollowUpHelper.launchWhatsAppFollowUp(
                            context = context,
                            pelanggan = pelanggan,
                            message = msg,
                            onMarkContacted = { onMarkContacted(pelanggan.id) }
                        )
                    }
                )
                pelangganUiModel.crmPrediction.avgGallonsPerOrder?.let { avgGalon ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = stringResource(R.string.crm_detail_avg_galon, avgGalon),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (isContacted && lastContactedMillis != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = santoyaColors.successContainer.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, santoyaColors.success.copy(alpha = 0.4f)),
                        onClick = { onClearContacted(pelanggan.id) },
                        modifier = Modifier.testTag("btn_detail_clear_contacted")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = santoyaColors.onSuccessContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = stringResource(R.string.crm_contacted_badge, FormatUtils.formatTimeOnly(lastContactedMillis)),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = santoyaColors.onSuccessContainer
                            )
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.crm_reset_contacted_cd),
                                tint = santoyaColors.onSuccessContainer,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                } else if (pelangganUiModel.crmPrediction.status == CrmOrderStatus.DUE_SOON ||
                    pelangganUiModel.crmPrediction.status == CrmOrderStatus.OVERDUE
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        onClick = { onMarkContacted(pelanggan.id) },
                        modifier = Modifier.testTag("btn_detail_mark_contacted")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = stringResource(R.string.crm_mark_contacted_btn),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (pelanggan.harga_khusus != null && pelanggan.harga_khusus > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Khusus: ${FormatUtils.formatRupiah(pelanggan.harga_khusus)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                val kurirName = pelanggan.id_kurir_langganan?.let { kid ->
                    kurirList.find { it.id == kid }?.nama?.takeIf { it.isNotBlank() }
                }
                if (kurirName != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Kurir: $kurirName",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (pelanggan.no_hp.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val followUpCtx = CrmFollowUpHelper.resolveFollowUpContext(pelangganUiModel.crmPrediction)
                            val msg = CrmFollowUpHelper.buildContextualWhatsAppMessage(
                                customerName = pelanggan.nama,
                                prediction = pelangganUiModel.crmPrediction,
                                followUpContext = followUpCtx
                            )
                            CrmFollowUpHelper.launchWhatsAppFollowUp(
                                context = context,
                                pelanggan = pelanggan,
                                message = msg,
                                onMarkContacted = { onMarkContacted(pelanggan.id) }
                            )
                        },
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SantoyaSize.touchTarget)
                            .testTag("btn_detail_whatsapp")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.pelanggan_detail_btn_whatsapp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pelanggan.no_hp}")))
                        },
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SantoyaSize.touchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Call,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pelanggan.no_hp,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (!KaryawanScopeHelper.isSoftDeletedPelanggan(pelanggan) && onAddContactPhone != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddContactPhone,
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.35f)
                            .heightIn(min = SantoyaSize.touchTarget)
                            .testTag("btn_detail_add_contact")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContactPhone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.pelanggan_detail_btn_add_contact),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SantoyaSize.touchTarget)
                            .testTag("btn_detail_manual_phone")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.pelanggan_detail_btn_manual_phone),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 2 Kartu Ringkas Berdampingan: Total Belanja & Sisa Kasbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.pelanggan_detail_total_belanja),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SantoyaAdaptiveCurrencyText(
                            amount = stats?.totalOmzet ?: 0.0,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            minFontSize = 12.sp
                        )
                        Text(
                            text = stringResource(
                                R.string.pelanggan_detail_galon_order,
                                stats?.totalGalon ?: 0,
                                stats?.totalOrder ?: 0
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val hasDebt = pelanggan.sisa_kasbon > 0.0
                Surface(
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    color = if (hasDebt) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                    } else {
                        santoyaColors.successContainer.copy(alpha = 0.55f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (hasDebt) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                        } else {
                            santoyaColors.success.copy(alpha = 0.25f)
                        }
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.kasbon_remaining_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SantoyaAdaptiveCurrencyText(
                            amount = pelanggan.sisa_kasbon.coerceAtLeast(0.0),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasDebt) MaterialTheme.colorScheme.error else santoyaColors.success,
                            minFontSize = 12.sp
                        )
                        Text(
                            text = if (hasDebt) {
                                "${sortedTxs.count { it.transaksi.status.equals("Kasbon", ignoreCase = true) }} nota belum lunas"
                            } else {
                                stringResource(R.string.pelanggan_kasbon_lunas_badge)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasDebt) MaterialTheme.colorScheme.error else santoyaColors.onSuccessContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Tab Riwayat: Transaksi vs Pelunasan Kasbon
            val detailTabs = listOf(
                SantoyaTabPillItem(
                    label = stringResource(R.string.pelanggan_detail_tab_transaksi, sortedTxs.size),
                    testTag = "detail_tab_transaksi"
                ),
                SantoyaTabPillItem(
                    label = stringResource(R.string.pelanggan_detail_tab_pelunasan, sortedLogs.size),
                    testTag = "detail_tab_pelunasan"
                )
            )
            SantoyaAdaptiveTabPillRow(
                items = detailTabs,
                selectedIndex = detailTabIndex,
                onSelect = { detailTabIndex = it }
            )

            // Daftar Riwayat
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 260.dp)
            ) {
                if (detailTabIndex == 0) {
                    if (sortedTxs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.pelanggan_detail_empty_transaksi),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sortedTxs, key = { "tx_${it.transaksi.id}_${it.transaksi.syncId}" }) { detail ->
                                val tx = detail.transaksi
                                val isKasbon = tx.status.equals("Kasbon", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(SantoyaRadius.md),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${detail.produk?.nama_produk ?: "Galon RO"} (${tx.jumlah} gln)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${FormatUtils.formatDateTimeUi(tx.tanggal)} • ${FormatUtils.formatMetodeBayarLabel(tx.metode_bayar)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isKasbon && tx.uangMuka > 0L) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.pelanggan_kasbon_uang_muka_summary,
                                                        FormatUtils.formatRupiah(tx.uangMuka.toDouble()),
                                                        FormatUtils.formatRupiah(tx.remainingKasbonAmount())
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = santoyaColors.warning,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = FormatUtils.formatRupiah(tx.total_harga),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = tx.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isKasbon) MaterialTheme.colorScheme.error else santoyaColors.success
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (sortedLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.pelanggan_detail_empty_pelunasan),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sortedLogs, key = { "log_${it.id}_${it.syncId}" }) { log ->
                                Surface(
                                    shape = RoundedCornerShape(SantoyaRadius.md),
                                    color = santoyaColors.successContainer.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, santoyaColors.success.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                                contentDescription = null,
                                                tint = santoyaColors.success,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${FormatUtils.formatDateTimeUi(log.tanggal)} • ${FormatUtils.formatMetodeBayarLabel(log.metode_bayar)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Sisa: ${FormatUtils.formatRupiah(log.sisa_kasbon_sebelum)} → ${FormatUtils.formatRupiah(log.sisa_kasbon_sesudah)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (log.catatan.isNotBlank()) {
                                                    Text(
                                                        text = log.catatan,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "+${FormatUtils.formatRupiah(log.jumlah_bayar)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Bold,
                                            color = santoyaColors.success
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Tombol Aksi Cepat Bawah
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPesanGalon,
                    shape = RoundedCornerShape(SantoyaRadius.md),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = SantoyaSize.touchTarget)
                        .testTag("btn_detail_pesan_galon")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.pelanggan_detail_btn_order),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                if (pelanggan.sisa_kasbon > 0.0) {
                    Button(
                        onClick = onBayarKasbon,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SantoyaSize.touchTarget)
                            .testTag("btn_detail_bayar_kasbon")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.pelanggan_kasbon_pay_btn),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = onPrintTagihan,
                        modifier = Modifier.size(SantoyaSize.touchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Print,
                            contentDescription = stringResource(R.string.pelanggan_kasbon_print_cd),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onShareTagihan,
                        modifier = Modifier.size(SantoyaSize.touchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.pelanggan_kasbon_share_cd),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * State kosong seragam untuk tab dan hasil pencarian pada layar Pelanggan.
 */
@Composable
fun PelangganEmptyListState(
    emptyType: SantoyaEmptyType = SantoyaEmptyType.PELANGGAN,
    icon: ImageVector? = null,
    title: String,
    subtitle: String,
    onResetSearch: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        SantoyaEmptyState(
            emptyType = emptyType,
            title = title,
            subtitle = subtitle,
            accentColor = if (emptyType == SantoyaEmptyType.KASBON) {
                MaterialTheme.santoyaColors.success
            } else {
                MaterialTheme.colorScheme.primary
            },
            secondaryActionLabel = if (onResetSearch != null) "Reset Pencarian" else null,
            secondaryActionIcon = if (onResetSearch != null) Icons.Outlined.Clear else null,
            onSecondaryAction = onResetSearch
        )
    }
}
