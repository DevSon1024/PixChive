package com.devson.pixchive.ui.screens.folderlist.state

import android.net.Uri
import com.devson.pixchive.model.Video

/**
 * Represents one-off UI events (like showing a Toast, navigating to a player)
 * that should be handled by the UI and then consumed.
 */
sealed interface FolderListUiEvent {
    data class ShowToast(val message: String) : folderlistUiEvent
    data class NavigateToPlayer(val video: Video, val playlist: List<Video>, val startPosition: Long) : folderlistUiEvent
    data class ShareVideos(val uris: List<Uri>) : folderlistUiEvent
    object NavigateUp : folderlistUiEvent
}
