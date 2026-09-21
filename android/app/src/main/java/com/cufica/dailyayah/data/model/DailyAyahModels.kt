package com.cufica.dailyayah.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyAyah(
    val text: String,
    val reference: String,
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null,
    val hadithText: String? = null,
    val hadithReference: String? = null,
    val duaText: String? = null,
    val duaReference: String? = null,
    val source: String,
    val publishedDateTR: String,
    val fetchedAt: String,
    val hash: String,
    val isStale: Boolean = false
)

@Serializable
data class DailyAyahHistoryResponse(
    val days: Int,
    val items: List<DailyAyah>
)

@Serializable
data class TafsirAyah(
    val surahNumber: Int,
    val surahName: String,
    val totalAyahCount: Int,
    val mushafOrder: Int? = null,
    val nuzulOrder: Int? = null,
    val aboutText: String? = null,
    val ayahNumber: Int,
    val ayahRangeStart: Int,
    val ayahRangeEnd: Int,
    val arabicText: String? = null,
    val mealText: String,
    val tafsirText: String,
    val sourceReference: String? = null,
    val sourceUrl: String
)

@Serializable
data class ZikirmatikState(
    val name: String = "Subhanallah",
    val target: Int = 33,
    val groupCount: Int = 1,
    val count: Int = 0
) {
    val totalTarget: Int
        get() = target * groupCount

    val currentGroup: Int
        get() = minOf((count / target) + 1, groupCount)

    val countInCurrentGroup: Int
        get() = count % target
}

@Serializable
data class ZikirProfile(
    val id: String,
    val name: String,
    val target: Int,
    val groupCount: Int,
    val count: Int = 0
) {
    fun toState() = ZikirmatikState(name, target, groupCount, count)
}

@Serializable
data class PrayerTimes(
    val city: String,
    val country: String,
    val date: String,
    val timeZone: String,
    val imsak: String,
    val gunes: String,
    val ogle: String,
    val ikindi: String,
    val aksam: String,
    val yatsi: String,
    val source: String,
    val fetchedAt: String
)

@Serializable
data class PrayerTimesCitiesResponse(val items: List<String>)