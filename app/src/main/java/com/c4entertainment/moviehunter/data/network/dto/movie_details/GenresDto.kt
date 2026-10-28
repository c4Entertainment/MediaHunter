package com.c4entertainment.moviehunter.data.network.dto.movie_details
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class GenresDto (

  @field:Json(name = "id"   ) var id   : Int?    = null,
  @field:Json(name = "name" ) var name : String? = null

)