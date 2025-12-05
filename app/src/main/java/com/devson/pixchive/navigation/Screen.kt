package com.devson.pixchive.navigation

import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Home : Screen("home")

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

    // UPDATED: Add chapterPath to ImageViewer
    object ImageViewer : Screen("image/{folderId}/{chapterPath}/{imageIndex}") {
        fun createRoute(folderId: String, chapterPath: String, imageIndex: Int): String {
            val encodedPath = URLEncoder.encode(chapterPath, StandardCharsets.UTF_8.toString())
            return "image/$folderId/$encodedPath/$imageIndex"
        }
    }
}