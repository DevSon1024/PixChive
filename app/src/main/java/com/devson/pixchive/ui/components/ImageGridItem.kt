package com.devson.pixchive.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.devson.pixchive.data.local.ImageEntity
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGridItem(
    image: ImageEntity,
    columns: Int,
    onClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    // Determine what to show based on column count
    val showDetails = columns <= 2
    val showName = columns <= 4

    // Calculate approx fetch size based on columns
    val fetchSize = when {
        columns <= 2 -> 600
        columns <= 4 -> 400
        else -> 200
    }

    // formattedSize is pre-computed at scan time in FolderScanner and stored in the entity.
    // No Math.log10 / Math.pow ever runs here — it's a simple field read.
    val formattedSize = image.formattedSize

    // remember ensures the same ImageRequest object is reused across recompositions
    // as long as the URI and fetchSize haven't changed.  Without this, AsyncImage
    // receives a brand-new (non-equal) model every scroll frame and re-issues the
    // Coil load, causing the "images reloading" flicker.
    val imageRequest = remember(image.uri, fetchSize) {
        ImageRequest.Builder(context)
            .data(image.uri)
            .size(fetchSize)
            .crossfade(false)
            .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = image.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Overlay Text (Name) only if enabled
                if (showName) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(androidx.compose.ui.Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(
                                text = image.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (showDetails) {
                                Text(
                                    text = formattedSize,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
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
                    if (deleteItem(File(image.path))) {
                        onRefresh()
                    }
                }
            )
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
        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun deleteItem(file: File): Boolean {
    return try {
        val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
        if (deleted) true else false
    } catch (e: Exception) {
        false
    }
}