package com.cufica.dailyayah.di

import com.cufica.dailyayah.data.DailyAyahRepository
import com.cufica.dailyayah.data.DefaultDailyAyahRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDailyAyahRepository(
        implementation: DefaultDailyAyahRepository
    ): DailyAyahRepository
}