#!/usr/bin/env bash
# check-old-skill-names.sh
#
# CI guard: fails the build if any of the 19 OLD skill names from
# EPIC-0036 (Skill Taxonomy and Naming Refactor) are reintroduced
# anywhere in the repository outside the explicitly allow-listed
# locations.
#
# Source of truth for the rename mapping:
#   - adr/ADR-0003-skill-taxonomy-and-naming.md
#   - plans/epic-0036/skill-renames.md (sections 2.1 / 2.2 / 2.3)
#
# Exit codes:
#   0 — clean (no violations)
#   1 — at least one old name was found in a non-allow-listed file
#   2 — invocation error (missing dependency, etc.)


set -u

# Resolve repo root so the script works from any cwd.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SCRIPT_REL="scripts/check-old-skill-names.sh"

# 19 forbidden old names (10 from STORY-0036-0004 + 9 from
# STORY-0036-0005). Order matches the rename table in
# plans/epic-0036/skill-renames.md.
FORBIDDEN_NAMES=(
    "x-story-epic"
    "x-story-epic-full"
    "x-story-map"
    "x-epic-plan"
    "x-dev-implement"
    "x-dev-story-implement"
    "x-dev-epic-implement"
    "x-dev-architecture-plan"
    "x-dev-arch-update"
    "x-dev-adr-automation"
    "run-e2e"
    "run-smoke-api"
    "run-smoke-socket"
    "run-contract-tests"
    "run-perf-test"
    "x-pr-fix-comments"
    "x-pr-fix-epic-comments"
    "x-runtime-protection"
    "x-security-secret-scan"
)

# Allow-listed paths (relative to repo root). A match is suppressed
# when its file path begins with any of these prefixes. The list
# covers historical artifacts (plans, ADRs, specs, results, prior
# CHANGELOG entries), generated outputs (.claude, golden tests),
# build directories, and the guard machinery itself.
EXCLUDED_PREFIXES=(
    ".git/"
    ".claude/"
    "java/target/"
    "java/src/test/resources/golden/"
    "target/"
    "build/"
    "node_modules/"
    "plans/"
    "adr/"
    "specs/"
    "results/"
    "tests/guard/"
    "docs/release-notes/"
    "scripts/check-old-skill-names.sh"
    "CHANGELOG.md"
    # Prune regression test literally references the old
    # names to assert they get removed from output.
    "java/src/test/java/dev/iadev/application/assembler/SkillsAssemblerPruneTest.java"
)

is_excluded() {
    local path="$1"
    local prefix
    for prefix in "${EXCLUDED_PREFIXES[@]}"; do
        if [[ "${path}" == "${prefix}"* ]]; then
            return 0
        fi
    done
    return 1
}

require_grep() {
    if ! command -v grep >/dev/null 2>&1; then
        echo "check-old-skill-names: 'grep' not available" >&2
        exit 2
    fi
}

build_pattern_file() {
    local pattern_file="$1"
    local name
    : > "${pattern_file}"
    for name in "${FORBIDDEN_NAMES[@]}"; do
        printf '%s\n' "${name}" >> "${pattern_file}"
    done
}

extract_matched_name() {
    local content="$1"
    local name
    for name in "${FORBIDDEN_NAMES[@]}"; do
        if [[ "${content}" == *"${name}"* ]]; then
            printf '%s' "${name}"
            return 0
        fi
    done
    printf 'unknown'
}

scan_repository() {
    local violations_file="$1"
    local pattern_file
    pattern_file="$(mktemp -t guard-patterns.XXXXXX)"
    build_pattern_file "${pattern_file}"

    cd "${REPO_ROOT}" || exit 2
    : > "${violations_file}"

    # Single grep over the whole tree using the pattern file
    # (-F fixed strings, -f patterns, -n line numbers,
    # -r recursive). --exclude-dir prunes large build trees up
    # front so the script meets the < 5s NFR even on a populated
    # repo.
    grep -F -n -r -f "${pattern_file}" \
        --binary-files=without-match \
        --exclude-dir=.git \
        --exclude-dir=target \
        --exclude-dir=build \
        --exclude-dir=node_modules \
        . 2>/dev/null \
        | sed 's|^\./||' \
        | while IFS=: read -r file line content; do
            if [[ -z "${file}" || -z "${line}" ]]; then
                continue
            fi
            if is_excluded "${file}"; then
                continue
            fi
            local matched
            matched=$(extract_matched_name "${content}")
            printf '%s:%s:%s:%s\n' \
                "${file}" "${line}" "${matched}" "${content}" \
                >> "${violations_file}"
        done

    rm -f "${pattern_file}"
}

main() {
    require_grep
    local violations_file
    violations_file="$(mktemp -t guard-violations.XXXXXX)"
    trap 'rm -f "${violations_file}"' EXIT

    scan_repository "${violations_file}"

    if [[ ! -s "${violations_file}" ]]; then
        echo "check-old-skill-names: clean (0 violations)"
        exit 0
    fi

    local count
    count=$(wc -l < "${violations_file}" | tr -d ' ')
    echo "check-old-skill-names: ${count} violation(s) found" >&2
    echo "Old EPIC-0036 skill names appear in tracked files" \
        "outside the allow-list." >&2
    echo "Allowed locations: plans/, adr/, CHANGELOG.md," \
        "docs/release-notes/, .claude/ (generated)," \
        "java/src/test/resources/golden/ (generated)." >&2
    echo "" >&2
    echo "Violations (file:line:old_name:context):" >&2
    cat "${violations_file}" >&2
    exit 1
}

main "$@"
