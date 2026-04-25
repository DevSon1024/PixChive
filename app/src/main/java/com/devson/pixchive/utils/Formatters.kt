package com.devson.pixchive.utils

import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.devson.pixchive.model.SortField
import com.devson.pixchive.model.Image
import com.devson.pixchive.model.ImageFolder
import com.devson.pixchive.model.ViewSettings
import com.devson.pixchive.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.log10
import kotlin.math.pow

fun formatSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(sizeBytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", sizeBytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

@Composable
fun formatSortField(field: SortField): String {
    return stringResource(getSortFieldStringRes(field))
}

fun formatDate(epochMs: Long): String {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return df.format(Date(epochMs))
}

fun getSortFieldStringRes(field: SortField): Int {
    return when (field) {
        SortField.NAME -> R.string.sort_field_title
        SortField.DATE -> R.string.sort_field_date
        SortField.SIZE -> R.string.sort_field_size
        SortField.RESOLUTION -> R.string.sort_field_resolution
        SortField.PATH -> R.string.sort_field_path
        SortField.TYPE -> R.string.sort_field_type
    }
}

/**
 * Converts a "WIDTHxHEIGHT" resolution string (e.g. "1280x720") into a compact
 * "Xp" label (e.g. "720p") using the smaller dimension - works for both landscape
 * and portrait videos. Returns null if the string can't be parsed.
 */
fun formatResolutionCompact(resolution: String?): String? {
    if (resolution.isNullOrBlank()) return null
    val parts = resolution.split("x", "X", "×")
    if (parts.size != 2) return null
    val w = parts[0].trim().toIntOrNull() ?: return null
    val h = parts[1].trim().toIntOrNull() ?: return null
    return "${minOf(w, h)}p"
}



fun formatLogTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Bottom App Bar Component
@Composable
fun SelectionBottomAppBar(
    selectedFolders: Set<ImageFolder>,
    imagesByFolder: Map<ImageFolder, List<Image>>,
    viewSettings: ViewSettings,
    onImageSelected: (Image, List<Image>) -> Unit,
    onClearSelection: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShowInfo: () -> Unit,
    onShare: () -> Unit,
) {

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Move
            ActionColumn(icon = Icons.AutoMirrored.Filled.DriveFileMove, label = stringResource(R.string.action_move), onClick = onMove)
            // Copy
            ActionColumn(icon = Icons.Filled.ContentCopy, label = stringResource(R.string.action_copy), onClick = onCopy)
            // Delete
            ActionColumn(icon = Icons.Filled.Delete, label = stringResource(R.string.action_delete), onClick = onDelete)

            // Rename
            if (selectedFolders.size == 1) {
                ActionColumn(icon = Icons.Filled.DriveFileRenameOutline, label = stringResource(R.string.action_rename), onClick = onRename)
            }

            // Share
            ActionColumn(icon = Icons.Filled.Share, label = stringResource(R.string.action_share), onClick = onShare)

            // Info
            ActionColumn(icon = Icons.Filled.Info, label = stringResource(R.string.action_info).substringBefore(" "), onClick = onShowInfo)

        }
    }
}

@Composable
private fun ActionColumn(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label)
        Text(label, fontSize = 10.sp)
    }
}