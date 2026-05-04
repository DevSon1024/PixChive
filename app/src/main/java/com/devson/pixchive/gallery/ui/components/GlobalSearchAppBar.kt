package com.devson.pixchive.gallery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.devson.pixchive.gallery.data.models.GalleryImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchAppBar(
    title: String,
    searchQuery: String,
    suggestions: List<GalleryImage>,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    var isSearchActive by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    TopAppBar(
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search images...") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (searchQuery.isNotBlank()) {
                            onSearch(searchQuery)
                            isSearchActive = false
                            keyboardController?.hide()
                        }
                    }),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )
            } else {
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        navigationIcon = {
            if (isSearchActive) {
                IconButton(onClick = { 
                    isSearchActive = false
                    onQueryChange("")
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search")
                }
            } else if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else {
                IconButton(onClick = { isSearchActive = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        },
        actions = {
            if (!isSearchActive) {
                IconButton(onClick = { isSearchActive = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                actions()
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        )
    )

    if (isSearchActive && suggestions.isNotEmpty()) {
        SearchSuggestionsPopup(
            suggestions = suggestions,
            keyboard = keyboardController,
            onSuggestionClick = { 
                onQueryChange(it)
                onSearch(it)
                isSearchActive = false
            }
        )
    }
}

@Composable
fun SearchSuggestionsPopup(
    suggestions: List<GalleryImage>,
    keyboard: SoftwareKeyboardController?,
    topBarHeight: Dp = 85.dp,
    onSuggestionClick: (String) -> Unit
) {
    val density = LocalDensity.current
    val offsetPx = with(density) { topBarHeight.roundToPx() }
    Popup(
        offset = IntOffset(0, offsetPx),
        properties = PopupProperties(focusable = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            LazyColumn {
                items(suggestions) { image ->
                    ListItem(
                        headlineContent = {
                            Text(image.realPath.substringAfterLast('/'), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.clickable {
                            keyboard?.hide()
                            onSuggestionClick(image.realPath.substringAfterLast('/'))
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
