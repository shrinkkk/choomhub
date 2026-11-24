package com.choomhub.app.domain.model

import java.time.Instant

/**
 * The preset moods a person can choose. Kept free of UI concerns; the
 * presentation layer decides colours and artwork for each. [key] is what's
 * stored in Firestore and sent to the push Worker; it never changes.
 */
enum class Mood(val label: String, val key: String) {
    Cozy("Cozy", "cozy"),
    Sleepy("Sleepy", "sleepy"),
    Content("Content", "content"),
    Calm("Calm", "calm"),
    Restless("Restless", "restless"),
    MissingYou("Missing you", "missing_you"),
    Tired("Tired", "tired"),
    Low("Low", "low"),
    Stressed("Stressed", "stressed"),
    Lonely("Lonely", "lonely"),
    Wired("Wired", "wired"),
    ;

    companion object {
        fun fromKey(key: String?): Mood? = entries.firstOrNull { it.key == key }
    }
}

/** Someone's current mood, with an optional short line about what's going on. */
data class MoodUpdate(
    val mood: Mood,
    val note: String?,
    /** Null only for a moment, while a just-saved mood waits for the server's clock. */
    val updatedAt: Instant?,
)

/** Both people's current moods. */
data class CoupleMoods(val mine: MoodUpdate?, val partner: MoodUpdate?)

object MoodRules {
    const val MAX_NOTE = 80

    /** Trims and flattens the note to one line; blank becomes null. Longer notes are cut. */
    fun cleanNote(input: String?): String? =
        input?.replace(Regex("\\s+"), " ")?.trim()?.take(MAX_NOTE)?.ifEmpty { null }
}
