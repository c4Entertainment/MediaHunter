package com.c4entertainment.moviehunter.data.network.dto.tv

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TvSeasonsDto(
    @field:Json(name = "seasons") var seasons: List<TvSeasonDto> = listOf()
)

@JsonClass(generateAdapter = true)
data class TvSeasonDto(
    @field:Json(name = "season_number") var seasonNumber: Int,
    @field:Json(name = "name") var name: String? = null,
    @field:Json(name = "episode_count") var episodeCount: Int? = null,
)
