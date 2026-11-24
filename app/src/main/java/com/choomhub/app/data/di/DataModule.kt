package com.choomhub.app.data.di

import com.choomhub.app.core.firebase.FirebaseConfig
import com.choomhub.app.data.firebase.FirebaseSpaceRepository
import com.choomhub.app.data.firebase.FirestoreMeetupRepository
import com.choomhub.app.data.firebase.FirestoreNotificationRepository
import com.choomhub.app.data.firebase.FirestoreProfileRepository
import com.choomhub.app.data.meetup.LocalMeetupRepository
import com.choomhub.app.data.profile.DataStoreProfileRepository
import com.choomhub.app.data.push.FirebaseIdTokenSource
import com.choomhub.app.data.push.IdTokenSource
import com.choomhub.app.data.push.InMemoryNotificationRepository
import com.choomhub.app.data.push.NoopNotifier
import com.choomhub.app.data.push.PushEndpoint
import com.choomhub.app.data.push.WorkManagerNotifier
import com.choomhub.app.data.sample.SampleHomeRepository
import com.choomhub.app.data.space.DeviceOnlySpaceRepository
import com.choomhub.app.domain.repository.HomeRepository
import com.choomhub.app.domain.model.PushAvailability
import com.choomhub.app.domain.repository.MeetupRepository
import com.choomhub.app.domain.repository.NotificationRepository
import com.choomhub.app.domain.repository.Notifier
import com.choomhub.app.domain.repository.ProfileRepository
import com.choomhub.app.domain.repository.SpaceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/*
 * Repository bindings. Shared data (space, names, meetup) uses Firestore when
 * Firebase is configured in local.properties, otherwise device-only storage.
 * Each lives in its own module so tests can replace just that one.
 */

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: SampleHomeRepository): HomeRepository
}

@Module
@InstallIn(SingletonComponent::class)
object SpaceModule {
    @Provides
    @Singleton
    fun provideSpaceRepository(
        firebase: Provider<FirebaseSpaceRepository>,
        deviceOnly: Provider<DeviceOnlySpaceRepository>,
    ): SpaceRepository = if (FirebaseConfig.isConfigured) firebase.get() else deviceOnly.get()
}

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {
    @Provides
    @Singleton
    fun provideProfileRepository(
        firestore: Provider<FirestoreProfileRepository>,
        local: Provider<DataStoreProfileRepository>,
    ): ProfileRepository = if (FirebaseConfig.isConfigured) firestore.get() else local.get()
}

@Module
@InstallIn(SingletonComponent::class)
object MeetupModule {
    @Provides
    @Singleton
    fun provideMeetupRepository(
        firestore: Provider<FirestoreMeetupRepository>,
        local: Provider<LocalMeetupRepository>,
    ): MeetupRepository = if (FirebaseConfig.isConfigured) firestore.get() else local.get()
}

@Module
@InstallIn(SingletonComponent::class)
object PushModule {
    @Provides
    @Singleton
    fun provideNotifier(workManager: Provider<WorkManagerNotifier>): Notifier = when {
        !FirebaseConfig.isConfigured -> NoopNotifier(PushAvailability.DeviceOnly)
        FirebaseConfig.notifyEndpoint() == null -> NoopNotifier(PushAvailability.NotSetUp)
        else -> workManager.get()
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        firestore: Provider<FirestoreNotificationRepository>,
        local: Provider<InMemoryNotificationRepository>,
    ): NotificationRepository = if (FirebaseConfig.isConfigured) firestore.get() else local.get()
}

/** What the push Worker client needs; kept separate so tests can swap [PushModule] alone. */
@Module
@InstallIn(SingletonComponent::class)
object PushClientModule {
    @Provides
    fun providePushEndpoint(): PushEndpoint = PushEndpoint(FirebaseConfig.notifyEndpoint())

    @Provides
    fun provideIdTokenSource(impl: FirebaseIdTokenSource): IdTokenSource = impl
}
