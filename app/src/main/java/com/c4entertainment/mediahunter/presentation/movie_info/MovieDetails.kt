package com.c4entertainment.mediahunter.presentation.movie_info

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.c4entertainment.mediahunter.domain.model.movie_details.Cast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
fun MovieDetails(
    state: MovieDetailsState,
    isRefreshing: Boolean,
    viewModel: MovieDetailsViewModel,
    id: String,
    type: String,
    modifier: Modifier = Modifier
) {
    val TAG = "MovieDetails"
    val pullToRefreshState = rememberPullToRefreshState()

    Box(modifier) {
        if (state.error == null) {
            Column(modifier) {

                PullToRefreshBox(
                    modifier = modifier,
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.getMovie(id.toInt(), type) },
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
                    LazyColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        state.movie?.let { movie ->
                            item {
                                MovieDetailsPoster(movie = movie)
                            }

                            item {
                                HorizontalDivider(Modifier.padding(top = 8.dp, bottom = 16.dp))
                            }

                            item {
                                Text(
                                    text = movie.overview,
                                    Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                )
                            }

                            val rating: Int = (movie.voteAverage * 10).roundToInt()
                            if (rating > 0)
                                item {
                                    Text(
                                        text = "Rating: $rating %",
                                        Modifier.padding(
                                            start = 8.dp,
                                            end = 8.dp,
                                            bottom = 8.dp
                                        )
                                    )
                                }


                            var date = movie.releaseDate
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                                val d = LocalDate.parse(date)
                                val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
                                date = d.format(formatter)
                            }

                            item {
                                Text(
                                    text = "Release Date: $date",
                                    Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                )
                            }

                            state.movieDetails?.genres?.let { genres ->
                                item {
                                    Text(
                                        text = "Genres: ${genres.joinToString { it.name ?: "" }}",
                                        Modifier.padding(
                                            start = 8.dp,
                                            end = 8.dp,
                                            bottom = 8.dp
                                        )
                                    )
                                }
                            }
                        }

                        state.movieDetails?.credits?.cast?.let { cast ->

                            item {
                                Log.d(TAG, "MovieDetails: $cast")

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    items(
                                        count = cast.size,
                                        key = { index -> cast[index].id?: index},

                                    ) { index  ->
                                        ActorCard(actor = cast[index])
                                    }
                                }

                            }
                        }

                        item {
                            HorizontalDivider(Modifier.padding(top = 16.dp, bottom = 8.dp))
                        }
                    }

                }

            }

        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else if (state.error != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error
                    )
                    // TODO: Add retry button
//                Button(onClick = { // TODO:
//                     }) {
//                    Text(text = "Retry")
//                }
                }
            }
        }
    }
}

@Composable
fun ActorCard(actor: Cast) {
    val url = "https://www.google.com/search?q=${actor.name}"
    val launchResourceIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable(
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { context.startActivity(launchResourceIntent) }
            )
    ) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w300${actor.profilePath}",
                placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                error = painterResource(id = android.R.drawable.ic_menu_gallery),
                contentDescription = actor.name,
                modifier = Modifier
//                    .padding(4.dp)
                    .height(200.dp)
            )
        Text(
            text = actor.name ?: "",
        )
    }
}

