package com.c4entertainment.moviehunter.data.network.dto.catalog

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RawgGamesDto(
    @field:Json(name = "results") val results: List<RawgGameDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RawgGameDto(
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "released") val released: String? = null,
    @field:Json(name = "background_image") val backgroundImage: String? = null,
)
