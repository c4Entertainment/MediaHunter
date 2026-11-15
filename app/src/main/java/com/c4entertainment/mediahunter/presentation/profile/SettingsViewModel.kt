/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.c4entertainment.mediahunter.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c4entertainment.mediahunter.domain.model.HomeSections
import com.c4entertainment.mediahunter.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userDataRepository: UserSettingsRepository,
) : ViewModel() {
    val settingsUiState: StateFlow<SettingsUiState> =
        userDataRepository.userSettings
            .map { userData ->
                SettingsUiState.Success(
                    settings = UserEditableSettings(
                        useGrid  = userData.useGrid,
                        useFingerPrint = userData.useFingerPrint,
                        useDynamicColor = userData.useDynamicColor,
                        pureBlack = userData.pureBlack,
                        darkThemeConfig = userData.darkThemeConfig,
                        telegramBot = userData.telegramBot,
                        hasOwnTmdbKey = userData.tmdbApiKey.isNotEmpty(),
                        startOn = userData.startOn,
                        sectionOrder = userData.sectionOrder,
                    ),
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SettingsUiState.Loading,
            )


    fun setStartOn(section: String?) {
        viewModelScope.launch { userDataRepository.setStartOn(section) }
    }

    fun setSectionOrder(order: List<String>) {
        viewModelScope.launch { userDataRepository.setSectionOrder(order) }
    }

    fun toggleUseGrid(useFingerPrint: Boolean) {
        viewModelScope.launch {
            userDataRepository.useGrid(useFingerPrint)
        }
    }

    fun toggleUseFingerPrint(useFingerPrint: Boolean) {
        viewModelScope.launch {
            userDataRepository.useFingerPrint(useFingerPrint)
        }
    }

    fun updateDarkThemeConfig(darkThemeConfig: String) {
        viewModelScope.launch {
            userDataRepository.setDarkThemeConfig(darkThemeConfig)
        }
    }

    fun updatePureBlack(pureBlack: Boolean) {
        viewModelScope.launch {
            userDataRepository.setPureBlack(pureBlack)
        }
    }

    /** Saves the bot after [normalizeTelegramBot]; blank input clears it. */
    fun updateTelegramBot(input: String) {
        viewModelScope.launch {
            userDataRepository.setTelegramBot(normalizeTelegramBot(input))
        }
    }

    /** Saves the user's own TMDB key; blank switches back to the built-in key. */
    fun updateTmdbApiKey(input: String) {
        viewModelScope.launch {
            userDataRepository.setTmdbApiKey(input.trim())
        }
    }

    fun updateDynamicColorPreference(useDynamicColor: Boolean) {
        viewModelScope.launch {
            userDataRepository.setDynamicColorPreference(useDynamicColor)
        }
    }

}

/**
 * Represents the settings which the user can edit within the app.
 */
data class UserEditableSettings(
    val useGrid: Boolean,
    val useFingerPrint: Boolean,
    val useDynamicColor: Boolean,
    val pureBlack: Boolean,
    val darkThemeConfig: String,
    val telegramBot: String = "",
    /** Whether the user set their own TMDB key; the key itself never leaves the data layer. */
    val hasOwnTmdbKey: Boolean = false,
    val startOn: String? = null,
    val sectionOrder: List<String> = HomeSections.defaultOrder,
)

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Success(val settings: UserEditableSettings) : SettingsUiState
}

/**
 * Reduces whatever the user pasted (@name, t.me/name, a tg:// link...) to the bare username:
 * drops the link prefixes and anything after "?" or "/", then keeps only [A-Za-z0-9_].
 */
fun normalizeTelegramBot(input: String): String {
    var s = input.trim()
    for (prefix in listOf("https://t.me/", "http://t.me/", "t.me/", "tg://resolve?domain=")) {
        if (s.startsWith(prefix, ignoreCase = true)) {
            s = s.substring(prefix.length)
            break
        }
    }
    s = s.removePrefix("@").substringBefore('?').substringBefore('/')
    return s.filter { it.isAsciiLetterOrDigit() || it == '_' }
}

private fun Char.isAsciiLetterOrDigit() = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'
