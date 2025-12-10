package com.devson.pixchive.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixchive_prefs")

class PreferencesManager(private val context: Context) {

    private val gson = Gson()

    companion object {
        private val FOLDERS_KEY = stringPreferencesKey("comic_folders")

        // Folder Screen Prefs
        private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")
        private val LAYOUT_MODE_KEY = stringPreferencesKey("layout_mode")
        private val FOLDER_GRID_COLUMNS_KEY = intPreferencesKey("folder_grid_columns") // NEW
        private val FOLDER_SORT_OPTION_KEY = stringPreferencesKey("folder_sort_option") // NEW

        // Home Screen Prefs
        private val HOME_LAYOUT_MODE_KEY = stringPreferencesKey("home_layout_mode")
        private val HOME_SORT_OPTION_KEY = stringPreferencesKey("home_sort_option")
        private val HOME_GRID_COLUMNS_KEY = intPreferencesKey("home_grid_columns") // NEW

        private val SHOW_HIDDEN_FILES_KEY = booleanPreferencesKey("show_hidden_files")
        private val IGNORED_PATHS_KEY = stringSetPreferencesKey("ignored_paths")
    }

    // --- Common / Folders Data ---

    suspend fun saveFolders(folders: List<ComicFolder>) {
        val json = gson.toJson(folders)
        context.dataStore.edit { preferences ->
            preferences[FOLDERS_KEY] = json
        }
    }

    val foldersFlow: Flow<List<ComicFolder>> = context.dataStore.data.map { preferences ->
        val json = preferences[FOLDERS_KEY] ?: ""
        if (json.isEmpty()) {
            emptyList<ComicFolder>()
        } else {
            try {
                val type = object : TypeToken<List<ComicFolder>>() {}.type
                gson.fromJson<List<ComicFolder>>(json, type) ?: emptyList<ComicFolder>()
            } catch (e: Exception) {
                emptyList<ComicFolder>()
            }
        }
    }.distinctUntilChanged()

    // --- Folder Screen Preferences ---

    suspend fun saveViewMode(mode: String) {
        context.dataStore.edit { preferences -> preferences[VIEW_MODE_KEY] = mode }
    }

    val viewModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[VIEW_MODE_KEY] ?: "explorer"
    }.distinctUntilChanged()

    suspend fun saveLayoutMode(mode: String) {
        context.dataStore.edit { preferences -> preferences[LAYOUT_MODE_KEY] = mode }
    }

    val layoutModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAYOUT_MODE_KEY] ?: "grid"
    }.distinctUntilChanged()

    suspend fun saveFolderGridColumns(columns: Int) {
        context.dataStore.edit { preferences -> preferences[FOLDER_GRID_COLUMNS_KEY] = columns }
    }

    val folderGridColumnsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[FOLDER_GRID_COLUMNS_KEY] ?: 3
    }.distinctUntilChanged()

    suspend fun saveFolderSortOption(option: String) {
        context.dataStore.edit { preferences -> preferences[FOLDER_SORT_OPTION_KEY] = option }
    }

    val folderSortOptionFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FOLDER_SORT_OPTION_KEY] ?: "name_asc"
    }.distinctUntilChanged()


    // --- Home Screen Preferences ---

    suspend fun saveHomeLayoutMode(mode: String) {
        context.dataStore.edit { preferences -> preferences[HOME_LAYOUT_MODE_KEY] = mode }
    }

    val homeLayoutModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HOME_LAYOUT_MODE_KEY] ?: "list"
    }.distinctUntilChanged()

    suspend fun saveHomeSortOption(option: String) {
        context.dataStore.edit { preferences -> preferences[HOME_SORT_OPTION_KEY] = option }
    }

    val homeSortOptionFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HOME_SORT_OPTION_KEY] ?: "date_newest"
    }.distinctUntilChanged()

    suspend fun saveHomeGridColumns(columns: Int) {
        context.dataStore.edit { preferences -> preferences[HOME_GRID_COLUMNS_KEY] = columns }
    }

    val homeGridColumnsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[HOME_GRID_COLUMNS_KEY] ?: 2
    }.distinctUntilChanged()

    // --- Global Settings ---

    suspend fun setShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { preferences -> preferences[SHOW_HIDDEN_FILES_KEY] = show }
    }

    val showHiddenFilesFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_HIDDEN_FILES_KEY] ?: false
    }.distinctUntilChanged()

    suspend fun addIgnoredPath(path: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[IGNORED_PATHS_KEY] ?: emptySet()
            preferences[IGNORED_PATHS_KEY] = current + path
        }
    }

    val ignoredPathsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[IGNORED_PATHS_KEY] ?: emptySet()
    }.distinctUntilChanged()
}