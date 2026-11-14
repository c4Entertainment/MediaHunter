package com.c4entertainment.mediahunter.domain.model.catalog

/**
 * One entry in a Home catalog row, e.g. an album, book, game or anime.
 *
 * [searchTerms] is what a torrent search for this item should look for.
 */
data class CatalogItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val searchTerms: String,
    /** Set for anime, so a search can target the exact show and episode. */
    val anime: AnimeRef? = null,
    /** Set for artists, which open a sheet with their albums instead of searching directly. */
    val artist: ArtistRef? = null,
)

data class ArtistRef(
    val deezerId: Long,
    val name: String,
)

/** One release in an artist's discography. [recordType] is Deezer's: "album", "ep" or "compile". */
data class ArtistAlbum(
    val title: String,
    val recordType: String,
    val year: String?,
    val imageUrl: String?,
    val searchTerms: String,
)

data class AnimeRef(
    val aniListId: Long,
    /** Romaji first (what releases mostly use), then English. */
    val titles: List<String>,
    val year: Int?,
    /** Episodes out so far: the total once finished, aired ones while airing. */
    val availableEpisodes: Int?,
)
