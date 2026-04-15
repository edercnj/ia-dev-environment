# Story Planning Report -- story-0039-0006

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0006 |
| Epic ID | 0039 |
| Date | 2026-04-15 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |
| Schema | v1 |

## Planning Summary

Automates optional GitHub Release creation after tag push in the `x-release` skill Phase 11 PUBLISH. Introduces one pure domain class (`ChangelogBodyExtractor`), new CLI flag (`--no-github-release`), state field (`githubReleaseUrl`), and two error codes. Honours RULE-007 (confirmation mandatory when enabled). Warn-only on `gh` CLI failure so a successful tag push is never undone by a release-notes hiccup.

## Architecture Assessment

- **Layers affected:** domain (new `ChangelogBodyExtractor`), adapter.outbound (gh CLI invocation via existing ProcessRunner port), config (SKILL.md + error catalog), cross-cutting (state file).
- **New classes:** `dev.iadev.release.changelog.ChangelogBodyExtractor` — pure, zero framework deps, fits domain purity rule (04-architecture-summary).
- **Existing modifications:** `x-release` SKILL.md Phase 11 block; error catalog (adapter.outbound); state-file schema (+ one nullable field).
- **Dependency direction:** compliant — domain has zero outward deps; gh invocation lives in adapter.outbound behind ProcessRunner port.
- **No new ADR required** — follows existing `AskUserQuestion` + ProcessRunner patterns.

## Test Strategy Summary

- **Acceptance tests (AT):** 5 Gherkin scenarios map 1:1 to executable tests (smoke + unit).
- **Unit tests (UT) in TPP order:**
  - Level 1 nil: empty CHANGELOG, null version arg.
  - Level 2 constant: single-version CHANGELOG, single-line body.
  - Level 3 scalar: version-at-EOF, version-not-present (`Optional.empty`).
  - Level 4 collection: multiline body with bullet lists preserved verbatim.
- **Smoke test:** mocked `gh release create` asserts argv + state update.
- **Coverage target:** line >=95%, branch >=90% on `ChangelogBodyExtractor`.

## Security Assessment Summary

- **OWASP mapping:** A03 (Injection) — CHANGELOG content passed to `gh --notes`; must use argv (not shell); A08 (Software/Data Integrity) — tag already pushed, release-notes is best-effort.
- **Controls:**
  1. Version arg validated against SemVer pattern before regex (prevents ReDoS).
  2. `--notes` body passed via argv, never shell-interpolated.
  3. `gh` stderr filtered for token echoes before logging.
- **Data protection:** no PII in CHANGELOG; release URL is public by construction.
- **Secrets:** none added — relies on ambient `gh auth status`.

## Implementation Approach

- **Tech Lead decision:** keep `ChangelogBodyExtractor` as a pure domain class (single-method `Optional<String> extractBody(String content, String version)`). Rejected alternative: embedding regex inline in the orchestrator (violates SRP + coverage goals).
- **Pre-commit chain:** format -> lint -> compile (standard Rule 07).
- **Worktree:** not required for this story; runs in standard working tree.
- **Source-of-truth (RULE-001):** SKILL.md edits go to `java/src/main/resources/targets/claude/skills/core/x-release/SKILL.md`; `.claude/` regenerated, not hand-edited.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 12 |
| Architecture tasks | 3 |
| Test tasks (RED/GREEN) | 4 (2 RED + 2 GREEN) |
| Security tasks | 2 |
| Quality gate tasks | 2 |
| Validation tasks | 1 |
| Merged tasks | 2 (ARCH+QA on extractor impl and on state field) |
| Augmented tasks | 1 (TASK-005 augmented with SEC injection safety criterion) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| ReDoS via crafted CHANGELOG | SEC | MEDIUM | LOW | Validate version arg; bounded regex |
| gh CLI failure aborts release | ARCH | HIGH | MEDIUM | Warn-only policy (RULE-007 edge); tag already pushed |
| SKILL.md edit not regenerated | TL | MEDIUM | MEDIUM | CI golden-file check + memory note about `mvn process-resources` |
| Shell injection via --notes body | SEC | HIGH | LOW | argv invocation (no shell); ProcessRunner port |
| State file schema drift | ARCH | LOW | LOW | Field is nullable; backward compatible |

## DoR Status

READY -- see `dor-story-0039-0006.md`.
