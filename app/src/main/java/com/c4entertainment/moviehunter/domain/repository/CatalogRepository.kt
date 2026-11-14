package com.c4entertainment.moviehunter.domain.repository

import com.c4entertainment.moviehunter.domain.model.catalog.CatalogItem
import com.c4entertainment.moviehunter.domain.model.catalog.CatalogSection

interface CatalogRepository {
    /** What's trending or popular in [section] right now. */
    suspend fun getItems(section: CatalogSection): Result<List<CatalogItem>>

    /** Items in [section] matching [query]. */
    suspend fun search(section: CatalogSection, query: String): Result<List<CatalogItem>>

    /** The Kitsu ID for an AniList anime, or null if Kitsu doesn't know it or can't be reached. */
    suspend fun kitsuIdForAniList(aniListId: Long): String?
}
