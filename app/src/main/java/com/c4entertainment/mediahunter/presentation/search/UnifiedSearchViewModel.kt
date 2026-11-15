package com.c4entertainment.mediahunter.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.c4entertainment.mediahunter.data.network.api.TMDBMovieAPI
import com.c4entertainment.mediahunter.data.paging.SearchPagingSource
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistAlbum
import com.c4entertainment.mediahunter.domain.model.catalog.ArtistRef
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogItem
import com.c4entertainment.mediahunter.domain.model.movie.Movie
import com.c4entertainment.mediahunter.domain.repository.CatalogRepository
import com.c4entertainment.mediahunter.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The outcome of one catalog search, as the screen shows it. */
sealed interface CatalogSearchResult {
    data object Loading : CatalogSearchResult
    data class Success(val items: List<CatalogItem>) : CatalogSearchResult
    data class Error(val message: String) : CatalogSearchResult
}

/** The search that is on screen: blank [query] means nothing has been searched yet. */
data class ActiveSearch(val section: SearchSection, val query: String) {
    val isBlank: Boolean get() = query.isBlank()
}

private data class SearchKey(val section: SearchSection, val query: String)

@HiltViewModel
class UnifiedSearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val movieApi: TMDBMovieAPI,
    private val catalogRepository: CatalogRepository,
    private val userSettingsRepository: UserSettingsRepository,
) : ViewModel() {

    /** What the text field starts with, after rotation or process death too. */
    val initialQuery: String = savedStateHandle.get<String>(KEY_QUERY).orEmpty()

    private val _section = MutableStateFlow(SearchSection.fromName(savedStateHandle.get<String>(KEY_SECTION)))
    val section: StateFlow<SearchSection> = _section.asStateFlow()

    /** The query the results below answer; it trails the text field by the debounce. */
    private val _active = MutableStateFlow(ActiveSearch(_section.value, ""))
    val active: StateFlow<ActiveSearch> = _active.asStateFlow()

    private var currentQuery = initialQuery
    private var debounceJob: Job? = null

    private val catalogResults = MutableStateFlow<Map<SearchKey, CatalogSearchResult>>(emptyMap())
    private val catalogJobs = mutableMapOf<SearchKey, Job>()
    private val pagers = mutableMapOf<SearchKey, Flow<PagingData<Movie>>>()

    val catalogResult: StateFlow<CatalogSearchResult?> = combine(_active, catalogResults) { active, results ->
        if (active.isBlank || active.section.catalog == null) null
        else results[SearchKey(active.section, active.query)] ?: CatalogSearchResult.Loading
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val movieResults: Flow<PagingData<Movie>> = _active.flatMapLatest { active ->
        val type = active.section.tmdbType
        if (active.isBlank || type == null) flowOf(PagingData.empty())
        else pagers.getOrPut(SearchKey(active.section, active.query)) {
            Pager(PagingConfig(pageSize = 20)) {
                SearchPagingSource(movieApi, type, active.query)
            }.flow.cachedIn(viewModelScope)
        }
    }

    val useGrid: StateFlow<Boolean> = userSettingsRepository.userSettings
        .map { it.useGrid }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val recentSearches: StateFlow<List<String>> = userSettingsRepository.userSettings
        .map { it.recentSearches }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val telegramBot: StateFlow<String> = userSettingsRepository.userSettings
        .map { it.telegramBot }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        // A restored query searches again without being recorded a second time.
        if (initialQuery.isNotBlank()) run(initialQuery.trim(), record = false)
    }

    suspend fun kitsuIdFor(aniListId: Long): String? = catalogRepository.kitsuIdForAniList(aniListId)

    suspend fun artistAlbums(ref: ArtistRef): Result<List<ArtistAlbum>> =
        catalogRepository.artistAlbums(ref.deezerId, ref.name)

    /** Typing: search once the user pauses; a blank query goes back to the recent searches. */
    fun onQueryChange(query: String) {
        currentQuery = query
        savedStateHandle[KEY_QUERY] = query
        debounceJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _active.value = ActiveSearch(_section.value, "")
            return
        }
        debounceJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            run(trimmed, record = trimmed.length >= MIN_RECORDED_LENGTH)
        }
    }

    /** Enter: search now, and remember it whatever its length. */
    fun onSubmit() {
        debounceJob?.cancel()
        val trimmed = currentQuery.trim()
        if (trimmed.isNotEmpty()) run(trimmed, record = true)
    }

    /** A recent search: fill the field (done by the screen) and search it. */
    fun onRecentSelected(query: String) {
        currentQuery = query
        savedStateHandle[KEY_QUERY] = query
        onSubmit()
    }

    /** Switching chips keeps the query and runs it for the new one. */
    fun onSectionSelected(section: SearchSection) {
        _section.value = section
        savedStateHandle[KEY_SECTION] = section.name
        debounceJob?.cancel()
        val trimmed = currentQuery.trim()
        if (trimmed.isEmpty()) _active.value = ActiveSearch(section, "") else run(trimmed, record = false)
    }

    fun retry() {
        val active = _active.value
        if (active.isBlank) return
        val key = SearchKey(active.section, active.query)
        catalogJobs.remove(key)?.cancel()
        catalogResults.value = catalogResults.value - key
        run(active.query, record = false)
    }

    fun clearRecentSearches() {
        viewModelScope.launch { userSettingsRepository.clearRecentSearches() }
    }

    private fun run(query: String, record: Boolean) {
        val section = _section.value
        _active.value = ActiveSearch(section, query)
        if (record) viewModelScope.launch { userSettingsRepository.addRecentSearch(query) }
        val catalog = section.catalog ?: return
        val key = SearchKey(section, query)
        // Finished and in-flight searches are reused; only a failure runs again.
        if (catalogJobs[key]?.isActive == true || catalogResults.value[key] is CatalogSearchResult.Success) return
        catalogResults.value = catalogResults.value + (key to CatalogSearchResult.Loading)
        catalogJobs[key] = viewModelScope.launch {
            val result = catalogRepository.search(catalog, query).fold(
                onSuccess = { CatalogSearchResult.Success(it) },
                onFailure = { CatalogSearchResult.Error(it.message ?: "Couldn't search ${section.label.lowercase()}") },
            )
            catalogResults.value = catalogResults.value + (key to result)
        }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_SECTION = "section"
        const val SEARCH_DEBOUNCE_MS = 400L
        const val MIN_RECORDED_LENGTH = 2
    }
}
