package com.devson.pixchive.ui.screens.imagelist.state

import android.net.Uri
import com.devson.pixchive.model.Video

/**
 * Represents one-off UI events (like showing a Toast, navigating to a player)
 * that should be handled by the UI and then consumed.
 */
sealed interface VideoListUiEvent {
    data class ShowToast(val message: String) : VideoListUiEvent
    data class NavigateToPlayer(val image: Video, val playlist: List<Video>, val startPosition: Long) : VideoListUiEvent
    data class ShareImages(val uris: List<Uri>) : VideoListUiEvent
    object NavigateUp : VideoListUiEvent
}
