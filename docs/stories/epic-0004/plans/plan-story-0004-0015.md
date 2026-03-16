# Implementation Plan — story-0004-0015: ADR Automation

**Story:** ADR Automation — Geração e Indexação Automática
**Dependencies:** story-0004-0001 (ADR Template), story-0004-0006 (Architecture Plan Skill)

---

## 1. Affected Layers and Components

This is a **CLI library** project (not hexagonal). The codebase has three logical layers:

| Layer | Role | Affected? |
|-------|------|-----------|
| `src/domain/` | Pure logic, mappings, registry | Yes — new ADR domain logic |
| `src/assembler/` | File I/O assemblers + pipeline orchestrator | Yes — new `AdrAssembler` |
| `resources/` | Templates, skill files (source of truth) | Yes — ADR template + skill template |
| `tests/` | Unit + integration + golden file tests | Yes — new test files |

**Key insight:** Story-0004-0015 is a **skill** (invoked by Claude/agents at runtime), NOT a pipeline assembler that runs during `ia-dev-env generate`. The ADR automation logic is a **SKILL.md template** that gets copied into `.claude/skills/` during generation. The runtime logic (extract mini-ADRs, expand, number, index) executes inside the AI agent context, not as TypeScript code.

However, there are two distinct parts:
1. **Skill template** (`resources/skills-templates/core/x-dev-adr-automation/SKILL.md`) — prompt/instructions for the AI agent
2. **Pipeline integration** — the skill template must be picked up by `SkillsAssembler` (already handles core skills automatically via directory scanning)
3. **ADR index template** — `resources/templates/_TEMPLATE-ADR-INDEX.md` for the generated `docs/adr/README.md`

---

## 2. New Classes/Interfaces to Create

### 2.1 Resource Templates (Source of Truth per RULE-002)

| File | Purpose |
|------|---------|
| `resources/skills-templates/core/x-dev-adr-automation/SKILL.md` | Skill definition with frontmatter + complete prompt for ADR automation agent |

**Note:** The `_TEMPLATE-ADR.md` and `docs/adr/README.md` template are prerequisites from story-0004-0001. This plan assumes they exist.

### 2.2 SKILL.md Content Structure

The SKILL.md must contain:

```yaml
---
name: x-dev-adr-automation
description: "Automates ADR generation from architecture plan mini-ADRs: extracts inline decisions, expands to full ADR format, assigns sequential numbering, updates the ADR index, and adds cross-references."
allowed-tools:
  - Read
  - Write
  - Edit
  - Bash
  - Grep
  - Glob
argument-hint: "[architecture-plan-path] [story-id]"
---
```

Followed by markdown sections:
- `## When to Use` — Decision tree (after architecture plan, when mini-ADRs exist)
- `## Input Format` — Mini-ADR structure (title, context, decision, rationale)
- `## Output Format` — Full ADR structure with frontmatter
- `## Algorithm` — Step-by-step instructions for the agent:
  1. Read architecture plan, extract mini-ADRs
  2. Scan `docs/adr/` for existing ADRs, find max number
  3. For each mini-ADR: check duplicates by title similarity
  4. Expand to full ADR using `_TEMPLATE-ADR.md`
  5. Write `docs/adr/ADR-NNNN-title-kebab.md`
  6. Update `docs/adr/README.md` index table
  7. Add cross-references (story-ref in ADR, ADR links in plan)
- `## Duplicate Detection` — Title similarity rules
- `## Cross-Reference Rules` — How to link story to ADR and back
- `## Examples` — Before/after examples of mini-ADR to full ADR conversion

### 2.3 No New TypeScript Source Files Required

The `SkillsAssembler` already auto-discovers core skills by scanning `resources/skills-templates/core/*/`. Placing `x-dev-adr-automation/SKILL.md` in the core directory is sufficient. No new assembler, no new domain class, no pipeline modification needed.

**Verification:** In `skills-assembler.ts`, `selectCoreSkills()` iterates `resources/skills-templates/core/` entries and includes any directory as a skill. The new `x-dev-adr-automation/` directory will be automatically picked up.

---

## 3. Existing Classes to Modify

| File | Change | Reason |
|------|--------|--------|
| None | — | `SkillsAssembler.selectCoreSkills()` auto-discovers core skills by directory scan |

**No source code modifications required.** The existing pipeline handles this automatically:
- `SkillsAssembler` scans `resources/skills-templates/core/` directories
- `copyTemplateTree` copies the skill directory with placeholder replacement
- `ReadmeAssembler` counts and lists skills in the generated README.md
- Golden file tests for all 8 profiles will need updates (new skill in output)

---

## 4. Dependency Direction Validation

```
resources/skills-templates/core/x-dev-adr-automation/SKILL.md  (template, source of truth)
    |
    v  [copied by SkillsAssembler at generate-time]
    |
output/.claude/skills/x-dev-adr-automation/SKILL.md  (generated output)
    |
    v  [loaded by Claude/agent at runtime]
    |
AI agent executes ADR automation instructions
```

- No circular dependencies introduced
- Template depends on nothing (it is a static resource)
- SkillsAssembler depends on resources (existing pattern)
- Output depends on pipeline (existing pattern)

---

## 5. Integration Points

### 5.1 With story-0004-0001 (ADR Template)

The SKILL.md references:
- `_TEMPLATE-ADR.md` — the template used to expand mini-ADRs into full ADRs
- `docs/adr/README.md` — the index template to update

The skill instructions must reference the correct path where the agent will find the template at runtime (in the generated project's `docs/adr/` structure).

### 5.2 With story-0004-0006 (Architecture Plan Skill)

The SKILL.md defines the mini-ADR extraction format, which must align with the mini-ADR format produced by `x-dev-architecture-plan`. The mini-ADR format from story-0004-0006 spec:

```markdown
### ADR: [Title]
- **Context:** ...
- **Decision:** ...
- **Rationale:** ...
```

### 5.3 With `x-dev-lifecycle` (if invoked from lifecycle)

The skill can be invoked standalone (`/x-dev-adr-automation`) or by the lifecycle orchestrator after the architecture plan phase. The SKILL.md must document both invocation paths.

### 5.4 Dual Copy Consistency (RULE-001)

The skill must be generated in both:
- `.claude/skills/x-dev-adr-automation/SKILL.md`
- `.github/skills/x-dev-adr-automation/SKILL.md`

The `GithubSkillsAssembler` already mirrors core skills to `.github/skills/`. No additional code needed.

---

## 6. Database Changes

None. This is a CLI tool that generates files. No database involved.

---

## 7. API Changes

None. No HTTP/REST/gRPC interfaces. The only "API" is the skill invocation via `/x-dev-adr-automation [architecture-plan-path] [story-id]`.

---

## 8. Event Changes

None. No event-driven architecture in this project.

---

## 9. Configuration Changes

No changes to `ProjectConfig`, `setup-config.*.yaml`, or any configuration model. The skill is unconditionally included as a core skill (like `x-dev-implement`, `x-dev-lifecycle`, etc.).

---

## 10. Risk Assessment

### Low Risk
- **Auto-discovery works:** `SkillsAssembler.selectCoreSkills()` scans directories — adding a new directory is zero-code-change.
- **Dual copy handled:** `GithubSkillsAssembler` already mirrors all core skills.
- **Template engine handles placeholders:** `{project_name}` etc. in SKILL.md will be replaced automatically.

### Medium Risk
- **Golden file updates for all 8 profiles:** Adding a new core skill changes the output for every profile. All 8 golden file directories must be regenerated. The `ReadmeAssembler` output also changes (skill count, skill table).
- **Mini-ADR format alignment with story-0004-0006:** If story-0004-0006 is not yet implemented, the mini-ADR extraction format in this skill is speculative. Mitigation: define the format in this skill and have story-0004-0006 conform to it.
- **Story-0004-0001 dependency:** The skill references `_TEMPLATE-ADR.md` which must exist. If not yet implemented, the skill can still be generated but its runtime instructions would reference a non-existent template.

### Low-Medium Risk
- **Skill content quality:** The SKILL.md is a prompt for an AI agent. The quality of ADR generation depends on prompt engineering, which is hard to test with golden file tests. Golden files validate the skill is generated correctly; they cannot validate the agent's runtime behavior.

---

## 11. Implementation Tasks (Ordered)

### Phase 1: Template Creation
1. Create `resources/skills-templates/core/x-dev-adr-automation/SKILL.md` with:
   - YAML frontmatter (name, description, allowed-tools, argument-hint)
   - When to Use section with decision tree
   - Input Format section (mini-ADR structure from architecture plan)
   - Output Format section (full ADR with frontmatter)
   - Algorithm section (step-by-step agent instructions)
   - Sequential Numbering section (scan docs/adr/, find max, increment)
   - Duplicate Detection section (title similarity check)
   - Cross-Reference section (story-ref in ADR, ADR links in plan)
   - Index Update section (append row to docs/adr/README.md table)
   - Examples section (before/after mini-ADR to full ADR)

### Phase 2: Placeholder Integration
2. Add `{project_name}` placeholders where the generated skill needs project-specific values
3. Verify `TemplateEngine.replacePlaceholders()` handles all used placeholders

### Phase 3: Golden File Updates
4. Regenerate golden files for all 8 profiles:
   - `tests/golden/go-gin/`
   - `tests/golden/java-quarkus/`
   - `tests/golden/java-spring/`
   - `tests/golden/kotlin-ktor/`
   - `tests/golden/python-click-cli/`
   - `tests/golden/python-fastapi/`
   - `tests/golden/rust-axum/`
   - `tests/golden/typescript-nestjs/`

### Phase 4: Unit Tests
5. Create `tests/node/content/x-dev-adr-automation-content.test.ts`:
   - Verify SKILL.md contains required frontmatter fields
   - Verify SKILL.md contains all required sections (When to Use, Input Format, Output Format, Algorithm, etc.)
   - Verify mini-ADR extraction format is documented
   - Verify duplicate detection rules are present
   - Verify cross-reference rules are present
   - Verify sequential numbering algorithm is described

### Phase 5: Integration Tests
6. Verify `SkillsAssembler` picks up the new skill (existing golden file tests cover this)
7. Verify the skill appears in the generated README.md skill table
8. Verify dual copy in `.github/skills/`

---

## 12. Test Strategy

| Test Type | What | File |
|-----------|------|------|
| Unit (content) | SKILL.md structure, sections, frontmatter | `tests/node/content/x-dev-adr-automation-content.test.ts` |
| Integration (golden) | Skill present in all 8 profile outputs | `tests/node/integration/byte-for-byte.test.ts` (existing) |
| Integration (assembly) | `SkillsAssembler` includes new skill | `tests/node/assembler/skills-assembler.test.ts` (existing, auto-covered) |

**Coverage impact:** Minimal new TypeScript code (just the template file). The coverage threshold concern is about the test file itself, not the template. Existing coverage remains above 99%.

---

## 13. File Inventory

### New Files (2)
1. `resources/skills-templates/core/x-dev-adr-automation/SKILL.md` — Skill template
2. `tests/node/content/x-dev-adr-automation-content.test.ts` — Content validation tests

### Modified Files (8 golden directories)
3-10. `tests/golden/{profile}/.claude/skills/x-dev-adr-automation/SKILL.md` — One per profile
11-18. `tests/golden/{profile}/.claude/README.md` — Updated skill count and table
19-26. `tests/golden/{profile}/.github/skills/x-dev-adr-automation/SKILL.md` — GitHub dual copy

**Estimated total: ~26 files changed** (mostly golden file regeneration).

---

## 14. Acceptance Criteria Mapping

| Gherkin Scenario | Implementation |
|-----------------|----------------|
| Mini-ADR converted with sequential numbering | SKILL.md Algorithm section + Numbering section |
| ADR index auto-updated | SKILL.md Index Update section |
| Cross-reference story to ADR | SKILL.md Cross-Reference section |
| Duplicate detected and skipped | SKILL.md Duplicate Detection section |
| Empty docs/adr/ directory handled | SKILL.md Algorithm section (start from ADR-0001) |
| Architecture plan updated with ADR links | SKILL.md Cross-Reference section |

All Gherkin scenarios map to prompt instructions in the SKILL.md, not to TypeScript logic. The skill is an AI agent directive, not compiled code.
