#!/usr/bin/env bash
#
# repro-bug-b.sh — reproduce EPIC-0048 Bug B (CLAUDE.md absent at root).
#
# Bug B description: the `ia-dev-env generate` CLI does NOT produce a
# CLAUDE.md file at the root of the generated project, even though
# FileCategorizer.isRootFile recognizes CLAUDE.md as a root file. No
# assembler currently produces it. The fix in story-0048-0011 adds
# ClaudeMdAssembler (single-responsibility, ADR-0048-B).
#
# This script is the RED-first acceptance test for that story:
#   - On current develop (pre-fix): exit 1 ("Bug B confirmed ...")
#   - On post-fix develop: exit 0
#
# The minimum acceptable CLAUDE.md size is 100 bytes (per story-0048-0001
# DoD §3.4). Smaller files trigger exit 1 with a size message.
#
# Prerequisites:
#   - `ia-dev-env` CLI installed and on PATH (run `./mvnw -pl java -am install`
#     from repo root to install into ~/.local/bin/).
#   - `mktemp`, `stat`, `test` available (any POSIX system).
#
# Exit codes:
#   0 — CLAUDE.md present and >= MIN_SIZE bytes (Bug B absent / fixed)
#   1 — CLAUDE.md absent OR too small (Bug B confirmed)
#   2 — prerequisite failure (CLI missing, generate failed, etc.)
#
# EPIC-0048 / story-0048-0001 / TASK-0048-0001-004

set -euo pipefail

readonly PROFILE="java-spring"
readonly CLI_BIN="${IA_DEV_ENV_BIN:-ia-dev-env}"
readonly MIN_SIZE=100

# --- Prerequisites -----------------------------------------------------

if ! command -v "${CLI_BIN}" >/dev/null 2>&1; then
    printf 'repro-bug-b: CLI %q not found on PATH. Install via: ./mvnw -pl java -am install\n' "${CLI_BIN}" >&2
    exit 2
fi

TMPDIR_OUT="$(mktemp -d -t repro-bug-b-XXXXXX)"
readonly TMPDIR_OUT
GEN_LOG="$(mktemp -t repro-bug-b-gen-XXXXXX)"
readonly GEN_LOG
# shellcheck disable=SC2064
trap "rm -rf '${TMPDIR_OUT}' '${GEN_LOG}'" EXIT

# --- Act ---------------------------------------------------------------

printf 'repro-bug-b: generating profile %q into %q ...\n' "${PROFILE}" "${TMPDIR_OUT}" >&2

if ! "${CLI_BIN}" generate --stack "${PROFILE}" --output "${TMPDIR_OUT}/out" >"${GEN_LOG}" 2>&1; then
    printf 'repro-bug-b: `ia-dev-env generate` failed (exit != 0). Cannot verify Bug B.\n' >&2
    printf 'repro-bug-b: generator output:\n' >&2
    sed 's/^/  /' "${GEN_LOG}" >&2
    exit 2
fi

# --- Assert ------------------------------------------------------------

readonly CLAUDE_MD="${TMPDIR_OUT}/out/CLAUDE.md"

if [[ ! -f "${CLAUDE_MD}" ]]; then
    printf 'repro-bug-b: FAIL — Bug B confirmed: CLAUDE.md not generated at %q\n' "${CLAUDE_MD}" >&2
    exit 1
fi

# stat -f %z on BSD/macOS, -c %s on GNU; wc -c is portable.
SIZE="$(wc -c <"${CLAUDE_MD}" | tr -d ' ')"

if [[ "${SIZE}" -lt "${MIN_SIZE}" ]]; then
    printf 'repro-bug-b: FAIL — Bug B confirmed: CLAUDE.md exists but is too small (%s bytes, minimum %s)\n' "${SIZE}" "${MIN_SIZE}" >&2
    exit 1
fi

printf 'repro-bug-b: PASS — CLAUDE.md generated (%s bytes >= %s)\n' "${SIZE}" "${MIN_SIZE}" >&2
exit 0
