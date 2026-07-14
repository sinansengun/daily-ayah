import SwiftUI
import UIKit

struct OnboardingView: View {
    let onFinish: () -> Void

    @State private var selectedPage = 0

    private let pages = OnboardingPage.pages

    var body: some View {
        ZStack {
            background

            VStack(spacing: 0) {
                TabView(selection: $selectedPage) {
                    ForEach(Array(pages.enumerated()), id: \.element.id) { index, page in
                        OnboardingPageView(page: page)
                            .tag(index)
                            .padding(.horizontal, 24)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.easeInOut(duration: 0.24), value: selectedPage)

                controls
                    .padding(.horizontal, 24)
                    .padding(.bottom, 24)
            }
        }
    }

    private var background: some View {
        ZStack {
            Image("AppBackground")
                .resizable()
                .scaledToFill()
                .ignoresSafeArea()
                .opacity(0.22)

            LinearGradient(
                colors: [
                    Color(.systemBackground).opacity(0.92),
                    Color(.systemBackground).opacity(0.78),
                    Color(.systemBackground).opacity(0.94)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        }
    }

    private var controls: some View {
        VStack(spacing: 18) {
            HStack(spacing: 8) {
                ForEach(pages.indices, id: \.self) { index in
                    Circle()
                        .fill(index == selectedPage ? Color.primary : Color.secondary.opacity(0.28))
                        .frame(width: 7, height: 7)
                        .scaleEffect(index == selectedPage ? 1.12 : 1)
                }
            }
            .accessibilityHidden(true)

            Button {
                if selectedPage == pages.count - 1 {
                    onFinish()
                } else {
                    selectedPage += 1
                }
            } label: {
                Text(selectedPage == pages.count - 1 ? "Başla" : "Devam")
                    .font(.headline)
                    .frame(minWidth: 132)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.regular)
            .accessibilityLabel(selectedPage == pages.count - 1 ? "Başla" : "Sonraki sayfa")
        }
    }
}

private struct OnboardingPageView: View {
    let page: OnboardingPage

    var body: some View {
        VStack(spacing: 24) {
            Spacer(minLength: 18)

            VStack(spacing: 10) {
                Text(page.title)
                    .font(.title.bold())
                    .multilineTextAlignment(.center)

                Text(page.message)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: 360)
                    .padding(.bottom, page.artworkHeight > 0 ? 8 : 0)

                    if page.artworkHeight > 0 {
                    artwork
                        .frame(maxWidth: .infinity)
                        .frame(height: page.artworkHeight)
                    }

            if !page.steps.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(page.steps, id: \.self) { step in
                        Label(step, systemImage: "checkmark.circle.fill")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .labelStyle(.titleAndIcon)
                    }
                }
                .frame(maxWidth: 360, alignment: .leading)
                .padding(.top, 2)
            }

            Spacer(minLength: 18)
        }
        .accessibilityElement(children: .contain)
    }

    @ViewBuilder
    private var artwork: some View {
        switch page.artwork {
        case .appFlow:
            AppFlowArtwork()
        case .image(let name, let fallbackSystemImage):
            ScreenshotArtwork(assetName: name, fallbackSystemImage: fallbackSystemImage)
        case .ready:
            ReadyArtwork()
        }
    }
}

private struct AppFlowArtwork: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "sun.max.fill")
                    .font(.title2)
                    .foregroundStyle(.yellow)
                    .frame(width: 38, height: 38)
                    .background(Color.yellow.opacity(0.14), in: Circle())

                VStack(alignment: .leading, spacing: 8) {
                    Text("Günün Ayeti")
                        .font(.headline)
                    Text("Eğer size yasaklanan büyük günahlardan kaçınırsanız sizin küçük günahlarınızı örteriz ve sizi değerli bir yere koyarız.")
                        .font(.body)
                        .lineLimit(4)
                    Text("Nisâ, 4/31")
                        .font(.subheadline.bold())
                        .foregroundStyle(.secondary)
                }
            }

            HStack(spacing: 12) {
                Label("Tefsir", systemImage: "book.pages")
                Label("Kopyala", systemImage: "doc.on.doc")
                Label("Paylaş", systemImage: "square.and.arrow.up")
            }
            .font(.caption.weight(.semibold))
            .foregroundStyle(.secondary)
        }
        .padding(22)
        .frame(maxWidth: 360, alignment: .leading)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 26, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 26, style: .continuous)
                .strokeBorder(Color.primary.opacity(0.08))
        }
        .shadow(color: .black.opacity(0.08), radius: 20, y: 10)
        .accessibilityLabel("Günün ayeti, tefsir, kopyalama ve paylaşma özellikleri")
    }
}

private struct ScreenshotArtwork: View {
    let assetName: String
    let fallbackSystemImage: String

    var body: some View {
        Group {
            if let image = UIImage(named: assetName) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
            } else {
                VStack(spacing: 14) {
                    Image(systemName: fallbackSystemImage)
                        .font(.system(size: 54, weight: .semibold))
                    Text(assetName)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 26, style: .continuous))
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .strokeBorder(Color.primary.opacity(0.08))
        }
        .shadow(color: .black.opacity(0.12), radius: 16, y: 8)
        .accessibilityLabel(assetName)
    }
}

private struct ReadyArtwork: View {
    var body: some View {
        ZStack {
            Circle()
                .fill(Color.accentColor.opacity(0.14))
                .frame(width: 190, height: 190)

            VStack(spacing: 14) {
                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: 66, weight: .semibold))
                    .foregroundStyle(Color.accentColor)

                Text("DailyAyah")
                    .font(.title2.bold())
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityLabel("DailyAyah hazır")
    }
}

private struct OnboardingPage: Identifiable {
    enum Artwork {
        case appFlow
        case image(name: String, fallbackSystemImage: String)
        case ready
    }

    let id: String
    let title: String
    let message: String
    let steps: [String]
    let artwork: Artwork
    let artworkHeight: CGFloat

    static let pages: [OnboardingPage] = [
        OnboardingPage(
            id: "daily-flow",
            title: "Günün Ayeti",
            message: "Günün ayetini, referansını ve ilgili tefsir bağlantılarını sade bir akışta takip et.",
            steps: ["Tefsire ve sure bilgisine hızlıca ulaş", "Ayetleri kopyala veya paylaş"],
            artwork: .image(name: "home-ayah", fallbackSystemImage: "book.pages"),
            artworkHeight: 320
        ),
        OnboardingPage(
            id: "home-widget",
            title: "Ana Ekranda Gör",
            message: "DailyAyah widget'ını ana ekranına ekleyerek günün ayetini her an görebilirsin.",
            steps: ["Ana ekrana basılı tut", "+ ile DailyAyah widget'ını ekle"],
            artwork: .image(name: "home-widget", fallbackSystemImage: "apps.iphone"),
            artworkHeight: 320
        ),
        OnboardingPage(
            id: "lock-widget",
            title: "Kilit Ekranında Takip Et",
            message: "Kilit ekranı widget'ı ile telefonu açmadan ayeti ve referansı okuyabilirsin.",
            steps: ["Kilit ekranına basılı tut", "Özelleştir bölümünden DailyAyah'ı seç"],
            artwork: .image(name: "lock-widget", fallbackSystemImage: "lock.iphone"),
            artworkHeight: 320
        ),
        OnboardingPage(
            id: "ready",
            title: "Hazırsın",
            message: "Tutorial'a daha sonra ana ekrandaki yardım simgesinden tekrar ulaşabilirsin.",
            steps: [],
            artwork: .ready,
            artworkHeight: 0
        )
    ]
}
