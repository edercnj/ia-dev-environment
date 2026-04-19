#!/usr/bin/env bash
# telemetry-subagent.sh — Claude Code `SubagentStop` hook.
#
# Emits a `subagent.end` telemetry event when a subagent run completes.
#
# Fail-open per RULE-004.

set +e
set -u

if [[ "${CLAUDE_TELEMETRY_DISABLED:-0}" == "1" ]]; then
    cat >/dev/null 2>&1
    exit 0
fi

HOOK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=telemetry-lib.sh
# shellcheck disable=SC1091
source "${HOOK_DIR}/telemetry-lib.sh"

if ! command -v jq >/dev/null 2>&1; then
    echo "telemetry-subagent: jq not found" >&2
    cat >/dev/null 2>&1
    exit 0
fi

if command -v timeout >/dev/null 2>&1; then
    HOOK_PAYLOAD="$(timeout 5 cat 2>/dev/null)"
elif command -v gtimeout >/dev/null 2>&1; then
    HOOK_PAYLOAD="$(gtimeout 5 cat 2>/dev/null)"
else
    HOOK_PAYLOAD="$(cat 2>/dev/null)"
fi

SESSION_ID="$(printf '%s' "${HOOK_PAYLOAD:-}" \
    | jq -r '.session_id // empty' 2>/dev/null)"
if [[ -z "${SESSION_ID}" ]]; then
    SESSION_ID="${CLAUDE_SESSION_ID:-session-$(date +%s)}"
fi

resolve_context

EVENT="$(build_event "${SESSION_ID}" "subagent.end")"
if [[ -z "${EVENT}" ]]; then
    echo "telemetry-subagent: build_event produced empty output" >&2
    exit 0
fi

printf '%s' "${EVENT}" | "${HOOK_DIR}/telemetry-emit.sh" || true

exit 0
