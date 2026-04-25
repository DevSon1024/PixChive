package com.devson.pixchive.model

import com.devson.pixchive.data.local.ImageEntity

/**
 * Extension function to apply current sorting settings directly to a list of ImageEntities.
 */
fun List<ImageEntity>.applySort(
    sortField: SortField,
    sortDirection: SortDirection
): List<ImageEntity> {
    return when (sortField) {
        SortField.NAME -> if (sortDirection == SortDirection.ASCENDING) {
            this.sortedBy { it.name.lowercase() }
        } else {
            this.sortedByDescending { it.name.lowercase() }
        }
        SortField.DATE -> if (sortDirection == SortDirection.ASCENDING) {
            this.sortedBy { it.dateModified }
        } else {
            this.sortedByDescending { it.dateModified }
        }
        SortField.SIZE -> if (sortDirection == SortDirection.ASCENDING) {
            this.sortedBy { it.size }
        } else {
            this.sortedByDescending { it.size }
        }
        SortField.PATH -> if (sortDirection == SortDirection.ASCENDING) {
            this.sortedBy { it.path }
        } else {
            this.sortedByDescending { it.path }
        }
        SortField.TYPE -> if (sortDirection == SortDirection.ASCENDING) {
            this.sortedBy { it.name.substringAfterLast('.', "").lowercase() }
        } else {
            this.sortedByDescending { it.name.substringAfterLast('.', "").lowercase() }
        }
        SortField.RESOLUTION -> if (sortDirection == SortDirection.ASCENDING) {
            this.sortedBy { it.size } // Entity lacks resolution field, fallback to size
        } else {
            this.sortedByDescending { it.size }
        }
    }
}