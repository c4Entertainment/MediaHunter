package com.c4entertainment.mediahunter.components.movie_listing.item

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.c4entertainment.mediahunter.components.StatusPill
import com.c4entertainment.mediahunter.domain.model.TitleStatus
import androidx.compose.ui.Modifier
import com.c4entertainment.mediahunter.components.MediaCard
import com.c4entertainment.mediahunter.domain.model.movie.Movie
import com.c4entertainment.mediahunter.util.TmdbImage

@Composable
fun MovieGridItem(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: TitleStatus? = null,
) {
    MediaCard(
        title = movie.title,
        subtitle = movie.releaseDate.take(4),
        imageUrl = TmdbImage.url(TmdbImage.GRID_POSTER, movie.posterPath),
        aspectRatio = 2f / 3f,
        onClick = onClick,
        modifier = modifier,
        overlay = status?.let { { StatusPill(it, Modifier.align(Alignment.TopStart).padding(8.dp)) } },
    )
}
