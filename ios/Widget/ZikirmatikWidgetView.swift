import SwiftUI
import WidgetKit

struct ZikirmatikWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: ZikirmatikEntry

    private var progress: Double {
        min(Double(entry.state.count) / Double(entry.state.target), 1)
    }

    var body: some View {
        HStack(spacing: 12) {
            Gauge(value: progress) {
                EmptyView()
            } currentValueLabel: {
                Text("\(entry.state.count)")
                    .font(.title3.bold().monospacedDigit())
            }
            .gaugeStyle(.accessoryCircularCapacity)
            .tint(Color.accentColor)
            .frame(width: 58, height: 58)

            if family == .systemMedium {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Zikirmatik")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                    Text(entry.state.name)
                        .font(.headline)
                        .lineLimit(2)
                    Text("Hedef: \(entry.state.totalTarget)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 0)
            }
        }
        .widgetURL(URL(string: "dailyayah://zikirmatik"))
        .containerBackground(for: .widget) {
            Color(.systemBackground)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Zikirmatik, \(entry.state.name), \(entry.state.count) / \(entry.state.totalTarget)")
    }
}