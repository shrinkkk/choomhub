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

    /** Base URL of the push Worker (push-worker/), e.g. https://choomhub-push.<you>.workers.dev. */
    val pushWorkerUrl: String = BuildConfig.PUSH_WORKER_URL

    /** FCM sender id = the Firebase project number, the second part of the app id ("1:<number>:android:<hash>"). */
    val senderId: String? get() = senderIdFrom(appId)

    fun senderIdFrom(appId: String): String? = appId.split(':').getOrNull(1)?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }

    /** The Worker endpoint, or null when not configured or not https. */
    fun notifyEndpoint(baseUrl: String = pushWorkerUrl): String? {
        val trimmed = baseUrl.trim().trimEnd('/')
        if (!trimmed.startsWith("https://") || trimmed.length <= "https://".length) return null
        return "$trimmed/v1/notify"
    }

    val isConfigured: Boolean
        get() = listOf(projectId, appId, apiKey, webClientId).all { it.isNotBlank() }
}
