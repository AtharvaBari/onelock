package com.onelock.app.ui

import android.app.Application
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onelock.app.OneLockApp
import com.onelock.app.data.AppInfo
import com.onelock.app.data.AppLockConfig
import com.onelock.app.data.GlobalLockConfig
import com.onelock.app.data.IntruderLogItem
import com.onelock.app.data.MaskedNotificationItem
import com.onelock.app.data.ProtectionShieldConfig
import com.onelock.app.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class AppFilter(val label: String) {
    ALL("All"),
    LOCKED("Locked"),
    SOCIAL("Social & Messaging"),
    SYSTEM("System & Settings"),
    CUSTOM("Custom Rules")
}

enum class NavigationTab(val title: String) {
    HOME("Home"),
    VAULT("Vault & Logs"),
    SHIELD("Shield"),
    SETTINGS("Settings")
}

data class MainUiState(
    val apps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val lockedPackageNames: Set<String> = emptySet(),
    val searchQuery: String = "",
    val activeFilter: AppFilter = AppFilter.ALL,
    val currentTab: NavigationTab = NavigationTab.HOME,
    val isLoading: Boolean = true,
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isNotificationListenerGranted: Boolean = false,
    val isCameraGranted: Boolean = false,
    val isBatteryOptimizationsIgnored: Boolean = false,
    val globalConfig: GlobalLockConfig = GlobalLockConfig(),
    val shieldConfig: ProtectionShieldConfig = ProtectionShieldConfig(),
    val maskedNotifications: List<MaskedNotificationItem> = emptyList(),
    val intruderLogs: List<IntruderLogItem> = emptyList(),
    val selectedAppForDetail: AppInfo? = null,
    val isSelfUnlocked: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = OneLockApp.preferencesManager
    private val iconCache = ConcurrentHashMap<String, Drawable>()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    companion object {
        private val SOCIAL_KEYWORDS = setOf(
            "whatsapp", "telegram", "signal", "instagram", "facebook", "messenger",
            "snapchat", "discord", "slack", "twitter", "x.corp", "reddit", "wechat",
            "line", "viber", "skype", "tiktok", "messages", "messaging", "sms"
        )

        private val SYSTEM_KEYWORDS = setOf(
            "settings", "installer", "gallery", "camera", "files", "contact",
            "dialer", "phone", "browser", "chrome", "sec.android", "google.android"
        )
    }

    init {
        loadInstalledApps()
        refreshPermissions()
        refreshLogs()
    }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        val a11y = PermissionUtils.isAccessibilityServiceEnabled(context)
        val overlay = PermissionUtils.canDrawOverlays(context)
        val notifListener = PermissionUtils.isNotificationListenerEnabled(context)
        val camera = PermissionUtils.isCameraPermissionGranted(context)
        val battery = PermissionUtils.isIgnoringBatteryOptimizations(context)

        _uiState.value = _uiState.value.copy(
            isAccessibilityEnabled = a11y,
            isOverlayGranted = overlay,
            isNotificationListenerGranted = notifListener,
            isCameraGranted = camera,
            isBatteryOptimizationsIgnored = battery,
            globalConfig = prefs.getGlobalConfig(),
            shieldConfig = prefs.getShieldConfig(),
            lockedPackageNames = prefs.getLockedPackages()
        )
    }

    fun refreshLogs() {
        _uiState.value = _uiState.value.copy(
            maskedNotifications = prefs.getMaskedNotifications(),
            intruderLogs = prefs.getIntruderLogs()
        )
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val resolveInfos = pm.queryIntentActivities(intent, 0)
                val currentPkg = getApplication<Application>().packageName
                val locked = prefs.getLockedPackages()

                val appList = mutableListOf<AppInfo>()
                for (info in resolveInfos) {
                    val pkg = info.activityInfo.packageName
                    if (pkg == currentPkg) continue

                    val name = try {
                        info.loadLabel(pm).toString()
                    } catch (_: Exception) {
                        pkg
                    }

                    try {
                        val icon = info.loadIcon(pm)
                        iconCache[pkg] = icon
                    } catch (_: Exception) {}

                    val isAppLocked = locked.contains(pkg)
                    val hasCustom = prefs.hasCustomLock(pkg)

                    appList.add(
                        AppInfo(
                            packageName = pkg,
                            name = name,
                            isLocked = isAppLocked,
                            hasCustomLock = hasCustom
                        )
                    )
                }

                appList.sortedWith(compareBy({ !it.isLocked }, { it.name.lowercase() }))
            }

            _uiState.value = _uiState.value.copy(
                apps = apps,
                lockedPackageNames = prefs.getLockedPackages(),
                globalConfig = prefs.getGlobalConfig(),
                shieldConfig = prefs.getShieldConfig(),
                maskedNotifications = prefs.getMaskedNotifications(),
                intruderLogs = prefs.getIntruderLogs(),
                isLoading = false
            )
            applyFilters()
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return iconCache[packageName] ?: try {
            val pm = getApplication<Application>().packageManager
            val d = pm.getApplicationIcon(packageName)
            iconCache[packageName] = d
            d
        } catch (_: Exception) {
            null
        }
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
        refreshPermissions()
        refreshLogs()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun setFilter(filter: AppFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
        applyFilters()
    }

    fun selectAppForDetail(app: AppInfo?) {
        _uiState.value = _uiState.value.copy(selectedAppForDetail = app)
    }

    fun toggleAppLock(packageName: String) {
        val newState = prefs.toggleAppLock(packageName)
        val currentLocked = HashSet(prefs.getLockedPackages())

        val updatedApps = _uiState.value.apps.map {
            if (it.packageName == packageName) it.copy(isLocked = newState) else it
        }

        _uiState.value = _uiState.value.copy(
            apps = updatedApps,
            lockedPackageNames = currentLocked
        )
        applyFilters()
    }

    fun setAppLocked(packageName: String, locked: Boolean) {
        prefs.setAppLocked(packageName, locked)
        val currentLocked = HashSet(prefs.getLockedPackages())

        val updatedApps = _uiState.value.apps.map {
            if (it.packageName == packageName) it.copy(isLocked = locked) else it
        }

        _uiState.value = _uiState.value.copy(
            apps = updatedApps,
            lockedPackageNames = currentLocked
        )
        applyFilters()
    }

    fun getAppConfig(packageName: String): AppLockConfig {
        return prefs.getAppConfig(packageName)
    }

    fun saveAppConfig(config: AppLockConfig) {
        prefs.saveAppConfig(config)
        val hasCustom = prefs.hasCustomLock(config.packageName)
        val updatedApps = _uiState.value.apps.map {
            if (it.packageName == config.packageName) it.copy(hasCustomLock = hasCustom) else it
        }
        _uiState.value = _uiState.value.copy(apps = updatedApps)
        applyFilters()
    }

    fun resetAppConfigToDefault(packageName: String) {
        prefs.resetAppConfigToDefault(packageName)
        val updatedApps = _uiState.value.apps.map {
            if (it.packageName == packageName) it.copy(hasCustomLock = false) else it
        }
        _uiState.value = _uiState.value.copy(apps = updatedApps)
        applyFilters()
    }

    fun saveGlobalConfig(config: GlobalLockConfig) {
        prefs.saveGlobalConfig(config)
        _uiState.value = _uiState.value.copy(globalConfig = config)
    }

    fun saveShieldConfig(config: ProtectionShieldConfig) {
        prefs.saveShieldConfig(config)
        _uiState.value = _uiState.value.copy(shieldConfig = config)
    }

    fun deleteNotification(id: String) {
        prefs.deleteMaskedNotification(id)
        refreshLogs()
    }

    fun clearNotifications() {
        prefs.clearMaskedNotifications()
        refreshLogs()
    }

    fun deleteIntruderLog(id: String) {
        prefs.deleteIntruderLog(id)
        refreshLogs()
    }

    fun clearIntruderLogs() {
        prefs.clearIntruderLogs()
        refreshLogs()
    }

    fun setSelfUnlocked(unlocked: Boolean) {
        _uiState.value = _uiState.value.copy(isSelfUnlocked = unlocked)
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val filter = state.activeFilter

        val filtered = state.apps.filter { app ->
            val pkgLower = app.packageName.lowercase()
            val nameLower = app.name.lowercase()

            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.LOCKED -> state.lockedPackageNames.contains(app.packageName)
                AppFilter.SOCIAL -> {
                    SOCIAL_KEYWORDS.any { pkgLower.contains(it) || nameLower.contains(it) }
                }
                AppFilter.SYSTEM -> {
                    SYSTEM_KEYWORDS.any { pkgLower.contains(it) || nameLower.contains(it) }
                }
                AppFilter.CUSTOM -> app.hasCustomLock
            }

            val matchesQuery = query.isEmpty() ||
                    nameLower.contains(query) ||
                    pkgLower.contains(query)

            matchesFilter && matchesQuery
        }

        _uiState.value = _uiState.value.copy(filteredApps = filtered)
    }
}
