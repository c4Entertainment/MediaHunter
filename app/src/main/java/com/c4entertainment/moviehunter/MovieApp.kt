package com.c4entertainment.moviehunter

import android.app.Application
import com.c4entertainment.moviehunter.preferences.SettingsDataStore
import com.prajwalch.torrentsearch.TorrentSearch
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MovieApp: Application() {
    override fun onCreate() {
        // Must run before either app's DataStore is opened.
        SettingsDataStore.migrateLegacyFile(this)
        super.onCreate()
        TorrentSearch.init(this)
    }
}
