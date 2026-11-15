package com.c4entertainment.mediahunter.data.repository

import com.c4entertainment.mediahunter.data.network.api.AniListAPI
import com.c4entertainment.mediahunter.data.network.api.KitsuAPI
import com.c4entertainment.mediahunter.data.network.api.DeezerAPI
import com.c4entertainment.mediahunter.data.network.api.OpenLibraryAPI
import com.c4entertainment.mediahunter.data.network.api.RawgAPI
import com.c4entertainment.mediahunter.data.network.dto.catalog.AniListMediaDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.AniListRequest
import com.c4entertainment.mediahunter.data.network.dto.catalog.AniListVariables
import com.c4entertainment.mediahunter.data.network.dto.catalog.DeezerAlbumDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.DeezerArtistDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.DeezerTrackDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.OpenLibraryWorkDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.RawgGameDto
import com.c4entertainment.mediahunter.domain.model.catalog.AnimeRef
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistAlbum
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistRef
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.domain.model.catalog.MusicChart
import com.c4entertainment.mediahunter.domain.model.catalog.MusicRegion
import com.c4entertainment.mediahunter.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import javax.inject.Inject

class CatalogRepositoryImpl @Inject constructor(
    private val deezer: DeezerAPI,
    private val openLibrary: OpenLibraryAPI,
    private val rawg: RawgAPI,
    private val aniList: AniListAPI,
    private val kitsu: KitsuAPI,
) : CatalogRepository {

    private val kitsuIds = mutableMapOf<Long, String>()

    override suspend fun kitsuIdForAniList(aniListId: Long): String? {
        kitsuIds[aniListId]?.let { return it }
        return try {
            kitsu.getMappings(externalId = aniListId).data
                .firstNotNullOfOrNull { mapping ->
                    mapping.relationships?.item?.data?.takeIf { it.type == "anime" }?.id
                }
                ?.also { kitsuIds[aniListId] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getItems(section: CatalogSection, chart: MusicChart) = load {
        when (section) {
            CatalogSection.Albums -> when {
                chart.editorsPicks -> deezer.getEditorsPicks().data
                chart == MusicChart() -> topAlbums()
                else -> albumsOf(tracks(chart))
            }.map { it.toCatalogItem() }
            CatalogSection.Songs -> tracks(chart).map { it.toCatalogItem() }
            CatalogSection.Artists -> artists(chart).map { it.toCatalogItem() }
            CatalogSection.Books -> openLibrary.getTrending().works.map { it.toCatalogItem() }
            CatalogSection.Games -> {
                val today = LocalDate.now()
                rawg.getGames(dates = "${today.minusYears(1)},$today").results.map { it.toCatalogItem() }
            }
            CatalogSection.Anime -> anime(AniListVariables())
        }
    }

    override suspend fun search(section: CatalogSection, query: String) = load {
        when (section) {
            CatalogSection.Albums -> deezer.searchAlbums(query).data.map { it.toCatalogItem() }
            CatalogSection.Songs -> deezer.searchTracks(query).data.map { it.toCatalogItem() }
            CatalogSection.Artists -> deezer.searchArtists(query).data.map { it.toCatalogItem() }
            CatalogSection.Books -> openLibrary.search(query).docs.map { it.toCatalogItem() }
            CatalogSection.Games -> {
                // RAWG ranks by relevance when no ordering is given.
                rawg.getGames(search = query, ordering = null).results.map { it.toCatalogItem() }
            }
            CatalogSection.Anime -> anime(AniListVariables(search = query, sort = listOf("SEARCH_MATCH")))
        }
    }

    /**
     * The album chart first, then the albums of the top tracks, in chart
     * order. Singles (named after their track) are left to Songs.
     */
    private suspend fun topAlbums(): List<DeezerAlbumDto> = coroutineScope {
        val albumChart = async { deezer.getTopAlbums().data }
        val trackChart = async { runCatching { deezer.getChartTracks().data }.getOrDefault(emptyList()) }
        (albumChart.await() + albumsOf(trackChart.await())).distinctBy { it.id }
    }

    /** Albums of [tracks] in chart order, without singles (named after their track). */
    private fun albumsOf(tracks: List<DeezerTrackDto>): List<DeezerAlbumDto> = tracks
        .filter { track -> !track.album?.title.equals(track.title, ignoreCase = true) }
        .mapNotNull { track ->
            track.album?.let { album -> album.copy(artist = album.artist ?: track.artist) }
        }
        .distinctBy { it.id }

    /** A genre has its own chart; otherwise the region's official "Top" playlist. */
    private suspend fun tracks(chart: MusicChart): List<DeezerTrackDto> =
        chart.genre?.let { deezer.getChartTracks(it.deezerId).data }
            ?: deezer.getPlaylistTracks(chart.region.playlistId).data

    private suspend fun artists(chart: MusicChart): List<DeezerArtistDto> = when {
        chart.genre != null -> deezer.getChartArtists(chart.genre.deezerId).data
        chart.region == MusicRegion.Worldwide -> deezer.getChartArtists(0).data
        // Only genres and the world have artist charts: derive a region's from its tracks.
        else -> tracks(chart).mapNotNull { it.artist }.distinctBy { it.id }
    }

    override suspend fun artistAlbums(artistId: Long, artistName: String): Result<List<ArtistAlbum>> = try {
        Result.success(
            deezer.getArtistAlbums(artistId).data
                .filter { it.recordType in ARTIST_RELEASE_TYPES }
                .sortedByDescending { it.releaseDate.orEmpty() }
                .map { album ->
                    ArtistAlbum(
                        title = album.title,
                        recordType = album.recordType.orEmpty(),
                        year = album.releaseDate?.take(4)?.takeIf { it.length == 4 },
                        imageUrl = album.coverBig,
                        searchTerms = "$artistName ${album.title.forTorrentSearch()}",
                    )
                }
                .distinctBy { it.searchTerms }
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun load(block: suspend () -> List<CatalogItem>): Result<List<CatalogItem>> = try {
        Result.success(block().distinctBy { it.id })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun anime(variables: AniListVariables) =
        aniList.query(AniListRequest(AniListAPI.MEDIA_QUERY, variables))
            .data?.page?.media.orEmpty()
            .mapNotNull { it.toCatalogItem() }
}

private val ARTIST_RELEASE_TYPES = setOf("album", "ep", "compile")

private fun DeezerArtistDto.toCatalogItem() = CatalogItem(
    id = "deezer-artist-$id",
    title = name,
    subtitle = null,
    imageUrl = pictureBig,
    searchTerms = name,
    artist = id?.let { ArtistRef(it, name) },
)

private fun DeezerAlbumDto.toCatalogItem(): CatalogItem {
    val artist = artist?.name
    return CatalogItem(
        id = "deezer-$id",
        title = title,
        subtitle = artist,
        imageUrl = coverBig,
        searchTerms = listOfNotNull(artist, title.forTorrentSearch()).joinToString(" "),
    )
}

private fun DeezerTrackDto.toCatalogItem(): CatalogItem {
    val artist = artist?.name
    return CatalogItem(
        id = "deezer-track-$id",
        title = title,
        subtitle = listOfNotNull(artist, album?.title?.takeIf { !it.equals(title, ignoreCase = true) })
            .joinToString(" · ")
            .ifEmpty { null },
        imageUrl = album?.coverBig,
        searchTerms = listOfNotNull(artist, title.forTorrentSearch()).joinToString(" "),
    )
}

private fun OpenLibraryWorkDto.toCatalogItem(): CatalogItem {
    val author = authorNames?.firstOrNull()
    return CatalogItem(
        id = "openlibrary-$key",
        title = title,
        subtitle = listOfNotNull(author, firstPublishYear?.toString()).joinToString(" · "),
        imageUrl = coverId?.let(OpenLibraryAPI::coverUrl),
        searchTerms = listOfNotNull(title, author).joinToString(" by "),
    )
}

private fun RawgGameDto.toCatalogItem() = CatalogItem(
    id = "rawg-$id",
    title = name,
    subtitle = released,
    imageUrl = backgroundImage,
    searchTerms = name,
)

private fun AniListMediaDto.toCatalogItem(): CatalogItem? {
    // Anime torrents are usually named with the romaji title.
    val searchTitle = title.romaji ?: title.english ?: return null
    return CatalogItem(
        id = "anilist-$id",
        title = title.english ?: searchTitle,
        subtitle = listOfNotNull(
            seasonYear?.toString(),
            episodes?.let { "$it eps" },
        ).joinToString(" · ").ifEmpty { null },
        imageUrl = coverImage?.large,
        searchTerms = searchTitle,
        anime = AnimeRef(
            aniListId = id,
            titles = listOfNotNull(title.romaji, title.english).distinct(),
            year = seasonYear,
            availableEpisodes = nextAiringEpisode?.episode?.minus(1) ?: episodes,
        ),
    )
}

/**
 * Drops what store listings add but uploads rarely carry, since torrent
 * searches need every word to match: "(P3)", "[10-Year Edition]",
 * "(Deluxe)", "(feat. X)", "(Remastered 2016)", "- Single", "- EP".
 * "PARTYNEXTDOOR 3 (P3) [10-YEAR EDITION]" becomes "PARTYNEXTDOOR 3".
 */
private fun String.forTorrentSearch(): String {
    val cleaned = replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "")
        .replace(Regex("""(?i)\s+-\s+(single|ep)$"""), "")
        .trim()
    return cleaned.ifEmpty { this }
}
