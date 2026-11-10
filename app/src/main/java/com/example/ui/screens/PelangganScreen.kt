package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import com.example.data.entity.Kurir
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Pelanggan
import com.example.data.entity.TransaksiDetail
import com.example.util.FormatUtils
import com.example.util.SantoyaReceiptHelper
import com.example.viewmodel.ActiveReceiptDialog
import com.example.viewmodel.KasbonSortOption
import com.example.viewmodel.SantoyaScreen
import com.example.viewmodel.SantoyaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PelangganScreen(
    viewModel: SantoyaViewModel,
    pelangganUiModels: List<com.example.viewmodel.PelangganUiModel> = emptyList(),
    transaksiList: List<TransaksiDetail> = emptyList(),
    onBack: () -> Unit = { viewModel.navigateTo(SantoyaScreen.DASHBOARD) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kurirList by viewModel.allKurir.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPelanggan by remember { mutableStateOf<Pelanggan?>(null) }
    var payingKasbonPelanggan by remember { mutableStateOf<Pelanggan?>(null) }
    var pelangganToDelete by remember { mutableStateOf<Pelanggan?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Semua (${pelangganUiModels.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                val kasbonCount = pelangganUiModels.count { model ->
                    val p = model.pelanggan
                    p.sisa_kasbon > 0 || transaksiList.any { t ->
                        t.transaksi.id_pelanggan == p.id && t.transaksi.status.equals("Kasbon", ignoreCase = true)
                    }
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Kasbon ($kasbonCount)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "Top Ranking",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            text = "Pasif",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> SemuaPelangganTab(
                    viewModel = viewModel,
                    pelangganUiModels = pelangganUiModels,
                    transaksiList = transaksiList,
                    kurirList = kurirList,
                    onEdit = { editingPelanggan = it },
                    onBayarKasbon = { payingKasbonPelanggan = it },
                    onDelete = { pelangganToDelete = it }
                )
                1 -> {
                    val pelangganList = pelangganUiModels.map { it.pelanggan }
                    KasbonPelangganTab(
                        viewModel = viewModel,
                        pelangganList = pelangganList,
                        transaksiList = transaksiList,
                        onBayarKasbon = { payingKasbonPelanggan = it }
                    )
                }
                2 -> {
                    val pelangganList = pelangganUiModels.map { it.pelanggan }
                    TopRankingTab(
                        viewModel = viewModel,
                        pelangganList = pelangganList,
                        transaksiList = transaksiList
                    )
                }
                3 -> {
                    val pelangganList = pelangganUiModels.map { it.pelanggan }
                    PasifPelangganTab(
                        viewModel = viewModel,
                        pelangganList = pelangganList,
                        transaksiList = transaksiList
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("tambah_pelanggan_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Pelanggan")
        }
    }

    if (showAddDialog) {
        PelangganFormDialog(
            title = "Tambah Pelanggan Baru",
            pelanggan = null,
            kurirList = kurirList,
            onDismiss = { showAddDialog = false },
            onConfirm = { nama, alamat, noHp, hargaKhusus, idKurirLangganan ->
                viewModel.addPelanggan(
                    nama = nama,
                    alamat = alamat,
                    noHp = noHp,
                    hargaKhusus = hargaKhusus,
                    idKurirLangganan = idKurirLangganan
                )
                showAddDialog = false
            }
        )
    }

    if (editingPelanggan != null) {
        val target = editingPelanggan!!
        PelangganFormDialog(
            title = "Edit Data Pelanggan",
            pelanggan = target,
            kurirList = kurirList,
            onDismiss = { editingPelanggan = null },
            onConfirm = { nama, alamat, noHp, hargaKhusus, idKurirLangganan ->
                viewModel.updatePelanggan(
                    target.copy(
                        nama = nama,
                        alamat = alamat,
                        no_hp = noHp,
                        harga_khusus = hargaKhusus,
                        id_kurir_langganan = idKurirLangganan
                    )
                )
                editingPelanggan = null
            }
        )
    }

    if (payingKasbonPelanggan != null) {
        val target = payingKasbonPelanggan!!
        PelunasanKasbonDialog(
            pelanggan = target,
            onDismiss = { payingKasbonPelanggan = null },
            onConfirmBayar = { nominal ->
                viewModel.bayarKasbon(target.id, nominal)
                val sisa = (target.sisa_kasbon - nominal).coerceAtLeast(0.0)
                val receipt = SantoyaReceiptHelper.generatePelunasanReceipt(target, nominal, sisa)
                viewModel.showReceiptDialog(
                    ActiveReceiptDialog(
                        title = "Bukti Pelunasan Kasbon",
                        receiptText = receipt,
                        customerPhone = target.no_hp,
                        data = Triple(target, nominal, sisa)
                    )
                )
                payingKasbonPelanggan = null
            }
        )
    }

    if (pelangganToDelete != null) {
        val target = pelangganToDelete!!
        AlertDialog(
            onDismissRequest = { pelangganToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Peringatan",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Konfirmasi Hapus Pelanggan",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus data pelanggan ini? Tindakan ini tidak dapat dibatalkan.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = target.nama,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (target.no_hp.isNotBlank()) {
                                Text(
                                    text = "No. HP: ${target.no_hp}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (target.alamat.isNotBlank()) {
                                Text(
                                    text = "Alamat: ${target.alamat}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (target.sisa_kasbon > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PERHATIAN: Pelanggan ini masih memiliki sisa kasbon sebesar ${FormatUtils.formatRupiah(target.sisa_kasbon)}! Pastikan sisa kasbon diselesaikan sebelum menghapus.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Catatan: Riwayat penjualan sebelumnya yang terkait dengan pelanggan ini tetap aman tercatat dalam laporan pembukuan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePelanggan(target)
                        pelangganToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ya, Hapus Pelanggan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pelangganToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SemuaPelangganTab(
    viewModel: SantoyaViewModel,
    pelangganUiModels: List<com.example.viewmodel.PelangganUiModel>,
    transaksiList: List<TransaksiDetail>,
    kurirList: List<Kurir> = emptyList(),
    onEdit: (Pelanggan) -> Unit,
    onBayarKasbon: (Pelanggan) -> Unit,
    onDelete: (Pelanggan) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val filtered = viewModel.getFilteredPelangganUiModels(searchQuery, pelangganUiModels)

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari nama, alamat, atau no HP...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Bersihkan",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("input_search_pelanggan")
        )

        if (filtered.isEmpty()) {
            EmptyListState(
                icon = Icons.Default.People,
                title = if (searchQuery.isBlank()) "Belum ada pelanggan" else "Pelanggan tidak ditemukan",
                subtitle = if (searchQuery.isBlank()) "Tekan tombol + untuk menambahkan pelanggan baru." else "Coba kata kunci pencarian yang lain."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.pelanggan.id }) { item ->
                    PelangganItemCard(
                        pelangganUiModel = item,
                        kurirList = kurirList,
                        onEdit = { onEdit(item.pelanggan) },
                        onDelete = { onDelete(item.pelanggan) },
                        onBayarKasbon = { onBayarKasbon(item.pelanggan) },
                        onCall = {
                            if (item.pelanggan.no_hp.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.pelanggan.no_hp}"))
                                context.startActivity(intent)
                            }
                        },
                        onPrintKasbon = if (item.pelanggan.sisa_kasbon > 0) {
                            {
                                val unpaid = transaksiList.filter {
                                    it.transaksi.id_pelanggan == item.pelanggan.id &&
                                        it.transaksi.status.equals("Kasbon", ignoreCase = true)
                                }.map { it.transaksi }
                                val receipt = SantoyaReceiptHelper.generateTagihanKasbon(item.pelanggan, unpaid)
                                viewModel.showReceiptDialog(
                                    ActiveReceiptDialog(
                                        title = "Tagihan Kasbon - ${item.pelanggan.nama}",
                                        receiptText = receipt,
                                        customerPhone = item.pelanggan.no_hp,
                                        data = Pair(item.pelanggan, unpaid)
                                    )
                                )
                            }
                        } else null,
                        onShareKasbon = if (item.pelanggan.sisa_kasbon > 0) {
                            {
                                val unpaid = transaksiList.filter {
                                    it.transaksi.id_pelanggan == item.pelanggan.id &&
                                        it.transaksi.status.equals("Kasbon", ignoreCase = true)
                                }.map { it.transaksi }
                                val receipt = SantoyaReceiptHelper.generateTagihanKasbon(item.pelanggan, unpaid)
                                SantoyaReceiptHelper.shareReceiptText(context, receipt, item.pelanggan.no_hp)
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun PelangganItemCard(
    pelangganUiModel: com.example.viewmodel.PelangganUiModel,
    kurirList: List<Kurir> = emptyList(),
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBayarKasbon: () -> Unit,
    onCall: () -> Unit,
    onPrintKasbon: (() -> Unit)? = null,
    onShareKasbon: (() -> Unit)? = null
) {
    val pelanggan = pelangganUiModel.pelanggan
    val hasKasbon = pelanggan.sisa_kasbon > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = pelanggan.nama.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pelanggan.nama,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (pelanggan.no_hp.isNotBlank()) pelanggan.no_hp else "Tanpa No. HP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasKasbon && onPrintKasbon != null) {
                        IconButton(onClick = onPrintKasbon, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Cetak Tagihan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (hasKasbon && onShareKasbon != null) {
                        IconButton(onClick = onShareKasbon, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Bagi WA",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (pelanggan.no_hp.isNotBlank()) {
                        IconButton(onClick = onCall, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Telepon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (pelanggan.alamat.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Alamat: ${pelanggan.alamat}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    pelangganUiModel.estimatedIntervalDays?.let { days ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Est: ${
                                when (days) {
                                    1 -> "Besok"
                                    2 -> "Lusa"
                                    else -> "$days Hari"
                                }
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            if (pelanggan.harga_khusus != null || pelanggan.id_kurir_langganan != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (pelanggan.harga_khusus != null && pelanggan.harga_khusus > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Harga Khusus: ${FormatUtils.formatRupiah(pelanggan.harga_khusus)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pelanggan.id_kurir_langganan != null) {
                        val kName = kurirList.find { it.id == pelanggan.id_kurir_langganan }?.nama ?: "Kurir #${pelanggan.id_kurir_langganan}"
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Kurir: $kName",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = if (hasKasbon) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasKasbon) "Sisa Kasbon:" else "Status Tagihan:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (hasKasbon) FormatUtils.formatRupiah(pelanggan.sisa_kasbon) else "Lunas / Rp 0",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasKasbon) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    )
                }
            }

            if (hasKasbon) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onBayarKasbon,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Form Pelunasan Kasbon", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun KasbonPelangganTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>,
    onBayarKasbon: (Pelanggan) -> Unit
) {
    val context = LocalContext.current
    var sortOption by remember { mutableStateOf(KasbonSortOption.TERBANYAK) }
    var searchQuery by remember { mutableStateOf("") }

    val sortedKasbon = viewModel.getSortedKasbonPelanggan(pelangganList, sortOption, searchQuery, transaksiList)
    val totalPiutang = sortedKasbon.sumOf { it.sisa_kasbon }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari nama atau alamat kasbon...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Urutkan:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            KasbonSortOption.values().forEach { option ->
                FilterChip(
                    selected = sortOption == option,
                    onClick = { sortOption = option },
                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }, maxLines = 1) },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        if (sortedKasbon.isEmpty()) {
            EmptyListState(
                icon = Icons.Default.HourglassBottom,
                title = if (searchQuery.isBlank()) "Tidak ada tagihan kasbon" else "Pelanggan tidak ditemukan",
                subtitle = if (searchQuery.isBlank()) "Semua kasbon telah dilunasi dengan baik." else "Coba kata kunci pencarian yang lain."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Total Piutang Kasbon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text(
                                        text = FormatUtils.formatRupiah(totalPiutang),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                text = "${sortedKasbon.size} Pelanggan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                items(sortedKasbon, key = { it.id }) { cust ->
                    val unpaid = transaksiList.filter {
                        it.transaksi.id_pelanggan == cust.id &&
                            it.transaksi.status.equals("Kasbon", ignoreCase = true)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cust.nama,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = cust.alamat.ifBlank { "Alamat belum diisi" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = FormatUtils.formatRupiah(cust.sisa_kasbon),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            if (unpaid.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Rincian ${unpaid.size} Transaksi Kasbon:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        unpaid.take(3).forEach { t ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "• ${FormatUtils.formatDateShort(t.transaksi.tanggal)} (${t.transaksi.jumlah} galon)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = FormatUtils.formatRupiah(t.transaksi.total_harga),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        if (unpaid.size > 3) {
                                            Text(
                                                text = "+ ${unpaid.size - 3} nota lainnya...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val receipt = SantoyaReceiptHelper.generateTagihanKasbon(cust, unpaid.map { it.transaksi })
                                            viewModel.showReceiptDialog(
                                                ActiveReceiptDialog(
                                                    title = "Tagihan Kasbon - ${cust.nama}",
                                                    receiptText = receipt,
                                                    customerPhone = cust.no_hp,
                                                    data = Pair(cust, unpaid.map { it.transaksi })
                                                )
                                            )
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "Cetak Tagihan",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val receipt = SantoyaReceiptHelper.generateTagihanKasbon(cust, unpaid.map { it.transaksi })
                                            SantoyaReceiptHelper.shareReceiptText(context, receipt, cust.no_hp)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Bagi WA",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onBayarKasbon(cust) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pelunasan Kasbon", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopRankingTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>
) {
    val ranking = viewModel.getTopRankingPelanggan(pelangganList, transaksiList)

    if (ranking.isEmpty()) {
        EmptyListState(
            icon = Icons.Default.EmojiEvents,
            title = "Belum Ada Ranking Penjualan",
            subtitle = "Ranking akan terhitung otomatis saat transaksi penjualan disimpan."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(ranking) { index, pair ->
                val cust = pair.first
                val galonCount = pair.second
                val rank = index + 1

                val badgeBgColor = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = badgeBgColor,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#$rank",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rank <= 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cust.nama,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = cust.alamat.ifBlank { "Pelanggan Santoya" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "$galonCount Galon",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasifPelangganTab(
    viewModel: SantoyaViewModel,
    pelangganList: List<Pelanggan>,
    transaksiList: List<TransaksiDetail>
) {
    val context = LocalContext.current
    var selectedDaysFilter by remember { mutableStateOf(7L) }
    val pasifList = viewModel.getPasifPelanggan(pelangganList, transaksiList, selectedDaysFilter)

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Rentang:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                listOf(7L to "> 7 Hari", 14L to "> 14 Hari", 30L to "> 30 Hari", 60L to "> 60 Hari").forEach { pair ->
                    FilterChip(
                        selected = selectedDaysFilter == pair.first,
                        onClick = { selectedDaysFilter = pair.first },
                        label = { Text(pair.second, maxLines = 1) },
                        modifier = Modifier.height(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }
        }

        if (pasifList.isEmpty()) {
            EmptyListState(
                icon = Icons.Default.PersonOff,
                title = "Semua Pelanggan Aktif",
                subtitle = "Tidak ada pelanggan pasif dalam rentang waktu $selectedDaysFilter hari terakhir."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pasifList) { pair ->
                    val cust = pair.first
                    val days = pair.second

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PersonOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cust.nama,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (days >= 999L) "Belum ada riwayat pesanan" else "$days hari belum memesan",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (cust.no_hp.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val text = "Halo kak ${cust.nama}, kami dari Santoya RO. Apakah persediaan galon air minum RO kakak sudah habis dan ingin kami antar hari ini?"
                                        SantoyaReceiptHelper.shareReceiptText(context, text, cust.no_hp)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hubungi", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PelangganFormDialog(
    title: String,
    pelanggan: Pelanggan?,
    kurirList: List<Kurir> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (nama: String, alamat: String, noHp: String, hargaKhusus: Double?, idKurirLangganan: Int?) -> Unit
) {
    var nama by remember { mutableStateOf(pelanggan?.nama ?: "") }
    var alamat by remember { mutableStateOf(pelanggan?.alamat ?: "") }
    var noHp by remember { mutableStateOf(pelanggan?.no_hp ?: "") }
    var hargaKhususStr by remember { mutableStateOf(pelanggan?.harga_khusus?.toLong()?.toString() ?: "") }
    var selectedKurirId by remember { mutableStateOf<Int?>(pelanggan?.id_kurir_langganan) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nama,
                    onValueChange = {
                        nama = it
                        errorMessage = null
                    },
                    label = { Text("Nama Lengkap *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text("Alamat / Desa / Blok") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = noHp,
                    onValueChange = { noHp = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("No. HP / WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "Otomatisasi Transaksi (Opsional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                OutlinedTextField(
                    value = hargaKhususStr,
                    onValueChange = { hargaKhususStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Harga Khusus Galon (Rp)") },
                    placeholder = { Text("Kosongkan jika harga umum") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "Kurir Langganan",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedKurirId == null,
                        onClick = { selectedKurirId = null },
                        label = { Text("Tanpa Kurir", maxLines = 1) },
                        modifier = Modifier.height(32.dp)
                    )
                    kurirList.forEach { kurir ->
                        FilterChip(
                            selected = selectedKurirId == kurir.id,
                            onClick = { selectedKurirId = kurir.id },
                            label = { Text(kurir.nama, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isBlank()) {
                        errorMessage = "Nama pelanggan wajib diisi!"
                        return@Button
                    }
                    val hKhusus = hargaKhususStr.toDoubleOrNull()
                    onConfirm(nama.trim(), alamat.trim(), noHp.trim(), hKhusus, selectedKurirId)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun PelunasanKasbonDialog(
    pelanggan: Pelanggan,
    onDismiss: () -> Unit,
    onConfirmBayar: (nominal: Double) -> Unit
) {
    var nominalStr by remember { mutableStateOf(pelanggan.sisa_kasbon.toLong().toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pelunasan Kasbon", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pelanggan: ${pelanggan.nama}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Total Kasbon: ${FormatUtils.formatRupiah(pelanggan.sisa_kasbon)}",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = nominalStr,
                    onValueChange = {
                        nominalStr = it.filter { c -> c.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Nominal Bayar (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { nominalStr = pelanggan.sisa_kasbon.toLong().toString() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lunas Penuh", maxLines = 1)
                    }
                    if (pelanggan.sisa_kasbon > 10000) {
                        OutlinedButton(
                            onClick = { nominalStr = (pelanggan.sisa_kasbon / 2).toLong().toString() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bayar 50%", maxLines = 1)
                        }
                    }
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nominal = nominalStr.toDoubleOrNull()
                    if (nominal == null || nominal <= 0) {
                        errorMessage = "Nominal pembayaran tidak valid!"
                        return@Button
                    }
                    if (nominal > pelanggan.sisa_kasbon) {
                        errorMessage = "Nominal melebihi total kasbon!"
                        return@Button
                    }
                    onConfirmBayar(nominal)
                }
            ) {
                Text("Konfirmasi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun EmptyListState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
