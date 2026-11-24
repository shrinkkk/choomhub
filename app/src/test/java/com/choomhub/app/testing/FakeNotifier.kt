package com.choomhub.app.testing

import com.choomhub.app.domain.model.NotificationCategory
import com.choomhub.app.domain.model.NotificationPrefs
import com.choomhub.app.domain.model.PushAvailability
import com.choomhub.app.domain.model.PushEvent
import com.choomhub.app.domain.model.TestPushResult
import com.choomhub.app.domain.repository.NotificationRepository
import com.choomhub.app.domain.repository.Notifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Records what would have been sent. */
class FakeNotifier(
    override val availability: PushAvailability = PushAvailability.DeviceOnly,
    var testResult: TestPushResult = TestPushResult.Sent,
) : Notifier {
    val sent = mutableListOf<Pair<PushEvent, String?>>()
    val kinds = mutableListOf<String?>()
    var forgotten = 0
    var tests = 0

    override fun notifyPartner(event: PushEvent, detail: String?, kind: String?) {
        sent += event to detail
        kinds += kind
    }

    override suspend fun sendTest(): TestPushResult {
        tests++
        return testResult
    }

    override suspend fun forgetThisPhone() {
        forgotten++
    }
}

class FakeNotificationRepository : NotificationRepository {
    val prefs = MutableStateFlow(NotificationPrefs())

    override fun observePrefs(): Flow<NotificationPrefs> = prefs

    override suspend fun setEnabled(category: NotificationCategory, enabled: Boolean) {
        prefs.update { NotificationPrefs(if (enabled) it.disabled - category else it.disabled + category) }
    }
}
