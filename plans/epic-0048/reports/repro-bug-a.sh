#!/usr/bin/env bash
#
# repro-bug-a.sh — reproduce EPIC-0048 Bug A (empty directories in output).
#
# Bug A description: the `ia-dev-env generate` CLI creates directories
# like `.github/` (and possibly others) as EMPTY leaf dirs in the output
# tree. The fix in story-0048-0009 will enforce "zero empty directories"
# via CopyHelpers + pruneEmptyDirs post-assembly step.
#
# This script is the RED-first acceptance test for that story:
#   - Expected pre-fix semantics: exit 1 with list of empty dirs
#   - Expected post-fix semantics: exit 0
#
# IMPORTANT — empirical finding from TASK-0048-0001-003 on develop commit
# d8f7ff0c2 (2026-04-22): the CLI produces ZERO empty directories across
# ALL 17 bundled stacks (9 Java + 8 non-Java). Bug A is NOT REPRODUCIBLE
# on current develop. This script therefore returns exit 0 on develop
# today. See investigation-report.md §3 for the full scan results and
# implications for story-0048-0009. The script remains useful as a
# regression gate — if Bug A surfaces in a future commit, this test
# will catch it.
#
# Prerequisites:
#   - `ia-dev-env` CLI installed and on PATH (run `./mvnw -pl java -am install`
#     from repo root to install into ~/.local/bin/).
#   - `mktemp` available (any POSIX system).
#
# Exit codes:
#   0 — no empty directories found (Bug A absent / fixed)
#   1 — empty directories found (Bug A confirmed)
#   2 — prerequisite failure (CLI missing, generate failed, etc.)
#
# EPIC-0048 / story-0048-0001 / TASK-0048-0001-003

set -euo pipefail

readonly PROFILE="java-spring"
readonly CLI_BIN="${IA_DEV_ENV_BIN:-ia-dev-env}"

# --- Prerequisites -----------------------------------------------------

if ! command -v "${CLI_BIN}" >/dev/null 2>&1; then
    printf 'repro-bug-a: CLI %q not found on PATH. Install via: ./mvnw -pl java -am install\n' "${CLI_BIN}" >&2
    exit 2
fi

TMPDIR_OUT="$(mktemp -d -t repro-bug-a-XXXXXX)"
readonly TMPDIR_OUT
trap 'rm -rf "${TMPDIR_OUT}"' EXIT

# --- Act ---------------------------------------------------------------

printf 'repro-bug-a: generating profile %q into %q ...\n' "${PROFILE}" "${TMPDIR_OUT}" >&2

GEN_LOG="$(mktemp -t repro-bug-a-gen-XXXXXX)"
readonly GEN_LOG
if ! "${CLI_BIN}" generate --stack "${PROFILE}" --output "${TMPDIR_OUT}/out" >"${GEN_LOG}" 2>&1; then
    printf 'repro-bug-a: `ia-dev-env generate` failed (exit != 0). Cannot verify Bug A.\n' >&2
    printf 'repro-bug-a: generator output:\n' >&2
    sed 's/^/  /' "${GEN_LOG}" >&2
    rm -f "${GEN_LOG}"
    exit 2
fi
rm -f "${GEN_LOG}"

# --- Assert ------------------------------------------------------------

EMPTY_DIRS_FILE="$(mktemp -t repro-bug-a-empty-XXXXXX)"
readonly EMPTY_DIRS_FILE
# shellcheck disable=SC2064
trap "rm -rf '${TMPDIR_OUT}' '${EMPTY_DIRS_FILE}'" EXIT

find "${TMPDIR_OUT}/out" -type d -empty >"${EMPTY_DIRS_FILE}"

EMPTY_COUNT="$(wc -l <"${EMPTY_DIRS_FILE}" | tr -d ' ')"

if [[ "${EMPTY_COUNT}" -eq 0 ]]; then
    printf 'repro-bug-a: PASS — no empty directories found in %q\n' "${TMPDIR_OUT}/out" >&2
    exit 0
fi

printf 'repro-bug-a: FAIL — Bug A confirmed. %s empty director(y|ies) found:\n' "${EMPTY_COUNT}" >&2
# Strip the tmpdir prefix for readable output.
sed "s|${TMPDIR_OUT}/out|<output>|g" "${EMPTY_DIRS_FILE}" | sort -u >&2

exit 1
