package com.devson.pixchive.feature.reader.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.devson.pixchive.core.data.Chapter
import com.devson.pixchive.core.designsystem.component.OptionItem
import com.devson.pixchive.core.designsystem.component.OptionsBottomSheet
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChapterGridItem(
    chapter: Chapter,
    columns: Int,
    savedPage: Int = 0,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    val fetchSize = if (columns <= 2) 360 else 240
    val firstImagePath = chapter.images.firstOrNull()?.path
    val imageRequest = remember(firstImagePath, fetchSize) {
        if (firstImagePath != null) {
            ImageRequest.Builder(context)
                .data(File(firstImagePath))
                .size(fetchSize)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .allowHardware(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .build()
        } else null
    }

    val isCompleted = chapter.imageCount > 0 && savedPage >= (chapter.imageCount - 1)
    val progressFraction = if (chapter.imageCount > 0 && savedPage > 0) {
        ((savedPage + 1).toFloat() / chapter.imageCount).coerceIn(0f, 1f)
    } else 0f

    Box(modifier = Modifier.padding(2.dp)) {
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
                .aspectRatio(0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMenu = true
                        }
                    )
            ) {
                // Top section: Comic / Manga cover art
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    if (firstImagePath != null) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = chapter.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Folder,
                            null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }

                    // Top-right status badge
                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Read",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Read",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    } else if (progressFraction > 0f) {
                        val progressPercent = (progressFraction * 100).toInt()
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "$progressPercent%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (chapter.imageCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            Text(
                                text = "${chapter.imageCount}p",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Bottom section: Title, progress bar, and page count
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = chapter.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.SemiBold
                        ),
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (progressFraction > 0f) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isCompleted) {
                            "${chapter.imageCount} pages"
                        } else if (savedPage > 0) {
                            "Page ${savedPage + 1}/${chapter.imageCount}"
                        } else {
                            "${chapter.imageCount} pages"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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