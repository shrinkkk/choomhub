package com.choomhub.app.data.push

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.choomhub.app.data.firebase.firebaseUid
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps users/{uid}/devices/{fid} pointing at this phone while you're signed in.
 *
 * FCM identifies the phone by its Firebase Installation ID (FID). FCM reports
 * it through [onRegistered] once the phone is registered; it's kept in
 * DataStore and uploaded for whoever is signed in. The push Worker reads it
 * with its own credentials and removes it if FCM says it's gone.
 */
@Singleton
class PushRegistrar @Inject constructor(
    private val store: DataStore<Preferences>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /** Starts keeping the registration up to date. Call once, when Firebase is configured. */
    fun start() {
        // Touching FirebaseMessaging lets it auto-register; the result arrives in onRegistered.
        FirebaseMessaging.getInstance()
        scope.launch {
            combine(firebaseUid(), store.data.map { it[FID] }) { uid, fid -> uid to fid }
                .distinctUntilChanged()
                .collect { (uid, fid) -> if (uid != null && fid != null) upload(uid, fid) }
        }
    }

    fun onRegistered(fid: String) {
        scope.launch { store.edit { it[FID] = fid } }
    }

    fun onUnregistered(fid: String) {
        scope.launch {
            store.edit { if (it[FID] == fid) it.remove(FID) }
            FirebaseAuth.getInstance().currentUser?.uid?.let { uid -> runCatching { device(uid, fid).delete() } }
        }
    }

    /** Removes this phone from the signed-in account so pushes for it stop. */
    suspend fun forgetThisPhone() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val fid = store.data.first()[FID] ?: return
        // Waits briefly so the delete reaches the server before sign-out; offline it stays queued.
        withTimeoutOrNull(FORGET_TIMEOUT_MS) { runCatching { device(uid, fid).delete().await() } }
    }

    private fun upload(uid: String, fid: String) {
        // Not awaited: Firestore queues it offline and sends it when it can.
        device(uid, fid)
            .set(mapOf("platform" to "android", "updatedAt" to FieldValue.serverTimestamp()))
            .addOnFailureListener { Log.w("ChoomHub", "Push registration upload failed: ${it.javaClass.simpleName}") }
    }

    private fun device(uid: String, fid: String) = db.collection("users").document(uid).collection("devices").document(fid)

    private companion object {
        val FID = stringPreferencesKey("fcm_installation_id")
        const val FORGET_TIMEOUT_MS = 3_000L
    }
}
