package com.c4entertainment.mediahunter.presentation.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.c4entertainment.mediahunter.data.mappers.toMovie
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistAlbum
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistRef
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection
import com.c4entertainment.mediahunter.domain.model.catalog.MusicChart
import com.c4entertainment.mediahunter.domain.model.catalog.MusicGenre
import com.c4entertainment.mediahunter.domain.model.catalog.MusicRegion
import com.c4entertainment.mediahunter.domain.model.movie.Movie
import com.c4entertainment.mediahunter.domain.repository.CatalogRepository
import com.c4entertainment.mediahunter.domain.repository.MovieRepository
import com.c4entertainment.mediahunter.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    val TAG = "MovieViewModel"

    /** The Kitsu ID Torrentio uses for an AniList anime, or null if it can't be found. */
    suspend fun kitsuIdFor(aniListId: Long): String? = catalogRepository.kitsuIdForAniList(aniListId)

    val settingsUiState: StateFlow<SettingsUiState> =
        userSettingsRepository.userSettings
            .map { userData ->
                SettingsUiState.Success(
                    settings = UserEditableSettings(
                        movieTV = userData.movieTV,
                        useGrid = userData.useGrid,
                    ),
                )
            }
            .stateIn(
                scope = viewModelScope,
                // Starting eagerly means the user data is ready when the SettingsDialog is laid out
                // for the first time. Without this, due to b/221643630 the layout is done using the
                // "Loading" text, then replaced with the user editable fields once loaded, however,
                // the layout height doesn't change meaning all the fields are squashed into a small
                // scrollable column.
                // TODO: Change to SharingStarted.WhileSubscribed(5_000) when b/221643630 is fixed
                started = SharingStarted.Eagerly,
                initialValue = SettingsUiState.Loading,
            )

    private val _searchedMovies = MutableStateFlow<PagingData<Movie>>(PagingData.empty())
    val searchedMovies = _searchedMovies

    /** The non-TMDB section shown on Home, or null when showing movies/TV. */
    private val _selectedSection = MutableStateFlow<CatalogSection?>(null)
    val selectedSection: StateFlow<CatalogSection?> = _selectedSection.asStateFlow()

    private val _catalogState = MutableStateFlow(CatalogUiState())
    val catalogState: StateFlow<CatalogUiState> = _catalogState.asStateFlow()

    private val catalogCache = mutableMapOf<Pair<CatalogSection, MusicChart?>, List<CatalogItem>>()
    private var catalogJob: Job? = null

    /** The Telegram music bot username, empty when unset; the share sheets label themselves with it. */
    val telegramBot: StateFlow<String> = userSettingsRepository.userSettings
        .map { it.telegramBot }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _musicChart = MutableStateFlow(MusicChart())
    val musicChart: StateFlow<MusicChart> = _musicChart.asStateFlow()

    init {
        // The remembered chart has to be in place before the first music load.
        viewModelScope.launch {
            val settings = userSettingsRepository.userSettings.first()
            // A chart picked before this arrived wins over the stored one.
            if (_musicChart.value == MusicChart()) {
                _musicChart.value = MusicChart(
                    region = MusicRegion.entries.firstOrNull { it.name == settings.musicRegion }
                        ?: MusicRegion.Worldwide,
                    genre = MusicGenre.entries.firstOrNull { it.name == settings.musicGenre },
                )
            }
            val section = _selectedSection.value
            if (section != null && section.isMusic) {
                loadCatalog(section, forceRefresh = false)
            }
        }
    }

    /** The albums of a Deezer artist, for the artist sheet. */
    suspend fun artistAlbums(ref: ArtistRef): Result<List<ArtistAlbum>> =
        catalogRepository.artistAlbums(ref.deezerId, ref.name)


    fun onMovieEvent(event: MovieEvent) {
        when (event) {
            is MovieEvent.OnMovieTVToggled -> {
                catalogJob?.cancel()
                _selectedSection.value = null
                viewModelScope.launch {
                    userSettingsRepository.setMovieTV(event.movieTV)
                    getMovies(event.movieTV)
                }
            }

            is MovieEvent.OnSortToggled -> {
                viewModelScope.launch {
                    userSettingsRepository.toggleSort(event.sort)
//                    getMovies(event.sort, searchQuery.value)
                }
            }

            is MovieEvent.OnCatalogSectionSelected -> {
                _selectedSection.value = event.section
                loadCatalog(event.section, forceRefresh = false)
            }

            is MovieEvent.OnMusicChartChanged -> {
                val chart = event.chart
                _musicChart.value = chart
                viewModelScope.launch {
                    userSettingsRepository.setMusicChart(chart.region.name, chart.genre?.name)
                }
                val section = _selectedSection.value
                if (section != null && section.isMusic) loadCatalog(section, forceRefresh = false)
            }

            MovieEvent.OnCatalogRefresh -> {
                val section = _selectedSection.value ?: return
                loadCatalog(section, forceRefresh = true)
            }

            is MovieEvent.OnToggleView -> {
                viewModelScope.launch {
                    toggleView(event.isGrid)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            getMovies(userSettingsRepository.userSettings.first().movieTV)
        }
    }


    private fun getMovies(type: String) {
        viewModelScope.launch {
            repository.getMovies(
                type = type
            ).map { pagingData ->
                pagingData.map { movieEntity ->
                    movieEntity.toMovie()
                }
            }.cachedIn(viewModelScope).collect {
                _searchedMovies.value = it
            }
        }
    }

    private fun loadCatalog(section: CatalogSection, forceRefresh: Boolean) {
        catalogJob?.cancel()
        val chart = _musicChart.value.takeIf { section.isMusic }
        val cacheKey = section to chart
        val cached = catalogCache[cacheKey].orEmpty()
        _catalogState.value = CatalogUiState(items = cached, isLoading = forceRefresh || cached.isEmpty())
        if (!forceRefresh && cached.isNotEmpty()) return

        catalogJob = viewModelScope.launch {
            catalogRepository.getItems(section, chart ?: MusicChart())
                .onSuccess { items ->
                    catalogCache[cacheKey] = items
                    _catalogState.value = CatalogUiState(items = items)
                }
                .onFailure { error ->
                    _catalogState.value = CatalogUiState(
                        items = cached,
                        error = error.message ?: "Couldn't load ${section.label.lowercase()}",
                    )
                }
        }
    }

    private fun toggleView(isGrid: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.useGrid(isGrid)
        }
    }

}

data class CatalogUiState(
    val items: List<CatalogItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class UserEditableSettings(
    val movieTV: String,
    val useGrid: Boolean,
)

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Success(val settings: UserEditableSettings) : SettingsUiState
}