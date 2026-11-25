package com.choomhub.app.feature.pet

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Line icons for the Pet House, in the same hand as the app's own (24x24, rounded 1.8 stroke). */
internal object PetIcons {
    val Bowl: ImageVector by lazy {
        icon("Feed") {
            moveTo(3f, 12f); lineTo(21f, 12f)
            moveTo(4f, 12f); curveTo(4.8f, 17.5f, 8f, 19.5f, 12f, 19.5f); curveTo(16f, 19.5f, 19.2f, 17.5f, 20f, 12f)
            moveTo(8f, 9.5f); lineTo(8.6f, 9f)
            moveTo(11.5f, 8.5f); lineTo(12.2f, 8.2f)
            moveTo(15f, 9.6f); lineTo(15.7f, 9.2f)
        }
    }

    val Drop: ImageVector by lazy {
        icon("Water") {
            moveTo(12f, 3.5f)
            curveTo(16f, 9f, 18.5f, 12f, 18.5f, 15f)
            curveTo(18.5f, 18.6f, 15.6f, 21f, 12f, 21f)
            curveTo(8.4f, 21f, 5.5f, 18.6f, 5.5f, 15f)
            curveTo(5.5f, 12f, 8f, 9f, 12f, 3.5f)
            close()
            moveTo(9f, 15.5f); curveTo(9.2f, 17f, 10f, 17.8f, 11.2f, 18f)
        }
    }

    val Scoop: ImageVector by lazy {
        icon("Litter") {
            moveTo(4f, 4f); lineTo(10.5f, 11f)
            moveTo(9.5f, 11f); lineTo(20f, 11f); lineTo(18.8f, 19.5f); lineTo(10.7f, 19.5f); close()
            moveTo(13f, 13.5f); lineTo(13f, 17f)
            moveTo(16f, 13.5f); lineTo(16f, 17f)
        }
    }

    val Heart: ImageVector by lazy {
        icon("Pet") {
            moveTo(12f, 20f)
            curveTo(4f, 14.5f, 3f, 10.5f, 3f, 8.3f)
            curveTo(3f, 5.6f, 5f, 4f, 7.3f, 4f)
            curveTo(9.5f, 4f, 11f, 5.4f, 12f, 7f)
            curveTo(13f, 5.4f, 14.5f, 4f, 16.7f, 4f)
            curveTo(19f, 4f, 21f, 5.6f, 21f, 8.3f)
            curveTo(21f, 10.5f, 20f, 14.5f, 12f, 20f)
            close()
        }
    }

    val Comb: ImageVector by lazy {
        icon("Brush") {
            moveTo(4f, 7.5f); lineTo(20f, 7.5f); lineTo(20f, 11f); lineTo(4f, 11f); close()
            moveTo(6f, 11f); lineTo(6f, 17.5f)
            moveTo(9f, 11f); lineTo(9f, 17.5f)
            moveTo(12f, 11f); lineTo(12f, 17.5f)
            moveTo(15f, 11f); lineTo(15f, 17.5f)
            moveTo(18f, 11f); lineTo(18f, 17.5f)
        }
    }

    val Wand: ImageVector by lazy {
        icon("Play") {
            moveTo(3.5f, 20.5f); lineTo(13f, 9f)
            moveTo(13f, 9f); curveTo(13.5f, 5f, 17.5f, 3f, 21f, 3f); curveTo(21f, 6.5f, 18.5f, 10.5f, 13f, 9f)
            moveTo(15.5f, 7.5f); lineTo(18.5f, 5f)
        }
    }

    val Cross: ImageVector by lazy {
        icon("Care") {
            moveTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 9f); lineTo(20f, 9f); lineTo(20f, 15f); lineTo(15f, 15f)
            lineTo(15f, 20f); lineTo(9f, 20f); lineTo(9f, 15f); lineTo(4f, 15f); lineTo(4f, 9f); lineTo(9f, 9f); close()
        }
    }

    val Hanger: ImageVector by lazy {
        icon("Wardrobe") {
            moveTo(10f, 6.5f)
            curveTo(10f, 4.4f, 14f, 4.4f, 14f, 6.5f)
            curveTo(14f, 8.1f, 12f, 8.3f, 12f, 9.8f)
            lineTo(3f, 16.5f); lineTo(21f, 16.5f); lineTo(12f, 9.8f)
        }
    }

    val Bag: ImageVector by lazy {
        icon("Shop") {
            moveTo(5f, 8f); lineTo(19f, 8f); lineTo(18f, 20f); lineTo(6f, 20f); close()
            moveTo(9f, 10.5f); lineTo(9f, 7f); curveTo(9f, 3.6f, 15f, 3.6f, 15f, 7f); lineTo(15f, 10.5f)
        }
    }

    val Book: ImageVector by lazy {
        icon("Diary") {
            moveTo(6f, 3.5f); lineTo(19f, 3.5f); lineTo(19f, 20.5f); lineTo(6f, 20.5f)
            curveTo(4.9f, 20.5f, 4.5f, 19.8f, 4.5f, 19f); lineTo(4.5f, 5f); curveTo(4.5f, 4.2f, 4.9f, 3.5f, 6f, 3.5f); close()
            moveTo(8f, 3.5f); lineTo(8f, 20.5f)
            moveTo(11f, 8f); lineTo(16f, 8f)
            moveTo(11f, 11f); lineTo(15f, 11f)
        }
    }

    val Gem: ImageVector by lazy {
        icon("Gifts") {
            moveTo(7f, 5f); lineTo(17f, 5f); lineTo(21f, 10f); lineTo(12f, 20f); lineTo(3f, 10f); close()
            moveTo(3f, 10f); lineTo(21f, 10f)
            moveTo(9.5f, 5f); lineTo(8.5f, 10f); lineTo(12f, 20f)
            moveTo(14.5f, 5f); lineTo(15.5f, 10f); lineTo(12f, 20f)
        }
    }

    val Trophy: ImageVector by lazy {
        icon("Trophies") {
            moveTo(7f, 4f); lineTo(17f, 4f); lineTo(17f, 9f)
            curveTo(17f, 12f, 15f, 14f, 12f, 14f); curveTo(9f, 14f, 7f, 12f, 7f, 9f); close()
            moveTo(7f, 6f); curveTo(3.5f, 6f, 3.5f, 11f, 7.5f, 11f)
            moveTo(17f, 6f); curveTo(20.5f, 6f, 20.5f, 11f, 16.5f, 11f)
            moveTo(12f, 14f); lineTo(12f, 18f)
            moveTo(8f, 20f); lineTo(16f, 20f)
        }
    }

    val Back: ImageVector by lazy {
        icon("Back") {
            moveTo(14.5f, 5f); lineTo(7.5f, 12f); lineTo(14.5f, 19f)
        }
    }
}

private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = block)
        .build()
