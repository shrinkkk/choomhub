package com.choomhub.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import java.time.LocalTime

/**
 * The time of day the app is dressed for. Every phase stays dark and muted
 * (bruised violet, plum, wine, ink); only the night is near-black.
 */
enum class SkyPhase {
    Dawn,
    Day,
    Dusk,
    Night,
    ;

    companion object {
        private val DAWN = LocalTime.of(5, 0)
        private val DAY = LocalTime.of(8, 0)
        private val DUSK = LocalTime.of(17, 0)
        private val NIGHT = LocalTime.of(19, 30)

        fun at(time: LocalTime): SkyPhase = when {
            time < DAWN -> Night
            time < DAY -> Dawn
            time < DUSK -> Day
            time < NIGHT -> Dusk
            else -> Night
        }
    }
}

/** Backdrop gradient, top to bottom, plus how present the stars are. */
@Immutable
data class SkyColors(val top: Color, val middle: Color, val bottom: Color, val starAlpha: Float)

val LocalSkyPhase = staticCompositionLocalOf { SkyPhase.Night }

internal fun skyColorsFor(phase: SkyPhase): SkyColors = when (phase) {
    SkyPhase.Night -> SkyColors(Color(0xFF050506), Color(0xFF09090B), Color(0xFF15121B), starAlpha = 1f)
    // Bruised violet fading to a faint mauve near the floor.
    SkyPhase.Dawn -> SkyColors(Color(0xFF110C18), Color(0xFF1B1322), Color(0xFF2E1D2A), starAlpha = 0.35f)
    // Overcast violet: the lightest the app ever gets, still dark.
    SkyPhase.Day -> SkyColors(Color(0xFF1C1528), Color(0xFF1F182B), Color(0xFF17121F), starAlpha = 0f)
    // Plum into wine.
    SkyPhase.Dusk -> SkyColors(Color(0xFF120B14), Color(0xFF1F111B), Color(0xFF341824), starAlpha = 0.5f)
}

/** Surfaces pick up a little of the sky so cards don't look pasted on. */
internal fun choomHubColorsFor(phase: SkyPhase): ChoomHubColors = when (phase) {
    SkyPhase.Night -> DefaultChoomHubColors
    SkyPhase.Dawn -> DefaultChoomHubColors.copy(
        night = Color(0xFF1B1322), nightDeep = Color(0xFF0F0A14),
        surface = Color(0xFF1A1420), surfaceElevated = Color(0xFF211A28), surfaceHighest = Color(0xFF2B2333),
        outline = Color(0xFF3A3042),
    )
    SkyPhase.Day -> DefaultChoomHubColors.copy(
        night = Color(0xFF1F182B), nightDeep = Color(0xFF130E1B),
        surface = Color(0xFF1A1523), surfaceElevated = Color(0xFF221C2D), surfaceHighest = Color(0xFF2C2539),
        outline = Color(0xFF3B3349),
    )
    SkyPhase.Dusk -> DefaultChoomHubColors.copy(
        night = Color(0xFF1F111B), nightDeep = Color(0xFF110A10),
        surface = Color(0xFF1A1218), surfaceElevated = Color(0xFF22171F), surfaceHighest = Color(0xFF2C1F28),
        outline = Color(0xFF3C2B35),
    )
}
