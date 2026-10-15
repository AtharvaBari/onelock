package com.onelock.app.engine

import com.onelock.app.OneLockApp

/**
 * Global LockState proxying to UnlockSessionManager and tracking active lock status.
 */
object LockState {

    @Volatile
    var isLockScreenShowing: Boolean = false

    var isLockScreenActive: Boolean
        get() = isLockScreenShowing
        set(value) { isLockScreenShowing = value }

    @Volatile
    var currentlyLockingPackage: String? = null

    @Volatile
    var lastUnlockTimestamp: Long = 0L

    @Volatile
    var isLaunchingPermissionIntent: Boolean = false

    fun unlock(packageName: String) {
        UnlockSessionManager.markUnlocked(packageName)
        lastUnlockTimestamp = System.currentTimeMillis()
    }

    fun resetUnlocked(@Suppress("UNUSED_PARAMETER") reason: String = "") {
        UnlockSessionManager.clearAll()
    }

    fun isUnlocked(packageName: String): Boolean {
        return UnlockSessionManager.isAppUnlocked(packageName)
    }
}

