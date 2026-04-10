# Task Plan -- TASK-0034-0001-005

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0001-005 |
| Story ID | story-0034-0001 |
| Epic ID | 0034 |
| Source Agent | QA Engineer + Tech Lead (merged) |
| Type | migration (golden files deletion) |
| TDD Phase | GREEN (boundary test) |
| Layer | adapter.test |
| Estimated Effort | M |
| Date | 2026-04-10 |

## Objective

Delete the `.github/` golden file fixtures across all 17 generator profiles, while preserving `.github/workflows/` (RULE-003, CRITICAL). This task has the highest volume (~2324 files) and the highest risk of rule violation — losing `.github/workflows/` files would delete GitHub Actions CI/CD configurations that are ORTHOGONAL to Copilot support.

This task can run in parallel with TASK-004 (both branch from TASK-003). The orchestrator may execute them concurrently if git strategy allows.

## Implementation Guide

1. **Pre-delete inventory (RULE-003 baseline):**
   ```bash
   find java/src/test/resources/golden/*/.github/workflows -type f | wc -l
   ```
   Expected: **95** per baseline. Record this number.

2. **List all `.github/` subdirs to be deleted:** For each profile in `java/src/test/resources/golden/`:
   ```bash
   for p in java/src/test/resources/golden/*/; do
     echo "=== $(basename $p) ==="
     find "${p}.github/" -mindepth 1 -maxdepth 1 -type d 2>/dev/null
   done
   ```
   Record the expected-to-delete subdirs (e.g., `agents/`, `instructions/`, `skills/`, `hooks/`, `prompts/`) vs. expected-to-keep (`workflows/`).

3. **Execute deletion with explicit exclusion:** Use a shell loop that deletes every child of `.github/` EXCEPT `workflows/`:
   ```bash
   for profile_github in java/src/test/resources/golden/*/.github; do
     if [ -d "$profile_github" ]; then
       find "$profile_github" -mindepth 1 -maxdepth 1 -not -name 'workflows' -exec rm -rf {} +
     fi
   done
   ```
   Also delete any `.github/copilot-*.md` or `.github/copilot-*.yml` files that are direct children of `.github/` (not in a subdir):
   ```bash
   find java/src/test/resources/golden/*/.github -maxdepth 1 -type f -name 'copilot-*' -delete
   ```
   And any other direct-child files that are not workflows (e.g., `.github/issue_template.md`, `.github/PULL_REQUEST_TEMPLATE.md` — if they exist, they belong to Copilot context):
   ```bash
   find java/src/test/resources/golden/*/.github -maxdepth 1 -type f -delete
   ```
   **WARNING:** The last command deletes all direct file children. Verify first that no workflows-related file lives at `.github/` root level (they should all be under `.github/workflows/`).

4. **Post-delete verification (RULE-003 invariant):**
   ```bash
   find java/src/test/resources/golden/*/.github/workflows -type f | wc -l
   ```
   Expected: **95** (identical to baseline).
   ```bash
   find java/src/test/resources/golden/*/.github -mindepth 1 -maxdepth 1 -not -name 'workflows' | wc -l
   ```
   Expected: **0**.

5. **Verify no non-workflow files remain:**
   ```bash
   find java/src/test/resources/golden/*/.github -type f -not -path '*/workflows/*' | wc -l
   ```
   Expected: **0**.

6. **Verify workflows file list identical:**
   ```bash
   find java/src/test/resources/golden/*/.github/workflows -type f | sort > /tmp/post-workflows.txt
   # Compare against pre-deletion snapshot captured in step 1
   diff /tmp/pre-workflows.txt /tmp/post-workflows.txt
   ```
   Expected: no difference.

7. Run `mvn compile` and `mvn test -Dtest=AssemblerRegressionSmokeTest`. Expected: BUILD SUCCESS. If the smoke test fails because `expected-artifacts.json` still lists `.github/` non-workflow files, that regeneration is deferred to story-0034-0005 — document in commit body and skip the assertion via `-DskipTests` for that ONE test, OR regenerate the manifest here (preferred if it is a trivial `mvn exec:java` call).

8. Commit as a single atomic commit.

## Definition of Done

- [ ] [QA-004/RULE-003] Pre-delete workflows file count recorded (expected: 95)
- [ ] All 17 profiles' `.github/` directories no longer contain any file outside `workflows/`
- [ ] Post-delete workflows file count == pre-delete count (95)
- [ ] Post-delete workflows file list (sorted, full paths) == pre-delete list (diff empty)
- [ ] `find java/src/test/resources/golden/*/.github -type f -not -path '*/workflows/*' | wc -l` == 0
- [ ] ~2324 files deleted (record actual count in commit body for traceability)
- [ ] `mvn compile` green
- [ ] `AssemblerRegressionSmokeTest` either passes or is documented as failing due to `expected-artifacts.json` staleness (to be regenerated in story-0034-0005)
- [ ] Commit message: `test(golden)!: delete copilot .github/ files preserving workflows`
- [ ] Commit body includes: pre-workflow count, post-workflow count, pre-total `.github/` count, post-total `.github/` count

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0001-003 | Enum/CLI edits complete to ensure any smoke test running after golden deletion expects the new state |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| **Shell glob accidentally deletes `.github/workflows/`** | Medium | CRITICAL | Explicit `-not -name 'workflows'` exclusion in find command. Post-delete count check is MANDATORY gate. If count != 95, revert and re-execute. |
| **Non-workflow file at `.github/` root level that should be kept** (e.g., `.github/CODEOWNERS`) | Low | Medium | Pre-delete inspection of `find .github -maxdepth 1 -type f` in step 2. If any surprising file appears, pause and ask reviewer. |
| `expected-artifacts.json` staleness causes `AssemblerRegressionSmokeTest` failure | High | Low | Known gap. Story-0034-0005 regenerates the manifest. Document as expected failure in PR description. Alternative: regenerate manifest in this same task. |
| Shell loop deletes files in profiles that don't have `.github/` | Low | None | `[ -d ]` guard. |
| Line-ending or encoding differences cause `diff /tmp/pre-workflows.txt /tmp/post-workflows.txt` to report false positives | Low | Low | Compare file NAMES (paths) only, not content. Content diff is not required. |
| Some profiles may have `.github/workflows/` with subdirectories (e.g., `.github/workflows/reusable/`) | Low | Medium | `find -type f` handles subdirectories transparently. Count check is recursive. |
