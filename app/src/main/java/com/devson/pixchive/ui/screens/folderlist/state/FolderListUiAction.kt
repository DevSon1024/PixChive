package com.devson.pixchive.ui.screens.folderlist.state

import com.devson.pixchive.model.Video
import com.devson.pixchive.model.VideoFolder

sealed class FolderListUiAction {
    data class OnVideoClick(val video: Video) : folderlistUiAction()
    data class OnVideoLongClick(val video: Video) : folderlistUiAction()
    data class OnFolderClick(val folder: VideoFolder) : folderlistUiAction()
    data class OnFolderLongClick(val folder: VideoFolder) : folderlistUiAction()
    object OnSelectAll : folderlistUiAction()
    object OnClearSelection : folderlistUiAction()
    data class OnSearch(val query: String) : folderlistUiAction()
    data class OnSearchActiveChange(val active: Boolean) : folderlistUiAction()
    object OnBack : folderlistUiAction()
    data class OnMarkStatus(val status: String) : folderlistUiAction()
    object OnPlayAll : folderlistUiAction()
    object OnMove : folderlistUiAction()
    object OnCopy : folderlistUiAction()
    object OnDelete : folderlistUiAction()
    object OnRename : folderlistUiAction()
    object OnShare : folderlistUiAction()
    object OnShowInfo : folderlistUiAction()
    object OnShowSettings : folderlistUiAction()
}
