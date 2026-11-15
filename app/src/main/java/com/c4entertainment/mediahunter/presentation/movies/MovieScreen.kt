package com.c4entertainment.mediahunter.presentation.movies

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistAlbum
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistRef
import com.c4entertainment.mediahunter.domain.model.catalog.MusicChart
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.domain.model.movie.Movie
import com.c4entertainment.mediahunter.navigation.Screen
import com.c4entertainment.mediahunter.presentation.movies.catalog.CatalogItemActionHost
import com.c4entertainment.mediahunter.presentation.search.SearchSection
import com.c4entertainment.mediahunter.util.MovieTVFilterConfig
import com.prajwalch.torrentsearch.TorrentSearch


private const val TAG = "MovieScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieScreen(
    navController: NavController,
    viewModel: MovieViewModel,
) {

    val settingsState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val movies = viewModel.searchedMovies.collectAsLazyPagingItems()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val useGrid = shouldUseGrid(settingsState)
    val movieTV = shouldShowMovieOrTV(settingsState)
    val selectedSection by viewModel.selectedSection.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
    val musicChart by viewModel.musicChart.collectAsStateWithLifecycle()
    val telegramBot by viewModel.telegramBot.collectAsStateWithLifecycle()

    MovieScreen(
        movies = movies,
        scrollBehavior = scrollBehavior,
        movieEvent = viewModel::onMovieEvent,
        navigate = navController::navigate,
        useGrid = useGrid,
        movieTV = movieTV,
        selectedSection = selectedSection,
        catalogState = catalogState,
        resolveKitsuId = viewModel::kitsuIdFor,
        musicChart = musicChart,
        telegramBot = telegramBot,
        loadArtistAlbums = viewModel::artistAlbums,
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieScreen(
    movies: LazyPagingItems<Movie>,
    scrollBehavior: TopAppBarScrollBehavior,
    movieEvent: (MovieEvent) -> Unit,
    navigate: (String) -> Unit,
    useGrid: Boolean,
    movieTV: String,
    selectedSection: CatalogSection?,
    catalogState: CatalogUiState,
    resolveKitsuId: suspend (aniListId: Long) -> String?,
    musicChart: MusicChart,
    telegramBot: String,
    loadArtistAlbums: suspend (ArtistRef) -> Result<List<ArtistAlbum>>,
) {

    val context = LocalContext.current

    CatalogItemActionHost(
        telegramBot = telegramBot,
        resolveKitsuId = resolveKitsuId,
        loadArtistAlbums = loadArtistAlbums,
    ) { onCatalogItemClick ->
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MovieScreenTopBar(isGridView = useGrid, showViewToggle = selectedSection == null, toggleView = {
                movieEvent(MovieEvent.OnToggleView(!useGrid))
            }, onSettingsClicked = {
                navigate(Screen.SettingsScreen.route)
            }, onTorrentSearchClicked = {
                context.startActivity(TorrentSearch.homeIntent(context))
            }, onSearchClicked = {
                // Search opens on whatever Home is showing.
                navigate(Screen.SearchScreen.createRoute(SearchSection.forHome(selectedSection, movieTV)))
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
                movieEvent(MovieEvent.OnMovieTVToggled(MovieTVFilterConfig.MOVIE))
            },
            toggleTV = {
                movieEvent(MovieEvent.OnMovieTVToggled(MovieTVFilterConfig.TV))
            },
            onClickMovie = { id -> navigate("movie/${movieTV}/${id}") },
            selectedSection = selectedSection,
            catalogState = catalogState,
            onSectionSelected = {
                movieEvent(MovieEvent.OnCatalogSectionSelected(it))
            },
            onCatalogRefresh = { movieEvent(MovieEvent.OnCatalogRefresh) },
            musicChart = musicChart,
            onMusicChartChange = { movieEvent(MovieEvent.OnMusicChartChanged(it)) },
            onCatalogItemClick = { item ->
                selectedSection?.let { onCatalogItemClick(it, item) }
            },
        )
    }
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

