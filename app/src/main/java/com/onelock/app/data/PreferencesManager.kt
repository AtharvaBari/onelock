package com.onelock.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PreferencesManager"
        private const val PREFS_NAME = "onelock_prefs"
        private const val KEY_LOCKED_PACKAGES = "locked_packages"
        private const val KEY_GLOBAL_CONFIG_JSON = "global_config_json"
        private const val KEY_SHIELD_CONFIG_JSON = "shield_config_json"
        private const val KEY_APP_OVERRIDES_JSON = "app_overrides_json"
        private const val KEY_NOTIFICATIONS_JSON = "masked_notifications_json"
        private const val KEY_INTRUDER_LOGS_JSON = "intruder_logs_json"

        // Legacy key
        private const val KEY_LEGACY_PIN = "pin_code"
    }

    init {
        migrateLegacyIfNecessary()
    }

    private fun migrateLegacyIfNecessary() {
        if (!prefs.contains(KEY_GLOBAL_CONFIG_JSON) && prefs.contains(KEY_LEGACY_PIN)) {
            val legacyPin = prefs.getString(KEY_LEGACY_PIN, "0000") ?: "0000"
            val config = GlobalLockConfig(
                defaultLockType = LockType.PIN_4,
                baseCredential = legacyPin,
                biometricEnabled = true,
                isOnboardingCompleted = true
            )
            saveGlobalConfig(config)
        }
    }

    // ==========================================
    // Locked Packages Set
    // ==========================================

    fun getLockedPackages(): Set<String> {
        return prefs.getStringSet(KEY_LOCKED_PACKAGES, emptySet())?.toSet() ?: emptySet()
    }

    fun isAppLocked(packageName: String): Boolean {
        return getLockedPackages().contains(packageName)
    }

    fun setAppLocked(packageName: String, locked: Boolean) {
        val current = HashSet(getLockedPackages())
        if (locked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
    }

    fun toggleAppLock(packageName: String): Boolean {
        val newState = !isAppLocked(packageName)
        setAppLocked(packageName, newState)
        return newState
    }

    fun getLockedCount(): Int {
        return getLockedPackages().size
    }

    // ==========================================
    // Global Lock Configuration
    // ==========================================

    fun getGlobalConfig(): GlobalLockConfig {
        val jsonString = prefs.getString(KEY_GLOBAL_CONFIG_JSON, null)
        if (jsonString.isNullOrEmpty()) {
            return GlobalLockConfig(
                defaultLockType = LockType.PIN_4,
                baseCredential = "0000",
                biometricEnabled = true,
                isOnboardingCompleted = false,
                reLockTimeout = ReLockTimeout.IMMEDIATE,
                scrambleKeypad = false,
                hidePatternTrail = false
            )
        }
        return try {
            val obj = JSONObject(jsonString)
            val lockTypeName = obj.optString("lockType", LockType.PIN_4.name)
            val lockType = try { LockType.valueOf(lockTypeName) } catch (_: Exception) { LockType.PIN_4 }
            val credential = obj.optString("credential", "0000")
            val biometricEnabled = obj.optBoolean("biometricEnabled", true)
            val isOnboardingCompleted = obj.optBoolean("onboardingCompleted", false)
            val timeoutName = obj.optString("reLockTimeout", ReLockTimeout.IMMEDIATE.name)
            val timeout = try { ReLockTimeout.valueOf(timeoutName) } catch (_: Exception) { ReLockTimeout.IMMEDIATE }
            val scrambleKeypad = obj.optBoolean("scrambleKeypad", false)
            val hidePatternTrail = obj.optBoolean("hidePatternTrail", false)

            GlobalLockConfig(
                defaultLockType = lockType,
                baseCredential = credential,
                biometricEnabled = biometricEnabled,
                isOnboardingCompleted = isOnboardingCompleted,
                reLockTimeout = timeout,
                scrambleKeypad = scrambleKeypad,
                hidePatternTrail = hidePatternTrail
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing global config JSON", e)
            GlobalLockConfig()
        }
    }

    fun saveGlobalConfig(config: GlobalLockConfig) {
        try {
            val obj = JSONObject().apply {
                put("lockType", config.defaultLockType.name)
                put("credential", config.baseCredential)
                put("biometricEnabled", config.biometricEnabled)
                put("onboardingCompleted", config.isOnboardingCompleted)
                put("reLockTimeout", config.reLockTimeout.name)
                put("scrambleKeypad", config.scrambleKeypad)
                put("hidePatternTrail", config.hidePatternTrail)
            }
            prefs.edit().putString(KEY_GLOBAL_CONFIG_JSON, obj.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving global config JSON", e)
        }
    }

    // ==========================================
    // Protection Shield Configuration
    // ==========================================

    fun getShieldConfig(): ProtectionShieldConfig {
        val jsonString = prefs.getString(KEY_SHIELD_CONFIG_JSON, null)
        if (jsonString.isNullOrEmpty()) {
            return ProtectionShieldConfig()
        }
        return try {
            val obj = JSONObject(jsonString)
            ProtectionShieldConfig(
                antiTamperEnabled = obj.optBoolean("antiTamperEnabled", true),
                recentAppsBlurEnabled = obj.optBoolean("recentAppsBlurEnabled", true),
                notificationMaskingEnabled = obj.optBoolean("notificationMaskingEnabled", false),
                intruderSelfieEnabled = obj.optBoolean("intruderSelfieEnabled", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing shield config JSON", e)
            ProtectionShieldConfig()
        }
    }

    fun saveShieldConfig(config: ProtectionShieldConfig) {
        try {
            val obj = JSONObject().apply {
                put("antiTamperEnabled", config.antiTamperEnabled)
                put("recentAppsBlurEnabled", config.recentAppsBlurEnabled)
                put("notificationMaskingEnabled", config.notificationMaskingEnabled)
                put("intruderSelfieEnabled", config.intruderSelfieEnabled)
            }
            prefs.edit().putString(KEY_SHIELD_CONFIG_JSON, obj.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving shield config JSON", e)
        }
    }

    // ==========================================
    // Per-App Overrides
    // ==========================================

    private fun getOverridesJsonObject(): JSONObject {
        val raw = prefs.getString(KEY_APP_OVERRIDES_JSON, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    fun getAppConfig(packageName: String): AppLockConfig {
        val root = getOverridesJsonObject()
        if (!root.has(packageName)) {
            return AppLockConfig(
                packageName = packageName,
                useCustomLock = false,
                customLockType = LockType.PIN_4,
                customCredential = "",
                biometricEnabled = true,
                customTimeout = null
            )
        }
        return try {
            val appObj = root.getJSONObject(packageName)
            val useCustom = appObj.optBoolean("useCustom", false)
            val lockTypeName = appObj.optString("lockType", LockType.PIN_4.name)
            val lockType = try { LockType.valueOf(lockTypeName) } catch (_: Exception) { LockType.PIN_4 }
            val credential = appObj.optString("credential", "")
            val biometricEnabled = appObj.optBoolean("biometricEnabled", true)
            val customTimeout = if (appObj.has("customTimeout")) {
                try { ReLockTimeout.valueOf(appObj.getString("customTimeout")) } catch (_: Exception) { null }
            } else null

            AppLockConfig(
                packageName = packageName,
                useCustomLock = useCustom,
                customLockType = lockType,
                customCredential = credential,
                biometricEnabled = biometricEnabled,
                customTimeout = customTimeout
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing app override for $packageName", e)
            AppLockConfig(packageName = packageName)
        }
    }

    fun saveAppConfig(config: AppLockConfig) {
        try {
            val root = getOverridesJsonObject()
            val appObj = JSONObject().apply {
                put("useCustom", config.useCustomLock)
                put("lockType", config.customLockType.name)
                put("credential", config.customCredential)
                put("biometricEnabled", config.biometricEnabled)
                if (config.customTimeout != null) {
                    put("customTimeout", config.customTimeout.name)
                } else {
                    remove("customTimeout")
                }
            }
            root.put(config.packageName, appObj)
            prefs.edit().putString(KEY_APP_OVERRIDES_JSON, root.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving app override for ${config.packageName}", e)
        }
    }

    fun resetAppConfigToDefault(packageName: String) {
        try {
            val root = getOverridesJsonObject()
            if (root.has(packageName)) {
                root.remove(packageName)
                prefs.edit().putString(KEY_APP_OVERRIDES_JSON, root.toString()).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting app override for $packageName", e)
        }
    }

    fun hasCustomLock(packageName: String): Boolean {
        val cfg = getAppConfig(packageName)
        return cfg.useCustomLock && cfg.customCredential.isNotEmpty()
    }

    // ==========================================
    // Effective Lock Resolution
    // ==========================================

    fun getEffectiveLockConfig(packageName: String): EffectiveLockConfig {
        val appConfig = getAppConfig(packageName)
        val globalConfig = getGlobalConfig()

        val lockType = if (appConfig.useCustomLock && appConfig.customCredential.isNotEmpty()) {
            appConfig.customLockType
        } else {
            globalConfig.defaultLockType
        }

        val credential = if (appConfig.useCustomLock && appConfig.customCredential.isNotEmpty()) {
            appConfig.customCredential
        } else {
            globalConfig.baseCredential
        }

        val biometric = if (appConfig.useCustomLock) {
            appConfig.biometricEnabled
        } else {
            globalConfig.biometricEnabled
        }

        val timeout = appConfig.customTimeout ?: globalConfig.reLockTimeout

        return EffectiveLockConfig(
            packageName = packageName,
            lockType = lockType,
            credential = credential,
            biometricEnabled = biometric,
            timeout = timeout,
            isCustom = appConfig.useCustomLock && appConfig.customCredential.isNotEmpty()
        )
    }

    // ==========================================
    // Masked Notifications Log
    // ==========================================

    fun getMaskedNotifications(): List<MaskedNotificationItem> {
        val raw = prefs.getString(KEY_NOTIFICATIONS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<MaskedNotificationItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MaskedNotificationItem(
                        id = obj.getString("id"),
                        packageName = obj.getString("packageName"),
                        appName = obj.optString("appName", ""),
                        title = obj.optString("title", ""),
                        text = obj.optString("text", ""),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notifications JSON", e)
            emptyList()
        }
    }

    fun addMaskedNotification(item: MaskedNotificationItem) {
        val current = getMaskedNotifications().toMutableList()
        current.add(0, item)
        // Keep last 100 notifications
        val trimmed = if (current.size > 100) current.take(100) else current
        saveMaskedNotifications(trimmed)
    }

    fun deleteMaskedNotification(id: String) {
        val current = getMaskedNotifications().filterNot { it.id == id }
        saveMaskedNotifications(current)
    }

    fun clearMaskedNotifications() {
        saveMaskedNotifications(emptyList())
    }

    private fun saveMaskedNotifications(list: List<MaskedNotificationItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("packageName", item.packageName)
                    put("appName", item.appName)
                    put("title", item.title)
                    put("text", item.text)
                    put("timestamp", item.timestamp)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_NOTIFICATIONS_JSON, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notifications JSON", e)
        }
    }

    // ==========================================
    // Intruder Selfie Logs
    // ==========================================

    fun getIntruderLogs(): List<IntruderLogItem> {
        val raw = prefs.getString(KEY_INTRUDER_LOGS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<IntruderLogItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    IntruderLogItem(
                        id = obj.getString("id"),
                        imagePath = obj.getString("imagePath"),
                        packageName = obj.getString("packageName"),
                        appName = obj.optString("appName", ""),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing intruder logs JSON", e)
            emptyList()
        }
    }

    fun addIntruderLog(item: IntruderLogItem) {
        val current = getIntruderLogs().toMutableList()
        current.add(0, item)
        val trimmed = if (current.size > 50) current.take(50) else current
        saveIntruderLogs(trimmed)
    }

    fun deleteIntruderLog(id: String) {
        val logs = getIntruderLogs()
        val item = logs.find { it.id == id }
        item?.let {
            try {
                File(it.imagePath).delete()
            } catch (_: Exception) {}
        }
        val current = logs.filterNot { it.id == id }
        saveIntruderLogs(current)
    }

    fun clearIntruderLogs() {
        val logs = getIntruderLogs()
        for (item in logs) {
            try {
                File(item.imagePath).delete()
            } catch (_: Exception) {}
        }
        saveIntruderLogs(emptyList())
    }

    private fun saveIntruderLogs(list: List<IntruderLogItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("imagePath", item.imagePath)
                    put("packageName", item.packageName)
                    put("appName", item.appName)
                    put("timestamp", item.timestamp)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_INTRUDER_LOGS_JSON, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving intruder logs JSON", e)
        }
    }
}
