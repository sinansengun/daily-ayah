package com.cufica.dailyayah.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyAyahEntity::class, CacheReferenceEntity::class, TafsirAyahEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DailyAyahDatabase : RoomDatabase() {
    abstract fun dailyAyahDao(): DailyAyahDao
}