# Test Plan — story-0004-0015: ADR Automation

## Summary

This story adds a new core skill template (`resources/skills-templates/core/x-dev-adr-automation/SKILL.md`) that is auto-discovered by `SkillsAssembler` and copied to `.claude/skills/` in all generated outputs. A corresponding GitHub template must be added to `resources/github-skills-templates/` and registered in `GithubSkillsAssembler.SKILL_GROUPS` for dual copy (RULE-001). The `CodexSkillsAssembler` mirrors from `.claude/skills/` automatically.

No TypeScript logic changes are required for the `.claude/` side (auto-discovery). The `.github/` side requires adding the template file and an entry in the `SKILL_GROUPS` constant. Testing focuses on: (1) content validation of the SKILL.md template, (2) golden file parity across all 8 profiles, and (3) acceptance that the skill appears in generated output.

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Content validation (unit) | Verify SKILL.md structure, frontmatter, required sections | YES | `tests/node/content/x-dev-adr-automation-content.test.ts` |
| Golden file integration | Verify pipeline output matches updated golden files for all 8 profiles | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |
| Assembler unit tests | Verify `SkillsAssembler` includes new skill | NO (existing) | `tests/node/assembler/skills-assembler.test.ts` |
| Assembler unit tests | Verify `GithubSkillsAssembler` includes new skill | NO (existing, but `SKILL_GROUPS` needs update) | `tests/node/assembler/github-skills-assembler.test.ts` |

---

## 2. Unit Tests — Content Validation

### File: `tests/node/content/x-dev-adr-automation-content.test.ts`

Source file under test: `resources/skills-templates/core/x-dev-adr-automation/SKILL.md`

TPP ordering: degenerate (file exists, parseable) -> unconditional (required structure) -> conditions (specific section content) -> edge cases (example conversion, cross-reference details).

#### UT-1: SKILL.md exists and has valid YAML frontmatter

| Field | Details |
|-------|---------|
| ID | UT-1 |
| Test Name | `skillMd_fileExists_hasValidYamlFrontmatter` |
| What It Validates | File exists, starts with `---`, contains `name: x-dev-adr-automation`, contains `description:` field |
| Depends On | None |
| Parallel | Yes (independent of other content tests once file is read) |

#### UT-2: SKILL.md contains required sections

| Field | Details |
|-------|---------|
| ID | UT-2 |
| Test Name | `skillMd_containsRequiredSections_allPresent` |
| What It Validates | SKILL.md contains all required H2/H3 sections: When to Use, Input Format, Output Format, Algorithm, Duplicate Detection, Cross-Reference Rules, Examples |
| Depends On | UT-1 (file must exist) |
| Parallel | Yes (with UT-3 through UT-7) |

Implemented as `it.each` over the required section headings:

| # | Section Heading (substring) | Test Name |
|---|----------------------------|-----------|
| 2a | `When to Use` | `skillMd_containsSection_WhenToUse` |
| 2b | `Input Format` | `skillMd_containsSection_InputFormat` |
| 2c | `Output Format` | `skillMd_containsSection_OutputFormat` |
| 2d | `Algorithm` | `skillMd_containsSection_Algorithm` |
| 2e | `Duplicate Detection` | `skillMd_containsSection_DuplicateDetection` |
| 2f | `Cross-Reference` | `skillMd_containsSection_CrossReference` |
| 2g | `Examples` or `Example` | `skillMd_containsSection_Examples` |

#### UT-3: SKILL.md contains duplicate detection instructions

| Field | Details |
|-------|---------|
| ID | UT-3 |
| Test Name | `skillMd_duplicateDetection_containsTitleSimilarityCheck` |
| What It Validates | The Duplicate Detection section describes checking existing ADRs by title similarity before creating a new one. Must contain keywords: `title`, `existing`, `skip` or `warning`. |
| Depends On | UT-1 |
| Parallel | Yes |

#### UT-4: SKILL.md contains cross-reference rules

| Field | Details |
|-------|---------|
| ID | UT-4 |
| Test Name | `skillMd_crossReference_containsStoryRefAndADRLinks` |
| What It Validates | Cross-Reference section contains `story-ref` (frontmatter in generated ADR) and instructions to update the architecture plan with ADR links |
| Depends On | UT-1 |
| Parallel | Yes |

#### UT-5: SKILL.md contains sequential numbering algorithm

| Field | Details |
|-------|---------|
| ID | UT-5 |
| Test Name | `skillMd_algorithm_containsSequentialNumbering` |
| What It Validates | Algorithm section describes scanning `docs/adr/` for existing ADRs, finding the maximum number, and incrementing. Must contain `ADR-` pattern and numbering instructions (e.g., `NNNN`, `max`, `next`, `sequential`). |
| Depends On | UT-1 |
| Parallel | Yes |

#### UT-6: SKILL.md contains mini-ADR input format definition

| Field | Details |
|-------|---------|
| ID | UT-6 |
| Test Name | `skillMd_inputFormat_containsMiniADRFields` |
| What It Validates | Input Format section defines the 4 mini-ADR fields from the story data contract: `title`, `context`, `decision`, `rationale`. All 4 must appear. |
| Depends On | UT-1 |
| Parallel | Yes |

Implemented as `it.each` over the 4 required fields:

| # | Field | Test Name |
|---|-------|-----------|
| 6a | `title` | `skillMd_inputFormat_containsField_title` |
| 6b | `context` | `skillMd_inputFormat_containsField_context` |
| 6c | `decision` | `skillMd_inputFormat_containsField_decision` |
| 6d | `rationale` | `skillMd_inputFormat_containsField_rationale` |

#### UT-7: SKILL.md contains example conversion (before/after)

| Field | Details |
|-------|---------|
| ID | UT-7 |
| Test Name | `skillMd_examples_containsBeforeAfterConversion` |
| What It Validates | Examples section contains both a mini-ADR input example and the corresponding full ADR output example. Must contain `Status` (output field), `Consequences` (output field), and `Context` appearing in an example context. |
| Depends On | UT-1 |
| Parallel | Yes |

#### UT-8: SKILL.md frontmatter contains allowed-tools

| Field | Details |
|-------|---------|
| ID | UT-8 |
| Test Name | `skillMd_frontmatter_containsAllowedTools` |
| What It Validates | Frontmatter includes `allowed-tools` with at least `Read`, `Write`, `Edit`, `Glob`, `Grep` |
| Depends On | UT-1 |
| Parallel | Yes |

#### UT-9: SKILL.md frontmatter contains argument-hint

| Field | Details |
|-------|---------|
| ID | UT-9 |
| Test Name | `skillMd_frontmatter_containsArgumentHint` |
| What It Validates | Frontmatter includes `argument-hint` field |
| Depends On | UT-1 |
| Parallel | Yes |

#### UT-10: SKILL.md output format contains ADR frontmatter fields

| Field | Details |
|-------|---------|
| ID | UT-10 |
| Test Name | `skillMd_outputFormat_containsADRFrontmatterFields` |
| What It Validates | Output Format section describes the full ADR structure including `status`, `date`, `story-ref` frontmatter fields and `Status`, `Context`, `Decision`, `Consequences` sections |
| Depends On | UT-1 |
| Parallel | Yes |

#### UT-11: SKILL.md contains index update instructions

| Field | Details |
|-------|---------|
| ID | UT-11 |
| Test Name | `skillMd_algorithm_containsIndexUpdateInstructions` |
| What It Validates | Algorithm or a dedicated section describes updating `docs/adr/README.md` with new entries after ADR creation |
| Depends On | UT-1 |
| Parallel | Yes |

#### UT-12: SKILL.md contains Global Output Policy

| Field | Details |
|-------|---------|
| ID | UT-12 |
| Test Name | `skillMd_containsGlobalOutputPolicy_englishOnly` |
| What It Validates | SKILL.md contains the standard Global Output Policy block (`English ONLY` or equivalent) per project convention |
| Depends On | UT-1 |
| Parallel | Yes |

---

## 3. Integration Tests — Golden Files

### Existing file: `tests/node/integration/byte-for-byte.test.ts`

No new tests required. The existing byte-for-byte parity tests cover this story after golden files are regenerated.

#### IT-1: Skill directory exists in generated `.claude/skills/x-dev-adr-automation/`

| Field | Details |
|-------|---------|
| ID | IT-1 |
| Test Name | `pipelineMatchesGoldenFiles_{profile}` (existing parametrized test) |
| What It Validates | Pipeline output matches golden files byte-for-byte, which now include `.claude/skills/x-dev-adr-automation/SKILL.md` |
| Depends On | Golden files updated for all 8 profiles |
| Parallel | No (profiles run sequentially via `describe.sequential.each`) |

#### IT-2: Skill appears in generated `.claude/README.md` skill table

| Field | Details |
|-------|---------|
| ID | IT-2 |
| Test Name | `pipelineMatchesGoldenFiles_{profile}` (same existing test) |
| What It Validates | The generated README.md includes `x-dev-adr-automation` in the skills table and the updated skill count. Covered by byte-for-byte parity since README.md is a golden file. |
| Depends On | Golden files updated with new README.md content |
| Parallel | Same as IT-1 |

#### IT-3: Dual copy exists in `.github/skills/x-dev-adr-automation/`

| Field | Details |
|-------|---------|
| ID | IT-3 |
| Test Name | `pipelineMatchesGoldenFiles_{profile}` (same existing test) |
| What It Validates | The `.github/skills/x-dev-adr-automation/SKILL.md` is generated. Requires: (a) GitHub template at `resources/github-skills-templates/{group}/x-dev-adr-automation.md`, (b) entry in `SKILL_GROUPS` constant. Byte-for-byte parity confirms presence. |
| Depends On | `SKILL_GROUPS` updated in `github-skills-assembler.ts`, golden files updated |
| Parallel | Same as IT-1 |

#### IT-4: All 8 profiles include the skill

| Field | Details |
|-------|---------|
| ID | IT-4 |
| Test Name | `pipelineMatchesGoldenFiles_{profile}` for each of the 8 profiles |
| What It Validates | All profiles (go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs) produce identical skill output since this is an unconditional core skill |
| Depends On | IT-1, IT-2, IT-3 |
| Parallel | No (sequential per profile) |

---

## 4. Acceptance Tests

#### AT-1: New core skill is auto-discovered and appears in all output locations

| Field | Details |
|-------|---------|
| ID | AT-1 |
| Test Name | Validated by the combination of UT-1 (template exists) + IT-1/IT-3/IT-4 (generated output matches golden files) |
| What It Validates | End-to-end: placing `x-dev-adr-automation/SKILL.md` in `resources/skills-templates/core/` causes `SkillsAssembler` to auto-discover it, the pipeline to generate it in `.claude/skills/`, `CodexSkillsAssembler` to mirror it to `.agents/skills/`, and `GithubSkillsAssembler` to generate the `.github/skills/` copy — all verified by golden file parity |
| Depends On | UT-1, IT-1, IT-2, IT-3, IT-4 |
| Parallel | No (requires all tests to pass) |

---

## 5. Golden Files Requiring Update

**Total: ~26+ golden files** (8 profiles x 3 output directories + README.md changes)

### 5.1 `.claude/` golden files (8 new files)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-dev-adr-automation/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-dev-adr-automation/SKILL.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-dev-adr-automation/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-dev-adr-automation/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-dev-adr-automation/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-dev-adr-automation/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-dev-adr-automation/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-dev-adr-automation/SKILL.md` |

### 5.2 `.agents/` golden files (8 new files)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.agents/skills/x-dev-adr-automation/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.agents/skills/x-dev-adr-automation/SKILL.md` |
| java-spring | `tests/golden/java-spring/.agents/skills/x-dev-adr-automation/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.agents/skills/x-dev-adr-automation/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.agents/skills/x-dev-adr-automation/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.agents/skills/x-dev-adr-automation/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.agents/skills/x-dev-adr-automation/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.agents/skills/x-dev-adr-automation/SKILL.md` |

### 5.3 `.github/` golden files (8 new files)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.github/skills/x-dev-adr-automation/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.github/skills/x-dev-adr-automation/SKILL.md` |
| java-spring | `tests/golden/java-spring/.github/skills/x-dev-adr-automation/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.github/skills/x-dev-adr-automation/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.github/skills/x-dev-adr-automation/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.github/skills/x-dev-adr-automation/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.github/skills/x-dev-adr-automation/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.github/skills/x-dev-adr-automation/SKILL.md` |

### 5.4 README.md updates (8 profiles x 2 outputs = up to 16 files)

Golden file README.md files for `.claude/README.md` (and potentially `.github/` equivalent) will need updated skill counts and skill table entries.

### 5.5 Golden File Update Strategy

```bash
# After creating the source templates, regenerate via pipeline:
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  npx ts-node src/cli.ts generate \
    --config "resources/config-templates/setup-config.${profile}.yaml" \
    --output "tests/golden/${profile}" \
    --resources resources
done
```

Alternatively, use the mechanical copy approach for the skill files only and re-run the pipeline for README.md updates.

---

## 6. Implementation Discovery: `.github/` Side Requires Code Change

**Critical finding from code review:** The implementation plan states "No source code modifications required" for `GithubSkillsAssembler`. This is **incorrect** for the `.github/` dual copy.

`SkillsAssembler.selectCoreSkills()` auto-discovers by scanning directories -- adding `x-dev-adr-automation/` to `resources/skills-templates/core/` is sufficient for `.claude/`.

However, `GithubSkillsAssembler` uses a **hardcoded** `SKILL_GROUPS` constant (see `src/assembler/github-skills-assembler.ts`, lines 23-55). To generate `.github/skills/x-dev-adr-automation/SKILL.md`, the implementation must:

1. Create `resources/github-skills-templates/{group}/x-dev-adr-automation.md` (the GitHub-format template)
2. Add `"x-dev-adr-automation"` to the appropriate group in `SKILL_GROUPS` (likely `"dev"` group)

The `CodexSkillsAssembler` mirrors from `.claude/skills/` and requires no changes.

This affects tests IT-3 and AT-1, which depend on the `.github/` output being generated.

---

## 7. Content Verification — Key Sections and Keywords

The content validation tests use `toContain()` for substring checks and `toMatch()` for regex patterns to avoid brittleness.

### 7.1 Frontmatter

| Keyword/Pattern | Purpose | Test |
|-----------------|---------|------|
| `name: x-dev-adr-automation` | Correct skill name | UT-1 |
| `description:` | Description field present | UT-1 |
| `allowed-tools:` | Tool permissions declared | UT-8 |
| `argument-hint:` | Usage hint present | UT-9 |

### 7.2 Required Sections

| Keyword/Pattern | Purpose | Test |
|-----------------|---------|------|
| `When to Use` | Decision tree for invocation | UT-2a |
| `Input Format` | Mini-ADR structure definition | UT-2b |
| `Output Format` | Full ADR structure definition | UT-2c |
| `Algorithm` | Step-by-step agent instructions | UT-2d |
| `Duplicate Detection` | Dedup rules | UT-2e |
| `Cross-Reference` | Linking rules | UT-2f |
| `Example` | Before/after conversion | UT-2g |

### 7.3 Business Logic Keywords

| Keyword/Pattern | Purpose | Test |
|-----------------|---------|------|
| `title` + `context` + `decision` + `rationale` | Mini-ADR 4 fields | UT-6 |
| `docs/adr/` | Target directory | UT-5 |
| `ADR-` + numbering pattern | Sequential naming | UT-5 |
| `story-ref` | Cross-reference frontmatter | UT-4 |
| `skip` or `warning` + `duplicate` | Dedup behavior | UT-3 |
| `README.md` or `index` | Index update | UT-11 |
| `Status` + `Consequences` | Full ADR output fields | UT-7, UT-10 |

---

## 8. Suggested Test Implementation Pattern

```typescript
import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const SKILL_PATH = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-dev-adr-automation/SKILL.md",
);

const content = fs.readFileSync(SKILL_PATH, "utf-8");

const REQUIRED_SECTIONS = [
  "When to Use",
  "Input Format",
  "Output Format",
  "Algorithm",
  "Duplicate Detection",
  "Cross-Reference",
];

const MINI_ADR_FIELDS = ["title", "context", "decision", "rationale"];

describe("x-dev-adr-automation SKILL.md — frontmatter", () => {
  // UT-1: YAML frontmatter
  it("skillMd_fileExists_hasValidYamlFrontmatter", () => {
    expect(content).toMatch(/^---\n/);
    expect(content).toContain("name: x-dev-adr-automation");
    expect(content).toContain("description:");
  });

  // UT-8: allowed-tools
  it("skillMd_frontmatter_containsAllowedTools", () => {
    expect(content).toContain("allowed-tools");
  });

  // UT-9: argument-hint
  it("skillMd_frontmatter_containsArgumentHint", () => {
    expect(content).toContain("argument-hint");
  });
});

describe("x-dev-adr-automation SKILL.md — required sections", () => {
  // UT-2: required sections
  it.each(
    REQUIRED_SECTIONS.map((s) => [s]),
  )("skillMd_containsSection_%s", (section) => {
    expect(content).toContain(section);
  });

  // UT-2g: Examples section
  it("skillMd_containsSection_Examples", () => {
    expect(content).toMatch(/[Ee]xample/);
  });
});

describe("x-dev-adr-automation SKILL.md — duplicate detection", () => {
  // UT-3
  it("skillMd_duplicateDetection_containsTitleSimilarityCheck", () => {
    expect(content).toMatch(/[Dd]uplicate/);
    expect(content).toMatch(/title/i);
    expect(content).toMatch(/skip|warning/i);
  });
});

describe("x-dev-adr-automation SKILL.md — cross-reference rules", () => {
  // UT-4
  it("skillMd_crossReference_containsStoryRefAndADRLinks", () => {
    expect(content).toContain("story-ref");
    expect(content).toMatch(/architecture plan|plan.*ADR.*link/i);
  });
});

describe("x-dev-adr-automation SKILL.md — sequential numbering", () => {
  // UT-5
  it("skillMd_algorithm_containsSequentialNumbering", () => {
    expect(content).toContain("docs/adr/");
    expect(content).toMatch(/ADR-\d{4}|ADR-NNNN|sequential/i);
  });
});

describe("x-dev-adr-automation SKILL.md — mini-ADR input format", () => {
  // UT-6
  it.each(
    MINI_ADR_FIELDS.map((f) => [f]),
  )("skillMd_inputFormat_containsField_%s", (field) => {
    expect(content).toMatch(new RegExp(field, "i"));
  });
});

describe("x-dev-adr-automation SKILL.md — example conversion", () => {
  // UT-7
  it("skillMd_examples_containsBeforeAfterConversion", () => {
    expect(content).toMatch(/[Ss]tatus/);
    expect(content).toMatch(/[Cc]onsequences/);
  });
});

describe("x-dev-adr-automation SKILL.md — output format", () => {
  // UT-10
  it("skillMd_outputFormat_containsADRFrontmatterFields", () => {
    expect(content).toMatch(/status/i);
    expect(content).toMatch(/date/i);
    expect(content).toMatch(/story-ref/);
  });
});

describe("x-dev-adr-automation SKILL.md — index update", () => {
  // UT-11
  it("skillMd_algorithm_containsIndexUpdateInstructions", () => {
    expect(content).toMatch(/README\.md|index/i);
    expect(content).toMatch(/docs\/adr/);
  });
});

describe("x-dev-adr-automation SKILL.md — global output policy", () => {
  // UT-12
  it("skillMd_containsGlobalOutputPolicy_englishOnly", () => {
    expect(content).toMatch(/English ONLY/i);
  });
});
```

---

## 9. TDD Execution Order

| Step | Action | Test State |
|------|--------|-----------|
| 1 | Write content validation tests (`tests/node/content/x-dev-adr-automation-content.test.ts`) | RED (SKILL.md does not exist yet) |
| 2 | Create `resources/skills-templates/core/x-dev-adr-automation/SKILL.md` | GREEN (content tests pass) |
| 3 | Create `resources/github-skills-templates/{group}/x-dev-adr-automation.md` | N/A (GitHub template) |
| 4 | Add entry to `SKILL_GROUPS` in `src/assembler/github-skills-assembler.ts` | N/A (code change for dual copy) |
| 5 | Regenerate golden files for all 8 profiles | N/A (golden files updated) |
| 6 | Run byte-for-byte integration tests | GREEN (golden file parity) |
| 7 | Run full test suite (`npx vitest run`) | GREEN (all existing + new tests pass) |

---

## 10. Backward Compatibility

- Adding a new core skill directory does NOT affect existing skills
- `SkillsAssembler.selectCoreSkills()` is additive — it returns all directories sorted alphabetically
- Golden file changes are limited to: (a) new skill files, (b) README.md skill count/table updates
- Existing tests continue to pass because no existing files are modified

---

## 11. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Golden file mismatch after adding skill | Regenerate all 8 profiles via pipeline; byte-for-byte tests catch drift |
| `.github/` copy missing (SKILL_GROUPS not updated) | IT-3 golden file test catches missing `.github/skills/x-dev-adr-automation/` directory |
| SKILL.md content tests too brittle | Use `toContain()` and `toMatch()` for semantic presence, not exact line matching |
| README.md skill count changes break golden files | Regeneration of golden files includes updated README.md |
| Implementation plan omission (no GitHub code change noted) | Section 6 documents the discovery; implementation must update `SKILL_GROUPS` |

---

## 12. Files Summary

### 12.1 New Files

| # | File | Description |
|---|------|-------------|
| 1 | `resources/skills-templates/core/x-dev-adr-automation/SKILL.md` | Claude Code skill template (source of truth, RULE-002) |
| 2 | `resources/github-skills-templates/{group}/x-dev-adr-automation.md` | GitHub Copilot skill template (RULE-001 dual copy) |
| 3 | `tests/node/content/x-dev-adr-automation-content.test.ts` | Content validation tests |

### 12.2 Modified Files

| # | File | Change |
|---|------|--------|
| 4 | `src/assembler/github-skills-assembler.ts` | Add `"x-dev-adr-automation"` to `SKILL_GROUPS.dev` array |

### 12.3 Golden Files Updated (~26+ files)

8 profiles x 3 output directories (`.claude/`, `.agents/`, `.github/`) + README.md updates per profile.

### 12.4 Existing Tests Covering This Story (unchanged)

| File | What It Covers |
|------|---------------|
| `tests/node/integration/byte-for-byte.test.ts` | Golden file parity for all 8 profiles |
| `tests/node/assembler/skills-assembler.test.ts` | Claude skill copy mechanism |
| `tests/node/assembler/codex-skills-assembler.test.ts` | Agents skill mirroring |
| `tests/node/assembler/github-skills-assembler.test.ts` | GitHub skill copy mechanism |

---

## 13. Test Count Summary

| Category | New Tests | Existing Tests |
|----------|-----------|----------------|
| Content validation (UT-1 through UT-12) | ~20 (including `it.each` expansions) | 0 |
| Golden file integration (IT-1 through IT-4) | 0 | 40 (8 profiles x 5 assertions) |
| Assembler unit tests | 0 | ~50 (across 3 assembler test files) |
| **Total** | **~20** | **~90** |

---

## 14. Verification Checklist

- [ ] `npx vitest run tests/node/content/x-dev-adr-automation-content.test.ts` — all content validation tests pass
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` — all 8 profiles pass
- [ ] `npx vitest run` — full suite passes
- [ ] Coverage remains >= 95% line, >= 90% branch
- [ ] No compiler/linter warnings
- [ ] `x-dev-adr-automation` appears in generated `.claude/skills/`, `.agents/skills/`, and `.github/skills/` directories
- [ ] Generated `.claude/README.md` includes `x-dev-adr-automation` in skill table with correct count
