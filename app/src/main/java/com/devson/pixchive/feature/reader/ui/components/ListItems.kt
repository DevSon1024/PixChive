package com.devson.pixchive.feature.reader.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.devson.pixchive.core.data.Chapter
import com.devson.pixchive.core.data.local.ImageEntity
import com.devson.pixchive.core.designsystem.component.OptionItem
import com.devson.pixchive.core.designsystem.component.OptionsBottomSheet
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChapterListItem(
    chapter: Chapter,
    savedPage: Int = 0,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val isCompleted = chapter.imageCount > 0 && savedPage >= (chapter.imageCount - 1)
    val progressFraction = if (chapter.imageCount > 0 && savedPage > 0) {
        ((savedPage + 1).toFloat() / chapter.imageCount).coerceIn(0f, 1f)
    } else 0f

    OutlinedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            1.dp,
            if (isCompleted) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Portrait cover thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 62.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    val firstImage = chapter.images.firstOrNull()
                    if (firstImage != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(if (firstImage.path.isNotEmpty()) File(firstImage.path) else firstImage.uri)
                                .size(240)
                                .bitmapConfig(Bitmap.Config.RGB_565)
                                .allowHardware(true)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapter.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                               else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isCompleted) {
                                "${chapter.imageCount} pages"
                            } else if (savedPage > 0) {
                                "Page ${savedPage + 1}/${chapter.imageCount}"
                            } else {
                                "${chapter.imageCount} pages"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Read",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else if (progressFraction > 0f) {
                            val progressPercent = (progressFraction * 100).toInt()
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$progressPercent%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (showMenu) {
                        OptionsBottomSheet(
                            title = chapter.displayName,
                            subtitle = "${chapter.imageCount} pages",
                            options = listOf(
                                OptionItem(
                                    label = "Remove from list",
                                    icon = Icons.Default.Close,
                                    isDestructive = true,
                                    onClick = onRemove
                                )
                            ),
                            onDismiss = { showMenu = false }
                        )
                    }
                }
            }

            // Linear accent progress bar along bottom
            if (progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun ChapterImageListItem(
    image: ImageEntity,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnShareClick by rememberUpdatedState(onShareClick)
    val currentOnDeleteClick by rememberUpdatedState(onDeleteClick)
    val currentOnInfoClick by rememberUpdatedState(onInfoClick)

    OutlinedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { currentOnClick() })
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(if (image.path.isNotEmpty()) File(image.path) else image.uri)
                        .size(200)
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .allowHardware(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = image.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatSize(image.size)} | ${formatDateString(image.dateModified)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (showMenu) {
                    val options = mutableListOf<OptionItem>().apply {
                        if (currentOnInfoClick != null) {
                            add(
                                OptionItem(
                                    label = "Details",
                                    icon = Icons.Default.Info,
                                    onClick = { currentOnInfoClick?.invoke() }
                                )
                            )
                        }
                        add(
                            OptionItem(
                                label = "Share",
                                icon = Icons.Default.Share,
                                onClick = currentOnShareClick
                            )
                        )
                        add(
                            OptionItem(
                                label = "Delete",
                                icon = Icons.Default.Delete,
                                isDestructive = true,
                                onClick = currentOnDeleteClick
                            )
                        )
                    }
                    OptionsBottomSheet(
                        title = image.name,
                        subtitle = "${formatSize(image.size)} | ${formatDateString(image.dateModified)}",
                        options = options,
                        onDismiss = { showMenu = false }
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val group = digitGroups.coerceIn(0, units.size - 1)
    return String.format("%.1f %s", bytes / Math.pow(1024.0, group.toDouble()), units[group])
}

private fun formatDateString(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown Date"
    val sdf = SimpleDateFormat("d MMMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}