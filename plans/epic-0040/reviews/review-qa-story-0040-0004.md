# QA Specialist Review — story-0040-0004

**ENGINEER:** QA
**STORY:** story-0040-0004
**PR:** #414
**SCORE:** 34/36
**STATUS:** Partial

---

## Context

Story adds a configurable telemetry flag on `ProjectConfig`, threads it through `HooksAssembler` (file copy) and `SettingsAssembler`/`HookConfigBuilder`/`JsonSettingsBuilder` (JSON emission). Covered by 18 new test scenarios across 5 test files plus 1 new smoke test class.

## PASSED

- **[QA-01] Test-first discipline** (2/2) — Every change followed RED→GREEN: `ProjectConfigTest#TelemetryEnabledField` (5), `HooksAssemblerTest#TelemetryHooks` (5), `SettingsAssemblerTelemetryTest` (7), `HookConfigBuilderTest#TelemetryVariants` (3), `TelemetrySettingsSmokeTest` (2).
- **[QA-02] Scenario categories present** (2/2) — Degenerate (`telemetryEnabled=false`), happy (enabled), error (missing source aborts), boundary (idempotency), coexistence (Java-Maven with both hooks). Matches story §7.2 mandatory categories.
- **[QA-03] Test naming convention** (2/2) — `methodUnderTest_scenario_expectedBehavior` observed throughout (e.g., `fromMap_noTelemetry_defaultsToTrue`, `assemble_telemetryEnabled_copiesAllScripts`, `compiledLang_postToolUseHasTwoEntries`).
- **[QA-04] Acceptance criteria coverage** (2/2) — All 6 Gherkin scenarios from story §7 are exercised by tests: `Telemetria desativada` → `Disabled.pythonDisabled_noHooksSection`; `5 hooks (happy)` → `Enabled.pythonEnabled_fiveTelemetryEvents`; `coexistência` → `Coexistence.compiledLang_postToolUseHasTwoEntries`; `Golden test` → regenerated golden parity; `missing file` → `assemble_missingTelemetryFile_throws`; `idempotency` → `Idempotency.assemble_twice_identicalOutput`.
- **[QA-05] Coverage thresholds met** (2/2) — `mvn verify` passes JaCoCo hard gate: 95% line, 90% branch. HookConfigBuilder: 99%/100%; ProjectConfig: 99%/90%.
- **[QA-06] No weak assertions** (2/2) — Tests assert specific strings, sizes, file existence, executable bits, and JSON-brace balance. No naked `isNotNull()`.
- **[QA-07] Fixtures & shared helpers** (2/2) — Uses `TestConfigBuilder.telemetryEnabled(boolean)` builder method rather than duplicating ProjectConfig construction. `SettingsAssemblerTelemetryTest` extracts `assembleSettings(config, tempDir)` helper.
- **[QA-08] Cross-file consistency** (2/2) — Legacy tests that were previously implicitly relying on "no hooks for Python" were updated consistently with `.telemetryEnabled(false)` across `HooksAssemblerTest`, `SettingsHooksAndJsonTest`, `AssemblerMiscCoverageTest`.
- **[QA-09] Smoke test exercises the real pipeline** (2/2) — `TelemetrySettingsSmokeTest` runs the same assembler classes invoked by `mvn process-resources`, covering java-spring (compiled) and python-fastapi (non-compiled) paths.
- **[QA-10] Golden parity** (2/2) — All 17 profiles regenerated via `GoldenFileRegenerator`; `mvn test` shows byte-for-byte parity (0 golden failures).
- **[QA-11] Manifest updated** (2/2) — `expected-artifacts.json` regenerated via `ExpectedArtifactsGenerator` (not manually edited) — integration smoke now green.
- **[QA-12] Refactoring discipline** (2/2) — `JsonSettingsBuilder.build(permissions, hookPresence)` legacy 2-arg overload preserved via delegation (no behavior change for callers); `HookConfigBuilder` extracted `appendPostCompileEntry` / `appendTelemetryPostToolEntry` / `appendTelemetryEvent` helpers.

## PARTIAL

- **[QA-13] Negative / edge-case breadth** (1/2) — Missing-file error path is covered, but the `chmod 0755 fails` scenario (story §5.3 — "Log warning + continua") is not directly tested. Current behavior: `HooksAssembler.makeExecutable` swallows `UnsupportedOperationException` (Windows path) but rethrows `IOException` as `UncheckedIOException`, contradicting the story's "log warning + continua" intent. **File:** `HooksAssembler.java:makeExecutable`. **Fix:** downgrade `IOException` in `makeExecutable` to a warning log and continue, consistent with the story contract; add a test.

## FAILED

- None.

## Severity Distribution

- CRITICAL: 0
- HIGH: 0
- MEDIUM: 1 (QA-13 chmod fail-open intent vs. current rethrow)
- LOW: 0

## Notes

The MEDIUM finding was already the behavior **before** this story (the `makeExecutable` helper is pre-existing), so it is an inherited divergence rather than a regression. It's flagged here because the story documentation (§5.3 table) claims a different behavior than the code actually implements. Either code or docs need alignment; neither blocks merge.
