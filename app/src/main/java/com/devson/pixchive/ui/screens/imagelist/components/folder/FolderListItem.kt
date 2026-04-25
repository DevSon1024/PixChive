package com.devson.pixchive.ui.screens.imagelist.components.folder

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.pixchive.R
import com.devson.pixchive.model.Image
import com.devson.pixchive.model.ImageFolder
import com.devson.pixchive.model.ViewSettings
import com.devson.pixchive.ui.components.FolderShape
import com.devson.pixchive.ui.screens.imagelist.components.list.ImageThumbnail
import com.devson.pixchive.ui.screens.imagelist.components.selection.SelectionCheckmarkOverlay
import com.devson.pixchive.utils.formatDate
import com.devson.pixchive.utils.formatSize

@Composable
fun FolderMediaPreview(
    images: List<Image>,
    isSelected: Boolean,
    settings: ViewSettings,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.secondaryContainer
 
    Box(
        modifier = modifier
            .clip(FolderShape())
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (images.isNotEmpty() && settings.showThumbnail) {
            ImageThumbnail(
                uri = images.first().uri,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle bottom scrim so folder looks layered
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1.0f  to Color.Black.copy(alpha = 0.28f)
                        )
                    )
            )
        } else {
            // No thumbnail - show a tinted icon
            Icon(
                imageVector  = Icons.Filled.FolderOpen,
                contentDescription = null,
                tint         = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                modifier     = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListItem(
    folder: ImageFolder,
    images: List<Image>,
    settings: ViewSettings,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val bgColor by animateColorAsState(
        targetValue  = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "folderListBg"
    )
    val borderColor by animateColorAsState(
        targetValue  = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        animationSpec = tween(180),
        label = "folderListBorder"
    )
 
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 0.dp else 1.dp
        ),
        border = BorderStroke(if (isSelected) 1.5.dp else 0.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder thumbnail
                Box(modifier = Modifier.then(if (settings.selectByThumbnail) Modifier.clickable { onLongClick() } else Modifier)) {
                    FolderMediaPreview(
                        images   = images,
                        isSelected = false,
                        settings = settings,
                        modifier = Modifier.size(width = 72.dp, height = 54.dp)
                    )
                }
 
                Spacer(modifier = Modifier.width(14.dp))
 
                // Folder name + metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = folder.name,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        color      = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FolderMetadataChips(images, settings)
                }
            }
 
            // Animated checkmark in top-right
            SelectionCheckmarkOverlay(visible = isSelected)
        }
    }
}
 
@Composable
fun FolderMetadataChips(images: List<Image>, settings: ViewSettings, isGrid: Boolean = false) {
    val tokens = buildList {
        add(Pair(stringResource(R.string.folder_images_count, images.size), true))
        if (settings.showSize) {
            val totalSize = images.sumOf { it.size }
            add(Pair(formatSize(totalSize), false))
        }
        if (settings.showDate) {
            val oldest = images.minOfOrNull { it.dateAdded } ?: 0L
            if (oldest > 0) add(Pair(formatDate(oldest), false))
        }
    }.filter { it.first.isNotBlank() }
 
    if (tokens.isEmpty()) return
 
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        tokens.take(if (isGrid) 2 else 3).forEach { (text, isPrimary) ->
            FolderMetaChip(text = text, isPrimary = isPrimary)
        }
    }
}
 
@Composable
private fun FolderMetaChip(text: String, isPrimary: Boolean) {
    val bgColor   = if (isPrimary)
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
 
    val textColor = if (isPrimary)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant
 
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text       = text,
            style      = MaterialTheme.typography.labelSmall,
            fontSize   = 10.5.sp,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
            color      = textColor,
            maxLines   = 1
        )
    }
}

@Composable
fun FolderInfoDialog(
    selectedFolders: Set<ImageFolder>,
    imagesByFolder: Map<ImageFolder, List<Image>>,
    onDismiss: () -> Unit
) {
    val allImages = selectedFolders.flatMap { folder -> imagesByFolder[folder] ?: emptyList() }
    val totalImages = allImages.size
    val totalSize = allImages.sumOf { it.size }
    val location = if (selectedFolders.size == 1) {
        val firstImage = allImages.firstOrNull()
        if (firstImage != null && firstImage.path.contains("/")) {
            firstImage.path.substringBeforeLast("/")
        } else {
            selectedFolders.first().name
        }
    } else {
        stringResource(R.string.folder_info_multiple_locations)
    }
    val oldestDate = if (selectedFolders.size == 1) {
        allImages.minOfOrNull { it.dateAdded }
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        },
        title = {
            Text(
                if (selectedFolders.size == 1) selectedFolders.first().name else stringResource(R.string.folder_selection_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(label = stringResource(R.string.folder_info_total_videos), value = "$totalImages")
                InfoRow(label = stringResource(R.string.folder_info_total_size), value = formatSize(totalSize))
                InfoRow(label = stringResource(R.string.folder_info_location), value = location)
                if (oldestDate != null && oldestDate > 0L) {
                    InfoRow(label = stringResource(R.string.folder_info_creation_date), value = formatDate(oldestDate))
                }
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.5f)
        )
    }
}
