package com.choomhub.app

import android.content.Intent
import android.os.Bundle
import com.choomhub.app.core.navigation.PendingOpen
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.ui.ChoomHubApp
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import com.choomhub.app.feature.widget.LatestSnapWidget
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.choomhub.app.core.designsystem.theme.SkyPhase
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Leaving the app is a good moment to bring the home-screen widget up to date. */
    override fun onStop() {
        super.onStop()
        lifecycleScope.launch { LatestSnapWidget.refresh(applicationContext) }
    }

    @Inject lateinit var clock: Clock

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleOpen(intent)
        val ambientMotion = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) != 0f
        setContent {
            // The sky follows the phone's clock; re-checked every half minute.
            val sky by produceState(SkyPhase.at(LocalTime.now(clock))) {
                while (true) {
                    delay(30_000)
                    value = SkyPhase.at(LocalTime.now(clock))
                }
            }
            ChoomHubTheme(sky) {
                CompositionLocalProvider(LocalAmbientMotionEnabled provides ambientMotion) {
                    ChoomHubApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpen(intent)
    }

    private fun handleOpen(intent: Intent?) {
        intent?.getStringExtra(EXTRA_OPEN)?.let { PendingOpen.request.value = it }
        // Share → ChoomHub from Spotify: the link goes to Home's song card.
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                com.choomhub.app.data.song.SongInbox.link.value = text
                PendingOpen.request.value = PendingOpen.HOME
            }
        }
    }

    companion object {
        /** Extra naming a screen to open (see [PendingOpen]). */
        const val EXTRA_OPEN = "open"
        const val OPEN_FOCUS_ROOM = PendingOpen.FOCUS_ROOM
    }
}
