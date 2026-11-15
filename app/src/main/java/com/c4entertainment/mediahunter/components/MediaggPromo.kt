package com.c4entertainment.mediahunter.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.c4entertainment.mediahunter.R

/** Mediagg, our media player app. For now Media Hunter only advertises it. */
object Mediagg {
    const val PACKAGE = "app.mediagg"
    const val WEBSITE = "https://mediagg.app"

    fun isInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(PACKAGE) != null

    /** Opens Mediagg when it's installed, otherwise its website. */
    fun open(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE))
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // No app can open a web link; nothing sensible to do.
        }
    }
}

/** The advert card on Details. */
@Composable
fun MediaggCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val installed = remember { Mediagg.isInstalled(context) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MediaggIcon(Modifier.size(48.dp))
                Text(
                    text = stringResource(R.string.mediagg_name),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.mediagg_pitch),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = { Mediagg.open(context) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(if (installed) R.string.mediagg_open else R.string.mediagg_get))
            }
        }
    }
}

/** The row in Settings → About. */
@Composable
fun MediaggSettingsRow() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(stringResource(R.string.mediagg_name)) },
        supportingContent = { Text(stringResource(R.string.mediagg_summary)) },
        leadingContent = { MediaggIcon(Modifier.size(24.dp)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.mediagg_open)) { Mediagg.open(context) },
    )
}

@Composable
private fun MediaggIcon(modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.mediagg_icon),
        contentDescription = null,
        modifier = modifier.clip(MaterialTheme.shapes.medium),
    )
}
