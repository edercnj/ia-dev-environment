# Test Plan — STORY-015: ReadmeAssembler

## Summary

- Total test classes: 1 (`readme-assembler.test.ts`)
- Total test methods: ~70 (estimated)
- Categories covered: Unit, Contract (parametrized)
- Estimated line coverage: ~97%
- Estimated branch coverage: ~95%

## Test File: `tests/node/assembler/readme-assembler.test.ts`

### Fixture Setup

```
beforeEach:
  - tmpDir = fs.mkdtempSync(...)
  - outputDir = path.join(tmpDir, ".claude")
  - resourcesDir = path.join(tmpDir, "resources")
  - Create subdirs: rules/, skills/, agents/, hooks/, github/
  - buildConfig(overrides) factory

afterEach:
  - fs.rmSync(tmpDir, { recursive: true, force: true })
```

### Helpers Needed

- `buildConfig(overrides)` — returns `ProjectConfig` with sensible defaults
- `createRule(outputDir, filename, content?)` — creates `.md` file in rules/
- `createSkill(outputDir, name, description, isKP?)` — creates `skills/{name}/SKILL.md`
- `createAgent(outputDir, name)` — creates `agents/{name}.md`
- `createHook(outputDir, name)` — creates file in hooks/
- `createGithubFiles(outputDir, component, count)` — creates files in github/{component}/

---

## Group 1: Counting Functions

### countRules

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 1 | returns count of md files in rules dir | 3 rules → 3 | Happy |
| 2 | returns zero when rules dir missing | No rules/ → 0 | Error |
| 3 | returns zero when rules dir empty | Empty rules/ → 0 | Boundary |

### countSkills

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 4 | returns count of SKILL.md files | 5 skills → 5 | Happy |
| 5 | returns zero when skills dir missing | No skills/ → 0 | Error |
| 6 | counts both regular skills and KPs | 3 regular + 2 KP → 5 | Boundary |

### countAgents

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 7 | returns count of md files in agents dir | 4 agents → 4 | Happy |
| 8 | returns zero when agents dir missing | No agents/ → 0 | Error |

### countKnowledgePacks

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 9 | returns count of KP skills only | 2 KP + 3 regular → 2 | Happy |
| 10 | returns zero when skills dir missing | No skills/ → 0 | Error |
| 11 | returns zero when no KPs | 3 regular → 0 | Boundary |

### countHooks

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 12 | returns count of files in hooks dir | 2 files → 2 | Happy |
| 13 | returns zero when hooks dir missing | No hooks/ → 0 | Error |

### countSettings

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 14 | returns 2 when both settings files exist | Both → 2 | Happy |
| 15 | returns 1 when only settings.json exists | One → 1 | Boundary |
| 16 | returns 0 when neither exists | None → 0 | Boundary |

### countGithubFiles

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 17 | returns recursive count of all files | Nested structure → total | Happy |
| 18 | returns zero when github dir missing | No github/ → 0 | Error |

### countGithubComponent

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 19 | returns count of files in subdirectory | 3 instructions → 3 | Happy |
| 20 | returns zero when component dir missing | No dir → 0 | Error |
| 21 | does not count subdirectories as files | dirs only → 0 | Boundary |

### countGithubSkills

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 22 | returns count of SKILL.md in subdirs | 4 skills → 4 | Happy |
| 23 | returns zero when skills dir missing | No dir → 0 | Error |

---

## Group 2: Detection/Extraction Functions

### isKnowledgePack

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 24 | returns true for user-invocable false | `user-invocable: false` → true | Happy |
| 25 | returns true for Knowledge Pack heading | `# Knowledge Pack` → true | Happy |
| 26 | returns false for regular skill | Regular SKILL.md → false | Happy |
| 27 | returns false for user-invocable true | `user-invocable: true` → false | Boundary |

**Parametrized (it.each):**

```typescript
it.each([
  ["user-invocable: false", true],
  ["# Knowledge Pack\n...", true],
  ["user-invocable: true\n...", false],
  ["name: my-skill\ndescription: foo", false],
])("isKnowledgePack(%s) → %s", ...)
```

### extractRuleNumber

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 28 | extracts number from numbered filename | `"01-name.md"` → `"01"` | Happy |
| 29 | extracts multi-digit number | `"123-name.md"` → `"123"` | Happy |
| 30 | returns empty for non-numbered filename | `"name.md"` → `""` | Error |

### extractRuleScope

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 31 | extracts scope from numbered filename | `"01-project-identity.md"` → `"project identity"` | Happy |
| 32 | extracts scope from non-numbered file | `"my-rule.md"` → `"my rule"` | Boundary |

### extractSkillDescription

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 33 | extracts description from SKILL.md | `description: "foo"` → `"foo"` | Happy |
| 34 | strips quotes from description | `description: 'bar'` → `"bar"` | Boundary |
| 35 | returns empty when no description | No description line → `""` | Error |

---

## Group 3: Table Builder Functions

### buildRulesTable

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 36 | builds markdown table with rules | 3 rules → table with 3 rows | Happy |
| 37 | returns fallback when rules dir missing | No dir → "No rules configured." | Error |
| 38 | returns fallback when rules dir empty | Empty dir → "No rules configured." | Boundary |
| 39 | sorts rules alphabetically by filename | Unsorted → sorted output | Happy |

### buildSkillsTable

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 40 | builds table excluding knowledge packs | 3 skills + 2 KP → 3 rows | Happy |
| 41 | returns fallback when skills dir missing | No dir → "No skills configured." | Error |
| 42 | returns fallback when only KPs exist | All KPs → "No skills configured." | Boundary |
| 43 | includes description from SKILL.md | Description extracted → in table | Happy |

### buildAgentsTable

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 44 | builds table with agent names | 3 agents → 3 rows | Happy |
| 45 | returns fallback when agents dir missing | No dir → "No agents configured." | Error |
| 46 | returns fallback when agents dir empty | Empty → "No agents configured." | Boundary |

### buildKnowledgePacksTable

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 47 | builds table with KP names only | 2 KPs + 3 regular → 2 rows | Happy |
| 48 | returns fallback when skills dir missing | No dir → "No knowledge packs configured." | Error |
| 49 | returns fallback when no KPs found | Only regular skills → fallback | Boundary |

### buildMappingTable

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 50 | builds static mapping table | Always → 8 mapping rows | Happy |
| 51 | includes github total when dir exists | github/ with files → total shown | Happy |
| 52 | omits total when github dir missing | No github/ → no total line | Boundary |

### buildGenerationSummary

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 53 | builds summary with all component counts | Full structure → 12 rows | Happy |
| 54 | includes version from DEFAULT_FOUNDATION | → `"ia-dev-env v0.1.0"` | Happy |
| 55 | skills count excludes KPs | 5 total - 2 KP = 3 skills | Happy |
| 56 | github instructions includes global | copilot-instructions.md exists → +1 | Boundary |
| 57 | github MCP counted when file exists | copilot-mcp.json → 1 | Boundary |

---

## Group 4: Section Builder Functions

### buildHooksSection

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 58 | builds hook section for typescript-npm | → contains `.ts` and `tsc` | Happy |
| 59 | returns fallback when no hook template | Unknown lang/tool → "No hooks configured." | Error |
| 60 | includes correct file extension and compile command | Per LANGUAGE_COMMANDS | Happy |

### buildSettingsSection

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 61 | returns static settings section | Always → contains both headings | Happy |

### buildStructureBlock

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 62 | returns directory structure with tree chars | → contains `.claude/` tree | Happy |

### buildTipsBlock

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 63 | includes architecture style and interfaces | → contains arch + ifaces | Happy |

---

## Group 5: Generation Functions

### generateReadme (full mode)

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 64 | replaces all 12 placeholders in template | Template → no `{{` remaining | Happy |
| 65 | includes project name from config | → contains project name | Happy |
| 66 | includes correct counts in output | → "5 rules", etc. | Happy |

### generateMinimalReadme

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 67 | generates header with project name | → `# .claude/` + name | Happy |
| 68 | includes structure block | → contains tree | Happy |
| 69 | includes tips block with arch and interfaces | → arch style + ifaces | Happy |

---

## Group 6: ReadmeAssembler.assemble (Class Method)

| # | Test Name | Description | Type |
|---|-----------|-------------|------|
| 70 | uses template when it exists | Template present → full README | Happy |
| 71 | uses minimal fallback when template missing | No template → minimal README | Fallback |
| 72 | writes README.md to output dir | → file exists | Happy |
| 73 | returns file path in result | → paths include README.md | Happy |

---

## Coverage Estimation

| Function Group | Functions | Branches | Est. Tests | Line % | Branch % |
|---------------|-----------|----------|-----------|--------|----------|
| Counting (9) | 9 | 14 | 23 | 100% | 100% |
| Detection/Extraction (4) | 4 | 8 | 8 | 100% | 100% |
| Table Builders (6) | 6 | 16 | 22 | 97% | 95% |
| Section Builders (4) | 4 | 2 | 6 | 100% | 100% |
| Generation (2) | 2 | 0 | 6 | 100% | N/A |
| Class Method (1) | 1 | 2 | 4 | 100% | 100% |
| **Total** | **26** | **42** | **~69** | **~97%** | **~95%** |

## Risks and Gaps

1. **Template placeholder format:** `{{DOUBLE_BRACES}}` must NOT go through `TemplateEngine.replacePlaceholders()` (which handles `{single}`). Test must verify no `{{` remain after generation.
2. **File sorting:** Python `sorted(glob(...))` may produce different order than Node's `readdirSync`. Tests must verify alphabetical sorting explicitly.
3. **Unicode tree characters:** The structure block uses `├──`, `└──`, `│`. Verify encoding preservation.
4. **DEFAULT_FOUNDATION.version:** Tests should import and verify the actual version constant, not hardcode.
