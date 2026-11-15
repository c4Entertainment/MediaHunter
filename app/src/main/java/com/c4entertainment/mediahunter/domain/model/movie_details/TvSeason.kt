package com.c4entertainment.mediahunter.domain.model.movie_details

data class TvSeason(
    val number: Int,
    val name: String,
    val episodeCount: Int,
    val posterPath: String? = null,
    /** yyyy-MM-dd */
    val airDate: String? = null,
)

/** One episode, e.g. the latest one to air. */
data class TvEpisode(
    val season: Int,
    val episode: Int,
    val name: String?,
    /** yyyy-MM-dd */
    val airDate: String?,
    val overview: String? = null,
    val runtime: Int? = null,
    val stillPath: String? = null,
)


/** A season's page: its episodes and what it says about itself. */
data class SeasonDetails(
    val number: Int,
    val name: String,
    val overview: String?,
    val posterPath: String?,
    val airDate: String?,
    val episodes: List<TvEpisode>,
)
