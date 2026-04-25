package com.devson.pixchive.model

enum class ViewMode {
    ALL_FOLDERS,
    FILES,
    FOLDERS
}

enum class LayoutMode {
    LIST,
    GRID
}

enum class SortField {
    NAME,
    DATE,
    SIZE
    // DURATION has been removed for images
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

data class ViewSettings(
    val viewMode: ViewMode = ViewMode.ALL_FOLDERS,
    val layoutMode: LayoutMode = LayoutMode.GRID,
    val gridColumns: Int = 3,
    val sortField: SortField = SortField.DATE,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    
    // File Scanning
    val showHiddenFiles: Boolean = false,
    val recognizeNoMedia: Boolean = false,
    val scanFoldersList: Set<String> = emptySet(),

    // UI Toggles (Adjusted for Images)
    val showThumbnail: Boolean = true,
    val showFileExtension: Boolean = true,
    val showResolution: Boolean = false, // Useful if you extract image resolution later
    val showPath: Boolean = false,
    val showSize: Boolean = true,
    val showDate: Boolean = true,
    
    // Floating Button Settings
    val showFloatingButton: Boolean = true,
    val enableFabPreview: Boolean = true
)