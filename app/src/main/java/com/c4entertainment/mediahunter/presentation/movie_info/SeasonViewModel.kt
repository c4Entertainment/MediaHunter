package com.c4entertainment.mediahunter.presentation.movie_info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c4entertainment.mediahunter.domain.model.movie_details.SeasonDetails
import com.c4entertainment.mediahunter.domain.model.movie_details.TitleDetails
import com.c4entertainment.mediahunter.domain.repository.MovieRepository
import com.c4entertainment.mediahunter.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeasonState(
    /** The show, for its name, backdrop and the torrent search target. */
    val show: TitleDetails? = null,
    val season: SeasonDetails? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SeasonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
) : ViewModel() {

    val tvId = savedStateHandle.get<String>("tvId")?.toIntOrNull() ?: 0
    val seasonNumber = savedStateHandle.get<String>("season")?.toIntOrNull() ?: 1

    var state by mutableStateOf(SeasonState())
        private set

    init {
        load()
    }

    fun load() {
        val refreshing = state.season != null
        state = state.copy(isLoading = !refreshing, isRefreshing = refreshing, error = null)
        viewModelScope.launch {
            val show = async { repository.getTitleDetails(id = tvId, type = "tv") }
            val season = async { repository.getSeason(tvId = tvId, season = seasonNumber) }
            val showResult = show.await()
            val seasonResult = season.await()
            state = if (showResult is Resource.Success && seasonResult is Resource.Success) {
                state.copy(show = showResult.data, season = seasonResult.data, isLoading = false, isRefreshing = false)
            } else {
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    // A failed refresh keeps the page it already has.
                    error = if (state.season == null) {
                        (seasonResult.errorMessage ?: showResult.errorMessage) ?: "An unexpected error occurred"
                    } else {
                        null
                    },
                )
            }
        }
    }
}
