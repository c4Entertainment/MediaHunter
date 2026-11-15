package com.c4entertainment.mediahunter.components

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Pulsing alpha for placeholders; static when the user turned animations off. */
@Composable
private fun rememberPulseAlpha(): State<Float> {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }
    if (reduceMotion) return remember { mutableFloatStateOf(0.7f) }
    return rememberInfiniteTransition(label = "placeholderPulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "placeholderAlpha",
    )
}

/** A grid of poster-shaped boxes standing in for the first page of results. */
@Composable
fun LoadingPlaceholders(
    modifier: Modifier = Modifier,
    aspectRatio: Float = 2f / 3f,
    minCellSize: Dp = 128.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val alpha by rememberPulseAlpha()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCellSize),
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = contentPadding,
    ) {
        items(List(24) { it }) {
            Box(
                Modifier
                    .padding(start = 1.dp, end = 1.dp, bottom = 5.dp)
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .alpha(alpha)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

/** A column of backdrop-height boxes for the list layout. */
@Composable
fun LoadingListPlaceholders(
    modifier: Modifier = Modifier,
    itemHeight: Dp = 220.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val alpha by rememberPulseAlpha()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = contentPadding,
    ) {
        items(List(6) { it }) {
            Box(
                Modifier
                    .padding(bottom = 4.dp)
                    .fillMaxWidth()
                    .height(itemHeight)
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}
