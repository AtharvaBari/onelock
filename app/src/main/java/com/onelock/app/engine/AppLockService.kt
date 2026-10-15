package com.onelock.app.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.onelock.app.OneLockApp
import com.onelock.app.ui.LockScreenActivity

class AppLockService : AccessibilityService() {

    private var screenOffReceiver: BroadcastReceiver? = null
    private val launcherPackages = mutableSetOf("com.sec.android.app.launcher")
    private lateinit var privacyOverlayManager: PrivacyOverlayManager

    private var lastLockLaunchPkg: String? = null
    private var lastLockLaunchTime: Long = 0L

    companion object {
        private const val TAG = "OneLock-Service"
        var isServiceRunning: Boolean = false
            private set

        /**
         * Samsung system overlays and transient UI surfaces that must NEVER trigger
         * an "app exit" or clear active unlocked sessions.
         */
        private val TRANSIENT_SYSTEM_PACKAGES = setOf(
            "com.samsung.android.app.cocktailbarservice", // Samsung Edge Panel / Sidebar
            "com.android.systemui",                       // Notification shade, Quick Panel, Volume dialog, Biometric prompt
            "com.samsung.android.honeyboard",             // Samsung Keyboard
            "com.google.android.inputmethod.latin",        // Gboard
            "com.google.android.inputmethod.korean",
            "com.touchtype.swiftkey",
            "com.samsung.android.app.smartcapture",       // Screenshot / Smart Select toolbar
            "com.samsung.android.app.galaxyfinder",       // One UI Finder overlay
            "com.google.android.permissioncontroller",
            "com.samsung.android.permissioncontroller"
        )

        private val SETTINGS_PACKAGES = setOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.android.packageinstaller"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        privacyOverlayManager = PrivacyOverlayManager(this)

        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            info.eventTypes = info.eventTypes or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED
            serviceInfo = info
            Log.d(TAG, "AppLock Accessibility Service connected with interactive multi-window flags")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure interactive window accessibility flags", e)
        }

        queryHomeLaunchers()
        registerScreenOffReceiver()
    }

    private fun isTransientPackage(pkg: String?): Boolean {
        if (pkg.isNullOrEmpty()) return true
        if (pkg == applicationContext.packageName) return true
        if (TRANSIENT_SYSTEM_PACKAGES.contains(pkg)) return true
        if (pkg.startsWith("com.android.inputmethod") || pkg.startsWith("com.google.android.inputmethod")) return true
        return false
    }

    private fun queryHomeLaunchers() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfos = packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (info in resolveInfos) {
                info.activityInfo?.packageName?.let {
                    launcherPackages.add(it)
                }
            }
            Log.d(TAG, "Known launchers: $launcherPackages")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query launcher packages", e)
        }
    }

    private fun registerScreenOffReceiver() {
        if (screenOffReceiver == null) {
            screenOffReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                        Log.d(TAG, "Screen off detected -> Resetting all unlock sessions")
                        UnlockSessionManager.clearAll()
                        privacyOverlayManager.hideOverlay()
                    }
                }
            }
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            registerReceiver(screenOffReceiver, filter)
        }
    }

    /**
     * Inspects interactive on-screen windows (including Samsung Pop-up View and Split-Screen)
     * to extract all currently visible application packages.
     */
    private fun getVisibleApplicationPackages(eventPackage: String?): Set<String> {
        val visible = mutableSetOf<String>()
        try {
            val windowList = windows
            if (!windowList.isNullOrEmpty()) {
                for (window in windowList) {
                    if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val rootNode = window.root
                        val pkg = rootNode?.packageName?.toString()
                        if (!pkg.isNullOrEmpty() &&
                            !isTransientPackage(pkg) &&
                            !launcherPackages.contains(pkg)
                        ) {
                            visible.add(pkg)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying interactive windows", e)
        }

        // Fallback or augmentation: if eventPackage is an active application
        if (eventPackage != null &&
            !isTransientPackage(eventPackage) &&
            !launcherPackages.contains(eventPackage)
        ) {
            visible.add(eventPackage)
        }

        return visible
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        val eventPackage = event.packageName?.toString()

        // Listen to window state changes and window hierarchy changes
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val prefs = OneLockApp.preferencesManager
        val shield = prefs.getShieldConfig()

        // 1. Check for Recents / Overview state for Recent Apps Privacy Shield Blur
        val className = event.className?.toString() ?: ""
        val isRecentsTriggered = className.contains("Recents", ignoreCase = true) ||
                className.contains("Overview", ignoreCase = true) ||
                (eventPackage != null && launcherPackages.contains(eventPackage) && className.contains("TaskSwitch", ignoreCase = true))

        if (isRecentsTriggered && shield.recentAppsBlurEnabled) {
            val activeLocked = UnlockSessionManager.activeUnlockedSessions.firstOrNull { prefs.isAppLocked(it) }
            activeLocked?.let { privacyOverlayManager.showOverlay(it) }
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !isRecentsTriggered) {
            privacyOverlayManager.hideOverlay()
        }

        // 2. Anti-Tamper & Force-Stop Protection in system settings / installer
        if (eventPackage != null && shield.antiTamperEnabled && SETTINGS_PACKAGES.contains(eventPackage)) {
            if (inspectNodeHierarchyForTamper(rootInActiveWindow)) {
                Log.w(TAG, "Anti-Tamper triggered inside $eventPackage! Launching self-lock protection")
                launchLockScreen(applicationContext.packageName)
                return
            }
        }

        // 3. Samsung System Overlay & Transient UI Whitelist:
        // Ignore Edge Panel, Keyboards, System UI, and LockScreen transitions so session stays unlocked!
        if (isTransientPackage(eventPackage)) {
            return
        }

        // 4. Query all currently visible application packages (supports Multi-Window & Pop-up View)
        val visibleAppPackages = getVisibleApplicationPackages(eventPackage)

        // Dismiss privacy overlay if transitioning to unlocked app or launcher
        val isHomeLauncher = eventPackage != null && launcherPackages.contains(eventPackage)
        if (isHomeLauncher || (visibleAppPackages.isNotEmpty() && visibleAppPackages.all { !prefs.isAppLocked(it) || LockState.isUnlocked(it) })) {
            privacyOverlayManager.hideOverlay()
        }

        // 5. App Exit Handling:
        // Only when a package completely leaves visibleAppPackages (and visibleAppPackages is non-empty,
        // or user returned to home launcher with no pop-up windows visible), start its re-lock timeout.
        if (visibleAppPackages.isNotEmpty() || isHomeLauncher) {
            val exitedPackages = UnlockSessionManager.activeUnlockedSessions.filter { !visibleAppPackages.contains(it) }
            for (exitedPkg in exitedPackages) {
                Log.d(TAG, "App no longer visible in interactive windows -> Recording exit: $exitedPkg")
                UnlockSessionManager.onAppExited(exitedPkg)
            }
        }

        // 6. Visible Packages Inspection:
        // For every visible app window on screen:
        for (visiblePkg in visibleAppPackages) {
            if (prefs.isAppLocked(visiblePkg)) {
                val isUnlocked = LockState.isUnlocked(visiblePkg)
                if (isUnlocked) {
                    // Ensure it is actively tracked in visible sessions
                    UnlockSessionManager.onAppResumed(visiblePkg)
                } else {
                    // Locked app requires authentication -> launch LockScreen for this app only
                    Log.d(TAG, "Intercepting locked visible package: $visiblePkg")
                    // Immediately cover floating pop-up bar with full-screen privacy overlay
                    privacyOverlayManager.showOverlay(visiblePkg)
                    launchLockScreen(visiblePkg)
                    break
                }
            }
        }
    }

    /**
     * Inspects accessibility view hierarchy to detect attempts to Force Stop, Uninstall,
     * or modify OneLock settings.
     */
    private fun inspectNodeHierarchyForTamper(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        try {
            var mentionsOneLock = false
            var mentionsAction = false

            fun traverse(node: AccessibilityNodeInfo?) {
                if (node == null || (mentionsOneLock && mentionsAction)) return

                val text = node.text?.toString() ?: ""
                val desc = node.contentDescription?.toString() ?: ""
                val combined = "$text $desc".lowercase()

                if (combined.contains("onelock") || combined.contains("com.onelock.app")) {
                    mentionsOneLock = true
                }
                if (combined.contains("force stop") || combined.contains("uninstall") ||
                    combined.contains("disable") || combined.contains("clear data") ||
                    combined.contains("app info")
                ) {
                    mentionsAction = true
                }

                for (i in 0 until node.childCount) {
                    traverse(node.getChild(i))
                }
            }

            traverse(root)
            return mentionsOneLock && mentionsAction
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting node hierarchy", e)
            return false
        }
    }

    private fun launchLockScreen(packageName: String) {
        val now = System.currentTimeMillis()
        if (LockState.isLockScreenActive && lastLockLaunchPkg == packageName && now - lastLockLaunchTime < 1000L) {
            return
        }
        lastLockLaunchPkg = packageName
        lastLockLaunchTime = now

        val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
            putExtra(LockScreenActivity.EXTRA_PACKAGE_NAME, packageName)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        }
        startActivity(lockIntent)
    }

    override fun onInterrupt() {
        Log.w(TAG, "AppLock Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        privacyOverlayManager.hideOverlay()
        screenOffReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering screenOffReceiver", e)
            }
            screenOffReceiver = null
        }
        Log.d(TAG, "AppLock Accessibility Service destroyed")
    }
}

