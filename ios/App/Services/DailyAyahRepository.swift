import Foundation
#if canImport(WidgetKit)
import WidgetKit
#endif

protocol DailyAyahProviding {
    func loadPreferredAyah() async -> DailyAyah?
    func refreshNowAndReloadWidget() async -> DailyAyah?
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

        notifyWidgetReload()
        return fresh
    }

    private func notifyWidgetReload() {
        #if canImport(WidgetKit)
        if #available(iOS 14.0, *) {
            WidgetCenter.shared.reloadAllTimelines()
        }
        #endif
    }
}
