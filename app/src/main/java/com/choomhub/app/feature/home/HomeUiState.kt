package com.choomhub.app.feature.home

import com.choomhub.app.core.common.TimeLabels
import com.choomhub.app.domain.model.AffectionKind
import com.choomhub.app.domain.model.HomeSnapshot
import com.choomhub.app.domain.model.MoodOption
import com.choomhub.app.domain.model.PointBalances
import com.choomhub.app.domain.model.PointsRole
import com.choomhub.app.domain.model.SharedCatStatus
import java.time.Clock
import java.time.ZonedDateTime

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val greeting: String,
        val dateLabel: String,
        /** Name your partner gave you; null until synced. */
        val viewerName: String?,
        val partner: PartnerMoodUi?,
        val snap: SnapUi?,
        val points: PointBalances,
        val myRole: PointsRole?,
        val partnerName: String?,
        val cat: SharedCatStatus,
        val isSampleData: Boolean,
        /** Your own mood, shown under theirs so you can change it. */
        val myMood: MyMoodUi? = null,
        /** The latest gesture from them, and the latest from you. */
        val lastReceived: GestureUi? = null,
        val lastSent: GestureUi? = null,
        /** e.g. "You sent one 5 min ago"; null if you haven't. */
        val mySnapLabel: String? = null,
        /** The newest ledger entry either way, e.g. "Wren awarded you 5" + "for the dishes" + "2 h ago". */
        val lastAward: AwardUi? = null,
    ) : HomeUiState
}

data class AwardUi(val text: String, val reason: String?, val whenLabel: String)

data class MyMoodUi(val mood: MoodOption, val note: String?)

/** e.g. "Wren sent a hug" + "3 min ago". */
data class GestureUi(val kind: AffectionKind, val text: String, val whenLabel: String)

data class PartnerMoodUi(
    val name: String,
    val mood: MoodOption,
    val note: String?,
    val updatedLabel: String,
)

data class SnapUi(
    val id: String,
    val senderName: String,
    val caption: String?,
    val sentAt: ZonedDateTime,
    val relativeLabel: String,
    val isNew: Boolean,
    /** Width / height of the photo. */
    val aspect: Float = 4f / 3f,
)

/** Pure mapping from domain data to what the screen renders. */
internal fun HomeSnapshot.toUiState(clock: Clock): HomeUiState.Ready {
    val now = clock.instant()
    val zone = clock.zone
    val localNow = now.atZone(zone)
    return HomeUiState.Ready(
        greeting = TimeLabels.greeting(localNow.toLocalTime()),
        dateLabel = TimeLabels.longDate(localNow),
        viewerName = viewerName,
        partner = partnerMood?.let {
            PartnerMoodUi(
                name = it.partner.displayName,
                mood = it.mood,
                note = it.note?.takeIf(String::isNotBlank),
                updatedLabel = TimeLabels.relative(it.updatedAt ?: now, now, zone),
            )
        },
        snap = partnerSnap?.let {
            val at = it.sentAt ?: now
            SnapUi(
                id = it.id,
                senderName = partnerName ?: "They",
                caption = it.caption,
                sentAt = at.atZone(zone),
                relativeLabel = TimeLabels.relative(at, now, zone),
                isNew = it.id != seenSnapId,
                aspect = if (it.width > 0 && it.height > 0) it.width.toFloat() / it.height else 4f / 3f,
            )
        },
        lastAward = awards.firstOrNull()?.let { a ->
            val who = partnerName ?: "They"
            val n = kotlin.math.abs(a.amount)
            AwardUi(
                text = when {
                    a.toMe && a.amount > 0 -> "$who awarded you $n"
                    a.toMe -> "$who took $n from you"
                    a.amount > 0 -> "You awarded $who $n"
                    else -> "You took $n from $who"
                },
                reason = a.reason,
                whenLabel = TimeLabels.relative(a.at ?: now, now, zone),
            )
        },
        mySnapLabel = mySnap?.let {
            "You sent one ${TimeLabels.relative(it.sentAt ?: now, now, zone)}" +
                (it.partnerReaction?.let { r -> " \u00B7 ${partnerName ?: "they"} $r" } ?: "")
        },
        points = points,
        myRole = myRole,
        partnerName = partnerName ?: partnerMood?.partner?.displayName,
        cat = cat,
        isSampleData = isSampleData,
        myMood = myMood?.let { MyMoodUi(it.mood, it.note) },
        lastReceived = recentGestures.firstOrNull { !it.fromMe }?.let { g ->
            GestureUi(g.kind, "${partnerName ?: "They"} ${g.kind.sent}", TimeLabels.relative(g.sentAt ?: now, now, zone))
        },
        lastSent = recentGestures.firstOrNull { it.fromMe }?.let { g ->
            GestureUi(g.kind, "You ${g.kind.sent}", TimeLabels.relative(g.sentAt ?: now, now, zone))
        },
    )
}
