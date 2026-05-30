package com.devson.pixchive.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.devson.pixchive.ui.screens.settings.AboutScreen
import com.devson.pixchive.ui.screens.settings.PrivacyPolicyScreen
import com.devson.pixchive.ui.screens.ChapterViewScreen
import com.devson.pixchive.ui.screens.FolderViewScreen
import com.devson.pixchive.ui.screens.HomeScreen
import com.devson.pixchive.ui.screens.SettingsScreen
import com.devson.pixchive.ui.screens.FavoritesScreen
import com.devson.pixchive.ui.reader.ReaderScreen
import com.devson.pixchive.ui.screens.settings.DeveloperOptionsScreen
import com.devson.pixchive.ui.screens.settings.LogsScreen
import com.devson.pixchive.ui.screens.settings.AppearanceSettingsScreen
import com.devson.pixchive.viewmodel.FolderViewModel
import com.devson.pixchive.gallery.ui.ImageViewScreen
import com.devson.pixchive.gallery.ui.ImageFolderScreen
import com.devson.pixchive.ui.screens.settings.RecycleBinScreen
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: Any = HomeDestination
) {
    val safeNavigateBack: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            composable<HomeDestination> {
                HomeScreen(
                    onFolderClick = { folderId ->
                        navController.navigate(FolderViewDestination(folderId))
                    },
                    onSettingsClick = {
                        navController.navigate(SettingsDestination)
                    },
                    onFavoritesClick = {
                        navController.navigate(FavoritesDestination)
                    },
                    onResumeChapter = { folderId, chapterPath, initialPage ->
                        navController.navigate(
                            ImageViewerDestination(
                                folderId,
                                chapterPath,
                                initialPage
                            )
                        )
                    },
                    onBrowseGalleryClick = { _ ->
                        navController.navigate(GalleryDestination)
                    }
                )
            }

            composable<GalleryDestination> {
                com.devson.pixchive.gallery.ui.GalleryMainScreen(
                    onNavigateBack = safeNavigateBack,
                    onSettingsClick = { navController.navigate(SettingsDestination) },
                    onRecycleBinClick = { navController.navigate(RecycleBinDestination) },
                    onFolderClick = { bucketId ->
                        navController.navigate(ImageFolderDestination(bucketId))
                    },
                    onImageClick = { bucketId, index ->
                        navController.navigate(GalleryImageViewerDestination(bucketId, index))
                    },
                    onSearch = { query ->
                        navController.navigate(SearchResultsDestination(query))
                    }
                )
            }

            composable<SearchResultsDestination> { backStackEntry ->
                val destination: SearchResultsDestination = backStackEntry.toRoute()
                com.devson.pixchive.gallery.ui.SearchResultsScreen(
                    query = destination.query,
                    onBack = safeNavigateBack,
                    onImageClick = { index, _ ->
                        navController.navigate(GalleryImageViewerDestination("search:${destination.query}", index))
                    }
                )
            }

            composable<RecycleBinDestination> {
                RecycleBinScreen(onBack = safeNavigateBack)
            }

            composable<ImageFolderDestination> { backStackEntry ->
                val destination: ImageFolderDestination = backStackEntry.toRoute()
                ImageFolderScreen(
                    bucketId = destination.bucketId,
                    onNavigateBack = safeNavigateBack,
                    onImageClick = { index ->
                        navController.navigate(GalleryImageViewerDestination(destination.bucketId, index))
                    },
                    onSettingsClick = { navController.navigate(SettingsDestination) },
                    onSearch = { query ->
                        navController.navigate(SearchResultsDestination(query))
                    }
                )
            }

            composable<GalleryImageViewerDestination> { backStackEntry ->
                val destination: GalleryImageViewerDestination = backStackEntry.toRoute()
                ImageViewScreen(
                    bucketId = destination.bucketId,
                    initialIndex = destination.initialIndex,
                    onNavigateBack = safeNavigateBack
                )
            }

            composable<SettingsDestination> {
                SettingsScreen(
                    onNavigateBack = safeNavigateBack,
                    onNavigateToAbout = { navController.navigate(AboutDestination) },
                    onNavigateToPrivacyPolicy = { navController.navigate(PrivacyPolicyDestination) },
                    onNavigateToDeveloperOptions = { navController.navigate(DeveloperOptionsDestination) },
                    onNavigateToAppearance = { navController.navigate(AppearanceSettingsDestination) }
                )
            }

            composable<AppearanceSettingsDestination> {
                AppearanceSettingsScreen(
                    onNavigateBack = safeNavigateBack
                )
            }

            composable<DeveloperOptionsDestination> {
                DeveloperOptionsScreen(
                    onNavigateBack = safeNavigateBack,
                    onNavigateToLogs = { navController.navigate(LogsDestination) }
                )
            }

            composable<LogsDestination> {
                LogsScreen(onNavigateBack = safeNavigateBack)
            }

            composable<AboutDestination> {
                AboutScreen(
                    onNavigateBack = safeNavigateBack
                )
            }

            composable<PrivacyPolicyDestination> {
                PrivacyPolicyScreen(
                    onNavigateBack = safeNavigateBack
                )
            }

            navigation<ComicFlow>(
                startDestination = FolderViewDestination(folderId = "")
            ) {
                composable<FolderViewDestination> { backStackEntry ->
                    val destination: FolderViewDestination = backStackEntry.toRoute()
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<ComicFlow>()
                    }
                    val folderViewModel: FolderViewModel = viewModel(parentEntry)
                    FolderViewScreen(
                        folderId = destination.folderId,
                        onNavigateBack = safeNavigateBack,
                        onChapterClick = { chapterPath ->
                            navController.navigate(
                                ChapterViewDestination(destination.folderId, chapterPath)
                            )
                        },
                        onImageClick = { imageIndex ->
                            navController.navigate(
                                ImageViewerDestination(destination.folderId, "flat_view", imageIndex)
                            )
                        },
                        viewModel = folderViewModel
                    )
                }

                composable<ChapterViewDestination> { backStackEntry ->
                    val destination: ChapterViewDestination = backStackEntry.toRoute()
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<ComicFlow>()
                    }
                    val folderViewModel: FolderViewModel = viewModel(parentEntry)
                    ChapterViewScreen(
                        folderId = destination.folderId,
                        chapterPath = destination.chapterPath,
                        onNavigateBack = safeNavigateBack,
                        onImageClick = { imageIndex ->
                            navController.navigate(
                                ImageViewerDestination(destination.folderId, destination.chapterPath, imageIndex)
                            )
                        },
                        viewModel = folderViewModel
                    )
                }

                composable<ImageViewerDestination> { backStackEntry ->
                    val destination: ImageViewerDestination = backStackEntry.toRoute()
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<ComicFlow>()
                    }
                    val folderViewModel: FolderViewModel = viewModel(parentEntry)
                    ReaderScreen(
                        folderId = destination.folderId,
                        chapterPath = destination.chapterPath,
                        initialIndex = destination.imageIndex,
                        onNavigateBack = safeNavigateBack,
                        viewModel = folderViewModel
                    )
                }

                composable<FavoritesDestination> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<ComicFlow>()
                    }
                    val folderViewModel: FolderViewModel = viewModel(parentEntry)
                    FavoritesScreen(
                        onNavigateBack = safeNavigateBack,
                        onImageClick = { index ->
                            navController.navigate(
                                ImageViewerDestination("favorites", "favorites_view", index)
                            )
                        },
                        viewModel = folderViewModel
                    )
                }
            }
        }
    }
}