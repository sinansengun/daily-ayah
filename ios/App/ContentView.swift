import SwiftUI
import UIKit

struct ContentView: View {
    @State private var ayah: DailyAyah?
    @State private var history: [DailyAyah] = []
    @State private var selectedIndex = 0
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

    private var displayedAyah: DailyAyah? {
        guard history.indices.contains(selectedIndex) else {
            return ayah
        }

        return history[selectedIndex]
    }

    private var canShowPreviousDay: Bool {
        selectedIndex < history.count - 1
    }

    private var canShowNextDay: Bool {
        selectedIndex > 0
    }

    var body: some View {
        NavigationStack {
            Group {
                if isLoading && ayah == nil {
                    ProgressView()
                        .controlSize(.large)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
                } else {
                    VStack(spacing: 0) {
                        dayPager

                        if let displayedAyah {
                            dayNavigation(for: displayedAyah)
                                .padding(.top, 8)
                                .padding(.bottom, 16)
                        }
                    }
                }
            }
        }
        .task {
            await refresh()
        }
    }

    @ViewBuilder
    private var dayPager: some View {
        if history.isEmpty {
            ScrollView {
                Text("Henuz icerik yok")
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 20)
                    .padding(.top, 30)
            }
        } else {
            TabView(selection: $selectedIndex) {
                ForEach(Array(history.enumerated()), id: \.element.publishedDateTR) { index, item in
                    ScrollView {
                        contentList(for: item)
                            .frame(maxWidth: .infinity, alignment: .topLeading)
                            .padding(.horizontal, 20)
                            .padding(.top, 30)
                    }
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .animation(.easeInOut(duration: 0.24), value: selectedIndex)
        }
    }

    @ViewBuilder
    private func contentList(for ayah: DailyAyah) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            contentCard(title: "Günün Ayeti", text: ayah.text, reference: ayah.reference)

            if let hadithText = ayah.hadithText, !hadithText.isEmpty {
                contentCard(title: "Günün Hadisi", text: hadithText, reference: ayah.hadithReference)
            }

            if let duaText = ayah.duaText, !duaText.isEmpty {
                contentCard(title: "Günün Duası", text: duaText, reference: ayah.duaReference)
            }

            if let errorMessage {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func dayNavigation(for ayah: DailyAyah) -> some View {
        VStack(spacing: 8) {
            ZStack {
                if selectedIndex > 0 {
                    Button("Bugüne dön") {
                        showToday()
                    }
                    .font(.footnote)
                    .buttonStyle(.plain)
                }
            }
            .frame(height: 18)
            .animation(nil, value: selectedIndex)

            HStack(spacing: 16) {
                Button {
                    showPreviousDay()
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.headline)
                        .frame(width: 36, height: 32)
                }
                .disabled(!canShowPreviousDay)

                Text(formattedDate(from: ayah.publishedDateTR))
                    .font(.footnote.weight(.bold))
                    .foregroundStyle(.secondary)
                    .frame(minWidth: 96)

                Button {
                    showNextDay()
                } label: {
                    Image(systemName: "chevron.right")
                        .font(.headline)
                        .frame(width: 36, height: 32)
                }
                .disabled(!canShowNextDay)
            }
            .buttonStyle(.plain)
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
                    HStack(spacing: 4) {
                        Text(reference)
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.primary)

                        Spacer(minLength: 8)

                        Button {
                            UIPasteboard.general.string = shareText(title: title, text: text, reference: reference)
                        } label: {
                            Image(systemName: "doc.on.doc")
                                .font(.subheadline)
                                .frame(width: 24, height: 28)
                        }
                        .accessibilityLabel("Kopyala")

                        ShareLink(item: shareText(title: title, text: text, reference: reference)) {
                            Image(systemName: "square.and.arrow.up")
                                .font(.subheadline)
                                .frame(width: 24, height: 28)
                        }
                        .accessibilityLabel("Paylaş")
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(.secondary)
                    .padding(.top, 6)
                }
            }
            .padding(.leading, 14)
        }
    }

    private func shareText(title: String, text: String, reference: String) -> String {
        """
        \(title)

        \(text)

        \(reference)
        """
    }

    @MainActor
    private func refresh() async {
        isLoading = true
        defer { isLoading = false }

        let result = await repository.refreshNowAndReloadWidget()
        var loadedHistory = await repository.loadHistory(days: 15)

        if let result {
            ayah = result
            if !loadedHistory.contains(where: { $0.publishedDateTR == result.publishedDateTR }) {
                loadedHistory.insert(result, at: 0)
            }

            history = Array(loadedHistory.prefix(15))
            selectedIndex = 0
            errorMessage = nil
        } else {
            history = Array(loadedHistory.prefix(15))
            selectedIndex = history.isEmpty ? 0 : min(selectedIndex, history.count - 1)
            errorMessage = "Veri alinamadi. Lutfen tekrar deneyin."
        }
    }

    private func showPreviousDay() {
        guard canShowPreviousDay else { return }
        withAnimation(.easeInOut(duration: 0.24)) {
            selectedIndex += 1
        }
    }

    private func showNextDay() {
        guard canShowNextDay else { return }
        withAnimation(.easeInOut(duration: 0.24)) {
            selectedIndex -= 1
        }
    }

    private func showToday() {
        guard selectedIndex != 0 else { return }
        withAnimation(.easeInOut(duration: 0.24)) {
            selectedIndex = 0
        }
    }

    private func formattedDate(from rawValue: String) -> String {
        guard let date = Self.inputDateFormatter.date(from: rawValue) else {
            return rawValue
        }

        return Self.outputDateFormatter.string(from: date)
    }
}
