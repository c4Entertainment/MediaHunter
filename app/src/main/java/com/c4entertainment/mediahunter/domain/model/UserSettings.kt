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

package com.c4entertainment.mediahunter.domain.model

/**
 * Class summarizing user interest data
 */
data class UserSettings(
    val darkThemeConfig: String,
    val useDynamicColor: Boolean,
    /** Black instead of dark grey backgrounds in dark theme (AMOLED). */
    val pureBlack: Boolean,
    val useGrid: Boolean,
    val useFingerPrint: Boolean,
    val sort: Boolean,
    val showVideos: Boolean,
    val movieTV: String,
    /** [com.c4entertainment.mediahunter.domain.model.catalog.MusicRegion] name. */
    val musicRegion: String = "Worldwide",
    /** [com.c4entertainment.mediahunter.domain.model.catalog.MusicGenre] name, or null for all genres. */
    val musicGenre: String? = null,
    /** Telegram username (no "@") of the music bot searches are sent to; empty means the chat picker. */
    val telegramBot: String = "",
    /** Home genre filter (TMDB genre id) and release year per mode; null means Any. */
    val movieGenre: Int? = null,
    val movieYear: Int? = null,
    val tvGenre: Int? = null,
    val tvYear: Int? = null,
    /** Section Home opens on at app start: a [HomeSections] id, or null to keep the last Movies/TV choice. */
    val startOn: String? = null,
    /** Home's chip order as [HomeSections] ids; always lists every section. */
    val sectionOrder: List<String> = HomeSections.defaultOrder,
    /** Queries run on the Search screen, newest first. */
    val recentSearches: List<String> = emptyList(),
)
