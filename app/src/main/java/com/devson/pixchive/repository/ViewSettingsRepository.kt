package com.devson.pixchive.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.devson.pixchive.model.LayoutMode
import com.devson.pixchive.model.SortDirection
import com.devson.pixchive.model.SortField
import com.devson.pixchive.model.ViewMode
import com.devson.pixchive.model.ViewSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.viewSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "view_settings")

class ViewSettingsRepository(private val context: Context) {

    companion object {
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val LAYOUT_MODE = stringPreferencesKey("layout_mode")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val SHOW_THUMBNAIL = booleanPreferencesKey("show_thumbnail")
        val SHOW_FILE_EXTENSION = booleanPreferencesKey("show_file_extension")
        val SHOW_RESOLUTION = booleanPreferencesKey("show_resolution")
        val SHOW_PATH = booleanPreferencesKey("show_path")
        val SHOW_SIZE = booleanPreferencesKey("show_size")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        
        val SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")
        val RECOGNIZE_NOMEDIA = booleanPreferencesKey("recognize_nomedia")
        val SELECT_BY_THUMBNAIL = booleanPreferencesKey("select_by_thumbnail")
        val SCAN_FOLDERS_LIST = stringSetPreferencesKey("scan_folders_list")
    }

    val viewSettingsFlow: Flow<ViewSettings> = context.viewSettingsDataStore.data.map { preferences ->
        ViewSettings(
            viewMode = try { ViewMode.valueOf(preferences[VIEW_MODE] ?: ViewMode.ALL_FOLDERS.name) } catch (e: Exception) { ViewMode.ALL_FOLDERS },
            layoutMode = try { LayoutMode.valueOf(preferences[LAYOUT_MODE] ?: LayoutMode.LIST.name) } catch (e: Exception) { LayoutMode.LIST },
            gridColumns = preferences[GRID_COLUMNS] ?: 2,
            sortField = try { SortField.valueOf(preferences[SORT_FIELD] ?: SortField.NAME.name) } catch (e: Exception) { SortField.NAME },
            sortDirection = try { SortDirection.valueOf(preferences[SORT_DIRECTION] ?: SortDirection.ASCENDING.name) } catch (e: Exception) { SortDirection.ASCENDING },
            showThumbnail = preferences[SHOW_THUMBNAIL] ?: true,
            showFileExtension = preferences[SHOW_FILE_EXTENSION] ?: false,
            showResolution = preferences[SHOW_RESOLUTION] ?: false,
            showPath = preferences[SHOW_PATH] ?: false,
            showSize = preferences[SHOW_SIZE] ?: true,
            showDate = preferences[SHOW_DATE] ?: false,
            showHiddenFiles = preferences[SHOW_HIDDEN_FILES] ?: false,
            recognizeNoMedia = preferences[RECOGNIZE_NOMEDIA] ?: false,
            selectByThumbnail = preferences[SELECT_BY_THUMBNAIL] ?: false,
            scanFoldersList = preferences[SCAN_FOLDERS_LIST] ?: setOf("/storage", "/storage/emulated/0"),
        )
    }

    suspend fun updateViewMode(mode: ViewMode) {
        context.viewSettingsDataStore.edit { it[VIEW_MODE] = mode.name }
    }

    suspend fun updateLayoutMode(mode: LayoutMode) {
        context.viewSettingsDataStore.edit { it[LAYOUT_MODE] = mode.name }
    }

    suspend fun updateGridColumns(columns: Int) {
        context.viewSettingsDataStore.edit { it[GRID_COLUMNS] = columns }
    }

    suspend fun updateSortField(field: SortField) {
        context.viewSettingsDataStore.edit { it[SORT_FIELD] = field.name }
    }

    suspend fun updateSortDirection(direction: SortDirection) {
        context.viewSettingsDataStore.edit { it[SORT_DIRECTION] = direction.name }
    }

    suspend fun updateShowThumbnail(show: Boolean) {
        context.viewSettingsDataStore.edit { it[SHOW_THUMBNAIL] = show }
    }

    suspend fun updateShowFileExtension(show: Boolean) {
        context.viewSettingsDataStore.edit { it[SHOW_FILE_EXTENSION] = show }
    }

    suspend fun updateShowResolution(show: Boolean) {
        context.viewSettingsDataStore.edit { it[SHOW_RESOLUTION] = show }
    }

    suspend fun updateShowPath(show: Boolean) {
        context.viewSettingsDataStore.edit { it[SHOW_PATH] = show }
    }

    suspend fun updateShowSize(show: Boolean) {
        context.viewSettingsDataStore.edit { it[SHOW_SIZE] = show }
    }

    suspend fun updateShowDate(show: Boolean) {
        context.viewSettingsDataStore.edit { it[SHOW_DATE] = show }
    }

    suspend fun updateShowHiddenFiles(show: Boolean) {
        context.viewSettingsDataStore.edit { it[SHOW_HIDDEN_FILES] = show }
    }

    suspend fun updateRecognizeNoMedia(recognize: Boolean) {
        context.viewSettingsDataStore.edit { it[RECOGNIZE_NOMEDIA] = recognize }
    }

    suspend fun updateSelectByThumbnail(select: Boolean) {
        context.viewSettingsDataStore.edit { it[SELECT_BY_THUMBNAIL] = select }
    }

    suspend fun updateScanFoldersList(folders: Set<String>) {
        context.viewSettingsDataStore.edit { it[SCAN_FOLDERS_LIST] = folders }
    }
}
