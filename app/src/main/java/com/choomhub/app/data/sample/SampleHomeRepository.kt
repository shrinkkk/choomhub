package com.choomhub.app.data.sample

import com.choomhub.app.domain.model.HomeSnapshot
import com.choomhub.app.domain.model.Member
import com.choomhub.app.domain.model.Mood
import com.choomhub.app.domain.model.PartnerMood
import com.choomhub.app.domain.model.PointBalances
import com.choomhub.app.domain.model.SharedCatStatus
import com.choomhub.app.domain.model.Snap
import com.choomhub.app.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/**
 * Local, in-memory sample data so the UI can be built and reviewed before the
 * backend exists. Clearly flagged with [HomeSnapshot.isSampleData] so screens
 * can say they are showing sample content.
 *
 * Replace the Hilt binding in [com.choomhub.app.data.di.DataModule] with a
 * Room-backed repository when the mood/snaps/points milestones land; no UI
 * changes are needed.
 */
class SampleHomeRepository @Inject constructor(
    private val clock: Clock,
) : HomeRepository {

    override fun observeHome(): Flow<HomeSnapshot> = flow {
        val now = clock.instant()
        emit(
            HomeSnapshot(
                viewer = SampleMembers.Viewer,
                partnerMood = PartnerMood(
                    partner = SampleMembers.Partner,
                    mood = Mood.Sleepy,
                    note = "rewatching ghibli under three blankets",
                    updatedAt = now.minus(Duration.ofMinutes(12)),
                ),
                latestSnap = Snap(
                    id = "sample-snap-1",
                    sender = SampleMembers.Partner,
                    caption = "the sky looked like you today",
                    sentAt = now.minus(Duration.ofMinutes(47)),
                    isUnseen = true,
                ),
                points = PointBalances(goodBoy = 128, goodGirl = 142),
                cat = SharedCatStatus(isAdopted = false, name = null),
                isSampleData = true,
            ),
        )
    }
}

/**
 * Placeholder identities for sample data. The real names come from the
 * couple's profiles once onboarding exists.
 */
internal object SampleMembers {
    val Viewer = Member(id = "sample-viewer", displayName = "Choom")
    val Partner = Member(id = "sample-partner", displayName = "Moonie")
}
