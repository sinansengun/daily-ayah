import Foundation

enum DailyAyahClientError: LocalizedError {
    case invalidURL
    case invalidResponse
    case httpError(statusCode: Int)
    case decodeFailure

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid API URL"
        case .invalidResponse:
            return "Invalid server response"
        case .httpError(let statusCode):
            return "HTTP error: \(statusCode)"
        case .decodeFailure:
            return "Response decode failed"
        }
    }
}

protocol DailyAyahClientProtocol {
    func fetchDailyAyah() async throws -> DailyAyah
    func fetchHistory(days: Int) async throws -> DailyAyahHistoryResponse
    func fetchHealth() async throws -> HealthResponse
    func fetchTafsir(surahNumber: Int, ayahNumber: Int) async throws -> TafsirAyah
}

final class DailyAyahClient: DailyAyahClientProtocol {
    private let baseURL: URL
    private let session: URLSession
    private let decoder: JSONDecoder

    init(baseURL: URL = APIConfig.baseURL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session
        self.decoder = JSONDecoder()
    }

    func fetchDailyAyah() async throws -> DailyAyah {
        let endpoint = "/daily-ayah"
        return try await sendRequest(path: endpoint)
    }

    func fetchHistory(days: Int) async throws -> DailyAyahHistoryResponse {
        let normalizedDays = min(max(days, 1), 30)
        let endpoint = "/daily-ayah/history?days=\(normalizedDays)"
        return try await sendRequest(path: endpoint)
    }

    func fetchHealth() async throws -> HealthResponse {
        try await sendRequest(path: "/health")
    }

    func fetchTafsir(surahNumber: Int, ayahNumber: Int) async throws -> TafsirAyah {
        try await sendRequest(path: "/tafsir/\(surahNumber)/\(ayahNumber)")
    }

    private func sendRequest<T: Decodable>(path: String) async throws -> T {
        guard let url = URL(string: path, relativeTo: baseURL) else {
            throw DailyAyahClientError.invalidURL
        }

        let (data, response) = try await session.data(from: url)

        guard let http = response as? HTTPURLResponse else {
            throw DailyAyahClientError.invalidResponse
        }

        guard (200...299).contains(http.statusCode) else {
            throw DailyAyahClientError.httpError(statusCode: http.statusCode)
        }

        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw DailyAyahClientError.decodeFailure
        }
    }
}
