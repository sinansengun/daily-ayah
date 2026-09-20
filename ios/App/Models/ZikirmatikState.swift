import Foundation

struct ZikirmatikState: Codable, Equatable {
    let name: String
    let target: Int
    let groupCount: Int
    let count: Int

    static let `default` = ZikirmatikState(name: "Subhanallah", target: 33, groupCount: 1, count: 0)

    static let presets = [
        ZikirmatikPreset(name: "Subhanallah", target: 33),
        ZikirmatikPreset(name: "Elhamdulillah", target: 33),
        ZikirmatikPreset(name: "Allahu Ekber", target: 34),
        ZikirmatikPreset(name: "La ilahe illallah", target: 100),
        ZikirmatikPreset(name: "Estağfirullah", target: 100)
    ]

    init(name: String, target: Int, groupCount: Int = 1, count: Int = 0) {
        self.name = name
        self.target = max(1, target)
        self.groupCount = max(1, groupCount)
        self.count = max(0, count)
    }

    var totalTarget: Int {
        target * groupCount
    }

    var currentGroup: Int {
        min((count / target) + 1, groupCount)
    }

    var countInCurrentGroup: Int {
        count % target
    }

    private enum CodingKeys: String, CodingKey {
        case name
        case target
        case groupCount
        case count
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let name = try container.decode(String.self, forKey: .name)
        let target = try container.decode(Int.self, forKey: .target)
        let groupCount = try container.decodeIfPresent(Int.self, forKey: .groupCount) ?? 1
        let count = try container.decode(Int.self, forKey: .count)

        self.init(name: name, target: target, groupCount: groupCount, count: count)
    }
}

struct ZikirmatikPreset: Identifiable, Equatable {
    let name: String
    let target: Int

    var id: String { name }
}

enum ZikirmatikWidgetKind {
    static let value = "ZikirmatikWidget"
}