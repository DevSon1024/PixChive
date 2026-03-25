package com.devson.pixchive

import android.content.ContentResolver.SCHEME_CONTENT
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreImageThumbnailFetcher(
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val bitmap = options.context.contentResolver.loadThumbnail(
                    uri,
                    Size(512, 512),
                    null
                )

                DrawableResult(
                    drawable = BitmapDrawable(options.context.resources, bitmap),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isApplicable(data)) return null
            return MediaStoreImageThumbnailFetcher(data, options)
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == SCHEME_CONTENT &&
                data.authority == MediaStore.AUTHORITY &&
                data.pathSegments.any { it == "images" }
        }
    }
}
