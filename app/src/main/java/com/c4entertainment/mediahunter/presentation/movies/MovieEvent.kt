package com.c4entertainment.mediahunter.presentation.movies

import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.domain.model.catalog.MusicChart

sealed class MovieEvent {
    data class OnSearchQueryChange(val query: String): MovieEvent()

    data class OnMovieTVToggled(val movieTV: String): MovieEvent()

    data class OnMovieTVBackupStored(val movieTV: String): MovieEvent()
    data class OnSortToggled(val sort: Boolean): MovieEvent()

    data class OnToggleView(val isGrid: Boolean): MovieEvent()
    object OnSearchInitiated: MovieEvent()

    data class OnCatalogSectionSelected(val section: CatalogSection): MovieEvent()
    object OnCatalogRefresh: MovieEvent()
    data class OnMusicChartChanged(val chart: MusicChart): MovieEvent()
    /** Searches the selected catalog section; a blank query goes back to what's trending. */
    data class OnCatalogSearch(val query: String, val immediate: Boolean = false): MovieEvent()
}
