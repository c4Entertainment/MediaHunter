package com.c4entertainment.mediahunter.presentation.movie_info

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.prajwalch.torrentsearch.TorrentSearch
import com.prajwalch.torrentsearch.domain.model.SearchTarget

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailsScreen(
    id: String,
    type: String,
    viewModel: MovieDetailsViewModel,
    navigateBack: () -> Unit
) {

    val TAG = "MovieDetailsScreen"

    val state = viewModel.state
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current
    var showTvTorrentSheet by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState { viewModel.tabs.size }
    val scope = rememberCoroutineScope()
    // The target lets TorrentSearch look up by IMDb ID and hide releases of
    // other titles, years or episodes.
    val searchTorrents = { query: String, target: SearchTarget ->
        context.startActivity(TorrentSearch.searchIntent(context, query, target = target))
    }


    Log.d(TAG, "MovieDetailsScreen: ${state.movieDetails})")


    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MovieDetailsTopBar(
                title = state.movie?.title ?: "",
                navigateBack = navigateBack,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            val movie = state.movie
            // Details tab only, so it never covers the video player.
            if (movie != null && pagerState.currentPage == 0) {
                ExtendedFloatingActionButton(
                    text = { Text("Find torrents") },
                    icon = { Icon(Icons.Filled.Download, contentDescription = null) },
                    // Shrinks to the icon once scrolling hides the top bar.
                    expanded = scrollBehavior.state.collapsedFraction < 0.5f,
                    onClick = {
                        if (type == "tv") {
                            showTvTorrentSheet = true
                        } else {
                            searchTorrents(
                                torrentQuery(movie.title, movie.releaseDate.take(4)),
                                searchTarget(
                                    kind = SearchTarget.Kind.Movie,
                                    title = movie.title,
                                    originalTitle = movie.originalTitle,
                                    releaseDate = movie.releaseDate,
                                    imdbId = state.movieDetails?.imdbId,
                                ),
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->

        val movie = state.movie
        if (showTvTorrentSheet && movie != null) {
            TvTorrentSearchSheet(
                title = movie.title,
                seasons = state.tvSeasons,
                latestEpisode = state.latestEpisode,
                onSearch = { query, season, episode ->
                    showTvTorrentSheet = false
                    searchTorrents(
                        query,
                        searchTarget(
                            kind = SearchTarget.Kind.Series,
                            title = movie.title,
                            originalTitle = movie.originalTitle,
                            releaseDate = movie.releaseDate,
                            imdbId = state.movieDetails?.imdbId,
                            season = season,
                            episode = episode,
                        ),
                    )
                },
                onDismiss = { showTvTorrentSheet = false },
            )
        }


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                viewModel.tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    )
                }
            }

            val ytVideos =
                state.movieDetails?.videos?.results?.filter { videos -> videos.site == "YouTube" }?.reversed()

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> MovieDetails(
                        state = state,
                        isRefreshing = state.isLoading,
                        viewModel = viewModel,
                        id = id,
                        type = type,
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> MovieVideos(
                        modifier = Modifier.fillMaxSize(),
                        movieVideos = ytVideos,
                        navigateBack = navigateBack,
                    )
                }
            }
        }
    }
}

/** Describes the title being viewed so TorrentSearch can match releases to it exactly. */
private fun searchTarget(
    kind: SearchTarget.Kind,
    title: String,
    originalTitle: String,
    releaseDate: String,
    imdbId: String?,
    season: Int? = null,
    episode: Int? = null,
) = SearchTarget(
    kind = kind,
    title = title,
    alternativeTitles = listOf(originalTitle).filter { it.isNotBlank() && it != title },
    year = releaseDate.take(4).toIntOrNull(),
    imdbId = imdbId?.takeIf { it.isNotBlank() },
    season = season,
    episode = episode,
)
