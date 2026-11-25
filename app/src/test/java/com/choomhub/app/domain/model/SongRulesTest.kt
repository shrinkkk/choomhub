package com.choomhub.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongRulesTest {
    @Test
    fun `finds the link in what Spotify shares`() {
        assertEquals(
            "https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC?si=abc123",
            SongRules.findLink("Listen to this on Spotify: https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC?si=abc123"),
        )
        assertEquals("https://spotify.link/AbCdE", SongRules.findLink("https://spotify.link/AbCdE"))
        assertEquals("spotify:track:4uLU6hMCjMI75M1A2tKUQC", SongRules.findLink("spotify:track:4uLU6hMCjMI75M1A2tKUQC"))
        assertNull(SongRules.findLink("https://youtube.com/watch?v=x"))
    }

    @Test
    fun `reads what a link points at`() {
        assertEquals("track" to "4uLU6hMCjMI75M1A2tKUQC", SongRules.parse("https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC?si=x"))
        assertEquals("track" to "4uLU6hMCjMI75M1A2tKUQC", SongRules.parse("https://open.spotify.com/intl-de/track/4uLU6hMCjMI75M1A2tKUQC"))
        assertEquals("track" to "4uLU6hMCjMI75M1A2tKUQC", SongRules.parse("https://open.spotify.com/embed/track/4uLU6hMCjMI75M1A2tKUQC?utm_source=oembed"))
        assertEquals("playlist" to "37i9dQZF1DXcBWIGoYBM5M", SongRules.parse("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"))
        assertNull(SongRules.parse("https://spotify.link/AbCdE"))
    }

    @Test
    fun `only songs can be sent`() {
        assertNull(SongRules.refusal("track"))
        assertTrue(SongRules.refusal("playlist")!!.contains("playlist"))
        assertTrue(SongRules.refusal("album")!!.contains("album"))
        assertTrue(SongRules.isTrackId("4uLU6hMCjMI75M1A2tKUQC"))
        assertTrue(!SongRules.isTrackId("short"))
    }

    @Test
    fun `the artist comes from the preview text`() {
        assertEquals("Rick Astley", SongRules.artistFromDescription("Rick Astley · Whenever You Need Somebody · Song · 1987"))
        assertNull(SongRules.artistFromDescription(null))
        assertEquals("Velvet Hours · The Paper Moths", SongRules.describe("Velvet Hours", "The Paper Moths"))
        assertEquals("Velvet Hours", SongRules.describe("Velvet Hours", null))
    }
}
