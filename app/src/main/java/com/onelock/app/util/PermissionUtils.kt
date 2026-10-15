package com.onelock.app.util

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.onelock.app.engine.AppLockNotificationListener
import com.onelock.app.engine.AppLockService

object PermissionUtils {

    /**
     * Checks if AppLockService is enabled in Accessibility settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (AppLockService.isServiceRunning) return true

        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedComponent = ComponentName(context, AppLockService::class.java).flattenToString()

        for (service in enabledServices) {
            val id = service.id
            if (id.contains(context.packageName) && id.contains(AppLockService::class.java.simpleName)) {
                return true
            }
            if (id.equals(expectedComponent, ignoreCase = true)) {
                return true
            }
        }

        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return settingValue.contains(context.packageName)
    }

    /**
     * Checks if SYSTEM_ALERT_WINDOW (Display over other apps) permission is granted.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Checks if Notification Listener Service is granted.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        if (AppLockNotificationListener.isConnected) return true

        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        val myComponent = ComponentName(context, AppLockNotificationListener::class.java).flattenToString()
        return flat.contains(myComponent) || flat.contains(context.packageName)
    }

    /**
     * Checks if Camera permission is granted for Intruder Selfie.
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if the app is exempt from battery optimizations.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openAccessibilitySettings(context: Context) {
        com.onelock.app.engine.LockState.isLaunchingPermissionIntent = true
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openOverlaySettings(context: Context) {
        com.onelock.app.engine.LockState.isLaunchingPermissionIntent = true
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openNotificationListenerSettings(context: Context) {
        com.onelock.app.engine.LockState.isLaunchingPermissionIntent = true
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        com.onelock.app.engine.LockState.isLaunchingPermissionIntent = true
        try {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
