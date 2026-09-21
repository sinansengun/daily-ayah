#!/usr/bin/env swift

import AppKit
import Foundation

struct IconSize {
    let filename: String
    let pixels: Int
}

let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let outputDirectory = root.appendingPathComponent("ios/App/Assets.xcassets/AppIcon.appiconset")
let sizes = [
    IconSize(filename: "icon-1024.png", pixels: 1024),
    IconSize(filename: "icon-20@2x.png", pixels: 40),
    IconSize(filename: "icon-20@3x.png", pixels: 60),
    IconSize(filename: "icon-29@2x.png", pixels: 58),
    IconSize(filename: "icon-29@3x.png", pixels: 87),
    IconSize(filename: "icon-40@2x.png", pixels: 80),
    IconSize(filename: "icon-40@3x.png", pixels: 120),
    IconSize(filename: "icon-60@2x.png", pixels: 120),
    IconSize(filename: "icon-60@3x.png", pixels: 180),
    IconSize(filename: "icon-76@1x.png", pixels: 76),
    IconSize(filename: "icon-76@2x.png", pixels: 152),
    IconSize(filename: "icon-83.5@2x.png", pixels: 167)
]

func color(_ hex: UInt32) -> NSColor {
    NSColor(
        red: CGFloat((hex >> 16) & 0xff) / 255,
        green: CGFloat((hex >> 8) & 0xff) / 255,
        blue: CGFloat(hex & 0xff) / 255,
        alpha: 1
    )
}

func drawIcon(size: Int) -> Data? {
    let canvas = CGFloat(size)
    let scale = canvas / 1024
    func point(_ x: CGFloat, _ y: CGFloat) -> NSPoint {
        NSPoint(x: x * scale, y: y * scale)
    }
    guard let representation = NSBitmapImageRep(
        bitmapDataPlanes: nil,
        pixelsWide: size,
        pixelsHigh: size,
        bitsPerSample: 8,
        samplesPerPixel: 4,
        hasAlpha: true,
        isPlanar: false,
        colorSpaceName: .deviceRGB,
        bytesPerRow: 0,
        bitsPerPixel: 0
    ), let context = NSGraphicsContext(bitmapImageRep: representation) else { return nil }
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = context

    color(0x146B52).setFill()
    NSBezierPath(rect: NSRect(x: 0, y: 0, width: canvas, height: canvas)).fill()

    color(0x0E5C45).setFill()
    NSBezierPath(ovalIn: NSRect(x: -180 * scale, y: -330 * scale, width: 1380 * scale, height: 900 * scale)).fill()

    color(0xD8A43D).setFill()
    NSBezierPath(ovalIn: NSRect(x: 350 * scale, y: 542 * scale, width: 324 * scale, height: 324 * scale)).fill()
    color(0x146B52).setFill()
    NSBezierPath(ovalIn: NSRect(x: 480 * scale, y: 542 * scale, width: 324 * scale, height: 324 * scale)).fill()

    color(0x0A4938).withAlphaComponent(0.45).setFill()
    let shadow = NSBezierPath()
    shadow.move(to: NSPoint(x: 512 * scale, y: 174 * scale))
    shadow.curve(to: NSPoint(x: 190 * scale, y: 196 * scale), controlPoint1: NSPoint(x: 392 * scale, y: 238 * scale), controlPoint2: NSPoint(x: 278 * scale, y: 232 * scale))
    shadow.line(to: NSPoint(x: 190 * scale, y: 484 * scale))
    shadow.curve(to: NSPoint(x: 512 * scale, y: 414 * scale), controlPoint1: NSPoint(x: 302 * scale, y: 520 * scale), controlPoint2: NSPoint(x: 424 * scale, y: 488 * scale))
    shadow.curve(to: NSPoint(x: 834 * scale, y: 484 * scale), controlPoint1: NSPoint(x: 600 * scale, y: 488 * scale), controlPoint2: NSPoint(x: 722 * scale, y: 520 * scale))
    shadow.line(to: NSPoint(x: 834 * scale, y: 196 * scale))
    shadow.curve(to: NSPoint(x: 512 * scale, y: 174 * scale), controlPoint1: NSPoint(x: 746 * scale, y: 232 * scale), controlPoint2: NSPoint(x: 632 * scale, y: 238 * scale))
    shadow.fill()

    color(0xFCFCF8).setFill()
    let leftPage = NSBezierPath()
    leftPage.move(to: NSPoint(x: 512 * scale, y: 204 * scale))
    leftPage.curve(to: NSPoint(x: 210 * scale, y: 228 * scale), controlPoint1: NSPoint(x: 416 * scale, y: 268 * scale), controlPoint2: NSPoint(x: 308 * scale, y: 270 * scale))
    leftPage.line(to: NSPoint(x: 210 * scale, y: 498 * scale))
    leftPage.curve(to: NSPoint(x: 512 * scale, y: 430 * scale), controlPoint1: NSPoint(x: 318 * scale, y: 528 * scale), controlPoint2: NSPoint(x: 428 * scale, y: 496 * scale))
    leftPage.close()
    leftPage.fill()

    let rightPage = NSBezierPath()
    rightPage.move(to: NSPoint(x: 512 * scale, y: 204 * scale))
    rightPage.curve(to: NSPoint(x: 814 * scale, y: 228 * scale), controlPoint1: NSPoint(x: 608 * scale, y: 268 * scale), controlPoint2: NSPoint(x: 716 * scale, y: 270 * scale))
    rightPage.line(to: NSPoint(x: 814 * scale, y: 498 * scale))
    rightPage.curve(to: NSPoint(x: 512 * scale, y: 430 * scale), controlPoint1: NSPoint(x: 706 * scale, y: 528 * scale), controlPoint2: NSPoint(x: 596 * scale, y: 496 * scale))
    rightPage.close()
    rightPage.fill()

    color(0xD8A43D).setStroke()
    let spine = NSBezierPath()
    spine.lineWidth = max(2, 14 * scale)
    spine.lineCapStyle = .round
    spine.move(to: NSPoint(x: 512 * scale, y: 212 * scale))
    spine.line(to: NSPoint(x: 512 * scale, y: 428 * scale))
    spine.stroke()

    color(0x146B52).withAlphaComponent(0.65).setStroke()
    for offset: CGFloat in [0, 46] {
        let leftLine = NSBezierPath()
        leftLine.lineWidth = max(1, 9 * scale)
        leftLine.lineCapStyle = .round
        leftLine.move(to: point(278 + offset, 390 - offset / 3))
        leftLine.curve(
            to: point(446 + offset / 3, 364 - offset / 4),
            controlPoint1: point(334 + offset, 404 - offset / 3),
            controlPoint2: point(396, 390 - offset / 4)
        )
        leftLine.stroke()

        let rightLine = NSBezierPath()
        rightLine.lineWidth = max(1, 9 * scale)
        rightLine.lineCapStyle = .round
        rightLine.move(to: point(746 - offset, 390 - offset / 3))
        rightLine.curve(
            to: point(578 - offset / 3, 364 - offset / 4),
            controlPoint1: point(690 - offset, 404 - offset / 3),
            controlPoint2: point(628, 390 - offset / 4)
        )
        rightLine.stroke()
    }

    NSGraphicsContext.restoreGraphicsState()
    return representation.representation(using: .png, properties: [:])
}

for icon in sizes {
    guard let data = drawIcon(size: icon.pixels) else {
        fputs("Unable to render \(icon.filename)\n", stderr)
        exit(1)
    }
    try data.write(to: outputDirectory.appendingPathComponent(icon.filename))
}

print("Generated \(sizes.count) app icon files.")