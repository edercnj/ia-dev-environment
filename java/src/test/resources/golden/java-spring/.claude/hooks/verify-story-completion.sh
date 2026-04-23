#!/usr/bin/env bash
# verify-story-completion.sh — Claude Code `Stop` hook, EIE Camada 2.
#
# Detects when a turn just completed story-level work (PR created, merge done,
# or git commit on a feat/story-* branch) and verifies that the required
# sub-skills produced their evidence artifacts in plans/epic-*/plans/ and
# plans/epic-*/reports/.
#
# On missing evidence, exits with code 2 and emits a visible warning on stderr
# — Claude Code surfaces this to the LLM as a blocking notification that MUST
# be addressed before the next turn.
#
# Fail-open for non-EIE-relevant turns: if no story activity detected in this
# turn, exit 0 silently. See .claude/rules/24-execution-integrity.md.

set +e
set -u

if [[ "${CLAUDE_EIE_DISABLED:-0}" == "1" ]]; then
    cat >/dev/null 2>&1
    exit 0
fi

# Consume hook payload (JSON from Claude Code)
if command -v timeout >/dev/null 2>&1; then
    HOOK_PAYLOAD="$(timeout 3 cat 2>/dev/null)"
elif command -v gtimeout >/dev/null 2>&1; then
    HOOK_PAYLOAD="$(gtimeout 3 cat 2>/dev/null)"
else
    HOOK_PAYLOAD="$(cat 2>/dev/null)"
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "${PROJECT_DIR}" 2>/dev/null || exit 0

# Discover most recent epic telemetry file
TELEMETRY="$(ls -t plans/epic-*/telemetry/events.ndjson 2>/dev/null | head -1)"
[[ -z "${TELEMETRY}" ]] && exit 0

# Heuristic: look for recent "story completion" signals in this session.
# Signal A — `gh pr create` invocation in recent 500 telemetry events.
# Signal B — git commit message mentioning "feat(story-" in HEAD.
HAS_PR_CREATE=0
if command -v jq >/dev/null 2>&1; then
    RECENT_PR="$(tail -500 "${TELEMETRY}" 2>/dev/null \
        | jq -r 'select(.tool_name=="Bash") | .tool_input.command // empty' 2>/dev/null \
        | grep -E "gh\s+pr\s+create" 2>/dev/null \
        | tail -1)"
    if [[ -n "${RECENT_PR}" ]]; then
        HAS_PR_CREATE=1
    fi
fi

HAS_STORY_COMMIT=0
LATEST_COMMIT_MSG="$(git log -1 --format=%B 2>/dev/null || true)"
if [[ "${LATEST_COMMIT_MSG}" =~ feat\(story-[0-9]{4}-[0-9]{4} ]]; then
    HAS_STORY_COMMIT=1
fi

# If neither signal present, this turn was not a story-completion turn
if [[ "${HAS_PR_CREATE}" -eq 0 && "${HAS_STORY_COMMIT}" -eq 0 ]]; then
    exit 0
fi

# Extract story ID from the commit message or PR
STORY_ID=""
if [[ "${LATEST_COMMIT_MSG}" =~ (story-[0-9]{4}-[0-9]{4}) ]]; then
    STORY_ID="${BASH_REMATCH[1]}"
fi
if [[ -z "${STORY_ID}" ]]; then
    BRANCH="$(git branch --show-current 2>/dev/null || true)"
    if [[ "${BRANCH}" =~ (story-[0-9]{4}-[0-9]{4}) ]]; then
        STORY_ID="${BASH_REMATCH[1]}"
    fi
fi
[[ -z "${STORY_ID}" ]] && exit 0

# Extract epic ID from story ID (story-XXXX-YYYY → epic XXXX)
if [[ "${STORY_ID}" =~ story-([0-9]{4})-[0-9]{4} ]]; then
    EPIC_ID="${BASH_REMATCH[1]}"
else
    exit 0
fi

PLANS_DIR="plans/epic-${EPIC_ID}/plans"
REPORTS_DIR="plans/epic-${EPIC_ID}/reports"

# Check mandatory evidence artifacts (Rule 24 §4.1)
MISSING=()

if [[ ! -f "${REPORTS_DIR}/verify-envelope-${STORY_ID}.json" ]]; then
    MISSING+=("x-internal-story-verify → ${REPORTS_DIR}/verify-envelope-${STORY_ID}.json")
fi

if ! ls "${PLANS_DIR}/review-"*"-${STORY_ID}.md" >/dev/null 2>&1 \
        && ! [[ -f "${PLANS_DIR}/review-story-${STORY_ID}.md" ]]; then
    MISSING+=("x-review → ${PLANS_DIR}/review-story-${STORY_ID}.md")
fi

if ! ls "${PLANS_DIR}/techlead-review-"*"-${STORY_ID}.md" >/dev/null 2>&1 \
        && ! [[ -f "${PLANS_DIR}/techlead-review-story-${STORY_ID}.md" ]]; then
    MISSING+=("x-review-pr → ${PLANS_DIR}/techlead-review-story-${STORY_ID}.md")
fi

if [[ ! -f "${REPORTS_DIR}/story-completion-report-${STORY_ID}.md" ]]; then
    MISSING+=("x-internal-story-report → ${REPORTS_DIR}/story-completion-report-${STORY_ID}.md")
fi

if [[ ${#MISSING[@]} -gt 0 ]]; then
    cat >&2 <<EOF

🔒 EXECUTION INTEGRITY WARNING (Rule 24 — Camada 2)
=====================================================

Story: ${STORY_ID}
Detected: story-completion turn (commit or PR) but evidence artifacts MISSING.

The following MANDATORY sub-skills appear to have been SKIPPED or INLINED
instead of invoked as real tool calls:

EOF
    for m in "${MISSING[@]}"; do
        printf "  ❌ %s\n" "${m}" >&2
    done
    cat >&2 <<EOF

ACTION REQUIRED before proceeding:
  1. Go back to ${STORY_ID}
  2. Invoke the missing skills as REAL tool calls:
     Skill(skill: "x-internal-story-verify", args: "--story-id ${STORY_ID} ...")
     Skill(skill: "x-review", args: "${STORY_ID}")
     Skill(skill: "x-review-pr", args: "${STORY_ID}")
     Skill(skill: "x-internal-story-report", args: "...")
  3. Each must produce its evidence file (listed above).

LEGITIMATE BYPASS: pass --skip-review / --skip-verification explicitly via
the calling skill's argv. Any other skip is a Rule 24 violation.

This warning will be enforced as a HARD FAILURE by CI when the PR is
merged to develop — see scripts/audit-execution-integrity.sh.

To temporarily disable this hook locally (not recommended):
  export CLAUDE_EIE_DISABLED=1
=====================================================
EOF
    exit 2
fi

exit 0
