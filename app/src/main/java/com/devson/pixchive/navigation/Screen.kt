package com.devson.pixchive.navigation

import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Home : Screen("home")

    object FolderView : Screen("folder/{folderId}/{viewMode}") {
        fun createRoute(folderId: String, viewMode: String): String {
            return "folder/$folderId/$viewMode"
        }
    }

    object ChapterView : Screen("chapter/{folderId}/{chapterPath}") {
        fun createRoute(folderId: String, chapterPath: String): String {
            val encodedPath = URLEncoder.encode(chapterPath, StandardCharsets.UTF_8.toString())
            return "chapter/$folderId/$encodedPath"
        }
    }

    object ImageViewer : Screen("image/{folderId}/{imageIndex}") {
        fun createRoute(folderId: String, imageIndex: Int): String {
            return "image/$folderId/$imageIndex"
        }
    }
}
