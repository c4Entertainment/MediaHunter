package com.c4entertainment.mediahunter.components.movie_listing.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.domain.model.movie.Movie
import com.c4entertainment.mediahunter.util.TmdbImage

/** Backdrop row for the list view, shaped and padded to sit with the grid cards. */
@Composable
fun MovieListItem(movie: Movie, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                onClickLabel = stringResource(R.string.media_card_open, movie.title),
                onClick = onClick,
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = TmdbImage.url(TmdbImage.LIST_BACKDROP, movie.backdropPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            error = painterResource(id = R.drawable.baseline_image_24),
        )
        // Scrim so the white title stays readable on any backdrop.
        Row(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = .55f))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = movie.title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(.8f)
            )
            MovieListItemInfo(
                modifier = Modifier.weight(3f),
                info = movie.releaseDate.take(4),
            )
        }
    }
}
