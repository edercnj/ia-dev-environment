> Returns to [slim body](../SKILL.md) after reading the required phase.

# x-task-implement — Full Protocol

## Step 0 — Pre-Check: Plan Reuse (RULE-002)

1. Resolve paths from story ID (XXXX, YYYY):
   - Implementation plan: `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`
   - Architecture plan: `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
   - Test plan: `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`
   - Task breakdown: `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`
   - Per-task plans: `plans/epic-XXXX/plans/plan-task-TASK-*.md`

2. Staleness check: if `mtime(story) > mtime(plan)` → plan is stale (log WARNING; still use as context — do NOT regenerate).

3. Context combination log:

| Impl | Arch | Test | Log |
|------|------|------|-----|
| Absent | Absent | Absent | `"No plans found, proceeding with direct implementation"` |
| Present | Present | Present | `"Using all 3 plans as implementation context"` |

4. Task-aware mode: if per-task plans + task breakdown exist and are fresh → iterate TASK-NNN; else use UT-N mode.

---

## Step 0.5 — Worktree-First Branch Creation (Rule 14 + ADR-0004)

**Mandatory** before any `git checkout -b` or worktree create call.

**Step 0.5a — Detect context:**

    Skill(skill: "x-git-worktree", model: "haiku", args: "detect-context")

Returns `{inWorktree, worktreePath, mainRepoPath}`.

**Three-way mode decision:**

| `inWorktree` | `--worktree` flag | Mode | Action |
|---|---|---|---|
| `true` | any | Mode 1 — REUSE | Reuse parent worktree; `TASK_OWNS_WORKTREE=false` |
| `false` | absent | Mode 3 — LEGACY | Normal `git checkout -b` in main checkout |
| `false` | present | Mode 2 — CREATE | Create dedicated worktree; `TASK_OWNS_WORKTREE=true` |

**Mode 2 creation:**

    Skill(skill: "x-git-worktree", model: "haiku", args: "create --branch feat/task-XXXX-YYYY-NNN-desc --base develop --id task-XXXX-YYYY-NNN")

Step 0.5e records `TASK_OWNS_WORKTREE` for Step 5 cleanup decision.

---

## Step 1 — Prepare + Understand (Subagent)

Dispatch a preparation subagent (Rule 13 Pattern 2 — SUBAGENT-GENERAL) that:
1. Reads KPs: architecture-principles.md, coding-conventions.md, version-features.md, testing.md, layer-templates.md
2. Reads all available plan artifacts from Step 0
3. Produces a TDD implementation plan with:
   - Layer order (domain → ports → adapters → application → inbound)
   - Acceptance test scenarios (AT-N) from test plan or story
   - Unit test scenarios (UT-N) in TPP order per AT-N
4. Returns the plan inline (used in Steps 2–4)

---

## Step 2 — TDD Loop (Double-Loop, TPP Order)

For each acceptance test AT-N (outer loop):
  For each unit test UT-N for AT-N (inner loop, TPP order):

**RED:** Write failing test (name: `[method]_[scenario]_[expected]`). Run `{{TEST_COMMAND}}`. MUST fail.

**GREEN:** Write minimum production code. Run `{{COMPILE_COMMAND}}` → `{{TEST_COMMAND}}`. ALL must pass.

**REFACTOR:** Improve design without adding behavior. Run `{{TEST_COMMAND}}`. Still GREEN. Check: extract method (> 25 lines), eliminate duplication, improve naming.

**Compile check after each cycle:**
```bash
{{COMPILE_COMMAND}}
# Expected: zero errors, zero warnings
```

**Code conventions (per KP):**
- Methods ≤ 25 lines, classes ≤ 250 lines
- Named constants, no magic numbers
- Never return null — use Optional/empty types
- Constructor injection; immutable DTOs

---

## Step 3 — Validate (v1 and v2)

1. All acceptance tests AT-N must be GREEN.
2. Coverage check: line ≥ 95%, branch ≥ 90% on modified files.
3. If coverage below threshold: add missing test scenarios in TPP order; rerun.

---

## Step 4 — TDD Atomic Commits

Commit pattern per Red-Green-Refactor cycle:
```
test(scope): add test for [scenario] (RED)
feat(scope): implement [functionality] (GREEN)
refactor(scope): improve [aspect]
```

For acceptance tests:
```
test(scope): add acceptance test for [AT-N scenario] (RED)
test(scope): update acceptance test for [AT-N scenario] (GREEN)  # if AT content changed
```

Invoke via Skill tool (Rule 13 Pattern 1):

    Skill(skill: "x-git-commit", model: "haiku", args: "--type feat --scope task-XXXX-YYYY-NNN --subject \"implement [scenario]\"")

Pre-commit chain (RULE-007): format → lint → compile → commit.

---

## Step 4.5 — CI-Watch (Decision Table)

| `inWorktree` (Step 0.5) | `--worktree` | `--no-ci-watch` | Schema | CI-Watch fires? |
|---|---|---|---|---|
| `true` (Mode 1) | any | any | any | No — `"CI-Watch delegated to parent orchestrator"` |
| `false` | absent (Mode 3) | any | any | No — `"CI-Watch skipped: no --worktree"` |
| `false` | present (Mode 2) | present | any | No — `"CI-Watch skipped: --no-ci-watch"` |
| `false` | present (Mode 2) | absent | v1 | No — `"CI-Watch skipped: schema v1"` |
| `false` | present (Mode 2) | absent | v2 | **Yes** |

When firing: invoke `Skill(skill: "x-pr-watch-ci", args: "--pr-number {N} --poll-interval-seconds 60 --timeout-minutes 30 --require-copilot-review=false")`.

**State-file schema:** `.claude/state/task-watch-{TASK-ID}.json`
```json
{"prNumber":N,"startedAt":"<ISO-8601>","lastPollAt":"<ISO-8601>","pollCount":N,"checksSnapshot":[{"name":"...","conclusion":"..."}],"copilotReview":null,"schemaVersion":"1.0"}
```
Write protocol: write to `{path}.tmp` → rename atomically.

---

## Step 5 — Mode-Aware Worktree Cleanup (Rule 14 §5)

| Mode | Task Result | Action |
|------|------------|--------|
| Mode 1 (REUSE) | any | Do NOT remove (parent orchestrator owns). Do NOT `git checkout develop` (Rule 14 §2). |
| Mode 2 (CREATE) | success | `Skill(skill: "x-git-worktree", model: "haiku", args: "remove --id task-XXXX-YYYY-NNN")` → then `git checkout develop && git pull origin develop` in mainRepoPath. |
| Mode 2 (CREATE) | failed | Preserve worktree for diagnosis (Rule 14 §4). Log path for operator triage. |
| Mode 3 (LEGACY) | any | `git checkout develop && git pull origin develop`. |

---

## v2 Extensions (EPIC-0038 — Task-First Execution)

### Phase 0c — Schema Version Detection

Read `plans/epic-XXXX/execution-state.json` via `SchemaVersionResolver`.
- `planningSchemaVersion == "2.0"` → v2 path (this appendix)
- Absent / `"1.0"` / malformed → v1 path (standard flow above)

### Phase 0d — Input Resolution (v2)

Resolves three artifacts from `<task-id>` (e.g. `TASK-0039-0001-003`):

| Artifact | Pattern |
|----------|---------|
| Task file | `plans/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md` |
| Task plan | `plans/epic-XXXX/plans/plan-task-TASK-XXXX-YYYY-NNN.md` |
| Map | `plans/epic-XXXX/plans/task-implementation-map-STORY-XXXX-YYYY.md` |

Missing any → `TASK_ARTIFACT_NOT_FOUND {path}`.

### Phase 0e — Pre-Execution Gates (v2)

1. Dependencies: verify each TASK-ID in `## 4. Dependências` has `status=="DONE"` in `execution-state.json`. Unmet → `UNMET_DEPENDENCY {task-id}`.
2. Testability: `INDEPENDENT` → no pre-gate. `REQUIRES_MOCK` → verify mock exists. `COALESCED` → verify partner in batch.
3. Schema: task file must pass story-0038-0001 schema validation.

### Phase 3 — Post-Execution Output Verification (v2)

For each output declared in `§2.2` of the task file:

| Output pattern | Verification |
|----------------|--------------|
| `"class X created"` | `grep -r "class X" java/src/main/java` returns 1+ matches |
| `"method Y exists"` | `grep -r "void Y\|Y(" {scope}` matches |
| `"test Z passes"` | `{{TEST_COMMAND}} -Dtest=Z` exits 0 |
| `"file F exists"` | `test -f F` |
| `"build green"` | `{{COMPILE_COMMAND}}` exits 0 |

Any failure → `OUTPUT_CONTRACT_VIOLATION {output}`.

### Phase 3.5 — Task-Level Status Transition (v2, RULE-046-03)

Between output verification and commit. Writes `**Status:** Concluída` to task file and map row:

```bash
java -cp target/ia-dev-env.jar dev.iadev.cli.TaskMapRowUpdaterCli \
  plans/epic-XXXX/plans/task-implementation-map-STORY-XXXX-YYYY.md \
  TASK-XXXX-YYYY-NNN Concluída
```

Exit codes: `0` (updated), `20` (`STATUS_SYNC_FAILED`), `40` (`INVALID_ARGS`).

Stage artifacts: `git add task-TASK-*.md task-implementation-map-*.md` (included in Phase 4 commit).

COALESCED pairs: repeat for both task files and map rows in same commit.

### Phase 4 (v2) — Atomic Commit

Scope: `task(TASK-XXXX-YYYY-NNN)`. Append footer when Phase 3.5 ran:
```
Status: Em Andamento -> Concluída
Map row updated: task-implementation-map-STORY-XXXX-YYYY.md
```

COALESCED: `Coalesces-with: TASK-AAAA-BBBB-CCC` footer.

### Phase 5 (v2) — Status Report

Updates `execution-state.json`:
```json
{"tasks":{"TASK-ID":{"status":"DONE","commitSha":"<sha>","completedAt":"<ISO-8601>"}}}
```

### Error Codes (v2)

| Code | Condition |
|------|-----------|
| `TASK_ARTIFACT_NOT_FOUND` | task / plan / map file missing |
| `UNMET_DEPENDENCY` | declared dependency not DONE |
| `SCHEMA_VIOLATION` | ERROR-level schema violations |
| `OUTPUT_CONTRACT_VIOLATION` | declared output not verified |
| `RED_NOT_OBSERVED` | RED phase passed unexpectedly |
| `REFACTOR_BROKE_TESTS` | refactor broke previously-green tests |
| `STATUS_SYNC_FAILED` | `**Status:**` header or map row write failed |

---

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-test-plan` | reads | Consumes test plan for AT-N/UT-N ordering |
| `x-story-implement` | called-by | Phase 2 of full lifecycle |
| `x-git-commit` | calls (Step 4) | Atomic TDD commits |
| `x-git-worktree` | invokes (Steps 0.5 + 5) | Context detection, worktree create/remove |
| `x-pr-watch-ci` | calls (Step 4.5) | CI polling in standalone v2 mode |
| `x-pr-create` | calls (after Step 4) | PR creation targeting parent branch |
