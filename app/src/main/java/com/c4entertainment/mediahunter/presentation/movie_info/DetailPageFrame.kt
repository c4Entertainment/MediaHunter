package com.c4entertainment.mediahunter.presentation.movie_info

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.c4entertainment.mediahunter.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The floating bar's height, below the status bar. */
val DetailBarHeight = 56.dp

/** Scrolled this far into the header, the bar turns solid and shows the title. */
private val CollapseAfter = 160.dp

/** How much of the poster's colour the solid bar takes over the page's surface. */
private const val BAR_TINT = 0.42f

/** How opaque the round behind a bar button over the header is. */
private const val ROUND_ALPHA = 0.55f

/**
 * A Details page's frame: the header runs to the very top, under the status bar, with Back,
 * Share and More floating over it on rounds that read on any picture. Once the header has
 * scrolled away the bar turns solid in the poster's colour and shows the title and a round
 * Find torrents button.
 *
 * @param list the page's list, whose first item is the header.
 * @param seed the poster's colour, or null for the theme's.
 * @param tmdbUrl the page on themoviedb.org, for Share and Open in TMDB.
 */
@Composable
fun DetailPageFrame(
    title: String,
    list: LazyListState,
    seed: Color?,
    tmdbUrl: String,
    onBack: () -> Unit,
    onFindTorrents: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val collapseAt = with(LocalDensity.current) { CollapseAfter.roundToPx() }
    val collapsed by remember(list, collapseAt) {
        derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > collapseAt }
    }
    val surface = MaterialTheme.colorScheme.surface
    val barColour = seed?.copy(alpha = BAR_TINT)?.compositeOver(surface)
        ?: MaterialTheme.colorScheme.surfaceContainer
    val ground by animateColorAsState(if (collapsed) barColour else Color.Transparent, label = "barGround")
    var menuOpen by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        content()
        // The background also covers the status bar, so the solid bar reaches the top.
        Box(Modifier.fillMaxWidth().background(ground)) {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(DetailBarHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Round(raised = !collapsed) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
                AnimatedVisibility(
                    visible = collapsed,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                if (!collapsed) Spacer(Modifier.weight(1f))
                if (collapsed && onFindTorrents != null) {
                    Surface(
                        onClick = onFindTorrents,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.details_find_torrents))
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Round(raised = !collapsed) {
                    IconButton(onClick = { context.shareLink(tmdbUrl) }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.details_share))
                    }
                }
                Round(raised = !collapsed) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.details_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.details_open_tmdb)) },
                                onClick = {
                                    menuOpen = false
                                    context.openLink(tmdbUrl)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A bar button over the header, on a round of the page's surface so it reads on any picture. */
@Composable
private fun Round(raised: Boolean, button: @Composable () -> Unit) {
    val ground by animateColorAsState(
        if (raised) MaterialTheme.colorScheme.surface.copy(alpha = ROUND_ALPHA) else Color.Transparent,
        label = "roundGround",
    )
    Box(Modifier.padding(2.dp).background(ground, CircleShape)) { button() }
}

/** The poster's most telling colour (vibrant, else dominant), or null until it is known. */
@Composable
fun rememberSeedColor(url: String?): Color? {
    val context = LocalContext.current
    var seed by remember(url) { mutableStateOf<Color?>(null) }
    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // Palette reads the pixels.
            .size(128)
            .build()
        val result = context.imageLoader.execute(request) as? SuccessResult ?: return@LaunchedEffect
        seed = withContext(Dispatchers.Default) {
            val palette = Palette.from(result.image.toBitmap()).generate()
            (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb?.let(::Color)
        }
    }
    return seed
}

fun tmdbUrl(type: String, id: Int) = "https://www.themoviedb.org/$type/$id"

private fun Context.shareLink(url: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
    startActivity(Intent.createChooser(send, null))
}

private fun Context.openLink(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
