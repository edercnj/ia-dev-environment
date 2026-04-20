# Story Planning Report -- story-0045-0005

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0045-0005 |
| Epic ID | 0045 |
| Date | 2026-04-20 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story 0045-0005 adds **opt-in** `--ci-watch` flag to `x-release`, inserting Phase CI-WATCH between OPEN-RELEASE-PR and APPROVAL-GATE (Phase 8). Unlike stories 0003/0004 (CI-Watch default in v2), release preserves backward compat: flag absent → current flow unchanged. Hard abort on exit 20/30 (no tag on CI-failed/timeout). Extends release state-file to v1.1 with `ciWatchResult` including `prNumber`+`headSha` for resume integrity. 27 proposals consolidated into 23 tasks.

## Architecture Assessment

- **Affected files:** `x-release/SKILL.md` (Phase CI-WATCH, args, state-file section, APPROVAL-GATE wiring); goldens across targets.
- **Phase insertion:** `OPEN-RELEASE-PR → (if --ci-watch) CI_WATCH_PENDING → x-pr-watch-ci → {exit 0/10 → CI_WATCH_COMPLETE → APPROVAL-GATE} | {exit 20/30 → RELEASE_ABORTED}`.
- **State-file schema v1.1:** `ciWatchResult{status, exitCode, releaseVersion, prNumber, headSha, checksSnapshot, copilotReview?, schemaVersion="1.1"}` + new phase values; legacy v1.0 files parse with `ciWatchResult=null`.
- **Flag precedence:** `--no-ci-watch` wins over `--ci-watch` (forward-compat with future RULE-045-01 v2 default-on flip).

## Test Strategy Summary

- **TPP ladder:** nil (flag absent → skipped, byte-identical legacy flow) → constant (happy path green) → scalar (abort on 20/30) → conditional (resume from CI_WATCH_COMPLETE) → iteration (flag combinations `--dry-run`/`--interactive`/`--no-ci-watch`).
- **Critical invariants:** APPROVAL-GATE 3-option menu preserved; state-file round-trip both v1.0 and v1.1; flag absence preserves exact legacy golden.
- **Acceptance tests:** happy path persists SUCCESS; CI_FAILED aborts no-tag; resume skips re-invocation on CI_WATCH_COMPLETE; flag combinations orthogonal.

## Security Assessment Summary

Release context = HIGH stakes (tag signs production). 4 security controls:
- **Resume integrity (TASK-014):** `ciWatchResult` binds `prNumber`+`headSha`; resume re-queries live PR; `STATE_FILE_PR_MISMATCH` abort on stale SHA. Prevents replay/forge of green CI-Watch across PRs or force-pushed commits.
- **Schema version strict (TASK-015):** schemaVersion bumped to "1.1"; unknown top-level fields → `STATE_FILE_SCHEMA_UNKNOWN` warn; no implicit upgrade on write.
- **Menu sanitization (TASK-016):** truncate ≤120 chars; strip backticks/newlines/markdown control; whitelist exit codes; reject unknown. Prevents prompt injection via malicious check-run titles.
- **Flag precedence tests (TASK-017):** locks opt-in contract; prevents regression to "default-on" drift before v2 decision is formal.

## Implementation Approach

Tech Lead enforces: `--no-ci-watch` precedence documented; state-file schemaVersion bump 1.0→1.1 with backward-compat reader; Rule 20 3-option invariant preserved in Phase 8; frontmatter contains both `Skill` and `AskUserQuestion`; deprecation warnings for `--ci-watch --skip-review` (semantic overlap) and `--continue-after-merge` without `ciWatchResult` (legacy resume); Rule 13 Pattern 1 audit (zero bare-slash); Conventional Commits `feat(task-0045-0005-NNN):`; `mvn process-resources` before golden regen.

PO refinements:
- Release abort on exit 20/30 is intentional (safety-first); FIX-PR remediation is post-abort manual — document rationale in §7
- Flag precedence matrix exhaustive (pairwise combos with all `x-release` flags)
- CLI `--help` output updated; `x-release --help | grep ci-watch` returns 0
- `x-release` schema-agnostic — v1 epic + `--ci-watch` works normally (Rule 19 compat); §3.5 + Gherkin added

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 23 |
| Architecture tasks | 5 |
| Test tasks | 8 |
| Security tasks | 4 |
| Quality gate tasks | 4 |
| Validation tasks | 2 |

## Consolidated Risk Matrix

| Risk | Source | Severity | Likelihood | Mitigation |
|------|--------|----------|------------|------------|
| Stale state-file → tag on non-verified commits (replay) | Security | CRITICAL | Low | TASK-014 prNumber+headSha binding + live PR re-query |
| Schema v1.1 deserialization regressions for legacy state | Security | High | Low | TASK-015 strict deserializer + TASK-013 round-trip test |
| Prompt injection via check-run titles in menu | Security | Medium | Low | TASK-016 sanitization (truncate + strip ctrl chars) |
| Flag drift to "default-on" before RULE-045-01 v2 decision | TechLead | High | Medium | TASK-017 precedence tests + TASK-018 precedence matrix |
| Tag created on CI-failed (release safety) | PO | CRITICAL | Very Low | TASK-009 hard abort on exit 20/30; no tag phase entered |
| Resume skips re-verification illegitimately | Security | High | Low | TASK-017(c) CI_WATCH_COMPLETE + status != SUCCESS → abort |
| APPROVAL-GATE menu shape drift | TechLead | High | Low | TASK-019 invariance IT (exactly 3 options) |

## DoR Status

See `dor-story-0045-0005.md`.
