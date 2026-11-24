package com.afterglow.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Material 3 colour scheme mapped onto the Afterglow palette. The app is
 * always dark by design, so there is intentionally no light scheme and no
 * dynamic (wallpaper) colour.
 */
private val AfterglowColorScheme = darkColorScheme(
    primary = AfterglowPalette.Purple,
    onPrimary = AfterglowPalette.NightDeep,
    primaryContainer = AfterglowPalette.PurpleDeep,
    onPrimaryContainer = AfterglowPalette.Lavender,
    secondary = AfterglowPalette.Pink,
    onSecondary = AfterglowPalette.NightDeep,
    secondaryContainer = AfterglowPalette.PinkDeep,
    onSecondaryContainer = AfterglowPalette.Pink,
    tertiary = AfterglowPalette.Tomato,
    onTertiary = AfterglowPalette.NightDeep,
    tertiaryContainer = AfterglowPalette.TomatoDeep,
    onTertiaryContainer = AfterglowPalette.Tomato,
    background = AfterglowPalette.Night,
    onBackground = AfterglowPalette.TextMain,
    surface = AfterglowPalette.Surface,
    onSurface = AfterglowPalette.TextMain,
    surfaceVariant = AfterglowPalette.SurfaceElevated,
    onSurfaceVariant = AfterglowPalette.TextSecondary,
    surfaceContainerLowest = AfterglowPalette.NightDeep,
    surfaceContainerLow = AfterglowPalette.Night,
    surfaceContainer = AfterglowPalette.Surface,
    surfaceContainerHigh = AfterglowPalette.SurfaceElevated,
    surfaceContainerHighest = AfterglowPalette.SurfaceHighest,
    outline = AfterglowPalette.Outline,
    outlineVariant = AfterglowPalette.SurfaceHighest,
    inverseSurface = AfterglowPalette.TextMain,
    inverseOnSurface = AfterglowPalette.Night,
    error = AfterglowPalette.Tomato,
    onError = AfterglowPalette.NightDeep,
)

@Composable
fun AfterglowTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAfterglowColors provides DefaultAfterglowColors,
        LocalAfterglowSpacing provides AfterglowSpacing(),
    ) {
        MaterialTheme(
            colorScheme = AfterglowColorScheme,
            typography = AfterglowTypography,
            shapes = AfterglowShapes,
            content = content,
        )
    }
}

/** Accessors for Afterglow-specific tokens, mirroring `MaterialTheme.*`. */
object AfterglowTheme {
    val colors: AfterglowColors
        @Composable @ReadOnlyComposable
        get() = LocalAfterglowColors.current

    val spacing: AfterglowSpacing
        @Composable @ReadOnlyComposable
        get() = LocalAfterglowSpacing.current
}
