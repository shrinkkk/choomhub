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
    fun `songs, albums and playlists can be sent, nothing else`() {
        assertNull(SongRules.refusal("track"))
        assertNull(SongRules.refusal("playlist"))
        assertNull(SongRules.refusal("album"))
        assertTrue(SongRules.refusal("artist")!!.contains("artist"))
        assertTrue(SongRules.refusal("episode")!!.contains("podcast"))
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

    @Test
    fun `albums and playlists open as themselves`() {
        assertEquals("https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC", SongRules.playUrl("4uLU6hMCjMI75M1A2tKUQC"))
        assertEquals("https://open.spotify.com/album/4aawyAB9vmqN3uQ7FjRGTy", SongRules.playUrl("4aawyAB9vmqN3uQ7FjRGTy", SongKind.Album))
        assertEquals("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M", SongRules.playUrl("37i9dQZF1DXcBWIGoYBM5M", SongKind.Playlist))
        assertEquals(SongKind.Playlist, SongKind.fromWire("playlist"))
        assertEquals(SongKind.Track, SongKind.fromWire(null))
        assertEquals("an album", SongKind.Album.withArticle)
    }

    @Test
    fun `who made it comes from the embed page`() {
        // Trimmed from Spotify's embed page: the entity first, then its tracks (with their own subtitles).
        val album = """{"type":"album","name":"Global Warming","uri":"spotify:album:4aawyAB9vmqN3uQ7FjRGTy","id":"4aawyAB9vmqN3uQ7FjRGTy","title":"Global Warming","subtitle":"Pitbull","isPreRelease":false},{"subtitle":"Pitbull, Sensato"}"""
        assertEquals("Pitbull", SongRules.makerFromEmbed(album, SongKind.Album, "4aawyAB9vmqN3uQ7FjRGTy"))
        val playlist = """"uri":"spotify:playlist:37i9dQZF1DXcBWIGoYBM5M","id":"37i9dQZF1DXcBWIGoYBM5M","title":"Today\u2019s \"Top\" Hits","subtitle":"Jas \u0026 Shri","authors":null"""
        assertEquals("Jas & Shri", SongRules.makerFromEmbed(playlist, SongKind.Playlist, "37i9dQZF1DXcBWIGoYBM5M"))
        assertNull(SongRules.makerFromEmbed(playlist, SongKind.Album, "37i9dQZF1DXcBWIGoYBM5M"))
        assertNull(SongRules.makerFromEmbed("<html></html>", SongKind.Playlist, "37i9dQZF1DXcBWIGoYBM5M"))
    }
}
