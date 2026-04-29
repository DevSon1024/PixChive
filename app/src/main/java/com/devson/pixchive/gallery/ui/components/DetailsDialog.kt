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
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image

@Composable
fun DetailsDialog(
    selectedFolders: List<GalleryFolder> = emptyList(),
    selectedImages: List<GalleryImage> = emptyList(),
    folder: GalleryFolder? = null,
    onDismiss: () -> Unit
) {
    val title: String
    val body: @Composable () -> Unit
    
    val effectiveFolders = if (selectedFolders.isEmpty() && folder != null) listOf(folder) else selectedFolders

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
        effectiveFolders.isEmpty() && selectedImages.size == 1 -> {
            val image = selectedImages[0]
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date(image.dateModified * 1000L))
            title = "Image Details"
            body = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DetailRow("Path:", image.realPath)
                    Spacer(Modifier.height(6.dp))
                    DetailRow("Modified:", dateStr)
                    Spacer(Modifier.height(6.dp))
                    DetailRow("Resolution:", "N/A (EXIF)")
                    Spacer(Modifier.height(6.dp))
                    DetailRow("Camera:", "N/A (EXIF)")
                }
            }
        }
        else -> {
            val total = effectiveFolders.size + selectedImages.size
            title = "Multiple Items"
            body = { Text("$total items selected. Size calculation coming soon.") }
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
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
