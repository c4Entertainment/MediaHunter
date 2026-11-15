package com.c4entertainment.mediahunter.domain.repository

import androidx.paging.PagingData
import com.c4entertainment.mediahunter.data.cache.entity.movie.MovieEntity
import com.c4entertainment.mediahunter.data.cache.entity.search.MovieSearchEntity
import com.c4entertainment.mediahunter.domain.model.movie_details.SeasonDetails
import com.c4entertainment.mediahunter.domain.model.movie_details.TitleDetails
import com.c4entertainment.mediahunter.util.Resource
import kotlinx.coroutines.flow.Flow

interface MovieRepository {

    fun getMovies(sort: Boolean = false, type: String): Flow<PagingData<MovieEntity>>

    fun searchMovies(sort: Boolean = false, type:String, query: String): Flow<PagingData<MovieSearchEntity>>

    suspend fun getTitleDetails(id: Int, type: String): Resource<TitleDetails>

    suspend fun getSeason(tvId: Int, season: Int): Resource<SeasonDetails>

    suspend fun clearSearch(): Flow<Resource<Boolean>>

}