package com.choomhub.app.data.push

import com.choomhub.app.domain.model.PushEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** The signed-in person's Firebase ID token, which the push Worker checks. */
fun interface IdTokenSource {
    suspend fun idToken(forceRefresh: Boolean): String?
}

class FirebaseIdTokenSource @Inject constructor() : IdTokenSource {
    override suspend fun idToken(forceRefresh: Boolean): String? =
        FirebaseAuth.getInstance().currentUser?.getIdToken(forceRefresh)?.await()?.token
}

/** Calls the Worker as the signed-in person, refreshing the ID token once if it was refused. */
class PushDelivery @Inject constructor(
    private val api: PushApi,
    private val tokens: IdTokenSource,
) {
    suspend fun deliver(event: PushEvent, detail: String?, kind: String?): PushResponse {
        val token = runCatching { tokens.idToken(forceRefresh = false) }.getOrNull()
            ?: return PushResponse.Rejected(401, "signed-out")
        val first = api.notify(token, event, detail, kind)
        if (first != PushResponse.Unauthorized) return first
        val fresh = runCatching { tokens.idToken(forceRefresh = true) }.getOrNull() ?: return first
        return api.notify(fresh, event, detail, kind)
    }
}
