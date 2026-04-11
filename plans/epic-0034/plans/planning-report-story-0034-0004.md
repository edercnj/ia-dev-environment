# Story Planning Report -- story-0034-0004

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0034-0004 |
| Epic ID | 0034 |
| Date | 2026-04-10 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story-0034-0004 is the **hygienization phase** of EPIC-0034. Stories 0001-0003 delete the three legacy target platforms (GitHub Copilot, Codex, Agents) and their directly-owned classes/resources/golden files. Story 0004 operates on the aftermath: 10+ shared classes across the `application/assembler/`, `domain/model/`, `domain/stack/`, `cli/`, and `smoke/` packages still carry residual conditional logic — `hasCopilot`/`hasCodex` flags, `.github/templates/` output constants, parameterized smoke-test scenarios with `@Nested class Copilot`/`@Nested class Codex`, dead category names in `FileTreeWalker`, and stale help text in `IaDevEnvApplication`. This story removes that residue. The output is a codebase where a new contributor opening `ReadmeAssembler`, `PlatformContextBuilder`, `PlatformFilter`, or any smoke test sees linear Claude-only logic with no dead branches to reason about.

The story is decomposed into **7 sequential tasks** (including an inserted `002b` per story §8). Execution is strictly linear because nearly every task edits files that the preceding task touched; there is no safe parallelism. The most critical task is `TASK-0034-0004-003` (PlanTemplatesAssembler cleanup), which operates adjacent to the RULE-004-protected `resources/shared/templates/` directory. That task has two independent integrity gates (pre- and post-edit) that verify the template directory is byte-for-byte unchanged; failure of either gate aborts the story.

The planning surface the story explicitly in scope of 5 roles in parallel synthesis:
- **Architect:** mapped 31 discrete code-edit actions across the 7 tasks, preserving the dependency direction (no domain depending on adapters).
- **QA Engineer:** identified 26 discrete test-update actions spanning unit + smoke + integration layers, plus an explicit RULE-006 green-to-removed audit step in task 005.
- **Security Engineer:** augmented 12 DoD items with CWE-tagged controls (CWE-22 path traversal around template I/O, CWE-209 error message leakage in validators, CWE-710 dead-code orphan-import hygiene).
- **Tech Lead:** set 20 numeric quality gates (line count thresholds per file, method length, method count, coverage deltas, LOC reduction target, commit format audit).
- **Product Owner:** cross-referenced each of the 7 Gherkin scenarios in story §7 to a concrete shell-command evidence step in task 006, producing a 10-item verification checklist.

Consolidation applied: the `merge` rule collapsed Architect+TL+QA overlap on tasks 001/002/004; the `augment` rule injected Security controls into tasks 003/004; the `Tech Lead wins` rule resolved the PlatformFilter inlining disagreement in task 002b; the `PO amends` rule added cross-reference DoD items to task 006 without modifying the story's Gherkin scenarios.

## Architecture Assessment

**Layers affected:** application (primary — 8 classes edited in `assembler/` package), domain (2 — `PlatformParser` in `model/`, `StackValidator` in `stack/`), cli (3 — `PlatformPrecedenceResolver`, `IaDevEnvApplication`, indirectly `GenerateCommand`), smoke test adapter (4 smoke test classes).

**Classes deleted (1):** `ReadmeGithubCounter.java`. No other class-level deletions — the rest of the work is method-level edits and conditional-branch removals.

**Classes edited with non-trivial body changes (12):**
- `ReadmeAssembler.java`, `ReadmeUtils.java`, `MappingTableBuilder.java`, `SummaryTableBuilder.java`, `SummaryRowFilter.java`, `PlatformContextBuilder.java`, `PlatformFilter.java`, `PlanTemplatesAssembler.java`, `EpicReportAssembler.java`, `FileTreeWalker.java`, `PlatformParser.java`, `StackValidator.java`.

**Classes edited with metadata-only changes (2):**
- `PlatformPrecedenceResolver.java` (Javadoc only), `IaDevEnvApplication.java` (`@Command.description` + Javadoc).

**New classes possibly introduced (1 conditional):**
- `PlanTemplateDefinitions.java` — extracted from `PlanTemplatesAssembler` IF the post-edit line count still exceeds Rule 03's 250-line limit. The extraction is a fallback triggered inside task 003 if measured line count warrants it.

**Dependency direction validation:** all edits preserve the inner→outer dependency flow. No new adapter→domain reverse dependencies are introduced. The `PlatformFilter` simplification in task 002b keeps the public API stable so application-layer callers remain unaffected. The `FileTreeWalker` edit in task 004 removes category-name string literals without altering the method signature — `ExpectedArtifactsGenerator` still calls the method unchanged.

**Package-structure violation risk:** none. Story 0004 stays entirely within existing packages; no new packages are created. The `PlanTemplateDefinitions` helper (if extracted) lives in the same `application.assembler` package as its source class.

## Test Strategy Summary

**Outer loop (Acceptance Tests — AT-N):** 7 Gherkin scenarios in story §7 map to 7 verification steps in task 006. Scenario 1 (build verde) maps to `mvn clean verify`. Scenario 2 (templates intactos) maps to `git diff + find wc -l`. Scenarios 3, 4, 5, 6, 7 are direct grep/read/run commands.

**Inner loop (Unit Tests — UT-N):** distributed across tasks 001-005:
- Task 001: `ReadmeAssemblerTest` re-run (no new tests; existing test is the safety net for the delete).
- Task 002: `MappingTableBuilderTest`, `SummaryTableBuilderTest` updated — test cases for multi-column output removed, single-column retained.
- Task 002b: `PlatformContextBuilderTest`, `PlatformFilterTest` updated — multi-platform intersection tests removed.
- Task 003: `PlanTemplatesAssemblerTest`, `EpicReportAssemblerTest` updated; new integration test added asserting single-target copy behavior.
- Task 004: `FileTreeWalkerTest`, `ExpectedArtifactsGeneratorTest`, `PlatformParserTest`, `StackValidatorTest`, `PlatformPrecedenceResolverTest`, `IaDevEnvApplicationTest` updated.
- Task 005: 4 smoke tests edited (PlatformDirectory, AssemblerRegression, CliModes, GoldenFileCoverage) — method count reduces from ~25 to ~10 in `PlatformDirectorySmokeTest` alone.

**Integration Tests (IT-N):** 1 new IT in task 003 — asserts that `PlanTemplatesAssembler.assemble()` produces exactly 15 files in `.claude/templates/` and no `.github/templates/` in the output temp dir.

**Contract Tests:** none needed — this story has no public API surface changes beyond removing already-dead platform identifiers, which are covered by `StackValidatorTest` and `PlatformParserTest`.

**Coverage impact:** task 002b reduces branch count (removes dead branches), which should improve branch percentage. Task 005 reduces test count (removes trivial negative assertions), which could nominally lower the absolute covered-lines count but not the ratio. Task 006 verifies the ratio holds at >= 95% line / >= 90% branch.

**RULE-006 (TDD compliance on removal):** explicit audit in task 005 pre-flight — every deleted test method must have been green in the story-0034-0003 final merge commit. No test is deleted on the grounds of being broken.

**TPP ordering:** tasks 001→005 follow nil (degenerate branch removal) → constant (row-list constant reduction) → scalar (single-flag collapse) → conditional (validator simplification) → iteration (parameterized scenario collapse). Task 006 is VERIFY with no TPP level.

## Security Assessment Summary

**OWASP Top 10 mapping:**
- **A03 Injection:** not directly applicable — this story edits validators that are input-parsers for YAML config. `PlatformParser` and `StackValidator` error messages must not echo raw user input in a way that enables log injection. Existing `ConfigValidationException` messages use `%s`-formatting with the offending value; task 004 DoD item `[SEC-007]` verifies no format-string vulnerabilities are introduced.
- **A04 Insecure Design:** not applicable — no design changes, only simplification.
- **A05 Security Misconfiguration:** task 003 is the only task touching file-I/O paths (template copy destinations). The change (remove `.github/templates/` write) strictly reduces the surface, not increases it.
- **A06 Vulnerable Components:** not applicable — no dependency changes in this story.
- **A08 Software and Data Integrity:** indirectly addressed by RULE-004 template integrity gate — the story guarantees shared/templates/ is not modified, so the `ia-dev-env` generator's output contract for Claude templates remains verifiable.
- **A09 Logging Failures:** CWE-209 gate in task 004 verifies error messages do not leak internal paths or stack traces.

**Input validation:** all user-input paths flow through `PlatformParser.parse()` and `GenerateCommand`. Both are edited in task 004. DoD items `[SEC-007]` and `[SEC-008]` verify no silent fallthrough for unknown inputs.

**Secrets management:** no credentials introduced or modified. Task 006 `[SEC-012]` runs a final grep for hardcoded secrets as a sanity check.

**Path traversal (CWE-22):** task 003 edits `PlanTemplatesAssembler` which resolves user-controlled `outputDir` to `.claude/templates/`. The existing code uses `outputDir.resolve(target)` without explicit normalization, but this was not introduced by the story — the pattern pre-dates EPIC-0034. `[SEC-004]` in task 003 at least verifies the template SOURCES are not accidentally written to via the outputDir path (the integrity gate).

**Dead code (CWE-710):** task 001 `[SEC-001]` and task 002 `[SEC-002]` grep for orphan imports and residual platform strings to prevent dead-code accumulation.

**Risk level:** **LOW**. Story surface is refactoring + deletion of known-dead code paths. No new data flows, no new I/O sinks, no new authentication paths, no new crypto. The only non-trivial security concern is RULE-004 protection of the shared templates directory, which is gated twice.

**Compliance:** not applicable — project identity (`01-project-identity.md`) declares "none" for compliance frameworks. Check #11 (Compliance Assessment) is N/A.

## Implementation Approach

**Chosen strategy:** strict sequential execution, 1 task at a time, each task producing 1 atomic commit on the story branch `feature/epic-0034-remove-non-claude-targets`. No worktree parallelism. No task-level PRs (epic-0034 uses story-level PR per story-0001 template). Tech Lead authority applied in task 002b to keep `PlatformFilter` as a class rather than inline it.

**Tooling:**
- Build: `mvn -pl java compile test` per task (fast iteration), `mvn clean verify` only in task 006.
- Grep: plain `grep -rn` (no `rg` wrapper needed — plain greps are faster to type and copy-paste into PR bodies).
- Integrity gate: `git diff origin/main -- <path>` + `find | wc -l`.
- Commit: Conventional Commits with explicit task-ID trailer; no `--no-verify`, hooks enforced.

**Branch strategy:** all 7 tasks commit directly to `feature/epic-0034-remove-non-claude-targets`. No sub-branches per task. Task 006 creates the PR against the epic integration branch (or directly against `develop` if the epic integration branch is short-lived — defer decision to epic implementation map §8).

**Rollback strategy:** each task's commit is atomic and scoped. If task N fails post-merge (e.g. hidden regression surfaces in a later story), revert commit N; the story branch goes back to the post-task-(N-1) state. No data migrations, no schema changes, no config-map updates — rollback is pure git.

**Cross-cutting hygiene applied:**
- Every task commit includes a task-ID trailer (Rule 08 Conventional Commits).
- Every code-editing task verifies the file stays <= 250 lines and no method > 25 lines (Rule 03).
- Every test-editing task follows `[methodUnderTest]_[scenario]_[expectedBehavior]` naming (Rule 05).
- Error-handling pattern consistency: all validators use `ConfigValidationException` with the same `%s`-formatted message structure (Rule 05 cross-file consistency).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 7 (including inserted 002b) |
| Architecture tasks | 5 (001, 002, 002b, 003, 004) |
| Test tasks | 1 (005) |
| Security-augmented tasks | 3 (003, 004, 006) |
| Quality gate tasks | 1 (006) |
| Validation tasks | 1 (006 — PO checklist folded in) |
| Merged tasks (during consolidation) | 7 (every task has merged sources from >= 2 agents) |
| Augmented tasks (security injected) | 3 (003, 004, 006) |
| Tech Lead overrides | 1 (002b PlatformFilter inlining rejected) |
| PO amendments to story Gherkin | 0 (existing 7 scenarios cover surface; PO added verification-tracing only) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| RULE-004 violation: accidental edit to `shared/templates/` | Security + PO | **CRITICAL** | Low | Dual integrity gates (tasks 003 + 006); automated `git diff + find wc -l`; PR review sign-off |
| `PlanTemplatesAssembler.java` remains > 250 lines post-edit (Rule 03) | Tech Lead | Medium | Medium | Task 003 fallback: extract `PlanTemplateDefinitions` helper; line count measured in DoD |
| Coverage regression: branch % drops below 90% | QA + Tech Lead | High | Medium | Task 002b reduces dead branches (improves ratio); task 006 measures; degradation <= 2pp absorbed |
| LOC reduction target (>= 10%) not met | Product Owner | Low | Medium | Document actual ratio in PR; target is aspirational, not hard gate; reviewer can accept a lower reduction if justified |
| Hidden test references to deleted `ReadmeGithubCounter` in the test tree | QA | Medium | Low | Task 001 DoD item (g) greps for `ReadmeGithubCounterTest.java`; CI build catches any surviving references |
| RULE-006 violation: deleted test was silently failing in baseline | QA | High | Low | Task 005 pre-flight audit step; PR review confirmation |
| CLI manual smoke target glob mismatches actual jar name | Tech Lead | Low | Low | Task 006 uses `java/target/ia-dev-env-*.jar` glob; fallback is `ls java/target/*.jar` |
| Story-0003 LOC baseline file missing at task-006 execution time | Tech Lead | Medium | Medium | Task 006 fallback: measure retroactively from story-0003 merge commit via `git show` |
| `PlatformFilter` simplification changes observable behavior in an edge case (empty descriptor list) | Architect | Low | Low | Task 002b keeps public API stable; existing tests cover empty-input case |
| `grep -rn 'agents'` returns many benign hits (Claude `.claude/agents/` is valid) | Tech Lead | Low | High | Narrow grep in task 006 to exclude Claude paths; document allowed residues |
| Error messages in validators accidentally leak stack context (CWE-209) | Security | High | Low | Task 004 DoD item `[SEC-007]` reads each error message; explicit manual inspection |
| `EpicReportAssembler.GITHUB_OUTPUT_SUBDIR` cleanup missed (second assembler with same constant) | Architect | Medium | Medium | Task 003 explicitly covers BOTH assemblers; escalation note documents the discovery |
| Path-resolution for `.claude/templates/` in integration test drifts between local and CI | QA | Low | Low | Use `@TempDir` Path; relative paths from tempDir only |
| Breaking-change flag propagation: `--platform copilot` rejection vs. `--platform all` acceptance | Product Owner | Low | Low | Task 005 CliModesSmoke tests both cases explicitly |

## DoR Status

**READY.** All 10 mandatory checks pass. Both conditional checks (#11 Compliance, #12 Contract tests) are N/A per the project's declared configuration (`compliance: none`, `contract_tests: false`).

- Mandatory checks passed: **10/10**
- Conditional checks applicable: **0/2** (both N/A)
- Total artifacts written: **10** (1 tasks breakdown + 7 task plans + 1 planning report + 1 DoR checklist)
- Verdict evidence: see `plans/epic-0034/plans/dor-story-0034-0004.md`

Story is **READY** for x-dev-story-implement execution.
