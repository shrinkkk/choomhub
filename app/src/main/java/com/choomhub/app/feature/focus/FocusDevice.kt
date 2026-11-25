package com.choomhub.app.feature.focus

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.choomhub.app.MainActivity
import com.choomhub.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Do Not Disturb while you're in the focus room (alarms still ring, so the break alarm does too). */
@Singleton
class DoNotDisturb @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Whether you've let ChoomHub switch Do Not Disturb (a one-time Android setting). */
    val isAllowed: Boolean get() = manager?.isNotificationPolicyAccessGranted == true

    /** Opens the Android screen where you allow it. */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun turnOn() {
        val m = manager ?: return
        if (!isAllowed) return
        val current = m.currentInterruptionFilter
        if (current == NotificationManager.INTERRUPTION_FILTER_ALARMS && prefs.getBoolean(KEY_OURS, false)) return
        prefs.edit {
            putInt(KEY_PREVIOUS, current)
            putBoolean(KEY_OURS, true)
        }
        runCatching { m.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS) }
    }

    /** Puts Do Not Disturb back how it was, unless you've changed it yourself since. */
    fun restore() {
        val m = manager ?: return
        if (!prefs.getBoolean(KEY_OURS, false)) return
        val previous = prefs.getInt(KEY_PREVIOUS, NotificationManager.INTERRUPTION_FILTER_ALL)
        prefs.edit { putBoolean(KEY_OURS, false) }
        if (isAllowed && m.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
            runCatching { m.setInterruptionFilter(previous) }
        }
    }

    private companion object {
        const val PREFS = "focus_room"
        const val KEY_PREVIOUS = "dnd_previous"
        const val KEY_OURS = "dnd_ours"
    }
}

/** The focus room's own notifications: the stopwatch while you're in, and the end-of-break alarm. */
@Singleton
class FocusNotifications @Inject constructor(@ApplicationContext private val context: Context) {

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val session = NotificationChannel(SESSION_CHANNEL, "Focus room stopwatch", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Your stopwatch or break timer while you're in the focus room"
            setShowBadge(false)
        }
        val alarm = NotificationChannel(ALARM_CHANNEL, "Break's over", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Rings when your break ends, even in Do Not Disturb"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
            enableVibration(true)
            setBypassDnd(true)
        }
        manager.createNotificationChannels(listOf(session, alarm))
    }

    private fun canPost(): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun openRoom(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN,
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_FOCUS_ROOM)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(action: String, label: String): NotificationCompat.Action {
        val intent = Intent(context, FocusActionReceiver::class.java).setAction(action)
        val pending = PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(0, label, pending)
    }

    private fun base(channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_stat_choomhub)
        .setColor(ContextCompat.getColor(context, R.color.notification_accent))
        .setContentIntent(openRoom())

    /** While focusing: a stopwatch that counts up from [sessionMs]. */
    fun showFocusing(sessionMs: Long, now: Instant) = post(
        SESSION_ID,
        base(SESSION_CHANNEL)
            .setContentTitle("In the focus room")
            .setContentText("Focusing")
            .setOngoing(true)
            .setSilent(true)
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setWhen(now.toEpochMilli() - sessionMs)
            .addAction(action(FocusActionReceiver.ACTION_LEAVE, "Leave"))
            .build(),
    )

    /** On a break: counts down to [endsAt] (then shows how far over you are). */
    fun showBreak(endsAt: Instant) = post(
        SESSION_ID,
        base(SESSION_CHANNEL)
            .setContentTitle("On a break")
            .setContentText("Your desk is waiting")
            .setOngoing(true)
            .setSilent(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)
            .setWhen(endsAt.toEpochMilli())
            .addAction(action(FocusActionReceiver.ACTION_RESUME, "Back to focus"))
            .addAction(action(FocusActionReceiver.ACTION_LEAVE, "Leave"))
            .build(),
    )

    /** The break ended: rings (through Do Not Disturb) and offers the three ways on. */
    fun showBreakOver() = post(
        ALARM_ID,
        base(ALARM_CHANNEL)
            .setContentTitle("Break's over")
            .setContentText("Back to your desk?")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(action(FocusActionReceiver.ACTION_RESUME, "Back to focus"))
            .addAction(action(FocusActionReceiver.ACTION_EXTEND, "5 more minutes"))
            .addAction(action(FocusActionReceiver.ACTION_LEAVE, "Leave"))
            .build(),
    )

    fun clearBreakOver() = NotificationManagerCompat.from(context).cancel(ALARM_ID)

    fun clearAll() {
        NotificationManagerCompat.from(context).cancel(SESSION_ID)
        NotificationManagerCompat.from(context).cancel(ALARM_ID)
    }

    private fun post(id: Int, notification: android.app.Notification) {
        if (!canPost()) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and the post.
        }
    }

    companion object {
        const val SESSION_CHANNEL = "focus_session"
        const val ALARM_CHANNEL = "focus_alarm"
        private const val SESSION_ID = 2001
        private const val ALARM_ID = 2002
        private const val REQUEST_OPEN = 2003
    }
}

/** Schedules the end-of-break alarm, exactly, so it rings even with the phone asleep. */
@Singleton
class BreakAlarm @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager get() = context.getSystemService(AlarmManager::class.java)

    private fun pending(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST,
        Intent(context, BreakAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun schedule(at: Instant) {
        val m = manager ?: return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || m.canScheduleExactAlarms()
        if (exact) {
            val show = PendingIntent.getActivity(
                context,
                REQUEST + 1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
            m.setAlarmClock(AlarmManager.AlarmClockInfo(at.toEpochMilli(), show), pending())
        } else {
            m.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pending())
        }
    }

    fun cancel() {
        manager?.cancel(pending())
    }

    private companion object {
        const val REQUEST = 2010
    }
}
