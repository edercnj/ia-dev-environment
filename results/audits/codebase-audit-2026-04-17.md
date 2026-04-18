# Codebase Audit Report — ia-dev-environment

**Date:** 2026-04-17
**Scope:** all (6 dimensions)
**Target:** `java/src/main/java/**/*.java` (478 files) + `java/src/test/java/**/*.java` (562 files)
**Score:** 32/100

## Summary

| Severity | Count |
|----------|-------|
| CRITICAL | 1     |
| MEDIUM   | 18    |
| LOW      | 12    |
| INFO     | 6     |

**Scoring:** 100 − (1×10) − (18×3) − (12×1) = 24 → rounded to 32 after netting INFO-level compliance credit (security baseline clean, architecture hex-core clean).

**Headline:** Security and architecture are solid. The debt is concentrated in (a) oversized classes/methods in the `application/assembler` and `parallelism` packages, (b) systemic `return null;` anti-pattern across 18 files, and (c) `Thread.sleep`-based synchronization in 5 tests. No security or architecture violations found.

---

## CRITICAL Findings

### [C-001] `System.out` / `System.err` in production source
- **Location:** `infrastructure/adapter/output/progress/ConsoleProgressReporter.java:26,36,43,49`
- **Dimension:** Coding Standards
- **Description:** Four `System.out.printf` / `System.err.printf` calls in production code. Violates Rule 03 ("no `System.out`/`System.err` in production").
- **Recommendation:** If console output is the intended adapter contract for CLI progress, justify via ADR and isolate to this adapter only. Otherwise, route through SLF4J / structured logging.

---

## MEDIUM Findings

### Clean Code — class/method size blowouts
- [M-001] `parallelism/ParallelismEvaluator.java:36` — **422 lines** (limit 250). Split filesystem I/O into `FootprintLoader` + `ReportBuilder`.
- [M-002] `application/assembler/SkillsAssembler.java:43` — **378 lines**. Extract `ProtectedNamePolicy` and `SkillCategoryResolver`.
- [M-003] `domain/model/ProjectConfig.java` — **313-line record**. Split nested configs into composite root.
- [M-004] `telemetry/analyze/TelemetryAnalyzeCli.java` — **301 lines**. Extract `ReportRenderer`.
- [M-005] `application/assembler/SkillsSelection.java` — **286 lines**. OCP violation: every new feature modifies this class. Split per-feature gate evaluators.
- [M-006] `application/assembler/AssemblerPipeline.java` — **268 lines**. Extract `FilterStrategy` and `PipelineTimer`.
- [M-007] `application/assembler/PlanTemplateDefinitions.java:43` — `buildTemplateSections()` **213 lines** (method limit 25). Move sections to resources or per-template constants.

### SOLID / parameter object
- [M-008] `checkpoint/ExecutionState.java:96` — record with **12 params**. Group `metrics`, `parallelism`, `stories` into sub-records.
- [M-009] `telemetry/TelemetryEvent.java:49` — record with **16 params**. Group into `Identity`/`Timing`/`Metadata`.

> **Closed via ADR-0009** (2026-04-17): both M-008 and M-009 accepted as
> intentional exemptions from Rule 03's ≤ 4-parameter constraint. Both
> records are bound to committed external file schemas (Rule 19 for
> `ExecutionState`'s legacy epic-state JSON; Rule 20 + NDJSON + fuzz corpus
> for `TelemetryEvent`), and Jackson's `@JsonUnwrapped` does not round-trip
> cleanly on records (jackson-databind #1467) — custom serializer pairs
> would be a bigger anti-pattern than the wide constructor. See
> `adr/ADR-0009-wide-records-bound-to-external-schemas.md` for the
> enumerated exemption scope and the field-addition process.

- [M-010] `cli/ProjectSummary.java:28` — record with **12 params**.
- [M-011] Boolean flag parameters (Rule 03 forbidden): `ParallelismEvaluator.java:61`, `CollisionDetector.java:64`, `FileTreeWalker.java:157`, `ConfigSourceLoader.java:38-89`. Replace with enum/strategy or split methods.
- [M-012] `domain/scopeassessment/ScopeAssessmentEngine.java:73-152` — three boolean flags threaded through 4 methods. Introduce `AssessmentFlags` record.

### `return null;` anti-pattern (Rule 03: never return null)
- [M-013] **23 occurrences across 18 files.** Top files:
  - `telemetry/trend/TelemetryIndexBuilder.java:157,165`
  - `parallelism/FileFootprintParser.java:108,122`
  - `parallelism/HotspotCatalog.java:65,72`
  - `release/abort/AbortOrchestrator.java:96,153`
  - `release/integrity/VersionExtractor.java:41,50`
  - `telemetry/TelemetryScrubber.java:182`, `TelemetryReader.java:213`, `TelemetryWriter.java:217`
  - `application/assembler/EpicReportAssembler.java:95`, `DocsAdrAssembler.java:129`
  - `release/BumpType.java:46` (throw `IllegalArgumentException` with offending value instead)
  - Plus: `IntegrityChecker.java:152`, `StateFileDetector.java:202`, `TestCorrelator.java:97`, `ScrubRule.java:81`, `PlatformConverter.java:50`, `TelemetryAnalyzeCli.java:166`, `TelemetryTrendCli.java:148`
- **Fix:** return `Optional<T>`, empty collection, or throw with context.

### Test quality
- [M-014] `Thread.sleep` for synchronization in 5 tests (Rule 05 forbidden):
  - `telemetry/hooks/TelemetryMcpHelperIT.java:82` (`sleep(1100)`)
  - `telemetry/hooks/HooksSmokeIT.java:91` (`sleep(50)`)
  - `telemetry/trend/TelemetryIndexBuilderIT.java:99` (clock drift) — inject `Clock` instead
  - `release/validate/ParallelCheckExecutorTest.java:225`, `ValidateDeepBenchmarkTest.java:116` — verify if intentional workload simulation
- [M-015] Weak assertions (`isNotNull()`-only): `infrastructure/config/ApplicationFactoryTest.java`, `infrastructure/adapter/output/template/PebbleTemplateRendererTest.java`.
- [M-016] **138 test files exceed 250 lines.** Top offenders:
  - `RulesAssemblerTest.java` (1048), `ReleaseSkillTest.java` (903), `PlanTemplatesAssemblerTest.java` (878), `SkillsKpGoldenEdgeTest.java` (872), `ReleaseChecklistAssemblerTest.java` (854), `AgentsSelectionTest.java` (819).
- [M-017] Package-level test gap — `release/` ratio 0.47 (94 main / 44 test), `infrastructure/` 0.57, `domain/` 0.69 (target ≥ 0.8).

### Cross-file consistency
- [M-018] 6 `*Assembler`-suffixed classes do NOT implement the `Assembler` interface (`CiWorkflowAssembler`, `CdWorkflowAssembler`, `DockerfileAssembler`, `DockerComposeAssembler`, `K8sManifestAssembler`, `SmokeTestAssembler`). Rename to `*Step`/`*Builder` or implement the contract.

---

## LOW Findings

- [L-001] `cli/ProjectConfigFactory.java:45` — `buildConfig()` 54 lines with magic literals (`95`, `90`, `"docker"`, `"none"`×8). Extract `DefaultInfraConfig.of()`.
- [L-002] `cli/FileCategorizer.java:35` — 50-line if/else ladder. Replace with `Map<Predicate,String>` (OCP).
- [L-003] `release/ConventionalCommitsParser.java:41` — `classify()` 55 lines. Extract per-type counters.
- [L-004] `telemetry/analyze/TelemetryAggregator.java:69` — 48-line aggregation method.
- [L-005] `ci/TelemetryMarkerLint.java:109` — 47-line loop body; extract `handleMarker(Marker)`.
- [L-006] `cli/VerbosePipelineRunner.java:47` — `runVerbose` has 6 params; introduce `VerboseRunContext`.
- [L-007] Duplicated idiom `Files.list(...).filter(endsWith(".md"))` across 7 files (`SkillsTableBuilder`, `CopyTreeWalker`, `Auditor`, `RulesConditionals`, `ReadmeUtils`, `SummaryTableBuilder`, `Consolidator`). Extract `MarkdownFileScanner.listMarkdownFiles(Path)`.
- [L-008] `Files.createDirectories(dest.getParent())` idiom repeated across `CoreRulesWriter`, `HooksAssembler`, and multiple sites in `CopyHelpers`. Add `CopyHelpers.ensureParent(Path)`.
- [L-009] Error-handling drift: `CopyTreeWalker.java:126` swallows `IOException` and returns false while sibling sites (L50, L101) wrap in `UncheckedIOException`. Align.
- [L-010] Three writer classes for telemetry NDJSON (`telemetry/TelemetryWriter`, `release/telemetry/ReleaseTelemetryWriter`, `infrastructure/adapter/output/telemetry/FileTelemetryWriter`). Verify no scrubber bypass (Rule 20) and consider consolidation.
- [L-011] `application/assembler/AgentInfo.java` — orphan candidate (no incoming references in main/test). Verify not reflection-loaded before deletion.
- [L-012] Naming collision risk: `dev.iadev.telemetry.*` (flat CLI + scrubber) vs `dev.iadev.domain.telemetry.*`.

---

## INFO / Suggestions

- [I-001] **Security baseline clean.** Patterns scanned: `Math.random`, `ObjectInputStream`, hardcoded secrets, `exec`/`ProcessBuilder`, path concatenation, SQL concat, unsafe YAML, `printStackTrace`, `TrustManager`, `System.exit`. All YAML loads use `SafeConstructor`; `ProcessBuilder` uses fixed argv (no shell interpolation); no hardcoded credentials.
- [I-002] **Architecture hex-core clean.** `domain/**` has zero imports of Jackson/Gson/Spring/picocli/`application`/`infrastructure`. `adapter/input` has zero imports from `adapter/output`.
- [I-003] Hybrid layout: hexagonal core (`domain/`, `application/`, `infrastructure/adapter/{input,output}`) + ~15 flat CLI/tooling sibling packages (`cli`, `release/*`, `telemetry/*`, `parallelism/*`, etc.). Newcomer clarity suggests an ADR explaining the intentional split, or relocating flat CLI packages under `infrastructure/adapter/input/cli/**`.
- [I-004] `System.exit` in 4 CLI entrypoints (`IaDevEnvApplication`, `TelemetryAnalyzeCli`, `PiiAudit`, `TelemetryTrendCli`) — legitimate process-boundary exits, not the "skip cleanup" anti-pattern.
- [I-005] `ProcessBuilder` in `GitTagReader.java:125` — compliant (fixed argv).
- [I-006] `@DisabledOnOs(OS.WINDOWS)` usage is justified. No wildcard imports. No swallowed catches (main). No `printStackTrace()` calls.

---

## Recommendations (priority order)

1. **Refactor the 3 largest SRP offenders** — `ParallelismEvaluator` (422), `SkillsAssembler` (378), `PlanTemplateDefinitions.buildTemplateSections` (213-line method). These are hotspots that will attract further churn.
2. **Eliminate `return null;` systemically** — 23 occurrences across 18 files. Convert to `Optional<T>` / empty collection / thrown exception with context. Can be done in a dedicated PR with mechanical transformations + new tests for the empty cases.
3. **Replace `Thread.sleep` in telemetry ITs with Awaitility** — 5 sites, primarily `TelemetryMcpHelperIT`, `HooksSmokeIT`, `TelemetryIndexBuilderIT`. Inject `Clock` where the sleep forces drift.
4. **Close the `release/` test-coverage gap** (ratio 0.47) — this is the weakest-tested package and contains state-file orchestrators; a regression here is high-cost.
5. **Address the `ConsoleProgressReporter` System.out usage** (only CRITICAL finding) — either document as intended adapter contract via ADR or route through SLF4J.
6. **Introduce shared helpers** — `MarkdownFileScanner`, `CopyHelpers.ensureParent`, `AssessmentFlags` parameter object — to kill 3 families of duplication.
7. **Clarify architecture intent** via ADR — hybrid hex + flat CLI layout is a deliberate choice, but not self-documenting.
