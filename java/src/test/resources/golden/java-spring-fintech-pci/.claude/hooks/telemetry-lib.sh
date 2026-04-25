#!/usr/bin/env bash
# telemetry-lib.sh — shared helpers sourced by all telemetry-*.sh hooks.
#
# Exposes:
#   resolve_context()   Writes epicId/storyId/taskId to three variables named
#                       TELEMETRY_EPIC_ID, TELEMETRY_STORY_ID, TELEMETRY_TASK_ID
#                       following the RULE-005 resolution order:
#                         (1) env var CLAUDE_TELEMETRY_CONTEXT (JSON)
#                         (2) current Git branch (feature/epic-NNNN-...)
#                         (3) plans/epic-*/execution-state.json currentPhase
#                         (4) fallback "unknown"
#   build_event()       jq -n wrapper that produces a minimal event with the
#                       five mandatory fields + resolved context.
#   now_iso()           Cross-platform UTC ISO-8601 timestamp with ms precision.
#   new_event_id()      UUIDv4 via uuidgen, with a /proc/sys/kernel/random
#                       fallback for environments without uuidgen.

# No `set +e` here — callers set their own shell options. This file must be
# sourcable under any option set.

# ---------------------------------------------------------------------------
# now_iso — print UTC timestamp (millisecond precision) matching the schema
# regex: ^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?Z$
# macOS `date` has no %N; GNU `date` does. Try both.
# ---------------------------------------------------------------------------
now_iso() {
    local ts
    # GNU date supports %N (nanoseconds). Detect by checking that the output
    # does NOT contain a literal "N" (BSD/macOS leaves "%3N" as-is or as "3N").
    ts="$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ 2>/dev/null)"
    if [[ -n "${ts}" && "${ts}" != *N* ]]; then
        printf '%s' "${ts}"
        return 0
    fi
    # BSD/macOS fallback — second precision.
    date -u +%Y-%m-%dT%H:%M:%SZ
}

# ---------------------------------------------------------------------------
# new_event_id — generate a UUIDv4.
# ---------------------------------------------------------------------------
new_event_id() {
    if command -v uuidgen >/dev/null 2>&1; then
        uuidgen | tr 'A-Z' 'a-z'
        return 0
    fi
    if [[ -r /proc/sys/kernel/random/uuid ]]; then
        cat /proc/sys/kernel/random/uuid
        return 0
    fi
    # Last resort — weak, but valid v4 shape.
    local hex
    hex="$(od -An -N16 -tx1 /dev/urandom 2>/dev/null | tr -d ' \n' \
        | cut -c1-32)"
    if [[ ${#hex} -eq 32 ]]; then
        printf '%s-%s-4%s-8%s-%s\n' \
            "${hex:0:8}" "${hex:8:4}" "${hex:13:3}" \
            "${hex:17:3}" "${hex:20:12}"
    else
        printf '00000000-0000-4000-8000-000000000000\n'
    fi
}

# ---------------------------------------------------------------------------
# resolve_context — populate TELEMETRY_EPIC_ID / _STORY_ID / _TASK_ID.
# ---------------------------------------------------------------------------
resolve_context() {
    TELEMETRY_EPIC_ID="unknown"
    TELEMETRY_STORY_ID=""
    TELEMETRY_TASK_ID=""

    # (1) Explicit env var (JSON blob). Parsed via jq when present.
    if [[ -n "${CLAUDE_TELEMETRY_CONTEXT:-}" ]]; then
        if command -v jq >/dev/null 2>&1; then
            local epic story task
            epic="$(printf '%s' "${CLAUDE_TELEMETRY_CONTEXT}" \
                | jq -r '.epicId // empty' 2>/dev/null)"
            story="$(printf '%s' "${CLAUDE_TELEMETRY_CONTEXT}" \
                | jq -r '.storyId // empty' 2>/dev/null)"
            task="$(printf '%s' "${CLAUDE_TELEMETRY_CONTEXT}" \
                | jq -r '.taskId // empty' 2>/dev/null)"
            if [[ -n "${epic}" ]]; then
                TELEMETRY_EPIC_ID="${epic}"
                TELEMETRY_STORY_ID="${story}"
                TELEMETRY_TASK_ID="${task}"
                return 0
            fi
        fi
    fi

    # (2) Git branch — `feature/epic-NNNN-...` or `feat/story-NNNN-MMMM-...`.
    local branch=""
    if command -v git >/dev/null 2>&1; then
        branch="$(git -C "${CLAUDE_PROJECT_DIR:-.}" rev-parse \
            --abbrev-ref HEAD 2>/dev/null)"
    fi

    if [[ -n "${branch}" ]]; then
        # Story branch first (more specific): feat/story-NNNN-MMMM-...
        if [[ "${branch}" =~ ^(feat|feature|fix|hotfix)/story-([0-9]{4})-([0-9]{4}) ]]; then
            TELEMETRY_EPIC_ID="EPIC-${BASH_REMATCH[2]}"
            TELEMETRY_STORY_ID="story-${BASH_REMATCH[2]}-${BASH_REMATCH[3]}"
            return 0
        fi
        # Task branch: feat/task-NNNN-MMMM-NNN-...
        if [[ "${branch}" =~ ^(feat|feature|fix|hotfix)/task-([0-9]{4})-([0-9]{4})-([0-9]{3}) ]]; then
            TELEMETRY_EPIC_ID="EPIC-${BASH_REMATCH[2]}"
            TELEMETRY_STORY_ID="story-${BASH_REMATCH[2]}-${BASH_REMATCH[3]}"
            TELEMETRY_TASK_ID="TASK-${BASH_REMATCH[2]}-${BASH_REMATCH[3]}-${BASH_REMATCH[4]}"
            return 0
        fi
        # Epic branch: feature/epic-NNNN-...
        if [[ "${branch}" =~ ^(feat|feature|fix|hotfix)/epic-([0-9]{4}) ]]; then
            TELEMETRY_EPIC_ID="EPIC-${BASH_REMATCH[2]}"
            return 0
        fi
    fi

    # (3) execution-state.json scan — pick the file with the most recent
    # mtime whose epic is genuinely active (RULE-005). Cross-platform mtime
    # via BSD `stat -f %m` or GNU `stat -c %Y`.
    #
    # An epic is considered active only when ALL of the following hold:
    #   - currentPhase is a non-empty string AND not "0" / 0 (numeric)
    #   - currentPhase does not end in -complete / -done (case-insensitive)
    #   - epicStatus is not in {success, complete, completed, done}
    #   - completedAt and finishedAt are null
    # Otherwise the epic is treated as concluded or never started, and the
    # state file is skipped — preventing telemetry from continuing to write
    # into the directory of a closed epic when the user is on develop.
    if command -v jq >/dev/null 2>&1 && [[ -n "${CLAUDE_PROJECT_DIR:-}" ]]; then
        local state_file newest_file="" mtime newest_mtime=0
        # shellcheck disable=SC2044
        for state_file in \
                "${CLAUDE_PROJECT_DIR}"/plans/epic-*/execution-state.json; do
            [[ -f "${state_file}" ]] || continue
            local cp
            cp="$(jq -r '
                if (.currentPhase == null
                        or .currentPhase == ""
                        or .currentPhase == 0
                        or .currentPhase == "0") then empty
                elif ((.currentPhase | type) == "string"
                        and (.currentPhase
                            | ascii_downcase
                            | test("(^|-)(complete|completed|done)$"))) then empty
                elif ((.epicStatus // "")
                        | tostring
                        | ascii_downcase
                        | test("^(success|complete|completed|done)$")) then empty
                elif (.completedAt != null or .finishedAt != null) then empty
                else (.currentPhase | tostring)
                end
            ' "${state_file}" 2>/dev/null)"
            [[ -z "${cp}" ]] && continue
            mtime="$(stat -f %m "${state_file}" 2>/dev/null \
                || stat -c %Y "${state_file}" 2>/dev/null \
                || echo 0)"
            if [[ "${mtime}" =~ ^[0-9]+$ ]] \
                    && (( mtime > newest_mtime )); then
                newest_mtime="${mtime}"
                newest_file="${state_file}"
            fi
        done
        if [[ -n "${newest_file}" ]]; then
            local dir name
            dir="$(dirname "${newest_file}")"
            name="$(basename "${dir}")"
            if [[ "${name}" =~ ^epic-([0-9]{4})$ ]]; then
                TELEMETRY_EPIC_ID="EPIC-${BASH_REMATCH[1]}"
                return 0
            fi
        fi
    fi

    # (4) Fallback. Already set above.
    return 0
}

# ---------------------------------------------------------------------------
# build_event — compose a minimal JSON event and write to stdout.
#   $1: sessionId (required)
#   $2: type      (required — enum value from the schema)
#
# Context fields come from resolve_context(). Caller may enrich the result
# with `jq` before piping to telemetry-emit.sh.
# ---------------------------------------------------------------------------
build_event() {
    local session_id="$1"
    local event_type="$2"

    # Context may already be resolved by the caller; don't clobber.
    if [[ -z "${TELEMETRY_EPIC_ID:-}" ]]; then
        resolve_context
    fi

    local eid ts
    eid="$(new_event_id)"
    ts="$(now_iso)"

    jq -n \
        --arg sv "1.0.0" \
        --arg eid "${eid}" \
        --arg ts "${ts}" \
        --arg sid "${session_id}" \
        --arg t "${event_type}" \
        --arg epic "${TELEMETRY_EPIC_ID}" \
        --arg story "${TELEMETRY_STORY_ID}" \
        --arg task "${TELEMETRY_TASK_ID}" \
        '{
            schemaVersion: $sv,
            eventId: $eid,
            timestamp: $ts,
            sessionId: $sid,
            type: $t
        }
        + (if $epic != "" then {epicId: $epic} else {} end)
        + (if $story != "" then {storyId: $story} else {} end)
        + (if $task != "" then {taskId: $task} else {} end)'
}
