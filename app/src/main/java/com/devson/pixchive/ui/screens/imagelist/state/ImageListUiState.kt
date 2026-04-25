package com.devson.pixchive.ui.screens.imagelist.state

import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.model.ViewSettings

data class ImageListUiState(
    // Grouping by parent folder path or Chapter name
    val imagesByFolder: Map<String, List<ImageEntity>> = emptyMap(),
    val selectedImages: Set<ImageEntity> = emptySet(),
    val selectedFolders: Set<String> = emptySet(),
    val selectedFolder: String? = null,
    val viewSettings: ViewSettings = ViewSettings(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val explorerNodes: Pair<List<String>, List<ImageEntity>> = Pair(emptyList(), emptyList()),
    val currentExplorerPath: String? = null,
    val searchText: String = "",
    val searchActive: Boolean = false,
    val searchSuggestions: List<String> = emptyList()
)