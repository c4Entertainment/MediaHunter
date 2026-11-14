package com.c4entertainment.mediahunter

import android.app.Application
import com.prajwalch.torrentsearch.TorrentSearch
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MediaHunterApp: Application() {
    override fun onCreate() {
        super.onCreate()
        TorrentSearch.init(this)
    }
}
