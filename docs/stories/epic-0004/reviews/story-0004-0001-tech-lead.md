============================================================
 TECH LEAD REVIEW -- story-0004-0001
============================================================
 Decision:  GO
 Score:     38/40
 Critical:  0 issues
 Medium:    1 issue
 Low:       1 issue
------------------------------------------------------------

## A. Code Hygiene (8/8)

1. **No unused imports** -- PASS (1/1). All imports in `docs-adr-assembler.ts` are used: `fs`, `path`, `ProjectConfig`, `TemplateEngine`.
2. **No unused variables** -- PASS (1/1). All declared constants are referenced. `MANDATORY_SECTIONS` is used in `hasAllMandatorySections()`.
3. **No dead code** -- PASS (1/1). Every function and constant is exercised.
4. **No compiler/linter warnings** -- PASS (1/1). `npx tsc --noEmit` passes cleanly. No warnings.
5. **Method signatures correct** -- PASS (1/1). `assemble()` returns `string[]`, matching the `AssemblerDescriptor` interface contract. `getNextAdrNumber` returns `number`, `formatAdrFilename` returns `string`.
6. **No magic numbers/strings** -- PASS (1/1). All values extracted to named constants: `TEMPLATE_FILENAME`, `TEMPLATES_SUBDIR`, `ADR_OUTPUT_SUBDIR`, `README_FILENAME`, `ADR_FILE_PATTERN`, `ADR_NUMBER_PAD_WIDTH`, `ADR_TITLE_HEADING`, `MANDATORY_SECTIONS`.
7. **No TODO/FIXME left behind** -- PASS (1/1). No TODO/FIXME/HACK/XXX in production or test code.
8. **Clean formatting** -- PASS (1/1). No lines exceed 120 characters. Consistent 2-space indentation. Blank lines between logical sections.

## B. Naming (4/4)

9. **Intention-revealing names** -- PASS (1/1). `DocsAdrAssembler`, `getNextAdrNumber`, `formatAdrFilename`, `buildReadmeContent`, `hasAllMandatorySections` -- all clearly convey intent.
10. **No disinformation** -- PASS (1/1). Names accurately describe behavior. `getNextAdrNumber` returns the next number (not the current max). `formatAdrFilename` formats (not validates).
11. **Meaningful distinctions** -- PASS (1/1). No noise words or confusing pairs. `TEMPLATE_FILENAME` vs `README_FILENAME` are clearly distinct.
12. **Consistent vocabulary** -- PASS (1/1). Uses "ADR" consistently (not mixing "adr"/"ADR"/"ArchDecisionRecord"). Follows the `*Assembler` naming convention established by all other assemblers.

## C. Functions (5/5)

13. **Single responsibility** -- PASS (1/1). `buildReadmeContent` only builds content. `hasAllMandatorySections` only validates. `assemble` orchestrates I/O. `getNextAdrNumber` only scans. `formatAdrFilename` only formats.
14. **Size <= 25 lines** -- PASS (1/1). Longest function is `assemble()` at 18 lines (L59-L81). `buildReadmeContent` is 15 lines. `getNextAdrNumber` is 13 lines. `formatAdrFilename` is 10 lines.
15. **Max 4 parameters** -- PASS (1/1). `assemble()` has exactly 4 parameters (matching the established assembler interface). All others have 1-2.
16. **No boolean flag parameters** -- PASS (1/1). No boolean parameters anywhere.
17. **Command-Query separation** -- PASS (1/1). `buildReadmeContent` and `hasAllMandatorySections` are pure queries. `assemble` is a command that returns file paths (consistent with all other assemblers).

## D. Vertical Formatting (4/4)

18. **Blank lines between concepts** -- PASS (1/1). Module doc, imports, constants, private functions, class, and exported utility functions are separated by blank lines.
19. **Newspaper Rule** -- PASS (1/1). File flows: module doc -> imports -> constants -> private helpers -> public class -> exported utilities. High-level (class) before lower-level (utility functions).
20. **Class size <= 250 lines** -- PASS (1/1). File is 123 lines total. The class itself is ~24 lines.
21. **Related code grouped together** -- PASS (1/1). Constants grouped at top. Helper functions grouped before class. Exported utilities grouped at bottom.

## E. Design (3/3)

22. **Law of Demeter** -- PASS (1/1). No train wrecks. Direct property access: `config.project.name` (two dots, acceptable for configuration DTOs).
23. **CQS applied** -- PASS (1/1). Pure query functions separated from the `assemble()` command.
24. **DRY** -- PASS (1/1). `MANDATORY_SECTIONS` constant used in both validation logic and implicitly by tests. `ADR_FILE_PATTERN` used only once but extracted for readability and reuse by downstream stories.

## F. Error Handling (3/3)

25. **Rich exceptions with context** -- PASS (1/1). N/A for this assembler -- errors are handled by returning empty arrays (fail-safe pattern). The pipeline wrapper (`executeAssemblers`) catches unexpected errors and wraps them in `PipelineError` with assembler name and reason.
26. **No null returns** -- PASS (1/1). Returns `[]` (empty array) for degenerate cases. Never returns null or undefined.
27. **No generic catch-all** -- PASS (1/1). No try-catch in the assembler itself. Error propagation handled by the pipeline orchestrator.

## G. Architecture (4/5)

28. **SRP at class level** -- PASS (1/1). `DocsAdrAssembler` has one responsibility: generate the ADR directory structure. The utility functions (`getNextAdrNumber`, `formatAdrFilename`) are standalone exports for downstream stories -- appropriately co-located with the ADR module.
29. **DIP -- depends on abstractions** -- PASS (1/1). `assemble()` accepts `ProjectConfig` and `TemplateEngine` interfaces. Does not instantiate dependencies internally.
30. **Layer boundaries respected** -- PASS (1/1). Imports only from `models.js` and `template-engine.js` (same layer). Uses `node:fs` and `node:path` (standard library). No cross-layer violations.
31. **Follows implementation plan** -- FAIL (0/1). The `_engine` parameter is accepted but unused (prefixed with underscore per convention). The template is read directly via `fs.readFileSync` rather than through the engine. While this matches several other assemblers (settings, hooks, readme, protocols, github-mcp, github-hooks, codex-skills) that also ignore the engine parameter, the implementation plan specified using the engine for template rendering. However, since the ADR README content is built programmatically (not from a template), this is a reasonable design choice. **Marking as FAIL for plan deviation, but this is a MEDIUM severity finding -- the approach is valid and consistent with peer assemblers.**
32. **No circular dependencies** -- PASS (1/1). Clean dependency graph. `docs-adr-assembler.ts` imports from `models.js` and `template-engine.js` only.

## H. Framework & Infrastructure (4/4)

33. **DI used correctly** -- PASS (1/1). Assembler receives dependencies via method parameters. No service locator or hidden globals.
34. **Config externalized** -- PASS (1/1). Project name comes from `ProjectConfig`. Template path derived from `resourcesDir`. No hardcoded paths.
35. **Native-compatible** -- PASS (1/1). Uses only `node:fs` and `node:path` built-ins. No native add-ons or platform-specific code.
36. **Observability hooks present** -- PASS (1/1). N/A for a CLI template generator. Pipeline orchestrator provides timing and file-count metrics. Consistent with other assemblers.

## I. Tests (3/3)

37. **Coverage >= 95% line, >= 90% branch** -- PASS (1/1). QA report confirms 100% line, 100% branch, 100% function coverage on `docs-adr-assembler.ts`. Verified: the file appears as the only assembler with `100 | 100 | 100 | 100` in the coverage report.
38. **All acceptance criteria have tests** -- PASS (1/1). All 6 Gherkin scenarios from the story are covered:
    - AC #1 (template structure): `Template Structure Validation` describe block (10 tests)
    - AC #2 (docs/adr/ generation): `assemble_validConfig_generatesDocsAdrReadme`
    - AC #3 (project name placeholder): `assemble_validConfig_readmeContainsProjectName`, `template_containsProjectNamePlaceholder`
    - AC #4 (sequential numbering): `getNextAdrNumber` describe block (9 tests including parametrized)
    - AC #5 (missing sections rejected): `assemble_templateMissingMandatorySection_returnsEmptyArray`, `assemble_templateWithAllSectionsPresent_generatesReadme`
    - AC #6 (backward compatibility): `assemble_existingFilesInOutputDir_doesNotModifyThem`
39. **Test quality** -- PASS (1/1). All tests follow AAA pattern. Naming convention `method_scenario_expected`. Fresh `mkdtemp()` per test (no interdependency). Parametrized tests for both `getNextAdrNumber` (PT-1, 4 rows) and `formatAdrFilename` (PT-2, 5 rows). Edge cases covered: empty title, 5-digit overflow, non-ADR files in directory.

## J. Security & Production (1/1)

40. **Sensitive data protected, thread-safe** -- PASS (1/1). No sensitive data handled. `MANDATORY_SECTIONS` is `as const` (frozen). Node.js single-threaded with synchronous I/O (sequential, no race conditions). Security review scored 20/20.

------------------------------------------------------------

## Specialist Review Summary

| Specialist | Score | Status |
|-----------|-------|--------|
| Security | 20/20 | Approved |
| Performance | 26/26 | Approved |
| QA | 27/36 | Rejected |

### QA Findings Addressed

The QA review scored 27/36 with several CRITICAL/MAJOR findings. Assessment of each:

1. **AC #5 (missing mandatory sections) has no test** -- RESOLVED. Tests `assemble_templateMissingMandatorySection_returnsEmptyArray` and `assemble_templateWithAllSectionsPresent_generatesReadme` now exist at lines 142-228.
2. **Exception paths not tested** -- PARTIALLY VALID. The assembler uses a fail-safe pattern (return `[]`) rather than exceptions. The `templateMissing` and `templateMissingMandatorySection` tests cover the two fail-safe paths. Write-failure testing would require mocking `fs.writeFileSync` which adds minimal value for a CLI tool.
3. **Edge cases not covered** -- RESOLVED. Tests added for non-ADR files in directory (line 308), empty title (line 366), 5-digit number overflow (line 370).
4. **TDD commit pattern not visible** -- ACKNOWLEDGED. This is a process finding, not a code quality finding. Single-commit delivery makes TDD verification impossible from git history. No code impact.
5. **No backward compatibility E2E test** -- RESOLVED. Test `assemble_existingFilesInOutputDir_doesNotModifyThem` at line 232 validates AC #6.

------------------------------------------------------------

## Pipeline Integration

- `pipeline.ts`: DocsAdrAssembler registered at index 16 (before ReadmeAssembler) with `target: "root"` -- correct, since output goes to `docs/adr/` under the project root.
- `index.ts`: Barrel export added with story comment marker -- consistent with all other assemblers.
- `codex-config-assembler.test.ts`: Count updated from 17 to 18 assemblers -- verified.
- `pipeline.test.ts`: EXPECTED_ORDER updated, count assertion updated to 18, target verification test added (`buildAssemblers_docsAdrAssembler_hasRootTarget`).
- All 8 golden profiles updated with `docs/adr/README.md`.

## Findings

### MEDIUM -- Plan deviation: engine parameter unused (Item 31)

The `_engine` parameter is accepted but not used. The README content is built programmatically via `buildReadmeContent()` rather than through the template engine. This is consistent with 7 other assemblers that follow the same pattern, but deviates from the implementation plan which suggested using the engine. No code change needed -- the approach is valid and the `_` prefix correctly signals the unused parameter.

### LOW -- Empty title produces `ADR-0001-.md` (edge case)

`formatAdrFilename(1, "")` returns `ADR-0001-.md` (trailing hyphen before `.md`). This is tested and documented, but downstream consumers should validate title is non-empty. Low severity because this function is a utility for downstream stories and the edge case is acknowledged.

------------------------------------------------------------

## Summary

The implementation is clean, well-structured, and follows established patterns. The `DocsAdrAssembler` correctly integrates into the pipeline as assembler #17 (of 18), generates the `docs/adr/README.md` index file with project-specific content, and validates the ADR template for mandatory sections before proceeding. Utility functions `getNextAdrNumber` and `formatAdrFilename` are properly exported for downstream stories (story-0004-0006, story-0004-0015).

Code quality is high: 123-line production file with 8 named constants, 4 focused functions, and complete test coverage (40 tests, 100%/100%/100%). All 8 golden profiles updated. No compiler warnings. Security (20/20) and Performance (26/26) reviews both approved.

The two findings are minor: one is a plan deviation that matches existing patterns, and the other is a known edge case that is tested and documented. Neither blocks merge.

**Verdict: GO -- ready for merge.**
