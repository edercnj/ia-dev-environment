#!/usr/bin/env bash
# scripts/setup-hooks.sh — story-0057-0007 (EPIC-0057).
#
# One-time installer for the project-level git hooks under .githooks/.
# Idempotent: re-running is safe and confirms the hooksPath is set.
#
# Usage:
#   scripts/setup-hooks.sh           # install
#   scripts/setup-hooks.sh --status  # check current configuration
#   scripts/setup-hooks.sh --uninstall

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

usage() {
    cat <<EOF
usage: $(basename "$0") [--status|--uninstall|--help]

  (no args)   install: git config core.hooksPath .githooks
  --status    show current core.hooksPath setting
  --uninstall reset core.hooksPath to default
EOF
    exit 0
}

case "${1:-install}" in
    install|--install)
        if [[ ! -d ".githooks" ]]; then
            echo "error: .githooks/ directory not found in repo root" >&2
            exit 1
        fi
        git config core.hooksPath .githooks
        chmod +x .githooks/* 2>/dev/null || true
        echo "OK — core.hooksPath set to .githooks"
        echo "    pre-push hook installed."
        echo "    Bypass for a single push: CLAUDE_SMOKE_DISABLED=1 git push"
        ;;
    --status)
        current=$(git config --get core.hooksPath || echo "<default>")
        echo "core.hooksPath = ${current}"
        ;;
    --uninstall)
        git config --unset core.hooksPath || true
        echo "OK — core.hooksPath reset to default."
        ;;
    -h|--help) usage ;;
    *) usage ;;
esac
