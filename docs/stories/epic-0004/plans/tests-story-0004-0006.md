# Test Plan — story-0004-0006

## Summary

This story creates a new skill `x-dev-architecture-plan` with two source templates (Claude Code and GitHub Copilot), registers it in `SKILL_GROUPS["dev"]` in the `GithubSkillsAssembler`, and updates golden files for all 8 profiles across 3 output directories. Testing validates SKILL.md content (frontmatter, decision tree, KP list, output structure, mini-ADR format, subagent prompt), assembler registration, and byte-for-byte golden file parity.

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Content validation (Claude source) | Verify SKILL.md structure, frontmatter, sections | YES | `tests/node/content/x-dev-architecture-plan-content.test.ts` |
| Content validation (GitHub source) | Verify GitHub template contains equivalent content | YES | `tests/node/content/x-dev-architecture-plan-content.test.ts` (same file, separate describe) |
| Dual copy consistency | Verify both sources contain semantically equivalent content (RULE-001) | YES | `tests/node/content/x-dev-architecture-plan-content.test.ts` |
| Assembler unit tests | Verify SKILL_GROUPS["dev"] includes new skill | YES | `tests/node/assembler/github-skills-assembler.test.ts` (modify existing) |
| Golden file integration | Verify pipeline output matches updated golden files | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |
| Assembler integration | Verify SkillsAssembler auto-discovers new core skill | NO (existing) | `tests/node/assembler/skills-assembler.test.ts` |

---

## 2. Unit Tests (UT-N) — Content Validation

### 2.1 File: `tests/node/content/x-dev-architecture-plan-content.test.ts`

**Source files under test:**
- Claude: `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`
- GitHub: `resources/github-skills-templates/dev/x-dev-architecture-plan.md`

Tests follow TPP ordering: degenerate (file exists) -> simple (frontmatter fields) -> conditional (section content) -> complex (cross-section validation).

#### 2.1.1 Claude Source — File Existence and Frontmatter (Degenerate)

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 1 | UT-1 | `claudeSource_fileExists_atExpectedPath` | SKILL.md file exists at `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md` | None | Yes | P0 |
| 2 | UT-2 | `claudeSource_frontmatter_containsRequiredFields` | YAML frontmatter contains `name`, `description`, `allowed-tools`, `argument-hint` | UT-1 | Yes | P0 |
| 3 | UT-3 | `claudeSource_frontmatter_nameEqualsXDevArchitecturePlan` | Frontmatter `name` field equals `x-dev-architecture-plan` | UT-2 | Yes | P0 |
| 4 | UT-4 | `claudeSource_frontmatter_descriptionIsNonEmpty` | Frontmatter `description` is present and non-empty | UT-2 | Yes | P1 |
| 5 | UT-5 | `claudeSource_frontmatter_allowedToolsContainsReadWriteEditBashGrepGlob` | `allowed-tools` includes Read, Write, Edit, Bash, Grep, Glob | UT-2 | Yes | P0 |
| 6 | UT-6 | `claudeSource_frontmatter_argumentHintContainsStoryIdOrFeature` | `argument-hint` contains "STORY-ID" or "feature" | UT-2 | Yes | P1 |

#### 2.1.2 Claude Source — Global Output Policy

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 7 | UT-7 | `claudeSource_containsGlobalOutputPolicySection` | Contains `## Global Output Policy` section with English ONLY mandate | UT-1 | Yes | P1 |

#### 2.1.3 Claude Source — When to Use (Decision Tree)

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 8 | UT-8 | `claudeSource_whenToUse_sectionExists` | Contains `## When to Use` heading (or equivalent decision tree heading) | UT-1 | Yes | P0 |
| 9 | UT-9 | `claudeSource_whenToUse_containsFullPlanOutcome` | Decision tree includes "Full" plan outcome with criteria: new service, new integration, contract change, infra change | UT-8 | Yes | P0 |
| 10 | UT-10 | `claudeSource_whenToUse_containsSimplifiedPlanOutcome` | Decision tree includes "Simplified" plan outcome with criteria: new feature in existing service, no contract change | UT-8 | Yes | P0 |
| 11 | UT-11 | `claudeSource_whenToUse_containsSkipOutcome` | Decision tree includes "Skip" outcome with criteria: bug fix, refactoring, documentation change | UT-8 | Yes | P0 |
| 12 | UT-12 | `claudeSource_whenToUse_containsMermaidDecisionTree` | Decision tree section contains a Mermaid `graph TD` diagram | UT-8 | Yes | P1 |

#### 2.1.4 Claude Source — Knowledge Packs

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 13 | UT-13 | `claudeSource_knowledgePacks_sectionExists` | Contains `## Knowledge Packs` heading | UT-1 | Yes | P0 |
| 14 | UT-14 | `claudeSource_knowledgePacks_listsAtLeast6KPs` | Lists at least 6 knowledge packs with paths | UT-13 | Yes | P0 |
| 15 | UT-15 | `claudeSource_knowledgePacks_containsArchitectureKP` | Lists `architecture` KP with path reference | UT-13 | Yes | P0 |
| 16 | UT-16 | `claudeSource_knowledgePacks_containsProtocolsKP` | Lists `protocols` KP with path reference | UT-13 | Yes | P0 |
| 17 | UT-17 | `claudeSource_knowledgePacks_containsSecurityKP` | Lists `security` KP with path reference | UT-13 | Yes | P0 |
| 18 | UT-18 | `claudeSource_knowledgePacks_containsObservabilityKP` | Lists `observability` KP with path reference | UT-13 | Yes | P0 |
| 19 | UT-19 | `claudeSource_knowledgePacks_containsInfrastructureKP` | Lists `infrastructure` KP with path reference | UT-13 | Yes | P1 |
| 20 | UT-20 | `claudeSource_knowledgePacks_containsResilienceKP` | Lists `resilience` KP with path reference | UT-13 | Yes | P1 |
| 21 | UT-21 | `claudeSource_knowledgePacks_containsComplianceKP` | Lists `compliance` KP (conditional — if compliance active) | UT-13 | Yes | P1 |

#### 2.1.5 Claude Source — Output Structure

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 22 | UT-22 | `claudeSource_outputStructure_sectionExists` | Contains `## Output Structure` heading | UT-1 | Yes | P0 |
| 23 | UT-23 | `claudeSource_outputStructure_containsAtLeast10Sections` | Lists at least 10 required output sections | UT-22 | Yes | P0 |
| 24 | UT-24 | `claudeSource_outputStructure_containsComponentDiagram` | Includes "Component Diagram" with Mermaid `graph TD` reference | UT-22 | Yes | P0 |
| 25 | UT-25 | `claudeSource_outputStructure_containsSequenceDiagrams` | Includes "Sequence Diagram" with Mermaid `sequenceDiagram` reference | UT-22 | Yes | P0 |
| 26 | UT-26 | `claudeSource_outputStructure_containsDeploymentDiagram` | Includes "Deployment Diagram" section | UT-22 | Yes | P0 |
| 27 | UT-27 | `claudeSource_outputStructure_containsExternalConnections` | Includes "External Connections" with table format (System, Protocol, Purpose, SLO) | UT-22 | Yes | P0 |
| 28 | UT-28 | `claudeSource_outputStructure_containsArchitectureDecisions` | Includes "Architecture Decisions" section referencing mini-ADR format | UT-22 | Yes | P0 |
| 29 | UT-29 | `claudeSource_outputStructure_containsTechnologyStack` | Includes "Technology Stack" with table format (Component, Technology, Rationale) | UT-22 | Yes | P0 |
| 30 | UT-30 | `claudeSource_outputStructure_containsNFRs` | Includes "NFR" section with table format (Metric, Target, Measurement) | UT-22 | Yes | P0 |
| 31 | UT-31 | `claudeSource_outputStructure_containsObservabilityStrategy` | Includes "Observability Strategy" section (metrics, spans, alerts) | UT-22 | Yes | P0 |
| 32 | UT-32 | `claudeSource_outputStructure_containsResilienceStrategy` | Includes "Resilience Strategy" section (circuit breaker, retry, fallback, degradation) | UT-22 | Yes | P0 |
| 33 | UT-33 | `claudeSource_outputStructure_containsImpactAnalysis` | Includes "Impact Analysis" section (affected services, risks) | UT-22 | Yes | P0 |
| 34 | UT-34 | `claudeSource_outputStructure_containsDataModel` | Includes "Data Model" section (optional, Mermaid ER or table) | UT-22 | Yes | P1 |

#### 2.1.6 Claude Source — Mini-ADR Format

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 35 | UT-35 | `claudeSource_miniAdrFormat_sectionExists` | Contains `## Mini-ADR Format` heading | UT-1 | Yes | P0 |
| 36 | UT-36 | `claudeSource_miniAdrFormat_containsContext` | Mini-ADR format includes "Context" field | UT-35 | Yes | P0 |
| 37 | UT-37 | `claudeSource_miniAdrFormat_containsDecision` | Mini-ADR format includes "Decision" field | UT-35 | Yes | P0 |
| 38 | UT-38 | `claudeSource_miniAdrFormat_containsRationale` | Mini-ADR format includes "Rationale" field | UT-35 | Yes | P0 |
| 39 | UT-39 | `claudeSource_miniAdrFormat_containsStoryRef` | Mini-ADR format includes "Story-Ref" or cross-reference field | UT-35 | Yes | P1 |

#### 2.1.7 Claude Source — Subagent Prompt

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 40 | UT-40 | `claudeSource_subagentPrompt_sectionExists` | Contains `## Subagent Prompt` heading | UT-1 | Yes | P0 |
| 41 | UT-41 | `claudeSource_subagentPrompt_containsArchitectPersona` | Subagent prompt specifies Architect persona (e.g., "Senior Architect") | UT-40 | Yes | P0 |
| 42 | UT-42 | `claudeSource_subagentPrompt_referencesKPReading` | Subagent prompt instructs reading knowledge packs | UT-40 | Yes | P1 |
| 43 | UT-43 | `claudeSource_subagentPrompt_referencesDecisionTreeEvaluation` | Subagent prompt instructs evaluating the decision tree (Full/Simplified/Skip) | UT-40 | Yes | P1 |
| 44 | UT-44 | `claudeSource_subagentPrompt_referencesOutputGeneration` | Subagent prompt instructs generating the architecture plan following output structure | UT-40 | Yes | P1 |
| 45 | UT-45 | `claudeSource_subagentPrompt_containsOutputPath` | Subagent prompt specifies output path pattern `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` | UT-40 | Yes | P1 |

#### 2.1.8 Claude Source — User-Invocable Configuration

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 46 | UT-46 | `claudeSource_frontmatter_doesNotContainUserInvocableFalse` | Frontmatter does NOT contain `user-invocable: false` | UT-1 | Yes | P0 |

#### 2.1.9 Claude Source — Placeholder Tokens

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 47 | UT-47 | `claudeSource_containsProjectNamePlaceholder` | Contains `{{PROJECT_NAME}}` or `{project_name}` placeholder token | UT-1 | Yes | P1 |

---

### 2.2 GitHub Source Template Validation

**Source file under test:** `resources/github-skills-templates/dev/x-dev-architecture-plan.md`

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 48 | UT-48 | `githubSource_fileExists_atExpectedPath` | GitHub template file exists at `resources/github-skills-templates/dev/x-dev-architecture-plan.md` | None | Yes | P0 |
| 49 | UT-49 | `githubSource_containsSkillNameReference` | Contains `x-dev-architecture-plan` skill name | UT-48 | Yes | P0 |
| 50 | UT-50 | `githubSource_containsDescriptionContent` | Contains a description of the skill purpose (architecture plan generation) | UT-48 | Yes | P0 |
| 51 | UT-51 | `githubSource_containsWhenToUseSection` | Contains "When to Use" guidance or decision tree reference | UT-48 | Yes | P0 |
| 52 | UT-52 | `githubSource_containsFullPlanReference` | References "Full" plan outcome | UT-48 | Yes | P0 |
| 53 | UT-53 | `githubSource_containsSimplifiedPlanReference` | References "Simplified" plan outcome | UT-48 | Yes | P0 |
| 54 | UT-54 | `githubSource_containsSkipReference` | References "Skip" outcome | UT-48 | Yes | P0 |
| 55 | UT-55 | `githubSource_containsKnowledgePacksReference` | References knowledge packs to read | UT-48 | Yes | P1 |
| 56 | UT-56 | `githubSource_containsOutputStructureReference` | References the output structure or architecture plan sections | UT-48 | Yes | P1 |
| 57 | UT-57 | `githubSource_containsMiniAdrReference` | References mini-ADR format (Context, Decision, Rationale) | UT-48 | Yes | P1 |
| 58 | UT-58 | `githubSource_containsPlaceholderTokens` | Contains `{{PROJECT_NAME}}` or `{project_name}` placeholder tokens | UT-48 | Yes | P1 |

---

### 2.3 Dual Copy Consistency (RULE-001)

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 59 | UT-59 | `dualCopy_bothContainSkillName` | Both templates reference `x-dev-architecture-plan` | UT-1, UT-48 | Yes | P0 |
| 60 | UT-60 | `dualCopy_bothContainDecisionTreeWith3Outcomes` | Both templates describe Full, Simplified, and Skip outcomes | UT-1, UT-48 | Yes | P0 |
| 61 | UT-61 | `dualCopy_bothContainKnowledgePackReferences` | Both templates reference knowledge packs to read | UT-1, UT-48 | Yes | P1 |
| 62 | UT-62 | `dualCopy_bothContainOutputStructure` | Both templates describe the architecture plan output structure | UT-1, UT-48 | Yes | P1 |
| 63 | UT-63 | `dualCopy_bothContainMiniAdrFormat` | Both templates describe mini-ADR format with Context/Decision/Rationale | UT-1, UT-48 | Yes | P1 |

---

### 2.4 Assembler Registration (SKILL_GROUPS)

**Test file:** `tests/node/assembler/github-skills-assembler.test.ts` (modify existing)

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 64 | UT-64 | `SKILL_GROUPS_devGroup_containsXDevArchitecturePlan` | `SKILL_GROUPS["dev"]` array includes `"x-dev-architecture-plan"` | None | Yes | P0 |
| 65 | UT-65 | `SKILL_GROUPS_devGroup_contains4Skills` | `SKILL_GROUPS["dev"]` has length 4 (was 3: x-dev-implement, x-dev-lifecycle, layer-templates; now +1: x-dev-architecture-plan) | None | Yes | P0 |

---

## 3. Integration Tests (IT-N) — Golden File Parity

**Test file:** `tests/node/integration/byte-for-byte.test.ts` (existing, no changes needed)

These tests are already implemented. After creating the new templates, registering in `SKILL_GROUPS["dev"]`, and regenerating golden files, the existing byte-for-byte tests validate the new skill output.

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 66 | IT-1 | `pipelineMatchesGoldenFiles_{profile}` (x8 profiles) | Full pipeline output matches golden files byte-for-byte, including new `.claude/skills/x-dev-architecture-plan/SKILL.md` | All UT pass | No (sequential per profile) | P0 |
| 67 | IT-2 | `noMissingFiles_{profile}` (x8 profiles) | No expected golden files are missing from pipeline output (catches case where skill template is not discovered) | All UT pass | No (sequential per profile) | P0 |
| 68 | IT-3 | `noExtraFiles_{profile}` (x8 profiles) | No unexpected extra files in pipeline output (catches case where golden files are not regenerated) | All UT pass | No (sequential per profile) | P0 |

**Profiles tested:** go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs

**Golden files verified per profile (new skill):**

| Output Directory | Path in Golden | Source Template |
|------------------|----------------|-----------------|
| `.claude/` | `tests/golden/{profile}/.claude/skills/x-dev-architecture-plan/SKILL.md` | `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md` (via `SkillsAssembler` auto-discovery) |
| `.agents/` | `tests/golden/{profile}/.agents/skills/x-dev-architecture-plan/SKILL.md` | Same as Claude (via `CodexSkillsAssembler` mirror) |
| `.github/` | `tests/golden/{profile}/.github/skills/x-dev-architecture-plan/SKILL.md` | `resources/github-skills-templates/dev/x-dev-architecture-plan.md` (via `GithubSkillsAssembler` SKILL_GROUPS["dev"]) |

**Total new golden files: 24** (3 directories x 8 profiles)

Additionally, existing golden files that aggregate skill listings (CLAUDE.md, README, AGENTS.md, copilot-instructions.md) will need content updates to include the new skill in their counts and tables.

---

## 4. Acceptance Tests (AT-N) — End-to-End Validation

These are meta-level validations confirmed by the combination of unit + integration tests passing together.

| # | Test ID | Test Name | What It Validates | Depends On | Parallel | Priority |
|---|---------|-----------|-------------------|------------|----------|----------|
| 69 | AT-1 | `fullPipeline_generatesXDevArchitecturePlanSkill_inAll3OutputLocations` | Running the full pipeline produces `x-dev-architecture-plan` skill in `.claude/skills/`, `.agents/skills/`, and `.github/skills/` for every profile | IT-1 through IT-3 | No | P0 |
| 70 | AT-2 | `skillIsUserInvocable_noUserInvocableFalseInFrontmatter` | The generated SKILL.md does NOT contain `user-invocable: false`, confirming it appears in the `/` menu | UT-46 | Yes | P0 |

AT-1 is verified by the byte-for-byte integration tests passing with the updated golden files. AT-2 is directly verified by UT-46.

---

## 5. Content Verification — Key Patterns That Must Appear

The following patterns are validated by content tests using `toContain()` or `toMatch()` (not brittle exact-line matching):

### 5.1 YAML Frontmatter

| Pattern | Validated By |
|---------|-------------|
| `name: x-dev-architecture-plan` | UT-3 |
| `description:` (non-empty) | UT-4 |
| `allowed-tools:` with Read, Write, Edit, Bash, Grep, Glob | UT-5 |
| `argument-hint:` with STORY-ID reference | UT-6 |
| Absence of `user-invocable: false` | UT-46 |

### 5.2 Decision Tree

| Pattern | Validated By |
|---------|-------------|
| "Full" + (new service OR new integration OR contract change OR infra change) | UT-9 |
| "Simplified" + (existing service OR no contract change) | UT-10 |
| "Skip" + (bug fix OR refactoring OR documentation) | UT-11 |
| Mermaid `graph TD` block | UT-12 |

### 5.3 Knowledge Packs (>= 6 entries)

| Pattern | Validated By |
|---------|-------------|
| `architecture` reference | UT-15 |
| `protocols` reference | UT-16 |
| `security` reference | UT-17 |
| `observability` reference | UT-18 |
| `infrastructure` reference | UT-19 |
| `resilience` reference | UT-20 |
| `compliance` reference (conditional) | UT-21 |

### 5.4 Output Structure (>= 10 sections)

| Pattern | Validated By |
|---------|-------------|
| Component Diagram | UT-24 |
| Sequence Diagram | UT-25 |
| Deployment Diagram | UT-26 |
| External Connections | UT-27 |
| Architecture Decisions | UT-28 |
| Technology Stack | UT-29 |
| NFR | UT-30 |
| Observability Strategy | UT-31 |
| Resilience Strategy | UT-32 |
| Impact Analysis | UT-33 |
| Data Model (optional) | UT-34 |

### 5.5 Mini-ADR Format

| Pattern | Validated By |
|---------|-------------|
| `Context` field | UT-36 |
| `Decision` field | UT-37 |
| `Rationale` field | UT-38 |
| Story reference field | UT-39 |

---

## 6. Existing Tests — Impact Assessment

### 6.1 Golden File Integration Tests (NO changes to test logic)

- **File:** `tests/node/integration/byte-for-byte.test.ts`
- **Impact:** Golden files must be regenerated to include the new skill. The test logic itself is unchanged.
- **Expected result:** 40 assertions pass (5 per profile x 8 profiles)

### 6.2 Assembler Unit Tests (MODIFY `github-skills-assembler.test.ts`)

- **File:** `tests/node/assembler/github-skills-assembler.test.ts`
- **Changes needed:**
  - Update any assertion that checks `SKILL_GROUPS` total size (currently 8 groups — unchanged, since a skill is added to an existing group)
  - Add UT-64 and UT-65 to validate dev group membership and count
- **Existing test `SKILL_GROUPS_has8Groups` (line 67):** Unchanged (still 8 groups)

### 6.3 Skills Assembler Unit Tests (NO changes)

- **File:** `tests/node/assembler/skills-assembler.test.ts`
- **Impact:** None — `SkillsAssembler.selectCoreSkills()` auto-discovers directories under `resources/skills-templates/core/`. Creating the new directory is sufficient.

### 6.4 Codex Skills Assembler Tests (NO changes)

- **File:** `tests/node/assembler/codex-skills-assembler.test.ts`
- **Impact:** None — mirrors the Claude skills assembler output.

---

## 7. Golden Files Requiring Update

### 7.1 New Golden Files (24 files)

8 profiles x 3 output directories:

| Profile | `.claude/skills/x-dev-architecture-plan/SKILL.md` | `.agents/skills/x-dev-architecture-plan/SKILL.md` | `.github/skills/x-dev-architecture-plan/SKILL.md` |
|---------|---|---|---|
| go-gin | CREATE | CREATE | CREATE |
| java-quarkus | CREATE | CREATE | CREATE |
| java-spring | CREATE | CREATE | CREATE |
| kotlin-ktor | CREATE | CREATE | CREATE |
| python-click-cli | CREATE | CREATE | CREATE |
| python-fastapi | CREATE | CREATE | CREATE |
| rust-axum | CREATE | CREATE | CREATE |
| typescript-nestjs | CREATE | CREATE | CREATE |

### 7.2 Modified Golden Files (estimated ~24-40 files)

Existing golden files that enumerate skills in tables, counts, or listings:
- `CLAUDE.md` per profile (skill table and count)
- `.claude/README.md` per profile (if skill listing exists)
- `.github/copilot-instructions.md` per profile (if skill listing exists)
- Agent files that reference available skills

### 7.3 Golden File Regeneration Strategy

```bash
# After creating templates and registering in SKILL_GROUPS:
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  npx tsx src/cli.ts generate \
    --config "resources/config-templates/setup-config.${profile}.yaml" \
    --output "tests/golden/${profile}/"
done
```

---

## 8. Suggested Test Implementation Pattern

```typescript
import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-dev-architecture-plan/SKILL.md",
);
const GITHUB_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/github-skills-templates/dev/x-dev-architecture-plan.md",
);

const claudeContent = fs.readFileSync(CLAUDE_SOURCE, "utf-8");
const githubContent = fs.readFileSync(GITHUB_SOURCE, "utf-8");

describe("x-dev-architecture-plan Claude source — frontmatter", () => {
  // UT-1 through UT-6, UT-46
});

describe("x-dev-architecture-plan Claude source — global output policy", () => {
  // UT-7
});

describe("x-dev-architecture-plan Claude source — when to use (decision tree)", () => {
  // UT-8 through UT-12
});

describe("x-dev-architecture-plan Claude source — knowledge packs", () => {
  // UT-13 through UT-21
});

describe("x-dev-architecture-plan Claude source — output structure", () => {
  // UT-22 through UT-34
});

describe("x-dev-architecture-plan Claude source — mini-ADR format", () => {
  // UT-35 through UT-39
});

describe("x-dev-architecture-plan Claude source — subagent prompt", () => {
  // UT-40 through UT-45
});

describe("x-dev-architecture-plan GitHub source", () => {
  // UT-48 through UT-58
});

describe("x-dev-architecture-plan dual copy consistency (RULE-001)", () => {
  // UT-59 through UT-63
});
```

---

## 9. TDD Execution Order

| Step | Action | Test State |
|------|--------|-----------|
| 1 | Write content validation tests (`tests/node/content/x-dev-architecture-plan-content.test.ts`) with all 63 content test cases | RED (source files do not exist) |
| 2 | Write assembler tests UT-64, UT-65 in `github-skills-assembler.test.ts` | RED (skill not in SKILL_GROUPS yet) |
| 3 | Create `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md` | Partial GREEN (Claude content tests pass, GitHub tests still RED) |
| 4 | Create `resources/github-skills-templates/dev/x-dev-architecture-plan.md` | GREEN (all content + consistency tests pass) |
| 5 | Add `"x-dev-architecture-plan"` to `SKILL_GROUPS["dev"]` in `github-skills-assembler.ts` | GREEN (UT-64, UT-65 pass) |
| 6 | Regenerate golden files for all 8 profiles (script from Section 7.3) | N/A (golden files updated) |
| 7 | Run byte-for-byte integration tests | GREEN (golden file parity confirmed) |
| 8 | Run full test suite (`npx vitest run`) | GREEN (all tests pass) |

---

## 10. Verification Checklist

- [ ] `npx vitest run tests/node/content/x-dev-architecture-plan-content.test.ts` — all 63 content tests pass
- [ ] `npx vitest run tests/node/assembler/github-skills-assembler.test.ts` — all assembler tests pass (including new UT-64, UT-65)
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` — all 8 profiles pass (40 assertions)
- [ ] `npx vitest run` — full suite passes
- [ ] Coverage remains >= 95% line, >= 90% branch
- [ ] Zero compiler/linter warnings
- [ ] Deployed copy (`.claude/skills/x-dev-architecture-plan/SKILL.md`) matches Claude source template after placeholder resolution

---

## 11. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Golden file cascade — adding a core skill affects ALL 8 profiles | Regeneration script (Section 7.3) updates all profiles mechanically; byte-for-byte tests catch any mismatch |
| Content tests too brittle | Use `toContain()` for substring checks and `toMatch()` for regex patterns; test semantic presence, not formatting |
| Dual copy inconsistency (RULE-001) | Dedicated consistency tests (UT-59 through UT-63) verify both copies have equivalent content |
| SKILL_GROUPS["dev"] count drift | Explicit count assertion (UT-65) catches unintended additions/removals |
| KP paths in SKILL.md reference non-existent files | Content tests validate path patterns; runtime SKILL.md instructs subagent to check existence before reading |
| Missing GitHub template registration | UT-64 validates SKILL_GROUPS registration; IT-2 (noMissingFiles) catches missing output |
| Template placeholder resolution failure | Byte-for-byte golden file tests verify resolved output matches expected content per profile |

---

## 12. Files Summary

### 12.1 New Files

| # | File | Description |
|---|------|-------------|
| 1 | `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md` | Claude Code source template (RULE-002) |
| 2 | `resources/github-skills-templates/dev/x-dev-architecture-plan.md` | GitHub Copilot source template (RULE-002) |
| 3 | `tests/node/content/x-dev-architecture-plan-content.test.ts` | Content validation tests (63 tests) |
| 4-27 | `tests/golden/{8 profiles}/{.claude,.agents,.github}/skills/x-dev-architecture-plan/SKILL.md` | 24 golden files |

### 12.2 Modified Files

| # | File | Description |
|---|------|-------------|
| 1 | `src/assembler/github-skills-assembler.ts` | Add to `SKILL_GROUPS["dev"]` |
| 2 | `tests/node/assembler/github-skills-assembler.test.ts` | Add UT-64, UT-65 |
| 3-N | Various existing golden files | Skill count and listing updates in CLAUDE.md, README, etc. |

### 12.3 Existing Test Files (unchanged, covering this story)

| File | Test Count | Coverage |
|------|-----------|----------|
| `tests/node/integration/byte-for-byte.test.ts` | 40 (8 profiles x 5 assertions) | Golden file parity |
| `tests/node/assembler/skills-assembler.test.ts` | ~30 | Claude copy mechanism (auto-discovery) |
| `tests/node/assembler/codex-skills-assembler.test.ts` | ~15 | Agents copy mechanism |

---

## 13. Test Count Summary

| Category | New Tests | Existing Tests |
|----------|-----------|----------------|
| Content validation — Claude source (UT-1 to UT-47) | 47 | 0 |
| Content validation — GitHub source (UT-48 to UT-58) | 11 | 0 |
| Dual copy consistency (UT-59 to UT-63) | 5 | 0 |
| Assembler registration (UT-64 to UT-65) | 2 | 0 |
| Golden file integration (IT-1 to IT-3) | 0 | 40 |
| Assembler unit tests (existing) | 0 | ~60 |
| **Total** | **65** | **~100** |
