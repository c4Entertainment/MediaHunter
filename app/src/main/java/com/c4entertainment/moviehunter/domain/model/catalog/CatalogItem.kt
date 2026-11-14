package com.c4entertainment.moviehunter.domain.model.catalog

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
)

data class AnimeRef(
    val aniListId: Long,
    /** Romaji first (what releases mostly use), then English. */
    val titles: List<String>,
    val year: Int?,
    /** Episodes out so far: the total once finished, aired ones while airing. */
    val availableEpisodes: Int?,
)
