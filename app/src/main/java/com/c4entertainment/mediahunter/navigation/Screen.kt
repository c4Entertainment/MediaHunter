package com.c4entertainment.mediahunter.navigation

import com.c4entertainment.mediahunter.util.Constants.MOVIE_SCREEN
import com.c4entertainment.mediahunter.util.Constants.SEARCH_SCREEN

sealed class Screen(val route: String) {
    object MovieScreen: Screen(MOVIE_SCREEN)

    object SearchScreen: Screen(SEARCH_SCREEN)

    object SettingsScreen: Screen("Settings")
}
