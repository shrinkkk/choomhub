package com.choomhub.app.feature.home

import com.choomhub.app.core.common.TimeLabels
import com.choomhub.app.domain.model.HomeSnapshot
import com.choomhub.app.domain.model.Mood
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
    ) : HomeUiState
}

data class PartnerMoodUi(
    val name: String,
    val mood: Mood,
    val note: String?,
    val updatedLabel: String,
)

data class SnapUi(
    val senderName: String,
    val caption: String?,
    val sentAt: ZonedDateTime,
    val relativeLabel: String,
    val isNew: Boolean,
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
                updatedLabel = TimeLabels.relative(it.updatedAt, now, zone),
            )
        },
        snap = latestSnap?.let {
            SnapUi(
                senderName = it.sender.displayName,
                caption = it.caption?.takeIf(String::isNotBlank),
                sentAt = it.sentAt.atZone(zone),
                relativeLabel = TimeLabels.relative(it.sentAt, now, zone),
                isNew = it.isUnseen,
            )
        },
        points = points,
        myRole = myRole,
        partnerName = partnerMood?.partner?.displayName,
        cat = cat,
        isSampleData = isSampleData,
    )
}
