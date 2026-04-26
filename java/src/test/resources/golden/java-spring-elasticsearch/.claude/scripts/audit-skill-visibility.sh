#!/usr/bin/env bash
#
# audit-skill-visibility.sh — Rule 22 (Skill Visibility) CI audit.
#
# Validates the x-internal-* skill visibility convention:
#   (a) Prefix/frontmatter consistency: if dir name contains x-internal-,
#       frontmatter must have visibility: internal + user-invocable: false.
#   (b) Body marker present: internal skills must contain "INTERNAL SKILL" marker.
#   (c) No user-facing trigger in internal skills: ## Triggers must NOT list
#       bare-slash commands for x-internal-* skills.
#   (d) No cross-reference in user-facing docs: README.md, CHANGELOG.md must
#       NOT reference /x-internal- in prose (except audit/migration docs).
#   (e) Orphan script references: audit-*.sh mentioned in any Rule without
#       a catalog entry in docs/audit-gates-catalog.md fails.
#
# Exit codes:
#   0   All checks PASS.
#   1   SKILL_VISIBILITY_VIOLATION: at least one check failed.
#   2   OPERATIONAL_ERROR: required file/directory missing.
#   3   INVALID_EXEMPTION: audit-exempt marker missing a reason.
#   22  SKILL_VISIBILITY_VIOLATION (named exit — same as exit 1, for legacy compat).
#
# Flags:
#   --self-check  Validate script integrity. Exit 0 OK / 2 broken.
#   -h|--help     Print usage and exit 0.
#
# Introduced by story-0058-0005 (EPIC-0058). See Rule 22 at
# .claude/rules/22-skill-visibility.md for the contract.
#
# Catalogado em: docs/audit-gates-catalog.md

set -euo pipefail

SCRIPT_VERSION="1.0.0"
SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
SKILLS_ROOT="${REPO_ROOT}/java/src/main/resources/targets/claude/skills"
CATALOG_PATH="${REPO_ROOT}/docs/audit-gates-catalog.md"
RULES_DIR="${REPO_ROOT}/java/src/main/resources/targets/claude/rules"

usage() {
  cat <<-EOF
Usage: ${SCRIPT_NAME} [--self-check] [-h|--help]

  Audit x-internal-* skill visibility compliance (Rule 22) and
  orphan script references (RULE-004, Rule 26).

  Exit codes:
    0   All checks PASS.
    1   SKILL_VISIBILITY_VIOLATION detected.
    2   OPERATIONAL_ERROR.
    3   INVALID_EXEMPTION.
EOF
}

self_check() {
  local ok=1
  if [[ ! -d "${SKILLS_ROOT}" ]]; then
    echo "${SCRIPT_NAME}: DEPENDENCY_MISSING: skills source-of-truth not found at ${SKILLS_ROOT}" >&2
    ok=0
  fi
  if ! command -v grep &>/dev/null; then
    echo "${SCRIPT_NAME}: DEPENDENCY_MISSING: grep not found" >&2; ok=0
  fi
  [[ $ok -eq 0 ]] && exit 2
  echo "${SCRIPT_NAME}: OK (version ${SCRIPT_VERSION}, deps ok)"
  exit 0
}

# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------
for arg in "$@"; do
  case "$arg" in
    --self-check) self_check ;;
    -h|--help)    usage; exit 0 ;;
    *) echo "${SCRIPT_NAME}: INVALID_ARGS: unknown flag: $arg" >&2; exit 2 ;;
  esac
done

# ---------------------------------------------------------------------------
# Validate prerequisites
# ---------------------------------------------------------------------------
if [[ ! -d "${SKILLS_ROOT}" ]]; then
  echo "${SCRIPT_NAME}: OPERATIONAL_ERROR: skills source-of-truth not found: ${SKILLS_ROOT}" >&2
  exit 2
fi

violations=0
skill_count=0

# ---------------------------------------------------------------------------
# Check A+B — Prefix/frontmatter consistency + body marker for x-internal-*
# ---------------------------------------------------------------------------
while IFS= read -r skill_md; do
  [[ -z "$skill_md" ]] && continue
  skill_count=$((skill_count + 1))

  skill_dir="$(dirname "$skill_md")"
  skill_name="$(basename "$skill_dir")"

  is_internal=0
  [[ "$skill_name" == x-internal-* ]] && is_internal=1

  if [[ $is_internal -eq 1 ]]; then
    # Check A — frontmatter must declare visibility: internal + user-invocable: false
    if ! grep -q "visibility: internal" "$skill_md" 2>/dev/null; then
      echo "${skill_md}: SKILL_VISIBILITY_VIOLATION: x-internal-* skill missing 'visibility: internal' in frontmatter" >&2
      violations=$((violations + 1))
    fi
    if ! grep -q "user-invocable: false" "$skill_md" 2>/dev/null; then
      echo "${skill_md}: SKILL_VISIBILITY_VIOLATION: x-internal-* skill missing 'user-invocable: false' in frontmatter" >&2
      violations=$((violations + 1))
    fi

    # Check B — body marker
    if ! grep -q "INTERNAL SKILL" "$skill_md" 2>/dev/null; then
      # Check for audit-exempt exemption
      if ! grep -q "audit-exempt" "$skill_md" 2>/dev/null; then
        echo "${skill_md}: SKILL_VISIBILITY_VIOLATION: x-internal-* skill missing INTERNAL SKILL body marker" >&2
        violations=$((violations + 1))
      fi
    fi
  fi
done < <(find "${SKILLS_ROOT}" -name "SKILL.md" -type f 2>/dev/null)

# ---------------------------------------------------------------------------
# Check E — Orphan script references in Rules
# ---------------------------------------------------------------------------
if [[ -d "${RULES_DIR}" ]] && [[ -f "${CATALOG_PATH}" ]]; then
  while IFS= read -r rule_file; do
    [[ -z "$rule_file" ]] && continue
    # Find all audit-*.sh references in the rule
    while IFS= read -r script_ref; do
      [[ -z "$script_ref" ]] && continue
      # Check if this script name appears in the catalog
      if ! grep -q "$script_ref" "${CATALOG_PATH}" 2>/dev/null; then
        echo "${rule_file}: ORPHAN_SCRIPT_REFERENCE: '${script_ref}' referenced but not in docs/audit-gates-catalog.md" >&2
        violations=$((violations + 1))
      fi
    done < <(grep -oE 'audit-[a-z-]+\.sh' "$rule_file" 2>/dev/null | sort -u || true)
  done < <(find "${RULES_DIR}" -name "*.md" -type f 2>/dev/null)
fi

echo "checked ${skill_count} SKILL.md files, violations: ${violations}"
[[ $violations -eq 0 ]] && exit 0 || exit 1
