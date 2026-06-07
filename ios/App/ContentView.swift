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
            Group {
                if isLoading && ayah == nil {
                    ProgressView()
                        .controlSize(.large)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
                } else {
                    VStack(spacing: 0) {
                        ScrollView {
                            VStack(alignment: .leading, spacing: 16) {
                                if let ayah {
                                    contentCard(title: "Günün Ayeti", text: ayah.text, reference: ayah.reference)

                                    if let hadithText = ayah.hadithText, !hadithText.isEmpty {
                                        contentCard(title: "Günün Hadisi", text: hadithText, reference: ayah.hadithReference)
                                    }

                                    if let duaText = ayah.duaText, !duaText.isEmpty {
                                        contentCard(title: "Günün Duası", text: duaText, reference: ayah.duaReference)
                                    }
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

                        if let ayah {
                            Text("Tarih: \(formattedDate(from: ayah.publishedDateTR))")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                                .frame(maxWidth: .infinity, alignment: .center)
                                .padding(.top, 8)
                                .padding(.bottom, 16)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 30)
                }
            }
        }
        .task {
            await refresh()
        }
    }

    @ViewBuilder
    private func contentCard(title: String, text: String, reference: String?) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.largeTitle)
                .bold()

            VStack(alignment: .leading, spacing: 6) {
                Text(text)
                    .font(.body)

                if let reference, !reference.isEmpty {
                    Text(reference)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.black)
                        .padding(.top, 6)
                }
            }
            .padding(.leading, 14)
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
