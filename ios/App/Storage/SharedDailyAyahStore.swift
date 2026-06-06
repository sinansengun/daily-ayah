import Foundation

protocol SharedDailyAyahStoring {
    func saveCurrent(_ ayah: DailyAyah)
    func saveLastSuccessful(_ ayah: DailyAyah)
    func loadCurrent() -> DailyAyah?
    func loadLastSuccessful() -> DailyAyah?
}

final class SharedDailyAyahStore: SharedDailyAyahStoring {
    private enum Keys {
        static let currentAyah = "daily_ayah_current"
        static let lastSuccessfulAyah = "daily_ayah_last_successful"
    }

    private let defaults: UserDefaults
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(appGroupID: String = AppGroupConfig.identifier) {
        if let shared = UserDefaults(suiteName: appGroupID) {
            self.defaults = shared
        } else {
            self.defaults = .standard
        }
    }

    func saveCurrent(_ ayah: DailyAyah) {
        guard let data = try? encoder.encode(ayah) else { return }
        defaults.set(data, forKey: Keys.currentAyah)
    }

    func saveLastSuccessful(_ ayah: DailyAyah) {
        guard let data = try? encoder.encode(ayah) else { return }
        defaults.set(data, forKey: Keys.lastSuccessfulAyah)
    }

    func loadCurrent() -> DailyAyah? {
        decode(forKey: Keys.currentAyah)
    }

    func loadLastSuccessful() -> DailyAyah? {
        decode(forKey: Keys.lastSuccessfulAyah)
    }

    private func decode(forKey key: String) -> DailyAyah? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? decoder.decode(DailyAyah.self, from: data)
    }
}
