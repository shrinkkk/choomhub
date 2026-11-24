package com.choomhub.app.data.firebase

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

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

/** The signed-in Firebase user's uid, or null when signed out. */
internal fun firebaseUid(): Flow<String?> = callbackFlow {
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
}.distinctUntilChanged()

/** Live query results as a Flow; cached results first, like [observe]. Errors emit null. */
internal fun com.google.firebase.firestore.Query.observe(): Flow<com.google.firebase.firestore.QuerySnapshot?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.w("ChoomHub", "Query listen failed: ${error.code}")
            trySend(null)
        } else {
            trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}
