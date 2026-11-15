package com.c4entertainment.mediahunter.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.c4entertainment.mediahunter.data.mappers.toMovie
import com.c4entertainment.mediahunter.data.network.api.TMDBMovieAPI
import com.c4entertainment.mediahunter.domain.model.movie.Movie

/** TMDB discover results for one genre and/or year, most popular first, paged in memory. */
class DiscoverPagingSource(
    private val movieAPI: TMDBMovieAPI,
    /** "movie" or "tv". */
    private val type: String,
    private val genreId: Int?,
    private val year: Int?,
) : PagingSource<Int, Movie>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: 1
        return try {
            val isTv = type == "tv"
            val response = movieAPI.discover(
                type = type,
                genre = genreId,
                releaseYear = year.takeIf { !isTv },
                firstAirYear = year.takeIf { isTv },
                page = page,
            )
            // Discover results carry no media_type; Details needs it from the list.
            val movies = response.results.orEmpty().map { it.toMovie().copy(mediaType = type) }
            val lastPage = response.totalPages ?: page
            LoadResult.Page(
                data = movies,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (movies.isEmpty() || page >= lastPage) null else page + 1,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }
}
