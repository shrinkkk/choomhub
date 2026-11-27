package com.choomhub.app.data.focus

import com.choomhub.app.data.firebase.FirebaseSpaceRepository
import com.choomhub.app.data.firebase.observe
import com.choomhub.app.domain.model.CatActivity
import com.choomhub.app.domain.model.CatOuting
import com.choomhub.app.domain.model.FocusChange
import com.choomhub.app.domain.model.FocusRate
import com.choomhub.app.domain.model.FocusRoomData
import com.choomhub.app.domain.model.FocusRules
import com.choomhub.app.domain.model.FocusSession
import com.choomhub.app.domain.model.FocusState
import com.choomhub.app.domain.model.FocusTotals
import com.choomhub.app.domain.model.JamLink
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.model.memberIdOrNull
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
 * couples/{id}/focusTotals/{uid}  { totalMs, weekMs, weekStart ("2026-11-23"), pointHours, togetherMs, togetherHours }
 * couples/{id}/shared/focusRate   { rate, proposed?, proposedBy?, at }  points per focus hour, agreed by both
 * couples/{id}/shared/jam         { url, by, at }
 * couples/{id}/shared/cat         { activity, since, by }  where the cat went when last tapped
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
        val me = space.memberIdOrNull
        if (coupleId == null || me == null) return@flatMapLatest flowOf(FocusRoomData())
        val partner = (space as? SpaceState.Paired)?.partnerUid
        val c = couple(coupleId)
        combine(
            c.collection("focus").document(me).observe(),
            partner?.let { c.collection("focus").document(it).observe() } ?: flowOf(null),
            c.collection("focusTotals").document(me).observe(),
            partner?.let { c.collection("focusTotals").document(it).observe() } ?: flowOf(null),
            c.collection("shared").document("jam").observe(),
            c.collection("shared").document("cat").observe(),
            c.collection("shared").document("focusRate").observe(),
        ) { values ->
            val mine = values[0]
            val theirs = values[1]
            val myTotals = values[2]
            val theirTotals = values[3]
            val jam = values[4]
            val cat = values[5]
            val rate = values[6]
            FocusRoomData(
                rate = rateFrom(rate, me),
                me = sessionFrom(mine),
                partner = sessionFrom(theirs),
                myTotals = totalsFrom(myTotals),
                partnerTotals = totalsFrom(theirTotals),
                jam = jam?.getString("url")?.let { JamLink(it, jam.getString("by") == me, jam.getTimestamp("at")?.toDate()?.toInstant()) },
                cat = cat?.let { doc ->
                    val activity = CatActivity.fromWire(doc.getString("activity"))
                    val since = doc.getTimestamp("since")?.toDate()?.toInstant()
                    if (activity != null && since != null) CatOuting(activity, since) else null
                },
            )
        }
    }.distinctUntilChanged()

    override suspend fun apply(change: FocusChange) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.memberIdOrNull ?: return
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
                "breakMs" to s.breakMs,
                "breakSince" to s.breakSince?.let { Timestamp(Date.from(it)) },
                "startTotalMs" to s.startTotalMs,
                "recent" to s.recent.map { mapOf("from" to it.first, "to" to it.last) },
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
                "togetherMs" to t.togetherMs,
                "togetherHours" to t.togetherHours,
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

    private suspend fun rateDoc(): Pair<com.google.firebase.firestore.DocumentReference, String>? {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return null
        val me = space.memberIdOrNull ?: return null
        return couple(coupleId).collection("shared").document("focusRate") to me
    }

    override suspend fun proposeRate(rate: Int) {
        val (ref, me) = rateDoc() ?: return
        val current = observeRoom().first().rate.rate
        ref.set(mapOf("rate" to current, "proposed" to rate.coerceIn(0, FocusRules.MAX_RATE), "proposedBy" to me, "at" to FieldValue.serverTimestamp()))
    }

    override suspend fun answerRate(agree: Boolean) {
        val (ref, _) = rateDoc() ?: return
        val r = observeRoom().first().rate
        val proposed = r.proposed ?: return
        if (r.proposedByMe) return
        ref.set(mapOf("rate" to if (agree) proposed else r.rate, "proposed" to null, "proposedBy" to null, "at" to FieldValue.serverTimestamp()))
    }

    override suspend fun takeBackRate() {
        val (ref, _) = rateDoc() ?: return
        val r = observeRoom().first().rate
        if (!r.proposedByMe) return
        ref.set(mapOf("rate" to r.rate, "proposed" to null, "proposedBy" to null, "at" to FieldValue.serverTimestamp()))
    }

    override suspend fun sendCat(outing: CatOuting) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.memberIdOrNull ?: return
        couple(coupleId).collection("shared").document("cat").set(
            mapOf("activity" to outing.activity.wire, "since" to Timestamp(Date.from(outing.since)), "by" to me),
        )
    }

    override suspend fun setJam(url: String?) {
        val space = spaceRepository.observeSpace().first()
        val coupleId = space.coupleIdOrNull ?: return
        val me = space.memberIdOrNull ?: return
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
                breakMs = doc.getLong("breakMs") ?: 0,
                breakSince = doc.getTimestamp("breakSince")?.toDate()?.toInstant(),
                startTotalMs = doc.getLong("startTotalMs") ?: 0,
                recent = (doc.get("recent") as? List<*>).orEmpty().mapNotNull { item ->
                    val m = item as? Map<*, *> ?: return@mapNotNull null
                    val from = (m["from"] as? Number)?.toLong() ?: return@mapNotNull null
                    val to = (m["to"] as? Number)?.toLong() ?: return@mapNotNull null
                    if (to > from) from..to else null
                },
            )
        }

        fun rateFrom(doc: DocumentSnapshot?, me: String): FocusRate {
            if (doc == null || !doc.exists()) return FocusRate()
            val rate = doc.getLong("rate")?.toInt()?.coerceIn(0, FocusRules.MAX_RATE) ?: FocusRules.DEFAULT_RATE
            val proposed = doc.getLong("proposed")?.toInt()?.coerceIn(0, FocusRules.MAX_RATE)
            return FocusRate(rate, proposed, proposed != null && doc.getString("proposedBy") == me)
        }

        fun totalsFrom(doc: DocumentSnapshot?): FocusTotals {
            if (doc == null || !doc.exists()) return FocusTotals()
            return FocusTotals(
                totalMs = doc.getLong("totalMs") ?: 0,
                weekMs = doc.getLong("weekMs") ?: 0,
                weekStart = doc.getString("weekStart")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                pointHours = doc.getLong("pointHours")?.toInt() ?: 0,
                togetherMs = doc.getLong("togetherMs") ?: 0,
                togetherHours = doc.getLong("togetherHours")?.toInt() ?: 0,
            )
        }
    }
}
