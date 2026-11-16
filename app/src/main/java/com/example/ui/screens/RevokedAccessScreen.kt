package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.companion.kzpmrw.R
import com.example.ui.theme.SantoyaRadius
import com.example.ui.theme.SantoyaSize

/** Menahan UI selama data pemilik lama dihapus dari perangkat. */
@Composable
fun SessionPreparingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.session_preparing_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.session_preparing_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

/**
 * Layar penuh yang tidak bisa ditutup ketika pemilik menonaktifkan akun karyawan.
 * Menahan semua interaksi agar tidak ada transaksi baru yang dibuat tanpa hak akses.
 */
@Composable
fun RevokedAccessDialog(
    ownerEmail: String = "",
    onSignOut: () -> Unit
) {
    var showConfirmSignOutDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 460.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.revoked_access_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.revoked_access_message),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (ownerEmail.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.revoked_access_owner_label, ownerEmail),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.revoked_access_local_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showConfirmSignOutDialog = true },
                        shape = RoundedCornerShape(SantoyaRadius.md),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = SantoyaSize.touchTarget)
                            .testTag("btn_revoked_sign_out")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.revoked_access_sign_out_button),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        if (showConfirmSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmSignOutDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.revoked_access_confirm_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.revoked_access_confirm_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmSignOutDialog = false
                            onSignOut()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.testTag("btn_confirm_revoked_sign_out")
                    ) {
                        Text(
                            text = stringResource(R.string.revoked_access_confirm_button),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmSignOutDialog = false },
                        modifier = Modifier.testTag("btn_cancel_revoked_sign_out")
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            )
        }
    }
}

/**
 * Layar konfirmasi peran ketika akun Google baru login dan belum terdaftar di `staff_links` maupun `depots`.
 * Mencegah akun karyawan secara tidak sengaja membuat depot bayangan sendiri.
 */
@Composable
fun RoleConfirmationDialog(
    userEmail: String,
    onRecheckStaffLink: ((Boolean) -> Unit) -> Unit,
    onConfirmAsOwner: () -> Unit,
    onSignOut: () -> Unit
) {
    var isChecking by remember { mutableStateOf(false) }
    var showOwnerConfirmDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 460.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.role_confirm_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.role_confirm_subtitle, userEmail.ifBlank { "-" }),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(SantoyaRadius.lg),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.role_confirm_staff_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.role_confirm_staff_card_desc, userEmail.ifBlank { "-" }),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    if (!isChecking) {
                                        isChecking = true
                                        onRecheckStaffLink {
                                            isChecking = false
                                        }
                                    }
                                },
                                enabled = !isChecking,
                                shape = RoundedCornerShape(SantoyaRadius.md),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = SantoyaSize.touchTarget)
                                    .testTag("btn_role_recheck_staff")
                            ) {
                                if (isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.role_confirm_btn_checking),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.role_confirm_btn_recheck),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (!com.example.util.KaryawanScopeHelper.isDesignatedStaffEmail(userEmail)) {
                        TextButton(
                            onClick = { showOwnerConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = SantoyaSize.touchTarget)
                                .testTag("btn_role_choose_owner")
                        ) {
                            Text(
                                text = stringResource(R.string.role_confirm_owner_btn),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = SantoyaSize.touchTarget)
                            .testTag("btn_role_sign_out")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.role_confirm_sign_out_btn),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (showOwnerConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showOwnerConfirmDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.role_confirm_owner_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.role_confirm_owner_dialog_msg, userEmail.ifBlank { "-" }),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showOwnerConfirmDialog = false
                            onConfirmAsOwner()
                        },
                        modifier = Modifier.testTag("btn_confirm_role_owner")
                    ) {
                        Text(
                            text = stringResource(R.string.role_confirm_owner_dialog_confirm),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showOwnerConfirmDialog = false },
                        modifier = Modifier.testTag("btn_cancel_role_owner")
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            )
        }
    }
}

