package com.devson.pixchive.navigation

import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object About : Screen("about")
    object PrivacyPolicy : Screen("privacy")
    object AppearanceSettings : Screen("appearance_settings")

    object DeveloperOptions : Screen("developer_options")
    object Logs : Screen("logs")

    object FolderView : Screen("folder/{folderId}") {
        fun createRoute(folderId: String): String {
            return "folder/$folderId"
        }
    }

    object ChapterView : Screen("chapter/{folderId}/{chapterPath}") {
        fun createRoute(folderId: String, chapterPath: String): String {
            val encodedPath = URLEncoder.encode(chapterPath, StandardCharsets.UTF_8.toString())
            return "chapter/$folderId/$encodedPath"
        }
    }

    object ImageViewer : Screen("image/{folderId}/{chapterPath}/{imageIndex}") {
        fun createRoute(folderId: String, chapterPath: String, imageIndex: Int): String {
            val encodedPath = URLEncoder.encode(chapterPath, StandardCharsets.UTF_8.toString())
            return "image/$folderId/$encodedPath/$imageIndex"
        }
    }

    object ImageList : Screen("image_list")
    object ImageFolder : Screen("image_folder/{bucketId}") {
        fun createRoute(bucketId: String) = "image_folder/$bucketId"
    }
    object GalleryImageViewer : Screen("gallery_image_viewer/{bucketId}/{initialIndex}") {
        fun createRoute(bucketId: String, initialIndex: Int) = "gallery_image_viewer/$bucketId/$initialIndex"
    }
    object AllImages : Screen("all_images")
    object RecycleBin : Screen("recycle_bin")
}