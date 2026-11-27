package com.choomhub.app.domain.model

import java.time.Instant

/**
 * One choice in the mood list. The list is shared by the space and can be
 * edited (Settings → Moods); [key] identifies the choice and never changes,
 * [label] is what people see.
 */
data class MoodOption(
    val key: String,
    val label: String,
    /** The colour it's shown in (Settings → Moods): "blue.1", "rainbow", or null for its default. */
    val colour: String? = null,
)

/** The same moods, with each one's colour taken from the space's list (matched by key). */
fun CoupleMoods.coloured(options: List<MoodOption>): CoupleMoods {
    fun MoodUpdate.fill() = copy(mood = mood.copy(colour = options.firstOrNull { it.key == mood.key }?.colour))
    return CoupleMoods(mine?.fill(), partner?.fill())
}

/**
 * The default mood list, used until the space edits it. Kept free of UI
 * concerns; the presentation layer decides colours for each.
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

    val option: MoodOption get() = MoodOption(key, label)

    companion object {
        fun fromKey(key: String?): Mood? = entries.firstOrNull { it.key == key }

        val defaults: List<MoodOption> get() = entries.map { it.option }
    }
}

/** Someone's current mood, with an optional short line about what's going on. */
data class MoodUpdate(
    val mood: MoodOption,
    val note: String?,
    /** Null only for a moment, while a just-saved mood waits for the server's clock. */
    val updatedAt: Instant?,
)

/** Both people's current moods. */
data class CoupleMoods(val mine: MoodUpdate?, val partner: MoodUpdate?)

object MoodRules {
    const val MAX_NOTE = 80
    const val MAX_LABEL = 24
    const val MAX_OPTIONS = 40

    /** Cleans a new mood's name; null if it's empty. */
    fun cleanLabel(input: String): String? = input.replace(Regex("\\s+"), " ").trim().take(MAX_LABEL).ifEmpty { null }

    /** A fresh key for a custom mood, e.g. "c_k3v9q2xa". */
    fun newKey(random: kotlin.random.Random = kotlin.random.Random): String =
        "c_" + (1..8).map { "abcdefghjkmnpqrstuvwxyz23456789"[random.nextInt(31)] }.joinToString("")

    /** Trims and flattens the note to one line; blank becomes null. Longer notes are cut. */
    fun cleanNote(input: String?): String? =
        input?.replace(Regex("\\s+"), " ")?.trim()?.take(MAX_NOTE)?.ifEmpty { null }
}
