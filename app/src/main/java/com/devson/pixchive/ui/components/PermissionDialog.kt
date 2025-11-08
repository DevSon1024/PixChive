package com.devson.pixchive.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun PermissionDialog(
    icon: ImageVector = Icons.Default.Folder,
    title: String,
    message: String,
    confirmText: String = "Grant Permission",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun PermissionDeniedDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionDialog(
        icon = Icons.Default.Folder,
        title = "Permission Required",
        message = "PixChive needs storage permission to access your comic images. " +
                "Please grant the permission in app settings.",
        confirmText = "Open Settings",
        dismissText = "Not Now",
        onConfirm = onOpenSettings,
        onDismiss = onDismiss
    )
}

@Composable
fun PermissionRationaleDialog(
    rationale: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionDialog(
        icon = Icons.Default.Folder,
        title = "Storage Access Needed",
        message = rationale,
        confirmText = "Continue",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
