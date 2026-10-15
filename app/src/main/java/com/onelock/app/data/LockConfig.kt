package com.onelock.app.data

enum class LockType(val displayName: String, val subtitle: String) {
    PIN_4("4-Digit PIN", "Quick numerical PIN that auto-submits"),
    PIN_6("6-Digit PIN", "Extended 6-digit numerical PIN"),
    PATTERN("Pattern Lock", "Connect dots on a 3x3 interactive grid"),
    PASSWORD("Alphanumeric Password", "Custom password with letters, numbers, and symbols")
}

enum class ReLockTimeout(val displayName: String, val durationMs: Long) {
    IMMEDIATE("Immediately on app exit", 0L),
    SEC_30("After 30 seconds", 30_000L),
    MIN_1("After 1 minute", 60_000L),
    MIN_5("After 5 minutes", 300_000L),
    SCREEN_OFF("Only when screen turns off", Long.MAX_VALUE)
}

data class ProtectionShieldConfig(
    val antiTamperEnabled: Boolean = true,
    val recentAppsBlurEnabled: Boolean = true,
    val notificationMaskingEnabled: Boolean = false,
    val intruderSelfieEnabled: Boolean = false
)

data class GlobalLockConfig(
    val defaultLockType: LockType = LockType.PIN_4,
    val baseCredential: String = "0000",
    val biometricEnabled: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val reLockTimeout: ReLockTimeout = ReLockTimeout.IMMEDIATE,
    val scrambleKeypad: Boolean = false,
    val hidePatternTrail: Boolean = false
)

data class AppLockConfig(
    val packageName: String,
    val useCustomLock: Boolean = false,
    val customLockType: LockType = LockType.PIN_4,
    val customCredential: String = "",
    val biometricEnabled: Boolean = true,
    val customTimeout: ReLockTimeout? = null
)

data class EffectiveLockConfig(
    val packageName: String,
    val lockType: LockType,
    val credential: String,
    val biometricEnabled: Boolean,
    val timeout: ReLockTimeout,
    val isCustom: Boolean
)

data class MaskedNotificationItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

data class IntruderLogItem(
    val id: String,
    val imagePath: String,
    val packageName: String,
    val appName: String,
    val timestamp: Long
)
