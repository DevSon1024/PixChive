package com.devson.pixchive.ui.screens.imagelist.state

import com.devson.pixchive.model.Image
import com.devson.pixchive.model.ImageFolder
import com.devson.pixchive.model.ViewSettings

data class ImageListUiState(
    val imagesByFolder: Map<ImageFolder, List<Image>> = emptyMap(),
    val selectedImages: Set<Image> = emptySet(),
    val selectedFolders: Set<ImageFolder> = emptySet(),
    val selectedFolder: ImageFolder? = null,
    val viewSettings: ViewSettings = ViewSettings(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val explorerNodes: Pair<List<ImageFolder>, List<Image>> = Pair(emptyList(), emptyList()),
    val currentExplorerPath: String? = null,
    val searchText: String = "",
    val searchActive: Boolean = false,
    val searchSuggestions: List<String> = emptyList()
)