# Test Plan -- STORY-016: Pipeline Orchestrator

## Summary

- Total test classes: 1 (`pipeline-orchestrator.test.ts`)
- Total test methods: ~52 (estimated)
- Categories covered: Unit, Integration, Contract (parametrized)
- Estimated line coverage: ~97%
- Estimated branch coverage: ~93%

## Test File: `tests/node/assembler/pipeline-orchestrator.test.ts`

### Key Dependencies Under Test

| Module | Export | Description |
|--------|--------|-------------|
| `src/assembler/index.ts` | `runPipeline` | Main entry point |
| `src/assembler/index.ts` | `_buildAssemblers` | Builds ordered list of 14 assemblers |
| `src/assembler/index.ts` | `_executeAssemblers` | Sequential execution loop |
| `src/assembler/index.ts` | `_runReal` | Real mode with atomicOutput |
| `src/assembler/index.ts` | `_runDry` | Dry-run mode with temp dir |
| `src/models.ts` | `PipelineResult` | Aggregated result type |
| `src/exceptions.ts` | `PipelineError` | Error type with assemblerName + reason |
| `src/utils.ts` | `atomicOutput` | Async callback-based atomic write |

### Assembler Return Type Map

The pipeline must handle two distinct return types from assemblers:

| Return Type | Assemblers |
|-------------|-----------|
| `AssembleResult {files, warnings}` | RulesAssembler, AgentsAssembler, GithubMcpAssembler, GithubAgentsAssembler |
| `string[]` | SkillsAssembler, PatternsAssembler, ProtocolsAssembler, HooksAssembler, SettingsAssembler, GithubInstructionsAssembler, GithubSkillsAssembler, GithubHooksAssembler, GithubPromptsAssembler, ReadmeAssembler |

### Fixture Setup

```
beforeEach:
  - tmpDir = fs.mkdtempSync(path.join(tmpdir(), "pipeline-test-"))
  - resourcesDir = path.join(tmpDir, "resources")
  - outputDir = path.join(tmpDir, "output")
  - Create minimal resources subdirs needed by assemblers
  - buildConfig(overrides) factory returning ProjectConfig

afterEach:
  - fs.rmSync(tmpDir, { recursive: true, force: true })
```

### Mocking Strategy

All 14 individual assemblers have their own dedicated test suites. The pipeline orchestrator tests should **mock the assembler instances** to isolate orchestration logic from assembler internals.

```typescript
// Mock approach: vi.mock() each assembler module
// Each assembler's assemble() returns predictable files/warnings
// This isolates pipeline logic: ordering, aggregation, error handling

vi.mock("../../../src/assembler/rules-assembler.js", () => ({
  RulesAssembler: vi.fn().mockImplementation(() => ({
    assemble: vi.fn().mockReturnValue({
      files: ["rules/01-identity.md"],
      warnings: [],
    }),
  })),
}));
// ... repeat for all 14 assemblers
```

Alternative: Use a lightweight spy approach where assemblers are injected or created via a factory that can be overridden.

### Helpers Needed

- `buildConfig(overrides)` -- returns `ProjectConfig` with minimal valid defaults
- `createMockAssembler(files, warnings?)` -- returns mock with `{files, warnings}` return
- `createMockStringAssembler(files)` -- returns mock with `string[]` return
- `setupMinimalResources(tmpDir)` -- creates skeleton resource dirs

---

## Group 1: _buildAssemblers -- Assembler Construction and Ordering (RULE-008)

### 1.1 Assembler Order Verification

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 1 | buildAssemblers_returns14Assemblers | List length is exactly 14 | Happy |
| 2 | buildAssemblers_correctOrder | Assemblers are in RULE-008 order | Happy |
| 3 | buildAssemblers_firstIsRulesAssembler | Index 0 is RulesAssembler | Boundary |
| 4 | buildAssemblers_lastIsReadmeAssembler | Index 13 is ReadmeAssembler | Boundary |

**Parametrized (it.each):**

```typescript
it.each([
  [0,  "RulesAssembler"],
  [1,  "SkillsAssembler"],
  [2,  "AgentsAssembler"],
  [3,  "PatternsAssembler"],
  [4,  "ProtocolsAssembler"],
  [5,  "HooksAssembler"],
  [6,  "SettingsAssembler"],
  [7,  "GithubInstructionsAssembler"],
  [8,  "GithubMcpAssembler"],
  [9,  "GithubSkillsAssembler"],
  [10, "GithubAgentsAssembler"],
  [11, "GithubHooksAssembler"],
  [12, "GithubPromptsAssembler"],
  [13, "ReadmeAssembler"],
])("buildAssemblers_index%i_is%s", (index, expectedName) => ...)
```

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 5 | buildAssemblers_indexN_isExpectedAssembler | Parametrized 14-row contract test | Contract |

---

## Group 2: _executeAssemblers -- Sequential Execution

### 2.1 Execution Order and Call Verification

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 6 | executeAssemblers_callsAllAssemblersInOrder | All 14 assemble() called sequentially | Happy |
| 7 | executeAssemblers_passesCorrectArguments | Each assembler receives (config, outputDir, resourcesDir, engine) | Happy |
| 8 | executeAssemblers_skillsAssembler_receivesResourcesDir | SkillsAssembler gets resourcesDir as additional param | Happy |
| 9 | executeAssemblers_agentsAssembler_receivesResourcesDir | AgentsAssembler gets resourcesDir as additional param | Happy |

### 2.2 Result Aggregation

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 10 | executeAssemblers_aggregatesFilesFromAllAssemblers | Files from all 14 combined in single array | Happy |
| 11 | executeAssemblers_aggregatesWarningsFromAssembleResultType | Warnings from AssembleResult assemblers collected | Happy |
| 12 | executeAssemblers_stringArrayAssemblers_treatedAsFilesOnly | string[] return treated as files with no warnings | Happy |
| 13 | executeAssemblers_emptyAssemblerResults_handledGracefully | Assembler returning empty files/warnings does not break | Boundary |
| 14 | executeAssemblers_mixedReturnTypes_allAggregatedCorrectly | Mix of AssembleResult and string[] merged correctly | Happy |

### 2.3 Warning Aggregation from Multiple Assemblers

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 15 | executeAssemblers_warningsFromTwoAssemblers_bothCollected | RulesAssembler + GithubMcpAssembler warnings merged | Happy |
| 16 | executeAssemblers_noWarnings_emptyWarningsArray | All assemblers return no warnings | Boundary |

---

## Group 3: runPipeline -- Real Mode (_runReal)

### 3.1 Atomic Output Integration

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 17 | runPipeline_realMode_callsAtomicOutput | atomicOutput invoked with outputDir | Happy |
| 18 | runPipeline_realMode_executesAssemblersInsideCallback | Assemblers run within atomicOutput temp dir | Happy |
| 19 | runPipeline_realMode_filesWrittenToFinalOutputDir | After atomicOutput completes, files exist at dest | Happy |

### 3.2 Atomic Output Failure Protection

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 20 | runPipeline_realMode_assemblerFails_outputDirNotCorrupted | If assembler throws, outputDir remains clean | Error |
| 21 | runPipeline_realMode_assemblerFails_tempDirCleanedUp | Temp dir removed after failure | Error |

### 3.3 PipelineResult for Real Mode

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 22 | runPipeline_realMode_success_isTrue | PipelineResult.success = true | Happy |
| 23 | runPipeline_realMode_outputDir_matchesInput | PipelineResult.outputDir = provided outputDir | Happy |
| 24 | runPipeline_realMode_filesGenerated_containsAllFiles | PipelineResult.filesGenerated has entries from all assemblers | Happy |
| 25 | runPipeline_realMode_warnings_containsAggregatedWarnings | PipelineResult.warnings merged from all assemblers | Happy |

---

## Group 4: runPipeline -- Dry-Run Mode (_runDry)

### 4.1 No Side Effects

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 26 | runPipeline_dryRun_doesNotCreateOutputDir | outputDir not created or modified | Happy |
| 27 | runPipeline_dryRun_doesNotCallAtomicOutput | atomicOutput never invoked | Happy |
| 28 | runPipeline_dryRun_usesTemporaryDir | Assemblers execute in a temp directory | Happy |
| 29 | runPipeline_dryRun_tempDirCleanedAfterExecution | Temp dir removed after dry-run completes | Happy |

### 4.2 PipelineResult for Dry-Run

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 30 | runPipeline_dryRun_success_isTrue | PipelineResult.success = true | Happy |
| 31 | runPipeline_dryRun_filesGenerated_containsExpectedFiles | List of files that would be generated | Happy |
| 32 | runPipeline_dryRun_warnings_includesDryRunIndicator | Warnings contain dry-run note | Happy |
| 33 | runPipeline_dryRun_outputDir_reflectsProvidedDir | PipelineResult.outputDir = provided outputDir (not temp) | Boundary |

---

## Group 5: Duration Measurement

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 34 | runPipeline_durationMs_isPositiveNumber | durationMs > 0 | Happy |
| 35 | runPipeline_durationMs_isTypeNumber | typeof durationMs === "number" | Boundary |
| 36 | runPipeline_durationMs_reflectsElapsedTime | durationMs >= mock delay (if injected) | Happy |
| 37 | runPipeline_dryRun_durationMs_isPositiveNumber | durationMs > 0 in dry-run mode too | Happy |

---

## Group 6: Error Handling (PipelineError)

### 6.1 PipelineError Construction

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 38 | pipelineError_hasCorrectName | error.name === "PipelineError" | Happy |
| 39 | pipelineError_hasAssemblerName | error.assemblerName set correctly | Happy |
| 40 | pipelineError_hasReason | error.reason contains original error message | Happy |
| 41 | pipelineError_messageFormat | error.message includes assemblerName and reason | Happy |

### 6.2 Pipeline Error Propagation

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 42 | runPipeline_assemblerThrows_throwsPipelineError | Error wrapped in PipelineError | Error |
| 43 | runPipeline_assemblerThrows_pipelineErrorHasAssemblerName | PipelineError.assemblerName identifies which assembler failed | Error |
| 44 | runPipeline_assemblerThrows_pipelineErrorHasOriginalMessage | PipelineError.reason carries original error message | Error |
| 45 | runPipeline_firstAssemblerFails_subsequentNotCalled | Remaining assemblers skipped on failure | Error |
| 46 | runPipeline_lastAssemblerFails_previousFilesNotInResult | Partial results not returned on failure | Error |

---

## Group 7: Edge Cases and Boundary Conditions

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 47 | runPipeline_allAssemblersReturnEmpty_success | All return empty files/warnings, still succeeds | Boundary |
| 48 | runPipeline_largeFilesList_allAggregated | Many files from each assembler, all in result | Boundary |
| 49 | runPipeline_warningsFromAllFourAssembleResultTypes_merged | RulesAssembler + AgentsAssembler + GithubMcpAssembler + GithubAgentsAssembler warnings combined | Boundary |

---

## Group 8: Integration Tests (Optional -- Real Assemblers)

These tests use real assembler instances with minimal resource fixtures. They validate the full pipeline end-to-end without mocks.

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 50 | integration_runPipeline_realMode_producesOutputFiles | Real config + resources generate files | Integration |
| 51 | integration_runPipeline_dryRun_producesResult | Dry-run returns valid PipelineResult | Integration |
| 52 | integration_runPipeline_resultMatchesExpectedStructure | PipelineResult has all required fields with correct types | Integration |

---

## Coverage Estimation

| Group | Functions/Paths | Branches | Est. Tests | Line % | Branch % |
|-------|----------------|----------|-----------|--------|----------|
| 1. _buildAssemblers | 1 | 0 | 5 | 100% | N/A |
| 2. _executeAssemblers | 1 | 4 (return type check, empty check) | 11 | 100% | 95% |
| 3. _runReal | 1 | 2 (success/failure) | 9 | 100% | 100% |
| 4. _runDry | 1 | 1 (cleanup) | 8 | 100% | 100% |
| 5. Duration | 0 (cross-cutting) | 0 | 4 | 100% | N/A |
| 6. Error Handling | 1 | 3 (error type, wrap, propagate) | 9 | 95% | 90% |
| 7. Edge Cases | 0 (cross-cutting) | 2 | 3 | 95% | 90% |
| 8. Integration | 0 (full stack) | 0 | 3 | 90% | 85% |
| **Total** | **5** | **12** | **~52** | **~97%** | **~93%** |

---

## Risks and Gaps

1. **Return type polymorphism:** The pipeline must correctly distinguish between `AssembleResult` (with `.files` and `.warnings` properties) and plain `string[]` returns. The type guard logic is a critical branch to test thoroughly. A common pattern is `if ("files" in result)` or `if (Array.isArray(result))`.

2. **atomicOutput callback signature:** `atomicOutput` is async and callback-based (`atomicOutput<T>(destDir, callback: (tempDir: string) => Promise<T>)`). Tests must verify the pipeline correctly passes a callback that uses the temp dir, not the final output dir, during assembly.

3. **SkillsAssembler and AgentsAssembler special params:** The story notes these receive `resourcesDir` as an additional parameter. If the pipeline treats them differently from other assemblers, those code paths need explicit coverage.

4. **Dry-run temp dir lifecycle:** The temp dir must be created before assemblers run and removed after. If `mkdtemp` or cleanup fails, the error path must be covered.

5. **PipelineError vs raw Error:** If an assembler throws a non-Error value (e.g., string), the wrapping logic must handle it. This is a subtle branch worth testing.

6. **Duration precision:** Using `performance.now()` vs `Date.now()` produces different precision. Tests should assert `durationMs >= 0` rather than exact values to avoid flakiness.

7. **Assembler constructor dependencies:** Some assemblers (SkillsAssembler, AgentsAssembler) may need `resourcesDir` at construction time vs at `assemble()` call time. The pipeline must pass it consistently.

8. **Order-dependent output:** ReadmeAssembler (index 13) scans files generated by all preceding assemblers. In the pipeline, it must run last. If order is violated, the README will be incomplete. The parametrized order test in Group 1 guards against this.

---

## Test Naming Convention

All tests follow the project standard:

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

Examples:
- `buildAssemblers_returns14Assemblers`
- `runPipeline_dryRun_doesNotCreateOutputDir`
- `executeAssemblers_warningsFromTwoAssemblers_bothCollected`
