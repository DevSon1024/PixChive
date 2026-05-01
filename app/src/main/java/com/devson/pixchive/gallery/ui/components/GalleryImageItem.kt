package com.devson.pixchive.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
private fun InfoChip(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
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
    // columnCount drives dense mode: 3+ cols = only extension badge shown
    columnCount: Int = 2,
    viewSettings: GalleryViewSettings = GalleryViewSettings()
) {
    val haptics = LocalHapticFeedback.current
    val isDense = columnCount >= 3

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
        // List layout mirrors VideoListItem
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 3.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 0.dp else 1.dp,
                pressedElevation = 0.dp
            ),
            border = BorderStroke(if (isSelected) 1.5.dp else 0.dp, borderColor),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail: Tapping toggles selection
                Card(
                    modifier = Modifier
                        .size(width = 100.dp, height = 75.dp)
                        .clickable { 
                            if (onThumbnailClick != null) onThumbnailClick() 
                            else onLongClick() 
                        },
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        if (viewSettings.showThumbnail) {
                            AsyncImage(
                                model = image.uri,
                                contentDescription = "Image thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ThumbnailPlaceholder()
                        }
                        // Extension badge overlaid bottom-end
                        if (viewSettings.showFileExt && extension.isNotEmpty() && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(5.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = extension,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        SelectionCheckmarkOverlay(visible = isSelected, isDense = true)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = baseName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                    )

                    if (viewSettings.showPath) {
                        Text(
                            text = image.realPath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (viewSettings.showResolution && image.width > 0 && image.height > 0) {
                            InfoChip(text = "${image.width}x${image.height}", bgColor = Color(0xFFE1F5FE), textColor = Color(0xFF0288D1))
                        }
                        if (viewSettings.showSize) {
                            InfoChip(
                                text = formatSize(image.size),
                                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (viewSettings.showDate) {
                            InfoChip(text = formatDate(image.dateModified), bgColor = Color(0xFFFBE9E7), textColor = Color(0xFFD84315))
                        }
                    }
                }
            }
        }
    } else {
        // Grid layout: dense (3-4 cols) = thumbnail + ext badge only, no bottom label
        Card(
            modifier = modifier
                .fillMaxWidth()
                .galleryItemClick(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                ),
            shape = RoundedCornerShape(if (isDense) 8.dp else 14.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
            border = BorderStroke(if (isSelected) 1.5.dp else 0.dp, borderColor)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Square thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(
                            if (isDense) RoundedCornerShape(8.dp)
                            else RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                        )
                ) {
                    if (viewSettings.showThumbnail) {
                        AsyncImage(
                            model = image.uri,
                            contentDescription = "Image thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ThumbnailPlaceholder()
                    }

                    // Dense mode: only ext badge overlaid on thumbnail bottom-start
                    if (isDense && viewSettings.showFileExt && extension.isNotEmpty() && !isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = extension,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    // Extension badge (2-col only, bottom-end)
                    if (!isDense && viewSettings.showFileExt && extension.isNotEmpty() && !isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = extension,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    SelectionCheckmarkOverlay(visible = isSelected, isDense = isDense)
                }

                // Bottom label: only shown for 2-col (non-dense) mode
                if (!isDense) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 10.dp)
                    ) {
                        Text(
                            text = baseName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                        )

                        if (viewSettings.showPath) {
                            Text(
                                text = image.realPath,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 1.dp, bottom = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (viewSettings.showResolution && image.width > 0 && image.height > 0) {
                                InfoChip(text = "${image.width}x${image.height}", bgColor = Color(0xFFE1F5FE), textColor = Color(0xFF0288D1))
                            }
                            if (viewSettings.showSize) {
                                InfoChip(
                                    text = formatSize(image.size),
                                    bgColor = MaterialTheme.colorScheme.surfaceVariant,
                                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (viewSettings.showDate) {
                                InfoChip(
                                    text = formatDate(image.dateModified),
                                    bgColor = Color(0xFFFBE9E7),
                                    textColor = Color(0xFFD84315)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}