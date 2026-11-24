package com.choomhub.app.data.firebase

import com.choomhub.app.domain.model.NotificationCategory
import com.choomhub.app.domain.model.NotificationPrefs
import com.choomhub.app.domain.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** users/{uid}/settings/notifications. The push Worker reads it before sending. */
@Singleton
class FirestoreNotificationRepository @Inject constructor() : NotificationRepository {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePrefs(): Flow<NotificationPrefs> = firebaseUid().flatMapLatest { uid ->
        if (uid == null) flowOf(NotificationPrefs()) else doc(uid).observe().map(::prefsFrom)
    }

    override suspend fun setEnabled(category: NotificationCategory, enabled: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Not awaited: applied to the local cache at once, synced when online.
        doc(uid).set(mapOf(category.key to enabled, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
    }

    private fun doc(uid: String) = db.collection("users").document(uid).collection("settings").document("notifications")

    companion object {
        fun prefsFrom(snapshot: DocumentSnapshot?): NotificationPrefs =
            NotificationPrefs(NotificationCategory.entries.filter { snapshot?.getBoolean(it.key) == false }.toSet())
    }
}
