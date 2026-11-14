package com.c4entertainment.mediahunter.presentation.movies.catalog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.external.ExternalApp
import com.c4entertainment.mediahunter.domain.model.external.ExternalApps

/**
 * Asks what to do with an album or song: search torrents, or hand it to each
 * registered downloader app (or send the user to get it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicActionSheet(
    item: CatalogItem,
    apps: List<ExternalApp>,
    onSearchTorrents: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            item.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_search_torrents)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.ManageSearch, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onSearchTorrents),
            )
            apps.forEach { app ->
                if (ExternalApps.isInstalled(context, app)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.action_download_with, app.name)) },
                        leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                        modifier = Modifier.clickable {
                            context.sendToApp(app, item.searchTerms)
                            onDismiss()
                        },
                    )
                } else {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.action_get_app, app.name)) },
                        supportingContent = { Text(stringResource(R.string.action_get_app_summary)) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                        modifier = Modifier.clickable {
                            context.openInstallPage(app)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

/** Hands [query] to [app]; a Toast says so when it can't be opened. */
fun Context.sendToApp(app: ExternalApp, query: String) {
    runCatching { startActivity(app.intentFor(query).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure {
            Toast.makeText(this, getString(R.string.downloader_failed, app.name), Toast.LENGTH_SHORT).show()
        }
}

/** Opens [app]'s download page in the browser. */
fun Context.openInstallPage(app: ExternalApp) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.installUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
