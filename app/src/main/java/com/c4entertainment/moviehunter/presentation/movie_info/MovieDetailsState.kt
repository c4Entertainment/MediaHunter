package com.c4entertainment.moviehunter.presentation.movie_info

import com.c4entertainment.moviehunter.domain.model.movie_details.MovieDetails
import com.c4entertainment.moviehunter.domain.model.movie_details.TvEpisode
import com.c4entertainment.moviehunter.domain.model.movie_details.TvSeason
import com.c4entertainment.moviehunter.domain.model.movie.Movie

data class MovieDetailsState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val movie: Movie? = null,
    val error: String? = null,

    val movieDetails: MovieDetails? = null,

    // Only loaded for TV shows, to build season/episode torrent searches.
    val tvSeasons: List<TvSeason> = emptyList(),
    val latestEpisode: TvEpisode? = null,


)
