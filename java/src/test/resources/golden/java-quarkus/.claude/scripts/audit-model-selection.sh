#!/usr/bin/env bash
#
# audit-model-selection.sh — Rule 23 (Model Selection Strategy) CI audit.
#
# Fails any PR that reintroduces a Rule 23 violation:
#
#   Check A — Declared orchestrator skills must carry `model:` in frontmatter
#   Check B — `Agent(subagent_type: "general-purpose", ...)` must declare model:
#   Check C — `Skill(skill: "x-...", ...)` must declare model: in orchestrators
#   Check D — `.claude/agents/*.md` must NOT declare `Recommended Model: Adaptive`
#
# Exit codes:
#   0  — all checks PASS
#   1  — at least one check FAIL
#   2  — invoked outside a valid ia-dev-environment repo (no source-of-truth dir)
#
# The script operates on the source-of-truth tree
# (java/src/main/resources/targets/claude/...) — that is the canonical input
# to the generator; the generated `.claude/` output is a deterministic
# projection.
#
# Introduced by story-0050-0009 (EPIC-0050). See Rule 23 at
# java/src/main/resources/targets/claude/rules/23-model-selection.md
# for the contract this script enforces.

set -euo pipefail

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
SKILLS_ROOT="${REPO_ROOT}/java/src/main/resources/targets/claude/skills"
AGENTS_ROOT="${REPO_ROOT}/java/src/main/resources/targets/claude/agents"

if [[ ! -d "${SKILLS_ROOT}" || ! -d "${AGENTS_ROOT}" ]]; then
  echo "FATAL: source-of-truth tree not found." >&2
  echo "  Expected: ${SKILLS_ROOT}" >&2
  echo "  Expected: ${AGENTS_ROOT}" >&2
  echo "  Are you running this from an ia-dev-environment repo?" >&2
  exit 2
fi

# Returns the absolute path of SKILL.md for a skill name, searching
# all subcategories under skills/. Prints nothing and returns 1 if missing.
find_skill_md() {
  local skill_name="$1"
  find "${SKILLS_ROOT}" -type d -name "${skill_name}" -print0 \
    | while IFS= read -r -d '' dir; do
        if [[ -f "${dir}/SKILL.md" ]]; then
          printf '%s\n' "${dir}/SKILL.md"
          return 0
        fi
      done
}

# ---------------------------------------------------------------------------
# Check A — Frontmatter `model:` in declared orchestrator skills
# ---------------------------------------------------------------------------
ORCHESTRATORS=(
  x-epic-implement x-story-implement x-release x-review
  x-epic-orchestrate x-pr-fix-epic x-task-implement x-epic-decompose
)

fail_a=0
echo "=== Audit: Model Selection (Rule 23) ==="
echo "Check A — Frontmatter model: in orchestrator skills"
for skill in "${ORCHESTRATORS[@]}"; do
  skill_md="$(find_skill_md "${skill}")"
  if [[ -z "${skill_md}" ]]; then
    echo "  WARN: ${skill} — SKILL.md not found (skipped)"
    continue
  fi
  # Limit search to frontmatter block (first 20 lines) to avoid matches
  # inside body prose.
  if ! head -20 "${skill_md}" | grep -qE "^model:\s+(opus|sonnet|haiku)"; then
    echo "  FAIL: ${skill} (${skill_md#"${REPO_ROOT}"/}) — missing 'model:' in frontmatter"
    fail_a=1
  fi
done
echo "Check A: $([[ ${fail_a} -eq 0 ]] && echo PASS || echo FAIL)"

# ---------------------------------------------------------------------------
# Check B — Agent(subagent_type: "general-purpose", ...) must have model:
# within 5 lines of the opening paren.
# ---------------------------------------------------------------------------
PLANNING_SKILLS=(x-story-plan x-arch-plan x-test-plan)

fail_b=0
echo "Check B — Agent(subagent_type: \"general-purpose\") with explicit model:"
for skill in "${PLANNING_SKILLS[@]}"; do
  skill_md="$(find_skill_md "${skill}")"
  if [[ -z "${skill_md}" ]]; then
    echo "  WARN: ${skill} — SKILL.md not found (skipped)"
    continue
  fi
  # Collect line numbers of Agent( openings.
  while IFS= read -r line_no; do
    # Window of 5 lines starting from the Agent( line.
    if ! sed -n "${line_no},$((line_no + 5))p" "${skill_md}" \
          | grep -qE "model:\s*\"(opus|sonnet|haiku)\""; then
      echo "  FAIL: ${skill_md#"${REPO_ROOT}"/}:${line_no} — Agent() without explicit model:"
      fail_b=1
    fi
  done < <(grep -nE "Agent\(" "${skill_md}" \
            | awk -F: '{ print $1 }' \
            | while IFS= read -r lno; do
                # Include only the Agent() blocks that specify general-purpose.
                if sed -n "${lno},$((lno + 3))p" "${skill_md}" \
                      | grep -q 'subagent_type: "general-purpose"'; then
                  printf '%s\n' "${lno}"
                fi
              done || true)
done
echo "Check B: $([[ ${fail_b} -eq 0 ]] && echo PASS || echo FAIL)"

# ---------------------------------------------------------------------------
# Check C — Skill(skill: "x-...", ...) must have model: in orchestrators
# ---------------------------------------------------------------------------
ORCHESTRATOR_SUBSET_C=(x-epic-implement x-story-implement x-review x-task-implement)

# Internal skills inherit parent tier per Rule 23 Exceptions — not audited.
# Utility / per-story-branch dispatch skills are listed as expected Skill()
# callees that must declare model: when the callee tier differs from parent.

fail_c=0
echo "Check C — Skill(...) with explicit model: in orchestrators"
# Audit only **indented dispatch blocks** (lines beginning with 4+ spaces
# followed by `Skill(`). Prose mentions inside backticks (`Skill(...)`)
# or inside bullet-list sentences are documentation, not dispatch, and
# are skipped. Internal skills (x-internal-*) inherit tier per Rule 23
# Exceptions; x-parallel-eval and x-pr-watch-ci are runtime utilities
# likewise exempt.
for skill in "${ORCHESTRATOR_SUBSET_C[@]}"; do
  skill_md="$(find_skill_md "${skill}")"
  if [[ -z "${skill_md}" ]]; then
    echo "  WARN: ${skill} — SKILL.md not found (skipped)"
    continue
  fi
  mapfile -t matches < <(
    grep -nE '^\s{4,}Skill\(skill: "x-[a-z-]+' "${skill_md}" \
      | grep -vE 'x-internal-' \
      | grep -vE 'x-parallel-eval|x-pr-watch-ci' \
      || true
  )
  for match in "${matches[@]}"; do
    if [[ -z "${match}" ]]; then continue; fi
    if [[ "${match}" != *"model:"* ]]; then
      echo "  FAIL: ${skill_md#"${REPO_ROOT}"/}:${match%%:*} — Skill() without model:"
      fail_c=1
    fi
  done
done
echo "Check C: $([[ ${fail_c} -eq 0 ]] && echo PASS || echo FAIL)"

# ---------------------------------------------------------------------------
# Check D — No agent with Recommended Model: Adaptive
# ---------------------------------------------------------------------------
fail_d=0
echo "Check D — Agents with deterministic Recommended Model (no Adaptive)"
if grep -rl "Adaptive" "${AGENTS_ROOT}" 2>/dev/null | grep -q . ; then
  grep -rln "Adaptive" "${AGENTS_ROOT}" \
    | sed "s|^|  FAIL: |"
  fail_d=1
fi
echo "Check D: $([[ ${fail_d} -eq 0 ]] && echo PASS || echo FAIL)"

# ---------------------------------------------------------------------------
# Aggregation
# ---------------------------------------------------------------------------
overall_fail=$(( fail_a + fail_b + fail_c + fail_d ))
echo
if [[ ${overall_fail} -eq 0 ]]; then
  echo "=== ALL CHECKS PASS ==="
  exit 0
else
  echo "=== AUDIT FAILED (${overall_fail} check(s) failed) ==="
  exit 1
fi
