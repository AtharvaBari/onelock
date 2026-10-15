package com.onelock.app.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val name: String,
    val isLocked: Boolean = false,
    val hasCustomLock: Boolean = false,
    val isSystemApp: Boolean = false
)

data class LoadedAppItem(
    val appInfo: AppInfo,
    val icon: Drawable? = null
)
