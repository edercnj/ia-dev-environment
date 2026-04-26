#!/usr/bin/env bash
#
# audit-flow-version.sh — Rule 19 (Backward Compatibility) CI audit.
#
# Validates that every execution-state.json under plans/epic-*/ carries
# a valid `flowVersion` field ("1" or "2"). Prevents silent fallback to
# legacy flow when flowVersion is absent or malformed.
#
# Exit codes:
#   0  — all files conform (or only warnings in non-strict mode)
#   1  — FLOW_VERSION_VIOLATION: at least one file has an invalid value
#   1  — FLOW_VERSION_MISSING_STRICT: at least one file is missing the field
#         in --strict mode
#   2  — DEPENDENCY_MISSING: jq absent and grep fallback failed
#   2  — INVALID_ARGS: unknown flag
#
# Flags:
#   --strict      Treat absent flowVersion as a hard violation (exit 1).
#                 Default: absent field is a warning only (exit 0).
#   --self-check  Validate script integrity (deps, permissions). Exit 0 OK / 2 broken.
#   -h|--help     Print usage and exit 0.
#
# Introduced by story-0058-0003 (EPIC-0058). See Rule 19 at
# .claude/rules/19-backward-compatibility.md for the contract.
#
# Catalogado em: docs/audit-gates-catalog.md

set -euo pipefail

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------
SCRIPT_VERSION="1.0.0"
SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
PLANS_GLOB="${REPO_ROOT}/plans/epic-*/execution-state.json"
VALID_VALUES=("1" "2")

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
usage() {
  cat <<-EOF
Usage: ${SCRIPT_NAME} [--strict] [--self-check] [-h|--help]

  Validate flowVersion in all plans/epic-*/execution-state.json files.

  Options:
    --strict      Treat absent flowVersion as a hard violation (exit 1).
    --self-check  Verify script integrity and deps; exit 0 if OK, 2 if broken.
    -h, --help    Print this usage.

  Exit codes:
    0  All files conform.
    1  FLOW_VERSION_VIOLATION or FLOW_VERSION_MISSING_STRICT detected.
    2  DEPENDENCY_MISSING or INVALID_ARGS.
EOF
}

is_valid_flow_version() {
  local v="$1"
  for valid in "${VALID_VALUES[@]}"; do
    [[ "$v" == "$valid" ]] && return 0
  done
  return 1
}

extract_flow_version() {
  local file="$1"
  if command -v jq &>/dev/null; then
    jq -r '.flowVersion // "ABSENT"' "$file" 2>/dev/null || echo "ABSENT"
  else
    # fallback grep: look for "flowVersion": "X"
    local match
    match=$(grep -o '"flowVersion"[[:space:]]*:[[:space:]]*"[^"]*"' "$file" 2>/dev/null \
            | head -1 \
            | sed 's/.*"\([^"]*\)"$/\1/')
    echo "${match:-ABSENT}"
  fi
}

self_check() {
  local ok=1
  if ! command -v bash &>/dev/null; then
    echo "${SCRIPT_NAME}: DEPENDENCY_MISSING: bash not found" >&2
    ok=0
  fi
  # jq is preferred but not strictly required (grep fallback available)
  if ! command -v jq &>/dev/null; then
    echo "${SCRIPT_NAME}: WARNING: jq not found; grep fallback will be used" >&2
  fi
  if [[ $ok -eq 0 ]]; then
    exit 2
  fi
  echo "${SCRIPT_NAME}: OK (version ${SCRIPT_VERSION}, deps ok)"
  exit 0
}

# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------
STRICT=0
for arg in "$@"; do
  case "$arg" in
    --self-check) self_check ;;
    --strict)     STRICT=1 ;;
    -h|--help)    usage; exit 0 ;;
    *) echo "${SCRIPT_NAME}: INVALID_ARGS: unknown flag: $arg" >&2; exit 2 ;;
  esac
done

# ---------------------------------------------------------------------------
# Main scan loop
# ---------------------------------------------------------------------------
violations=0
warnings=0
checked=0

# shellcheck disable=SC2206
files=( ${PLANS_GLOB} )

if [[ ${#files[@]} -eq 0 ]] || [[ ! -f "${files[0]}" ]]; then
  echo "checked 0 files, violations: 0"
  exit 0
fi

for file in "${files[@]}"; do
  [[ -f "$file" ]] || continue
  checked=$((checked + 1))

  flow_version="$(extract_flow_version "$file")"

  if [[ "$flow_version" == "ABSENT" ]]; then
    if [[ $STRICT -eq 1 ]]; then
      echo "${file}: FLOW_VERSION_MISSING_STRICT: flowVersion field absent in --strict mode" >&2
      violations=$((violations + 1))
    else
      echo "${file}: FLOW_VERSION_MISSING: flowVersion absent; defaulting to legacy (warning)" >&2
      warnings=$((warnings + 1))
    fi
  elif ! is_valid_flow_version "$flow_version"; then
    echo "${file}: FLOW_VERSION_VIOLATION: flowVersion=\"${flow_version}\" not in {1,2}" >&2
    violations=$((violations + 1))
  fi
done

echo "checked ${checked} files, violations: ${violations}, warnings: ${warnings}"

[[ $violations -eq 0 ]] && exit 0 || exit 1
