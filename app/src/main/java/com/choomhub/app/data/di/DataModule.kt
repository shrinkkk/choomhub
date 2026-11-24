package com.choomhub.app.data.di

import com.choomhub.app.data.sample.SampleHomeRepository
import com.choomhub.app.domain.repository.HomeRepository
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
