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
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if isLoading {
                        ProgressView("Yukleniyor...")
                    }

                    if let ayah {
                        contentCard(title: "Gunun Ayeti", text: ayah.text, reference: ayah.reference)

                        if let hadithText = ayah.hadithText, !hadithText.isEmpty {
                            contentCard(title: "Gunun Hadisi", text: hadithText, reference: ayah.hadithReference)
                        }

                        if let duaText = ayah.duaText, !duaText.isEmpty {
                            contentCard(title: "Gunun Duasi", text: duaText, reference: ayah.duaReference)
                        }

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
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding()
            .navigationTitle("Gunun Icerikleri")
        }
        .task {
            await refresh()
        }
    }

    @ViewBuilder
    private func contentCard(title: String, text: String, reference: String?) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)

            Text(text)
                .font(.body)

            if let reference, !reference.isEmpty {
                Text(reference)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
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
