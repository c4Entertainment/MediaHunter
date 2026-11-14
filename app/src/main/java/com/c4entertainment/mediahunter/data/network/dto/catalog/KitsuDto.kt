package com.c4entertainment.mediahunter.data.network.dto.catalog

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `GET /mappings?filter[externalSite]=anilist/anime&filter[externalId]=<id>` */
@JsonClass(generateAdapter = true)
data class KitsuMappingsDto(
    @field:Json(name = "data") val data: List<KitsuMappingDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class KitsuMappingDto(
    @field:Json(name = "relationships") val relationships: KitsuMappingRelationshipsDto? = null,
)

@JsonClass(generateAdapter = true)
data class KitsuMappingRelationshipsDto(
    @field:Json(name = "item") val item: KitsuRelationshipDto? = null,
)

@JsonClass(generateAdapter = true)
data class KitsuRelationshipDto(
    @field:Json(name = "data") val data: KitsuResourceIdDto? = null,
)

@JsonClass(generateAdapter = true)
data class KitsuResourceIdDto(
    @field:Json(name = "type") val type: String? = null,
    @field:Json(name = "id") val id: String? = null,
)
