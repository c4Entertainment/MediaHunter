package com.c4entertainment.mediahunter.lock

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.c4entertainment.mediahunter.R
import com.c4entertainment.mediahunter.ui.theme.MediaHunterTheme

/**
 * Covers the app until the user passes the device's fingerprint or screen
 * lock. Backing out closes the app instead of revealing it.
 *
 * Started with [EXTRA_CONFIRM] it only confirms the user can unlock (used
 * when turning the lock on) and returns RESULT_OK; backing out just returns.
 */
class LockActivity : FragmentActivity() {

    companion object {
        const val EXTRA_CONFIRM = "com.c4entertainment.mediahunter.lock.CONFIRM"
    }

    private val confirmOnly get() = intent.getBooleanExtra(EXTRA_CONFIRM, false)

    private lateinit var prompt: BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Must be created in onCreate, before the activity starts.
        prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AppLock.markUnlocked()
                    setResult(RESULT_OK)
                    finish()
                    overridePendingTransition(0, 0)
                }
                // Cancelling or errors leave the lock screen up with its Unlock button.
            },
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Close every screen in the task rather than showing what's behind.
                if (confirmOnly) finish() else finishAffinity()
            }
        })

        setContent {
            MediaHunterTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(
                                if (confirmOnly) R.string.app_lock_confirm_title else R.string.app_locked_title
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = ::authenticate) {
                            Text(stringResource(R.string.app_unlock))
                        }
                    }
                }
            }
        }

        if (savedInstanceState == null) authenticate()
    }

    override fun onRestart() {
        super.onRestart()
        // The system dismisses the prompt when the app is left; ask again on return.
        authenticate()
    }

    private fun authenticate() {
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_unlock_prompt_title))
                .setSubtitle(getString(R.string.app_unlock_prompt_subtitle))
                .setAllowedAuthenticators(AppLock.AUTHENTICATORS)
                .build()
        )
    }
}
