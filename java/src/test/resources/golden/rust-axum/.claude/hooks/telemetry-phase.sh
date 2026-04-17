#!/usr/bin/env bash
# telemetry-phase.sh — helper: emit phase.start / phase.end,
# subagent.start / subagent.end, and tool.call (via mcp-start / mcp-end)
# markers.
#
# Contract (story-0040-0006 §5.1 + story-0040-0007 §5.1 + story-0040-0008 §5.1):
#   telemetry-phase.sh start          SKILL PHASE             (3 args)
#   telemetry-phase.sh end            SKILL PHASE STATUS      (4 args, STATUS
#                                                              is one of
#                                                              ok|failed|skipped)
#   telemetry-phase.sh subagent-start SKILL ROLE              (3 args)
#   telemetry-phase.sh subagent-end   SKILL ROLE STATUS       (4 args)
#   telemetry-phase.sh mcp-start      SKILL MCPMETHOD         (3 args)
#   telemetry-phase.sh mcp-end        SKILL MCPMETHOD STATUS  (4 args)
#
# Behaviour:
#   - Builds a minimal, schema-valid telemetry event (schemaVersion 1.0.0)
#     with the fields required by _TEMPLATE-TELEMETRY-EVENT.json plus the
#     `skill`, `phase` and (for end) `status` attributes. For subagent-*
#     sub-commands the event carries `type=subagent.start|subagent.end` and
#     `metadata.role` (the agent's role, e.g. Architect, QA, Security).
#   - For mcp-* sub-commands the event carries `type=tool.call` with
#     `tool="mcp__atlassian__<method>"`, `metadata.mcpMethod=<method>`, and
#     (on mcp-end) `durationMs` computed from a per-method start file under
#     `$TMPDIR/claude-telemetry/mcp-<method>.start` (epoch millis).
#   - Pipes the serialized JSON to `telemetry-emit.sh`, which appends it to
#     the canonical NDJSON file and enforces scrubbing + advisory locking.
#   - Fail-open (RULE-004): any error (missing helper, invalid args, jq
#     missing, write failure) is logged to stderr; exit code is ALWAYS 0 so
#     an orchestrator skill never aborts because telemetry broke.
#
# Environment:
#   CLAUDE_TELEMETRY_DISABLED=1  → suppress 100% of emission (RULE-006).
#   CLAUDE_PROJECT_DIR           → repo root; required to locate peer helpers.
#   CLAUDE_SESSION_ID            → session id embedded in every event.
#
# This script is intentionally small and self-contained: skills invoke it
# with a single Bash command twice per phase (one start, one end). Overhead
# must stay below the 50ms budget declared in story-0040-0006 §4.

# Fail-open. Callers (skills) must not observe a non-zero exit from us.
set +e
# Catch programmer errors (undefined vars in our own code).
set -u

# ---------------------------------------------------------------------------
# RULE-006 — short-circuit when telemetry is disabled. No side effects.
# ---------------------------------------------------------------------------
if [[ "${CLAUDE_TELEMETRY_DISABLED:-0}" == "1" ]]; then
    exit 0
fi

# ---------------------------------------------------------------------------
# Argument validation. Fail-open: warn on stderr, exit 0.
# ---------------------------------------------------------------------------
KIND="${1:-}"
SKILL_NAME="${2:-}"
# Third arg is PHASE for phase markers, ROLE for subagent markers, MCP_METHOD
# for mcp-* markers. We keep variable aliases so later branches stay readable.
PHASE_NAME="${3:-}"
ROLE_NAME="${3:-}"
MCP_METHOD="${3:-}"
STATUS_ARG="${4:-}"

case "${KIND}" in
    start|end|subagent-start|subagent-end|mcp-start|mcp-end) : ;;
    *)
        echo "telemetry-phase: first arg must be 'start', 'end'," \
             "'subagent-start', 'subagent-end'," \
             "'mcp-start', or 'mcp-end'" >&2
        exit 0
        ;;
esac

if [[ -z "${SKILL_NAME}" ]]; then
    echo "telemetry-phase: missing skill name (arg 2)" >&2
    exit 0
fi

case "${KIND}" in
    start|end)
        if [[ -z "${PHASE_NAME}" ]]; then
            echo "telemetry-phase: missing phase name (arg 3)" >&2
            exit 0
        fi
        # Story contract §5.1: phase name max 64 chars.
        if (( ${#PHASE_NAME} > 64 )); then
            echo "telemetry-phase: phase name exceeds 64 chars —" \
                 "truncated" >&2
            PHASE_NAME="${PHASE_NAME:0:64}"
        fi
        ;;
    subagent-start|subagent-end)
        if [[ -z "${ROLE_NAME}" ]]; then
            echo "telemetry-phase: missing role (arg 3) for ${KIND}" >&2
            exit 0
        fi
        # story-0040-0007 §5.1: role is a short identifier; cap at 64.
        if (( ${#ROLE_NAME} > 64 )); then
            echo "telemetry-phase: role exceeds 64 chars — truncated" >&2
            ROLE_NAME="${ROLE_NAME:0:64}"
        fi
        ;;
    mcp-start|mcp-end)
        if [[ -z "${MCP_METHOD}" ]]; then
            echo "telemetry-phase: missing mcpMethod (arg 3) for ${KIND}" >&2
            exit 0
        fi
        # story-0040-0008 §5.1: mcp method is a short identifier; cap at 64.
        if (( ${#MCP_METHOD} > 64 )); then
            echo "telemetry-phase: mcpMethod exceeds 64 chars — truncated" >&2
            MCP_METHOD="${MCP_METHOD:0:64}"
        fi
        ;;
esac

if [[ "${KIND}" == "end" || "${KIND}" == "subagent-end" \
        || "${KIND}" == "mcp-end" ]]; then
    case "${STATUS_ARG}" in
        ok|failed|skipped) : ;;
        "") STATUS_ARG="ok" ;;
        *)
            echo "telemetry-phase: invalid status '${STATUS_ARG}'," \
                 "defaulting to 'ok'" >&2
            STATUS_ARG="ok"
            ;;
    esac
fi

# ---------------------------------------------------------------------------
# Locate peer helpers. All siblings live next to this script.
# ---------------------------------------------------------------------------
HOOK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EMIT_SCRIPT="${HOOK_DIR}/telemetry-emit.sh"
LIB_SCRIPT="${HOOK_DIR}/telemetry-lib.sh"

# Fail-open when the emit helper is missing — the skill keeps running and the
# operator learns via stderr.
if [[ ! -x "${EMIT_SCRIPT}" && ! -f "${EMIT_SCRIPT}" ]]; then
    echo "telemetry-phase: emit helper not found at ${EMIT_SCRIPT}" >&2
    exit 0
fi

if [[ ! -f "${LIB_SCRIPT}" ]]; then
    echo "telemetry-phase: lib helper not found at ${LIB_SCRIPT}" >&2
    exit 0
fi

# ---------------------------------------------------------------------------
# jq is mandatory (same contract as every other telemetry-*.sh helper).
# ---------------------------------------------------------------------------
if ! command -v jq >/dev/null 2>&1; then
    echo "telemetry-phase: jq not found — event dropped" >&2
    exit 0
fi

# shellcheck source=telemetry-lib.sh
# shellcheck disable=SC1091
source "${LIB_SCRIPT}"

# ---------------------------------------------------------------------------
# Session id resolution. Claude Code injects CLAUDE_SESSION_ID in most modes;
# synthesize a deterministic fallback otherwise so the schema stays valid.
# ---------------------------------------------------------------------------
SESSION_ID="${CLAUDE_SESSION_ID:-}"
if [[ -z "${SESSION_ID}" ]]; then
    SESSION_ID="session-$(date +%s 2>/dev/null || echo 0)"
fi

# ---------------------------------------------------------------------------
# Build the event. Start with the shared base (build_event) and enrich with
# skill/phase/status fields so consumers can aggregate by phase.
# ---------------------------------------------------------------------------
case "${KIND}" in
    start)          EVENT_TYPE="phase.start" ;;
    end)            EVENT_TYPE="phase.end" ;;
    subagent-start) EVENT_TYPE="subagent.start" ;;
    subagent-end)   EVENT_TYPE="subagent.end" ;;
    # MCP sub-commands emit tool.call events (story-0040-0008 §5.2).
    mcp-start|mcp-end) EVENT_TYPE="tool.call" ;;
esac

# ---------------------------------------------------------------------------
# MCP timer handling (story-0040-0008 §5.1 / §5.3).
# mcp-start writes an epoch-millis timestamp to a per-method file under
# $TMPDIR/claude-telemetry/mcp-<method>.start; mcp-end reads & removes it to
# compute durationMs. When the timer file is missing (e.g. skill called
# mcp-end without a mcp-start) the event is still emitted but durationMs is
# omitted — RULE-004 fail-open.
# ---------------------------------------------------------------------------
MCP_DURATION_MS=""
if [[ "${KIND}" == "mcp-start" || "${KIND}" == "mcp-end" ]]; then
    MCP_TIMER_DIR="${TMPDIR:-/tmp}/claude-telemetry"
    # mkdir best-effort — any failure degrades to "no timer", still fail-open.
    mkdir -p "${MCP_TIMER_DIR}" 2>/dev/null
    MCP_TIMER_FILE="${MCP_TIMER_DIR}/mcp-${MCP_METHOD}.start"
fi

if [[ "${KIND}" == "mcp-start" ]]; then
    # Record start in epoch millis. `date +%s%3N` works on GNU coreutils; on
    # BSD / macOS we fall back to seconds * 1000 (the 50ms budget tolerates
    # second-level granularity).
    NOW_MS="$(date +%s%3N 2>/dev/null)"
    if [[ -z "${NOW_MS}" || "${NOW_MS}" == *N* ]]; then
        NOW_MS="$(( $(date +%s 2>/dev/null || echo 0) * 1000 ))"
    fi
    printf '%s' "${NOW_MS}" > "${MCP_TIMER_FILE}" 2>/dev/null
elif [[ "${KIND}" == "mcp-end" ]]; then
    if [[ -f "${MCP_TIMER_FILE}" ]]; then
        START_MS="$(cat "${MCP_TIMER_FILE}" 2>/dev/null)"
        rm -f "${MCP_TIMER_FILE}" 2>/dev/null
        END_MS="$(date +%s%3N 2>/dev/null)"
        if [[ -z "${END_MS}" || "${END_MS}" == *N* ]]; then
            END_MS="$(( $(date +%s 2>/dev/null || echo 0) * 1000 ))"
        fi
        if [[ -n "${START_MS}" && "${START_MS}" =~ ^[0-9]+$ \
                && "${END_MS}" =~ ^[0-9]+$ ]]; then
            DIFF=$(( END_MS - START_MS ))
            if (( DIFF < 0 )); then DIFF=0; fi
            MCP_DURATION_MS="${DIFF}"
        fi
    fi
fi

BASE_EVENT="$(build_event "${SESSION_ID}" "${EVENT_TYPE}" 2>/dev/null)"
if [[ -z "${BASE_EVENT}" ]]; then
    echo "telemetry-phase: build_event produced empty output" >&2
    exit 0
fi

case "${KIND}" in
    start)
        ENRICHED="$(printf '%s' "${BASE_EVENT}" | jq -c \
            --arg skill "${SKILL_NAME}" \
            --arg phase "${PHASE_NAME}" \
            '. + {skill: $skill, phase: $phase}' 2>/dev/null)"
        ;;
    end)
        ENRICHED="$(printf '%s' "${BASE_EVENT}" | jq -c \
            --arg skill "${SKILL_NAME}" \
            --arg phase "${PHASE_NAME}" \
            --arg status "${STATUS_ARG}" \
            '. + {skill: $skill, phase: $phase, status: $status}' \
            2>/dev/null)"
        ;;
    subagent-start)
        ENRICHED="$(printf '%s' "${BASE_EVENT}" | jq -c \
            --arg skill "${SKILL_NAME}" \
            --arg role "${ROLE_NAME}" \
            '. + {skill: $skill, metadata: {role: $role}}' 2>/dev/null)"
        ;;
    subagent-end)
        ENRICHED="$(printf '%s' "${BASE_EVENT}" | jq -c \
            --arg skill "${SKILL_NAME}" \
            --arg role "${ROLE_NAME}" \
            --arg status "${STATUS_ARG}" \
            '. + {skill: $skill, status: $status, metadata: {role: $role}}' \
            2>/dev/null)"
        ;;
    mcp-start)
        # tool.call event: tool="mcp__atlassian__<method>", metadata.mcpMethod.
        ENRICHED="$(printf '%s' "${BASE_EVENT}" | jq -c \
            --arg skill "${SKILL_NAME}" \
            --arg mcpMethod "${MCP_METHOD}" \
            --arg tool "mcp__atlassian__${MCP_METHOD}" \
            '. + {skill: $skill, tool: $tool,
                  metadata: {mcpMethod: $mcpMethod}}' \
            2>/dev/null)"
        ;;
    mcp-end)
        if [[ -n "${MCP_DURATION_MS}" ]]; then
            ENRICHED="$(printf '%s' "${BASE_EVENT}" | jq -c \
                --arg skill "${SKILL_NAME}" \
                --arg mcpMethod "${MCP_METHOD}" \
                --arg tool "mcp__atlassian__${MCP_METHOD}" \
                --arg status "${STATUS_ARG}" \
                --argjson durationMs "${MCP_DURATION_MS}" \
                '. + {skill: $skill, tool: $tool,
                      status: $status, durationMs: $durationMs,
                      metadata: {mcpMethod: $mcpMethod}}' \
                2>/dev/null)"
        else
            # Timer missing — emit the event without durationMs (§5.3).
            ENRICHED="$(printf '%s' "${BASE_EVENT}" | jq -c \
                --arg skill "${SKILL_NAME}" \
                --arg mcpMethod "${MCP_METHOD}" \
                --arg tool "mcp__atlassian__${MCP_METHOD}" \
                --arg status "${STATUS_ARG}" \
                '. + {skill: $skill, tool: $tool,
                      status: $status,
                      metadata: {mcpMethod: $mcpMethod}}' \
                2>/dev/null)"
        fi
        ;;
esac

if [[ -z "${ENRICHED}" ]]; then
    echo "telemetry-phase: jq enrichment produced empty output" >&2
    exit 0
fi

# ---------------------------------------------------------------------------
# Pipe to the canonical emit helper. Fail-open on every failure path.
# ---------------------------------------------------------------------------
printf '%s' "${ENRICHED}" | "${EMIT_SCRIPT}" || {
    echo "telemetry-phase: emit helper failed" >&2
}

exit 0
