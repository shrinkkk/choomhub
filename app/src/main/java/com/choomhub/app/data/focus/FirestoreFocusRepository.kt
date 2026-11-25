package com.choomhub.app.data.focus

import com.choomhub.app.data.firebase.FirebaseSpaceRepository
import com.choomhub.app.data.firebase.observe
import com.choomhub.app.domain.model.FocusChange
import com.choomhub.app.domain.model.FocusRoomData
import com.choomhub.app.domain.model.FocusRules
import com.choomhub.app.domain.model.FocusSession
import com.choomhub.app.domain.model.FocusState
import com.choomhub.app.domain.model.FocusTotals
import com.choomhub.app.domain.model.JamLink
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.accountOrNull
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.repository.FocusRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.util.Date
import javax.inject.Inject

/**
 * couples/{id}/focus/{uid}        { state, bankedMs, focusSince, breakEndsAt, updatedAt }
 * couples/{id}/focusTotals/{uid}  { totalMs, weekMs, weekStart ("2026-11-23"), pointHours }
 * couples/{id}/shared/jam         { url, by, at }
 *
 * Session times are the phone's own clock (they're written offline too); both phones only
 * ever subtract them from "now", so a few seconds of clock difference is all it costs.
 */
class FirestoreFocusRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
) : FocusRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun couple(id: String) = db.collection(FirebaseSpaceRepository.COUPLES).document(id)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRoom(): Flow<FocusRoomData> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull
        val me = space.accountOrNull?.uid
        if (coupleId == null || me == null) return@flatMapLatest flowOf(FocusRoomData())
        val partner = (space as? SpaceState.Paired)?.partnerUid
        val c = couple(coupleId)
        combine(
            c.collection("focus").document(me).observe(),
            partner?.let { c.collection("focus").document(it).observe() } ?: flowOf(null),
            c.collection("focusTotals").document(me).observe(),
            partner?.let { c.collection("focusTotals").document(it).observe() } ?: flowOf(null),
            c.collection("shared").document("jam").observe(),
        ) { mine, theirs, myTotals, theirTotals, jam ->
            FocusRoomData(
                me = sessionFrom(mine),
                partner = sessionFrom(theirs),
                myTotals = totalsFrom(myTotals),
                partnerTotals = totalsFrom(theirTotals),
                jam = jam?.getString("url")?.let { JamLink(it, jam.getString("by") == me, jam.getTimestamp("at")?.toDate()?.toInstant()) },
            )
        }
    }.distinctUntilChanged()

    override suspend fun apply(change: FocusChange) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.accountOrNull?.uid ?: return
        val c = couple(coupleId)
        val s = change.session
        val batch = db.batch()
        batch.set(
            c.collection("focus").document(me),
            mapOf(
                "state" to s.state.wire,
                "bankedMs" to s.bankedMs,
                "focusSince" to s.focusSince?.let { Timestamp(Date.from(it)) },
                "breakEndsAt" to s.breakEndsAt?.let { Timestamp(Date.from(it)) },
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
        val t = change.totals
        batch.set(
            c.collection("focusTotals").document(me),
            mapOf(
                "totalMs" to t.totalMs,
                "weekMs" to t.weekMs,
                "weekStart" to t.weekStart?.toString(),
                "pointHours" to t.pointHours,
            ),
        )
        if (change.points > 0) {
            batch.set(
                c.collection("points").document(),
                mapOf(
                    "from" to me,
                    "to" to me,
                    "amount" to change.points,
                    "reason" to FocusRules.POINTS_REASON,
                    "at" to FieldValue.serverTimestamp(),
                ),
            )
        }
        // Not awaited: it applies at once and syncs when there's a connection.
        batch.commit()
    }

    override suspend fun setJam(url: String?) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.accountOrNull?.uid ?: return
        val ref = couple(coupleId).collection("shared").document("jam")
        if (url == null) {
            ref.delete()
        } else {
            ref.set(mapOf("url" to url, "by" to me, "at" to FieldValue.serverTimestamp()))
        }
    }

    companion object {
        fun sessionFrom(doc: DocumentSnapshot?): FocusSession {
            if (doc == null || !doc.exists()) return FocusSession()
            return FocusSession(
                state = FocusState.fromWire(doc.getString("state")),
                bankedMs = doc.getLong("bankedMs") ?: 0,
                focusSince = doc.getTimestamp("focusSince")?.toDate()?.toInstant(),
                breakEndsAt = doc.getTimestamp("breakEndsAt")?.toDate()?.toInstant(),
            )
        }

        fun totalsFrom(doc: DocumentSnapshot?): FocusTotals {
            if (doc == null || !doc.exists()) return FocusTotals()
            return FocusTotals(
                totalMs = doc.getLong("totalMs") ?: 0,
                weekMs = doc.getLong("weekMs") ?: 0,
                weekStart = doc.getString("weekStart")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                pointHours = doc.getLong("pointHours")?.toInt() ?: 0,
            )
        }
    }
}
