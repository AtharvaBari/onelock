package com.onelock.app

import android.app.Application
import com.onelock.app.data.PreferencesManager

class OneLockApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
    }

    companion object {
        lateinit var instance: OneLockApp
            private set
        lateinit var preferencesManager: PreferencesManager
            private set
    }
}
