#!/usr/bin/env bash
# telemetry-pretool.sh — Claude Code `PreToolUse` hook.
#
# Records the start timestamp for the tool call into a per-session temp dir.
# The matching telemetry-posttool.sh reads the timestamp back and computes
# `durationMs` for the emitted `tool.call` event.
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
    echo "telemetry-pretool: jq not found" >&2
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

# Claude Code hook payload fields we consume:
#   session_id      -> session identifier
#   tool_name       -> canonical tool name (Bash, Read, ...)
#   tool_use_id     -> opaque id that correlates Pre/Post (fallback: tool_name)
SESSION_ID="$(printf '%s' "${HOOK_PAYLOAD}" \
    | jq -r '.session_id // empty' 2>/dev/null)"
TOOL_NAME="$(printf '%s' "${HOOK_PAYLOAD}" \
    | jq -r '.tool_name // empty' 2>/dev/null)"
TOOL_USE_ID="$(printf '%s' "${HOOK_PAYLOAD}" \
    | jq -r '.tool_use_id // .tool_name // empty' 2>/dev/null)"

if [[ -z "${TOOL_USE_ID}" ]]; then
    echo "telemetry-pretool: no tool_use_id/tool_name — skipping" >&2
    exit 0
fi

# Write the start timestamp (epoch milliseconds) to $TMPDIR/claude-telemetry/.
TMP_ROOT="${TMPDIR:-/tmp}"
TMP_TELEMETRY="${TMP_ROOT%/}/claude-telemetry"
mkdir -p "${TMP_TELEMETRY}" 2>/dev/null || exit 0

# Epoch-ms resolution. GNU date supports %N; macOS does not — use Python fallback.
now_ms() {
    local ms
    ms="$(date +%s%3N 2>/dev/null)"
    # GNU date: "1729178400123". BSD/macOS: "1729178400%3N" or "...3N".
    if [[ -n "${ms}" && "${ms}" =~ ^[0-9]+$ ]]; then
        printf '%s' "${ms}"
        return 0
    fi
    # Perl fallback — ms precision, portable on macOS and Linux.
    if command -v perl >/dev/null 2>&1; then
        ms="$(perl -MTime::HiRes=time -e 'printf "%d", time*1000' \
            2>/dev/null)"
        if [[ -n "${ms}" && "${ms}" =~ ^[0-9]+$ ]]; then
            printf '%s' "${ms}"
            return 0
        fi
    fi
    # Python fallback.
    if command -v python3 >/dev/null 2>&1; then
        ms="$(python3 -c 'import time; print(int(time.time()*1000))' \
            2>/dev/null)"
        if [[ -n "${ms}" && "${ms}" =~ ^[0-9]+$ ]]; then
            printf '%s' "${ms}"
            return 0
        fi
    fi
    # Last resort: second precision + 000.
    printf '%s000' "$(date +%s)"
}

# File naming: the tool_use_id may contain slashes or other chars. Sanitize.
SAFE_ID="$(printf '%s' "${TOOL_USE_ID}" | tr -c 'A-Za-z0-9._-' '_')"
START_FILE="${TMP_TELEMETRY}/${SAFE_ID}.start"

# One line: "startMs<TAB>toolName<TAB>sessionId"
printf '%s\t%s\t%s\n' "$(now_ms)" "${TOOL_NAME}" "${SESSION_ID}" \
    >"${START_FILE}" 2>/dev/null || {
        echo "telemetry-pretool: cannot write ${START_FILE}" >&2
        exit 0
    }

exit 0
