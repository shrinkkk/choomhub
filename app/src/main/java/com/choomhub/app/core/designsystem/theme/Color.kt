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
    // Ink-and-bone: near-black surfaces, off-white "paper" accents, colour used sparingly.
    val Night = Color(0xFF09090B)
    val NightDeep = Color(0xFF050506)
    val Surface = Color(0xFF121114)
    val SurfaceElevated = Color(0xFF19181C)
    val SurfaceHighest = Color(0xFF232126)
    val Outline = Color(0xFF302E33)

    // "Purple" is now a pale bone used for primary actions; the old violet survives
    // only as a faint grey-lilac, and the rose as a dusty pink for small accents.
    val Purple = Color(0xFFE2DACB)
    val PurpleDeep = Color(0xFF3A3731)
    val Pink = Color(0xFFC4A3A8)
    val PinkDeep = Color(0xFF3E2C30)
    val Lavender = Color(0xFFB8B2BF)
    val Tomato = Color(0xFFC0504A)
    val TomatoDeep = Color(0xFF4A1F1E)
    val Wine = Color(0xFF5C2233)
    val Leaf = Color(0xFF7D8C76)
    val Candle = Color(0xFFE6D6AE)
    val Bone = Color(0xFFEDE6D6)

    val TextMain = Color(0xFFECE6DA)
    val TextSecondary = Color(0xFF9B958E)
    val TextFaint = Color(0xFF5F5B57)
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
