package com.c4entertainment.mediahunter.data.network.dto.movie_details

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
@JsonClass(generateAdapter = true)
data class ProductionCompanyDto (

  @field:Json(name = "id"             ) var id            : Int?    = null,
  @field:Json(name = "logo_path"      ) var logoPath      : String? = null,
  @field:Json(name = "name"           ) var name          : String? = null,
  @field:Json(name = "origin_country" ) var originCountry : String? = null

)