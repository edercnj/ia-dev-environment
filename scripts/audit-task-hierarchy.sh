#!/usr/bin/env bash
# audit-task-hierarchy.sh — Camada 4 (CI audit) of Rule 25 enforcement.
#
# Scans every SKILL.md under java/src/main/resources/targets/claude/skills/core/
# for orchestrators in the canonical 8 (Anexo B of EPIC-0055) and verifies:
#
#   1. Every `## Phase N` section contains at least one `TaskCreate(` call.
#   2. Every `TaskCreate(` has a downstream `TaskUpdate(..., status: "completed")`
#      within the same SKILL.md (or an `<!-- audit-exempt: reason -->` marker).
#   3. Every `subject: "..."` literal matches the Rule 25 canonical regex.
#
# Grandfathered orchestrators listed in audits/task-hierarchy-baseline.txt are
# exempted during the deprecation window (2 releases post-EPIC-0055 merge).
#
# Exit codes:
#   0  — OK
#   25 — TASK_HIERARCHY_VIOLATION
#   4  — TASK_HIERARCHY_ENFORCEMENT_BROKEN (self-check failure)
#   64 — EX_USAGE (unknown flag)
#
# Usage:
#   scripts/audit-task-hierarchy.sh                                 # full audit
#   scripts/audit-task-hierarchy.sh --self-check                    # verify wiring
#   scripts/audit-task-hierarchy.sh --skills-root <path>            # override root
#   scripts/audit-task-hierarchy.sh --json                          # JSON report
#
# See: .claude/rules/25-task-hierarchy.md §Audit Contract

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
DEFAULT_SKILLS_ROOT="${REPO_ROOT}/java/src/main/resources/targets/claude/skills/core"
DEFAULT_BASELINE="${REPO_ROOT}/audits/task-hierarchy-baseline.txt"
DEFAULT_RULE_SOURCE="${REPO_ROOT}/java/src/main/resources/targets/claude/rules/25-task-hierarchy.md"
DEFAULT_RULE_GENERATED="${REPO_ROOT}/.claude/rules/25-task-hierarchy.md"

SKILLS_ROOT="$DEFAULT_SKILLS_ROOT"
BASELINE="$DEFAULT_BASELINE"
MODE="audit"
OUTPUT_JSON="false"

# Canonical 8 orchestrators (Anexo B of EPIC-0055 spec)
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

# Rule 25 §3 subject regex (POSIX ERE — `›` handled via grep -E with UTF-8)
SUBJECT_REGEX='^(([A-Z][A-Z0-9-]+|epic-[0-9]{4}|story-[0-9]{4}-[0-9]{4}|task-[0-9]{4}-[0-9]{4}(-[0-9]{3})?|Phase [0-9]+))( › [A-Za-z0-9_.:() -]+){0,3}$'

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
  local broken=0
  # Rule 25 is the source of truth in java/src/main/resources/; the
  # generated copy under .claude/rules/ is optional (gitignored in this
  # project). Pass when EITHER form exists.
  if [ ! -f "$DEFAULT_RULE_SOURCE" ] && [ ! -f "$DEFAULT_RULE_GENERATED" ]; then
    echo "missing: $DEFAULT_RULE_SOURCE (nor $DEFAULT_RULE_GENERATED)" >&2
    broken=1
  fi
  [ -f "$DEFAULT_BASELINE" ] || { echo "missing: $DEFAULT_BASELINE" >&2; broken=1; }
  [ -d "$DEFAULT_SKILLS_ROOT" ] || { echo "missing: $DEFAULT_SKILLS_ROOT" >&2; broken=1; }
  if [ "$broken" -ne 0 ]; then
    echo "TASK_HIERARCHY_ENFORCEMENT_BROKEN" >&2
    exit 4
  fi
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

check_skill() {
  local skill_file="$1"
  local skill_name
  skill_name="$(basename "$(dirname "$skill_file")")"

  # Only audit canonical orchestrators (others are exempt by design)
  is_canonical_orchestrator "$skill_name" || return 0

  # Baseline grandfather
  if is_baselined "$skill_name"; then
    return 0
  fi

  local violations=()

  # Check 1: every `## Phase N` has at least one TaskCreate (Rule 25 Invariant 1).
  # `<!-- phase-no-gate: -->` is a gate exemption only (Invariant 4) — it does
  # NOT exempt TaskCreate. If you need to exempt TaskCreate too, combine with
  # `<!-- audit-exempt -->` per Check 2.
  local current_phase=""
  local current_phase_line=0
  local phase_has_taskcreate="false"
  local line_no=0

  while IFS= read -r line; do
    line_no=$((line_no + 1))
    if echo "$line" | grep -qE "^## Phase [0-9]+"; then
      # Flush previous phase
      if [ -n "$current_phase" ] && [ "$phase_has_taskcreate" = "false" ]; then
        violations+=("$skill_file:$current_phase_line:missing TaskCreate in $current_phase")
      fi
      current_phase=$(echo "$line" | sed -E 's/^## (Phase [0-9]+).*/\1/')
      current_phase_line=$line_no
      phase_has_taskcreate="false"
    elif echo "$line" | grep -qE "TaskCreate\("; then
      phase_has_taskcreate="true"
    fi
  done < "$skill_file"

  # Flush final phase
  if [ -n "$current_phase" ] && [ "$phase_has_taskcreate" = "false" ]; then
    violations+=("$skill_file:$current_phase_line:missing TaskCreate in $current_phase")
  fi

  # Check 2: every TaskCreate has a downstream TaskUpdate(completed) or audit-exempt
  local tc_count
  local tu_count
  tc_count=$(grep -cE "TaskCreate\(" "$skill_file" || echo 0)
  tu_count=$(grep -cE 'TaskUpdate\(.*status.*"completed"' "$skill_file" || echo 0)
  local exempt_count
  exempt_count=$(grep -cE "<!-- audit-exempt" "$skill_file" || echo 0)

  if [ "$tc_count" -gt 0 ] && [ $((tu_count + exempt_count)) -lt "$tc_count" ]; then
    violations+=("$skill_file:0:$tc_count TaskCreate calls but only $tu_count TaskUpdate(completed) + $exempt_count exempt markers")
  fi

  # Check 3: subject regex compliance (Rule 25 §3). Subject violations
  # contribute to `violations` and flip the return code — stderr-only was
  # previously silent to CI and defeated the audit purpose.
  while IFS= read -r grep_line; do
    [ -z "$grep_line" ] && continue
    local ln body subject
    ln="${grep_line%%:*}"
    body="${grep_line#*:}"
    subject=$(printf '%s' "$body" | sed -E 's/.*subject:[[:space:]]*"([^"]+)".*/\1/')
    if ! printf '%s' "$subject" | grep -qE "$SUBJECT_REGEX"; then
      violations+=("$skill_file:$ln:subject regex violation — '$subject'")
    fi
  done < <(grep -nE 'subject:[[:space:]]*"[^"]+"' "$skill_file" 2>/dev/null)

  if [ ${#violations[@]} -gt 0 ]; then
    printf '%s\n' "${violations[@]}"
    return 25
  fi
  return 0
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

  local actual_exit=0
  [ "$total_violations" -gt 0 ] && actual_exit=25

  if [ "$OUTPUT_JSON" = "true" ]; then
    printf '{"exit_code":%d,"violations":[' "$actual_exit"
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
      echo "TASK_HIERARCHY_VIOLATION — $total_violations violation(s)" >&2
      for v in "${all_violations[@]}"; do
        echo "  $v" >&2
      done
    fi
    exit 25
  fi

  [ "$OUTPUT_JSON" = "true" ] || echo "audit-task-hierarchy: OK"
  exit 0
}

main "$@"
