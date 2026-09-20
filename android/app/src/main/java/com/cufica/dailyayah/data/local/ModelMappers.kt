package com.cufica.dailyayah.data.local

import com.cufica.dailyayah.data.model.DailyAyah
import com.cufica.dailyayah.data.model.TafsirAyah

fun DailyAyah.toEntity(id: String, recordType: String) = DailyAyahEntity(
    id = id,
    recordType = recordType,
    publishedDateTR = publishedDateTR,
    text = text,
    reference = reference,
    surahNumber = surahNumber,
    ayahNumber = ayahNumber,
    hadithText = hadithText,
    hadithReference = hadithReference,
    duaText = duaText,
    duaReference = duaReference,
    source = source,
    fetchedAt = fetchedAt,
    hash = hash,
    isStale = isStale
)

fun DailyAyahEntity.toModel() = DailyAyah(
    text = text,
    reference = reference,
    surahNumber = surahNumber,
    ayahNumber = ayahNumber,
    hadithText = hadithText,
    hadithReference = hadithReference,
    duaText = duaText,
    duaReference = duaReference,
    source = source,
    publishedDateTR = publishedDateTR,
    fetchedAt = fetchedAt,
    hash = hash,
    isStale = isStale
)

fun TafsirAyah.toEntity() = TafsirAyahEntity(
    key = "$surahNumber-$ayahNumber",
    surahNumber = surahNumber,
    surahName = surahName,
    totalAyahCount = totalAyahCount,
    mushafOrder = mushafOrder,
    nuzulOrder = nuzulOrder,
    aboutText = aboutText,
    ayahNumber = ayahNumber,
    ayahRangeStart = ayahRangeStart,
    ayahRangeEnd = ayahRangeEnd,
    arabicText = arabicText,
    mealText = mealText,
    tafsirText = tafsirText,
    sourceReference = sourceReference,
    sourceUrl = sourceUrl
)

fun TafsirAyahEntity.toModel() = TafsirAyah(
    surahNumber = surahNumber,
    surahName = surahName,
    totalAyahCount = totalAyahCount,
    mushafOrder = mushafOrder,
    nuzulOrder = nuzulOrder,
    aboutText = aboutText,
    ayahNumber = ayahNumber,
    ayahRangeStart = ayahRangeStart,
    ayahRangeEnd = ayahRangeEnd,
    arabicText = arabicText,
    mealText = mealText,
    tafsirText = tafsirText,
    sourceReference = sourceReference,
    sourceUrl = sourceUrl
)