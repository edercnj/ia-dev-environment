# Task Plan -- TASK-0034-0003-004

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0003-004 |
| Story ID | story-0034-0003 |
| Epic ID | 0034 |
| Source Agent | QA + Security + Tech Lead + Product Owner (consolidated) |
| Type | quality-gate + validation |
| TDD Phase | VERIFY |
| Layer | config + test |
| Estimated Effort | S |
| Date | 2026-04-10 |

## Objective

Perform the full final verification cycle: `mvn clean verify` green, coverage thresholds met (line ≥ 95%, branch ≥ 90%), grep sanity for residual `.agents/` references zero, CLI smoke test for `--platform claude-code`, `PlatformFilter.java` untouched, and create the PR with the required metrics checklist.

## Implementation Guide

1. **Clean build:** `mvn clean verify` — MUST be green.
2. **Coverage validation:** Inspect `target/site/jacoco/index.html` (or JaCoCo XML) for line and branch coverage.
   - Line ≥ 95% (baseline: 95.69%)
   - Branch ≥ 90% (baseline: 90.69%)
   - Absolute degradation ≤ 2pp vs baseline per RULE-002
   - Record actual values for PR body.
3. **Grep sanity (aggregate):**
   ```bash
   grep -rn "\.agents/\|CODEX_AGENTS\|AgentsAssembler\|AgentsSelection" java/src/main
   ```
   Expected: 0 matches.
4. **Boundary check:**
   ```bash
   find java/src/test/resources/golden -type d -name '.agents'
   ```
   Expected: empty output.
5. **Enum invariant check:** `AssemblerTargetTest` (edited in TASK-002) should pass and verify single-element enum.
6. **CLI regression smoke test:**
   ```bash
   java -jar target/*.jar generate --platform claude-code --output-dir /tmp/epic-0034-0003-smoke
   echo "exit: $?"
   test -d /tmp/epic-0034-0003-smoke/.claude && echo "claude OK" || echo "claude MISSING"
   test ! -d /tmp/epic-0034-0003-smoke/.agents && echo "agents absent OK" || echo "agents LEAKED"
   rm -rf /tmp/epic-0034-0003-smoke
   ```
7. **Cross-story cumulative check:** Record total `.agents/` files deleted in story 0003 (expected ~2910) and cumulative deletions across stories 0001+0002+0003. Document arithmetic in PR body.
8. **Scope boundary check:**
   ```bash
   git diff --name-only origin/develop..HEAD -- '*/PlatformFilter*'
   ```
   Expected: empty (PlatformFilter owned by story 0034-0004).
9. **Final secrets scan:**
   ```bash
   grep -rnE '(password|secret|token|api_?key)' java/src/main/resources/targets/
   ```
   Expected: 0 new unintended matches.
10. **Create PR:** Use `x-pr-create` or manually via `gh pr create` against `feature/epic-0034-remove-non-claude-targets` with:
    - Title: `feat(assembler)!: story-0034-0003 remove generic Agents target`
    - Body containing metrics table (before/after), JaCoCo report link, cumulative deletion math, and explicit note that story 0034-0004 (higienization) owns `PlatformFilter` cleanup and further shared code refactoring.

## Definition of Done

- [ ] [TL-001/RULE-001] `mvn clean verify` green
- [ ] [TL-002/RULE-002] JaCoCo line coverage ≥ 95%, branch coverage ≥ 90%
- [ ] Coverage degradation ≤ 2pp vs baseline (95.69% line / 90.69% branch)
- [ ] [TL-004/QA-AT-3] `grep -rn "\.agents/\|CODEX_AGENTS\|AgentsAssembler\|AgentsSelection" java/src/main` returns 0
- [ ] [QA-AT-4] `find golden -type d -name '.agents'` returns 0
- [ ] [QA-AT-2] `AssemblerTargetTest` verifies `values().length == 1`
- [ ] [QA-AT-5] CLI smoke test: `--platform claude-code` succeeds, produces `.claude/` only, no `.agents/` leaked
- [ ] [PO-001] Cumulative golden deletion math documented in PR body (stories 0001+0002+0003)
- [ ] [PO-002] PR body contains metrics table: main classes deleted (2), test classes deleted (6 + 1 fixture), golden dirs removed (17), enum size (2→1)
- [ ] [PO-004/TL-007] `git diff --name-only` does NOT include `PlatformFilter.java`
- [ ] [TL-005] All commits on branch follow Conventional Commits format
- [ ] [TL-006] PR created with BREAKING CHANGE footer noting `AssemblerTarget.CODEX_AGENTS` removal
- [ ] PR body links to JaCoCo report artifact
- [ ] [PO-003] PR body notes "Phase 2 of epic-0034 complete — next story (0004) is higienization"
- [ ] [SEC-005] Final secrets scan under `targets/` returns 0 new matches

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0003-003 | Golden file deletion must precede final verify; otherwise `mvn clean verify` does not reflect the final state |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Coverage drops below threshold despite proportional test removal | LOW | Story blocked at DoD | Pre-compute delta: removed 6 test classes covered `AgentsAssembler` + `AgentsSelection` (both also removed). Net impact should be ≈0 pp |
| CLI smoke test fails because `claude-code` platform has hidden dependency on `.agents/` code | LOW | Story blocked; investigation required | Pre-run smoke test locally before committing deletion; if fails, root-cause via `x-ops-troubleshoot` |
| PR cannot be merged because prerequisite stories (0001/0002) are still open | MEDIUM | PR blocked at review | Confirm prerequisite PRs merged before creating 0003 PR; otherwise target the story PR against the stacked branch |
| Grep sanity shows false positive in comments or in CHANGELOG.md | LOW | Manual review required | Exclude comments/changelog paths from grep; use `--include='*.java'` filter |
