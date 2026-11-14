package com.c4entertainment.moviehunter.data.network.dto.tv

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TvSeasonsDto(
    @field:Json(name = "seasons") var seasons: List<TvSeasonDto> = listOf(),
    @field:Json(name = "last_episode_to_air") var lastEpisodeToAir: TvEpisodeDto? = null,
)

@JsonClass(generateAdapter = true)
data class TvEpisodeDto(
    @field:Json(name = "season_number") var seasonNumber: Int? = null,
    @field:Json(name = "episode_number") var episodeNumber: Int? = null,
    @field:Json(name = "name") var name: String? = null,
    @field:Json(name = "air_date") var airDate: String? = null,
)

@JsonClass(generateAdapter = true)
data class TvSeasonDto(
    @field:Json(name = "season_number") var seasonNumber: Int,
    @field:Json(name = "name") var name: String? = null,
    @field:Json(name = "episode_count") var episodeCount: Int? = null,
)
