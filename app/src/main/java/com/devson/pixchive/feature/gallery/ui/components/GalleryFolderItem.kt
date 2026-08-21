package com.devson.pixchive.feature.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.devson.pixchive.core.data.models.GalleryFolder
import com.devson.pixchive.core.data.models.GalleryViewSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp * 1000L))
}

@Composable
private fun FolderThumbnail(
    folder: GalleryFolder,
    showThumbnail: Boolean,
    modifier: Modifier = Modifier,
    thumbnailSizePx: Int = 384
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (showThumbnail) {
            val request = remember(folder.bucketId, thumbnailSizePx) {
                ImageRequest.Builder(context)
                    .data(folder.thumbnailUri)
                    .size(thumbnailSizePx)
                    .crossfade(true)
                    .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                    .allowHardware(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = folder.folderName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.60f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.25f)
                        )
                    )
            )
        } else {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun MetaChip(text: String, isPrimary: Boolean) {
    val bg = if (isPrimary)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    else
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f)
    val fg = if (isPrimary)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        contentColor = fg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderMetaRow(
    folder: GalleryFolder,
    viewSettings: GalleryViewSettings,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        val countText = if (folder.imageCount == 1) "1 item" else "${folder.imageCount} items"
        MetaChip(countText, isPrimary = true)
        if (viewSettings.showSize && folder.size > 0L) {
            MetaChip(formatSize(folder.size), isPrimary = false)
        }
        if (viewSettings.showDate && folder.dateModified > 0L) {
            MetaChip(formatDate(folder.dateModified), isPrimary = false)
        }
    }
}

/**
 * Modern Material 3 List Item for Gallery Folders / Albums.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryFolderListItem(
    folder: GalleryFolder,
    isSelected: Boolean,
    isSelectionModeActive: Boolean = false,
    viewSettings: GalleryViewSettings = GalleryViewSettings(),
    showThumbnail: Boolean = viewSettings.showThumbnail,
    onClick: () -> Unit,
    onThumbnailClick: (() -> Unit)? = null,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    val bgColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(180),
        label = "folderBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        animationSpec = tween(180),
        label = "folderBorder"
    )

    val handleToggle = {
        onThumbnailClick?.invoke() ?: onLongPress()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        tonalElevation = 1.dp,
        border = BorderStroke(if (isSelected) 1.5.dp else 0.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionModeActive) {
                        handleToggle()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = { handleToggle() },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        }
                    )
            ) {
                FolderThumbnail(
                    folder = folder,
                    showThumbnail = showThumbnail,
                    modifier = Modifier.fillMaxSize()
                )
                SelectionCheckmarkOverlay(visible = isSelected, isDense = true)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = folder.folderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (viewSettings.showPath) {
                    Text(
                        text = folder.folderPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FolderMetaRow(folder = folder, viewSettings = viewSettings)
            }
        }
    }
}

/**
 * Grid Item for Gallery Folders / Albums supporting customizable aspect ratios and ContentScale.Crop.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryFolderItem(
    folder: GalleryFolder,
    isSelected: Boolean,
    isSelectionModeActive: Boolean = false,
    isListMode: Boolean = false,
    gridColumns: Int = 2,
    viewSettings: GalleryViewSettings = GalleryViewSettings(),
    showThumbnail: Boolean = viewSettings.showThumbnail,
    onClick: () -> Unit,
    onThumbnailClick: (() -> Unit)? = null,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1.0f
) {
    if (isListMode) {
        GalleryFolderListItem(
            folder = folder,
            isSelected = isSelected,
            isSelectionModeActive = isSelectionModeActive,
            viewSettings = viewSettings,
            showThumbnail = showThumbnail,
            onClick = onClick,
            onThumbnailClick = onThumbnailClick,
            onLongPress = onLongPress,
            modifier = modifier
        )
        return
    }

    val haptics = LocalHapticFeedback.current

    val bgColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(180),
        label = "folderBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        animationSpec = tween(180),
        label = "folderBorder"
    )

    val handleToggle = {
        onThumbnailClick?.invoke() ?: onLongPress()
    }

    val clickMod = Modifier.galleryItemClick(
        onClick = onClick,
        onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongPress()
        },
        isSelectionModeActive = isSelectionModeActive,
        onToggleSelection = handleToggle
    )

    when {
        gridColumns <= 1 -> {
            // 1-col: wide landscape card
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .then(clickMod),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 120.dp, height = 80.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            FolderThumbnail(
                                folder = folder,
                                showThumbnail = showThumbnail,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = folder.folderName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (viewSettings.showPath) {
                                Text(
                                    text = folder.folderPath,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            FolderMetaRow(folder = folder, viewSettings = viewSettings)
                        }
                    }
                    SelectionCheckmarkOverlay(visible = isSelected)
                }
            }
        }

        gridColumns == 2 -> {
            val card2Shape = RoundedCornerShape(16.dp)
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(card2Shape)
                    .then(clickMod),
                shape = card2Shape,
                colors = CardDefaults.cardColors(containerColor = bgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Image with Target Aspect Ratio and ContentScale.Crop
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        FolderThumbnail(
                            folder = folder,
                            showThumbnail = showThumbnail,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio)
                        )
                        SelectionCheckmarkOverlay(visible = isSelected)
                    }

                    // Text Area with Consistent 12.dp Padding
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = folder.folderName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        if (viewSettings.showPath) {
                            Text(
                                text = folder.folderPath,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FolderMetaRow(folder = folder, viewSettings = viewSettings)
                    }
                }
            }
        }

        else -> {
            // 3+ columns: Compact Card with Scrim
            val card3Shape = RoundedCornerShape(12.dp)
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(card3Shape)
                    .then(clickMod),
                shape = card3Shape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
                border = BorderStroke(if (isSelected) 1.5.dp else 0.dp, borderColor)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FolderThumbnail(
                        folder = folder,
                        showThumbnail = showThumbnail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio)
                    )

                    // Gradient scrim for label readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.40f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.76f)
                                )
                            )
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
                        )
                    }

                    // Metadata at bottom overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = folder.folderName,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${folder.imageCount} items",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }

                    SelectionCheckmarkOverlay(visible = isSelected)
                }
            }
        }
    }
}