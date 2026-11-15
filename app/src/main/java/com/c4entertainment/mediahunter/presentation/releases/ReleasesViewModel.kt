package com.c4entertainment.mediahunter.presentation.releases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c4entertainment.mediahunter.data.repository.ReleasesRepository
import com.c4entertainment.mediahunter.domain.model.movie_details.Release
import com.c4entertainment.mediahunter.domain.model.movie_details.ReleaseEntry
import com.c4entertainment.mediahunter.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class ReleasesUiState(
    val weekStart: LocalDate,
    val kinds: Set<Release.Kind> = DEFAULT_KINDS,
    val entries: List<ReleaseEntry> = emptyList(),
    val isLoading: Boolean = true,
    /** A pull-to-refresh is running over entries that are already on screen. */
    val isRefreshing: Boolean = false,
    val error: String? = null,
) {
    val weekEnd: LocalDate get() = weekStart.plusDays(6)
}

val DEFAULT_KINDS = setOf(Release.Kind.Digital, Release.Kind.Physical)

/** First day of the week containing [date], by the device's locale. */
fun weekStartOf(date: LocalDate): LocalDate {
    val first: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    return date.with(TemporalAdjusters.previousOrSame(first))
}

@HiltViewModel
class ReleasesViewModel @Inject constructor(
    private val repository: ReleasesRepository,
    private val settings: UserSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReleasesUiState(weekStart = weekStartOf(LocalDate.now())))
    val state: StateFlow<ReleasesUiState> = _state.asStateFlow()

    private val refreshTick = MutableStateFlow(0)
    private var handledTick = 0

    init {
        val kinds = settings.userSettings
            .map { s -> s.releaseKinds.mapNotNull { n -> Release.Kind.entries.firstOrNull { it.name == n } }.toSet() }
            .map { it.ifEmpty { DEFAULT_KINDS } }
            .distinctUntilChanged()
        viewModelScope.launch {
            combine(_state.map { it.weekStart }.distinctUntilChanged(), kinds, refreshTick) { week, k, tick ->
                Triple(week, k, tick)
            }.collectLatest { (week, k, tick) -> load(week, k, refresh = tick != handledTick) }
        }
    }

    private suspend fun load(week: LocalDate, kinds: Set<Release.Kind>, refresh: Boolean) {
        handledTick = refreshTick.value
        _state.update {
            if (refresh && it.entries.isNotEmpty()) it.copy(kinds = kinds, isRefreshing = true, error = null)
            else it.copy(kinds = kinds, entries = emptyList(), isLoading = true, isRefreshing = false, error = null)
        }
        repository.releasesBetween(week, week.plusDays(6), kinds, refresh)
            .onSuccess { entries ->
                _state.update { it.copy(entries = entries, isLoading = false, isRefreshing = false) }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(isLoading = false, isRefreshing = false, error = e.message ?: "Couldn't load")
                }
            }
    }

    fun shiftWeek(weeks: Long) = _state.update { it.copy(weekStart = it.weekStart.plusWeeks(weeks)) }

    fun goToThisWeek() = _state.update { it.copy(weekStart = weekStartOf(LocalDate.now())) }

    fun toggleKind(kind: Release.Kind) {
        val current = _state.value.kinds
        val next = if (kind in current) current - kind else current + kind
        // At least one kind stays on, or the calendar would always be empty.
        if (next.isEmpty()) return
        viewModelScope.launch { settings.setReleaseKinds(next.map { it.name }.toSet()) }
    }

    fun refresh() {
        refreshTick.update { it + 1 }
    }

    fun retry() = refresh()
}
