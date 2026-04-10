# Task Plan -- TASK-0034-0001-006

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0001-006 |
| Story ID | story-0034-0001 |
| Epic ID | 0034 |
| Source Agent | QA + Security + Tech Lead + PO (merged) |
| Type | quality-gate + validation |
| TDD Phase | VERIFY |
| Layer | config + test |
| Estimated Effort | S |
| Date | 2026-04-10 |

## Objective

Final verification gate for the story. Cleans up the 18 `setup-config.*.yaml` files (removing `copilot` platform references), runs `mvn clean verify` with coverage check, executes all 6 acceptance test scenarios manually/automated, creates the story PR. This task consolidates ALL agents' VERIFY proposals into a single PR gate.

Note: This is the PR creation task and carries the breaking-change communication responsibility.

## Implementation Guide

### Phase 6.A — YAML cleanup

1. List all 18 YAMLs:
   ```bash
   ls java/src/main/resources/shared/config-templates/setup-config.*.yaml | wc -l
   # expected: 18
   ```
2. For each YAML, remove every occurrence of `copilot` in platform options. Use a single `sed` pass or edit manually. Common patterns:
   - `# Options: claude-code, copilot, codex, all` -> `# Options: claude-code, codex, all`
   - `platform: copilot` lines -> delete the line (if present)
   - `- copilot` list entries under `platforms:` -> delete
3. Verify cleanup:
   ```bash
   grep -l 'copilot' java/src/main/resources/shared/config-templates/setup-config.*.yaml
   # expected: no output
   ```

### Phase 6.B — Security hygiene check

4. `[SEC-001]` Scan YAMLs for accidentally leaked secrets:
   ```bash
   grep -rE '(password|secret|token|api_?key)' java/src/main/resources/shared/config-templates/setup-config.*.yaml
   ```
   Expected: 0 matches (or only allowlisted example patterns like `api_key: <YOUR_API_KEY>`).

### Phase 6.C — Full build + coverage gate

5. Run `mvn clean verify` from `java/`. Expected: BUILD SUCCESS with all tests passing.
6. `[TL-004/RULE-002]` Parse JaCoCo report at `java/target/site/jacoco/index.html`:
   - Line coverage >= 95% (baseline 95.69%)
   - Branch coverage >= 90% (baseline 90.69%)
   - Degradation <= 2pp vs. baseline (so line >= 93.69%, branch >= 88.69%)

### Phase 6.D — Grep sanity checks

7. `[QA-005/AT-5]` Run all grep sanity checks:
   ```bash
   grep -rn 'GithubInstructionsAssembler\|GithubMcpAssembler\|GithubSkillsAssembler\|GithubAgentsAssembler\|GithubHooksAssembler\|GithubPromptsAssembler\|GithubAgentRenderer\|PrIssueTemplateAssembler' java/src/main/java
   # expected: 0 matches
   grep -rn 'Platform.COPILOT' java/src/main
   # expected: 0 matches
   grep -rn 'AssemblerTarget.GITHUB' java/src/main
   # expected: 0 matches
   grep -ri 'copilot' java/src/main/java
   # expected: 0 matches (source code only)
   ```

### Phase 6.E — CLI smoke tests (6 acceptance criteria)

8. Build jar: `mvn package -DskipTests`. Locate it at `java/target/ia-dev-env-*.jar` (or similar).

9. `[AT-1]` Already covered by Phase 6.C (mvn clean verify).

10. `[AT-2]` CLI error case:
    ```bash
    java -jar java/target/ia-dev-env-*.jar generate --platform copilot --output-dir /tmp/test-out-copilot
    ```
    Expected: exit code != 0; stderr contains `Invalid platform` and does NOT list `copilot` in accepted values.

11. `[AT-3]` CLI happy path claude-code:
    ```bash
    rm -rf /tmp/test-out-claude
    java -jar java/target/ia-dev-env-*.jar generate --platform claude-code --output-dir /tmp/test-out-claude
    ```
    Expected: exit code 0; `/tmp/test-out-claude/.claude/` exists; `/tmp/test-out-claude/.github/` does NOT exist (except `.github/workflows/` if configured).

12. `[AT-4]` RULE-003 invariant — already verified in TASK-0034-0001-005. Re-check:
    ```bash
    find java/src/test/resources/golden/*/.github/workflows -type f | wc -l
    # expected: 95
    ```

13. `[AT-6]` CLI degenerate (no `--platform`):
    ```bash
    rm -rf /tmp/test-out-default
    java -jar java/target/ia-dev-env-*.jar generate --output-dir /tmp/test-out-default
    ```
    Expected: exit code 0; `.claude/` directory generated (default is claude-code).

14. `[PO-003]` Codex platform regression check:
    ```bash
    rm -rf /tmp/test-out-codex
    java -jar java/target/ia-dev-env-*.jar generate --platform codex --output-dir /tmp/test-out-codex
    ```
    Expected: exit code 0 (codex removal is story 0034-0002, not this story). `/tmp/test-out-codex/.codex/` exists.

### Phase 6.F — Commit history validation

15. `[TL-005]` Run `git log --oneline feature/epic-0034-remove-non-claude-targets...develop`. Expected:
    - 6 atomic commits (one per task)
    - All use Conventional Commits format with `!` suffix or `BREAKING CHANGE:` footer
    - At least TASK-003's commit has a BREAKING CHANGE footer

### Phase 6.G — PR creation

16. `[TL-007]` Create PR via `gh pr create`:
    ```bash
    gh pr create \
      --base develop \
      --head feature/epic-0034-remove-non-claude-targets \
      --title "feat!: remove GitHub Copilot support (story-0034-0001)" \
      --body "<prepared body>"
    ```

17. `[PO-004]` PR body MUST contain:
    - Link to story file
    - Before/after artifact count table:
      | Artifact | Before | After | Delta |
      |----------|--------|-------|-------|
      | Java main classes (Github*) | 8 | 0 | -8 |
      | Java test classes (Github*) | 15 | 0 | -15 |
      | Test fixtures | 1 | 0 | -1 |
      | Resources (targets/github-copilot/) | 131 | 0 | -131 |
      | Golden files (.github/ non-workflows) | 2324 | 0 | -2324 |
      | Golden files (.github/workflows/) [PROTECTED] | 95 | 95 | 0 |
      | YAML config-templates | 18 | 18 (cleaned) | 0 |
    - JaCoCo coverage: baseline line 95.69% -> post-story X.XX%; baseline branch 90.69% -> post-story Y.YY%
    - Commit list (6 commits)
    - BREAKING CHANGE declaration
    - Link to `plans/epic-0034/plans/tasks-story-0034-0001.md`

18. Commit message for this task: `chore(yaml)!: remove copilot from config-templates and finalize story`

## Definition of Done

### YAML cleanup

- [ ] 18 YAMLs processed; all `copilot` references removed
- [ ] `grep -l 'copilot' java/src/main/resources/shared/config-templates/setup-config.*.yaml` returns nothing
- [ ] [SEC-001] No secrets in YAMLs (grep passes)

### Build + coverage

- [ ] [QA-006/RULE-002/TL-004] `mvn clean verify` green
- [ ] JaCoCo line coverage >= 95% (baseline 95.69%); branch >= 90% (baseline 90.69%); degradation <= 2pp

### Grep sanity

- [ ] [QA-005/AT-5] All grep commands in Phase 6.D return 0

### Acceptance criteria

- [ ] [AT-1] Build verde (covered by mvn clean verify)
- [ ] [AT-2] CLI rejects `--platform copilot` with proper error
- [ ] [AT-3] CLI works for `--platform claude-code`
- [ ] [AT-4] `.github/workflows/` file count in golden = 95 (unchanged)
- [ ] [AT-5] Grep sanity all pass
- [ ] [AT-6] CLI default (no `--platform`) works
- [ ] [PO-003] CLI still works for `--platform codex` (next story deliverable)

### Commit history + PR

- [ ] [TL-005] 6 Conventional Commits on branch
- [ ] At least one commit has BREAKING CHANGE footer
- [ ] [TL-007] PR created
- [ ] [PO-004] PR body includes before/after table, JaCoCo data, commit list, link to tasks file

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0001-004 | Resources deletion must complete for coverage and grep sanity to reflect final state |
| TASK-0034-0001-005 | Golden file deletion must complete for AT-4 verification |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Coverage degradation >2pp because deleted test classes removed more coverage than deleted production code | Medium | High | Per RULE-006 (TDD compliance), deletion should be proportional. If degradation occurs, analyze JaCoCo report to find uncovered production paths; revert or add smoke tests. |
| `mvn clean verify` fails due to smoke test assertion on artifact count | Medium | Medium | Regenerate `expected-artifacts.json` via `ExpectedArtifactsGenerator` in this task or defer to story-0034-0005 (document expected failure). |
| PR body assembly is error-prone | Low | Low | Use a template file committed to `plans/epic-0034/plans/pr-body-story-0034-0001.md` for reproducibility. |
| JaCoCo branch coverage dips below 90% even with 2pp tolerance | Low | High | Run incremental coverage check after TASK-002 (before final PR) to catch early. Add targeted test if needed. |
| `gh pr create` fails because branch not pushed | Low | Low | `git push -u origin feature/epic-0034-remove-non-claude-targets` first. |
| CLI smoke tests fail because jar artifact name differs from expected glob | Low | Low | Use `ls java/target/*.jar` to discover the actual artifact name. |
