package com.c4entertainment.mediahunter.presentation.movie_info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.components.ErrorMessage
import com.c4entertainment.mediahunter.domain.model.movie_details.TvEpisode
import com.c4entertainment.mediahunter.util.TmdbImage

/** One season of a show: its header, overview and every episode, each a torrent search away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonScreen(
    viewModel: SeasonViewModel,
    navigateBack: () -> Unit,
) {
    val state = viewModel.state
    val show = state.show
    val season = state.season
    val context = LocalContext.current
    val list = rememberLazyListState()
    val posterPath = season?.posterPath ?: show?.posterPath
    val seed = rememberSeedColor(posterPath?.let { TmdbImage.url(TmdbImage.GRID_POSTER, it) })
    val title = stringResource(R.string.season_title, viewModel.seasonNumber)
    val findTorrents: (() -> Unit)? = show?.let { { context.searchTorrents(it, viewModel.seasonNumber) } }

    DetailPageFrame(
        title = title,
        list = list,
        seed = seed,
        tmdbUrl = tmdbUrl("tv", viewModel.tvId) + "/season/${viewModel.seasonNumber}",
        onBack = navigateBack,
        onFindTorrents = findTorrents,
    ) {
        when {
            show != null && season != null -> {
                val refreshState = rememberPullToRefreshState()
                val barRoom = detailBarRoom()
                val meta = listOfNotNull(
                    season.airDate?.take(4)?.takeIf { it.length == 4 },
                    season.episodes.size.takeIf { it > 0 }?.let { episodeCountText(it) },
                ).takeIf { it.isNotEmpty() }?.joinToString(" · ")
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::load,
                    state = refreshState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = refreshState,
                            isRefreshing = state.isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = barRoom),
                        )
                    },
                ) {
                    LazyColumn(
                        state = list,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
                        ),
                    ) {
                        item(key = "header") {
                            DetailHeader(
                                title = title,
                                tagline = show.title,
                                meta = meta,
                                posterUrl = posterPath?.let { TmdbImage.url(TmdbImage.GRID_POSTER, it) },
                                backdropUrl = show.backdropPath?.let { TmdbImage.url(TmdbImage.BACKDROP_LARGE, it) },
                                seed = seed,
                                onFindTorrents = findTorrents ?: {},
                            )
                        }
                        season.overview?.let { overview ->
                            item(key = "overview") { DetailOverview(overview) }
                        }
                        if (season.episodes.isNotEmpty()) {
                            item(key = "episodesTitle") { DetailSectionTitle(stringResource(R.string.details_episodes)) }
                        }
                        items(season.episodes, key = { it.episode }) { episode ->
                            EpisodeRow(episode) { context.searchTorrents(show, episode.season, episode.episode) }
                        }
                    }
                }
            }

            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorMessage(onRetry = viewModel::load)
            }

            else -> DetailLoading()
        }
    }
}

private val StillShape = RoundedCornerShape(8.dp)

/** An episode as a list row: still, "3. Name", air date and runtime, two lines of overview. */
@Composable
private fun EpisodeRow(episode: TvEpisode, onClick: () -> Unit) {
    val title = stringResource(R.string.season_episode_title, episode.episode, episode.name ?: "")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.details_find_torrents), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(StillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = episode.stillPath?.let { TmdbImage.url("w300", it) },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                title.trimEnd(' ', '.'),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            listOfNotNull(formatDate(episode.airDate), episode.runtime?.let { runtimeText(it) })
                .takeIf { it.isNotEmpty() }
                ?.let {
                    Text(
                        it.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            episode.overview?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
