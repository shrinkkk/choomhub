package com.choomhub.app.testing

import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.PointsRole
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

    override suspend fun setMyRole(role: PointsRole): Boolean {
        if (names.value.roleChosenByPartner && names.value.myRole != role) return false
        names.value = names.value.copy(myRole = role)
        return true
    }
}
