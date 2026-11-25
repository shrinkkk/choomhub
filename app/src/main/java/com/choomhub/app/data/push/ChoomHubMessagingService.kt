package com.choomhub.app.data.push

import com.choomhub.app.core.notifications.AppNotifications
import com.choomhub.app.feature.widget.WidgetRefreshWorker
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
        val notification = message.notification
        if (notification != null) {
            AppNotifications.show(
                context = this,
                title = notification.title.orEmpty(),
                body = notification.body.orEmpty(),
                channelId = notification.channelId,
                tag = notification.tag ?: message.data["event"] ?: "general",
            )
            return
        }
        // Data-only messages (snaps) reach the app even in the background, so it can refresh
        // the home-screen widget as well as show the notification.
        val data = message.data
        val title = data["title"] ?: return
        AppNotifications.show(this, title, data["body"].orEmpty(), data["channel"], data["tag"] ?: data["event"] ?: "general")
        if (data["event"] == "snap") WidgetRefreshWorker.enqueue(this)
    }
}
