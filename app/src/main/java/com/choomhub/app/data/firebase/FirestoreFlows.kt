package com.choomhub.app.data.firebase

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Live document as a Flow. Emits the cached copy first (works offline), then
 * server updates. Permission errors (e.g. you were never a member) emit null
 * instead of crashing; nothing about the document contents is logged.
 */
internal fun DocumentReference.observe(): Flow<DocumentSnapshot?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.w("ChoomHub", "Listen failed for ${path.substringBefore('/')}: ${error.code}")
            trySend(null)
        } else {
            trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}
