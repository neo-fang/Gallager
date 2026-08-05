import ClaudeSpyCommon
import Foundation
import ImageIO
import UniformTypeIdentifiers

/// Converts clipboard image representations into a payload that fits the
/// relay's existing single-frame upload budget.
enum RelayImagePreparer {
    private static let jpegQualities: [CGFloat] = [0.9, 0.8, 0.7, 0.6, 0.5]
    private static let minimumLargestDimension = 320

    static func prepare(_ image: ClipboardImage, maxBytes: Int) -> ClipboardImage? {
        guard maxBytes > 0 else { return nil }

        // Preserve already-efficient clipboard representations byte-for-byte.
        // TIFF is deliberately excluded: even small TIFF payloads are poorly
        // supported by coding agents and almost always have a smaller PNG form.
        if image.format != .tiff, image.data.count <= maxBytes {
            return image
        }

        guard let source = CGImageSourceCreateWithData(image.data as CFData, nil) else {
            return nil
        }

        // Lossless normalization fixes the common case where the pasteboard
        // exposes only a multi-megabyte TIFF for an ordinary screenshot.
        if let png = encodeSource(source, as: .png), png.count <= maxBytes {
            return ClipboardImage(data: png, format: .png)
        }

        guard let (width, height) = pixelSize(of: source) else { return nil }
        var largestDimension = max(width, height)

        while true {
            guard let thumbnail = makeThumbnail(source, maxPixelSize: largestDimension) else {
                return nil
            }

            for quality in jpegQualities {
                if let jpeg = encode(thumbnail, as: .jpeg, quality: quality),
                   jpeg.count <= maxBytes
                {
                    return ClipboardImage(data: jpeg, format: .jpeg)
                }
            }

            guard largestDimension > minimumLargestDimension else { return nil }
            largestDimension = max(
                minimumLargestDimension,
                Int(Double(largestDimension) * 0.8)
            )
        }
    }

    private static func pixelSize(of source: CGImageSource) -> (width: Int, height: Int)? {
        guard
            let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
            let width = properties[kCGImagePropertyPixelWidth] as? Int,
            let height = properties[kCGImagePropertyPixelHeight] as? Int,
            width > 0,
            height > 0
        else { return nil }

        return (width, height)
    }

    private static func makeThumbnail(
        _ source: CGImageSource,
        maxPixelSize: Int
    ) -> CGImage? {
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize,
        ]
        return CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary)
    }

    private static func encodeSource(
        _ source: CGImageSource,
        as type: UTType
    ) -> Data? {
        guard let image = CGImageSourceCreateImageAtIndex(source, 0, nil) else { return nil }
        return encode(image, as: type)
    }

    private static func encode(
        _ image: CGImage,
        as type: UTType,
        quality: CGFloat? = nil
    ) -> Data? {
        let output = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(
            output,
            type.identifier as CFString,
            1,
            nil
        ) else { return nil }

        var properties: [CFString: Any] = [:]
        if let quality {
            properties[kCGImageDestinationLossyCompressionQuality] = quality
        }
        CGImageDestinationAddImage(destination, image, properties as CFDictionary)
        guard CGImageDestinationFinalize(destination) else { return nil }
        return output as Data
    }
}
