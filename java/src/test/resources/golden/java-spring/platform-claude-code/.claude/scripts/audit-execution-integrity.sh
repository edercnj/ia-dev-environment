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
# Source-of-truth fallback paths used when the runtime .claude/ tree is
# absent (CI checkouts — .claude/ is gitignored as a generated output).
RULE_SOT="java/src/main/resources/targets/claude/rules/24-execution-integrity.md"
HOOK_SOT="java/src/main/resources/targets/claude/hooks/verify-story-completion.sh"

self_check() {
    local broken=0
    if [[ ! -f "${RULE_FILE}" && ! -f "${RULE_SOT}" ]]; then
        echo "SELF_CHECK_FAIL: ${RULE_FILE} (and SOT ${RULE_SOT}) missing" >&2
        broken=1
    fi
    if [[ ! -f "${HOOK_FILE}" && ! -f "${HOOK_SOT}" ]]; then
        echo "SELF_CHECK_FAIL: ${HOOK_FILE} (and SOT ${HOOK_SOT}) missing" >&2
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
    # Returns:
    #   0 — marker present with a non-empty reason
    #   1 — marker absent (or story file not found)
    #   3 — marker present but malformed (empty / whitespace-only reason)
    local story_id="$1"
    local story_file
    story_file="$(find plans -name "${story_id}.md" 2>/dev/null | head -1)"
    [[ -z "${story_file}" ]] && return 1
    # Look for any audit-exempt marker (valid or malformed)
    if ! grep -qE '<!--\s*audit-exempt' "${story_file}"; then
        return 1
    fi
    # Valid form MUST have a non-empty reason:
    #   <!-- audit-exempt: <at least one non-space char> -->
    if grep -qE '<!--\s*audit-exempt:\s*[^[:space:]-][^-]*-->' "${story_file}"; then
        return 0
    fi
    # Marker present but malformed
    echo "EIE_INVALID_EXEMPTION: ${story_file} has audit-exempt marker without a reason" >&2
    return 3
}

discover_merged_stories() {
    # Merge-strategy-agnostic story discovery:
    # scans first-parent commit subjects for `feat(story-XXXX-YYYY...)` OR
    # `fix(story-XXXX-YYYY...)` AND also inspects `--merges` commits for
    # `feat/story-*` merged branch names. Covers squash/rebase (conventional
    # commit subject) and merge-commit strategies uniformly.
    local since_ref="${1:-}"
    local range_arg=""
    if [[ -n "${since_ref}" ]]; then
        range_arg="${since_ref}..HEAD"
    fi
    {
        # Squash/rebase: conventional commit subjects on first-parent
        git log --first-parent ${range_arg} --format='%s' 2>/dev/null \
            | grep -oE '(feat|fix)\(story-[0-9]{4}-[0-9]{4}' \
            | grep -oE 'story-[0-9]{4}-[0-9]{4}'
        # Merge commits referencing feat/story-* branches
        git log --merges --first-parent ${range_arg} \
            --format='%H %s' 2>/dev/null \
            | grep -oE 'feat/story-[0-9]{4}-[0-9]{4}[a-zA-Z0-9-]*' \
            | sed -E 's|^feat/||' \
            | grep -oE 'story-[0-9]{4}-[0-9]{4}'
    } | sort -u
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

    # EPIC-0057 — hard artefact for x-dependency-audit (Camada 3, Rule 24
    # §32-42 expanded). Mirrors the .conf HARD_DEPENDENCY_AUDIT pattern.
    if [[ ! -f "${reports_dir}/dependency-audit-${story_id}.md" ]]; then
        missing+=("dependency-audit (x-dependency-audit)")
    fi

    # EPIC-0057 — hard artefact for x-pr-watch-ci (Rule 45). State file
    # is keyed by PR number; we resolve it from execution-state.json
    # storyStatuses.<id>.prNumber when present and fall back to a
    # directory-presence check otherwise. The .claude/state/ directory
    # itself is a precondition — its absence indicates the runtime tree
    # has never been generated, in which case the check short-circuits
    # (CI checkouts that don't generate .claude/ are not penalised).
    local pr_state_dir=".claude/state"
    local pr_number=""
    local epic_num
    epic_num=$(echo "${story_id}" | grep -oE '^story-[0-9]{4}' | sed 's/story-//')
    local exec_state="plans/epic-${epic_num}/execution-state.json"
    if [[ -f "${exec_state}" ]] && command -v jq >/dev/null 2>&1; then
        pr_number=$(jq -r --arg sid "${story_id}" \
            '.storyStatuses[$sid].prNumber // empty' \
            "${exec_state}" 2>/dev/null || true)
    fi
    if [[ -d "${pr_state_dir}" ]]; then
        if [[ -n "${pr_number}" ]]; then
            if [[ ! -f "${pr_state_dir}/pr-watch-${pr_number}.json" ]]; then
                missing+=("pr-watch (x-pr-watch-ci) → ${pr_state_dir}/pr-watch-${pr_number}.json")
            fi
        else
            # PR number unknown — fall back to directory-level presence.
            if ! compgen -G "${pr_state_dir}/pr-watch-[0-9]*.json" >/dev/null 2>&1; then
                missing+=("pr-watch (x-pr-watch-ci) — no pr-watch-*.json under ${pr_state_dir}")
            fi
        fi
    fi

    if [[ ${#missing[@]} -gt 0 ]]; then
        printf "  ❌ %s — missing: %s\n" "${story_id}" "$(IFS=,; echo "${missing[*]}")" >&2
        return 1
    fi
    return 0
}

usage_error() {
    cat >&2 <<EOF
usage: $(basename "$0") [--self-check] [--since <git-ref>]
                        [--story-id <story-XXXX-YYYY>] [--json]

  --self-check        verify enforcement infrastructure is wired
  --since <ref>       limit scan to merges since the given git ref
  --story-id <id>     audit only the specified story (skips git log)
  --json              emit a single-line JSON envelope on stdout
                      (status, storiesAudited, storiesPassed,
                       storiesFailed, failures[])
EOF
    exit 2
}

emit_json_envelope() {
    local status="$1"
    local total="$2"
    local passed="$3"
    local failed="$4"
    local failures_json="$5"
    printf '{"status":"%s","storiesAudited":%d,"storiesPassed":%d,"storiesFailed":%d,"failures":%s}\n' \
        "${status}" "${total}" "${passed}" "${failed}" "${failures_json}"
}

main() {
    local since_ref=""
    local single_story=""
    local json_mode="false"
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --self-check) self_check ;;
            --since)
                if [[ $# -lt 2 || -z "${2:-}" || "${2:0:2}" == "--" ]]; then
                    echo "error: --since requires a git-ref argument" >&2
                    usage_error
                fi
                since_ref="$2"
                shift 2
                ;;
            --story-id)
                if [[ $# -lt 2 || -z "${2:-}" || "${2:0:2}" == "--" ]]; then
                    echo "error: --story-id requires a story id argument" >&2
                    usage_error
                fi
                if ! [[ "$2" =~ ^story-[0-9]{4}-[0-9]{4}$ ]]; then
                    echo "error: --story-id must match story-XXXX-YYYY pattern" >&2
                    usage_error
                fi
                single_story="$2"
                shift 2
                ;;
            --json)
                json_mode="true"
                shift
                ;;
            -h|--help) usage_error ;;
            *)
                echo "error: unknown flag '$1'" >&2
                usage_error
                ;;
        esac
    done

    BASELINE_STORIES="$(load_baseline)"

    local stories
    if [[ -n "${single_story}" ]]; then
        stories="${single_story}"
    else
        stories="$(discover_merged_stories "${since_ref}")"
    fi
    if [[ -z "${stories}" ]]; then
        if [[ "${json_mode}" == "true" ]]; then
            emit_json_envelope "OK" 0 0 0 "[]"
        else
            echo "EIE audit: no merged story branches found in scope."
        fi
        exit 0
    fi

    local violations=0
    local passed=0
    local total=0
    local exempted=0
    local grandfathered=0
    local invalid_exemptions=0
    local failures_json="["
    local failures_first="true"

    if [[ "${json_mode}" != "true" ]]; then
        echo "EIE audit — Rule 24 Camada 3"
        echo "============================"
    fi
    for story in ${stories}; do
        total=$((total + 1))
        if is_grandfathered "${story}"; then
            [[ "${json_mode}" != "true" ]] && \
                printf "  ⚪ %s — grandfathered (baseline)\n" "${story}"
            grandfathered=$((grandfathered + 1))
            passed=$((passed + 1))
            continue
        fi
        has_audit_exempt "${story}"
        local exempt_rc=$?
        if [[ ${exempt_rc} -eq 0 ]]; then
            [[ "${json_mode}" != "true" ]] && \
                printf "  ⚪ %s — audit-exempt\n" "${story}"
            exempted=$((exempted + 1))
            passed=$((passed + 1))
            continue
        elif [[ ${exempt_rc} -eq 3 ]]; then
            [[ "${json_mode}" != "true" ]] && \
                printf "  ❌ %s — malformed audit-exempt marker\n" "${story}" >&2
            invalid_exemptions=$((invalid_exemptions + 1))
            # Append to failures envelope so JSON consumers see WHICH
            # story has the malformed marker (Copilot review feedback —
            # PR #653 review comment 3).
            if [[ "${failures_first}" == "true" ]]; then
                failures_first="false"
            else
                failures_json+=","
            fi
            failures_json+="{\"storyId\":\"${story}\",\"status\":\"EIE_INVALID_EXEMPTION\"}"
            continue
        fi
        if check_evidence "${story}"; then
            [[ "${json_mode}" != "true" ]] && printf "  ✅ %s\n" "${story}"
            passed=$((passed + 1))
        else
            violations=$((violations + 1))
            if [[ "${failures_first}" == "true" ]]; then
                failures_first="false"
            else
                failures_json+=","
            fi
            failures_json+="{\"storyId\":\"${story}\",\"status\":\"EIE_EVIDENCE_MISSING\"}"
        fi
    done
    failures_json+="]"

    if [[ "${json_mode}" != "true" ]]; then
        echo "----------------------------"
        echo "Total: ${total} | grandfathered: ${grandfathered} | exempt: ${exempted} | invalid-exempt: ${invalid_exemptions} | violations: ${violations}"
    fi

    if [[ ${invalid_exemptions} -gt 0 ]]; then
        if [[ "${json_mode}" == "true" ]]; then
            emit_json_envelope "EIE_INVALID_EXEMPTION" \
                "${total}" "${passed}" "${invalid_exemptions}" "${failures_json}"
        else
            cat >&2 <<EOF

EIE_INVALID_EXEMPTION — ${invalid_exemptions} story(ies) have malformed '<!-- audit-exempt -->' markers.

Fix: the marker MUST carry a non-empty reason, e.g.:
  <!-- audit-exempt: reason=migration-only-no-runtime-code approved-by=tech-lead date=YYYY-MM-DD -->
EOF
        fi
        exit 3
    fi

    if [[ ${violations} -gt 0 ]]; then
        if [[ "${json_mode}" == "true" ]]; then
            emit_json_envelope "EIE_EVIDENCE_MISSING" \
                "${total}" "${passed}" "${violations}" "${failures_json}"
        else
            cat >&2 <<EOF

EIE_EVIDENCE_MISSING — ${violations} merged story(ies) lack mandatory evidence artifacts.

Fix by either:
  (a) Running x-internal-story-verify / x-review / x-review-pr / x-internal-story-report
      as REAL Skill(...) tool calls on the affected story, committing the resulting
      artifacts, and amending the story PR.
  (b) Adding '<!-- audit-exempt: <reason> -->' to the story markdown (reviewed exceptions only).

See .claude/rules/24-execution-integrity.md §3 (Camada 3).
EOF
        fi
        exit 1
    fi

    if [[ "${json_mode}" == "true" ]]; then
        emit_json_envelope "OK" "${total}" "${passed}" 0 "[]"
    else
        echo "OK — execution integrity preserved."
    fi
    exit 0
}

main "$@"
