package com.devson.pixchive.ui.screens.folderlist.folder

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.ui.screens.folderlist.model.ViewSettings
import com.devson.pixchive.ui.screens.folderlist.selection.SelectionCheckmarkOverlay
import com.devson.pixchive.ui.screens.folderlist.utils.formatDate
import com.devson.pixchive.ui.screens.folderlist.utils.formatSize

@Composable
fun FolderMediaPreview(
    folder: ComicFolder? = null,
    settings: ViewSettings,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.secondaryContainer
    val context = LocalContext.current
 
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (folder != null && settings.showThumbnail) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(folder.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.28f)
                        )
                    )
            )
        } else {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListItem(
    folder: ComicFolder,
    settings: ViewSettings,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "folderListBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(180),
        label = "folderListBorder"
    )
 
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
        border = BorderStroke(if (isSelected) 1.5.dp else 0.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.then(if (settings.selectByThumbnail) Modifier.clickable { onLongClick() } else Modifier)) {
                    FolderMediaPreview(
                        folder = folder,
                        settings = settings,
                        modifier = Modifier.size(width = 72.dp, height = 54.dp)
                    )
                }
 
                Spacer(modifier = Modifier.width(14.dp))
 
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FolderMetadataChips(folder, settings)
                }
            }
            SelectionCheckmarkOverlay(visible = isSelected)
        }
    }
}
 
@Composable
fun FolderMetadataChips(folder: ComicFolder, settings: ViewSettings, isGrid: Boolean = false) {
    val tokens = buildList {
        add(Pair("${folder.chapterCount} items", true))
        if (settings.showDate) {
            val oldest = folder.dateAdded
            if (oldest > 0) add(Pair(formatDate(oldest), false))
        }
    }.filter { it.first.isNotBlank() }
 
    if (tokens.isEmpty()) return
 
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tokens.take(if (isGrid) 2 else 3).forEach { (text, isPrimary) ->
            FolderMetaChip(text = text, isPrimary = isPrimary)
        }
    }
}
 
@Composable
private fun FolderMetaChip(text: String, isPrimary: Boolean) {
    val bgColor = if (isPrimary) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val textColor = if (isPrimary) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
 
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
fun FolderInfoDialog(
    selectedFolders: Set<ComicFolder>,
    imagesByFolder: Map<ComicFolder, List<ImageEntity>>,
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
        "Multiple Locations"
    }
    val oldestDate = if (selectedFolders.size == 1) {
        allImages.minOfOrNull { it.dateModified }
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        title = { Text(if (selectedFolders.size == 1) selectedFolders.first().name else "Selection Info", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(label = "Total Images", value = "$totalImages")
                InfoRow(label = "Total Size", value = formatSize(totalSize))
                InfoRow(label = "Location", value = location)
                if (oldestDate != null && oldestDate > 0L) {
                    InfoRow(label = "Modified Date", value = formatDate(oldestDate))
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
