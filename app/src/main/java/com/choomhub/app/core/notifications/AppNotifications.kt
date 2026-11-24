package com.choomhub.app.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.choomhub.app.MainActivity
import com.choomhub.app.R
import com.choomhub.app.domain.model.NotificationCategory

/** Notification channels and showing notifications while the app is open. */
object AppNotifications {
    /** Channel for things that fit no category (e.g. the test notification). */
    const val GENERAL_CHANNEL = "general"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = NotificationCategory.entries.map { category ->
            NotificationChannel(category.channelId, category.label, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = category.description
            }
        } + NotificationChannel(GENERAL_CHANNEL, "Other", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Test notifications and anything else"
        }
        manager.createNotificationChannels(channels)
    }

    /** Whether notifications from this app can appear at all right now. */
    fun canPost(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, title: String, body: String, channelId: String?, tag: String) {
        if (!canPost(context)) return
        val known = NotificationCategory.entries.map { it.channelId } + GENERAL_CHANNEL
        val channel = channelId?.takeIf { it in known } ?: GENERAL_CHANNEL
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_choomhub)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(tag, NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and the post; nothing to show.
        }
    }

    /** One id for all; the tag decides which notifications replace each other. */
    private const val NOTIFICATION_ID = 1
}
