package com.devson.pixchive.ui.screens.imagelist.state

import com.devson.pixchive.model.Video
import com.devson.pixchive.model.VideoFolder

sealed class VideoListUiAction {
    data class OnVideoClick(val image: Video) : VideoListUiAction()
    data class OnVideoLongClick(val image: Video) : VideoListUiAction()
    data class OnFolderClick(val folder: VideoFolder) : VideoListUiAction()
    data class OnFolderLongClick(val folder: VideoFolder) : VideoListUiAction()
    object OnSelectAll : VideoListUiAction()
    object OnClearSelection : VideoListUiAction()
    data class OnSearch(val query: String) : VideoListUiAction()
    data class OnSearchActiveChange(val active: Boolean) : VideoListUiAction()
    object OnBack : VideoListUiAction()
    data class OnMarkStatus(val status: String) : VideoListUiAction()
    object OnPlayAll : VideoListUiAction()
    object OnMove : VideoListUiAction()
    object OnCopy : VideoListUiAction()
    object OnDelete : VideoListUiAction()
    object OnRename : VideoListUiAction()
    object OnShare : VideoListUiAction()
    object OnShowInfo : VideoListUiAction()
    object OnShowSettings : VideoListUiAction()
}
