# Test Plan — story-0005-0003: SKILL.md Skeleton + Input Parsing

## Summary

This story creates the `x-dev-epic-implement` skill template (`resources/skills-templates/core/x-dev-epic-implement/SKILL.md`) and its GitHub Copilot counterpart (`resources/github-skills-templates/dev/x-dev-epic-implement.md`). The `.claude/` side uses auto-discovery (no code change); the `.github/` side requires registering in `SKILL_GROUPS.dev`; the `.agents/` side mirrors `.claude/` automatically.

Testing covers: (1) content validation of the SKILL.md template (frontmatter, input parsing, prerequisites, phases), (2) GitHub assembler registration, (3) golden file parity across all 8 profiles.

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Content validation (unit) | Verify SKILL.md structure, frontmatter, required sections, input parsing, prerequisites, phases | YES | `tests/node/content/x-dev-epic-implement-content.test.ts` |
| GitHub assembler registration (unit) | Verify `SKILL_GROUPS.dev` includes `x-dev-epic-implement` | YES (update existing) | `tests/node/assembler/github-skills-assembler.test.ts` |
| Golden file integration | Verify pipeline output matches updated golden files for all 8 profiles | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |
| Skills assembler (unit) | Auto-discovery of new core skill directory | NO (existing) | `tests/node/assembler/skills-assembler.test.ts` |

---

## 2. Acceptance Tests (Outer Loop)

### AT-1: New core skill is auto-discovered and appears in all 3 output locations

| Field | Details |
|-------|---------|
| ID | AT-1 |
| Test Name | Validated by the combination of UT-1 (template exists) + IT-1/IT-2/IT-3 (golden file parity includes `.claude/skills/`, `.github/skills/`, `.agents/skills/` copies) |
| What It Validates | End-to-end: placing `x-dev-epic-implement/SKILL.md` in `resources/skills-templates/core/` causes `SkillsAssembler` to auto-discover it, the pipeline generates it in `.claude/skills/`, `CodexSkillsAssembler` mirrors to `.agents/skills/`, and `GithubSkillsAssembler` generates `.github/skills/` copy — all verified by golden file byte-for-byte parity |
| Depends On | UT-1, UT-12, IT-1, IT-2, IT-3 |
| Parallel | No (requires all sub-tests to pass) |

### AT-2: SKILL.md is a valid, invocable skill with correct frontmatter and complete skeleton

| Field | Details |
|-------|---------|
| ID | AT-2 |
| Test Name | Validated by the combination of UT-1 through UT-11 (content tests covering every Gherkin acceptance criterion) |
| What It Validates | The SKILL.md has valid YAML frontmatter (`name`, `description`, `allowed-tools`, `argument-hint`), documents input parsing for all 7 arguments (1 positional + 6 flags), lists 5 prerequisite checks with error messages, and contains 4 phase sections (0-3) |
| Depends On | All UT-* tests |
| Parallel | No |

---

## 3. Unit Tests — Content Validation

### File: `tests/node/content/x-dev-epic-implement-content.test.ts`

Source file under test: `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`

TPP ordering: degenerate (file exists) -> constant (frontmatter fields) -> scalar (individual sections) -> collection (multiple flags, multiple prerequisites) -> edge (phase placeholders, error messages).

---

#### UT-1: SKILL.md exists and has valid YAML frontmatter

| Field | Details |
|-------|---------|
| ID | UT-1 |
| Test Name | `skillMd_fileExists_hasValidYamlFrontmatter` |
| What It Validates | File exists, starts with `---`, contains `name: x-dev-epic-implement`, contains `description:` field |
| Depends On | TASK-T1 |
| Parallel | Yes |

#### UT-2: Frontmatter contains allowed-tools with required tools

| Field | Details |
|-------|---------|
| ID | UT-2 |
| Test Name | `skillMd_frontmatter_containsAllowedTools` |
| What It Validates | Frontmatter includes `allowed-tools` containing `Read`, `Write`, `Edit`, `Bash`, `Grep`, `Glob`, `Skill` |
| Depends On | TASK-T1 |
| Parallel | Yes |

Implemented as `it.each` over the 7 required tools:

| # | Tool | Test Name |
|---|------|-----------|
| 2a | `Read` | `skillMd_frontmatter_allowedTools_containsTool_Read` |
| 2b | `Write` | `skillMd_frontmatter_allowedTools_containsTool_Write` |
| 2c | `Edit` | `skillMd_frontmatter_allowedTools_containsTool_Edit` |
| 2d | `Bash` | `skillMd_frontmatter_allowedTools_containsTool_Bash` |
| 2e | `Grep` | `skillMd_frontmatter_allowedTools_containsTool_Grep` |
| 2f | `Glob` | `skillMd_frontmatter_allowedTools_containsTool_Glob` |
| 2g | `Skill` | `skillMd_frontmatter_allowedTools_containsTool_Skill` |

#### UT-3: Frontmatter contains argument-hint with all flags

| Field | Details |
|-------|---------|
| ID | UT-3 |
| Test Name | `skillMd_frontmatter_containsArgumentHintWithAllFlags` |
| What It Validates | Frontmatter `argument-hint` includes `EPIC-ID`, `--phase`, `--story`, `--skip-review`, `--dry-run`, `--resume`, `--parallel` |
| Depends On | TASK-T1 |
| Parallel | Yes |

Implemented as `it.each` over the 7 argument tokens:

| # | Token | Test Name |
|---|-------|-----------|
| 3a | `EPIC-ID` | `skillMd_frontmatter_argumentHint_contains_EPIC-ID` |
| 3b | `--phase` | `skillMd_frontmatter_argumentHint_contains_--phase` |
| 3c | `--story` | `skillMd_frontmatter_argumentHint_contains_--story` |
| 3d | `--skip-review` | `skillMd_frontmatter_argumentHint_contains_--skip-review` |
| 3e | `--dry-run` | `skillMd_frontmatter_argumentHint_contains_--dry-run` |
| 3f | `--resume` | `skillMd_frontmatter_argumentHint_contains_--resume` |
| 3g | `--parallel` | `skillMd_frontmatter_argumentHint_contains_--parallel` |

#### UT-4: SKILL.md contains Global Output Policy

| Field | Details |
|-------|---------|
| ID | UT-4 |
| Test Name | `skillMd_containsGlobalOutputPolicy_englishOnly` |
| What It Validates | Contains `Global Output Policy` section with `English ONLY` per project convention (matching `x-dev-lifecycle` pattern) |
| Depends On | TASK-T1 |
| Parallel | Yes |

#### UT-5: SKILL.md contains required top-level sections

| Field | Details |
|-------|---------|
| ID | UT-5 |
| Test Name | `skillMd_containsRequiredSections_allPresent` |
| What It Validates | Contains all required H2/H3 sections: When to Use, Input Parsing, Prerequisites Check, Phase 0, Phase 1, Phase 2, Phase 3 |
| Depends On | TASK-T1 |
| Parallel | Yes |

Implemented as `it.each` over the required section headings:

| # | Section Heading (substring) | Test Name |
|---|----------------------------|-----------|
| 5a | `When to Use` | `skillMd_containsSection_WhenToUse` |
| 5b | `Input Parsing` | `skillMd_containsSection_InputParsing` |
| 5c | `Prerequisites Check` or `Prerequisites` | `skillMd_containsSection_PrerequisitesCheck` |
| 5d | `Phase 0` | `skillMd_containsSection_Phase0` |
| 5e | `Phase 1` | `skillMd_containsSection_Phase1` |
| 5f | `Phase 2` | `skillMd_containsSection_Phase2` |
| 5g | `Phase 3` | `skillMd_containsSection_Phase3` |

#### UT-6: Input Parsing section documents epic ID as required positional argument

| Field | Details |
|-------|---------|
| ID | UT-6 |
| Test Name | `skillMd_inputParsing_containsEpicIdAsRequired` |
| What It Validates | Input Parsing section contains reference to epic ID as a mandatory/required positional argument. Must contain `epic` (case-insensitive) and `required` or `mandatory` or `positional`. |
| Depends On | TASK-T1 |
| Parallel | Yes |

#### UT-7: Input Parsing section documents all 6 optional flags

| Field | Details |
|-------|---------|
| ID | UT-7 |
| Test Name | `skillMd_inputParsing_containsAllOptionalFlags` |
| What It Validates | Input Parsing section documents all 6 optional flags: `--phase`, `--story`, `--skip-review`, `--dry-run`, `--resume`, `--parallel` |
| Depends On | TASK-T1 |
| Parallel | Yes |

Implemented as `it.each` over the 6 flags:

| # | Flag | Test Name |
|---|------|-----------|
| 7a | `--phase` | `skillMd_inputParsing_containsFlag_--phase` |
| 7b | `--story` | `skillMd_inputParsing_containsFlag_--story` |
| 7c | `--skip-review` | `skillMd_inputParsing_containsFlag_--skip-review` |
| 7d | `--dry-run` | `skillMd_inputParsing_containsFlag_--dry-run` |
| 7e | `--resume` | `skillMd_inputParsing_containsFlag_--resume` |
| 7f | `--parallel` | `skillMd_inputParsing_containsFlag_--parallel` |

#### UT-8: Prerequisites Check section contains 5 prerequisite verifications

| Field | Details |
|-------|---------|
| ID | UT-8 |
| Test Name | `skillMd_prerequisitesCheck_contains5Checks` |
| What It Validates | Prerequisites section documents all 5 checks: epic directory existence (`docs/stories/epic-`), epic file (`EPIC-`), implementation map (`IMPLEMENTATION-MAP.md`), story files (at least one `story-` file), resume checkpoint (`execution-state.json` when `--resume`) |
| Depends On | TASK-T1 |
| Parallel | Yes |

Implemented as `it.each` over the 5 prerequisite keywords:

| # | Keyword/Pattern | Test Name |
|---|-----------------|-----------|
| 8a | `docs/stories/epic-` | `skillMd_prerequisites_containsCheck_epicDirectory` |
| 8b | `EPIC-` (file reference) | `skillMd_prerequisites_containsCheck_epicFile` |
| 8c | `IMPLEMENTATION-MAP` | `skillMd_prerequisites_containsCheck_implementationMap` |
| 8d | `story-` (file reference) | `skillMd_prerequisites_containsCheck_storyFiles` |
| 8e | `execution-state.json` | `skillMd_prerequisites_containsCheck_resumeCheckpoint` |

#### UT-9: Prerequisites section contains error messages for missing prerequisites

| Field | Details |
|-------|---------|
| ID | UT-9 |
| Test Name | `skillMd_prerequisites_containsErrorGuidance` |
| What It Validates | Prerequisites section includes guidance for recovery when a prerequisite is missing. Must reference at least one of: `not found`, `abort`, `error`, `missing`. |
| Depends On | TASK-T1 |
| Parallel | Yes |

#### UT-10: Phase 1-3 are placeholders referencing future stories

| Field | Details |
|-------|---------|
| ID | UT-10 |
| Test Name | `skillMd_phases1to3_arePlaceholders` |
| What It Validates | Phase 1, Phase 2, and Phase 3 sections indicate they are placeholders for future implementation. Each section contains `placeholder` or `story-0005` or `TODO` or `implemented in` or `extended by`. |
| Depends On | TASK-T1 |
| Parallel | Yes |

#### UT-11: Phase 0 contains preparation steps (non-placeholder)

| Field | Details |
|-------|---------|
| ID | UT-11 |
| Test Name | `skillMd_phase0_containsPreparationSteps` |
| What It Validates | Phase 0 (Preparation) is NOT a placeholder and contains substantive content about parsing, prerequisites, and branch creation. Must contain at least two of: `parse` or `parsing`, `prerequisite` or `prerequisites`, `branch`. |
| Depends On | TASK-T1 |
| Parallel | Yes |

---

## 4. Unit Tests — GitHub Skills Assembler Registration

### File: `tests/node/assembler/github-skills-assembler.test.ts` (existing, modified)

#### UT-12: SKILL_GROUPS.dev includes x-dev-epic-implement

| Field | Details |
|-------|---------|
| ID | UT-12 |
| Test Name | `SKILL_GROUPS_devGroup_includesXDevEpicImplement` |
| What It Validates | `SKILL_GROUPS["dev"]` contains `"x-dev-epic-implement"` after registration |
| Depends On | TASK-T3 |
| Parallel | Yes |

#### UT-13: SKILL_GROUPS.dev count is updated

| Field | Details |
|-------|---------|
| ID | UT-13 |
| Test Name | `SKILL_GROUPS_devGroup_contains7Skills` |
| What It Validates | `SKILL_GROUPS["dev"]` has length 7 (was 6, now includes `x-dev-epic-implement`) |
| Depends On | TASK-T3 |
| Parallel | Yes |

**Note:** The existing test `SKILL_GROUPS_devGroup_contains5Skills` (which currently asserts 6 entries despite its name) must be updated to assert 7 entries and renamed to `SKILL_GROUPS_devGroup_contains7Skills`.

---

## 5. Unit Tests — GitHub Skill Template Content

### File: `tests/node/content/x-dev-epic-implement-content.test.ts` (same file, additional describe block)

Source file under test: `resources/github-skills-templates/dev/x-dev-epic-implement.md`

#### UT-14: GitHub skill template exists and has valid YAML frontmatter

| Field | Details |
|-------|---------|
| ID | UT-14 |
| Test Name | `githubSkillMd_fileExists_hasValidYamlFrontmatter` |
| What It Validates | File exists, starts with `---`, contains `name: x-dev-epic-implement`, contains `description:` field |
| Depends On | TASK-T2 |
| Parallel | Yes |

#### UT-15: GitHub skill template contains When to Use section

| Field | Details |
|-------|---------|
| ID | UT-15 |
| Test Name | `githubSkillMd_containsSection_WhenToUse` |
| What It Validates | GitHub template contains `When to Use` section (condensed version of the Claude template) |
| Depends On | TASK-T2 |
| Parallel | Yes |

---

## 6. Integration Tests — Golden Files

### Existing file: `tests/node/integration/byte-for-byte.test.ts`

No new test code required. The existing byte-for-byte parity tests cover this story after golden files are regenerated.

#### IT-1: Pipeline output matches golden files for all 8 profiles (.claude/ copy)

| Field | Details |
|-------|---------|
| ID | IT-1 |
| Test Name | `pipelineMatchesGoldenFiles_{profile}` (existing parametrized test) |
| What It Validates | Pipeline output matches golden files byte-for-byte, which now include `.claude/skills/x-dev-epic-implement/SKILL.md` |
| Depends On | TASK-T4 (golden files updated) |
| Parallel | No (profiles run sequentially via `describe.sequential.each`) |

#### IT-2: Pipeline output matches golden files for all 8 profiles (.github/ copy)

| Field | Details |
|-------|---------|
| ID | IT-2 |
| Test Name | `pipelineMatchesGoldenFiles_{profile}` (same existing test) |
| What It Validates | The `.github/skills/x-dev-epic-implement/SKILL.md` is generated and matches golden files. Requires `SKILL_GROUPS.dev` registration (TASK-T3) |
| Depends On | TASK-T3, TASK-T4 |
| Parallel | Same as IT-1 (sequential per profile) |

#### IT-3: Pipeline output matches golden files for all 8 profiles (.agents/ copy)

| Field | Details |
|-------|---------|
| ID | IT-3 |
| Test Name | `pipelineMatchesGoldenFiles_{profile}` (same existing test) |
| What It Validates | The `.agents/skills/x-dev-epic-implement/SKILL.md` is mirrored from `.claude/` and matches golden files |
| Depends On | TASK-T4 |
| Parallel | Same as IT-1 |

#### IT-4: Updated README.md matches golden files for all 8 profiles

| Field | Details |
|-------|---------|
| ID | IT-4 |
| Test Name | `pipelineMatchesGoldenFiles_{profile}` (same existing test) |
| What It Validates | Generated `.claude/README.md` includes `x-dev-epic-implement` in the skills table and updated skill count |
| Depends On | TASK-T4 |
| Parallel | Same as IT-1 |

#### IT-5: All 8 profiles produce identical skill output

| Field | Details |
|-------|---------|
| ID | IT-5 |
| Test Name | `pipelineMatchesGoldenFiles_{profile}` for each of the 8 profiles |
| What It Validates | All profiles (go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs) produce identical skill output since this is an unconditional core skill with no `{single_brace}` placeholders |
| Depends On | IT-1, IT-2, IT-3, IT-4 |
| Parallel | No (sequential per profile) |

---

## 7. Test Dependency Graph

```
AT-1 (end-to-end presence in all outputs)
├── UT-1  (file exists, frontmatter valid)
├── UT-12 (SKILL_GROUPS includes skill)
├── IT-1  (golden .claude/)
├── IT-2  (golden .github/)
├── IT-3  (golden .agents/)
└── IT-4  (golden README.md)

AT-2 (valid invocable skill with complete skeleton)
├── UT-1  (frontmatter: name, description)
├── UT-2  (frontmatter: allowed-tools)
├── UT-3  (frontmatter: argument-hint with all flags)
├── UT-4  (Global Output Policy)
├── UT-5  (required sections: When to Use, Input Parsing, Prerequisites, Phases)
├── UT-6  (epic ID as required)
├── UT-7  (6 optional flags documented)
├── UT-8  (5 prerequisite checks)
├── UT-9  (error messages for missing prerequisites)
├── UT-10 (Phase 1-3 are placeholders)
└── UT-11 (Phase 0 has substantive content)
```

---

## 8. TDD Execution Order (Double-Loop)

| Step | Action | Test State | Loop |
|------|--------|-----------|------|
| 1 | Write AT-1 and AT-2 as comments describing acceptance criteria | DEFINED | Outer |
| 2 | Write UT-1 (`skillMd_fileExists_hasValidYamlFrontmatter`) | RED | Inner |
| 3 | Create `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` with minimal frontmatter | GREEN | Inner |
| 4 | Write UT-2 (`allowed-tools` 7 subtests) | RED | Inner |
| 5 | Add `allowed-tools` line to frontmatter | GREEN | Inner |
| 6 | Write UT-3 (`argument-hint` 7 subtests) | RED | Inner |
| 7 | Add `argument-hint` line to frontmatter | GREEN | Inner |
| 8 | Write UT-4 (`Global Output Policy`) | RED | Inner |
| 9 | Add Global Output Policy section | GREEN | Inner |
| 10 | Write UT-5 (`required sections` 7 subtests) | RED | Inner |
| 11 | Add section headings: When to Use, Input Parsing, Prerequisites Check, Phase 0-3 | GREEN | Inner |
| 12 | Write UT-6 (`epic ID as required`) | RED | Inner |
| 13 | Flesh out Input Parsing section with epic ID description | GREEN | Inner |
| 14 | Write UT-7 (`6 optional flags`) | RED | Inner |
| 15 | Flesh out Input Parsing section with all 6 flag descriptions | GREEN | Inner |
| 16 | Write UT-8 (`5 prerequisite checks`) | RED | Inner |
| 17 | Flesh out Prerequisites Check section with all 5 checks | GREEN | Inner |
| 18 | Write UT-9 (`error guidance`) | RED | Inner |
| 19 | Add error messages and recovery guidance to prerequisites | GREEN | Inner |
| 20 | Write UT-10 (`Phase 1-3 placeholders`) | RED | Inner |
| 21 | Mark Phase 1-3 sections as placeholders | GREEN | Inner |
| 22 | Write UT-11 (`Phase 0 substantive content`) | RED | Inner |
| 23 | Flesh out Phase 0 with preparation steps | GREEN | Inner |
| 24 | Refactor: review SKILL.md for consistency with `x-dev-lifecycle` pattern | REFACTOR | Inner |
| 25 | Write UT-14 (`GitHub template exists with frontmatter`) | RED | Inner |
| 26 | Create `resources/github-skills-templates/dev/x-dev-epic-implement.md` | GREEN | Inner |
| 27 | Write UT-15 (`GitHub template has When to Use`) | RED | Inner |
| 28 | Add When to Use section to GitHub template | GREEN | Inner |
| 29 | Write UT-12, UT-13 (`SKILL_GROUPS.dev` registration) | RED | Inner |
| 30 | Add `"x-dev-epic-implement"` to `SKILL_GROUPS.dev` in `github-skills-assembler.ts` | GREEN | Inner |
| 31 | Update existing test assertion for dev group count | GREEN | Inner |
| 32 | Regenerate golden files for all 8 profiles | N/A | Outer |
| 33 | Run `byte-for-byte.test.ts` | GREEN (IT-1 through IT-5) | Outer |
| 34 | Run full test suite (`npx vitest run`) | GREEN (AT-1, AT-2 validated) | Outer |

---

## 9. Content Verification — Key Sections and Keywords

### 9.1 Frontmatter

| Keyword/Pattern | Purpose | Test |
|-----------------|---------|------|
| `name: x-dev-epic-implement` | Correct skill name | UT-1 |
| `description:` | Description field present | UT-1 |
| `allowed-tools:` with `Read, Write, Edit, Bash, Grep, Glob, Skill` | Tool permissions | UT-2 |
| `argument-hint:` with all flags | Usage hint | UT-3 |

### 9.2 Required Sections

| Keyword/Pattern | Purpose | Test |
|-----------------|---------|------|
| `Global Output Policy` + `English ONLY` | Standard policy block | UT-4 |
| `When to Use` | Decision tree for invocation | UT-5a |
| `Input Parsing` | Argument documentation | UT-5b |
| `Prerequisites` | Pre-checks before execution | UT-5c |
| `Phase 0` | Preparation phase | UT-5d |
| `Phase 1` | Execution loop placeholder | UT-5e |
| `Phase 2` | Consolidation placeholder | UT-5f |
| `Phase 3` | Verification placeholder | UT-5g |

### 9.3 Input Parsing Keywords

| Keyword/Pattern | Purpose | Test |
|-----------------|---------|------|
| `epic` + `required` or `mandatory` or `positional` | Epic ID is required | UT-6 |
| `--phase`, `--story`, `--skip-review`, `--dry-run`, `--resume`, `--parallel` | All 6 flags | UT-7 |

### 9.4 Prerequisites Keywords

| Keyword/Pattern | Purpose | Test |
|-----------------|---------|------|
| `docs/stories/epic-` | Epic directory check | UT-8a |
| `EPIC-` | Epic file check | UT-8b |
| `IMPLEMENTATION-MAP` | Map file check | UT-8c |
| `story-` | Story files existence check | UT-8d |
| `execution-state.json` | Resume checkpoint check | UT-8e |
| `not found` or `abort` or `error` or `missing` | Error handling | UT-9 |

### 9.5 Phase Keywords

| Keyword/Pattern | Purpose | Test |
|-----------------|---------|------|
| `placeholder` or `story-0005` or `TODO` or `implemented in` or `extended by` | Phases 1-3 are deferred | UT-10 |
| `pars` + `prerequisite` + `branch` (at least 2 of 3) | Phase 0 has real content | UT-11 |

---

## 10. Suggested Test Implementation Pattern

```typescript
import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const SKILL_PATH = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-dev-epic-implement/SKILL.md",
);

const GITHUB_SKILL_PATH = path.resolve(
  __dirname, "../../..",
  "resources/github-skills-templates/dev/x-dev-epic-implement.md",
);

const content = fs.readFileSync(SKILL_PATH, "utf-8");

const REQUIRED_TOOLS = [
  "Read", "Write", "Edit", "Bash", "Grep", "Glob", "Skill",
];

const ARGUMENT_TOKENS = [
  "EPIC-ID", "--phase", "--story",
  "--skip-review", "--dry-run", "--resume", "--parallel",
];

const REQUIRED_SECTIONS = [
  "When to Use",
  "Input Parsing",
  "Prerequisites",
  "Phase 0",
  "Phase 1",
  "Phase 2",
  "Phase 3",
];

const OPTIONAL_FLAGS = [
  "--phase", "--story", "--skip-review",
  "--dry-run", "--resume", "--parallel",
];

const PREREQUISITE_KEYWORDS = [
  ["docs/stories/epic-", "epicDirectory"],
  ["EPIC-", "epicFile"],
  ["IMPLEMENTATION-MAP", "implementationMap"],
  ["story-", "storyFiles"],
  ["execution-state.json", "resumeCheckpoint"],
] as const;

describe("x-dev-epic-implement SKILL.md — frontmatter", () => {
  it("skillMd_fileExists_hasValidYamlFrontmatter", () => {
    expect(content).toMatch(/^---\n/);
    expect(content).toContain("name: x-dev-epic-implement");
    expect(content).toContain("description:");
  });

  it.each(
    REQUIRED_TOOLS.map((t) => [t]),
  )("skillMd_frontmatter_allowedTools_containsTool_%s", (tool) => {
    expect(content).toContain("allowed-tools:");
    expect(content).toMatch(
      new RegExp(`allowed-tools:.*${tool}`, "s"),
    );
  });

  it.each(
    ARGUMENT_TOKENS.map((t) => [t]),
  )("skillMd_frontmatter_argumentHint_contains_%s", (token) => {
    expect(content).toContain("argument-hint:");
    expect(content).toContain(token);
  });
});

describe("x-dev-epic-implement SKILL.md — global output policy", () => {
  it("skillMd_containsGlobalOutputPolicy_englishOnly", () => {
    expect(content).toContain("Global Output Policy");
    expect(content).toMatch(/English ONLY/i);
  });
});

describe("x-dev-epic-implement SKILL.md — required sections", () => {
  it.each(
    REQUIRED_SECTIONS.map((s) => [s]),
  )("skillMd_containsSection_%s", (section) => {
    expect(content).toContain(section);
  });
});

describe("x-dev-epic-implement SKILL.md — input parsing", () => {
  it("skillMd_inputParsing_containsEpicIdAsRequired", () => {
    expect(content).toMatch(/epic/i);
    expect(content).toMatch(/required|mandatory|positional/i);
  });

  it.each(
    OPTIONAL_FLAGS.map((f) => [f]),
  )("skillMd_inputParsing_containsFlag_%s", (flag) => {
    expect(content).toContain(flag);
  });
});

describe("x-dev-epic-implement SKILL.md — prerequisites check", () => {
  it.each(
    PREREQUISITE_KEYWORDS.map(([kw, label]) => [kw, label]),
  )("skillMd_prerequisites_containsCheck_%s", (keyword) => {
    expect(content).toContain(keyword as string);
  });

  it("skillMd_prerequisites_containsErrorGuidance", () => {
    expect(content).toMatch(/not found|abort|error|missing/i);
  });
});

describe("x-dev-epic-implement SKILL.md — phase structure", () => {
  it("skillMd_phases1to3_arePlaceholders", () => {
    const phase1Idx = content.indexOf("Phase 1");
    const phase1Content = content.slice(
      phase1Idx, content.indexOf("Phase 2", phase1Idx),
    );
    expect(phase1Content).toMatch(
      /placeholder|story-0005|TODO|implemented in|extended by/i,
    );

    const phase2Idx = content.indexOf("Phase 2");
    const phase2Content = content.slice(
      phase2Idx, content.indexOf("Phase 3", phase2Idx),
    );
    expect(phase2Content).toMatch(
      /placeholder|story-0005|TODO|implemented in|extended by/i,
    );

    const phase3Idx = content.indexOf("Phase 3");
    const phase3Content = content.slice(phase3Idx);
    expect(phase3Content).toMatch(
      /placeholder|story-0005|TODO|implemented in|extended by/i,
    );
  });

  it("skillMd_phase0_containsPreparationSteps", () => {
    const phase0Idx = content.indexOf("Phase 0");
    const phase0Content = content.slice(
      phase0Idx, content.indexOf("Phase 1", phase0Idx),
    );
    const hasParsing = /pars/i.test(phase0Content);
    const hasPrereqs = /prerequisite/i.test(phase0Content);
    const hasBranch = /branch/i.test(phase0Content);
    const matchCount = [hasParsing, hasPrereqs, hasBranch]
      .filter(Boolean).length;
    expect(matchCount).toBeGreaterThanOrEqual(2);
  });
});

describe("x-dev-epic-implement GitHub template", () => {
  const ghContent = fs.readFileSync(GITHUB_SKILL_PATH, "utf-8");

  it("githubSkillMd_fileExists_hasValidYamlFrontmatter", () => {
    expect(ghContent).toMatch(/^---\n/);
    expect(ghContent).toContain("name: x-dev-epic-implement");
    expect(ghContent).toContain("description:");
  });

  it("githubSkillMd_containsSection_WhenToUse", () => {
    expect(ghContent).toContain("When to Use");
  });
});
```

---

## 11. Existing Tests Affected by This Story

### 11.1 `tests/node/assembler/github-skills-assembler.test.ts`

| Current Test | Current Assertion | Required Change |
|-------------|-------------------|----------------|
| `SKILL_GROUPS_devGroup_contains5Skills` | `expect(SKILL_GROUPS["dev"]).toEqual([...6 items...])` | Update to include `"x-dev-epic-implement"` and rename to `_contains7Skills` |

### 11.2 `tests/node/assembler/skills-assembler.test.ts`

No changes required. `selectCoreSkills()` auto-discovers from the filesystem, and existing tests use ephemeral temp directories with synthetic skills. The real skill is validated via golden file integration tests.

### 11.3 `tests/node/integration/byte-for-byte.test.ts`

No code changes required. Updated golden files make the existing tests cover the new skill.

---

## 12. Golden Files Requiring Update

**Total: ~26+ golden files** (8 profiles x 3 output directories + README.md changes)

### 12.1 `.claude/` golden files (8 new files)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-dev-epic-implement/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-dev-epic-implement/SKILL.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-dev-epic-implement/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-dev-epic-implement/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-dev-epic-implement/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-dev-epic-implement/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-dev-epic-implement/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-dev-epic-implement/SKILL.md` |

### 12.2 `.agents/` golden files (8 new files)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.agents/skills/x-dev-epic-implement/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.agents/skills/x-dev-epic-implement/SKILL.md` |
| java-spring | `tests/golden/java-spring/.agents/skills/x-dev-epic-implement/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.agents/skills/x-dev-epic-implement/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.agents/skills/x-dev-epic-implement/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.agents/skills/x-dev-epic-implement/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.agents/skills/x-dev-epic-implement/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.agents/skills/x-dev-epic-implement/SKILL.md` |

### 12.3 `.github/` golden files (8 new files)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.github/skills/x-dev-epic-implement/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.github/skills/x-dev-epic-implement/SKILL.md` |
| java-spring | `tests/golden/java-spring/.github/skills/x-dev-epic-implement/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.github/skills/x-dev-epic-implement/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.github/skills/x-dev-epic-implement/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.github/skills/x-dev-epic-implement/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.github/skills/x-dev-epic-implement/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.github/skills/x-dev-epic-implement/SKILL.md` |

### 12.4 README.md updates (8 modified files)

Golden file `README.md` files for `.claude/README.md` regenerated with updated skill count and table.

---

## 13. Gherkin Scenario Coverage Mapping

| Gherkin Scenario | Test IDs |
|------------------|----------|
| Invocation with valid epic ID and existing directory | UT-6, UT-8 (prerequisites documented) |
| Invocation with all optional flags | UT-3, UT-7 (all flags in frontmatter and parsing) |
| Failure when epic directory does not exist | UT-8a, UT-9 (error guidance) |
| Failure when IMPLEMENTATION-MAP.md is missing | UT-8c, UT-9 |
| Failure when --resume without checkpoint | UT-8e, UT-9 |
| SKILL.md contains valid YAML frontmatter | UT-1, UT-2, UT-3 |
| Invocation without epic ID | UT-6 (documents required nature of epic ID) |

---

## 14. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Golden file mismatch after adding skill | Regenerate all 8 profiles via pipeline; byte-for-byte tests catch drift |
| `.github/` copy missing (`SKILL_GROUPS` not updated) | UT-12 explicitly tests registration; IT-2 golden file test catches missing output |
| SKILL.md content tests too brittle | Use `toContain()` and `toMatch()` for semantic presence, not exact line matching |
| README.md skill count changes break golden files | Regeneration of golden files includes updated README.md |
| Phase placeholders not extensible by future stories | UT-10 validates placeholder markers are present; review ensures consistent heading format |
| Existing `SKILL_GROUPS_devGroup_contains5Skills` test breaks | UT-13 replaces it with correct count assertion |

---

## 15. Test Count Summary

| Category | New Tests | Existing Tests (Covering Story) |
|----------|-----------|------|
| Content validation — Claude SKILL.md (UT-1 through UT-11) | ~30 (including `it.each` expansions: 7 tools + 7 tokens + 7 sections + 6 flags + 5 prereqs + 4 standalone) | 0 |
| Content validation — GitHub template (UT-14, UT-15) | 2 | 0 |
| Assembler registration (UT-12, UT-13) | 2 (1 new + 1 modified) | 0 |
| Golden file integration (IT-1 through IT-5) | 0 | 40 (8 profiles x 5 assertions) |
| **Total** | **~34** | **~40** |

---

## 16. Verification Checklist

- [ ] `npx vitest run tests/node/content/x-dev-epic-implement-content.test.ts` — all content validation tests pass
- [ ] `npx vitest run tests/node/assembler/github-skills-assembler.test.ts` — updated registration test passes
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` — all 8 profiles pass
- [ ] `npx vitest run` — full suite passes
- [ ] Coverage remains >= 95% line, >= 90% branch
- [ ] No compiler/linter warnings
- [ ] `x-dev-epic-implement` appears in generated `.claude/skills/`, `.agents/skills/`, and `.github/skills/` directories
- [ ] Generated `.claude/README.md` includes `x-dev-epic-implement` in skill table with correct count
