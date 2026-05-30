package com.devson.pixchive.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.gallery.data.models.GalleryViewSettings
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
    val date = Date(timestamp * 1000L)
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
}

@Composable
private fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color = Color.Black.copy(alpha = 0.45f),
    textColor: Color = Color.White,
    borderColor: Color = Color.White.copy(alpha = 0.15f),
    fontWeight: FontWeight = FontWeight.Medium,
    fontSize: androidx.compose.ui.unit.TextUnit = 9.sp
) {
    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, borderColor)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
        )
    }
}

@Composable
private fun ThumbnailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun SelectionCheckmarkOverlay(visible: Boolean, isDense: Boolean = false) {
    val iconSize = if (isDense) 20.dp else 26.dp
    val circleSize = if (isDense) 32.dp else 40.dp

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.45f else 0f,
        animationSpec = tween(180),
        label = "scrimAlpha"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkScale"
    )

    if (scrimAlpha > 0f || visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = scrimAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(checkScale)
                    .size(circleSize)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryImageItem(
    image: GalleryImage,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isListMode: Boolean = false,
    onThumbnailClick: (() -> Unit)? = null,
    columnCount: Int = 2,
    viewSettings: GalleryViewSettings = GalleryViewSettings()
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val isDense = columnCount >= 3

    val fetchSize = when {
        isListMode -> 160
        columnCount <= 2 -> 400
        columnCount <= 4 -> 256
        else -> 160
    }

    val imageRequest = remember(image.uri, fetchSize) {
        ImageRequest.Builder(context)
            .data(image.uri)
            .size(fetchSize)
            .crossfade(false)
            .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    val fileName = image.realPath.substringAfterLast('/')
    val baseName = fileName.substringBeforeLast('.', fileName)
    val extension = fileName.substringAfterLast('.', "").uppercase()

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "itemBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(180),
        label = "itemBorder"
    )

    if (isListMode) {
        val rowShape = RoundedCornerShape(12.dp)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
                .background(bgColor)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(
                        onClick = { onThumbnailClick?.invoke() ?: onLongClick() },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    )
            ) {
                if (viewSettings.showThumbnail) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ThumbnailPlaceholder()
                }
                SelectionCheckmarkOverlay(visible = isSelected, isDense = true)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = baseName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (viewSettings.showPath) {
                    Text(
                        text = image.realPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewSettings.showFileExt && extension.isNotEmpty()) {
                        InfoChip(text = extension, bgColor = MaterialTheme.colorScheme.secondaryContainer, textColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    if (viewSettings.showResolution && image.width > 0) {
                        InfoChip(text = "${image.width}x${image.height}", bgColor = MaterialTheme.colorScheme.tertiaryContainer, textColor = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    if (viewSettings.showSize) {
                        InfoChip(text = formatSize(image.size), bgColor = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (viewSettings.showDate) {
                        InfoChip(text = formatDate(image.dateModified), bgColor = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    } else {
        val gridShape = RoundedCornerShape(if (isDense) 10.dp else 16.dp)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(gridShape)
                .galleryItemClick(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
                .background(bgColor)
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = borderColor,
                    shape = gridShape
                )
        ) {
            if (viewSettings.showThumbnail) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ThumbnailPlaceholder()
            }

                // Metadata Overlays (Grid Mode)
                if (columnCount <= 2) {
                    // Top Overlay: Filename
                    if (viewSettings.showPath) {
                        InfoChip(
                            text = baseName,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .fillMaxWidth(0.85f)
                        )
                    }

                    // Bottom Overlay: Metadata Row
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewSettings.showFileExt && extension.isNotEmpty()) {
                            InfoChip(text = extension, fontWeight = FontWeight.Bold)
                        }
                        if (viewSettings.showResolution && image.width > 0) {
                            InfoChip(text = "${image.width}x${image.height}")
                        }
                        if (viewSettings.showSize) {
                            InfoChip(text = formatSize(image.size))
                        }
                        if (viewSettings.showDate) {
                            InfoChip(
                                text = formatDate(image.dateModified),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                } else {
                    // Column count 3 or 4: Only show extension chip
                    if (viewSettings.showFileExt && extension.isNotEmpty()) {
                        InfoChip(
                            text = extension,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        )
                    }
                }

                SelectionCheckmarkOverlay(visible = isSelected, isDense = isDense)
        }
    }
}