package com.devson.pixchive.model

/**
 * Represents a video media item to be played.
 *
 * @param uri The URI of the video (can be local or remote).
 * @param title The title of the video.
 * @param duration The duration of the video in milliseconds.
 * @param size The size of the video in bytes.
 * @param folderName The folder/album name where the video resides (for local videos).
 */
data class Image(
    val uri: String,
    val title: String,
    val size: Long = 0L,
    val folderId: String = "",
    val folderName: String = "Unknown",
    val dateAdded: Long = 0L,
    val path: String = "",
    val resolution: String? = null
)

fun List<Image>.applySort(field: SortField, direction: SortDirection): List<Image> {
    return if (direction == SortDirection.ASCENDING) {
        when (field) {
            SortField.TITLE -> sortedBy { it.title.lowercase() }
            SortField.DATE -> sortedBy { it.dateAdded }
            SortField.SIZE -> sortedBy { it.size }
            SortField.RESOLUTION -> sortedBy { it.resolution.orEmpty() }
            SortField.PATH -> sortedBy { it.path.lowercase() }
        }
    } else {
        when (field) {
            SortField.TITLE -> sortedByDescending { it.title.lowercase() }
            SortField.DATE -> sortedByDescending { it.dateAdded }
            SortField.SIZE -> sortedByDescending { it.size }
            SortField.RESOLUTION -> sortedByDescending { it.resolution.orEmpty() }
            SortField.PATH -> sortedByDescending { it.path.lowercase() }
        }
    }
}
