# Tech Lead Review — story-0004-0007

```
============================================================
 TECH LEAD REVIEW -- story-0004-0007
============================================================
 Decision:  NO-GO
 Score:     37/40
 Critical:  1 issue
 Medium:    2 issues
 Low:       2 issues
------------------------------------------------------------
```

## Specialist Review Summary

| Engineer | Score | Status |
|----------|-------|--------|
| Security | 20/20 | Approved |
| Performance | 26/26 | Approved |
| DevOps | 20/20 | Approved |
| QA | 28/36 | Rejected |

The QA review identified three critical blockers related to TDD commit discipline (commits bundle tests and implementation together) and missing acceptance criterion coverage (AC5 non-REST exclusion, AC6 existing spec preservation). Performance, Security, and DevOps reviews found no issues.

---

## Section-by-Section Scoring

### A. Code Hygiene (8/8)

| # | Check | Score | Notes |
|---|-------|-------|-------|
| 1 | No unused imports | 2/2 | `github-skills-assembler.ts` imports exactly what is used: `fs`, `path`, `ProjectConfig`, `TemplateEngine`, `replacePlaceholdersInDir`. No dead imports. |
| 2 | No unused variables | 2/2 | All declared variables are consumed. No dead code. |
| 3 | No compiler warnings | 2/2 | `npx --no-install tsc --noEmit` passes with zero warnings. |
| 4 | No magic numbers/strings | 2/2 | Constants are named: `GITHUB_SKILLS_TEMPLATES_DIR`, `SKILL_MD`, `INFRA_GROUP`. The string `"references"` at lines 150 and 152 is used as a path segment -- acceptable as a structural convention, consistent with the same literal used in `copy-helpers.ts` and `skills-assembler.ts`. |

### B. Naming (4/4)

| # | Check | Score | Notes |
|---|-------|-------|-------|
| 5 | Intention-revealing names | 2/2 | `copyReferences` clearly expresses intent. Parameters `srcDir`, `skillDir`, `name`, `engine` are descriptive. `refsDir`, `destRefs` follow established abbreviation patterns in the codebase (`copy-helpers.ts` uses similar naming). |
| 6 | No disinformation | 2/2 | Method names accurately describe behavior. `copyReferences` copies references. `replacePlaceholdersInDir` replaces placeholders in a directory. No misleading names. |

### C. Functions (5/5)

| # | Check | Score | Notes |
|---|-------|-------|-------|
| 7 | Single responsibility | 1/1 | `copyReferences` does one thing: copies a reference directory tree with placeholder replacement. `renderSkill` delegates to `copyReferences` after writing the skill file. |
| 8 | Size <= 25 lines | 2/2 | `copyReferences`: 7 lines (144-155). `renderSkill`: 14 lines (121-142). `assemble`: 12 lines (72-90). `generateGroup`: 8 lines (105-119). `filterSkills`: 6 lines (93-103). All well under the 25-line limit. |
| 9 | Max 4 parameters | 1/1 | `copyReferences` takes 4 parameters: `srcDir`, `skillDir`, `name`, `engine`. At the limit but acceptable -- all are semantically distinct and necessary. `assemble` also takes 4: `config`, `outputDir`, `resourcesDir`, `engine`. |
| 10 | No boolean flag parameters | 1/1 | No boolean flags used as function parameters in new code. |

### D. Vertical Formatting (4/4)

| # | Check | Score | Notes |
|---|-------|-------|-------|
| 11 | Blank lines between concepts | 1/1 | Methods are separated by blank lines. Logical sections within methods are cleanly delineated. |
| 12 | Newspaper Rule | 1/1 | Public method `assemble` at top, private methods follow in call order: `filterSkills`, `generateGroup`, `renderSkill`, `copyReferences`. High-level to low-level. |
| 13 | Class size <= 250 lines | 1/1 | `github-skills-assembler.ts` is 156 lines total (including constants, imports, and exports). Well under the 250-line limit. |
| 14 | Related code proximity | 1/1 | `copyReferences` is placed immediately after `renderSkill` which calls it. Logical grouping is maintained. |

### E. Design (3/3)

| # | Check | Score | Notes |
|---|-------|-------|-------|
| 15 | Law of Demeter | 1/1 | No train wreck chains. `copyReferences` calls `fs.existsSync`, `fs.cpSync`, and `replacePlaceholdersInDir` -- all are direct operations on parameters, not chained object traversals. |
| 16 | CQS (Command-Query Separation) | 1/1 | `copyReferences` is a command (returns `void`, produces side effects). `filterSkills` is a query (returns filtered array). Clean separation. |
| 17 | DRY | 1/1 | The `copyReferences` pattern (`fs.cpSync` + `replacePlaceholdersInDir`) mirrors `copyTemplateTree` in `copy-helpers.ts` (line 47-54). This is not duplication but intentional reuse of the same pattern in a different context. `copyTemplateTree` is a standalone function for full directory trees; `copyReferences` is a private method that handles the specific sub-case of reference subdirectories within a skill that was rendered via a different mechanism (single-file `renderSkill`). The QA review suggested extracting a shared function, but this would create unnecessary coupling between the assemblers. The current approach is acceptable. |

### F. Error Handling (2/3)

| # | Check | Score | Notes |
|---|-------|-------|-------|
| 18 | Rich exceptions with context | 1/1 | Not directly applicable -- `copyReferences` uses a fail-safe pattern (early return if directory does not exist). Underlying `fs.cpSync` and `replacePlaceholdersInDir` will throw with OS-level context if file operations fail. This is consistent with all other assemblers in the codebase. |
| 19 | No null returns | 1/1 | `copyReferences` returns `void`. The existing `renderSkill` returns `null` for missing source files (line 129), which is the established pattern, and the caller filters nulls (line 116). |
| 20 | No generic catch | 0/1 | **[LOW]** `copyReferences` has no error handling for `fs.cpSync` failures (e.g., permission denied, disk full). If `fs.cpSync` throws at line 153, the entire `assemble` call will crash with a raw Node.js filesystem error. While this matches the existing pattern in `copyTemplateTree` at `copy-helpers.ts:52` (which also has no try/catch), best practice would be to catch and wrap with context (e.g., source path, destination path, skill name). This is a pre-existing pattern issue, not a regression introduced by this story. |

### G. Architecture (5/5)

| # | Check | Score | Notes |
|---|-------|-------|-------|
| 21 | SRP at class level | 1/1 | `GithubSkillsAssembler` has one responsibility: assembling GitHub skill files from templates. `copyReferences` is a natural extension of this responsibility. |
| 22 | DIP | 1/1 | `copyReferences` depends on `TemplateEngine` (abstraction), not on concrete implementations. File system access uses Node.js built-in APIs, consistent with the entire assembler layer. |
| 23 | Layer boundaries | 1/1 | The new code lives in `src/assembler/` which is the correct layer for file assembly operations. No domain layer violations. |
| 24 | Follows implementation plan | 1/1 | The plan (section 5.3) recommended Option 1: "Add references support to `GithubSkillsAssembler.renderSkill()` to also copy reference files." This is exactly what was implemented. |
| 25 | Cross-file consistency | 1/1 | **Key finding:** `copyReferences` follows the same `fs.cpSync` + `replacePlaceholdersInDir` pattern established in `copyTemplateTree` at `copy-helpers.ts:47-54`. The method signature is consistent with the class's private method style. The import of `replacePlaceholdersInDir` from `./copy-helpers.js` is correct and consistent with how other assemblers use the function. |

### H. Framework & Infra (4/4)

| # | Check | Score | Notes |
|---|-------|-------|-------|
| 26 | DI / constructor injection | 1/1 | `TemplateEngine` is passed as a parameter, not instantiated internally. Consistent with existing pattern. |
| 27 | Externalized config | 1/1 | `resourcesDir` is passed as a parameter. No hardcoded paths. Template placeholders use the existing `buildDefaultContext()` mechanism. |
| 28 | Native-compatible | 1/1 | No native-incompatible APIs. Uses `fs.cpSync` (stable since Node 16.7), `fs.existsSync`, `fs.mkdirSync` -- all standard Node.js APIs. |
| 29 | Observability | 1/1 | N/A for CLI tool. The assembler returns file paths in the results array for traceability. |

### I. Tests (6/8)

| # | Check | Score | Notes |
|---|-------|-------|-------|
| 30 | Coverage thresholds met | 2/2 | Full suite: 99.5% lines, 97.67% branches. `github-skills-assembler.ts`: 100% stmts, 100% branches. Well above 95%/90% thresholds. |
| 31 | Scenarios covered | 2/4 | **[CRITICAL]** 123 tests pass across 3 test files. Coverage of the new `copyReferences` method is thorough: copies references, no references (early return), placeholder replacement, multiple files, nested lib group. Content tests validate 27 assertions for Claude template and 9 for GitHub template, plus 13 dual-copy consistency assertions. Pipeline integration tests verify all 7 REST profiles + python-click-cli. **However**, the pipeline tests for python-click-cli validate the "unconditional copy with runtime skip" approach (the template IS present in non-REST output with skip instructions in its content), but the test plan's original IT-2 (`pipelineOutput_pythonClickCli_excludesOpenapiGenerator`) was replaced without updating the test plan document. Also, there is no test for AC6 ("existing spec not overwritten" / incremental update) -- though this is a runtime behavior of the AI agent, not the pipeline, the acceptance criterion exists and should at minimum be validated by a content test asserting the template contains incremental update instructions. |
| 32 | Test naming convention | 2/2 | All tests follow `[methodUnderTest]_[scenario]_[expectedBehavior]` format. Examples: `renderSkill_withReferencesDir_copiesReferencesToOutput`, `templateContent_containsOpenAPI31Requirement_specVersion`, `pipelineSuccess_go-gin`. Parametrized tests use `%s` substitution correctly (e.g., `templateContent_containsHTTPMethod_%s`). |

### J. Security & Production (1/1)

| # | Check | Score | Notes |
|---|-------|-------|-------|
| 33 | Sensitive data protected | 0.5/0.5 | No sensitive data in any changed file. Template contains only documentation patterns. |
| 34 | Thread-safe | 0.5/0.5 | Single-threaded CLI tool. No shared mutable state. Sync I/O by design. |

---

## Cross-File Consistency Analysis

### 1. `copyReferences()` vs `copyTemplateTree()` Pattern Consistency

**Verdict: CONSISTENT**

| Aspect | `copyTemplateTree` (copy-helpers.ts:47-54) | `copyReferences` (github-skills-assembler.ts:144-155) |
|--------|---------------------------------------------|-------------------------------------------------------|
| Guard clause | N/A (separate `IfExists` variant) | `if (!fs.existsSync(refsDir)) return` (line 151) |
| Copy mechanism | `fs.cpSync(src, dest, { recursive: true })` | `fs.cpSync(refsDir, destRefs, { recursive: true })` |
| Placeholder replacement | `replacePlaceholdersInDir(dest, engine)` | `replacePlaceholdersInDir(destRefs, engine)` |
| Pattern | Copy-then-walk | Copy-then-walk |

The pattern is identical. `copyReferences` adds an `existsSync` guard (similar to `copyTemplateTreeIfExists`) because the references directory is optional for any given skill.

### 2. Test Naming Convention Compliance

**Verdict: COMPLIANT**

All 123 tests across the 3 files follow the `[methodUnderTest]_[scenario]_[expectedBehavior]` convention. Parametrized tests use `it.each` with `%s` substitution correctly (e.g., `templateContent_containsHTTPMethod_%s`).

### 3. Template Dual Copy Consistency (RULE-001)

**Verdict: CONSISTENT**

The Claude and GitHub source templates are byte-for-byte identical (verified via `diff`). After pipeline processing, the golden files differ only in placeholder-resolved values (`{project_name}`, `{framework_name}`, `{language_name}`) which are profile-specific. The dual-copy consistency tests validate 13 assertions covering: OpenAPI 3.1, `$ref`, output path, YAML, RFC 7807, HTTP methods, adapter scanning, DTO extraction, and error handling.

### 4. Golden Files Correctness

**Verdict: CORRECT WITH DESIGN NOTE**

All 8 profiles (including python-click-cli) receive the `openapi-generator.md` reference in all three outputs (`.claude/`, `.agents/`, `.github/`). The python-click-cli golden files contain the template with resolved placeholders specific to that profile (e.g., `click` for framework_name, `python` for language_name). The `.claude/` and `.github/` golden files for a given profile are identical (verified via diff for java-spring). This is correct because the core skill template is the same source, just distributed to different output targets.

The template itself contains "Skip when: No REST interface is configured" as a runtime instruction for the AI agent, which satisfies AC5 via Scenario B (unconditional inclusion with runtime skip documentation).

### 5. Assembler Change Backward Compatibility

**Verdict: NO REGRESSION**

The `copyReferences` method is called at the end of `renderSkill` (line 140). If no `references/{name}` directory exists in the source templates (which is true for all skills except `x-dev-lifecycle`), the method returns immediately at line 151 (`if (!fs.existsSync(refsDir)) return`). This means existing behavior for all other skills is completely unchanged -- only a single `fs.existsSync` check is added per skill, with negligible performance impact.

---

## Issue Details

### CRITICAL-1: Missing AC6 Content Test (Incremental Update)

**Location:** `tests/node/content/openapi-generator-content.test.ts`

**Description:** Story acceptance criterion AC6 requires validation that an existing `docs/api/openapi.yaml` is preserved/updated rather than overwritten. The template DOES contain this content (Section 6.1 "Incremental Updates" at lines 329-334 of the Claude template), but there is no test asserting its presence. While the actual incremental behavior is a runtime concern of the AI agent (not the pipeline), the content test should verify the template instructs the agent to preserve existing endpoints.

**Severity:** CRITICAL -- acceptance criterion without test coverage.

**Fix:** Add a content test such as:
```
it("templateContent_containsIncrementalUpdate_preserveExistingEndpoints", () => {
  const content = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
  expect(content).toMatch(/[Pp]reserve/);
  expect(content).toMatch(/[Ii]ncremental/);
});
```

**Note:** This test already exists in the Claude source template section (line 137-141) but the QA review flagged it as missing for AC6 specifically. Upon re-reading the content test file, I confirm `templateContent_containsIncrementalUpdate_preserveExistingEndpoints` IS present at line 137. The QA review's AC6 finding was incorrect -- the test does exist. **Downgrading from CRITICAL to RESOLVED.** Score adjustment: +2 to section I.

**REVISED:** After re-reading the test file, the test `templateContent_containsIncrementalUpdate_preserveExistingEndpoints` at line 137 DOES cover AC6. The QA review missed this. This issue is **RESOLVED**.

### MEDIUM-1: TDD Commit Discipline Not Demonstrated

**Location:** Git history

**Description:** The QA review correctly identified that commit `f73ff68` bundles tests and implementation together. The TDD [Red-Green-Refactor] workflow requires separate commits: (1) tests only (RED), (2) implementation (GREEN), (3) optional refactoring. The commit history shows:
1. `f73ff68` feat: add OpenAPI/Swagger documentation generator template [TDD] -- bundles everything
2. `53d97a0` fix: add missing golden files [TDD]
3. `e770c06` test: add missing AC tests and pipeline integration tests [TDD]

The `e770c06` commit adds the pipeline integration test file (`openapi-generator-pipeline.test.ts`) and potentially the incremental update content test -- these are post-implementation tests, which is the opposite of TDD.

**Severity:** MEDIUM -- process violation, not a code quality issue. The tests themselves are well-structured and thorough.

**Fix:** In future stories, split into separate commits: first commit the failing tests (RED), then commit the implementation (GREEN), then commit any refactoring.

### MEDIUM-2: Missing Edge Case Tests for `copyReferences`

**Location:** `tests/node/assembler/github-skills-assembler.test.ts`

**Description:** The QA review identified missing edge case coverage for `copyReferences`:
- Empty references directory
- Non-.md files in references directory (should be copied without placeholder replacement)
- References directory with nested subdirectories

The current tests cover: references present, references absent, placeholder replacement, multiple files, nested lib group. These are the primary scenarios, but the edge cases would strengthen confidence.

**Severity:** MEDIUM -- not a regression risk, but reduces confidence in the `copyReferences` method's robustness for future use cases.

**Fix:** Add 2-3 edge case tests in the "references" describe block.

### LOW-1: No Error Wrapping for `fs.cpSync` Failures

**Location:** `src/assembler/github-skills-assembler.ts:153`

**Description:** If `fs.cpSync` throws (permission denied, disk full, symlink loop), the error propagates as a raw Node.js filesystem error without context about which skill or reference directory failed. This is consistent with `copyTemplateTree` in `copy-helpers.ts:52` which also has no wrapping, so it is a pre-existing pattern, not a regression.

**Severity:** LOW -- pre-existing pattern, no regression.

**Fix:** Consider wrapping in a try/catch that adds skill name and path context. This is an improvement for the entire assembler layer, not specific to this story.

### LOW-2: Repetitive `fs.readFileSync` Calls in Content Tests

**Location:** `tests/node/content/openapi-generator-content.test.ts`

**Description:** Each test case calls `fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8")` or `fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8")` independently. While this ensures test isolation (no shared mutable state), it results in approximately 50+ redundant file reads during the test run. The content tests in `x-story-create-content.test.ts` use a similar pattern, so this is consistent with existing codebase conventions. However, since the file is read-only and never modified during tests, reading once at the describe scope level (as shown in the test plan's suggested pattern at section 8) would be cleaner.

**Severity:** LOW -- performance overhead is negligible for a single small file, and the pattern is consistent with existing tests.

**Fix:** Optional. Could extract `const content = fs.readFileSync(...)` to the describe scope.

---

## Revised Scoring After Analysis

Upon detailed analysis:
- AC6 IS covered by `templateContent_containsIncrementalUpdate_preserveExistingEndpoints` at line 137 (QA review missed this)
- AC5 is covered by the pipeline integration test `claudeOutput_includesTemplate_coreSkillUnconditional` + `templateContent_containsSkipInstruction_runtimeGuard` (Scenario B approach)
- The critical TDD commit discipline issue remains as a MEDIUM process concern

| Section | Points | Score |
|---------|--------|-------|
| A. Code Hygiene | 8 | 8/8 |
| B. Naming | 4 | 4/4 |
| C. Functions | 5 | 5/5 |
| D. Vertical Formatting | 4 | 4/4 |
| E. Design | 3 | 3/3 |
| F. Error Handling | 3 | 2/3 |
| G. Architecture | 5 | 5/5 |
| H. Framework & Infra | 4 | 4/4 |
| I. Tests | 3 | 2/3 |
| J. Security & Production | 1 | 1/1 |
| **Total** | **40** | **38/40** |

```
============================================================
 TECH LEAD REVIEW -- story-0004-0007 (REVISED)
============================================================
 Decision:  NO-GO
 Score:     38/40
 Critical:  0 issues (AC6 finding resolved upon analysis)
 Medium:    2 issues
 Low:       2 issues
------------------------------------------------------------
```

## Decision Rationale

**NO-GO** -- Despite the clean code quality (38/40), two medium issues prevent approval:

1. **TDD commit discipline (MEDIUM-1):** The commit history does not demonstrate Red-Green-Refactor. Tests and implementation are bundled in a single commit, and pipeline integration tests were added in a later commit after implementation. This violates the project's TDD workflow as documented in the quality gates. While the tests are comprehensive and correct, the process evidence is absent.

2. **Missing edge case tests (MEDIUM-2):** The `copyReferences` method lacks tests for empty directories, non-.md files, and nested subdirectories. These are important for future-proofing the method as more skills gain references.

## Required Fixes for GO

1. **Add edge case tests** for `copyReferences`:
   - `renderSkill_emptyReferencesDir_createsEmptyReferencesDir` (or skips, depending on `fs.cpSync` behavior)
   - `renderSkill_nonMdFilesInReferences_copiedWithoutPlaceholderReplacement`

2. **Acknowledge TDD process gap** -- either restructure commits (if feasible) or document the deviation. Since the code quality is high and all tests pass with excellent coverage, this can be accepted as a one-time process exception with a commitment to proper Red-Green-Refactor in future stories.

## Compilation & Test Verification

- `npx --no-install tsc --noEmit`: PASS (zero errors)
- `npx vitest run` (full suite): 1825 tests passed across 56 files
- Coverage: 99.5% lines, 97.67% branches (above 95%/90% thresholds)
- Story-specific tests: 123/123 passed
