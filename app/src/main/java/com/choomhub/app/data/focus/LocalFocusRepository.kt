package com.choomhub.app.data.focus

import com.choomhub.app.domain.model.FocusChange
import com.choomhub.app.domain.model.FocusRoomData
import com.choomhub.app.domain.model.JamLink
import com.choomhub.app.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Without Firebase: the room works on this phone alone, until the app is closed. */
@Singleton
class LocalFocusRepository @Inject constructor() : FocusRepository {
    private val room = MutableStateFlow(FocusRoomData())

    override fun observeRoom(): Flow<FocusRoomData> = room

    override suspend fun apply(change: FocusChange) {
        room.update { it.copy(me = change.session, myTotals = change.totals) }
    }

    override suspend fun sendCat(outing: com.choomhub.app.domain.model.CatOuting) {
        room.update { it.copy(cat = outing) }
    }

    override suspend fun setJam(url: String?) {
        room.update { it.copy(jam = url?.let { u -> JamLink(u, fromMe = true, at = Instant.now()) }) }
    }
}
