# Implementation Plan: STORY-0003-0002 — Refactoring Guidelines for Coding Standards KP

## Summary

Add a new core resource file `14-refactoring-guidelines.md` containing refactoring triggers, prioritized techniques, and safety rules. Route it to the `coding-standards` knowledge pack via `core-kp-routing.ts`. Update all 8 golden-file profiles (3 output directories each: `.claude/`, `.agents/`, `.github/`). Adjust unit test assertions for the new route count.

---

## 1. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| Resource (content) | `resources/core/14-refactoring-guidelines.md` | **New file** |
| Domain (routing) | `src/domain/core-kp-routing.ts` | Modify: add 1 static route |
| Assembler | `src/assembler/rules-assembler.ts` | **No change** (generic `routeCoreToKps` picks up new route automatically) |
| Assembler | `src/assembler/codex-skills-assembler.ts` | **No change** (mirrors `.claude/skills/` automatically) |
| Assembler | `src/assembler/github-skills-assembler.ts` | **No change** (`.github/skills/coding-standards/` already has SKILL.md; references are not copied by this assembler) |
| Tests (unit) | `tests/node/domain/core-kp-routing.test.ts` | Modify: bump route counts (11 -> 12 static, 12 -> 13 active for microservice) |
| Tests (integration) | `tests/node/integration/byte-for-byte.test.ts` | **No change** (test structure unchanged; golden files drive assertions) |
| Golden files | `tests/golden/{profile}/.claude/skills/coding-standards/references/` | Add `refactoring-guidelines.md` to all 8 profiles |
| Golden files | `tests/golden/{profile}/.agents/skills/coding-standards/references/` | Add `refactoring-guidelines.md` to all 8 profiles |

---

## 2. New Files to Create

| File | Location | Description |
|------|----------|-------------|
| `14-refactoring-guidelines.md` | `resources/core/` | Source of truth for refactoring guidelines content (~80 lines). Contains 3 sections: Refactoring Triggers, Prioritized Techniques, Safety Rules. |
| `refactoring-guidelines.md` | `tests/golden/{profile}/.claude/skills/coding-standards/references/` | Golden copy (byte-identical to source) for all 8 profiles |
| `refactoring-guidelines.md` | `tests/golden/{profile}/.agents/skills/coding-standards/references/` | Codex mirror golden copy for all 8 profiles |

**Total new files: 1 source + 16 golden copies = 17 files**

---

## 3. Existing Files to Modify

### 3.1 `src/domain/core-kp-routing.ts`

**Change:** Add one entry to the `CORE_TO_KP_MAPPING` array.

```typescript
// Add after the line for "02-solid-principles.md" (keeps coding-standards routes grouped)
{ sourceFile: "14-refactoring-guidelines.md", kpName: "coding-standards", destFile: "refactoring-guidelines.md" },
```

**Also update:** JSDoc comment `/** 11 static routes ... */` to `/** 12 static routes ... */`.

### 3.2 `tests/node/domain/core-kp-routing.test.ts`

**Changes:**
- `contains_11_staticRoutes` assertion: update `toHaveLength(11)` to `toHaveLength(12)`
- `lastRoute_isStoryDecomposition` assertion: update index from `[10]` to `[11]` (the new route is inserted at position 2, shifting subsequent indices)
- `microservice_includes12Routes` assertion: update `toHaveLength(12)` to `toHaveLength(13)`
- `library_excludesCloudNative_returns11Routes` assertion: update `toHaveLength(11)` to `toHaveLength(12)`
- `monolith_includesCloudNative` assertion: update `toHaveLength(12)` to `toHaveLength(13)`

**Decision on insertion position:** The new route should be appended at the end of the `CORE_TO_KP_MAPPING` array (after story-decomposition), following the pattern of sequential numbering. This avoids shifting the index of `lastRoute_isStoryDecomposition`, requiring only the length assertions to change. However, the test for `lastRoute` must then change to expect `14-refactoring-guidelines.md` as the new last entry, OR the route can be appended at the end and the `lastRoute` test updated.

**Recommended approach:** Append at the end of the array. Update the `lastRoute` test to check index `[11]` for `14-refactoring-guidelines.md` (the new last route). If preserving the story-decomposition test is desired, add a separate test for the refactoring route.

### 3.3 No changes to `src/assembler/rules-assembler.ts`

The `routeCoreToKps()` method iterates `getActiveRoutes()` and copies each matching source file. Adding the route to `CORE_TO_KP_MAPPING` is sufficient -- the assembler picks it up automatically.

---

## 4. Dependency Direction Validation

```
resources/core/14-refactoring-guidelines.md  (static content, no code dependency)
          |
          v
src/domain/core-kp-routing.ts  (routing table references filename string)
          |
          v
src/assembler/rules-assembler.ts  (reads routing table, copies files)
          |
          v
Output: .claude/skills/coding-standards/references/refactoring-guidelines.md
          |
          v
src/assembler/codex-skills-assembler.ts  (mirrors .claude/ -> .agents/)
```

Direction: Content flows outward from resources through domain routing to assemblers to output. No inward dependency violations. Domain layer (`core-kp-routing.ts`) depends only on `ProjectConfig` (domain model) -- it does not import adapter or framework code.

---

## 5. Integration Points

| From | To | Mechanism |
|------|----|-----------|
| `core-kp-routing.ts` | `rules-assembler.ts` | `getActiveRoutes()` returns the new route; `routeCoreToKps()` copies the file |
| `.claude/skills/` output | `codex-skills-assembler.ts` | `CodexSkillsAssembler` scans `.claude/skills/coding-standards/references/` and copies to `.agents/skills/coding-standards/references/` |
| Golden files | `byte-for-byte.test.ts` | `verifyOutput()` compares generated output against golden files byte-for-byte |

---

## 6. Database Changes

None. This project is a CLI tool with no database.

---

## 7. API Changes

None. No REST/gRPC endpoints affected.

---

## 8. Event Changes

None. No event-driven components affected.

---

## 9. Configuration Changes

None. No config templates or environment variables need updating. The `setup-config.{profile}.yaml` files are unchanged because the new core file is unconditionally routed (not conditional on any config value).

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden file mismatch breaks byte-for-byte tests | **High** (expected) | **Blocking** | Regenerate all 16 golden copies (8 profiles x 2 dirs) from the source file before running tests |
| Route count assertions fail in unit tests | **High** (expected) | **Blocking** | Update all count assertions in `core-kp-routing.test.ts` as described in section 3.2 |
| Content duplication with existing CC-05 (DRY) or SOLID | **Low** | **Minor** | The refactoring file covers *when* and *how* to refactor; CC-05 covers the DRY principle itself. Cross-reference but do not duplicate |
| File numbering collision | **None** | **None** | Verified: numbers 04 and 14 are unused in routing; 14 is the next available after 13 |
| Backward compatibility regression | **None** | **None** | Additive change only -- no existing content modified, no routes removed |
| `.github/skills/coding-standards/` missing references | **None** | **None** | The `GithubSkillsAssembler` generates SKILL.md files only (from templates), not references. References in `.github/` are NOT expected. Only `.claude/` and `.agents/` carry references. Confirmed by golden file inspection. |

---

## Implementation Order

1. **Create content:** `resources/core/14-refactoring-guidelines.md`
2. **Add route:** `src/domain/core-kp-routing.ts` -- append entry to `CORE_TO_KP_MAPPING`
3. **Update unit tests:** `tests/node/domain/core-kp-routing.test.ts` -- fix assertions
4. **Run pipeline:** Execute `npx vitest run tests/node/domain/core-kp-routing.test.ts` to verify routing
5. **Regenerate golden files:** Run the pipeline for all 8 profiles and copy output to golden directories
6. **Run full test suite:** `npx vitest run` to confirm byte-for-byte parity and full coverage
7. **Verify coverage:** Ensure >= 95% line, >= 90% branch coverage is maintained

---

## Content Specification for `14-refactoring-guidelines.md`

The file must contain exactly these sections (per story acceptance criteria):

```
## Refactoring Guidelines
### Refactoring Triggers
- Function > 25 lines -> Extract Method
- Class > 250 lines -> Extract Class
- Method used once, no readability benefit -> Inline Method
- Name does not reveal intent (CC-01) -> Rename
- Duplicated code (3+ lines, CC-05) -> Extract shared function
- Conditional logic growing -> Replace Conditional with Polymorphism

### Prioritized Techniques (TDD Frequency Order)
1. Extract Method
2. Rename Variable/Method/Class
3. Replace Magic Number with Named Constant
4. Extract Interface (DIP)
5. Move Method (SRP)
6. Replace Conditional with Polymorphism

### Safety Rules
1. ALL tests must be GREEN before starting any refactoring
2. ALL tests must remain GREEN after each refactoring step
3. NEVER add behavior during refactoring
4. Refactoring is a sequence of small, safe steps -- each independently reversible
5. If any test breaks during refactoring, UNDO the last step immediately
```

Language: English only (per RULE-012: Generated Content Language).
