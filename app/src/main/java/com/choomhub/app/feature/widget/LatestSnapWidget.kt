package com.choomhub.app.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.choomhub.app.MainActivity
import com.choomhub.app.core.common.TimeLabels
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SnapRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock

/**
 * Home-screen widget with their latest snap. It refreshes when a snap arrives
 * (the push for it wakes the app), when the app sees a new one, and hourly as
 * a fallback.
 */
class LatestSnapWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val content = load(context)
        provideContent { WidgetBody(content) }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun snaps(): SnapRepository
        fun profile(): ProfileRepository
        fun clock(): Clock
    }

    private class Content(val photo: Bitmap?, val caption: String?, val line: String, val empty: Boolean)

    private suspend fun load(context: Context): Content {
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        val name = withTimeoutOrNull(5_000) { deps.profile().observeNames().first().partnerName } ?: "them"
        val snap = withTimeoutOrNull(10_000) { deps.snaps().observeRecent().first().firstOrNull { !it.fromMe } }
            ?: return Content(null, null, "Nothing from $name yet", empty = true)
        val bytes = withTimeoutOrNull(15_000) { deps.snaps().observeImage(snap.id).filterNotNull().first() } ?: snap.thumb
        val photo = bytes?.let { decodeScaled(it, 640) }
        val clock = deps.clock()
        val ago = snap.sentAt?.let { TimeLabels.relative(it, clock.instant(), clock.zone) } ?: "just now"
        return Content(photo, snap.caption, "from $name · $ago", empty = false)
    }

    private fun decodeScaled(bytes: ByteArray, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    @androidx.compose.runtime.Composable
    private fun WidgetBody(content: Content) {
        val night = ColorProvider(Color(0xFF0F0B14))
        val bone = ColorProvider(Color(0xFFEDE6D6))
        val faint = ColorProvider(Color(0xFF9B958E))
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(night)
                .cornerRadius(20.dp)
                .padding(10.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            if (content.photo != null) {
                Image(
                    provider = ImageProvider(content.photo),
                    contentDescription = content.caption ?: "Their latest snap",
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight().cornerRadius(12.dp),
                )
            } else {
                Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                    Text(if (content.empty) "no snap yet" else "developing…", style = TextStyle(color = faint, fontSize = 12.sp))
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            if (content.caption != null) {
                Text(content.caption, maxLines = 1, style = TextStyle(color = bone, fontSize = 13.sp, fontStyle = FontStyle.Italic))
            }
            Text(content.line, maxLines = 1, style = TextStyle(color = faint, fontSize = 11.sp))
        }
    }

    companion object {
        /** Redraws every placed widget (cheap if there are none). */
        suspend fun refresh(context: Context) {
            runCatching { LatestSnapWidget().updateAll(context) }
        }
    }
}

class LatestSnapWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LatestSnapWidget()
}

/** Refreshes the widget in the background (used when a snap push arrives). */
@androidx.hilt.work.HiltWorker
class WidgetRefreshWorker @dagger.assisted.AssistedInject constructor(
    @dagger.assisted.Assisted context: Context,
    @dagger.assisted.Assisted params: androidx.work.WorkerParameters,
) : androidx.work.CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        LatestSnapWidget.refresh(applicationContext)
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                .build()
            androidx.work.WorkManager.getInstance(context)
                .enqueueUniqueWork("widget-refresh", androidx.work.ExistingWorkPolicy.REPLACE, request)
        }
    }
}
