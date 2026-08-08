#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="$PROJECT_ROOT/Config/Shared-Base.xcconfig"

# shellcheck source=scripts/common.sh
source "$SCRIPT_DIR/common.sh"

assert_primary_worktree

VERSION="$(get_version)"
BUILD_STAMP="$(get_build_stamp)"
SOURCE_REVISION="$(get_source_revision)"
LOCAL_BUILD_ROOT="$PROJECT_ROOT/.build-local"
DERIVED_DATA="$LOCAL_BUILD_ROOT/DerivedData/macOS"
SOURCE_PACKAGES="$LOCAL_BUILD_ROOT/SourcePackages"
PACKAGE_ROOT="$LOCAL_BUILD_ROOT/package-macos"
DIST_DIR="$PROJECT_ROOT/dist"
DMG_PATH="$DIST_DIR/Gallager-$VERSION-zengjice.dmg"
APP_PATH="$DERIVED_DATA/Build/Products/Release/Gallager.app"
SIGNING_IDENTITY="$(find_apple_development_identity)"

[ -n "$SIGNING_IDENTITY" ] || log_error 'No Apple Development signing identity is available.'

mkdir -p "$DERIVED_DATA" "$SOURCE_PACKAGES" "$DIST_DIR"

log_info "Building Gallager $VERSION from $PROJECT_ROOT"
/usr/bin/xcodebuild \
    -workspace "$PROJECT_ROOT/ClaudeSpy.xcworkspace" \
    -scheme ClaudeSpyServer \
    -configuration Release \
    -destination 'platform=macOS,arch=arm64' \
    -derivedDataPath "$DERIVED_DATA" \
    -clonedSourcePackagesDirPath "$SOURCE_PACKAGES" \
    -disablePackageRepositoryCache \
    -skipMacroValidation \
    -skipPackagePluginValidation \
    GALLAGER_BUILD_STAMP="$BUILD_STAMP" \
    GALLAGER_SOURCE_REVISION="$SOURCE_REVISION" \
    CODE_SIGN_STYLE=Manual \
    CODE_SIGN_IDENTITY="$SIGNING_IDENTITY" \
    OTHER_CODE_SIGN_FLAGS=--timestamp=none \
    build

[ -d "$APP_PATH" ] || log_error "Build did not produce $APP_PATH"
/usr/bin/codesign --verify --deep --strict "$APP_PATH" \
    || log_error 'Built app signature is invalid.'

[ "$(/usr/libexec/PlistBuddy -c 'Print :GallagerBuildStamp' "$APP_PATH/Contents/Info.plist")" = "$BUILD_STAMP" ] \
    || log_error 'Built app is missing the expected build stamp.'
[ "$(/usr/libexec/PlistBuddy -c 'Print :GallagerSourceRevision' "$APP_PATH/Contents/Info.plist")" = "$SOURCE_REVISION" ] \
    || log_error 'Built app is missing the expected source revision.'

if [ -e "$PACKAGE_ROOT" ]; then
    /bin/rm -rf -- "$PACKAGE_ROOT"
fi
mkdir -p "$PACKAGE_ROOT/root"
/usr/bin/ditto "$APP_PATH" "$PACKAGE_ROOT/root/Gallager.app"
/bin/ln -s /Applications "$PACKAGE_ROOT/root/Applications"

if [ -e "$DMG_PATH" ]; then
    /bin/rm -f -- "$DMG_PATH"
fi
/usr/bin/hdiutil create \
    -volname Gallager \
    -srcfolder "$PACKAGE_ROOT/root" \
    -format UDZO \
    -ov \
    "$DMG_PATH"
/usr/bin/hdiutil verify "$DMG_PATH" >/dev/null

log_success "DMG: $DMG_PATH"
log_success "Build: $VERSION ($(get_build_number)) · $BUILD_STAMP · $SOURCE_REVISION"
