#!/usr/bin/env bash
# audit-rule-20.sh
#
# CI guard: fails the build if any SKILL.md in the skills/core tree
# contains a real invocation of x-pr-create without also containing
# a real invocation of x-pr-watch-ci OR declaring --no-ci-watch.
#
# "Real invocation" means a line of the form:
#   Skill(skill: "x-pr-create"
# This deliberately excludes table rows, rationale prose, and comments
# that merely mention the skill name.
#
# Source of truth: Rule 21 — CI-Watch (RULE-045-01)
# Related: Rule 13 — Skill Invocation Protocol
# Story: story-0045-0002 (EPIC-0045)
#
# Usage:
#   audit-rule-20.sh [--skills-dir <path>] [--help]
#
# Flags:
#   --skills-dir <path>  Override the default skills directory (useful for test
#                        isolation with synthetic fixture trees).
#   --help               Print this usage and exit 0.
#
# Exit codes:
#   0  AUDIT PASSED (zero violations)
#   1  AUDIT FAILED (one or more files with x-pr-create but no x-pr-watch-ci
#                    and no --no-ci-watch)
#   2  Execution error (grep not found, skills directory invalid, etc.)

set -euo pipefail

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

SKILLS_DIR="${REPO_ROOT}/java/src/main/resources/targets/claude/skills"

# Pattern that identifies a real Skill(...) invocation of x-pr-create.
# Must match: Skill(skill: "x-pr-create"
PR_CREATE_INVOKE_PATTERN='Skill\(skill:[[:space:]]*"x-pr-create"'

# Pattern that identifies a real Skill(...) invocation of x-pr-watch-ci.
PR_WATCH_CI_INVOKE_PATTERN='Skill\(skill:[[:space:]]*"x-pr-watch-ci"'

# ERE pattern that matches the --no-ci-watch opt-out string.
# Using an ERE pattern with grep -E avoids the issue where grep -F
# on some systems interprets the leading -- as a flag terminator.
NO_CI_WATCH_OPTOUT_PATTERN='--no-ci-watch'

# ---------------------------------------------------------------------------
# Globals
# ---------------------------------------------------------------------------

VIOLATIONS=0
VIOLATION_LINES=()
FILES_SCANNED=0

# ---------------------------------------------------------------------------
# Usage
# ---------------------------------------------------------------------------

usage() {
    sed -n '1,/^# Exit codes:/p' "${BASH_SOURCE[0]}" \
        | grep '^#' \
        | sed 's/^# \{0,1\}//'
}

# ---------------------------------------------------------------------------
# Dependency checks
# ---------------------------------------------------------------------------

require_grep() {
    if ! command -v grep >/dev/null 2>&1; then
        printf 'audit-rule-20: grep not found\n' >&2
        exit 2
    fi
}

require_skills_dir() {
    if [[ ! -d "${SKILLS_DIR}" ]]; then
        printf 'audit-rule-20: skills directory not found at %s\n' \
            "${SKILLS_DIR}" >&2
        exit 2
    fi
}

# ---------------------------------------------------------------------------
# File scanner
# ---------------------------------------------------------------------------

# Returns 0 if the file contains at least one real x-pr-create invocation.
file_invokes_pr_create() {
    local file="$1"
    grep -qE "${PR_CREATE_INVOKE_PATTERN}" "${file}" 2>/dev/null
}

# Returns 0 if the file contains a real x-pr-watch-ci invocation
# OR declares --no-ci-watch.
file_has_ci_watch_or_optout() {
    local file="$1"
    if grep -qE "${PR_WATCH_CI_INVOKE_PATTERN}" "${file}" 2>/dev/null; then
        return 0
    fi
    # Use grep -e with a leading character anchor to avoid --no-ci-watch
    # being interpreted as a grep flag on some systems.
    if grep -qE -- "${NO_CI_WATCH_OPTOUT_PATTERN}" "${file}" 2>/dev/null; then
        return 0
    fi
    return 1
}

scan_file() {
    local file="$1"

    if ! file_invokes_pr_create "${file}"; then
        # No x-pr-create invocation — nothing to check.
        return
    fi

    if file_has_ci_watch_or_optout "${file}"; then
        # Compliant: has x-pr-watch-ci or --no-ci-watch.
        return
    fi

    # Violation: invokes x-pr-create but missing x-pr-watch-ci and --no-ci-watch.
    local rel
    rel="${file#"${REPO_ROOT}/"}"
    VIOLATION_LINES+=("  ${rel}: invokes x-pr-create but missing x-pr-watch-ci (or --no-ci-watch opt-out)")
    (( VIOLATIONS++ )) || true
}

# ---------------------------------------------------------------------------
# Main scanner loop
# ---------------------------------------------------------------------------

scan_all() {
    while IFS= read -r -d '' f; do
        scan_file "${f}"
        (( FILES_SCANNED++ )) || true
    done < <(find "${SKILLS_DIR}" -name "SKILL.md" -print0 2>/dev/null)
}

# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

main() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --help|-h)
                usage
                exit 0
                ;;
            --skills-dir)
                if [[ $# -lt 2 ]]; then
                    printf 'audit-rule-20: --skills-dir requires a path argument\n' >&2
                    exit 2
                fi
                SKILLS_DIR="$2"
                shift 2
                ;;
            --skills-dir=*)
                SKILLS_DIR="${1#--skills-dir=}"
                shift
                ;;
            *)
                printf 'audit-rule-20: unknown argument: %s\n' "$1" >&2
                exit 2
                ;;
        esac
    done

    require_grep
    require_skills_dir

    scan_all

    if [[ "${VIOLATIONS}" -eq 0 ]]; then
        printf 'AUDIT PASSED: %d files scanned, 0 violations\n' \
            "${FILES_SCANNED}"
        exit 0
    else
        printf 'AUDIT FAILED: %d violation(s)\n' "${VIOLATIONS}"
        local v
        for v in "${VIOLATION_LINES[@]}"; do
            printf '%s\n' "${v}"
        done
        exit 1
    fi
}

main "$@"
