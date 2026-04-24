#!/usr/bin/env bash
# verify-phase-gates.sh — Camada 2 (Stop hook) of Rule 25 enforcement.
#
# Registered in settings.json as a Stop-event hook. Runs at the end of every
# LLM turn. Reads execution-state.json.taskTracking.phaseGateResults[] for
# the active epic and emits a WARNING on stderr when a gate failed, giving
# the operator immediate feedback that a phase transition is incomplete.
#
# Fail-open by design: any parsing error / missing state file / disabled
# taskTracking yields exit 0 silently. Only explicit gate failures emit
# exit 2 + WARNING.
#
# Invariants:
#   - Looks only at phaseGateResults entries with passed=false.
#   - Does not mutate state.
#   - Runs under 50ms P95 (single file read + jq projection).
#
# See: .claude/rules/25-task-hierarchy.md §Enforcement Layers — Layer 2

set -u

# Resolve project dir (prefer CLAUDE_PROJECT_DIR, fallback to git toplevel)
PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"

# Honor global opt-out
if [ "${CLAUDE_PHASE_GATE_DISABLED:-0}" = "1" ]; then
  exit 0
fi

# Identify active branch. Only warn when on an epic/ or feat/ branch.
BRANCH="$(git -C "$PROJECT_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")"
case "$BRANCH" in
  epic/*|feat/*|feature/*|fix/*) ;;
  *) exit 0 ;;
esac

# Find the most recent plans/epic-*/execution-state.json under project dir
STATE_FILE=""
while IFS= read -r candidate; do
  if [ -z "$STATE_FILE" ] || [ "$candidate" -nt "$STATE_FILE" ]; then
    STATE_FILE="$candidate"
  fi
done < <(find "$PROJECT_DIR/plans" -maxdepth 3 -type f -name "execution-state.json" 2>/dev/null)

[ -z "$STATE_FILE" ] && exit 0

# jq is required; fail-open if absent
command -v jq >/dev/null 2>&1 || exit 0

# Short-circuit on disabled taskTracking
local_enabled=$(jq -r '.taskTracking.enabled // false' "$STATE_FILE" 2>/dev/null || echo "false")
[ "$local_enabled" = "true" ] || exit 0

# Extract failed gate entries
FAILED=$(jq -c '.taskTracking.phaseGateResults[]? | select(.passed == false)' "$STATE_FILE" 2>/dev/null || echo "")

[ -z "$FAILED" ] && exit 0

{
  echo ""
  echo "⚠️  PHASE GATE WARNING — Rule 25 Layer 2 (Stop hook)"
  echo "   State: $STATE_FILE"
  echo "   Failed gate(s):"
  while IFS= read -r entry; do
    [ -z "$entry" ] && continue
    phase=$(echo "$entry" | jq -r '.phase')
    mode=$(echo "$entry" | jq -r '.mode')
    missing_tasks=$(echo "$entry" | jq -c '.missingTasks // []')
    missing_artifacts=$(echo "$entry" | jq -c '.missingArtifacts // []')
    echo "   - $phase ($mode): missingTasks=$missing_tasks missingArtifacts=$missing_artifacts"
  done <<< "$FAILED"
  echo ""
  echo "   Run 'scripts/audit-phase-gates.sh --json' for full report."
  echo ""
} >&2

exit 2
