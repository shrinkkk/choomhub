package com.choomhub.app.feature.home.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme
import com.choomhub.app.domain.model.Mood

/**
 * Colour associated with each mood. Lives with the home feature for now and
 * moves to the mood feature when that is built.
 */
@Composable
internal fun moodColor(mood: Mood): Color {
    val c = ChoomHubTheme.colors
    return when (mood) {
        Mood.Cozy, Mood.Loved -> c.pink
        Mood.Sleepy, Mood.Calm -> c.lavender
        Mood.Happy, Mood.Excited -> c.candle
        Mood.MissingYou, Mood.Lonely -> c.purple
        Mood.Tired -> c.textSecondary
        Mood.Sad -> Color(0xFF8FA8D8)
        Mood.Stressed -> c.tomato
    }
}

/** A soft glowing orb tinted with the mood's colour. */
@Composable
internal fun MoodOrb(mood: Mood, modifier: Modifier = Modifier) {
    val color = moodColor(mood)
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), center, r), r, center)
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), color), center, r * 0.42f), r * 0.38f, center)
    }
}
