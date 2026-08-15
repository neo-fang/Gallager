#if os(macOS)
    import AppKit
    import ClaudeSpyCommon
    import Testing
    @testable import ClaudeSpyServerFeature

    @Suite("Relay Image Preparer")
    struct RelayImagePreparerTests {
        @Test("Small efficient images pass through unchanged")
        func smallImagesPassThrough() throws {
            let bitmap = makeBitmap(width: 32, height: 32, noisy: false)

            for (type, format) in [
                (NSBitmapImageRep.FileType.png, ImageFormat.png),
                (.jpeg, .jpeg),
            ] {
                let data = try #require(bitmap.representation(using: type, properties: [:]))
                let original = ClipboardImage(data: data, format: format)

                let prepared = RelayImagePreparer.prepare(original, maxBytes: data.count)

                #expect(prepared == original)
            }
        }

        @Test("TIFF clipboard data is normalized to PNG")
        func tiffIsNormalized() throws {
            let bitmap = makeBitmap(width: 128, height: 96, noisy: false)
            let tiff = try #require(bitmap.representation(using: .tiff, properties: [:]))
            let prepared = try #require(
                RelayImagePreparer.prepare(
                    ClipboardImage(data: tiff, format: .tiff),
                    maxBytes: 100 * 1_024
                )
            )

            #expect(prepared.format == .png)
            #expect(prepared.data.count <= 100 * 1_024)
            #expect(NSImage(data: prepared.data) != nil)
        }

        @Test("Oversized PNG is compressed below the relay limit")
        func oversizedPNGIsCompressed() throws {
            let bitmap = makeBitmap(width: 512, height: 512, noisy: true)
            let png = try #require(bitmap.representation(using: .png, properties: [:]))
            let maxBytes = 64 * 1_024
            #expect(png.count > maxBytes)

            let prepared = try #require(
                RelayImagePreparer.prepare(
                    ClipboardImage(data: png, format: .png),
                    maxBytes: maxBytes
                )
            )

            #expect(prepared.format == .jpeg)
            #expect(prepared.data.count <= maxBytes)
            #expect(NSImage(data: prepared.data) != nil)
        }

        @Test("Invalid oversized image is rejected")
        func invalidImageIsRejected() {
            let invalid = ClipboardImage(data: Data(repeating: 0xFF, count: 1_024), format: .png)

            #expect(RelayImagePreparer.prepare(invalid, maxBytes: 512) == nil)
        }

        private func makeBitmap(
            width: Int,
            height: Int,
            noisy: Bool
        ) -> NSBitmapImageRep {
            let bitmap = NSBitmapImageRep(
                bitmapDataPlanes: nil,
                pixelsWide: width,
                pixelsHigh: height,
                bitsPerSample: 8,
                samplesPerPixel: 4,
                hasAlpha: true,
                isPlanar: false,
                colorSpaceName: .deviceRGB,
                bytesPerRow: width * 4,
                bitsPerPixel: 32
            )!
            let bytes = bitmap.bitmapData!
            var randomState: UInt32 = 0x1234_5678

            for offset in stride(from: 0, to: width * height * 4, by: 4) {
                if noisy {
                    randomState = 1_664_525 &* randomState &+ 1_013_904_223
                    bytes[offset] = UInt8(truncatingIfNeeded: randomState >> 24)
                    randomState = 1_664_525 &* randomState &+ 1_013_904_223
                    bytes[offset + 1] = UInt8(truncatingIfNeeded: randomState >> 24)
                    randomState = 1_664_525 &* randomState &+ 1_013_904_223
                    bytes[offset + 2] = UInt8(truncatingIfNeeded: randomState >> 24)
                } else {
                    bytes[offset] = 40
                    bytes[offset + 1] = 120
                    bytes[offset + 2] = 200
                }
                bytes[offset + 3] = 255
            }

            return bitmap
        }
    }
#endif
