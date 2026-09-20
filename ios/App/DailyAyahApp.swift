import SwiftUI

@main
struct DailyAyahApp: App {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var selectedTab = AppTab.dailyAyah

    var body: some Scene {
        WindowGroup {
            if hasCompletedOnboarding {
                TabView(selection: $selectedTab) {
                    ContentView()
                        .tabItem {
                            Label("Günün Ayeti", systemImage: "book.closed.fill")
                        }
                        .tag(AppTab.dailyAyah)

                    ZikirmatikView()
                        .tabItem {
                            Label("Zikir", systemImage: "circle.grid.3x3.fill")
                        }
                        .tag(AppTab.zikirmatik)
                }
                .onOpenURL { url in
                    if url.host == "zikirmatik" {
                        selectedTab = .zikirmatik
                    }
                }
            } else {
                OnboardingView {
                    hasCompletedOnboarding = true
                }
            }
        }
    }
}

private enum AppTab: Hashable {
    case dailyAyah
    case zikirmatik
}
