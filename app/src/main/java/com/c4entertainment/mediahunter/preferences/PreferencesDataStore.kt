package com.c4entertainment.mediahunter.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.c4entertainment.mediahunter.MediaHunterApp
import com.c4entertainment.mediahunter.domain.model.HomeSections
import com.c4entertainment.mediahunter.domain.model.UserSettings
import com.c4entertainment.mediahunter.util.DarkThemeConfig
import com.c4entertainment.mediahunter.util.MovieTVFilterConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore
@Inject
constructor(app: MediaHunterApp) {

//    private val datastore: DataStore<Preferences> = app.createDataStore(name = "settings")

    private val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_DATASTORE_NAME)

    private val datastore = app.datastore

    private val scope = CoroutineScope(Main)

    private val TAG = "SettingsDataStore"



    //create a flow to listen to changes in the datastore
     val userSettings = datastore.data.map { preferences ->
        UserSettings(
            useGrid = preferences[USE_GRID] ?: true,
            useDynamicColor = preferences[USE_DYNAMIC_COLOR] ?: true,
            pureBlack = preferences[PURE_BLACK] ?: false,
            useFingerPrint = preferences[USE_FINGERPRINT] ?: false,
            darkThemeConfig  = preferences[DATA_THEME_CONFIG] ?: DarkThemeConfig.FOLLOW_SYSTEM,
            sort = preferences[SORT] ?: false,
            showVideos = preferences[SHOW_VIDEOS] ?: true,
            movieTV = preferences[MOVIE_TV_FILTER] ?: MovieTVFilterConfig.MOVIE,
            musicRegion = preferences[MUSIC_REGION] ?: "Worldwide",
            musicGenre = preferences[MUSIC_GENRE],
            movieGenre = preferences[MOVIE_GENRE],
            movieYear = preferences[MOVIE_YEAR],
            tvGenre = preferences[TV_GENRE],
            tvYear = preferences[TV_YEAR],
            startOn = preferences[START_ON],
            sectionOrder = HomeSections.resolveOrder(
                preferences[SECTION_ORDER]?.split(",")?.filter { it.isNotEmpty() }.orEmpty()
            ),
            telegramBot = preferences[TELEGRAM_BOT] ?: "",
            tmdbApiKey = preferences[TMDB_API_KEY] ?: "",
            recentSearches = preferences[RECENT_SEARCHES]?.split(RECENT_SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty(),
        )
    }

    fun toggleUseGrid(useGrid:Boolean) {
        scope.launch {
            datastore.edit { preferences ->
                preferences[USE_GRID] = useGrid
            }
        }
    }

    fun toggleShowVideos(showVideos:Boolean) {
        scope.launch {
            datastore.edit { preferences ->
                preferences[SHOW_VIDEOS] = showVideos
            }
        }
    }

    fun toggleSort(sort:Boolean) {
        scope.launch {
            datastore.edit { preferences ->
                preferences[SORT] = sort
            }
        }
    }

    fun toggleUseFingerPrint(useFingerPrint:Boolean) {
        scope.launch {
            datastore.edit { preferences ->
                preferences[USE_FINGERPRINT] = useFingerPrint
            }
        }
    }

    fun toggleUseDynamicColor(useDynamicColor: Boolean) {
        scope.launch {
            datastore.edit { preferences ->
                preferences[USE_DYNAMIC_COLOR] = useDynamicColor
            }
        }
    }

    fun setPureBlack(pureBlack: Boolean) {
        scope.launch {
            datastore.edit { preferences ->
                preferences[PURE_BLACK] = pureBlack
            }
        }
    }

    fun setDataThemeConfig(darkThemeConfig: String) {
        scope.launch {
            datastore.edit { preferences ->
                preferences[DATA_THEME_CONFIG] = darkThemeConfig
            }
        }
    }

    fun setMovieTvFilter(movieTvFilter: String) {
        scope.launch {
            datastore.edit { preferences ->
                preferences[MOVIE_TV_FILTER] = movieTvFilter
            }
        }
    }

    fun setMusicChart(region: String, genre: String?) {
        scope.launch {
            datastore.edit { preferences ->
                preferences[MUSIC_REGION] = region
                if (genre != null) preferences[MUSIC_GENRE] = genre else preferences.remove(MUSIC_GENRE)
            }
        }
    }

    fun setStartOn(section: String?) {
        scope.launch {
            datastore.edit { preferences ->
                if (section != null) preferences[START_ON] = section else preferences.remove(START_ON)
            }
        }
    }

    fun setSectionOrder(order: List<String>) {
        scope.launch {
            datastore.edit { preferences ->
                if (order.isEmpty()) preferences.remove(SECTION_ORDER)
                else preferences[SECTION_ORDER] = order.joinToString(",")
            }
        }
    }

    fun setHomeFilter(type: String, genre: Int?, year: Int?) {
        val genreKey = if (type == MovieTVFilterConfig.TV) TV_GENRE else MOVIE_GENRE
        val yearKey = if (type == MovieTVFilterConfig.TV) TV_YEAR else MOVIE_YEAR
        scope.launch {
            datastore.edit { preferences ->
                if (genre != null) preferences[genreKey] = genre else preferences.remove(genreKey)
                if (year != null) preferences[yearKey] = year else preferences.remove(yearKey)
            }
        }
    }

    fun setTelegramBot(bot: String) {
        scope.launch {
            datastore.edit { preferences ->
                if (bot.isEmpty()) preferences.remove(TELEGRAM_BOT) else preferences[TELEGRAM_BOT] = bot
            }
        }
    }

    fun setTmdbApiKey(key: String) {
        scope.launch {
            datastore.edit { preferences ->
                if (key.isEmpty()) preferences.remove(TMDB_API_KEY) else preferences[TMDB_API_KEY] = key
            }
        }
    }

    /** Puts [query] first in the recent searches, dropping a duplicate (ignoring case) and anything past the limit. */
    fun addRecentSearch(query: String) {
        val clean = query.replace(RECENT_SEPARATOR, " ").trim()
        if (clean.isEmpty()) return
        scope.launch {
            datastore.edit { preferences ->
                val current = preferences[RECENT_SEARCHES]?.split(RECENT_SEPARATOR).orEmpty()
                val updated = (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) })
                    .filter { it.isNotEmpty() }
                    .take(MAX_RECENT_SEARCHES)
                preferences[RECENT_SEARCHES] = updated.joinToString(RECENT_SEPARATOR)
            }
        }
    }

    fun clearRecentSearches() {
        scope.launch {
            datastore.edit { preferences -> preferences.remove(RECENT_SEARCHES) }
        }
    }

    companion object {
        const val SETTINGS_DATASTORE_NAME = "media_hunter_settings"

        private val USE_GRID = booleanPreferencesKey("use_grid_key")
        private val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color_key")
        private val PURE_BLACK = booleanPreferencesKey("pure_black_key")
        private val DATA_THEME_CONFIG = stringPreferencesKey("data_theme_config_key")
        private val USE_FINGERPRINT = booleanPreferencesKey("use_fingerprint_key")
        private val SORT = booleanPreferencesKey("sort_key")
        private val SHOW_VIDEOS = booleanPreferencesKey("show_videos_key")
        private val MOVIE_TV_FILTER = stringPreferencesKey("movie_tv_filter_key")

        private val MUSIC_REGION = stringPreferencesKey("music_region_key")
        private val MUSIC_GENRE = stringPreferencesKey("music_genre_key")
        private val MOVIE_GENRE = intPreferencesKey("movie_genre_key")
        private val MOVIE_YEAR = intPreferencesKey("movie_year_key")
        private val TV_GENRE = intPreferencesKey("tv_genre_key")
        private val TV_YEAR = intPreferencesKey("tv_year_key")
        private val START_ON = stringPreferencesKey("start_on_key")
        private val SECTION_ORDER = stringPreferencesKey("section_order_key")
        private val TELEGRAM_BOT = stringPreferencesKey("telegram_bot_key")
        private val TMDB_API_KEY = stringPreferencesKey("tmdb_api_key")
        private val RECENT_SEARCHES = stringPreferencesKey("recent_searches_key")
        private const val RECENT_SEPARATOR = "\n"
        private const val MAX_RECENT_SEARCHES = 10
    }
}