---
name: parallelism-heuristics
description: "Canonical catalog of file-collision heuristics for parallel task, story, and epic execution. Defines the File Footprint block format, the three Conflict Categories (hard / regen / soft), a hotspot list of high-contention paths, and the degrade-with-warning policy. Consumed by x-task-plan, x-story-plan, x-epic-map, x-parallel-eval, and x-dev-*-implement."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: Parallelism Heuristics

## Purpose

Single source of truth for detecting file-level collisions between work units
(tasks, stories, or epics) that might run in parallel. Downstream planning and
execution skills consume this pack so they all apply the same rules when
deciding whether two units can safely run concurrently, must be serialized, or
must be merged into a single commit.

Without this pack, each skill would re-implement its own heuristics and the
catalog of hotspots (hand-curated high-contention paths such as shared
assemblers and root-level metadata files) would drift between them. The pack
keeps the catalog auditable — edits here propagate to every consumer.

## Scope

- **Applies to:** any work unit that can declare a `File Footprint` block in
  its markdown artifact (task file, story file, epic file, or plan file).
- **Consumed by (initial):** `x-task-plan`, `x-story-plan`, `x-epic-map`,
  `x-parallel-eval`, `x-dev-*-implement`.
- **Not applicable to:** units that do not touch files directly (pure Q&A
  skills, reporting skills, lint-only skills).

## Footprint Format

Every planning artifact that participates in parallelism analysis MUST declare
a `## File Footprint` section using the canonical shape below. Subsections
MUST appear in the order `write:` → `read:` → `regen:` (any may be empty and
omitted). Paths are repository-relative and one per bullet. Glob patterns
(`**/*.java`, `src/test/resources/golden/**`) are permitted.

```markdown
## File Footprint

### write:
- path/to/file1.java
- path/to/file2.md

### read:
- path/to/contextual.java

### regen:
- src/test/resources/golden/**
```

Subsection semantics:

| Subsection | Meaning |
| :--- | :--- |
| `write:` | Paths the unit intentionally creates or modifies by hand. |
| `read:` | Paths the unit needs to read for context; NOT modified. |
| `regen:` | Paths that will be regenerated as a side-effect of a build step (e.g., `mvn process-resources`, golden regeneration). The unit does not edit them manually. |

## Conflict Categories (RULE-003)

Given two units A and B with footprints, pairwise overlap is classified as:

| # | Category | Detection | Resolution |
| :--- | :--- | :--- | :--- |
| 1 | **Hard**  | `A.write ∩ B.write ≠ ∅` | Serialize — never run in parallel; pick a deterministic order (e.g., task ID ascending). |
| 2 | **Regen** | `A.write ∩ B.regen ≠ ∅` **or** `A.regen ∩ B.regen ≠ ∅` | Serialize — regenerated outputs will be clobbered if both run concurrently. |
| 3 | **Soft**  | Overlap only in `read:` (`A.read ∩ B.read ≠ ∅` with no write/regen overlap) | Ignore — read-only overlap is safe for parallelism. |

No overlap at all → units are independent and MAY run in parallel without
further checks.

## Hotspot Catalog

High-contention paths extracted from the repository's history of merge
conflicts. Whenever a unit touches one of these, planning skills SHOULD warn
the author and encourage splitting or serializing the work.

The list below is the initial seed (story-0041-0001). Later stories add new
entries as hotspots are observed; the catalog is intentionally auditable.

- `java/src/main/java/dev/iadev/application/assembler/SettingsAssembler.java`
- `java/src/main/java/dev/iadev/application/assembler/HooksAssembler.java`
- `java/src/main/java/dev/iadev/application/assembler/SkillsAssembler.java`
- `CLAUDE.md`
- `.gitignore`
- `CHANGELOG.md`
- `pom.xml`
- `java/src/test/resources/golden/**`
- `.claude/templates/**` (regen-only; hand-edits here violate RULE-007 —
  source of truth lives under `java/src/main/resources/targets/claude/`)

## Degradation Policy (RULE-005)

Units MAY omit the `## File Footprint` block. Consumers MUST degrade
gracefully:

1. Treat a missing or empty footprint as "unknown" — NOT as "empty".
2. Emit a `WARNING` log line identifying the unit and the skill that noticed
   the omission.
3. Disable parallel dispatch for that unit: schedule it serially with every
   other unit in the same wave to avoid accidental conflicts.
4. NEVER hard-fail solely because the footprint is absent; authors may
   back-fill later and re-running planning should recover parallelism.

Malformed footprints (wrong headings, missing subsections, unparseable bullet
lines) follow the same rule: warn, treat as unknown, serialize.

## Examples

### Example 1 — Simple isolated task (independent)

```markdown
## File Footprint

### write:
- java/src/main/java/dev/iadev/domain/model/Greeter.java

### read:
- java/src/main/java/dev/iadev/domain/model/Person.java
```

Two tasks with this shape but different `write:` paths are independent and run
in parallel.

### Example 2 — Task with regen side-effect

```markdown
## File Footprint

### write:
- java/src/main/resources/targets/claude/rules/21-new-rule.md

### regen:
- .claude/rules/21-new-rule.md
- java/src/test/resources/golden/**
```

Any other task touching `java/src/test/resources/golden/**` creates a **Regen**
conflict and MUST be serialized.

### Example 3 — Story aggregating multiple tasks

```markdown
## File Footprint

### write:
- java/src/main/java/dev/iadev/application/assembler/SkillsAssembler.java
- java/src/main/java/dev/iadev/application/assembler/SkillsCopyHelper.java

### read:
- java/src/main/resources/targets/claude/skills/**

### regen:
- .claude/skills/**
- java/src/test/resources/golden/**
```

Touches two hotspots (`SkillsAssembler.java` and
`java/src/test/resources/golden/**`). Planning skills SHOULD warn the author
and serialize this story against any sibling touching the same files.

## References

- `story-planning` KP (layered decomposition, dependency DAG) — complementary
  knowledge pack that defines the units this pack classifies.
