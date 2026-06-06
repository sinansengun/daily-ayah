import SwiftUI

struct ContentView: View {
    @State private var ayah: DailyAyah?
    @State private var isLoading = false
    @State private var errorMessage: String?

    private static let inputDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    private static let outputDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "tr_TR")
        formatter.dateFormat = "dd.MM.yyyy"
        return formatter
    }()

    private let repository: DailyAyahProviding = DailyAyahRepository()

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                if isLoading {
                    ProgressView("Yukleniyor...")
                }

                if let ayah {
                    Text(ayah.text)
                        .font(.body)

                    Text(ayah.reference)
                        .font(.headline)

                    Text("Tarih: \(formattedDate(from: ayah.publishedDateTR))")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                } else {
                    Text("Henuz icerik yok")
                        .foregroundStyle(.secondary)
                }

                if let errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }

                Spacer()
            }
            .padding()
            .navigationTitle("Gunun Ayeti")
        }
        .task {
            await refresh()
        }
    }

    @MainActor
    private func refresh() async {
        isLoading = true
        defer { isLoading = false }

        let result = await repository.loadPreferredAyah()

        if let result {
            ayah = result
            errorMessage = nil
        } else {
            errorMessage = "Veri alinamadi. Lutfen tekrar deneyin."
        }
    }

    private func formattedDate(from rawValue: String) -> String {
        guard let date = Self.inputDateFormatter.date(from: rawValue) else {
            return rawValue
        }

        return Self.outputDateFormatter.string(from: date)
    }
}
