package com.choomhub.app.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import kotlin.math.hypot

/**
 * The Memory Lane look: slightly faded, warm, a little less saturated, with a soft
 * vignette and light grain ("B · medium" from the preview). It's applied when a kept
 * snap is drawn; the stored photo is never changed.
 */
object PolaroidFilter {
    private const val SATURATION = 0.82f
    private const val CONTRAST = 0.88f
    private val warmth = floatArrayOf(1.06f, 1.01f, 0.90f)

    /** Lifted, slightly teal blacks, like old instant film (0..255 offsets). */
    private val lift = floatArrayOf(27.4f, 39.4f, 37.0f)

    val colorFilter: ColorFilter by lazy { ColorFilter.colorMatrix(ColorMatrix(matrix())) }

    /** Row-major 4×5 colour matrix: warmth × (saturation × contrast), plus the lift. */
    internal fun matrix(): FloatArray {
        val lr = 0.2126f
        val lg = 0.7152f
        val lb = 0.0722f
        val s = SATURATION
        val sat = arrayOf(
            floatArrayOf(lr * (1 - s) + s, lg * (1 - s), lb * (1 - s)),
            floatArrayOf(lr * (1 - s), lg * (1 - s) + s, lb * (1 - s)),
            floatArrayOf(lr * (1 - s), lg * (1 - s), lb * (1 - s) + s),
        )
        val out = FloatArray(20)
        for (row in 0..2) {
            for (col in 0..2) out[row * 5 + col] = warmth[row] * sat[row][col] * CONTRAST
            out[row * 5 + 4] = lift[row]
        }
        out[18] = 1f
        return out
    }
}

/** The vignette and grain of [PolaroidFilter], drawn over a photo. */
fun Modifier.polaroidFinish(): Modifier = this
    .drawWithCache {
        val radius = hypot(size.width / 2f, size.height / 2f)
        val vignette = Brush.radialGradient(
            0.0f to Color.Transparent,
            0.45f to Color.Transparent,
            0.75f to Color.Black.copy(alpha = 0.12f),
            1.0f to Color.Black.copy(alpha = 0.36f),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = radius,
        )
        onDrawWithContent {
            drawContent()
            drawRect(vignette)
        }
    }
    .filmGrain(0.07f)
