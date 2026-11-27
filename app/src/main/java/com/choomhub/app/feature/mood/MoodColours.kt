package com.choomhub.app.feature.mood

import com.choomhub.app.core.designsystem.component.OneLineText
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.choomhub.app.core.designsystem.theme.ChoomHubTextStyles
import com.choomhub.app.core.designsystem.theme.ChoomHubTheme

/**
 * Colours a mood can be, each in three shades (deep, mid, soft), picked to sit in the app's
 * night palette rather than shout over it. Stored as "blue.1" etc; "rainbow" is its own.
 */
data class MoodHue(val key: String, val name: String, val shades: List<Color>)

object MoodColours {
    val HUES = listOf(
        MoodHue("red", "Red", listOf(Color(0xFF8E2A35), Color(0xFFC2464F), Color(0xFFE08A8F))),
        MoodHue("orange", "Orange", listOf(Color(0xFFA8502A), Color(0xFFE08A4A), Color(0xFFF0B98A))),
        MoodHue("gold", "Gold", listOf(Color(0xFFA8822E), Color(0xFFE6C36A), Color(0xFFF1E0A6))),
        MoodHue("green", "Green", listOf(Color(0xFF4E6B4A), Color(0xFF7D9C76), Color(0xFFB5C9A8))),
        MoodHue("teal", "Teal", listOf(Color(0xFF2F6B6B), Color(0xFF5E9C98), Color(0xFF9CC7C1))),
        MoodHue("blue", "Blue", listOf(Color(0xFF4F6A9A), Color(0xFF7F93B8), Color(0xFFB3C1DB))),
        MoodHue("purple", "Purple", listOf(Color(0xFF5B3F7A), Color(0xFF8B6FAE), Color(0xFFBFA8D9))),
        MoodHue("pink", "Pink", listOf(Color(0xFF9C3D62), Color(0xFFD27A9A), Color(0xFFEDB3C6))),
        MoodHue("brown", "Brown", listOf(Color(0xFF6B4A36), Color(0xFF9C7458), Color(0xFFC9A98C))),
        MoodHue("grey", "Grey", listOf(Color(0xFF5D5866), Color(0xFF8C8794), Color(0xFFBDB8C4))),
        MoodHue("white", "White", listOf(Color(0xFFB9B2A6), Color(0xFFE6DED0), Color(0xFFF7F3EC))),
    )

    const val RAINBOW = "rainbow"

    /** The rainbow, muted to match: an oil-slick sheen rather than a toy box. */
    val RAINBOW_COLOURS = listOf(Color(0xFFC2464F), Color(0xFFE08A4A), Color(0xFFE6C36A), Color(0xFF7D9C76), Color(0xFF5E9C98), Color(0xFF7F93B8), Color(0xFF8B6FAE), Color(0xFFD27A9A))

    /** The colour for a stored key ("blue.1"), or null for the default / rainbow. */
    fun colourOf(key: String?): Color? {
        val (hue, shade) = key?.split('.')?.takeIf { it.size == 2 } ?: return null
        return HUES.firstOrNull { it.key == hue }?.shades?.getOrNull(shade.toIntOrNull() ?: return null)
    }

    fun nameOf(key: String?): String = when {
        key == null -> "Default"
        key == RAINBOW -> "Rainbow"
        else -> {
            val (hue, shade) = key.split('.')
            val h = HUES.firstOrNull { it.key == hue }?.name ?: "Default"
            "$h, ${listOf("deep", "mid", "soft").getOrElse(shade.toIntOrNull() ?: 1) { "mid" }}"
        }
    }
}

/** Text in a mood's colour: plain colours as they are, the rainbow as a gradient. */
fun moodTextStyle(base: TextStyle, colourKey: String?, fallback: Color): TextStyle = when (colourKey) {
    MoodColours.RAINBOW -> base.copy(brush = Brush.linearGradient(MoodColours.RAINBOW_COLOURS))
    else -> base.copy(color = MoodColours.colourOf(colourKey) ?: fallback)
}

/** A small colour wheel showing the mood's current colour in the middle; opens the picker. */
@Composable
fun ColourWheelButton(colourKey: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val current = MoodColours.colourOf(colourKey)
    Box(
        modifier.size(30.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Change colour", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) {
            val segments = MoodColours.HUES.dropLast(2).map { it.shades[1] }
            val sweep = 360f / segments.size
            segments.forEachIndexed { i, c -> drawArc(c, i * sweep - 90f, sweep, true, Offset.Zero, size) }
            drawCircle(Color(0xFF15111A), size.minDimension * 0.3f)
            when {
                colourKey == MoodColours.RAINBOW -> drawCircle(Brush.sweepGradient(MoodColours.RAINBOW_COLOURS), size.minDimension * 0.22f)
                current != null -> drawCircle(current, size.minDimension * 0.22f)
                else -> drawCircle(Color(0xFF8C8794), size.minDimension * 0.18f, style = Stroke(1.5f))
            }
        }
    }
}

/** The picker: every colour in its three shades, the rainbow, and the default. */
@Composable
fun ColumnScope.MoodColourPickerContent(label: String, selected: String?, onPick: (String?) -> Unit, onClose: () -> Unit) {
    val colors = ChoomHubTheme.colors
    val spacing = ChoomHubTheme.spacing
    Text("Colour", style = MaterialTheme.typography.headlineMedium, color = colors.textMain)
    Text(label, style = moodTextStyle(MaterialTheme.typography.displaySmall, selected, colors.textMain))
    Text(MoodColours.nameOf(selected).lowercase(), style = ChoomHubTextStyles.Handwritten, color = colors.textFaint)
    Spacer(Modifier.height(spacing.lg))
    MoodColours.HUES.forEach { hue ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(hue.name.uppercase(), style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary, modifier = Modifier.width(72.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                hue.shades.forEachIndexed { i, c ->
                    val key = "${hue.key}.$i"
                    Swatch(Brush.linearGradient(listOf(c, c)), selected == key, Modifier.testTag("colour_$key")) { onPick(key) }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("RAINBOW", style = ChoomHubTextStyles.Eyebrow, color = colors.textSecondary, modifier = Modifier.width(72.dp))
        Swatch(Brush.sweepGradient(MoodColours.RAINBOW_COLOURS), selected == MoodColours.RAINBOW, Modifier.testTag("colour_rainbow")) { onPick(MoodColours.RAINBOW) }
    }
    Spacer(Modifier.height(spacing.md))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton({ onPick(null) }) { OneLineText("DEFAULT", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary) }
        TextButton(onClose) { OneLineText("DONE", style = MaterialTheme.typography.labelLarge, color = colors.pink) }
    }
}

@Composable
private fun Swatch(brush: Brush, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = ChoomHubTheme.colors
    Box(
        modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.bone else colors.outline.copy(alpha = 0.5f), CircleShape)
            .padding(if (selected) 4.dp else 2.dp)
            .clip(CircleShape)
            .background(brush)
            .clickable(role = Role.RadioButton, onClick = onClick),
    )
}
