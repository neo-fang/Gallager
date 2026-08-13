# CtrlX

CtrlX lets you monitor and control coding-agent sessions running in tmux from
macOS and iOS. It mirrors native terminal sessions instead of wrapping one
specific agent provider, so existing tmux workflows and provider commands stay
available.

## What it does

- Mirrors local and remote tmux sessions, windows, and panes in real time.
- Detects working, completed, permission, question, and plan-approval states.
- Sends terminal input, answers agent prompts, and shows desktop or iOS notifications.
- Provides a file browser, Git workbench, prompt editor, and token/cost telemetry.
- Uses end-to-end encryption; the Relay routes encrypted frames and cannot read terminals.
- Supports additional coding agents through open sidecar plugins.

## Components

| Component | Responsibility | Source |
| --- | --- | --- |
| CtrlX for Mac | tmux mirroring, agent integration, workbench and remote host | `ClaudeSpyPackage/Sources/ClaudeSpyServerFeature` |
| CtrlX Relay | pairing, encrypted WebSocket routing and optional push dispatch | `ClaudeSpyPackage/Sources/ClaudeSpyExternalServer` |
| CtrlX for iOS | remote viewing, input and agent actions | `ClaudeSpyPackage/Sources/ClaudeSpyFeature` |

Internal targets and Swift modules retain their historical `ClaudeSpy*` names.
They are implementation details and deliberately remain stable to reduce
upstream merge risk.

## Build from source

Requirements:

- A recent Xcode with Swift 6.3 or newer.
- macOS 15 or newer.
- tmux installed for Mac terminal sessions.

Open `ClaudeSpy.xcworkspace` and build:

- `ClaudeSpyServer` for the macOS application.
- `ClaudeSpy` for the iOS application.

Build the Relay locally:

```sh
cd ClaudeSpyPackage
cp .env.example .env.local
docker compose up -d
```

Run package tests:

```sh
swift test --package-path ClaudeSpyPackage
```

Release artifacts must be built from the primary worktree using the checked-in
packaging scripts. Local signing overrides and all credentials remain in ignored
configuration files.

## Source corresponding to binaries

Every published CtrlX binary and hosted Relay version must identify an immutable
Git tag and commit containing its complete corresponding source and build scripts.
A release record has this shape:

```text
Release: v3.0.0
Binary: CtrlX-3.0.0.dmg
Source: refs/tags/v3.0.0
Commit: <full Git commit>
License: GNU AGPL-3.0
```

The hosted Relay must expose the same information through `/version` and `/source`.
Links to a moving branch alone are not sufficient corresponding source.

## Origin and license

CtrlX is an independent distribution based on
[Gallager](https://github.com/gpambrozio/Gallager). The distribution baseline is
commit `919c7772928531d4d0bb266bdf275691d361901e` dated 2026-08-14. CtrlX is
maintained by JarvisZeng and is not affiliated with or endorsed by the Gallager
project.

The complete Git history and original copyright notices are retained. CtrlX is
distributed under [GNU AGPL-3.0](LICENSE). If you provide a modified Relay over a
network, you must offer users the complete corresponding source for that running
version. See [NOTICE.md](NOTICE.md), [MODIFICATIONS.md](MODIFICATIONS.md), and
[THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).

## Development plan

The independent-distribution work is tracked in
[`docs/v3.0.0/IMPLEMENTATION_PLAN.md`](docs/v3.0.0/IMPLEMENTATION_PLAN.md).
