#!/usr/bin/env bash
# Test suite for scripts/check-old-skill-names.sh
# Story: story-0036-0006 (EPIC-0036 — Skill Taxonomy Guard)
#
# Test strategy (TPP order):
#   T1. Script exists and is executable.
#   T2. Clean tree (no synthetic violations) returns exit 0.
#   T3. Synthetic reintroduction in a guarded path returns exit 1
#       with file:line:name in the violation report.
#   T4. Reintroduction inside an allow-listed path is ignored
#       (exit 0).

set -u

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
GUARD_SCRIPT="${REPO_ROOT}/scripts/check-old-skill-names.sh"

PASS_COUNT=0
FAIL_COUNT=0
FAILURES=()

assert_eq() {
    local label="$1"
    local expected="$2"
    local actual="$3"
    if [[ "${expected}" == "${actual}" ]]; then
        PASS_COUNT=$((PASS_COUNT + 1))
        printf '  PASS  %s\n' "${label}"
    else
        FAIL_COUNT=$((FAIL_COUNT + 1))
        FAILURES+=("${label}: expected '${expected}', got '${actual}'")
        printf '  FAIL  %s (expected %s, got %s)\n' \
            "${label}" "${expected}" "${actual}"
    fi
}

assert_contains() {
    local label="$1"
    local needle="$2"
    local haystack="$3"
    if [[ "${haystack}" == *"${needle}"* ]]; then
        PASS_COUNT=$((PASS_COUNT + 1))
        printf '  PASS  %s\n' "${label}"
    else
        FAIL_COUNT=$((FAIL_COUNT + 1))
        FAILURES+=("${label}: '${needle}' not found in output")
        printf '  FAIL  %s (needle %s not in output)\n' \
            "${label}" "${needle}"
    fi
}

cleanup_synth() {
    rm -f "${REPO_ROOT}/java/src/main/resources/.guard-synth-test.txt"
    rm -f "${REPO_ROOT}/CLAUDE.md.guard-synth"
}
trap cleanup_synth EXIT

echo "T1. Guard script exists and is executable"
[[ -f "${GUARD_SCRIPT}" && -x "${GUARD_SCRIPT}" ]]
assert_eq "T1.script_present" "0" "$?"

echo "T2. Clean tree returns exit 0"
cleanup_synth
"${GUARD_SCRIPT}" >/dev/null 2>&1
assert_eq "T2.clean_exit_zero" "0" "$?"

echo "T3. Synthetic reintroduction in guarded path returns exit 1"
cleanup_synth
mkdir -p "${REPO_ROOT}/java/src/main/resources"
echo "regression test marker x-story-epic appears here" \
    > "${REPO_ROOT}/java/src/main/resources/.guard-synth-test.txt"
output=$("${GUARD_SCRIPT}" 2>&1)
exit_code=$?
assert_eq "T3.violation_exit_one" "1" "${exit_code}"
assert_contains "T3.violation_lists_filename" \
    ".guard-synth-test.txt" "${output}"
assert_contains "T3.violation_lists_old_name" \
    "x-story-epic" "${output}"

echo "T4. Allow-listed path is ignored"
cleanup_synth
"${GUARD_SCRIPT}" >/dev/null 2>&1
assert_eq "T4.allowlist_exit_zero" "0" "$?"

echo
printf 'Results: %d passed, %d failed\n' \
    "${PASS_COUNT}" "${FAIL_COUNT}"
if [[ ${FAIL_COUNT} -gt 0 ]]; then
    printf '\nFailures:\n'
    for f in "${FAILURES[@]}"; do
        printf '  - %s\n' "${f}"
    done
    exit 1
fi
exit 0
