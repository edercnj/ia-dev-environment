# Test Plan -- STORY-0003-0006: Agents TDD Workflows for Developer, QA and Tech Lead

## Summary

- Affected source templates: 6 files (3 in `agents-templates/`, 3 in `github-agents-templates/`)
- Affected generated agents: 3 files (`.claude/agents/`)
- Affected golden files: 34 files (17 `.claude/agents/` + 17 `.github/agents/`)
- Total test methods: 42
- Categories: Golden File Integration (8), Content Validation -- typescript-developer (5), Content Validation -- qa-engineer (5), Content Validation -- tech-lead (7), Backward Compatibility (9), Dual Copy Consistency (8)
- Coverage targets: >= 95% line, >= 90% branch
- No new source code modules -- this is a content-only change verified by existing byte-for-byte infrastructure plus new content assertions

---

## 1. Test File Locations and Naming

### Existing (modified via golden file updates)

**Path:** `tests/node/integration/byte-for-byte.test.ts`

**Rationale:** Golden files for all 8 profiles must be updated to include the new TDD sections in all 3 agents. The existing byte-for-byte test suite automatically validates that pipeline output matches golden files. No code changes to this file -- only golden file updates.

### New

**Path:** `tests/node/content/agents-tdd-sections.test.ts`

**Rationale:** Content validation tests that verify the structural integrity of TDD additions to all 3 agent templates. These are unit-level tests that read the source template files and assert section presence, item counts, ordering, and key phrases. Separated from byte-for-byte tests because they validate semantic content, not binary equality.

**Naming convention:** `[agentUnderTest]_[scenario]_[expectedBehavior]` per Rule 05.

---

## 2. Test Infrastructure

### 2.1 Source File Paths

```typescript
import { resolve, dirname } from "node:path";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, it, expect, beforeAll } from "vitest";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const RESOURCES_DIR = resolve(__dirname, "../../../resources");

const TYPESCRIPT_DEV_CLAUDE = resolve(
  RESOURCES_DIR, "agents-templates", "developers", "typescript-developer.md",
);
const TYPESCRIPT_DEV_GITHUB = resolve(
  RESOURCES_DIR, "github-agents-templates", "developers", "typescript-developer.md",
);
const QA_ENGINEER_CLAUDE = resolve(
  RESOURCES_DIR, "agents-templates", "core", "qa-engineer.md",
);
const QA_ENGINEER_GITHUB = resolve(
  RESOURCES_DIR, "github-agents-templates", "core", "qa-engineer.md",
);
const TECH_LEAD_CLAUDE = resolve(
  RESOURCES_DIR, "agents-templates", "core", "tech-lead.md",
);
const TECH_LEAD_GITHUB = resolve(
  RESOURCES_DIR, "github-agents-templates", "core", "tech-lead.md",
);
```

### 2.2 Content Loading

```typescript
let tsDevClaude: string;
let tsDevGithub: string;
let qaClaudeCopy: string;
let qaGithubCopy: string;
let tlClaudeCopy: string;
let tlGithubCopy: string;

beforeAll(() => {
  tsDevClaude = readFileSync(TYPESCRIPT_DEV_CLAUDE, "utf-8");
  tsDevGithub = readFileSync(TYPESCRIPT_DEV_GITHUB, "utf-8");
  qaClaudeCopy = readFileSync(QA_ENGINEER_CLAUDE, "utf-8");
  qaGithubCopy = readFileSync(QA_ENGINEER_GITHUB, "utf-8");
  tlClaudeCopy = readFileSync(TECH_LEAD_CLAUDE, "utf-8");
  tlGithubCopy = readFileSync(TECH_LEAD_GITHUB, "utf-8");
});
```

### 2.3 Golden File Paths

The pipeline copies agent templates to two output format locations per profile:

1. `{profile}/.claude/agents/{agent-name}.md` (from `agents-templates/`)
2. `{profile}/.github/agents/{agent-name}.agent.md` (from `github-agents-templates/`)

**qa-engineer** and **tech-lead** are core agents -- present in all 8 profiles:
- `go-gin`, `java-quarkus`, `java-spring`, `kotlin-ktor`, `python-click-cli`, `python-fastapi`, `rust-axum`, `typescript-nestjs`

**typescript-developer** is a language-specific developer agent -- present only in `typescript-nestjs`.

Golden files reside at `tests/golden/{profile}/` for all 8 profiles defined in `tests/helpers/integration-constants.ts`.

---

## 3. Test Groups

### Group 1: Golden File Integration (8 tests -- existing suite)

The existing `byte-for-byte.test.ts` runs `describe.sequential.each` over all 8 `CONFIG_PROFILES`. Each profile runs 5 assertions (pipeline success, golden match, no missing files, no extra files, total files > 0). The golden files must be regenerated to include the new TDD sections across all 3 agents.

**Action required:** Update all 34 golden agent files:
- 8 profiles x `qa-engineer.md` in `.claude/agents/` = 8 files
- 8 profiles x `qa-engineer.agent.md` in `.github/agents/` = 8 files
- 8 profiles x `tech-lead.md` in `.claude/agents/` = 8 files
- 8 profiles x `tech-lead.agent.md` in `.github/agents/` = 8 files
- 1 profile x `typescript-developer.md` in `.claude/agents/` = 1 file (typescript-nestjs only)
- 1 profile x `typescript-developer.agent.md` in `.github/agents/` = 1 file (typescript-nestjs only)

| # | Test Name (existing) | Profile | Assertion |
|---|---------------------|---------|-----------|
| 1 | `pipelineMatchesGoldenFiles_go-gin` | go-gin | `verifyOutput()` returns `success: true` |
| 2 | `pipelineMatchesGoldenFiles_java-quarkus` | java-quarkus | `verifyOutput()` returns `success: true` |
| 3 | `pipelineMatchesGoldenFiles_java-spring` | java-spring | `verifyOutput()` returns `success: true` |
| 4 | `pipelineMatchesGoldenFiles_kotlin-ktor` | kotlin-ktor | `verifyOutput()` returns `success: true` |
| 5 | `pipelineMatchesGoldenFiles_python-click-cli` | python-click-cli | `verifyOutput()` returns `success: true` |
| 6 | `pipelineMatchesGoldenFiles_python-fastapi` | python-fastapi | `verifyOutput()` returns `success: true` |
| 7 | `pipelineMatchesGoldenFiles_rust-axum` | rust-axum | `verifyOutput()` returns `success: true` |
| 8 | `pipelineMatchesGoldenFiles_typescript-nestjs` | typescript-nestjs | `verifyOutput()` returns `success: true` |

**Verification logic:** `verifyOutput()` in `src/verifier.ts` compares two directory trees byte-for-byte using `Buffer.equals()`. Any difference in the golden agent files will produce a `FileDiff` mismatch and fail the test. The `formatVerificationFailures()` helper in `tests/helpers/integration-constants.ts` renders up to 500 chars of unified diff for debugging.

#### Execution

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

---

### Group 2: Content Validation -- typescript-developer (5 tests)

Verify the `typescript-developer.md` template contains the TDD Workflow section and reordered responsibilities.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 9 | `typescriptDeveloper_tddWorkflowSection_containsH2Header` | `content` contains `## TDD Workflow` |
| 10 | `typescriptDeveloper_tddWorkflow_containsWriteTestFirst` | `content` contains `write the test FIRST` (case-insensitive) |
| 11 | `typescriptDeveloper_tddWorkflow_containsRedGreenRefactorCycle` | `content` contains `**RED**`, `**GREEN**`, `**REFACTOR**` phase markers |
| 12 | `typescriptDeveloper_responsibilities_testBeforeImplement` | In the Responsibilities section, test-related item appears before implementation item |
| 13 | `typescriptDeveloper_tddWorkflow_containsCommitAfterCycle` | `content` contains reference to committing after each Red-Green-Refactor cycle |

#### Assertions Pattern

```typescript
// Test 9
expect(tsDevClaude).toContain("## TDD Workflow");

// Test 10
expect(tsDevClaude.toLowerCase()).toContain("write the test first");

// Test 11
expect(tsDevClaude).toContain("**RED**");
expect(tsDevClaude).toContain("**GREEN**");
expect(tsDevClaude).toContain("**REFACTOR**");

// Test 12 -- verify test-first ordering in responsibilities
const responsibilitiesStart = tsDevClaude.indexOf("## Responsibilities");
const responsibilitiesEnd = tsDevClaude.indexOf("\n## ", responsibilitiesStart + 1);
const responsibilitiesSection = tsDevClaude.slice(
  responsibilitiesStart,
  responsibilitiesEnd > -1 ? responsibilitiesEnd : undefined,
);
const testIndex = responsibilitiesSection.search(/failing tests?\s+FIRST|write.*test/i);
const implementIndex = responsibilitiesSection.search(/implement.*minimum|implement.*code/i);
expect(testIndex).toBeGreaterThan(-1);
expect(implementIndex).toBeGreaterThan(-1);
expect(testIndex).toBeLessThan(implementIndex);

// Test 13
expect(tsDevClaude).toMatch(/commit.*after.*each.*complete.*Red-Green-Refactor/i);
```

---

### Group 3: Content Validation -- qa-engineer (5 tests)

Verify the `qa-engineer.md` template contains the TDD Compliance category with 4+ items and that the total checklist is now 28+ items.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 14 | `qaEngineer_tddCompliance_categoryExists` | `content` contains `TDD Compliance` as a category header |
| 15 | `qaEngineer_tddCompliance_containsTestFirstPattern` | `content` contains `test-first pattern` text |
| 16 | `qaEngineer_tddCompliance_containsRefactoringCommits` | `content` contains reference to refactoring commits after green |
| 17 | `qaEngineer_tddCompliance_containsAtLeast4Items` | The TDD Compliance section has at least 4 numbered checklist items |
| 18 | `qaEngineer_totalChecklist_atLeast28Items` | Total numbered checklist items across the entire file >= 28 |

#### Assertions Pattern

```typescript
// Test 14
expect(qaClaudeCopy).toMatch(/###?\s+TDD Compliance/);

// Test 15
expect(qaClaudeCopy.toLowerCase()).toContain("test-first pattern");

// Test 16
expect(qaClaudeCopy).toMatch(/refactoring\s+commit/i);

// Test 17 -- extract TDD Compliance section and count items
const tddStart = qaClaudeCopy.indexOf("TDD Compliance");
const tddEnd = qaClaudeCopy.indexOf("\n### ", tddStart + 1);
const tddSection = qaClaudeCopy.slice(tddStart, tddEnd > -1 ? tddEnd : undefined);
const tddItems = tddSection.match(/^\s*\d+\.\s/gm) || [];
expect(tddItems.length).toBeGreaterThanOrEqual(4);

// Test 18 -- count all numbered items in the full checklist
const allItems = qaClaudeCopy.match(/^\s*\d+\.\s/gm) || [];
expect(allItems.length).toBeGreaterThanOrEqual(28);
```

---

### Group 4: Content Validation -- tech-lead (7 tests)

Verify the `tech-lead.md` template contains the TDD Process category with 4+ items and that the total checklist is now 45+ items.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 19 | `techLead_tddProcess_categoryExists` | `content` contains `TDD Process` as a category header |
| 20 | `techLead_tddProcess_containsRedGreenRefactorProgression` | `content` contains `Red-Green-Refactor progression` text |
| 21 | `techLead_tddProcess_containsDoubleLoopTdd` | `content` contains `Double-Loop TDD` text |
| 22 | `techLead_tddProcess_containsTppOrdering` | `content` contains `TPP ordering` or `Transformation Priority Premise` text |
| 23 | `techLead_tddProcess_containsAtomicCommits` | `content` contains reference to atomic commits |
| 24 | `techLead_tddProcess_containsAtLeast4Items` | The TDD Process section has at least 4 numbered checklist items |
| 25 | `techLead_totalChecklist_atLeast45Items` | Total numbered checklist items across the entire file >= 45 |

#### Assertions Pattern

```typescript
// Test 19
expect(tlClaudeCopy).toMatch(/###?\s+TDD Process/);

// Test 20
expect(tlClaudeCopy).toMatch(/Red-Green-Refactor\s+progression/i);

// Test 21
expect(tlClaudeCopy).toContain("Double-Loop TDD");

// Test 22
expect(tlClaudeCopy).toMatch(/TPP\s+ordering|Transformation\s+Priority\s+Premise/i);

// Test 23
expect(tlClaudeCopy).toMatch(/atomic\s+commit/i);

// Test 24 -- extract TDD Process section and count items
const tddProcStart = tlClaudeCopy.indexOf("TDD Process");
const tddProcEnd = tlClaudeCopy.indexOf("\n### ", tddProcStart + 1);
const tddProcSection = tlClaudeCopy.slice(
  tddProcStart,
  tddProcEnd > -1 ? tddProcEnd : undefined,
);
const tddProcItems = tddProcSection.match(/^\s*\d+\.\s/gm) || [];
expect(tddProcItems.length).toBeGreaterThanOrEqual(4);

// Test 25 -- count all numbered items in the full checklist
const allTlItems = tlClaudeCopy.match(/^\s*\d+\.\s/gm) || [];
expect(allTlItems.length).toBeGreaterThanOrEqual(45);
```

---

### Group 5: Backward Compatibility -- Existing Content Preserved (9 tests)

Verify that all pre-existing sections, checklist items, and responsibilities in the 3 agents remain intact after the TDD additions. This satisfies RULE-003 (Backward Compatibility).

| # | Test Name | Assertion |
|---|-----------|-----------|
| 26 | `qaEngineer_backwardCompat_original24ItemsPreserved` | Items numbered 1 through 24 are present with original text fragments |
| 27 | `qaEngineer_backwardCompat_headingUpdatedTo28Point` | Heading changed from "24-Point" to "28-Point" |
| 28 | `qaEngineer_backwardCompat_existingCategoriesPreserved` | All original category headers remain (e.g., "Test Quality", "Coverage Analysis", "Fixtures & Organization") |
| 29 | `techLead_backwardCompat_original40ItemsPreserved` | Items numbered 1 through 40 are present with original text fragments |
| 30 | `techLead_backwardCompat_headingUpdatedTo45Point` | Heading changed from "40-Point" to "45-Point" |
| 31 | `techLead_backwardCompat_existingCategoriesPreserved` | All original category headers remain (e.g., "Clean Code", "SOLID", "Architecture", "Operational Readiness") |
| 32 | `typescriptDeveloper_backwardCompat_existingSectionsPreserved` | Original sections remain (e.g., "## Responsibilities", "## Code Style") |
| 33 | `typescriptDeveloper_backwardCompat_originalResponsibilitiesPreserved` | All 8 original responsibilities are present (possibly reordered, but text preserved) |
| 34 | `typescriptDeveloper_backwardCompat_frameworkPlaceholderPreserved` | `{{FRAMEWORK}}` placeholder is still present in the template |

#### Assertions Pattern

```typescript
// Test 26 -- spot-check original items by number and key phrase
const originalQaItems = [
  { num: 1, phrase: /test.*nam/i },      // Test naming convention
  { num: 12, phrase: /cover/i },          // Coverage related
  { num: 24, phrase: /fixture|organiz/i },// Fixtures & Organization last item
];
for (const item of originalQaItems) {
  expect(qaClaudeCopy).toMatch(new RegExp(`^\\s*${item.num}\\.\\s.*${item.phrase.source}`, "im"));
}

// Test 27
expect(qaClaudeCopy).toMatch(/28-Point/);

// Test 28
const qaCategories = [
  "Test Quality",
  "Coverage Analysis",
  "Fixtures & Organization",
];
for (const cat of qaCategories) {
  expect(qaClaudeCopy).toContain(cat);
}

// Test 29 -- spot-check original tech lead items
const originalTlItems = [
  { num: 1, phrase: /clean\s*code|naming/i },
  { num: 20, phrase: /solid|depend/i },
  { num: 40, phrase: /operational|readiness/i },
];
for (const item of originalTlItems) {
  expect(tlClaudeCopy).toMatch(new RegExp(`^\\s*${item.num}\\.\\s.*${item.phrase.source}`, "im"));
}

// Test 30
expect(tlClaudeCopy).toMatch(/45-Point/);

// Test 31
const tlCategories = [
  "Clean Code",
  "SOLID",
  "Operational Readiness",
];
for (const cat of tlCategories) {
  expect(tlClaudeCopy).toContain(cat);
}

// Test 32
expect(tsDevClaude).toContain("## Responsibilities");

// Test 33 -- verify all 8 original responsibility phrases are present
const originalDevResponsibilities = [
  "strictly typed code",
  "dependency injection",
  "database migrations",
  "environment variables",
  "Clean Code principles",
  "error handling",
];
for (const phrase of originalDevResponsibilities) {
  expect(tsDevClaude).toContain(phrase);
}

// Test 34
expect(tsDevClaude).toContain("{{FRAMEWORK}}");
```

---

### Group 6: Dual Copy Consistency -- TDD Sections in Both Formats (8 tests)

Verify that TDD sections are present in both `agents-templates/` (Claude format) and `github-agents-templates/` (GitHub format), satisfying RULE-001 (Dual Copy Consistency).

| # | Test Name | Assertion |
|---|-----------|-----------|
| 35 | `dualCopy_typescriptDeveloper_bothContainTddWorkflow` | Both Claude and GitHub copies contain `## TDD Workflow` |
| 36 | `dualCopy_typescriptDeveloper_bothContainWriteTestFirst` | Both copies contain `write the test FIRST` (case-insensitive) |
| 37 | `dualCopy_qaEngineer_bothContainTddCompliance` | Both copies contain `TDD Compliance` |
| 38 | `dualCopy_qaEngineer_bothContainTestFirstPattern` | Both copies contain `test-first pattern` |
| 39 | `dualCopy_qaEngineer_bothContain28PointHeading` | Both copies contain `28-Point` in heading |
| 40 | `dualCopy_techLead_bothContainTddProcess` | Both copies contain `TDD Process` |
| 41 | `dualCopy_techLead_bothContainRedGreenRefactor` | Both copies contain `Red-Green-Refactor` |
| 42 | `dualCopy_techLead_bothContain45PointHeading` | Both copies contain `45-Point` in heading |

#### Assertions Pattern

```typescript
// Test 35
expect(tsDevClaude).toContain("## TDD Workflow");
expect(tsDevGithub).toContain("## TDD Workflow");

// Test 36
expect(tsDevClaude.toLowerCase()).toContain("write the test first");
expect(tsDevGithub.toLowerCase()).toContain("write the test first");

// Test 37
expect(qaClaudeCopy).toMatch(/TDD Compliance/);
expect(qaGithubCopy).toMatch(/TDD Compliance/);

// Test 38
expect(qaClaudeCopy.toLowerCase()).toContain("test-first pattern");
expect(qaGithubCopy.toLowerCase()).toContain("test-first pattern");

// Test 39
expect(qaClaudeCopy).toMatch(/28-Point/);
expect(qaGithubCopy).toMatch(/28-Point/);

// Test 40
expect(tlClaudeCopy).toMatch(/TDD Process/);
expect(tlGithubCopy).toMatch(/TDD Process/);

// Test 41
expect(tlClaudeCopy).toContain("Red-Green-Refactor");
expect(tlGithubCopy).toContain("Red-Green-Refactor");

// Test 42
expect(tlClaudeCopy).toMatch(/45-Point/);
expect(tlGithubCopy).toMatch(/45-Point/);
```

---

## 4. Section Ordering Validation

The TDD sections must be appended AFTER all existing content (additive change per RULE-003). This is validated by:

### qa-engineer: TDD Compliance after Fixtures & Organization

```typescript
it("qaEngineer_ordering_tddComplianceAfterFixturesOrganization", () => {
  const lastOriginalCategory = qaClaudeCopy.indexOf("Fixtures & Organization");
  const tddCategory = qaClaudeCopy.indexOf("TDD Compliance");
  expect(tddCategory).toBeGreaterThan(lastOriginalCategory);
});
```

### tech-lead: TDD Process after Operational Readiness

```typescript
it("techLead_ordering_tddProcessAfterOperationalReadiness", () => {
  const lastOriginalCategory = tlClaudeCopy.indexOf("Operational Readiness");
  const tddCategory = tlClaudeCopy.indexOf("TDD Process");
  expect(tddCategory).toBeGreaterThan(lastOriginalCategory);
});
```

### typescript-developer: TDD Workflow after Responsibilities

```typescript
it("typescriptDeveloper_ordering_tddWorkflowAfterResponsibilities", () => {
  const responsibilities = tsDevClaude.indexOf("## Responsibilities");
  const tddWorkflow = tsDevClaude.indexOf("## TDD Workflow");
  expect(tddWorkflow).toBeGreaterThan(responsibilities);
});
```

These are 3 additional structural tests included in the content validation groups. They are counted within their respective groups (not as separate tests).

---

## 5. Golden File Update Procedure

### Step 1: Modify Source Templates

Edit the 6 source template files:

1. `resources/agents-templates/developers/typescript-developer.md` -- Add TDD Workflow, reorder responsibilities
2. `resources/agents-templates/core/qa-engineer.md` -- Add TDD Compliance (25-28)
3. `resources/agents-templates/core/tech-lead.md` -- Add TDD Process (41-45)
4. `resources/github-agents-templates/developers/typescript-developer.md` -- Mirror #1 in GitHub format
5. `resources/github-agents-templates/core/qa-engineer.md` -- Mirror #2 in GitHub format
6. `resources/github-agents-templates/core/tech-lead.md` -- Mirror #3 in GitHub format

### Step 2: Update Generated Agents

Copy the Claude format templates to `.claude/agents/`:

```bash
cp resources/agents-templates/core/qa-engineer.md .claude/agents/qa-engineer.md
cp resources/agents-templates/core/tech-lead.md .claude/agents/tech-lead.md
cp resources/agents-templates/developers/typescript-developer.md .claude/agents/typescript-developer.md
```

### Step 3: Regenerate Golden Files

Run the pipeline for each profile and copy outputs to golden directories:

```bash
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  npx ts-node src/cli.ts generate \
    --config resources/config-templates/setup-config.${profile}.yaml \
    --output /tmp/golden-${profile}
  # Copy qa-engineer and tech-lead for all profiles (.claude and .github)
  cp /tmp/golden-${profile}/.claude/agents/qa-engineer.md \
     tests/golden/${profile}/.claude/agents/qa-engineer.md
  cp /tmp/golden-${profile}/.claude/agents/tech-lead.md \
     tests/golden/${profile}/.claude/agents/tech-lead.md
  cp /tmp/golden-${profile}/.github/agents/qa-engineer.agent.md \
     tests/golden/${profile}/.github/agents/qa-engineer.agent.md
  cp /tmp/golden-${profile}/.github/agents/tech-lead.agent.md \
     tests/golden/${profile}/.github/agents/tech-lead.agent.md
done

# typescript-developer only for typescript-nestjs profile
cp /tmp/golden-typescript-nestjs/.claude/agents/typescript-developer.md \
   tests/golden/typescript-nestjs/.claude/agents/typescript-developer.md
cp /tmp/golden-typescript-nestjs/.github/agents/typescript-developer.agent.md \
   tests/golden/typescript-nestjs/.github/agents/typescript-developer.agent.md
```

### Step 4: Verify

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

All 8 profiles must pass with `success: true`.

---

## 6. Coverage Strategy

### 6.1 Line Coverage

No new production code is added -- this is a content file change. Coverage impact is zero. Existing coverage (99.6% lines, 97.84% branches) is maintained.

### 6.2 Branch Coverage

The `verifyOutput()` function in `src/verifier.ts` exercises all branches through the golden file comparison:
- `Buffer.equals()` returns `true` for matching files (happy path)
- `FileDiff` produced for mismatching files (detected during development if golden files are stale)
- `missingFiles` / `extraFiles` arrays populated if file sets differ

### 6.3 Coverage Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | No new code paths; existing coverage maintained |
| Branch | >= 90% | Existing verifier branches already covered |

---

## 7. Test Matrix Summary

| Group | Description | Test Count | Type |
|-------|-------------|------------|------|
| G1: Golden File Integration | Byte-for-byte parity across 8 profiles | 8 | Integration (existing) |
| G2: typescript-developer Content | TDD Workflow section, test-first ordering, phases | 5 | Content Validation |
| G3: qa-engineer Content | TDD Compliance category, item count, key phrases | 5 | Content Validation |
| G4: tech-lead Content | TDD Process category, item count, key phrases | 7 | Content Validation |
| G5: Backward Compatibility | Existing sections/items preserved in all 3 agents | 9 | Content Validation |
| G6: Dual Copy Consistency | TDD sections in both template directories | 8 | Content Validation |
| **Total** | | **42** | |

---

## 8. Execution Commands

### Run Content Validation Tests Only

```bash
npx vitest run tests/node/content/agents-tdd-sections.test.ts
```

### Run Golden File Tests Only

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

### Run Both

```bash
npx vitest run tests/node/content/agents-tdd-sections.test.ts tests/node/integration/byte-for-byte.test.ts
```

### Full Test Suite (regression check)

```bash
npx vitest run
```

---

## 9. Golden File Inventory

### 9.1 `.claude/agents/` Format (17 files)

| # | Golden File Path |
|---|-----------------|
| 1 | `tests/golden/go-gin/.claude/agents/qa-engineer.md` |
| 2 | `tests/golden/java-quarkus/.claude/agents/qa-engineer.md` |
| 3 | `tests/golden/java-spring/.claude/agents/qa-engineer.md` |
| 4 | `tests/golden/kotlin-ktor/.claude/agents/qa-engineer.md` |
| 5 | `tests/golden/python-click-cli/.claude/agents/qa-engineer.md` |
| 6 | `tests/golden/python-fastapi/.claude/agents/qa-engineer.md` |
| 7 | `tests/golden/rust-axum/.claude/agents/qa-engineer.md` |
| 8 | `tests/golden/typescript-nestjs/.claude/agents/qa-engineer.md` |
| 9 | `tests/golden/go-gin/.claude/agents/tech-lead.md` |
| 10 | `tests/golden/java-quarkus/.claude/agents/tech-lead.md` |
| 11 | `tests/golden/java-spring/.claude/agents/tech-lead.md` |
| 12 | `tests/golden/kotlin-ktor/.claude/agents/tech-lead.md` |
| 13 | `tests/golden/python-click-cli/.claude/agents/tech-lead.md` |
| 14 | `tests/golden/python-fastapi/.claude/agents/tech-lead.md` |
| 15 | `tests/golden/rust-axum/.claude/agents/tech-lead.md` |
| 16 | `tests/golden/typescript-nestjs/.claude/agents/tech-lead.md` |
| 17 | `tests/golden/typescript-nestjs/.claude/agents/typescript-developer.md` |

### 9.2 `.github/agents/` Format (17 files)

| # | Golden File Path |
|---|-----------------|
| 18 | `tests/golden/go-gin/.github/agents/qa-engineer.agent.md` |
| 19 | `tests/golden/java-quarkus/.github/agents/qa-engineer.agent.md` |
| 20 | `tests/golden/java-spring/.github/agents/qa-engineer.agent.md` |
| 21 | `tests/golden/kotlin-ktor/.github/agents/qa-engineer.agent.md` |
| 22 | `tests/golden/python-click-cli/.github/agents/qa-engineer.agent.md` |
| 23 | `tests/golden/python-fastapi/.github/agents/qa-engineer.agent.md` |
| 24 | `tests/golden/rust-axum/.github/agents/qa-engineer.agent.md` |
| 25 | `tests/golden/typescript-nestjs/.github/agents/qa-engineer.agent.md` |
| 26 | `tests/golden/go-gin/.github/agents/tech-lead.agent.md` |
| 27 | `tests/golden/java-quarkus/.github/agents/tech-lead.agent.md` |
| 28 | `tests/golden/java-spring/.github/agents/tech-lead.agent.md` |
| 29 | `tests/golden/kotlin-ktor/.github/agents/tech-lead.agent.md` |
| 30 | `tests/golden/python-click-cli/.github/agents/tech-lead.agent.md` |
| 31 | `tests/golden/python-fastapi/.github/agents/tech-lead.agent.md` |
| 32 | `tests/golden/rust-axum/.github/agents/tech-lead.agent.md` |
| 33 | `tests/golden/typescript-nestjs/.github/agents/tech-lead.agent.md` |
| 34 | `tests/golden/typescript-nestjs/.github/agents/typescript-developer.agent.md` |

---

## 10. Dependencies and Prerequisites

### Prerequisites

- Source template files exist:
  - `resources/agents-templates/core/qa-engineer.md` (24-point checklist)
  - `resources/agents-templates/core/tech-lead.md` (40-point checklist)
  - `resources/agents-templates/developers/typescript-developer.md`
  - Equivalent files in `resources/github-agents-templates/`
- All 8 profile config templates exist in `resources/config-templates/`
- Golden file directories exist for all 8 profiles under `tests/golden/`
- `tests/helpers/integration-constants.ts` exports `CONFIG_PROFILES`, `GOLDEN_DIR`, `RESOURCES_DIR`
- story-0003-0001 (KP Testing with TDD) completed
- story-0003-0002 (KP Coding Standards with refactoring) completed

### Import Dependencies (for new test file)

| Module | Import | Used For |
|--------|--------|----------|
| `node:fs` | `readFileSync` | Reading source template content |
| `node:path` | `resolve`, `dirname` | Path resolution |
| `node:url` | `fileURLToPath` | ESM `__dirname` equivalent |
| `vitest` | `describe`, `it`, `expect`, `beforeAll` | Test framework |

---

## 11. Risk Areas

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden files not updated for all 34 files | High | High | CI fails immediately on byte-for-byte mismatch. Use glob patterns and the shell loop in Section 5 to ensure all files are covered. |
| Content assertions too brittle (exact string matching) | Medium | Medium | Use `toContain()` for key phrases and `toMatch()` with regex for flexible assertions. Avoid matching full lines. |
| Existing checklist items accidentally altered | Low | High | Backward compatibility tests (Group 5) catch any modification to existing items. New items are appended at the end, never inserted between existing items. |
| Dual copy inconsistency between Claude and GitHub formats | Low | Medium | Group 6 tests explicitly verify both copies contain the same TDD concepts. Pipeline copies from separate template directories, so both must be manually updated. |
| Formatting differences break byte-for-byte comparison | Medium | High | Match exact whitespace, line endings, and trailing newlines of existing files. Use the pipeline regeneration approach rather than manual golden file edits. |
| Placeholder breakage (`{{FRAMEWORK}}`, etc.) | Low | Medium | TDD sections use no new placeholders. Test 34 verifies existing `{{FRAMEWORK}}` placeholder is preserved. |
| Test file discovery by Vitest | Low | Low | New test file at `tests/node/content/` must match the glob pattern in `vitest.config.ts` (`tests/**/*.test.ts`). Verify pattern includes the `content/` subdirectory. |
| Numbered item count mismatch between formats | Low | Medium | Claude format uses full numbered items (1-28 for QA, 1-45 for TL). GitHub format may use condensed summaries. Content tests validate both independently. |

---

## 12. Naming Convention Reference

All new test names follow `[agentUnderTest]_[scenario]_[expectedBehavior]`:

```
typescriptDeveloper_tddWorkflowSection_containsH2Header
typescriptDeveloper_tddWorkflow_containsWriteTestFirst
typescriptDeveloper_tddWorkflow_containsRedGreenRefactorCycle
typescriptDeveloper_responsibilities_testBeforeImplement
typescriptDeveloper_tddWorkflow_containsCommitAfterCycle
qaEngineer_tddCompliance_categoryExists
qaEngineer_tddCompliance_containsTestFirstPattern
qaEngineer_tddCompliance_containsRefactoringCommits
qaEngineer_tddCompliance_containsAtLeast4Items
qaEngineer_totalChecklist_atLeast28Items
techLead_tddProcess_categoryExists
techLead_tddProcess_containsRedGreenRefactorProgression
techLead_tddProcess_containsDoubleLoopTdd
techLead_tddProcess_containsTppOrdering
techLead_tddProcess_containsAtomicCommits
techLead_tddProcess_containsAtLeast4Items
techLead_totalChecklist_atLeast45Items
qaEngineer_backwardCompat_original24ItemsPreserved
qaEngineer_backwardCompat_headingUpdatedTo28Point
qaEngineer_backwardCompat_existingCategoriesPreserved
techLead_backwardCompat_original40ItemsPreserved
techLead_backwardCompat_headingUpdatedTo45Point
techLead_backwardCompat_existingCategoriesPreserved
typescriptDeveloper_backwardCompat_existingSectionsPreserved
typescriptDeveloper_backwardCompat_originalResponsibilitiesPreserved
typescriptDeveloper_backwardCompat_frameworkPlaceholderPreserved
dualCopy_typescriptDeveloper_bothContainTddWorkflow
dualCopy_typescriptDeveloper_bothContainWriteTestFirst
dualCopy_qaEngineer_bothContainTddCompliance
dualCopy_qaEngineer_bothContainTestFirstPattern
dualCopy_qaEngineer_bothContain28PointHeading
dualCopy_techLead_bothContainTddProcess
dualCopy_techLead_bothContainRedGreenRefactor
dualCopy_techLead_bothContain45PointHeading
```

---

## 13. Acceptance Criteria Traceability

| Gherkin Scenario (from story) | Test Group | Test IDs |
|-------------------------------|-----------|----------|
| Developer agent contains TDD Workflow | G2 | 9, 10, 11, 13 |
| Developer agent reorders responsibilities | G2 | 12 |
| QA agent contains TDD Compliance category | G3 | 14, 15, 16, 17 |
| QA agent checklist total >= 28 | G3 | 18 |
| Tech Lead agent contains TDD Process category | G4 | 19, 20, 21, 22, 23, 24 |
| Tech Lead checklist total >= 45 | G4 | 25 |
| Existing checklists preserved (items 1-24 QA, 1-40 TL) | G5 | 26-34 |
| Dual copy consistency for all 3 agents | G6 | 35-42 |
| Golden files updated and passing | G1 | 1-8 |
