package com.cufica.dailyayah.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DailyAyahDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyAyah(item: DailyAyahEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReference(reference: CacheReferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTafsir(item: TafsirAyahEntity)

    @Query("SELECT * FROM daily_ayah WHERE id = :id LIMIT 1")
    suspend fun dailyAyah(id: String): DailyAyahEntity?

    @Query("SELECT * FROM cache_reference WHERE `key` = :key LIMIT 1")
    suspend fun cacheReference(key: String): CacheReferenceEntity?

    @Query("SELECT * FROM tafsir_ayah WHERE `key` = :key LIMIT 1")
    suspend fun tafsir(key: String): TafsirAyahEntity?

    @Query("SELECT * FROM daily_ayah WHERE record_type = 'history' ORDER BY published_date_tr DESC LIMIT :limit")
    suspend fun history(limit: Int): List<DailyAyahEntity>

    @Query("DELETE FROM daily_ayah WHERE record_type = 'history' AND id NOT IN (SELECT id FROM daily_ayah WHERE record_type = 'history' ORDER BY published_date_tr DESC LIMIT :limit)")
    suspend fun trimHistory(limit: Int)

    @Transaction
    suspend fun upsertHistory(items: List<DailyAyahEntity>) {
        for (item in items) {
            upsertDailyAyah(item)
        }
        trimHistory(MAX_HISTORY_ITEMS)
    }

    companion object {
        const val MAX_HISTORY_ITEMS = 15
        const val CURRENT = "current"
        const val LAST_SUCCESSFUL = "last_successful"
        const val HISTORY = "history"
    }
}