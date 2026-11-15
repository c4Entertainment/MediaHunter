package com.c4entertainment.mediahunter.presentation.movie_info

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.components.ErrorMessage
import com.c4entertainment.mediahunter.components.MediaCard
import com.c4entertainment.mediahunter.domain.model.movie_details.Release
import com.c4entertainment.mediahunter.domain.model.movie_details.TitleDetails
import com.c4entertainment.mediahunter.domain.model.movie_details.TvEpisode
import com.c4entertainment.mediahunter.util.TmdbImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun MovieDetailsScreen(
    id: String,
    type: String,
    viewModel: MovieDetailsViewModel,
    navigateBack: () -> Unit,
    openTitle: (type: String, id: Int) -> Unit,
    openSeason: (tvId: Int, season: Int) -> Unit,
) {
    val state = viewModel.state
    val details = state.details
    val context = LocalContext.current
    val list = rememberLazyListState()
    val seed = rememberSeedColor(details?.posterPath?.let { TmdbImage.url(TmdbImage.GRID_POSTER, it) })
    var showTvTorrentSheet by remember { mutableStateOf(false) }

    val findTorrents: (() -> Unit)? = details?.let {
        { if (it.isTv) showTvTorrentSheet = true else context.searchTorrents(it) }
    }

    DetailPageFrame(
        title = details?.title.orEmpty(),
        list = list,
        seed = seed,
        tmdbUrl = tmdbUrl(type, id.toIntOrNull() ?: 0),
        onBack = navigateBack,
        onFindTorrents = findTorrents,
    ) {
        when {
            details != null -> DetailsList(
                details = details,
                list = list,
                seed = seed,
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::load,
                viewModel = viewModel,
                onFindTorrents = findTorrents ?: {},
                openTitle = openTitle,
                openSeason = openSeason,
            )

            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorMessage(onRetry = viewModel::load)
            }

            else -> DetailLoading()
        }
    }

    if (showTvTorrentSheet && details != null) {
        TvTorrentSearchSheet(
            title = details.title,
            seasons = details.seasons,
            latestEpisode = details.lastEpisode,
            onSearch = { season, episode ->
                showTvTorrentSheet = false
                context.searchTorrents(details, season, episode)
            },
            onDismiss = { showTvTorrentSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsList(
    details: TitleDetails,
    list: LazyListState,
    seed: androidx.compose.ui.graphics.Color?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    viewModel: MovieDetailsViewModel,
    onFindTorrents: () -> Unit,
    openTitle: (type: String, id: Int) -> Unit,
    openSeason: (tvId: Int, season: Int) -> Unit,
) {
    val context = LocalContext.current
    val refreshState = rememberPullToRefreshState()
    val barRoom = detailBarRoom()
    val facts = buildList {
        addAll(releaseFacts(details))
        val directorLabel = if (details.isTv) R.string.details_created_by else R.string.details_director
        details.directors.takeIf { it.isNotEmpty() }?.let { add(stringResource(directorLabel) to it.joinToString(", ")) }
        details.writers.takeIf { it.isNotEmpty() }?.let { add(stringResource(R.string.details_writers) to it.joinToString(", ")) }
        details.studios.takeIf { it.isNotEmpty() }?.let { add(stringResource(R.string.details_studios) to it.joinToString(", ")) }
    }
    val meta = metaLine(details)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = refreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = refreshState,
                isRefreshing = isRefreshing,
                // Below the floating bar, not under it.
                modifier = Modifier.align(Alignment.TopCenter).padding(top = barRoom),
            )
        },
    ) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp),
        ) {
            item(key = "header") {
                DetailHeader(
                    title = details.title,
                    tagline = details.tagline,
                    meta = meta,
                    posterUrl = details.posterPath?.let { TmdbImage.url(TmdbImage.GRID_POSTER, it) },
                    backdropUrl = details.backdropPath?.let { TmdbImage.url(TmdbImage.BACKDROP_LARGE, it) },
                    seed = seed,
                    onFindTorrents = onFindTorrents,
                )
            }
            details.overview?.let { overview ->
                item(key = "overview") { DetailOverview(overview) }
            }
            item(key = "facts") { DetailFacts(genres = details.genres, facts = facts) }

            if (details.isTv) {
                detailRow(
                    key = "next",
                    title = { stringResource(R.string.details_next_up) },
                    items = listOfNotNull(details.lastEpisode, details.nextEpisode).distinct(),
                    itemKey = { "${it.season}-${it.episode}" },
                ) { episode ->
                    EpisodeCard(episode, fallbackImage = details.backdropPath) {
                        context.searchTorrents(details, episode.season, episode.episode)
                    }
                }
                detailRow(
                    key = "seasons",
                    title = { stringResource(R.string.details_seasons) },
                    items = details.seasons,
                    itemKey = { it.number },
                ) { season ->
                    MediaCard(
                        title = stringResource(R.string.season_title, season.number),
                        imageUrl = (season.posterPath ?: details.posterPath)?.let { TmdbImage.url(TmdbImage.GRID_POSTER, it) },
                        aspectRatio = 2f / 3f,
                        subtitle = episodeCountText(season.episodeCount),
                        onClick = { openSeason(details.id, season.number) },
                        modifier = Modifier.width(PosterCardWidth),
                        reserveTextSpace = true,
                    )
                }
            }

            detailRow(
                key = "videos",
                title = { stringResource(R.string.details_videos) },
                items = details.videos.filter { !it.key.isNullOrBlank() },
                itemKey = { it.key.orEmpty() },
                header = viewModel.playingVideoId?.let { playing ->
                    {
                        VideoPlayerHeader(
                            videoId = playing,
                            name = details.videos.firstOrNull { it.key == playing }?.name.orEmpty(),
                            startSecond = { viewModel.playbackSecond },
                            autoplay = { viewModel.autoplay },
                            onStarted = { viewModel.autoplay = false },
                            onSecond = { viewModel.playbackSecond = it },
                            onClose = viewModel::stopPlaying,
                        )
                    }
                },
            ) { video ->
                val key = video.key.orEmpty()
                val playing = key == viewModel.playingVideoId
                Box(Modifier.width(LandscapeCardWidth)) {
                    MediaCard(
                        title = video.name.orEmpty(),
                        imageUrl = "https://img.youtube.com/vi/$key/hqdefault.jpg",
                        aspectRatio = 16f / 9f,
                        onClick = { viewModel.play(key) },
                        modifier = Modifier.fillMaxWidth(),
                        reserveTextSpace = true,
                    )
                    if (playing) PlayingBadge(Modifier.align(Alignment.TopStart).padding(10.dp))
                }
            }

            detailRow(
                key = "recommendations",
                title = { stringResource(R.string.details_more_like_this) },
                items = details.recommendations,
                itemKey = { "${it.type}-${it.id}" },
            ) { title ->
                MediaCard(
                    title = title.title,
                    imageUrl = title.posterPath?.let { TmdbImage.url(TmdbImage.GRID_POSTER, it) },
                    aspectRatio = 2f / 3f,
                    subtitle = title.year,
                    onClick = { openTitle(title.type, title.id) },
                    modifier = Modifier.width(PosterCardWidth),
                        reserveTextSpace = true,
                )
            }

            castRow(details.cast) { name ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(name)}")))
            }
        }
    }
}

/** An episode as a landscape card: "S1 E3 · Title" over its air date. */
@Composable
fun EpisodeCard(episode: TvEpisode, fallbackImage: String?, onClick: () -> Unit) {
    val tag = stringResource(R.string.details_episode_tag, episode.season, episode.episode)
    MediaCard(
        title = listOfNotNull(tag, episode.name).joinToString(" · "),
        imageUrl = (episode.stillPath ?: fallbackImage)?.let { TmdbImage.url(TmdbImage.LIST_BACKDROP, it) },
        aspectRatio = 16f / 9f,
        subtitle = formatDate(episode.airDate),
        onClick = onClick,
        modifier = Modifier.width(LandscapeCardWidth),
                        reserveTextSpace = true,
    )
}

/** "2024 · 2h 46m · PG-13 · ★ 8.2"; for a show "2010 – 2011 · 35m · ★ 6.2". */
@Composable
private fun metaLine(details: TitleDetails): String? {
    val year = details.releaseDate.take(4).takeIf { it.length == 4 }
    val years = if (details.isTv && year != null) {
        val last = details.lastAirDate?.take(4)
        when {
            details.inProduction -> "$year – "
            last != null && last != year -> "$year – $last"
            else -> year
        }
    } else {
        year
    }
    val parts = listOfNotNull(
        years,
        details.runtime?.let { runtimeText(it) },
        details.certification,
        details.voteAverage.takeIf { it > 0 }?.let { stringResource(R.string.details_rating, "%.1f".format(it)) },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
fun runtimeText(minutes: Int): String =
    if (minutes >= 60) stringResource(R.string.details_runtime_hm, minutes / 60, minutes % 60)
    else stringResource(R.string.details_runtime_m, minutes)

/** "Mar 4, 2019" from TMDB's yyyy-MM-dd, or null. */
fun formatDate(date: String?): String? =
    date?.let { runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }.getOrNull() }

@Composable
fun episodeCountText(count: Int) = pluralStringResource(R.plurals.episode_count, count, count)

/**
 * Status and release dates, first in the facts: for torrents the digital and
 * disc dates matter most, and an upcoming date says why nothing turns up yet.
 */
@Composable
private fun releaseFacts(details: TitleDetails): List<Pair<String, String>> = buildList {
    statusLabel(details.status)?.let { add(stringResource(R.string.details_status) to it) }
    if (details.isTv) {
        formatDate(details.releaseDate)?.let { add(stringResource(R.string.details_first_aired) to it) }
        val next = details.nextEpisode
        if (next != null) {
            val date = formatDate(next.airDate) ?: stringResource(R.string.details_date_tba)
            add(stringResource(R.string.details_next_episode) to
                stringResource(R.string.details_episode_on, next.season, next.episode, date))
        } else if (details.status in setOf("Ended", "Canceled")) {
            formatDate(details.lastAirDate)?.let { add(stringResource(R.string.details_last_aired) to it) }
        }
    } else if (details.releases.isNotEmpty()) {
        details.releases.forEach { release ->
            val label = when (release.kind) {
                Release.Kind.Theatrical -> R.string.details_release_cinemas
                Release.Kind.Digital -> R.string.details_release_digital
                Release.Kind.Physical -> R.string.details_release_disc
            }
            formatDate(release.date, markUpcoming = true)?.let { add(stringResource(label) to it) }
        }
    } else {
        formatDate(details.releaseDate, markUpcoming = true)?.let { add(stringResource(R.string.details_release) to it) }
    }
}

@Composable
private fun statusLabel(status: String?): String? = when (status) {
    null -> null
    "Released" -> stringResource(R.string.details_status_released)
    "Post Production" -> stringResource(R.string.details_status_post_production)
    "In Production" -> stringResource(R.string.details_status_in_production)
    "Planned" -> stringResource(R.string.details_status_planned)
    "Rumored" -> stringResource(R.string.details_status_rumored)
    "Canceled" -> stringResource(R.string.details_status_canceled)
    "Returning Series" -> stringResource(R.string.details_status_returning)
    "Ended" -> stringResource(R.string.details_status_ended)
    "Pilot" -> stringResource(R.string.details_status_pilot)
    else -> status
}

/** "17 Jul 2026" in the reader's locale; "… (upcoming)" for a future date when asked. */
@Composable
private fun formatDate(isoDate: String?, markUpcoming: Boolean = false): String? {
    val date = isoDate?.takeIf { it.length >= 10 }
        ?.let { runCatching { java.time.LocalDate.parse(it.take(10)) }.getOrNull() } ?: return null
    val text = date.format(
        java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
    )
    return if (markUpcoming && date.isAfter(java.time.LocalDate.now())) {
        stringResource(R.string.details_date_upcoming, text)
    } else text
}
