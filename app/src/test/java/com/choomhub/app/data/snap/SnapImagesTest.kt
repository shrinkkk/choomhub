package com.choomhub.app.data.snap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.domain.model.SnapRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SnapImagesTest {

    /** Random noise is the worst case for JPEG size. */
    private fun noise(w: Int, h: Int): Bitmap {
        val random = Random(1)
        val pixels = IntArray(w * h) { 0xFF000000.toInt() or random.nextInt(0xFFFFFF) }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    @Test
    fun `a large noisy photo is shrunk under the limits`() {
        val prepared = SnapImages.prepare(noise(4000, 3000))
        assertNotNull(prepared)
        prepared!!
        assertEquals(SnapRules.FULL_EDGE, maxOf(prepared.width, prepared.height))
        assertTrue("full ${prepared.full.size}", prepared.full.size <= SnapRules.MAX_FULL_BYTES)
        assertTrue("thumb ${prepared.thumb.size}", prepared.thumb.size <= SnapRules.MAX_THUMB_BYTES)
        val thumb = BitmapFactory.decodeByteArray(prepared.thumb, 0, prepared.thumb.size)
        assertEquals(SnapRules.THUMB_EDGE, maxOf(thumb.width, thumb.height))
    }

    @Test
    fun `a small photo keeps its size and shape`() {
        val prepared = SnapImages.prepare(noise(600, 800))!!
        assertEquals(600, prepared.width)
        assertEquals(800, prepared.height)
    }

    @Test
    fun `captions and collection names are cleaned`() {
        assertEquals("crows again", SnapRules.cleanCaption("  crows \n again "))
        assertEquals(null, SnapRules.cleanCaption("   "))
        assertEquals(SnapRules.MAX_CAPTION, SnapRules.cleanCaption("x".repeat(500))!!.length)
        assertEquals(null, SnapRules.cleanCollectionName(" "))
    }
}
