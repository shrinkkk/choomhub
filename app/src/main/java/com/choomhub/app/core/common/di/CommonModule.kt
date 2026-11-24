package com.choomhub.app.core.common.di

import com.choomhub.app.core.common.ClockSecondTicker
import com.choomhub.app.core.common.SecondTicker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    /** Injected everywhere "now" matters so time-based logic stays testable. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun provideSecondTicker(clock: Clock): SecondTicker = ClockSecondTicker(clock)
}
