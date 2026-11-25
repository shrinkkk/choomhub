package com.choomhub.app.data.pet

import com.choomhub.app.data.firebase.FirebaseSpaceRepository
import com.choomhub.app.data.firebase.observe
import com.choomhub.app.domain.model.Food
import com.choomhub.app.domain.model.NeedLevel
import com.choomhub.app.domain.model.PendingCare
import com.choomhub.app.domain.model.PetDiaryEntry
import com.choomhub.app.domain.model.PetGift
import com.choomhub.app.domain.model.PetNeed
import com.choomhub.app.domain.model.PetPatch
import com.choomhub.app.domain.model.PetRules
import com.choomhub.app.domain.model.PetState
import com.choomhub.app.domain.model.PetTrait
import com.choomhub.app.domain.model.TeamCare
import com.choomhub.app.domain.model.coupleIdOrNull
import com.choomhub.app.domain.model.memberIdOrNull
import com.choomhub.app.domain.repository.PetRepository
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.Date
import javax.inject.Inject

/**
 * couples/{id}/pet/cat       the cat (one document: needs, money, things, counters, gifts...)
 * couples/{id}/petDiary/{id} { text, at, by }  its diary
 *
 * Needs are stored as a value at a moment and worked out from there, so nothing has to run on a
 * server. Coins and bond are kept per person ({uid: n}); counters and food go up with
 * increments, so both phones can write at once without losing anything.
 */
class FirestorePetRepository @Inject constructor(
    private val spaceRepository: SpaceRepository,
) : PetRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun couple(id: String) = db.collection(FirebaseSpaceRepository.COUPLES).document(id)
    private fun cat(id: String) = couple(id).collection("pet").document("cat")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePet(): Flow<PetState> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull
        val me = space.memberIdOrNull
        if (coupleId == null || me == null) flowOf(PetState(loaded = true)) else cat(coupleId).observe().map { petFrom(it, me) }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDiary(): Flow<List<PetDiaryEntry>> = spaceRepository.observeSpace().flatMapLatest { space ->
        val coupleId = space.coupleIdOrNull ?: return@flatMapLatest flowOf(emptyList())
        couple(coupleId).collection("petDiary").orderBy("at", Query.Direction.DESCENDING).limit(60).observe().map { snap ->
            snap?.documents.orEmpty().mapNotNull { d ->
                d.getString("text")?.let { PetDiaryEntry(d.id, it, d.getTimestamp("at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.toInstant()) }
            }
        }
    }.distinctUntilChanged()

    private suspend fun ids(): Pair<String, String>? {
        val space = spaceRepository.observeSpace().first()
        return (space.coupleIdOrNull ?: return null) to (space.memberIdOrNull ?: return null)
    }

    override suspend fun proposeName(name: String) {
        val (coupleId, me) = ids() ?: return
        cat(coupleId).set(mapOf("proposedName" to name, "proposedBy" to me, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
    }

    override suspend fun adopt(name: String, traits: List<PetTrait>, favorite: Food, disliked: Food, now: Instant) {
        val (coupleId, me) = ids() ?: return
        val ts = Timestamp(Date.from(now))
        cat(coupleId).set(
            mapOf(
                "name" to name,
                "adoptedAt" to ts,
                "proposedName" to null,
                "proposedBy" to null,
                "traits" to traits.map { it.key },
                "favoriteFood" to favorite.key,
                "dislikedFood" to disliked.key,
                "needs" to PetNeed.entries.associate { it.key to mapOf("v" to 80.0, "at" to ts) },
                "weight" to PetRules.BASE_WEIGHT.toDouble(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        )
        diary(coupleId, me, listOf("They brought me home and called me $name. I have decided to allow it."))
    }

    override suspend fun apply(patch: PetPatch) {
        val (coupleId, me) = ids() ?: return
        val now = Timestamp.now()
        val u = mutableMapOf<String, Any?>("updatedAt" to FieldValue.serverTimestamp())
        patch.needs.forEach { (need, v) -> u["needs.${need.key}"] = mapOf("v" to v.toDouble(), "at" to now) }
        patch.weight?.let { u["weight"] = it.toDouble() }
        if (patch.coins != 0) u["coins.$me"] = FieldValue.increment(patch.coins.toLong())
        if (patch.bond != 0) u["bond.$me"] = FieldValue.increment(patch.bond.toLong())
        patch.counters.forEach { (k, n) -> if (n != 0) u["counters.$k"] = FieldValue.increment(n.toLong()) }
        patch.inventory.forEach { (k, n) -> if (n != 0) u["inventory.$k"] = FieldValue.increment(n.toLong()) }
        patch.own?.let { u["owned"] = FieldValue.arrayUnion(it) }
        patch.outfit?.let { u["outfit"] = it.ifEmpty { null } }
        if (patch.clearSick) u["sickSince"] = null else patch.sickSince?.let { u["sickSince"] = Timestamp(Date.from(it)) }
        patch.medicineLeft?.let { u["medicineLeft"] = it }
        patch.lastMedicineDay?.let { u["lastMedicineDay"] = it.toString() }
        patch.vetDone?.let { u["vetDone"] = it }
        patch.muddy?.let { u["muddy"] = it }
        patch.nailsAt?.let { u["nailsAt"] = Timestamp(Date.from(it)) }
        if (patch.clearPending) {
            u["pending"] = null
        } else {
            patch.pending?.let { u["pending"] = mapOf("kind" to it.kind.key, "by" to me, "at" to Timestamp(Date.from(it.at))) }
        }
        patch.gift?.let { u["gifts"] = FieldValue.arrayUnion(mapOf("id" to it, "at" to now, "for" to me)) }
        patch.streak?.let { (n, day) ->
            u["streak"] = n
            u["streakDay"] = day.toString()
        }
        patch.lastDailyCheck?.let { u["lastDailyCheck"] = it.toString() }
        patch.seenDay?.let { u["seen.$me"] = it.toString() }
        patch.coinDay?.let { (day, n) -> u["coinDay.$me"] = mapOf("day" to day.toString(), "n" to n) }
        patch.pettingAt?.let { u["pettingAt.$me"] = Timestamp(Date.from(it)) }
        // Not awaited, like every write here: it applies at once and syncs when there's a connection.
        cat(coupleId).set(nest(u), SetOptions.merge())
        diary(coupleId, me, patch.diary)
    }

    private fun diary(coupleId: String, me: String, lines: List<String>) {
        if (lines.isEmpty()) return
        val batch = db.batch()
        lines.forEach { line ->
            batch.set(
                couple(coupleId).collection("petDiary").document(),
                mapOf("text" to line.take(300), "at" to FieldValue.serverTimestamp(), "by" to me),
            )
        }
        batch.commit()
    }

    companion object {
        /**
         * set(merge) doesn't understand "a.b" paths the way update() does, so dotted keys are
         * turned into nested maps (merge then only touches those leaves).
         */
        @Suppress("UNCHECKED_CAST")
        fun nest(flat: Map<String, Any?>): Map<String, Any?> {
            val out = mutableMapOf<String, Any?>()
            flat.forEach { (key, value) ->
                val parts = key.split('.')
                var node = out
                parts.dropLast(1).forEach { part -> node = node.getOrPut(part) { mutableMapOf<String, Any?>() } as MutableMap<String, Any?> }
                node[parts.last()] = value
            }
            return out
        }

        @Suppress("UNCHECKED_CAST")
        fun petFrom(doc: DocumentSnapshot?, me: String): PetState {
            if (doc == null || !doc.exists()) return PetState(loaded = true)
            fun map(field: String) = (doc.get(field) as? Map<String, Any?>).orEmpty()
            fun day(s: Any?) = (s as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            fun ts(v: Any?) = (v as? Timestamp)?.toDate()?.toInstant()
            val needs = map("needs").mapNotNull { (k, v) ->
                val need = PetNeed.entries.firstOrNull { it.key == k } ?: return@mapNotNull null
                val m = v as? Map<String, Any?> ?: return@mapNotNull null
                val value = (m["v"] as? Number)?.toFloat() ?: return@mapNotNull null
                val at = ts(m["at"]) ?: return@mapNotNull null
                need to NeedLevel(value, at)
            }.toMap()
            val coins = map("coins")
            val bond = map("bond")
            val pending = map("pending").takeIf { it.isNotEmpty() }?.let { p ->
                val kind = TeamCare.fromKey(p["kind"] as? String) ?: return@let null
                PendingCare(kind, byMe = p["by"] == me, at = ts(p["at"]) ?: return@let null)
            }
            val gifts = (doc.get("gifts") as? List<*>).orEmpty().mapNotNull { g ->
                val m = g as? Map<String, Any?> ?: return@mapNotNull null
                PetGift(m["id"] as? String ?: return@mapNotNull null, ts(m["at"]) ?: Instant.EPOCH, forMe = m["for"] == me)
            }
            val myCoinDay = map("coinDay")[me] as? Map<String, Any?>
            val partnerPetting = map("pettingAt").filterKeys { it != me }.values.mapNotNull { ts(it) }.maxOrNull()
            return PetState(
                loaded = true,
                name = doc.getString("name"),
                adoptedAt = doc.getTimestamp("adoptedAt")?.toDate()?.toInstant(),
                proposedName = doc.getString("proposedName"),
                proposedByMe = doc.getString("proposedBy") == me,
                traits = (doc.get("traits") as? List<*>).orEmpty().mapNotNull { PetTrait.fromKey(it as? String) },
                favoriteFood = Food.fromKey(doc.getString("favoriteFood")),
                dislikedFood = Food.fromKey(doc.getString("dislikedFood")),
                needs = needs,
                weight = doc.getDouble("weight")?.toFloat() ?: PetRules.BASE_WEIGHT,
                sickSince = doc.getTimestamp("sickSince")?.toDate()?.toInstant(),
                medicineLeft = doc.getLong("medicineLeft")?.toInt() ?: 0,
                lastMedicineDay = day(doc.getString("lastMedicineDay")),
                vetDone = doc.getBoolean("vetDone") ?: false,
                muddy = doc.getBoolean("muddy") ?: false,
                nailsAt = doc.getTimestamp("nailsAt")?.toDate()?.toInstant(),
                outfit = doc.getString("outfit"),
                owned = (doc.get("owned") as? List<*>).orEmpty().filterIsInstance<String>().toSet(),
                inventory = map("inventory").mapNotNull { (k, v) -> (v as? Number)?.let { k to it.toInt() } }.toMap(),
                myCoins = (coins[me] as? Number)?.toInt() ?: 0,
                partnerCoins = coins.filterKeys { it != me }.values.sumOf { (it as? Number)?.toInt() ?: 0 },
                myBond = (bond[me] as? Number)?.toInt() ?: 0,
                partnerBond = bond.filterKeys { it != me }.values.sumOf { (it as? Number)?.toInt() ?: 0 },
                streak = doc.getLong("streak")?.toInt() ?: 0,
                streakDay = day(doc.getString("streakDay")),
                counters = map("counters").mapNotNull { (k, v) -> (v as? Number)?.let { k to it.toInt() } }.toMap(),
                gifts = gifts,
                pending = pending,
                lastDailyCheck = day(doc.getString("lastDailyCheck")),
                mySeenDay = day(map("seen")[me]),
                myCoinDay = day(myCoinDay?.get("day")),
                myCoinToday = (myCoinDay?.get("n") as? Number)?.toInt() ?: 0,
                partnerPettingAt = partnerPetting,
            )
        }
    }
}
