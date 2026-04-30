package com.devson.pixchive.gallery.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val date = Date(timestamp * 1000L) // Assuming MediaStore returns seconds
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(date)
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
            painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_gallery),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
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
    viewSettings: GalleryViewSettings = GalleryViewSettings()
) {
    val haptics = LocalHapticFeedback.current

    val fileName = image.realPath.substringAfterLast('/')
    val baseName = fileName.substringBeforeLast('.', fileName)
    val extension = fileName.substringAfterLast('.', "").uppercase()

    if (isListMode) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
                .fillMaxWidth()
                .galleryItemClick(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(75.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                    SelectionCheckmarkOverlay(visible = isSelected)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = baseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (viewSettings.showFileExt && extension.isNotEmpty()) {
                            InfoChip(text = extension, bgColor = Color(0xFFE8F5E9), textColor = Color(0xFF2E7D32))
                        }
                        if (viewSettings.showResolution && image.width > 0 && image.height > 0) {
                            InfoChip(text = "${image.width}x${image.height}", bgColor = Color(0xFFE1F5FE), textColor = Color(0xFF0288D1))
                        }
                        if (viewSettings.showSize) {
                            InfoChip(text = formatSize(image.size), bgColor = Color(0xFFF5F5F5), textColor = Color(0xFF757575))
                        }
                        if (viewSettings.showDate) {
                            InfoChip(text = formatDate(image.dateModified), bgColor = Color(0xFFFBE9E7), textColor = Color(0xFFD84315))
                        }
                    }
                }
            }
        }
    } else {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = modifier
                .fillMaxWidth()
                .galleryItemClick(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
        ) {
            Box {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
                    }

                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = baseName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (viewSettings.showFileExt && extension.isNotEmpty()) {
                                InfoChip(text = extension, bgColor = Color(0xFFE8F5E9), textColor = Color(0xFF2E7D32))
                            }
                            if (viewSettings.showResolution && image.width > 0 && image.height > 0) {
                                InfoChip(text = "${image.width}x${image.height}", bgColor = Color(0xFFE1F5FE), textColor = Color(0xFF0288D1))
                            }
                            if (viewSettings.showSize) {
                                InfoChip(
                                    text = formatSize(image.size),
                                    bgColor = Color(0xFFF5F5F5),
                                    textColor = Color(0xFF757575)
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
                SelectionCheckmarkOverlay(visible = isSelected)
            }
        }
    }
}
