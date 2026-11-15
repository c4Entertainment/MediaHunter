package com.c4entertainment.mediahunter.presentation.movie_info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c4entertainment.mediahunter.domain.repository.MovieRepository
import com.c4entertainment.mediahunter.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
) : ViewModel() {

    private val id = savedStateHandle.get<String>("id")?.toIntOrNull() ?: 0
    private val type = savedStateHandle.get<String>("type") ?: "movie"

    var state by mutableStateOf(MovieDetailsState())
        private set

    /** The video playing inline under "Videos", if any. */
    var playingVideoId by mutableStateOf<String?>(null)
        private set

    /** Where the inline player was last, so a rebuilt player resumes there. */
    var playbackSecond = 0f
    /** True right after a tap, so the player starts rather than only cues. */
    var autoplay = false

    fun play(videoId: String) {
        if (videoId == playingVideoId) return
        playbackSecond = 0f
        autoplay = true
        playingVideoId = videoId
    }

    fun stopPlaying() {
        playingVideoId = null
        playbackSecond = 0f
    }

    init {
        load()
    }

    /** Loads the page; with content already shown, as a pull to refresh. */
    fun load() {
        val refreshing = state.details != null
        state = state.copy(isLoading = !refreshing, isRefreshing = refreshing, error = null)
        viewModelScope.launch {
            state = when (val result = repository.getTitleDetails(id = id, type = type)) {
                is Resource.Success -> state.copy(details = result.data, isLoading = false, isRefreshing = false)
                else -> state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    // A failed refresh keeps the page it already has.
                    error = if (state.details == null) result.errorMessage ?: "An unexpected error occurred" else null,
                )
            }
        }
    }
}
