package com.cufica.dailyayah.data

import com.cufica.dailyayah.data.local.CacheReferenceEntity
import com.cufica.dailyayah.data.local.DailyAyahDao
import com.cufica.dailyayah.data.local.toEntity
import com.cufica.dailyayah.data.local.toModel
import com.cufica.dailyayah.data.model.DailyAyah
import com.cufica.dailyayah.data.model.TafsirAyah
import com.cufica.dailyayah.data.remote.DailyAyahClient
import com.cufica.dailyayah.data.remote.NetworkResult
import javax.inject.Inject
import javax.inject.Singleton

interface DailyAyahRepository {
    suspend fun loadPreferredAyah(): DailyAyah?
    suspend fun refreshNow(): DailyAyah?
    suspend fun loadHistory(days: Int): List<DailyAyah>
    suspend fun loadTafsir(surahNumber: Int, ayahNumber: Int): TafsirAyah?
}

@Singleton
class DefaultDailyAyahRepository @Inject constructor(
    private val client: DailyAyahClient,
    private val dao: DailyAyahDao
) : DailyAyahRepository {
    override suspend fun loadPreferredAyah(): DailyAyah? {
        val etag = dao.cacheReference(CURRENT_KEY)?.etag
        return when (val result = client.fetchDailyAyah(etag)) {
            is NetworkResult.Success -> saveCurrent(result.value, result.etag)
            NetworkResult.NotModified -> loadCurrent() ?: loadLastSuccessful()
            else -> loadCurrent() ?: loadLastSuccessful()
        }
    }

    override suspend fun refreshNow(): DailyAyah? = loadPreferredAyah()

    override suspend fun loadHistory(days: Int): List<DailyAyah> {
        val normalizedDays = days.coerceIn(1, 30)
        return when (val result = client.fetchHistory(normalizedDays)) {
            is NetworkResult.Success -> {
                val items = result.value.items
                dao.upsertHistory(items.map { item ->
                    item.toEntity(
                        id = "$HISTORY_PREFIX${item.publishedDateTR}",
                        recordType = DailyAyahDao.HISTORY
                    )
                })
                items.take(DailyAyahDao.MAX_HISTORY_ITEMS)
            }
            else -> dao.history(normalizedDays).map { it.toModel() }
        }
    }

    override suspend fun loadTafsir(surahNumber: Int, ayahNumber: Int): TafsirAyah? {
        val key = "$surahNumber-$ayahNumber"
        dao.tafsir(key)?.let { return it.toModel() }

        return when (val result = client.fetchTafsir(surahNumber, ayahNumber)) {
            is NetworkResult.Success -> result.value.also { dao.upsertTafsir(it.toEntity()) }
            else -> null
        }
    }

    private suspend fun saveCurrent(ayah: DailyAyah, etag: String?): DailyAyah {
        dao.upsertDailyAyah(ayah.toEntity(CURRENT_KEY, DailyAyahDao.CURRENT))
        dao.upsertReference(CacheReferenceEntity(CURRENT_KEY, ayah.publishedDateTR, etag))

        if (!ayah.isStale) {
            dao.upsertDailyAyah(ayah.toEntity(LAST_SUCCESSFUL_KEY, DailyAyahDao.LAST_SUCCESSFUL))
        }

        return ayah
    }

    private suspend fun loadCurrent(): DailyAyah? = dao.dailyAyah(CURRENT_KEY)?.toModel()

    private suspend fun loadLastSuccessful(): DailyAyah? = dao.dailyAyah(LAST_SUCCESSFUL_KEY)?.toModel()

    private companion object {
        const val CURRENT_KEY = "current"
        const val LAST_SUCCESSFUL_KEY = "last_successful"
        const val HISTORY_PREFIX = "history-"
    }
}