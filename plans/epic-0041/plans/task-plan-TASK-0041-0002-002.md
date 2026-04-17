# Task Plan: TASK-0041-0002-002

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0041-0002-002 |
| Story ID | story-0041-0002 |
| Epic ID | epic-0041 |
| Layer | Domain |
| Type | Unit |
| TDD Cycles | 4 |
| Estimated Effort | M |
| Generated | 2026-04-17 |

## Objective

Provide a tolerant Java parser (`FileFootprintParser`) that extracts the `## File Footprint` block from a task plan's markdown body into an immutable, alphabetically-sorted `FileFootprint` record. A plan that predates the block returns `FileFootprint.EMPTY` with a single warning log line (RULE-006 backward compatibility).

## Implementation Guide

### Target Class/Method

- `dev.iadev.parallelism.FileFootprint` — record with `writes`, `reads`, `regens` sets (alphabetical via `TreeSet`).
- `dev.iadev.parallelism.FileFootprintParser#parse(String)` — line-scan parser.

### Design Pattern

Plain-value record + stateless static parser. No external dependencies; JUL logger only.

### Implementation Steps (Layer Order)

1. Create record `FileFootprint(Set<String>, Set<String>, Set<String>)` with compact constructor normalization and `EMPTY` sentinel.
2. Implement parser with 4 helpers: `locateHeader`, `classifySubHeader`, `extractBulletPath`, `appendPath`.
3. Add 5 fixtures under `src/test/resources/fixtures/parallelism/`.
4. Add 9 unit tests covering degenerate / happy / boundary / backward-compat / determinism cases.

## TDD Cycles

| # | Level | Red | Green |
|---|-------|-----|-------|
| 1 | Degenerate | `parse(null)` returns EMPTY | early-return on blank input |
| 2 | Happy | `parse(simple)` yields single write | sub-section classification |
| 3 | Boundary | out-of-order paths sort alphabetically | `TreeSet` normalization in record |
| 4 | Backward-compat | legacy plan (no block) returns EMPTY + warn | `locateHeader` negative path |

## Affected Files

| # | Path | Action | Layer | Purpose |
|---|------|--------|-------|---------|
| 1 | `java/src/main/java/dev/iadev/parallelism/FileFootprint.java` | CREATE | Domain | Record |
| 2 | `java/src/main/java/dev/iadev/parallelism/FileFootprintParser.java` | CREATE | Domain | Parser |
| 3 | `java/src/test/java/dev/iadev/parallelism/FileFootprintParserTest.java` | CREATE | Test | Unit tests |
| 4 | `java/src/test/resources/fixtures/parallelism/footprint-*.md` | CREATE | Test fixture | 5 fixtures |

## Security Checklist

- [ ] Parser tolerates malformed input (no exception on arbitrary markdown). Severity: MEDIUM. Reference: CWE-20.

## Dependencies

| Depends On | Reason |
|------------|--------|
| TASK-0041-0002-001 | Plan authors emit the block this parser consumes |

## File Footprint

### write:
- `java/src/main/java/dev/iadev/parallelism/FileFootprint.java`
- `java/src/main/java/dev/iadev/parallelism/FileFootprintParser.java`
- `java/src/test/java/dev/iadev/parallelism/FileFootprintParserTest.java`
- `java/src/test/resources/fixtures/parallelism/footprint-empty.md`
- `java/src/test/resources/fixtures/parallelism/footprint-full.md`
- `java/src/test/resources/fixtures/parallelism/footprint-legacy.md`
- `java/src/test/resources/fixtures/parallelism/footprint-partial.md`
- `java/src/test/resources/fixtures/parallelism/footprint-simple.md`

### read:
- `java/src/main/resources/targets/claude/skills/knowledge-packs/parallelism-heuristics/SKILL.md`

## Definition of Done

- [ ] Record + parser compile cleanly
- [ ] `FileFootprintParserTest` — 9 tests green
- [ ] ≥ 95% line coverage on new classes
- [ ] Legacy fixture returns EMPTY without throwing
