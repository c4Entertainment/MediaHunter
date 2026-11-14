package com.c4entertainment.mediahunter.presentation.movies.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.components.movie_listing.PagingDataErrorWidget
import com.c4entertainment.mediahunter.components.movie_listing.PagingDataNoResultsWidget
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.presentation.movies.CatalogUiState
import com.prajwalch.torrentsearch.domain.model.Category

/**
 * The TorrentSearch category to search in when an item of this section is tapped.
 *
 * Music and books search everything: many of their torrents are filed as "Other"
 * or uncategorised, and few providers support those categories.
 */
val CatalogSection.torrentCategory: Category
    get() = when (this) {
        // Music keeps Knaben to music and skips movie/anime-only providers.
        CatalogSection.Albums, CatalogSection.Songs -> Category.Music
        CatalogSection.Books -> Category.All
        CatalogSection.Games -> Category.Games
        CatalogSection.Anime -> Category.Anime
    }

/** Names the data source too, which RAWG's terms require. */
private val CatalogSection.sourceLabel: String
    get() = when (this) {
        CatalogSection.Albums -> "Top albums on Deezer"
        CatalogSection.Songs -> "Top songs on Deezer"
        CatalogSection.Books -> "Trending today on Open Library"
        CatalogSection.Games -> "Popular this year · data from RAWG.io"
        CatalogSection.Anime -> "Trending on AniList"
    }

private val CatalogSection.sourceName: String
    get() = when (this) {
        CatalogSection.Albums, CatalogSection.Songs -> "Deezer"
        CatalogSection.Books -> "Open Library"
        CatalogSection.Games -> "RAWG.io"
        CatalogSection.Anime -> "AniList"
    }

private val CatalogSection.searchHint: String
    get() = when (this) {
        CatalogSection.Albums -> "Search albums or artists"
        CatalogSection.Songs -> "Search songs or artists"
        CatalogSection.Books -> "Search books or authors"
        CatalogSection.Games -> "Search games"
        CatalogSection.Anime -> "Search anime"
    }

/** Styled like the Movies/TV [com.c4entertainment.mediahunter.presentation.search.SearchWidget]; searches as you type. */
@Composable
private fun CatalogSearchField(
    section: CatalogSection,
    initialQuery: String,
    onSearch: (query: String, immediate: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var text by rememberSaveable(section) { mutableStateOf(initialQuery) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(section) { focusRequester.requestFocus() }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onSearch(it, false)
        },
        placeholder = { Text(section.searchHint) },
        singleLine = true,
        shape = RoundedCornerShape(32.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            // Like SearchWidget: the first tap clears the text, the next closes search.
            IconButton(onClick = {
                if (text.isNotEmpty()) {
                    text = ""
                    onSearch("", true)
                } else {
                    onClose()
                }
            }) {
                Icon(Icons.Default.Close, contentDescription = "Clear or close search")
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboard?.hide()
            onSearch(text, true)
        }),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .focusRequester(focusRequester),
    )
}

private val CatalogSection.imageAspectRatio: Float
    get() = when (this) {
        CatalogSection.Albums, CatalogSection.Songs -> 1f
        CatalogSection.Games -> 16f / 9f
        CatalogSection.Books, CatalogSection.Anime -> 2f / 3f
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogContent(
    section: CatalogSection,
    state: CatalogUiState,
    onRefresh: () -> Unit,
    onItemClick: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
    searchOpen: Boolean = false,
    onSearch: (query: String, immediate: Boolean) -> Unit = { _, _ -> },
    onCloseSearch: () -> Unit = {},
) = Column(modifier) {
    if (searchOpen) {
        CatalogSearchField(
            section = section,
            initialQuery = state.query.orEmpty(),
            onSearch = onSearch,
            onClose = onCloseSearch,
        )
    }
    when {
        state.items.isEmpty() && state.isLoading -> Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        state.items.isEmpty() && state.error != null ->
            PagingDataErrorWidget(retry = onRefresh, errorMessage = state.error)

        state.items.isEmpty() -> PagingDataNoResultsWidget()

        else -> PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (section == CatalogSection.Games) 160.dp else 128.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = state.query?.let { "Results for “$it” · ${section.sourceName}" }
                            ?: section.sourceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                items(state.items, key = { it.id }) { item ->
                    CatalogGridItem(
                        item = item,
                        aspectRatio = section.imageAspectRatio,
                        modifier = Modifier
                            .padding(start = 1.dp, end = 1.dp, bottom = 5.dp)
                            .clickable { onItemClick(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogGridItem(
    item: CatalogItem,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.baseline_image_24),
            error = painterResource(id = R.drawable.baseline_image_24),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(4.dp)),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        item.subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
