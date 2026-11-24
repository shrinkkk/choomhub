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
    val Night = Color(0xFF14121B)
    val NightDeep = Color(0xFF0E0C14)
    val Surface = Color(0xFF201C2B)
    val SurfaceElevated = Color(0xFF292337)
    val SurfaceHighest = Color(0xFF332C44)
    val Outline = Color(0xFF3D3550)

    val Purple = Color(0xFFA78BFA)
    val PurpleDeep = Color(0xFF4C3A7A)
    val Pink = Color(0xFFD8A5BA)
    val PinkDeep = Color(0xFF5E3A4D)
    val Lavender = Color(0xFFC8B6E5)
    val Tomato = Color(0xFFE87575)
    val TomatoDeep = Color(0xFF6B2E33)
    val Leaf = Color(0xFF8FB996)
    val Candle = Color(0xFFF2C98A)

    val TextMain = Color(0xFFF1EAF7)
    val TextSecondary = Color(0xFFA99DB8)
    val TextFaint = Color(0xFF6F6580)
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
    val leaf: Color,
    val candle: Color,
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
    leaf = ChoomHubPalette.Leaf,
    candle = ChoomHubPalette.Candle,
    textMain = ChoomHubPalette.TextMain,
    textSecondary = ChoomHubPalette.TextSecondary,
    textFaint = ChoomHubPalette.TextFaint,
)

internal val LocalChoomHubColors = staticCompositionLocalOf { DefaultChoomHubColors }
