package com.devson.pixchive.ui.screens.imagelist.state

import android.net.Uri
import com.devson.pixchive.model.Image

/**
 * Represents one-off UI events (like showing a Toast, navigating to a player)
 * that should be handled by the UI and then consumed.
 */
sealed interface ImageListUiEvent {
    data class ShowToast(val message: String) : ImageListUiEvent
    data class NavigateToReader(val folderId: String, val imageIndex: Int) : ImageListUiEvent
    data class ShareImages(val uris: List<Uri>) : ImageListUiEvent
    object NavigateUp : ImageListUiEvent
}
