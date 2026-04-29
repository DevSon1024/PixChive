package com.devson.pixchive.gallery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryBottomOptionSheet(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (selectedCount == 1) "1 item selected" else "$selectedCount items selected",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider()
            OptionRow(icon = Icons.AutoMirrored.Filled.DriveFileMove, label = "Move", onClick = { onDismiss(); onMove() })
            OptionRow(icon = Icons.Default.ContentCopy, label = "Copy", onClick = { onDismiss(); onCopy() })
            if (selectedCount == 1) {
                OptionRow(icon = Icons.Default.Edit, label = "Rename", onClick = { onDismiss(); onRename() })
            }
            OptionRow(icon = Icons.Default.Share, label = "Share", onClick = { onDismiss(); onShare() })
            OptionRow(icon = Icons.Default.Info, label = "Info", onClick = { onDismiss(); onInfo() })
            OptionRow(
                icon = Icons.Default.Delete,
                label = "Delete",
                onClick = { onDismiss(); onDelete() },
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}
