package com.cufica.dailyayah.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_ayah")
data class DailyAyahEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "record_type")
    val recordType: String,
    @ColumnInfo(name = "published_date_tr")
    val publishedDateTR: String,
    val text: String,
    val reference: String,
    @ColumnInfo(name = "surah_number")
    val surahNumber: Int?,
    @ColumnInfo(name = "ayah_number")
    val ayahNumber: Int?,
    @ColumnInfo(name = "hadith_text")
    val hadithText: String?,
    @ColumnInfo(name = "hadith_reference")
    val hadithReference: String?,
    @ColumnInfo(name = "dua_text")
    val duaText: String?,
    @ColumnInfo(name = "dua_reference")
    val duaReference: String?,
    val source: String,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: String,
    val hash: String,
    @ColumnInfo(name = "is_stale")
    val isStale: Boolean
)

@Entity(tableName = "cache_reference")
data class CacheReferenceEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "published_date_tr")
    val publishedDateTR: String,
    val etag: String?
)

@Entity(tableName = "tafsir_ayah")
data class TafsirAyahEntity(
    @PrimaryKey
    val key: String,
    @ColumnInfo(name = "surah_number")
    val surahNumber: Int,
    @ColumnInfo(name = "surah_name")
    val surahName: String,
    @ColumnInfo(name = "total_ayah_count")
    val totalAyahCount: Int,
    @ColumnInfo(name = "mushaf_order")
    val mushafOrder: Int?,
    @ColumnInfo(name = "nuzul_order")
    val nuzulOrder: Int?,
    @ColumnInfo(name = "about_text")
    val aboutText: String?,
    @ColumnInfo(name = "ayah_number")
    val ayahNumber: Int,
    @ColumnInfo(name = "ayah_range_start")
    val ayahRangeStart: Int,
    @ColumnInfo(name = "ayah_range_end")
    val ayahRangeEnd: Int,
    @ColumnInfo(name = "arabic_text")
    val arabicText: String?,
    @ColumnInfo(name = "meal_text")
    val mealText: String,
    @ColumnInfo(name = "tafsir_text")
    val tafsirText: String,
    @ColumnInfo(name = "source_reference")
    val sourceReference: String?,
    @ColumnInfo(name = "source_url")
    val sourceUrl: String
)