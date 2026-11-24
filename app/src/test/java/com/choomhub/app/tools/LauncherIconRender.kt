package com.choomhub.app.tools

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.feature.home.meetup.CatExpression
import com.choomhub.app.feature.home.meetup.drawCatFace
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the launcher icon's foreground layer (the Bored cat) to
 * app/build/outputs/icon/ (one folder per density). Not a check: when the icon art
 * changes, run it and copy the PNGs into the matching app/src/main/res/mipmap folders
 * (see docs/DECISIONS.md, ADR-029).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherIconRender {

    @Test
    fun renderForeground() {
        mapOf("mdpi" to 108, "hdpi" to 162, "xhdpi" to 216, "xxhdpi" to 324, "xxxhdpi" to 432).forEach { (bucket, px) ->
            render(px, File("build/outputs/icon/mipmap-$bucket").apply { mkdirs() })
        }
    }

    private fun render(px: Int, dir: File) {
        val image = ImageBitmap(px, px)
        val unit = px / 108f
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(px.toFloat(), px.toFloat())) {
            // The cat's 100-unit grid shown at CAT_DP, centred; its neck runs past the bottom of the mask.
            val s = CAT_DP * unit / 100f
            translate((px - 100f * s) / 2f, (px - 100f * s) / 2f + TOP_NUDGE_DP * unit) {
                scale(s, s, pivot = Offset.Zero) { drawCatFace(CatExpression.Bored) }
            }
        }
        File(dir, "ic_launcher_foreground.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val CAT_DP = 70f
        const val TOP_NUDGE_DP = 2f
    }
}
