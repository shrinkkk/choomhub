package com.afterglow.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale on a 4dp grid. Use these instead of ad-hoc dp values so that
 * rhythm stays consistent between screens.
 */
@Immutable
data class AfterglowSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    /** Horizontal padding for screen content. */
    val screenGutter: Dp = 20.dp,
    /** Vertical gap between home-screen sections. */
    val sectionGap: Dp = 28.dp,
)

internal val LocalAfterglowSpacing = staticCompositionLocalOf { AfterglowSpacing() }
