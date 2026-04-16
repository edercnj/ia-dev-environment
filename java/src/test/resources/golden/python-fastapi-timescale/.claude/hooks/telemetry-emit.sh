#!/usr/bin/env bash
# telemetry-emit.sh — helper: stdin (NDJSON) → append to canonical file.
#
# Contract:
#   - Reads ONE telemetry event (JSON object) from stdin per invocation.
#   - Applies minimal regex scrubbing for AWS keys, JWTs and bearer tokens.
#   - Appends a single NDJSON line to the canonical path resolved from
#     the event's epicId (or fallback "unknown").
#   - Fail-open (RULE-004): any error goes to stderr; exit code is always 0
#     except for programmer errors caught by `set -u`.
#
# Environment:
#   CLAUDE_TELEMETRY_DISABLED=1  → suppress 100% of emission (RULE-006).
#   CLAUDE_PROJECT_DIR           → repo root; required to resolve storage path.
#   CLAUDE_TELEMETRY_DIR         → override storage root (used by tests).
#
# Storage (RULE-007):
#   ${CLAUDE_PROJECT_DIR}/plans/epic-XXXX/telemetry/events.ndjson
#   ${CLAUDE_PROJECT_DIR}/plans/unknown/telemetry/events.ndjson   (fallback)

# Fail-open: never abort the session even if a helper fails.
set +e
# Catch programmer errors (undefined vars).
set -u

# ---------------------------------------------------------------------------
# RULE-006 — Feature flag opt-out. Short-circuit before any I/O.
# ---------------------------------------------------------------------------
if [[ "${CLAUDE_TELEMETRY_DISABLED:-0}" == "1" ]]; then
    # Drain stdin so callers that pipe don't observe SIGPIPE.
    cat >/dev/null 2>&1
    exit 0
fi

# ---------------------------------------------------------------------------
# Dependencies. jq is required; if missing, emit a warning and exit 0.
# ---------------------------------------------------------------------------
if ! command -v jq >/dev/null 2>&1; then
    echo "telemetry-emit: jq not found — event dropped" >&2
    cat >/dev/null 2>&1
    exit 0
fi

# ---------------------------------------------------------------------------
# Read the entire event from stdin. Timeout any read at 5s (RULE-004).
# macOS has no `timeout(1)` by default; fall back to direct read.
# ---------------------------------------------------------------------------
EVENT_JSON=""
if command -v timeout >/dev/null 2>&1; then
    EVENT_JSON="$(timeout 5 cat)"
elif command -v gtimeout >/dev/null 2>&1; then
    EVENT_JSON="$(gtimeout 5 cat)"
else
    EVENT_JSON="$(cat)"
fi

if [[ -z "${EVENT_JSON}" ]]; then
    echo "telemetry-emit: empty stdin — nothing to emit" >&2
    exit 0
fi

# ---------------------------------------------------------------------------
# RULE-003 — Zero PII: minimal in-shell scrubbing (AWS, JWT, bearer).
# Full scrubbing is enforced by TelemetryScrubber in story-0040-0005.
# ---------------------------------------------------------------------------
scrub_line() {
    local raw="$1"
    # AWS Access Key IDs.
    raw="$(printf '%s' "${raw}" | sed -E \
        's/AKIA[0-9A-Z]{16}/AKIA***REDACTED***/g')"
    # JWTs (header.payload.signature — the prefix alone is enough to detect).
    raw="$(printf '%s' "${raw}" | sed -E \
        's/eyJ[A-Za-z0-9._-]{10,}/eyJ***REDACTED***/g')"
    # Bearer tokens (common Authorization header shape).
    raw="$(printf '%s' "${raw}" | sed -E \
        's/([Bb]earer )[A-Za-z0-9._~+/\-]+=*/\1***REDACTED***/g')"
    printf '%s' "${raw}"
}

EVENT_JSON="$(scrub_line "${EVENT_JSON}")"

# ---------------------------------------------------------------------------
# Validate the scrubbed payload parses as JSON. Malformed input is dropped.
# ---------------------------------------------------------------------------
if ! printf '%s' "${EVENT_JSON}" | jq -e . >/dev/null 2>&1; then
    echo "telemetry-emit: payload is not valid JSON — dropped" >&2
    exit 0
fi

# ---------------------------------------------------------------------------
# Resolve storage path. epicId ("EPIC-NNNN" or "unknown") maps to
# ${CLAUDE_PROJECT_DIR}/plans/epic-NNNN/telemetry/events.ndjson.
# ---------------------------------------------------------------------------
PROJECT_DIR="${CLAUDE_TELEMETRY_DIR:-${CLAUDE_PROJECT_DIR:-}}"
if [[ -z "${PROJECT_DIR}" ]]; then
    echo "telemetry-emit: neither CLAUDE_TELEMETRY_DIR nor CLAUDE_PROJECT_DIR is set — event dropped" >&2
    exit 0
fi

EPIC_ID="$(printf '%s' "${EVENT_JSON}" | jq -r '.epicId // "unknown"')"

# Canonical directory name: EPIC-0040 → plans/epic-0040/telemetry/.
# "unknown" stays as plans/unknown/telemetry/.
if [[ "${EPIC_ID}" == unknown ]]; then
    EPIC_DIR_NAME="unknown"
else
    # Strip "EPIC-" prefix, lowercase.
    EPIC_NUM="${EPIC_ID#EPIC-}"
    EPIC_DIR_NAME="epic-${EPIC_NUM}"
fi

TARGET_DIR="${PROJECT_DIR}/plans/${EPIC_DIR_NAME}/telemetry"
TARGET_FILE="${TARGET_DIR}/events.ndjson"

# Best-effort mkdir; fail-open on failure.
mkdir -p "${TARGET_DIR}" 2>/dev/null || {
    echo "telemetry-emit: cannot create ${TARGET_DIR}" >&2
    exit 0
}

# ---------------------------------------------------------------------------
# Serialize the JSON to a single NDJSON line (jq -c removes inner newlines).
# ---------------------------------------------------------------------------
NDJSON_LINE="$(printf '%s' "${EVENT_JSON}" | jq -c .)"
if [[ -z "${NDJSON_LINE}" ]]; then
    echo "telemetry-emit: jq produced empty line — dropped" >&2
    exit 0
fi

# ---------------------------------------------------------------------------
# RULE-002 — Append-only with advisory lock. `flock(1)` is Linux-only and is
# the preferred path in CI; macOS dev machines fall back to an mkdir-based
# lock which is also append-atomic in practice (O_APPEND + single write).
# ---------------------------------------------------------------------------
append_ndjson() {
    local line="$1"
    local file="$2"
    local lockdir="${file}.lock"
    local attempt=0

    if command -v flock >/dev/null 2>&1; then
        # flock(1) path — preferred, 5s timeout.
        (
            flock -w 5 9 || exit 1
            printf '%s\n' "${line}" >&9
        ) 9>>"${file}"
        return $?
    fi

    # mkdir fallback: spin up to 50 × 100ms = 5s.
    while (( attempt < 50 )); do
        if mkdir "${lockdir}" 2>/dev/null; then
            printf '%s\n' "${line}" >>"${file}"
            local rc=$?
            rmdir "${lockdir}" 2>/dev/null
            return "${rc}"
        fi
        attempt=$(( attempt + 1 ))
        sleep 0.1 2>/dev/null || :
    done
    return 1
}

if ! append_ndjson "${NDJSON_LINE}" "${TARGET_FILE}"; then
    echo "telemetry-emit: append failed for ${TARGET_FILE}" >&2
    exit 0
fi

exit 0
