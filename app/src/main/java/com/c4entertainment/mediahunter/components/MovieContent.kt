package com.c4entertainment.mediahunter.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.data.network.apiKeyProblem
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.c4entertainment.mediahunter.components.movie_listing.MovieTVChip
import com.c4entertainment.mediahunter.components.movie_listing.view.GridContent
import com.c4entertainment.mediahunter.components.movie_listing.view.ListContent
import com.c4entertainment.mediahunter.domain.model.HomeSections
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.domain.model.catalog.MusicChart
import com.c4entertainment.mediahunter.domain.model.movie.Movie
import com.c4entertainment.mediahunter.presentation.movies.CatalogUiState
import com.c4entertainment.mediahunter.presentation.movies.catalog.CatalogContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieContent(
    modifier: Modifier,
    moviesOrTV: String,
    content: LazyPagingItems<Movie>,
    toggleMovie: () -> Unit,
    toggleTV: () -> Unit,
    useGrid: Boolean,
    onClickMovie: (Int) -> Unit,
    // Catalog sections (Music, Books...) are Home-only; leave these out elsewhere.
    selectedSection: CatalogSection? = null,
    catalogState: CatalogUiState = CatalogUiState(),
    onSectionSelected: ((CatalogSection) -> Unit)? = null,
    onCatalogRefresh: () -> Unit = {},
    onCatalogItemClick: (CatalogItem) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    musicChart: MusicChart = MusicChart(),
    onMusicChartChange: (MusicChart) -> Unit = {},
    /** Shown above the Movies/TV list, e.g. genre and year chips. */
    filters: (@Composable () -> Unit)? = null,
    sectionOrder: List<String> = HomeSections.defaultOrder,
) {
    val isRefreshing = content.loadState.refresh is LoadState.Loading
    val pullToRefreshState = rememberPullToRefreshState()

    Column(
        modifier = modifier
    ) {
        MovieTVChip(
            movieTV = moviesOrTV,
            toggleMovie = toggleMovie,
            toggleTV = toggleTV,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected,
            order = sectionOrder,
        )

        if (selectedSection != null) {
            CatalogContent(
                section = selectedSection,
                state = catalogState,
                onRefresh = onCatalogRefresh,
                onItemClick = onCatalogItemClick,
                onOpenSettings = onOpenSettings,
                musicChart = musicChart,
                onMusicChartChange = onMusicChartChange,
            )
            return@Column
        }

        filters?.invoke()

        Box(
            modifier = Modifier
                .fillMaxSize()

        ) {

            if (content.loadState.refresh is LoadState.Loading) {
                if (useGrid) LoadingPlaceholders() else LoadingListPlaceholders()
            } else if (content.itemCount == 0) {
                if (content.loadState.refresh is LoadState.Error) {
                    ErrorMessage(
                        onRetry = { content.refresh() },
                        body = tmdbKeyProblemBody((content.loadState.refresh as LoadState.Error).error.apiKeyProblem()),
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = { content.refresh() },
                    ) {
                        NoResultsMessage(hint = stringResource(R.string.state_no_results_pull_to_refresh))
                    }
                }
            } else {

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { content.refresh() },
                    state = pullToRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            modifier = Modifier.align(Alignment.TopCenter),
                            isRefreshing = isRefreshing,
                            state = pullToRefreshState,
                            containerColor = MaterialTheme.colorScheme.primary,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    },
                ) {

                    if (useGrid)
                        GridContent(
                            movies = content,
                            onNavigateToMovieDetail = { id ->
                                onClickMovie(id)
                            }
                        )
                    else
                        ListContent(
                            movies = content,
                            onNavigateToMovieDetail = { id ->
                                onClickMovie(id)
                            }
                        )
                }
            }
        }
    }
}