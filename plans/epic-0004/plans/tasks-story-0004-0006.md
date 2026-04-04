# Task Breakdown -- story-0004-0006: New Skill `x-dev-architecture-plan`

## Overview

This story creates a new skill `x-dev-architecture-plan` with templates for both Claude Code and GitHub Copilot, registers it in the GitHub skills assembler, and regenerates golden files for all 8 profiles. No application logic changes beyond a single string addition to `SKILL_GROUPS["dev"]`.

**Total files: 2 source templates + 1 assembler modification + 1 test modification + ~40-60 golden files = ~44-64 files.**

---

## G1: Claude Code Skill Template -- `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`

### TASK-G1-01: Create SKILL.md with YAML frontmatter

**File:** `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`
**Change type:** CREATE
**TDD Phase:** RED -- Write test asserting SKILL.md exists and contains valid frontmatter fields

Create the SKILL.md file with YAML frontmatter containing all mandatory fields from the data contract (story section 5):

| Field | Value |
|-------|-------|
| `name` | `x-dev-architecture-plan` |
| `description` | Multi-line description covering architecture documentation generation |
| `allowed-tools` | `Read, Write, Edit, Bash, Grep, Glob` |
| `argument-hint` | `"[STORY-ID or feature-name]"` |

Must NOT contain `user-invocable: false` (acceptance criterion: Cenario 6).

Add Global Output Policy section (English ONLY, Technical/Direct/Concise) following the pattern in `x-dev-implement/SKILL.md`.

**Validation:** Frontmatter parses as valid YAML. Fields match data contract exactly.

---

### TASK-G1-02: Add Decision Tree section (`## When to Use`)

**File:** `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`
**Section:** `## When to Use`
**Change type:** Additive (content within file created in G1-01)
**TDD Phase:** RED -- Write test asserting section exists with 3 outcomes

Add the decision tree with three outcomes and clear criteria:

| Outcome | Criteria |
|---------|----------|
| **Full Architecture Plan** | New service, new integration, public contract change, infrastructure change |
| **Simplified Plan** | New feature in existing service without contract change |
| **Skip** | Bug fix, internal refactoring, documentation-only change |

Include Mermaid `graph TD` diagram from story section 6.1.

**Validation:** Section contains all 3 outcomes. Mermaid diagram is syntactically valid.

---

### TASK-G1-03: Add Knowledge Packs section (`## Knowledge Packs`)

**File:** `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`
**Section:** `## Knowledge Packs`
**Change type:** Additive
**TDD Phase:** RED -- Write test asserting >= 6 KPs with relative paths

List all knowledge packs with relative paths and reading order:

1. `skills/architecture/references/architecture-principles.md`
2. `skills/architecture/references/architecture-patterns.md`
3. `skills/protocols/references/` (REST, gRPC, GraphQL, event-driven conventions)
4. `skills/security/references/` (OWASP, headers, secrets management)
5. `skills/observability/references/` (tracing, metrics, logging)
6. `skills/infrastructure/references/` (Docker, K8s, 12-factor)
7. `skills/resilience/references/` (circuit breaker, retry, fallback)
8. `skills/compliance/references/` (conditional -- if compliance active)

Include reading strategy: Full Plan reads all KPs; Simplified Plan reads architecture only; Skip reads none.

**Validation:** At least 6 KPs listed with relative paths.

---

### TASK-G1-04: Add Output Structure section (`## Output Structure`)

**File:** `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`
**Section:** `## Output Structure`
**Change type:** Additive
**TDD Phase:** RED -- Write test asserting >= 10 mandatory sections

Define the architecture plan output structure with all sections from the data contract:

| # | Section | Format | Required |
|---|---------|--------|----------|
| 1 | `# Architecture Plan` | Markdown H1 with story/feature | M |
| 2 | `## Component Diagram` | Mermaid `graph TD` | M |
| 3 | `## Sequence Diagrams` | Mermaid `sequenceDiagram` | M |
| 4 | `## Deployment Diagram` | Mermaid `graph TD` with infra nodes | M |
| 5 | `## External Connections` | Table (System, Protocol, Purpose, SLO) | M |
| 6 | `## Architecture Decisions` | Mini-ADR format | M |
| 7 | `## Technology Stack` | Table (Component, Technology, Rationale) | M |
| 8 | `## NFRs` | Table (Metric, Target, Measurement) | M |
| 9 | `## Data Model` | Mermaid ER or table | O |
| 10 | `## Observability Strategy` | Metrics, spans, alerts | M |
| 11 | `## Resilience Strategy` | CB, retry, fallback, degradation | M |
| 12 | `## Impact Analysis` | Affected services and risks | M |

Specify output path convention: `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`

**Validation:** At least 10 sections listed. Includes Component Diagram, Sequence Diagrams, NFRs, Observability, Resilience.

---

### TASK-G1-05: Add Mini-ADR Format section (`## Mini-ADR Format`)

**File:** `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`
**Section:** `## Mini-ADR Format`
**Change type:** Additive
**TDD Phase:** RED -- Write test asserting format contains Context, Decision, Rationale, Story-Ref

Define the simplified inline ADR format:

```
### ADR-NNN: [Title]

**Context:** [Why this decision is needed]
**Decision:** [What was decided]
**Rationale:** [Why this option over alternatives]
**Story-Ref:** [STORY-ID or EPIC-ID]
```

Add guidance on when to create mini-ADRs: technology selection, pattern choice, integration approach, trade-off resolution.

**Validation:** Format contains all 4 fields: Context, Decision, Rationale, Story-Ref.

---

### TASK-G1-06: Add Subagent Prompt section (`## Subagent Prompt`)

**File:** `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`
**Section:** `## Subagent Prompt`
**Change type:** Additive
**TDD Phase:** GREEN -- Complete the SKILL.md with the subagent prompt

Define the complete prompt for the Architect persona subagent (launched via Task tool), following the pattern in `x-dev-implement/SKILL.md`:

- **Persona:** Senior Architect
- **Step 1:** Read story/requirements and extract scope
- **Step 2:** Evaluate decision tree (Full/Simplified/Skip)
- **Step 3:** Read all applicable KPs based on decision tree outcome
- **Step 4:** Generate architecture plan following output structure (TASK-G1-04)
- **Step 5:** Write output to `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`

Add integration notes:
- Standalone: `/x-dev-architecture-plan [STORY-ID]`
- Via lifecycle: Phase 1 of `x-dev-lifecycle`
- KP existence check: if a KP path does not exist, skip with a note

**Validation:** Subagent prompt references all 5 steps. Persona is Architect.

---

## G2: GitHub Copilot Skill Template -- `resources/github-skills-templates/dev/x-dev-architecture-plan.md`

### TASK-G2-01: Create GitHub Copilot skill template

**File:** `resources/github-skills-templates/dev/x-dev-architecture-plan.md`
**Change type:** CREATE
**TDD Phase:** RED -- Write test asserting GitHub skill template exists
**Parallel:** Yes (independent of G1 tasks)

Create a simplified GitHub Copilot version following the pattern of `resources/github-skills-templates/dev/x-dev-implement.md`:

- YAML frontmatter with `name` and `description` only (no `allowed-tools` or `argument-hint` -- GitHub Copilot convention)
- Same logical structure as the Claude Code SKILL.md but with GitHub-specific path conventions:
  - KP references use `.github/skills/X/SKILL.md` instead of `skills/X/references/Y.md`
- All sections from G1 (When to Use, Knowledge Packs, Output Structure, Mini-ADR Format, Subagent Prompt)
- Uses `{{PLACEHOLDER}}` template variables where applicable (resolved by `TemplateEngine` at generation time)

**Validation:** File exists. Frontmatter contains `name` and `description`. Parity with Claude Code template in logical content.

---

## G3: Assembler Registration -- `src/assembler/github-skills-assembler.ts`

### TASK-G3-01: Add `x-dev-architecture-plan` to SKILL_GROUPS["dev"]

**File:** `src/assembler/github-skills-assembler.ts`
**Change type:** MODIFY (single line addition)
**TDD Phase:** RED -- Write test asserting "x-dev-architecture-plan" is in SKILL_GROUPS["dev"]
**Depends On:** TASK-G2-01 (template must exist for pipeline to succeed)

Current `SKILL_GROUPS["dev"]` (line 28-30):
```typescript
"dev": [
    "x-dev-implement", "x-dev-lifecycle", "layer-templates",
],
```

After:
```typescript
"dev": [
    "x-dev-implement", "x-dev-lifecycle", "x-dev-architecture-plan",
    "layer-templates",
],
```

Placement: after `x-dev-lifecycle` to keep `x-dev-*` skills grouped alphabetically, with `layer-templates` last as a non-invocable knowledge pack.

**Validation:** `SKILL_GROUPS["dev"]` contains 4 entries. `"x-dev-architecture-plan"` is present.

---

### TASK-G3-02: Update unit test expectations for dev group count

**File:** `tests/node/assembler/github-skills-assembler.test.ts`
**Change type:** MODIFY
**TDD Phase:** GREEN -- Update test expectations to match new count
**Depends On:** TASK-G3-01

Review and update any test assertions that check:
- Count of skills in the `"dev"` group (currently 3, will become 4)
- Iteration over `SKILL_GROUPS["dev"]` entries
- Tests that create only partial dev templates (e.g., `assemble_templateFileMissing_skipsSkill` creates only `x-dev-implement` -- this test remains valid, but new tests should verify the 4th skill)

Specific tests to review:
- `SKILL_GROUPS_has8Groups` -- unchanged (still 8 groups)
- `assemble_templateFileMissing_skipsSkill` -- unchanged (still creates only `x-dev-implement`)
- Any test counting total dev group skills -- update from 3 to 4

Add new test:
- `SKILL_GROUPS_devGroup_containsArchitecturePlan` -- assert `"x-dev-architecture-plan"` is in `SKILL_GROUPS["dev"]`

**Validation:** All existing tests pass. New test confirms the skill is registered.

---

## G4: Golden Files -- Regenerate all 8 profiles

### TASK-G4-01: Regenerate golden files via pipeline

**Files:** ~40-60 golden files across 8 profiles (16 new + ~24-40 modified)
**Change type:** CREATE (new skill files) + MODIFY (existing aggregate files)
**TDD Phase:** RED then GREEN
**Depends On:** TASK-G1-01 through G1-06, TASK-G2-01, TASK-G3-01

**New golden files (16 -- 2 per profile x 8 profiles):**

| # | Claude Code Golden | GitHub Copilot Golden |
|---|--------------------|-----------------------|
| 1 | `tests/golden/go-gin/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/go-gin/.github/skills/x-dev-architecture-plan/SKILL.md` |
| 2 | `tests/golden/java-quarkus/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/java-quarkus/.github/skills/x-dev-architecture-plan/SKILL.md` |
| 3 | `tests/golden/java-spring/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/java-spring/.github/skills/x-dev-architecture-plan/SKILL.md` |
| 4 | `tests/golden/kotlin-ktor/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/kotlin-ktor/.github/skills/x-dev-architecture-plan/SKILL.md` |
| 5 | `tests/golden/python-click-cli/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/python-click-cli/.github/skills/x-dev-architecture-plan/SKILL.md` |
| 6 | `tests/golden/python-fastapi/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/python-fastapi/.github/skills/x-dev-architecture-plan/SKILL.md` |
| 7 | `tests/golden/rust-axum/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/rust-axum/.github/skills/x-dev-architecture-plan/SKILL.md` |
| 8 | `tests/golden/typescript-nestjs/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/typescript-nestjs/.github/skills/x-dev-architecture-plan/SKILL.md` |

**Modified golden files (existing aggregate files):**
- `CLAUDE.md` golden files for all 8 profiles (skill table with updated counts and listing)
- `.claude/README.md` golden files (if they enumerate skills)
- `.github/copilot-instructions.md` golden files (if they enumerate skills)
- Any agents-related golden files that list available skills

**Procedure:**
1. Run `npx vitest run tests/node/integration/byte-for-byte.test.ts` -- expect failures (RED)
2. For each profile, run the pipeline and copy output to golden directory:
   ```bash
   npx tsx src/cli.ts generate \
     --config resources/config-templates/setup-config.{profile}.yaml \
     --output tests/golden/{profile}/
   ```
3. Re-run `npx vitest run tests/node/integration/byte-for-byte.test.ts` -- all pass (GREEN)

**Validation:** All 8 profiles pass byte-for-byte parity. No missing files, no extra files, no content mismatches.

---

## G5: Final Verification

### TASK-G5-01: TypeScript compilation check

**Command:** `npx tsc --noEmit`
**Expected result:** Clean compilation
**Purpose:** Verify the single-line change to `github-skills-assembler.ts` compiles without errors

---

### TASK-G5-02: Full test suite with coverage

**Command:** `npx vitest run --coverage`
**Expected result:** All tests passing (existing ~1,384 + new tests from G3-02)
**Coverage targets:** Line >= 95%, Branch >= 90% (expected unchanged at 99.6%/97.84%)
**Purpose:** Verify no regressions across the entire test suite

---

### TASK-G5-03: Byte-for-byte parity validation

**Command:** `npx vitest run tests/node/integration/byte-for-byte.test.ts`
**Expected result:** All 8 profiles pass
**Validation:**
- Each `.claude/skills/x-dev-architecture-plan/SKILL.md` golden file matches pipeline output from `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`
- Each `.github/skills/x-dev-architecture-plan/SKILL.md` golden file matches pipeline output from `resources/github-skills-templates/dev/x-dev-architecture-plan.md`
- All existing golden files (CLAUDE.md, README, agents, etc.) also match after skill count/listing updates

---

### TASK-G5-04: Acceptance criteria validation (Gherkin scenarios)

Manual or automated verification of all 6 acceptance criteria from story section 7:

| Scenario | Validation |
|----------|------------|
| SKILL.md with correct frontmatter | Frontmatter contains `name`, `description`, `allowed-tools`, `argument-hint`. Name is `x-dev-architecture-plan`. |
| Decision tree present and complete | `## When to Use` section has 3 outcomes: Full Plan, Simplified Plan, Skip. |
| Knowledge packs with correct paths | `## Knowledge Packs` section lists >= 6 KPs with relative paths. |
| Output structure follows template | `## Output Structure` section has >= 10 mandatory sections including Component Diagram, Sequence Diagrams, NFRs, Observability, Resilience. |
| Mini-ADR format defined | `## Mini-ADR Format` section has Context, Decision, Rationale, Story-Ref. |
| Skill is user-invocable | Frontmatter does NOT contain `user-invocable: false`. |

---

## Execution Order

```
TASK-G1-01  (create SKILL.md with frontmatter)
    |
    v
TASK-G1-02 through TASK-G1-06  (sequential, same file -- add all sections)
    |                                   |
    v                                   v (parallel with G1)
TASK-G2-01  (GitHub template)     TASK-G3-01  (assembler registration)
    |                                   |
    |                                   v
    |                             TASK-G3-02  (update test expectations)
    |                                   |
    +-----------------------------------+
                    |
                    v
              TASK-G5-01  (compile check)
                    |
                    v
              TASK-G4-01  (regenerate golden files)
                    |
                    v
        TASK-G5-02 + TASK-G5-03 + TASK-G5-04  (full suite + parity + acceptance)
```

**Parallelism notes:**
- G1 tasks are sequential (same file, each builds on the previous)
- G2-01 and G3-01 are independent of each other and can run in parallel
- G2-01 is independent of G1 tasks (different file, different template)
- G3-01 and G3-02 are sequential (G3-02 depends on G3-01)
- G4-01 depends on ALL template and registration tasks being complete
- G5 tasks are final verification, depending on G4-01

---

## Summary

| Group | Tasks | Files | Impact |
|-------|-------|-------|--------|
| G1: Claude Code source template | 6 tasks | 1 file (CREATE) | HIGH (primary deliverable) |
| G2: GitHub Copilot template | 1 task | 1 file (CREATE) | MEDIUM (dual copy parity) |
| G3: Assembler + tests | 2 tasks | 2 files (MODIFY) | LOW (single-line code change + test update) |
| G4: Golden files | 1 task | ~40-60 files (CREATE + MODIFY) | MEDIUM (regeneration, no manual edits) |
| G5: Verification | 4 tasks | 0 files (read-only checks) | LOW (validation only) |
| **Total** | **14 tasks** | **~44-64 files** | |
