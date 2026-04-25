package com.devson.pixchive.model

/**
 * Represents an image media item.
 *
 * @param uri       The URI of the image (content:// or file://).
 * @param title     The display title / file name.
 * @param size      File size in bytes.
 * @param folderId  Folder path / MediaStore bucket ID.
 * @param folderName Folder / album display name.
 * @param dateAdded Date the file was added (Unix epoch seconds).
 * @param path      Absolute file-system path.
 * @param resolution  Optional WxH string, e.g. "1920x1080".
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
            SortField.NAME -> sortedBy { it.title.lowercase() }
            SortField.DATE -> sortedBy { it.dateAdded }
            SortField.SIZE -> sortedBy { it.size }
            SortField.RESOLUTION -> sortedBy { it.resolution }
            SortField.PATH -> sortedBy { it.path }
            SortField.TYPE -> sortedBy { it.title.substringAfterLast('.', "").lowercase() }
        }
    } else {
        when (field) {
            SortField.NAME -> sortedByDescending { it.title.lowercase() }
            SortField.DATE -> sortedByDescending { it.dateAdded }
            SortField.SIZE -> sortedByDescending { it.size }
            SortField.RESOLUTION -> sortedByDescending { it.resolution }
            SortField.PATH -> sortedByDescending { it.path }
            SortField.TYPE -> sortedByDescending { it.title.substringAfterLast('.', "").lowercase() }
        }
    }
}
