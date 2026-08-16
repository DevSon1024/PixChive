package com.devson.pixchive.navigation

import kotlinx.serialization.Serializable

/**
 * Base sealed interface for all type-safe navigation routes in PixChive.
 */
sealed interface PixChiveDestination

// Top-Level / Primary Shell Destinations
@Serializable
object HomeDestination : PixChiveDestination

@Serializable
object GalleryDestination : PixChiveDestination

@Serializable
object SettingsDestination : PixChiveDestination

// Comic Reader Domain Destinations
@Serializable
object ComicFlow : PixChiveDestination

@Serializable
data class FolderViewDestination(val folderId: String) : PixChiveDestination

@Serializable
data class ChapterViewDestination(
    val folderId: String,
    val chapterPath: String
) : PixChiveDestination

@Serializable
data class ImageViewerDestination(
    val folderId: String,
    val chapterPath: String,
    val imageIndex: Int = 0
) : PixChiveDestination

@Serializable
object FavoritesDestination : PixChiveDestination

// Gallery Domain Sub-Destinations
@Serializable
data class ImageFolderDestination(val bucketId: String) : PixChiveDestination

@Serializable
data class GalleryImageViewerDestination(
    val bucketId: String,
    val initialIndex: Int = 0
) : PixChiveDestination

@Serializable
data class SearchResultsDestination(val query: String) : PixChiveDestination

@Serializable
object RecycleBinDestination : PixChiveDestination

// Settings Domain Sub-Destinations
@Serializable
object AboutDestination : PixChiveDestination

@Serializable
object PrivacyPolicyDestination : PixChiveDestination

@Serializable
object AppearanceSettingsDestination : PixChiveDestination

@Serializable
object DeveloperOptionsDestination : PixChiveDestination

@Serializable
object LogsDestination : PixChiveDestination

@Serializable
object CustomHomeSettingsDestination : PixChiveDestination