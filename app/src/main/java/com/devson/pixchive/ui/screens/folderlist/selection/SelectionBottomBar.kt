package com.devson.pixchive.ui.screens.folderlist.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.pixchive.data.local.ImageEntity

@Composable
fun SelectionBottomBar(
    selectedImages: Set<ImageEntity>,
    onViewAll: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShowInfo: () -> Unit,
    onShare: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionColumn(
                icon = Icons.Filled.OpenInNew,
                label = "Open",
                onClick = onViewAll
            )
            ActionColumn(icon = Icons.AutoMirrored.Filled.DriveFileMove, label = "Move", onClick = onMove)
            ActionColumn(icon = Icons.Filled.ContentCopy, label = "Copy", onClick = onCopy)
            ActionColumn(icon = Icons.Filled.Delete, label = "Delete", onClick = onDelete)
            if (selectedImages.size == 1) {
                ActionColumn(
                    icon = Icons.Filled.DriveFileRenameOutline,
                    label = "Rename",
                    onClick = onRename
                )
            }
            ActionColumn(icon = Icons.Filled.Share, label = "Share", onClick = onShare)
            ActionColumn(icon = Icons.Filled.Info, label = "Info", onClick = onShowInfo)
        }
    }
}

@Composable
private fun ActionColumn(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label)
        Text(label, fontSize = 10.sp)
    }
}
