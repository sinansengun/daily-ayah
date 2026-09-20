package com.cufica.dailyayah.data

import android.content.Context
import com.cufica.dailyayah.data.model.PrayerTimes
import com.cufica.dailyayah.data.remote.DailyAyahApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class PrayerTimesRepository @Inject constructor(
    private val api: DailyAyahApi,
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences("prayer_times_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(city: String): PrayerTimes? {
        val remote = runCatching {
        api.fetchPrayerTimes(city).takeIf { it.isSuccessful }?.body()
        }.getOrNull()
        if (remote != null) {
            preferences.edit().putString(city, json.encodeToString(remote)).apply()
            return remote
        }

        return loadCached(city)
    }

    fun loadCached(city: String): PrayerTimes? = preferences.getString(city, null)?.let { cached ->
        runCatching { json.decodeFromString<PrayerTimes>(cached) }.getOrNull()
    }

    suspend fun cities(): List<String> = runCatching {
        api.fetchPrayerTimesCities().takeIf { it.isSuccessful }?.body()?.items.orEmpty()
    }.getOrDefault(DefaultCities)

    companion object {
        val DefaultCities = listOf("Istanbul", "Ankara", "Izmir", "Bursa", "Antalya")
    }
}