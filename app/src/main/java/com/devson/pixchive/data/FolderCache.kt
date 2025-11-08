package com.devson.pixchive.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class FolderCache(private val context: Context) {

    private val gson = Gson()
    private val cacheDir = File(context.cacheDir, "folder_cache")

    // In-memory cache
    private val memoryCache = mutableMapOf<String, CachedFolderData>()

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Get cached folder data (checks memory first, then disk)
     */
    fun getCachedData(folderId: String): CachedFolderData? {
        // Check memory cache first
        memoryCache[folderId]?.let { return it }

        // Check disk cache
        val cacheFile = File(cacheDir, "$folderId.json")
        if (cacheFile.exists()) {
            try {
                val json = cacheFile.readText()
                val data = gson.fromJson<CachedFolderData>(
                    json,
                    object : TypeToken<CachedFolderData>() {}.type
                )
                memoryCache[folderId] = data
                return data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

    /**
     * Save folder data to cache (memory + disk)
     */
    fun saveToCache(folderId: String, data: CachedFolderData) {
        // Save to memory
        memoryCache[folderId] = data

        // Save to disk
        try {
            val cacheFile = File(cacheDir, "$folderId.json")
            val json = gson.toJson(data)
            cacheFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clear cache for specific folder
     */
    fun clearCache(folderId: String) {
        memoryCache.remove(folderId)
        val cacheFile = File(cacheDir, "$folderId.json")
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }

    /**
     * Clear all cache
     */
    fun clearAllCache() {
        memoryCache.clear()
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Check if cache is valid (within 24 hours)
     */
    fun isCacheValid(folderId: String): Boolean {
        val data = getCachedData(folderId) ?: return false
        val ageInHours = (System.currentTimeMillis() - data.lastScanned) / (1000 * 60 * 60)
        return ageInHours < 24 // Cache valid for 24 hours
    }
}