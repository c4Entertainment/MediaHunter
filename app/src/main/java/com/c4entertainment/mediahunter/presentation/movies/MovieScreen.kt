package com.c4entertainment.mediahunter.presentation.movies

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.c4entertainment.mediahunter.MainActivityUiState
import com.c4entertainment.mediahunter.components.MovieContent
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.presentation.movies.catalog.AnimeTorrentSearchSheet
import com.c4entertainment.mediahunter.presentation.movies.catalog.searchTarget
import com.prajwalch.torrentsearch.domain.model.Category
import kotlinx.coroutines.launch
import com.c4entertainment.mediahunter.domain.model.movie.Movie
import com.c4entertainment.mediahunter.presentation.movie_info.torrentQuery
import com.c4entertainment.mediahunter.presentation.movies.catalog.torrentCategory
import com.c4entertainment.mediahunter.navigation.Screen
import com.c4entertainment.mediahunter.util.MovieTVFilterConfig
import com.prajwalch.torrentsearch.TorrentSearch


private const val TAG = "MovieScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieScreen(
    navController: NavController,
    viewModel: MovieViewModel,
    showSettingsDialog: () -> Unit,
) {

    val settingsState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val movies = viewModel.searchedMovies.collectAsLazyPagingItems()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val useGrid = shouldUseGrid(settingsState)
    val movieTV = shouldShowMovieOrTV(settingsState)
    val selectedSection by viewModel.selectedSection.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()

    MovieScreen(
        movies = movies,
        scrollBehavior = scrollBehavior,
        movieEvent = viewModel::onMovieEvent,
        navigate = navController::navigate,
        showSettingsDialog = showSettingsDialog,
        useGrid = useGrid,
        movieTV = movieTV,
        selectedSection = selectedSection,
        catalogState = catalogState,
        resolveKitsuId = viewModel::kitsuIdFor,
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieScreen(
    movies: LazyPagingItems<Movie>,
    scrollBehavior: TopAppBarScrollBehavior,
    movieEvent: (MovieEvent) -> Unit,
    navigate: (String) -> Unit,
    showSettingsDialog: () -> Unit,
    useGrid: Boolean,
    movieTV: String,
    selectedSection: CatalogSection?,
    catalogState: CatalogUiState,
    resolveKitsuId: suspend (aniListId: Long) -> String?,
) {

    val context = LocalContext.current
    // Searching a catalog section (Music, Books...) happens in place on Home.
    var catalogSearchOpen by rememberSaveable { mutableStateOf(false) }
    val closeCatalogSearch = {
        catalogSearchOpen = false
        movieEvent(MovieEvent.OnCatalogSearch(""))
    }
    BackHandler(enabled = catalogSearchOpen && selectedSection != null, onBack = closeCatalogSearch)
    // Anime opens an episode picker; the search then targets that exact episode.
    var animeSheetItem by remember { mutableStateOf<CatalogItem?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MovieScreenTopBar(isGridView = useGrid, toggleView = {
                movieEvent(MovieEvent.OnToggleView(!useGrid))
            }, onSettingsClicked = showSettingsDialog, onTorrentSearchClicked = {
                context.startActivity(TorrentSearch.homeIntent(context))
            }, onTorrentSettingsClicked = {
                context.startActivity(TorrentSearch.settingsIntent(context))
            }, onSearchClicked = {
                if (selectedSection != null) {
                    catalogSearchOpen = true
                    return@MovieScreenTopBar
                }
                movieEvent(MovieEvent.OnMovieTVBackupStored(movieTV))
                navigate(Screen.SearchScreen.route)
            }, scrollBehavior = scrollBehavior
            )
        },
    ) { paddingValues ->
        MovieContent(
            modifier = Modifier.padding(paddingValues),
            useGrid = useGrid,
            moviesOrTV = movieTV,
            content = movies,
            toggleMovie = {
                catalogSearchOpen = false
                movieEvent(MovieEvent.OnMovieTVToggled(MovieTVFilterConfig.MOVIE))
            },
            toggleTV = {
                catalogSearchOpen = false
                movieEvent(MovieEvent.OnMovieTVToggled(MovieTVFilterConfig.TV))
            },
            onClickMovie = { id -> navigate("movie/${movieTV}/${id}") },
            selectedSection = selectedSection,
            catalogState = catalogState,
            onSectionSelected = {
                catalogSearchOpen = false
                movieEvent(MovieEvent.OnCatalogSectionSelected(it))
            },
            onCatalogRefresh = { movieEvent(MovieEvent.OnCatalogRefresh) },
            catalogSearchOpen = catalogSearchOpen,
            onCatalogSearch = { query, immediate -> movieEvent(MovieEvent.OnCatalogSearch(query, immediate)) },
            onCloseCatalogSearch = closeCatalogSearch,
            onCatalogItemClick = { item ->
                val section = selectedSection ?: return@MovieContent
                if (item.anime != null) {
                    animeSheetItem = item
                    return@MovieContent
                }
                context.startActivity(
                    TorrentSearch.searchIntent(
                        context,
                        torrentQuery(item.searchTerms),
                        section.torrentCategory,
                    )
                )
            },
        )
    }

    val sheetItem = animeSheetItem
    val sheetAnime = sheetItem?.anime
    if (sheetItem != null && sheetAnime != null) {
        AnimeTorrentSearchSheet(
            item = sheetItem,
            anime = sheetAnime,
            onSearch = { query, episode ->
                animeSheetItem = null
                scope.launch {
                    // Without a Kitsu ID, Torrentio is skipped but the other
                    // providers still search and get filtered.
                    val kitsuId = resolveKitsuId(sheetAnime.aniListId)
                    context.startActivity(
                        TorrentSearch.searchIntent(
                            context,
                            query,
                            Category.Anime,
                            target = sheetAnime.searchTarget(kitsuId, episode),
                        )
                    )
                }
            },
            onDismiss = { animeSheetItem = null },
        )
    }

    LaunchedEffect(key1 = movies.loadState) {
        if (movies.loadState.refresh is LoadState.Error) {
            Log.e(
                TAG,
                "MovieScreen: Load Error" + (movies.loadState.refresh as LoadState.Error).error.message
            )
        }
    }

}

@Composable
private fun shouldUseGrid(
    uiState: SettingsUiState,
): Boolean = when (uiState) {
    SettingsUiState.Loading -> false
    is SettingsUiState.Success -> uiState.settings.useGrid
}

@Composable
private fun shouldShowMovieOrTV(
    uiState: SettingsUiState,
): String = when (uiState) {
    SettingsUiState.Loading -> MovieTVFilterConfig.MOVIE
    is SettingsUiState.Success -> uiState.settings.movieTV
}

