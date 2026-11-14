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
import com.c4entertainment.mediahunter.data.network.dto.catalog.DeezerTrackDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.OpenLibraryWorkDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.RawgGameDto
import com.c4entertainment.mediahunter.domain.model.catalog.AnimeRef
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
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

    override suspend fun getItems(section: CatalogSection) = load {
        when (section) {
            CatalogSection.Albums -> topAlbums().map { it.toCatalogItem() }
            CatalogSection.Songs -> deezer.getTopTracks().data.map { it.toCatalogItem() }
            CatalogSection.Books -> openLibrary.getTrending().works.map { it.toCatalogItem() }
            CatalogSection.Games -> {
                requireRawgKey()
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
            CatalogSection.Books -> openLibrary.search(query).docs.map { it.toCatalogItem() }
            CatalogSection.Games -> {
                requireRawgKey()
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
        val trackChart = async { runCatching { deezer.getTopTracks().data }.getOrDefault(emptyList()) }
        val fromTracks = trackChart.await()
            .filter { track -> !track.album?.title.equals(track.title, ignoreCase = true) }
            .mapNotNull { track ->
                track.album?.let { album -> album.copy(artist = album.artist ?: track.artist) }
            }
        (albumChart.await() + fromTracks).distinctBy { it.id }
    }

    private suspend fun load(block: suspend () -> List<CatalogItem>): Result<List<CatalogItem>> = try {
        Result.success(block().distinctBy { it.id })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun requireRawgKey() = check(RawgAPI.API_KEY.isNotBlank() && RawgAPI.API_KEY != "null") {
        "Games need a free RAWG key. Add RAWG_API_KEY to local.properties."
    }

    private suspend fun anime(variables: AniListVariables) =
        aniList.query(AniListRequest(AniListAPI.MEDIA_QUERY, variables))
            .data?.page?.media.orEmpty()
            .mapNotNull { it.toCatalogItem() }
}

private fun DeezerAlbumDto.toCatalogItem(): CatalogItem {
    val artist = artist?.name
    return CatalogItem(
        id = "deezer-$id",
        title = title,
        subtitle = artist,
        imageUrl = coverBig,
        searchTerms = listOfNotNull(artist, title).joinToString(" "),
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
        searchTerms = listOfNotNull(artist, title).joinToString(" "),
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
