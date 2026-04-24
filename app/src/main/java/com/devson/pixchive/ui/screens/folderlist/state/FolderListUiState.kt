package com.devson.pixchive.ui.screens.folderlist.state

import com.devson.pixchive.model.Video
import com.devson.pixchive.model.VideoFolder
import com.devson.pixchive.model.ViewSettings

data class FolderListUiState(
    val videosByFolder: Map<VideoFolder, List<Video>> = emptyMap(),
    val selectedVideos: Set<Video> = emptySet(),
    val selectedFolders: Set<VideoFolder> = emptySet(),
    val selectedFolder: VideoFolder? = null,
    val viewSettings: ViewSettings = ViewSettings(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val explorerNodes: Pair<List<VideoFolder>, List<Video>> = Pair(emptyList(), emptyList()),
    val currentExplorerPath: String? = null,
    val searchText: String = "",
    val searchActive: Boolean = false,
    val searchSuggestions: List<String> = emptyList()
)
