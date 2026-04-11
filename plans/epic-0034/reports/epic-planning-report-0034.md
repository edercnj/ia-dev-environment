# Epic Planning Report -- EPIC-0034

> **Epic ID:** EPIC-0034
> **Title:** Remoção de Targets Não-Claude do Gerador
> **Date:** 2026-04-10
> **Total Stories:** 5
> **Stories Planned:** 5
> **Overall Status:** READY

## Readiness Summary

| Metric | Count |
|--------|-------|
| Stories Total | 5 |
| Stories Planned | 5 |
| Stories Ready (DoR READY) | 5 |
| Stories Not Ready (DoR NOT_READY) | 0 |
| Stories Pending | 0 |

## Per-Story Results

| # | Story ID | Phase | Planning Status | DoR Verdict | Tasks | Duration |
|---|----------|-------|-----------------|-------------|-------|----------|
| 1 | story-0034-0001 | 0 | READY | READY | 6 | ~44 min |
| 2 | story-0034-0002 | 1 | READY | READY | 6 | ~12 min |
| 3 | story-0034-0003 | 2 | READY | READY | 4 | ~11 min |
| 4 | story-0034-0004 | 3 | READY | READY | 7 | ~14 min |
| 5 | story-0034-0005 | 4 | READY | READY | 5 | ~12 min |

> **Total tasks:** 28 (6+6+4+7+5). Single extra task added: TASK-002b in story-0034-0004 for PlatformContextBuilder + PlatformFilter simplification.

## Blockers

Nenhum bloqueador. Todas as 5 stories receberam verdict READY dos 10/10 checks obrigatórios de DoR. Os 2 checks condicionais (compliance e contract tests) são N/A para este projeto (`compliance: none`, `contract_tests: false` per rule 01).

## Findings per Story

### story-0034-0001 (Remove GitHub Copilot)

- **Scope reconciliation:** 15 test classes (not 14 as in story §3.2), 18 YAMLs (not 17 as in story §3.7). Baseline is authoritative.
- **RULE-003 mitigation in TASK-005:** explicit `-not -name 'workflows'` exclusion plus pre/post count check (expected: 95 `.github/workflows/` files unchanged across all 17 profiles).
- **AssemblerFactory atomicity:** edit MUST be in TASK-001 with Github class deletion because it references `Platform.COPILOT` which cannot be removed until TASK-003.
- **FileCategorizer workflows fall-through:** no explicit `.github/workflows/` branch; removing 6 `.github/*` branches will cause workflows files to be categorized as "Other" (acceptable — documented).
- **PlatformConverter dynamic:** `ACCEPTED_VALUES` is computed from `Platform.allUserSelectable()` — no literal edit needed.
- **BREAKING CHANGE commit in TASK-003.** CHANGELOG update deferred to story-0034-0005 per epic plan.
- **Coverage risk acknowledged:** baseline 95.69% line / 90.69% branch; RULE-002 gate at TASK-006 requires ≤2pp degradation.

### story-0034-0002 (Remove Codex)

- **CRITICAL — DocsAdrAssembler relocation:** `AssemblerFactory.buildCodexAssemblers()` registers `DocsAdrAssembler` (a SHARED descriptor, not Codex) as an exception. TASK-001 must relocate it to a surviving builder before deleting the method or `DocsAdrAssembler` becomes orphaned.
- **CODEX_AGENTS preservation trap:** `AssemblerTarget.CODEX_AGENTS(".agents")` sits adjacent to `CODEX(".codex")` in the enum. MUST NOT be deleted in this story (owned by story-0034-0003). Explicit DoD sanity grep in TASK-003.
- **17 dependent test files** (beyond the 6 Codex-specific tests) reference `Platform.CODEX` / `AssemblerTarget.CODEX` / `hasCodex` and require atomic updates in TASK-003. TASK-002 and TASK-003 may need coalescence if test-compile fails between them.
- **Same YAML count delta as story 0001:** 18 actual vs. 17 in story text.
- **Largest single-story golden deletion:** 2944 files (17 profiles × ~173 files/profile).
- **Manifest regeneration:** TASK-006 is the designated point for regeneration or deferral to story-0034-0005.

### story-0034-0003 (Remove Agents)

- **Classification resolved baseline ambiguity:** baseline reported 4 main + 12 test matching `*Agents*`. Filesystem audit confirmed 2 of the 4 main classes (`GithubAgentsAssembler`, `CodexAgentsMdAssembler`) and 5 of the 12 tests (`GithubAgents*`, `CodexAgentsMdAssemblerTest`) belong to stories 0001/0002. Story 0003 actual scope: 2 main + 6 tests + 1 fixture.
- **No `targets/agents/` source directory:** filesystem verified. Story §3.3 is correct. Defensive `test -d && rm -rf` added to TASK-003 as safety net.
- **17 `.agents/` golden subdirectories confirmed** across all 17 profiles — matching baseline's 2910 file count.
- **Scope boundary (TL-007):** `PlatformFilter.java` explicitly owned by story-0034-0004. TASK-002 and TASK-004 enforce this via `git diff --name-only` verification.
- **Cosmetic arithmetic issue:** story §3.5 projects ~8273 cumulative files; actual 2324+2944+2910 = 8178 (95-file delta = `.github/workflows/` protected count). TASK-004 DoD requires documenting correct arithmetic in PR body.
- **Strictly linear task DAG:** 001→002→003→004; no parallelism.

### story-0034-0004 (Hygienize Shared Code)

- **Task count = 7 (not 6):** story §8 inserts TASK-002b (`Simplify PlatformContextBuilder + PlatformFilter`) between 002 and 003. Strict linear: 001→002→002b→003→004→005→006.
- **RULE-004 enforcement doubled:** tasks 003 and 006 each run `git diff -- java/src/main/resources/shared/templates/` + `find | wc -l == 57` pre/post gates.
- **`PlanTemplatesAssembler.java` already over Rule-03 limit (354 lines).** Task 003 has documented fallback: extract `TEMPLATE_SECTIONS` map and `buildTemplateSections()` into a new `PlanTemplateDefinitions` helper class.
- **`EpicReportAssembler` carries same `GITHUB_OUTPUT_SUBDIR` constant independently.** Story §3.2 only mentions `PlanTemplatesAssembler`. Task 003 explicitly covers both to avoid dangling dual-target write.
- **Story text package-path drift:** `FileTreeWalker` is under `dev/iadev/smoke/`, `PlatformParser` under `dev/iadev/domain/model/`, `PlatformPrecedenceResolver` under `dev/iadev/cli/`. TASK-004 uses verified paths.
- **`ReadmeAssembler` has NO hasCopilot/hasCodex branches currently** — residual logic lives in dependencies (`MappingTableBuilder`, `SummaryTableBuilder`, `SummaryRowFilter`) edited in TASK-002.
- **Task 006 requires LOC baseline file** from story-0034-0003 at `plans/epic-0034/reports/story-0003-loc-baseline.txt`. Fallback via `git show` on merge commit if file missing.
- **Tech Lead override:** TASK-002b keeps `PlatformFilter` as a class (Architect proposed inlining) — public API stable, class serves as extension point. Recorded in commit body.
- **12 CWE-tagged DoD items** across tasks 003/004/006 (CWE-22 path traversal, CWE-209 error leakage, CWE-710 dead code).

### story-0034-0005 (Documentation + Final Verification)

- **CRITICAL generated-output risk:** `CLAUDE.md` raiz and `.claude/rules/*.md` are likely generated outputs per project structure. Source of truth under `java/src/main/resources/targets/claude/` must be edited first — direct hand-edits to generated files will be lost on next regeneration.
- **MEMORY.md prerequisites propagated:** `mvn process-resources` before any regenerator (stale output risk) and canonical regen command location (README.md ~L820) explicitly captured in TASK-003 Step 1 and Step 3.
- **Security augmentation:** 4 of 5 tasks received CWE-specific DoD criteria (CWE-798 for docs, CWE-209 for CLI error messages, CWE-22 for regenerator output path).
- **Rule 08 BREAKING CHANGE enforcement:** TASK-002 commit template includes `BREAKING CHANGE:` footer; TASK-005 Step 1 greps for non-conforming commits before PR creation.
- **Rule 09 branching enforcement:** TASK-005 explicitly targets `develop`, not `main`; captured as TL-005 quality gate.
- **Verification evidence matrix:** TASK-004 produces 9-scenario evidence matrix (PO-002) archived under `plans/epic-0034/reports/task-005-004/` including JaCoCo report, CLI smoke logs, and build output.

## Cross-Story Observations

### Baseline Reconciliations Discovered

| Item | Story Text | Actual (Baseline) | Resolution |
|------|-----------|-------------------|------------|
| Github test classes | 14 + fixture | 15 + fixture | Story 0001 TASK list updated |
| YAMLs to edit | 17 | 18 | Stories 0001 + 0002 updated |
| Cumulative golden deletion | ~8273 | 8178 (delta = 95 workflows) | Story 0003 TASK-004 PR body documents correct number |
| Agents main classes | 2 | 4 (2 belong to 0001/0002) | Story 0003 scope confirmed as 2 |
| Agents test classes | 6 | 12 (5 belong to 0001/0002) | Story 0003 scope confirmed as 6 + 1 fixture |
| Shared class line limits | <= 250 | PlanTemplatesAssembler = 354 | Story 0004 has extraction fallback |

### Critical Architectural Traps Surfaced

1. **DocsAdrAssembler relocation (story 0002)** — shared class registered in buildCodexAssemblers, cannot be orphaned.
2. **CODEX_AGENTS scope boundary (story 0002)** — enum entry adjacent to CODEX must NOT be deleted until story 0003.
3. **CLAUDE.md generated-output awareness (story 0005)** — hand-edits would be lost; must edit source first.
4. **FileCategorizer workflows fall-through (story 0001)** — acceptable but documented.
5. **`.github/workflows/` RULE-003 protection (story 0001)** — explicit exclusion pattern + count validation.
6. **`resources/shared/templates/` RULE-004 protection (story 0004)** — dual gate in tasks 003 and 006.

### Dependency Chain Integrity

All 5 stories are strictly sequential:
```
story-0001 → story-0002 → story-0003 → story-0004 → story-0005
  Phase 0     Phase 1      Phase 2      Phase 3       Phase 4
```

Planning ran in parallel (stories 0002-0005 dispatched concurrently after 0001 completed) because PLANNING artifacts are file-based and do not require execution dependency satisfaction. EXECUTION must still respect the phase order.

## Generated Artifacts

### Per-Story Breakdown (43 total)

**DoR Checklists (5):**
- `plans/epic-0034/plans/dor-story-0034-0001.md`
- `plans/epic-0034/plans/dor-story-0034-0002.md`
- `plans/epic-0034/plans/dor-story-0034-0003.md`
- `plans/epic-0034/plans/dor-story-0034-0004.md`
- `plans/epic-0034/plans/dor-story-0034-0005.md`

**Task Breakdowns (5):**
- `plans/epic-0034/plans/tasks-story-0034-0001.md` (6 tasks)
- `plans/epic-0034/plans/tasks-story-0034-0002.md` (6 tasks)
- `plans/epic-0034/plans/tasks-story-0034-0003.md` (4 tasks)
- `plans/epic-0034/plans/tasks-story-0034-0004.md` (7 tasks)
- `plans/epic-0034/plans/tasks-story-0034-0005.md` (5 tasks)

**Individual Task Plans (28):**
- story-0034-0001: TASK-001 through TASK-006 (6)
- story-0034-0002: TASK-001 through TASK-006 (6)
- story-0034-0003: TASK-001 through TASK-004 (4)
- story-0034-0004: TASK-001, TASK-002, TASK-002b, TASK-003 through TASK-006 (7)
- story-0034-0005: TASK-001 through TASK-005 (5)

**Planning Reports (5):**
- `plans/epic-0034/plans/planning-report-story-0034-0001.md`
- `plans/epic-0034/plans/planning-report-story-0034-0002.md`
- `plans/epic-0034/plans/planning-report-story-0034-0003.md`
- `plans/epic-0034/plans/planning-report-story-0034-0004.md`
- `plans/epic-0034/plans/planning-report-story-0034-0005.md`

### Auxiliary Artifacts

- `plans/epic-0034/baseline-pre-epic.md` — pre-execution snapshot
- `plans/epic-0034/execution-state.json` — checkpoint (v2.0, shared with x-dev-epic-implement)
- `plans/epic-0034/reports/epic-execution-plan-0034.md` — dry-run execution plan (from earlier session)
- `plans/epic-0034/reports/epic-planning-report-0034.md` — this report

### Story File Updates (5)

Each story file received a Section 8.1 append with the consolidated task summary from multi-agent planning:
- `plans/epic-0034/story-0034-0001.md`
- `plans/epic-0034/story-0034-0002.md`
- `plans/epic-0034/story-0034-0003.md`
- `plans/epic-0034/story-0034-0004.md`
- `plans/epic-0034/story-0034-0005.md`

## Next Steps

1. **Review global DoR from `epic-0034.md` §3** — 8 items to verify (some already satisfied via baseline capture and PR #266 merge).
2. **Promote `epic-0034.md` status** from `Em Refinamento` to `Pronto` once global DoR is satisfied.
3. **Commit all planning artifacts** to `feature/epic-0034-remove-non-claude-targets` and optionally open PR for pre-execution review.
4. **Execute `/x-dev-epic-implement epic-0034`** for actual execution (this skill's output is INPUT to that skill).

## Overall Verdict

**READY.** All 5 stories are READY. Zero blockers. Minor story-text reconciliations surfaced and documented during planning — these will be corrected during execution as part of the regular task flow (not blocking).
