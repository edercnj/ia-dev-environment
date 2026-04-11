# Task Plan -- TASK-0034-0004-003

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0004-003 |
| Story ID | story-0034-0004 |
| Epic ID | 0034 |
| Source Agent | merged(Architect, Security Engineer, Tech Lead, Product Owner) |
| Type | implementation (edit) + integrity-gate |
| TDD Phase | GREEN |
| Layer | application + config |
| Estimated Effort | M (~3-4 hours) |
| Date | 2026-04-10 |

## Objective

Stop copying plan templates to `.github/templates/` from both `PlanTemplatesAssembler` and `EpicReportAssembler`. The shared template source files under `java/src/main/resources/shared/templates/` are PROTECTED (RULE-004) and must remain byte-for-byte intact across the entire edit. This task owns the single most important invariant of the story.

## Implementation Guide

**RULE-004 PRE-FLIGHT CHECK** (run before any edit):
```
baseline_count=$(find java/src/main/resources/shared/templates -type f | wc -l)
echo "Baseline template count: $baseline_count"  # expected: 57
```
Record the number. Any subsequent step must not reduce it.

1. **PlanTemplatesAssembler.java**:
   - Delete `private static final String GITHUB_OUTPUT_SUBDIR = ".github/templates";`.
   - In `copyToTargets()`: change `List<String> targets = List.of(CLAUDE_OUTPUT_SUBDIR, GITHUB_OUTPUT_SUBDIR);` to `List<String> targets = List.of(CLAUDE_OUTPUT_SUBDIR);` (single element). Consider inlining the loop to a single write call if clarity improves.
   - Update the class-level Javadoc: remove the phrase "to both .claude/templates/ and .github/templates/"; replace with "to .claude/templates/ only". Remove the "RULE-004 dual-target copy" language (it refers to an internal Javadoc RULE-004, different from story-0034 RULE-004 — rewrite to avoid reviewer confusion).
   - Verify: does the file now exceed 250 lines? Current: 354 lines pre-edit. After removing 1 constant + shrinking 1 method: ~348 lines. **Still over Rule-03 limit.** Apply follow-up refactor: extract `buildTemplateSections()` + the `TEMPLATE_SECTIONS` map to a new `PlanTemplateDefinitions` helper class in the same package. `PlanTemplatesAssembler` should then drop to ~150 lines.
2. **EpicReportAssembler.java**:
   - Delete `private static final String GITHUB_OUTPUT_SUBDIR = ".github/templates";` (line ~35).
   - Locate the copy-loop body and reduce targets to `CLAUDE_OUTPUT_SUBDIR` only.
   - Update Javadoc similarly.
3. **Integrity gate #1 (post-edit, pre-test)**:
```
git diff HEAD -- java/src/main/resources/shared/templates/
# expected: empty output
find java/src/main/resources/shared/templates -type f | wc -l
# expected: 57 (matches baseline)
```
If either check fails: abort, revert, open incident.
4. Update `PlanTemplatesAssemblerTest.java`: assertions that verified files appeared in `.github/templates/` subdir of the temp output dir are removed; add an inverted assertion that `.github/templates/` does NOT exist post-assemble.
5. Update `EpicReportAssemblerTest.java` similarly.
6. Add/update an integration test that asserts `.claude/templates/` contains all 15 entries listed in `PlanTemplatesAssembler.TEMPLATE_SECTIONS.keySet()` after a full assemble run.
7. If line-extraction refactor happened: create `PlanTemplateDefinitions.java` with the map builder, re-route `PlanTemplatesAssembler` to reference it, re-compile, re-test.
8. **Integrity gate #2 (post-test, pre-commit)**: re-run the two gate commands. Empty diff + count 57.
9. `grep -rn 'GITHUB_OUTPUT_SUBDIR' java/src/main/java` — expected 0.
10. Commit.

## Definition of Done

- [ ] `GITHUB_OUTPUT_SUBDIR` constant deleted from `PlanTemplatesAssembler.java`
- [ ] `GITHUB_OUTPUT_SUBDIR` constant deleted from `EpicReportAssembler.java`
- [ ] Both assemblers write only to `.claude/templates/` in the output dir
- [ ] **`git diff HEAD -- java/src/main/resources/shared/templates/` is empty** (RULE-004 gate #1)
- [ ] **`find java/src/main/resources/shared/templates -type f | wc -l` == 57** (RULE-004 gate #2)
- [ ] `grep -rn 'GITHUB_OUTPUT_SUBDIR' java/src/main/java` returns 0
- [ ] Class-level Javadoc on both assemblers updated (no dual-target mentions)
- [ ] `PlanTemplatesAssemblerTest` and `EpicReportAssemblerTest` updated and green
- [ ] Integration test asserts 15 files in `.claude/templates/` AND absence of `.github/templates/`
- [ ] `PlanTemplatesAssembler.java` <= 250 lines (extract helper if needed)
- [ ] `PlanTemplateDefinitions.java` helper class created if extraction was chosen (document in commit body)
- [ ] `mvn -pl java compile test -Dtest=PlanTemplatesAssemblerTest,EpicReportAssemblerTest` green
- [ ] Conventional commit with scope `task-0034-0004-003` AND explicit `RULE-004-compliant` footer

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0004-002b | Previous task completed the PlatformContextBuilder simplification; this task is purely about template copy routing and does not depend on data from 002b, but the linear dependency chain is preserved for atomic rollback. |

## Estimated Effort

- Pre-flight baseline capture: 5 min
- PlanTemplatesAssembler edit + potential helper extraction: 60-90 min
- EpicReportAssembler edit: 20 min
- Integrity gates (x2): 5 min
- Test updates (3 test files): 60 min
- Integration test authoring: 30 min
- Compile + test run: 15 min
- Commit: 10 min
- **Total: ~3h 30 min**

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| RULE-004 violation: an edit accidentally touches a file under `resources/shared/templates/` | Low | **CRITICAL** | Integrity gate runs at 2 points (post-edit and pre-commit); CI also runs the gate; manual reviewer MUST verify the diff |
| `PlanTemplatesAssembler.java` remains > 250 lines even after extraction | Medium | Medium (Rule 03 violation) | Accept the need to create `PlanTemplateDefinitions.java` helper; budget extra 30 min; if the split is too invasive, open follow-up issue and proceed with a documented exception in PR body |
| Integration test path-resolution for `.claude/templates/` drifts between local and CI | Low | Low | Use `@TempDir` Path fixture; assertions use relative paths from `tempDir` only |
| Removing `.github/templates/` writes breaks a downstream test that expected those files | Medium | Medium | Run full `mvn -pl java test` (not just the 3 impacted test classes) after the edit to surface hidden dependencies |
| The old Javadoc contains RULE-004 references that are internally defined (not story-0034 RULE-004); rewriting causes documentation drift | Medium | Low | Use generic language ("templates are copied verbatim to .claude/templates/"); avoid quoting any RULE-N in Javadoc |
