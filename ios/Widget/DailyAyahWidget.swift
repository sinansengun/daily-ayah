import SwiftUI
import WidgetKit

struct DailyAyahWidget: Widget {
    let kind = "DailyAyahWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: DailyAyahTimelineProvider()) { entry in
            DailyAyahWidgetView(entry: entry)
        }
        .configurationDisplayName("Günün Ayeti")
        .description("Diyanet'ten gunluk ayeti gosterir.")
        .supportedFamilies([
            .systemSmall,
            .systemMedium,
            .accessoryInline,
            .accessoryCircular,
            .accessoryRectangular
        ])
    }
}

@main
struct DailyAyahWidgetBundle: WidgetBundle {
    var body: some Widget {
        DailyAyahWidget()
    }
}
