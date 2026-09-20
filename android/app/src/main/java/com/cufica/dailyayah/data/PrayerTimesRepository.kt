package com.cufica.dailyayah.data

import com.cufica.dailyayah.data.model.PrayerTimes
import com.cufica.dailyayah.data.remote.DailyAyahApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimesRepository @Inject constructor(
    private val api: DailyAyahApi
) {
    suspend fun load(city: String): PrayerTimes? = runCatching {
        api.fetchPrayerTimes(city).takeIf { it.isSuccessful }?.body()
    }.getOrNull()

    suspend fun cities(): List<String> = runCatching {
        api.fetchPrayerTimesCities().takeIf { it.isSuccessful }?.body()?.items.orEmpty()
    }.getOrDefault(DefaultCities)

    companion object {
        val DefaultCities = listOf("Istanbul", "Ankara", "Izmir", "Bursa", "Antalya")
    }
}