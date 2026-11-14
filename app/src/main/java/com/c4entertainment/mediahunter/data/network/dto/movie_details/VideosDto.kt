package com.c4entertainment.mediahunter.data.network.dto.movie_details

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class VideosDto (

  @field:Json(name = "results" ) var results : List<MovieVideoDto> = listOf()

)
