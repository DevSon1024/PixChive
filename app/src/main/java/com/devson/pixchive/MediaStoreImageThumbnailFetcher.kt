package com.devson.pixchive

import android.content.ContentResolver.SCHEME_CONTENT
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import coil.size.pxOrElse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreImageThumbnailFetcher(
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        return withContext(Dispatchers.IO) {
            try {
                val targetW = options.size.width.pxOrElse { 256 }.coerceAtMost(512)
                val targetH = options.size.height.pxOrElse { 256 }.coerceAtMost(512)

                val raw = options.context.contentResolver.loadThumbnail(
                    uri,
                    Size(targetW, targetH),
                    null
                )

                // Downsample if the returned bitmap is larger than target to avoid excess memory
                val bitmap = downsampleIfNeeded(raw, targetW, targetH)

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

    private fun downsampleIfNeeded(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= targetW && h <= targetH) return src

        val scale = minOf(targetW.toFloat() / w, targetH.toFloat() / h)
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)

        // Use RGB_565 to halve memory vs ARGB_8888
        val scaled = Bitmap.createScaledBitmap(src, newW, newH, true)
        if (scaled !== src) src.recycle()
        return if (scaled.config == Bitmap.Config.RGB_565) scaled
        else {
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
            // Convert config in-place by copying
            val converted = scaled.copy(Bitmap.Config.RGB_565, false)
            scaled.recycle()
            converted ?: src
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
