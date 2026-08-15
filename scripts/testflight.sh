#!/bin/bash

set -euo pipefail

cat >&2 <<'MESSAGE'
CtrlX TestFlight publication is intentionally disabled.

The upstream Gallager code is AGPL-3.0, while Apple distribution terms may add
restrictions. A signed local iOS build can be produced with
scripts/package-local-ios.sh, but TestFlight/App Store upload must remain blocked
until the required legal review or copyright-holder exception is documented.

See docs/v3.0.0/ISSUES.md.
MESSAGE
exit 1
