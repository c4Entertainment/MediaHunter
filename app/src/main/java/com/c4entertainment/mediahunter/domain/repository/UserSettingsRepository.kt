package com.c4entertainment.mediahunter.domain.repository

import com.c4entertainment.mediahunter.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {

    /**
     * Stream of [UserSettings]
     */
    val userSettings: Flow<UserSettings>

    /**
     * Sets the desired dark theme config.
     */
    suspend fun setDarkThemeConfig(darkThemeConfig: String)

    /**
     * Sets the preferred dynamic color config.
     */
    suspend fun setDynamicColorPreference(useDynamicColor: Boolean)

    /** Black backgrounds in dark theme. */
    suspend fun setPureBlack(pureBlack: Boolean)

    /**
     * Sets whether user should view list or grid
     */
    suspend fun useGrid(useGrid: Boolean)

    /**
     * Sets whether user should use fingerprint
     */
    suspend fun useFingerPrint(useFingerPrint: Boolean)

    /**
     * Sets whether movies should be sort
     */
    suspend fun toggleSort(sort: Boolean)

    suspend fun toggleShowVideos(showVideos: Boolean)

    suspend fun setMovieTV(movieTV: String)

    /** Remembers the music chart filters; [genre] null means all genres. */
    suspend fun setMusicChart(region: String, genre: String?)

    /** Section Home opens on at app start; null keeps the last Movies/TV choice. */
    suspend fun setStartOn(section: String?)

    /** Home's chip order; an empty list restores the default. */
    suspend fun setSectionOrder(order: List<String>)

    /** Remembers Home's genre and year filters for [type] ("movie" or "tv"); null means Any. */
    suspend fun setHomeFilter(type: String, genre: Int?, year: Int?)

    /** Remembers the Telegram music bot username; empty clears it. */
    suspend fun setTelegramBot(bot: String)

    /** Remembers the user's own TMDB API key; blank clears it and the built-in key is used again. */
    suspend fun setTmdbApiKey(key: String)

    /** Remembers the user's own RAWG API key; blank clears it. */
    suspend fun setRawgApiKey(key: String)

    /** Remembers which release kinds the Releases calendar shows. */
    suspend fun setReleaseKinds(kinds: Set<String>)

    /** Remembers a Search screen query, newest first, without duplicates. */
    suspend fun addRecentSearch(query: String)

    suspend fun clearRecentSearches()
}