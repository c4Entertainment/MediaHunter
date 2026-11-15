package com.c4entertainment.mediahunter.presentation.movies.catalog

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import com.c4entertainment.mediahunter.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.catalog.AnimeRef
import com.c4entertainment.mediahunter.presentation.movie_info.torrentQuery
import com.prajwalch.torrentsearch.domain.model.SearchTarget

/** Lets the user search torrents for a whole anime or one episode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeTorrentSearchSheet(
    item: CatalogItem,
    anime: AnimeRef,
    /** [episode] is null for the whole series. */
    onSearch: (query: String, episode: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { heading() }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.anime_whole_series)) },
                supportingContent = { Text(stringResource(R.string.anime_whole_series_summary)) },
                modifier = Modifier.clickable { onSearch(torrentQuery(item.searchTerms), null) }
            )

            val episodes = anime.availableEpisodes ?: 0
            if (episodes > 0) {
                Text(
                    text = stringResource(R.string.anime_episodes),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics { heading() }
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    for (episode in 1..episodes) {
                        val number = "%02d".format(episode)
                        AssistChip(
                            // Fansub releases are named "Title - 05".
                            onClick = { onSearch(torrentQuery(item.searchTerms, number), episode) },
                            label = { Text(stringResource(R.string.episode_short, number)) }
                        )
                    }
                }
            }
        }
    }
}

/** Describes the anime so TorrentSearch can look it up on Torrentio and match releases exactly. */
fun AnimeRef.searchTarget(kitsuId: String?, episode: Int?) = SearchTarget(
    kind = SearchTarget.Kind.Anime,
    title = titles.first(),
    alternativeTitles = titles.drop(1),
    year = year,
    kitsuId = kitsuId,
    episode = episode,
)
