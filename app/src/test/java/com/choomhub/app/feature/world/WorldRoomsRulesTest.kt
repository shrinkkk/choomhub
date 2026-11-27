package com.choomhub.app.feature.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.choomhub.app.domain.model.MeetupLog
import com.choomhub.app.domain.model.MeetupPlace
import com.choomhub.app.domain.model.MeetupRules
import com.choomhub.app.domain.model.ShelfSong
import com.choomhub.app.feature.world.atlas.MapWindow
import com.choomhub.app.feature.world.atlas.atlasPins
import com.choomhub.app.feature.world.atlas.projectPoint
import com.choomhub.app.feature.world.atlas.unproject
import com.choomhub.app.feature.world.constellation.starAt
import com.choomhub.app.feature.world.constellation.starPoints
import com.choomhub.app.feature.world.constellation.starPositions
import com.choomhub.app.feature.world.constellation.starsOf
import com.choomhub.app.feature.world.shelf.ShelfFilter
import com.choomhub.app.feature.world.shelf.shownFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WorldRoomsRulesTest {
    private val now = Instant.parse("2026-11-27T18:00:00Z")
    private fun log(id: String, day: Int, minutes: Int? = 120, place: String? = null, lat: Double? = null, lon: Double? = null, live: Boolean = false, startedAt: Instant? = null) =
        MeetupLog(
            id, LocalDate.of(2026, 11, day), minutes = minutes, live = live, startedAt = startedAt,
            places = if (place != null || lat != null) listOf(MeetupPlace(place.orEmpty(), lat, lon)) else emptyList(), createdAt = now,
        )

    @Test
    fun photosMoveWithinTheRowAndTheFrontOneIsTheThumbnail() {
        val order = listOf("a", "b", "c", "d")
        assertEquals(listOf("a", "c", "b", "d"), MeetupRules.move(order, "c", -1))
        assertEquals(listOf("a", "b", "d", "c"), MeetupRules.move(order, "c", 1))
        assertEquals(listOf("d", "a", "b", "c"), MeetupRules.move(order, "d", -MeetupRules.MAX_PHOTOS))
        assertEquals(order, MeetupRules.move(order, "d", 5))
        assertEquals(order, MeetupRules.move(order, "zz", 1))
    }

    @Test
    fun durationsReadLikeAClock() {
        assertEquals("45m", MeetupRules.duration(45))
        assertEquals("6h", MeetupRules.duration(360))
        assertEquals("5h 40m", MeetupRules.duration(340))
    }

    @Test
    fun aLiveMeetupKeepsCountingUntilItsStopped() {
        val live = log("l", 27, minutes = null, live = true, startedAt = now.minusSeconds(90 * 60))
        assertEquals(90, live.minutesAt(now))
        assertTrue(cabinetLine(listOf(live, log("a", 3)), now).startsWith("● Logging now · 1h 30m · 2 kept"))
        assertEquals("1 kept", cabinetLine(listOf(log("a", 3)), now))
    }

    @Test
    fun starsGoOldestFirst() {
        val (ordered, stars) = starsOf(listOf(log("c", 20), log("a", 2), log("b", 9)), now)
        assertEquals(listOf("a", "b", "c"), ordered.map { it.id })
        assertEquals(listOf("a", "b", "c"), stars.map { it.id })
    }

    @Test
    fun aTapFindsTheNearestStarWithinReachAtAnyZoom() {
        val size = Size(1000f, 2000f)
        val count = 14
        for (zoom in listOf(1f, 2.5f, 4f)) {
            val pan = Offset(-120f, 340f)
            val points = starPoints(count, size, zoom, pan)
            points.forEachIndexed { i, p -> assertEquals(i, starAt(p + Offset(3f, -2f), count, size, zoom, pan, 40f)) }
        }
        assertNull(starAt(Offset(-500f, -500f), count, size, 1f, Offset.Zero, 40f))
        // Every star has its own spot in the sky.
        val spots = starPositions(40)
        assertEquals(40, spots.map { (it.x * 100).toInt() to (it.y * 100).toInt() }.toSet().size)
        assertTrue(spots.all { it.x in 0f..1f && it.y in 0f..1f })
    }

    @Test
    fun meetupsAtTheSamePlaceShareOnePin() {
        val logs = listOf(
            log("a", 1, place = "Lodhi Garden", lat = 28.5931, lon = 77.2197),
            log("b", 8, place = "Lodhi Garden", lat = 28.5940, lon = 77.2205),
            log("c", 15, place = "Hauz Khas", lat = 28.5494, lon = 77.2001),
            log("d", 22, place = "Somewhere unpinned"),
        )
        val pins = atlasPins(logs)
        assertEquals(2, pins.size)
        val lodhi = pins.first { it.second.size == 2 }
        assertEquals(listOf("b", "a"), lodhi.second.map { it.id })
        assertEquals("2 places in Delhi · Lodhi Garden ×2", atlasLine(logs))
        assertEquals("Pin a date to a place in the archive", atlasLine(emptyList()))
    }

    @Test
    fun theMapProjectsAndUnprojectsTheSamePoint() {
        val size = Size(800f, 1000f)
        val win = MapWindow.Delhi
        val at = projectPoint(win, size, 77.2197f, 28.5931f)
        val (lon, lat) = unproject(win, size, at)
        assertEquals(77.2197f, lon, 0.0005f)
        assertEquals(28.5931f, lat, 0.0005f)
    }

    @Test
    fun theShelfFiltersBySender() {
        val songs = listOf(
            ShelfSong("1", "t1", "Song one", "A", null, true, now.minusSeconds(600), 8),
            ShelfSong("2", "t2", "Song two", "B", null, false, now, null),
            ShelfSong("3", "t3", "Song three", "C", null, true, now.minusSeconds(60), 6),
        )
        assertEquals(3, songs.shownFor(ShelfFilter.All).size)
        assertEquals(listOf("1", "3"), songs.shownFor(ShelfFilter.Mine).map { it.key })
        assertEquals(listOf("2"), songs.shownFor(ShelfFilter.Theirs).map { it.key })
        assertEquals("3 records · last from Wren", shelfLine(songs, "you", "Wren"))
        assertEquals("Every song you send lands here", shelfLine(emptyList(), "you", "Wren"))
    }

    @Test
    fun everyPlaceOfADateGetsAPinAndADateCountsOncePerPin() {
        val day = MeetupLog(
            "x", LocalDate.of(2026, 11, 2),
            places = listOf(
                MeetupPlace("Lodhi Garden", 28.5931, 77.2197), MeetupPlace("Khan Market", 28.6003, 77.2270),
                MeetupPlace("Lodhi Garden again", 28.5933, 77.2199), MeetupPlace("unpinned"),
            ),
            createdAt = now,
        )
        val other = log("y", 5, place = "Lodhi Garden", lat = 28.5935, lon = 77.2201)
        val pins = atlasPins(listOf(day, other))
        assertEquals(2, pins.size)
        val lodhi = pins.first { it.first.name == "Lodhi Garden" }
        assertEquals(listOf("y", "x"), lodhi.second.map { it.id })
        assertEquals(2, lodhi.first.count)
        // The preview shows the first place only.
        assertEquals("Lodhi Garden", day.place)
        assertEquals(3, day.pins.size)
    }

    @Test
    fun placesAreTidiedBeforeSaving() {
        val cleaned = MeetupRules.cleanPlaces(listOf(MeetupPlace("  Lodhi   Garden "), MeetupPlace(""), MeetupPlace("", 28.6, 77.2)) + List(8) { MeetupPlace("p$it") })
        assertEquals("Lodhi Garden", cleaned[0].name)
        assertEquals(MeetupPlace("", 28.6, 77.2), cleaned[1])
        assertEquals(MeetupRules.MAX_PLACES, cleaned.size)
    }

    @Test
    fun theSameSpotFoundTwiceIsOneChoice() {
        val found = listOf(
            com.choomhub.app.data.place.FoundPlace(28.5931, 77.2197, "Social", "Hauz Khas Village"),
            com.choomhub.app.data.place.FoundPlace(28.5935, 77.2200, "Social", "Hauz Khas Village, again"),
            com.choomhub.app.data.place.FoundPlace(28.6315, 77.2167, "Social", "Connaught Place"),
        )
        assertEquals(listOf("Hauz Khas Village", "Connaught Place"), com.choomhub.app.data.place.PlaceRules.distinct(found).map { it.address })
    }
}
