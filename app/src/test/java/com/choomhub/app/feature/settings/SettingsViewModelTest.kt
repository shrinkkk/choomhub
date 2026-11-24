package com.choomhub.app.feature.settings

import com.choomhub.app.domain.model.CoupleNames
import com.choomhub.app.domain.model.NotificationCategory
import com.choomhub.app.domain.model.PushAvailability
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.model.TestPushResult
import com.choomhub.app.testing.FakeNotificationRepository
import com.choomhub.app.testing.FakeNotifier
import com.choomhub.app.testing.FakeProfileRepository
import com.choomhub.app.testing.FakeSpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val profile = FakeProfileRepository(CoupleNames(partnerName = "Wren", myName = null))
    private val space = FakeSpaceRepository(SpaceState.Paired(FakeSpaceRepository().me, "c1", "partner"))
    private val prefs = FakeNotificationRepository()
    private val notifier = FakeNotifier(PushAvailability.Ready)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = SettingsViewModel(profile, space, prefs, notifier)

    @Test
    fun `turning a category off is saved and shown`() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notifications.collect {} }
        assertTrue(vm.notifications.value.prefs.isEnabled(NotificationCategory.Meetup))

        vm.setNotificationsEnabled(NotificationCategory.Meetup, false)
        assertFalse(vm.notifications.value.prefs.isEnabled(NotificationCategory.Meetup))
        assertTrue(vm.notifications.value.prefs.isEnabled(NotificationCategory.Space))
        assertEquals(PushAvailability.Ready, vm.notifications.value.availability)
    }

    @Test
    fun `test send reports success and failure`() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notifications.collect {} }
        vm.sendTestNotification()
        assertEquals(TestSendState.Done(ok = true, message = "Sent. It should show up in a few seconds."), vm.notifications.value.test)

        notifier.testResult = TestPushResult.NotSent("Couldn't reach the push service. Check your connection.")
        vm.sendTestNotification()
        assertEquals(TestSendState.Done(ok = false, message = "Couldn't reach the push service. Check your connection."), vm.notifications.value.test)
        assertEquals(2, notifier.tests)
    }

    @Test
    fun `renaming tells the partner only when the name changes`() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.names.collect {} }
        assertNull(vm.renamePartner("Wren"))
        assertTrue(notifier.sent.isEmpty())

        assertNull(vm.renamePartner("Rook"))
        assertEquals("Rook", profile.names.value.partnerName)
        assertEquals(listOf(PushEvent.Renamed to null), notifier.sent)
    }

    @Test
    fun `signing out forgets this phone, then signs out`() {
        vm().signOut()
        assertEquals(1, notifier.forgotten)
        assertEquals(SpaceState.SignedOut, space.space.value)
    }
}
