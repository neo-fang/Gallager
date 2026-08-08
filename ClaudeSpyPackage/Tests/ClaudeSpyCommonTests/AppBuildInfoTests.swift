import Testing
@testable import ClaudeSpyCommon

@Suite("App build information")
struct AppBuildInfoTests {
    @Test("Displays version, build, timestamp, and source revision")
    func completeBuildIdentity() {
        let info = AppBuildInfo(infoDictionary: [
            "CFBundleShortVersionString": "2.7",
            "CFBundleVersion": "40",
            AppBuildInfo.buildStampKey: "20260808-2040",
            AppBuildInfo.sourceRevisionKey: "22f1add",
        ])

        #expect(info.displayVersion == "2.7 (40) · 20260808-2040 · 22f1add")
    }

    @Test("Falls back to the standard version when build metadata is absent")
    func standardVersionFallback() {
        let info = AppBuildInfo(infoDictionary: [
            "CFBundleShortVersionString": "2.7",
            "CFBundleVersion": "40",
        ])

        #expect(info.displayVersion == "2.7 (40)")
    }

    @Test("Ignores empty build metadata")
    func emptyMetadata() {
        let info = AppBuildInfo(infoDictionary: [
            "CFBundleShortVersionString": "2.7",
            "CFBundleVersion": "40",
            AppBuildInfo.buildStampKey: "  ",
            AppBuildInfo.sourceRevisionKey: "\n",
        ])

        #expect(info.displayVersion == "2.7 (40)")
    }

    @Test("Handles missing standard version fields")
    func missingStandardVersion() {
        #expect(AppBuildInfo(infoDictionary: nil).displayVersion == "Unknown version")
        #expect(
            AppBuildInfo(infoDictionary: ["CFBundleVersion": "40"]).displayVersion
                == "Build 40"
        )
    }
}
