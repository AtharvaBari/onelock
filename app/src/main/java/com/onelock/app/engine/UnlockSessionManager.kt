package com.onelock.app.engine

import android.util.Log
import com.onelock.app.OneLockApp
import com.onelock.app.data.ReLockTimeout
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages active unlocked app sessions, multi-window visibility, and smart re-lock timeouts.
 */
object UnlockSessionManager {
    private const val TAG = "UnlockSessionManager"
    const val GRACE_PERIOD_MS = 3000L

    // Map of package names that are currently actively unlocked to their last active timestamp (epoch ms)
    val activeUnlockedPackages = ConcurrentHashMap<String, Long>()

    // Backwards compatibility alias for activeUnlockedPackages.keys
    val activeUnlockedSessions: Set<String>
        get() = activeUnlockedPackages.keys

    // Maps packageName -> timestamp (epoch ms) when the user exited this app
    private val appExitTimestamps = ConcurrentHashMap<String, Long>()

    // Maps packageName -> timestamp (epoch ms) when the app was successfully unlocked
    private val unlockTimestamps = ConcurrentHashMap<String, Long>()

    /**
     * Called when an app is unlocked successfully.
     * Refreshes all active unlocked packages so switching back and forth in multi-window
     * never asks for a credential again.
     */
    fun markUnlocked(packageName: String) {
        val now = System.currentTimeMillis()
        Log.d(TAG, "Package unlocked: $packageName at $now")
        activeUnlockedPackages[packageName] = now
        unlockTimestamps[packageName] = now
        appExitTimestamps.remove(packageName)

        // Refresh all currently unlocked packages in activeUnlockedPackages
        for (pkg in activeUnlockedPackages.keys) {
            activeUnlockedPackages[pkg] = now
            unlockTimestamps[pkg] = now
        }

        LockState.lastUnlockTimestamp = now
        PrivacyOverlayManager.dismissActiveOverlay()
    }

    /**
     * Refreshes timestamps for all active sessions during multi-window focus switches.
     */
    fun refreshActiveSessions() {
        val now = System.currentTimeMillis()
        for (pkg in activeUnlockedPackages.keys) {
            activeUnlockedPackages[pkg] = now
            unlockTimestamps[pkg] = now
        }
    }

    /**
     * Promotes an already valid app back to active unlocked sessions when it regains visibility.
     */
    fun onAppResumed(packageName: String) {
        val now = System.currentTimeMillis()
        activeUnlockedPackages[packageName] = now
        appExitTimestamps.remove(packageName)
    }

    /**
     * Called by AppLockService when an app completely disappears from visible application windows.
     * Subject to freeze conditions during lock overlay or recent unlock.
     */
    fun onAppExited(packageName: String) {
        val now = System.currentTimeMillis()

        // 1. Freeze session exits if lock screen is showing, recent unlock occurred (<2000ms), or OneLock is active
        if (LockState.isLockScreenShowing || (now - LockState.lastUnlockTimestamp < 2000L)) {
            Log.d(TAG, "Freeze active: Lock overlay showing or recent unlock (<2000ms). Skipping exit for $packageName")
            return
        }

        // 2. Minimum 3000ms multi-window grace period
        val unlockTime = unlockTimestamps[packageName] ?: 0L
        val elapsedSinceUnlock = now - unlockTime
        if (elapsedSinceUnlock < GRACE_PERIOD_MS) {
            Log.d(TAG, "Grace period active for $packageName ($elapsedSinceUnlock ms < ${GRACE_PERIOD_MS}ms) -> Ignoring exit")
            return
        }

        // 3. Remove from active sessions and record exit timestamp for timeout evaluation
        if (activeUnlockedPackages.remove(packageName) != null) {
            Log.d(TAG, "App exited from visible sessions: $packageName at $now")
            appExitTimestamps[packageName] = now
        }
    }

    /**
     * Checks if the package is currently considered unlocked under its configured timeout rule.
     */
    fun isAppUnlocked(packageName: String, timeout: ReLockTimeout? = null): Boolean {
        val now = System.currentTimeMillis()

        // 1. Minimum 3000ms Multi-Window Grace Period Debounce after any unlock or window change
        val unlockTime = unlockTimestamps[packageName] ?: 0L
        if (now - unlockTime < GRACE_PERIOD_MS) {
            return true
        }
        if (now - LockState.lastUnlockTimestamp < GRACE_PERIOD_MS && activeUnlockedPackages.containsKey(packageName)) {
            return true
        }

        // 2. Active in visible multi-window session
        if (activeUnlockedPackages.containsKey(packageName)) {
            return true
        }

        // 3. Check exit timestamp for timeout rules
        val exitTime = appExitTimestamps[packageName] ?: return false

        val effectiveTimeout = timeout ?: OneLockApp.preferencesManager.getEffectiveLockConfig(packageName).timeout

        return when (effectiveTimeout) {
            ReLockTimeout.IMMEDIATE -> {
                // Immediate re-lock when user completely left the app
                false
            }
            ReLockTimeout.SEC_30 -> {
                val elapsed = now - exitTime
                elapsed < 30_000L
            }
            ReLockTimeout.MIN_1 -> {
                val elapsed = now - exitTime
                elapsed < 60_000L
            }
            ReLockTimeout.MIN_5 -> {
                val elapsed = now - exitTime
                elapsed < 300_000L
            }
            ReLockTimeout.SCREEN_OFF -> {
                // Stays unlocked until screen turns off
                true
            }
        }
    }

    /**
     * Wipes all unlock sessions immediately (e.g. on Screen Off or manual reset).
     */
    fun clearAll() {
        Log.d(TAG, "Clearing all unlock sessions (Screen Off / Reset)")
        activeUnlockedPackages.clear()
        appExitTimestamps.clear()
        unlockTimestamps.clear()
    }

    fun lockPackage(packageName: String) {
        activeUnlockedPackages.remove(packageName)
        appExitTimestamps.remove(packageName)
        unlockTimestamps.remove(packageName)
    }
}
