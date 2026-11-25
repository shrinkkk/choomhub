package com.choomhub.app.data.song

import kotlinx.coroutines.flow.MutableStateFlow

/** A link shared into the app from Spotify (Share → ChoomHub), waiting for Home to pick it up. */
object SongInbox {
    val link = MutableStateFlow<String?>(null)
}
