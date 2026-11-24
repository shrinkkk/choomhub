package com.choomhub.app.data.di

import com.choomhub.app.data.meetup.LocalMeetupRepository
import com.choomhub.app.data.profile.DataStoreProfileRepository
import com.choomhub.app.data.sample.SampleHomeRepository
import com.choomhub.app.domain.repository.HomeRepository
import com.choomhub.app.domain.repository.MeetupRepository
import com.choomhub.app.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds domain repository interfaces to their current implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: SampleHomeRepository): HomeRepository
}

/** Shared next-meetup. Local until the shared space is connected. */
@Module
@InstallIn(SingletonComponent::class)
abstract class MeetupModule {

    @Binds
    @Singleton
    abstract fun bindMeetupRepository(impl: LocalMeetupRepository): MeetupRepository
}

/** Kept separate so tests can swap the profile store for an in-memory fake. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: DataStoreProfileRepository): ProfileRepository
}
