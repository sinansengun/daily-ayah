package com.cufica.dailyayah.di

import com.cufica.dailyayah.data.DailyAyahRepository
import com.cufica.dailyayah.data.PrayerTimesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    fun repository(): DailyAyahRepository
    fun prayerTimesRepository(): PrayerTimesRepository
}