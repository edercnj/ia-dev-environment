# STORY-010: SkillsAssembler Migration -- Implementation Plan

## 1. Affected Layers and Components

| Layer | Directory | Impact |
|-------|-----------|--------|
| assembler | `src/assembler/` | New `skills-assembler.ts` (main class) |
| assembler (barrel) | `src/assembler/index.ts` | Add re-export for `skills-assembler.ts` |
| tests | `tests/node/assembler/` | New `skills-assembler.test.ts` |

This is a **library-layer** module with no adapter/domain boundary concerns.
The assembler depends on domain helpers and copy-helpers -- dependency direction
flows from assembler toward domain, matching the established pattern.

## 2. New Classes/Interfaces to Create

| File | Location | Est. Lines | Purpose |
|------|----------|------------|---------|
| `skills-assembler.ts` | `src/assembler/` | ~180 | `SkillsAssembler` class -- 1:1 migration of `skills.py` (285 Python lines) |
| `skills-assembler.test.ts` | `tests/node/assembler/` | ~450 | Full test suite covering selection logic and assembly |

### `SkillsAssembler` Class API

```typescript
export class SkillsAssembler {
  // --- Selection (pure logic, no I/O) ---
  selectCoreSkills(resourcesDir: string): string[];
  selectConditionalSkills(config: ProjectConfig): string[];
  selectKnowledgePacks(config: ProjectConfig): string[];

  // --- Assembly (file I/O via copy-helpers) ---
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[];
}
```

**Design decisions:**

- All paths are `string` (not `Path`) -- consistent with `RulesAssembler` and
  the Node.js `path` module convention used throughout the TS codebase.
- Return type is `string[]` (list of generated paths) -- matches Python return
  type `List[Path]` semantically, and aligns with how the pipeline collects results.
- No `AssembleResult` wrapper needed (unlike RulesAssembler which returns warnings
  from the auditor). The Python `SkillsAssembler.assemble()` returns `List[Path]`
  directly with no warnings, so `string[]` is the correct 1:1 migration.
- Private helper methods mirror the Python structure exactly:
  `_selectInterfaceSkills`, `_selectInfraSkills`, `_selectTestingSkills`,
  `_selectSecuritySkills`, `_selectDataPacks`, `_copyCoreSkill`,
  `_copyConditionalSkill`, `_copyKnowledgePack`, `_copyNonSkillItems`,
  `_copyStackPatterns`, `_copyInfraPatterns`, `_assembleCore`,
  `_assembleConditional`, `_assembleKnowledge`.

### Constants (module-level)

```typescript
const SKILLS_TEMPLATES_DIR = "skills-templates";
const CORE_DIR = "core";
const CONDITIONAL_DIR = "conditional";
const KNOWLEDGE_PACKS_DIR = "knowledge-packs";
const INFRA_PATTERNS_DIR = "infra-patterns";
const STACK_PATTERNS_DIR = "stack-patterns";
const LIB_DIR = "lib";
const SKILL_MD = "SKILL.md";
const SKILLS_OUTPUT = "skills";
```

## 3. Existing Classes to Modify

| File | Change |
|------|--------|
| `src/assembler/index.ts` | Add `export * from "./skills-assembler.js";` |

No other production code changes required. All dependencies are already migrated.

## 4. Dependency Direction Validation

```
skills-assembler.ts
  |-- imports --> src/models.ts (ProjectConfig)           [OK: assembler -> models]
  |-- imports --> src/template-engine.ts (TemplateEngine)  [OK: assembler -> engine]
  |-- imports --> src/assembler/conditions.ts              [OK: same layer]
  |-- imports --> src/assembler/copy-helpers.ts            [OK: same layer]
  |-- imports --> src/domain/skill-registry.ts             [OK: assembler -> domain]
  |-- imports --> src/domain/stack-pack-mapping.ts         [OK: assembler -> domain]
  |-- imports --> node:fs, node:path                       [OK: stdlib]
```

All dependencies point inward (assembler -> domain) or peer (assembler -> assembler).
No circular dependencies introduced.

## 5. Integration Points

### Upstream consumers (future, not in this story)

The Python pipeline (`__init__.py`) calls `SkillsAssembler.assemble()` as one of
14 assemblers. The TS equivalent pipeline will eventually call:

```typescript
const skills = new SkillsAssembler();
generated.push(...skills.assemble(config, outputDir, resourcesDir, engine));
```

This is a **future integration** (pipeline migration story). For STORY-010,
the class is exported but not yet wired into a TS pipeline.

### Dependencies already migrated

| Dependency | Migrated in | Verified |
|------------|-------------|----------|
| `conditions.ts` (hasInterface, hasAnyInterface) | STORY-008 | Yes |
| `copy-helpers.ts` (copyTemplateFile, copyTemplateTree, copyTemplateTreeIfExists) | STORY-008 | Yes |
| `skill-registry.ts` (CORE_KNOWLEDGE_PACKS, buildInfraPackRules) | STORY-007 | Yes |
| `stack-pack-mapping.ts` (getStackPackName) | STORY-006 | Yes |
| `models.ts` (ProjectConfig, InfraConfig, etc.) | STORY-003 | Yes |
| `template-engine.ts` (TemplateEngine) | STORY-005 | Yes |

## 6. Database Changes

None. This is a file-generation tool with no database.

## 7. API Changes

None. No HTTP/CLI interface changes.

## 8. Event Changes

None. No event-driven components.

## 9. Configuration Changes

None. No new environment variables or configuration files.

## 10. Risk Assessment

### Low Risk

- **Pattern established**: This is the 4th assembler migrated (after conditions,
  copy-helpers, rules-assembler). The pattern is well-proven.
- **All dependencies available**: Every import used by `skills.py` has a
  tested TS counterpart already in the codebase.
- **Pure function selection logic**: The `select*` methods are pure functions
  that depend only on `ProjectConfig` and can be tested without any file I/O.
- **Comprehensive Python test suite**: 60+ Python tests across
  `test_skills_selection.py` and `test_skills_assembly.py` provide an exact
  specification for the TS port.

### Medium Risk

- **`_copyNonSkillItems` method**: Uses `shutil.copytree` / `shutil.copy2` in
  Python. The TS equivalent must use `fs.cpSync` (recursive) and `fs.copyFileSync`.
  The "skip existing" semantics require careful handling since `fs.cpSync` does not
  have a built-in skip-if-exists option -- the method must check `fs.existsSync`
  before each copy, matching the Python `target.exists()` guard.
- **File class line limit**: The Python source is 285 lines. The TS version
  splits into two files to stay under 250 per Rule 03:
  - `skills-selection.ts` (90 lines) — pure selection logic, no file I/O
  - `skills-assembler.ts` (219 lines) — class with file I/O assembly

### Mitigation

- Port selection methods first (pure logic, easy to test in isolation)
- Port assembly methods second (I/O, requires tmp directory fixtures)
- Selection functions extracted into `skills-selection.ts` to comply with the
  250-line limit

## Implementation Order

1. Create `src/assembler/skills-assembler.ts` with constants and class skeleton
2. Implement selection methods: `selectCoreSkills`, `selectConditionalSkills`,
   `selectKnowledgePacks` (and all private `_select*` helpers)
3. Implement copy methods: `_copyCoreSkill`, `_copyConditionalSkill`,
   `_copyKnowledgePack`, `_copyNonSkillItems`, `_copyStackPatterns`,
   `_copyInfraPatterns`
4. Implement orchestration: `assemble`, `_assembleCore`, `_assembleConditional`,
   `_assembleKnowledge`
5. Update `src/assembler/index.ts` with the new export
6. Create `tests/node/assembler/skills-assembler.test.ts` with:
   - Selection tests (mirroring `test_skills_selection.py`)
   - Assembly tests (mirroring `test_skills_assembly.py`)
7. Verify compilation with `npx tsc --noEmit`
8. Run tests with `npx vitest run` and verify >= 95% line / >= 90% branch coverage

## Test Strategy

### Selection tests (no I/O, ~30 tests)

| Group | Tests | Pattern |
|-------|-------|---------|
| `selectConditionalSkills` | 26 | Create config variants, assert skill in/not-in result |
| `selectKnowledgePacks` | 5 | Core packs always included, data packs conditional |
| `selectCoreSkills` | 5 | Filesystem scanning with tmp dirs |

### Assembly tests (I/O with tmp dirs, ~20 tests)

| Group | Tests | Pattern |
|-------|-------|---------|
| `_copyCoreSkill` | 2 | Directory creation, placeholder replacement |
| `_copyConditionalSkill` | 2 | Exists returns path, missing returns null |
| `_copyKnowledgePack` | 5 | Overwrite SKILL.md, preserve existing, edge cases |
| `_copyStackPatterns` | 2 | Known framework copies, unknown returns null |
| `_copyInfraPatterns` | 4 | K8s, Docker, Terraform, all-none |
| `assemble` (integration) | 5 | Full pipeline with mixed config |

### Test helpers to create

```typescript
function buildFullConfig(overrides?: Partial<...>): ProjectConfig;
function buildMinimalConfig(): ProjectConfig;
function createSkillTemplate(resourcesDir, category, skillName, content): string;
function createKpTemplate(resourcesDir, packName, content): string;
```

These mirror the Python `conftest.py` fixtures and `_create_skill_template` /
`_create_kp_template` helpers from `test_skills_assembly.py`.
