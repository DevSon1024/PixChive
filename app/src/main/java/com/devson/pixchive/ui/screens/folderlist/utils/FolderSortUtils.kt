package com.devson.pixchive.ui.screens.folderlist.utils

import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.ui.screens.folderlist.model.SortDirection
import com.devson.pixchive.ui.screens.folderlist.model.SortField

fun List<ComicFolder>.applyFolderSort(
    folderMap: Map<ComicFolder, List<ImageEntity>>,
    field: SortField,
    direction: SortDirection
): List<ComicFolder> {
    val sorted = when (field) {
        SortField.TITLE -> sortedBy { it.name.lowercase() }
        SortField.DATE  -> sortedBy { folder -> folderMap[folder]?.maxOfOrNull { it.dateModified } ?: 0L }
        SortField.SIZE  -> sortedBy { folder -> folderMap[folder]?.sumOf { it.size } ?: 0L }
        SortField.PATH  -> sortedBy { it.path.lowercase() }
        SortField.TYPE  -> sortedBy { it.name.lowercase() }
    }
    return if (direction == SortDirection.DESCENDING) sorted.reversed() else sorted
}
