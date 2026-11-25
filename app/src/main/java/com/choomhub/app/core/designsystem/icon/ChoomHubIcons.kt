package com.choomhub.app.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn line icons used for navigation. Defined as vectors so they can be
 * tinted with [androidx.compose.material3.Icon]. Each is a 24x24 viewport
 * with a soft rounded 1.8 stroke.
 */
object ChoomHubIcons {
    val Home: ImageVector by lazy {
        lineIcon("Home") {
            moveTo(3.5f, 11f); lineTo(12f, 4f); lineTo(20.5f, 11f)
            moveTo(6f, 9.5f); lineTo(6f, 19.5f); lineTo(18f, 19.5f); lineTo(18f, 9.5f)
            // Arched door.
            moveTo(10f, 19.5f); lineTo(10f, 15f)
            arcTo(2f, 2f, 0f, false, true, 14f, 15f)
            lineTo(14f, 19.5f)
        }
    }

    val Mailbox: ImageVector by lazy {
        lineIcon("Messages") {
            moveTo(4.5f, 6f); lineTo(19.5f, 6f)
            arcTo(1.5f, 1.5f, 0f, false, true, 21f, 7.5f)
            lineTo(21f, 16.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, 19.5f, 18f)
            lineTo(4.5f, 18f)
            arcTo(1.5f, 1.5f, 0f, false, true, 3f, 16.5f)
            lineTo(3f, 7.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, 4.5f, 6f)
            close()
            moveTo(3.6f, 7f); lineTo(12f, 13f); lineTo(20.4f, 7f)
        }
    }

    val Sparkles: ImageVector by lazy {
        lineIcon("Activities") {
            moveTo(10f, 4f)
            curveTo(10.6f, 9f, 11.9f, 10.4f, 17f, 11f)
            curveTo(11.9f, 11.6f, 10.6f, 13f, 10f, 18f)
            curveTo(9.4f, 13f, 8.1f, 11.6f, 3f, 11f)
            curveTo(8.1f, 10.4f, 9.4f, 9f, 10f, 4f)
            close()
            moveTo(18f, 3.5f); lineTo(18f, 7.5f)
            moveTo(16f, 5.5f); lineTo(20f, 5.5f)
            moveTo(17.5f, 16.5f); lineTo(17.5f, 20.5f)
            moveTo(15.5f, 18.5f); lineTo(19.5f, 18.5f)
        }
    }

    val Moon: ImageVector by lazy {
        lineIcon("Our World") {
            moveTo(16f, 4.6f)
            arcTo(8f, 8f, 0f, true, false, 16f, 19.4f)
            arcTo(8.5f, 8.5f, 0f, false, true, 16f, 4.6f)
            close()
            moveTo(19.5f, 9f); lineTo(19.5f, 11f)
            moveTo(18.5f, 10f); lineTo(20.5f, 10f)
        }
    }

    val Sliders: ImageVector by lazy {
        lineIcon("Settings") {
            moveTo(4f, 7f); lineTo(13f, 7f)
            moveTo(17f, 7f); lineTo(20f, 7f)
            circle(15f, 7f, 2f)
            moveTo(4f, 12f); lineTo(7f, 12f)
            moveTo(11f, 12f); lineTo(20f, 12f)
            circle(9f, 12f, 2f)
            moveTo(4f, 17f); lineTo(14f, 17f)
            moveTo(18f, 17f); lineTo(20f, 17f)
            circle(16f, 17f, 2f)
        }
    }
}

/** Line icons for the gesture buttons on Home. */
object GestureIcons {
    /** Two people in a hug: heads tipped together, shoulders meeting, one arm around the other. */
    val Hug: ImageVector by lazy {
        lineIcon("Hug") {
            circle(8.1f, 8.6f, 2.9f)
            circle(15.9f, 8.6f, 2.9f)
            // Shoulders meeting in the middle.
            moveTo(2.8f, 21.5f)
            curveTo(2.8f, 16.6f, 5f, 14f, 8.1f, 14f)
            curveTo(9.6f, 14f, 10.9f, 14.6f, 12f, 15.6f)
            curveTo(13.1f, 14.6f, 14.4f, 14f, 15.9f, 14f)
            curveTo(19f, 14f, 21.2f, 16.6f, 21.2f, 21.5f)
            // One arm wrapped around the other's back.
            moveTo(6.2f, 19.2f)
            curveTo(9.4f, 17.2f, 14.2f, 17.1f, 18.2f, 18.8f)
        }
    }

    /** One person leaning in to kiss the other's forehead, with a small heart above. */
    val ForeheadKiss: ImageVector by lazy {
        lineIcon("Forehead kiss") {
            // Taller person, head tipped toward the other.
            circle(9.6f, 9.4f, 3.1f)
            moveTo(3.2f, 21.5f)
            curveTo(3.2f, 16.5f, 5.6f, 14.4f, 8.8f, 14.4f)
            curveTo(10.2f, 14.4f, 11.3f, 14.8f, 12.1f, 15.5f)
            // Shorter person, forehead to the other's lips.
            circle(15.6f, 13.6f, 2.8f)
            moveTo(10.8f, 21.5f)
            curveTo(11f, 18.3f, 12.9f, 17.2f, 15.6f, 17.2f)
            curveTo(18.3f, 17.2f, 20.3f, 18.3f, 20.6f, 21.5f)
            // A small heart above where they meet.
            moveTo(15.8f, 7.6f)
            curveTo(14.3f, 6.6f, 13.8f, 5.6f, 14.5f, 4.9f)
            curveTo(14.9f, 4.5f, 15.5f, 4.6f, 15.8f, 5.1f)
            curveTo(16.1f, 4.6f, 16.7f, 4.5f, 17.1f, 4.9f)
            curveTo(17.8f, 5.6f, 17.3f, 6.6f, 15.8f, 7.6f)
            close()
        }
    }

    val Paw: ImageVector by lazy {
        lineIcon("Head pats") {
            moveTo(8f, 16.5f)
            curveTo(8f, 13.5f, 10f, 12f, 12f, 12f)
            curveTo(14f, 12f, 16f, 13.5f, 16f, 16.5f)
            curveTo(16f, 18.5f, 14.5f, 19.5f, 12f, 19.5f)
            curveTo(9.5f, 19.5f, 8f, 18.5f, 8f, 16.5f)
            close()
            circle(6.3f, 11.2f, 1.6f)
            circle(9.4f, 7.6f, 1.7f)
            circle(14.6f, 7.6f, 1.7f)
            circle(17.7f, 11.2f, 1.6f)
        }
    }

    val Tomato: ImageVector by lazy {
        lineIcon("Tomato") {
            moveTo(12f, 7.8f)
            curveTo(17f, 7.8f, 20.5f, 10.7f, 20.5f, 14.4f)
            curveTo(20.5f, 18.3f, 16.8f, 20.5f, 12f, 20.5f)
            curveTo(7.2f, 20.5f, 3.5f, 18.3f, 3.5f, 14.4f)
            curveTo(3.5f, 10.7f, 7f, 7.8f, 12f, 7.8f)
            close()
            moveTo(12f, 8.6f); lineTo(8.6f, 6.6f)
            moveTo(12f, 8.6f); lineTo(15.4f, 6.6f)
            moveTo(12f, 8.6f); lineTo(7.8f, 10f)
            moveTo(12f, 8.6f); lineTo(16.2f, 10f)
            moveTo(12f, 8.6f); lineTo(12f, 3.8f)
            lineTo(13.6f, 2.8f)
        }
    }
}

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, true, cx + r, cy)
    arcTo(r, r, 0f, true, true, cx - r, cy)
    close()
}

/** Bold marks for the points boxes: plus to award, minus to take away, cross to close the form. */
object PointIcons {
    val Plus: ImageVector by lazy { lineIcon("Plus", width = 3f) { moveTo(12f, 4.5f); lineTo(12f, 19.5f); moveTo(4.5f, 12f); lineTo(19.5f, 12f) } }
    val Minus: ImageVector by lazy { lineIcon("Minus", width = 3f) { moveTo(4.5f, 12f); lineTo(19.5f, 12f) } }
    val Close: ImageVector by lazy { lineIcon("Close", width = 3f) { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) } }
}

private fun lineIcon(name: String, width: Float = 1.8f, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    ).build()
