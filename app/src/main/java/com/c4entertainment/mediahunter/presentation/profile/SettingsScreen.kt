package com.c4entertainment.mediahunter.presentation.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.c4entertainment.mediahunter.BuildConfig
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.lock.AppLock
import com.c4entertainment.mediahunter.lock.LockActivity
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
    val lockOn = (uiState as? SettingsUiState.Success)?.settings?.useFingerPrint == true
    val canLock = remember { AppLock.canLock(context) }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

private fun android.content.Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
