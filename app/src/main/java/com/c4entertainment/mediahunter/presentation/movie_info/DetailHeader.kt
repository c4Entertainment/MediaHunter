package com.c4entertainment.mediahunter.presentation.movie_info

import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.components.rememberPulseAlpha
import com.c4entertainment.mediahunter.domain.model.movie_details.Cast
import com.c4entertainment.mediahunter.util.TmdbImage

private val PosterWidth = 116.dp
private val PosterShape = RoundedCornerShape(8.dp)
private const val POSTER_RATIO = 2f / 3f

/** How far the poster stands up into the backdrop. */
private val PosterOverlap = 72.dp

/** Below the bar, where there is no backdrop. */
private val HeaderTopGap = 16.dp

private const val BACKDROP_RATIO = 9f / 16f
private const val BACKDROP_DRIFT_MILLIS = 12_000

/** The backdrop's smallest enlargement, and how much more it grows across the drift. */
private const val BACKDROP_ZOOM = 1.12f
private const val BACKDROP_ZOOM_TRAVEL = 0.12f

/** Laid out this much larger than its frame and only ever shrunk, so the drift never blurs it. */
private const val BACKDROP_OVERSCAN = 1.25f

/** How far it moves sideways, end to end, as a share of its width. */
private const val BACKDROP_PAN = 0.10f

private const val GLOW_ALPHA = 0.38f
private const val GLOW_CENTRE = 0.32f
private const val GLOW_RADIUS = 0.62f
private const val GLOW_FADE_MILLIS = 600

/** True when the user turned animations off in the system settings. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** How far below the top the header's words start: the status bar and the floating bar. */
@Composable
fun detailBarRoom() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + DetailBarHeight

/**
 * The top of a Details page: the backdrop across the top (drifting slowly, fading into the page),
 * with the poster standing on its lower edge beside the title, tagline, facts and Find torrents.
 * Without a backdrop the poster sits on a glow of its own colour.
 */
@Composable
fun DetailHeader(
    title: String,
    tagline: String?,
    meta: String?,
    posterUrl: String?,
    backdropUrl: String?,
    seed: Color?,
    onFindTorrents: () -> Unit,
) {
    val barRoom = detailBarRoom()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val backdropHeight = maxWidth * BACKDROP_RATIO
        if (backdropUrl != null) {
            Backdrop(backdropUrl, Modifier.fillMaxWidth().height(backdropHeight))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (backdropUrl == null) Modifier.artworkGlow(seed) else Modifier)
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = if (backdropUrl != null) {
                        (backdropHeight - PosterOverlap).coerceAtLeast(barRoom)
                    } else {
                        barRoom + HeaderTopGap
                    },
                ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .width(PosterWidth)
                    .shadow(12.dp, PosterShape)
                    .clip(PosterShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(POSTER_RATIO),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                tagline?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                meta?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Button(
                    onClick = onFindTorrents,
                    modifier = Modifier.padding(top = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.details_find_torrents))
                }
            }
        }
    }
}

/** The backdrop with a slow pan and zoom, fading into the page at the bottom. */
@Composable
private fun Backdrop(url: String, modifier: Modifier) {
    val reduceMotion = rememberReduceMotion()
    val drift: State<Float> = if (reduceMotion) {
        remember { mutableFloatStateOf(0.5f) }
    } else {
        rememberInfiniteTransition(label = "backdrop").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(BACKDROP_DRIFT_MILLIS, easing = EaseInOutSine),
                RepeatMode.Reverse,
            ),
            label = "drift",
        )
    }
    BoxWithConstraints(modifier.clipToBounds()) {
        val frameWidth = maxWidth
        val frameHeight = maxHeight
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .requiredSize(frameWidth * BACKDROP_OVERSCAN, frameHeight * BACKDROP_OVERSCAN)
                .graphicsLayer {
                    // Read here, not in composition: it changes every frame.
                    val at = drift.value
                    val scale = (BACKDROP_ZOOM + BACKDROP_ZOOM_TRAVEL * at) / BACKDROP_OVERSCAN
                    scaleX = scale
                    scaleY = scale
                    translationX = (at - 0.5f) * frameWidth.toPx() * BACKDROP_PAN
                },
        )
        val ground = MaterialTheme.colorScheme.surface
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(ground.copy(alpha = 0f), ground))),
        )
    }
}

/** A soft radial light in the poster's colour, faded in once the colour is known. */
@Composable
private fun Modifier.artworkGlow(seed: Color?): Modifier {
    val colour = seed ?: return this
    val strength = remember(colour) { Animatable(0f) }
    LaunchedEffect(colour) { strength.animateTo(GLOW_ALPHA, tween(GLOW_FADE_MILLIS)) }
    return drawBehind {
        val centre = Offset(size.width / 2, size.height * GLOW_CENTRE)
        drawRect(
            Brush.radialGradient(
                colors = listOf(colour.copy(alpha = strength.value), colour.copy(alpha = 0f)),
                center = centre,
                radius = maxOf(size.width, size.height) * GLOW_RADIUS,
            ),
        )
    }
}

/** An overview cut to four lines, with Show more only when it was actually cut. */
@Composable
fun DetailOverview(text: String, modifier: Modifier = Modifier) {
    var expanded by remember(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        SelectionContainer {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                // Only measured while collapsed, or expanding would take the toggle away.
                onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .clickable { expanded = !expanded },
            )
        }
        if (overflows) {
            Text(
                stringResource(if (expanded) R.string.details_show_less else R.string.details_show_more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { expanded = !expanded },
            )
        }
    }
}

/** Genres as pills, then labelled lines; anything missing is left out. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailFacts(genres: List<String>, facts: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    if (genres.isEmpty() && facts.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (genres.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                genres.forEach { genre ->
                    Text(
                        genre,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
        facts.forEach { (label, value) ->
            Row {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(96.dp),
                )
                Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DetailSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { heading() },
    )
}

/** Width of a poster card in a row, and of a landscape one. */
val PosterCardWidth = 132.dp
val LandscapeCardWidth = 192.dp

/** A titled horizontal row of cards. */
fun <T> LazyListScope.detailRow(
    key: String,
    title: @Composable () -> String,
    items: List<T>,
    itemKey: (T) -> Any,
    header: (@Composable () -> Unit)? = null,
    card: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = key) {
        Column(Modifier.fillMaxWidth()) {
            DetailSectionTitle(title())
            header?.invoke()
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items, key = itemKey) { card(it) }
            }
        }
    }
}

/** The cast, as round faces; tapping one searches the web for them. */
fun LazyListScope.castRow(cast: List<Cast>, onLookUp: (String) -> Unit) {
    val people = cast.filter { !it.name.isNullOrBlank() }
    if (people.isEmpty()) return
    item(key = "cast") {
        Column(Modifier.fillMaxWidth()) {
            DetailSectionTitle(stringResource(R.string.details_cast))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(people, key = { "${it.id}-${it.character}" }) { person ->
                    Face(person, onClick = { onLookUp(person.name.orEmpty()) })
                }
            }
        }
    }
}

private val FaceSize = 96.dp

@Composable
private fun Face(person: Cast, onClick: () -> Unit) {
    val name = person.name.orEmpty()
    Column(
        modifier = Modifier
            .width(FaceSize)
            .clickable(
                onClickLabel = stringResource(R.string.details_actor_search, name),
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(FaceSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (person.profilePath.isNullOrBlank()) {
                // Initials say who, where a row of grey silhouettes would only say "missing".
                Text(
                    initials(name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                AsyncImage(
                    model = TmdbImage.url("w185", person.profilePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        person.character?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "Bela Lugosi" is "BL". */
private fun initials(name: String): String {
    val words = name.split(' ').filter { it.firstOrNull()?.isLetter() == true }
    return listOfNotNull(words.firstOrNull()?.first(), words.drop(1).lastOrNull()?.first())
        .joinToString("")
        .uppercase()
}

/** The page's own shape in grey while it loads: poster, three lines of text, a few rows. */
@Composable
fun DetailLoading(modifier: Modifier = Modifier) {
    val alpha by rememberPulseAlpha()
    val block = Modifier.alpha(alpha).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = detailBarRoom() + HeaderTopGap),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                Modifier
                    .width(PosterWidth)
                    .aspectRatio(POSTER_RATIO)
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.surfaceVariant, PosterShape),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(block.fillMaxWidth(0.8f).height(24.dp))
                Box(block.fillMaxWidth(0.6f).height(14.dp))
                Box(block.fillMaxWidth(0.4f).height(14.dp))
            }
        }
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(3) { Box(block.fillMaxWidth().height(14.dp)) }
        }
        repeat(2) {
            Box(Modifier.padding(horizontal = 16.dp).then(block).width(120.dp).height(18.dp))
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(3) {
                    Box(
                        Modifier
                            .width(PosterCardWidth - 12.dp)
                            .aspectRatio(POSTER_RATIO)
                            .alpha(alpha)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                    )
                }
            }
        }
    }
}
