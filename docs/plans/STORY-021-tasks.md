# Task Decomposition -- STORY-021: Lib Skills in GitHub Skills Assembler

**Status:** PENDING
**Date:** 2026-03-11
**Blocked By:** STORY-014 (GitHub Assemblers) -- complete
**Blocks:** STORY-016 (pipeline orchestrator)

---

## G1 -- Foundation (GitHub lib skill templates)

**Purpose:** Create the 3 GitHub skill template files for lib skills. These are pure markdown data files adapted from the existing `.claude/skills/` source templates (`resources/skills-templates/core/lib/`) to the GitHub Copilot format (YAML frontmatter with `name` + `description`, markdown body). No TypeScript code changes.
**Dependencies:** None
**Compiles independently:** N/A -- no TypeScript changes in this group.

### T1.1 -- Create `x-lib-task-decomposer.md` GitHub skill template

- **File:** `resources/github-skills-templates/lib/x-lib-task-decomposer.md` (create)
- **What to implement:**
  - YAML frontmatter with `name: x-lib-task-decomposer` and `description: ...` (adapted from source at `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md`)
  - Markdown body adapted for GitHub Copilot context (remove Claude-specific references like `.claude/skills/`, adapt paths to `.github/skills/` equivalents)
  - Keep `{project_name}` single-brace placeholders for `TemplateEngine.replacePlaceholders()` processing
  - Follow the format of existing GitHub skill templates (e.g., `resources/github-skills-templates/dev/x-dev-implement.md`)
- **Source reference:** `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md`
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T1.2 -- Create `x-lib-audit-rules.md` GitHub skill template

- **File:** `resources/github-skills-templates/lib/x-lib-audit-rules.md` (create)
- **What to implement:**
  - YAML frontmatter with `name: x-lib-audit-rules` and `description: ...` (adapted from source at `resources/skills-templates/core/lib/x-lib-audit-rules/SKILL.md`)
  - Markdown body adapted for GitHub Copilot context
  - Same conventions as T1.1
- **Source reference:** `resources/skills-templates/core/lib/x-lib-audit-rules/SKILL.md`
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T1.3 -- Create `x-lib-group-verifier.md` GitHub skill template

- **File:** `resources/github-skills-templates/lib/x-lib-group-verifier.md` (create)
- **What to implement:**
  - YAML frontmatter with `name: x-lib-group-verifier` and `description: ...` (adapted from source at `resources/skills-templates/core/lib/x-lib-group-verifier/SKILL.md`)
  - Markdown body adapted for GitHub Copilot context
  - Same conventions as T1.1
- **Source reference:** `resources/skills-templates/core/lib/x-lib-group-verifier/SKILL.md`
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### Compilation checkpoint G1

```
# No compilation needed -- pure resource files
ls resources/github-skills-templates/lib/   # verify 3 files exist
```

---

## G2 -- Core (assembler modifications for lib group + nesting)

**Purpose:** Add the "lib" group to `SKILL_GROUPS`, introduce `NESTED_GROUPS` set, and modify `generateGroup`/`renderSkill` to support an optional subdirectory prefix so lib skills are output under `github/skills/lib/{skillName}/SKILL.md` instead of the flat `github/skills/{skillName}/SKILL.md`.
**Dependencies:** G1 (templates must exist for integration testing, but compilation is independent)
**Compiles independently:** Yes -- no new imports or dependencies introduced.

### T2.1 -- Add `NESTED_GROUPS` constant and "lib" group to `SKILL_GROUPS`

- **File:** `src/assembler/github-skills-assembler.ts` (modify)
- **What to implement:**
  1. Add `const NESTED_GROUPS = new Set(["lib"]);` after the existing `INFRA_GROUP` constant (line 17), and export it for testability.
  2. Add `"lib"` entry to `SKILL_GROUPS` after the `"git-troubleshooting"` entry:
     ```typescript
     "lib": ["x-lib-task-decomposer", "x-lib-audit-rules", "x-lib-group-verifier"],
     ```
  3. Group count increases from 7 to 8.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T2.2 -- Add `subDir` parameter to `renderSkill` for nested output paths

- **File:** `src/assembler/github-skills-assembler.ts` (modify)
- **What to implement:**
  1. Add optional `subDir?: string` parameter to `renderSkill` method signature.
  2. Modify the `skillDir` path calculation:
     ```typescript
     const skillDir = subDir
       ? path.join(outputDir, "github", "skills", subDir, name)
       : path.join(outputDir, "github", "skills", name);
     ```
  3. All existing calls to `renderSkill` continue to work without `subDir` (parameter is optional, defaults to `undefined`).
- **Dependencies on other tasks:** None (can be done in parallel with T2.1)
- **Estimated complexity:** S

### T2.3 -- Add `subDir` parameter to `generateGroup` and wire nesting in `assemble`

- **File:** `src/assembler/github-skills-assembler.ts` (modify)
- **What to implement:**
  1. Add optional `subDir?: string` parameter to `generateGroup` method signature.
  2. Pass `subDir` through to each `renderSkill` call:
     ```typescript
     const dest = this.renderSkill(engine, srcDir, outputDir, name, subDir);
     ```
  3. In the `assemble` method, compute `subDir` for each group and pass it to `generateGroup`:
     ```typescript
     const subDir = NESTED_GROUPS.has(group) ? group : undefined;
     results.push(
       ...this.generateGroup(engine, srcDir, outputDir, filtered, subDir),
     );
     ```
- **Dependencies on other tasks:** T2.1 (needs `NESTED_GROUPS`), T2.2 (needs `renderSkill` updated)
- **Estimated complexity:** S

### Compilation checkpoint G2

```
npx tsc --noEmit   # zero errors with lib group + nesting support
```

---

## G3 -- Tests (unit tests for lib group, nesting, assemble output)

**Purpose:** Add comprehensive unit tests covering the new lib group, subdirectory nesting, and all edge cases. Update existing group count assertion.
**Dependencies:** G1 (templates for reference), G2 (source code must compile)
**Pattern reference:** Existing tests in `tests/node/assembler/github-skills-assembler.test.ts` for structure, temp dir management, and helper conventions.

### T3.1 -- Update existing group count test and add `createAllLibSkills` helper

- **File:** `tests/node/assembler/github-skills-assembler.test.ts` (modify)
- **What to implement:**
  1. Update `SKILL_GROUPS_has7Groups` test: change assertion from `toHaveLength(7)` to `toHaveLength(8)`. Rename test to `SKILL_GROUPS_has8Groups`.
  2. Add `NESTED_GROUPS` to the import from `github-skills-assembler.js`.
  3. Add helper function following the `createAllInfraSkills` pattern:
     ```typescript
     function createAllLibSkills(resourcesDir: string): void {
       for (const name of SKILL_GROUPS["lib"]!) {
         createSkillTemplate(resourcesDir, "lib", name);
       }
     }
     ```
- **Dependencies on other tasks:** G2 (needs `NESTED_GROUPS` export, `SKILL_GROUPS` with "lib")
- **Estimated complexity:** S

### T3.2 -- Add `describe("GithubSkillsAssembler -- lib group")` test block

- **File:** `tests/node/assembler/github-skills-assembler.test.ts` (modify, append new describe block)
- **What to implement:**
  New describe block with `beforeEach`/`afterEach` temp dir management (same pattern as existing `"GithubSkillsAssembler -- assemble"` block), containing:

  1. `assemble_libGroup_generates3LibSkills` -- Create all 3 lib templates via `createAllLibSkills`, assemble, verify result contains 3 lib skill paths.
  2. `assemble_libGroup_outputNestedUnderLib` -- Verify all output paths contain `github/skills/lib/` segment (not flat `github/skills/x-lib-*`).
  3. `assemble_libGroup_templateDirMissing_skipsGroup` -- No lib template dir in resources, verify 0 lib files in result.
  4. `assemble_libGroup_partialTemplates_generatesAvailable` -- Only 1 of 3 lib templates exists (e.g., `x-lib-task-decomposer`), verify only 1 file generated.
  5. `assemble_libGroup_appliesPlaceholderReplacement` -- Template contains `{project_name}`, verify output file contains `my-app`.
  6. `assemble_libGroup_outputStructure_lib_skillName_SKILL_md` -- Verify exact path: `outputDir/github/skills/lib/x-lib-task-decomposer/SKILL.md`.
  7. `assemble_mixedGroups_libNestedOthersFlat` -- Create both lib templates and dev templates. Verify lib skills are under `github/skills/lib/` while dev skills remain flat under `github/skills/` (no nesting).
  8. `assemble_libGroupNotFiltered_allSkillsGenerated` -- Use config with all infra set to `"none"` (which filters out infra skills). Verify lib skills are still generated (unconditional, no config-based filtering).
  9. `NESTED_GROUPS_containsLib` -- Verify `NESTED_GROUPS.has("lib")` returns `true` and `NESTED_GROUPS.size` is `1`.
- **Dependencies on other tasks:** T3.1 (needs helper and updated imports)
- **Estimated complexity:** M

### Test execution checkpoint G3

```
npx vitest run tests/node/assembler/github-skills-assembler.test.ts
```

---

## G4 -- Verification (coverage validation, compilation check)

**Purpose:** Final verification that all code compiles cleanly, all tests pass, and coverage thresholds are met for the modified assembler.
**Dependencies:** G1, G2, G3 all complete.

### T4.1 -- Full compilation check

- **Command:** `npx tsc --noEmit`
- **Expected:** Zero errors across the entire project.
- **Dependencies on other tasks:** G2

### T4.2 -- Run all GithubSkillsAssembler tests

- **Command:** `npx vitest run tests/node/assembler/github-skills-assembler.test.ts`
- **Expected:** All tests pass (existing + new).
- **Dependencies on other tasks:** G3

### T4.3 -- Coverage verification

- **Command:** `npx vitest run --coverage tests/node/assembler/github-skills-assembler.test.ts`
- **Expected:** >= 95% line coverage, >= 90% branch coverage on `src/assembler/github-skills-assembler.ts`.
- **Coverage strategy:**
  - `NESTED_GROUPS.has(group)` exercised for both `true` (lib) and `false` (dev, story, etc.)
  - `subDir` present/undefined branch exercised in `renderSkill` and `generateGroup`
  - Lib template exists/missing branches exercised
  - Mixed groups (lib nested + non-lib flat) in single `assemble` call
  - Lib group not subject to `filterSkills` config-based filtering (unconditional)
- **Dependencies on other tasks:** G3

### T4.4 -- Verify output structure parity with story acceptance criteria

- **What to verify (manual or via test):**
  1. 3 lib skills generated in `github/skills/lib/` directory
  2. Templates exist in `resources/github-skills-templates/lib/`
  3. Group "lib" present in `SKILL_GROUPS` with 3 skills
  4. Nesting preserved: files in `github/skills/lib/` not `github/skills/`
  5. Lib skills have GitHub Copilot-compatible format (YAML frontmatter)
- **Dependencies on other tasks:** G1, G2, G3

---

## Summary Table

| Group | Purpose | Files to Create | Files to Modify | Tasks | Test Cases | Complexity |
|-------|---------|----------------|----------------|-------|------------|------------|
| G1 | Lib GitHub skill templates | 3 (resources) | 0 | 3 | 0 | S |
| G2 | Assembler: lib group + nesting | 0 | 1 (src) | 3 | 0 | S |
| G3 | Unit tests | 0 | 1 (test) | 2 | ~9 | M |
| G4 | Verification | 0 | 0 | 4 | 0 (verification) | S |
| **Total** | | **3 new files** | **2 modified** | **12 tasks** | **~9 test cases** | |

## Dependency Graph

```
G1: FOUNDATION (templates) ----+
                                |
G2: CORE (assembler changes) --+--> G3: TESTS --> G4: VERIFICATION
```

- G1 and G2 are independent of each other for compilation purposes (G1 is pure resources, G2 is TypeScript).
- G3 depends on both G1 (templates as source reference) and G2 (compiled source).
- G4 depends on G3 (tests must exist to verify coverage).

## File Inventory

### Resource files (3 new)

| File | Content |
|------|---------|
| `resources/github-skills-templates/lib/x-lib-task-decomposer.md` | GitHub skill template (YAML frontmatter + markdown) |
| `resources/github-skills-templates/lib/x-lib-audit-rules.md` | GitHub skill template (YAML frontmatter + markdown) |
| `resources/github-skills-templates/lib/x-lib-group-verifier.md` | GitHub skill template (YAML frontmatter + markdown) |

### Source files (1 modified)

| File | Change |
|------|--------|
| `src/assembler/github-skills-assembler.ts` | Add `NESTED_GROUPS` constant, add "lib" to `SKILL_GROUPS`, add `subDir` param to `renderSkill`/`generateGroup`, wire nesting in `assemble` |

### Test files (1 modified)

| File | Change |
|------|--------|
| `tests/node/assembler/github-skills-assembler.test.ts` | Update group count test (7->8), add `createAllLibSkills` helper, add `NESTED_GROUPS` import, add 9 new tests in lib group describe block |

## Key Implementation Notes

1. **Nesting is opt-in via `NESTED_GROUPS`:** Only the "lib" group uses subdirectory nesting. All other groups continue with the flat `github/skills/{name}/` structure. This avoids breaking changes.
2. **Lib skills are unconditional:** Unlike infrastructure skills which are filtered by config predicates, lib skills are always generated. The `filterSkills` method returns all skills for non-infrastructure groups, so no change needed.
3. **Template format consistency:** The 3 new GitHub skill templates must follow the same YAML frontmatter + markdown body format as existing templates (e.g., `x-dev-implement.md`). Adapt content from `.claude/skills/` source templates, not copy verbatim.
4. **`{single_brace}` placeholders:** Templates use `{project_name}` for TemplateEngine processing. Do not use `{{double_brace}}` in these templates.
5. **Existing `createSkillTemplate` helper works for lib:** The test helper creates templates at `resourcesDir/github-skills-templates/{group}/{skillName}.md`. For lib, group is `"lib"` and `mkdirSync({ recursive: true })` handles the nested path.
6. **`countGithubSkills` in ReadmeAssembler (STORY-015) already handles nesting:** The recursive `SKILL.md` counting under `github/skills/` will automatically discover lib skills in the nested `lib/` subdirectory.
