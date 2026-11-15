package com.c4entertainment.mediahunter.components.movie_listing

import com.c4entertainment.mediahunter.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.c4entertainment.mediahunter.components.Chip
import com.c4entertainment.mediahunter.domain.model.HomeSections
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.util.MovieTVFilterConfig

/** Home's section picker: Movies and TV from TMDB, then the catalog sections. */
@Composable
fun MovieTVChip(
    movieTV: String,
    toggleMovie: () -> Unit,
    toggleTV: () -> Unit,
    selectedSection: CatalogSection? = null,
    /** Null hides the catalog chips. */
    onSectionSelected: ((CatalogSection) -> Unit)? = null,
    /** Chip order as [HomeSections] ids. */
    order: List<String> = HomeSections.defaultOrder,
    releasesSelected: Boolean = false,
    onReleasesSelected: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        order.forEach { id ->
            if (id == HomeSections.RELEASES) {
                if (onReleasesSelected != null) Chip(
                    selected = releasesSelected,
                    onSelectedChange = onReleasesSelected,
                ) { Text(text = stringResource(R.string.home_releases)) }
                return@forEach
            }
            when (val section = HomeSections.catalogSection(id)) {
                null -> {
                    val isMovie = id == MovieTVFilterConfig.MOVIE
                    Chip(
                        selected = selectedSection == null && !releasesSelected && movieTV == id,
                        onSelectedChange = if (isMovie) toggleMovie else toggleTV,
                    ) {
                        Text(text = stringResource(if (isMovie) R.string.home_movies else R.string.home_tv_shows))
                    }
                }

                else -> if (onSectionSelected != null) Chip(
                    selected = selectedSection == section,
                    onSelectedChange = { onSectionSelected(section) },
                ) {
                    Text(text = stringResource(section.labelRes))
                }
            }
        }
    }
}
