package com.cufica.dailyayah.data.remote

import com.cufica.dailyayah.data.model.DailyAyah
import com.cufica.dailyayah.data.model.DailyAyahHistoryResponse
import com.cufica.dailyayah.data.model.TafsirAyah
import com.cufica.dailyayah.data.model.PrayerTimes
import com.cufica.dailyayah.data.model.PrayerTimesCitiesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface DailyAyahApi {
    @GET("daily-ayah")
    suspend fun fetchDailyAyah(
        @Header("If-None-Match") etag: String? = null
    ): Response<DailyAyah>

    @GET("daily-ayah/history")
    suspend fun fetchHistory(
        @Query("days") days: Int
    ): Response<DailyAyahHistoryResponse>

    @GET("tafsir/{surahNumber}/{ayahNumber}")
    suspend fun fetchTafsir(
        @Path("surahNumber") surahNumber: Int,
        @Path("ayahNumber") ayahNumber: Int
    ): Response<TafsirAyah>

    @GET("prayer-times")
    suspend fun fetchPrayerTimes(@Query("city") city: String): Response<PrayerTimes>

    @GET("prayer-times/cities")
    suspend fun fetchPrayerTimesCities(): Response<PrayerTimesCitiesResponse>
}