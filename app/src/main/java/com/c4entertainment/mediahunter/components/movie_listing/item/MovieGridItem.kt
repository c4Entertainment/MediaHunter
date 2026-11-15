package com.c4entertainment.mediahunter.components.movie_listing.item

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.c4entertainment.mediahunter.components.MediaCard
import com.c4entertainment.mediahunter.domain.model.movie.Movie

@Composable
fun MovieGridItem(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MediaCard(
        title = movie.title,
        subtitle = movie.releaseDate.take(4),
        imageUrl = "https://image.tmdb.org/t/p/original/${movie.posterPath}",
        aspectRatio = 2f / 3f,
        onClick = onClick,
        modifier = modifier,
    )
}
