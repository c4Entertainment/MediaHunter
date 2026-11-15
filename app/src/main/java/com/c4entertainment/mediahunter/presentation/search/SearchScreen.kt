package com.c4entertainment.mediahunter.presentation.search

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.components.Chip
import com.c4entertainment.mediahunter.components.LoadingListPlaceholders
import com.c4entertainment.mediahunter.components.LoadingPlaceholders
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.components.ErrorMessage
import com.c4entertainment.mediahunter.components.NoResultsMessage
import com.c4entertainment.mediahunter.components.movie_listing.view.GridContent
import com.c4entertainment.mediahunter.components.movie_listing.view.ListContent
import com.c4entertainment.mediahunter.domain.model.movie.Movie
import com.c4entertainment.mediahunter.presentation.movies.catalog.CatalogItemActionHost
import com.c4entertainment.mediahunter.presentation.movies.catalog.CatalogResultsGrid
import com.c4entertainment.mediahunter.presentation.movies.catalog.imageAspectRatio
import com.c4entertainment.mediahunter.presentation.movies.catalog.sourceName

/** One search box over every section; the chips pick where the query runs. */
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: UnifiedSearchViewModel,
) {
    val section by viewModel.section.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val catalogResult by viewModel.catalogResult.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val useGrid by viewModel.useGrid.collectAsStateWithLifecycle()
    val telegramBot by viewModel.telegramBot.collectAsStateWithLifecycle()
    val movies = viewModel.movieResults.collectAsLazyPagingItems()

    // The field keeps its own text so typing never waits on the ViewModel.
    var text by rememberSaveable { mutableStateOf(viewModel.initialQuery) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    CatalogItemActionHost(
        telegramBot = telegramBot,
        resolveKitsuId = viewModel::kitsuIdFor,
        loadArtistAlbums = viewModel::artistAlbums,
    ) { onCatalogItemClick ->
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // Edge to edge no longer resizes for the keyboard, so inset for it too.
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        ) {
            SearchField(
                text = text,
                onTextChange = {
                    text = it
                    viewModel.onQueryChange(it)
                },
                onSubmit = {
                    keyboard?.hide()
                    viewModel.onSubmit()
                },
                onBack = {
                    keyboard?.hide()
                    navController.popBackStack()
                },
                focusRequester = focusRequester,
            )
            SearchSectionChips(selected = section, onSelected = viewModel::onSectionSelected)

            Box(Modifier.weight(1f)) {
                when {
                    text.isBlank() -> RecentSearches(
                        queries = recentSearches,
                        onSelected = {
                            text = it
                            keyboard?.hide()
                            viewModel.onRecentSelected(it)
                        },
                        onClear = viewModel::clearRecentSearches,
                    )

                    // Typed, but the debounce hasn't started a search yet.
                    active.isBlank -> LoadingPlaceholders()

                    active.section.tmdbType != null -> MovieResults(
                        movies = movies,
                        sectionLabel = stringResource(active.section.labelRes),
                        useGrid = useGrid,
                        onClick = { id ->
                            navController.navigate("movie/${active.section.tmdbType}/$id")
                        },
                    )

                    else -> {
                        val catalog = active.section.catalog
                        when (val result = catalogResult) {
                            null, CatalogSearchResult.Loading -> LoadingPlaceholders(
                                aspectRatio = catalog?.imageAspectRatio ?: (2f / 3f),
                                minCellSize = if (catalog == CatalogSection.Games) 160.dp else 128.dp,
                            )
                            is CatalogSearchResult.Error -> ErrorMessage(onRetry = viewModel::retry)

                            is CatalogSearchResult.Success ->
                                if (result.items.isEmpty() || catalog == null) {
                                    NoResults(stringResource(active.section.labelRes))
                                } else {
                                    CatalogResultsGrid(
                                        section = catalog,
                                        items = result.items,
                                        caption = stringResource(
                                            R.string.search_results_caption,
                                            active.query,
                                            catalog.sourceName,
                                        ),
                                        onItemClick = { onCatalogItemClick(catalog, it) },
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.search_back),
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            shape = RoundedCornerShape(32.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { onTextChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.search_clear),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
    }
}

@Composable
private fun SearchSectionChips(
    selected: SearchSection,
    onSelected: (SearchSection) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SearchSection.entries.forEach { section ->
            Chip(
                selected = selected == section,
                onSelectedChange = { onSelected(section) },
            ) {
                Text(text = stringResource(section.labelRes))
            }
        }
    }
}

@Composable
private fun RecentSearches(
    queries: List<String>,
    onSelected: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (queries.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.search_recent_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.search_recent_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics { heading() },
                )
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.search_recent_clear))
                }
            }
        }
        items(queries, key = { it }) { query ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = stringResource(R.string.search_recent_item, query)) { onSelected(query) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Text(text = query, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MovieResults(
    movies: LazyPagingItems<Movie>,
    sectionLabel: String,
    useGrid: Boolean,
    onClick: (Int) -> Unit,
) {
    val refresh = movies.loadState.refresh
    when {
        movies.itemCount > 0 ->
            if (useGrid) GridContent(movies = movies, onNavigateToMovieDetail = onClick)
            else ListContent(movies = movies, onNavigateToMovieDetail = onClick)

        refresh is LoadState.Error -> ErrorMessage(onRetry = movies::retry)

        // Before the first load, paging reports NotLoading with no items;
        // only an exhausted source means there really are no results.
        movies.loadState.append.endOfPaginationReached -> NoResults(sectionLabel)
        else -> if (useGrid) LoadingPlaceholders() else LoadingListPlaceholders()
    }
}

@Composable
private fun NoResults(sectionLabel: String) {
    NoResultsMessage(hint = stringResource(R.string.search_no_results_hint, sectionLabel))
}
