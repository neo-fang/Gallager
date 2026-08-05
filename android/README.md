# Gallager for Android

This directory contains the native Android companion for Gallager. It speaks the
same relay, pairing, WebSocket, and end-to-end-encryption protocol as the existing
Mac and iOS apps; the relay and Mac host do not require Android-specific changes.

## Included

- six-letter pairing through the hosted or a self-hosted relay;
- X25519 key agreement, HKDF-SHA256, and ChaCha20-Poly1305 wire compatibility;
- private-key persistence encrypted by Android Keystore;
- reconnecting WebSocket viewer connection and encrypted peer-version handshake;
- live session list with agent state, project, and Git-branch metadata;
- live ANSI/VT terminal stream with foreground/background colors and text styles;
- remote UTF-8 input plus Escape, Ctrl-C, Tab, arrows, Backspace, and Enter;
- create tmux sessions, create windows, split panes, and close windows/sessions;
- local unpairing plus relay-side pair deletion.

The Android app currently does **not** implement FCM push notifications, the
mouse protocol, structured agent response forms, host project discovery,
file/Git browsers, image upload, or Google Play distribution. These are
follow-up work; the core pairing and terminal-control path is functional
without them.

## Requirements

- Android 8.0 (API 26) or newer;
- JDK 17;
- Android SDK Platform 35 and Build Tools 35.0.0;
- a Mac running Gallager 2.0 or newer.

## Build

Open this `android` directory in Android Studio, or use the checked-in wrapper:

```sh
cd android
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected Android device with:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Pair with a Mac

1. On the Mac, open Gallager **Settings → Remote Access** and generate a code.
2. Open Gallager on Android and enter the six-letter code.
3. Keep the default `wss://relay.gallager.app`, or enter the same self-hosted
   relay URL configured on the Mac.
4. Select a session to start its encrypted terminal stream.

The Android private X25519 key is encrypted with an AES-GCM key held by Android
Keystore. Terminal frames and commands are encrypted end to end; the relay only
sees the outer `encrypted` envelope.

## Protocol compatibility tests

Android unit tests cover key agreement, encryption, protocol JSON, terminal enum
decoding, and transcript handling. The Swift networking test target also contains
`AndroidProtocolCompatibilityTests.swift`, which decodes representative Android
pairing, registration, encrypted, and command frames with Gallager's production
Swift models.
