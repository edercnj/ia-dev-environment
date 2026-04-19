#!/usr/bin/env bash
# audit-interactive-gates.sh
#
# CI guard: fails the build if any SKILL.md (or references/*.md) in the
# in-scope directories reintroduces:
#   1. HALT text in a pause/exit context without AskUserQuestion in the
#      same ~30-line block (Regex 1).
#   2. Deprecated flags (--interactive, --manual-task-approval,
#      --manual-contract-approval, --manual-batch-approval) outside the
#      allowlisted sections ## Triggers and ## Examples, using a
#      tokenised end-of-token lookahead so that --interactive-merge
#      is NOT a false positive (Regex 2).
#
# Scope: java/src/main/resources/targets/claude/skills/core/{ops,dev,review}/**
# Explicitly excluded: core/lib/** (utility skills, not interactive gates)
#
# Source of truth: Rule 20 — Interactive Gates Convention
# Related: Rule 13 — Skill Invocation Protocol
# Story: story-0043-0006 (EPIC-0043)
#
# Usage:
#   audit-interactive-gates.sh [--baseline] [--skills-dir <path>] [--help]
#
# Flags:
#   --baseline          Read audits/interactive-gates-baseline.txt and ignore
#                       matches in files listed there (allows CI green during
#                       migration).
#   --skills-dir <path> Override the default skills directory (useful for test
#                       isolation with synthetic fixture trees).
#   --help              Print this usage and exit 0.
#
# Exit codes:
#   0  AUDIT PASSED (zero violations outside baseline)
#   1  AUDIT FAILED (violations found)
#   2  Execution error (grep not found, SKILLS_DIR invalid, etc.)

set -euo pipefail

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

SKILLS_DIR="${REPO_ROOT}/java/src/main/resources/targets/claude/skills"
SCOPE_DIRS=(
    "${SKILLS_DIR}/core/ops"
    "${SKILLS_DIR}/core/dev"
    "${SKILLS_DIR}/core/review"
)

BASELINE_FILE="${REPO_ROOT}/audits/interactive-gates-baseline.txt"

# Allowlisted section headings — matches in these sections are ignored.
ALLOWED_SECTIONS_PATTERN="^(## Triggers|## Examples)"

# Regex 1: HALT text without AskUserQuestion within ~30 lines.
# We search for files containing HALT (in a pause/exit context) and
# then check if AskUserQuestion appears within 30 lines before or after.
HALT_PATTERN="(HALT|Skill pausada|paused\.)"

# Regex 2 patterns (tokenised with end-of-token lookahead via ERE):
#   The lookahead character class [[:space:]\]},.:;!?] covers the common
#   terminators. We also anchor on end-of-line ($) to catch line-final tokens.
#   The patterns use ERE (-E flag to grep).
#
#   --interactive (but NOT --interactive-merge or --interactive-*)
DEPRECATED_FLAG_INTERACTIVE='--interactive([[:space:]][[:space:]]*|"|\]|\)|}|,|\.|:|;|!|\?|$)'
#   --manual-task-approval
DEPRECATED_FLAG_MANUAL_TASK='--manual-task-approval([[:space:]][[:space:]]*|"|\]|\)|}|,|\.|:|;|!|\?|$)'
#   --manual-contract-approval
DEPRECATED_FLAG_MANUAL_CONTRACT='--manual-contract-approval([[:space:]][[:space:]]*|"|\]|\)|}|,|\.|:|;|!|\?|$)'
#   --manual-batch-approval
DEPRECATED_FLAG_MANUAL_BATCH='--manual-batch-approval([[:space:]][[:space:]]*|"|\]|\)|}|,|\.|:|;|!|\?|$)'

# Combined deprecated-flag pattern (alternation)
DEPRECATED_FLAGS_PATTERN="(${DEPRECATED_FLAG_INTERACTIVE}|${DEPRECATED_FLAG_MANUAL_TASK}|${DEPRECATED_FLAG_MANUAL_CONTRACT}|${DEPRECATED_FLAG_MANUAL_BATCH})"

# In references/*.md, Regex 2 applies only when the line looks like an
# executable instruction (Skill( call, Invoke, leading $ shell prompt,
# or an operational bullet). Headings/tables/prose are exempt.
REFERENCES_EXEC_PATTERN='^[[:space:]]*(Skill\(|Invoke|\$[[:space:]]|\* .*(Skill\(|Invoke))'

# ---------------------------------------------------------------------------
# Globals
# ---------------------------------------------------------------------------

USE_BASELINE=false
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
        printf 'audit-interactive-gates: grep not found\n' >&2
        exit 2
    fi
}

require_skills_dir() {
    local found=0
    local d
    for d in "${SCOPE_DIRS[@]}"; do
        if [[ -d "${d}" ]]; then
            found=1
            break
        fi
    done
    if [[ "${found}" -eq 0 ]]; then
        printf 'audit-interactive-gates: skills directory not found at %s\n' \
            "${SKILLS_DIR}" >&2
        exit 2
    fi
}

# ---------------------------------------------------------------------------
# Baseline loading
# ---------------------------------------------------------------------------

# Loads baseline paths (relative to SKILLS_DIR) into BASELINE_PATHS array.
declare -a BASELINE_PATHS=()

load_baseline() {
    if [[ ! -f "${BASELINE_FILE}" ]]; then
        printf 'WARNING: --baseline specified but %s not found; treating as empty\n' \
            "${BASELINE_FILE}" >&2
        return
    fi
    while IFS= read -r line; do
        # Strip comments and blank lines.
        line="${line%%#*}"
        line="${line//[[:space:]]/}"
        [[ -z "${line}" ]] && continue
        BASELINE_PATHS+=("${line}")
    done < "${BASELINE_FILE}"
}

is_in_baseline() {
    local file_abs="$1"
    # file_abs is absolute; compare against SKILLS_DIR-relative paths.
    local rel
    rel="${file_abs#"${SKILLS_DIR}/"}"
    local p
    for p in "${BASELINE_PATHS[@]+"${BASELINE_PATHS[@]}"}"; do
        if [[ "${rel}" == "${p}" ]]; then
            return 0
        fi
    done
    return 1
}

# ---------------------------------------------------------------------------
# Section-aware line filtering
# ---------------------------------------------------------------------------

# Returns 0 if the line is inside a ## Triggers or ## Examples section,
# 1 otherwise.  Uses a simple state machine that tracks the current heading.
#
# Usage: is_in_allowlisted_section <file> <lineno>
#
# This is O(file) per call, which is acceptable for the small files audited.
is_in_allowlisted_section() {
    local file="$1"
    local target_lineno="$2"
    local current_lineno=0
    local in_allowed=false
    local line

    while IFS= read -r line; do
        (( current_lineno++ )) || true

        if [[ "${line}" =~ ^##[[:space:]] ]]; then
            # Any new H2 heading resets the state.
            if [[ "${line}" =~ ^##[[:space:]]+(Triggers|Examples) ]]; then
                in_allowed=true
            else
                in_allowed=false
            fi
        fi

        if [[ "${current_lineno}" -eq "${target_lineno}" ]]; then
            if "${in_allowed}"; then
                return 0
            else
                return 1
            fi
        fi
    done < "${file}"
    return 1
}

# ---------------------------------------------------------------------------
# Regex 1: HALT without AskUserQuestion in a ~30-line window
# ---------------------------------------------------------------------------

audit_halt() {
    local file="$1"
    local lineno="$2"
    # Read ±30 lines around the HALT occurrence and check for AskUserQuestion.
    local start=$(( lineno > 30 ? lineno - 30 : 1 ))
    local window
    window=$(sed -n "${start},$((lineno + 30))p" "${file}" 2>/dev/null || true)
    if printf '%s\n' "${window}" | grep -qE "AskUserQuestion"; then
        return 1  # AskUserQuestion found nearby — not a violation
    fi
    return 0  # No AskUserQuestion found — violation
}

scan_halt_violations() {
    local file="$1"
    local is_reference=false
    [[ "${file}" == */references/*.md ]] && is_reference=true

    local lineno=0
    local line
    local in_frontmatter=false
    local in_code_fence=false

    while IFS= read -r line; do
        (( lineno++ )) || true

        # Skip YAML frontmatter (first ---...--- block at file start).
        if [[ "${lineno}" -eq 1 && "${line}" == "---" ]]; then
            in_frontmatter=true
            continue
        fi
        if "${in_frontmatter}"; then
            if [[ "${line}" == "---" ]]; then
                in_frontmatter=false
            fi
            continue
        fi

        # Track code fences; skip their content.
        if [[ "${line}" =~ ^[[:space:]]*\`\`\` ]]; then
            if "${in_code_fence}"; then in_code_fence=false; else in_code_fence=true; fi
            continue
        fi
        if "${in_code_fence}"; then continue; fi

        # Skip allowlisted sections.
        if is_in_allowlisted_section "${file}" "${lineno}"; then
            continue
        fi

        # Skip documentation lines that describe the legacy HALT output text.
        if printf '%s' "${line}" | grep -qF "HALT text"; then
            continue
        fi

        # Check Regex 1.
        if printf '%s' "${line}" | grep -qE "${HALT_PATTERN}"; then
            if audit_halt "${file}" "${lineno}"; then
                local rel="${file#"${REPO_ROOT}/"}"
                VIOLATION_LINES+=("  ${rel}:${lineno}: HALT without AskUserQuestion in same block")
                (( VIOLATIONS++ )) || true
            fi
        fi
    done < "${file}"
}

# ---------------------------------------------------------------------------
# Regex 2: Deprecated flags in executable context
# ---------------------------------------------------------------------------

# Returns 0 if the line looks like an executable instruction in a
# references/*.md file (Skill( call, Invoke, $ prompt, operational bullet).
is_executable_line_in_references() {
    local line="$1"
    if printf '%s' "${line}" | grep -qE "${REFERENCES_EXEC_PATTERN}"; then
        return 0
    fi
    return 1
}

scan_deprecated_flag_violations() {
    local file="$1"
    local is_reference=false
    [[ "${file}" == */references/*.md ]] && is_reference=true

    local lineno=0
    local line
    local in_frontmatter=false
    local in_code_fence=false

    while IFS= read -r line; do
        (( lineno++ )) || true

        # Skip YAML frontmatter (first ---...--- block at file start).
        if [[ "${lineno}" -eq 1 && "${line}" == "---" ]]; then
            in_frontmatter=true
            continue
        fi
        if "${in_frontmatter}"; then
            if [[ "${line}" == "---" ]]; then
                in_frontmatter=false
            fi
            continue
        fi

        # Track code fences; skip their content.
        if [[ "${line}" =~ ^[[:space:]]*\`\`\` ]]; then
            if "${in_code_fence}"; then in_code_fence=false; else in_code_fence=true; fi
            continue
        fi
        if "${in_code_fence}"; then continue; fi

        # Skip allowlisted sections.
        if is_in_allowlisted_section "${file}" "${lineno}"; then
            continue
        fi

        # For references/*.md, apply Regex 2 only on executable lines.
        if "${is_reference}"; then
            if ! is_executable_line_in_references "${line}"; then
                continue
            fi
        fi

        # Check Regex 2 (deprecated flags).
        if printf '%s' "${line}" | grep -qE "${DEPRECATED_FLAGS_PATTERN}"; then
            # Exclude lines that are documenting the deprecation itself (case-insensitive).
            if printf '%s' "${line}" | grep -iqE "deprecated|no longer needed"; then
                continue
            fi
            local rel="${file#"${REPO_ROOT}/"}"
            local matched
            matched=$(printf '%s' "${line}" | grep -oE "${DEPRECATED_FLAGS_PATTERN}" | head -1)
            VIOLATION_LINES+=("  ${rel}:${lineno}: deprecated flag ${matched} in delegation context")
            (( VIOLATIONS++ )) || true
        fi
    done < "${file}"
}

# ---------------------------------------------------------------------------
# File scanner
# ---------------------------------------------------------------------------

scan_file() {
    local file="$1"

    if "${USE_BASELINE}" && is_in_baseline "${file}"; then
        return
    fi

    scan_halt_violations "${file}"
    scan_deprecated_flag_violations "${file}"
}

# ---------------------------------------------------------------------------
# Main scanner loop
# ---------------------------------------------------------------------------

scan_all() {
    local d

    for d in "${SCOPE_DIRS[@]}"; do
        [[ -d "${d}" ]] || continue

        # Find SKILL.md files (strict audit).
        while IFS= read -r -d '' f; do
            # Exclude core/lib explicitly (not in SCOPE_DIRS, but defensive).
            [[ "${f}" == */core/lib/* ]] && continue
            scan_file "${f}"
            (( FILES_SCANNED++ )) || true
        done < <(find "${d}" -name "SKILL.md" -print0 2>/dev/null)

        # Find references/*.md files (differentiated audit).
        while IFS= read -r -d '' f; do
            [[ "${f}" == */core/lib/* ]] && continue
            scan_file "${f}"
            (( FILES_SCANNED++ )) || true
        done < <(find "${d}" -path "*/references/*.md" -print0 2>/dev/null)
    done
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
            --baseline)
                USE_BASELINE=true
                shift
                ;;
            --skills-dir)
                if [[ $# -lt 2 ]]; then
                    printf 'audit-interactive-gates: --skills-dir requires a path argument\n' >&2
                    exit 2
                fi
                SKILLS_DIR="$2"
                SCOPE_DIRS=(
                    "${SKILLS_DIR}/core/ops"
                    "${SKILLS_DIR}/core/dev"
                    "${SKILLS_DIR}/core/review"
                )
                shift 2
                ;;
            --skills-dir=*)
                SKILLS_DIR="${1#--skills-dir=}"
                SCOPE_DIRS=(
                    "${SKILLS_DIR}/core/ops"
                    "${SKILLS_DIR}/core/dev"
                    "${SKILLS_DIR}/core/review"
                )
                shift
                ;;
            *)
                printf 'audit-interactive-gates: unknown argument: %s\n' "$1" >&2
                exit 2
                ;;
        esac
    done

    require_grep
    require_skills_dir

    if "${USE_BASELINE}"; then
        load_baseline
    fi

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
