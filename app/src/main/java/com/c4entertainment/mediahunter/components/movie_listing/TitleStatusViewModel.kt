package com.c4entertainment.mediahunter.components.movie_listing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.ViewModel
import com.c4entertainment.mediahunter.data.repository.ReleasesRepository
import com.c4entertainment.mediahunter.domain.model.TitleStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Gives browse cards the status pill; the repository caches, so scrolling back is free. */
@HiltViewModel
class TitleStatusViewModel @Inject constructor(
    private val repository: ReleasesRepository,
) : ViewModel() {
    fun cached(id: Int, type: String) = repository.cachedStatus(id, type)
    suspend fun status(id: Int, type: String) = repository.status(id, type)
}

/** The status for a card, null until known; fetched only while the card is composed. */
@Composable
fun rememberTitleStatus(viewModel: TitleStatusViewModel, id: Int, type: String): TitleStatus? {
    val status by produceState(viewModel.cached(id, type), id, type) {
        if (value == null) value = viewModel.status(id, type)
    }
    return status
}
