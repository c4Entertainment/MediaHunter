package com.c4entertainment.mediahunter.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.c4entertainment.mediahunter.R

/** Series longer than this get range chips instead of one chip per episode. */
const val EPISODE_RANGE_SIZE = 50

/**
 * Episode chips for 1..[count]. Up to [EPISODE_RANGE_SIZE] they all show; beyond that a row of
 * range chips ("1–50", "51–100"...) picks which slice is drawn, starting with the one holding
 * [latest]. [leading] is drawn before the episode chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EpisodePicker(
    count: Int,
    latest: Int,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    episodeChip: @Composable (Int) -> Unit,
) {
    val ranges = (1..count).chunked(EPISODE_RANGE_SIZE).map { it.first()..it.last() }
    val chipsModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    val spacing = Arrangement.spacedBy(8.dp)

    if (ranges.size <= 1) {
        FlowRow(horizontalArrangement = spacing, modifier = chipsModifier) {
            leading?.invoke()
            for (episode in 1..count) episodeChip(episode)
        }
        return
    }

    var selected by rememberSaveable(count, latest) {
        mutableIntStateOf(((latest.coerceIn(1, count)) - 1) / EPISODE_RANGE_SIZE)
    }
    if (leading != null) {
        FlowRow(horizontalArrangement = spacing, modifier = chipsModifier) { leading() }
    }
    LazyRow(
        horizontalArrangement = spacing,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(ranges.size) { index ->
            val range = ranges[index]
            FilterChip(
                selected = selected == index,
                onClick = { selected = index },
                label = { Text(stringResource(R.string.episode_range, range.first, range.last)) },
            )
        }
    }
    FlowRow(horizontalArrangement = spacing, modifier = chipsModifier.padding(top = 8.dp)) {
        for (episode in ranges[selected.coerceIn(0, ranges.lastIndex)]) episodeChip(episode)
    }
}
