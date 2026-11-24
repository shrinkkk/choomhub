package com.choomhub.app.core.firebase

import com.choomhub.app.BuildConfig

/**
 * Firebase client configuration, injected at build time from the git-ignored
 * `local.properties` (see docs/ARCHITECTURE.md -> Configuration). These are
 * public client identifiers; access control lives in firestore.rules.
 *
 * When they are missing, the app runs in device-only mode (no sign-in, no sync).
 */
object FirebaseConfig {
    val projectId: String = BuildConfig.FIREBASE_PROJECT_ID
    val appId: String = BuildConfig.FIREBASE_APP_ID
    val apiKey: String = BuildConfig.FIREBASE_API_KEY
    val webClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    val isConfigured: Boolean
        get() = listOf(projectId, appId, apiKey, webClientId).all { it.isNotBlank() }
}
