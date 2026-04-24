package com.devson.pixchive.ui.screens.folderlist.state

import android.net.Uri
import com.devson.pixchive.data.local.ImageEntity

sealed interface FolderListUiEvent {
    data class ShowToast(val message: String) : FolderListUiEvent
    data class NavigateToViewer(val image: ImageEntity, val playlist: List<ImageEntity>) : FolderListUiEvent
    data class ShareImages(val uris: List<Uri>) : FolderListUiEvent
    object NavigateUp : FolderListUiEvent
}
