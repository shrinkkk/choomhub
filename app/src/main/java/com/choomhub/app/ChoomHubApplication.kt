package com.choomhub.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.choomhub.app.core.firebase.FirebaseConfig
import com.choomhub.app.core.firebase.FirebaseInitializer
import com.choomhub.app.core.notifications.AppNotifications
import com.choomhub.app.data.push.PushRegistrar
import dagger.hilt.android.HiltAndroidApp
import androidx.work.WorkManager
import javax.inject.Inject
import javax.inject.Provider

/** Application entry point. Hilt generates the dependency graph from here. */
@HiltAndroidApp
class ChoomHubApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var pushRegistrar: Provider<PushRegistrar>

    /** WorkManager starts on demand with Hilt's factory (its default initializer is removed in the manifest). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.initialize(this)
        AppNotifications.createChannels(this)
        com.choomhub.app.feature.focus.FocusNotifications(this).createChannels()
        if (FirebaseConfig.isConfigured) pushRegistrar.get().start()
        // Snap prompts were removed; cancel any still booked from an earlier version.
        WorkManager.getInstance(this).cancelUniqueWork("snap-prompt")
        // Gentle check-ins on the cat (they do nothing until it's adopted).
        com.choomhub.app.feature.pet.PetReminderWorker.schedule(this)
    }
}
