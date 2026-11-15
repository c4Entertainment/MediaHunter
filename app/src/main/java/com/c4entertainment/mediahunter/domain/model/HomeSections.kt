package com.c4entertainment.mediahunter.domain.model

import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.util.MovieTVFilterConfig

/**
 * Ids of the chips on Home: [MovieTVFilterConfig.MOVIE], [MovieTVFilterConfig.TV] and the
 * [CatalogSection] names. The chip order and the start section are stored as these ids.
 */
object HomeSections {
    /** Id of the Releases calendar, which is a section but not a [CatalogSection]. */
    const val RELEASES = "Releases"

    val defaultOrder: List<String> =
        listOf(MovieTVFilterConfig.MOVIE, MovieTVFilterConfig.TV, RELEASES) + CatalogSection.entries.map { it.name }

    /** [stored] without unknown ids, with any missing section appended, so every chip always shows. */
    fun resolveOrder(stored: List<String>): List<String> {
        val kept = stored.filter { it in defaultOrder }.distinct()
        return kept + defaultOrder.filterNot { it in kept }
    }

    fun catalogSection(id: String?): CatalogSection? = CatalogSection.entries.firstOrNull { it.name == id }
}
