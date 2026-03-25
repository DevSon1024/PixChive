package com.devson.pixchive

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.devson.pixchive.data.local.AppDatabase

class PixChiveApplication : Application(), ImageLoaderFactory {

    // Expose database instance
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(MediaStoreImageThumbnailFetcher.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    // 40% of available RAM - keeps more decoded bitmaps warm
                    // so the Pager reload stutter is less noticeable even if
                    // a page is briefly evicted.
                    .maxSizePercent(0.40)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // 10% of free disk - large enough to keep thousands of
                    // thumbnails cached across sessions. 5% was too small and
                    // caused constant re-decode from storage.
                    .maxSizePercent(0.10)
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
