# Task Plan -- TASK-0034-0004-006

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0004-006 |
| Story ID | story-0034-0004 |
| Epic ID | 0034 |
| Source Agent | merged(QA Engineer, Security Engineer, Tech Lead, Product Owner) — all 5 agents co-own the final gate |
| Type | quality-gate + validation + PR |
| TDD Phase | VERIFY |
| Layer | cross-cutting |
| Estimated Effort | S (~1.5 hours for verification + PR authoring) |
| Date | 2026-04-10 |

## Objective

Final verification of the entire story. No code changes. Run the full quality gate: `mvn clean verify` green with coverage thresholds, RULE-004 template integrity gate, grep sanity checks, LOC reduction measurement, manual CLI smoke, and PR authoring with a comprehensive body linking to JaCoCo report + metrics delta.

## Implementation Guide

1. **Clean build** from a fresh workspace state:
```
cd java
mvn clean verify
```
Capture the build log; attach to PR as `build-log.txt` or paste last 100 lines in PR body.
2. **Coverage gate** (RULE-002):
```
# From JaCoCo HTML report
grep -A1 'Total' java/target/site/jacoco/index.html | head -20
```
Expected: line >= 95%, branch >= 90%. Compare to baseline-pre-epic.md (95.69% / 90.69%). Degradation <= 2pp.
3. **RULE-004 template integrity gate**:
```
git diff origin/main -- java/src/main/resources/shared/templates/
# expected: empty output
find java/src/main/resources/shared/templates -type f | wc -l
# expected: 57
```
If either fails: **block PR merge and file incident**.
4. **Grep sanity sweep**:
```
grep -rn 'hasCopilot\|hasCodex\|ReadmeGithubCounter' java/src/main
grep -rn 'hasCopilot\|hasCodex' java/src/test
grep -rnE '@Nested.*(Copilot|Codex|Agents)' java/src/test/java/dev/iadev/smoke
grep -rn 'GITHUB_OUTPUT_SUBDIR' java/src/main/java
```
Expected: all zero matches.
5. **LOC reduction measurement** (story §5.1 metric):
```
# Read baseline from story 0003 final commit
baseline=$(cat plans/epic-0034/reports/story-0003-loc-baseline.txt)
current=$(find java/src/main/java/dev/iadev/application/assembler -name '*.java' | xargs wc -l | tail -1 | awk '{print $1}')
ratio=$(echo "scale=3; $current / $baseline" | bc)
echo "Baseline: $baseline lines, current: $current lines, ratio: $ratio (target: <= 0.90)"
```
If ratio > 0.90 (less than 10% reduction): flag in PR body, request tech-lead review of whether further cleanup is needed or the target is aspirational.
6. **File-size / method-size checks** (Rule 03):
```
find java/src/main/java/dev/iadev/application/assembler -name '*.java' -exec wc -l {} \; | awk '$1 > 250'
# expected: empty (no file over 250 lines)
```
Manual method-size check: reviewer spot-checks files edited in this story.
7. **CLI manual smoke** (story §7 Gherkin "Degenerate — CLI ainda funciona"):
```
java -jar java/target/ia-dev-env-*.jar generate --platform claude-code --output-dir /tmp/epic-0034-story-0004-verify
ls -la /tmp/epic-0034-story-0004-verify/.claude/
# expected: rules/, skills/, agents/ subdirs present
[ -d /tmp/epic-0034-story-0004-verify/.github ] && echo "FAIL" || echo "OK"
# expected: OK
```
8. **CLI rejection smoke**:
```
java -jar java/target/ia-dev-env-*.jar generate --platform copilot --output-dir /tmp/reject-test 2>&1 | tee reject.out
[ $? -ne 0 ] && grep "Invalid platform" reject.out && echo "OK" || echo "FAIL"
```
9. **Commit log audit** (Rule 08):
```
git log feature/epic-0034-remove-non-claude-targets --oneline | grep -E 'task-0034-0004-00[1-6b]'
# expected: 7 commits (001, 002, 002b, 003, 004, 005, 006)
```
Verify each commit message includes the task-ID scope trailer.
10. **Create PR**:
- Title: `refactor(epic-0034): story 0004 — hygienize shared classes post-target-removal`
- Body (draft in advance):
  - §3.5 metrics table: LOC before/after, shared-templates count (57→57), platform flags before/after
  - Links to JaCoCo report (attach as PR artifact or paste summary)
  - Explicit RULE-004 compliance statement: "Confirmed empty diff on `java/src/main/resources/shared/templates/` vs. `origin/main`"
  - Grep sanity results (all zero)
  - Commit list (7 task commits)
  - CLI smoke evidence
- Target branch: `feature/epic-0034-remove-non-claude-targets` (epic integration branch)
11. Author the final verification commit (no code changes; empty commit documenting the gate pass):
```
git commit --allow-empty -m "chore(verify): finalize story 0034-0004 hygienization and open PR" \
  --trailer "task: task-0034-0004-006"
```

## Definition of Done

- [ ] `mvn clean verify` green from clean state; build log attached to PR
- [ ] JaCoCo line coverage >= 95% AND branch >= 90%; degradation <= 2pp vs. baseline
- [ ] `git diff origin/main -- java/src/main/resources/shared/templates/` empty (RULE-004)
- [ ] `find java/src/main/resources/shared/templates -type f | wc -l` == 57
- [ ] All 4 grep sanity checks return zero matches
- [ ] LOC ratio for `application/assembler/` <= 0.90 (>=10% reduction) OR documented exception in PR
- [ ] No file in `application/assembler/` > 250 lines; no method > 25 lines
- [ ] CLI success smoke: `--platform claude-code` produces `.claude/`, no `.github/`
- [ ] CLI rejection smoke: `--platform copilot` exits non-zero with "Invalid platform" in stderr
- [ ] All 7 commits on the branch follow Conventional Commits with task-ID trailers
- [ ] PR body includes §3.5 metrics table, RULE-004 statement, JaCoCo link, grep results, CLI evidence
- [ ] PR created against `feature/epic-0034-remove-non-claude-targets`
- [ ] Gherkin §7 scenario-by-scenario verification checklist posted as PR comment
- [ ] Final conventional commit with scope `task-0034-0004-006` in trailer (empty commit acceptable)

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0004-005 | Previous task completed smoke test hygienization; nothing further can be code-edited in this story. |

## Estimated Effort

- Clean build + JaCoCo review: 15 min (dominated by build time)
- RULE-004 gate + grep sweeps: 10 min
- LOC measurement + file-size checks: 10 min
- CLI manual smokes: 10 min
- Commit log audit: 5 min
- PR authoring (body + metrics + links): 30 min
- Gherkin checklist comment: 15 min
- Final empty commit: 2 min
- **Total: ~1h 40 min**

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| `mvn clean verify` fails on a test unrelated to this story (transient infrastructure) | Low | Medium | Retry once; if repeat failure, debug root cause; do not mask with `-fae` |
| Coverage regression: line or branch drops below threshold | Medium | High | Task 005 records pre/post method count to predict impact; if regression occurs, add targeted tests for any un-covered path |
| LOC reduction falls below 10% target | Medium | Low | Target is aspirational per story §3.5 wording ("entrega"); document actual ratio in PR and let reviewer decide if additional cleanup is warranted |
| RULE-004 violation discovered at verification time (late discovery) | Low | **CRITICAL** | Dual gates in tasks 003 and 006 catch it; if it still slips through, revert the offending task and re-run |
| The story-0003 baseline file (`story-0003-loc-baseline.txt`) is missing | Medium | Medium | Task 006 DoD requires fallback: measure baseline retroactively from the story-0003 merge commit; document the fallback method in PR body |
| PR body link to JaCoCo report rots over time | Low | Low | Attach the HTML report directory as a PR artifact OR paste the summary table verbatim in the PR body |
| CLI manual smoke requires a built jar; build step is already covered but target name may differ | Low | Low | Use `java/target/ia-dev-env-*.jar` glob; if no match, inspect `java/target/` directly |
| `grep -rn 'agents'` returns many benign hits (Claude `.claude/agents/` is valid) | High | Low | Narrow the grep: exclude `.claude/`, `claude/`, and legitimate `agents.md` references; document acceptable residues in PR body |
