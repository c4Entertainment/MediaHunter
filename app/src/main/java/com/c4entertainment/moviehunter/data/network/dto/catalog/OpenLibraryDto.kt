package com.c4entertainment.moviehunter.data.network.dto.catalog

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenLibraryTrendingDto(
    @field:Json(name = "works") val works: List<OpenLibraryWorkDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class OpenLibrarySearchDto(
    @field:Json(name = "docs") val docs: List<OpenLibraryWorkDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class OpenLibraryWorkDto(
    @field:Json(name = "key") val key: String,
    @field:Json(name = "title") val title: String,
    @field:Json(name = "author_name") val authorNames: List<String>? = null,
    @field:Json(name = "cover_i") val coverId: Long? = null,
    @field:Json(name = "first_publish_year") val firstPublishYear: Int? = null,
)
