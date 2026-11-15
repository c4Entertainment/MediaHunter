package com.c4entertainment.mediahunter.navigation

import com.c4entertainment.mediahunter.presentation.search.SearchSection
import com.c4entertainment.mediahunter.util.Constants.MOVIE_SCREEN
import com.c4entertainment.mediahunter.util.Constants.SEARCH_SCREEN

sealed class Screen(val route: String) {
    object MovieScreen: Screen(MOVIE_SCREEN)

    /** Takes the chip to open on, e.g. `Search?section=Songs`; Movies when left out. */
    object SearchScreen: Screen("$SEARCH_SCREEN?section={section}") {
        fun createRoute(section: SearchSection) = "$SEARCH_SCREEN?section=${section.name}"
    }

    object SettingsScreen: Screen("Settings")
}
