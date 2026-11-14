package com.c4entertainment.mediahunter.data.network.dto.movie_details

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MovieDetails2Dto (

    @field:Json(name ="movie_results")
    var movieResults : List<MovieDetailsDto>,

    )