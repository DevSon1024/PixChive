package com.devson.pixchive.ui.screens.folderlist.state

import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.local.ImageEntity

sealed class FolderListUiAction {
    data class OnImageClick(val image: ImageEntity) : FolderListUiAction()
    data class OnImageLongClick(val image: ImageEntity) : FolderListUiAction()
    data class OnFolderClick(val folder: ComicFolder) : FolderListUiAction()
    data class OnFolderLongClick(val folder: ComicFolder) : FolderListUiAction()
    object OnSelectAll : FolderListUiAction()
    object OnClearSelection : FolderListUiAction()
    data class OnSearch(val query: String) : FolderListUiAction()
    data class OnSearchActiveChange(val active: Boolean) : FolderListUiAction()
    object OnBack : FolderListUiAction()
    data class OnMarkStatus(val status: String) : FolderListUiAction()
    object OnViewAll : FolderListUiAction()
    object OnMove : FolderListUiAction()
    object OnCopy : FolderListUiAction()
    object OnDelete : FolderListUiAction()
    object OnRename : FolderListUiAction()
    object OnShare : FolderListUiAction()
    object OnShowInfo : FolderListUiAction()
    object OnShowSettings : FolderListUiAction()
}
