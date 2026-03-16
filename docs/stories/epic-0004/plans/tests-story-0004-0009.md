# Test Plan — story-0004-0009

## Summary

This story adds a CLI documentation generator section to the `x-dev-lifecycle` SKILL.md template within the documentation phase (added by story-0004-0005). The generator instructs the AI agent to scan CLI command definitions and produce `docs/api/cli-reference.md`. No TypeScript source code changes. Testing relies on content validation of the 2 modified source-of-truth templates, dual copy consistency checks, and byte-for-byte golden file parity across all 8 profiles x 3 output directories (24 golden files).

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Content validation (Claude source) | Verify CLI doc generator section exists in `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | YES | `tests/node/content/x-dev-lifecycle-cli-generator-content.test.ts` |
| Content validation (GitHub source) | Verify CLI doc generator section exists in `resources/github-skills-templates/dev/x-dev-lifecycle.md` | YES | `tests/node/content/x-dev-lifecycle-cli-generator-content.test.ts` (same file, separate describe block) |
| Dual copy consistency | Verify both sources contain semantically identical CLI generator content | YES | `tests/node/content/x-dev-lifecycle-cli-generator-content.test.ts` |
| Structural preservation | Verify existing Phase structure and content unchanged | YES | `tests/node/content/x-dev-lifecycle-cli-generator-content.test.ts` |
| Golden file integration | Verify pipeline output matches updated golden files | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |
| Assembler unit tests | Verify copy logic works for x-dev-lifecycle | NO (existing) | `tests/node/assembler/skills-assembler.test.ts` |

---

## 2. Acceptance Tests (Outer Loop — Double-Loop TDD)

Acceptance tests map directly to the story's Gherkin acceptance criteria. They remain RED throughout the inner loop and turn GREEN when all related unit tests (content validations) complete.

### AT-1: CLI doc generator section present in lifecycle template

| Field | Value |
|-------|-------|
| ID | AT-1 |
| Gherkin | "DADO que o project identity define interfaces como ['cli'] QUANDO a fase de documentacao invoca o gerador CLI ENTAO o arquivo docs/api/cli-reference.md deve ser criado" |
| Test type | Content validation (integration-like) |
| Depends On | TASK-1 (Claude source modification) |
| Parallel | No (anchors the outer loop) |

**Validation:** Both source-of-truth templates contain a CLI Documentation Generator section with heading, interface condition, output path, and framework-specific scan instructions.

### AT-2: Dual copy consistency for CLI generator

| Field | Value |
|-------|-------|
| ID | AT-2 |
| Gherkin | Derived from RULE-001 (Dual Copy Consistency) |
| Test type | Dual copy consistency |
| Depends On | TASK-1 + TASK-2 |
| Parallel | No (requires both templates modified) |

**Validation:** Both templates contain semantically equivalent CLI generator content, with only expected path differences (`.claude/skills/` vs `.github/skills/`).

### AT-3: Golden file parity after template update

| Field | Value |
|-------|-------|
| ID | AT-3 |
| Gherkin | "Golden file tests validando output" (DoD Local) |
| Test type | Integration (golden file byte-for-byte) |
| Depends On | TASK-1 + TASK-2 + TASK-3 |
| Parallel | No (requires golden files updated) |

**Validation:** All 8 profiles pass byte-for-byte golden file comparison (existing `byte-for-byte.test.ts`).

---

## 3. New Tests — Content Validation

### 3.1 File: `tests/node/content/x-dev-lifecycle-cli-generator-content.test.ts`

This new test file validates the CLI documentation generator content added to both source-of-truth templates. It follows the established pattern in `tests/node/content/x-story-create-content.test.ts`.

---

### 3.2 Claude Source Template — CLI Generator Section Exists

**Source file under test:** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

#### TPP Level 1 — Degenerate Cases (section exists at all)

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 1 | UT-1 | `claudeSource_cliGenerator_containsSectionHeading` | A heading containing "CLI Documentation Generator" or "CLI Doc" exists | TASK-1 | Yes (with UT-2, UT-3) |
| 2 | UT-2 | `claudeSource_cliGenerator_containsInterfaceCondition` | The section references `"cli"` as the interface trigger (e.g., `interface: cli` or `interfaces.*cli`) | TASK-1 | Yes (with UT-1, UT-3) |
| 3 | UT-3 | `claudeSource_cliGenerator_containsOutputPath` | The section specifies output path `docs/api/cli-reference.md` | TASK-1 | Yes (with UT-1, UT-2) |

#### TPP Level 2 — Unconditional Paths (required subsections)

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 4 | UT-4 | `claudeSource_cliGenerator_containsCLIReferenceTitle` | Output format includes `# CLI Reference` heading | TASK-1 | Yes (with UT-5 through UT-9) |
| 5 | UT-5 | `claudeSource_cliGenerator_containsQuickStartSection` | Output format includes `## Quick Start` section | TASK-1 | Yes (with UT-4, UT-6 through UT-9) |
| 6 | UT-6 | `claudeSource_cliGenerator_containsGlobalFlagsSection` | Output format includes `## Global Flags` section | TASK-1 | Yes (with UT-4, UT-5, UT-7 through UT-9) |
| 7 | UT-7 | `claudeSource_cliGenerator_containsCommandSection` | Output format includes `## Command: {name}` per-command section pattern | TASK-1 | Yes (with UT-4 through UT-6, UT-8, UT-9) |
| 8 | UT-8 | `claudeSource_cliGenerator_containsExitCodesSection` | Output format includes `## Exit Codes` section | TASK-1 | Yes (with UT-4 through UT-7, UT-9) |
| 9 | UT-9 | `claudeSource_cliGenerator_containsSubcommandSection` | Output format includes `### Subcommand:` nested section pattern | TASK-1 | Yes (with UT-4 through UT-8) |

#### TPP Level 3 — Simple Conditions (per-command detail requirements)

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 10 | UT-10 | `claudeSource_cliGenerator_containsFlagsTable` | Command sections include a flags table with columns (Flag, Type, Default, Description) | TASK-1 | Yes (with UT-11 through UT-13) |
| 11 | UT-11 | `claudeSource_cliGenerator_containsArgsTable` | Command sections include an arguments table with columns (Argument, Type, Required, Description) | TASK-1 | Yes (with UT-10, UT-12, UT-13) |
| 12 | UT-12 | `claudeSource_cliGenerator_containsUsageLine` | Command sections include a usage line pattern (e.g., `$ {tool-name}`) | TASK-1 | Yes (with UT-10, UT-11, UT-13) |
| 13 | UT-13 | `claudeSource_cliGenerator_containsExamplePerCommand` | Command sections require at least 1 example in code block | TASK-1 | Yes (with UT-10 through UT-12) |

#### TPP Level 4 — Complex Conditions (framework-specific scan patterns)

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 14 | UT-14 | `claudeSource_cliGenerator_containsCommanderPattern` | Scan instructions reference Commander.js patterns (`.command()`, `.option()`) | TASK-1 | Yes (with UT-15 through UT-17) |
| 15 | UT-15 | `claudeSource_cliGenerator_containsClickPattern` | Scan instructions reference Click patterns (`@click.command`, `@click.option`) | TASK-1 | Yes (with UT-14, UT-16, UT-17) |
| 16 | UT-16 | `claudeSource_cliGenerator_containsCobraPattern` | Scan instructions reference Cobra patterns (`cobra.Command`) | TASK-1 | Yes (with UT-14, UT-15, UT-17) |
| 17 | UT-17 | `claudeSource_cliGenerator_containsClapPattern` | Scan instructions reference Clap patterns (`#[derive(Parser)]` or `#[arg()]`) | TASK-1 | Yes (with UT-14 through UT-16) |

#### TPP Level 5 — Iteration (quick start requirements)

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 18 | UT-18 | `claudeSource_cliGenerator_quickStartRequiresMultipleExamples` | Quick Start section requires at least 2 usage examples | TASK-1 | Yes (with UT-19) |
| 19 | UT-19 | `claudeSource_cliGenerator_exitCodesRequiresTable` | Exit Codes section specifies a table format with Code and Meaning columns | TASK-1 | Yes (with UT-18) |

#### TPP Level 6 — Edge Cases (skip behavior, backward compatibility)

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 20 | UT-20 | `claudeSource_cliGenerator_skipWhenNoCLIInterface` | Instructions specify to skip silently when `interfaces` does not contain `"cli"` | TASK-1 | Yes (with UT-21 through UT-23) |
| 21 | UT-21 | `claudeSource_cliGenerator_containsFrameworkPlaceholder` | Uses `{{FRAMEWORK}}` placeholder or equivalent for framework-agnostic scan | TASK-1 | Yes (with UT-20, UT-22, UT-23) |
| 22 | UT-22 | `claudeSource_cliGenerator_noWarningOnSkip` | Skip behavior is silent (no warning, no output) | TASK-1 | Yes (with UT-20, UT-21, UT-23) |
| 23 | UT-23 | `claudeSource_cliGenerator_sectionPositionedInDocPhase` | CLI generator section appears within the documentation phase area of the template (after Phase 2, before or within Phase 3 context) | TASK-1 | No (requires understanding of phase structure) |

**IT-1: Claude source template integration** (positioned after UT-1 through UT-23)

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 24 | IT-1 | `claudeSource_cliGenerator_allRequiredSectionsPresent` | Composite check: all 6 output format sections (CLI Reference, Quick Start, Global Flags, Command, Subcommand, Exit Codes) present in a single read of the file | TASK-1 | No |

---

### 3.3 Claude Source Template — Structural Preservation

These tests ensure the CLI generator addition does not break existing template structure.

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 25 | UT-24 | `claudeSource_preservesPhaseCount` | "9 phases (0-8)" wording is preserved | TASK-1 | Yes (with UT-25 through UT-30) |
| 26 | UT-25 | `claudeSource_preservesCriticalExecutionRule` | "NEVER stop before Phase 8" rule is preserved | TASK-1 | Yes (with UT-24, UT-26 through UT-30) |
| 27 | UT-26 | `claudeSource_preservesPhase2TDDHeading` | Phase 2 heading with "TDD" is preserved | TASK-1 | Yes (with UT-24, UT-25, UT-27 through UT-30) |
| 28 | UT-27 | `claudeSource_preservesG1G7Fallback` | G1-G7 Fallback section is preserved | TASK-1 | Yes (with UT-24 through UT-26, UT-28 through UT-30) |
| 29 | UT-28 | `claudeSource_preservesPhase1ArchitectSubagent` | Phase 1 "Senior Architect" subagent prompt preserved | TASK-1 | Yes (with UT-24 through UT-27, UT-29, UT-30) |
| 30 | UT-29 | `claudeSource_preservesIntegrationNotes` | Integration Notes section preserved | TASK-1 | Yes (with UT-24 through UT-28, UT-30) |
| 31 | UT-30 | `claudeSource_preservesAllPlaceholderTokens` | All `{{PLACEHOLDER}}` tokens remain (PROJECT_NAME, LANGUAGE, COMPILE_COMMAND, TEST_COMMAND, COVERAGE_COMMAND, LANGUAGE_VERSION) | TASK-1 | Yes (with UT-24 through UT-29) |

---

### 3.4 GitHub Source Template — CLI Generator Section

**Source file under test:** `resources/github-skills-templates/dev/x-dev-lifecycle.md`

Same 23 CLI generator content tests as Section 3.2, but targeting the GitHub copy. Each test name prefixed with `githubSource_` instead of `claudeSource_`.

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 32-54 | UT-31 to UT-53 | Same as UT-1 through UT-23 but with `githubSource_` prefix | Same validations against GitHub template | TASK-2 | Same parallelism as Claude counterparts |

**Key differences from Claude tests:**
- UT-21 (framework placeholder): may reference `.github/skills/` paths instead of `skills/`
- UT-23 (section position): validates position within GitHub template's documentation phase structure

**IT-2: GitHub source template integration** (positioned after UT-31 through UT-53)

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 55 | IT-2 | `githubSource_cliGenerator_allRequiredSectionsPresent` | Composite check: all 6 output format sections present in GitHub template | TASK-2 | No |

---

### 3.5 GitHub Source Template — Structural Preservation

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 56-62 | UT-54 to UT-60 | Same as UT-24 through UT-30 but with `githubSource_` prefix | Same structural preservation validations against GitHub template | TASK-2 | Same parallelism as Claude counterparts |

**Additional GitHub-specific preservation:**

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 63 | UT-61 | `githubSource_preservesDetailedReferencesSection` | "Detailed References" section with `.github/skills/` paths preserved | TASK-2 | Yes |
| 64 | UT-62 | `githubSource_preservesDistinctPhase7CompletionMessage` | "Phase 7/7 completed. Lifecycle complete." preserved | TASK-2 | Yes |

---

### 3.6 Dual Copy Consistency (RULE-001)

| # | ID | Test Name | What It Validates | Depends On | Parallel |
|---|-----|-----------|-------------------|------------|----------|
| 65 | DC-1 | `dualCopy_bothContainCLIGeneratorHeading` | Both sources contain a CLI Documentation Generator heading | TASK-1 + TASK-2 | Yes (with DC-2 through DC-10) |
| 66 | DC-2 | `dualCopy_bothContainInterfaceCLICondition` | Both sources reference `"cli"` as the interface trigger | TASK-1 + TASK-2 | Yes |
| 67 | DC-3 | `dualCopy_bothContainOutputPath` | Both sources specify `docs/api/cli-reference.md` as output | TASK-1 + TASK-2 | Yes |
| 68 | DC-4 | `dualCopy_bothContainQuickStartSection` | Both sources include Quick Start in the output format | TASK-1 + TASK-2 | Yes |
| 69 | DC-5 | `dualCopy_bothContainGlobalFlagsSection` | Both sources include Global Flags in the output format | TASK-1 + TASK-2 | Yes |
| 70 | DC-6 | `dualCopy_bothContainCommandSectionPattern` | Both sources include Command section pattern | TASK-1 + TASK-2 | Yes |
| 71 | DC-7 | `dualCopy_bothContainSubcommandSectionPattern` | Both sources include Subcommand section pattern | TASK-1 + TASK-2 | Yes |
| 72 | DC-8 | `dualCopy_bothContainExitCodesSection` | Both sources include Exit Codes section | TASK-1 + TASK-2 | Yes |
| 73 | DC-9 | `dualCopy_bothContainSkipBehavior` | Both sources describe silent skip when no CLI interface | TASK-1 + TASK-2 | Yes |
| 74 | DC-10 | `dualCopy_bothContainFourFrameworkPatterns` | Both sources reference scan patterns for Commander.js, Click, Cobra, and Clap | TASK-1 + TASK-2 | Yes |
| 75 | DC-11 | `dualCopy_bothContainFlagsTableColumns` | Both sources specify the same flags table columns (Flag, Type, Default, Description) | TASK-1 + TASK-2 | Yes |
| 76 | DC-12 | `dualCopy_phaseCount_identical` | Both sources still declare "9 phases (0-8)" | TASK-1 + TASK-2 | Yes |
| 77 | DC-13 | `dualCopy_pathDifferences_onlyExpected` | The only differences between the two templates in the CLI generator section are expected path references (`skills/` vs `.github/skills/`) and structural differences documented in RULE-001 | TASK-1 + TASK-2 | No |

---

## 4. Existing Tests — No Changes Needed

### 4.1 Golden File Integration Tests

- **File:** `tests/node/integration/byte-for-byte.test.ts`
- **What it validates:** Pipeline output matches golden files byte-for-byte for all 8 profiles
- **How it covers this story:** After updating both source templates and regenerating 24 golden files, the pipeline will produce output identical to the updated golden files
- **Expected result:** All 8 profiles pass (40 test assertions: 5 per profile)
- **Test logic unchanged:** The test infrastructure is generic and works with any content

### 4.2 Assembler Unit Tests

- **File:** `tests/node/assembler/skills-assembler.test.ts` — Tests `SkillsAssembler` copy logic for `.claude/` output
- **File:** `tests/node/assembler/codex-skills-assembler.test.ts` — Tests `CodexSkillsAssembler` mirror logic for `.agents/` output
- **File:** `tests/node/assembler/github-skills-assembler.test.ts` — Tests `GithubSkillsAssembler` copy logic for `.github/` output
- **Impact:** None — assembler logic unchanged; these tests continue to validate the copy mechanism works

---

## 5. Golden Files Requiring Update

**Total: 24 golden files** (8 profiles x 3 output directories)

### 5.1 `.claude/` golden files (8 files, identical to Claude source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-dev-lifecycle/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/SKILL.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-dev-lifecycle/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/SKILL.md` |

### 5.2 `.agents/` golden files (8 files, identical to Claude source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.agents/skills/x-dev-lifecycle/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.agents/skills/x-dev-lifecycle/SKILL.md` |
| java-spring | `tests/golden/java-spring/.agents/skills/x-dev-lifecycle/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.agents/skills/x-dev-lifecycle/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.agents/skills/x-dev-lifecycle/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.agents/skills/x-dev-lifecycle/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.agents/skills/x-dev-lifecycle/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.agents/skills/x-dev-lifecycle/SKILL.md` |

### 5.3 `.github/` golden files (8 files, identical to GitHub source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.github/skills/x-dev-lifecycle/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.github/skills/x-dev-lifecycle/SKILL.md` |
| java-spring | `tests/golden/java-spring/.github/skills/x-dev-lifecycle/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.github/skills/x-dev-lifecycle/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.github/skills/x-dev-lifecycle/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.github/skills/x-dev-lifecycle/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.github/skills/x-dev-lifecycle/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.github/skills/x-dev-lifecycle/SKILL.md` |

### 5.4 Golden File Update Strategy

```bash
# After editing the source templates:
CLAUDE_SRC="resources/skills-templates/core/x-dev-lifecycle/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/dev/x-dev-lifecycle.md"
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_SRC" "tests/golden/$profile/.claude/skills/x-dev-lifecycle/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/$profile/.agents/skills/x-dev-lifecycle/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/$profile/.github/skills/x-dev-lifecycle/SKILL.md"
done
```

---

## 6. Content Verification — Key Sections That Must Appear

The following sections and keywords MUST be present in the updated templates. Content tests use `toContain()` or `toMatch()` for substring/regex matching (not brittle exact line matching).

### 6.1 CLI Generator Section — Heading and Trigger

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `CLI Documentation Generator` or `CLI Doc` | Section heading |
| `interface: cli` or `interfaces.*cli` | Interface trigger condition |
| `docs/api/cli-reference.md` | Output path |

### 6.2 Output Format Sections

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `# CLI Reference` | Title heading in output |
| `## Quick Start` | Quick start section |
| `## Global Flags` | Global flags section |
| `## Command:` or `Command: {name}` | Per-command section |
| `### Subcommand:` or `Subcommand: {parent} {child}` | Nested subcommand sections |
| `## Exit Codes` | Exit codes section |

### 6.3 Per-Command Detail Requirements

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `Flag` + `Type` + `Default` + `Description` | Flags table columns |
| `Argument` + `Type` + `Required` + `Description` | Arguments table columns |
| `$ {tool-name}` or usage line pattern | Usage line format |
| `example` or `code block` (case-insensitive) | Example requirement |

### 6.4 Framework-Specific Scan Patterns

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `.command()` or `.option()` or `Commander` | Commander.js scan pattern |
| `@click.command` or `@click.option` or `Click` | Click scan pattern |
| `cobra.Command` or `Cobra` | Cobra scan pattern |
| `#[derive(Parser)]` or `#[arg()]` or `Clap` | Clap scan pattern |

### 6.5 Skip Behavior

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `skip` + `silently` or `no output` or `no warning` | Silent skip when no CLI interface |
| `does NOT contain.*cli` or `not.*cli` | Negative condition |

---

## 7. Backward Compatibility Verification

These tests ensure no existing functionality is removed:

### 7.1 Phase Structure Preservation

| Verification | How Tested |
|--------------|-----------|
| 9 phases (0-8) count preserved | UT-24/UT-54: regex match `9 phases.*0-8` |
| "NEVER stop before Phase 8" preserved | UT-25/UT-55: exact substring match |
| Phase 2 TDD heading preserved | UT-26/UT-56: substring match |
| G1-G7 Fallback preserved | UT-27/UT-57: substring match |
| Phase 1 architect subagent preserved | UT-28/UT-58: "Senior Architect" text preserved |
| Integration Notes preserved | UT-29/UT-59: substring match |

### 7.2 Placeholder Tokens

| Token | How Tested |
|-------|-----------|
| `{{PROJECT_NAME}}` | UT-30/UT-60: substring match |
| `{{LANGUAGE}}` | UT-30/UT-60: substring match |
| `{{LANGUAGE_VERSION}}` | UT-30/UT-60: substring match |
| `{{COMPILE_COMMAND}}` | UT-30/UT-60: substring match |
| `{{TEST_COMMAND}}` | UT-30/UT-60: substring match |
| `{{COVERAGE_COMMAND}}` | UT-30/UT-60: substring match |

---

## 8. Suggested Test Implementation Pattern

```typescript
import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-dev-lifecycle/SKILL.md",
);
const GITHUB_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/github-skills-templates/dev/x-dev-lifecycle.md",
);

const claudeContent = fs.readFileSync(CLAUDE_SOURCE, "utf-8");
const githubContent = fs.readFileSync(GITHUB_SOURCE, "utf-8");

const OUTPUT_FORMAT_SECTIONS = [
  "CLI Reference",
  "Quick Start",
  "Global Flags",
  "Exit Codes",
];

const FRAMEWORK_PATTERNS = [
  ["Commander", ".command()"],
  ["Click", "@click.command"],
  ["Cobra", "cobra.Command"],
  ["Clap", "Clap"],
];

const PLACEHOLDER_TOKENS = [
  "{{PROJECT_NAME}}",
  "{{LANGUAGE}}",
  "{{LANGUAGE_VERSION}}",
  "{{COMPILE_COMMAND}}",
  "{{TEST_COMMAND}}",
  "{{COVERAGE_COMMAND}}",
];

describe("x-dev-lifecycle CLI generator content validation", () => {
  describe("Claude source — CLI generator section exists", () => {
    // UT-1 through UT-3: Degenerate (section exists)
  });

  describe("Claude source — output format subsections", () => {
    // UT-4 through UT-9: Unconditional (required sections)
  });

  describe("Claude source — per-command detail requirements", () => {
    // UT-10 through UT-13: Simple conditions
  });

  describe("Claude source — framework-specific scan patterns", () => {
    // UT-14 through UT-17: Complex conditions
  });

  describe("Claude source — quick start and exit codes", () => {
    // UT-18 through UT-19: Iteration
  });

  describe("Claude source — edge cases", () => {
    // UT-20 through UT-23: Edge cases
  });

  describe("Claude source — structural preservation", () => {
    // UT-24 through UT-30: Backward compatibility
  });

  describe("GitHub source — CLI generator section exists", () => {
    // UT-31 through UT-53 (mirror of Claude UT-1 through UT-23)
  });

  describe("GitHub source — structural preservation", () => {
    // UT-54 through UT-62: Backward compatibility + GitHub-specific
  });

  describe("dual copy consistency (RULE-001)", () => {
    // DC-1 through DC-13
  });
});
```

---

## 9. TDD Execution Order

Following test-first approach:

| Step | Action | Test State |
|------|--------|-----------|
| 1 | Write content validation tests (`tests/node/content/x-dev-lifecycle-cli-generator-content.test.ts`) with all 77 test cases | RED (tests fail because source files not yet modified) |
| 2 | Edit Claude source template (`resources/skills-templates/core/x-dev-lifecycle/SKILL.md`) — add CLI generator section | Partial GREEN (Claude tests pass; GitHub tests and dual copy tests still RED) |
| 3 | Edit GitHub source template (`resources/github-skills-templates/dev/x-dev-lifecycle.md`) — mirror CLI generator section | GREEN (all content + consistency tests pass) |
| 4 | Update deployed copy (`.claude/skills/x-dev-lifecycle/SKILL.md`) to match Claude source | N/A (deployed copy updated) |
| 5 | Copy sources to 24 golden files (script from Section 5.4) | N/A (golden files updated) |
| 6 | Run byte-for-byte integration tests | GREEN (golden file parity confirmed) |
| 7 | Run full test suite (`npx vitest run`) | GREEN (all existing tests pass, plus 77 new tests) |

---

## 10. Verification Checklist

- [ ] `npx vitest run tests/node/content/x-dev-lifecycle-cli-generator-content.test.ts` — all 77 content validation tests pass
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` — all 8 profiles pass (40 assertions)
- [ ] `npx vitest run` — full suite passes
- [ ] Coverage remains >= 95% line, >= 90% branch (no TypeScript code changes, so coverage unaffected)
- [ ] No compiler/linter warnings introduced
- [ ] Deployed copy (`.claude/skills/x-dev-lifecycle/SKILL.md`) matches Claude source template exactly

---

## 11. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| story-0004-0005 not yet merged | Verify documentation phase skeleton exists in `x-dev-lifecycle/SKILL.md` before starting; if not, this story is blocked |
| Golden file mismatch after source edit | Mechanical copy script (Section 5.4) eliminates drift; byte-for-byte tests catch any mismatch immediately |
| Content test too brittle (exact string matching) | Use `toContain()` for substring checks and `toMatch()` for regex patterns; test semantic presence, not formatting |
| Dual copy inconsistency (RULE-001) | Dedicated consistency tests (DC-1 through DC-13) verify both copies have equivalent CLI generator content |
| CLI generator section breaks existing phase numbering | Structural preservation tests (UT-24 through UT-30 and UT-54 through UT-60) verify phase count and structure unchanged |
| Framework scan pattern incomplete | Tests UT-14 through UT-17 validate all 4 framework patterns (Commander.js, Click, Cobra, Clap) are documented |
| Deployed copy diverges from source | Verification checklist item: deployed copy must match Claude source template exactly |

---

## 12. Task-to-Test Mapping

| Task | Description | Tests Covering |
|------|-------------|----------------|
| TASK-1 | Add CLI generator section to Claude source template | UT-1 through UT-23, IT-1, UT-24 through UT-30 |
| TASK-2 | Mirror changes to GitHub source template (RULE-001) | UT-31 through UT-53, IT-2, UT-54 through UT-62 |
| TASK-3 | Regenerate 24 golden files | AT-3 (via existing `byte-for-byte.test.ts`) |

---

## 13. Files Summary

### 13.1 Files Modified (Source Templates + Deployed Copy)

| # | File | Description |
|---|------|-------------|
| 1 | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Claude Code source of truth (RULE-002) |
| 2 | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | GitHub Copilot source of truth (RULE-002) |
| 3 | `.claude/skills/x-dev-lifecycle/SKILL.md` | Deployed copy for this project |

### 13.2 Golden Files Updated (24 files)

8 profiles x 3 output directories (`.claude/`, `.agents/`, `.github/`) = 24 golden files.

### 13.3 New Test File (1 file)

| File | Test Count |
|------|-----------|
| `tests/node/content/x-dev-lifecycle-cli-generator-content.test.ts` | 77 |

### 13.4 Existing Test Files (unchanged, covering this story)

| File | Test Count | Coverage |
|------|-----------|----------|
| `tests/node/integration/byte-for-byte.test.ts` | 40 (8 profiles x 5 assertions) | Golden file parity |
| `tests/node/assembler/skills-assembler.test.ts` | ~20 | Claude copy mechanism |
| `tests/node/assembler/codex-skills-assembler.test.ts` | ~15 | Agents copy mechanism |
| `tests/node/assembler/github-skills-assembler.test.ts` | ~15 | GitHub copy mechanism |

---

## 14. Test Count Summary

| Category | New Tests | Existing Tests |
|----------|-----------|----------------|
| Content validation — Claude (UT-1 to UT-23, IT-1) | 24 | 0 |
| Structural preservation — Claude (UT-24 to UT-30) | 7 | 0 |
| Content validation — GitHub (UT-31 to UT-53, IT-2) | 24 | 0 |
| Structural preservation — GitHub (UT-54 to UT-62) | 9 | 0 |
| Dual copy consistency (DC-1 to DC-13) | 13 | 0 |
| Golden file integration | 0 | 40 (8 profiles x 5 assertions) |
| Assembler unit tests | 0 | ~50 (across 3 assembler test files) |
| **Total** | **77** | **~90** |
