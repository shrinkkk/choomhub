package com.choomhub.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.NameRules
import com.choomhub.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val names: StateFlow<CoupleNames> = profileRepository.observeNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoupleNames(partnerName = null, myName = null))

    /**
     * Renames your partner. Returns an error message to show, or null if the
     * name was accepted and is being saved.
     */
    fun renamePartner(input: String): String? {
        val result = NameRules.validate(input)
        if (result !is NameRules.Result.Valid) return NameRules.messageFor(result)
        viewModelScope.launch { profileRepository.setPartnerName(result.name) }
        return null
    }
}
