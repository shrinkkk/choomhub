package com.choomhub.app.data.push

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choomhub.app.core.firebase.FirebaseConfig
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.TestPushResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Robolectric only for org.json; no network is touched. */
@RunWith(AndroidJUnit4::class)
class PushApiTest {

    @Test
    fun `request body is valid JSON with escaped detail`() {
        assertEquals("""{"event":"meetup_cleared"}""", PushApi.requestBody(PushEvent.MeetupCleared, null))
        assertEquals("""{"event":"meetup_set","detail":"Sat \"14\" Dec\n\\ 7:30"}""", PushApi.requestBody(PushEvent.MeetupSet, "Sat \"14\" Dec\n\\ 7:30"))
        assertEquals(PushApi.MAX_DETAIL, PushApi.requestBody(PushEvent.MeetupSet, "x".repeat(500)).count { it == 'x' })
        assertEquals("\"\\u0001\"", PushApi.quote("\u0001"))
        assertEquals("""{"event":"gesture","kind":"hug"}""", PushApi.requestBody(PushEvent.Gesture, null, "hug"))
        assertEquals("""{"event":"gesture","kind":"hug","id":"3f1c2b9e-7a4d"}""", PushApi.requestBody(PushEvent.Gesture, null, "hug", "3f1c2b9e-7a4d"))
    }

    @Test
    fun `worker responses are classified`() {
        assertEquals(PushResponse.Delivered(2), PushApi.parse(200, """{"sent":2,"failed":0}"""))
        assertEquals(PushResponse.NotDelivered("muted"), PushApi.parse(200, """{"sent":0,"reason":"muted"}"""))
        assertEquals(PushResponse.NotDelivered("unknown"), PushApi.parse(200, "garbage"))
        assertEquals(PushResponse.Unauthorized, PushApi.parse(401, """{"error":"unauthenticated"}"""))
        assertEquals(PushResponse.Rejected(403, "not-paired"), PushApi.parse(403, """{"error":"not-paired"}"""))
        assertEquals(PushResponse.Rejected(400, null), PushApi.parse(400, null))
        assertTrue(PushApi.parse(500, """{"error":"internal"}""") is PushResponse.Unavailable)
        assertTrue(PushApi.parse(429, "") is PushResponse.Unavailable)
    }

    @Test
    fun `queued sends retry only on temporary trouble, and give up eventually`() {
        assertEquals(PushWorker.Outcome.Done, PushWorker.outcome(PushResponse.Delivered(1), 0))
        assertEquals(PushWorker.Outcome.Done, PushWorker.outcome(PushResponse.NotDelivered("muted"), 0))
        assertEquals(PushWorker.Outcome.GiveUp, PushWorker.outcome(PushResponse.Rejected(403, "not-paired"), 0))
        assertEquals(PushWorker.Outcome.GiveUp, PushWorker.outcome(PushResponse.Unauthorized, 0))
        assertEquals(PushWorker.Outcome.Retry, PushWorker.outcome(PushResponse.Unavailable("network"), 0))
        assertEquals(PushWorker.Outcome.GiveUp, PushWorker.outcome(PushResponse.Unavailable("network"), PushWorker.MAX_ATTEMPTS - 1))
    }

    @Test
    fun `a refused ID token is refreshed once`() = runTest {
        val seen = mutableListOf<Boolean>()
        val tokens = IdTokenSource { force -> seen += force; if (force) "fresh" else "stale" }
        val delivery = PushDelivery(FakeApi(mapOf("stale" to PushResponse.Unauthorized, "fresh" to PushResponse.Delivered(1))), tokens)
        assertEquals(PushResponse.Delivered(1), delivery.deliver(PushEvent.Test, null, null))
        assertEquals(listOf(false, true), seen)

        val signedOut = PushDelivery(FakeApi(emptyMap()), { null })
        assertEquals(PushResponse.Rejected(401, "signed-out"), signedOut.deliver(PushEvent.Test, null, null))
    }

    @Test
    fun `test results become plain messages`() {
        assertEquals(TestPushResult.Sent, WorkManagerNotifier.testResultFor(PushResponse.Delivered(1)))
        val noDevice = WorkManagerNotifier.testResultFor(PushResponse.NotDelivered("no-devices")) as TestPushResult.NotSent
        assertTrue(noDevice.message.startsWith("This phone isn't registered yet"))
    }

    @Test
    fun `endpoint and sender id come from local config`() {
        assertEquals("https://choomhub-push.me.workers.dev/v1/notify", FirebaseConfig.notifyEndpoint("https://choomhub-push.me.workers.dev/ "))
        assertNull(FirebaseConfig.notifyEndpoint("http://insecure.example"))
        assertNull(FirebaseConfig.notifyEndpoint(""))
        assertEquals("965020805907", FirebaseConfig.senderIdFrom("1:965020805907:android:abc123"))
        assertNull(FirebaseConfig.senderIdFrom("nonsense"))
    }
}

/** Answers by token, so the test can see which token was used. */
private class FakeApi(private val byToken: Map<String, PushResponse>) : PushApi(PushEndpoint("https://example.test/v1/notify")) {
    override suspend fun notify(idToken: String, event: PushEvent, detail: String?, kind: String?, id: String?): PushResponse =
        byToken[idToken] ?: PushResponse.Rejected(0, "unexpected token")
}
