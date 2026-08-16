package com.devson.pixchive.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devson.pixchive.core.designsystem.theme.PixchiveTheme

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
    PixChiveDialog(
        onDismissRequest = onDismiss,
        title = title,
        icon = icon,
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(dismissText)
            }
        }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PermissionDeniedDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionDialog(
        icon = Icons.Default.Lock,
        title = "Permission Required",
        message = "PixChive needs storage permission to access your gallery and comic files. " +
                "Please grant the permission in system app settings.",
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
        icon = Icons.Default.Security,
        title = "Storage Access Needed",
        message = rationale,
        confirmText = "Continue",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Preview(showBackground = true)
@Composable
private fun PermissionDialogPreview() {
    PixchiveTheme {
        PermissionDeniedDialog(
            onOpenSettings = {},
            onDismiss = {}
        )
    }
}