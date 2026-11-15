package com.c4entertainment.mediahunter.presentation.movies.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistAlbum
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistRef
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.domain.model.external.ExternalApps
import com.c4entertainment.mediahunter.presentation.movie_info.torrentQuery
import com.prajwalch.torrentsearch.TorrentSearch
import com.prajwalch.torrentsearch.domain.model.Category
import kotlinx.coroutines.launch

/**
 * What tapping a catalog item does, shared by Home and Search: anime opens the episode picker,
 * artists their discography, sections with downloader apps the music sheet, and anything else
 * goes straight to a torrent search. Holds the sheets' state and draws them after [content].
 */
@Composable
fun CatalogItemActionHost(
    telegramBot: String,
    resolveKitsuId: suspend (aniListId: Long) -> String?,
    loadArtistAlbums: suspend (ArtistRef) -> Result<List<ArtistAlbum>>,
    content: @Composable (onItemClick: (CatalogSection, CatalogItem) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Anime opens an episode picker; the search then targets that exact episode.
    var animeSheetItem by remember { mutableStateOf<CatalogItem?>(null) }
    // Artists open a sheet with their discography and albums.
    var artistSheetItem by remember { mutableStateOf<CatalogItem?>(null) }
    var musicSheetItem by remember { mutableStateOf<Pair<CatalogSection, CatalogItem>?>(null) }

    content { section, item ->
        when {
            item.anime != null -> animeSheetItem = item
            item.artist != null -> artistSheetItem = item
            // Only sections with downloader apps get the choice.
            ExternalApps.forSection(section).isNotEmpty() -> musicSheetItem = section to item
            else -> context.startActivity(
                TorrentSearch.searchIntent(
                    context,
                    torrentQuery(item.searchTerms),
                    section.torrentCategory,
                )
            )
        }
    }

    musicSheetItem?.let { (musicSection, musicItem) ->
        MusicActionSheet(
            item = musicItem,
            apps = ExternalApps.forSection(musicSection, telegramBot),
            telegramBot = telegramBot,
            onSearchTorrents = {
                musicSheetItem = null
                context.startActivity(
                    TorrentSearch.searchIntent(
                        context,
                        torrentQuery(musicItem.searchTerms),
                        musicSection.torrentCategory,
                    )
                )
            },
            onDismiss = { musicSheetItem = null },
        )
    }

    val sheetItem = animeSheetItem
    val sheetAnime = sheetItem?.anime
    if (sheetItem != null && sheetAnime != null) {
        AnimeTorrentSearchSheet(
            item = sheetItem,
            anime = sheetAnime,
            onSearch = { query, episode ->
                animeSheetItem = null
                scope.launch {
                    // Without a Kitsu ID, Torrentio is skipped but the other
                    // providers still search and get filtered.
                    val kitsuId = resolveKitsuId(sheetAnime.aniListId)
                    context.startActivity(
                        TorrentSearch.searchIntent(
                            context,
                            query,
                            Category.Anime,
                            target = sheetAnime.searchTarget(kitsuId, episode),
                        )
                    )
                }
            },
            onDismiss = { animeSheetItem = null },
        )
    }

    val sheetArtist = artistSheetItem?.artist
    if (sheetArtist != null) {
        ArtistTorrentSearchSheet(
            artist = sheetArtist,
            imageUrl = artistSheetItem?.imageUrl,
            telegramBot = telegramBot,
            loadAlbums = { loadArtistAlbums(sheetArtist) },
            onSearch = { query ->
                artistSheetItem = null
                context.startActivity(
                    TorrentSearch.searchIntent(context, torrentQuery(query), Category.Music)
                )
            },
            onDismiss = { artistSheetItem = null },
        )
    }
}
