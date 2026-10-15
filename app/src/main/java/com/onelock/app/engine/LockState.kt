package com.onelock.app.engine

import com.onelock.app.OneLockApp

/**
 * Global LockState proxying to UnlockSessionManager and tracking active lock status.
 */
object LockState {

    @Volatile
    var isLockScreenActive: Boolean = false

    fun unlock(packageName: String) {
        UnlockSessionManager.markUnlocked(packageName)
    }

    fun resetUnlocked(@Suppress("UNUSED_PARAMETER") reason: String = "") {
        UnlockSessionManager.clearAll()
    }

    fun isUnlocked(packageName: String): Boolean {
        val effectiveConfig = OneLockApp.preferencesManager.getEffectiveLockConfig(packageName)
        return UnlockSessionManager.isAppUnlocked(packageName, effectiveConfig.timeout)
    }
}
