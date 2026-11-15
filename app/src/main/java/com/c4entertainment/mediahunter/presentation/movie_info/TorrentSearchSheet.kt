package com.c4entertainment.mediahunter.presentation.movie_info

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.components.EpisodePicker
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Context
import com.c4entertainment.mediahunter.domain.model.movie_details.TitleDetails
import com.prajwalch.torrentsearch.TorrentSearch
import com.prajwalch.torrentsearch.domain.model.SearchTarget
import com.c4entertainment.mediahunter.domain.model.movie_details.TvEpisode
import com.c4entertainment.mediahunter.domain.model.movie_details.TvSeason

/** Builds a torrent search query: torrent names rarely keep punctuation like ":" or "'". */
fun torrentQuery(title: String, suffix: String = ""): String {
    val cleanTitle = title
        .replace("['’]".toRegex(), "")
        .replace("[^\\p{L}\\p{N}&]+".toRegex(), " ")
        .trim()
    return "$cleanTitle $suffix".trim()
}

private fun seasonTag(season: Int) = "S%02d".format(season)

private fun episodeTag(season: Int, episode: Int) = "S%02dE%02d".format(season, episode)

/**
 * Opens TorrentSearch for a movie or a whole show, or for one season ([episode] null) or one
 * episode of it. The target lets TorrentSearch look up by IMDb ID and hide releases of other
 * titles, years or episodes.
 */
fun Context.searchTorrents(details: TitleDetails, season: Int? = null, episode: Int? = null) {
    val query = when {
        !details.isTv -> torrentQuery(details.title, details.releaseDate.take(4))
        season == null -> torrentQuery(details.title)
        episode == null -> torrentQuery(details.title, seasonTag(season))
        else -> torrentQuery(details.title, episodeTag(season, episode))
    }
    val target = SearchTarget(
        kind = if (details.isTv) SearchTarget.Kind.Series else SearchTarget.Kind.Movie,
        title = details.title,
        alternativeTitles = listOf(details.originalTitle).filter { it.isNotBlank() && it != details.title },
        year = details.releaseDate.take(4).toIntOrNull(),
        imdbId = details.imdbId,
        season = season,
        episode = episode,
    )
    startActivity(TorrentSearch.searchIntent(this, query, target = target))
}

/** Lets the user search torrents for a whole show, one season, or one episode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvTorrentSearchSheet(
    title: String,
    seasons: List<TvSeason>,
    latestEpisode: TvEpisode?,
    /** [season] and [episode] are null for the whole series, [episode] for a whole season. */
    onSearch: (season: Int?, episode: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var expandedSeason by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.details_find_torrents),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { heading() }
        )

        LazyColumn {
            if (latestEpisode != null) {
                item {
                    val tag = episodeTag(latestEpisode.season, latestEpisode.episode)
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.tv_latest_episode)) },
                        supportingContent = {
                            Text(listOfNotNull(tag, latestEpisode.name, latestEpisode.airDate).joinToString(" · "))
                        },
                        modifier = Modifier.clickable {
                            onSearch(latestEpisode.season, latestEpisode.episode)
                        }
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.tv_complete_series)) },
                    supportingContent = { Text(torrentQuery(title)) },
                    modifier = Modifier.clickable { onSearch(null, null) }
                )
            }

            items(seasons, key = { it.number }) { season ->
                val isExpanded = expandedSeason == season.number

                ListItem(
                    headlineContent = { Text(season.name) },
                    supportingContent = { Text(pluralStringResource(R.plurals.episode_count, season.episodeCount, season.episodeCount)) },
                    trailingContent = {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = stringResource(if (isExpanded) R.string.tv_collapse else R.string.tv_expand)
                        )
                    },
                    modifier = Modifier.clickable {
                        expandedSeason = if (isExpanded) null else season.number
                    }
                )

                if (isExpanded) {
                    EpisodePicker(
                        count = season.episodeCount,
                        latest = latestEpisode?.takeIf { it.season == season.number }?.episode
                            ?: season.episodeCount,
                        leading = {
                            AssistChip(
                                onClick = {
                                    onSearch(season.number, null)
                                },
                                label = { Text(stringResource(R.string.tv_whole_season)) }
                            )
                        },
                    ) { episode ->
                        AssistChip(
                            onClick = {
                                onSearch(season.number, episode)
                            },
                            label = { Text(stringResource(R.string.episode_short, "%02d".format(episode))) }
                        )
                    }
                }
            }
        }
    }
}
