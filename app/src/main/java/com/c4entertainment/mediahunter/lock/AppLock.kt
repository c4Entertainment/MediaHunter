package com.c4entertainment.mediahunter.lock

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.biometric.BiometricManager
import com.c4entertainment.mediahunter.domain.repository.UserSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Locks the whole app, TorrentSearch's screens included, behind the
 * device's fingerprint or screen lock when the user turns it on.
 *
 * The app locks on launch and whenever it returns from the background.
 * Whatever activity comes to the front is then covered by [LockActivity]
 * until the user unlocks.
 */
object AppLock {
    /**
     * Fingerprint (or face) with the device PIN/pattern/password as fallback.
     * BIOMETRIC_STRONG with DEVICE_CREDENTIAL isn't supported before API 30;
     * BIOMETRIC_WEAK with it works on every version.
     */
    const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    @Volatile private var enabled = false
    @Volatile private var locked = true
    private var startedActivities = 0
    private var resumedActivity: WeakReference<Activity>? = null

    fun install(application: Application, settings: UserSettingsRepository) {
        application.registerActivityLifecycleCallbacks(callbacks)

        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            settings.userSettings
                .map { it.useFingerPrint }
                .distinctUntilChanged()
                .collect { useLock ->
                    enabled = useLock
                    // Settings load after the first screen appears; cover it then.
                    resumedActivity?.get()?.let(::showLockIfNeeded)
                }
        }
    }

    /** Whether this device has a fingerprint or screen lock the app can use. */
    fun canLock(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Called after a successful authentication: on the lock screen, or when
     * turning the lock on in Settings, so this session counts as unlocked.
     */
    fun markUnlocked() {
        locked = false
    }

    private fun showLockIfNeeded(activity: Activity) {
        if (!enabled || !locked || activity is LockActivity) return
        // Biometrics removed since the lock was turned on: don't lock the user out.
        if (!canLock(activity)) return
        activity.startActivity(
            Intent(activity, LockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        activity.overridePendingTransition(0, 0)
    }

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            startedActivities++
        }

        override fun onActivityResumed(activity: Activity) {
            resumedActivity = WeakReference(activity)
            showLockIfNeeded(activity)
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities--
            // The last visible screen went away: the app is in the background.
            // Rotations restart activities but never drop the count to zero for
            // long, since the new instance starts before the old one stops.
            if (startedActivities <= 0 && !activity.isChangingConfigurations) {
                startedActivities = 0
                locked = true
            }
        }

        override fun onActivityPaused(activity: Activity) {
            if (resumedActivity?.get() === activity) resumedActivity = null
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
