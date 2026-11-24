package com.afterglow.app.domain.model

/**
 * The preset moods a person can choose. Kept free of UI concerns; the
 * presentation layer decides colours and artwork for each.
 * Custom free-text descriptions travel alongside as [PartnerMood.note].
 */
enum class Mood(val label: String) {
    Cozy("Cozy"),
    Sleepy("Sleepy & soft"),
    Happy("Happy"),
    MissingYou("Missing you"),
    Loved("Feeling loved"),
    Calm("Calm"),
    Excited("Excited"),
    Tired("Tired"),
    Sad("A bit sad"),
    Stressed("Stressed"),
    Lonely("Lonely"),
}
