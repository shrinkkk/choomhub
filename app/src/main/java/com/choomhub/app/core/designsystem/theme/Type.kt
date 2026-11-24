package com.choomhub.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.choomhub.app.R

/**
 * Typography.
 *
 * - **Cormorant Garamond** (bundled, variable weight) for display text and
 *   captions: high-contrast, old-book, a little gothic.
 * - **Space Mono** (bundled) for small labels, eyebrows and timestamps: the
 *   zine / typewriter edge that keeps things from feeling too precious.
 * - The platform sans for body copy, for readability.
 *
 * Both bundled fonts are SIL Open Font License; licence texts ship in
 * assets/licenses.
 */
internal val DisplayFamily = FontFamily(
    Font(R.font.cormorant_garamond, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.cormorant_garamond, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.cormorant_garamond, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(
        R.font.cormorant_garamond_italic,
        FontWeight.Normal,
        FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.cormorant_garamond_italic,
        FontWeight.Medium,
        FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
)

internal val MonoFamily = FontFamily(Font(R.font.space_mono_regular, FontWeight.Normal))

internal val BodyFamily: FontFamily = FontFamily.Default

internal val ChoomHubTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 44.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.04.em,
    ),
    labelMedium = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.06.em,
    ),
    labelSmall = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.06.em,
    ),
)

/** Extra styles that Material's type scale has no slot for. */
object ChoomHubTextStyles {
    /** Italic serif for captions, quotes and asides. */
    val Handwritten = TextStyle(
        fontFamily = DisplayFamily,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    )

    /** Uppercase mono overline for section eyebrows and dates. */
    val Eyebrow = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.16.em,
    )
}
