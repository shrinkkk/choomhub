package com.choomhub.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.choomhub.app.core.firebase.FirebaseConfig
import com.choomhub.app.core.firebase.FirebaseInitializer
import com.choomhub.app.core.notifications.AppNotifications
import com.choomhub.app.data.push.PushRegistrar
import dagger.hilt.android.HiltAndroidApp
import com.choomhub.app.feature.snaps.prompt.SnapPromptScheduler
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/** Application entry point. Hilt generates the dependency graph from here. */
@HiltAndroidApp
class ChoomHubApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var pushRegistrar: Provider<PushRegistrar>

    @Inject lateinit var snapPrompts: Provider<SnapPromptScheduler>

    /** WorkManager starts on demand with Hilt's factory (its default initializer is removed in the manifest). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.initialize(this)
        AppNotifications.createChannels(this)
        if (FirebaseConfig.isConfigured) pushRegistrar.get().start()
        // Keeps the daily snap prompt booked (WorkManager drops nothing on restart, but the first run needs this).
        MainScope().launch { snapPrompts.get().scheduleNext() }
    }
}
