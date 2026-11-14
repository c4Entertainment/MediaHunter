package com.c4entertainment.mediahunter.data.network.dto.movie_details

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class BelongsToCollectionDto(

    @field:Json(name = "id"            ) val id            : Int?    = null,
    @field:Json(name = "name"          ) val name          : String? = null,
    @field:Json(name = "poster_path"   ) val posterPath    : String? = null,
    @field:Json(name = "backdrop_path" ) val backdropPath  : String? = null

)
