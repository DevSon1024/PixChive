package com.devson.pixchive.ui.screens.folderlist.model

enum class LayoutMode { LIST, GRID }

enum class ViewMode { ALL_FOLDERS, FILES, FOLDERS }

enum class SortField { TITLE, DATE, SIZE, PATH, TYPE }

enum class SortDirection { ASCENDING, DESCENDING }

data class ViewSettings(
    val viewMode: ViewMode = ViewMode.ALL_FOLDERS,
    val layoutMode: LayoutMode = LayoutMode.GRID,
    val gridColumns: Int = 3,
    val showThumbnail: Boolean = true,
    val showSize: Boolean = true,
    val showDate: Boolean = false,
    val selectByThumbnail: Boolean = false,
    val sortField: SortField = SortField.TITLE,
    val sortDirection: SortDirection = SortDirection.ASCENDING
)
