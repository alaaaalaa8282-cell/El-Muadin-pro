package com.AbuAlaa

import android.app.Application
import com.AbuAlaa.noor.config.ConfigManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Noor ConfigManager
        ConfigManager.init(this)
    }
}
