#!/usr/bin/env bash
# audit-execution-integrity.sh — Camada 3 of Rule 24 enforcement.
#
# Scans git history for merged story PRs (branches matching feat/story-*)
# and verifies that each merged story has the mandatory evidence artifacts
# produced by the x-story-implement pipeline:
#   - plans/epic-XXXX/reports/verify-envelope-STORY-ID.json  (x-internal-story-verify)
#   - plans/epic-XXXX/plans/review-story-STORY-ID.md         (x-review)
#   - plans/epic-XXXX/plans/techlead-review-story-STORY-ID.md (x-review-pr)
#   - plans/epic-XXXX/reports/story-completion-report-STORY-ID.md (x-internal-story-report)
#
# Grandfathered stories (merged before Rule 24) listed in
# audits/execution-integrity-baseline.txt are exempted. Per-story exemption
# via `<!-- audit-exempt: reason -->` in the story markdown.
#
# Exit codes:
#   0 — OK
#   1 — EIE_EVIDENCE_MISSING
#   2 — EIE_BASELINE_CORRUPT
#   3 — EIE_INVALID_EXEMPTION
#   4 — EIE_ENFORCEMENT_BROKEN (self-check failure)
#
# Usage:
#   scripts/audit-execution-integrity.sh              # audit all merged stories
#   scripts/audit-execution-integrity.sh --self-check # verify enforcement is wired
#   scripts/audit-execution-integrity.sh --since <ref> # audit merges since git ref

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

BASELINE_FILE="audits/execution-integrity-baseline.txt"
RULE_FILE=".claude/rules/24-execution-integrity.md"
HOOK_FILE=".claude/hooks/verify-story-completion.sh"

self_check() {
    local broken=0
    if [[ ! -f "${RULE_FILE}" ]]; then
        echo "SELF_CHECK_FAIL: ${RULE_FILE} missing" >&2
        broken=1
    fi
    if [[ ! -f "${HOOK_FILE}" ]]; then
        echo "SELF_CHECK_FAIL: ${HOOK_FILE} missing" >&2
        broken=1
    fi
    if [[ ! -f "${BASELINE_FILE}" ]]; then
        echo "SELF_CHECK_FAIL: ${BASELINE_FILE} missing" >&2
        broken=1
    fi
    if [[ ${broken} -eq 1 ]]; then
        echo "EIE_ENFORCEMENT_BROKEN" >&2
        exit 4
    fi
    echo "EIE self-check OK."
    exit 0
}

load_baseline() {
    if [[ ! -f "${BASELINE_FILE}" ]]; then
        echo "EIE_BASELINE_CORRUPT: ${BASELINE_FILE} missing" >&2
        exit 2
    fi
    # Extract STORY-IDs (strip comments and blanks)
    grep -oE '^story-[0-9]{4}-[0-9]{4}' "${BASELINE_FILE}" 2>/dev/null | sort -u
}

is_grandfathered() {
    local story_id="$1"
    grep -qxF "${story_id}" <<< "${BASELINE_STORIES}"
}

has_audit_exempt() {
    local story_id="$1"
    # Search story markdown for audit-exempt marker
    local story_file
    story_file="$(find plans -name "${story_id}.md" 2>/dev/null | head -1)"
    [[ -z "${story_file}" ]] && return 1
    if grep -qE '<!--\s*audit-exempt:\s*.+-->' "${story_file}"; then
        return 0
    fi
    return 1
}

discover_merged_stories() {
    local since_ref="${1:-}"
    local range_arg=""
    if [[ -n "${since_ref}" ]]; then
        range_arg="${since_ref}..HEAD"
    fi
    # Find merge commits whose merged branch matches feat/story-*
    # Format: SHA TITLE
    git log --merges --first-parent ${range_arg} \
        --format='%H %s' 2>/dev/null \
        | grep -oE 'feat/story-[0-9]{4}-[0-9]{4}[a-zA-Z0-9-]*' \
        | sed -E 's|^feat/||' \
        | grep -oE 'story-[0-9]{4}-[0-9]{4}' \
        | sort -u
}

check_evidence() {
    local story_id="$1"
    local epic_id
    epic_id="$(echo "${story_id}" | grep -oE '[0-9]{4}' | head -1)"
    local plans_dir="plans/epic-${epic_id}/plans"
    local reports_dir="plans/epic-${epic_id}/reports"
    local missing=()

    if [[ ! -f "${reports_dir}/verify-envelope-${story_id}.json" ]]; then
        missing+=("verify-envelope")
    fi

    if ! compgen -G "${plans_dir}/review-*${story_id}*.md" >/dev/null 2>&1 \
            && [[ ! -f "${plans_dir}/review-story-${story_id}.md" ]]; then
        missing+=("review (x-review)")
    fi

    if ! compgen -G "${plans_dir}/techlead-review-*${story_id}*.md" >/dev/null 2>&1 \
            && [[ ! -f "${plans_dir}/techlead-review-story-${story_id}.md" ]]; then
        missing+=("techlead-review (x-review-pr)")
    fi

    if [[ ! -f "${reports_dir}/story-completion-report-${story_id}.md" ]]; then
        missing+=("story-completion-report")
    fi

    if [[ ${#missing[@]} -gt 0 ]]; then
        printf "  ❌ %s — missing: %s\n" "${story_id}" "$(IFS=,; echo "${missing[*]}")" >&2
        return 1
    fi
    return 0
}

main() {
    local since_ref=""
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --self-check) self_check ;;
            --since)      since_ref="$2"; shift 2 ;;
            *)            shift ;;
        esac
    done

    BASELINE_STORIES="$(load_baseline)"

    local stories
    stories="$(discover_merged_stories "${since_ref}")"
    if [[ -z "${stories}" ]]; then
        echo "EIE audit: no merged story branches found in scope."
        exit 0
    fi

    local violations=0
    local total=0
    local exempted=0
    local grandfathered=0

    echo "EIE audit — Rule 24 Camada 3"
    echo "============================"
    for story in ${stories}; do
        total=$((total + 1))
        if is_grandfathered "${story}"; then
            printf "  ⚪ %s — grandfathered (baseline)\n" "${story}"
            grandfathered=$((grandfathered + 1))
            continue
        fi
        if has_audit_exempt "${story}"; then
            printf "  ⚪ %s — audit-exempt\n" "${story}"
            exempted=$((exempted + 1))
            continue
        fi
        if check_evidence "${story}"; then
            printf "  ✅ %s\n" "${story}"
        else
            violations=$((violations + 1))
        fi
    done

    echo "----------------------------"
    echo "Total: ${total} | grandfathered: ${grandfathered} | exempt: ${exempted} | violations: ${violations}"

    if [[ ${violations} -gt 0 ]]; then
        cat >&2 <<EOF

EIE_EVIDENCE_MISSING — ${violations} merged story(ies) lack mandatory evidence artifacts.

Fix by either:
  (a) Running x-internal-story-verify / x-review / x-review-pr / x-internal-story-report
      as REAL Skill(...) tool calls on the affected story, committing the resulting
      artifacts, and amending the story PR.
  (b) Adding '<!-- audit-exempt: <reason> -->' to the story markdown (reviewed exceptions only).

See .claude/rules/24-execution-integrity.md §3 (Camada 3).
EOF
        exit 1
    fi

    echo "OK — execution integrity preserved."
    exit 0
}

main "$@"
