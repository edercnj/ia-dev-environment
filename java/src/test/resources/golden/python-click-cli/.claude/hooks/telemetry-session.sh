#!/usr/bin/env bash
# telemetry-session.sh — Claude Code `SessionStart` hook.
#
# Reads the Claude hook payload from stdin, extracts sessionId, and emits a
# `session.start` telemetry event via telemetry-emit.sh.
#
# Fail-open per RULE-004: any error is logged to stderr; exit code is 0.

set +e
set -u

# Feature flag opt-out (RULE-006) — short-circuit before any work.
if [[ "${CLAUDE_TELEMETRY_DISABLED:-0}" == "1" ]]; then
    cat >/dev/null 2>&1
    exit 0
fi

# Resolve own directory to source telemetry-lib.sh and find telemetry-emit.sh.
HOOK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=telemetry-lib.sh
# shellcheck disable=SC1091
source "${HOOK_DIR}/telemetry-lib.sh"

# jq is required for every hook.
if ! command -v jq >/dev/null 2>&1; then
    echo "telemetry-session: jq not found" >&2
    cat >/dev/null 2>&1
    exit 0
fi

# Read Claude's hook payload (bounded by timeout when available).
if command -v timeout >/dev/null 2>&1; then
    HOOK_PAYLOAD="$(timeout 5 cat 2>/dev/null)"
elif command -v gtimeout >/dev/null 2>&1; then
    HOOK_PAYLOAD="$(gtimeout 5 cat 2>/dev/null)"
else
    HOOK_PAYLOAD="$(cat 2>/dev/null)"
fi

SESSION_ID="$(printf '%s' "${HOOK_PAYLOAD:-}" \
    | jq -r '.session_id // empty' 2>/dev/null)"

# Fallback to env var (CLAUDE_SESSION_ID is set by Claude Code in some modes)
# and finally to a synthesized id so the event still validates (sessionId is
# required by the schema).
if [[ -z "${SESSION_ID}" ]]; then
    SESSION_ID="${CLAUDE_SESSION_ID:-session-$(date +%s)}"
fi

resolve_context

EVENT="$(build_event "${SESSION_ID}" "session.start")"
if [[ -z "${EVENT}" ]]; then
    echo "telemetry-session: build_event produced empty output" >&2
    exit 0
fi

printf '%s' "${EVENT}" | "${HOOK_DIR}/telemetry-emit.sh" || true

exit 0
