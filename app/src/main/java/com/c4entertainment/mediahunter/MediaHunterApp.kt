package com.c4entertainment.mediahunter

import android.app.Application
import com.c4entertainment.mediahunter.domain.repository.UserSettingsRepository
import com.c4entertainment.mediahunter.lock.AppLock
import com.c4entertainment.mediahunter.util.DarkThemeConfig
import com.prajwalch.torrentsearch.TorrentSearch
import com.prajwalch.torrentsearch.domain.model.DarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MediaHunterApp: Application() {
    @Inject lateinit var userSettingsRepository: UserSettingsRepository

    override fun onCreate() {
        super.onCreate()
        // Media Hunter's Settings own the theme and language for the whole app.
        TorrentSearch.init(this, hostManagesAppearance = true, hostManagesLanguage = true)
        AppLock.install(this, userSettingsRepository)
        syncAppearanceToTorrentSearch()
    }

    /** Keeps TorrentSearch's screens themed like the rest of the app. */
    private fun syncAppearanceToTorrentSearch() {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            userSettingsRepository.userSettings
                .map { Triple(it.darkThemeConfig, it.useDynamicColor, it.pureBlack) }
                .distinctUntilChanged()
                .collect { (darkThemeConfig, dynamicColor, pureBlack) ->
                    TorrentSearch.setAppearance(
                        darkTheme = when (darkThemeConfig) {
                            DarkThemeConfig.LIGHT -> DarkTheme.Off
                            DarkThemeConfig.DARK -> DarkTheme.On
                            else -> DarkTheme.FollowSystem
                        },
                        dynamicTheme = dynamicColor,
                        pureBlack = pureBlack,
                    )
                }
        }
    }
}
