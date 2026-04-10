# Task Plan -- TASK-0034-0002-005

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0002-005 |
| Story ID | story-0034-0002 |
| Epic ID | 0034 |
| Source Agent | QA Engineer + Tech Lead (merged) |
| Type | migration (delete) |
| TDD Phase | GREEN |
| Layer | adapter.test |
| Estimated Effort | M |
| Date | 2026-04-10 |

## Objective

Delete the `.codex/` subdirectory from every golden file profile under `java/src/test/resources/golden/{profile}/`. This is the largest volume of file deletion in the entire epic: 17 profiles × ~173 files/profile = **2944 files** (per baseline `plans/epic-0034/baseline-pre-epic.md` §Golden Files). `.agents/` subdirs must remain UNTOUCHED (story 0034-0003 scope), and `.github/workflows/` is ALREADY PRESERVED from story 0034-0001 (not in this directory path but mentioned for continuity). Parallelizable with TASK-004.

## Implementation Guide

### Pre-delete baseline confirmation

```bash
cd java
find src/test/resources/golden -type d -name ".codex" | wc -l
# Expected: 17

find src/test/resources/golden -type d -name ".codex" -exec find {} -type f \; | wc -l
# Expected: 2944

find src/test/resources/golden -type d -name ".agents" | wc -l
# Expected: 17 (must remain after this task — story 0034-0003 scope)
```

If any count diverges from baseline, halt and reconcile before deletion.

### Delete

For each of the 17 profiles under `java/src/test/resources/golden/`, delete the `.codex/` subdirectory recursively:

```bash
cd java/src/test/resources/golden
# Iterate all profile dirs
for profile in */; do
  if [ -d "${profile}.codex" ]; then
    rm -rf "${profile}.codex"
  fi
done
```

Alternative (single command):

```bash
find java/src/test/resources/golden -type d -name ".codex" -prune -exec rm -rf {} +
```

### Post-delete sanity checks

```bash
# All .codex/ dirs gone
find java/src/test/resources/golden -type d -name ".codex" | wc -l
# Expected: 0

# No config.toml files remaining (TOML was exclusive to Codex)
find java/src/test/resources/golden -name "config.toml" | wc -l
# Expected: 0

# .agents/ still intact (preserved for story 0003)
find java/src/test/resources/golden -type d -name ".agents" | wc -l
# Expected: 17

# .github/workflows/ still intact (preserved from story 0001, RULE-003)
find java/src/test/resources/golden -type d -name "workflows" | wc -l
# Expected: (whatever story 0001 left; grep across golden dirs if needed)
```

### Build & verify

- Run `mvn clean compile test-compile` from `java/`. Expected: BUILD SUCCESS.
- Run `mvn test`. Expected: most tests green; `AssemblerRegressionSmokeTest` and golden-file comparison tests may fail because the expected count changed. Acceptable to defer fix to TASK-006 with `expected-artifacts.json` regeneration. Document the expected-fail list in commit body.

### Commit

```
test(golden)!: delete codex .codex golden files

Remove the .codex/ subdir from all 17 golden profiles under
java/src/test/resources/golden/. Total 2944 files deleted.

.agents/ subdirs preserved (story 0034-0003 scope).
.github/workflows/ preserved (RULE-003, unchanged from story 0034-0001).

Refs: EPIC-0034, story-0034-0002, RULE-005
```

## Definition of Done

- [ ] [QA-004] Post-delete `find java/src/test/resources/golden -type d -name '.codex'` returns 0
- [ ] [QA-004b] Post-delete `find java/src/test/resources/golden -name 'config.toml'` returns 0
- [ ] 2944 files deleted (verified by git diff file count; allow ±5 tolerance if baseline drift occurred)
- [ ] `.agents/` subdirs untouched: `find ... -type d -name '.agents' | wc -l` returns 17
- [ ] `.github/workflows/` untouched (RULE-003 verified via count before/after)
- [ ] `mvn clean compile test-compile` green
- [ ] `mvn test` green OR expected-fail list documented in commit body (must be limited to golden-comparison tests; any other failure halts the task)
- [ ] Commit follows Conventional Commits with `test(golden)!:` prefix

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0002-003 | Production code must no longer reference `.codex/` output paths before the golden fixtures can be safely deleted |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Shell glob accidentally deletes `.agents/` or `.claude/` along with `.codex/` | Low | CRITICAL | Use `-name ".codex"` (exact match) not `-name ".c*"`. Post-delete `find` sanity check for `.agents/` and `.claude/` counts. |
| `expected-artifacts.json` drift causes `AssemblerRegressionSmokeTest` failure | High | Medium | Defer manifest regeneration to TASK-006; document expected-fail in commit body |
| Golden count differs from baseline (2944) due to intermediate golden regeneration | Low | Low | Allow ±5 tolerance; if drift > 5 files, investigate via `git log golden/` before proceeding |
| Parallel task TASK-004 races with this one | None | None | Tasks touch disjoint paths (`src/main/resources/` vs `src/test/resources/golden/`). |
| Some profile has `.codex` as a regular file (not dir) | Very Low | Low | `-type d` filter prevents file matches; safe |
