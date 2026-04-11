# Pre-flight Conflict Analysis Reference

> **Context:** This reference is loaded at the start of each phase for parallel story dispatch.
> Part of x-epic-implement skill.

## Phase 0.5 — Pre-flight Conflict Analysis

At the start of **each phase N**, before dispatching any stories for that phase, the
orchestrator performs a pre-flight analysis to detect file-level overlaps between stories
in the same phase. By default (advisory mode), overlaps are logged as warnings but all
stories still execute in parallel. With `--strict-overlap`, stories with high code overlap
are demoted to sequential execution within phase N (RULE-005).
The results are written to `preflight-analysis-phase-{N}.md`, which the core loop
consumes when deciding per-story parallel vs sequential scheduling.

**Skip condition:** When `--sequential` is set, Phase 0.5 is skipped entirely. Log:
`"Pre-flight analysis skipped (sequential mode)"` and proceed directly to Phase 1.
In sequential mode there is no parallel dispatch, so conflict analysis adds no value.

### 0.5.1 Read Implementation Plans

For each story in the current phase N, attempt to read its implementation plan:

1. Compute plan path: `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`
2. Read the plan file and extract the list of affected files:
   - Look for sections titled "Affected files", "Existing classes to modify", or
     "New classes/interfaces to create"
   - Collect all file paths referenced in those sections
3. If the plan file does not exist for a story:
   - Mark the story as `unpredictable`
   - Log: `"WARNING: No implementation plan for {storyId} — classified as unpredictable"`
   - An `unpredictable` story is treated as a potential conflict with any other story

**Per-story data structure:**
```json
{
  "storyId": "story-XXXX-YYYY",
  "planPath": "plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md",
  "affectedFiles": ["src/main/java/.../File.java", "pom.xml"],
  "hasPlan": true
}
```

### 0.5.2 Build File Overlap Matrix

For each pair of stories (A, B) in the same phase, compute the intersection of their
affected file sets:

1. Intersect `affectedFiles(A)` with `affectedFiles(B)`
2. Record the overlap count: `overlapCount = |intersection|`
3. The matrix is symmetric: `overlap(A, B) == overlap(B, A)` — compute each pair once

### 0.5.3 Classify Overlaps

For each pair with `overlapCount > 0` (or involving an `unpredictable` story), apply
the classification rules in priority order:

| Classification | Criteria | Action |
|----------------|----------|--------|
| `unpredictable` | One or both stories have no implementation plan (`hasPlan: false`) | Demote to sequential execution (conservative) |
| `config-only` | ALL overlapping files are configuration files (`*.yaml`, `*.json`, `*.properties`, `*.toml`, `*.env`, `pom.xml`, `build.gradle`, `package.json`) | Allow parallel dispatch + smart merge (config files are generally merge-friendly) |
| `code-overlap-low` | 1–2 overlapping files are code files (`.ts`, `.java`, `.py`, `.go`, `.rs`, `.kt`) | Allow parallel dispatch with WARNING logged: `"WARNING: Low code overlap ({N} file(s)) between {storyA} and {storyB}"` |
| `code-overlap-high` | 3+ overlapping files are code files | Demote to sequential execution |
| `no-overlap` | Zero overlapping files and both stories have plans | Allow parallel dispatch (no action needed) |

**Per-pair data structure:**
```json
{
  "storyA": "story-XXXX-YYYY",
  "storyB": "story-XXXX-ZZZZ",
  "overlappingFiles": ["UserService.java", "UserRepository.java"],
  "overlapCount": 2,
  "classification": "code-overlap-low",
  "action": "parallel-with-warning"
}
```

### 0.5.4 Generate Execution Plan (Dual-Mode: Advisory / Strict)

The execution plan output depends on the `--strict-overlap` flag (RULE-005):

#### Default Mode (Advisory — no `--strict-overlap`)

All stories are dispatched in parallel regardless of overlap classification.
Overlaps produce warnings but do NOT block parallel execution. With per-story PRs,
conflicts are resolved automatically via auto-rebase (Section 1.4e) after PR merge.

**Output file:** Save the analysis to `plans/epic-XXXX/plans/preflight-analysis-phase-N.md`:

```markdown
# Pre-flight Conflict Analysis — Phase {N}

## File Overlap Matrix

| Story A | Story B | Overlapping Files | Classification |
|---------|---------|-------------------|----------------|
| story-XXXX-0001 | story-XXXX-0002 | pom.xml | config-only |
| story-XXXX-0001 | story-XXXX-0003 | UserService.java, UserRepository.java, UserController.java | code-overlap-high |
| story-XXXX-0002 | story-XXXX-0003 | — | no-overlap |

## Advisory Warnings
- WARNING: code-overlap-high between story-XXXX-0001 and story-XXXX-0003 (3 files: UserService.java, UserRepository.java, UserController.java). Auto-rebase will execute after the first PR of the phase merges.
- WARNING: story-XXXX-0004 has no implementation plan (classified as unpredictable). Monitor PR for conflicts.

## Execution Plan
All stories execute in parallel (advisory warnings above do not block execution).
```

#### Strict Mode (`--strict-overlap`)

When `--strict-overlap` is set, partition stories into two groups (original behavior):

- **Parallel Batch:** Stories with `no-overlap`, `config-only`, or `code-overlap-low`.
- **Sequential Queue:** Stories with `code-overlap-high` or `unpredictable`.
  Sequential order respects critical path priority (RULE-007).

**Output file (strict mode):**

```markdown
# Pre-flight Conflict Analysis — Phase {N}

## File Overlap Matrix
(same table as advisory mode)

## Adjusted Execution Plan
### Parallel Batch
- story-XXXX-0002 (no-overlap)
### Sequential Queue
1. story-XXXX-0001 (code-overlap-high with story-XXXX-0003)
2. story-XXXX-0003 (code-overlap-high with story-XXXX-0001)
3. story-XXXX-0004 (unpredictable — no implementation plan)
```

> **Precedence:** `--sequential` > `--strict-overlap` > default advisory.
> If `--sequential` is set, all stories execute sequentially regardless of
> `--strict-overlap`. If only `--strict-overlap` is set, the partitioning
> applies. If neither is set, all stories execute in parallel with warnings.

### 0.5.5 Integration with Core Loop (Section 1.3)

The execution plan produced by Phase 0.5 is consumed by the Core Loop:

**Default mode (advisory):**
1. The core loop treats all executable stories as parallel-eligible
2. If preflight analysis exists, warnings are logged but do NOT affect dispatch
3. All stories are dispatched via worktree parallel dispatch (Section 1.4a)

**Strict mode (`--strict-overlap`):**
1. Before calling `getExecutableStories()`, the orchestrator reads the preflight
   analysis for the current phase from `preflight-analysis-phase-N.md`
2. Stories in the **Parallel Batch** are dispatched via worktree parallel dispatch
   (Section 1.4a) as normal
3. Stories in the **Sequential Queue** are removed from the parallel batch and
   enqueued for sequential dispatch (Section 1.4) after the parallel batch completes
4. The sequential queue ordering respects critical path priority (RULE-007)

**Common rules:**
5. If no preflight analysis exists for a phase (e.g., Phase 0.5 was skipped or
   this is a `--resume` run), all executable stories default to parallel dispatch
6. With per-story PRs, conflicts from parallel overlap are resolved automatically
   via auto-rebase (Section 1.4e). The `--strict-overlap` mode is recommended for
   epics with many stories editing the same files.
