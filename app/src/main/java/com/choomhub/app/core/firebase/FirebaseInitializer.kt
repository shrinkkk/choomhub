package com.choomhub.app.core.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Initialises Firebase from [FirebaseConfig] instead of a checked-in
 * google-services.json. Firestore's on-device cache (offline persistence) is
 * on by default on Android and is what makes the app offline-first.
 */
object FirebaseInitializer {
    fun initialize(context: Context) {
        if (!FirebaseConfig.isConfigured || FirebaseApp.getApps(context).isNotEmpty()) return
        val options = FirebaseOptions.Builder()
            .setProjectId(FirebaseConfig.projectId)
            .setApplicationId(FirebaseConfig.appId)
            .setApiKey(FirebaseConfig.apiKey)
        // Push (FCM) needs the sender id; it is derived from the app id, so no extra setting.
        FirebaseConfig.senderId?.let(options::setGcmSenderId)
        FirebaseApp.initializeApp(context, options.build())
    }
}
