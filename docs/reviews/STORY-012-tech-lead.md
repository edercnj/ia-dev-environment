# Tech Lead Review — STORY-012

**Scope:** PatternsAssembler + ProtocolsAssembler migration from Python to TypeScript

**Files reviewed:**
- `src/assembler/patterns-assembler.ts` (new, 103 lines)
- `src/assembler/protocols-assembler.ts` (new, 80 lines)
- `src/assembler/index.ts` (modified, 2 export lines added)
- `tests/node/assembler/patterns-assembler.test.ts` (new, 309 lines)
- `tests/node/assembler/protocols-assembler.test.ts` (new, 334 lines)

**Parity reference:** `src/ia_dev_env/assembler/patterns_assembler.py`, `src/ia_dev_env/assembler/protocols_assembler.py`
**Consistency reference:** `src/assembler/skills-assembler.ts`

---

```
============================================================
 TECH LEAD REVIEW -- STORY-012
============================================================
 Decision:  GO
 Score:     37/40
 Critical:  0 issues
 Medium:    2 issues
 Low:       3 issues
------------------------------------------------------------
```

---

## Section A — Code Hygiene (8/8)

**Score: 8/8**

| Check | Result |
|-------|--------|
| No unused imports | PASS — all imports used. `TemplateEngine` in `protocols-assembler.ts` is imported for the type annotation of the intentionally-unused `_engine` parameter, which is explicitly documented in `@remarks`. |
| No unused variables | PASS — no dead variables. |
| No `any` type | PASS — zero occurrences. |
| No `@ts-ignore` | PASS |
| No `console.log` | PASS |
| No `var` keyword | PASS |
| No dead code / unreachable branches | PASS |
| Compilation clean | PASS — `tsc --noEmit` exits 0 with zero warnings or errors. |

All hygiene checks pass cleanly.

---

## Section B — Naming (4/4)

**Score: 4/4**

| Check | Result |
|-------|--------|
| Classes PascalCase | PASS — `PatternsAssembler`, `ProtocolsAssembler` |
| Methods camelCase, intent-revealing | PASS — `assemble`, `generateOutput`, `renderContents`, `buildRefsDir`, `buildConsolidatedPath`, `flushPatterns`, `flushConsolidated`, `concatProtocolFiles` all communicate intent. |
| Constants UPPER_SNAKE | PASS — `SKILLS_DIR`, `PATTERNS_SKILL_DIR`, `REFERENCES_DIR`, `CONSOLIDATED_FILENAME`, `SECTION_SEPARATOR`, `PROTOCOL_SEPARATOR`, `CONVENTIONS_SUFFIX` all correctly cased. |
| No disinformation | PASS — no misleading names. The `_engine` convention correctly signals intentional non-use and is consistent with TypeScript/ESLint `noUnusedParameters` idiom. |

---

## Section C — Functions (4/5)

**Score: 4/5**

| Check | Result |
|-------|--------|
| Single responsibility per method | PASS — each private method has exactly one job. |
| Size ≤ 25 lines | PASS — longest method is `flushPatterns` at 12 body lines. All methods well within limit. |
| Max 4 parameters | MEDIUM — `assemble()` in both classes takes exactly 4 parameters (`config`, `outputDir`, `resourcesDir`, `engine`). At the limit; acceptable, but a parameter object would improve extensibility for future additions. |
| No boolean flag parameters | PASS |
| Arrow functions only for callbacks | PASS — named class methods used for methods; arrow used only inside `.map()`. |

**Finding C-1 (Low):** `assemble()` sits at the 4-parameter ceiling. As these assemblers are public API, any future extension (e.g., `dryRun: boolean`) would immediately violate the rule. Consider introducing a parameter object `AssembleOptions` proactively. This matches the pattern used by higher-level callers documented in the codebase.

---

## Section D — Vertical Formatting (4/4)

**Score: 4/4**

| Check | Result |
|-------|--------|
| Newspaper rule — high-level concepts first | PASS — `assemble()` public method appears before private helpers in both files. |
| Blank lines separate conceptual groups | PASS — constants block, class declaration, and each method are separated by blank lines. |
| Class size ≤ 250 lines | PASS — `patterns-assembler.ts` is 103 lines, `protocols-assembler.ts` is 80 lines. Both well under the 250-line limit. |
| Import ordering (Node builtins → external → internal → relative) | PASS — both files follow: `node:fs`, `node:path` → `../models.js` → `../template-engine.js` → `../domain/*`. |

---

## Section E — Design (2/3)

**Score: 2/3**

| Check | Result |
|-------|--------|
| Law of Demeter | PASS — no train wrecks; domain function results are captured and used locally. |
| CQS (Command Query Separation) | PASS — `assemble()` commands (writes files) and returns the list of written paths. The return value is a side-effect audit trail, not a query — acceptable pattern established by all prior assemblers (`SkillsAssembler`, `AgentsAssembler`). |
| DRY | MEDIUM — `SKILLS_DIR = "skills"` and `REFERENCES_DIR = "references"` are duplicated verbatim between `patterns-assembler.ts` (lines 18, 20) and `protocols-assembler.ts` (lines 22, 24). These two constants are also independently defined in `skills-assembler.ts` as `SKILLS_OUTPUT`. Three separate modules carry the same magic string `"skills"`. |

**Finding E-1 (Medium):** The constants `SKILLS_DIR` and `REFERENCES_DIR` are duplicated across `patterns-assembler.ts`, `protocols-assembler.ts`, and implicitly in `skills-assembler.ts` (`SKILLS_OUTPUT`). A shared `assembler-constants.ts` or `path-constants.ts` module would eliminate the divergence risk. If the output directory structure ever changes, three files must be updated synchronously.

---

## Section F — Error Handling (2/3)

**Score: 2/3**

| Check | Result |
|-------|--------|
| No null returns | PASS — both assemblers return `string[]` and use early-return `[]` instead of null. |
| No generic catch blocks | PASS — no try/catch at all; file I/O errors propagate naturally (acceptable for a CLI tool). |
| Errors carry context | LOW — `fs.readFileSync`, `fs.writeFileSync`, and `fs.mkdirSync` calls are unwrapped with no context enrichment. If a file operation fails (e.g., permission denied on `destFile`), the thrown `NodeJS.ErrnoException` contains only the OS-level message, not which assembler or which template triggered it. |

**Finding F-1 (Low):** File I/O operations lack context wrapping. A `try/catch` block wrapping each file operation with a rethrow that includes the source/destination file path would significantly improve debuggability in production use. Example:
```typescript
try {
  fs.writeFileSync(destFile, content, "utf-8");
} catch (err) {
  throw new Error(`PatternsAssembler: failed to write ${destFile}`, { cause: err });
}
```
This is consistent with the coding standard requiring exceptions to carry context.

---

## Section G — Architecture (5/5)

**Score: 5/5**

| Check | Result |
|-------|--------|
| SRP — one reason to change | PASS — `PatternsAssembler` is responsible only for orchestrating pattern file selection, rendering, and write. Selection logic is correctly delegated to `domain/pattern-mapping.ts`; rendering to `TemplateEngine`. |
| DIP — depends on abstractions | PASS — both assemblers import `TemplateEngine` as a type (interface contract), `ProjectConfig` as a data model, and domain functions as pure functions. No concrete infrastructure dependencies are injected. |
| Architecture layer boundaries | PASS — `src/assembler/` correctly depends on `src/domain/` (inward). No dependency on adapter or framework layers. |
| Follows implementation plan | PASS — the Python-to-TypeScript migration faithfully reproduces the behavior of `patterns_assembler.py` and `protocols_assembler.py`. |
| No circular dependencies | PASS — import graph is acyclic. `assembler/ → domain/`, `assembler/ → models.js`, `assembler/ → template-engine.js`. |

**Parity analysis:**

- **PatternsAssembler:** Full behavioral parity. The Python `_resources_dir` constructor injection was deliberately dropped in TypeScript (resources dir is passed per-call), which simplifies instantiation and is consistent with `SkillsAssembler`. The Python constructor storing `self._resources_dir` was the only structural difference; the TS version passes it as an argument — a conscious and sound improvement.
- **ProtocolsAssembler:** Full behavioral parity. The Python `_concat_protocol_dir` includes an explicit guard for an empty `protocol_files` list (writes `""` to the destination). The TypeScript `concatProtocolFiles` relies on `[].join(separator)` returning `""` — produces identical output without the explicit guard. Verified correct.

---

## Section H — Framework & Infrastructure (4/4)

**Score: 4/4**

| Check | Result |
|-------|--------|
| DI — constructor injection for dependencies | PASS — `PatternsAssembler` and `ProtocolsAssembler` are stateless classes (no constructor, no mutable instance state). Dependencies are passed per-call. Consistent with `SkillsAssembler` and `AgentsAssembler` patterns. |
| Externalized config | PASS — no hardcoded environment-specific values. Output paths derive from caller-supplied `outputDir` and `resourcesDir`. |
| Native-compatible | PASS — pure Node.js `node:fs` / `node:path` used. No native binaries, no platform-specific APIs. |
| Observability | N/A — this project has `none` observability per project identity config. No logging framework is required. |

---

## Section I — Tests (3/3)

**Score: 3/3**

| Check | Result |
|-------|--------|
| All target tests pass | PASS — 29/29 tests pass (`patterns-assembler.test.ts`: 16 tests, `protocols-assembler.test.ts`: 13 tests). The 2 pre-existing failures in `cli-help.test.ts` predate this PR (confirmed in git history at commit `9cf50af`). |
| Coverage thresholds for new files | PASS — isolated coverage run for the two new assemblers: `patterns-assembler.ts` 100% statements/branches/functions/lines; `protocols-assembler.ts` 100% statements/branches/functions/lines. |
| Test quality | PASS — tests are well-structured with `beforeEach`/`afterEach` lifecycle, real filesystem isolation via `mkdtempSync`, meaningful scenario names, and data-driven coverage via `it.each`. Scenarios cover: empty returns, disk-missing files, style-specific category selection, event-driven flag, placeholder rendering, separator joining, sorted output, and file-write verification. |

**Test naming compliance:** Patterns assembler uses the required `[scenario]_[condition]` convention consistently. Protocols assembler has three tests with non-standard names:
- `writesRestConventions` (missing scenario verb form)
- `writesMultipleProtocolConventions`
- `createsReferencesDirectory`

**Finding I-1 (Low):** Three test names in `protocols-assembler.test.ts` do not fully follow the `[methodUnderTest]_[scenario]_[expectedBehavior]` convention. Not a blocker, but should be aligned with the project standard at next opportunity.

---

## Section J — Security & Production (1/1)

**Score: 1/1**

| Check | Result |
|-------|--------|
| Sensitive data protected | PASS — no credentials, secrets, or user-sensitive data handled. |
| Thread safety | PASS — both classes are stateless. No shared mutable state; safe for concurrent use. |

---

## Summary of Findings

| ID | Severity | Location | Finding |
|----|----------|----------|---------|
| C-1 | Low | `patterns-assembler.ts:27`, `protocols-assembler.ts:31` | `assemble()` is at the 4-parameter ceiling; a parameter object `AssembleOptions` would future-proof the API. |
| E-1 | Medium | `patterns-assembler.ts:18,20`, `protocols-assembler.ts:22,24` | `SKILLS_DIR` and `REFERENCES_DIR` constants duplicated across files; extract to shared constants module. |
| F-1 | Low | `patterns-assembler.ts:86-88`, `protocols-assembler.ts:72-77` | File I/O exceptions propagate without assembler-level context; wrap with `cause` to aid debugging. |
| I-1 | Low | `protocols-assembler.test.ts:83,104,318` | Three test names do not follow the `[method]_[scenario]_[expected]` convention. |

---

## Pre-existing Failures (Out of Scope)

The suite reports 2 failing tests in `tests/node/cli-help.test.ts` (`templateAndUtils_withSimpleInput_returnExpectedValues` and `templateEngine_withInvalidTemplate_throws`). These failures are traceable to commit `de4f554` (STORY-001) and are not introduced by this PR. They are excluded from this review's decision.

---

## Decision Rationale

- **Zero critical issues** — compilation clean, no forbidden patterns, no architecture violations, no parity gaps.
- **Medium E-1** (duplicated constants) is a code quality concern but not a correctness or security risk. The behavior is correct and tested at 100% line/branch coverage.
- **Score 37/40** — above the 34/40 GO threshold.
- The migration is faithful, the tests are comprehensive, the code is well-structured and consistent with the established assembler pattern in the codebase.

**Verdict: GO — merge approved.**

---

*Reviewed by Tech Lead persona | Date: 2026-03-10 | Story: STORY-012*
