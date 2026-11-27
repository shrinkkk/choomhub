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
        "focus", "focusTotals", "pet", "petDiary", "rewards", "rewardProposals", "purchases", "songs", "bounties",
    )

    private const val BATCH = 200 // a snap is two deletes; a batch takes 500

    suspend fun deleteEverything(db: FirebaseFirestore, couple: DocumentReference) {
        for (name in COLLECTIONS) {
            while (true) {
                val docs = couple.collection(name).limit(BATCH.toLong()).get(Source.SERVER).await().documents
                if (docs.isEmpty()) break
                val batch = db.batch()
                docs.forEach { doc ->
                    // A snap's picture lives in a document under it.
                    if (name == "snaps") batch.delete(doc.reference.collection("image").document("full"))
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                if (docs.size < BATCH) break
            }
        }
    }
}
