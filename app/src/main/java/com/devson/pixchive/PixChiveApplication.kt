package com.devson.pixchive

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.devson.pixchive.core.data.local.AppDatabase
import com.devson.pixchive.core.utils.AppLogger
import com.devson.pixchive.core.data.MediaStoreImageThumbnailFetcher

class PixChiveApplication : Application(), ImageLoaderFactory {

    // Expose database instance
    lateinit var database: AppDatabase
        private set

   override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        database = AppDatabase.getDatabase(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(MediaStoreImageThumbnailFetcher.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    // 50% of available RAM — keeps decoded bitmaps warm across
                    // rapid scrolling and screen transitions. Raised from 40%
                    // to further reduce pager-reload stutter.
                    .maxSizePercent(0.50)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // 15% of free disk — large enough to keep tens of thousands
                    // of thumbnails cached across app restarts. Raised from 10%
                    // to reduce cold-start re-decode from storage.
                    .maxSizePercent(0.15)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .crossfade(false)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .allowHardware(true)
            .build()
    }
}