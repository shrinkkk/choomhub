package com.choomhub.app.data.push

import com.choomhub.app.domain.model.NotificationCategory
import com.choomhub.app.domain.model.NotificationPrefs
import com.choomhub.app.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Device-only builds have no partner to hear from, so choices needn't persist. */
@Singleton
class InMemoryNotificationRepository @Inject constructor() : NotificationRepository {
    private val prefs = MutableStateFlow(NotificationPrefs())

    override fun observePrefs(): Flow<NotificationPrefs> = prefs

    override suspend fun setEnabled(category: NotificationCategory, enabled: Boolean) {
        prefs.update { NotificationPrefs(if (enabled) it.disabled - category else it.disabled + category) }
    }
}
