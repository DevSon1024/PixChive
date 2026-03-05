package com.devson.pixchive.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.pixchive.data.local.ImageEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val listItemDateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

@Composable
fun ImageListItem(
    image: ImageEntity,
    onClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    // formattedSize is pre-computed at scan time in FolderScanner and stored in the entity.
    // No Math.log10 / Math.pow ever runs here — it's a simple field read.
    val formattedSize = image.formattedSize
    val formattedDate = remember(image.dateModified) { formatDate(image.dateModified) }

    // Stable ImageRequest — only rebuilt when URI changes, not on every recomposition.
    // Without remember, AsyncImage receives a new (non-equal) object every scroll frame
    // and re-triggers a Coil load even though the image hasn’t changed.
    val imageRequest = remember(image.uri) {
        ImageRequest.Builder(context)
            .data(image.uri)
            .size(200)
            .crossfade(false)
            .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .build()
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.size(48.dp),
                colors = CardDefaults.cardColors(containerColor = Color.LightGray)
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = image.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$formattedSize | $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = {
                            showMenu = false
                            shareItem(context, image)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            if (deleteItem(context, File(image.path))) {
                                onRefresh()
                            }
                        }
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
    }
}

// Helpers
private fun shareItem(context: Context, image: ImageEntity) {
    try {
        val file = File(image.path)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun deleteItem(context: Context, file: File): Boolean {
    return try {
        val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
        if (deleted) {
            Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
            true
        } else {
            Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
            false
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        false
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown Date"
    return listItemDateFormat.format(Date(timestamp))
}