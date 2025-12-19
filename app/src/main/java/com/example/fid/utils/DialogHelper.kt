package com.example.fid.utils

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.fid.R
import com.example.fid.ui.theme.DarkCard
import com.example.fid.ui.theme.ErrorRed
import com.example.fid.ui.theme.PrimaryGreen
import com.example.fid.ui.theme.TextPrimary
import com.example.fid.ui.theme.TextSecondary

/**
 * Helper composables for common dialogs
 */

@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = message,
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = PrimaryGreen
                )
            }
        },
        containerColor = DarkCard
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Confirmar",
    cancelText: String = "Cancelar",
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = message,
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (isDestructive) ErrorRed else PrimaryGreen
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = cancelText,
                    color = TextSecondary
                )
            }
        },
        containerColor = DarkCard
    )
}

@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = message,
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "OK",
                    color = PrimaryGreen
                )
            }
        },
        containerColor = DarkCard
    )
}

