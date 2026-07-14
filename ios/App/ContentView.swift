import SwiftUI
import UIKit

struct ContentView: View {
    @State private var ayah: DailyAyah?
    @State private var history: [DailyAyah] = []
    @State private var selectedIndex = 0
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var selectedTafsir: TafsirRoute?
    @State private var selectedSurah: SurahRoute?

    private static let inputDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    private static let outputDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "tr_TR")
        formatter.dateFormat = "d MMMM"
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
            .background {
                backgroundImage
            }
            .navigationDestination(item: $selectedTafsir) { route in
                TafsirDetailView(route: route, repository: repository)
            }
            .navigationDestination(item: $selectedSurah) { route in
                SurahDetailView(route: route, repository: repository)
            }
        }
        .task {
            await refresh()
        }
    }

    private var backgroundImage: some View {
        ZStack {
            Image("AppBackground")
                .resizable()
                .scaledToFill()
                .ignoresSafeArea()
                .opacity(0.28)

            Color(.systemBackground)
                .opacity(0.76)
                .ignoresSafeArea()
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
            contentCard(
                title: "Günün Ayeti",
                text: ayah.text,
                reference: ayah.reference,
                tafsirRoute: ayah.tafsirRoute,
                surahRoute: ayah.surahRoute
            )

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
                if canShowNextDay {
                    Button {
                        showNextDay()
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.headline)
                            .frame(width: 36, height: 32)
                    }
                } else {
                    Color.clear
                        .frame(width: 36, height: 32)
                }

                Text(formattedDate(from: ayah.publishedDateTR))
                    .font(.headline.weight(.bold))
                    .foregroundStyle(.secondary)
                    .frame(minWidth: 96)

                if canShowPreviousDay {
                    Button {
                        showPreviousDay()
                    } label: {
                        Image(systemName: "chevron.right")
                            .font(.headline)
                            .frame(width: 36, height: 32)
                    }
                } else {
                    Color.clear
                        .frame(width: 36, height: 32)
                }
            }
            .buttonStyle(.plain)
        }
    }

    @ViewBuilder
    private func contentCard(title: String, text: String, reference: String?, tafsirRoute: TafsirRoute? = nil, surahRoute: SurahRoute? = nil) -> some View {
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

                        actionsMenu(title: title, text: text, reference: reference, tafsirRoute: tafsirRoute, surahRoute: surahRoute)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(.secondary)
                    .padding(.top, 6)
                }
            }
            .padding(.leading, 14)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            if let tafsirRoute {
                selectedTafsir = tafsirRoute
            }
        }
    }

    private func actionsMenu(title: String, text: String, reference: String, tafsirRoute: TafsirRoute?, surahRoute: SurahRoute?) -> some View {
        Menu {
            if let tafsirRoute {
                Button {
                    selectedTafsir = tafsirRoute
                } label: {
                    Label("Ayet Tefsiri", systemImage: "book.pages")
                }
            }

            if let surahRoute {
                Button {
                    selectedSurah = surahRoute
                } label: {
                    Label("Sure Bilgisi", systemImage: "list.bullet.rectangle")
                }
            }

            Button {
                UIPasteboard.general.string = shareText(title: title, text: text, reference: reference)
            } label: {
                Label("Kopyala", systemImage: "doc.on.doc")
            }

            ShareLink(item: shareText(title: title, text: text, reference: reference)) {
                Label("Paylaş", systemImage: "square.and.arrow.up")
            }
        } label: {
            Image(systemName: "ellipsis")
                .font(.title3)
                .frame(width: 32, height: 32)
        }
        .accessibilityLabel("Aksiyonlar")
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

private struct TafsirRoute: Identifiable, Hashable {
    let surahNumber: Int
    let ayahNumber: Int
    let reference: String

    var id: String {
        "\(surahNumber)-\(ayahNumber)"
    }
}

private struct SurahRoute: Identifiable, Hashable {
    let surahNumber: Int
    let ayahNumber: Int
    let reference: String

    var id: String {
        "\(surahNumber)-\(ayahNumber)"
    }
}

private extension DailyAyah {
    var tafsirRoute: TafsirRoute? {
        guard let surahNumber, let ayahNumber else { return nil }
        return TafsirRoute(surahNumber: surahNumber, ayahNumber: ayahNumber, reference: reference)
    }

    var surahRoute: SurahRoute? {
        guard let surahNumber, let ayahNumber else { return nil }
        return SurahRoute(surahNumber: surahNumber, ayahNumber: ayahNumber, reference: reference)
    }
}

private struct SurahDetailView: View {
    let route: SurahRoute
    let repository: DailyAyahProviding

    @State private var tafsir: TafsirAyah?
    @State private var isLoading = true
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                if isLoading {
                    ProgressView()
                        .controlSize(.large)
                        .frame(maxWidth: .infinity, minHeight: 240)
                } else if let tafsir {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("\(tafsir.surahName) Suresi")
                            .font(.title.bold())

                        Text("\(tafsir.surahNumber). sure")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.secondary)
                    }

                    VStack(alignment: .leading, spacing: 14) {
                        detailRow(title: "Ayet sayısı", value: "\(tafsir.totalAyahCount)")

                        if let mushafOrder = tafsir.mushafOrder {
                            detailRow(title: "Mushaf sırası", value: "\(mushafOrder)")
                        }

                        if let nuzulOrder = tafsir.nuzulOrder {
                            detailRow(title: "Nüzul sırası", value: "\(nuzulOrder)")
                        }
                    }

                    if let aboutText = tafsir.aboutText, !aboutText.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Hakkında")
                                .font(.headline)

                            Text(aboutText)
                                .font(.body)
                                .lineSpacing(5)
                        }
                    }

                    if let sourceReference = tafsir.sourceReference, !sourceReference.isEmpty {
                        Text(sourceReference)
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.secondary)
                    }
                } else {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(errorMessage ?? "Sure bilgisi henüz hazır değil.")
                            .font(.headline)

                        Button("Tekrar dene") {
                            Task { await load() }
                        }
                        .buttonStyle(.bordered)
                    }
                    .frame(maxWidth: .infinity, minHeight: 240, alignment: .center)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 24)
        }
        .navigationTitle("Sure Bilgisi")
        .navigationBarTitleDisplayMode(.inline)
        .task(id: route.id) {
            await load()
        }
    }

    private func detailRow(title: String, value: String) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title)
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Spacer(minLength: 16)

            Text(value)
                .font(.body.weight(.semibold))
                .multilineTextAlignment(.trailing)
        }
    }

    @MainActor
    private func load() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        tafsir = await repository.loadTafsir(surahNumber: route.surahNumber, ayahNumber: route.ayahNumber)
        if tafsir == nil {
            errorMessage = "Sure bilgisi henüz hazır değil."
        }
    }
}

private struct TafsirDetailView: View {
    let route: TafsirRoute
    let repository: DailyAyahProviding

    @State private var tafsir: TafsirAyah?
    @State private var isLoading = true
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                if isLoading {
                    ProgressView()
                        .controlSize(.large)
                        .frame(maxWidth: .infinity, minHeight: 240)
                } else if let tafsir {
                    header(for: tafsir)

                    if let arabicText = tafsir.arabicText, !arabicText.isEmpty {
                        Text(arabicText)
                            .font(.title2)
                            .multilineTextAlignment(.trailing)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                            .lineSpacing(8)
                    }

                    section(title: "Meal", text: tafsir.mealText)
                    section(title: "Tefsir", text: tafsir.tafsirText)

                    if let sourceReference = tafsir.sourceReference, !sourceReference.isEmpty {
                        Text(sourceReference)
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.secondary)
                    }
                } else {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(errorMessage ?? "Bu ayetin tefsiri henüz hazır değil.")
                            .font(.body)
                            .foregroundStyle(.secondary)

                        Button("Tekrar dene") {
                            Task { await load() }
                        }
                        .buttonStyle(.bordered)
                    }
                    .frame(maxWidth: .infinity, minHeight: 240, alignment: .center)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 24)
        }
        .navigationTitle("Tefsir")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let tafsir {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        UIPasteboard.general.string = shareText(for: tafsir)
                    } label: {
                        Image(systemName: "doc.on.doc")
                    }
                    .accessibilityLabel("Kopyala")

                    ShareLink(item: shareText(for: tafsir)) {
                        Image(systemName: "square.and.arrow.up")
                    }
                    .accessibilityLabel("Paylaş")
                }
            }
        }
        .task(id: route.id) {
            await load()
        }
    }

    private func header(for tafsir: TafsirAyah) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            NavigationLink {
                SurahDetailView(
                    route: SurahRoute(surahNumber: tafsir.surahNumber, ayahNumber: tafsir.ayahNumber, reference: route.reference),
                    repository: repository
                )
            } label: {
                HStack(spacing: 6) {
                    Text("\(tafsir.surahName) Suresi")
                        .font(.title.bold())

                    Image(systemName: "chevron.right")
                        .font(.headline.weight(.semibold))
                }
            }
            .buttonStyle(.plain)

            Text(ayahTitle(for: tafsir))
                .font(.headline)
                .padding(.top, 8)
        }
    }

    private func section(title: String, text: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)

            Text(text)
                .font(.body)
                .lineSpacing(5)
        }
    }

    @MainActor
    private func load() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        tafsir = await repository.loadTafsir(surahNumber: route.surahNumber, ayahNumber: route.ayahNumber)
        if tafsir == nil {
            errorMessage = "Bu ayetin tefsiri henüz hazır değil."
        }
    }

    private func ayahTitle(for tafsir: TafsirAyah) -> String {
        if tafsir.ayahRangeStart != tafsir.ayahRangeEnd {
            return "\(tafsir.ayahRangeStart)-\(tafsir.ayahRangeEnd). Ayetler"
        }
        return "\(tafsir.ayahNumber). Ayet"
    }

    private func shareText(for tafsir: TafsirAyah) -> String {
        """
        \(tafsir.surahName) Suresi \(ayahTitle(for: tafsir))

        \(tafsir.mealText)

        \(tafsir.tafsirText)

        \(tafsir.sourceReference ?? tafsir.sourceUrl)
        """
    }
}
