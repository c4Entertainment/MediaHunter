package com.c4entertainment.mediahunter.data.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.c4entertainment.mediahunter.data.cache.MovieDatabase
import com.c4entertainment.mediahunter.data.cache.entity.movie.MovieEntity
import com.c4entertainment.mediahunter.data.cache.entity.search.MovieSearchEntity
import com.c4entertainment.mediahunter.data.mappers.toSeasonDetails
import com.c4entertainment.mediahunter.data.mappers.toTitleDetails
import com.c4entertainment.mediahunter.domain.model.movie_details.SeasonDetails
import com.c4entertainment.mediahunter.domain.model.movie_details.TitleDetails
import com.c4entertainment.mediahunter.data.network.MovieNetworkMediator
import com.c4entertainment.mediahunter.data.network.SearchMovieNetworkMediator
import com.c4entertainment.mediahunter.data.network.api.TMDBMovieAPI
import com.c4entertainment.mediahunter.domain.repository.MovieRepository
import com.c4entertainment.mediahunter.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    val tmDBApi: TMDBMovieAPI,
    val movieDatabase: MovieDatabase
) : MovieRepository {

    private val TAG = "MovieRepositoryImpl"

    private val movieDao = movieDatabase.movieDao
    private val movieSearchDao = movieDatabase.movieSearchDao

    @OptIn(ExperimentalPagingApi::class)
    override fun getMovies(sort: Boolean, type: String): Flow<PagingData<MovieEntity>> {
        val pagingSourceFactory = { movieDao.getMovies() }
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = MovieNetworkMediator(
                movieApi = tmDBApi,
                movieDb = movieDatabase,
                type = type,
                sort = sort,

            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun searchMovies(sort: Boolean, type:String, query: String): Flow<PagingData<MovieSearchEntity>> {
        val pagingSourceFactory = { movieSearchDao.getMovies() }
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = SearchMovieNetworkMediator(
                movieApi = tmDBApi,
                movieDb = movieDatabase,
                query = query,
                type = type,
                sort = sort,

            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
    }

    override suspend fun getTitleDetails(id: Int, type: String): Resource<TitleDetails> =
        networkCall { tmDBApi.getMovieDetails(id = id, type = type).toTitleDetails(type) }

    override suspend fun getSeason(tvId: Int, season: Int): Resource<SeasonDetails> =
        networkCall { tmDBApi.getSeason(id = tvId, season = season).toSeasonDetails(season) }

    private inline fun <T> networkCall(call: () -> T): Resource<T> =
        try {
            Resource.Success(call())
        } catch (e: IOException) {
            Resource.Error(e.message ?: "An error occurred")
        } catch (e: HttpException) {
            Resource.Error(e.message ?: "An error occurred")
        }

    override suspend fun clearSearch(): Flow<Resource<Boolean>> {
        return flow {
            movieSearchDao.clearAll()
            emit(Resource.Success(true))
        }
    }
}