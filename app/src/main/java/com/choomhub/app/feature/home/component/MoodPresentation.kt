package com.choomhub.app.feature.home.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.Mood

/**
 * Colour associated with each mood. Lives with the home feature for now and
 * moves to the mood feature when that is built.
 */
@Composable
fun moodColor(mood: Mood): Color {
    val c = ChoomHubTheme.colors
    return when (mood) {
        Mood.Cozy, Mood.Content -> c.pink
        Mood.Sleepy, Mood.Calm -> c.lavender
        Mood.Wired, Mood.Restless -> c.candle
        Mood.MissingYou, Mood.Lonely -> c.purple
        Mood.Tired -> c.textSecondary
        Mood.Low -> Color(0xFF7F93B8)
        Mood.Stressed -> c.tomato
    }
}
