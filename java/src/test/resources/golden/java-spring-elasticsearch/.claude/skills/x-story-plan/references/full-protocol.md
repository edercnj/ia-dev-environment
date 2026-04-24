<!--
Returns to [slim body](../SKILL.md) after reading the required phase.
TASK_PROPOSAL format and consolidation rules: see `planning-guide.md`.
-->

# x-story-plan — Full Protocol

## Phase 0 — Input Resolution

### 0.1 Parse Story Argument

Extract `XXXX` and `YYYY` from `story-XXXX-YYYY`. If argument does not match → abort with format error.

### 0.2 Resolve Epic Directory

Glob: `plans/epic-XXXX` or `plans/epic-XXXX-*`. If both exist, prefer exact match. No match → abort.

### 0.3 Resolve Paths

| Path | Pattern |
|------|---------|
| Story file | `<EPIC_DIR>/story-XXXX-YYYY.md` |
| Epic file | `<EPIC_DIR>/epic-XXXX.md` |
| Implementation map | `<EPIC_DIR>/IMPLEMENTATION-MAP.md` |
| Output dir | `<EPIC_DIR>/plans/` |
| Tasks file | `<EPIC_DIR>/plans/tasks-story-XXXX-YYYY.md` |
| Planning report | `<EPIC_DIR>/plans/planning-report-story-XXXX-YYYY.md` |
| DoR checklist | `<EPIC_DIR>/plans/dor-story-XXXX-YYYY.md` |

`mkdir -p <EPIC_DIR>/plans` before writing.

### 0.4 Staleness Check (RULE-002)

| Condition | Action | Log |
|-----------|--------|-----|
| Tasks file absent | Generate new | `"Generating story plan for {id}"` |
| `mtime(story) > mtime(tasks)` | Regenerate | `"Regenerating stale story plan"` |
| `mtime(story) <= mtime(tasks)` | Reuse | `"Reusing existing story plan from {date}"` |
| `--force` flag | Always regenerate | `"Force-regenerating story plan"` |

If reusing: skip to Phase 5 (DoR only). Do NOT invoke subagents.

### 0.5 Verify Story File

`test -f plans/epic-XXXX/story-XXXX-YYYY.md` → NOT_FOUND aborts.

---

## Phase 0b — Schema Version Detection

1. Read `plans/epic-XXXX/execution-state.json` via `SchemaVersionResolver`.
2. `planningSchemaVersion == "2.0"` → v2 path (run Phases 4a-4c after Phase 4).
3. Absent / `"1.0"` / malformed → v1 path (standard flow through Phase 5).

---

## Phase 1 — Context Gathering

Read story file (title, description, acceptance criteria, data contracts, dependencies, sub-tasks, non-functional requirements), epic file (cross-cutting rules, DoR/DoD), implementation map (phase assignment, dependency graph), and existing plan artifacts (arch, test, impl, security) as optional context.

Missing files → log WARNING and continue without that context.

---

## Phase 2 — Parallel Planning

### Template Detection (before dispatching)

```bash
test -f .claude/templates/_TEMPLATE-TASK-BREAKDOWN.md && echo "TB_AVAILABLE" || echo "TB_MISSING"
test -f .claude/templates/_TEMPLATE-STORY-PLANNING-REPORT.md && echo "SPR_AVAILABLE" || echo "SPR_MISSING"
test -f .claude/templates/_TEMPLATE-DOR-CHECKLIST.md && echo "DOR_AVAILABLE" || echo "DOR_MISSING"
```

### Subagent Context Scope

Each subagent receives the story file content, relevant context from Phase 1, and instructions to produce TASK_PROPOSAL entries per [`planning-guide.md`](planning-guide.md).

| Agent | Model | Scope |
|-------|-------|-------|
| Architect | Opus | Architecture decisions, layer design, component structure, ADRs |
| QA Engineer | Sonnet | Test strategies, AT/UT scenarios, coverage requirements |
| Security Engineer | Sonnet | Threat model, OWASP items, sensitive data handling |
| Tech Lead | Sonnet | Code quality, SOLID, refactoring, complexity limits |
| Product Owner | Sonnet | Business value, acceptance criteria, DoD alignment |

---

## Phase 3 — Consolidation

Apply deterministic merge rules from [`planning-guide.md §Consolidation Rules`](planning-guide.md):
1. Group proposals by layer and component overlap.
2. Majority-vote on duplicate proposals (same component, same type → keep highest-voted).
3. Preserve all unique proposals.
4. Topological sort by declared dependencies.
5. Assign sequential `TASK-XXXX-YYYY-NNN` IDs.

---

## Phase 4 — Artifact Generation

1. Write `tasks-story-XXXX-YYYY.md` from consolidated task list.
2. Write `planning-report-story-XXXX-YYYY.md` with per-agent summary and conflict resolution log.
3. Commit via `Skill(skill: "x-planning-commit", ...)` unless `--no-commit`.

### Phase 4a-4c (v2 only — `planningSchemaVersion == "2.0"`)

**Phase 4a — Task files:** For each TASK-XXXX-YYYY-NNN, emit `task-TASK-XXXX-YYYY-NNN.md` with I/O contract, testability, dependencies.

**Phase 4b — Parallel task plans:** Invoke `x-task-plan` per task in parallel (batch size ≤ 4):
```
Agent(subagent_type: "general-purpose", model: "sonnet", description: "x-task-plan for {TASK-ID}",
      prompt: "Invoke x-task-plan via Skill(skill: 'x-task-plan', args: '--task-file plans/epic-XXXX/plans/task-{TASK-ID}.md')")
```

**Phase 4c — Task map:** Generate `task-implementation-map-STORY-XXXX-YYYY.md` with topological sort + parallelism analysis via `x-parallel-eval`.

---

## Phase 5 — DoR Validation

**12 checks (v1 and v2):**

| # | Check | Required |
|---|-------|---------|
| 1 | Story file exists | Yes |
| 2 | Title and description present | Yes |
| 3 | Acceptance criteria (≥1 Gherkin scenario) | Yes |
| 4 | Data contracts defined (or N/A documented) | Yes |
| 5 | Dependencies listed (or none) | Yes |
| 6 | Non-functional requirements documented (or N/A) | Yes |
| 7 | Tasks breakdown generated | Yes |
| 8 | Each task has DoD criteria | Yes |
| 9 | Architecture layer assignments consistent | Yes |
| 10 | No circular dependencies | Yes |
| 11 | Effort estimates present | Yes |
| 12 | Planning report saved | Yes |

**v2 adds per-task READY checks:** each `task-TASK-*.md` passes schema validation and has `plan-task-TASK-*.md` sibling.

DoR not met → emit checklist diff + `DOR_NOT_MET` (non-blocking warning; story can proceed to implementation with known gaps).

---

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-story-implement` | caller | Delegates Phase 1 planning to this skill |
| `x-task-plan` | calls (Phase 4b, v2) | Per-task implementation plan generation |
| `x-parallel-eval` | calls (Phase 4c, v2) | File-overlap collision detection |
| `x-planning-commit` | calls (Phase 4) | Atomic commit of planning artifacts |
| `x-internal-epic-branch-ensure` | calls (Step P2) | Idempotent epic branch creation |
| `x-git-worktree` | calls (Step P1) | Detect worktree context (advisory) |
