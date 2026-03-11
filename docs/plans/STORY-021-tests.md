# Test Plan — STORY-021: Lib Skills in GitHub Skills Assembler

## Scope

Unit tests for the **lib group** addition to `GithubSkillsAssembler`. The story
adds a `"lib"` entry to `SKILL_GROUPS` with 3 skills (`x-lib-task-decomposer`,
`x-lib-audit-rules`, `x-lib-group-verifier`), introduces `NESTED_GROUPS` to
control subdirectory nesting, and modifies `renderSkill`/`generateGroup` to
accept an optional `subDir` parameter. Tests verify the new group is registered,
lib skills are generated under `github/skills/lib/`, non-lib groups remain flat,
placeholder replacement works, and edge cases (missing templates, partial
templates) are handled gracefully.

**Target file:** `tests/node/assembler/github-skills-assembler.test.ts`

**Coverage targets:** >= 95% line, >= 90% branch.

---

## Conventions

- Test names follow `[methodOrBehavior]_[scenario]_[expectedBehavior]`.
- `buildConfig(overrides?)` helper constructs a `ProjectConfig` with optional
  `orchestrator`, `container`, `iac`, `templating` fields. Defaults produce a
  minimal valid config with all infra set to `"none"`.
- `createSkillTemplate(resourcesDir, group, skillName, content?)` helper creates
  template files at `resourcesDir/github-skills-templates/{group}/{skillName}.md`.
- `TemplateEngine` is instantiated with `new TemplateEngine(resourcesDir, config)`.
- All tests use `mkdtempSync`-backed temporary directories, cleaned up in
  `afterEach` with `fs.rmSync(tmpDir, { recursive: true, force: true })`.
- Imports from source: `SKILL_GROUPS`, `INFRA_SKILL_CONDITIONS`,
  `GithubSkillsAssembler` (and potentially `NESTED_GROUPS` if exported).

---

## New Helper

```typescript
function createAllLibSkills(resourcesDir: string): void {
  for (const name of SKILL_GROUPS["lib"]!) {
    createSkillTemplate(resourcesDir, "lib", name);
  }
}
```

Follows the same pattern as the existing `createAllInfraSkills` helper.

---

## Part 1 — SKILL_GROUPS Constant Update

### Test Group: `SKILL_GROUPS and INFRA_SKILL_CONDITIONS`

---

**Test LIB-01**
- **Name:** `SKILL_GROUPS_has8Groups`
- **Scenario:** The `"lib"` group has been added, increasing the total from 7 to 8.
- **Action:** Update existing test `SKILL_GROUPS_has7Groups`.
- **Expected:**
  - `Object.keys(SKILL_GROUPS)` has length 8.

---

**Test LIB-02**
- **Name:** `SKILL_GROUPS_libGroupContains3Skills`
- **Scenario:** Verify the `"lib"` group has exactly the 3 expected lib skills.
- **Expected:**
  - `SKILL_GROUPS["lib"]` has length 3.
  - Contains `"x-lib-task-decomposer"`, `"x-lib-audit-rules"`,
    `"x-lib-group-verifier"`.

---

**Test LIB-03**
- **Name:** `SKILL_GROUPS_libGroupSkillsHaveXLibPrefix`
- **Scenario:** All lib skill names follow the `x-lib-*` naming convention.
- **Expected:**
  - Every entry in `SKILL_GROUPS["lib"]` starts with `"x-lib-"`.

---

## Part 2 — Lib Group: `assemble` Behavior

### Test Group: `GithubSkillsAssembler -- lib group`

All tests in this group use `beforeEach`/`afterEach` to manage `tmpDir`,
`resourcesDir`, `outputDir`, and `assembler` instance.

---

**Test LIB-04**
- **Name:** `assemble_libGroup_generates3LibSkills`
- **Scenario:** All 3 lib template files exist in `github-skills-templates/lib/`.
- **Setup:** Call `createAllLibSkills(resourcesDir)`.
- **Expected:**
  - Result array contains exactly 3 entries for lib skills.
  - Each entry path contains one of: `x-lib-task-decomposer`,
    `x-lib-audit-rules`, `x-lib-group-verifier`.

---

**Test LIB-05**
- **Name:** `assemble_libGroup_outputNestedUnderLib`
- **Scenario:** Verify output paths contain `github/skills/lib/` subdirectory.
- **Setup:** Call `createAllLibSkills(resourcesDir)`.
- **Expected:**
  - Every lib skill output path contains `github/skills/lib/`.
  - None of the lib skills appear directly under `github/skills/` (without the
    `lib/` subdirectory).

---

**Test LIB-06**
- **Name:** `assemble_libGroup_outputStructure_lib_skillName_SKILL_md`
- **Scenario:** Verify exact output path for each lib skill.
- **Setup:** Call `createAllLibSkills(resourcesDir)`.
- **Expected:**
  - Result includes:
    - `{outputDir}/github/skills/lib/x-lib-task-decomposer/SKILL.md`
    - `{outputDir}/github/skills/lib/x-lib-audit-rules/SKILL.md`
    - `{outputDir}/github/skills/lib/x-lib-group-verifier/SKILL.md`
  - Paths are constructed using `path.join()` for OS compatibility.

---

**Test LIB-07**
- **Name:** `assemble_libGroup_templateDirMissing_skipsGroup`
- **Scenario:** No `lib/` directory exists under `github-skills-templates/`.
- **Setup:** Do not create any lib templates. Optionally create a non-lib skill
  template to ensure the assembler still runs.
- **Expected:**
  - Result array does NOT contain any paths with `lib/` nesting.
  - No error is thrown.

---

**Test LIB-08**
- **Name:** `assemble_libGroup_partialTemplates_generatesAvailable`
- **Scenario:** Only 1 of the 3 lib template files exists.
- **Setup:** Create only `x-lib-task-decomposer` template in `lib/` group.
- **Expected:**
  - Result contains exactly 1 lib skill path.
  - The generated path corresponds to `x-lib-task-decomposer`.
  - Missing skills (`x-lib-audit-rules`, `x-lib-group-verifier`) are silently
    skipped with no error.

---

**Test LIB-09**
- **Name:** `assemble_libGroup_appliesPlaceholderReplacement`
- **Scenario:** A lib template contains `{project_name}` placeholder text.
- **Setup:** Create `x-lib-task-decomposer` template with content
  `"# Lib skill for {project_name}"`.
- **Expected:**
  - The output file at
    `{outputDir}/github/skills/lib/x-lib-task-decomposer/SKILL.md` contains
    `"# Lib skill for my-app"`.
  - The `{project_name}` placeholder is fully replaced.

---

**Test LIB-10**
- **Name:** `assemble_libGroup_createsLibSubdirectory`
- **Scenario:** The `github/skills/lib/` directory does not exist before
  `assemble` runs.
- **Setup:** Call `createAllLibSkills(resourcesDir)`.
- **Expected:**
  - `{outputDir}/github/skills/lib/` directory exists after `assemble`.
  - `fs.statSync(libDir).isDirectory()` returns `true`.

---

## Part 3 — Non-Lib Groups Remain Flat (Nesting Isolation)

### Test Group: `GithubSkillsAssembler -- nesting isolation`

---

**Test LIB-11**
- **Name:** `assemble_nonLibGroups_outputRemainsFlat`
- **Scenario:** Non-lib groups (e.g., `"dev"`) continue to output directly under
  `github/skills/{skillName}/` without any group subdirectory.
- **Setup:** Create `x-dev-implement` template in `dev/` group.
- **Expected:**
  - Output path is `{outputDir}/github/skills/x-dev-implement/SKILL.md`.
  - Output path does NOT contain `github/skills/dev/`.

---

**Test LIB-12**
- **Name:** `assemble_mixedGroups_libNestedOthersFlat`
- **Scenario:** Both lib and non-lib templates exist. Verify lib skills are
  nested while others remain flat.
- **Setup:**
  - Create all 3 lib templates via `createAllLibSkills(resourcesDir)`.
  - Create `x-dev-implement` template in `dev/` group.
- **Expected:**
  - Lib skills have paths containing `github/skills/lib/`.
  - Dev skill has path `github/skills/x-dev-implement/SKILL.md` (flat).
  - Total result length is 4 (3 lib + 1 dev).

---

## Part 4 — Lib Group Filtering (Unconditional)

### Test Group: `GithubSkillsAssembler -- lib group filtering`

---

**Test LIB-13**
- **Name:** `assemble_libGroupNotFiltered_allSkillsGenerated`
- **Scenario:** Config has all infrastructure set to `"none"`. Lib skills should
  still be generated because they are unconditional (not in `INFRA_SKILL_CONDITIONS`).
- **Setup:**
  - Config: `{ orchestrator: "none", container: "none", iac: "none", templating: "none" }`.
  - Call `createAllLibSkills(resourcesDir)`.
- **Expected:**
  - All 3 lib skills are in the result array.
  - Lib group bypasses `filterSkills` infrastructure conditions.

---

**Test LIB-14**
- **Name:** `filterSkills_libGroup_returnsAllSkills`
- **Scenario:** The `filterSkills` method treats the lib group the same as
  non-infrastructure groups -- returns all skills unfiltered.
- **Setup:**
  - Call `createAllLibSkills(resourcesDir)`.
  - Use config with all infra = `"none"`.
- **Expected:**
  - All 3 lib skills appear in the assembled output.
  - No lib skill is excluded by any config condition.

---

## Part 5 — `renderSkill` with `subDir` Parameter

### Test Group: `GithubSkillsAssembler -- renderSkill subDir`

These tests verify the `subDir` parameter behavior through the public `assemble`
method (since `renderSkill` is private). The branch coverage is achieved by
testing both `subDir = undefined` (non-lib groups) and `subDir = "lib"` (lib
group).

---

**Test LIB-15**
- **Name:** `renderSkill_withSubDir_writesToNestedPath`
- **Scenario:** When `subDir` is `"lib"`, the output path includes `lib/`
  between `skills/` and `{skillName}/`.
- **Setup:** Create 1 lib skill template.
- **Expected:**
  - Output file exists at `{outputDir}/github/skills/lib/{skillName}/SKILL.md`.
  - The file content matches the rendered template.

---

**Test LIB-16**
- **Name:** `renderSkill_withoutSubDir_writesToFlatPath`
- **Scenario:** When `subDir` is `undefined` (non-lib group), the output path
  does NOT include any group-level subdirectory.
- **Setup:** Create 1 dev skill template.
- **Expected:**
  - Output file exists at `{outputDir}/github/skills/{skillName}/SKILL.md`.
  - No intermediate group directory in path.

---

## Part 6 — `generateGroup` with Nested Output Path

### Test Group: `GithubSkillsAssembler -- generateGroup nesting`

---

**Test LIB-17**
- **Name:** `generateGroup_libGroup_srcDirMissing_returnsEmpty`
- **Scenario:** The `lib/` directory does not exist under
  `github-skills-templates/`. The `generateGroup` method should return `[]`.
- **Setup:** Do not create any `lib/` directory.
- **Expected:**
  - No lib skills in result.
  - No error thrown.
  - This tests the `if (!fs.existsSync(srcDir)) return []` branch.

---

**Test LIB-18**
- **Name:** `generateGroup_libGroup_allTemplatesPresent_returns3Paths`
- **Scenario:** All 3 lib templates present. The `generateGroup` processes them
  with the nested `subDir`.
- **Setup:** Call `createAllLibSkills(resourcesDir)`.
- **Expected:**
  - 3 paths returned, all under `github/skills/lib/`.

---

## Part 7 — Parity Tests

### Test Group: `GithubSkillsAssembler -- lib group parity`

---

**Test LIB-19**
- **Name:** `parity_libGroup_3SkillsGenerated_matchesExpected`
- **Scenario:** Run `assemble` with all lib templates and verify the output
  structure matches the expected `.github/skills/lib/` layout from the story
  acceptance criteria.
- **Setup:**
  - Create all 3 lib templates with known content (e.g., YAML frontmatter +
    body with `{project_name}` placeholder).
  - Run `assembler.assemble(config, outputDir, resourcesDir, engine)`.
- **Expected:**
  - Exactly 3 files generated under `github/skills/lib/`.
  - Each file is at `github/skills/lib/{skillName}/SKILL.md`.
  - Content has `{project_name}` replaced with `"my-app"`.
  - Structure matches GitHub Copilot expected layout.

---

**Test LIB-20**
- **Name:** `parity_libGroup_coexistsWithOtherGroups`
- **Scenario:** Run `assemble` with templates from multiple groups (dev, story,
  lib, infrastructure) to verify lib group output coexists correctly.
- **Setup:**
  - Create templates for `dev` (1 skill), `story` (1 skill),
    `lib` (3 skills), `infrastructure` (selected by config).
  - Config: `{ container: "docker" }` (enables `dockerfile` skill).
- **Expected:**
  - Dev skill at `github/skills/x-dev-implement/SKILL.md` (flat).
  - Story skill at `github/skills/x-story-create/SKILL.md` (flat).
  - 3 lib skills at `github/skills/lib/x-lib-*/SKILL.md` (nested).
  - Infrastructure skill at `github/skills/dockerfile/SKILL.md` (flat).
  - Total: 6 files.

---

## Part 8 — NESTED_GROUPS Constant (if exported)

### Test Group: `NESTED_GROUPS`

If `NESTED_GROUPS` is exported as a public constant:

---

**Test LIB-21**
- **Name:** `NESTED_GROUPS_containsOnlyLib`
- **Scenario:** The `NESTED_GROUPS` set contains exactly one entry: `"lib"`.
- **Expected:**
  - `NESTED_GROUPS.size === 1`.
  - `NESTED_GROUPS.has("lib")` is `true`.

---

**Test LIB-22**
- **Name:** `NESTED_GROUPS_doesNotContainOtherGroups`
- **Scenario:** No other group names are in `NESTED_GROUPS`.
- **Expected:**
  - `NESTED_GROUPS.has("dev")` is `false`.
  - `NESTED_GROUPS.has("infrastructure")` is `false`.
  - `NESTED_GROUPS.has("story")` is `false`.

---

## Coverage Strategy

### Branch Coverage Targets

| Branch | True Path | False Path | Test IDs |
|--------|-----------|------------|----------|
| `NESTED_GROUPS.has(group)` | Lib group passes `subDir = "lib"` | Non-lib group passes `subDir = undefined` | LIB-05, LIB-11 |
| `subDir` present in `renderSkill` | Output under `github/skills/lib/{name}` | Output under `github/skills/{name}` | LIB-15, LIB-16 |
| `subDir` present in `generateGroup` | Passes `subDir` to `renderSkill` | Passes `undefined` to `renderSkill` | LIB-18, LIB-17 (via assemble) |
| `fs.existsSync(srcDir)` for lib group | Lib dir exists, processes templates | Lib dir missing, returns `[]` | LIB-04, LIB-07 |
| `fs.existsSync(src)` for lib template | Template exists, renders skill | Template missing, returns `null` | LIB-04, LIB-08 |
| `group !== INFRA_GROUP` for lib group | Lib group returns all skills | N/A (covered by existing infra tests) | LIB-13, LIB-14 |

### Line Coverage Targets

Every new or modified line in `github-skills-assembler.ts` is exercised:

| Code Change | Test IDs |
|-------------|----------|
| `"lib"` entry in `SKILL_GROUPS` | LIB-01, LIB-02, LIB-03 |
| `NESTED_GROUPS` constant definition | LIB-21, LIB-22 (or indirectly via LIB-05, LIB-11) |
| `const subDir = NESTED_GROUPS.has(group) ? group : undefined` | LIB-05, LIB-11 |
| `renderSkill` `subDir` parameter and conditional path | LIB-15, LIB-16 |
| `generateGroup` `subDir` parameter propagation | LIB-17, LIB-18 |

---

## Summary Table

| ID | Behavior Under Test | Key Assertion |
|----|---------------------|---------------|
| LIB-01 | Group count increased to 8 | `Object.keys(SKILL_GROUPS).length === 8` |
| LIB-02 | Lib group has 3 skills | `SKILL_GROUPS["lib"].length === 3` |
| LIB-03 | Lib skills follow `x-lib-*` naming | All start with `"x-lib-"` |
| LIB-04 | All 3 lib skills generated | 3 lib entries in result |
| LIB-05 | Output nested under `lib/` | Paths contain `github/skills/lib/` |
| LIB-06 | Exact output path structure | `github/skills/lib/{name}/SKILL.md` |
| LIB-07 | Template dir missing: skip | No lib paths in result |
| LIB-08 | Partial templates: generate available | 1 of 3 generated |
| LIB-09 | Placeholder replacement applied | `{project_name}` replaced |
| LIB-10 | `lib/` subdirectory auto-created | Dir exists |
| LIB-11 | Non-lib groups remain flat | No group subdirectory |
| LIB-12 | Mixed groups: lib nested, others flat | 4 total (3 nested + 1 flat) |
| LIB-13 | Lib not filtered by infra conditions | All 3 generated with all-none config |
| LIB-14 | filterSkills treats lib as non-infra | All skills returned |
| LIB-15 | renderSkill with subDir: nested path | File at `skills/lib/{name}/SKILL.md` |
| LIB-16 | renderSkill without subDir: flat path | File at `skills/{name}/SKILL.md` |
| LIB-17 | generateGroup: lib srcDir missing | Returns empty |
| LIB-18 | generateGroup: lib all present | 3 nested paths |
| LIB-19 | Parity: 3 lib skills match expected | Content + structure match |
| LIB-20 | Parity: lib coexists with other groups | 6 total, correct nesting |
| LIB-21 | NESTED_GROUPS contains only "lib" | `size === 1` |
| LIB-22 | NESTED_GROUPS excludes other groups | `has("dev") === false` |

---

## Existing Tests to Update

| Current Test | Change Required | Reason |
|-------------|----------------|--------|
| `SKILL_GROUPS_has7Groups` | Update assertion from `7` to `8` | New `"lib"` group added |

All other existing tests remain unchanged. The lib group addition does not affect
infrastructure filtering, non-lib group rendering, or placeholder replacement
for existing groups.

---

## Notes

1. **`renderSkill` and `generateGroup` are private:** Tests exercise these
   methods indirectly through the public `assemble` method. Branch coverage is
   achieved by varying inputs (lib templates present/absent, lib + non-lib
   templates) to exercise both `subDir` defined/undefined paths.

2. **`NESTED_GROUPS` export decision:** Tests LIB-21 and LIB-22 depend on
   whether `NESTED_GROUPS` is exported. If it is kept private, these tests are
   replaced by indirect verification through LIB-05 (nested) and LIB-11 (flat).
   The implementation plan recommends exporting it for testability.

3. **Lib group is unconditional:** Unlike the `"infrastructure"` group which
   uses `INFRA_SKILL_CONDITIONS` for config-based filtering, the `"lib"` group
   always generates all 3 skills regardless of config. Tests LIB-13 and LIB-14
   explicitly verify this behavior.

4. **`createAllLibSkills` helper pattern:** Follows the existing
   `createAllInfraSkills` pattern. Iterates `SKILL_GROUPS["lib"]!` and calls
   `createSkillTemplate` for each skill name.

5. **Path construction:** All path assertions use `path.join()` to ensure
   cross-platform compatibility (forward vs backward slashes).

6. **Total new test count:** 22 tests (LIB-01 through LIB-22). Combined with
   the existing 13 tests (1 updated), the file will have approximately 35 tests
   covering all branches of the expanded `GithubSkillsAssembler`.

7. **Coverage projection:** The 6 new branches (3 boolean checks x 2 paths each)
   are all exercised by the test plan. Combined with existing branch coverage,
   the projected branch coverage is >= 90%. Line coverage is projected at >= 95%
   since every new line has at least one test exercising it.
