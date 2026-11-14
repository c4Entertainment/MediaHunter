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
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.c4entertainment.mediahunter.components.movie_listing.MovieTVChip
import com.c4entertainment.mediahunter.components.movie_listing.PagingDataErrorWidget
import com.c4entertainment.mediahunter.components.movie_listing.PagingDataNoResultsWidget
import com.c4entertainment.mediahunter.components.movie_listing.view.GridContent
import com.c4entertainment.mediahunter.components.movie_listing.view.ListContent
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
    catalogSearchOpen: Boolean = false,
    onCatalogSearch: (query: String, immediate: Boolean) -> Unit = { _, _ -> },
    onCloseCatalogSearch: () -> Unit = {},
    musicChart: MusicChart = MusicChart(),
    onMusicChartChange: (MusicChart) -> Unit = {},
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
        )

        if (selectedSection != null) {
            CatalogContent(
                section = selectedSection,
                state = catalogState,
                onRefresh = onCatalogRefresh,
                onItemClick = onCatalogItemClick,
                searchOpen = catalogSearchOpen,
                onSearch = onCatalogSearch,
                onCloseSearch = onCloseCatalogSearch,
                musicChart = musicChart,
                onMusicChartChange = onMusicChartChange,
            )
            return@Column
        }

        Box(
            modifier = Modifier
                .fillMaxSize()

        ) {

            if (content.loadState.refresh is LoadState.Loading) {
//                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (content.itemCount == 0) {
                if (content.loadState.refresh is LoadState.Error) {
                    PagingDataErrorWidget(
                        retry = { content.refresh() },
                        errorMessage = (content.loadState.refresh as LoadState.Error).error.message
                    )
                } else {
                    PagingDataNoResultsWidget()
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