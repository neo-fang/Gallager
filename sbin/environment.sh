#!/bin/bash

set -euo pipefail

select_ctrlx_environment() {
    local project_root="$1"
    local candidate
    for candidate in .env.local .env.production .env.development .env.test; do
        if [ -f "$project_root/ClaudeSpyPackage/$candidate" ]; then
            CTRLX_ENV_FILE="$candidate"
            export CTRLX_ENV_FILE
            set -a
            # shellcheck disable=SC1090
            source "$project_root/ClaudeSpyPackage/$candidate"
            set +a
            return 0
        fi
    done
    echo "No CtrlX Relay environment file found." >&2
    echo "Run ./sbin/auto-env.sh or copy a ClaudeSpyPackage/.env.*.example file." >&2
    return 1
}
