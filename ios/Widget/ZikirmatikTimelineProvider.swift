import Foundation
import WidgetKit

struct ZikirmatikEntry: TimelineEntry {
    let date: Date
    let state: ZikirmatikState
}

struct ZikirmatikTimelineProvider: TimelineProvider {
    private let store = SharedDailyAyahStore()

    func placeholder(in context: Context) -> ZikirmatikEntry {
        ZikirmatikEntry(date: Date(), state: .default)
    }

    func getSnapshot(in context: Context, completion: @escaping (ZikirmatikEntry) -> Void) {
        completion(loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ZikirmatikEntry>) -> Void) {
        completion(Timeline(entries: [loadEntry()], policy: .never))
    }

    private func loadEntry() -> ZikirmatikEntry {
        ZikirmatikEntry(date: Date(), state: store.loadZikirmatikState())
    }
}