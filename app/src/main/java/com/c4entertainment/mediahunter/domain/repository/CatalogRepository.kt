package com.c4entertainment.mediahunter.domain.repository

import com.c4entertainment.mediahunter.domain.model.catalog.ArtistAlbum
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.domain.model.catalog.MusicChart

interface CatalogRepository {
    /** What's trending or popular in [section] right now; [chart] only applies to music sections. */
    suspend fun getItems(section: CatalogSection, chart: MusicChart = MusicChart()): Result<List<CatalogItem>>

    /** The albums, EPs and compilations (no singles) of a Deezer artist, newest first. */
    suspend fun artistAlbums(artistId: Long, artistName: String): Result<List<ArtistAlbum>>

    /** Items in [section] matching [query]. */
    suspend fun search(section: CatalogSection, query: String): Result<List<CatalogItem>>

    /** The Kitsu ID for an AniList anime, or null if Kitsu doesn't know it or can't be reached. */
    suspend fun kitsuIdForAniList(aniListId: Long): String?
}
