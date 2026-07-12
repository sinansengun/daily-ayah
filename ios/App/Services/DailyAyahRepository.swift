import Foundation
#if canImport(WidgetKit)
import WidgetKit
#endif

protocol DailyAyahProviding {
    func loadPreferredAyah() async -> DailyAyah?
    func refreshNowAndReloadWidget() async -> DailyAyah?
    func loadHistory(days: Int) async -> [DailyAyah]
    func loadTafsir(surahNumber: Int, ayahNumber: Int) async -> TafsirAyah?
}

final class DailyAyahRepository: DailyAyahProviding {
    private let client: DailyAyahClientProtocol
    private let store: SharedDailyAyahStoring

    init(client: DailyAyahClientProtocol = DailyAyahClient(), store: SharedDailyAyahStoring = SharedDailyAyahStore()) {
        self.client = client
        self.store = store
    }

    // Source priority: fresh API -> shared cache -> last successful.
    func loadPreferredAyah() async -> DailyAyah? {
        if let fresh = try? await client.fetchDailyAyah() {
            store.saveCurrent(fresh)
            if !fresh.isStale {
                store.saveLastSuccessful(fresh)
            }
            return fresh
        }

        if let cached = store.loadCurrent() {
            return cached
        }

        return store.loadLastSuccessful()
    }

    // Call this from app-side manual refresh actions.
    func refreshNowAndReloadWidget() async -> DailyAyah? {
        guard let fresh = try? await client.fetchDailyAyah() else {
            return await loadPreferredAyah()
        }

        store.saveCurrent(fresh)
        if !fresh.isStale {
            store.saveLastSuccessful(fresh)
        }

        _ = await loadHistory(days: 15)

        notifyWidgetReload()
        return fresh
    }

    func loadHistory(days: Int) async -> [DailyAyah] {
        let normalizedDays = min(max(days, 1), 30)

        if let response = try? await client.fetchHistory(days: normalizedDays) {
            let history = response.items.map(DailyAyah.init(historyItem:))
            store.saveHistory(history)
            return history
        }

        return Array(store.loadHistory().prefix(normalizedDays))
    }

    func loadTafsir(surahNumber: Int, ayahNumber: Int) async -> TafsirAyah? {
        if let cached = store.loadTafsir(surahNumber: surahNumber, ayahNumber: ayahNumber) {
            return cached
        }

        guard let tafsir = try? await client.fetchTafsir(surahNumber: surahNumber, ayahNumber: ayahNumber) else {
            return nil
        }

        store.saveTafsir(tafsir)
        return tafsir
    }

    private func notifyWidgetReload() {
        #if canImport(WidgetKit)
        if #available(iOS 14.0, *) {
            WidgetCenter.shared.reloadAllTimelines()
        }
        #endif
    }
}
