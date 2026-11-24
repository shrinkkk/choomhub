package com.choomhub.app.data.push

import com.choomhub.app.domain.model.PushEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/** The Worker's notify endpoint, or null when pushes aren't configured. */
data class PushEndpoint(val url: String?)

/** What the push Worker said. */
sealed interface PushResponse {
    /** At least one of the recipient's phones got it. */
    data class Delivered(val sent: Int) : PushResponse

    /** Accepted but not sent: muted, no registered phone, or every phone refused it. */
    data class NotDelivered(val reason: String) : PushResponse

    /** The ID token was refused; a fresh one may work. */
    data object Unauthorized : PushResponse

    /** The request itself is wrong (e.g. not paired yet). Retrying won't help. */
    data class Rejected(val status: Int, val error: String?) : PushResponse

    /** Network trouble or the Worker/Google is having a bad moment. Worth retrying. */
    data class Unavailable(val detail: String) : PushResponse
}

/** Small HTTP client for push-worker/. No JSON library needed for one tiny body. */
open class PushApi @Inject constructor(private val endpoint: PushEndpoint) {

    open suspend fun notify(idToken: String, event: PushEvent, detail: String?, kind: String? = null): PushResponse = withContext(Dispatchers.IO) {
        val url = endpoint.url ?: return@withContext PushResponse.Rejected(0, "not-configured")
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return@withContext PushResponse.Unavailable("bad endpoint")
        }
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $idToken")
            connection.outputStream.use { it.write(requestBody(event, detail, kind).toByteArray()) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { s -> s.bufferedReader().readText().take(MAX_RESPONSE_CHARS) }
            parse(status, body)
        } catch (e: IOException) {
            PushResponse.Unavailable("network")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TIMEOUT_MS = 15_000
        private const val MAX_RESPONSE_CHARS = 4_096
        const val MAX_DETAIL = 80

        fun requestBody(event: PushEvent, detail: String?, kind: String? = null): String = buildString {
            append("{\"event\":").append(quote(event.wire))
            if (!kind.isNullOrBlank()) append(",\"kind\":").append(quote(kind))
            if (!detail.isNullOrBlank()) append(",\"detail\":").append(quote(detail.take(MAX_DETAIL)))
            append('}')
        }

        fun parse(status: Int, body: String?): PushResponse {
            val json = try {
                body?.let(::JSONObject)
            } catch (e: JSONException) {
                null
            }
            return when {
                status == 200 -> {
                    val sent = json?.optInt("sent", 0) ?: 0
                    if (sent > 0) PushResponse.Delivered(sent) else PushResponse.NotDelivered(json?.optString("reason").orEmpty().ifEmpty { "unknown" })
                }
                status == 401 -> PushResponse.Unauthorized
                status == 429 || status >= 500 -> PushResponse.Unavailable("http $status")
                else -> PushResponse.Rejected(status, json?.optString("error")?.ifEmpty { null })
            }
        }

        /** JSON string literal with the escapes JSON requires. */
        fun quote(text: String): String = buildString {
            append('"')
            for (c in text) {
                when {
                    c == '"' -> append("\\\"")
                    c == '\\' -> append("\\\\")
                    c == '\n' -> append("\\n")
                    c == '\r' -> append("\\r")
                    c == '\t' -> append("\\t")
                    c < ' ' -> append(String.format("\\u%04x", c.code))
                    else -> append(c)
                }
            }
            append('"')
        }
    }
}
