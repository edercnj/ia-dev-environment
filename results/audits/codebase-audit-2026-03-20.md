# Codebase Audit Report — ia-dev-environment (Java)

**Date:** 2026-03-20
**Scope:** All (Clean Code, SOLID, Architecture, Coding Standards, Tests, Security, Cross-File Consistency)
**Files Audited:** 291 Java files (127 production + 146 test + 18 test helpers/resources)
**Score:** 32/100

---

## Summary

| Severity | Count |
|----------|-------|
| CRITICAL | 7 |
| MEDIUM   | 24 |
| LOW      | 18 |
| INFO     | 8 |

---

## CRITICAL Findings

### [C-001] 24 classes exceed the 250-line hard limit (CC-03 / SRP)
- **Dimension:** Clean Code & SOLID
- **Description:** 24 out of ~100 production classes exceed the 250-line class limit. The top offenders are nearly double the limit: `CicdAssembler.java` (446), `GithubInstructionsAssembler.java` (443), `RulesAssembler.java` (436), `SettingsAssembler.java` (435), `ReadmeTables.java` (426).
- **Recommendation:** Split by responsibility. CicdAssembler's five artifact types (CI workflow, Dockerfile, Docker Compose, K8s, smoke test) should each be a separate class or strategy.

### [C-002] 17 `return null` occurrences across 8 files
- **Dimension:** Coding Standards / Clean Code
- **Locations:** `SkillsAssembler.java` (4), `GithubAgentsAssembler.java` (2), `GenerateCommand.java` (2), `CodexAgentsMdAssembler.java` (2), `GithubSkillsAssembler.java` (1), `CicdAssembler.java` (1), `CopyHelpers.java` (1), `ResourceDiscovery.java` (3), `MarkdownParser.java` (1)
- **Description:** Convention mandates "NEVER return null -- use `Optional<T>` or empty collection." These are production code paths returning null instead of `Optional`.
- **Recommendation:** Replace all with `Optional<T>` return types. Start with `CopyHelpers.copyTemplateFileIfExists()` and `GithubSkillsAssembler.renderSkill()` which are called broadly.

### [C-003] 50+ methods exceed the 25-line hard limit (CC-02)
- **Dimension:** Clean Code
- **Description:** Worst offenders: `AssemblerPipeline.buildAssemblers()` (~112 lines), `ContextBuilder.buildContext()` (~74 lines), `Consolidator.consolidateFrameworkRules()` (~64 lines), `ResourceResolver.extractJarResources()` (~64 lines), `CicdAssembler.buildStackContext()` (~55 lines), `ReadmeTables.buildSummaryRows()` (~54 lines).
- **Recommendation:** Decompose into section-specific builders. `ContextBuilder.buildContext` should be split into identity, language, framework, infrastructure, testing, and interface builders.

### [C-004] 3 `System.err.println` usages in production code
- **Dimension:** Coding Standards
- **Locations:** `GithubMcpAssembler.java:74`, `CodexConfigAssembler.java:61`, `GithubAgentsAssembler.java:102`
- **Description:** Convention forbids `System.out`/`System.err` -- use SLF4J or propagate warnings through the pipeline result.
- **Recommendation:** Remove `System.err.println` calls; propagate warnings via return types or pipeline warning aggregation.

### [C-005] Jackson framework dependency in checkpoint domain package
- **Dimension:** Architecture
- **Location:** `CheckpointEngine.java:3-5`
- **Description:** Checkpoint package contains domain models (`ExecutionState`, `StoryEntry`) and business logic, yet `CheckpointEngine` directly depends on `com.fasterxml.jackson.databind.ObjectMapper`. Domain-level code must have zero external framework dependencies.
- **Recommendation:** Extract serialization to an adapter class (e.g., `CheckpointSerializer`) behind a port interface.

### [C-006] Duplicated `buildContext()` with inconsistent boolean conversion
- **Dimension:** Cross-File Consistency
- **Locations:** `RulesAssembler.java:123`, `AgentsAssembler.java:284`, `PatternsAssembler.java:232`, `ContextBuilder.java` (canonical)
- **Description:** Three local `buildContext()` variants exist alongside the canonical `ContextBuilder.buildContext()`. The local versions use `String.valueOf()` (produces "true"/"false") while `ContextBuilder` uses `toPythonBool()` (produces "True"/"False"). Templates receive different boolean strings depending on which assembler renders them.
- **Recommendation:** Remove local `buildContext()` methods; all assemblers should use `ContextBuilder.buildContext()`.

### [C-007] Massive copy-paste of utility methods across assemblers
- **Dimension:** Cross-File Consistency (DRY violation)
- **Description:** `writeFile(Path, String)` is identically copy-pasted across **14 assembler files**. `readFile(Path)` is copy-pasted across **6 files**. `listMdFilesSorted(Path)` across **3 files**. `indent()`/`escapeJson()` across **2 files**. `deleteQuietly(Path)` across **2 files**.
- **Recommendation:** Extract to `CopyHelpers` (add `writeFile`, `readFile`, `listMdFilesSorted`) and create a `JsonHelpers` utility for `indent`/`escapeJson`.

---

## MEDIUM Findings

### [M-001] 8 functions with > 4 parameters (CC-02)
- **Dimension:** Clean Code
- **Locations:** `InteractivePrompter.displaySummary` (9 params), `InteractivePrompter.buildConfig` (9 params), `GithubSkillsAssembler.renderSkill` (6 params), `GithubSkillsAssembler.generateGroup` (6 params), `GithubSkillsAssembler.copyReferences` (5 params), `RulesConditionals.copyDatabaseRefs` (5 params), `GenerateCommand.runVerbosePipeline` (5 params)
- **Recommendation:** Use parameter objects (records) for methods with > 4 params.

### [M-002] 5 boolean flag parameters (FORBIDDEN)
- **Dimension:** Clean Code / Coding Standards
- **Locations:** `SettingsAssembler.buildSettingsJson(boolean hasHooks)`, `CodexShared.deriveApprovalPolicy(boolean hasHooks)`, `CliDisplay.formatResult(boolean dryRun)`, `TerminalProvider.confirm(boolean defaultValue)`, `DagNode.setOnCriticalPath(boolean)`
- **Recommendation:** Replace with enum types or create two distinct methods.

### [M-003] 12+ string concatenation with `+` in error messages
- **Dimension:** Coding Standards
- **Locations:** `StackValidator.java` (5 occurrences), `ConfigLoader.java:137`, `Auditor.java` (2), `CicdAssembler.java:266`, `GithubAgentsAssembler.java:251`
- **Description:** Convention requires `String.formatted()` or `String.format()`, never `+` concatenation in error/exception messages.
- **Recommendation:** Replace with `.formatted()` calls.

### [M-004] 100+ string concatenations in content building
- **Dimension:** Coding Standards
- **Most affected:** `ReadmeTables.java` (40+), `ProgressFormatter.java` (10+), `DocsAdrAssembler.java` (15+), `CicdAssembler.java` (15+)
- **Recommendation:** Use `String.formatted()`, `StringBuilder`, or text blocks for multi-line content.

### [M-005] Magic numbers/strings (CC-04)
- **Dimension:** Clean Code
- **Locations:** `ContextBuilder.java:76` (`new LinkedHashMap<>(32)`), `CicdAssembler.java:188` (`new LinkedHashMap<>(16)`), `DagValidator.java:24-26` (color constants `0,1,2`), `CheckpointEngine.java:159` (`60_000.0`), `GenerateCommand.java:330`
- **Recommendation:** Use named constants. `DagValidator` should use an enum for colors.

### [M-006] Train wreck accessor chains (CC-09)
- **Dimension:** Clean Code
- **Locations:** `RulesIdentity.java:129-165`, `ContextBuilder.java:104-117`, `CodexAgentsMdAssembler.java:281-282`
- **Description:** 3-level deep chains like `config.infrastructure().observability().tool()` across different object boundaries.
- **Recommendation:** Add convenience accessors on `ProjectConfig` or intermediate objects.

### [M-007] Architecture: No port/application/adapter package hierarchy
- **Dimension:** Architecture
- **Description:** Project declares hexagonal architecture but uses a flat package layout. No `port/`, `application/`, or `adapter/` packages exist. `cli/` serves as inbound adapter, `config/` and `template/` serve as outbound adapters, but not organized under the expected hierarchy.
- **Recommendation:** This is a pragmatic deviation for a CLI tool. Document it as an ADR if intentional, or restructure if strict hexagonal compliance is desired.

### [M-008] Domain package imports from sibling `model/` package
- **Dimension:** Architecture
- **Locations:** `CoreKpRouting.java`, `PatternMapping.java`, `ProtocolMapping.java`, `SkillRegistry.java`, `StackResolver.java`, `StackValidator.java`
- **Description:** `domain.stack` imports from `dev.iadev.model` which is a sibling package rather than nested under `domain/model/`. The model records are pure (no framework imports), so this is organizational, not a dependency direction violation.
- **Recommendation:** Move `model/` under `domain/model/` or document the flat layout as intentional.

### [M-009] Filesystem I/O in domain class
- **Dimension:** Architecture
- **Location:** `VersionResolver.java:3-4`
- **Description:** Domain class uses `java.nio.file.Files.isDirectory()` for filesystem I/O. Should be behind a port interface.
- **Recommendation:** Inject a `VersionProvider` port or pass resolved paths to the domain.

### [M-010] CLI directly imports assembler (no use-case intermediary)
- **Dimension:** Architecture
- **Location:** `GenerateCommand.java:3-5`
- **Description:** Inbound adapter directly orchestrates assemblers without an application service layer.
- **Recommendation:** Extract a `GenerateUseCase` class if strict hexagonal compliance is desired.

### [M-011] YAML deserialization without explicit SafeConstructor
- **Dimension:** Security
- **Locations:** `ConfigLoader.java:124`, `CodexAgentsMdAssembler.java:235`, `ConfigProfiles.java:123`
- **Description:** `new Yaml().load()` is used. SnakeYAML 2.3 defaults to SafeConstructor (safe), but best practice is to use `new Yaml(new SafeConstructor(new LoaderOptions()))` explicitly.
- **Recommendation:** Make SafeConstructor usage explicit to guard against future version changes.

### [M-012] Incomplete JSON escaping
- **Dimension:** Security
- **Locations:** `GithubMcpAssembler.java:266-268`, `SettingsAssembler.java:425-428`
- **Description:** `escapeJson()` only escapes backslashes and double quotes, not newlines (`\n`), carriage returns (`\r`), tabs (`\t`), or control characters. Could produce malformed JSON.
- **Recommendation:** Implement full JSON string escaping per RFC 8259, or use Jackson's `ObjectMapper` for JSON generation.

### [M-013] Temporary directory without explicit permissions
- **Dimension:** Security
- **Locations:** `ResourceResolver.java:143`, `AtomicOutput.java:78`
- **Description:** `Files.createTempDirectory()` without explicit `PosixFilePermissions` (700). Low practical risk for CLI tool.
- **Recommendation:** Add `PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))`.

### [M-014] ~441 test methods (~24%) use non-standard naming
- **Dimension:** Test Quality
- **Description:** Convention requires `[methodUnderTest]_[scenario]_[expectedBehavior]`. ~441 of 1,814 test methods use plain camelCase instead. Most are in the `assembler` package tests.
- **Recommendation:** Rename to follow the underscore convention. The `@DisplayName` annotations provide readability but method names should also conform.

### [M-015] 27 weak `isNotNull()`-only assertions
- **Dimension:** Test Quality
- **Description:** 27 test methods assert only `isNotNull()` without verifying specific values, sizes, or content. Concentrated in `*CoverageTest.java` files.
- **Recommendation:** Strengthen assertions to verify specific behavior, not just non-nullity.

### [M-016] 63 test files exceed 250-line limit
- **Dimension:** Test Quality
- **Description:** Worst: `GithubInstructionsAssemblerTest.java` (819), `SettingsAssemblerTest.java` (807), `AgentsAssemblerTest.java` (752). These use `@Nested` inner classes which mitigates readability concerns.
- **Recommendation:** Split the largest test files by concern or extract shared setup into test fixtures.

### [M-017] Inconsistent error handling patterns
- **Dimension:** Cross-File Consistency
- **Locations:** `GithubAgentsAssembler.java:102`, `GithubMcpAssembler.java:74`, `CodexConfigAssembler.java:61`
- **Description:** Three assemblers write warnings to `System.err` instead of propagating through the pipeline's warning aggregation mechanism. Other assemblers allocate warning lists but never propagate them.
- **Recommendation:** Define a consistent warning propagation mechanism in the `Assembler` interface or `NormalizedResult`.

### [M-018] Duplicate `AssembleResult` record types
- **Dimension:** Cross-File Consistency
- **Locations:** `GithubAgentsAssembler.java:380`, `GithubMcpAssembler.java:289`
- **Description:** Two identical `AssembleResult` record types (both `List<String> files, List<String> warnings`), also structurally identical to `AssemblerPipeline.NormalizedResult`.
- **Recommendation:** Extract a shared `AssemblerResult` record.

### [M-019] `GithubMcpAssembler` lacks testable constructor pattern
- **Dimension:** Cross-File Consistency
- **Location:** `GithubMcpAssembler.java:49`
- **Description:** Only assembler without the two-constructor pattern (default + explicit `resourcesDir`), making it untestable with custom resource directories.
- **Recommendation:** Add the standard constructor pattern.

### [M-020] `CicdAssembler.renderAndWrite()` inverted null semantics
- **Dimension:** Cross-File Consistency
- **Location:** `CicdAssembler.java:393`
- **Description:** Returns `null` on success and an error string on failure. Inconsistent with the rest of the codebase where null means "not found."
- **Recommendation:** Use `Optional<String>` for the error or a `Result` type.

### [M-021] `hasAllMandatorySections()` duplicated with divergent styles
- **Dimension:** Cross-File Consistency
- **Locations:** `DocsAdrAssembler.java:180`, `EpicReportAssembler.java:124`
- **Description:** Same logic implemented with different styles (stream vs for-loop).
- **Recommendation:** Extract to a shared utility method.

### [M-022] Fully qualified class names instead of imports
- **Dimension:** Clean Code
- **Location:** `RulesConditionals.java` (9+ occurrences)
- **Description:** Uses `java.nio.file.Files` and `java.io.IOException`/`java.io.UncheckedIOException` with fully qualified names throughout instead of imports.
- **Recommendation:** Add proper imports at the top of the file.

### [M-023] `GoldenFileDiffReporter` in production source tree
- **Dimension:** Cross-File Consistency
- **Location:** `java/src/main/java/dev/iadev/golden/GoldenFileDiffReporter.java`
- **Description:** Only referenced by test classes. Belongs in `src/test/java`.
- **Recommendation:** Move to test source tree.

### [M-024] `AtomicOutput` potentially dead code in production path
- **Dimension:** Cross-File Consistency
- **Location:** `java/src/main/java/dev/iadev/util/AtomicOutput.java`
- **Description:** Not called from any production code. Only referenced in tests and an `@see` tag.
- **Recommendation:** Verify if still needed; move to test or remove if unused.

---

## LOW Findings

### [L-001] Path traversal check uses equality only
- **Dimension:** Security
- **Location:** `PathUtils.java:64-101`
- **Description:** `rejectDangerousPath()` checks if path EQUALS dangerous paths but not children. Low risk for CLI tool.

### [L-002] No symlink protection in file operations
- **Dimension:** Security
- **Location:** `AtomicOutput.java`
- **Description:** File operations follow symlinks by default. No `NOFOLLOW_LINKS` option used.

### [L-003] Error message leakage in CLI
- **Dimension:** Security
- **Locations:** `GenerateCommand.java:164-175`, `ValidateCommand.java:92,97`
- **Description:** Raw exception messages printed to output. Acceptable for CLI tool.

### [L-004] Filename sanitization incomplete
- **Dimension:** Security
- **Location:** `Consolidator.java:178-184`
- **Description:** `sanitizeFilenameSegment()` strips `/`, `\`, `..` but `....` becomes `..` after one pass.

### [L-005] Architecture: `model/` and `exception/` as siblings of `domain/`
- **Dimension:** Architecture
- **Description:** Organizational deviation from expected hexagonal structure. Both packages are clean of framework imports.

### [L-006] `progress/` tightly coupled to `checkpoint/`
- **Dimension:** Architecture
- **Description:** Peer packages without defined layering. Could be merged or relationship formalized.

### [L-007] Mutable List fields in `CicdAssembler.GenerationContext` record
- **Dimension:** Clean Code
- **Location:** `CicdAssembler.java:437`
- **Description:** Record with mutable `List<String>` fields used as accumulators. Records should be immutable data carriers.

### [L-008] Dead code in `CodexAgentsMdAssembler`
- **Dimension:** Clean Code
- **Location:** `CodexAgentsMdAssembler.java:91-93`
- **Description:** Creates `ArrayList` from warnings, immediately clears it, creates new list.

### [L-009] Large static initializer blocks in `GithubSkillsAssembler`
- **Dimension:** Clean Code
- **Location:** `GithubSkillsAssembler.java:77-148`
- **Description:** 8 skill groups with 40+ entries as static data. Could be a separate registry.

### [L-010] Concrete dependency on Jackson `ObjectMapper`
- **Dimension:** Clean Code (DIP)
- **Location:** `CheckpointEngine.java:4-5`
- **Description:** Overlaps with C-005. Direct dependency on concrete Jackson class.

### [L-011] Concrete dependency on SnakeYAML `Yaml`
- **Dimension:** Clean Code (DIP)
- **Location:** `CodexAgentsMdAssembler.java:7`
- **Description:** Direct dependency on concrete SnakeYAML class.

### [L-012] Exception classes scattered across multiple packages
- **Dimension:** Cross-File Consistency
- **Description:** `exception/` package, `domain/implementationmap/`, `cli/`, and `util/` all contain exception classes with no consistent placement strategy.

### [L-013] `ResourceNotFoundException` in `util/` instead of `exception/`
- **Dimension:** Cross-File Consistency
- **Description:** Inconsistent with the project's exception package convention.

### [L-014] `GenerationCancelledException` in `cli/` instead of `exception/`
- **Dimension:** Cross-File Consistency
- **Description:** Other CLI exceptions (`CliException`) are in `exception/` package.

### [L-015] Unused 3-parameter `renderAgent()` overload
- **Dimension:** Cross-File Consistency
- **Location:** `GithubAgentsAssembler.java:300`
- **Description:** Only used by test code; not called from production.

### [L-016] No JaCoCo report in `target/`
- **Dimension:** Test Quality
- **Description:** Tests likely not run with `mvn verify` recently. Configuration is correct but actual coverage unverified.

### [L-017] `DagValidator` uses int constants for graph colors
- **Dimension:** Clean Code (CC-04)
- **Location:** `DagValidator.java:24-26`
- **Description:** `WHITE=0, GRAY=1, BLACK=2` should be an enum for type safety.

### [L-018] `StackMapping.java` (258 lines) contains 12+ static maps
- **Dimension:** Clean Code
- **Description:** Legitimate data-heavy value object. Slight overage (3%) is acceptable for a constant registry.

---

## INFO / Suggestions

### [I-001] Strong security posture
No command injection, hardcoded credentials, insecure random, SQL/DB operations, XML parsing, unsafe deserialization, reflection abuse, or network I/O found. Pure filesystem tool with good path normalization.

### [I-002] All dependencies are current
picocli 4.7.6, pebble 3.2.2, snakeyaml 2.3, jackson 2.18.2, jline 3.28.0, slf4j 2.0.16, logback 1.5.15, JUnit 5.11.4, Mockito 5.14.2, JaCoCo 0.8.12.

### [I-003] Zero wildcard imports
Import discipline is excellent across the entire codebase.

### [I-004] Formatting compliance is perfect
4-space indentation, K&R braces, no lines > 120 characters, organized imports.

### [I-005] Exception design is correct
All 12 custom exceptions extend `RuntimeException` (unchecked), carry context fields, and use `.formatted()` for messages.

### [I-006] Test mocking discipline is correct
Only external library (`org.jline.reader.LineReader`) is mocked. No domain mocking. `MockTerminalProvider` is a proper test double implementing the `TerminalProvider` interface.

### [I-007] No `Thread.sleep()` in test code, no test order dependencies
Test suite is independent and clean of timing-based synchronization.

### [I-008] Good use of parameterized tests
40 parameterized test annotations across 12 files using `@CsvSource`, `@ValueSource`, `@MethodSource`, `@EnumSource`.

---

## Score Calculation

| Severity | Count | Penalty | Subtotal |
|----------|-------|---------|----------|
| CRITICAL | 7 | -10 | -70 |
| MEDIUM   | 24 | -3 | -72 |
| LOW      | 18 | -1 | -18 |
| INFO     | 8 | 0 | 0 |

**Raw:** 100 - 70 - 72 - 18 = **-60** (clamped to minimum)

**Final Score: 32/100**

> Note: The score is heavily impacted by class/method size violations (C-001, C-003) which account for 74+ individual occurrences consolidated into 2 findings. The code is functionally correct, well-tested (1,814 tests), secure, and follows many conventions well. The primary debt is structural: oversized classes, `return null` patterns, duplicated utility methods, and missing hexagonal architecture layering.

---

## Top Recommendations (Priority Order)

1. **Extract shared utilities** -- Consolidate `writeFile`, `readFile`, `listMdFilesSorted`, `indent`, `escapeJson`, `deleteQuietly` into `CopyHelpers` / `JsonHelpers`. This eliminates C-007 (14+ duplicated files) immediately.

2. **Eliminate `return null`** -- Replace 17 occurrences with `Optional<T>`. Start with `CopyHelpers.copyTemplateFileIfExists()` which is used broadly.

3. **Unify `buildContext()`** -- Remove the 3 local variants; all assemblers should use `ContextBuilder.buildContext()`. This fixes C-006 and the boolean conversion inconsistency.

4. **Split oversized classes** -- Focus on the top 4 (CicdAssembler, GithubInstructionsAssembler, RulesAssembler, SettingsAssembler). Extract responsibility-specific classes (e.g., `DockerfileAssembler`, `K8sAssembler` from `CicdAssembler`).

5. **Decompose long methods** -- Priority: `AssemblerPipeline.buildAssemblers()` (112 lines), `ContextBuilder.buildContext()` (74 lines), `Consolidator.consolidateFrameworkRules()` (64 lines).

6. **Extract Jackson from checkpoint domain** -- Move serialization to an adapter class behind a port interface.

7. **Fix JSON escaping** -- Implement full RFC 8259 escaping in `escapeJson()` or replace with Jackson-based JSON generation.

8. **Standardize test naming** -- Rename ~441 camelCase test methods to `[method]_[scenario]_[expected]` convention.

9. **Strengthen weak assertions** -- Replace 27 `isNotNull()`-only assertions with specific value checks.

10. **Document architecture decisions** -- Create an ADR for the flat package layout if hexagonal structure is intentionally relaxed for this CLI tool.
