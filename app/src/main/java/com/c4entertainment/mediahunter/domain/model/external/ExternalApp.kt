package com.c4entertainment.mediahunter.domain.model.external

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.domain.model.catalog.CatalogSection

/** What an app's intent needs to know at hand-off time. */
data class HandOff(
    /** The installed package of the app, so the intent can target it. */
    val packageName: String,
    /** The user's Telegram music bot username (no "@"), or null when unset. */
    val telegramBot: String?,
)

/** An app Media Hunter can hand a search to, like a torrent client for magnets. */
data class ExternalApp(
    val id: String,
    val name: String,
    /** Package names in priority order; the first installed one is used. */
    val packageNames: List<String>,
    /** Where to get the app when it isn't installed. */
    val installUrl: String,
    /** Which catalog sections it makes sense for. */
    val sections: Set<CatalogSection>,
    /** Builds the hand-off intent for a search text (e.g. "Artist Album"). */
    val intentFor: (query: String, handOff: HandOff) -> Intent,
    /** The action row's text; may depend on config (the Telegram bot). */
    val label: (context: Context, telegramBot: String?) -> String = { context, _ ->
        context.getString(R.string.action_download_with, name)
    },
    /** Only offered when the user has set a Telegram music bot. */
    val needsBot: Boolean = false,
    /** Runs just before the intent is launched, e.g. to copy the query. */
    val beforeLaunch: ((context: Context, query: String, handOff: HandOff) -> Unit)? = null,
)

/**
 * Every app Media Hunter knows how to hand a search to.
 *
 * Adding an app = one entry in [all] plus a `<package>` line per package in the
 * manifest's `<queries>` block (without it [installedPackage] is always null on API 30+).
 */
object ExternalApps {
    private val YTDLNIS = ExternalApp(
        id = "ytdlnis",
        name = "YTDLnis",
        packageNames = listOf("com.deniscerri.ytdl"),
        installUrl = "https://github.com/deniscerri/ytdlnis/releases",
        sections = setOf(CatalogSection.Albums, CatalogSection.Songs, CatalogSection.Artists),
        // YTDLnis accepts shared plain text; we only send it an intent, not its code.
        intentFor = { query, handOff ->
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, query)
                .setPackage(handOff.packageName)
        },
    )

    /** Official, F-Droid/web, Telegram X, Plus Messenger and Nekogram. */
    private val TELEGRAM_PACKAGES = listOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram",
        "org.telegram.plus",
        "tw.nekomimi.nekogram",
    )

    /** Shares the search through Telegram's chat picker, for any bot or chat. */
    private val TELEGRAM = ExternalApp(
        id = "telegram",
        name = "Telegram",
        packageNames = TELEGRAM_PACKAGES,
        installUrl = "https://telegram.org/apps",
        sections = setOf(CatalogSection.Albums, CatalogSection.Songs, CatalogSection.Artists),
        label = { context, _ -> context.getString(R.string.action_send_to_telegram) },
        intentFor = { query, handOff ->
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, query)
                .setPackage(handOff.packageName)
        },
    )

    /**
     * Opens the user's music bot (set in Settings) with the search pre-filled.
     * Listed only when a bot is set, alongside the plain Telegram share.
     */
    private val TELEGRAM_BOT = ExternalApp(
        id = "telegram_bot",
        name = "Telegram bot",
        packageNames = TELEGRAM_PACKAGES,
        installUrl = "https://telegram.org/apps",
        sections = setOf(CatalogSection.Albums, CatalogSection.Songs, CatalogSection.Artists),
        needsBot = true,
        label = { context, bot -> context.getString(R.string.action_send_to_bot, bot.orEmpty()) },
        intentFor = { query, handOff ->
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("tg://resolve?domain=${handOff.telegramBot}&text=${Uri.encode(query)}"),
            ).setPackage(handOff.packageName)
        },
        // Some clients ignore the `text` parameter, so the clipboard is the fallback.
        beforeLaunch = { context, query, _ ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Search", query))
            Toast.makeText(context, context.getString(R.string.telegram_search_copied), Toast.LENGTH_SHORT).show()
        },
    )

    val all: List<ExternalApp> = listOf(YTDLNIS, TELEGRAM, TELEGRAM_BOT)

    /** Apps to list in Settings; the bot entry is configured by its own row there. */
    val listed: List<ExternalApp> get() = all.filterNot { it.needsBot }

    /** The apps that make sense for [section]; bot entries only when a bot is set. */
    fun forSection(section: CatalogSection, telegramBot: String? = null): List<ExternalApp> =
        all.filter { section in it.sections && (!it.needsBot || !telegramBot.isNullOrEmpty()) }

    /** The first of [app]'s packages that is installed, or null. */
    fun installedPackage(context: Context, app: ExternalApp): String? =
        app.packageNames.firstOrNull { context.packageManager.getLaunchIntentForPackage(it) != null }

    fun isInstalled(context: Context, app: ExternalApp): Boolean =
        installedPackage(context, app) != null
}
