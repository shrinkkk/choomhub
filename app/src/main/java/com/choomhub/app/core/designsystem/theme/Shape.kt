package com.choomhub.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner radii. Generous and soft, like cushions. */
object ChoomHubRadii {
    val Small = 10.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
}

internal val ChoomHubShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(ChoomHubRadii.Small),
    medium = RoundedCornerShape(ChoomHubRadii.Medium),
    large = RoundedCornerShape(ChoomHubRadii.Large),
    extraLarge = RoundedCornerShape(ChoomHubRadii.ExtraLarge),
)
