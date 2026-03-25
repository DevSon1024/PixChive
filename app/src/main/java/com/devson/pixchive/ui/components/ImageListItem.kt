package com.devson.pixchive.ui.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.CachePolicy
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

    val formattedSize = image.formattedSize
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    // Performance fix for list view
    val imageRequest = remember(image.path) {
        ImageRequest.Builder(context)
            .data(File(image.path))
            .size(200) // Fixed small size for lists
            .allowHardware(true) 
            .crossfade(false)
            .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = image.name,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            modifier = Modifier.size(64.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = image.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$formattedSize • ${formatDate(image.dateModified)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
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
}

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
    if (timestamp == 0L) return "Unknown"
    return listItemDateFormat.format(Date(timestamp))
}