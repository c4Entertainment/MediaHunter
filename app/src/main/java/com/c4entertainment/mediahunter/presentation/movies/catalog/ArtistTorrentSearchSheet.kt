package com.c4entertainment.mediahunter.presentation.movies.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistAlbum
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistRef

/** Lets the user search torrents for an artist's discography, everything by them, or one album. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistTorrentSearchSheet(
    artist: ArtistRef,
    imageUrl: String?,
    loadAlbums: suspend () -> Result<List<ArtistAlbum>>,
    onSearch: (query: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var albums by remember { mutableStateOf<Result<List<ArtistAlbum>>?>(null) }
    // Bumped by Retry to rerun the load.
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(artist.deezerId, attempt) {
        albums = null
        albums = loadAlbums()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Discography") },
                supportingContent = { Text("Complete collections") },
                modifier = Modifier.clickable { onSearch("${artist.name} discography") }
            )
            ListItem(
                headlineContent = { Text("Everything by ${artist.name}") },
                modifier = Modifier.clickable { onSearch(artist.name) }
            )

            Text(
                text = "Albums",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            val result = albums
            when {
                result == null -> CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp),
                    strokeWidth = 2.dp,
                )

                result.isFailure -> Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = result.exceptionOrNull()?.message ?: "Couldn't load albums",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = { attempt++ }) { Text("Retry") }
                }

                else -> {
                    val list = result.getOrNull().orEmpty()
                    if (list.isEmpty()) {
                        Text(
                            text = "No albums found",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    list.forEach { album ->
                        ListItem(
                            headlineContent = { Text(album.title) },
                            supportingContent = {
                                Text(listOfNotNull(album.year, album.recordType.displayName()).joinToString(" · "))
                            },
                            modifier = Modifier.clickable { onSearch(album.searchTerms) }
                        )
                    }
                }
            }
        }
    }
}

private fun String.displayName() = when (this) {
    "ep" -> "EP"
    "compile" -> "Compilation"
    else -> "Album"
}
