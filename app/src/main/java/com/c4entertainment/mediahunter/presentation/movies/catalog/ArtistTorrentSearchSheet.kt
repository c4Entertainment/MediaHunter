package com.c4entertainment.mediahunter.presentation.movies.catalog

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.domain.model.external.ExternalApps
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
    telegramBot: String,
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

    val context = LocalContext.current
    // Every installed app gets its own button; if none is installed, one
    // footer row points at the first app's install page.
    val apps = ExternalApps.forSection(CatalogSection.Artists, telegramBot)
    val installed = apps.filter { ExternalApps.isInstalled(context, it) }
    val missing = apps.firstOrNull()?.takeIf { installed.isEmpty() }
    val downloadButton: @Composable (String) -> Unit = { query ->
        Row {
            installed.forEach { app ->
                IconButton(onClick = {
                    context.sendToApp(app, query, telegramBot)
                    onDismiss()
                }) {
                    Icon(app.icon(), contentDescription = app.label(context, telegramBot))
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { heading() }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.artist_discography)) },
                supportingContent = { Text(stringResource(R.string.artist_discography_summary)) },
                modifier = Modifier.clickable { onSearch("${artist.name} discography") }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.artist_everything_by, artist.name)) },
                trailingContent = { downloadButton(artist.name) },
                modifier = Modifier.clickable { onSearch(artist.name) }
            )

            Text(
                text = stringResource(R.string.artist_albums),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { heading() }
            )
            val result = albums
            when {
                result == null -> CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp),
                    strokeWidth = 2.dp,
                )

                result.isFailure -> Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = result.exceptionOrNull()?.message ?: stringResource(R.string.artist_albums_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.state_retry)) }
                }

                else -> {
                    val list = result.getOrNull().orEmpty()
                    if (list.isEmpty()) {
                        Text(
                            text = stringResource(R.string.artist_no_albums),
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
                            trailingContent = { downloadButton(album.searchTerms) },
                            modifier = Modifier.clickable { onSearch(album.searchTerms) }
                        )
                    }
                }
            }
            if (missing != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_get_app_to_download, missing.name)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                    modifier = Modifier.clickable {
                        context.openInstallPage(missing)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun String.displayName() = stringResource(
    when (this) {
        "ep" -> R.string.album_type_ep
        "compile" -> R.string.album_type_compilation
        else -> R.string.album_type_album
    }
)
