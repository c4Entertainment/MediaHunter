package com.c4entertainment.moviehunter.data.network.dto.catalog

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeezerAlbumChartDto(
    @field:Json(name = "data") val data: List<DeezerAlbumDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DeezerTrackChartDto(
    @field:Json(name = "data") val data: List<DeezerTrackDto> = emptyList(),
)

/** A chart or search track. Its album object has no artist, so the track's is used. */
@JsonClass(generateAdapter = true)
data class DeezerTrackDto(
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "title") val title: String,
    @field:Json(name = "album") val album: DeezerAlbumDto? = null,
    @field:Json(name = "artist") val artist: DeezerArtistDto? = null,
)

@JsonClass(generateAdapter = true)
data class DeezerAlbumDto(
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "title") val title: String,
    @field:Json(name = "cover_big") val coverBig: String? = null,
    @field:Json(name = "record_type") val recordType: String? = null,
    @field:Json(name = "artist") val artist: DeezerArtistDto? = null,
)

@JsonClass(generateAdapter = true)
data class DeezerArtistDto(
    @field:Json(name = "name") val name: String,
)
