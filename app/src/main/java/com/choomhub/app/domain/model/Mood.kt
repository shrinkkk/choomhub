package com.choomhub.app.domain.model

/**
 * The preset moods a person can choose. Kept free of UI concerns; the
 * presentation layer decides colours and artwork for each.
 * Custom free-text descriptions travel alongside as [PartnerMood.note].
 */
enum class Mood(val label: String) {
    Cozy("Cozy"),
    Sleepy("Sleepy"),
    Content("Content"),
    Calm("Calm"),
    Restless("Restless"),
    MissingYou("Missing you"),
    Tired("Tired"),
    Low("Low"),
    Stressed("Stressed"),
    Lonely("Lonely"),
    Wired("Wired"),
}
