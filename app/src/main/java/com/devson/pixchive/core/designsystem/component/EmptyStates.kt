package com.devson.pixchive.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devson.pixchive.core.designsystem.theme.PixchiveTheme

/**
 * Universal Material 3 Empty State Component for PixChive.
 */
@Composable
fun PixChiveEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionButtonText: String? = null,
    actionButtonIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (actionButtonText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    if (actionButtonIcon != null) {
                        Icon(
                            imageVector = actionButtonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = actionButtonText,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            if (secondaryButtonText != null && onSecondaryActionClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onSecondaryActionClick) {
                    Text(text = secondaryButtonText)
                }
            }
        }
    }
}

@Composable
fun EmptyChaptersView(onImportClick: (() -> Unit)? = null) {
    PixChiveEmptyState(
        icon = Icons.AutoMirrored.Filled.MenuBook,
        title = "No Chapters Found",
        message = "This comic folder doesn't contain readable chapters yet.",
        actionButtonText = if (onImportClick != null) "Import Chapters" else null,
        actionButtonIcon = Icons.Default.Add,
        onActionClick = onImportClick
    )
}

@Composable
fun EmptyImagesView(onBrowseClick: (() -> Unit)? = null) {
    PixChiveEmptyState(
        icon = Icons.Default.ImageNotSupported,
        title = "No Images Found",
        message = "No images were discovered in this location.",
        actionButtonText = if (onBrowseClick != null) "Browse Other Folders" else null,
        actionButtonIcon = Icons.Default.FolderOpen,
        onActionClick = onBrowseClick
    )
}

@Composable
fun EmptyChapterImagesView(
    chapterName: String,
    totalChapters: Int
) {
    PixChiveEmptyState(
        icon = Icons.Default.Collections,
        title = "No Images in Chapter",
        message = "Chapter \"$chapterName\" does not contain any image files."
    )
}

@Composable
fun EmptyFavoritesView(onExploreClick: (() -> Unit)? = null) {
    PixChiveEmptyState(
        icon = Icons.Default.FavoriteBorder,
        title = "No Favorites Yet",
        message = "Tap the heart icon on any image or chapter to keep your top picks here.",
        actionButtonText = if (onExploreClick != null) "Explore Gallery" else null,
        actionButtonIcon = Icons.Default.Explore,
        onActionClick = onExploreClick
    )
}

@Composable
fun EmptyFoldersView(onAddFolderClick: (() -> Unit)? = null) {
    PixChiveEmptyState(
        icon = Icons.Default.CreateNewFolder,
        title = "No Folders Added",
        message = "Add local comic and image folders from your device to start reading.",
        actionButtonText = if (onAddFolderClick != null) "Add Folder" else null,
        actionButtonIcon = Icons.Default.Add,
        onActionClick = onAddFolderClick
    )
}

@Composable
fun EmptySearchResultsView(
    query: String,
    onClearQuery: (() -> Unit)? = null
) {
    PixChiveEmptyState(
        icon = Icons.Default.SearchOff,
        title = "No Results Found",
        message = "We couldn't find any images or chapters matching \"$query\".",
        actionButtonText = if (onClearQuery != null) "Clear Search" else null,
        actionButtonIcon = Icons.Default.Clear,
        onActionClick = onClearQuery
    )
}

@Composable
fun EmptyRecycleBinView() {
    PixChiveEmptyState(
        icon = Icons.Default.DeleteOutline,
        title = "Recycle Bin is Empty",
        message = "Deleted items are stored here temporarily before permanent removal."
    )
}

@Composable
fun PermissionRequiredView(onGrantPermission: () -> Unit) {
    PixChiveEmptyState(
        icon = Icons.Default.Lock,
        title = "Permission Required",
        message = "PixChive needs media storage permission to scan and display your gallery files.",
        actionButtonText = "Grant Permission",
        actionButtonIcon = Icons.Default.Security,
        onActionClick = onGrantPermission
    )
}

@Preview(name = "Empty Favorites Light", showBackground = true)
@Composable
private fun EmptyFavoritesPreview() {
    PixchiveTheme(forceDark = false) {
        EmptyFavoritesView(onExploreClick = {})
    }
}

@Preview(name = "Empty Chapters Dark", showBackground = true)
@Composable
private fun EmptyChaptersDarkPreview() {
    PixchiveTheme(forceDark = true) {
        EmptyChaptersView(onImportClick = {})
    }
}