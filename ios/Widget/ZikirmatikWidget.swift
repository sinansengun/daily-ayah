import SwiftUI
import WidgetKit

struct ZikirmatikWidget: Widget {
    let kind = ZikirmatikWidgetKind.value

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ZikirmatikTimelineProvider()) { entry in
            ZikirmatikWidgetView(entry: entry)
        }
        .configurationDisplayName("Zikirmatik")
        .description("Aktif zikir sayınızı ve hedefinizi gösterir.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}