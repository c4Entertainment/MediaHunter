package com.c4entertainment.mediahunter.presentation.search

import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.util.MovieTVFilterConfig

/** One chip on the Search screen: TMDB movies or TV, or a [CatalogSection]. */
enum class SearchSection(val label: String, val catalog: CatalogSection? = null) {
    Movies("Movies"),
    TvShows("TV Shows"),
    Anime("Anime", CatalogSection.Anime),
    Albums("Albums", CatalogSection.Albums),
    Songs("Songs", CatalogSection.Songs),
    Artists("Artists", CatalogSection.Artists),
    Books("Books", CatalogSection.Books),
    Games("Games", CatalogSection.Games);

    /** The TMDB type for Movies and TV Shows, null for catalog sections. */
    val tmdbType: String?
        get() = when (this) {
            Movies -> MovieTVFilterConfig.MOVIE
            TvShows -> MovieTVFilterConfig.TV
            else -> null
        }

    companion object {
        /** The chip to preselect for what Home is showing. */
        fun forHome(selected: CatalogSection?, movieTV: String): SearchSection =
            entries.firstOrNull { selected != null && it.catalog == selected }
                ?: if (movieTV == MovieTVFilterConfig.TV) TvShows else Movies

        fun fromName(name: String?): SearchSection =
            entries.firstOrNull { it.name == name } ?: Movies
    }
}
