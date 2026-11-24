package com.choomhub.app.domain.model

/**
 * How the two of you appear in the app.
 *
 * Each person names the *other*: you choose [partnerName] on your phone, and
 * your partner chooses the name that appears for you ([myName]) on theirs.
 * Until the shared space syncs, [myName] is unknown on this device.
 */
data class CoupleNames(
    /** The name you gave your partner. Null until onboarding is done. */
    val partnerName: String?,
    /** The name your partner gave you. Arrives via sync; null until then. */
    val myName: String?,
) {
    val needsOnboarding: Boolean get() = partnerName == null
}

/** Validation and normalisation for names typed by either person. */
object NameRules {
    const val MAX_LENGTH = 24

    sealed interface Result {
        data class Valid(val name: String) : Result
        data object Empty : Result
        data object TooLong : Result
    }

    /** Trims and collapses inner whitespace. */
    fun clean(input: String): String = input.trim().replace(Regex("\\s+"), " ")

    fun validate(input: String): Result {
        val cleaned = clean(input)
        return when {
            cleaned.isEmpty() -> Result.Empty
            cleaned.length > MAX_LENGTH -> Result.TooLong
            else -> Result.Valid(cleaned)
        }
    }

    fun messageFor(result: Result): String? = when (result) {
        Result.Empty -> "Give them a name first."
        Result.TooLong -> "Keep it under ${MAX_LENGTH + 1} characters."
        is Result.Valid -> null
    }
}
