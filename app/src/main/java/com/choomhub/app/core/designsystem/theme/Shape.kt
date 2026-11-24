package com.choomhub.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner radii. Kept tight: soft enough to feel finished, sharp enough not to feel bubbly. */
object ChoomHubRadii {
    val Small = 6.dp
    val Medium = 10.dp
    val Large = 14.dp
    val ExtraLarge = 20.dp
}

internal val ChoomHubShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(ChoomHubRadii.Small),
    medium = RoundedCornerShape(ChoomHubRadii.Medium),
    large = RoundedCornerShape(ChoomHubRadii.Large),
    extraLarge = RoundedCornerShape(ChoomHubRadii.ExtraLarge),
)
