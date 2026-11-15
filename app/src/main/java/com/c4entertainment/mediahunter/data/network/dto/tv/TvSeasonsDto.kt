package com.c4entertainment.mediahunter.data.network.dto.tv

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TvEpisodeDto(
    @field:Json(name = "season_number") var seasonNumber: Int? = null,
    @field:Json(name = "episode_number") var episodeNumber: Int? = null,
    @field:Json(name = "name") var name: String? = null,
    @field:Json(name = "air_date") var airDate: String? = null,
    @field:Json(name = "overview") var overview: String? = null,
    @field:Json(name = "runtime") var runtime: Int? = null,
    @field:Json(name = "still_path") var stillPath: String? = null,
)

@JsonClass(generateAdapter = true)
data class TvSeasonDto(
    @field:Json(name = "season_number") var seasonNumber: Int,
    @field:Json(name = "name") var name: String? = null,
    @field:Json(name = "episode_count") var episodeCount: Int? = null,
    @field:Json(name = "poster_path") var posterPath: String? = null,
    @field:Json(name = "air_date") var airDate: String? = null,
)
