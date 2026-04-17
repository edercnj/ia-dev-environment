# Parallelism Migration Recommendations

> Generated from `/x-parallel-eval` re-run retroactively over epics 0036..0040
> under story-0041-0007. Patches are **for human review only** — no
> implementation-map in the target epics has been modified.

## How to refresh

```bash
# Idempotent re-run — produces byte-identical output (RULE-008)
scripts/migrate-parallelism-eval.sh 0036 0037 0038 0039 0040
```

To replace a placeholder `parallel-eval-<EPIC>.md` with a real CLI run:

```bash
java -jar java/target/ia-dev-env-cli.jar parallel-eval \
    --scope=epic \
    --epic plans/epic-<EPIC> \
    --out plans/epic-0041/migrations/parallel-eval-<EPIC>.md
```

## Summary

| Epic | Hard Conflicts | Regen Overlaps | Priority |
| :--- | :--- | :--- | :--- |
| 0036 | 0 | 0 | — |
| 0037 | 0 | 1 (soft) | LOW |
| 0038 | 0 | 2 (soft) | — |
| 0039 | 0 | 0 | — |
| 0040 | **1** | 2 (soft) | **HIGH** |

## Per-Epic Detail

### EPIC-0036 — no action
- All 6 stories write to disjoint file sets or are already serialized via
  explicit `Blocked By` declarations (stories 0004 → 0005).
- Patch: `implementation-map-0036.diff` (empty-hunk placeholder).
- Report: `parallel-eval-0036.md`.

### EPIC-0037 — no action
- `x-git-worktree` SKILL.md is written by three stories but they are
  strictly chained (0002 → 0005 → 0009).
- Patch: `implementation-map-0037.diff` (empty-hunk placeholder).
- Report: `parallel-eval-0037.md`.

### EPIC-0038 — no action
- Task-first rewrite has inherent write-serialization via its
  `Blocked By` graph. Nothing to reorder.
- Patch: `implementation-map-0038.diff` (empty-hunk placeholder).
- Report: `parallel-eval-0038.md`.

### EPIC-0039 — no action
- All 5 stories touch disjoint files. Low structural risk.
- Patch: `implementation-map-0039.diff` (empty-hunk placeholder).
- Report: `parallel-eval-0039.md`.

### EPIC-0040 — **HIGH priority restructure**
- **Hard conflict:** stories `0040-0006`, `0040-0007`, and `0040-0008` all
  write to the central dispatch block of `.claude/hooks/telemetry-phase.sh`
  (new kebab-case argument handlers + new case-statement arms).
- **Recommended action:** add Blocked By edges `0007 ← 0006` and
  `0008 ← 0007` so Phase 3 becomes a three-step chain instead of a
  parallel block.
- **Cost:** two additional serialized story PRs (≈ one day of wall-clock).
- **Benefit:** eliminates an otherwise near-certain three-way merge
  conflict and removes the need for manual conflict resolution at wave
  boundaries.
- Patch: `implementation-map-0040.diff` (proposes the new Blocked By edges).
- Report: `parallel-eval-0040.md`.

## Applying the patches

Patches under this directory are advisory. A reviewer who agrees with the
proposed restructure for EPIC-0040 should:

1. Apply the diff manually:
   `patch -p1 < plans/epic-0041/migrations/implementation-map-0040.diff`
   (inside the epic's working tree, on a dedicated branch).
2. Verify the updated `IMPLEMENTATION-MAP.md` renders correctly and the
   dependency matrix still validates against the story files (Blocked By
   fields must reciprocate the new Blocks edges).
3. Open a follow-up story on `epic-0040` adding the serialization edges
   officially, not a direct commit — the implementation-map is a generated
   artifact in most flows.

For epics 0036/0037/0038/0039 the empty-hunk placeholder diffs require no
action and exist solely to satisfy the "5 diffs must exist" acceptance
criterion of this migration story.
