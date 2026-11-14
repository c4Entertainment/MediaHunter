package com.c4entertainment.moviehunter.domain.model.catalog

/** Home sections that come from outside TMDB. Each item opens a torrent search. */
enum class CatalogSection(val label: String) {
    Albums("Albums"),
    Songs("Songs"),
    Books("Books"),
    Games("Games"),
    Anime("Anime"),
}
