package com.c4entertainment.mediahunter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.domain.model.TitleStatus
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A dark pill with a coloured dot, readable on any poster: where a title is available right now. */
@Composable
fun StatusPill(status: TitleStatus, modifier: Modifier = Modifier) {
    val date = status.date?.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())) ?: ""
    val label = when (status.kind) {
        TitleStatus.Kind.Coming -> stringResource(R.string.status_coming, date)
        TitleStatus.Kind.Announced -> stringResource(R.string.status_announced)
        TitleStatus.Kind.InCinemas -> stringResource(R.string.status_in_cinemas)
        TitleStatus.Kind.DigitalSoon -> stringResource(R.string.status_digital_soon, date)
        TitleStatus.Kind.Digital -> stringResource(R.string.status_digital)
        TitleStatus.Kind.OnDisc -> stringResource(R.string.status_on_disc)
        TitleStatus.Kind.NewEpisode -> stringResource(R.string.status_new_episode, date)
        TitleStatus.Kind.Returning -> stringResource(R.string.status_returning)
        TitleStatus.Kind.Ended -> stringResource(R.string.status_ended)
        TitleStatus.Kind.Cancelled -> stringResource(R.string.status_cancelled)
        TitleStatus.Kind.Upcoming -> stringResource(R.string.status_upcoming)
    }
    val dot = when (status.kind.tone) {
        TitleStatus.Tone.Available -> MaterialTheme.colorScheme.tertiary
        TitleStatus.Tone.Upcoming -> MaterialTheme.colorScheme.primary
        TitleStatus.Tone.Neutral -> Color.Gray
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
    }
}
