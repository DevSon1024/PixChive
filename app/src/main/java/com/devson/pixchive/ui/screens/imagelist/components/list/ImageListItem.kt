package com.devson.pixchive.ui.screens.imagelist.components.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.pixchive.model.Image
import com.devson.pixchive.model.ViewSettings
import com.devson.pixchive.ui.screens.imagelist.components.common.ImageMetadataChips

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageListItem(
    image: Image,
    settings: ViewSettings,
    isSelected: Boolean = false,
    onClick: (Image) -> Unit,
    onLongClick: (Image) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val bgColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "listItemBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        animationSpec = tween(180),
        label = "listItemBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .combinedClickable(
                onClick = { onClick(image) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(image)
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 0.dp else 1.dp,
            pressedElevation = 0.dp
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Thumbnail ────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .size(width = 100.dp, height = 60.dp)
                    .then(if (settings.selectByThumbnail) Modifier.clickable { onLongClick(image) } else Modifier),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(Color.Transparent),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    if (settings.showThumbnail) {
                        ImageThumbnail(
                            uri = image.uri,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Selection overlay
                    ThumbnailSelectionOverlay(isSelected, isDense = true)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ── Text section ─────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (settings.showFileExtension) image.title
                    else image.title.substringBeforeLast("."),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(5.dp))

                ImageMetadataChips(image, settings)
            }
        }
    }
}
