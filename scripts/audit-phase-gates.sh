#!/usr/bin/env bash
# audit-phase-gates.sh — Camada 4 (CI audit) of Rule 25 phase-gate enforcement.
#
# Scans every SKILL.md under the canonical 8 orchestrators and verifies:
#
#   1. Every `## Phase N` section is wrapped by:
#      a) `Skill(skill: "x-internal-phase-gate", args: "--mode pre ...")` upstream, AND
#      b) `Skill(skill: "x-internal-phase-gate", args: "--mode post ...")` downstream,
#      OR marked `<!-- phase-no-gate: <reason> -->` on the preceding line.
#
# Exit codes:
#   0  — OK
#   26 — PHASE_GATE_VIOLATION
#   2  — PHASE_GATE_BASELINE_CORRUPT
#   4  — PHASE_GATE_ENFORCEMENT_BROKEN (self-check failure)
#
# Usage:
#   scripts/audit-phase-gates.sh                    # full audit
#   scripts/audit-phase-gates.sh --self-check       # verify wiring
#   scripts/audit-phase-gates.sh --json             # JSON report
#
# See: .claude/rules/25-task-hierarchy.md §Audit Contract

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
DEFAULT_SKILLS_ROOT="${REPO_ROOT}/java/src/main/resources/targets/claude/skills/core"
DEFAULT_BASELINE="${REPO_ROOT}/audits/task-hierarchy-baseline.txt"

SKILLS_ROOT="$DEFAULT_SKILLS_ROOT"
BASELINE="$DEFAULT_BASELINE"
MODE="audit"
OUTPUT_JSON="false"

CANONICAL_ORCHESTRATORS=(
  "x-epic-implement"
  "x-story-implement"
  "x-task-implement"
  "x-release"
  "x-epic-orchestrate"
  "x-review"
  "x-review-pr"
  "x-pr-merge-train"
)

parse_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --skills-root) SKILLS_ROOT="$2"; shift 2 ;;
      --baseline)    BASELINE="$2"; shift 2 ;;
      --self-check)  MODE="self-check"; shift ;;
      --json)        OUTPUT_JSON="true"; shift ;;
      -h|--help)
        grep '^# ' "$0" | sed 's/^# //'
        exit 0
        ;;
      *) echo "ERROR: unknown flag: $1" >&2; exit 64 ;;
    esac
  done
}

self_check() {
  [ -d "$DEFAULT_SKILLS_ROOT" ] || { echo "PHASE_GATE_ENFORCEMENT_BROKEN: missing $DEFAULT_SKILLS_ROOT" >&2; exit 4; }
  echo "self-check: OK"
  exit 0
}

is_baselined() {
  local skill="$1"
  [ -f "$BASELINE" ] || return 1
  grep -qE "^${skill}[[:space:]#]" "$BASELINE"
}

is_canonical_orchestrator() {
  local skill="$1"
  for o in "${CANONICAL_ORCHESTRATORS[@]}"; do
    [ "$skill" = "$o" ] && return 0
  done
  return 1
}

# For each `## Phase N` header in `skill_file`, verify a PRE and POST gate
# invocation of x-internal-phase-gate exists between this header and the next
# phase header (or EOF), OR a `<!-- phase-no-gate: -->` marker immediately
# precedes the phase header.
check_skill() {
  local skill_file="$1"
  local skill_name
  skill_name="$(basename "$(dirname "$skill_file")")"

  is_canonical_orchestrator "$skill_name" || return 0
  if is_baselined "$skill_name"; then
    return 0
  fi

  # Extract phase headers + line numbers
  local tmp
  tmp=$(mktemp)
  grep -nE "^## Phase [0-9]+" "$skill_file" > "$tmp" || true

  local violations=()
  local prev_line=0
  local prev_phase=""

  while IFS=: read -r line_no header; do
    if [ -n "$prev_phase" ]; then
      local body
      body=$(sed -n "${prev_line},${line_no}p" "$skill_file")
      check_phase_body "$skill_file" "$prev_phase" "$prev_line" "$body" violations
    fi
    prev_line=$line_no
    prev_phase=$(echo "$header" | sed -E 's/^## (Phase [0-9]+).*/\1/')
  done < "$tmp"

  # Flush final phase
  if [ -n "$prev_phase" ]; then
    local body
    body=$(sed -n "${prev_line},\$p" "$skill_file")
    check_phase_body "$skill_file" "$prev_phase" "$prev_line" "$body" violations
  fi

  rm -f "$tmp"

  if [ ${#violations[@]} -gt 0 ]; then
    printf '%s\n' "${violations[@]}"
    return 26
  fi
  return 0
}

check_phase_body() {
  local file="$1"
  local phase="$2"
  local line="$3"
  local body="$4"
  local -n viol_ref="$5"

  # Check for explicit no-gate marker preceding the phase
  local preceding
  preceding=$(sed -n "$((line-2)),$((line-1))p" "$file" 2>/dev/null)
  if echo "$preceding" | grep -qE "<!-- phase-no-gate:"; then
    return 0
  fi

  local has_pre="false"
  local has_post="false"
  if echo "$body" | grep -qE 'x-internal-phase-gate.*--mode pre'; then
    has_pre="true"
  fi
  if echo "$body" | grep -qE 'x-internal-phase-gate.*--mode post'; then
    has_post="true"
  fi

  [ "$has_pre" = "false" ] && viol_ref+=("$file:$line:$phase missing --mode pre gate")
  [ "$has_post" = "false" ] && viol_ref+=("$file:$line:$phase missing --mode post gate")
}

main() {
  parse_args "$@"

  if [ "$MODE" = "self-check" ]; then
    self_check
  fi

  [ -d "$SKILLS_ROOT" ] || { echo "ERROR: skills root not found: $SKILLS_ROOT" >&2; exit 64; }

  local total_violations=0
  local all_violations=()

  while IFS= read -r skill_file; do
    local v
    v=$(check_skill "$skill_file" 2>&1) || {
      total_violations=$((total_violations + 1))
      [ -n "$v" ] && all_violations+=("$v")
    }
  done < <(find "$SKILLS_ROOT" -name "SKILL.md" -path "*/core/*" 2>/dev/null)

  if [ "$OUTPUT_JSON" = "true" ]; then
    printf '{"exit_code":%d,"violations":[' "$total_violations"
    local first=true
    for v in "${all_violations[@]}"; do
      [ "$first" = "false" ] && printf ','
      # shellcheck disable=SC2001
      printf '"%s"' "$(echo "$v" | sed 's/"/\\"/g')"
      first=false
    done
    printf '],"total_violations":%d,"timestamp":"%s"}\n' \
      "$total_violations" "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  fi

  if [ "$total_violations" -gt 0 ]; then
    if [ "$OUTPUT_JSON" != "true" ]; then
      echo "PHASE_GATE_VIOLATION — $total_violations violation(s)" >&2
      for v in "${all_violations[@]}"; do
        echo "  $v" >&2
      done
    fi
    exit 26
  fi

  [ "$OUTPUT_JSON" = "true" ] || echo "audit-phase-gates: OK"
  exit 0
}

main "$@"
