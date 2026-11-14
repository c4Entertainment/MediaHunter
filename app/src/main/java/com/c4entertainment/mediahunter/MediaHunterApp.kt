package com.c4entertainment.mediahunter

import android.app.Application
import com.c4entertainment.mediahunter.domain.repository.UserSettingsRepository
import com.c4entertainment.mediahunter.lock.AppLock
import com.prajwalch.torrentsearch.TorrentSearch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MediaHunterApp: Application() {
    @Inject lateinit var userSettingsRepository: UserSettingsRepository

    override fun onCreate() {
        super.onCreate()
        TorrentSearch.init(this)
        AppLock.install(this, userSettingsRepository)
    }
}
