# Test Plan — story-0003-0004

## Summary

This story modifies only Markdown content (`resources/core/13-story-decomposition.md`) and its 16 golden file copies. No TypeScript source code changes. Testing relies entirely on existing infrastructure.

## Test Strategy

### Golden File Tests (Existing — No Changes Needed)

- **File:** `tests/node/integration/byte-for-byte.test.ts`
- **What it validates:** Pipeline output matches golden files byte-for-byte for all 8 profiles
- **How it covers this story:** After updating the source of truth and copying to golden files, the pipeline will produce output identical to the updated golden files
- **Expected result:** All 8 profiles pass (40 test assertions)

### Routing Tests (Existing — No Changes Needed)

- **File:** `tests/node/domain/core-kp-routing.test.ts`
- **What it validates:** `13-story-decomposition.md` is routed to `story-planning` KP
- **Impact:** None — routing entry unchanged

## New Tests Required

None. The story is purely additive content changes to an existing Markdown file.

## Verification Steps

1. Run `npx vitest run tests/node/integration/byte-for-byte.test.ts` — all golden file comparisons pass
2. Run `npx vitest run` — full suite passes (1,384+ tests)
3. Coverage remains ≥ 95% line, ≥ 90% branch (no code changes, so coverage unchanged)
