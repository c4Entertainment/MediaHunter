package com.c4entertainment.mediahunter.data.repository

import com.c4entertainment.mediahunter.data.mappers.toReleases
import com.c4entertainment.mediahunter.data.network.api.TMDBMovieAPI
import com.c4entertainment.mediahunter.domain.model.movie_details.Release
import com.c4entertainment.mediahunter.domain.model.movie_details.ReleaseEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Upcoming and recent US movie releases from TMDB: discover finds the movies with a release in the
 * window, then each movie's own release dates give the exact kind and day.
 */
@Singleton
class ReleasesRepository @Inject constructor(
    private val api: TMDBMovieAPI,
) {
    /** US release dates per movie id, least recently used first; moving between weeks reuses them. */
    private val cache = object : LinkedHashMap<Int, List<Release>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<Release>>) = size > CACHE_SIZE
    }

    /** Entries of [kinds] dated within [start]..[end] inclusive, oldest day first. */
    suspend fun releasesBetween(
        start: LocalDate,
        end: LocalDate,
        kinds: Set<Release.Kind>,
        refresh: Boolean = false,
    ): Result<List<ReleaseEntry>> = try {
        if (refresh) synchronized(cache) { cache.clear() }
        Result.success(load(start, end, kinds))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun load(start: LocalDate, end: LocalDate, kinds: Set<Release.Kind>): List<ReleaseEntry> {
        val types = kinds.flatMap { it.tmdbTypes }.sorted().joinToString("|")
        val movies = LinkedHashMap<Int, com.c4entertainment.mediahunter.data.network.dto.movie.MovieDto>()
        var page = 1
        do {
            val response = api.discoverReleases(
                releaseTypes = types,
                from = start.toString(),
                to = end.toString(),
                page = page,
            )
            response.results.orEmpty()
                // Drop obscure titles: little voted on and little looked at.
                .filter { (it.voteCount ?: 0) >= 5 || (it.popularity ?: 0.0) >= 5.0 }
                .forEach { movies.putIfAbsent(it.id, it) }
            val lastPage = response.totalPages ?: page
        } while (++page <= minOf(lastPage, MAX_PAGES))

        val permits = Semaphore(CONCURRENCY)
        return coroutineScope {
            movies.values.map { movie ->
                async {
                    val releases = synchronized(cache) { cache[movie.id] }
                        ?: permits.withPermit { api.getReleaseDates(movie.id) }
                            .results.firstOrNull { it.country == "US" }
                            ?.releaseDates.orEmpty().toReleases()
                            .also { synchronized(cache) { cache[movie.id] = it } }
                    releases.mapNotNull { release ->
                        val date = runCatching { LocalDate.parse(release.date) }.getOrNull()
                        if (release.kind !in kinds || date == null || date < start || date > end) null
                        else ReleaseEntry(
                            id = movie.id,
                            title = movie.title ?: movie.originalTitle ?: return@mapNotNull null,
                            posterPath = movie.posterPath?.takeIf { it.isNotBlank() },
                            backdropPath = movie.backdropPath?.takeIf { it.isNotBlank() },
                            overview = movie.overview?.takeIf { it.isNotBlank() },
                            year = movie.releaseDate?.take(4)?.takeIf { it.length == 4 },
                            popularity = movie.popularity ?: 0.0,
                            kind = release.kind,
                            date = date,
                        )
                    }
                }
            }.awaitAll().flatten()
        }.sortedWith(compareBy<ReleaseEntry> { it.date }.thenByDescending { it.popularity })
    }

    private val Release.Kind.tmdbTypes: List<Int>
        get() = when (this) {
            Release.Kind.Theatrical -> listOf(2, 3)
            Release.Kind.Digital -> listOf(4)
            Release.Kind.Physical -> listOf(5)
        }

    private companion object {
        const val MAX_PAGES = 3
        const val CONCURRENCY = 6
        const val CACHE_SIZE = 300
    }
}
