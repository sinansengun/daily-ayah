import Foundation
import WidgetKit

struct DailyAyahEntry: TimelineEntry {
    let date: Date
    let ayah: DailyAyah
    let loadedFromCache: Bool
}

struct DailyAyahTimelineProvider: TimelineProvider {
    private let repository: DailyAyahProviding

    init(repository: DailyAyahProviding = DailyAyahRepository()) {
        self.repository = repository
    }

    func placeholder(in context: Context) -> DailyAyahEntry {
        DailyAyahEntry(
            date: Date(),
            ayah: DailyAyah(
                text: "Allah'tan bağışlama dile.",
                reference: "Nisa, 4/106",
                source: "Diyanet Isleri Baskanligi",
                publishedDateTR: "2026-06-06",
                fetchedAt: Date().ISO8601Format(),
                hash: "placeholder",
                isStale: true
            ),
            loadedFromCache: true
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (DailyAyahEntry) -> Void) {
        Task {
            let entry = await loadEntry() ?? placeholder(in: context)
            completion(entry)
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<DailyAyahEntry>) -> Void) {
        Task {
            let entry = await loadEntry() ?? placeholder(in: context)
            let nextReload = Self.nextReloadDateTR(from: Date())
            completion(Timeline(entries: [entry], policy: .after(nextReload)))
        }
    }

    private func loadEntry() async -> DailyAyahEntry? {
        guard let ayah = await repository.loadPreferredAyah() else { return nil }

        return DailyAyahEntry(
            date: Date(),
            ayah: ayah,
            loadedFromCache: ayah.isStale
        )
    }

    private static func nextReloadDateTR(from now: Date) -> Date {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Europe/Istanbul") ?? .current

        let nowTR = now
        let startOfToday = calendar.startOfDay(for: nowTR)
        guard let tomorrow = calendar.date(byAdding: .day, value: 1, to: startOfToday),
              let scheduled = calendar.date(byAdding: .minute, value: 2, to: tomorrow) else {
            return now.addingTimeInterval(60 * 60)
        }

        return scheduled
    }
}
