package com.c4entertainment.moviehunter.components

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
import com.c4entertainment.moviehunter.components.movie_listing.MovieTVChip
import com.c4entertainment.moviehunter.components.movie_listing.PagingDataErrorWidget
import com.c4entertainment.moviehunter.components.movie_listing.PagingDataNoResultsWidget
import com.c4entertainment.moviehunter.components.movie_listing.view.GridContent
import com.c4entertainment.moviehunter.components.movie_listing.view.ListContent
import com.c4entertainment.moviehunter.domain.model.movie.Movie

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
        )

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