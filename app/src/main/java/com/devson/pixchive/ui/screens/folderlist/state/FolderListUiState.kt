package com.devson.pixchive.ui.screens.folderlist.state

import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.ui.screens.folderlist.model.ViewSettings

data class FolderListUiState(
    val imagesByFolder: Map<ComicFolder, List<ImageEntity>> = emptyMap(),
    val selectedImages: Set<ImageEntity> = emptySet(),
    val selectedFolders: Set<ComicFolder> = emptySet(),
    val selectedFolder: ComicFolder? = null,
    val viewSettings: ViewSettings = ViewSettings(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val explorerNodes: Pair<List<ComicFolder>, List<ImageEntity>> = Pair(emptyList(), emptyList()),
    val currentExplorerPath: String? = null,
    val searchText: String = "",
    val searchActive: Boolean = false,
    val searchSuggestions: List<String> = emptyList()
)
