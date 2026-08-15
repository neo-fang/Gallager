# CtrlX release process

CtrlX releases bind each binary to an immutable source commit.

1. Update `Config/Shared-Base.xcconfig`, version docs and `MODIFICATIONS.md`.
2. Run all boundary checks, Swift tests, website build and Mac/iOS build checks.
3. Commit from the primary worktree and create `v<version>` at that exact commit.
4. Copy `.env.example` to the selected root environment file and configure the
   signing identity, notary profile and owned download URL.
5. Run the zero-parameter `./scripts/release.sh`.

The script refuses dirty or untagged source, archives and signs `CtrlX.app`,
submits the app and DMG for notarization, generates `CtrlX-<version>.dmg`, a
Sparkle appcast, SHA-256 file and JSON manifest containing the full source
commit and AGPL license.

Sparkle stays disabled in the application until a CtrlX feed URL and EdDSA
public key are supplied in ignored `Config/Local-macOS.xcconfig`. Gallager's
feed, key and domains are never fallback values.

TestFlight/App Store upload is intentionally blocked by `scripts/testflight.sh`
until an AGPL/Apple terms review or copyright-holder exception is documented.
Local signed-device builds remain available through
`scripts/package-local-ios.sh`.
