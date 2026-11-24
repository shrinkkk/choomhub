package com.afterglow.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner radii. Generous and soft, like cushions. */
object AfterglowRadii {
    val Small = 10.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
}

internal val AfterglowShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(AfterglowRadii.Small),
    medium = RoundedCornerShape(AfterglowRadii.Medium),
    large = RoundedCornerShape(AfterglowRadii.Large),
    extraLarge = RoundedCornerShape(AfterglowRadii.ExtraLarge),
)
