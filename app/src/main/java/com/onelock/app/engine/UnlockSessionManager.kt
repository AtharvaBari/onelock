package com.onelock.app.engine

import android.util.Log
import com.onelock.app.data.ReLockTimeout
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages active unlocked app sessions, multi-window visibility, and smart re-lock timeouts.
 */
object UnlockSessionManager {
    private const val TAG = "UnlockSessionManager"
    const val GRACE_PERIOD_MS = 800L

    // Set of package names that are currently actively unlocked and visible on screen (including pop-up / split-screen)
    val activeUnlockedSessions = ConcurrentHashMap.newKeySet<String>()

    // Maps packageName -> timestamp (epoch ms) when the user exited this app
    private val appExitTimestamps = ConcurrentHashMap<String, Long>()

    // Maps packageName -> timestamp (epoch ms) when the app was successfully unlocked (used for 800ms grace period debounce)
    private val unlockTimestamps = ConcurrentHashMap<String, Long>()

    /**
     * Called when an app is unlocked successfully.
     */
    fun markUnlocked(packageName: String) {
        val now = System.currentTimeMillis()
        Log.d(TAG, "Package unlocked: $packageName at $now")
        activeUnlockedSessions.add(packageName)
        unlockTimestamps[packageName] = now
        appExitTimestamps.remove(packageName)
        PrivacyOverlayManager.dismissActiveOverlay()
    }

    /**
     * Promotes an already valid app back to active unlocked sessions when it regains visibility.
     */
    fun onAppResumed(packageName: String) {
        if (!activeUnlockedSessions.contains(packageName)) {
            Log.d(TAG, "Promoting package to active visible sessions: $packageName")
            activeUnlockedSessions.add(packageName)
            appExitTimestamps.remove(packageName)
        }
    }

    /**
     * Called by AppLockService when an app completely disappears from visible application windows.
     */
    fun onAppExited(packageName: String) {
        val now = System.currentTimeMillis()
        val unlockTime = unlockTimestamps[packageName] ?: 0L
        val elapsedSinceUnlock = now - unlockTime

        if (elapsedSinceUnlock < GRACE_PERIOD_MS) {
            Log.d(TAG, "Grace period active for $packageName ($elapsedSinceUnlock ms < ${GRACE_PERIOD_MS}ms) -> Ignoring exit")
            return
        }

        if (activeUnlockedSessions.remove(packageName)) {
            Log.d(TAG, "App exited from visible sessions: $packageName at $now")
            appExitTimestamps[packageName] = now
        }
    }

    /**
     * Checks if the package is currently considered unlocked under its configured timeout rule.
     */
    fun isAppUnlocked(packageName: String, timeout: ReLockTimeout): Boolean {
        val now = System.currentTimeMillis()

        // 1. 800ms Grace Period Debounce
        val unlockTime = unlockTimestamps[packageName] ?: 0L
        if (now - unlockTime < GRACE_PERIOD_MS) {
            return true
        }

        // 2. Active in visible multi-window session
        if (activeUnlockedSessions.contains(packageName)) {
            return true
        }

        // 3. Check exit timestamp for timeout rules
        val exitTime = appExitTimestamps[packageName] ?: return false

        return when (timeout) {
            ReLockTimeout.IMMEDIATE -> {
                // Immediate re-lock when user left the app
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
        activeUnlockedSessions.clear()
        appExitTimestamps.clear()
        unlockTimestamps.clear()
    }

    fun lockPackage(packageName: String) {
        activeUnlockedSessions.remove(packageName)
        appExitTimestamps.remove(packageName)
        unlockTimestamps.remove(packageName)
    }
}
