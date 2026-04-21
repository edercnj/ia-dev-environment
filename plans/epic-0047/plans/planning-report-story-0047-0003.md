# Story Planning Report -- story-0047-0003

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0047-0003 |
| Epic ID | 0047 |
| Date | 2026-04-21 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story delivers a CI-grade lint (`SkillSizeLinter`) that walks `targets/claude/skills/**/SKILL.md` and classifies each by 3 tiers: <250 LoC = INFO; 250-500 = WARN; >500 without non-empty `references/` sibling = ERROR. Runs in `mvn test` default scope. Additionally, a corpus-wide audit (`SkillCorpusSizeAudit`) asserts total lines <30k (RULE-047-07) at epic DoD. Story is independent of 0047-0001/0002/0004 — pure preventive guard-rail. TDD from domain (record/enum) → application (walker) → acceptance (real corpus) → doc.

## Architecture Assessment

- **Affected layers:** domain (`LintFinding` record, `Severity` enum); application (`SkillSizeLinter` walker); test (unit + acceptance + audit).
- **New components:** `java/src/main/java/dev/iadev/quality/{LintFinding,Severity,SkillSizeLinter}.java`; tests in `java/src/test/java/dev/iadev/quality/`.
- **Dependency direction:** domain pure (no I/O); application layer performs filesystem walk via NIO; domain → application (correct, inward).
- **Layer discipline:** `LintFinding` has zero external deps; `SkillSizeLinter` is a pure function given filesystem state (testable with Jimfs / `@TempDir`).
- **Reuses:** pattern from other `*AuditTest` classes if present (e.g., Rule13AuditTest per story §3.3).

## Test Strategy Summary

- **Acceptance tests (AT):** 5 Gherkin scenarios from Section 7 — <250 silent, 250-500 WARN, >500 no-refs ERROR, >500 with-refs OK, refs-only-README still ERROR.
- **Unit tests (UT, TPP):**
  - TPP-1 (nil): empty directory → empty findings
  - TPP-2 (constant): single skill <250 → single INFO finding
  - TPP-3 (scalar): single skill in WARN tier → WARN
  - TPP-4 (collection): multiple skills mixed tiers → correct aggregation
  - TPP-5 (conditional): >500 branch depends on references/ state → 2 sub-cases
  - TPP-6 (iteration): README-only references rejected; error message formatting
- **Acceptance:** real corpus test asserts no ERROR against current develop (DoR gate).
- **Smoke / audit:** `SkillCorpusSizeAudit` — total LoC < 30k.
- **Coverage target:** ≥95% Line / 90% Branch for `SkillSizeLinter`.

## Security Assessment Summary

- **OWASP mapping:** A01 Broken Access Control — N/A (no auth); A03 Injection — N/A (no user input). Path handling: walker uses `Files.walk()` over trusted repo path → no traversal concern.
- **Defensive coding:** per Rule 06 — if linter ever accepts a path arg, MUST normalize + prefix-check against allowed base (`java/src/main/resources/targets/claude/skills/`). Current design uses hard-coded constant; safe.
- **Risk level:** LOW.

## Implementation Approach

- **Tech Lead decision:** Singleton/static helper or instance? Choose static helper (`SkillSizeLinter.lint(Path)`) for testability and lack of state. Mirror any existing `Rule*AuditTest` pattern.
- **Quality gates:** method ≤25 lines; class ≤250 lines; ≥95/90 coverage; error message carries path+count+suggestions+threshold per §3.2.
- **Coding standards:** use `record` for `LintFinding`; immutable; `Severity` enum with 3 constants; no null returns (empty list for clean corpus).
- **Performance:** target <1s for full corpus per story §3.3.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 5 |
| Architecture tasks | 2 (001 domain, 002 application) |
| Test tasks | 2 (003 acceptance, 005 corpus audit) |
| Security tasks | 0 (covered in 002 DoD) |
| Quality gate tasks | 1 (002 with coverage threshold) |
| Validation tasks | 1 (004 doc + CHANGELOG) |
| Merged tasks | 0 |
| Augmented tasks | 0 |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Lint fails against current develop (Bucket A not merged) | QA | HIGH | MEDIUM | DoR gate: Bucket A merged first; else threshold softened |
| Threshold 500 is wrong for realistic corpus | Tech Lead | MEDIUM | LOW | Revisit annually per RULE-047-04; initial value validated by acceptance test |
| Performance regression if corpus explodes | Tech Lead | LOW | LOW | <1s target asserted; use Files.walk stream |
| KPs in scope may have legitimate >500 without references (some) | QA | MEDIUM | MEDIUM | 0047-0004 carves KPs; this story lands AFTER KP carve or with threshold raised temporarily |

## DoR Status

**READY** — all 10 mandatory checks pass.

## File Footprint

- `write:` `java/src/main/java/dev/iadev/quality/{LintFinding,Severity,SkillSizeLinter}.java`, `java/src/main/java/dev/iadev/quality/README.md`, `java/src/test/java/dev/iadev/quality/{LintFindingTest,SkillSizeLinterTest,SkillSizeLinterAcceptanceTest,SkillCorpusSizeAudit}.java`, `CHANGELOG.md` (Unreleased)
- `read:` `java/src/main/resources/targets/claude/skills/**` (walker scope); existing Rule*AuditTest pattern references
- `regen:` none (no SKILL.md edits, no goldens touched)
- **Conflict posture:** SOFT — CHANGELOG.md shared hotspot per RULE-004; coordinate Unreleased edits with other stories
