package com.devson.pixchive.ui.screens.imagelist.utils

import com.devson.pixchive.model.SortDirection
import com.devson.pixchive.model.SortField
import com.devson.pixchive.model.Image
import com.devson.pixchive.model.ImageFolder

fun List<ImageFolder>.applyFolderSort(
    folderMap: Map<ImageFolder, List<Image>>,
    field: SortField,
    direction: SortDirection
): List<ImageFolder> {
    val sorted = when (field) {
        SortField.NAME -> sortedBy { it.name.lowercase() }
        SortField.DATE -> sortedBy { folder -> folderMap[folder]?.maxOfOrNull { it.dateAdded } ?: 0L }
        SortField.SIZE -> sortedBy { folder -> folderMap[folder]?.sumOf { it.size } ?: 0L }
        else -> sortedBy { it.name.lowercase() }
    }
    return if (direction == SortDirection.DESCENDING) sorted.reversed() else sorted
}
