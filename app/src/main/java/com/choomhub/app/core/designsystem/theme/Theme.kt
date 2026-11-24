package com.choomhub.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/**
 * Material 3 colour scheme mapped onto the ChoomHub palette (surfaces follow the [SkyPhase]). The app is
 * always dark by design, so there is intentionally no light scheme and no
 * dynamic (wallpaper) colour.
 */
private fun colorSchemeFor(colors: ChoomHubColors) = darkColorScheme(
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
    background = colors.night,
    onBackground = ChoomHubPalette.TextMain,
    surface = colors.surface,
    onSurface = ChoomHubPalette.TextMain,
    surfaceVariant = colors.surfaceElevated,
    onSurfaceVariant = ChoomHubPalette.TextSecondary,
    surfaceContainerLowest = colors.nightDeep,
    surfaceContainerLow = colors.night,
    surfaceContainer = colors.surface,
    surfaceContainerHigh = colors.surfaceElevated,
    surfaceContainerHighest = colors.surfaceHighest,
    outline = colors.outline,
    outlineVariant = colors.surfaceHighest,
    inverseSurface = ChoomHubPalette.TextMain,
    inverseOnSurface = colors.night,
    error = ChoomHubPalette.Tomato,
    onError = ChoomHubPalette.NightDeep,
)

@Composable
fun ChoomHubTheme(phase: SkyPhase = SkyPhase.Night, content: @Composable () -> Unit) {
    val colors = remember(phase) { choomHubColorsFor(phase) }
    val scheme = remember(colors) { colorSchemeFor(colors) }
    CompositionLocalProvider(
        LocalChoomHubColors provides colors,
        LocalChoomHubSpacing provides ChoomHubSpacing(),
        LocalSkyPhase provides phase,
    ) {
        MaterialTheme(
            colorScheme = scheme,
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

    val sky: SkyPhase
        @Composable @ReadOnlyComposable
        get() = LocalSkyPhase.current

    val spacing: ChoomHubSpacing
        @Composable @ReadOnlyComposable
        get() = LocalChoomHubSpacing.current
}
