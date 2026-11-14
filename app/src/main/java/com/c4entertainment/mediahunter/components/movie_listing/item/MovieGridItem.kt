package com.c4entertainment.mediahunter.components.movie_listing.item

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.domain.model.movie.Movie
import com.c4entertainment.mediahunter.ui.theme.MediaHunterTheme

@Composable
fun MovieGridItem(
    movie: Movie,
    modifier: Modifier = Modifier
) {

    Box(modifier = modifier) {

        Column(
            modifier = modifier
        ) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/original/${movie.posterPath}",
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxSize()
                    .defaultMinSize(minHeight = 190.dp),
                placeholder = painterResource(id = R.drawable.baseline_image_24),
                error = painterResource(id = R.drawable.baseline_image_24),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = modifier,
//                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = movie.releaseDate,
                    style = MaterialTheme.typography.bodySmall,
                )

            }
//            Divider()
        }

    }


}

@Preview
@Composable
fun MovieGridItemPreview() {
    MediaHunterTheme {


    }
}