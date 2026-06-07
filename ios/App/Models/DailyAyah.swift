import Foundation

struct DailyAyah: Codable, Equatable {
    let text: String
    let reference: String
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
    let hadithText: String?
    let hadithReference: String?
    let duaText: String?
    let duaReference: String?
    let source: String
    let publishedDateTR: String
    let fetchedAt: String
    let hash: String
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
