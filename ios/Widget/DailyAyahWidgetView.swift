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
                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.ayah.text)
                        .font(.system(size: textFontSize, weight: .regular))
                        .lineLimit(textLineLimit)
                        .minimumScaleFactor(0.82)
                        .fixedSize(horizontal: false, vertical: true)

                    Spacer(minLength: 2)

                    Text(entry.ayah.reference)
                        .font(.system(size: referenceFontSize, weight: .semibold))
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .padding(0)
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

    private var textFontSize: CGFloat {
        family == .systemMedium ? 13 : 11
    }

    private var referenceFontSize: CGFloat {
        family == .systemMedium ? 12 : 10
    }

    private var textLineLimit: Int {
        family == .systemMedium ? 6 : 8
    }
}
