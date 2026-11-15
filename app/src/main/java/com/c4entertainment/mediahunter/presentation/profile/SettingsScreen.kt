package com.c4entertainment.mediahunter.presentation.profile

import com.c4entertainment.mediahunter.components.MediaggSettingsRow
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.fillMaxWidth
import com.c4entertainment.mediahunter.domain.model.HomeSections
import com.c4entertainment.mediahunter.util.MovieTVFilterConfig
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Download
import com.c4entertainment.mediahunter.domain.model.external.ExternalApps
import com.c4entertainment.mediahunter.presentation.movies.catalog.openInstallPage
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.c4entertainment.mediahunter.BuildConfig
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.lock.AppLock
import com.c4entertainment.mediahunter.lock.LockActivity
import com.c4entertainment.mediahunter.ui.theme.supportsDynamicTheming
import com.c4entertainment.mediahunter.util.DarkThemeConfig
import com.prajwalch.torrentsearch.TorrentSearch

private const val SOURCE_URL = "https://github.com/c4Entertainment/MediaHunter"

/** Media Hunter's settings, with TorrentSearch's own settings one tap away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    navigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val settings = (uiState as? SettingsUiState.Success)?.settings
    val lockOn = settings?.useFingerPrint == true
    val canLock = remember { AppLock.canLock(context) }
    var botDialogOpen by rememberSaveable { mutableStateOf(false) }

    // Turning the lock on needs one successful unlock, so nobody sets a lock
    // they can't open.
    val confirmUnlock = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.toggleUseFingerPrint(true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle(stringResource(R.string.settings_security))
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_lock)) },
                supportingContent = {
                    Text(
                        stringResource(
                            if (canLock) R.string.app_lock_summary else R.string.app_lock_unavailable
                        )
                    )
                },
                leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = lockOn && canLock,
                        enabled = canLock,
                        onCheckedChange = { turnOn ->
                            if (turnOn) {
                                confirmUnlock.launch(
                                    Intent(context, LockActivity::class.java)
                                        .putExtra(LockActivity.EXTRA_CONFIRM, true)
                                )
                            } else {
                                viewModel.toggleUseFingerPrint(false)
                            }
                        },
                    )
                },
            )

            HorizontalDivider()
            SectionTitle(stringResource(R.string.settings_appearance))
            if (settings != null) {
                AppearanceSettings(
                    settings = settings,
                    onDarkThemeChange = viewModel::updateDarkThemeConfig,
                    onDynamicColorChange = viewModel::updateDynamicColorPreference,
                    onPureBlackChange = viewModel::updatePureBlack,
                )
            }

            // Per-app language is Android 13+, and applies to the whole app,
            // TorrentSearch's screens included.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_language)) },
                    supportingContent = { Text(stringResource(R.string.app_language_summary)) },
                    leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APP_LOCALE_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                )
                            )
                        }
                    },
                )
            }

            HorizontalDivider()
            SectionTitle(stringResource(R.string.settings_home))
            if (settings != null) {
                HomeSettings(
                    settings = settings,
                    onLayoutChange = viewModel::toggleUseGrid,
                    onStartOnChange = viewModel::setStartOn,
                    onOrderChange = viewModel::setSectionOrder,
                )
            }

            HorizontalDivider()
            SectionTitle(stringResource(R.string.settings_downloader_apps))
            ExternalApps.listed.forEach { app ->
                val installed = ExternalApps.isInstalled(context, app)
                ListItem(
                    headlineContent = { Text(app.name) },
                    supportingContent = {
                        Text(stringResource(if (installed) R.string.downloader_installed else R.string.downloader_not_installed))
                    },
                    leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                    trailingContent = {
                        TextButton(onClick = {
                            if (installed) {
                                ExternalApps.installedPackage(context, app)
                                    ?.let { context.packageManager.getLaunchIntentForPackage(it) }
                                    ?.let { runCatching { context.startActivity(it) } }
                            } else {
                                context.openInstallPage(app)
                            }
                        }) {
                            Text(stringResource(if (installed) R.string.downloader_open else R.string.downloader_get))
                        }
                    },
                )
                if (app.id == "telegram") {
                    val bot = settings?.telegramBot.orEmpty()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.telegram_music_bot)) },
                        supportingContent = {
                            Text(
                                if (bot.isEmpty()) stringResource(R.string.telegram_music_bot_unset)
                                else stringResource(R.string.telegram_music_bot_set, bot)
                            )
                        },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                        modifier = Modifier.clickable { botDialogOpen = true },
                    )
                }
            }
            if (botDialogOpen) {
                TelegramBotDialog(
                    current = settings?.telegramBot.orEmpty(),
                    onSave = { viewModel.updateTelegramBot(it); botDialogOpen = false },
                    onDismiss = { botDialogOpen = false },
                )
            }

            HorizontalDivider()
            SectionTitle(stringResource(R.string.settings_torrent_search))
            ListItem(
                headlineContent = { Text(stringResource(R.string.torrent_search_settings)) },
                supportingContent = { Text(stringResource(R.string.torrent_search_settings_summary)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.ManageSearch, contentDescription = null) },
                modifier = Modifier.clickable {
                    context.startActivity(TorrentSearch.settingsIntent(context))
                },
            )

            HorizontalDivider()
            SectionTitle(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.source_code)) },
                supportingContent = { Text(SOURCE_URL.removePrefix("https://")) },
                leadingContent = { Icon(Icons.Default.Code, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                modifier = Modifier.clickable { context.openUrl(SOURCE_URL) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.send_feedback)) },
                supportingContent = { Text(stringResource(R.string.send_feedback_summary)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                modifier = Modifier.clickable { context.openUrl("$SOURCE_URL/issues") },
            )
            MediaggSettingsRow()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettings(
    settings: UserEditableSettings,
    onDarkThemeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onPureBlackChange: (Boolean) -> Unit,
) {
    val themes = listOf(
        DarkThemeConfig.FOLLOW_SYSTEM to R.string.theme_follow_system,
        DarkThemeConfig.LIGHT to R.string.theme_light,
        DarkThemeConfig.DARK to R.string.theme_dark,
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.theme)) },
        leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
        supportingContent = {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(top = 8.dp)) {
                themes.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = settings.darkThemeConfig == value,
                        onClick = { onDarkThemeChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themes.size),
                    ) {
                        Text(stringResource(label), maxLines = 1)
                    }
                }
            }
        },
    )
    if (supportsDynamicTheming()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.dynamic_color)) },
            supportingContent = { Text(stringResource(R.string.dynamic_color_summary)) },
            leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
            trailingContent = {
                Switch(checked = settings.useDynamicColor, onCheckedChange = onDynamicColorChange)
            },
        )
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.pure_black)) },
        supportingContent = { Text(stringResource(R.string.pure_black_summary)) },
        leadingContent = { Icon(Icons.Default.Contrast, contentDescription = null) },
        trailingContent = {
            Switch(
                checked = settings.pureBlack,
                // Only meaningful when dark theme can be on.
                enabled = settings.darkThemeConfig != DarkThemeConfig.LIGHT,
                onCheckedChange = onPureBlackChange,
            )
        },
    )
}

@Composable
private fun homeSectionLabel(id: String): String = when (id) {
    MovieTVFilterConfig.MOVIE -> stringResource(R.string.home_movies)
    MovieTVFilterConfig.TV -> stringResource(R.string.home_tv_shows)
    else -> HomeSections.catalogSection(id)?.labelRes?.let { stringResource(it) } ?: id
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSettings(
    settings: UserEditableSettings,
    onLayoutChange: (Boolean) -> Unit,
    onStartOnChange: (String?) -> Unit,
    onOrderChange: (List<String>) -> Unit,
) {
    var startDialog by rememberSaveable { mutableStateOf(false) }
    var orderDialog by rememberSaveable { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(stringResource(R.string.home_start_on)) },
        supportingContent = {
            Text(settings.startOn?.let { homeSectionLabel(it) } ?: stringResource(R.string.home_start_on_last))
        },
        leadingContent = { Icon(Icons.Default.Home, contentDescription = null) },
        modifier = Modifier.clickable { startDialog = true },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.home_layout)) },
        leadingContent = { Icon(Icons.Default.GridView, contentDescription = null) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.home_layout_summary))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(top = 8.dp)) {
                    listOf(true to R.string.grid, false to R.string.list).forEachIndexed { index, (grid, label) ->
                        SegmentedButton(
                            selected = settings.useGrid == grid,
                            onClick = { onLayoutChange(grid) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        ) { Text(stringResource(label), maxLines = 1) }
                    }
                }
            }
        },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.home_section_order)) },
        supportingContent = { Text(stringResource(R.string.home_section_order_summary)) },
        leadingContent = { Icon(Icons.Default.SwapVert, contentDescription = null) },
        modifier = Modifier.clickable { orderDialog = true },
    )

    if (startDialog) {
        val options = listOf<String?>(null) + HomeSections.defaultOrder
        AlertDialog(
            onDismissRequest = { startDialog = false },
            title = { Text(stringResource(R.string.home_start_on)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { id ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = settings.startOn == id,
                                    role = Role.RadioButton,
                                    onClick = { onStartOnChange(id); startDialog = false },
                                )
                                .padding(vertical = 4.dp),
                        ) {
                            RadioButton(selected = settings.startOn == id, onClick = null)
                            Text(
                                text = id?.let { homeSectionLabel(it) } ?: stringResource(R.string.home_start_on_last),
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { startDialog = false }) { Text(stringResource(R.string.telegram_music_bot_cancel)) }
            },
        )
    }

    if (orderDialog) {
        val order = settings.sectionOrder
        AlertDialog(
            onDismissRequest = { orderDialog = false },
            title = { Text(stringResource(R.string.home_section_order)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.home_section_order_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    DraggableSectionList(order = order, onOrderChange = onOrderChange)
                }
            },
            confirmButton = {
                TextButton(onClick = { orderDialog = false }) { Text(stringResource(R.string.home_done)) }
            },
            dismissButton = {
                TextButton(onClick = { onOrderChange(emptyList()) }) { Text(stringResource(R.string.home_reset)) }
            },
        )
    }
}

private fun List<String>.moved(from: Int, to: Int): List<String> =
    toMutableList().apply { add(to, removeAt(from)) }

/**
 * Rows that reorder by dragging: grab the handle, or long-press anywhere on a row.
 * The new order is saved when the drag ends. Screen readers get Move up / Move down actions.
 */
@Composable
private fun DraggableSectionList(order: List<String>, onOrderChange: (List<String>) -> Unit) {
    var items by remember(order) { mutableStateOf(order) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeight = 56.dp
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    val haptics = LocalHapticFeedback.current

    fun start(id: String) {
        draggedId = id
        dragOffset = 0f
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    fun drag(id: String, dy: Float) {
        dragOffset += dy
        val from = items.indexOf(id)
        val to = (from + (dragOffset / rowPx).roundToInt()).coerceIn(0, items.lastIndex)
        if (to != from) {
            items = items.moved(from, to)
            dragOffset -= (to - from) * rowPx
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    fun end() {
        if (draggedId != null && items != order) onOrderChange(items)
        draggedId = null
        dragOffset = 0f
    }

    Column {
        items.forEachIndexed { index, id ->
            key(id) {
                val dragging = id == draggedId
                val label = homeSectionLabel(id)
                val moveUp = stringResource(R.string.home_move_up, label)
                val moveDown = stringResource(R.string.home_move_down, label)
                val elevation by animateDpAsState(if (dragging) 6.dp else 0.dp, label = "drag elevation")
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (dragging) MaterialTheme.colorScheme.surfaceContainerHighest
                    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                    shadowElevation = elevation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                        .pointerInput(id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { start(id) },
                                onDrag = { change, amount -> change.consume(); drag(id, amount.y) },
                                onDragEnd = ::end,
                                onDragCancel = ::end,
                            )
                        }
                        .semantics(mergeDescendants = true) {
                            customActions = buildList {
                                if (index > 0) add(CustomAccessibilityAction(moveUp) {
                                    onOrderChange(items.moved(index, index - 1)); true
                                })
                                if (index < items.lastIndex) add(CustomAccessibilityAction(moveDown) {
                                    onOrderChange(items.moved(index, index + 1)); true
                                })
                            }
                        },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .pointerInput(id) {
                                    detectDragGestures(
                                        onDragStart = { start(id) },
                                        onDrag = { change, amount -> change.consume(); drag(id, amount.y) },
                                        onDragEnd = ::end,
                                        onDragCancel = ::end,
                                    )
                                },
                        )
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

private fun android.content.Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

/** Asks for the Telegram music bot; the ViewModel normalises whatever is typed or pasted. */
@Composable
private fun TelegramBotDialog(
    current: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(if (current.isEmpty()) "" else "@$current") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.telegram_music_bot)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.telegram_music_bot_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.telegram_music_bot_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave("") }) { Text(stringResource(R.string.telegram_music_bot_clear)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.telegram_music_bot_cancel)) }
            }
        },
    )
}
