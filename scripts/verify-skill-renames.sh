#!/usr/bin/env bash
#
# EPIC-0036 / STORY-0036-0006 — Skill Rename Guard Script
#
# Fails the build if any of the 19 pre-EPIC-0036 skill names
# reappear in source files. Allowed locations (historical
# record) are excluded via ALLOWED_PATHS below.
#
# Usage:
#   ./scripts/verify-skill-renames.sh
#
# Exit codes:
#   0  no forbidden names found
#   1  at least one forbidden name found (build should fail)
#   2  invoked from an unexpected directory or the underlying
#      scanner (rg/grep) failed for a reason other than
#      "no matches"
#
# CI integration is intentionally deferred — invoke the script
# manually or via a developer pre-push hook until a CI
# workflow lands in a follow-up PR.

set -euo pipefail

# Locate repo root (script may be invoked from anywhere)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [[ ! -d "${REPO_ROOT}/adr" || ! -f "${REPO_ROOT}/CLAUDE.md" ]]; then
    echo "ERROR: repo root not detected at ${REPO_ROOT}" >&2
    exit 2
fi

cd "${REPO_ROOT}"

# Full list of pre-EPIC-0036 skill names. Keep this sorted by
# story so audits can quickly map back to the rename commit.
declare -a FORBIDDEN_NAMES=(
    # STORY-0036-0004 (primary cluster)
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
    # STORY-0036-0005 (remaining)
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

# Paths allowed to mention old names. Three buckets:
#   1. This script itself (stores the forbidden list).
#   2. Authoritative migration record (ADR-0003 + CHANGELOG +
#      past epic plans / specs / steering / results) — frozen
#      historical artifacts that document what was done at
#      the time. Renaming history would falsify the record.
#   3. Generated output subtrees that ship pre-rename names
#      from older releases (e.g., legacy README listings).
# Build outputs (.git, target/) are excluded for speed.
#
# IMPORTANT: do NOT exclude tracked source directories
# wholesale. `.github/workflows/` and `.claude/rules/` ARE
# tracked and the guard MUST catch regressions there. Use
# the most specific subtree that contains generated content
# rather than the platform root.
declare -a ALLOWED_PATHS=(
    "scripts/verify-skill-renames.sh"
    "plans/"
    "adr/ADR-0003-skill-taxonomy-and-naming.md"
    "specs/"
    "steering/"
    "results/"
    "CHANGELOG.md"
    ".git/"
    "java/target/"
    "target/"
    # Generated skill assemblies (shipped from older releases
    # may still ship pre-rename listings — these are output,
    # not source).
    ".agents/skills/"
    ".claude/skills/"
    ".github/skills/"
    ".codex/skills/"
    # Example project templates that ship runnable scripts
    # literally named run-smoke-api.sh / run-smoke-socket.sh.
    # These are file paths, not pre-EPIC-0036 skill names.
    "java/src/main/resources/shared/templates/examples/"
)

is_allowed() {
    local file="$1"
    for prefix in "${ALLOWED_PATHS[@]}"; do
        if [[ "${file}" == "${prefix}"* ]]; then
            return 0
        fi
    done
    return 1
}

echo "Scanning repository for pre-EPIC-0036 skill names..."
echo "  Patterns: ${#FORBIDDEN_NAMES[@]}"
echo "  Repo root: ${REPO_ROOT}"

# Pass each forbidden name to the scanner as a fixed-string
# search term; no regex alternation or escaping is needed.
# Capture the scanner exit status separately so we can
# distinguish "no matches" (exit 1, expected for clean repos)
# from "scan failed" (exit 2+, must surface as a hard error).
RAW_MATCHES=""
scan_status=0
if command -v rg >/dev/null 2>&1; then
    set +e
    RAW_MATCHES="$(rg --fixed-strings --line-number \
        --no-heading --with-filename \
        --glob '!.git' --glob '!target' --glob '!**/target' \
        $(printf -- '-e %s ' "${FORBIDDEN_NAMES[@]}") \
        .)"
    scan_status=$?
    set -e
    if [[ "${scan_status}" -gt 1 ]]; then
        echo "ERROR: ripgrep scan failed with exit code ${scan_status}" >&2
        exit 2
    fi
else
    set +e
    RAW_MATCHES="$(grep -rHn -F \
        --exclude-dir='.git' \
        --exclude-dir='target' \
        $(printf -- '-e %s ' "${FORBIDDEN_NAMES[@]}") \
        .)"
    scan_status=$?
    set -e
    if [[ "${scan_status}" -gt 1 ]]; then
        echo "ERROR: grep scan failed with exit code ${scan_status}" >&2
        exit 2
    fi
fi

FORBIDDEN_HITS=0
while IFS= read -r line; do
    [[ -z "${line}" ]] && continue
    # Strip leading "./"
    file="${line#./}"
    file="${file%%:*}"
    if is_allowed "${file}"; then
        continue
    fi
    if [[ "${FORBIDDEN_HITS}" -eq 0 ]]; then
        echo ""
        echo "FORBIDDEN: pre-EPIC-0036 skill names found in source files:"
    fi
    echo "  ${line}"
    FORBIDDEN_HITS=$((FORBIDDEN_HITS + 1))
done <<< "${RAW_MATCHES}"

if [[ "${FORBIDDEN_HITS}" -gt 0 ]]; then
    echo ""
    echo "Found ${FORBIDDEN_HITS} forbidden occurrence(s)."
    echo "See adr/ADR-0003-skill-taxonomy-and-naming.md for the authoritative mapping."
    exit 1
fi

echo "OK: no pre-EPIC-0036 skill names found outside allowed locations."
exit 0
