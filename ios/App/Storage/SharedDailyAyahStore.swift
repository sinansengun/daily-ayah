import Foundation

protocol SharedDailyAyahStoring {
    func saveCurrent(_ ayah: DailyAyah)
    func saveLastSuccessful(_ ayah: DailyAyah)
    func saveHistory(_ history: [DailyAyah])
    func saveTafsir(_ tafsir: TafsirAyah)
    func loadCurrent() -> DailyAyah?
    func loadLastSuccessful() -> DailyAyah?
    func loadHistory() -> [DailyAyah]
    func loadTafsir(surahNumber: Int, ayahNumber: Int) -> TafsirAyah?
}

final class SharedDailyAyahStore: SharedDailyAyahStoring {
    private enum Keys {
        static let currentAyah = "daily_ayah_current"
        static let lastSuccessfulAyah = "daily_ayah_last_successful"
        static let history = "daily_ayah_history"
        static let tafsirPrefix = "daily_ayah_tafsir"
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

    func saveHistory(_ history: [DailyAyah]) {
        guard let data = try? encoder.encode(Array(history.prefix(15))) else { return }
        defaults.set(data, forKey: Keys.history)
    }

    func saveTafsir(_ tafsir: TafsirAyah) {
        guard let data = try? encoder.encode(tafsir) else { return }
        defaults.set(data, forKey: tafsirKey(surahNumber: tafsir.surahNumber, ayahNumber: tafsir.ayahNumber))
    }

    func loadCurrent() -> DailyAyah? {
        decode(forKey: Keys.currentAyah)
    }

    func loadLastSuccessful() -> DailyAyah? {
        decode(forKey: Keys.lastSuccessfulAyah)
    }

    func loadHistory() -> [DailyAyah] {
        guard let data = defaults.data(forKey: Keys.history) else { return [] }
        return (try? decoder.decode([DailyAyah].self, from: data)) ?? []
    }

    func loadTafsir(surahNumber: Int, ayahNumber: Int) -> TafsirAyah? {
        guard let data = defaults.data(forKey: tafsirKey(surahNumber: surahNumber, ayahNumber: ayahNumber)) else { return nil }
        return try? decoder.decode(TafsirAyah.self, from: data)
    }

    private func decode(forKey key: String) -> DailyAyah? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? decoder.decode(DailyAyah.self, from: data)
    }

    private func tafsirKey(surahNumber: Int, ayahNumber: Int) -> String {
        "\(Keys.tafsirPrefix)_\(surahNumber)_\(ayahNumber)"
    }
}
