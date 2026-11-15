package com.c4entertainment.mediahunter.presentation.movie_info

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.domain.model.movie.Movie
import com.c4entertainment.mediahunter.domain.model.movie_details.MovieDetails
import com.c4entertainment.mediahunter.util.TmdbImage

/** Full-width 16:9 backdrop with the title on a bottom scrim. */
@Composable
fun MovieDetailsPoster(movie: Movie, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        contentAlignment = Alignment.BottomStart,
    ) {
        AsyncImage(
            model = TmdbImage.url(TmdbImage.BACKDROP_LARGE, movie.backdropPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            placeholder = painterResource(id = R.drawable.baseline_image_24),
            error = painterResource(id = R.drawable.baseline_image_24),
        )
        // Image scrim: black fade with white text stays legible on any backdrop.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 0f,
                    )
                ),
        )
        Text(
            text = movie.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/** Rating, year, runtime and genres as small read-only chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieDetailsFacts(movie: Movie, details: MovieDetails?, modifier: Modifier = Modifier) {
    val facts = buildList {
        if (movie.voteAverage > 0) add(stringResource(R.string.details_rating, "%.1f".format(movie.voteAverage)))
        movie.releaseDate.take(4).takeIf { it.length == 4 }?.let { add(it) }
        details?.runtime?.takeIf { it > 0 }?.let {
            add(
                if (it >= 60) stringResource(R.string.details_runtime_hm, it / 60, it % 60)
                else stringResource(R.string.details_runtime_m, it)
            )
        }
        details?.genres?.mapNotNull { it.name?.takeIf(String::isNotBlank) }?.let { addAll(it) }
    }
    if (facts.isEmpty()) return
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        facts.forEach { InfoChip(it) }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
