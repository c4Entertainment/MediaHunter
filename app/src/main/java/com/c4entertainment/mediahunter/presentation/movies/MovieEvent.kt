package com.c4entertainment.mediahunter.presentation.movies

import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.domain.model.catalog.MusicChart

sealed class MovieEvent {
    data class OnMovieTVToggled(val movieTV: String): MovieEvent()

    data class OnSortToggled(val sort: Boolean): MovieEvent()

    /** Genre and year for the current Movies/TV mode; null means Any. */
    data class OnHomeFilterChanged(val genre: Int?, val year: Int?): MovieEvent()

    data class OnToggleView(val isGrid: Boolean): MovieEvent()

    data class OnCatalogSectionSelected(val section: CatalogSection): MovieEvent()
    object OnCatalogRefresh: MovieEvent()
    data class OnMusicChartChanged(val chart: MusicChart): MovieEvent()
}
