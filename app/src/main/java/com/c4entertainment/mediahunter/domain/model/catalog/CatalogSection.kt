package com.c4entertainment.mediahunter.domain.model.catalog

/** Home sections that come from outside TMDB. Each item opens a torrent search. */
enum class CatalogSection(val label: String) {
    Albums("Albums"),
    Songs("Songs"),
    Artists("Artists"),
    Books("Books"),
    Games("Games"),
    Anime("Anime"),
;

    /** Sections backed by Deezer charts, which the music filters apply to. */
    val isMusic: Boolean get() = this == Albums || this == Songs || this == Artists
}
