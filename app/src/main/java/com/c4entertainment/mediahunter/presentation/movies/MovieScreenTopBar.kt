package com.c4entertainment.mediahunter.presentation.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.c4entertainment.mediahunter.R
import kotlin.math.roundToInt

/**
 * Title row (app name, Settings, overflow menu) with a search bar below it.
 * The whole block slides away with [scrollBehavior], like a plain TopAppBar would.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MovieScreenTopBar(
    isGridView: Boolean,
    showViewToggle: Boolean,
    toggleView: () -> Unit,
    onSearchClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onTorrentSearchClicked: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            // The status bar stays covered; only the block below it slides away.
            .windowInsetsPadding(WindowInsets.statusBars)
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                // Tell the scroll behaviour how far the block can slide, then follow its offset.
                scrollBehavior.state.heightOffsetLimit = -placeable.height.toFloat()
                val offset = scrollBehavior.state.heightOffset.roundToInt()
                layout(placeable.width, (placeable.height + offset).coerceAtLeast(0)) {
                    placeable.placeRelative(0, offset)
                }
            }
    ) {
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.app_name)) },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            actions = {
                IconButton(onClick = onSettingsClicked) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.home_settings),
                    )
                }
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.home_more_options),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_open_torrentsearch)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ManageSearch, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onTorrentSearchClicked()
                        },
                    )
                    if (showViewToggle) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (isGridView) R.string.home_list_view else R.string.home_grid_view
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                toggleView()
                            },
                        )
                    }
                }
            },
        )
        HomeSearchBar(onClick = onSearchClicked)
    }
}

/** Looks like a search field but is just a button that opens the Search screen. */
@Composable
private fun HomeSearchBar(onClick: () -> Unit) {
    val label = stringResource(R.string.home_search_open)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(min = 48.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.home_search_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
