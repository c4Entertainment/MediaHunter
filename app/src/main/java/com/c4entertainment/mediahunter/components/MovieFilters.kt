package com.c4entertainment.mediahunter.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.domain.model.movie_details.Genre
import java.time.Year

private const val FIRST_YEAR = 1950

/** Genre and Year dropdown chips above the Movies/TV list; null means Any. */
@Composable
fun MovieFilters(
    genres: List<Genre>,
    genre: Int?,
    year: Int?,
    onChange: (genre: Int?, year: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        var genreOpen by remember { mutableStateOf(false) }
        val genreName = genres.firstOrNull { it.id == genre }?.name
        Box {
            FilterChip(
                selected = genre != null,
                onClick = { genreOpen = true },
                label = {
                    Text(
                        genreName?.let { stringResource(R.string.music_genre_value, it) }
                            ?: stringResource(R.string.music_genre)
                    )
                },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            )
            DropdownMenu(
                expanded = genreOpen,
                onDismissRequest = { genreOpen = false },
                modifier = Modifier.heightIn(max = 400.dp),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.filter_any)) },
                    onClick = {
                        genreOpen = false
                        onChange(null, year)
                    },
                )
                genres.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g.name.orEmpty()) },
                        onClick = {
                            genreOpen = false
                            onChange(g.id, year)
                        },
                    )
                }
            }
        }

        var yearOpen by remember { mutableStateOf(false) }
        Box {
            FilterChip(
                selected = year != null,
                onClick = { yearOpen = true },
                label = {
                    Text(
                        year?.let { stringResource(R.string.filter_year_value, it) }
                            ?: stringResource(R.string.filter_year)
                    )
                },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            )
            DropdownMenu(
                expanded = yearOpen,
                onDismissRequest = { yearOpen = false },
                modifier = Modifier.heightIn(max = 400.dp),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.filter_any)) },
                    onClick = {
                        yearOpen = false
                        onChange(genre, null)
                    },
                )
                for (y in Year.now().value downTo FIRST_YEAR) {
                    DropdownMenuItem(
                        text = { Text(y.toString()) },
                        onClick = {
                            yearOpen = false
                            onChange(genre, y)
                        },
                    )
                }
            }
        }
    }
}
