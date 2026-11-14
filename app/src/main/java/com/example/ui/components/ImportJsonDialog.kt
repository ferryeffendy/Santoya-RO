package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ExportHelper

@Composable
fun ImportJsonDialog(
    onDismiss: () -> Unit,
    onImportJson: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var jsonInput by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = ExportHelper.readTextFromUri(context, uri)
            if (content != null) {
                jsonInput = content
                selectedFileName = uri.lastPathSegment ?: "cadangan_santoya.json"
                Toast.makeText(context, "File cadangan terbaca", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal membaca file cadangan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    SantoyaFormBottomSheet(
        title = "Pulihkan dari File Cadangan",
        subtitle = "Impor file JSON atau tempel teks cadangan",
        headerIcon = Icons.Default.FileUpload,
        onDismiss = onDismiss,
        confirmLabel = "Pulihkan",
        confirmEnabled = jsonInput.isNotBlank(),
        confirmTestTag = "btn_confirm_import_json",
        onConfirm = {
            if (jsonInput.isNotBlank()) {
                focusManager.clearFocus()
                keyboardController?.hide()
                onImportJson(jsonInput)
                onDismiss()
            }
        }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pilih file cadangan dari HP, atau tempel teks cadangannya di bawah.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        FilledTonalButton(
            onClick = { filePickerLauncher.launch("*/*") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("btn_pick_json_file")
        ) {
            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedFileName != null) "File: $selectedFileName" else "Pilih File Cadangan",
                fontSize = 13.sp
            )
        }

        Text(
            text = "Atau tempel teks cadangan:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedTextField(
            value = jsonInput,
            onValueChange = { jsonInput = it },
            placeholder = {
                Text(
                    "{\n  \"data_pelanggan_nested\": [\n    {\n      \"nama\": \"Bu Siti\",\n      \"alamat\": \"Desa Santoya\",\n      \"no_hp\": \"0812...\",\n      \"sisa_kasbon\": 0,\n      \"riwayat_transaksi\": [...]\n    }\n  ]\n}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            minLines = 5,
            maxLines = 9,
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus()
                .testTag("input_json_content")
        )
    }
}
