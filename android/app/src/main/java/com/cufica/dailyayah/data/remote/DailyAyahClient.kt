package com.cufica.dailyayah.data.remote

import com.cufica.dailyayah.data.model.DailyAyah
import com.cufica.dailyayah.data.model.DailyAyahHistoryResponse
import com.cufica.dailyayah.data.model.TafsirAyah
import javax.inject.Inject

interface DailyAyahClient {
    suspend fun fetchDailyAyah(etag: String? = null): NetworkResult<DailyAyah>
    suspend fun fetchHistory(days: Int): NetworkResult<DailyAyahHistoryResponse>
    suspend fun fetchTafsir(surahNumber: Int, ayahNumber: Int): NetworkResult<TafsirAyah>
}

sealed interface NetworkResult<out T> {
    data class Success<T>(val value: T, val etag: String? = null) : NetworkResult<T>
    data object NotModified : NetworkResult<Nothing>
    data object NotFound : NetworkResult<Nothing>
    data class HttpError(val code: Int) : NetworkResult<Nothing>
    data class Failure(val cause: Throwable) : NetworkResult<Nothing>
}

class RetrofitDailyAyahClient @Inject constructor(
    private val api: DailyAyahApi
) : DailyAyahClient {
    override suspend fun fetchDailyAyah(etag: String?): NetworkResult<DailyAyah> =
        request { api.fetchDailyAyah(etag) }

    override suspend fun fetchHistory(days: Int): NetworkResult<DailyAyahHistoryResponse> =
        request { api.fetchHistory(days.coerceIn(1, 30)) }

    override suspend fun fetchTafsir(surahNumber: Int, ayahNumber: Int): NetworkResult<TafsirAyah> =
        request { api.fetchTafsir(surahNumber, ayahNumber) }

    private suspend fun <T> request(call: suspend () -> retrofit2.Response<T>): NetworkResult<T> = try {
        val response = call()
        when {
            response.isSuccessful && response.body() != null -> {
                NetworkResult.Success(response.body()!!, response.headers()["ETag"])
            }
            response.code() == 304 -> NetworkResult.NotModified
            response.code() == 404 -> NetworkResult.NotFound
            else -> NetworkResult.HttpError(response.code())
        }
    } catch (exception: Exception) {
        NetworkResult.Failure(exception)
    }
}