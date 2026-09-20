package com.cufica.dailyayah.di

import com.cufica.dailyayah.data.DailyAyahRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    fun repository(): DailyAyahRepository
}