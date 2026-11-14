package com.c4entertainment.mediahunter.presentation.movie_info

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

/** Lets the user search torrents for a whole show, one season, or one episode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvTorrentSearchSheet(
    title: String,
    seasons: List<TvSeason>,
    latestEpisode: TvEpisode?,
    /** [season] and [episode] are null for the whole series, [episode] for a whole season. */
    onSearch: (query: String, season: Int?, episode: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var expandedSeason by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Find torrents",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn {
            if (latestEpisode != null) {
                item {
                    val tag = episodeTag(latestEpisode.season, latestEpisode.episode)
                    ListItem(
                        headlineContent = { Text("Latest episode") },
                        supportingContent = {
                            Text(listOfNotNull(tag, latestEpisode.name, latestEpisode.airDate).joinToString(" · "))
                        },
                        modifier = Modifier.clickable {
                            onSearch(torrentQuery(title, tag), latestEpisode.season, latestEpisode.episode)
                        }
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = { Text("Complete series") },
                    supportingContent = { Text(torrentQuery(title)) },
                    modifier = Modifier.clickable { onSearch(torrentQuery(title), null, null) }
                )
            }

            items(seasons, key = { it.number }) { season ->
                val isExpanded = expandedSeason == season.number

                ListItem(
                    headlineContent = { Text(season.name) },
                    supportingContent = { Text("${season.episodeCount} episodes") },
                    trailingContent = {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    },
                    modifier = Modifier.clickable {
                        expandedSeason = if (isExpanded) null else season.number
                    }
                )

                if (isExpanded) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                onSearch(torrentQuery(title, seasonTag(season.number)), season.number, null)
                            },
                            label = { Text("Whole season") }
                        )
                        for (episode in 1..season.episodeCount) {
                            AssistChip(
                                onClick = {
                                    onSearch(
                                        torrentQuery(title, episodeTag(season.number, episode)),
                                        season.number,
                                        episode,
                                    )
                                },
                                label = { Text("E%02d".format(episode)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
