# Story Planning Report -- story-0045-0006

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0045-0006 |
| Epic ID | 0045 |
| Date | 2026-04-20 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story 0045-0006 closes EPIC-0045 with: (a) `Epic0045SmokeTest` end-to-end integration against a real ephemeral PR on the project repo, (b) comprehensive golden regeneration across all 5 upstream retrofits, (c) Rule 20 audit smoke across full repo, (d) 6 CHANGELOG entries, (e) epic status `Concluído`, (f) CLAUDE.md In-progress → Concluded migration. 28 proposals consolidated into 23 tasks.

## Architecture Assessment

- **Affected files:** `java/src/test/java/dev/iadev/smoke/Epic0045SmokeTest.java` (NEW), `SmokePrFixture.java` (NEW helper), `CHANGELOG.md` (6 entries), `plans/epic-0045/epic-0045.md` (status flip), `CLAUDE.md` (block migration), `src/test/resources/golden/**` (full regen).
- **Smoke lifecycle:** JUnit 5 single-class with `@Tag("e2e")` + `@EnabledIfEnvironmentVariable(SMOKE_E2E=true)`; `@BeforeAll` creates draft PR via gh CLI; `@AfterAll` tears down with idempotent cleanup registry.
- **Convergence:** Story converges from 5 upstream stories (0001/0002/0003/0004/0005) — wave-2 terminal story per implementation map §5.
- **Epic close checklist:** 7 gates mapping to TASK-014/015/016/020/021/023 artifacts.

## Test Strategy Summary

- **TPP ladder (4 smoke scenarios):** nil (nonexistent PR → exit 70) → constant (happy path → exit 0) → scalar (CI failed → exit 20) → conditional (Copilot timeout → exit 10). Fast-fail design: TPP-1 needs no PR creation, validates CI env + binary before consuming API quota.
- **Golden audit (TASK-008):** `GoldenFileRegenerator` dry-run over 6 artifact categories; zero drift.
- **Rule 20 smoke (TASK-009):** `scripts/audit-rule-20.sh` exit 0 on full repo; guards against silent regression.
- **Resource management:** `SmokePrFixture` helper encapsulates gh CLI with idempotent cleanup; unique branch naming avoids parallel-test collision.

## Security Assessment Summary

Smoke is the highest-risk surface (real remote mutations, real GitHub token). 4 controls:
- **PR lifecycle cleanup registry (TASK-010):** Static registry ensures @AfterAll cleans resources even on @BeforeAll mid-failure. Post-cleanup assertion verifies `gh pr list` + `git ls-remote` empty.
- **e2e tag + env gate (TASK-011):** Smoke must NOT run in default `mvn test`. Surefire excludes `@Tag("e2e")`; `SMOKE_E2E=true` env opt-in. Fail-fast if `GITHUB_TOKEN` absent. Documented `gh` scopes: `repo:public_repo` + `pull_request:write` only (no admin).
- **State-file cleanup + path guard (TASK-012):** `.claude/state/pr-watch-<N>.json` deleted in @AfterEach; path traversal guard on delete (`startsWith(baseStateDir)`).
- **Secret-leak audit (TASK-013):** Smoke stdout/stderr routed through `TelemetryScrubber`; `PiiAudit` CLI gates CHANGELOG + CLAUDE.md diff on closing commit; assert NONE of `{ghp_, ghs_, Bearer , AKIA, eyJ}` appear.

## Implementation Approach

Tech Lead enforces: e2e smoke profile isolation (`mvn test` wall-time unchanged; `mvn verify -P e2e` activates); comprehensive golden drift check (`git diff --exit-code` post-regen); Rule 20 dual audit (interactive-gates + telemetry PiiAudit both exit 0); CHANGELOG 6-entry structural check; SemVer MINOR bump (no public API removal); regression baseline (HEAD pass count ≥ baseline; coverage ≥ 95%/90%); epic close (CLAUDE.md In-progress block removed + Concluded added).

PO refinements:
- Extended Gherkin §7 with CI-failed + Copilot-absent scenarios (TASK-022)
- Closure checklist artifact produced in `reports/epic-0045-closure-checklist.md` (TASK-023)
- CHANGELOG Unreleased → version migration is release engineer's scope at next `x-release` (NOT this story)
- Integrated-flow smoke (PO-003) deferred as optional pre-release gate; @Disabled by default, env opt-in

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 23 |
| Architecture tasks | 5 (smoke skeleton + fixture delegated to QA REFACTOR; goldens, CHANGELOG, epic status, CLAUDE.md) |
| Test tasks | 8 (4 TPP smoke + golden audit + Rule 20 smoke + fixture refactor) |
| Security tasks | 4 (cleanup registry, e2e gate, state-file, secret audit) |
| Quality gate tasks | 5 (profile isolation, golden drift, Rule 20 dual audit, CHANGELOG+SemVer, regression baseline) |
| Validation tasks | 2 (extend Gherkin, closure checklist) |

## Consolidated Risk Matrix

| Risk | Source | Severity | Likelihood | Mitigation |
|------|--------|----------|------------|------------|
| Orphaned smoke branch/PR on remote (cleanup failure) | Security | High | Medium | TASK-010 cleanup registry with per-resource try/catch + @BeforeAll inner fault handling |
| Smoke runs in default `mvn test` leaking personal gh token | Security | Critical | Low | TASK-011 Surefire excludedGroups + SMOKE_E2E env gate + @EnabledIfEnvironmentVariable |
| Token leak in smoke stdout/stderr via `gh api` output | Security | High | Low | TASK-013 TelemetryScrubber on smoke output |
| Secret regex false negative in CHANGELOG/CLAUDE.md | Security | Medium | Low | TASK-013 PiiAudit CLI gate + defense-in-depth token search in smoke |
| Full golden regen misses a target → partial drift | QA | Medium | Medium | TASK-008 dry-run diff audit + TASK-018 git diff --exit-code |
| Smoke flakiness from gh rate-limit / Copilot disabled | PO | Low | Medium | TASK-022 accepts exit 10 as valid; retry policy delegated to implementation (PO-002 deferred) |
| SemVer bump miscalculation (MAJOR vs MINOR) | TechLead | High | Low | TASK-020 grep-diff vs last tag audits public API removal |
| Epic close without all 7 gates verified | PO | High | Low | TASK-023 closure checklist artifact with SHA + CI-run URL per gate |
| CHANGELOG entries cite wrong story IDs | TechLead | Medium | Low | TASK-020 `grep -c "story-0045-000[1-6]"` == 6 check |
| Integrated-flow (x-story-implement → menu → x-pr-fix) not smoke-covered | PO | Low | — | PO-003 integrated-flow smoke deferred as optional; @Disabled follow-up |

## DoR Status

See `dor-story-0045-0006.md`.
