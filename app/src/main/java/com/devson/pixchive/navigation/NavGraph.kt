package com.devson.pixchive.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devson.pixchive.ui.screens.ChapterViewScreen
import com.devson.pixchive.ui.screens.FolderViewScreen
import com.devson.pixchive.ui.screens.HomeScreen
import com.devson.pixchive.ui.screens.ImageViewerScreen
import com.devson.pixchive.viewmodel.FolderViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    val folderViewModel: FolderViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onFolderClick = { folderId, viewMode ->
                    navController.navigate(Screen.FolderView.createRoute(folderId, viewMode))
                }
            )
        }

        composable(
            route = Screen.FolderView.route,
            arguments = listOf(
                navArgument("folderId") { type = NavType.StringType },
                navArgument("viewMode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
            val viewMode = backStackEntry.arguments?.getString("viewMode") ?: "explorer"

            FolderViewScreen(
                folderId = folderId,
                viewMode = viewMode,
                onNavigateBack = { navController.popBackStack() },
                onChapterClick = { chapterPath ->
                    navController.navigate(Screen.ChapterView.createRoute(folderId, chapterPath))
                },
                onImageClick = { imageIndex ->
                    // This won't be used from FolderView, only from ChapterView
                    navController.navigate(Screen.ImageViewer.createRoute(folderId, "", imageIndex))
                },
                viewModel = folderViewModel
            )
        }

        composable(
            route = Screen.ChapterView.route,
            arguments = listOf(
                navArgument("folderId") { type = NavType.StringType },
                navArgument("chapterPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
            val encodedPath = backStackEntry.arguments?.getString("chapterPath") ?: ""
            val chapterPath = try {
                URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
            } catch (e: Exception) {
                encodedPath
            }

            ChapterViewScreen(
                folderId = folderId,
                chapterPath = chapterPath,
                onNavigateBack = { navController.popBackStack() },
                onImageClick = { imageIndex ->
                    // Pass chapterPath to ImageViewer
                    navController.navigate(Screen.ImageViewer.createRoute(folderId, chapterPath, imageIndex))
                },
                viewModel = folderViewModel
            )
        }

        // Image Viewer Screen - NOW WITH CHAPTER PATH
        composable(
            route = Screen.ImageViewer.route,
            arguments = listOf(
                navArgument("folderId") { type = NavType.StringType },
                navArgument("chapterPath") { type = NavType.StringType },
                navArgument("imageIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
            val encodedPath = backStackEntry.arguments?.getString("chapterPath") ?: ""
            val chapterPath = try {
                URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
            } catch (e: Exception) {
                encodedPath
            }
            val imageIndex = backStackEntry.arguments?.getInt("imageIndex") ?: 0

            ImageViewerScreen(
                folderId = folderId,
                chapterPath = chapterPath,
                initialIndex = imageIndex,
                onNavigateBack = { navController.popBackStack() },
                viewModel = folderViewModel
            )
        }
    }
}
