package com.c4entertainment.mediahunter.presentation.search

import androidx.annotation.StringRes
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.util.MovieTVFilterConfig

/** One chip on the Search screen: TMDB movies or TV, or a [CatalogSection]. */
enum class SearchSection(@StringRes val labelRes: Int, val catalog: CatalogSection? = null) {
    Movies(R.string.home_movies),
    TvShows(R.string.home_tv_shows),
    Anime(R.string.section_anime, CatalogSection.Anime),
    Albums(R.string.section_albums, CatalogSection.Albums),
    Songs(R.string.section_songs, CatalogSection.Songs),
    Artists(R.string.section_artists, CatalogSection.Artists),
    Books(R.string.section_books, CatalogSection.Books),
    Games(R.string.section_games, CatalogSection.Games);

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
