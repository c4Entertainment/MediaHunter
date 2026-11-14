package com.c4entertainment.mediahunter.data.network.dto.catalog

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AniListRequest(
    @field:Json(name = "query") val query: String,
    @field:Json(name = "variables") val variables: AniListVariables = AniListVariables(),
)

@JsonClass(generateAdapter = true)
data class AniListVariables(
    @field:Json(name = "search") val search: String? = null,
    @field:Json(name = "sort") val sort: List<String> = listOf("TRENDING_DESC"),
)

@JsonClass(generateAdapter = true)
data class AniListResponseDto(
    @field:Json(name = "data") val data: AniListDataDto? = null,
)

@JsonClass(generateAdapter = true)
data class AniListDataDto(
    @field:Json(name = "Page") val page: AniListPageDto? = null,
)

@JsonClass(generateAdapter = true)
data class AniListPageDto(
    @field:Json(name = "media") val media: List<AniListMediaDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AniListMediaDto(
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "title") val title: AniListTitleDto,
    @field:Json(name = "coverImage") val coverImage: AniListCoverDto? = null,
    @field:Json(name = "seasonYear") val seasonYear: Int? = null,
    @field:Json(name = "episodes") val episodes: Int? = null,
    /** Set while airing; episodes before it are out. */
    @field:Json(name = "nextAiringEpisode") val nextAiringEpisode: AniListNextEpisodeDto? = null,
    @field:Json(name = "format") val format: String? = null,
)

@JsonClass(generateAdapter = true)
data class AniListTitleDto(
    @field:Json(name = "romaji") val romaji: String? = null,
    @field:Json(name = "english") val english: String? = null,
)

@JsonClass(generateAdapter = true)
data class AniListCoverDto(
    @field:Json(name = "large") val large: String? = null,
)

@JsonClass(generateAdapter = true)
data class AniListNextEpisodeDto(
    @field:Json(name = "episode") val episode: Int? = null,
)
