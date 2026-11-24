package com.choomhub.app.data.push

import com.choomhub.app.core.notifications.AppNotifications
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives FCM callbacks. While the app is in the background Android shows
 * notification messages by itself; while it's open they arrive here and are
 * shown the same way.
 */
@AndroidEntryPoint
class ChoomHubMessagingService : FirebaseMessagingService() {

    @Inject lateinit var registrar: PushRegistrar

    override fun onRegistered(installationId: String) = registrar.onRegistered(installationId)

    override fun onUnregistered(installationId: String) = registrar.onUnregistered(installationId)

    /**
     * Legacy registration tokens aren't used: the app opts into installation-id
     * targeting (manifest meta-data) and [onRegistered] carries the id.
     */
    override fun onNewToken(token: String) = Unit

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        AppNotifications.show(
            context = this,
            title = notification.title.orEmpty(),
            body = notification.body.orEmpty(),
            channelId = notification.channelId,
            tag = notification.tag ?: message.data["event"] ?: "general",
        )
    }
}
