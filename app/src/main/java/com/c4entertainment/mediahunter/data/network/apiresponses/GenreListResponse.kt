package com.c4entertainment.mediahunter.data.network.apiresponses

import com.c4entertainment.mediahunter.data.network.dto.movie_details.GenresDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenreListResponse(
    @field:Json(name = "genres") var genres: List<GenresDto>? = null,
)
