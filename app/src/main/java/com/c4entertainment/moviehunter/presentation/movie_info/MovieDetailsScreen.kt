package com.c4entertainment.moviehunter.presentation.movie_info

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.prajwalch.torrentsearch.TorrentSearch

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
    val searchTorrents = { query: String ->
        context.startActivity(TorrentSearch.searchIntent(context, query))
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
                onFindTorrents = state.movie?.let { movie ->
                    {
                        if (type == "tv") {
                            showTvTorrentSheet = true
                        } else {
                            searchTorrents(torrentQuery(movie.title, movie.releaseDate.take(4)))
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->

        val movie = state.movie
        if (showTvTorrentSheet && movie != null) {
            TvTorrentSearchSheet(
                title = movie.title,
                seasons = state.tvSeasons,
                onSearch = { query ->
                    showTvTorrentSheet = false
                    searchTorrents(query)
                },
                onDismiss = { showTvTorrentSheet = false },
            )
        }


        val tabIndex = viewModel.tabIndex
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = tabIndex.value!!) {
                viewModel.tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(title) },
                        selected = tabIndex.value!! == index,
                        onClick = { viewModel.updateTabIndex(index) },
                    )
                }
            }

            val ytVideos =
                state.movieDetails?.videos?.results?.filter { videos -> videos.site == "YouTube" }?.reversed()

            when (tabIndex.value) {
                0 -> MovieDetails(
                    state = state,
                    isRefreshing = state.isLoading,
                    viewModel = viewModel,
                    id = id,
                    type = type,
                    modifier = Modifier
                        .fillMaxSize()
                        .draggable(
                            state = viewModel.dragState.value!!,
                            orientation = Orientation.Horizontal,
                            onDragStarted = { },
                            onDragStopped = {
                                viewModel.updateTabIndexBasedOnSwipe()
                            }),
                )

                1 -> MovieVideos(
                    modifier = Modifier
                        .fillMaxSize()
                        .draggable(
                            state = viewModel.dragState.value!!,
                            orientation = Orientation.Horizontal,
                            onDragStarted = { },
                            onDragStopped = {
                                viewModel.updateTabIndexBasedOnSwipe()
                            }),
                    movieVideos = ytVideos,
                    navigateBack = navigateBack
                    )
            }
        }
    }
}



