package com.c4entertainment.mediahunter.presentation.movies.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.components.LoadingPlaceholders
import com.c4entertainment.mediahunter.components.MediaCard
import com.c4entertainment.mediahunter.components.ErrorMessage
import com.c4entertainment.mediahunter.components.NoResultsMessage
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.domain.model.catalog.MusicChart
import com.c4entertainment.mediahunter.domain.model.catalog.MusicGenre
import com.c4entertainment.mediahunter.domain.model.catalog.MusicRegion
import com.c4entertainment.mediahunter.presentation.movies.CatalogUiState
import com.prajwalch.torrentsearch.domain.model.Category

/**
 * The TorrentSearch category to search in when an item of this section is tapped.
 *
 * Music and books search everything: many of their torrents are filed as "Other"
 * or uncategorised, and few providers support those categories.
 */
val CatalogSection.torrentCategory: Category
    get() = when (this) {
        // Music keeps Knaben to music and skips movie/anime-only providers.
        CatalogSection.Albums, CatalogSection.Songs, CatalogSection.Artists -> Category.Music
        CatalogSection.Books -> Category.All
        CatalogSection.Games -> Category.Games
        CatalogSection.Anime -> Category.Anime
    }

/** Names the data source too, which RAWG's terms require; music also names its chart. */
@Composable
private fun CatalogSection.sourceLabel(chart: MusicChart): String = when (this) {
    CatalogSection.Albums, CatalogSection.Songs, CatalogSection.Artists -> {
        val scope = when {
            this == CatalogSection.Albums && chart.editorsPicks -> null
            chart.genre != null -> chart.genre.label
            else -> chart.region.label
        }
        if (scope == null) stringResource(R.string.catalog_caption_editors_picks)
        else stringResource(R.string.catalog_caption_top, stringResource(labelRes).lowercase(), scope)
    }
    CatalogSection.Books -> stringResource(R.string.catalog_caption_books)
    CatalogSection.Games -> stringResource(R.string.catalog_caption_games)
    CatalogSection.Anime -> stringResource(R.string.catalog_caption_anime)
}

/** The data source's name, for captions. */
val CatalogSection.sourceName: String
    get() = when (this) {
        CatalogSection.Albums, CatalogSection.Songs, CatalogSection.Artists -> "Deezer"
        CatalogSection.Books -> "Open Library"
        CatalogSection.Games -> "RAWG.io"
        CatalogSection.Anime -> "AniList"
    }

val CatalogSection.imageAspectRatio: Float
    get() = when (this) {
        CatalogSection.Albums, CatalogSection.Songs, CatalogSection.Artists -> 1f
        CatalogSection.Games -> 16f / 9f
        CatalogSection.Books, CatalogSection.Anime -> 2f / 3f
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogContent(
    section: CatalogSection,
    state: CatalogUiState,
    onRefresh: () -> Unit,
    onItemClick: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
    musicChart: MusicChart = MusicChart(),
    onMusicChartChange: (MusicChart) -> Unit = {},
) = Column(modifier) {
    // Above the grid so it stays on screen for empty and error states too.
    if (section.isMusic) {
        MusicChartFilters(section, musicChart, onMusicChartChange)
    }
    when {
        state.items.isEmpty() && state.isLoading -> LoadingPlaceholders(
            aspectRatio = section.imageAspectRatio,
            minCellSize = if (section == CatalogSection.Games) 160.dp else 128.dp,
        )

        state.items.isEmpty() && state.error != null ->
            ErrorMessage(onRetry = onRefresh)

        state.items.isEmpty() -> PullToRefreshBox(
            isRefreshing = false,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            NoResultsMessage(hint = stringResource(R.string.state_no_results_pull_to_refresh))
        }

        else -> PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            CatalogResultsGrid(
                section = section,
                items = state.items,
                caption = section.sourceLabel(musicChart),
                onItemClick = onItemClick,
            )
        }
    }
}

/** The catalog grid shared by Home and Search: a [caption] row above one tile per item. */
@Composable
fun CatalogResultsGrid(
    section: CatalogSection,
    items: List<CatalogItem>,
    caption: String,
    onItemClick: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = if (section == CatalogSection.Games) 160.dp else 128.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        items(items, key = { it.id }) { item ->
            MediaCard(
                title = item.title,
                subtitle = item.subtitle,
                imageUrl = item.imageUrl,
                aspectRatio = section.imageAspectRatio,
                onClick = { onItemClick(item) },
            )
        }
    }
}

/** Region and genre dropdowns, plus an Editor's picks toggle on Albums. */
@Composable
private fun MusicChartFilters(
    section: CatalogSection,
    chart: MusicChart,
    onChange: (MusicChart) -> Unit,
) {
    // Editor's picks is an Albums-only chart: it must not lock the other sections' filters.
    val editorsPicks = section == CatalogSection.Albums && chart.editorsPicks
    val regionEnabled = chart.genre == null && !editorsPicks
    val genreEnabled = !editorsPicks
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            var regionOpen by remember { mutableStateOf(false) }
            val regionChosen = chart.region != MusicRegion.Worldwide
            Box {
                FilterChip(
                    selected = regionChosen,
                    onClick = { regionOpen = true },
                    enabled = regionEnabled,
                    label = {
                        Text(
                            if (regionChosen) stringResource(R.string.music_region_value, chart.region.label)
                            else stringResource(R.string.music_region)
                        )
                    },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                )
                DropdownMenu(
                    expanded = regionOpen,
                    onDismissRequest = { regionOpen = false },
                    modifier = Modifier.heightIn(max = 400.dp),
                ) {
                    MusicRegion.entries.forEach { region ->
                        DropdownMenuItem(
                            text = { Text(region.label) },
                            onClick = {
                                regionOpen = false
                                onChange(chart.copy(region = region))
                            },
                        )
                    }
                }
            }

            var genreOpen by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = chart.genre != null,
                    onClick = { genreOpen = true },
                    enabled = genreEnabled,
                    label = {
                        Text(
                            chart.genre?.let { stringResource(R.string.music_genre_value, it.label) }
                                ?: stringResource(R.string.music_genre)
                        )
                    },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                )
                DropdownMenu(expanded = genreOpen, onDismissRequest = { genreOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.music_genre_all)) },
                        onClick = {
                            genreOpen = false
                            onChange(chart.copy(genre = null))
                        },
                    )
                    MusicGenre.entries.forEach { genre ->
                        DropdownMenuItem(
                            text = { Text(genre.label) },
                            onClick = {
                                genreOpen = false
                                onChange(chart.copy(genre = genre))
                            },
                        )
                    }
                }
            }

            if (section == CatalogSection.Albums) {
                FilterChip(
                    selected = editorsPicks,
                    onClick = { onChange(chart.copy(editorsPicks = !chart.editorsPicks)) },
                    label = { Text(stringResource(R.string.music_editors_picks)) },
                )
            }
        }
        // Say why a chip is greyed out instead of leaving it a mystery.
        val note = when {
            editorsPicks -> R.string.music_note_editors_picks
            !regionEnabled -> R.string.music_note_region_genre
            else -> null
        }
        if (note != null) {
            Text(
                text = stringResource(note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
        }
    }
}
