package com.c4entertainment.mediahunter.presentation.releases

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.components.Chip
import com.c4entertainment.mediahunter.components.ErrorMessage
import com.c4entertainment.mediahunter.components.StateMessage
import com.c4entertainment.mediahunter.components.rememberPulseAlpha
import com.c4entertainment.mediahunter.domain.model.movie_details.Release
import com.c4entertainment.mediahunter.domain.model.movie_details.ReleaseEntry
import com.c4entertainment.mediahunter.util.TmdbImage
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Home's Releases section: a week of digital, disc and cinema releases, grouped by day. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReleasesContent(
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReleasesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val thisWeek = weekStartOf(today)
    val groups = remember(state.entries) { state.entries.groupBy { it.date }.toSortedMap() }
    // LazyColumn index of each day's header, for the day pills.
    val headerIndex = remember(groups) {
        var index = 0
        groups.mapValues { (_, list) -> index.also { index += 1 + list.size } }
    }

    Column(modifier.fillMaxSize()) {
        WeekHeader(
            weekStart = state.weekStart,
            weekEnd = state.weekEnd,
            thisWeek = thisWeek,
            onPrevious = { viewModel.shiftWeek(-1) },
            onNext = { viewModel.shiftWeek(1) },
            onReset = viewModel::goToThisWeek,
        )
        DayPills(
            weekStart = state.weekStart,
            today = today,
            daysWithReleases = groups.keys,
            onDayClick = { day ->
                headerIndex[day]?.let { scope.launch { listState.animateScrollToItem(it) } }
            },
        )
        KindChips(state.kinds, viewModel::toggleKind)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> LoadingRows()
                state.error != null -> ErrorMessage(onRetry = viewModel::retry)
                else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
                    if (groups.isEmpty()) {
                        StateMessage(
                            icon = Icons.Outlined.EventBusy,
                            title = stringResource(R.string.releases_empty_title, kindNames(state.kinds)),
                            body = stringResource(R.string.releases_empty_body),
                            actionLabel = stringResource(R.string.releases_next_week),
                            onAction = { viewModel.shiftWeek(1) },
                        )
                    } else {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            groups.forEach { (day, list) ->
                                stickyHeader(key = "day-$day") { DayHeader(day, today) }
                                items(list, key = { "${it.id}-${it.kind}-${it.date}" }) {
                                    ReleaseRow(it) { onMovieClick(it.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun kindNames(kinds: Set<Release.Kind>): String {
    val names = Release.Kind.entries.filter { it in kinds }
        .map { stringResource(it.labelRes).lowercase() }
    return if (names.size < 2) names.joinToString()
    else stringResource(R.string.releases_or, names.dropLast(1).joinToString(", "), names.last())
}

private val Release.Kind.labelRes: Int
    get() = when (this) {
        Release.Kind.Digital -> R.string.releases_kind_digital
        Release.Kind.Physical -> R.string.releases_kind_disc
        Release.Kind.Theatrical -> R.string.releases_kind_cinemas
    }

private fun rangeLabel(start: LocalDate, end: LocalDate): String {
    val locale = Locale.getDefault()
    val day = DateTimeFormatter.ofPattern("d", locale)
    val dayMonth = DateTimeFormatter.ofPattern("d MMM", locale)
    return if (start.month == end.month) "${start.format(day)} – ${end.format(dayMonth)}"
    else "${start.format(dayMonth)} – ${end.format(dayMonth)}"
}

@Composable
private fun WeekHeader(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    thisWeek: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
) {
    val title = when (weekStart) {
        thisWeek -> stringResource(R.string.releases_this_week)
        thisWeek.plusWeeks(1) -> stringResource(R.string.releases_next_week)
        thisWeek.minusWeeks(1) -> stringResource(R.string.releases_last_week)
        else -> rangeLabel(weekStart, weekEnd)
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.releases_prev_week))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                rangeLabel(weekStart, weekEnd),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (weekStart != thisWeek) {
            TextButton(onClick = onReset) { Text(stringResource(R.string.releases_this_week)) }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.releases_next_week_desc))
        }
    }
}

@Composable
private fun DayPills(
    weekStart: LocalDate,
    today: LocalDate,
    daysWithReleases: Set<LocalDate>,
    onDayClick: (LocalDate) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0L..6L) {
            val day = weekStart.plusDays(i)
            val has = day in daysWithReleases
            Column(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (day == today) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    )
                    .clickable(enabled = has) { onDayClick(day) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (has) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (has) MaterialTheme.colorScheme.primary else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun KindChips(kinds: Set<Release.Kind>, onToggle: (Release.Kind) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(Release.Kind.Digital, Release.Kind.Physical, Release.Kind.Theatrical).forEach { kind ->
            Chip(selected = kind in kinds, onSelectedChange = { onToggle(kind) }) {
                Text(stringResource(kind.labelRes))
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, today: LocalDate) {
    val label = day.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    val relative = when (day) {
        today -> stringResource(R.string.releases_today)
        today.plusDays(1) -> stringResource(R.string.releases_tomorrow)
        else -> null
    }
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (relative != null) {
                Text(
                    relative,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ReleaseRow(entry: ReleaseEntry, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .width(56.dp)
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = entry.posterPath?.let { TmdbImage.url(TmdbImage.GRID_POSTER, it) },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            KindBadge(entry.kind)
            val detail = entry.year ?: entry.overview
            if (detail != null) {
                Text(
                    // The year, then the start of the synopsis.
                    listOfNotNull(entry.year, entry.overview).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun KindBadge(kind: Release.Kind) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (kind) {
        Release.Kind.Digital -> colors.tertiaryContainer to colors.onTertiaryContainer
        Release.Kind.Physical -> colors.secondaryContainer to colors.onSecondaryContainer
        Release.Kind.Theatrical -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Text(
        stringResource(kind.labelRes),
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun LoadingRows() {
    val alpha by rememberPulseAlpha()
    Column(Modifier.fillMaxSize().alpha(alpha).padding(top = 8.dp)) {
        repeat(6) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .width(56.dp)
                        .aspectRatio(2f / 3f)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Box(Modifier.width(180.dp).height(14.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.surfaceVariant))
                    Box(Modifier.width(60.dp).height(14.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.surfaceVariant))
                }
            }
        }
    }
}
