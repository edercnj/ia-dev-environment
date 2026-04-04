# Performance Review — story-0004-0007

```
ENGINEER: Performance
STORY: story-0004-0007
SCORE: 26/26
STATUS: Approved
---
PASSED:
- [1] No N+1 queries (2/2) — N/A. No database queries. This is a CLI template assembler with file system operations only.
- [2] Connection pool sized (2/2) — N/A. No database or connection pooling. CLI tool operates on local file system.
- [3] Async where applicable (2/2) — Sync I/O is intentional and correct. The `copy-helpers.ts` module documents this: "All operations use synchronous node:fs by design. This module is consumed exclusively by CLI assemblers that run sequentially. Sync I/O simplifies control flow and matches the Python predecessor's behavior for output parity." The new `copyReferences()` method correctly follows this established pattern using `fs.cpSync()`, `fs.existsSync()`, and `replacePlaceholdersInDir()`. For a CLI tool that runs sequentially, sync I/O avoids unnecessary complexity with no throughput penalty.
- [4] Pagination on collections (2/2) — N/A. No paginated collections. The assembler iterates a small, bounded set of skill groups defined in `SKILL_GROUPS` constant.
- [5] Caching strategy (2/2) — N/A. CLI tool runs once per invocation. No caching needed; each run generates output from templates.
- [6] No unbounded lists (2/2) — All collections are bounded. `SKILL_GROUPS` is a compile-time constant with fixed entries. The `references/` directory contents are bounded by the template tree (single-digit file count per skill). The `walkAndReplace()` recursion in `replacePlaceholdersInDir` is bounded by directory depth (2-3 levels max).
- [7] Timeout on external calls (2/2) — N/A. No external calls (no network, no HTTP, no gRPC). All operations are local file system reads/writes.
- [8] Circuit breaker on external (2/2) — N/A. No external dependencies to protect with circuit breakers.
- [9] Thread safety (2/2) — N/A. Single-threaded CLI tool. Node.js event loop is single-threaded and the assembler runs sequentially. No shared mutable state across concurrent contexts.
- [10] Resource cleanup (2/2) — File handles are properly managed. All file operations use synchronous `fs.readFileSync`/`fs.writeFileSync`/`fs.cpSync` which open and close file descriptors within the call. No lingering file handles. Test cleanup is properly handled with `afterEach(() => fs.rmSync(tmpDir, { recursive: true, force: true }))`.
- [11] Lazy loading (2/2) — The `copyReferences()` method uses early return (`if (!fs.existsSync(refsDir)) return`) to skip unnecessary work when no references directory exists. This means the vast majority of skills (those without references) incur only a single `existsSync` check — effectively lazy/short-circuit behavior.
- [12] Batch operations (2/2) — `fs.cpSync(refsDir, destRefs, { recursive: true })` copies the entire reference directory tree in a single OS-level recursive copy operation, then `replacePlaceholdersInDir()` walks the result once. This is the optimal approach — a single recursive copy followed by a single walk, rather than individual file-by-file copy-and-replace.
- [13] Index usage (2/2) — N/A. No database operations. File lookups use deterministic path construction (string concatenation), not directory scanning for discovery.
```

## Analysis Summary

This story introduces:
1. **New Markdown template files** — Zero runtime performance impact. These are static content copied during generation.
2. **`copyReferences()` method in `GithubSkillsAssembler`** — 14 lines of code that follow the exact same I/O pattern established by `copyTemplateTree()` in `copy-helpers.ts`. Uses `fs.cpSync()` for recursive copy (single syscall) followed by `replacePlaceholdersInDir()` for placeholder substitution. Short-circuits with early return when no references directory exists.
3. **New test files** — No production performance impact. Test cleanup is properly handled.
4. **Updated golden files** — Static test fixtures with no runtime impact.

### Performance Characteristics of `copyReferences()`

- **Best case (no references):** 1x `fs.existsSync()` — negligible overhead (~0.1ms)
- **Worst case (references exist):** 1x `fs.existsSync()` + 1x `fs.cpSync()` + 1x `replacePlaceholdersInDir()` walk — bounded by file count (currently 1 file per skill with references)
- **Memory:** No in-memory buffering of large data. Files are read, transformed, and written individually by `walkAndReplace()`.
- **I/O pattern:** Matches existing `copyTemplateTree()` exactly. No regression risk.

No performance concerns identified.
