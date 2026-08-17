package com.devson.pixchive.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devson.pixchive.core.designsystem.component.CapsuleBottomBar
import com.devson.pixchive.core.designsystem.component.NavTabItem
import com.devson.pixchive.feature.gallery.viewmodel.AllImagesViewModel
import com.devson.pixchive.feature.gallery.viewmodel.ImageListViewModel

/**
 * Root App Shell for PixChive with responsive Scaffold and floating 4-tab capsule navigation.
 */
@Composable
fun PixChiveApp(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destination = navBackStackEntry?.destination

    // Observe image selection state to hide capsule navigation during selection mode
    val allImagesViewModel: AllImagesViewModel = viewModel()
    val imageListViewModel: ImageListViewModel = viewModel()
    val allImagesSelection by allImagesViewModel.selectedIds.collectAsState()
    val imageListSelection by imageListViewModel.selectedIds.collectAsState()

    val isSelectionActive = allImagesSelection.isNotEmpty() || imageListSelection.isNotEmpty()

    // Top-Level 4 Tabs definition
    val tabItems = remember {
        listOf(
            NavTabItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem("Gallery", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
            NavTabItem("Library", Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark),
            NavTabItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    // Determine if current screen is a top-level screen using type-safe route matching
    val selectedTabIndex = when {
        destination?.hasRoute<HomeDestination>() == true -> 0
        destination?.hasRoute<GalleryDestination>() == true -> 1
        destination?.hasRoute<LibraryDestination>() == true -> 2
        destination?.hasRoute<SettingsDestination>() == true -> 3
        else -> -1
    }

    // Capsule bottom bar is visible only on root tabs AND when multi-selection mode is inactive
    val showBottomBar = selectedTabIndex != -1 && !isSelectionActive

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CapsuleBottomBar(
                        items = tabItems,
                        selectedTabIndex = if (selectedTabIndex >= 0) selectedTabIndex else 0,
                        onTabSelected = { index ->
                            val targetDestination: PixChiveDestination = when (index) {
                                0 -> HomeDestination
                                1 -> GalleryDestination
                                2 -> LibraryDestination
                                3 -> SettingsDestination
                                else -> HomeDestination
                            }

                            if (selectedTabIndex != index) {
                                if (targetDestination == HomeDestination) {
                                    navController.popBackStack(
                                        navController.graph.findStartDestination().id,
                                        inclusive = false
                                    )
                                } else {
                                    navController.navigate(targetDestination) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { _ ->
        // NavHost occupies full screen for edge-to-edge drawing; screens handle their own insets
        NavGraph(
            navController = navController,
            modifier = Modifier.fillMaxSize()
        )
    }
}
