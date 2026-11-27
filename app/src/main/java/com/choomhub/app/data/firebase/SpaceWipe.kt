package com.choomhub.app.data.firebase

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * Deletes everything inside a space, for when the last person leaves (there's no server to do it
 * on the free plan). The rules allow these deletes only while the space is marked as closing and
 * you're its only member.
 */
internal object SpaceWipe {
    /** Every sub-collection a space has (see firebase/firestore.rules). */
    val COLLECTIONS = listOf(
        "names", "shared", "moods", "gestures", "snaps", "collections", "points",
        "focus", "focusTotals", "pet", "petDiary", "rewards", "rewardProposals", "purchases", "songs", "bounties", "records", "meetups",
    )

    private const val BATCH = 200 // a snap is two deletes; a batch takes 500

    suspend fun deleteEverything(db: FirebaseFirestore, couple: DocumentReference) {
        for (name in COLLECTIONS) {
            // A meetup can carry 12 photo documents, so fewer per batch (a batch takes 500 deletes).
            val size = if (name == "meetups") 30 else BATCH
            while (true) {
                val docs = couple.collection(name).limit(size.toLong()).get(Source.SERVER).await().documents
                if (docs.isEmpty()) break
                val batch = db.batch()
                docs.forEach { doc ->
                    // A snap's picture lives in a document under it.
                    if (name == "snaps") batch.delete(doc.reference.collection("image").document("full"))
                    // A meetup's photos live in documents under it.
                    if (name == "meetups") {
                        (doc.get("photos") as? List<*>).orEmpty().filterIsInstance<String>()
                            .forEach { batch.delete(doc.reference.collection("photos").document(it)) }
                    }
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                if (docs.size < size) break
            }
        }
    }
}
