package com.choomhub.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Raw palette. Screens should not reference these directly; use
 * [MaterialTheme.colorScheme] or [ChoomHubTheme.colors] so the palette can be
 * tuned in one place.
 */
internal object ChoomHubPalette {
    // Near-black with a violet cast, not pure black: keeps depth without looking like OLED-void.
    val Night = Color(0xFF0E0D12)
    val NightDeep = Color(0xFF09080C)
    val Surface = Color(0xFF17151C)
    val SurfaceElevated = Color(0xFF1E1B25)
    val SurfaceHighest = Color(0xFF282431)
    val Outline = Color(0xFF34303D)

    // Accents are deliberately desaturated: dusty violet, faded rose, old gold.
    val Purple = Color(0xFF8E7BB5)
    val PurpleDeep = Color(0xFF3A3150)
    val Pink = Color(0xFFB98A9E)
    val PinkDeep = Color(0xFF4A2E3B)
    val Lavender = Color(0xFFB9AECB)
    val Tomato = Color(0xFFC25B55)
    val TomatoDeep = Color(0xFF4F2224)
    val Wine = Color(0xFF5C2233)
    val Leaf = Color(0xFF6F8A72)
    val Candle = Color(0xFFD9B27C)
    val Bone = Color(0xFFD8D0C3)

    val TextMain = Color(0xFFE6E0EA)
    val TextSecondary = Color(0xFF9A90A6)
    val TextFaint = Color(0xFF625A6C)
}

/**
 * Brand colours that Material 3's [androidx.compose.material3.ColorScheme] has no
 * slot for (tomato, candlelight, the elevated card tone, etc.).
 */
@Immutable
data class ChoomHubColors(
    val night: Color,
    val nightDeep: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighest: Color,
    val outline: Color,
    val purple: Color,
    val pink: Color,
    val lavender: Color,
    val tomato: Color,
    val tomatoDeep: Color,
    val wine: Color,
    val leaf: Color,
    val candle: Color,
    val bone: Color,
    val textMain: Color,
    val textSecondary: Color,
    val textFaint: Color,
)

internal val DefaultChoomHubColors = ChoomHubColors(
    night = ChoomHubPalette.Night,
    nightDeep = ChoomHubPalette.NightDeep,
    surface = ChoomHubPalette.Surface,
    surfaceElevated = ChoomHubPalette.SurfaceElevated,
    surfaceHighest = ChoomHubPalette.SurfaceHighest,
    outline = ChoomHubPalette.Outline,
    purple = ChoomHubPalette.Purple,
    pink = ChoomHubPalette.Pink,
    lavender = ChoomHubPalette.Lavender,
    tomato = ChoomHubPalette.Tomato,
    tomatoDeep = ChoomHubPalette.TomatoDeep,
    wine = ChoomHubPalette.Wine,
    leaf = ChoomHubPalette.Leaf,
    candle = ChoomHubPalette.Candle,
    bone = ChoomHubPalette.Bone,
    textMain = ChoomHubPalette.TextMain,
    textSecondary = ChoomHubPalette.TextSecondary,
    textFaint = ChoomHubPalette.TextFaint,
)

internal val LocalChoomHubColors = staticCompositionLocalOf { DefaultChoomHubColors }
