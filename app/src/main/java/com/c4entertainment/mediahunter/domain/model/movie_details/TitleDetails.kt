package com.c4entertainment.mediahunter.domain.model.movie_details

/** Everything the Details page shows about a movie or a show, from one TMDB call. */
data class TitleDetails(
    val id: Int,
    /** "movie" or "tv", as in TMDB paths. */
    val type: String,
    val title: String,
    val originalTitle: String,
    /** Release date, or first air date for a show; yyyy-MM-dd. */
    val releaseDate: String,
    val lastAirDate: String?,
    val inProduction: Boolean,
    val tagline: String?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    /** Minutes; an episode's for a show. */
    val runtime: Int?,
    val certification: String?,
    val voteAverage: Double,
    val imdbId: String?,
    val genres: List<String>,
    /** Directors for a movie, creators for a show. */
    val directors: List<String>,
    val writers: List<String>,
    val studios: List<String>,
    val videos: List<MovieVideo>,
    val cast: List<Cast>,
    val recommendations: List<TitleSummary>,
    /** Without season 0, which holds specials. */
    val seasons: List<TvSeason>,
    val lastEpisode: TvEpisode?,
    val nextEpisode: TvEpisode?,
) {
    val isTv get() = type == "tv"
}

/** A title in a row of others, e.g. "More like this". */
data class TitleSummary(
    val id: Int,
    val type: String,
    val title: String,
    val posterPath: String?,
    val year: String?,
)
