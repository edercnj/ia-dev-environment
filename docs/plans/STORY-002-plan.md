# Implementation Plan -- STORY-002: Exceptions & Utils (Python to TypeScript Migration)

**Status:** PLANNED
**Date:** 2026-03-09
**Blocked By:** STORY-001 (project foundation)
**Blocks:** STORY-004 (config loader), STORY-016 (pipeline orchestrator)

---

## 1. Affected Layers and Components

This is a **library project** -- no database, no API, no events. All changes are in shared infrastructure modules.

| Layer | Component | Impact |
|-------|-----------|--------|
| Shared / Exceptions | `src/exceptions.ts` | Add `ConfigValidationError` and `PipelineError` alongside existing `CliError` |
| Shared / Utils | `src/utils.ts` | Add `atomicOutput`, `validateDestPath`, `rejectDangerousPath`, `setupLogging`, `findResourcesDir` alongside existing `normalizeDirectory` |
| Tests | `tests/node/exceptions.test.ts` | New test file for exception classes |
| Tests | `tests/node/utils.test.ts` | New test file for utility functions |

No other source files are modified. No assemblers, no CLI, no domain modules are touched.

---

## 2. New Classes/Interfaces to Create

### 2.1 Exception Classes (`src/exceptions.ts`)

| Class | Extends | Properties | Message Format |
|-------|---------|-----------|----------------|
| `ConfigValidationError` | `Error` | `readonly missingFields: string[]` | `"Missing required config sections: field1, field2"` |
| `PipelineError` | `Error` | `readonly assemblerName: string`, `readonly reason: string` | `"Pipeline failed at 'assemblerName': reason"` |

Both classes set `this.name` to the class name for proper error identification. Both use `readonly` properties (TypeScript strict mode).

### 2.2 Utility Functions (`src/utils.ts`)

| Function | Signature | Responsibility |
|----------|-----------|----------------|
| `PROTECTED_PATHS` | `ReadonlySet<string>` (module constant) | Frozen set: `["/", "/tmp", "/var", "/etc", "/usr"]` |
| `validateDestPath` | `(destDir: string) => Promise<string>` | Rejects symlinks via `lstat()`, resolves path, delegates to `rejectDangerousPath`, returns resolved path |
| `rejectDangerousPath` | `(resolvedPath: string) => void` | Throws `ValueError`-equivalent if path equals CWD, home dir, or is in PROTECTED_PATHS |
| `atomicOutput` | `<T>(destDir: string, callback: (tempDir: string) => Promise<T>) => Promise<T>` | Creates temp dir, runs callback, copies temp to dest, cleans up temp in `finally` block |
| `setupLogging` | `(verbose: boolean) => void` | Sets global log level (DEBUG vs INFO); uses `console`-based approach |
| `findResourcesDir` | `() => string` | Locates `resources/` relative to package root (`__dirname` equivalent via `import.meta.url`) |

### 2.3 Design Decisions

**`atomicOutput` pattern:** The Python version uses a context manager (`@contextmanager`). TypeScript has no direct equivalent. The idiomatic translation is a callback-based async function:

```typescript
async function atomicOutput<T>(
  destDir: string,
  callback: (tempDir: string) => Promise<T>,
): Promise<T>
```

This preserves the "setup -> yield -> teardown" semantics with guaranteed cleanup in `finally`.

**`validateDestPath` is async:** Uses `fs/promises.lstat()` to check for symlinks, which is inherently async in Node.js. The Python version uses synchronous `Path.is_symlink()` -- the TypeScript version prefers non-blocking I/O.

**`rejectDangerousPath` is sync:** Only performs string comparisons against `process.cwd()`, `os.homedir()`, and the `PROTECTED_PATHS` set. No I/O required.

**`findResourcesDir` uses `import.meta.url`:** Equivalent to Python's `__file__`. Resolves to package root using `fileURLToPath` + path traversal. Must account for both source (`src/`) and bundled (`dist/`) layouts.

### 2.4 New Test Files

| File | Coverage Target |
|------|----------------|
| `tests/node/exceptions.test.ts` | `ConfigValidationError`, `PipelineError` construction and properties |
| `tests/node/utils.test.ts` | All utility functions, path validation edge cases, atomic output success/failure |

---

## 3. Existing Classes to Modify

| File | Change |
|------|--------|
| `src/exceptions.ts` | Add `ConfigValidationError` and `PipelineError` below existing `CliError`. No changes to `CliError` itself. |
| `src/utils.ts` | Add all new functions and the `PROTECTED_PATHS` constant. Existing `normalizeDirectory` and regex constants remain unchanged. |

No other files require modification for this story.

---

## 4. Dependency Direction Validation

```
src/exceptions.ts
    imports: nothing (pure Error subclasses, zero dependencies)

src/utils.ts
    imports: node:fs/promises (lstat, cp, rm, mkdtemp, realpath, stat)
    imports: node:os (tmpdir, homedir)
    imports: node:path (resolve, dirname, join)
    imports: node:url (fileURLToPath)
```

**Assessment: COMPLIANT.**

- `exceptions.ts` has zero dependencies -- pure domain-level error types.
- `utils.ts` depends only on Node.js standard library modules. No framework imports (commander, inquirer, nunjucks). No cross-module imports to `cli.ts`, `config.ts`, or assemblers.
- Both modules are "leaf" dependencies -- imported by others, import nothing from the project.

Dependency graph after STORY-002:

```
src/index.ts ──> src/exceptions.ts   (existing, CliError)
src/cli.ts   ──> (future stories will import utils)
src/assembler/ ──> src/utils.ts      (future: atomicOutput consumer)
src/config.ts  ──> src/exceptions.ts (future: ConfigValidationError consumer)

src/exceptions.ts ──> (nothing)
src/utils.ts      ──> (node:fs, node:os, node:path, node:url only)
```

No circular dependencies. No upward dependency violations.

---

## 5. Integration Points

| Integration Point | Consumer | When |
|-------------------|----------|------|
| `ConfigValidationError` | `src/config.ts` (config loader) | STORY-004 |
| `PipelineError` | `src/assembler/index.ts` (pipeline orchestrator) | STORY-016 |
| `atomicOutput` | Pipeline orchestrator | STORY-016 |
| `validateDestPath` + `rejectDangerousPath` | Called internally by `atomicOutput`; also usable standalone | This story (internal), STORY-016 (external) |
| `findResourcesDir` | Config loader, assemblers | STORY-004+ |
| `setupLogging` | CLI entry point | STORY-018 |

All integration points are **downstream** -- this story provides foundations consumed by later stories. No upstream dependencies beyond STORY-001 stubs.

---

## 6. Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| **Symlink detection platform differences** | Medium | Medium | `lstat()` behavior is consistent across platforms for symlink detection. Test with `fs.symlinkSync()` in test setup. On Windows CI, symlink creation may require elevated privileges -- skip symlink tests conditionally if `fs.symlinkSync` throws `EPERM`. |
| **`atomicOutput` partial failure leaves temp dir** | Low | Low | `finally` block ensures cleanup. Test explicitly that temp dir is removed even when callback throws. |
| **`atomicOutput` race condition on dest dir** | Low | Low | The Python version also has this race (rmtree + copytree is not truly atomic). Accept the same behavior parity. Document in JSDoc that this is "atomic" in the sense of all-or-nothing, not filesystem-level atomic. |
| **`findResourcesDir` path resolution differs between source and dist** | Medium | Medium | Must handle both `src/utils.ts` (development via `tsx`) and `dist/utils.js` (production via `tsup`). Use `import.meta.url` with upward traversal to package root, then append `resources/`. Verify with both `npm run dev` and `npm run build` + direct execution. |
| **`cp` recursive not available in older Node.js** | Low | Low | `fs/promises.cp` with `recursive: true` requires Node.js >= 16.7. Project requires Node >= 18 (per `package.json` engines). Safe. |
| **PROTECTED_PATHS is Unix-only** | Low | Medium | The Python original also uses Unix paths only. This is a CLI tool primarily targeting Unix environments. Accept parity. Document the Unix assumption. |
| **`process.cwd()` and `os.homedir()` mocking in tests** | Medium | Medium | Use `vi.spyOn` to mock `process.cwd` and `os.homedir` in Vitest. Alternatively, accept parameters for these in `rejectDangerousPath` for testability (but keep the public API simple with defaults). |
| **Coverage threshold with async operations** | Low | Low | Ensure all branches in `atomicOutput` are covered: success path, callback failure path, dest already exists path, dest does not exist path. |

---

## 7. Implementation Order

1. **`src/exceptions.ts`** -- Add `ConfigValidationError` and `PipelineError`
2. **`tests/node/exceptions.test.ts`** -- Unit tests for both exception classes
3. **`src/utils.ts`** -- Add `PROTECTED_PATHS`, `rejectDangerousPath`, `validateDestPath`, `atomicOutput`, `setupLogging`, `findResourcesDir`
4. **`tests/node/utils.test.ts`** -- Unit tests for all utility functions
5. **Verify** -- `npm run lint` (tsc --noEmit), `npm run test:coverage`, thresholds pass

Inner-to-outer: exceptions first (no dependencies), then utils (depends on node stdlib only), then tests.

---

## 8. Node.js API Usage Map

| Python API | Node.js Equivalent | Import |
|------------|-------------------|--------|
| `Path.is_symlink()` | `lstat()` then check `stat.isSymbolicLink()` | `node:fs/promises` |
| `Path.resolve()` | `realpath()` or `path.resolve()` | `node:fs/promises`, `node:path` |
| `Path.cwd()` | `process.cwd()` | global |
| `Path.home()` | `os.homedir()` | `node:os` |
| `tempfile.mkdtemp()` | `mkdtemp(path.join(os.tmpdir(), 'ia-dev-env-'))` | `node:fs/promises`, `node:os` |
| `shutil.copytree()` | `cp(src, dest, { recursive: true })` | `node:fs/promises` |
| `shutil.rmtree()` | `rm(path, { recursive: true, force: true })` | `node:fs/promises` |
| `Path.exists()` | `stat()` with try/catch or `access()` | `node:fs/promises` |
| `Path.is_dir()` | `stat()` then check `stat.isDirectory()` | `node:fs/promises` |
| `Path(__file__).resolve()` | `fileURLToPath(import.meta.url)` | `node:url` |
| `logging.basicConfig()` | `console.debug`/`console.info` level control | global |
| `frozenset` | `Object.freeze(new Set([...]))` or `ReadonlySet` type | native |
