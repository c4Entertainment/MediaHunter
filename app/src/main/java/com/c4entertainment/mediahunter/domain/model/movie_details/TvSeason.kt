package com.c4entertainment.mediahunter.domain.model.movie_details

data class TvSeason(
    val number: Int,
    val name: String,
    val episodeCount: Int,
)

/** One episode, e.g. the latest one to air. */
data class TvEpisode(
    val season: Int,
    val episode: Int,
    val name: String?,
    /** yyyy-MM-dd */
    val airDate: String?,
)

/** What the torrent sheet needs about a show. */
data class TvSeasonsInfo(
    val seasons: List<TvSeason>,
    val latestEpisode: TvEpisode?,
)
