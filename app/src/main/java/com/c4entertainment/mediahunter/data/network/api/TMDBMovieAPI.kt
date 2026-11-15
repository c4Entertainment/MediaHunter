package com.c4entertainment.mediahunter.data.network.api

import com.c4entertainment.mediahunter.BuildConfig
import com.c4entertainment.mediahunter.data.network.apiresponses.GenreListResponse
import com.c4entertainment.mediahunter.data.network.apiresponses.MoviesResponse
import com.c4entertainment.mediahunter.data.network.dto.movie_details.MovieDetails2Dto
import com.c4entertainment.mediahunter.data.network.dto.movie_details.MovieDetailsDto
import com.c4entertainment.mediahunter.data.network.dto.tv.TvSeasonsDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TMDBMovieAPI {
    @GET("trending/{type}/{time_window}")
    suspend fun getTrendingMovies(
        @Path("type") type: String = "movie",
        @Path("time_window") length: String = "day",
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US",
        @Query("api_key") apiKey: String = API_KEY,
    ): MoviesResponse

    @GET("search/{type}")
    suspend fun searchMovies(
        @Path("type") type: String = "movie",
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US",
//        @Query("include_adult") includeAdult: Boolean = false,
        @Query("api_key") apiKey: String = API_KEY,
    ): MoviesResponse



    @GET("genre/{type}/list")
    suspend fun getGenres(
        @Path("type") type: String,
        @Query("language") language: String = "en-US",
        @Query("api_key") apiKey: String = API_KEY,
    ): GenreListResponse

    /** Browse by filter; null genre or year parameters are left out of the request. */
    @GET("discover/{type}")
    suspend fun discover(
        @Path("type") type: String,
        @Query("with_genres") genre: Int? = null,
        @Query("primary_release_year") releaseYear: Int? = null,
        @Query("first_air_date_year") firstAirYear: Int? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US",
        @Query("api_key") apiKey: String = API_KEY,
    ): MoviesResponse

    @GET("{type}/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Int,
        @Path("type") type: String = "movie",
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "videos,credits,external_ids",
        @Query("api_key") apiKey: String = API_KEY,
    ): MovieDetailsDto

    @GET("tv/{id}")
    suspend fun getTvSeasons(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String = API_KEY,
    ): TvSeasonsDto

    @GET("find/{external_id}")
    suspend fun getMovieDetailsByExternalId(
        @Path("external_id") externalID: String,
        @Query("api_key") apiKey: String = API_KEY,
        @Query("external_source") externalSource: String = "imdb_id",

    ): MovieDetails2Dto


    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val API_KEY = BuildConfig.TMDB_API_KEY
    }
}