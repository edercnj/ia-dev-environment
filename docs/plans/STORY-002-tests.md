# Test Plan -- STORY-002: Exceptions & Utils (TypeScript Migration)

**Status:** PLANNED
**Date:** 2026-03-09
**Story:** STORY-002
**Scope:** `ConfigValidationError`, `PipelineError`, `validateDestPath`, `rejectDangerousPath`, `atomicOutput`, `setupLogging`, `findResourcesDir`
**Framework:** vitest
**Coverage Targets:** Line >= 95%, Branch >= 90%

---

## 1. Context, Scope, and Assumptions

This plan covers the exception classes and utility functions introduced in STORY-002. All items are leaf dependencies with zero project imports -- they depend only on Node.js standard library modules.

Assumptions:
- Test naming convention: `[methodUnderTest]_[scenario]_[expectedBehavior]`
- Test location: `tests/node/exceptions.test.ts` and `tests/node/utils.test.ts`
- No integration, API, E2E, or contract tests needed -- all functions are pure utilities testable in isolation.
- Mocking limited to `node:fs/promises`, `process.cwd()`, `os.homedir()`, and `import.meta.url` resolution.

---

## 2. Applicability Matrix

| Category | Applicable? | Justification |
|----------|-------------|---------------|
| Unit | Yes | All functions are deterministic utilities testable in isolation. |
| Integration | No | No cross-module orchestration in this story. |
| API | No | No HTTP/gRPC surface. |
| E2E | No | No end-to-end flow. |
| Contract | No | No external data contracts. |
| Performance | No | No latency SLAs for utilities. |

---

## 3. Unit Test Scenarios

### 3.1 `ConfigValidationError` (`tests/node/exceptions.test.ts`)

| # | Scenario Name | Input | Expected Output | Rule |
|---|---------------|-------|-----------------|------|
| U-01 | `ConfigValidationError_withSingleField_formatsMessageCorrectly` | `missingFields: ["database"]` | `message === "Missing required config sections: database"` | Plan sec 2.1 |
| U-02 | `ConfigValidationError_withMultipleFields_joinsWithComma` | `missingFields: ["database", "server", "auth"]` | `message === "Missing required config sections: database, server, auth"` | Plan sec 2.1 |
| U-03 | `ConfigValidationError_constructor_setsNameProperty` | any fields | `error.name === "ConfigValidationError"` | Plan sec 2.1 |
| U-04 | `ConfigValidationError_constructor_storesMissingFieldsAsReadonly` | `["a", "b"]` | `error.missingFields` deep equals `["a", "b"]` | Plan sec 2.1 |
| U-05 | `ConfigValidationError_withEmptyArray_formatsEmptyMessage` | `missingFields: []` | `message === "Missing required config sections: "` | Edge case |
| U-06 | `ConfigValidationError_instanceof_isError` | any fields | `error instanceof Error === true` | Inheritance |

### 3.2 `PipelineError` (`tests/node/exceptions.test.ts`)

| # | Scenario Name | Input | Expected Output | Rule |
|---|---------------|-------|-----------------|------|
| U-07 | `PipelineError_withAssemblerAndReason_formatsMessageCorrectly` | `assemblerName: "RulesAssembler"`, `reason: "template not found"` | `message === "Pipeline failed at 'RulesAssembler': template not found"` | Plan sec 2.1 |
| U-08 | `PipelineError_constructor_setsNameProperty` | any | `error.name === "PipelineError"` | Plan sec 2.1 |
| U-09 | `PipelineError_constructor_storesAssemblerNameAsReadonly` | `"SkillsAssembler"` | `error.assemblerName === "SkillsAssembler"` | Plan sec 2.1 |
| U-10 | `PipelineError_constructor_storesReasonAsReadonly` | `"disk full"` | `error.reason === "disk full"` | Plan sec 2.1 |
| U-11 | `PipelineError_instanceof_isError` | any | `error instanceof Error === true` | Inheritance |
| U-12 | `PipelineError_withEmptyStrings_formatsWithEmptyValues` | `assemblerName: ""`, `reason: ""` | `message === "Pipeline failed at '': "` | Edge case |

### 3.3 `rejectDangerousPath` (`tests/node/utils.test.ts`)

| # | Scenario Name | Input | Expected Output | Rule |
|---|---------------|-------|-----------------|------|
| U-13 | `rejectDangerousPath_withRootPath_throws` | `"/"` | Throws error mentioning protected path | PROTECTED_PATHS |
| U-14 | `rejectDangerousPath_withTmpPath_throws` | `"/tmp"` | Throws error | PROTECTED_PATHS |
| U-15 | `rejectDangerousPath_withVarPath_throws` | `"/var"` | Throws error | PROTECTED_PATHS |
| U-16 | `rejectDangerousPath_withEtcPath_throws` | `"/etc"` | Throws error | PROTECTED_PATHS |
| U-17 | `rejectDangerousPath_withUsrPath_throws` | `"/usr"` | Throws error | PROTECTED_PATHS |
| U-18 | `rejectDangerousPath_withCwd_throws` | `process.cwd()` (mocked) | Throws error mentioning CWD | Plan sec 2.2 |
| U-19 | `rejectDangerousPath_withHomeDir_throws` | `os.homedir()` (mocked) | Throws error mentioning home dir | Plan sec 2.2 |
| U-20 | `rejectDangerousPath_withSafePath_doesNotThrow` | `"/home/user/projects/output"` | No error thrown | Happy path |
| U-21 | `rejectDangerousPath_withSubdirOfProtected_doesNotThrow` | `"/tmp/ia-dev-env-output"` | No error thrown (subdirectory is safe) | Edge case |

### 3.4 `validateDestPath` (`tests/node/utils.test.ts`)

| # | Scenario Name | Input | Expected Output | Rule |
|---|---------------|-------|-----------------|------|
| U-22 | `validateDestPath_withSymlink_rejectsWithError` | Symlinked path (created via `fs.symlinkSync` in setup) | Throws error about symlinks | Plan sec 2.2 |
| U-23 | `validateDestPath_withValidDirectory_returnsResolvedPath` | Real directory path | Returns resolved absolute path | Happy path |
| U-24 | `validateDestPath_withRelativePath_resolvesToAbsolute` | `"./output"` (relative) | Returns absolute resolved path | Path resolution |
| U-25 | `validateDestPath_withDangerousPath_delegatesToRejectDangerousPath` | `"/"` | Throws (delegates to `rejectDangerousPath`) | Integration |
| U-26 | `validateDestPath_withNonExistentPath_handlesGracefully` | Path that does not exist on disk | Either resolves or throws (document actual behavior) | Edge case |

### 3.5 `atomicOutput` (`tests/node/utils.test.ts`)

| # | Scenario Name | Input | Expected Output | Rule |
|---|---------------|-------|-----------------|------|
| U-27 | `atomicOutput_withSuccessfulCallback_copiesToDest` | Callback writes a file to tempDir | File exists at destDir after completion | Happy path |
| U-28 | `atomicOutput_withSuccessfulCallback_cleansTempDir` | Callback succeeds | Temp directory removed after completion | Cleanup guarantee |
| U-29 | `atomicOutput_withSuccessfulCallback_returnsCallbackResult` | Callback returns `42` | `atomicOutput` resolves to `42` | Generic return |
| U-30 | `atomicOutput_withFailingCallback_doesNotModifyDest` | Callback throws | Dest directory unchanged (pre-existing content intact) | Atomicity |
| U-31 | `atomicOutput_withFailingCallback_cleansTempDir` | Callback throws | Temp directory removed despite failure | Cleanup in `finally` |
| U-32 | `atomicOutput_withFailingCallback_propagatesOriginalError` | Callback throws `new Error("boom")` | Rejects with `"boom"` | Error propagation |
| U-33 | `atomicOutput_withExistingDestDir_replacesContents` | Dest already has files | Old files replaced with new callback output | Overwrite behavior |
| U-34 | `atomicOutput_withNonExistentDestDir_createsIt` | Dest does not exist | Dest directory created with callback output | Directory creation |
| U-35 | `atomicOutput_callbackReceivesTempDir_asFunctionArg` | Inspect callback arg | Callback receives a valid temp directory path | API contract |

### 3.6 `setupLogging` (`tests/node/utils.test.ts`)

| # | Scenario Name | Input | Expected Output | Rule |
|---|---------------|-------|-----------------|------|
| U-36 | `setupLogging_withVerboseTrue_setsDebugLevel` | `verbose: true` | Log level set to DEBUG (console.debug enabled) | Plan sec 2.2 |
| U-37 | `setupLogging_withVerboseFalse_setsInfoLevel` | `verbose: false` | Log level set to INFO (console.debug suppressed) | Plan sec 2.2 |

### 3.7 `findResourcesDir` (`tests/node/utils.test.ts`)

| # | Scenario Name | Input | Expected Output | Rule |
|---|---------------|-------|-----------------|------|
| U-38 | `findResourcesDir_fromSourceLayout_resolvesToResourcesDir` | Running from `src/` tree | Returns path ending in `resources/` | Plan sec 2.3 |
| U-39 | `findResourcesDir_returnedPath_isAbsolute` | Default invocation | Path starts with `/` | Path resolution |
| U-40 | `findResourcesDir_returnedPath_existsOnDisk` | Default invocation | `fs.existsSync(result) === true` | Validation |

### 3.8 `PROTECTED_PATHS` constant (`tests/node/utils.test.ts`)

| # | Scenario Name | Input | Expected Output | Rule |
|---|---------------|-------|-----------------|------|
| U-41 | `PROTECTED_PATHS_contents_containsAllExpectedPaths` | Read constant | Contains exactly `["/", "/tmp", "/var", "/etc", "/usr"]` | Plan sec 2.2 |
| U-42 | `PROTECTED_PATHS_type_isReadonlySet` | Read constant | `PROTECTED_PATHS instanceof Set === true` | Immutability |

---

## 4. Mock Strategy

### 4.1 `rejectDangerousPath` Tests

| Mock Target | Method | Reason |
|-------------|--------|--------|
| `process.cwd` | `vi.spyOn(process, 'cwd')` | Control CWD value to test rejection without depending on actual working directory. |
| `os.homedir` | `vi.mock('node:os')` or `vi.spyOn` | Control home directory value to test rejection independently. |

### 4.2 `validateDestPath` Tests

| Mock Target | Method | Reason |
|-------------|--------|--------|
| `fs/promises.lstat` | `vi.mock('node:fs/promises')` | Simulate symlink detection without creating real symlinks (platform-safe). |
| `fs/promises.realpath` | Same mock module | Control resolved path value. |
| Filesystem | Real temp directories via `fs.mkdtempSync` | For happy-path tests, use real filesystem to validate end-to-end behavior. |

Symlink tests should use real `fs.symlinkSync` where possible, with a conditional skip (`it.skipIf`) on platforms where symlink creation requires elevated privileges (Windows CI).

### 4.3 `atomicOutput` Tests

| Mock Target | Method | Reason |
|-------------|--------|--------|
| Filesystem | Real temp directories via `os.tmpdir()` + `mkdtemp` | `atomicOutput` performs real I/O; test with real temp dirs for fidelity. |
| No mocks | N/A | Prefer real filesystem operations. Use `afterEach` cleanup to remove test artifacts. |

### 4.4 `setupLogging` Tests

| Mock Target | Method | Reason |
|-------------|--------|--------|
| `console.debug` | `vi.spyOn(console, 'debug')` | Verify logging behavior without producing test output noise. |

### 4.5 `findResourcesDir` Tests

| Mock Target | Method | Reason |
|-------------|--------|--------|
| No mocks | N/A | Test against real package layout. Validates both `src/` and `dist/` resolution if both exist. |

---

## 5. Edge Cases Summary

### 5.1 Exception Edge Cases

| # | Scenario | Expected Behavior | Severity |
|---|----------|-------------------|----------|
| E-01 | `ConfigValidationError` with empty fields array | Message ends with `": "` (no fields listed) | Low |
| E-02 | `PipelineError` with empty assemblerName and reason | Message reads `"Pipeline failed at '': "` | Low |
| E-03 | Both exceptions preserve stack trace | `error.stack` is defined and includes constructor call site | Low |

### 5.2 Path Validation Edge Cases

| # | Scenario | Expected Behavior | Severity |
|---|----------|-------------------|----------|
| E-04 | Subdirectory of protected path (e.g., `/tmp/output`) | Allowed -- only exact matches are rejected | Medium |
| E-05 | Path with trailing slashes | Resolved before comparison (trailing slashes stripped) | Low |
| E-06 | CWD equals a protected path | Rejected by both CWD check and PROTECTED_PATHS check | Low |
| E-07 | Home directory equals a protected path (unlikely but possible for root user) | Rejected by both checks | Low |

### 5.3 Atomic Output Edge Cases

| # | Scenario | Expected Behavior | Severity |
|---|----------|-------------------|----------|
| E-08 | Callback writes nothing to temp dir | Dest dir gets empty directory contents | Low |
| E-09 | Callback throws synchronously (non-async error) | Caught by `finally`, temp cleaned, error propagated | Medium |
| E-10 | Dest dir has read-only permissions | `cp` throws `EACCES`, temp cleaned, error propagated | Low |
| E-11 | Temp dir creation fails (disk full) | `mkdtemp` throws, no cleanup needed (nothing was created) | Low |

### 5.4 Platform Edge Cases

| # | Scenario | Expected Behavior | Severity |
|---|----------|-------------------|----------|
| E-12 | PROTECTED_PATHS are Unix-only | Documented limitation. No Windows paths in set. | Low |
| E-13 | Symlink creation requires elevated privileges (Windows) | Test skipped conditionally with `it.skipIf` | Medium |

---

## 6. Coverage Assessment

### 6.1 Coverage Map

| Module | Public Methods | Branches (est.) | Test Count | Line Coverage (est.) | Branch Coverage (est.) |
|--------|---------------|-----------------|------------|---------------------|----------------------|
| `src/exceptions.ts` (new classes) | 2 constructors | 2 | 12 | 100% | 100% |
| `src/utils.ts` (new functions) | 5 functions + 1 constant | 14 | 28 | 97% | 93% |
| **Total** | **7** | **16** | **40** | **98%** | **95%** |

### 6.2 Branch Coverage Analysis

| Branch | Test IDs Covering It |
|--------|---------------------|
| `rejectDangerousPath`: path in PROTECTED_PATHS (true) | U-13 through U-17 |
| `rejectDangerousPath`: path in PROTECTED_PATHS (false) | U-20, U-21 |
| `rejectDangerousPath`: path === CWD (true) | U-18 |
| `rejectDangerousPath`: path === CWD (false) | U-13 through U-17, U-20 |
| `rejectDangerousPath`: path === homedir (true) | U-19 |
| `rejectDangerousPath`: path === homedir (false) | U-13 through U-18, U-20 |
| `validateDestPath`: lstat detects symlink (true) | U-22 |
| `validateDestPath`: lstat detects symlink (false) | U-23, U-24 |
| `atomicOutput`: callback succeeds (true) | U-27 through U-29, U-33 through U-35 |
| `atomicOutput`: callback throws (false) | U-30 through U-32 |
| `atomicOutput`: dest exists (true) | U-33 |
| `atomicOutput`: dest does not exist (false) | U-34 |
| `setupLogging`: verbose true | U-36 |
| `setupLogging`: verbose false | U-37 |

### 6.3 Assessment

All conditional branches have at least one test covering each side. Estimated coverage exceeds thresholds: Line >= 95%, Branch >= 90%.

---

## 7. Test Execution

### Run exception tests
```bash
npx vitest run tests/node/exceptions.test.ts
```

### Run utility tests
```bash
npx vitest run tests/node/utils.test.ts
```

### Run all STORY-002 tests
```bash
npx vitest run tests/node/exceptions.test.ts tests/node/utils.test.ts
```

### Run with coverage
```bash
npx vitest run tests/node/exceptions.test.ts tests/node/utils.test.ts --coverage
```

---

## 8. Test File Inventory

### New Test Files

| File | Type | Covers |
|------|------|--------|
| `tests/node/exceptions.test.ts` | Unit | U-01 through U-12 (both exception classes) |
| `tests/node/utils.test.ts` | Unit | U-13 through U-42 (all utility functions and constants) |

### No Modified Test Files

Existing tests in `tests/node/cli-help.test.ts` and `tests/node/index-bootstrap.test.ts` are not affected by STORY-002 changes.

---

## 9. Recommended Execution Order

1. Exception tests (U-01 through U-12) -- zero dependencies, fast
2. `PROTECTED_PATHS` and `rejectDangerousPath` tests (U-13 through U-21, U-41, U-42) -- sync, no I/O
3. `validateDestPath` tests (U-22 through U-26) -- async, requires filesystem mocks
4. `atomicOutput` tests (U-27 through U-35) -- async, uses real temp directories
5. `setupLogging` tests (U-36, U-37) -- console spies
6. `findResourcesDir` tests (U-38 through U-40) -- real filesystem validation
