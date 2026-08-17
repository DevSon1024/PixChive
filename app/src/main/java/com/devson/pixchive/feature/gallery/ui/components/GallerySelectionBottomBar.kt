package com.devson.pixchive.feature.gallery.ui.components

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.pixchive.core.data.FileOperationsViewModel
import com.devson.pixchive.core.data.models.GalleryImage
import com.devson.pixchive.core.utils.shareMedia

/**
 * Floating Capsule Selection Action Bar that morphs from the main bottom navbar during selection mode.
 */
@Composable
fun GallerySelectionBottomBar(
    selectedImages: List<GalleryImage> = emptyList(),
    selectedCount: Int = selectedImages.size,
    fileOpsViewModel: FileOperationsViewModel? = null,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .padding(bottom = 16.dp)
            .height(56.dp)
            .wrapContentWidth()
            .graphicsLayer {
                shadowElevation = 8.dp.toPx()
                shape = RoundedCornerShape(28.dp)
                clip = true
            },
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxHeight()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapsuleActionButton(
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                label = "Move",
                onClick = onMove
            )
            CapsuleActionButton(
                icon = Icons.Filled.ContentCopy,
                label = "Copy",
                onClick = onCopy
            )
            if (selectedCount == 1) {
                CapsuleActionButton(
                    icon = Icons.Filled.Edit,
                    label = "Rename",
                    onClick = onRename
                )
            }
            if (selectedImages.isNotEmpty()) {
                CapsuleActionButton(
                    icon = Icons.Filled.Share,
                    label = "Share",
                    onClick = { shareMedia(context, selectedImages) }
                )
            }
            CapsuleActionButton(
                icon = Icons.Filled.Info,
                label = "Info",
                onClick = onInfo
            )
            CapsuleActionButton(
                icon = Icons.Filled.Delete,
                label = "Delete",
                onClick = {
                    if (fileOpsViewModel != null) {
                        showDeleteDialog = true
                    } else {
                        onDelete()
                    }
                },
                tint = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showDeleteDialog) {
        val uris: List<Uri> = selectedImages.map { it.uri }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Images") },
            text = { Text("Choose how you want to delete the selected image(s).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileOpsViewModel?.deleteImages(context, uris, trash = true)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Move to Recycle Bin")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            fileOpsViewModel?.deleteImages(context, uris, trash = false)
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Permanently")
                    }
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun CapsuleActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}