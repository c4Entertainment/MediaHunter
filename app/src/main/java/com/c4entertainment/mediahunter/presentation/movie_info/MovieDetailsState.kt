package com.c4entertainment.mediahunter.presentation.movie_info

import com.c4entertainment.mediahunter.domain.model.movie_details.TitleDetails

data class MovieDetailsState(
    val details: TitleDetails? = null,
    /** First load, with nothing to show yet. */
    val isLoading: Boolean = true,
    /** Pull to refresh, over what is already shown. */
    val isRefreshing: Boolean = false,
    val error: String? = null,
)
