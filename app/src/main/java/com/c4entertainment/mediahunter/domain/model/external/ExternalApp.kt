package com.c4entertainment.mediahunter.domain.model.external

import android.content.Context
import android.content.Intent
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection

/** An app Media Hunter can hand a search to, like a torrent client for magnets. */
data class ExternalApp(
    val id: String,
    val name: String,
    val packageName: String,
    /** Where to get the app when it isn't installed. */
    val installUrl: String,
    /** Which catalog sections it makes sense for. */
    val sections: Set<CatalogSection>,
    /** Builds the hand-off intent for a search text (e.g. "Artist Album"). */
    val intentFor: (query: String) -> Intent,
)

/**
 * Every app Media Hunter knows how to hand a search to.
 *
 * Adding an app = one entry in [all] plus a `<package>` line in the manifest's
 * `<queries>` block (without it [isInstalled] is always false on API 30+).
 */
object ExternalApps {
    private val YTDLNIS = ExternalApp(
        id = "ytdlnis",
        name = "YTDLnis",
        packageName = "com.deniscerri.ytdl",
        installUrl = "https://github.com/deniscerri/ytdlnis/releases",
        sections = setOf(CatalogSection.Albums, CatalogSection.Songs, CatalogSection.Artists),
        // YTDLnis accepts shared plain text; we only send it an intent, not its code.
        intentFor = { query ->
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, query)
                .setPackage("com.deniscerri.ytdl")
        },
    )

    val all: List<ExternalApp> = listOf(YTDLNIS)

    /** The apps that make sense for [section]. */
    fun forSection(section: CatalogSection): List<ExternalApp> =
        all.filter { section in it.sections }

    fun isInstalled(context: Context, app: ExternalApp): Boolean =
        context.packageManager.getLaunchIntentForPackage(app.packageName) != null
}
