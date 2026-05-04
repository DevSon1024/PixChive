package com.devson.pixchive.gallery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.gallery.data.MediaStoreRepository
import com.devson.pixchive.gallery.data.models.GalleryImage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _suggestions = MutableStateFlow<List<GalleryImage>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GalleryImage>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    init {
        _searchQuery
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    _suggestions.value = repository.getSearchSuggestions(query)
                } else {
                    _suggestions.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun performSearch(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = repository.searchImages(query)
            _isSearching.value = false
        }
    }
}
