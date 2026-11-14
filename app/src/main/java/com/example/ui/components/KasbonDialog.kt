package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Pelanggan
import com.example.util.FormatUtils

@Composable
fun KasbonPelunasanDialog(
    pelanggan: Pelanggan,
    onDismiss: () -> Unit,
    onConfirmBayar: (Double) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var nominalInput by remember { mutableStateOf(pelanggan.sisa_kasbon.toLong().toString()) }
    val nominalDouble = nominalInput.toDoubleOrNull() ?: 0.0

    SantoyaFormBottomSheet(
        title = "Pelunasan Kasbon",
        subtitle = pelanggan.nama,
        headerIcon = Icons.Default.Payments,
        onDismiss = onDismiss,
        confirmLabel = "Simpan Pembayaran",
        confirmEnabled = nominalDouble > 0,
        confirmTestTag = "btn_confirm_bayar_kasbon",
        onConfirm = {
            if (nominalDouble > 0) {
                focusManager.clearFocus()
                keyboardController?.hide()
                onConfirmBayar(nominalDouble)
            }
        }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = pelanggan.nama,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (pelanggan.alamat.isNotBlank()) {
                    Text(
                        text = pelanggan.alamat,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sisa Kasbon",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.formatRupiah(pelanggan.sisa_kasbon),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Text(
            text = "Nominal Pembayaran",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = nominalDouble == pelanggan.sisa_kasbon,
                onClick = { nominalInput = pelanggan.sisa_kasbon.toLong().toString() },
                label = { Text("Bayar Penuh", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = nominalDouble == (pelanggan.sisa_kasbon / 2),
                onClick = { nominalInput = (pelanggan.sisa_kasbon / 2).toLong().toString() },
                label = { Text("50%", fontSize = 11.sp) },
                modifier = Modifier.weight(0.6f)
            )
            FilterChip(
                selected = nominalDouble == 10000.0,
                onClick = { nominalInput = "10000" },
                label = { Text("Rp 10.000", fontSize = 11.sp) },
                modifier = Modifier.weight(0.6f)
            )
        }

        OutlinedTextField(
            value = nominalInput,
            onValueChange = { nominalInput = it.filter { char -> char.isDigit() } },
            label = { Text("Nominal Bayar (Rp)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus()
                .testTag("input_nominal_kasbon")
        )

        if (nominalDouble > 0) {
            val sisaSetelahBayar = (pelanggan.sisa_kasbon - nominalDouble).coerceAtLeast(0.0)
            Text(
                text = "Sisa kasbon setelah bayar: ${FormatUtils.formatRupiah(sisaSetelahBayar)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (sisaSetelahBayar == 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
