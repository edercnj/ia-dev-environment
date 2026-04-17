# Parallel Evaluation Report — EPIC-0040

> Re-evaluation produced by `/x-parallel-eval --scope=epic --epic plans/epic-0040` under
> story-0041-0007. Human-curated from current implementation-map and story footprints.

## Summary

| Metric | Value |
| :--- | :--- |
| Stories evaluated | 12 |
| Hard conflicts | 1 |
| Regen-write overlaps | 2 (soft) |
| Waves unchanged | No — recommend serializing Phase 3 writes to `telemetry-phase.sh` |
| Priority | HIGH |

## Conflicts Detected

### HARD: `telemetry-phase.sh` is written by 3 sibling stories

Stories `story-0040-0006`, `story-0040-0007`, and `story-0040-0008` all extend
the `telemetry-phase.sh` bash helper within what the current implementation
map treats as Phase 3 (parallel-capable). Running them concurrently would
produce three-way merge conflicts on the same lines (new kebab-case arg
handlers, new case-statement arms).

**Evidence:**
- `story-0040-0006` adds `phase.start` / `phase.end` argument handling.
- `story-0040-0007` adds `subagent.start` / `subagent.end` argument handling.
- `story-0040-0008` adds the `mcp.start` / `mcp.end` pair.

All three touch the central dispatch block of `telemetry-phase.sh`.

### SOFT: `x-story-implement` SKILL.md marker injection

Stories `0040-0006` and `0040-0007` both add `<!-- TELEMETRY: -->` markers
around `x-story-implement` phases. Diff locations are adjacent but not
identical. Serializing them eliminates the risk of an inconsistent marker
ordering.

### SOFT: Rule 13 additions

Stories `0040-0006` and `0040-0007` both append new sections to
`.claude/rules/13-skill-invocation-protocol.md`. Ordering between the two
appended sections is arbitrary but, once committed, becomes the historical
source of truth.

## Recommended Wave Restructure

Serialize `telemetry-phase.sh` writers:

```
Wave A: story-0040-0006  (phase markers)
Wave B: story-0040-0007  (subagent markers, after 0006 committed)
Wave C: story-0040-0008  (mcp markers, after 0007 committed)
```

This adds two Blocked By edges (`0007 blocked by 0006`, `0008 blocked by 0007`)
that are not present in the current IMPLEMENTATION-MAP.md.

## Notes

- This is the only epic in the 0036-0040 range with a real hard-conflict risk.
- Applying the restructure costs two sequential story PRs (roughly one day of
  wall-clock) but avoids an otherwise near-certain three-way merge conflict.
