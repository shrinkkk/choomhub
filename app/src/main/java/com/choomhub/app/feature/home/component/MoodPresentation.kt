package com.choomhub.app.feature.home.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.MoodOption

/**
 * Colour for a mood. The defaults have fixed colours; custom moods get one of
 * the same muted accents, picked from their key so it stays the same.
 */
@Composable
fun moodColor(mood: MoodOption): Color {
    val c = ChoomHubTheme.colors
    return when (Mood.fromKey(mood.key)) {
        Mood.Cozy, Mood.Content -> c.pink
        Mood.Sleepy, Mood.Calm -> c.lavender
        Mood.Wired, Mood.Restless -> c.candle
        Mood.MissingYou, Mood.Lonely -> c.purple
        Mood.Tired -> c.textSecondary
        Mood.Low -> Color(0xFF7F93B8)
        Mood.Stressed -> c.tomato
        null -> listOf(c.pink, c.lavender, c.candle, c.purple, c.tomato, Color(0xFF7F93B8), Color(0xFF9C8FB0))[Math.floorMod(mood.key.hashCode(), 7)]
    }
}
