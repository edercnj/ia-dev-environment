# Story Planning Report -- story-0034-0001

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0034-0001 |
| Epic ID | 0034 |
| Date | 2026-04-10 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story 0034-0001 is the atomic removal of GitHub Copilot support from the `ia-dev-environment` generator. It is the largest and first of three target-removal stories in epic-0034 (~2,324 golden files, 131 resources, 8 Java main classes, 15 test classes + 1 fixture, 18 YAML cleanups, 7 shared class edits). Multi-agent planning surfaced two reconciliations against the story text: (1) actual test file count is 15 + 1 fixture (not 14 + 1 as stated), and (2) actual YAML count is 18 (not 17). Agent consensus aligns with the story's existing §8 task decomposition of 6 atomic tasks; no restructuring needed. The task DoD was augmented with security/quality gates from the Security Engineer and Tech Lead agents. The existing §8 task ordering is build-green-preserving and was validated against the compilation dependency graph.

## Architecture Assessment

Affected layers: `domain.model` (Platform enum removes CLAUDE_CODE peer), `application.assembler` (AssemblerTarget enum, AssemblerFactory factory, 8 assembler classes, PlatformContextBuilder), `cli` (PlatformConverter, GenerateCommand, FileCategorizer), `util` (OverwriteDetector). Dependency direction respected: domain changes only flow outward after the outer adapters stop referencing the removed constants. Implementation order validated: delete Github production classes FIRST (task 001) to remove inbound references to Platform.COPILOT; delete test classes SECOND (task 002); edit enums and shared hygiene classes THIRD (task 003) once no code still imports the removed constants; delete resources FOURTH (task 004); delete golden files FIFTH (task 005, parallelizable with task 004); final gate and YAML cleanup SIXTH (task 006). Post-edit `AssemblerFactory.buildAllAssemblers()` returns 26 assemblers instead of 34. No architectural layering violations. RULE-003 (`.github/workflows/` protected) enforced at the golden-deletion task (005) with explicit `find -not -name 'workflows'` exclusion. Class diagram: `Platform{CLAUDE_CODE,COPILOT,CODEX,SHARED}` → `Platform{CLAUDE_CODE,CODEX,SHARED}` and `AssemblerTarget{ROOT,CLAUDE,GITHUB,CODEX,CODEX_AGENTS}` → `AssemblerTarget{ROOT,CLAUDE,CODEX,CODEX_AGENTS}`. All 7 shared class line counts remain under Clean Code's 250-line limit post-edit.

## Test Strategy Summary

This is a deletion story, so the TDD inner loop is inverted from the traditional RED→GREEN→REFACTOR flow. Instead, the QA strategy is anchored on: (1) RULE-006 pre-delete green confirmation (baseline from `plans/epic-0034/baseline-pre-epic.md` reports 837 tests passing, 0 failures, 95.69% line / 90.69% branch coverage); (2) compile-verified GREEN cycles after each task commit; (3) existing smoke tests `CliModesSmokeTest`, `PlatformDirectorySmokeTest`, `AssemblerRegressionSmokeTest` acting as regression detectors. Six acceptance tests are mapped to the six Gherkin scenarios in story §7, covering TPP levels nil→constant→scalar→conditional→boundary. **AT-1** (build verde, scalar) validated by `mvn clean verify`. **AT-2** (CLI rejects `--platform copilot`, conditional) validated by CLI smoke. **AT-3** (CLI works for `claude-code`, constant) validated by CLI smoke. **AT-4** (`.github/workflows/` preserved, boundary invariant) validated by `find ... workflows -type f | wc -l` before/after comparison (expected: 95 files, unchanged). **AT-5** (grep sanity, scalar) validated in Phase 6.D of task 006. **AT-6** (CLI default, nil/degenerate) validated by CLI smoke without `--platform` flag. No new unit tests are written — the 15 existing Github tests are DELETED atomically with their corresponding production code (RULE-006 atomicity). Projected coverage post-deletion: >= 95% line / >= 90% branch (within 2pp of baseline).

## Security Assessment Summary

Low intrinsic security risk. OWASP Top 10 mapping: **A06 (Vulnerable/Outdated Components) — PRIMARY:** deletion reduces attack surface by eliminating 131 unmaintained resource files and 8 code paths. **A03 (Injection) — LOW:** `PlatformConverter` reflects user input in error messages via `String.formatted("Invalid platform: '%s'", value)`; for a CLI tool the risk is cosmetic (stderr injection of ANSI codes in user-controlled terminals). Current code is CWE-209 compliant — no internal paths, class names, or stack traces in the message. SEC-002 added as a VERIFY task to lock in the invariant. **A04, A05, A07, A08, A09, A10:** N/A. File system safety: SEC-003 (CWE-22, path traversal) and SEC-004 (credential hygiene) added as pre-delete checks for TASK-004's recursive `rm -rf` of `targets/github-copilot/`; pre-delete `find -type l` to verify no symlinks escape the base directory. SEC-001 added to TASK-006 to scan the 18 cleaned YAMLs for accidentally leaked secrets after edit. Compliance: not applicable to this project (no PII, no regulatory scope). No new cryptographic operations, no new authentication paths, no new deserialization surfaces.

## Implementation Approach

Tech Lead consolidates agent proposals and upholds the story's existing §8 task decomposition: 6 atomic tasks, each producing one Conventional Commit, with build-green-between-tasks as the core invariant (RULE-001). Task 003 carries the breaking-change footer because it is the commit that removes `Platform.COPILOT` and the CLI contract value. Task 006 is the final PR creation gate and carries the full verification burden: mvn clean verify, coverage ≥ 95%/90% (degradation ≤ 2pp vs. baseline 95.69%/90.69%), 6 acceptance test validations, grep sanity, commit history check, and PR body assembly with before/after metrics table. Tasks 004 and 005 are marked parallelizable (confirmed in `plans/epic-0034/implementation-map-0034.md` Ordem 4/5). Escalation notes captured in the task breakdown document three reconciliation items (test count delta, YAML count delta, FileCategorizer workflows fall-through decision) and one risk confirmation (PrIssueTemplateAssembler belongs to Copilot target per `AssemblerFactory.buildGithubOutputAssemblers()` lines 176-179). All 7 shared class edits in task 003 are minimal: remove constants, remove flags, update Javadoc — no refactoring beyond what deletion requires.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 6 |
| Architecture tasks (delete + edit main) | 4 (TASK-001, 003, 004, 005) |
| Test tasks (delete) | 1 (TASK-002) |
| Security tasks (VERIFY criteria merged into others) | 0 standalone; 4 augmented |
| Quality gate tasks (VERIFY criteria merged into TASK-006) | 0 standalone; 7 augmented |
| Validation tasks (PO merged into TASK-006) | 0 standalone; 4 augmented |
| Merged tasks (deduped across agents) | 0 (story §8 task IDs reused) |
| Augmented tasks (security/QA/TL criteria injected into story tasks) | 6 (every task has at least 1 cross-agent augmentation) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Shell glob accidentally deletes `.github/workflows/` | QA + Tech Lead | CRITICAL | Medium | Explicit `-not -name 'workflows'` exclusion in find command; mandatory post-delete count check (expected: 95) in TASK-005 |
| Symlinks inside `targets/github-copilot/` escape base dir during rm -rf | Security | HIGH | Very Low | Pre-delete `find -type l` check in TASK-004 |
| Coverage degradation > 2pp because deleted tests cover more than deleted production | QA + Tech Lead | HIGH | Medium | Incremental JaCoCo check after TASK-002; RULE-002 hard gate in TASK-006 |
| `AssemblerRegressionSmokeTest` fails due to stale `expected-artifacts.json` | QA | MEDIUM | High | Regenerate manifest in TASK-005 or TASK-006, or defer to story-0034-0005 with documented expected failure |
| `FileCategorizer` workflows regression (files categorized as "Other" post-edit) | Architect | LOW | High | Document decision in TASK-003 commit body; add single-line branch for `.github/workflows/` if reviewer objects |
| Story text count deltas (15 tests vs 14, 18 YAMLs vs 17) propagate to wrong implementation | Tech Lead | LOW | Medium | Escalation notes in task breakdown; baseline document is authoritative source of truth |
| Breaking-change communication missing from commit history | Tech Lead | MEDIUM | Low | BREAKING CHANGE footer mandatory on TASK-003 commit; CHANGELOG deferred to story-0034-0005 |
| PrIssueTemplateAssembler is not Copilot-specific | Architect | LOW | Low | Verified in AssemblerFactory lines 176-179: registered with GITHUB target and COPILOT platform. Safe to delete |

## DoR Status

See `plans/epic-0034/plans/dor-story-0034-0001.md`. Verdict: **READY** — all 10 mandatory checks pass. 6 Gherkin scenarios (exceeds minimum of 4), data contracts explicitly defined in story §5, dependencies clear (no blockers), task decomposition atomic, architecture/test/security sections populated in this report, no compliance check applicable (project config: compliance=none).
