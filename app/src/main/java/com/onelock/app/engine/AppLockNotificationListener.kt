package com.onelock.app.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.onelock.app.OneLockApp
import com.onelock.app.R
import com.onelock.app.data.MaskedNotificationItem
import com.onelock.app.ui.MainActivity
import java.util.UUID

class AppLockNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "OneLock-NotifListener"
        private const val CHANNEL_ID = "onelock_masked_channel"
        private const val CHANNEL_NAME = "OneLock Masked Notifications"
        var isConnected = false
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        createNotificationChannel()
        Log.d(TAG, "NotificationListener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.d(TAG, "NotificationListener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName ?: return

        // Prevent recursive loop with our own notifications
        if (pkg == packageName) return

        // Ignore ongoing notifications like media playback or foreground services
        if (sbn.isOngoing) return

        val prefs = OneLockApp.preferencesManager
        val shield = prefs.getShieldConfig()

        if (!shield.notificationMaskingEnabled) return
        if (!prefs.isAppLocked(pkg)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isEmpty() && text.isEmpty()) return

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkg
        }

        // 1. Record original notification privately to internal log
        val item = MaskedNotificationItem(
            id = UUID.randomUUID().toString(),
            packageName = pkg,
            appName = appName,
            title = title,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        prefs.addMaskedNotification(item)
        Log.d(TAG, "Intercepted and logged notification for $appName")

        // 2. Cancel the original notification so content never leaks on Samsung lock screen/status bar
        try {
            cancelNotification(sbn.key)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel original notification", e)
        }

        // 3. Post safe replacement notification
        postSafeMaskedNotification(pkg, appName)
    }

    private fun postSafeMaskedNotification(pkg: String, appName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            pkg.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(appName)
            .setContentText("🔒 Locked notification — Tap to view in OneLock")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = (pkg.hashCode() and 0x7FFFFFFF)
        notificationManager.notify(notificationId, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Replaces intercepted notifications with privacy-masked cards"
                setShowBadge(true)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }
}
