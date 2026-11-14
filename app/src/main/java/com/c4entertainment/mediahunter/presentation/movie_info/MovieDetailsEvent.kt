package com.c4entertainment.mediahunter.presentation.movie_info

sealed class MovieDetailsEvent {
    object OnStart : MovieDetailsEvent()
    object OnRefresh : MovieDetailsEvent()
}
