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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size
import coil.compose.AsyncImage
import com.devson.pixchive.gallery.data.models.GalleryFolder
import com.devson.pixchive.gallery.data.models.GalleryViewSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FolderShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cornerRadius = with(density) { 8.dp.toPx() }
        val tabWidth = size.width * 0.45f
        val tabHeight = size.height * 0.15f

        val path = Path().apply {
            moveTo(0f, cornerRadius)
            quadraticBezierTo(0f, 0f, cornerRadius, 0f)
            lineTo(tabWidth - cornerRadius, 0f)
            quadraticBezierTo(tabWidth, 0f, tabWidth + cornerRadius/2, cornerRadius/2)
            lineTo(tabWidth + cornerRadius*1.5f, tabHeight - cornerRadius/2)
            quadraticBezierTo(tabWidth + cornerRadius*2, tabHeight, tabWidth + cornerRadius*3, tabHeight)
            lineTo(size.width - cornerRadius, tabHeight)
            quadraticBezierTo(size.width, tabHeight, size.width, tabHeight + cornerRadius)
            lineTo(size.width, size.height - cornerRadius)
            quadraticBezierTo(size.width, size.height, size.width - cornerRadius, size.height)
            lineTo(cornerRadius, size.height)
            quadraticBezierTo(0f, size.height, 0f, size.height - cornerRadius)
            close()
        }
        return Outline.Generic(path)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp * 1000L) // Assuming MediaStore returns seconds, if milliseconds remove * 1000L
    // MediaStore.Images.Media.DATE_MODIFIED is in seconds.
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
fun GalleryFolderItem(
    folder: GalleryFolder,
    isSelected: Boolean,
    isListMode: Boolean = false,
    viewSettings: GalleryViewSettings = GalleryViewSettings(),
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

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
                        onLongPress()
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
                        .clip(FolderShape())
                ) {
                    if (viewSettings.showThumbnail) {
                        AsyncImage(
                            model = folder.thumbnailUri,
                            contentDescription = "Thumbnail for ${folder.folderName}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ThumbnailPlaceholder()
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = folder.folderName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (viewSettings.showPath) {
                        Text(
                            text = folder.folderPath,
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
                        InfoChip(text = "${folder.imageCount} images", bgColor = Color(0xFFFFF9C4), textColor = Color(0xFFF57F17))
                        if (viewSettings.showSize) {
                            InfoChip(text = formatSize(folder.size), bgColor = Color(0xFFF5F5F5), textColor = Color(0xFF757575))
                        }
                        if (viewSettings.showDate) {
                            InfoChip(text = formatDate(folder.dateModified), bgColor = Color(0xFFFBE9E7), textColor = Color(0xFFD84315))
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
                        onLongPress()
                    }
                )
        ) {
            Box {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(FolderShape())
                    ) {
                        if (viewSettings.showThumbnail) {
                            AsyncImage(
                                model = folder.thumbnailUri,
                                contentDescription = "Thumbnail for ${folder.folderName}",
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
                            text = folder.folderName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            InfoChip(
                                text = "${folder.imageCount} Images",
                                bgColor = Color(0xFFFFF9C4),
                                textColor = Color(0xFFF57F17)
                            )
                            if (viewSettings.showSize) {
                                InfoChip(
                                    text = formatSize(folder.size),
                                    bgColor = Color(0xFFF5F5F5),
                                    textColor = Color(0xFF757575)
                                )
                            }
                            if (viewSettings.showDate) {
                                InfoChip(
                                    text = formatDate(folder.dateModified),
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
