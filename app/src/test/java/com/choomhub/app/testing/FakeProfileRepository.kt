package com.choomhub.app.testing

import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [ProfileRepository] for tests. */
class FakeProfileRepository(
    initial: CoupleNames = CoupleNames(partnerName = null, myName = null),
) : ProfileRepository {
    val names = MutableStateFlow(initial)

    override fun observeNames(): Flow<CoupleNames> = names

    override suspend fun setPartnerName(name: String) {
        names.value = names.value.copy(partnerName = name)
    }
}
