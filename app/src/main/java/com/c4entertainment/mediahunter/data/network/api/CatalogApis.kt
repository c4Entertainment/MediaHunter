package com.c4entertainment.mediahunter.data.network.api

import com.c4entertainment.mediahunter.BuildConfig
import com.c4entertainment.mediahunter.data.network.dto.catalog.AniListRequest
import com.c4entertainment.mediahunter.data.network.dto.catalog.AniListResponseDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.DeezerAlbumChartDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.DeezerTrackChartDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.KitsuMappingsDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.OpenLibrarySearchDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.OpenLibraryTrendingDto
import com.c4entertainment.mediahunter.data.network.dto.catalog.RawgGamesDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Deezer's public API. No key needed. */
interface DeezerAPI {
    @GET("chart/0/albums")
    suspend fun getTopAlbums(@Query("limit") limit: Int = 50): DeezerAlbumChartDto

    /** The album chart can be tiny (3 entries in Nov 2026); the track chart fills it out. */
    @GET("chart/0/tracks")
    suspend fun getTopTracks(@Query("limit") limit: Int = 100): DeezerTrackChartDto

    @GET("search/track")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 50,
    ): DeezerTrackChartDto

    @GET("search/album")
    suspend fun searchAlbums(
        @Query("q") query: String,
        @Query("limit") limit: Int = 50,
    ): DeezerAlbumChartDto

    companion object {
        const val BASE_URL = "https://api.deezer.com/"
    }
}

/** Open Library. No key needed. */
interface OpenLibraryAPI {
    @GET("trending/daily.json")
    suspend fun getTrending(@Query("limit") limit: Int = 50): OpenLibraryTrendingDto

    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 50,
        @Query("fields") fields: String = "key,title,author_name,cover_i,first_publish_year",
    ): OpenLibrarySearchDto

    companion object {
        const val BASE_URL = "https://openlibrary.org/"
        fun coverUrl(coverId: Long) = "https://covers.openlibrary.org/b/id/$coverId-M.jpg"
    }
}

/** RAWG. Needs a free key (RAWG_API_KEY in local.properties) and attribution. */
interface RawgAPI {
    @GET("games")
    suspend fun getGames(
        @Query("dates") dates: String? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = "-added",
        @Query("page_size") pageSize: Int = 40,
        @Query("key") apiKey: String = API_KEY,
    ): RawgGamesDto

    companion object {
        const val BASE_URL = "https://api.rawg.io/api/"
        const val API_KEY = BuildConfig.RAWG_API_KEY
    }
}

/** AniList's GraphQL API. No key needed. */
interface AniListAPI {
    @POST(".")
    suspend fun query(@Body request: AniListRequest): AniListResponseDto

    companion object {
        const val BASE_URL = "https://graphql.anilist.co/"
        /** Trending anime, or the best matches for `$search` when it's set. */
        const val MEDIA_QUERY = $$"""
            query ($search: String, $sort: [MediaSort]) {
              Page(perPage: 50) {
                media(type: ANIME, search: $search, sort: $sort, isAdult: false) {
                  id
                  title { romaji english }
                  coverImage { large }
                  seasonYear
                  episodes
                  nextAiringEpisode { episode }
                  format
                }
              }
            }
        """
    }
}

/**
 * Kitsu's public API, used only to turn an AniList ID into the Kitsu ID
 * Torrentio indexes anime by. No key needed.
 */
interface KitsuAPI {
    @GET("mappings")
    suspend fun getMappings(
        @Query("filter[externalSite]") externalSite: String = "anilist/anime",
        @Query("filter[externalId]") externalId: Long,
    ): KitsuMappingsDto

    companion object {
        const val BASE_URL = "https://kitsu.io/api/edge/"
    }
}
