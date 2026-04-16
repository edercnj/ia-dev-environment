#!/usr/bin/env bash
# telemetry-stop.sh — Claude Code `Stop` hook.
#
# Emits a `session.end` telemetry event and cleans the per-session temp
# directory used by the PreTool/PostTool pair.
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
    echo "telemetry-stop: jq not found" >&2
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

EVENT="$(build_event "${SESSION_ID}" "session.end")"
if [[ -n "${EVENT}" ]]; then
    printf '%s' "${EVENT}" | "${HOOK_DIR}/telemetry-emit.sh" || true
fi

# Cleanup the per-session temp dir written by telemetry-pretool.sh.
TMP_ROOT="${TMPDIR:-/tmp}"
TMP_TELEMETRY="${TMP_ROOT%/}/claude-telemetry"
if [[ -d "${TMP_TELEMETRY}" ]]; then
    # Remove only files (no recursive force) — defensive.
    rm -f "${TMP_TELEMETRY}"/*.start 2>/dev/null
    rmdir "${TMP_TELEMETRY}" 2>/dev/null
fi

exit 0
