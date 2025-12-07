package com.devson.pixchive.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")
        private val LAYOUT_MODE_KEY = stringPreferencesKey("layout_mode")
        private val SHOW_HIDDEN_FILES_KEY = booleanPreferencesKey("show_hidden_files")
        private val IGNORED_PATHS_KEY = stringSetPreferencesKey("ignored_paths")
    }

    // Save folders
    suspend fun saveFolders(folders: List<ComicFolder>) {
        val json = gson.toJson(folders)
        context.dataStore.edit { preferences ->
            preferences[FOLDERS_KEY] = json
        }
    }

    // Get folders
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

    // Save view mode
    suspend fun saveViewMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_MODE_KEY] = mode
        }
    }

    val viewModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[VIEW_MODE_KEY] ?: "explorer"
    }.distinctUntilChanged()

    // Save layout mode
    suspend fun saveLayoutMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[LAYOUT_MODE_KEY] = mode
        }
    }

    val layoutModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAYOUT_MODE_KEY] ?: "grid"
    }.distinctUntilChanged()

    // Show Hidden Files
    suspend fun setShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_HIDDEN_FILES_KEY] = show
        }
    }

    val showHiddenFilesFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_HIDDEN_FILES_KEY] ?: false
    }.distinctUntilChanged()

    // Ignored Paths (Removed Folders)
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