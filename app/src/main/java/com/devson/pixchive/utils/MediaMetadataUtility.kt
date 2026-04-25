package com.devson.pixchive.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import com.devson.pixchive.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Locale
import androidx.core.net.toUri

data class DetailedImageMetadata(
    val image: Image,
    val resolution: String,
)

@OptIn(UnstableApi::class)
suspend fun getImageMetadata(
    context: Context,
    image: Image,
    metadataDao: com.devson.pixchive.data.ImageMetadataDao
): DetailedVideoMetadata = coroutineScope {
    // Check Cache
    val cached = withContext(Dispatchers.IO) { metadataDao.getMetadata(image.uri) }
    if (cached != null) {
        val tracks = deserializeTracks(cached.tracksJson)
        return@coroutineScope DetailedVideoMetadata(
            image = image,
            resolution = cached.resolution,
        )
    }

    // Save to Cache
    withContext(Dispatchers.IO) {
        metadataDao.insert(
            com.devson.pixchive.data.CachedVideoMetadata(
                uri = video.uri,
                format = containerFormat,
                resolution = finalRes,
            )
        )
    }

    DetailedVideoMetadata(
        image = image.copy(uri = resolvedPath),
        resolution = finalRes,
    )
}