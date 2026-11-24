package com.choomhub.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Material 3 colour scheme mapped onto the ChoomHub palette. The app is
 * always dark by design, so there is intentionally no light scheme and no
 * dynamic (wallpaper) colour.
 */
private val ChoomHubColorScheme = darkColorScheme(
    primary = ChoomHubPalette.Purple,
    onPrimary = ChoomHubPalette.NightDeep,
    primaryContainer = ChoomHubPalette.PurpleDeep,
    onPrimaryContainer = ChoomHubPalette.Lavender,
    secondary = ChoomHubPalette.Pink,
    onSecondary = ChoomHubPalette.NightDeep,
    secondaryContainer = ChoomHubPalette.PinkDeep,
    onSecondaryContainer = ChoomHubPalette.Pink,
    tertiary = ChoomHubPalette.Tomato,
    onTertiary = ChoomHubPalette.NightDeep,
    tertiaryContainer = ChoomHubPalette.TomatoDeep,
    onTertiaryContainer = ChoomHubPalette.Tomato,
    background = ChoomHubPalette.Night,
    onBackground = ChoomHubPalette.TextMain,
    surface = ChoomHubPalette.Surface,
    onSurface = ChoomHubPalette.TextMain,
    surfaceVariant = ChoomHubPalette.SurfaceElevated,
    onSurfaceVariant = ChoomHubPalette.TextSecondary,
    surfaceContainerLowest = ChoomHubPalette.NightDeep,
    surfaceContainerLow = ChoomHubPalette.Night,
    surfaceContainer = ChoomHubPalette.Surface,
    surfaceContainerHigh = ChoomHubPalette.SurfaceElevated,
    surfaceContainerHighest = ChoomHubPalette.SurfaceHighest,
    outline = ChoomHubPalette.Outline,
    outlineVariant = ChoomHubPalette.SurfaceHighest,
    inverseSurface = ChoomHubPalette.TextMain,
    inverseOnSurface = ChoomHubPalette.Night,
    error = ChoomHubPalette.Tomato,
    onError = ChoomHubPalette.NightDeep,
)

@Composable
fun ChoomHubTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalChoomHubColors provides DefaultChoomHubColors,
        LocalChoomHubSpacing provides ChoomHubSpacing(),
    ) {
        MaterialTheme(
            colorScheme = ChoomHubColorScheme,
            typography = ChoomHubTypography,
            shapes = ChoomHubShapes,
            content = content,
        )
    }
}

/** Accessors for ChoomHub-specific tokens, mirroring `MaterialTheme.*`. */
object ChoomHubTheme {
    val colors: ChoomHubColors
        @Composable @ReadOnlyComposable
        get() = LocalChoomHubColors.current

    val spacing: ChoomHubSpacing
        @Composable @ReadOnlyComposable
        get() = LocalChoomHubSpacing.current
}
