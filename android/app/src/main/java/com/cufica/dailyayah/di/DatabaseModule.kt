package com.cufica.dailyayah.di

import android.content.Context
import androidx.room.Room
import com.cufica.dailyayah.data.local.DailyAyahDao
import com.cufica.dailyayah.data.local.DailyAyahDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DailyAyahDatabase = Room.databaseBuilder(
        context,
        DailyAyahDatabase::class.java,
        "daily-ayah.db"
    ).build()

    @Provides
    fun provideDailyAyahDao(database: DailyAyahDatabase): DailyAyahDao = database.dailyAyahDao()
}