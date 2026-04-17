---
name: x-parallel-eval
description: "Detects and classifies file-collision risks between work units (tasks, stories, or epic phases) before parallel execution. Reads ## File Footprint blocks produced by x-task-plan / x-story-plan, applies the parallelism-heuristics knowledge pack (hard / regen / soft categories + hotspot overrides), and emits a collision matrix + serialization recommendation in Markdown (default) or JSON."
user-invocable: true
argument-hint: "--scope=epic|story|task [--epic PATH] [--a ID --b ID] [--out PATH] [--format markdown|json]"
allowed-tools:
  - Bash
  - Read
  - Skill
---

# Skill: x-parallel-eval

## Purpose

Evaluate the merge-conflict risk of running two or more work units in parallel,
using the canonical file-collision heuristics catalogued in the
`parallelism-heuristics` knowledge pack. The skill is intentionally cascading:
it consumes the structured `## File Footprint` blocks already emitted by
`x-task-plan` (Phase 4.5) and `x-story-plan` (Phase 6), so the output is
deterministic and independent of prose parsing (RULE-008).

The primary consumers are:

- Operators validating an epic before dispatching `x-epic-implement` in parallel.
- `x-epic-map` Step 8.5 (`story-0041-0005`), which invokes this skill as a gate.
- `x-dev-*-implement` future gates (`story-0041-0006`).

## When to Use

- Before approving an Implementation Map to confirm that declared parallel
  phases are actually conflict-free.
- When comparing two stories or two tasks in isolation to decide whether
  they can be coalesced into the same wave.
- As a read-only audit after the fact ("did we ship these in parallel safely?").

## CLI Arguments

| Flag | Required | Description |
| :--- | :--- | :--- |
| `--scope` | Yes | `epic`, `story`, or `task` |
| `--epic` | If `--scope=epic` | Path to the epic directory (e.g., `plans/epic-0041`) |
| `--a` / `--b` | If `--scope=story\|task` | IDs of the pair to compare |
| `--out` | No | Output file path (writes to stdout by default) |
| `--format` | No | `markdown` (default) or `json` |
| `--map` | No | Implementation map path; when supplied its declared phases override the topological derivation |
| `--include-soft` | No | Include SOFT (read-only) overlaps in the output |

## Exit Codes

| Code | Meaning |
| :--- | :--- |
| 0 | No conflicts detected |
| 1 | Warnings only (e.g., legacy footprints missing) |
| 2 | Hard or regen conflicts detected |

## Workflow

### Phase 1 — Resolve scope and load footprints

1. Parse the CLI arguments and classify scope (`epic` / `story` / `task`).
2. For `--scope=epic`: enumerate `story-XXXX-YYYY.md` files under the epic
   directory, extract the `## File Footprint` block and the `Blocked By`
   column, and derive phases topologically.
3. For `--scope=story|task`: read the two artefacts directly.
4. Any artefact lacking a `## File Footprint` block is reported as a warning
   (RULE-005 — Degrade with Warning) and flagged as non-parallel.

### Phase 2 — Classify collisions

For every pair within the same phase, apply the rules in the
`parallelism-heuristics` KP (RULE-003):

- `hard` — both units write the same path → must be serialized.
- `regen` — one writes, the other regenerates (or both regenerate) the same
  path → must be serialized.
- `soft` — overlap only in `read:` → safe to run in parallel (ignored unless
  `--include-soft` is passed).

Hotspot paths from the KP catalogue (RULE-004) force a `hard` classification
regardless of the nominal write / regen sets.

### Phase 3 — Emit report

1. Render the report in Markdown (default) or JSON (`--format=json`). The
   Markdown shape is:

       # Parallelism Evaluation — EPIC-XXXX

       **Scope:** epic | **Items analyzed:** N | **Conflicts:** H hard, R regen, S soft

       ## Collision Matrix
       | A | B | Category | Shared paths |
       | :--- | :--- | :--- | :--- |
       ...

       ## Recommended Serialization Groups
       - **Group N (serialize):** story-... → story-...
       - **Group N (parallel):** story-..., story-...

       ## Hotspot Touches
       - `path` touched by: story-X, story-Y

2. Write to stdout by default, or to `--out` when supplied.
3. Return the exit code derived from `hardCount + regenCount + warnings`
   (see table above).

## Invocation Examples

```bash
/x-parallel-eval --scope=epic --epic plans/epic-0041
/x-parallel-eval --scope=epic --epic plans/epic-0041 --out reports/parallelism.md
/x-parallel-eval --scope=story --a story-0041-0002 --b story-0041-0003
/x-parallel-eval --scope=task --a TASK-0041-0002-001 --b TASK-0041-0003-001
```

The skill shells out to the project CLI (`parallel-eval` subcommand backed by
`dev.iadev.parallelism.cli.ParallelEvalCli`). It is side-effect-free except
when `--out` is supplied.

## Knowledge Pack Reference

- `parallelism-heuristics` — canonical catalogue of the File Footprint block
  format, conflict categories (hard / regen / soft), hotspot list, and
  degrade-with-warning policy. Read before modifying collision rules.

## Integration Notes

| Consumer | Relationship | Context |
| :--- | :--- | :--- |
| `x-epic-map` | Invokes (Step 8.5) | Parallel-dispatch gate before an epic plan is approved |
| `x-story-plan` | Produces input | Emits the `## File Footprint` blocks read here |
| `x-task-plan` | Produces input | Emits the per-task footprints used in `--scope=task` |
| `x-dev-*-implement` | Invokes (future, EPIC-0041 §5+) | Pre-dispatch safety check before parallel worktrees |

## Determinism (RULE-008)

Running the skill twice with the same inputs MUST produce byte-identical
outputs. Implementation rules that enforce this:

- All sets are stored as `TreeSet` (alphabetical iteration).
- Collisions are sorted by `(a, b)` ascending before rendering.
- Phase derivation is lexicographic when ties appear in the topological
  ordering.

## Forbidden

- Inferring footprints from prose — the skill MUST use the structured
  `## File Footprint` block only (RULE-001).
- Hard-failing when a footprint is missing — degrade with warning (RULE-005).
- Mutating any file other than the `--out` target.
