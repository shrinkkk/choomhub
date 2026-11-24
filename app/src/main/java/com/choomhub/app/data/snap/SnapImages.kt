package com.choomhub.app.data.snap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.choomhub.app.domain.model.PreparedSnap
import com.choomhub.app.domain.model.SnapRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Turns a picked or captured photo into what's stored: a JPEG at most
 * [SnapRules.FULL_EDGE] px on its longest side and under [SnapRules.MAX_FULL_BYTES],
 * plus a small thumbnail. Upright (camera rotation applied), metadata dropped.
 */
class SnapImages @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun prepare(uri: Uri): PreparedSnap? = withContext(Dispatchers.Default) {
        val bitmap = runCatching { decodeUpright(uri, SnapRules.FULL_EDGE) }.getOrNull() ?: return@withContext null
        try {
            prepare(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /** Small preview for the send screen. */
    suspend fun preview(uri: Uri): Bitmap? = withContext(Dispatchers.Default) {
        runCatching { decodeUpright(uri, 900) }.getOrNull()
    }

    private fun decodeUpright(uri: Uri, maxEdge: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            // ImageDecoder applies the camera's rotation itself.
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (w, h) = info.size.width to info.size.height
                val scale = minOf(1f, maxEdge.toFloat() / max(w, h))
                decoder.setTargetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = context.contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val upright = if (rotation == 0f) decoded else
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true).also { decoded.recycle() }
        return scaleDown(upright, maxEdge)
    }

    companion object {
        /** Pure JPEG work, so it can be unit-tested with a generated bitmap. */
        fun prepare(bitmap: Bitmap): PreparedSnap? {
            val full = scaleDown(bitmap, SnapRules.FULL_EDGE)
            val fullBytes = compressUnder(full, SnapRules.MAX_FULL_BYTES, startQuality = 82) ?: return null
            val thumb = scaleDown(full, SnapRules.THUMB_EDGE)
            val thumbBytes = compressUnder(thumb, SnapRules.MAX_THUMB_BYTES, startQuality = 72) ?: return null
            val result = PreparedSnap(fullBytes, thumbBytes, full.width, full.height)
            if (thumb !== full && thumb !== bitmap) thumb.recycle()
            if (full !== bitmap) full.recycle()
            return result
        }

        fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
            val edge = max(bitmap.width, bitmap.height)
            if (edge <= maxEdge) return bitmap
            val scale = maxEdge.toFloat() / edge
            return bitmap.scale((bitmap.width * scale).roundToInt().coerceAtLeast(1), (bitmap.height * scale).roundToInt().coerceAtLeast(1))
        }

        /** Lowers JPEG quality (then size) until it fits [limit] bytes. */
        fun compressUnder(bitmap: Bitmap, limit: Int, startQuality: Int): ByteArray? {
            var current = bitmap
            repeat(3) { round ->
                var quality = startQuality
                while (quality >= 40) {
                    val out = ByteArrayOutputStream()
                    current.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    if (out.size() <= limit) return out.toByteArray()
                    quality -= 8
                }
                if (round < 2) current = scaleDown(current, (max(current.width, current.height) * 0.75f).roundToInt())
            }
            return null
        }
    }
}
