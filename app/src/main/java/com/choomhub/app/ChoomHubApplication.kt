package com.choomhub.app

import android.app.Application
import com.choomhub.app.core.firebase.FirebaseInitializer
import dagger.hilt.android.HiltAndroidApp

/** Application entry point. Hilt generates the dependency graph from here. */
@HiltAndroidApp
class ChoomHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.initialize(this)
    }
}
