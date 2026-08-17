package com.devson.pixchive.feature.reader.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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

    val showDetails = columns <= 2
    val showName = columns <= 4
    val fetchSize = if (columns <= 2) 400 else 256

    val firstImagePath = chapter.images.firstOrNull()?.path
    val imageRequest = remember(firstImagePath, fetchSize) {
        if (firstImagePath != null) {
            ImageRequest.Builder(context)
                .data(File(firstImagePath))
                .size(fetchSize)
                .allowHardware(true)
                .crossfade(false)
                .build()
        } else null
    }

    val shape = RoundedCornerShape(14.dp)
    val isCompleted = chapter.imageCount > 0 && savedPage >= (chapter.imageCount - 1)
    val progressFraction = if (chapter.imageCount > 0 && savedPage > 0) {
        ((savedPage + 1).toFloat() / chapter.imageCount).coerceIn(0f, 1f)
    } else 0f

    Box(modifier = Modifier.padding(3.dp)) {
        OutlinedCard(
            shape = shape,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
        ) {
            Box(
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
                if (firstImagePath != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = chapter.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Bottom gradient label & chapter information
                if (showName) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Black.copy(alpha = 0.88f)
                                    )
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = chapter.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (showDetails) {
                                Text(
                                    text = "${chapter.imageCount} pages",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Progress Badge at Top-End
                if (savedPage > 0 && chapter.imageCount > 0) {
                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shadowElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Read",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    } else {
                        val progressPercent = (progressFraction * 100).toInt()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shadowElevation = 2.dp
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

                // Linear accent progress bar along bottom
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomStart)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
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