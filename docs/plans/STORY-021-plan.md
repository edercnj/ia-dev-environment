# Implementation Plan — STORY-021: Lib Skills in GitHub Skills Assembler

## Story Summary

Add support for lib skills (`x-lib-task-decomposer`, `x-lib-audit-rules`, `x-lib-group-verifier`) in the `GithubSkillsAssembler`. Currently, the assembler defines 7 groups in `SKILL_GROUPS` but does not include "lib". The lib skills exist as `.claude/skills/` templates and are generated correctly by `SkillsAssembler`, but the `GithubSkillsAssembler` ignores them entirely — causing "Skill not found" when invoked in GitHub Copilot.

**Blocked by:** STORY-014 (GitHub Assemblers) — complete.
**Blocks:** STORY-016 (pipeline orchestration).

---

## 1. Affected Layers and Components

| Layer | Component | Action | Path |
|-------|-----------|--------|------|
| resources | lib GitHub skill templates | **Create** (3 files) | `resources/github-skills-templates/lib/x-lib-task-decomposer.md` |
| resources | lib GitHub skill templates | **Create** | `resources/github-skills-templates/lib/x-lib-audit-rules.md` |
| resources | lib GitHub skill templates | **Create** | `resources/github-skills-templates/lib/x-lib-group-verifier.md` |
| assembler | GithubSkillsAssembler | **Modify** | `src/assembler/github-skills-assembler.ts` |
| tests | GithubSkillsAssembler tests | **Modify** | `tests/node/assembler/github-skills-assembler.test.ts` |

---

## 2. New Classes/Interfaces to Create

### 2.1 Resource Templates (3 new files)

No new TypeScript classes or interfaces are needed. The deliverables are 3 new markdown template files in `resources/github-skills-templates/lib/`.

Each template follows the established GitHub skill template format observed across all existing groups (YAML frontmatter with `name` and `description`, then markdown body). The content is adapted from the corresponding `.claude/skills/` templates (`resources/skills-templates/core/lib/*/SKILL.md`) to the GitHub Copilot context.

#### `resources/github-skills-templates/lib/x-lib-task-decomposer.md`

- **Source reference:** `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md`
- **Content:** YAML frontmatter (`name: x-lib-task-decomposer`, `description: ...`), followed by the skill body adapted for GitHub Copilot. Uses `{project_name}` and other single-brace placeholders where needed (processed by `TemplateEngine.replacePlaceholders()`).

#### `resources/github-skills-templates/lib/x-lib-audit-rules.md`

- **Source reference:** `resources/skills-templates/core/lib/x-lib-audit-rules/SKILL.md`
- **Content:** Same structure. YAML frontmatter + markdown body.

#### `resources/github-skills-templates/lib/x-lib-group-verifier.md`

- **Source reference:** `resources/skills-templates/core/lib/x-lib-group-verifier/SKILL.md`
- **Content:** Same structure. YAML frontmatter + markdown body.

---

## 3. Existing Classes to Modify

### 3.1 `src/assembler/github-skills-assembler.ts` — Add "lib" group + nesting support

#### 3.1.1 Add "lib" to `SKILL_GROUPS`

Add a new entry to the `SKILL_GROUPS` constant:

```typescript
"lib": ["x-lib-task-decomposer", "x-lib-audit-rules", "x-lib-group-verifier"],
```

This entry is placed after `"git-troubleshooting"`. The group count increases from 7 to 8.

#### 3.1.2 Handle `lib/` subdirectory nesting in output path

**Critical issue:** The current `renderSkill` method generates output at:
```
outputDir/github/skills/{skillName}/SKILL.md
```

For lib skills, the story requires output at:
```
outputDir/github/skills/lib/{skillName}/SKILL.md
```

The `renderSkill` method must be modified to support an optional subdirectory prefix. Two approaches:

**Option A — Group-aware renderSkill (recommended):**

Pass the group name to `renderSkill`. For the `"lib"` group, prepend `lib/` to the output path:

```typescript
private renderSkill(
  engine: TemplateEngine,
  srcDir: string,
  outputDir: string,
  name: string,
  subDir?: string,
): string | null {
  const src = path.join(srcDir, `${name}.md`);
  if (!fs.existsSync(src)) return null;
  const rendered = engine.replacePlaceholders(
    fs.readFileSync(src, "utf-8"),
  );
  const skillDir = subDir
    ? path.join(outputDir, "github", "skills", subDir, name)
    : path.join(outputDir, "github", "skills", name);
  fs.mkdirSync(skillDir, { recursive: true });
  const dest = path.join(skillDir, SKILL_MD);
  fs.writeFileSync(dest, rendered, "utf-8");
  return dest;
}
```

The `generateGroup` and `assemble` methods propagate the group name so `renderSkill` can determine the `subDir`. Define a constant or a set for groups that require nesting:

```typescript
const NESTED_GROUPS = new Set(["lib"]);
```

In `generateGroup`, pass `subDir` when the group is in `NESTED_GROUPS`:

```typescript
private generateGroup(
  engine: TemplateEngine,
  srcDir: string,
  outputDir: string,
  skillNames: readonly string[],
  subDir?: string,
): string[] {
  if (!fs.existsSync(srcDir)) return [];
  const results: string[] = [];
  for (const name of skillNames) {
    const dest = this.renderSkill(engine, srcDir, outputDir, name, subDir);
    if (dest !== null) results.push(dest);
  }
  return results;
}
```

In `assemble`, pass `group` as `subDir` when appropriate:

```typescript
const subDir = NESTED_GROUPS.has(group) ? group : undefined;
results.push(
  ...this.generateGroup(engine, srcDir, outputDir, filtered, subDir),
);
```

**Option B — All groups nested under group name:** Would change the output structure for all groups (e.g., `github/skills/dev/x-dev-implement/`), which would be a breaking change. Rejected.

### 3.2 `tests/node/assembler/github-skills-assembler.test.ts` — Add lib group tests

Add the following test scenarios:

1. **`SKILL_GROUPS_has8Groups`** — Update existing test from 7 to 8.
2. **`assemble_libGroup_generates3LibSkills`** — Create all 3 lib templates, verify 3 output files.
3. **`assemble_libGroup_outputNestedUnderLib`** — Verify output path contains `github/skills/lib/x-lib-*`.
4. **`assemble_libGroup_templateDirMissing_skipsGroup`** — No lib template dir, no output.
5. **`assemble_libGroup_partialTemplates_generatesAvailable`** — Only 1 of 3 templates exists, generates 1.
6. **`assemble_libGroup_appliesPlaceholderReplacement`** — Template with `{project_name}` is rendered.
7. **`assemble_libGroup_outputStructure_lib_skillName_SKILL_md`** — Verify exact path: `outputDir/github/skills/lib/x-lib-task-decomposer/SKILL.md`.

---

## 4. Dependency Direction Validation

```
github-skills-assembler.ts ──imports──> models.ts (ProjectConfig)
                            ──imports──> template-engine.ts (TemplateEngine)
```

**Validated:** No new dependencies introduced. The `GithubSkillsAssembler` already depends on `models.ts` and `template-engine.ts`. The change adds a new group entry (data) and a minor output path calculation (logic). Dependencies point inward; no circular dependencies.

The new resource templates (`resources/github-skills-templates/lib/*.md`) are pure data files with no code dependencies.

---

## 5. Integration Points

### 5.1 Resource Templates (read-only, filesystem)

The `GithubSkillsAssembler.assemble()` reads templates from:
```
resourcesDir/github-skills-templates/{group}/{skillName}.md
```

For the lib group, the source path becomes:
```
resourcesDir/github-skills-templates/lib/x-lib-task-decomposer.md
```

This requires creating the `lib/` subdirectory under `resources/github-skills-templates/`.

### 5.2 TemplateEngine (read-only usage)

Templates use `{project_name}` and `{{PLACEHOLDER}}` style placeholders. Only `{single_brace}` placeholders are processed by `TemplateEngine.replacePlaceholders()`. The GitHub skill templates for lib skills may contain `{project_name}` which is already handled.

### 5.3 Pipeline Integration (future STORY-016)

The `GithubSkillsAssembler` is already registered in the pipeline. Adding the lib group to `SKILL_GROUPS` is transparent — the pipeline calls `assemble()` and receives additional output files. No pipeline changes needed.

### 5.4 ReadmeAssembler (STORY-015)

The `countGithubSkills` function in `ReadmeAssembler` counts `SKILL.md` files recursively under `github/skills/`. The nested `lib/` subdirectory will be automatically picked up by recursive counting, requiring no changes.

---

## 6. Database Changes

None. This story is pure file generation logic.

---

## 7. API Changes

None. These are internal assembler modules with no external API surface.

---

## 8. Event Changes

None. No event-driven components involved.

---

## 9. Configuration Changes

None. The assembler reads from existing `ProjectConfig` and the template directory. No new configuration fields are introduced. The lib group is unconditional (no feature gating by config conditions — lib skills are always generated, similar to "story", "dev", "review", etc.).

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Output path nesting breaks existing skill paths | Low | High | The `subDir` parameter is opt-in via `NESTED_GROUPS`. Only the "lib" group uses it. All other groups continue to use the flat `github/skills/{name}/` structure. Test both lib and non-lib groups in the same `assemble()` call. |
| Lib group not filtered (always generated) | Low | Low | The story does not specify any config-based conditions for lib skills — they are unconditional, analogous to "story" or "dev" groups. The `filterSkills` method already returns all skills for non-infrastructure groups, so no change needed. If future gating is required, add to `INFRA_SKILL_CONDITIONS` or create a separate `LIB_SKILL_CONDITIONS` map. |
| GitHub Copilot does not discover skills in nested `lib/` subdirectory | Medium | High | GitHub Copilot's skill discovery expects `{name}/SKILL.md` under `.github/skills/`. Verify that `lib/x-lib-task-decomposer/SKILL.md` is discoverable. If Copilot requires flat structure, an alternative is to use flat output (`github/skills/x-lib-task-decomposer/`) without the `lib/` nesting. The story explicitly requires `lib/` nesting (acceptance criteria: "files are in `.github/skills/lib/` not `.github/skills/`"). |
| Template content diverges from `.claude/skills/` source | Low | Medium | The GitHub templates are adapted (not copied verbatim) from the Claude skill templates. Content differences are expected (GitHub Copilot context vs Claude Code context). Ensure the core instructions and procedures are preserved. |
| Existing test `SKILL_GROUPS_has7Groups` fails immediately | Certain | Low | This is expected and intentional. Update the test assertion from 7 to 8. |
| `createSkillTemplate` helper does not support nested groups | Low | Medium | The existing test helper creates templates at `resourcesDir/github-skills-templates/{group}/{skillName}.md`. For the lib group, the group is "lib" and skill names are the same flat names. The helper works without modification since `mkdirSync({ recursive: true })` handles the nested path. |
| `NESTED_GROUPS` set grows with future groups | Low | Low | The set is a clear, explicit mechanism. Future groups requiring nesting (unlikely) simply add to the set. |

---

## 11. Implementation Groups (Execution Order)

### G1: Create lib GitHub skill templates (3 files)

**Files to create:**
- `resources/github-skills-templates/lib/x-lib-task-decomposer.md`
- `resources/github-skills-templates/lib/x-lib-audit-rules.md`
- `resources/github-skills-templates/lib/x-lib-group-verifier.md`

**Approach:**
1. Read the corresponding `.claude/skills/` source templates.
2. Adapt content to GitHub Copilot format (YAML frontmatter with `name` + `description`, same markdown body structure as other GitHub skill templates like `x-dev-implement.md`, `x-review.md`).
3. Keep single-brace `{project_name}` placeholders for TemplateEngine processing.
4. Remove Claude-specific references (e.g., `.claude/skills/`) and adapt paths to `.github/skills/` equivalents where referenced.

### G2: Modify GithubSkillsAssembler — add lib group + nesting

**Changes to `src/assembler/github-skills-assembler.ts`:**
1. Add `const NESTED_GROUPS = new Set(["lib"]);` after existing constants.
2. Add `"lib"` entry to `SKILL_GROUPS`.
3. Add optional `subDir` parameter to `renderSkill` and `generateGroup`.
4. In `assemble`, pass `group` as `subDir` when `NESTED_GROUPS.has(group)`.

**Compile check:** `npx tsc --noEmit`

### G3: Update and extend tests

**Changes to `tests/node/assembler/github-skills-assembler.test.ts`:**
1. Update `SKILL_GROUPS_has7Groups` to `SKILL_GROUPS_has8Groups` (assertion from 7 to 8).
2. Add new describe block: `"GithubSkillsAssembler — lib group"`.
3. Add helper `createAllLibSkills(resourcesDir)` following `createAllInfraSkills` pattern.
4. Add 7 test scenarios from section 3.2 above.

**Run tests:** `npx vitest run tests/node/assembler/github-skills-assembler.test.ts`
**Coverage:** `npx vitest run --coverage tests/node/assembler/github-skills-assembler.test.ts`

### G4: Verify coverage thresholds + compile clean

- Line coverage >= 95%
- Branch coverage >= 90%
- Zero compiler warnings

---

## 12. Testing Strategy

### Test infrastructure

Tests follow the established pattern in the existing test file:
- `beforeEach`: create `tmpDir` with `fs.mkdtempSync`, set up `resourcesDir` and `outputDir`.
- `afterEach`: clean up with `fs.rmSync(tmpDir, { recursive: true, force: true })`.
- `createSkillTemplate` helper already exists and works for nested groups.

### New helper

```typescript
function createAllLibSkills(resourcesDir: string): void {
  for (const name of SKILL_GROUPS["lib"]!) {
    createSkillTemplate(resourcesDir, "lib", name);
  }
}
```

### Test scenarios (detailed)

| # | Test Name | Scenario | Expected |
|---|-----------|----------|----------|
| 1 | `SKILL_GROUPS_has8Groups` | Count groups | 8 |
| 2 | `assemble_libGroup_generates3LibSkills` | All 3 lib templates present | 3 files in result |
| 3 | `assemble_libGroup_outputNestedUnderLib` | Verify path nesting | Paths contain `github/skills/lib/` |
| 4 | `assemble_libGroup_templateDirMissing_skipsGroup` | No lib dir in resources | 0 lib files in result |
| 5 | `assemble_libGroup_partialTemplates_generatesAvailable` | Only 1 template | 1 file in result |
| 6 | `assemble_libGroup_appliesPlaceholderReplacement` | Template has `{project_name}` | Output has `my-app` |
| 7 | `assemble_libGroup_outputStructure_lib_skillName_SKILL_md` | Exact path check | `outputDir/github/skills/lib/x-lib-task-decomposer/SKILL.md` |
| 8 | `assemble_mixedGroups_libNestedOthersFlat` | Both lib + dev templates | lib under `lib/`, dev flat |
| 9 | `assemble_libGroupNotFiltered_allSkillsGenerated` | Config with all "none" infra | Lib skills still generated (unconditional) |

### Coverage targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | Every new code path exercised (nested vs flat, lib present/missing) |
| Branch | >= 90% | `subDir` present/undefined branch, `NESTED_GROUPS.has()` true/false, lib template exists/missing |

---

## 13. Acceptance Criteria Checklist

From story Gherkin scenarios:

- [ ] 3 lib skills generated in `.github/skills/lib/` directory
- [ ] Templates exist in `resources/github-skills-templates/lib/`
- [ ] Group "lib" present in `SKILL_GROUPS` with 3 skills
- [ ] Nesting preserved: files in `.github/skills/lib/` not `.github/skills/`
- [ ] Lib skills compatible with GitHub Copilot format (YAML frontmatter)

From DoD:

- [ ] Coverage >= 95% line, >= 90% branch
- [ ] All tests passing
- [ ] Zero compiler/linter warnings
- [ ] JSDoc on modified public members
