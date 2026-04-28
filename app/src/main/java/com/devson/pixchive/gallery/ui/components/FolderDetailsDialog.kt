package com.devson.pixchive.gallery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.pixchive.gallery.data.models.GalleryFolder

@Composable
fun FolderDetailsDialog(
    folder: GalleryFolder,
    onDismiss: () -> Unit
) {
    // We derive a physical path estimation from the bucket ID/Name since MediaStore masks the root path.
    // Real path fetching for standard gallery requires querying the DATA column which is deprecated in scoped storage, 
    // but we can show the logical path here.
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(text = "Folder Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow("Name:", folder.folderName)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Items:", "${folder.imageCount} images")
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("System Bucket ID:", folder.bucketId)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Internal Storage → ${folder.folderName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
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
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}