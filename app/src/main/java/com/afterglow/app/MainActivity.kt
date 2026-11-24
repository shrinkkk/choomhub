package com.afterglow.app

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.afterglow.app.core.designsystem.theme.AfterglowTheme
import com.afterglow.app.core.designsystem.theme.LocalAmbientMotionEnabled
import com.afterglow.app.ui.AfterglowApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val ambientMotion = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) != 0f
        setContent {
            AfterglowTheme {
                CompositionLocalProvider(LocalAmbientMotionEnabled provides ambientMotion) {
                    AfterglowApp()
                }
            }
        }
    }
}
