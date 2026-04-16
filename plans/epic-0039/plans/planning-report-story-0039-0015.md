# Story Planning Report -- story-0039-0015

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0015 |
| Epic ID | 0039 |
| Date | 2026-04-15 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |
| Schema Version | v1 (legacy) |

## Planning Summary

Final consolidation story for EPIC-0039. Orchestrates the post-merge finalization: rewriting `x-release/SKILL.md` as the single source of truth for all 14 technical stories, adding 3 new plan templates to `PlanTemplateDefinitions` (bumping `TEMPLATE_COUNT` from 15 to 18), regenerating all 17 profile goldens in a single consolidated operation (RULE-008), producing an end-to-end walkthrough reference, and populating `CHANGELOG.md [Unreleased]`. Story is doc + test heavy with no new runtime surface.

## Architecture Assessment

- **Affected layers:** documentation (SKILL.md + references), application (`PlanTemplateDefinitions`), test (golden profiles + smoke test). No domain or adapter changes.
- **New components:**
  - `references/interactive-flow-walkthrough.md` (new doc)
  - 3 plan template entries in `PlanTemplateDefinitions`
  - `Epic0039FinalSmokeTest.java` (new test)
- **Modified components:**
  - `x-release/SKILL.md` (full rewrite)
  - `PlanTemplateDefinitions.java` (count + 3 map entries)
  - `CHANGELOG.md`
  - 17 golden profiles (regenerated)
- **Dependency direction:** adapter-free; change stays in documentation, application assembler, and test resources. Domain purity preserved.
- **Integration points:** `GoldenFileRegenerator` consumes source-of-truth templates under `java/src/main/resources/targets/claude/`; generated output lands in `.claude/templates/` and `src/test/resources/golden/**`.
- **Implementation order:** SKILL.md rewrite (TASK-001) + PlanTemplateDefinitions (TASK-003/004) + CHANGELOG (TASK-006) are independent and parallel; walkthrough (TASK-002) depends on TASK-001; golden regen (TASK-005) fans in TASK-001 and TASK-004; smoke (TASK-007/008) and quality gate (TASK-009) follow; PO validation (TASK-010) closes.

## Test Strategy Summary

- **Outer loop (acceptance):** 5 Gherkin scenarios in Section 7 of the story cover SKILL feature grep (degenerate), 17-golden regen (happy), `mvn verify` (acceptance), CHANGELOG coverage (boundary), walkthrough coverage (boundary). TPP order explicitly declared in §7.1.
- **Inner loop (unit):** UT-constant for `PlanTemplateDefinitions` (TASK-003 RED, TASK-004 GREEN) asserting `TEMPLATE_COUNT == 18` and 3 new entries with correct mandatory sections.
- **Smoke:** `Epic0039FinalSmokeTest` (TASK-007 RED, TASK-008 GREEN) asserts dummy profile generation matches expected byte-for-byte.
- **Coverage target:** no code-coverage delta expected on runtime modules; `PlanTemplateDefinitions` test coverage must remain >=95% line / >=90% branch.
- **Golden verification:** `GoldenFileCoverageTest` + `PipelineSmokeTest` re-run as part of `mvn verify` across 17 profiles.

## Security Assessment Summary

- **OWASP categories applicable:** none. Story touches documentation, static map entries (application assembler), golden test resources, and CHANGELOG. No user input, auth, crypto, persistence, or network surface introduced.
- **Controls needed:** none new. Existing file-path handling in `GoldenFileRegenerator` is pre-existing and unchanged.
- **Risk level:** low. Primary operational risk is an unreviewed golden diff masking an unintended change; mitigated by TASK-005 DoD requiring diff review.
- **Retained SEC verification tasks:** none (no security-sensitive components touched).

## Implementation Approach

- **Chosen approach:** Follow the Section 8 task sequence authored in the story. Apply deterministic consolidation rules to add one QA RED/GREEN pair for `PlanTemplateDefinitions` (TDD honesty), one QA RED/GREEN pair for the final smoke test, and two finalization tasks (TL quality gate + PO validation).
- **Quality gates:**
  - Method length <=25 lines in the single Java file touched (`PlanTemplateDefinitions`).
  - Keep a Changelog format preserved in `CHANGELOG.md`.
  - Golden byte-for-byte equality across 17 profiles.
  - `mvn verify` green across all 17 profiles (TASK-009).
  - Test-first in git history: TASK-003 before TASK-004; TASK-007 before TASK-008.
- **Refactoring:** none expected — the story is additive (doc + static map + goldens).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 10 |
| Architecture tasks | 4 (ARCH source) |
| Test tasks | 4 (QA RED/GREEN pairs for unit + smoke) |
| Security tasks | 0 (no security surface) |
| Quality gate tasks | 1 (TL) |
| Validation tasks | 1 (PO) |
| Merged tasks | 1 (TASK-004 merges ARCH+SEC+TL onto `PlanTemplateDefinitions`) |
| Augmented tasks | 0 (no security-sensitive components) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Unintended golden diff in a profile | ARCH | HIGH | MEDIUM | TASK-005 DoD mandates manual diff review before commit |
| `mvn verify` flake across 17 profiles | TL | MEDIUM | LOW | Run serially; investigate any failure to rule out genuine regression vs environmental |
| SKILL.md rewrite misses a feature from S01..S14 | ARCH | MEDIUM | MEDIUM | TASK-010 PO Gherkin #1 grep-asserts feature keywords; SKILL.md reviewed against epic scope |
| `TEMPLATE_COUNT` bump forgotten | QA | LOW | LOW | TASK-003 RED test explicitly asserts count == 18 |
| CHANGELOG drifts from actual story content | PO | LOW | LOW | TASK-010 PO Gherkin #4 validates Added/Changed/Removed entries against 15 stories |

## DoR Status

READY — see `dor-story-0039-0015.md`.
