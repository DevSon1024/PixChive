package com.devson.pixchive.ui.screens.imagelist.state

import com.devson.pixchive.model.Image
import com.devson.pixchive.model.ImageFolder

sealed class ImageListUiAction {
    data class OnImageClick(val image: Image) : ImageListUiAction()
    data class OnImageLongClick(val image: Image) : ImageListUiAction()
    data class OnFolderClick(val folder: ImageFolder) : ImageListUiAction()
    data class OnFolderLongClick(val folder: ImageFolder) : ImageListUiAction()
    object OnSelectAll : ImageListUiAction()
    object OnClearSelection : ImageListUiAction()
    data class OnSearch(val query: String) : ImageListUiAction()
    data class OnSearchActiveChange(val active: Boolean) : ImageListUiAction()
    object OnBack : ImageListUiAction()
    data class OnMarkStatus(val status: String) : ImageListUiAction()
    object OnPlayAll : ImageListUiAction()
    object OnMove : ImageListUiAction()
    object OnCopy : ImageListUiAction()
    object OnDelete : ImageListUiAction()
    object OnRename : ImageListUiAction()
    object OnShare : ImageListUiAction()
    object OnShowInfo : ImageListUiAction()
    object OnShowSettings : ImageListUiAction()
}
