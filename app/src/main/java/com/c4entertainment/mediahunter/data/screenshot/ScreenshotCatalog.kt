package com.c4entertainment.mediahunter.data.screenshot

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.c4entertainment.mediahunter.data.network.api.TMDBMovieAPI
import com.c4entertainment.mediahunter.domain.model.movie.Movie
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Store-screenshot builds only (BuildConfig.SCREENSHOT_MODE): Home shows a fixed
 * list of public-domain and Creative Commons titles, each fetched from TMDB by
 * id, instead of TMDB's popular lists.
 */
object ScreenshotCatalog {

    /** TMDB movie ids: Blender open movies and public-domain films. */
    private val MOVIE_IDS = listOf(
        10378,  // Big Buck Bunny (2008)
        45745,  // Sintel (2010)
        133701, // Tears of Steel (2012)
        9761,   // Elephants Dream (2006)
        358332, // Cosmos Laundromat (2015)
        10331,  // Night of the Living Dead (1968)
        653,    // Nosferatu (1922)
        961,    // The General (1926)
        3085,   // His Girl Friday (1940)
        4808,   // Charade (1963)
        10513,  // Plan 9 from Outer Space (1957)
        23033,  // Jungle Book (1942)
        457784, // Agent 327: Operation Barbershop (2017)
        406956, // Caminandes: Llamigos (2016)
    )

    /** TMDB tv ids: Pioneer One (2010, CC BY-NC-SA). */
    private val TV_IDS = listOf(33050)

    fun source(api: TMDBMovieAPI, type: String): PagingSource<Int, Movie> =
        Source(api, type, if (type == "tv") TV_IDS else MOVIE_IDS)

    private class Source(
        private val api: TMDBMovieAPI,
        private val type: String,
        private val ids: List<Int>,
    ) : PagingSource<Int, Movie>() {

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> = try {
            val movies = coroutineScope {
                ids.map { id ->
                    async {
                        runCatching { api.getMovieDetails(id, type, appendToResponse = "") }.getOrNull()
                    }
                }.awaitAll()
            }.filterNotNull().map { d ->
                Movie(
                    adult = false,
                    backdropPath = d.backdropPath.orEmpty(),
                    id = d.id,
                    title = d.title ?: d.name.orEmpty(),
                    originalLanguage = d.originalLanguage.orEmpty(),
                    originalTitle = d.originalTitle ?: d.originalName.orEmpty(),
                    overview = d.overview.orEmpty(),
                    posterPath = d.posterPath.orEmpty(),
                    mediaType = type,
                    popularity = d.popularity ?: 0.0,
                    releaseDate = d.releaseDate ?: d.firstAirDate.orEmpty(),
                    video = false,
                    voteAverage = d.voteAverage ?: 0.0,
                    voteCount = d.voteCount ?: 0,
                )
            }
            LoadResult.Page(data = movies, prevKey = null, nextKey = null)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

        override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null
    }
}
