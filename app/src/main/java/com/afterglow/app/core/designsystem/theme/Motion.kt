package com.afterglow.app.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether decorative, continuously running animations (twinkling stars,
 * drifting rain) should play. Set from the system "Remove animations"
 * accessibility setting in MainActivity, and turned off in UI tests.
 * One-shot feedback animations (press scale, fades) are always allowed.
 */
val LocalAmbientMotionEnabled = staticCompositionLocalOf { true }
