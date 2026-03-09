# Task Decomposition -- STORY-002: Exceptions & Utilities (Python to TypeScript Migration)

**Status:** PLANNED
**Date:** 2026-03-09
**Blocked By:** STORY-001 (project foundation)
**Blocks:** STORY-004 (config loader), STORY-016 (pipeline orchestrator)

---

## G1 -- Exception Classes

**Dependencies:** None (leaf module, zero imports)

### T1.1 -- Add `ConfigValidationError` class
- **File:** `src/exceptions.ts`
- **Description:** Create `ConfigValidationError` extending `Error` with `readonly missingFields: string[]` property. Set `this.name = "ConfigValidationError"`. Message format: `"Missing required config sections: field1, field2"`. Export from module.
- **Estimated lines:** ~10
- **Tier:** Junior

### T1.2 -- Add `PipelineError` class
- **File:** `src/exceptions.ts`
- **Description:** Create `PipelineError` extending `Error` with `readonly assemblerName: string` and `readonly reason: string` properties. Set `this.name = "PipelineError"`. Message format: `"Pipeline failed at 'assemblerName': reason"`. Export from module.
- **Estimated lines:** ~10
- **Tier:** Junior

### Compilation checkpoint
```
npx tsc --noEmit   # zero errors with both new classes exported
```

---

## G2 -- Exception Tests

**Dependencies:** G1

### T2.1 -- Unit tests for `ConfigValidationError`
- **File:** `tests/node/exceptions.test.ts` (new file)
- **Description:** Test construction with multiple missing fields. Verify: `message` matches expected format, `name` equals `"ConfigValidationError"`, `missingFields` array matches input, `instanceof Error` is `true`. Test with single field and empty array edge cases.
- **Estimated lines:** ~30
- **Tier:** Junior

### T2.2 -- Unit tests for `PipelineError`
- **File:** `tests/node/exceptions.test.ts`
- **Description:** Test construction with assembler name and reason. Verify: `message` matches expected format with quotes around assembler name, `name` equals `"PipelineError"`, `assemblerName` and `reason` properties match input, `instanceof Error` is `true`.
- **Estimated lines:** ~30
- **Tier:** Junior

### Compilation checkpoint
```
npx tsc --noEmit
npm run test -- tests/node/exceptions.test.ts   # all green
```

---

## G3 -- Sync Utility Functions (no I/O)

**Dependencies:** G1 (establishes the pattern; no code dependency -- `rejectDangerousPath` throws plain `Error`, not custom exceptions)

### T3.1 -- Add `PROTECTED_PATHS` constant
- **File:** `src/utils.ts`
- **Description:** Define and export `PROTECTED_PATHS` as `ReadonlySet<string>` containing `["/", "/tmp", "/var", "/etc", "/usr"]`. Use `Object.freeze(new Set([...]))` with explicit type annotation.
- **Estimated lines:** ~3
- **Tier:** Junior

### T3.2 -- Add `rejectDangerousPath` function
- **File:** `src/utils.ts`
- **Description:** Synchronous function `rejectDangerousPath(resolvedPath: string): void`. Throws `Error` with descriptive message if path equals `process.cwd()`, `os.homedir()`, or is contained in `PROTECTED_PATHS`. Add import for `node:os`. Export from module.
- **Estimated lines:** ~15
- **Tier:** Mid

### T3.3 -- Add `setupLogging` function
- **File:** `src/utils.ts`
- **Description:** `setupLogging(verbose: boolean): void`. When `verbose` is `false`, replace `console.debug` with a no-op function. When `verbose` is `true`, restore original `console.debug` (store reference at module level). Export from module.
- **Estimated lines:** ~10
- **Tier:** Mid

### T3.4 -- Add `findResourcesDir` function
- **File:** `src/utils.ts`
- **Description:** `findResourcesDir(): string`. Uses `import.meta.url` with `fileURLToPath` to get current file path, traverses up to package root (handles both `src/` and `dist/` layouts), appends `resources/`. Throws `Error` if resulting directory does not exist (check with `statSync`). Add imports for `node:url`, `node:path`, `node:fs`. Export from module.
- **Estimated lines:** ~15
- **Tier:** Mid

### Compilation checkpoint
```
npx tsc --noEmit   # all new exports compile cleanly
```

---

## G4 -- Async Utility Functions (I/O)

**Dependencies:** G3 (`validateDestPath` calls `rejectDangerousPath`)

### T4.1 -- Add `validateDestPath` function
- **File:** `src/utils.ts`
- **Description:** `validateDestPath(destDir: string): Promise<string>`. Uses `lstat()` from `node:fs/promises` to check if path is a symlink (reject with `Error` if `stat.isSymbolicLink()`). For non-existent paths, catch `ENOENT` from `lstat` and proceed (new dest is valid). Resolves path via `path.resolve()`, delegates to `rejectDangerousPath`, returns resolved path. Export from module.
- **Estimated lines:** ~18
- **Tier:** Mid

### T4.2 -- Add `atomicOutput` function
- **File:** `src/utils.ts`
- **Description:** Generic async function `atomicOutput<T>(destDir: string, callback: (tempDir: string) => Promise<T>): Promise<T>`. Steps: (1) create temp dir via `mkdtemp(path.join(os.tmpdir(), 'ia-dev-env-'))`, (2) execute `callback(tempDir)`, (3) remove existing `destDir` if it exists via `rm({ recursive: true, force: true })`, (4) copy temp to dest via `cp(tempDir, destDir, { recursive: true })`, (5) return callback result. Cleanup temp dir in `finally` block via `rm({ recursive: true, force: true })`. Add imports for `node:fs/promises` (`mkdtemp`, `cp`, `rm`, `stat`), `node:os` (`tmpdir`). Export from module.
- **Estimated lines:** ~25
- **Tier:** Senior

### Compilation checkpoint
```
npx tsc --noEmit   # all async functions compile cleanly
```

---

## G5 -- Tests for Sync Utilities

**Dependencies:** G3

### T5.1 -- Tests for `PROTECTED_PATHS`
- **File:** `tests/node/utils.test.ts` (new file)
- **Description:** Verify the set contains all 5 expected paths (`/`, `/tmp`, `/var`, `/etc`, `/usr`). Verify `size` equals 5. Verify the set is frozen (attempt to add throws or is a no-op with ReadonlySet type).
- **Estimated lines:** ~15
- **Tier:** Junior

### T5.2 -- Tests for `rejectDangerousPath`
- **File:** `tests/node/utils.test.ts`
- **Description:** Test rejection of CWD (mock `process.cwd` via `vi.spyOn`). Test rejection of home directory (mock `os.homedir` via `vi.spyOn`). Test rejection of each protected path (`/`, `/tmp`, `/var`, `/etc`, `/usr`). Test acceptance of a safe path (e.g., `/home/user/project`). Verify error messages carry context about why the path was rejected.
- **Estimated lines:** ~45
- **Tier:** Mid

### T5.3 -- Tests for `setupLogging`
- **File:** `tests/node/utils.test.ts`
- **Description:** Test that `setupLogging(false)` causes `console.debug` to become a no-op (spy on `console.debug`, call it, verify no output). Test that `setupLogging(true)` restores original behavior. Clean up after each test to avoid side effects.
- **Estimated lines:** ~25
- **Tier:** Mid

### T5.4 -- Tests for `findResourcesDir`
- **File:** `tests/node/utils.test.ts`
- **Description:** Test that it returns a path ending in `resources` and the directory exists on disk. Test error case when resources directory is not found (mock `statSync` to throw `ENOENT`).
- **Estimated lines:** ~20
- **Tier:** Mid

### Compilation checkpoint
```
npx tsc --noEmit
npm run test -- tests/node/utils.test.ts   # sync tests green
```

---

## G6 -- Tests for Async Utilities

**Dependencies:** G4, G5 (async functions implemented; sync test patterns established)

### T6.1 -- Tests for `validateDestPath`
- **File:** `tests/node/utils.test.ts`
- **Description:** Test with valid directory path (create temp dir, returns resolved absolute path). Test rejection of symlink (create symlink in temp dir via `fs.symlinkSync`, expect error; skip with `EPERM` guard on Windows). Test rejection when resolved path is dangerous (mock `process.cwd` to match input). Test with non-existent path (resolves without error for new destinations).
- **Estimated lines:** ~50
- **Tier:** Senior

### T6.2 -- Tests for `atomicOutput`
- **File:** `tests/node/utils.test.ts`
- **Description:** Test success path: callback writes a file to temp dir, verify file appears in dest dir after completion. Test failure path: callback throws, verify dest dir is unchanged (pre-existing content preserved or dir does not exist) and temp dir is cleaned up. Test overwrite path: dest dir already has content, gets fully replaced. Test cleanup: verify temp dir is removed in both success and failure cases (glob for `ia-dev-env-*` in `os.tmpdir()`).
- **Estimated lines:** ~65
- **Tier:** Senior

### Compilation checkpoint
```
npx tsc --noEmit
npm run test -- tests/node/utils.test.ts   # all tests green
```

---

## G7 -- Final Verification

**Dependencies:** G2, G5, G6

### T7.1 -- Full lint and compilation check
- **Description:** Run `npx tsc --noEmit`. Zero warnings, zero errors across entire project.
- **Estimated lines:** 0 (verification only)
- **Tier:** Junior

### T7.2 -- Full test suite with coverage
- **Description:** Run `npm run test:coverage`. Verify all tests pass. Verify line coverage >= 95% and branch coverage >= 90% for both `src/exceptions.ts` and `src/utils.ts`.
- **Estimated lines:** 0 (verification only)
- **Tier:** Junior

### T7.3 -- Export and integration sanity check
- **Description:** Verify all new symbols are properly exported: `ConfigValidationError`, `PipelineError`, `PROTECTED_PATHS`, `rejectDangerousPath`, `validateDestPath`, `atomicOutput`, `setupLogging`, `findResourcesDir`. Verify no circular dependencies. Verify existing `CliError` and `normalizeDirectory` are unaffected.
- **Estimated lines:** 0 (verification only)
- **Tier:** Junior

---

## Dependency Graph

```
G1 (Exception Classes)
 ├──> G2 (Exception Tests)
 └──> G3 (Sync Utilities)
       ├──> G4 (Async Utilities)
       │     └──> G6 (Async Utility Tests)
       └──> G5 (Sync Utility Tests)
             └──> G7 (Final Verification) <── also depends on G2, G6
```

**Parallelizable after G1:** G2 and G3 can run in parallel.
**Parallelizable after G3:** G4 and G5 can run in parallel.

## Critical Path

```
G1 --> G3 --> G4 --> G6 --> G7
```

---

## File Summary

| File | Group(s) | Action |
|------|----------|--------|
| `src/exceptions.ts` | G1 | Modified (add `ConfigValidationError`, `PipelineError` below existing `CliError`) |
| `src/utils.ts` | G3, G4 | Modified (add `PROTECTED_PATHS`, `rejectDangerousPath`, `setupLogging`, `findResourcesDir`, `validateDestPath`, `atomicOutput`) |
| `tests/node/exceptions.test.ts` | G2 | Created |
| `tests/node/utils.test.ts` | G5, G6 | Created |

---

## Totals

| Group | Tasks | Estimated Lines |
|-------|-------|----------------|
| G1 -- Exception Classes | 2 | ~20 |
| G2 -- Exception Tests | 2 | ~60 |
| G3 -- Sync Utilities | 4 | ~43 |
| G4 -- Async Utilities | 2 | ~43 |
| G5 -- Sync Utility Tests | 4 | ~105 |
| G6 -- Async Utility Tests | 2 | ~115 |
| G7 -- Final Verification | 3 | 0 |
| **Total** | **19 tasks** | **~386 lines** |

- **New files:** 2 (test files)
- **Modified files:** 2 (`src/exceptions.ts`, `src/utils.ts`)
- **Verification only:** 3 tasks (G7)
