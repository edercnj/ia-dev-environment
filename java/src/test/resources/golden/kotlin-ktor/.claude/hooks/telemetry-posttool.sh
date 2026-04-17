#!/usr/bin/env bash
# telemetry-posttool.sh — Claude Code `PostToolUse` hook.
#
# Reads the matching start timestamp written by telemetry-pretool.sh, computes
# the duration in milliseconds, and emits a `tool.call` event enriched with
# `durationMs`, `tool`, and `status`.
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
    echo "telemetry-posttool: jq not found" >&2
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

if [[ -z "${HOOK_PAYLOAD}" ]]; then
    exit 0
fi

SESSION_ID="$(printf '%s' "${HOOK_PAYLOAD}" \
    | jq -r '.session_id // empty' 2>/dev/null)"
if [[ -z "${SESSION_ID}" ]]; then
    SESSION_ID="${CLAUDE_SESSION_ID:-session-$(date +%s)}"
fi

TOOL_NAME="$(printf '%s' "${HOOK_PAYLOAD}" \
    | jq -r '.tool_name // empty' 2>/dev/null)"
TOOL_USE_ID="$(printf '%s' "${HOOK_PAYLOAD}" \
    | jq -r '.tool_use_id // .tool_name // empty' 2>/dev/null)"
if [[ -z "${TOOL_USE_ID}" ]]; then
    exit 0
fi
# is_error is the boolean carried by PostToolUse payloads when a tool errors.
IS_ERROR="$(printf '%s' "${HOOK_PAYLOAD}" \
    | jq -r '.tool_response.is_error // false' 2>/dev/null)"

STATUS="ok"
if [[ "${IS_ERROR}" == "true" ]]; then
    STATUS="failed"
fi

# Read back the start timestamp if present.
TMP_ROOT="${TMPDIR:-/tmp}"
TMP_TELEMETRY="${TMP_ROOT%/}/claude-telemetry"
SAFE_ID="$(printf '%s' "${TOOL_USE_ID}" | tr -c 'A-Za-z0-9._-' '_')"
START_FILE="${TMP_TELEMETRY}/${SAFE_ID}.start"

now_ms() {
    local ms
    ms="$(date +%s%3N 2>/dev/null)"
    if [[ -n "${ms}" && "${ms}" =~ ^[0-9]+$ ]]; then
        printf '%s' "${ms}"
        return 0
    fi
    if command -v perl >/dev/null 2>&1; then
        ms="$(perl -MTime::HiRes=time -e 'printf "%d", time*1000' \
            2>/dev/null)"
        if [[ -n "${ms}" && "${ms}" =~ ^[0-9]+$ ]]; then
            printf '%s' "${ms}"
            return 0
        fi
    fi
    if command -v python3 >/dev/null 2>&1; then
        ms="$(python3 -c 'import time; print(int(time.time()*1000))' \
            2>/dev/null)"
        if [[ -n "${ms}" && "${ms}" =~ ^[0-9]+$ ]]; then
            printf '%s' "${ms}"
            return 0
        fi
    fi
    printf '%s000' "$(date +%s)"
}

DURATION_MS=""
if [[ -f "${START_FILE}" ]]; then
    START_MS="$(awk -F'\t' 'NR==1{print $1}' "${START_FILE}" 2>/dev/null)"
    END_MS="$(now_ms)"
    if [[ -n "${START_MS}" && "${START_MS}" =~ ^[0-9]+$ ]]; then
        DURATION_MS=$(( END_MS - START_MS ))
        # Clamp to non-negative (RULE-001 schema: durationMs minimum 0).
        if (( DURATION_MS < 0 )); then
            DURATION_MS=0
        fi
    fi
    # Cleanup per-call temp file.
    rm -f "${START_FILE}" 2>/dev/null
fi

resolve_context

# Build the enriched event. Start from build_event's minimal shape, then fold
# in tool/durationMs/status.
BASE="$(build_event "${SESSION_ID}" "tool.call")"
if [[ -z "${BASE}" ]]; then
    echo "telemetry-posttool: build_event produced empty output" >&2
    exit 0
fi

if [[ -n "${DURATION_MS}" ]]; then
    EVENT="$(printf '%s' "${BASE}" \
        | jq --arg tool "${TOOL_NAME}" \
             --arg status "${STATUS}" \
             --argjson dur "${DURATION_MS}" \
             '. + {tool: $tool, status: $status, durationMs: $dur}')"
else
    EVENT="$(printf '%s' "${BASE}" \
        | jq --arg tool "${TOOL_NAME}" \
             --arg status "${STATUS}" \
             '. + {tool: $tool, status: $status}')"
fi

if [[ -z "${EVENT}" ]]; then
    echo "telemetry-posttool: jq assembly failed" >&2
    exit 0
fi

printf '%s' "${EVENT}" | "${HOOK_DIR}/telemetry-emit.sh" || true

exit 0
