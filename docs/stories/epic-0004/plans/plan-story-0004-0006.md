# Implementation Plan — story-0004-0006: New Skill `x-dev-architecture-plan`

**Story:** story-0004-0006
**Epic:** EPIC-0004 (Feature Lifecycle Evolution)
**Phase:** Phase 1 — Core + Extensions
**Blocked By:** story-0004-0001 (ADR Template), story-0004-0002 (Service Architecture Template)
**Blocks:** story-0004-0013, story-0004-0014, story-0004-0015, story-0004-0016

---

## 1. Affected Layers and Components

This story creates a **new template-only skill** — no application logic changes are needed in assemblers. The `SkillsAssembler.selectCoreSkills()` auto-discovers directories under `resources/skills-templates/core/`, so creating the directory is sufficient for the Claude Code copy. The `GithubSkillsAssembler` requires explicit registration in the `SKILL_GROUPS` constant.

| Layer | Component | Change Type |
|-------|-----------|-------------|
| Resources (templates) | `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md` | **CREATE** |
| Resources (templates) | `resources/github-skills-templates/dev/x-dev-architecture-plan.md` | **CREATE** |
| Application (assembler) | `src/assembler/github-skills-assembler.ts` | **MODIFY** — add to `SKILL_GROUPS["dev"]` |
| Tests (unit) | `tests/node/assembler/github-skills-assembler.test.ts` | **MODIFY** — update group count expectations |
| Tests (golden) | `tests/golden/{all 8 profiles}/.claude/skills/x-dev-architecture-plan/SKILL.md` | **CREATE** |
| Tests (golden) | `tests/golden/{all 8 profiles}/.github/skills/x-dev-architecture-plan/SKILL.md` | **CREATE** |
| Tests (golden) | Various existing golden files (CLAUDE.md, README, AGENTS.md, etc.) | **MODIFY** — skill count and listing updates |

---

## 2. New Files to Create

### 2.1 Claude Code Skill Template

**Path:** `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`

This is the primary deliverable. The `SkillsAssembler` auto-discovers it because `selectCoreSkills()` scans `resources/skills-templates/core/*/` directories (see `skills-assembler.ts:38-59`). No code change needed for Claude Code discovery.

**SKILL.md Content Structure** (derived from story requirements in sections 3-5):

```
---
name: x-dev-architecture-plan
description: >
  Generates architecture documentation with diagrams, ADRs, NFRs, and
  resilience/observability strategies. Reads all relevant knowledge packs
  and produces a structured architecture plan following the service
  architecture template. Use before implementation for design validation.
allowed-tools:
  - Read
  - Write
  - Edit
  - Bash
  - Grep
  - Glob
argument-hint: "[STORY-ID or feature-name]"
---

## Global Output Policy
(English ONLY, Technical/Direct/Concise)

# Skill: Architecture Plan (Orchestrator)

## When to Use (Decision Tree)
- Full Plan criteria (new service, new integration, contract change, infra change)
- Simplified Plan criteria (new feature in existing service, no contract change)
- Skip criteria (bug fix, internal refactoring, documentation change)
- Mermaid decision tree diagram

## Knowledge Packs
- architecture/references/architecture-principles.md
- architecture/references/architecture-patterns.md
- protocols/references/ (REST, gRPC, GraphQL, event-driven)
- security/references/ (OWASP, headers, secrets)
- observability/references/ (tracing, metrics, logging)
- infrastructure/references/ (Docker, K8s, 12-factor)
- resilience/references/ (circuit breaker, retry, fallback)
- compliance/references/ (if compliance active)

## Output Structure
(12 mandatory/optional sections per data contract in story section 5)
- Component Diagram (Mermaid graph TD)
- Sequence Diagrams (Mermaid sequenceDiagram)
- Deployment Diagram (Mermaid graph TD)
- External Connections (table)
- Architecture Decisions (Mini-ADR format)
- Technology Stack (table with rationale)
- NFRs (table with targets)
- Data Model (optional, Mermaid ER)
- Observability Strategy
- Resilience Strategy
- Impact Analysis

## Mini-ADR Format
- Simplified inline format: Context, Decision, Rationale, Story-Ref
- Based on _TEMPLATE-ADR.md (story-0004-0001)

## Subagent Prompt
- Architect persona subagent via Task tool
- Reads all KPs listed above
- Evaluates decision tree
- Generates output following template

## Integration Notes
- Standalone: /x-dev-architecture-plan [STORY-ID]
- Via lifecycle: Phase 1 of x-dev-lifecycle
- Output path: docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md
```

### 2.2 GitHub Copilot Skill Template

**Path:** `resources/github-skills-templates/dev/x-dev-architecture-plan.md`

Follows the same pattern as `resources/github-skills-templates/dev/x-dev-implement.md` — a simplified version referencing the full Claude Code skill for detailed guidance. Uses `{{PLACEHOLDER}}` template variables resolved by `TemplateEngine`.

### 2.3 Golden Files (8 Profiles)

For each of the 8 profiles, two new golden files must be created:

| Profile | Claude Code Golden | GitHub Golden |
|---------|-------------------|---------------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/go-gin/.github/skills/x-dev-architecture-plan/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/java-quarkus/.github/skills/x-dev-architecture-plan/SKILL.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/java-spring/.github/skills/x-dev-architecture-plan/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/kotlin-ktor/.github/skills/x-dev-architecture-plan/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/python-click-cli/.github/skills/x-dev-architecture-plan/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/python-fastapi/.github/skills/x-dev-architecture-plan/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/rust-axum/.github/skills/x-dev-architecture-plan/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-dev-architecture-plan/SKILL.md` | `tests/golden/typescript-nestjs/.github/skills/x-dev-architecture-plan/SKILL.md` |

**Total new golden files: 16** (2 per profile x 8 profiles)

Additionally, existing golden files that list skills (CLAUDE.md, AGENTS.md, README-type files) will need content updates to include the new skill in their listings.

---

## 3. Existing Files to Modify

### 3.1 `src/assembler/github-skills-assembler.ts`

**Change:** Add `"x-dev-architecture-plan"` to `SKILL_GROUPS["dev"]` array.

**Current** (line 28-30):
```typescript
"dev": [
    "x-dev-implement", "x-dev-lifecycle", "layer-templates",
],
```

**After:**
```typescript
"dev": [
    "x-dev-implement", "x-dev-lifecycle", "x-dev-architecture-plan",
    "layer-templates",
],
```

**Rationale:** The `GithubSkillsAssembler` does NOT auto-discover skills — it requires explicit registration in `SKILL_GROUPS`. Placement after `x-dev-lifecycle` keeps alphabetical grouping among `x-dev-*` skills, with `layer-templates` last as a non-invocable knowledge pack.

### 3.2 `tests/node/assembler/github-skills-assembler.test.ts`

**Change:** Update any test assertions that check the count of skills in the `"dev"` group (currently 3, will become 4). Review test cases that iterate over `SKILL_GROUPS["dev"]` to ensure they account for the new entry.

### 3.3 Golden Files — Content Updates

Existing golden files that aggregate skill listings will need modification:
- `CLAUDE.md` golden files (skill table with counts)
- `AGENTS.md` golden files (if they list available skills)
- Any `.claude/README.md` or `.github/copilot-instructions.md` that enumerate skills

The exact set of affected files is determined by running the pipeline and comparing with the byte-for-byte test. The recommended approach is:
1. Create the template files
2. Register in `SKILL_GROUPS`
3. Run the pipeline for each profile
4. Regenerate golden files from pipeline output

---

## 4. Dependency Direction Validation

This change is template-only and assembler-registration. No new runtime dependencies are introduced.

| Component | Depends On | Direction |
|-----------|-----------|-----------|
| `x-dev-architecture-plan/SKILL.md` (template) | Template engine placeholders (`{{PLACEHOLDER}}`) | Template resolved at generation time |
| `github-skills-assembler.ts` | No new imports — only a string added to `SKILL_GROUPS` constant | N/A |
| Golden files | Pipeline output | Test artifacts, not runtime dependencies |

No dependency direction concerns. The architecture layer rules (domain never imports adapter) are not applicable since this is a template resource, not application code.

---

## 5. Integration Points

### 5.1 SkillsAssembler (Claude Code) — Auto-Discovery

`SkillsAssembler.selectCoreSkills()` (`skills-assembler.ts:38-59`) scans `resources/skills-templates/core/*/` and returns directory names. Creating `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md` is sufficient — no code change needed. The `assembleCore()` method calls `copyCoreSkill()` which recursively copies the directory tree with template variable resolution.

### 5.2 GithubSkillsAssembler — Explicit Registration

`GithubSkillsAssembler.assemble()` (`github-skills-assembler.ts:71-89`) iterates over `SKILL_GROUPS` entries. The `"dev"` group maps to `resources/github-skills-templates/dev/` directory. Adding `"x-dev-architecture-plan"` to the array causes `renderSkill()` to look for `resources/github-skills-templates/dev/x-dev-architecture-plan.md`, render it with `TemplateEngine.replacePlaceholders()`, and write to `output/skills/x-dev-architecture-plan/SKILL.md`.

### 5.3 Byte-for-Byte Integration Test

The `byte-for-byte.test.ts` runs `runPipeline()` for all 8 profiles and compares output against golden files. After adding the new skill:
1. Pipeline output will include the new skill files
2. Golden files must be regenerated to match
3. The `verifyOutput()` function checks for missing files, extra files, and content mismatches

### 5.4 CLAUDE.md / README Generation

The `ClaudeMdAssembler` (or equivalent) generates the CLAUDE.md file listing all skills with their descriptions. The new skill will appear automatically in the skills table because it reads from the assembled output. Golden files for CLAUDE.md must be regenerated.

---

## 6. SKILL.md Content Structure — Detailed Specification

Based on the story data contract (section 5) and acceptance criteria (section 7):

### 6.1 YAML Frontmatter (Mandatory)

| Field | Value | Source |
|-------|-------|--------|
| `name` | `x-dev-architecture-plan` | Story section 5 |
| `description` | Multi-line description covering purpose | Story section 3 |
| `allowed-tools` | `Read, Write, Edit, Bash, Grep, Glob` | Story section 5 |
| `argument-hint` | `"[STORY-ID or feature-name]"` | Story section 5 |

Must NOT contain `user-invocable: false` (acceptance criterion: Cenario 6).

### 6.2 Decision Tree Section (`## When to Use`)

Three outcomes with clear criteria:

| Outcome | Criteria |
|---------|----------|
| **Full Architecture Plan** | New service, new integration, public contract change, infrastructure change |
| **Simplified Plan** | New feature in existing service without contract change |
| **Skip** | Bug fix, internal refactoring, documentation-only change |

Must include Mermaid `graph TD` diagram from story section 6.1.

### 6.3 Knowledge Packs Section (`## Knowledge Packs`)

Minimum 6 KPs with relative paths (acceptance criterion: Cenario 3):

1. `skills/architecture/references/architecture-principles.md`
2. `skills/architecture/references/architecture-patterns.md`
3. `skills/protocols/references/` (all protocol conventions)
4. `skills/security/references/` (OWASP, headers, secrets)
5. `skills/observability/references/` (tracing, metrics, logging)
6. `skills/infrastructure/references/` (Docker, K8s, 12-factor)
7. `skills/resilience/references/` (CB, retry, fallback)
8. `skills/compliance/references/` (conditional — if compliance active)

### 6.4 Output Structure Section (`## Output Structure`)

Minimum 10 mandatory sections (acceptance criterion: Cenario 4):

1. `# Architecture Plan` — title with story/feature reference
2. `## Component Diagram` — Mermaid `graph TD`
3. `## Sequence Diagrams` — Mermaid `sequenceDiagram` for main flows
4. `## Deployment Diagram` — Mermaid `graph TD` with infra nodes
5. `## External Connections` — table (System, Protocol, Purpose, SLO)
6. `## Architecture Decisions` — Mini-ADR format
7. `## Technology Stack` — table (Component, Technology, Rationale)
8. `## NFRs` — table (Metric, Target, Measurement)
9. `## Data Model` — optional, Mermaid ER or table
10. `## Observability Strategy` — metrics, spans, alerts
11. `## Resilience Strategy` — CB, retry, fallback, degradation
12. `## Impact Analysis` — affected services and risks

### 6.5 Mini-ADR Format Section (`## Mini-ADR Format`)

Simplified inline format (acceptance criterion: Cenario 5):

```markdown
### ADR-NNN: [Title]

**Context:** [Why this decision is needed]
**Decision:** [What was decided]
**Rationale:** [Why this option over alternatives]
**Story-Ref:** [STORY-ID or EPIC-ID]
```

### 6.6 Subagent Prompt Section (`## Subagent Prompt`)

A complete prompt for the Architect persona subagent (launched via Task tool), following the pattern established by `x-dev-implement`:

- Persona: Senior Architect
- Step 1: Read story/requirements
- Step 2: Evaluate decision tree (Full/Simplified/Skip)
- Step 3: Read all applicable KPs (based on decision tree outcome)
- Step 4: Generate architecture plan following output structure
- Step 5: Write output to `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`

---

## 7. Risk Assessment

### 7.1 Blocking Dependencies — Templates Do Not Exist Yet

| Dependency | Status | Risk | Mitigation |
|------------|--------|------|------------|
| `_TEMPLATE-SERVICE-ARCHITECTURE.md` (story-0004-0002) | **Does not exist** | HIGH — The skill references this template for output structure | The SKILL.md can embed the output structure inline (sections 6.4 above) rather than `{% include %}`. The template from 0002 will be a standalone file; the skill defines its own expected output format. |
| `_TEMPLATE-ADR.md` (story-0004-0001) | **Does not exist** | MEDIUM — The mini-ADR format is a simplified version | The SKILL.md defines its own mini-ADR format inline (section 6.5). The full ADR template from 0001 is for standalone ADR documents, not inline architecture plan ADRs. |

**Recommended approach:** Implement stories 0001 and 0002 first (as the dependency graph requires), then implement 0006. If implementing 0006 before its blockers are complete, the SKILL.md must be self-contained — define the output structure and mini-ADR format inline without referencing external template files. Once 0001 and 0002 are done, the SKILL.md can reference them for consistency but should remain functional without them.

### 7.2 Golden File Cascade

Adding a new core skill affects ALL 8 profiles' golden files. The `byte-for-byte.test.ts` will fail until ALL golden files are regenerated. This is a known pattern (every new skill triggers this).

**Mitigation:** Use the pipeline to regenerate golden files:
```bash
npx tsx src/cli.ts generate --config resources/config-templates/setup-config.{profile}.yaml --output tests/golden/{profile}/
```

### 7.3 Knowledge Pack Path Accuracy

The SKILL.md lists KP paths that must match real paths in the generated output. If any KP referenced does not exist for a given profile (e.g., compliance KP when compliance is not active), the skill must handle this gracefully.

**Mitigation:** The skill prompt should instruct the subagent to check if each KP exists before reading, and skip non-existent KPs with a note.

### 7.4 RULE-001 Dual Copy Consistency

Both `.claude/skills/x-dev-architecture-plan/SKILL.md` (Claude Code) and `.github/skills/x-dev-architecture-plan/SKILL.md` (GitHub Copilot) must be generated. These are different files from different templates:
- Claude Code: from `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`
- GitHub Copilot: from `resources/github-skills-templates/dev/x-dev-architecture-plan.md`

Both must convey the same skill behavior, but the GitHub version is typically a simplified reference pointing to the Claude Code version for full details.

---

## 8. Implementation Order (TDD)

Following the project's inner-layers-first convention and TDD workflow:

### Phase A: Template Creation (RED)
1. Write golden file tests expectations for the new skill (byte-for-byte will fail = RED)
2. Create `resources/skills-templates/core/x-dev-architecture-plan/SKILL.md`
3. Create `resources/github-skills-templates/dev/x-dev-architecture-plan.md`

### Phase B: Registration (GREEN)
4. Add `"x-dev-architecture-plan"` to `SKILL_GROUPS["dev"]` in `github-skills-assembler.ts`
5. Update unit test expectations in `github-skills-assembler.test.ts`
6. Run pipeline for all 8 profiles and regenerate golden files
7. Verify byte-for-byte tests pass (GREEN)

### Phase C: Validation (REFACTOR)
8. Review SKILL.md content against all 6 acceptance criteria (Gherkin scenarios)
9. Verify frontmatter fields match data contract
10. Verify decision tree has 3 outcomes
11. Verify KP list has >= 6 entries with correct paths
12. Verify output structure has >= 10 sections
13. Verify mini-ADR format has Context/Decision/Rationale/Story-Ref
14. Verify skill is user-invocable (no `user-invocable: false`)

---

## 9. Estimated File Count

| Category | Files | Action |
|----------|-------|--------|
| New template files | 2 | CREATE |
| New golden files | 16 | CREATE (2 per profile x 8 profiles) |
| Modified source files | 1 | `github-skills-assembler.ts` |
| Modified test files | 1 | `github-skills-assembler.test.ts` |
| Modified golden files | ~24-40 | CLAUDE.md, AGENTS.md, README per profile (estimated) |
| **Total** | **~44-60** | |
