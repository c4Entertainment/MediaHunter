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

    suspend fun setMovieTVBackup(movieTV: String)

    /** Remembers the music chart filters; [genre] null means all genres. */
    suspend fun setMusicChart(region: String, genre: String?)

    /** Remembers the Telegram music bot username; empty clears it. */
    suspend fun setTelegramBot(bot: String)
}