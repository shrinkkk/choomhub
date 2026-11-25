package com.choomhub.app.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow

/** A screen a notification asked to open; the main scaffold navigates there and clears it. */
object PendingOpen {
    const val FOCUS_ROOM = "focus_room"
    const val HOME = "home"

    val request = MutableStateFlow<String?>(null)
}
