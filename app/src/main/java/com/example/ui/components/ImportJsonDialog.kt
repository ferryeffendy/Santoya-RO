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
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize
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

    val readSuccessMsg = stringResource(R.string.import_dialog_read_success)
    val readErrorMsg = stringResource(R.string.import_dialog_read_error)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = ExportHelper.readTextFromUri(context, uri)
            if (content != null) {
                jsonInput = content
                selectedFileName = uri.lastPathSegment ?: "cadangan_santoya.json"
                Toast.makeText(context, readSuccessMsg, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, readErrorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SantoyaFormBottomSheet(
        title = stringResource(R.string.import_dialog_title),
        subtitle = stringResource(R.string.import_dialog_subtitle),
        headerIcon = Icons.Outlined.FileUpload,
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.import_dialog_confirm),
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
            shape = RoundedCornerShape(SantoyaRadius.md),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.import_dialog_info),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        FilledTonalButton(
            onClick = { filePickerLauncher.launch("*/*") },
            shape = RoundedCornerShape(SantoyaRadius.md),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SantoyaSize.touchTarget)
                .testTag("btn_pick_json_file")
        ) {
            Icon(imageVector = Icons.Outlined.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedFileName?.let {
                    stringResource(R.string.import_dialog_selected_file, it)
                } ?: stringResource(R.string.import_dialog_pick_file),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Text(
            text = stringResource(R.string.import_dialog_paste_label),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedTextField(
            value = jsonInput,
            onValueChange = { jsonInput = it },
            placeholder = {
                Text(
                    "{\n  \"data_pelanggan_nested\": [\n    {\n      \"nama\": \"Bu Siti\",\n      \"alamat\": \"Desa Santoya\",\n      \"no_hp\": \"0812...\",\n      \"sisa_kasbon\": 0,\n      \"riwayat_transaksi\": [...]\n    }\n  ]\n}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                )
            },
            minLines = 5,
            maxLines = 9,
            shape = RoundedCornerShape(SantoyaRadius.md),
            textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus()
                .testTag("input_json_content")
        )
    }
}
