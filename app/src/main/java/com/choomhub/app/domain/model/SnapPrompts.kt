package com.choomhub.app.domain.model

import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

/**
 * A small daily nudge to send a snap. Both phones pick the same prompt and the
 * same time for a given day (seeded by the date), with no server involved.
 */
object SnapPrompts {
    val all = listOf(
        "What's in front of you right now",
        "The sky, wherever you are",
        "Your shoes, as they are",
        "Something black",
        "The last thing you ate",
        "Your view from bed",
        "A shadow",
        "Something you'd steal from my room",
        "The nearest window",
        "Whatever you're drinking",
        "Something that looks like a crow",
        "Your hand, doing whatever it's doing",
        "The messiest corner around you",
        "Something old",
        "A sign or a poster near you",
        "What you're wearing today, no fixing it first",
        "Something red",
        "The most boring thing in the room",
        "Your ceiling",
        "A plant, any plant",
        "What your walk looks like",
        "Something you're avoiding doing",
        "A light you like",
        "Your desk, untouched",
        "Something that made you think of me",
        "Your current face, no retakes",
        "The weather",
        "Something tiny",
        "Your favourite thing in this room",
        "Whatever's playing, on the screen",
    )

    private val EARLIEST = LocalTime.of(12, 0)
    private const val WINDOW_MINUTES = 8 * 60

    private fun random(date: LocalDate) = Random(date.toEpochDay() * 7919 + 17)

    fun forDate(date: LocalDate): String = all[random(date).nextInt(all.size)]

    /** Some time between 12:00 and 20:00, different every day. */
    fun timeFor(date: LocalDate): LocalTime {
        val r = random(date)
        r.nextInt() // the first draw picks the prompt
        return EARLIEST.plusMinutes(r.nextInt(WINDOW_MINUTES).toLong())
    }
}
