#!/usr/bin/env bash
# audit-bypass-flags.sh — story-0057-0005 (EPIC-0057).
#
# Detects bypass flags (--no-ci-watch, --no-auto-remediation,
# --skip-pr-comments, --no-github-release, --no-jira) used outside
# `## Recovery` or `## Error Handling` sections in SKILL.md files
# under java/src/main/resources/targets/claude/skills/. Per Rule 24
# §30 + Rule 45, every --no-* / --skip-* flag that effectively skips
# a mandatory sub-skill must be confined to documented Recovery
# contexts. Happy-path occurrences are violations.
#
# Exit codes:
#   0 — OK (no hard violations; soft warnings only or none)
#   1 — BYPASS_FLAG_VIOLATION (at least one hard violation)
#   2 — usage error
#   3 — SOFT_WARNINGS (informational; --strict promotes to exit 1)
#
# Usage:
#   scripts/audit-bypass-flags.sh                    # default skills root
#   scripts/audit-bypass-flags.sh --skills-root <p>  # custom root
#   scripts/audit-bypass-flags.sh --json             # JSON envelope
#   scripts/audit-bypass-flags.sh --strict           # fail on soft warnings
#   scripts/audit-bypass-flags.sh --help             # usage banner

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

DEFAULT_SKILLS_ROOT="java/src/main/resources/targets/claude/skills"

# (flag, severity)
HARD_FLAGS=(
    "--no-ci-watch"
    "--no-auto-remediation"
    "--skip-pr-comments"
    "--no-github-release"
)
SOFT_FLAGS=(
    "--no-jira"
)

usage_error() {
    cat >&2 <<EOF
usage: $(basename "$0") [--skills-root <path>] [--json] [--strict]
                       [--help]

  --skills-root <p>   override the default skills root
                      (default: ${DEFAULT_SKILLS_ROOT})
  --json              emit a single-line JSON envelope on stdout
  --strict            promote soft warnings to hard violations
  --help              this banner

Detects --no-* / --skip-* bypass flags outside ## Recovery /
## Error Handling sections in SKILL.md files. Per Rule 24 §30
+ Rule 45 §--no-ci-watch Constraints.
EOF
    exit 2
}

# Find the most-recent ## section header before line N in file F.
# Echoes the section title (e.g., "Recovery", "Phase 2 — Implement").
section_for_line() {
    local file="$1"
    local target_line="$2"
    awk -v limit="${target_line}" '
        /^## / && NR <= limit { last = substr($0, 4) }
        END { print last }
    ' "${file}"
}

is_recovery_context() {
    local section="$1"
    case "${section}" in
        Recovery*|*"Error Handling"*|*"Recovery"*) return 0 ;;
        *) return 1 ;;
    esac
}

scan_flag_in_file() {
    local skill_file="$1"
    local flag="$2"
    local severity="$3"
    local out_file="$4"

    # Match the flag preceded by whitespace, followed by any non-word char
    # or EOL. Word-char follow would mean the flag is a substring of a
    # longer token (e.g., --no-ci-watch-extended) and is not the actual
    # bypass flag.
    grep -nE -- "[[:space:]]${flag}([^[:alnum:]_-]|$)" "${skill_file}" \
            2>/dev/null | while IFS=: read -r line_no _; do
        local section
        section="$(section_for_line "${skill_file}" "${line_no}")"
        if [[ -z "${section}" ]] || ! is_recovery_context "${section}"; then
            local skill_name
            skill_name="$(basename "$(dirname "${skill_file}")")"
            printf '%s\t%s\t%s\t%d\t%s\n' \
                "${skill_name}" "${flag}" "${severity}" \
                "${line_no}" "${section:-<top>}" \
                >> "${out_file}"
        fi
    done
}

main() {
    local skills_root="${DEFAULT_SKILLS_ROOT}"
    local json_mode="false"
    local strict_mode="false"

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --skills-root)
                if [[ $# -lt 2 || -z "${2:-}" || "${2:0:2}" == "--" ]]; then
                    echo "error: --skills-root requires a path" >&2
                    usage_error
                fi
                skills_root="$2"
                shift 2
                ;;
            --json) json_mode="true"; shift ;;
            --strict) strict_mode="true"; shift ;;
            -h|--help) usage_error ;;
            *)
                echo "error: unknown flag '$1'" >&2
                usage_error
                ;;
        esac
    done

    if [[ ! -d "${skills_root}" ]]; then
        echo "error: skills root '${skills_root}' is not a directory" >&2
        exit 2
    fi

    local tmp
    tmp="$(mktemp -t audit-bypass.XXXXXX)"
    trap 'rm -f "${tmp}"' EXIT

    local scanned=0
    while IFS= read -r -d '' skill_file; do
        scanned=$((scanned + 1))
        for flag in "${HARD_FLAGS[@]}"; do
            scan_flag_in_file "${skill_file}" "${flag}" "hard" "${tmp}"
        done
        for flag in "${SOFT_FLAGS[@]}"; do
            scan_flag_in_file "${skill_file}" "${flag}" "soft" "${tmp}"
        done
    done < <(find "${skills_root}" -name SKILL.md -type f -print0)

    local hard_count=0
    local soft_count=0
    if [[ -s "${tmp}" ]]; then
        hard_count=$(grep -c $'\thard\t' "${tmp}" || true)
        soft_count=$(grep -c $'\tsoft\t' "${tmp}" || true)
    fi

    if [[ "${json_mode}" == "true" ]]; then
        local violations_json="["
        local first="true"
        while IFS=$'\t' read -r skill flag sev line section; do
            [[ -z "${skill}" ]] && continue
            if [[ "${first}" == "true" ]]; then
                first="false"
            else
                violations_json+=","
            fi
            violations_json+=$(printf \
                '{"skill":"%s","flag":"%s","line":%d,"context":"%s","severity":"%s"}' \
                "${skill}" "${flag}" "${line}" "${section}" "${sev}")
        done < "${tmp}"
        violations_json+="]"

        local status="OK"
        if [[ "${hard_count}" -gt 0 ]]; then
            status="BYPASS_FLAG_VIOLATION"
        elif [[ "${soft_count}" -gt 0 ]]; then
            status="SOFT_WARNINGS"
        fi
        printf '{"status":"%s","skillsScanned":%d,"violationsFound":%d,"violations":%s}\n' \
            "${status}" "${scanned}" \
            "$((hard_count + soft_count))" \
            "${violations_json}"
    else
        echo "Bypass-flag audit — Rule 24 §30 + Rule 45"
        echo "=========================================="
        echo "Skills scanned: ${scanned}"
        if [[ -s "${tmp}" ]]; then
            echo "Violations:"
            while IFS=$'\t' read -r skill flag sev line section; do
                [[ -z "${skill}" ]] && continue
                local glyph="❌"
                [[ "${sev}" == "soft" ]] && glyph="⚠️"
                printf '  %s %s — %s @ line %d (section: %s) [%s]\n' \
                    "${glyph}" "${skill}" "${flag}" "${line}" "${section}" "${sev}"
            done < "${tmp}"
        else
            echo "No bypass-flag occurrences found in happy-path."
        fi
        echo "------------------------------------------"
        echo "Hard: ${hard_count} | Soft: ${soft_count}"
    fi

    if [[ "${hard_count}" -gt 0 ]]; then
        exit 1
    fi
    if [[ "${soft_count}" -gt 0 && "${strict_mode}" == "true" ]]; then
        exit 1
    fi
    if [[ "${soft_count}" -gt 0 ]]; then
        exit 3
    fi
    exit 0
}

main "$@"
