# STORY-008 Implementation Plan — Assembler Helpers

## Affected Layers

- `src/assembler/` — 4 new modules (copy-helpers, conditions, consolidator, auditor)
- `tests/node/assembler/` — 4 test files with temp file fixtures

## New Files

| File | Purpose |
|------|---------|
| `src/assembler/copy-helpers.ts` | Template file/tree copy with placeholder replacement |
| `src/assembler/conditions.ts` | Interface type predicates on ProjectConfig |
| `src/assembler/consolidator.ts` | File merge and framework rule grouping |
| `src/assembler/auditor.ts` | Rules directory audit with thresholds |
| `tests/node/assembler/copy-helpers.test.ts` | Unit tests for copy helpers |
| `tests/node/assembler/conditions.test.ts` | Unit tests for conditions |
| `tests/node/assembler/consolidator.test.ts` | Unit tests for consolidator |
| `tests/node/assembler/auditor.test.ts` | Unit tests for auditor |

## Dependencies

- `TemplateEngine` from `src/template-engine.ts` (STORY-005)
- `ProjectConfig`, `InterfaceConfig` from `src/models.ts` (STORY-003)
- `node:fs` for synchronous file operations (helpers intentionally use sync APIs)
- `node:path` for path manipulation

## Implementation Groups

1. **G1**: `conditions.ts` + tests (no fs dependency, pure logic)
2. **G2**: `auditor.ts` + tests (read-only fs, simple thresholds)
3. **G3**: `copy-helpers.ts` + tests (fs write + TemplateEngine)
4. **G4**: `consolidator.ts` + tests (fs read/write, pattern matching)

## Risk Assessment

- **Low risk**: All modules are stateless utility functions
- **Key concern**: Byte-for-byte parity with Python output (consolidator header/separators)
