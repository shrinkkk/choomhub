package com.choomhub.app

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.choomhub.app.ui.ChoomHubApp
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.choomhub.app.core.designsystem.theme.SkyPhase
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var clock: Clock

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
}
