package com.devson.pixchive.gallery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.pixchive.gallery.data.models.GalleryFolder
import com.devson.pixchive.gallery.data.models.GalleryImage
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.ui.reader.components.ImageDetailsDialog
import java.io.File

@Composable
fun DetailsDialog(
    selectedFolders: List<GalleryFolder> = emptyList(),
    selectedImages: List<GalleryImage> = emptyList(),
    folder: GalleryFolder? = null,
    onDismiss: () -> Unit
) {
    val effectiveFolders = if (selectedFolders.isEmpty() && folder != null) listOf(folder) else selectedFolders

    // If exactly 1 image and no folders are selected, show the full ImageDetailsDialog
    if (effectiveFolders.isEmpty() && selectedImages.size == 1) {
        val image = selectedImages[0]
        val imageFile = ImageFile(
            name = File(image.realPath).name,
            path = image.realPath,
            uri = image.uri,
            size = image.size,
            dateModified = image.dateModified
        )
        ImageDetailsDialog(
            image = imageFile,
            onDismiss = onDismiss
        )
        return
    }

    val title: String
    val body: @Composable () -> Unit

    when {
        effectiveFolders.size == 1 && selectedImages.isEmpty() -> {
            val f = effectiveFolders[0]
            title = "Folder Details"
            body = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DetailRow("Name:", f.folderName)
                    Spacer(Modifier.height(6.dp))
                    DetailRow("Images:", "${f.imageCount}")
                    Spacer(Modifier.height(6.dp))
                    DetailRow("Bucket ID:", f.bucketId)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Internal Storage → ${f.folderName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        effectiveFolders.size > 1 && selectedImages.isEmpty() -> {
            title = "Multiple Folders"
            body = { Text("${effectiveFolders.size} folders selected.") }
        }
        effectiveFolders.isEmpty() && selectedImages.size > 1 -> {
            title = "Multiple Images"
            val totalSize = selectedImages.sumOf { it.size }
            body = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DetailRow("Selected Images:", "${selectedImages.size}")
                    Spacer(Modifier.height(6.dp))
                    DetailRow("Total Size:", formatSize(totalSize))
                }
            }
        }
        else -> {
            val total = effectiveFolders.size + selectedImages.size
            title = "Multiple Items"
            val totalSize = effectiveFolders.sumOf { it.size } + selectedImages.sumOf { it.size }
            body = { 
                Column(modifier = Modifier.fillMaxWidth()) {
                    DetailRow("Selected Items:", "$total")
                    Spacer(Modifier.height(6.dp))
                    if (totalSize > 0L) {
                        DetailRow("Total Size:", formatSize(totalSize))
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            val icon = if (selectedImages.isNotEmpty() && effectiveFolders.isEmpty()) Icons.Default.Image else Icons.Default.Folder
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
        },
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = body,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0)
        else -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
