package com.c4entertainment.mediahunter.components.movie_listing

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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Chip(
            selected = selectedSection == null && movieTV == MovieTVFilterConfig.MOVIE,
            onSelectedChange = toggleMovie,
        ) {
            Text(text = "Movies")
        }

        Chip(
            selected = selectedSection == null && movieTV == MovieTVFilterConfig.TV,
            onSelectedChange = toggleTV,
        ) {
            Text(text = "TV Shows")
        }

        if (onSectionSelected != null) CatalogSection.entries.forEach { section ->
            Chip(
                selected = selectedSection == section,
                onSelectedChange = { onSectionSelected(section) },
            ) {
                Text(text = section.label)
            }
        }
    }
}
