package com.choomhub.app.domain.model

/**
 * Kinds of notification you can turn off. [key] is the field in
 * users/{uid}/settings/notifications and [channelId] the Android channel; both
 * must match push-worker/src/events.js.
 */
enum class NotificationCategory(val key: String, val channelId: String, val label: String, val description: String) {
    Gestures("gestures", "gestures", "Gestures", "Hugs, forehead kisses, head pats and tomatoes"),
    Snaps("snaps", "snaps", "Snaps", "When they send a snap or react to yours"),
    Points("points", "points", "Points", "When they award you points or take some away"),
    Moods("moods", "moods", "Moods", "When they change their mood"),
    Meetup("meetup", "meetup", "Meetup", "When the next meetup is set, moved or cleared"),
    Space("space", "space", "Space", "When they join, or change what they call you"),
    Focus("focus", "focus", "Focus room", "When they enter the focus room, nudge you, or leave a jam"),
    Pet("pet", "pet", "Pet House", "When they need your help with the cat, and when it needs something"),
    Songs("songs", "songs", "Songs", "When they send you a song or rate yours"),
}

/** Your notification choices. Everything is on until you turn it off. */
data class NotificationPrefs(val disabled: Set<NotificationCategory> = emptySet()) {
    fun isEnabled(category: NotificationCategory): Boolean = category !in disabled
}

/**
 * Things the app tells the push Worker about. [wire] is the event name the
 * Worker accepts; events with the same [collapseKey] replace each other while
 * still queued, so only the latest meetup change is sent.
 */
enum class PushEvent(val wire: String, val collapseKey: String) {
    MeetupSet("meetup_set", "meetup"),
    MeetupCleared("meetup_cleared", "meetup"),
    PartnerJoined("partner_joined", "joined"),
    Renamed("renamed", "renamed"),
    Gesture("gesture", "gesture"),
    MoodChanged("mood", "mood"),
    SnapSent("snap", "snap"),
    SnapReaction("snap_reaction", "snap_reaction"),
    PointsAwarded("points", "points"),
    FocusEntered("focus_enter", "focus_enter"),
    FocusNudge("focus_nudge", "focus_nudge"),
    JamLeft("jam", "jam"),
    PetHelp("pet_help", "pet_help"),
    PetNamed("pet_named", "pet_named"),
    Shop("shop", "shop"),
    SongSent("song", "song"),
    SongRated("song_rated", "song_rated"),
    Test("test", "test"),
    ;

    companion object {
        fun fromWire(wire: String?): PushEvent? = entries.firstOrNull { it.wire == wire }
    }
}

/** Whether pushes can work in this build. */
enum class PushAvailability {
    /** No Firebase config: nothing leaves the phone, so there is no one to notify. */
    DeviceOnly,

    /** Firebase is set up but the push Worker URL isn't (see docs/NOTIFICATIONS_SETUP.md). */
    NotSetUp,
    Ready,
}

/** What happened to the test notification. */
sealed interface TestPushResult {
    data object Sent : TestPushResult
    data class NotSent(val message: String) : TestPushResult
}
