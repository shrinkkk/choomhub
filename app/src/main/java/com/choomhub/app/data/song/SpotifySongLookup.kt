package com.choomhub.app.data.song

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import com.choomhub.app.domain.model.SongDraft
import com.choomhub.app.domain.model.SongRules
import com.choomhub.app.domain.repository.SongLookup
import com.choomhub.app.domain.repository.SongLookupException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

/**
 * Looks a link up with Spotify's public oEmbed endpoint (no account or key needed): the title,
 * the cover, and (from its embed address) whether it's a song and its id. Short spotify.link
 * links work too. The artist comes from the song page's preview text, if it's there.
 */
class SpotifySongLookup @Inject constructor() : SongLookup {

    override suspend fun lookup(link: String): Result<SongDraft> = withContext(Dispatchers.IO) {
        runCatching {
            val url = SongRules.findLink(link) ?: throw SongLookupException("That isn't a Spotify song link.")
            // A link we can read straight away: say so before going online.
            SongRules.parse(url)?.let { (kind, _) -> SongRules.refusal(kind)?.let { throw SongLookupException(it) } }
            val web = if (url.startsWith("spotify:")) {
                val (kind, id) = SongRules.parse(url)!!
                "https://open.spotify.com/$kind/$id"
            } else {
                url
            }
            val json = try {
                JSONObject(get("https://open.spotify.com/oembed?url=" + URLEncoder.encode(web, "UTF-8")))
            } catch (e: IOException) {
                throw SongLookupException("Couldn't reach Spotify. Check your connection.")
            }
            val (kind, id) = SongRules.parse(json.optString("iframe_url"))
                ?: throw SongLookupException("Spotify didn't recognise that link.")
            SongRules.refusal(kind)?.let { throw SongLookupException(it) }
            if (!SongRules.isTrackId(id)) throw SongLookupException("Spotify didn't recognise that link.")
            val title = json.optString("title").trim().take(SongRules.MAX_TITLE).ifEmpty { "A song" }
            val cover = json.optString("thumbnail_url").takeIf { it.startsWith("https://") }?.let { runCatching { coverJpeg(it) }.getOrNull() }
            val artist = runCatching { artist(id) }.getOrNull()
            SongDraft(id, title, artist, cover)
        }
    }

    private fun artist(trackId: String): String? {
        val page = get(SongRules.playUrl(trackId), maxBytes = 400_000)
        val description = Regex("""<meta property="og:description" content="([^"]*)"""").find(page)?.groupValues?.get(1)
        return SongRules.artistFromDescription(description?.let(::unescape))
    }

    private fun unescape(s: String) = s.replace("&amp;", "&").replace("&#x27;", "'").replace("&#39;", "'").replace("&quot;", "\"")

    /** The cover, shrunk to a small JPEG so it can travel with the song. */
    private fun coverJpeg(url: String): ByteArray? {
        val bytes = getBytes(url, maxBytes = 2_000_000)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val small = if (bitmap.width > COVER_PX) bitmap.scale(COVER_PX, COVER_PX * bitmap.height / bitmap.width) else bitmap
        return ByteArrayOutputStream().use { out ->
            small.compress(Bitmap.CompressFormat.JPEG, 82, out)
            out.toByteArray()
        }.takeIf { it.size <= MAX_COVER_BYTES }
    }

    private fun get(url: String, maxBytes: Int = 200_000): String = String(getBytes(url, maxBytes))

    private fun getBytes(url: String, maxBytes: Int): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) ChoomHub")
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            return connection.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(16_384)
                while (out.size() < maxBytes) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val COVER_PX = 240
        const val MAX_COVER_BYTES = 60_000
    }
}
