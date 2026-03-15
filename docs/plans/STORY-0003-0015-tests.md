# Test Plan -- STORY-0003-0015

## Test Strategy

This story is a **template/content-only change** (no TypeScript code modified). The primary verification mechanism is the existing golden file byte-for-byte parity test suite (`tests/node/integration/byte-for-byte.test.ts`), supplemented by targeted content assertions to confirm the 6 TDD checklist items, scoring update, and KP path update are correctly applied.

## Files Under Change

### Source Templates (2 files)

| File | Role |
|------|------|
| `resources/skills-templates/core/x-review/SKILL.md` | Source for `.claude/` and `.agents/` output |
| `resources/github-skills-templates/review/x-review.md` | Source for `.github/` output |

### Golden Files (24 files = 8 profiles x 3 output dirs)

Each profile produces `x-review/SKILL.md` in three output directories:

| Output Dir | Path Pattern |
|------------|--------------|
| `.claude/` | `tests/golden/{profile}/.claude/skills/x-review/SKILL.md` |
| `.agents/` | `tests/golden/{profile}/.agents/skills/x-review/SKILL.md` |
| `.github/` | `tests/golden/{profile}/.github/skills/x-review/SKILL.md` |

**Profiles:** go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs

---

## Test Scenarios

### 1. QA checklist contains 18 items (12 original + 6 TDD)

**Type:** Golden file content validation
**Files:** All 24 golden `x-review/SKILL.md` files + both source templates
**Verify:**
- QA checklist line starts with `**QA (18 items, /36):**`
- All 12 original items remain present: `Test exists for each AC`, `line coverage`, `branch coverage`, `test naming convention`, `AAA pattern`, `parametrized tests`, `exception paths tested`, `no test interdependency`, `fixtures centralized`, `unique test data`, `edge cases`, `integration tests for DB/API`
- All 6 new TDD items are present: `commits show test-first pattern`, `explicit refactoring after green`, `tests follow TPP progression`, `no test written after implementation`, `acceptance tests validate E2E behavior`, `TDD coverage thresholds maintained`

### 2. QA score denominator updated to /36 in checklist line

**Type:** Content assertion
**Files:** All 24 golden files + both source templates
**Verify:**
- QA checklist line contains `/36)` (not `/24)`)
- QA checklist line contains `18 items` (not `12 items`)

### 3. QA score denominator updated to /36 in consolidation table

**Type:** Content assertion
**Files:** All 24 golden files + both source templates
**Verify:**
- Phase 3 consolidation table contains `| QA            | XX/36 |` (not `XX/24`)

### 4. QA engineer KP paths include TDD references

**Type:** Content assertion
**Files:** All 24 golden files + both source templates
**Verify:**
- QA row in "Engineer -> Knowledge Pack Mapping" table includes focus instruction: `focus on TDD Workflow, Double-Loop TDD, and TPP sections`
- Original QA KP paths (`testing-philosophy.md`, `testing-conventions.md`) remain present

### 5. Other engineer checklists unchanged

**Type:** Regression / backward compatibility
**Files:** All 24 golden files + both source templates
**Verify:**
- Security checklist remains `**Security (10 items, /20):**` with same 10 items
- Performance checklist remains `**Performance (13 items, /26):**` with same 13 items
- Database checklist remains `**Database (8 items, /16):**` with same 8 items
- Observability checklist remains `**Observability (9 items, /18):**` with same 9 items
- DevOps checklist remains `**DevOps (10 items, /20):**` with same 10 items
- API checklist remains `**API (8 items, /16):**` with same 8 items
- Event checklist remains `**Event (14 items, /28):**` with same 14 items

### 6. Parallel review pattern preserved

**Type:** Structural validation
**Files:** All 24 golden files + both source templates
**Verify:**
- Phase 2 still contains "Launch one `general-purpose` subagent per applicable engineer"
- Phase 2 still contains "ALL review subagents MUST be launched in a SINGLE message"
- All 4 phases (1: Detect, 2: Parallel Reviews, 3: Consolidation, 4: Story Generation) remain present and in order

### 7. Triple-copy consistency (.claude, .agents, .github)

**Type:** Content comparison
**Files:** Per-profile golden file triplet
**Verify:**
- For each profile, `.claude/` and `.agents/` golden copies are byte-identical (they share the same source template)
- `.github/` golden copy contains the same QA checklist updates (18 items, /36) but may differ in KP path format (`.github/skills/` prefix vs bare `skills/` prefix) and em-dash vs double-dash formatting

### 8. Byte-for-byte parity passes for all 8 profiles

**Type:** Integration (existing test)
**Test file:** `tests/node/integration/byte-for-byte.test.ts`
**Verify:** `npm test -- tests/node/integration/byte-for-byte.test.ts` passes for all 8 profiles with:
- `pipelineSuccessForProfile_{profile}` -- pipeline runs without error
- `pipelineMatchesGoldenFiles_{profile}` -- generated output matches golden files byte-for-byte
- `noMissingFiles_{profile}` -- no golden files missing from output
- `noExtraFiles_{profile}` -- no unexpected files in output

### 9. No template artifacts in output

**Type:** Content validation
**Files:** Both source templates
**Verify:**
- No leftover `{{`, `{%`, or `{#` template syntax in source templates
- QA checklist updates use consistent character encoding (em-dash vs `>=` matches per-template conventions)

### 10. Source template encoding conventions preserved

**Type:** Content validation
**Verify:**
- `resources/skills-templates/core/x-review/SKILL.md` uses em-dash (`--`) and Unicode `>=` (`≥`) for QA checklist
- `resources/github-skills-templates/review/x-review.md` uses double-dash (`--`) and ASCII `>=` for QA checklist
- These conventions match the existing content patterns in each template

---

## Execution Plan

1. **Modify source templates** -- Add 6 TDD items to QA checklist, update item count (18), update denominator (/36), update KP paths in both source files
2. **Regenerate golden files** -- Run the pipeline for all 8 profiles to produce updated golden files in all 3 output dirs (24 files total)
3. **Run byte-for-byte tests** -- `npm test -- tests/node/integration/byte-for-byte.test.ts` must pass for all 8 profiles
4. **Run full test suite** -- `npm test` to verify no regressions (1,384+ tests across 46 files)
5. **Content spot-checks** -- Grep-verify scenarios 1-7 across golden files:
   - `grep -c "18 items, /36" tests/golden/*/.*claude*/skills/x-review/SKILL.md` should return 16 matches (8 profiles x 2 dirs)
   - `grep -c "18 items, /36" tests/golden/*/.github/skills/x-review/SKILL.md` should return 8 matches
   - `grep -c "12 items, /24" tests/golden/*/.*skills/x-review/SKILL.md` should return 0 matches
   - `grep "Security (10 items" tests/golden/go-gin/.claude/skills/x-review/SKILL.md` should still match
   - `grep "XX/36" tests/golden/go-gin/.claude/skills/x-review/SKILL.md` should match in consolidation table

## Risk Areas

- **Character encoding mismatch**: The `.claude` template uses em-dashes and Unicode symbols while the `.github` template uses ASCII equivalents. TDD items must follow each template's convention or byte-for-byte tests will fail.
- **Golden file count**: Must be exactly 24 files updated (8 profiles x 3 dirs). Missing any profile or output dir will cause `noMissingFiles` or mismatch failures.
- **Snapshot test**: `tests/node/__snapshots__/codex-templates.test.ts.snap` does not contain x-review content (it covers agents-md and config-toml templates), so no snapshot update is needed for this story.
