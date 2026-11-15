package com.c4entertainment.mediahunter.util

object Constants {
    //App
    const val TAG = "AppTag"

    //Buttons

    //Menu Items

    //Screens
    const val SEARCH_SCREEN = "Search"
    const val MOVIE_SCREEN = "Movie"

    //Labels
    const val EMAIL_LABEL = "Email"
    const val PASSWORD_LABEL = "Password"

    //Useful
    const val EMPTY_STRING = ""
    const val VERTICAL_DIVIDER = "|"

    //Texts

    //Messages

    //Error Messages

}

/** TMDB image URLs. Lists use small sizes; Details keeps the full-size originals. */
object TmdbImage {
    private const val BASE = "https://image.tmdb.org/t/p"

    const val GRID_POSTER = "w342"
    const val LIST_BACKDROP = "w780"
    const val BACKDROP_LARGE = "w1280"
    const val ORIGINAL = "original"

    fun url(size: String, path: String?): String = "$BASE/$size/$path"
}
