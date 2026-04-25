package com.devson.pixchive.ui.screens.imagelist.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.pixchive.model.Video
import com.devson.pixchive.model.ViewSettings
import com.devson.pixchive.util.formatDate
import com.devson.pixchive.util.formatDuration
import com.devson.pixchive.util.formatRelativeTime
import com.devson.pixchive.util.formatResolutionCompact
import com.devson.pixchive.util.formatSize

@Composable
fun VideoMetadataRow(
    image: Video,
    settings: ViewSettings,
    isGrid: Boolean = false,
    lastPositionMs: Long = 0L
) {
    VideoMetadataChips(image, settings, lastPositionMs, isGrid)
}
 
@Composable
fun VideoMetadataChips(
    image: Video,
    settings: ViewSettings,
    lastPositionMs: Long = 0L,
    isGrid: Boolean = false
) {
    data class MetaToken(val text: String, val isPrimary: Boolean = false)
 
    val tokens = buildList {
        if (settings.showLength && !settings.displayLengthOverThumbnail)
            add(MetaToken(formatDuration(image.duration), isPrimary = true))
        if (settings.showPlayedTime && image.lastPlayedAt != null && image.lastPlayedAt > 0)
            add(MetaToken(formatRelativeTime(LocalContext.current, image.lastPlayedAt)))
        if (settings.showResolution && !image.resolution.isNullOrEmpty())
            add(MetaToken(formatResolutionCompact(image.resolution) ?: image.resolution))
        if (settings.showFrameRate && image.frameRate != null && image.frameRate > 0f)
            add(MetaToken("${image.frameRate.toInt()} fps"))
        if (settings.showFileExtension)
            add(MetaToken(image.title.substringAfterLast('.', image.uri.substringAfterLast('.', "")).uppercase()))
        if (settings.showSize)
            add(MetaToken(formatSize(image.size)))
        if (settings.showDate && image.dateAdded > 0)
            add(MetaToken(formatDate(image.dateAdded)))
        if (settings.showPath)
            add(MetaToken(image.path))
    }.filter { it.text.isNotBlank() }
 
    if (tokens.isEmpty()) return
 
    Row(
        modifier          = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tokens.take(if (isGrid) 2 else 4).forEach { token ->
            MetadataChip(text = token.text, isPrimary = token.isPrimary, isGrid = isGrid)
        }
    }
}
 
@Composable
fun MetadataChip(text: String, isPrimary: Boolean, isGrid: Boolean = false) {
    val bgColor   = if (isPrimary)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
 
    val textColor = if (isPrimary)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant
 
    val fontSize = if (isGrid) 9.5.sp else 10.5.sp
 
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text       = text,
            style      = MaterialTheme.typography.labelSmall,
            fontSize   = fontSize,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
            color      = textColor,
            maxLines   = 1
        )
    }
}
