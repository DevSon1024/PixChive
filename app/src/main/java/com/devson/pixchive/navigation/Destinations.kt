package com.devson.pixchive.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeDestination

@Serializable
object ComicFlow

@Serializable
object SettingsDestination

@Serializable
object AboutDestination

@Serializable
object PrivacyPolicyDestination

@Serializable
object AppearanceSettingsDestination

@Serializable
object DeveloperOptionsDestination

@Serializable
object LogsDestination

@Serializable
data class FolderViewDestination(val folderId: String)

@Serializable
data class ChapterViewDestination(val folderId: String, val chapterPath: String)

@Serializable
data class ImageViewerDestination(val folderId: String, val chapterPath: String, val imageIndex: Int)

@Serializable
data class ImageFolderDestination(val bucketId: String)

@Serializable
data class GalleryImageViewerDestination(val bucketId: String, val initialIndex: Int)

@Serializable
object GalleryDestination

@Serializable
object RecycleBinDestination

@Serializable
data class SearchResultsDestination(val query: String)

@Serializable
object FavoritesDestination
