import SwiftUI
import WidgetKit

struct DailyAyahWidgetView: View {
    let entry: DailyAyahEntry

    var body: some View {
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
        .containerBackground(.fill.tertiary, for: .widget)
    }
}
