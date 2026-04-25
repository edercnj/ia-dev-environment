#!/usr/bin/env bash
# enforce-phase-sequence.sh — Camada 3 (PreToolUse hook) of Rule 25 enforcement.
#
# Registered in settings.json as a broad PreToolUse hook (matcher "*"),
# then self-filtered in this script to `Skill` invocations via the
# `tool_name` check below. Blocks attempts to invoke an orchestrator
# skill whose predecessor phase has no passed=true record in
# execution-state.json — preventing the LLM from silently skipping a
# phase.
#
# Fail-open by design for ambiguous cases:
#   - No state file → allow (legacy / fresh run).
#   - taskTracking.enabled=false → allow (legacy mode).
#   - Missing phaseGateResults → allow (bootstrap).
#   - Target skill not in canonical 8 → allow.
#
# Only fails closed (exit 2) when:
#   - Target skill IS a canonical orchestrator, AND
#   - taskTracking.enabled=true, AND
#   - Most recent phase in phaseGateResults has passed=false.
#
# See: .claude/rules/25-task-hierarchy.md §Enforcement Layers — Layer 3

set -u

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"

# Honor global opt-out
if [ "${CLAUDE_PHASE_GATE_DISABLED:-0}" = "1" ]; then
  exit 0
fi

# Canonical 8 orchestrators — only these are enforced
CANONICAL_ORCHESTRATORS="x-epic-implement x-story-implement x-task-implement x-release x-epic-orchestrate x-review x-review-pr x-pr-merge-train"

# The hook receives the tool-call payload on stdin (JSON).
# Schema (PreToolUse):
#   { "tool_name": "Skill", "tool_input": { "skill": "x-...", "args": "..." } }
#
# Fail-open on any parse error.
command -v jq >/dev/null 2>&1 || exit 0

PAYLOAD=$(cat 2>/dev/null || echo "")
[ -z "$PAYLOAD" ] && exit 0

TOOL_NAME=$(echo "$PAYLOAD" | jq -r '.tool_name // empty' 2>/dev/null || echo "")
[ "$TOOL_NAME" = "Skill" ] || exit 0

TARGET_SKILL=$(echo "$PAYLOAD" | jq -r '.tool_input.skill // empty' 2>/dev/null || echo "")
[ -z "$TARGET_SKILL" ] && exit 0

# Check if target is canonical
is_canonical=false
for o in $CANONICAL_ORCHESTRATORS; do
  if [ "$TARGET_SKILL" = "$o" ]; then
    is_canonical=true
    break
  fi
done
[ "$is_canonical" = "true" ] || exit 0

# Locate the most recent execution-state.json
STATE_FILE=""
while IFS= read -r candidate; do
  if [ -z "$STATE_FILE" ] || [ "$candidate" -nt "$STATE_FILE" ]; then
    STATE_FILE="$candidate"
  fi
done < <(find "$PROJECT_DIR/plans" -maxdepth 3 -type f -name "execution-state.json" 2>/dev/null)

[ -z "$STATE_FILE" ] && exit 0

# Short-circuit on disabled taskTracking
enabled=$(jq -r '.taskTracking.enabled // false' "$STATE_FILE" 2>/dev/null || echo "false")
[ "$enabled" = "true" ] || exit 0

# Find the most recent phase gate result
LATEST=$(jq -c '.taskTracking.phaseGateResults // [] | last // empty' "$STATE_FILE" 2>/dev/null || echo "")

# Bootstrap: no results yet → allow
[ -z "$LATEST" ] && exit 0

PASSED=$(echo "$LATEST" | jq -r '.passed // false')
if [ "$PASSED" = "false" ]; then
  PHASE=$(echo "$LATEST" | jq -r '.phase // "unknown"')
  MODE=$(echo "$LATEST" | jq -r '.mode // "unknown"')
  {
    echo ""
    echo "⛔ PHASE SEQUENCE BLOCK — Rule 25 Layer 3 (PreToolUse hook)"
    echo "   Cannot invoke '$TARGET_SKILL': predecessor phase failed its gate."
    echo "   Latest gate: $PHASE ($MODE) → passed=false"
    echo "   State: $STATE_FILE"
    echo ""
    echo "   Resolve the failing gate (add missing artifacts, complete missing"
    echo "   tasks, or mark the phase as <!-- phase-no-gate --> with reason)"
    echo "   before retrying."
    echo ""
  } >&2
  exit 2
fi

exit 0
