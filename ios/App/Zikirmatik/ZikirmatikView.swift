import SwiftUI
import WidgetKit

struct ZikirmatikView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var state: ZikirmatikState = .default
    @State private var isShowingSettings = false
    @State private var isShowingResetConfirmation = false
    @State private var justCompletedTarget = false

    private let store = SharedDailyAyahStore()

    private var progress: Double {
        Double(state.countInCurrentGroup) / Double(state.target)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                summary

                Spacer(minLength: 26)

                counterDial

                Spacer(minLength: 26)

                controls
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 18)
            .background {
                backgroundImage
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        isShowingSettings = true
                    } label: {
                        Image(systemName: "slider.horizontal.3")
                    }
                    .accessibilityLabel("Zikir ayarları")
                }
            }
        }
        .task {
            state = store.loadZikirmatikState()
        }
        .sheet(isPresented: $isShowingSettings) {
            ZikirmatikSettingsView(state: state) { newState in
                update(newState)
            }
        }
        .alert("Sayacı sıfırla?", isPresented: $isShowingResetConfirmation) {
            Button("Vazgeç", role: .cancel) {}
            Button("Sıfırla", role: .destructive) {
                update(ZikirmatikState(name: state.name, target: state.target, groupCount: state.groupCount))
            }
        } message: {
            Text("\(state.name) sayımı sıfırlanacak.")
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

    private var summary: some View {
        VStack(spacing: 8) {
            Text(state.name)
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)

            Text("Hedef \(state.target)")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            if state.groupCount > 1 {
                Text("Tur \(state.currentGroup) / \(state.groupCount)")
                    .font(.subheadline.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var counterDial: some View {
        GeometryReader { proxy in
            let dialSize = min(proxy.size.width, proxy.size.height)

            Button(action: increment) {
                ZStack {
                    Circle()
                        .stroke(Color.secondary.opacity(0.18), style: StrokeStyle(lineWidth: 1.5, lineCap: .round))

                    Circle()
                        .inset(by: 7)
                        .stroke(Color.secondary.opacity(0.18), style: StrokeStyle(lineWidth: 1.5, lineCap: .round))

                    Circle()
                        .inset(by: 7)
                        .trim(from: 0, to: progress)
                        .stroke(Color.accentColor, style: StrokeStyle(lineWidth: 3, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                        .animation(reduceMotion ? nil : .easeInOut(duration: 0.22), value: progress)

                    VStack(spacing: 8) {
                        Text("\(state.count)")
                            .font(.system(size: dialSize * 0.28, weight: .light, design: .rounded))
                            .monospacedDigit()
                            .contentTransition(.numericText())

                        Text("\(state.countInCurrentGroup) / \(state.target)")
                            .font(.subheadline.monospacedDigit())
                            .foregroundStyle(.secondary)
                    }
                }
                .frame(width: dialSize, height: dialSize)
                .contentShape(Circle())
                .scaleEffect(justCompletedTarget && !reduceMotion ? 1.035 : 1)
                .animation(reduceMotion ? nil : .spring(duration: 0.34, bounce: 0.42), value: justCompletedTarget)
            }
            .buttonStyle(.plain)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            .accessibilityLabel("Sayacı artır")
            .accessibilityValue("\(state.name), \(state.count) / \(state.target)")
            .accessibilityHint("Her dokunuş sayacı bir artırır")
            .accessibilityAction(named: "Geri al") {
                decrement()
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .frame(maxWidth: 360)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
    }

    private var controls: some View {
        HStack(spacing: 28) {
            Button(action: decrement) {
                Image(systemName: "arrow.uturn.backward")
                    .font(.title3.weight(.semibold))
                    .frame(width: 52, height: 52)
            }
            .buttonStyle(.bordered)
            .clipShape(Circle())
            .disabled(state.count == 0)
            .accessibilityLabel("Geri al")

            Button {
                isShowingResetConfirmation = true
            } label: {
                Image(systemName: "arrow.counterclockwise")
                    .font(.title3.weight(.semibold))
                    .frame(width: 52, height: 52)
            }
            .buttonStyle(.bordered)
            .clipShape(Circle())
            .disabled(state.count == 0)
            .accessibilityLabel("Sayacı sıfırla")
        }
    }

    private func increment() {
        let newCount = state.count + 1
        update(ZikirmatikState(name: state.name, target: state.target, groupCount: state.groupCount, count: newCount))

        if newCount == state.totalTarget {
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            justCompletedTarget = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.38) {
                justCompletedTarget = false
            }
        } else {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        }
    }

    private func decrement() {
        guard state.count > 0 else { return }
        update(ZikirmatikState(name: state.name, target: state.target, groupCount: state.groupCount, count: state.count - 1))
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }

    private func update(_ newState: ZikirmatikState) {
        state = newState
        store.saveZikirmatikState(newState)
        WidgetCenter.shared.reloadTimelines(ofKind: ZikirmatikWidgetKind.value)
    }
}

private struct ZikirmatikSettingsView: View {
    @Environment(\.dismiss) private var dismiss

    let onSave: (ZikirmatikState) -> Void

    @State private var name: String
    @State private var targetText: String
    @State private var groupCountText: String

    init(state: ZikirmatikState, onSave: @escaping (ZikirmatikState) -> Void) {
        self.onSave = onSave
        _name = State(initialValue: state.name)
        _targetText = State(initialValue: String(state.target))
        _groupCountText = State(initialValue: String(state.groupCount))
    }

    private var trimmedName: String {
        name.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var target: Int? {
        Int(targetText)
    }

    private var groupCount: Int? {
        Int(groupCountText)
    }

    private var canSave: Bool {
        !trimmedName.isEmpty && (target ?? 0) > 0 && (groupCount ?? 0) > 0
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Hazır zikirler") {
                    ForEach(ZikirmatikState.presets) { preset in
                        Button {
                            name = preset.name
                            targetText = String(preset.target)
                            groupCountText = "1"
                        } label: {
                            HStack {
                                Text(preset.name)
                                Spacer()
                                Text("\(preset.target)")
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                Section("Zikir") {
                    TextField("Zikir adı", text: $name)
                    LabeledContent("Hedef") {
                        TextField("33", text: $targetText)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                    LabeledContent("Tur sayısı") {
                        TextField("1", text: $groupCountText)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                }
            }
            .navigationTitle("Zikir ayarları")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Vazgeç") {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("Kaydet") {
                        guard let target, let groupCount else { return }
                        onSave(ZikirmatikState(name: trimmedName, target: target, groupCount: groupCount))
                        dismiss()
                    }
                    .disabled(!canSave)
                }
            }
        }
    }
}