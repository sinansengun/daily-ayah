import SwiftUI
import WidgetKit

struct DailyAyahWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: DailyAyahEntry

    var body: some View {
        Group {
            switch family {
            case .accessoryInline:
                Text(entry.ayah.reference)
                    .lineLimit(1)
            case .accessoryCircular:
                VStack(spacing: 2) {
                    Text("Ayet")
                        .font(.caption2)
                    Text(shortReference)
                        .font(.caption2)
                        .lineLimit(1)
                }
            case .accessoryRectangular:
                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.ayah.reference)
                        .font(.caption)
                        .lineLimit(1)
                    Text(entry.ayah.text)
                        .font(.caption2)
                        .lineLimit(2)
                }
            default:
                VStack(alignment: .leading, spacing: 4) {
                    Text(entry.ayah.text)
                        .font(.system(size: 13, weight: .regular))
                        .lineLimit(5)
                        .minimumScaleFactor(0.9)

                    Text(entry.ayah.reference)
                        .font(.system(size: 12, weight: .semibold))
                        .lineLimit(1)
                }
                .padding(8)
            }
        }
        .containerBackground(for: .widget) {
            if family == .systemSmall || family == .systemMedium {
                Color(.tertiarySystemFill)
            } else {
                Color.clear
            }
        }
    }

    private var shortReference: String {
        let parts = entry.ayah.reference.split(separator: ",")
        return parts.first.map(String.init) ?? entry.ayah.reference
    }
}
