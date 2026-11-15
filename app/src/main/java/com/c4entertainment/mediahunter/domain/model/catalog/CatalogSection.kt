package com.c4entertainment.mediahunter.domain.model.catalog

import androidx.annotation.StringRes
import com.c4entertainment.mediahunter.R

/** Home sections that come from outside TMDB. Each item opens a torrent search. */
enum class CatalogSection(@StringRes val labelRes: Int) {
    Albums(R.string.section_albums),
    Songs(R.string.section_songs),
    Artists(R.string.section_artists),
    Books(R.string.section_books),
    Games(R.string.section_games),
    Anime(R.string.section_anime),
;

    /** Sections backed by Deezer charts, which the music filters apply to. */
    val isMusic: Boolean get() = this == Albums || this == Songs || this == Artists
}
