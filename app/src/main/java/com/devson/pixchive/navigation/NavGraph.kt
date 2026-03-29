package com.devson.pixchive.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devson.pixchive.ui.screens.AboutScreen
import com.devson.pixchive.ui.screens.PrivacyPolicyScreen
import com.devson.pixchive.ui.screens.ChapterViewScreen
import com.devson.pixchive.ui.screens.FolderViewScreen
import com.devson.pixchive.ui.screens.HomeScreen
import com.devson.pixchive.ui.screens.SettingsScreen
import com.devson.pixchive.ui.screens.FavoritesScreen
import com.devson.pixchive.ui.reader.ReaderScreen
import com.devson.pixchive.viewmodel.FolderViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    val folderViewModel: FolderViewModel = viewModel()

    val safeNavigateBack: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            // Slide in from right to left when opening a new screen
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            // Slide current screen to the left when opening a new screen
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            // Slide previous screen in from left to right when pressing back
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            // Slide current screen out left to right when pressing back
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onFolderClick = { folderId ->
                    navController.navigate(Screen.FolderView.createRoute(folderId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onFavoritesClick = {
                    navController.navigate("favorites")
                },
                onResumeChapter = { folderId, chapterPath, initialPage ->
                    navController.navigate(Screen.ImageViewer.createRoute(folderId, chapterPath, initialPage))
                }
            )
        }

        // Favorites Route - Now uses shared folderViewModel
        composable("favorites") {
            FavoritesScreen(
                onNavigateBack = safeNavigateBack, // Use safe pop
                onImageClick = { index ->
                    // Pass "favorites" as ID so Reader knows what to load
                    navController.navigate(Screen.ImageViewer.createRoute("favorites", "favorites_view", index))
                },
                viewModel = folderViewModel // PASS SHARED VIEWMODEL HERE
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = safeNavigateBack, // Use safe pop
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = safeNavigateBack // Use safe pop
            )
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onNavigateBack = safeNavigateBack // Use safe pop
            )
        }

        composable(
            route = Screen.FolderView.route,
            arguments = listOf(
                navArgument("folderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""

            FolderViewScreen(
                folderId = folderId,
                onNavigateBack = safeNavigateBack, // Use safe pop
                onChapterClick = { chapterPath ->
                    navController.navigate(Screen.ChapterView.createRoute(folderId, chapterPath))
                },
                onImageClick = { imageIndex ->
                    navController.navigate(Screen.ImageViewer.createRoute(folderId, "flat_view", imageIndex))
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
                onNavigateBack = safeNavigateBack, // Use safe pop
                onImageClick = { imageIndex ->
                    navController.navigate(Screen.ImageViewer.createRoute(folderId, chapterPath, imageIndex))
                },
                viewModel = folderViewModel
            )
        }

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

            ReaderScreen(
                folderId = folderId,
                chapterPath = chapterPath,
                initialIndex = imageIndex,
                onNavigateBack = safeNavigateBack, // Use safe pop
                viewModel = folderViewModel
            )
        }
    }
}