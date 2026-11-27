package com.choomhub.app.feature.pet

import com.choomhub.app.data.pet.PetSeenState
import com.choomhub.app.data.pet.PetSeen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choomhub.app.core.common.SecondTicker
import com.choomhub.app.domain.model.PetAction
import com.choomhub.app.domain.model.PetDiaryEntry
import com.choomhub.app.domain.model.PetOutcome
import com.choomhub.app.domain.model.PetRules
import com.choomhub.app.domain.model.PetState
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.PetRepository
import com.choomhub.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.random.Random

/** What the Pet House shows. */
data class PetUi(
    val loaded: Boolean = false,
    val pet: PetState = PetState(),
    val now: Instant = Instant.EPOCH,
    /** Your name as your partner knows you (used in the cat's diary, which you both read). */
    val myName: String = "You",
    val partnerName: String = "them",
    val diary: List<PetDiaryEntry> = emptyList(),
    /** Pages with something this phone hasn't seen yet (a dot on their buttons). */
    val fresh: Set<PetPage> = emptySet(),
)

/** Which pages have something new since this phone last looked. */
internal fun freshPages(pet: PetState, diary: List<PetDiaryEntry>, seen: PetSeenState, now: Instant): Set<PetPage> = buildSet {
    val newestDiary = diary.mapNotNull { it.at }.maxOrNull()
    if (seen.diaryAt != null && newestDiary != null && newestDiary > seen.diaryAt) add(PetPage.Diary)
    if (seen.gifts != null && pet.gifts.size > seen.gifts) add(PetPage.Gifts)
    if (seen.trophies != null && unlockedTrophies(pet, now).any { it !in seen.trophies }) add(PetPage.Trophies)
}

internal fun unlockedTrophies(pet: PetState, now: Instant): Set<String> = PetRules.TROPHIES.filter { it.unlocked(pet, now) }.map { it.id }.toSet()

/** A line to show after doing something; [id] makes repeats show again. */
data class PetMessage(val text: String, val id: Long)

@HiltViewModel
class PetViewModel @Inject constructor(
    private val repository: PetRepository,
    profileRepository: ProfileRepository,
    ticker: SecondTicker,
    private val notifier: Notifier,
    private val clock: Clock,
    private val seen: PetSeen,
) : ViewModel() {

    /** Rolls the dice for bites and finds; replaced in tests. */
    internal var random: Random = Random.Default

    private var latest = PetUi()

    val state: StateFlow<PetUi> = combine(
        repository.observePet(),
        repository.observeDiary(),
        profileRepository.observeNames(),
        ticker.ticks().onStart { emit(clock.instant()) },
        seen.observe(),
    ) { pet, diary, names, now, seenState ->
        PetUi(
            loaded = pet.loaded,
            pet = pet,
            now = now,
            myName = names.myName ?: "someone",
            partnerName = names.partnerName ?: "them",
            diary = diary,
            fresh = freshPages(pet, diary, seenState, now),
        ).also { latest = it }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetUi())

    private val _message = MutableStateFlow<PetMessage?>(null)
    val message: StateFlow<PetMessage?> = _message.asStateFlow()

    private var messageId = 0L

    init {
        // The once-a-day things (a gift, a sniffle, coming home muddy) and your daily coins.
        viewModelScope.launch {
            val pet = repository.observePet().first { it.loaded }
            // First time on this phone: whatever's there now isn't news.
            val already = seen.observe().first()
            if (already.diaryAt == null) seen.diary(clock.instant())
            if (already.gifts == null) seen.gifts(pet.gifts.size)
            if (already.trophies == null) seen.trophies(unlockedTrophies(pet, clock.instant()))
            if (!pet.adopted) return@launch
            runDaily(pet)
        }
    }

    /** A page was opened: what's on it now counts as seen on this phone. */
    fun seen(page: PetPage) {
        val ui = latest
        viewModelScope.launch {
            when (page) {
                PetPage.Diary -> ui.diary.mapNotNull { it.at }.maxOrNull()?.let { seen.diary(it) }
                PetPage.Gifts -> seen.gifts(ui.pet.gifts.size)
                PetPage.Trophies -> seen.trophies(unlockedTrophies(ui.pet, clock.instant()))
                else -> Unit
            }
        }
    }

    private suspend fun runDaily(pet: PetState) {
        val patch = PetRules.daily(pet, clock.instant(), clock.zone)
        if (patch.isEmpty()) return
        repository.apply(patch)
        val name = pet.name ?: "The cat"
        when {
            patch.sickSince != null -> show("$name isn't feeling well today.")
            patch.gift != null -> show("$name left something on the shelf for you.")
            patch.muddy == true -> show("$name came back muddy from somewhere.")
            patch.coins > 0 -> show("+${patch.coins} coins for coming to see $name.")
        }
    }

    fun dismissMessage() {
        _message.value = null
    }

    private fun show(text: String) {
        _message.value = PetMessage(text, messageId++)
    }

    /** Does [action] and returns what happened (the panels show it too). */
    fun act(action: PetAction): PetOutcome {
        val ui = latest
        val today = clock.instant().atZone(clock.zone).toLocalDate()
        val diaryToday = ui.diary.count { it.at?.atZone(clock.zone)?.toLocalDate() == today }
        val outcome = PetRules.act(ui.pet, action, clock.instant(), clock.zone, random, ui.myName, ui.partnerName, diaryToday)
        if (!outcome.patch.isEmpty()) {
            viewModelScope.launch { repository.apply(outcome.patch) }
            if (action is PetAction.StartTeamCare) {
                notifier.notifyPartner(PushEvent.PetHelp, detail = ui.pet.name, kind = action.kind.key)
            }
        }
        show(outcome.message)
        return outcome
    }

    /** Suggests a name; your partner agrees (or adopts it anyway). */
    fun proposeName(input: String) {
        val name = PetRules.cleanName(input) ?: return
        viewModelScope.launch { repository.proposeName(name) }
        notifier.notifyPartner(PushEvent.PetNamed, detail = name)
    }

    /** Brings the cat home with [input] as its name: today becomes its birthday. */
    fun adopt(input: String) {
        val name = PetRules.cleanName(input) ?: return
        val (traits, favourite, disliked) = PetRules.adoptionTraits(random)
        viewModelScope.launch {
            repository.adopt(name, traits, favourite, disliked, clock.instant())
            // A first day: the welcome coins and the streak start now.
            val pet = repository.observePet().first { it.adopted }
            runDaily(pet)
        }
        show("Welcome home, $name.")
    }
}
