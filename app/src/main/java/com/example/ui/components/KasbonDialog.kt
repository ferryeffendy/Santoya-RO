package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.Pelanggan
import com.example.ui.theme.SantoyaRadius
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
        title = stringResource(R.string.kasbon_dialog_title),
        subtitle = pelanggan.nama,
        headerIcon = Icons.Outlined.Payments,
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.kasbon_dialog_confirm),
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
            shape = RoundedCornerShape(SantoyaRadius.md),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = pelanggan.nama,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (pelanggan.alamat.isNotBlank()) {
                    Text(
                        text = pelanggan.alamat,
                        style = MaterialTheme.typography.bodySmall,
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
                        text = stringResource(R.string.kasbon_remaining_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.formatRupiah(pelanggan.sisa_kasbon),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.kasbon_amount_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SantoyaFilterPill(
                label = stringResource(R.string.kasbon_pay_full),
                selected = nominalDouble == pelanggan.sisa_kasbon,
                onClick = { nominalInput = pelanggan.sisa_kasbon.toLong().toString() },
                centerContent = true,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 7.dp),
                modifier = Modifier.weight(1f)
            )
            SantoyaFilterPill(
                label = stringResource(R.string.kasbon_pay_half),
                selected = nominalDouble == (pelanggan.sisa_kasbon / 2),
                onClick = { nominalInput = (pelanggan.sisa_kasbon / 2).toLong().toString() },
                centerContent = true,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 7.dp),
                modifier = Modifier.weight(0.75f)
            )
            SantoyaFilterPill(
                label = stringResource(R.string.kasbon_pay_10k),
                selected = nominalDouble == 10000.0,
                onClick = { nominalInput = "10000" },
                centerContent = true,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 7.dp),
                modifier = Modifier.weight(0.95f)
            )
        }

        OutlinedTextField(
            value = nominalInput,
            onValueChange = { nominalInput = it.filter { char -> char.isDigit() } },
            label = { Text(stringResource(R.string.kasbon_input_label)) },
            singleLine = true,
            shape = RoundedCornerShape(SantoyaRadius.md),
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
            val kembalian = (nominalDouble - pelanggan.sisa_kasbon).coerceAtLeast(0.0)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(
                        R.string.kasbon_remaining_after,
                        FormatUtils.formatRupiah(sisaSetelahBayar)
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = if (sisaSetelahBayar == 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (kembalian > 0.0) {
                    Text(
                        text = stringResource(R.string.kasbon_kembalian_value, FormatUtils.formatRupiah(kembalian)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
