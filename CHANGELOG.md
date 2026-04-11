# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Removed

- **BREAKING:** `Platform.COPILOT` enum value (generator no longer emits `.github/` Copilot artifacts). (EPIC-0034 / story-0034-0001)
- **BREAKING:** `Platform.CODEX` enum value (generator no longer emits `.codex/` artifacts). (EPIC-0034 / story-0034-0002)
- **BREAKING:** `AssemblerTarget.GITHUB` — `.github/` assembler target retired. (EPIC-0034 / story-0034-0001)
- **BREAKING:** `AssemblerTarget.CODEX` — `.codex/` assembler target retired. (EPIC-0034 / story-0034-0002)
- **BREAKING:** `AssemblerTarget.CODEX_AGENTS` — `.agents/` (shared Codex-agents) target retired. (EPIC-0034 / story-0034-0003)
- **BREAKING:** 18 Java assembler classes deleted across three stories (8 Copilot + 7 Codex + 2 Agents + `ReadmeGithubCounter`), plus ~34 test classes and 2 test fixtures. (EPIC-0034 / stories 0034-0001..0004)
- **BREAKING:** Golden file fixtures under `.github/` (non-workflows), `.codex/`, and `.agents/` — ~8178 files total. The `.github/workflows/` fixtures are **preserved** (RULE-003). (EPIC-0034 / stories 0034-0001..0003)
- **BREAKING:** Resource directories `java/src/main/resources/targets/github-copilot/` (131 files) and `java/src/main/resources/targets/codex/` (15 files). `java/src/main/resources/shared/templates/` is preserved (RULE-004). (EPIC-0034 / stories 0034-0001..0002)

### Changed

- **BREAKING:** CLI `--platform` flag now accepts only `claude-code` (and the backward-compatibility keyword `all`, which is now functionally equivalent to `claude-code`). Previous values `copilot`, `codex`, and `agents` are rejected with a clear error message that lists accepted values. (EPIC-0034 / story-0034-0001)
- **BREAKING:** Default value of `--platform` is now `claude-code` when the flag is omitted. Previously the default was `all`. (EPIC-0034 / story-0034-0001)
- Generator output per profile reduced from ~9500 to ~830 artifacts (~91% fewer files). The `expected-artifacts.json` smoke manifest was regenerated to match. (EPIC-0034 / story-0034-0005)
- `CLAUDE.md` at repo root reduced from 289 to ~115 lines by removing multi-target documentation sections. (EPIC-0034 / story-0034-0005)
- `readme-template.md` cleaned of `.github/`, `.codex/`, and `.agents/` directory descriptions; artifact-conventions table now lists only Claude artifacts. (EPIC-0034 / story-0034-0005)
- `README.md` at repo root updated: tagline, overview, CLI reference, "What's Generated" tree, and project-structure diagram all reflect single-target (Claude Code) scope. (EPIC-0034 / story-0034-0005)

### Migration

Users with automated scripts or CI pipelines invoking `ia-dev-env` must update as follows:

- Replace `--platform copilot`, `--platform codex`, or `--platform agents` with `--platform claude-code`, **OR** drop the flag entirely (the new default is `claude-code`). `--platform all` remains accepted as a backward-compatibility alias and now means "generate claude-code only" (since `claude-code` is the only target).
- Remove any downstream tooling that consumes `.github/instructions/`, `.github/skills/`, `.github/prompts/`, `.codex/config.toml`, `.codex/requirements.toml`, or `.agents/skills/` artifacts — these are no longer produced by the generator.
- `.github/workflows/` files in generated projects are unaffected; CI/CD pipelines continue to work without changes (RULE-003).
- Claude Code users with existing `.claude/` output: no action required. Regenerate with the same command you used before, minus any platform flag.
- Per Rule 08 Semantic Versioning, the next release of this tool is a **MAJOR version bump** (2.x → 3.0.0).

### Rollback

This epic introduces no database migrations and no persistent state changes — the generator is stateless. To roll back, revert the merge commit for EPIC-0034 on `develop` and rebuild. Prior multi-target behavior is restored atomically with no data migration needed.

### Security

- CLI error messages for rejected platform values contain no class names, stack traces, or file paths (CWE-209 compliance verified via TASK-0034-0005-004 evidence).
- `ReadmeGithubCounter` class and all GitHub-specific readme-generation paths removed (attack surface reduction).
- `ExpectedArtifactsGenerator` output path verified as a compile-time constant (CWE-22 path traversal risk check, story-0034-0005).

## [2.3.0] - 2026-04-10

### Added
- **Rule 13 — Skill Invocation Protocol (EPIC-0033 / STORY-0033-0001):** New canonical rule at `java/src/main/resources/targets/claude/rules/13-skill-invocation-protocol.md` defining three permitted delegation patterns (INLINE-SKILL, SUBAGENT-GENERAL, SUBAGENT-RESEARCH) and forbidding the bare-slash `Invoke /x-foo` anti-pattern in delegation contexts. The slash form remains permitted in user-facing `## Triggers` and `## Examples` sections. Referenced 30+ times across `skills/core/` and in `CLAUDE.md` (rules index updated; gaps at 10/11/12 reserved for conditional rules `10-anti-patterns`, `11-security-pci`, `12-security-anti-patterns`). Documented in **ADR-0002**.
- **TaskCreate/TaskUpdate Level 2 visibility (EPIC-0033 / STORY-0033-0002):** `x-dev-epic-implement` now emits `TaskCreate("Story {id}: {title}")` per story dispatch and closes it after the subagent returns. `x-dev-story-implement` emits per-phase tracking tasks (Phase 0-3) plus per-task entries inside the Phase 2 inner loop. Description format follows CR-02. FAILED/BLOCKED terminal states surface via `"(FAILED) "` / `"(BLOCKED) "` description prefix. `execution-state.json` remains authoritative for resume/checkpoint logic (CR-04).
- **Planning subagent Level 3 visibility (EPIC-0033 / STORY-0033-0003):** Each of the 7 active planners in Phase 1 (1A Architecture, 1B Impl Plan, 1B Test Plan, 1C Task Decomposer, 1D Event Schema, 1E Security primary+fallback, 1F Compliance) appears individually in the Claude Code task list. Two strategies: orchestrator-managed (around `Skill(...)` calls) and subagent-managed (FIRST/LAST ACTION in subagent prompt). Skipped planners do NOT emit TaskCreate (AC-4).
- **Per-specialist tracking in `x-review` Phase 2 — Level 3 (EPIC-0033 audit hardening):** One `TaskCreate("Review: {specialist} — Story {STORY_ID}")` per active specialist (9 possible: qa, perf, db, obs, devops, data-modeling, security, api, events) as siblings with the `Skill(...)` calls in a single Batch A message. Batch B closes them after reviews return.
- **Explicit `--orchestrated` flag in `x-test-tdd` (EPIC-0033 / STORY-0033-0003):** Replaces the fragile implicit detection for compact-vs-full output format. `x-dev-story-implement` Phase 2 step 2.2.5 passes `--orchestrated`; direct user invocations omit it and get full multi-line format.
- **Parallelism + tracking batching documentation (EPIC-0033 audit hardening):** New section at the top of Phase 1B-1F in `x-dev-story-implement/SKILL.md` (with enumeration table of orchestrator-managed vs subagent-managed planners) and `planning-phases.md`. Documents the 3-step Batch A → Wait → Batch B pattern.
- **ADR-0002 — Skill Delegation Protocol:** Documents the architectural decision behind Rule 13, context, three permitted patterns, parallelism strategy, observability, trade-offs, and cross-references to the EPIC-0033 PR chain.
- **EPIC-0034 plans (docs):** Epic plan, story files, and implementation map for removing non-Claude targets.

### Changed
- **Skill naming standardization (EPIC-0032):** 25 skills renamed in the source of truth (`java/src/main/resources/targets/`) to follow the `x-{category}-{action}` convention (RULE-001). 15 core skills: `x-dev-lifecycle` → `x-dev-story-implement` (consolidation), `x-commit` → `x-git-commit`, `x-tdd` → `x-test-tdd`, `x-format` → `x-code-format`, `x-lint` → `x-code-lint`, `x-changelog` → `x-release-changelog`, `x-docs` → `x-doc-generate`, `x-codebase-audit` → `x-code-audit`, `x-spec-drift-check` → `x-spec-drift`, `x-setup-dev-environment` → `x-setup-env`, `x-ci-cd-generate` → `x-ci-generate`, `x-worktree` → `x-git-worktree`, `x-plan-task` → `x-task-plan`, `x-fix-pr-comments` → `x-pr-fix-comments`, `x-fix-epic-pr-comments` → `x-pr-fix-epic-comments`. 10 conditional skills: `x-sast-scan`/`x-dast-scan`/`x-secret-scan`/`x-container-scan`/`x-infra-scan`/`x-sonar-gate` grouped under `x-security-*`, `x-pentest` → `x-security-pentest`, `x-contract-lint` → `x-test-contract-lint`, `setup-environment` → `x-setup-stack`, `instrument-otel` → `x-obs-instrument`. All cross-references, Java source (`SkillGroupRegistry`, `SkillsSelection`, `SecurityBaselineWriter`), and documentation updated. No backward compatibility for old names (RULE-004).
- **Source of Truth rule (RULE-002):** `CLAUDE.md` and `AGENTS.md` now explicitly declare `java/src/main/resources/targets/` as the authoritative location for skills, knowledge packs, agents, rules and templates. Direct edits to generated directories (`.claude/`, `.github/`, `.codex/`, `.agents/`, `src/test/resources/golden/`) are forbidden (story-0032-0001).
- **13 logical delegation routes converted (24 physical locations) (EPIC-0033 / STORY-0033-0001):** All skill-to-skill delegations in `skills/core/` now use explicit `Skill(skill: "...", args: "...")` calls following Rule 13. Affected files: `x-test-tdd/SKILL.md` (4 RED/GREEN/REFACTOR/Slim Mode), `x-git-commit/SKILL.md` (2 format/lint), `x-review/SKILL.md` (9 specialists), `x-dev-story-implement/SKILL.md` + `references/verification-phase.md` (5 synced pairs), `planning-phases.md` (1A), `x-dev-epic-implement/SKILL.md` (2 Phase 4), `x-owasp-scan/SKILL.md` (1 A06). Audit grep confirms 0 bare-slash matches remaining.
- **`allowed-tools` hardened (EPIC-0033):** `x-test-tdd` += `Skill`; `x-git-commit` += `Skill`; `x-dev-epic-implement` += `Skill, Agent, TaskCreate, TaskUpdate`; `x-dev-story-implement` += `Skill, Agent, TaskCreate, TaskUpdate`; `x-review` += `TaskCreate, TaskUpdate`.
- **`x-review` Phase 2 restructured (EPIC-0033 / STORY-0033-0001):** Nine specialist dispatches converted from bullet list of bare-slash commands to nine explicit `Skill(...)` calls in a single Batch A message. Activation conditions moved to a bullet list before the code block so tool-call lines are comment-free and copy-safe.
- **Step 1E (Security Assessment) aligned between SKILL.md and planning-phases.md (EPIC-0033 audit hardening):** Pre-existing drift resolved — both files describe 1E identically as `Skill(x-threat-model)` primary + inline Security Engineer subagent fallback. Orchestrator explicitly closes `securityTaskId` before launching fallback.
- **Subagent launches in Phase 1 converted to explicit `Agent(...)` calls (EPIC-0033 audit hardening):** Phase 1B Impl Plan, 1D Event Schema, 1E fallback, and 1F Compliance now show canonical `Agent(subagent_type: "general-purpose", description: "...", prompt: "...")` tool calls in both `x-dev-story-implement/SKILL.md` and `planning-phases.md`.
- **Rule 13 Pattern 2 renamed from SUBAGENT-SKILL to SUBAGENT-GENERAL (EPIC-0033 audit hardening):** Now covers (a) subagent invokes another skill, (b) subagent produces an artifact, (c) subagent performs complex analysis. All three use the same `Agent(...)` call shape.
- **Non-Claude target removal (EPIC-0034):** `targets/github-copilot/` and `targets/codex/` directories removed from the source of truth. `targets/claude/` is now the sole authoritative target. Generator, tests, and golden fixtures updated accordingly.

### Fixed
- **Orphan reference to non-existent `x-test-contract-lint` removed (EPIC-0033 / STORY-0033-0001):** `x-dev-story-implement/SKILL.md:269` referenced a skill that does not exist in `skills/core/`. Replaced with inline linter guidance (redocly/cli, buf lint, spectral lint) plus a follow-up note.
- **CLAUDE.md rules index backfill (EPIC-0033 / STORY-0033-0001):** Root `CLAUDE.md` rules table previously listed only rules 01-06 — stale since 07-09 had been added without updating the index. Refreshed to include Rule 13 with an explanatory note about the reserved gap at 10-12 for conditional rules.
- **3 additional delegation routes using "Invoke skill X" phrasing (EPIC-0033 / STORY-0033-0003 drive-by):** Pre-existing Story 0001 misses at `x-dev-story-implement/SKILL.md:335` (Step 1A), line 399 (1B Test Plan), line 413 (1C Task Decomposition). The original audit grep did not match the `"Invoke skill X"` variant.
- **`x-test-tdd` implicit mode detection removed (EPIC-0033 / STORY-0033-0003):** Replaced fragile `"if this skill was invoked via Skill tool"` check with explicit `--orchestrated` flag.
- **`TaskUpdate(id: {placeholder})` disambiguation (EPIC-0033 audit — PR #258 review):** Removed curly-brace placeholders from 5 `TaskUpdate` calls in `x-dev-story-implement/SKILL.md` and added explanatory prose clarifying that each token is a runtime reference to the numeric integer returned by `TaskCreate`.
- **FAILED vs BLOCKED signaling split (EPIC-0033 audit — PR #258 review):** Phase 2 inner-loop closeout (step 2.2.11) previously conflated FAILED and BLOCKED. Split into distinct prefixes with CR-04 reference updated to `SUCCESS/FAILED/BLOCKED`.
- **1E fallback `securityTaskId` orphaning (EPIC-0033 audit — PR #259 review):** Added explicit `TaskUpdate(id: securityTaskId)` before the fallback `Agent(...)` launch so the orchestrator's tracking task doesn't stay open forever when `x-threat-model` is unavailable.
- **Trailing inline comments on `Skill(...)` calls in `x-review` Phase 2 (EPIC-0033 audit — PR #257 review):** Removed trailing `# only if ...` comments and moved activation conditions to a bullet list preceding the tool-call block.
- **Weak `XReviewSkillTemplateTest` assertions strengthened (EPIC-0033 audit — PR #257 review):** Three `phase2_references*Skill` test methods now use `containsPattern` with regex that verifies both skill name and args payload (`args: "{STORY_ID}"`), allowing whitespace variation.
- **Rule numbered 13, not 10, due to conditional rule collision (EPIC-0033 audit — PR #257 review):** Slots 10 (`10-anti-patterns`), 11 (`11-security-pci`), and 12 (`12-security-anti-patterns`) were already reserved. Renamed to `13-skill-invocation-protocol.md` with a gap note in the `CLAUDE.md` rules index.

## [2.2.2] - 2026-04-10

### Fixed
- **Installer (`java/install.sh`) — version detection from `pom.xml`**: replaced the hardcoded `VERSION`/`JAR_NAME` constants with a `resolve_version()` step that parses the version from `java/pom.xml` at install time. Restores the documented `--jar=PATH` flow by deriving the version from the JAR filename pattern `ia-dev-env-VERSION.jar` (with `unknown` fallback) when a pre-built JAR is supplied, so the script no longer fails with "pom.xml not found" outside the source tree.
- **Installer success banner — ANSI escape codes**: color variables migrated from `'\033[...]'` to bash ANSI-C quoting `$'\033[...]'` so the actual ESC byte is stored. Both the `cat <<EOF` heredocs in `print_success()` / `usage()` and the `printf`-based log helpers now render colors consistently — previously users saw raw `\033[0;32m` text in the success banner.
- **Installer Quick Start — list every bundled stack**: a new `list_bundled_stacks()` helper introspects the installed fat JAR via the JDK `jar` tool (with `unzip` fallback), discovering every `shared/config-templates/setup-config.<name>.yaml` resource. The success banner now prints one `ia-dev-env generate --stack <name> --output my-project/` example per bundled stack instead of only `java-quarkus`, and stays in sync automatically as profiles are added or removed.
- **CLI version single-sourced from `pom.xml` (`IaDevEnvApplication`)**: replaced the hardcoded `@Command(version = "2.0.0")` with `versionProvider = CliVersionProvider.class`. The new provider reads from `dev/iadev/version.properties`, a Maven-filtered resource seeded from `${project.version}` at build time, so `ia-dev-env --version` now reflects the actual `pom.xml` version (`ia-dev-env 2.2.2`).
- **Dev wrapper (`java/bin/ia-dev-env`) — JAR discovery**: dropped the stale hardcoded `JAR_NAME="ia-dev-env-2.0.0-SNAPSHOT.jar"` (which no longer matched any built artifact) and replaced it with a glob over `target/ia-dev-env-*.jar` that picks the newest match. Removes the last `SNAPSHOT` reference from the dev tooling.
- **Tests**: `IaDevEnvApplicationTest`, `DistributionTest`, and `WrapperScriptTest` updated to assert against a SemVer regex / glob pattern instead of pinning a specific version literal. New `CliVersionProviderTest` covers the happy path (single line, program name prefix, SemVer body, no `unknown` fallback, no leaked `${project.version}` placeholder).

## [2.2.1] - 2026-04-09

### Fixed
- Resolve merge conflict markers committed to develop across 5 source templates and ~265 golden files. Conflicts between EPIC-0030 (context isolation, output compaction, lazy-kp loading) and EPIC-0031 (circuit breaker, error classification, graceful degradation) caused 27 test failures.
- Restore EPIC-0030 content lost during EPIC-0031 merges: CONTEXT MANAGEMENT sections, CONTEXT ISOLATION markers, specific KP reference paths (`security/references/application-security.md`, `compliance/references/`), and subagent role prompts (Senior Architect, Event Engineer, Security Engineer) in `x-dev-story-implement` and `x-dev-epic-implement`.
- Reposition CONTEXT ISOLATION markers after role names in subagent prompts to satisfy `assertPromptSectionContains` test assertions.
- Fix `x-dev-epic-implement` subagent dispatch to use explicit `Skill(skill: "x-dev-story-implement")` invocation instead of descriptive workflow steps.
- Update expected-artifacts manifest to reflect restored orchestrator template structure.
- Regenerate golden files for all 17 profiles + 2 platform-specific outputs.

## [2.2.0] - 2026-04-09

### Added
- **Error Catalog & Standardized Error Responses (EPIC-0031):** New `references/error-catalog.md` with 13 standardized error codes across 4 categories (TRANSIENT, CONTEXT, PERMANENT, CIRCUIT). Detection patterns and prescribed actions for each code. Error Classification sections added to `x-dev-epic-implement` and `x-dev-story-implement`.
- **Transient Error Retry with Exponential Backoff (EPIC-0031):** Automatic retry for transient errors (overloaded, rate limit, timeout) with exponential backoff (2s, 4s, 8s). Tool calls retry 3x, subagent dispatch retries 2x. Permanent errors fail immediately — never retried.
- **Local Integrity Gate Between Phases (EPIC-0031):** Integrity gate now executes by default in `--no-merge` mode (never DEFERRED). Creates temp branch, merges SUCCESS stories, runs compile+test+coverage, deletes temp branch. New `--skip-gate` flag for explicit opt-out (SKIPPED, not DEFERRED). Post-gate AskUserQuestion prompt with 3 options.
- **Subagent Failure Recovery (EPIC-0031):** SubagentResult expanded with `errorType`, `errorMessage`, `errorCode` fields. Recovery strategies by error type: TRANSIENT (2 retries), CONTEXT (1 retry with reduced prompt), TIMEOUT (1 retry with --skip-verification). Escalation after 3 consecutive failures.
- **Circuit Breaker for Epic Execution (EPIC-0031):** 3-state machine (CLOSED/OPEN/HALF_OPEN) with threshold escalation: 1 failure = WARNING, 2 = pattern analysis, 3 consecutive = PAUSE with AskUserQuestion, 5 total in phase = ABORT. Resets on SUCCESS or `--resume`.
- **Checkpoint Error History Schema v3.0 (EPIC-0031):** New top-level fields: `errorHistory` (array of error entries with timestamp, errorCode, resolution), `circuitBreaker` (state tracking), `contextPressure` (degradation level). Backward compatible with v2.0. Resume shows error summary.
- **Graceful Degradation on Context Pressure (EPIC-0031):** 3-level progressive degradation: Level 1 (reduce verbosity, skip optional phases), Level 2 (force delegation to subagents), Level 3 (save state, suggest --resume). Progressive advancement rule — never skips levels.

### Fixed
- Standardized `===`/`!==` operators across all pseudocode in orchestrator skills (was inconsistent `==`/`===` mix).
- Circuit breaker state transition table now includes "5 total failures → phase abort" row.
- `circuitBreaker` and `contextPressure` checkpoint defaults now include all required fields.
- `lastFailurePattern` uses clean enum values (TRANSIENT/CONTEXT/PERMANENT/MIXED) instead of template strings.
- Local integrity gate scope clarified for `--no-merge` mode in reference documentation.

## [2.1.0] - 2026-04-09

### Added
- **Skill Naming Standardization Epic (EPIC-0032):** Epic, 8 stories, and implementation map for renaming 25 skills to follow consistent `x-{category}-{action}` convention.
- **Skill Delegation Fix Epic (EPIC-0033):** Epic, 4 stories, and implementation map addressing broken Skill tool chain, zero TaskCreate visibility, planning subagent black boxes, and x-dev-story-implement/x-dev-story-implement naming inconsistency.

## [Unreleased]

### Added
- **Platform filter (EPIC-0025)**: Add `--platform` flag to `generate` command for targeted AI platform generation (claude-code, copilot, codex, all). Supports YAML config via `platform:` section. Default: all (backward-compatible).
- **Artifact Persistence & Standardization (EPIC-0024):** 12 new plan and review templates providing standardized output formats for all planning and review artifacts. Templates: Implementation Plan, Test Plan, Architecture Plan, Task Breakdown, Security Assessment, Compliance Assessment, Specialist Review, Tech Lead Review, Consolidated Review Dashboard, Review Remediation, Epic Execution Plan, and Phase Completion Report.
- **PlanTemplatesAssembler (EPIC-0024):** New assembler that copies 12 templates verbatim to both `.claude/templates/` and `.github/templates/` with mandatory section validation (RULE-004 dual-target, RULE-010 validation).
- **Pre-checks in 8 skills (EPIC-0024):** Idempotency pre-checks added to x-dev-story-implement, x-test-plan, x-dev-architecture-plan, x-lib-task-decomposer, x-review, x-review-pr, x-dev-epic-implement, and x-dev-implement. Skills verify artifact staleness before regenerating (RULE-002).
- **Consolidated Review Dashboard (EPIC-0024):** Cumulative dashboard created by x-review and updated by x-review-pr with parseable scores (XX/YY format) and review history (RULE-005, RULE-006).
- **Remediation Tracking (EPIC-0024):** Review remediation template with findings tracker, deferred justifications, and re-review results for systematic issue resolution.
- **Epic Execution Plan (EPIC-0024):** Execution plan template saved before epic execution begins, enabling human audit of strategy, phase timeline, and resource requirements.
- **Phase Completion Reports (EPIC-0024):** Per-phase reports documenting stories completed, integrity gate results, coverage delta, and next phase readiness.

### Changed
- **x-dev-story-implement (EPIC-0024):** Now produces 6 artifact types with pre-checks (previously 2). Generates implementation plan, test plan, architecture plan, task breakdown, security assessment, and compliance assessment with staleness verification.
- **x-dev-implement (EPIC-0024):** Consumes existing plans as context when available, ensuring consistency between x-dev-story-implement planning and x-dev-implement execution.
- **x-review (EPIC-0024):** Generates consolidated review dashboard with parseable specialist scores. Dashboard is cumulative across review rounds.
- **x-review-pr (EPIC-0024):** Updates consolidated review dashboard with Tech Lead review round, providing complete quality visibility in a single file.
- Restructured project directories to adopt SDD (Spec-Driven Development) layout
  - `docs/` replaced by `steering/`, `specs/`, `plans/`, `results/`, `contracts/`, `adr/`
  - Java assembler output paths updated to match new structure
  - Skill templates updated with new directory references

### Added
- **Output Directory Cleanup + Overwrite Protection (story-0005-0015):** Removed redundant legacy `docs/epic/` output from `EpicReportAssembler` — epic template now only emits to `.claude/templates/` and `.github/templates/`. Added `--force` flag to `generate` command with overwrite protection: when the output directory already contains generated artifacts (`.claude/`, `.github/`, `steering/`, `specs/`, `plans/`, `results/`, `contracts/`, `adr/`; and legacy `docs/` when present), the CLI exits with an error listing conflicting directories unless `--force` is provided. `--dry-run` bypasses the overwrite check. New `src/overwrite-detector.ts` module with `checkExistingArtifacts()` and `formatConflictMessage()`. Golden files updated for all 8 profiles (removed legacy `docs/epic/`). 20 new tests (8 unit + 7 CLI + 5 integration).
- **E2E Tests + Generator Integration (story-0005-0014):** Capstone story for epic-0005. Orchestrator E2E test suite with 14 tests covering 6 scenarios: dry-run (no execution), happy path (5/5 SUCCESS), failure path (retry exhaustion + transitive block propagation), resume path (continue from checkpoint), partial execution (--phase filter), and parallel mode. Test infrastructure: configurable mock subagent dispatch, synthetic 5-story/3-phase implementation map, scenario runner composing all orchestrator modules (parser, checkpoint, retry, blocks, progress). Generator integration verified: `x-dev-epic-implement` skill already auto-discovered by SkillsAssembler and registered in GithubSkillsAssembler. Golden file tests already pass for all 8 profiles. CLAUDE.md updated with skill entry.
- **Parallel Execution with Worktrees (story-0005-0010):** Added parallel worktree dispatch capability to the `x-dev-epic-implement` SKILL.md template. When `--parallel` flag is active, executable stories in the same phase are dispatched concurrently in a SINGLE message via `Agent` tool with `isolation: "worktree"`. After all subagents complete, worktree branches are merged sequentially into the epic branch (critical path first per RULE-007). Includes conflict resolution subagent for automatic merge conflict handling and worktree cleanup rules. New sections: 1.4a (Parallel Worktree Dispatch), 1.4b (Merge Strategy), 1.4c (Conflict Resolution Subagent), 1.4d (Worktree Cleanup). 14 new content tests, 5 new dual-copy consistency terms, golden files regenerated for all 8 profiles. Extension point placeholder for story-0005-0010 removed from Section 1.7.
- **Progress Reporting + Execution Metrics (story-0005-0013):** New `src/progress/` module with real-time progress event emission and execution metrics tracking for the epic orchestrator. Features: 7 typed progress events (PHASE_START, STORY_START, STORY_COMPLETE, GATE_RESULT, RETRY, BLOCK, EPIC_COMPLETE) as a discriminated union, pure formatting functions for terminal output, pure metrics calculation (average duration, estimated remaining time), factory-based stateful reporter with injectable output sink, and checkpoint-persisted execution metrics. Extended `ExecutionMetrics` with 7 new optional fields (storiesFailed, storiesBlocked, elapsedMs, estimatedRemainingMs, averageStoryDurationMs, storyDurations, phaseDurations). All checkpoint changes are backward-compatible. 59 new tests (24 formatter, 13 calculator, 22 reporter), zero regressions on existing 3051 tests.
- **Partial Execution (story-0005-0009):** New `src/domain/implementation-map/partial-execution.ts` module with 4 pure functions for epic partial execution: `parsePartialExecutionMode` (mutual exclusivity validation), `validatePhasePrerequisites` (phases 0..N-1 completeness check), `validateStoryPrerequisites` (dependency satisfaction check), `getStoriesForPhase` (phase story filter). New types: `PartialExecutionMode` discriminated union, `PrerequisiteResult`. New error class: `PartialExecutionError`. SKILL.md templates updated with Partial Execution section (both Claude and GitHub copies). 23 unit tests + 12 content assertions, 100% line coverage, 96.77% branch coverage.
- **Resume Reclassification Engine (story-0005-0008):** New `src/checkpoint/resume.ts` module with pure functions for epic resume workflow. `reclassifyStories()` applies status transition rules (IN_PROGRESS→PENDING, FAILED with retries<MAX_RETRIES→PENDING, SUCCESS preserved). `reevaluateBlocked()` resolves BLOCKED stories when all dependencies reach SUCCESS. `prepareResume()` composes both operations. `MAX_RETRIES` constant (2) and `ReclassificationEntry` type added to checkpoint types. Resume Workflow section added to `x-dev-epic-implement` SKILL.md (both Claude and GitHub copies) documenting reclassification table, branch recovery, and BLOCKED reevaluation. 36 unit tests, 100% line/branch coverage.
- **Failure Handling: Retry + Block Propagation (story-0005-0007):** New `src/domain/failure/` module with pure-function retry evaluation and transitive block propagation. `evaluateRetry()` enforces RULE-005 (max 2 retries per story) with error context passthrough to retry subagents. `propagateBlocks()` performs BFS on the dependency DAG to transitively mark all dependents as BLOCKED (RULE-006). Discriminated union return types (`RetryDecision`, `BlockPropagationResult`) — no exceptions, no I/O. 26 tests, 100% line and branch coverage.
- **Consolidation Final — Review + Report + PR (story-0005-0011):** Replaced Phase 2 (Consolidation) and Phase 3 (Verification) placeholders in `x-dev-epic-implement` SKILL.md with full implementation content. Phase 2 dispatches tech lead review via `x-review-pr` subagent, generates `epic-execution-report.md` from template with all 18 placeholders resolved, creates PR via `gh pr create` with `[PARTIAL]` handling for incomplete epics. Phase 3 covers epic-level test suite validation, DoD checklist, final status determination (COMPLETE/PARTIAL/FAILED), and completion output. 28 new content tests, 7 new dual-copy consistency terms, golden files regenerated for all 8 profiles. Extension point placeholder for story-0005-0011 removed from Phase 1.
- **Integrity Gate Between Phases (story-0005-0006):** Extended `IntegrityGateEntry` with `branchCoverage` (optional number) and `regressionSource` (optional string). Added integrity gate section to `x-dev-epic-implement` SKILL.md with gate subagent prompt, regression diagnosis, gate result registration, and RULE-004 enforcement. 25 new tests. Golden files updated for all 8 profiles.
- **Dry-run mode (story-0005-0012):** Domain module `src/domain/dry-run/` implementing
  execution plan simulation for the epic orchestrator. Pure functions `buildDryRunPlan()`
  and `formatPlan()`/`formatStoryDetail()` compute and render the plan without side effects.
  Supports flag combinations: `--resume` (checkpoint status merge), `--phase N` (filter),
  `--parallel` (concurrency indicators), `--story XXXX-YYYY` (single story detail).
  35 tests (28 planner + 7 formatter), 98.94% line / 98.55% branch coverage.
- **Orchestrator Core Loop + Sequential Dispatcher (story-0005-0005):** Replaced Phase 1 placeholder in `x-dev-epic-implement` SKILL.md with full execution loop logic. Core loop iterates phases from the dependency DAG, dispatches subagents sequentially per story with critical path priority (RULE-007), validates SubagentResult contracts (RULE-008), updates checkpoint after each story (RULE-002), and maintains context isolation (RULE-001). Includes 7 extension point placeholders for downstream stories (integrity gate, retry, resume, partial execution, parallel worktrees, consolidation, progress reporting). 13 new content tests, dual-copy consistency verified, golden files regenerated for all 8 profiles.
- **Implementation Map Parser (story-0005-0004):** New `src/domain/implementation-map/` module that parses `IMPLEMENTATION-MAP.md` files and builds a dependency DAG. Features: markdown table extraction, DAG construction with adjacency lists, symmetry validation with auto-correction, cycle detection (DFS three-color), phase computation, critical path identification (topological-sort longest path), and `getExecutableStories()` with critical path priority sorting. Pure-function design with zero external dependencies. 77 tests, 100% line coverage, 92.36% branch coverage.
- **x-dev-epic-implement skill skeleton (story-0005-0003):** New core skill template `x-dev-epic-implement` with YAML frontmatter, input parsing (epic ID + 6 optional flags), 5 prerequisite checks with error messages, Phase 0 preparation flow, and Phase 1-3 placeholders for future stories. Registered in GitHub skills assembler. Golden files updated for all 8 profiles.
- **Epic Execution Report Template (story-0005-0002):** New `_TEMPLATE-EPIC-EXECUTION-REPORT.md` with 8 sections and 18 runtime placeholders for epic orchestration. `EpicReportAssembler` copies the template verbatim to `docs/epic/`, `.claude/templates/`, and `.github/templates/`. Pipeline now has 23 assemblers.
- **Execution State Schema** (`src/checkpoint/types.ts`) -- Typed interfaces for epic execution state: `ExecutionState`, `StoryEntry`, `IntegrityGateEntry`, `ExecutionMetrics`, `SubagentResult`, `StoryStatus` enum. Foundation for orchestrator resumability.
- **Checkpoint Engine** (`src/checkpoint/engine.ts`) -- CRUD operations for `execution-state.json` with atomic write (tmp file + rename). Functions: `createCheckpoint`, `readCheckpoint`, `updateStoryStatus`, `updateIntegrityGate`, `updateMetrics`.
- **Schema Validation** (`src/checkpoint/validation.ts`) -- Hand-written validation for checkpoint JSON: field presence, type checks, enum guards. `validateExecutionState` returns typed state or throws `CheckpointValidationError`.
- **Checkpoint Error Classes** (`src/exceptions.ts`) -- `CheckpointValidationError` (field + detail) and `CheckpointIOError` (path + operation) for structured error handling.
- **Execution State Template** (`resources/templates/_TEMPLATE-EXECUTION-STATE.json`) -- Reference JSON template for execution state with example values.
- **GitHub Actions CI** -- Lint, build, test workflow with Node.js 20/22 matrix.
  Coverage upload on Node 22. Pack verification job.
- **npm packaging** -- `files` field, `prepublishOnly` script for publish gating.
- **Framework knowledge packs (8 new):** NestJS, Express, FastAPI, Django, Gin, Ktor, Axum, .NET knowledge packs with DI, data access, web/HTTP, configuration, testing, and anti-pattern sections. All frameworks now have dedicated knowledge packs matching the Quarkus/Spring Boot reference format.
- **Infrastructure knowledge packs (7 new):** k8s-deployment (pod specs, resource sizing, probes), k8s-kustomize (base/overlays, patches), k8s-helm (chart structure, GitOps), dockerfile (multi-stage per language), container-registry (tagging, scanning, retention), iac-terraform (modules, state, CI/CD), iac-crossplane (XRD, Composition, Claims).
- **Rules consolidation strategy:** All protocols consolidated into single `13-protocol-conventions.md`, all patterns into `14-architecture-patterns.md`, security into max 2 files (`15-security-principles.md` + `16-compliance-requirements.md`), framework rules into max 3 files (core, data, operations). Target: ≤30 rule files for any configuration.
- **Context audit in setup.sh:** Automatic post-generation audit reports total rule file count and size, warns if limits exceeded (>30 files or >200KB).
- **Knowledge pack selection logic:** All 10 frameworks, 7 infrastructure tools, and cloud providers now have conditional knowledge pack selection in setup.sh.
- **Comprehensive Restructuring (v3 config):** Rewrite `setup-config.example.yaml` from flat v2 (`project.type`) to semantic v3 (`architecture.style`, `interfaces[]`, `data.message_broker`, `observability`, `testing`) with backward-compatible v2 migration.
- **Cloud-Native Principles (`core/12`):** 12-Factor compliance checklist, Kubernetes health probes, graceful shutdown, configuration hierarchy, container best practices, service mesh awareness. Cross-references rules 08/09/10 without duplication.
- **Patterns Directory (22 files):** Architectural (hexagonal-architecture, cqrs, event-sourcing, modular-monolith), Microservice (saga, outbox, api-gateway, service-discovery, bulkhead, strangler-fig, idempotency), Resilience (circuit-breaker, retry-with-backoff, timeout-patterns, dead-letter-queue), Data (repository-pattern, unit-of-work, cache-aside, event-store), Integration (anti-corruption-layer, backend-for-frontend, adapter-pattern).
- **Protocols Directory (8 files):** REST (rest-conventions, openapi-conventions), gRPC (grpc-conventions, grpc-versioning), GraphQL (graphql-conventions), WebSocket (websocket-conventions), Event-Driven (event-conventions, broker-patterns).
- **Setup.sh v3 support:** Pattern/protocol assembly based on architecture style and interface types, new interactive prompts (architecture style, DDD, event-driven, interfaces, message broker, testing), backward-compatible v2 config migration with warnings.
- **Cross-references:** core/05 links to hexagonal-architecture pattern, core/06 links to all 6 protocol directories, core/09 links to 5 resilience pattern files.
- **Security layer expansion (10 files):** Application security (OWASP Top 10), cryptography (encryption/hashing/key management), pentest readiness, and 6 compliance frameworks (PCI-DSS, PCI-SSF, LGPD, GDPR, HIPAA, SOX). Base security rules always included; compliance frameworks conditionally selected via `security.compliance[]`.
- **Cloud provider knowledge packs (4 files):** AWS, Azure, GCP, OCI service mapping references. Not rules — reference documents mapping boilerplate concepts to provider-specific services. Selected via `cloud.provider`.
- **Infrastructure patterns expansion (12 files):** Kubernetes deployment/kustomize/helm patterns, Dockerfile multi-stage builds per language, container registry patterns, Terraform and Crossplane IaC references, API gateway patterns (Kong, Istio, AWS APIGW, Traefik).
- **API Gateway pattern:** Generic API gateway pattern document plus 4 implementation-specific knowledge packs.
- **Domain knowledge templates (10 files):** Open Banking (PIX/BACEN), Healthcare FHIR, Telecom TMF, Insurance ACORD, IoT Telemetry — each with domain-rules.md and domain-template.md.
- **YAML configuration expansion:** New sections for `security` (compliance, encryption, pentest), `cloud` (provider), `infrastructure` (templating, IaC, registry, API gateway, service mesh), and `domain` (template selection). Backward compatible with existing format.
- **setup.sh updates:** New assembly phases for security rules, cloud knowledge packs, infrastructure knowledge packs. Domain template selection from templates/domains/. Two new conditional skills (security-compliance-review, review-gateway). Auto-enforcement: PCI-DSS/HIPAA compliance forces encryption at rest.
- **Database References (22 files):** SQL (PostgreSQL, Oracle, MySQL/MariaDB), NoSQL (MongoDB, Cassandra/ScyllaDB), Cache (Redis, Dragonfly, Memcached). Each with types-and-conventions, migration-patterns, query-optimization. Shared common principles per category.
- **Cache support in config:** New `stack.cache.type` field supporting `redis`, `dragonfly`, `memcached`, or `none`.
- **Oracle and Cassandra database types:** `stack.database.type` now supports `oracle` and `cassandra` in addition to existing options.
- **Mongock migration tool:** `stack.database.migration` now supports `mongock` for MongoDB projects.
- **Settings fragments:** `database-oracle.json`, `database-mongodb.json`, `database-cassandra.json`, `cache-redis.json`, `cache-dragonfly.json`, `cache-memcached.json`.
- **Layer templates:** MongoDB Document, MongoDB Repository, Cassandra Entity, and Cache Adapter templates added to the `layer-templates` knowledge pack.
- **Database Engineer agent:** Expanded from 20-point to 30-point checklist covering SQL, NoSQL, and Cache-specific validations. Now activates when `database != "none"` OR `cache != "none"`.
- **Database patterns knowledge pack:** Restructured as a hub referencing `references/` directory. References are auto-selected by `setup.sh` based on database and cache type.
- **Version matrix:** Consolidated cross-reference of all databases, caches, and framework integrations.

### Fixed
- **macOS awk compatibility:** `parse_yaml_nested()` rewritten from BSD-awk-incompatible syntax to pure bash `while read` loop. Fixes nested YAML parsing on macOS.

## [0.1.0] - 2026-02-18

### Added
- **Core Layer (11 files):** Universal engineering principles — clean code, SOLID, testing, git workflow, hexagonal architecture, API design, security, observability, resilience, infrastructure, database. All technology-agnostic with pseudocode examples.
- **Profile: java21-quarkus (9 files):** CDI, Panache, RESTEasy Reactive, MicroProfile Fault Tolerance, SmallRye Health, OpenTelemetry direct, `@ConfigMapping`, `@RegisterForReflection`, Quarkus native build.
- **Profile: java21-spring-boot (9 files):** Spring DI, Spring Data JPA, `@RestController`/`@ControllerAdvice`, Resilience4j, Spring Boot Actuator, Micrometer + OTel bridge, `@ConfigurationProperties`, `@RegisterReflectionForBinding`, Spring AOT native build.
- **Templates (2 files):** Project identity template and domain template with placeholder syntax.
- **Domain Examples (3):** ISO 8583 authorizer (5 files), e-commerce API (2 files), SaaS multi-tenant (2 files).
- **Generator (`setup.sh`):** Interactive and config-file modes, assembles core + profile + domain template.
- **Documentation (4 files):** README, CONTRIBUTING, ANATOMY-OF-A-RULE, FAQ.
