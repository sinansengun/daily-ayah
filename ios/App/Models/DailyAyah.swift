import Foundation

struct DailyAyah: Codable, Equatable {
    let text: String
    let reference: String
    let source: String
    let publishedDateTR: String
    let fetchedAt: String
    let hash: String
    let isStale: Bool
}

struct DailyAyahHistoryItem: Codable, Equatable {
    let text: String
    let reference: String
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
