package com.c4entertainment.mediahunter.data.repository

import com.c4entertainment.mediahunter.data.network.api.TMDBMovieAPI
import com.c4entertainment.mediahunter.domain.model.movie_details.Genre
import javax.inject.Inject
import javax.inject.Singleton

/** TMDB's genre lists for "movie" and "tv", fetched once per process. */
@Singleton
class TmdbGenres @Inject constructor(private val api: TMDBMovieAPI) {
    private val cache = mutableMapOf<String, List<Genre>>()

    /** Empty when the list can't be loaded; the next call tries again. */
    suspend fun get(type: String): List<Genre> {
        cache[type]?.let { return it }
        return try {
            api.getGenres(type).genres.orEmpty()
                .filter { it.id != null && !it.name.isNullOrBlank() }
                .map { Genre(it.id, it.name) }
                .sortedBy { it.name }
                .also { if (it.isNotEmpty()) cache[type] = it }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
