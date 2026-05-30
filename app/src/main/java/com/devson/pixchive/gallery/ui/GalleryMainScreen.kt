package com.devson.pixchive.gallery.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.gallery.ui.components.CapsuleBottomNavigation
import com.devson.pixchive.gallery.viewmodel.AllImagesViewModel
import com.devson.pixchive.gallery.viewmodel.ImageListViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@Composable
fun GalleryMainScreen(
    onNavigateBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onFolderClick: (String) -> Unit,
    onImageClick: (String, Int) -> Unit,
    onSearch: (String) -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val lastTab by prefsManager.lastGalleryTabFlow.collectAsState(initial = null)
    var isInitialized by remember { mutableStateOf(false) }
    
    val tabs = listOf("Albums", "Photos")
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val scope = rememberCoroutineScope()

    // Sync pager state from preferences once
    LaunchedEffect(lastTab) {
        if (lastTab != null && !isInitialized) {
            if (lastTab != pagerState.currentPage) {
                pagerState.scrollToPage(lastTab!!)
            }
            isInitialized = true
        }
    }

    // Sync pager state with preferences only after initialization
    LaunchedEffect(pagerState.currentPage) {
        if (isInitialized) {
            prefsManager.setLastGalleryTab(pagerState.currentPage)
        }
    }

    // We need to know if either screen is in selection mode to hide the capsule nav
    // and let the screens' own selection bars show up.
    val allImagesViewModel: AllImagesViewModel = viewModel()
    val imageListViewModel: ImageListViewModel = viewModel()
    
    val allImagesSelection by allImagesViewModel.selectedIds.collectAsState()
    val imageListSelection by imageListViewModel.selectedIds.collectAsState()
    
    val isInSelectionMode = (pagerState.currentPage == 0 && imageListSelection.isNotEmpty()) ||
                            (pagerState.currentPage == 1 && allImagesSelection.isNotEmpty())

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isInSelectionMode
        ) { page ->
            when (page) {
                0 -> AlbumsScreen(
                    onNavigateBack = onNavigateBack,
                    onFolderClick = onFolderClick,
                    onSettingsClick = onSettingsClick,
                    onSwitchToPhotos = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    onRecycleBinClick = onRecycleBinClick,
                    onSearch = onSearch,
                    viewModel = imageListViewModel
                )
                1 -> PhotosScreen(
                    onNavigateBack = onNavigateBack,
                    onSettingsClick = onSettingsClick,
                    onSearch = onSearch,
                    onImageClick = { index -> onImageClick("all_images", index) },
                    onSwitchToAlbums = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    onRecycleBinClick = onRecycleBinClick,
                    viewModel = allImagesViewModel
                )
            }
        }

        // Capsule Bottom Nav
        if (!isInSelectionMode) {
            CapsuleBottomNavigation(
                tabs = tabs,
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}
