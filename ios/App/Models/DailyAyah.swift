import Foundation

struct DailyAyah: Codable, Equatable {
    let text: String
    let reference: String
    let surahNumber: Int?
    let ayahNumber: Int?
    let hadithText: String?
    let hadithReference: String?
    let duaText: String?
    let duaReference: String?
    let source: String
    let publishedDateTR: String
    let fetchedAt: String
    let hash: String
    let isStale: Bool

    init(
        text: String,
        reference: String,
        surahNumber: Int? = nil,
        ayahNumber: Int? = nil,
        hadithText: String? = nil,
        hadithReference: String? = nil,
        duaText: String? = nil,
        duaReference: String? = nil,
        source: String,
        publishedDateTR: String,
        fetchedAt: String,
        hash: String,
        isStale: Bool
    ) {
        self.text = text
        self.reference = reference
        self.surahNumber = surahNumber
        self.ayahNumber = ayahNumber
        self.hadithText = hadithText
        self.hadithReference = hadithReference
        self.duaText = duaText
        self.duaReference = duaReference
        self.source = source
        self.publishedDateTR = publishedDateTR
        self.fetchedAt = fetchedAt
        self.hash = hash
        self.isStale = isStale
    }
}

struct DailyAyahHistoryItem: Codable, Equatable {
    let text: String
    let reference: String
    let surahNumber: Int?
    let ayahNumber: Int?
    let hadithText: String?
    let hadithReference: String?
    let duaText: String?
    let duaReference: String?
    let source: String
    let publishedDateTR: String
    let fetchedAt: String
    let hash: String
}

extension DailyAyah {
    init(historyItem: DailyAyahHistoryItem) {
        self.init(
            text: historyItem.text,
            reference: historyItem.reference,
            surahNumber: historyItem.surahNumber,
            ayahNumber: historyItem.ayahNumber,
            hadithText: historyItem.hadithText,
            hadithReference: historyItem.hadithReference,
            duaText: historyItem.duaText,
            duaReference: historyItem.duaReference,
            source: historyItem.source,
            publishedDateTR: historyItem.publishedDateTR,
            fetchedAt: historyItem.fetchedAt,
            hash: historyItem.hash,
            isStale: false
        )
    }
}

struct DailyAyahHistoryResponse: Codable, Equatable {
    let days: Int
    let items: [DailyAyahHistoryItem]
}

struct HealthResponse: Codable, Equatable {
    let status: String
    let now: String
    let hasData: Bool
    let lastFetchedAt: String?
}

struct TafsirAyah: Codable, Equatable {
    let surahNumber: Int
    let surahName: String
    let totalAyahCount: Int
    let mushafOrder: Int?
    let nuzulOrder: Int?
    let aboutText: String?
    let ayahNumber: Int
    let ayahRangeStart: Int
    let ayahRangeEnd: Int
    let arabicText: String?
    let mealText: String
    let tafsirText: String
    let sourceReference: String?
    let sourceUrl: String
}
