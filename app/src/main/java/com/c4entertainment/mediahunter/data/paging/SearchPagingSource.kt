package com.c4entertainment.mediahunter.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.c4entertainment.mediahunter.data.mappers.toMovie
import com.c4entertainment.mediahunter.data.network.api.TMDBMovieAPI
import com.c4entertainment.mediahunter.domain.model.movie.Movie

/**
 * One TMDB search, paged in memory. Unlike the Room-backed search it belongs to a single
 * ([type], [query]) pair, so Movies and TV results never overwrite each other.
 */
class SearchPagingSource(
    private val movieAPI: TMDBMovieAPI,
    /** "movie" or "tv". */
    private val type: String,
    private val query: String,
) : PagingSource<Int, Movie>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: 1
        return try {
            val response = movieAPI.searchMovies(type = type, query = query, page = page)
            val movies = response.results.orEmpty().map { it.toMovie() }
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
