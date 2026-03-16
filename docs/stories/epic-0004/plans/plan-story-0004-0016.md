# Implementation Plan -- story-0004-0016

## Security Threat Model Documentation

### 1. Summary

This story creates a STRIDE-based threat model template (`resources/templates/_TEMPLATE-THREAT-MODEL.md`) and the generation infrastructure to produce `docs/security/threat-model.md` from that template. The threat model is a living document updated incrementally by extracting security findings from architecture plan output and security review output, applying severity-based auto-add rules, and maintaining a risk summary with severity counts.

**Nature of the change:** This is a template-only and skill content story. Like most EPIC-0004 stories, it involves creating a new template file in `resources/templates/`, potentially a new skill or skill extension in `resources/skills-templates/core/`, and propagating via dual copy to `resources/github-skills-templates/`. No TypeScript pipeline code or assembler logic changes are required -- the template is a static resource, and the generation logic lives in skill instruction files (AI-interpreted markdown, not compiled code).

**Dependency:** Blocked by story-0004-0006 (`x-dev-architecture-plan`), which provides the architecture plan output format from which security findings are extracted.

---

### 2. Architecture -- How Templates and Skills Flow

```
resources/templates/_TEMPLATE-THREAT-MODEL.md       <-- NEW: Source of truth (template)
        |
        | Not assembled by pipeline (template for AI-driven generation)
        | Referenced by skill instructions and AI agents at runtime
        v
docs/security/threat-model.md                        <-- AI-generated output (per-project)

---

resources/skills-templates/core/x-review/SKILL.md   <-- MODIFIED: Add threat model update step
        |
        | copyTemplateTree (no {{placeholders}} in this file)
        | via: src/assembler/skills-assembler.ts :: assembleCore()
        v
{outputDir}/.claude/skills/x-review/SKILL.md        <-- pipeline output (.claude)
        |
        | mirror
        | via: src/assembler/codex-skills-assembler.ts
        v
{outputDir}/.agents/skills/x-review/SKILL.md        <-- pipeline output (.agents)

---

resources/github-skills-templates/review/x-review.md <-- MODIFIED: Parallel changes
        |
        | renderSkill
        | via: src/assembler/github-skills-assembler.ts
        v
{outputDir}/.github/skills/x-review/SKILL.md        <-- pipeline output (.github)
```

Key properties:
- The `_TEMPLATE-THREAT-MODEL.md` is a static template in `resources/templates/` (RULE-002, RULE-005). It is NOT processed by the pipeline assemblers. It is read by AI skills at runtime to generate per-project threat models.
- The skill modifications (x-review, and potentially x-dev-architecture-plan) are byte-for-byte copied by the pipeline -- no `{{placeholder}}` substitution is needed for this content.
- The `.agents` copy is an exact mirror of the `.claude` copy.
- All 8 profiles produce identical copies for these skills (profile-independent content).

---

### 3. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| Resources (templates) | `resources/templates/_TEMPLATE-THREAT-MODEL.md` | **NEW** -- STRIDE-based threat model template |
| Resources (Claude skills) | `resources/skills-templates/core/x-review/SKILL.md` | **MODIFIED** -- add post-review step: extract findings and update threat model |
| Resources (Claude skills) | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | **MODIFIED** -- reference threat model update in documentation phase |
| Resources (GitHub skills) | `resources/github-skills-templates/review/x-review.md` | **MODIFIED** -- parallel changes for GitHub copy |
| Resources (GitHub skills) | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | **MODIFIED** -- parallel changes for GitHub copy |
| Golden files (.claude) | `tests/golden/{profile}/.claude/skills/x-review/SKILL.md` | **MUST UPDATE** -- 8 files |
| Golden files (.agents) | `tests/golden/{profile}/.agents/skills/x-review/SKILL.md` | **MUST UPDATE** -- 8 files |
| Golden files (.github) | `tests/golden/{profile}/.github/skills/x-review/SKILL.md` | **MUST UPDATE** -- 8 files |
| Golden files (.claude) | `tests/golden/{profile}/.claude/skills/x-dev-lifecycle/SKILL.md` | **MUST UPDATE** -- 8 files (if modified) |
| Golden files (.agents) | `tests/golden/{profile}/.agents/skills/x-dev-lifecycle/SKILL.md` | **MUST UPDATE** -- 8 files (if modified) |
| Golden files (.github) | `tests/golden/{profile}/.github/skills/x-dev-lifecycle/SKILL.md` | **MUST UPDATE** -- 8 files (if modified) |

---

### 4. New Classes/Interfaces to Create (with Package Locations)

No new TypeScript classes or interfaces are needed. This story is entirely template and skill content changes.

| Artifact | Location | Type | Description |
|----------|----------|------|-------------|
| `_TEMPLATE-THREAT-MODEL.md` | `resources/templates/` | Markdown template | STRIDE-based threat model with trust boundaries, risk tables, severity enums, status enums, risk summary, and change history |

#### 4.1 Template Structure: `_TEMPLATE-THREAT-MODEL.md`

The template MUST contain these sections per the story data contract:

```
# Threat Model -- {{SERVICE_NAME}}

## Trust Boundaries
  (Mermaid diagram with External/DMZ/Internal subgraphs)

## STRIDE Analysis

### Spoofing
  | Threat | Severity | Mitigation | Status | Story Ref |

### Tampering
  | Threat | Severity | Mitigation | Status | Story Ref |

### Repudiation
  | Threat | Severity | Mitigation | Status | Story Ref |

### Information Disclosure
  | Threat | Severity | Mitigation | Status | Story Ref |

### Denial of Service
  | Threat | Severity | Mitigation | Status | Story Ref |

### Elevation of Privilege
  | Threat | Severity | Mitigation | Status | Story Ref |

## Risk Summary
  | Severity | Count |
  Critical, High, Medium, Low, Total

## Change History
  | Date | Story | Threats Added/Updated |
```

Severity enum: `Critical`, `High`, `Medium`, `Low`
Status enum: `Open`, `Mitigated`, `Accepted`, `Under Review`

---

### 5. Existing Classes to Modify

No TypeScript source files are modified. Only skill instruction files (markdown) are changed.

| File | Change |
|------|--------|
| `resources/skills-templates/core/x-review/SKILL.md` | Add a new step after the parallel review consolidation: "Extract security findings and update threat model". Define severity-based auto-add rules (Critical/High -> Open, Medium -> Under Review). Reference `_TEMPLATE-THREAT-MODEL.md` for format. |
| `resources/github-skills-templates/review/x-review.md` | Parallel change for GitHub Copilot -- same threat model update instructions with GitHub-specific paths |
| `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Add threat model update reference in the documentation phase. After architecture plan is generated, extract security-relevant findings and feed them into the threat model. |
| `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Parallel change for GitHub Copilot |

---

### 6. Dependency Direction Validation

```
resources/templates/_TEMPLATE-THREAT-MODEL.md   (static resource, no dependencies)
        |
        | referenced at runtime by AI skills
        v
resources/skills-templates/core/x-review/SKILL.md     (skill instructions)
resources/skills-templates/core/x-dev-lifecycle/SKILL.md (skill instructions)
        |
        | copyTemplateTree (pipeline assembly)
        v
{outputDir}/.claude/skills/*/SKILL.md                 (generated output)
```

**Validation:** All dependencies point in the correct direction:
- Templates are pure resources with zero dependencies (RULE-002).
- Skills reference templates, not the other way around.
- Generated outputs derive from resources via the pipeline.
- No circular dependencies introduced.
- Domain layer (`src/domain/`) is not affected.
- Assembler layer (`src/assembler/`) is not affected.
- No new imports or code dependencies are added to any TypeScript module.

---

### 7. Integration Points

#### 7.1 Architecture Plan -> Threat Model

The `x-dev-architecture-plan` skill (story-0004-0006) generates an architecture plan that includes an "Impact Analysis" section. The threat model update logic (defined in skill instructions, not compiled code) must:

1. Parse the architecture plan output for security-relevant findings in the "Impact Analysis" and "Resilience Strategy" sections.
2. Map each finding to a STRIDE category based on content analysis.
3. Apply severity-based auto-add rules:
   - Critical/High severity -> Status: `Open`
   - Medium severity -> Status: `Under Review`
   - Low severity -> Not auto-added (mentioned in plan for awareness only)
4. Insert new entries into the appropriate STRIDE category table in the threat model.
5. Update the Risk Summary counts.
6. Add a Change History entry with date, story reference, and summary of threats added/updated.

#### 7.2 Security Review -> Threat Model

The `/x-review` skill launches a Security Engineer subagent (one of 8 parallel specialist reviews). After consolidation:

1. Extract findings from the Security Engineer's report section.
2. Each finding carries a severity level assigned by the Security Engineer.
3. Apply the same severity-based auto-add rules as 7.1.
4. Merge findings incrementally -- do NOT overwrite existing entries.
5. If a finding matches an existing threat (by description similarity), update the existing entry rather than duplicating.

#### 7.3 Incremental Update (RULE-008)

The threat model is a living document (RULE-008: Incremental Architecture Updates):
- New findings are appended to the appropriate STRIDE category table.
- Existing entries are preserved -- never removed or overwritten.
- If an existing threat's status changes (e.g., from `Open` to `Mitigated`), update the status field.
- The Risk Summary is recomputed from all entries after each update.
- The Change History is append-only.

---

### 8. Database Changes

None. This story does not involve any database, migration, or persistence changes.

---

### 9. API Changes

None. This story does not modify any HTTP/gRPC/CLI endpoint. The threat model is a documentation artifact, not a runtime API.

---

### 10. Event Changes

None. No event-driven components are affected.

---

### 11. Configuration Changes

None. No changes to `ProjectConfig`, `setup-config.*.yaml`, or any runtime configuration. The template is a static resource that does not require configuration gating.

---

### 12. Implementation Tasks (Ordered)

| Step | Action | TDD Phase | Verification |
|------|--------|-----------|-------------|
| 1 | Write failing test: verify `resources/templates/_TEMPLATE-THREAT-MODEL.md` exists and contains required sections (STRIDE categories, trust boundaries, risk table columns, risk summary, change history) | RED | Test fails -- file does not exist |
| 2 | Create `resources/templates/_TEMPLATE-THREAT-MODEL.md` with full STRIDE template | GREEN | Test passes |
| 3 | Write failing test: verify x-review SKILL.md contains threat model extraction instructions | RED | Test fails -- instructions not present |
| 4 | Modify `resources/skills-templates/core/x-review/SKILL.md` -- add post-review threat model update step with severity rules | GREEN | Test passes |
| 5 | Modify `resources/github-skills-templates/review/x-review.md` -- parallel change for GitHub | GREEN | Manual diff to verify semantic equivalence |
| 6 | Write failing test: verify x-dev-lifecycle SKILL.md references threat model in documentation phase | RED | Test fails |
| 7 | Modify `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` -- add threat model reference | GREEN | Test passes |
| 8 | Modify `resources/github-skills-templates/dev/x-dev-lifecycle.md` -- parallel change | GREEN | Manual diff |
| 9 | Update golden files (.claude, .agents, .github) for x-review -- 24 files | GREEN | byte-for-byte test passes |
| 10 | Update golden files (.claude, .agents, .github) for x-dev-lifecycle -- 24 files (if modified) | GREEN | byte-for-byte test passes |
| 11 | Run full test suite | VERIFY | All tests pass, coverage >= 95% line / >= 90% branch |

---

### 13. Golden File Impact

#### 13.1 Template File -- No Golden File Impact

The `_TEMPLATE-THREAT-MODEL.md` lives in `resources/templates/` and is not assembled into any profile-specific output directory. Therefore, it has **no golden file impact**. A dedicated unit test validates its structure and content.

#### 13.2 x-review Skill -- 24 Golden Files

| Copy | Count | Path Pattern |
|------|-------|-------------|
| .claude | 8 | `tests/golden/{profile}/.claude/skills/x-review/SKILL.md` |
| .agents | 8 | `tests/golden/{profile}/.agents/skills/x-review/SKILL.md` |
| .github | 8 | `tests/golden/{profile}/.github/skills/x-review/SKILL.md` |

#### 13.3 x-dev-lifecycle Skill -- 24 Golden Files (if modified)

| Copy | Count | Path Pattern |
|------|-------|-------------|
| .claude | 8 | `tests/golden/{profile}/.claude/skills/x-dev-lifecycle/SKILL.md` |
| .agents | 8 | `tests/golden/{profile}/.agents/skills/x-dev-lifecycle/SKILL.md` |
| .github | 8 | `tests/golden/{profile}/.github/skills/x-dev-lifecycle/SKILL.md` |

#### 13.4 Golden File Update Strategy

```bash
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

# x-review
CLAUDE_SRC="resources/skills-templates/core/x-review/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/review/x-review.md"
for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_SRC" "tests/golden/$profile/.claude/skills/x-review/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/$profile/.agents/skills/x-review/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/$profile/.github/skills/x-review/SKILL.md"
done

# x-dev-lifecycle (if modified)
CLAUDE_LIFECYCLE="resources/skills-templates/core/x-dev-lifecycle/SKILL.md"
GITHUB_LIFECYCLE="resources/github-skills-templates/dev/x-dev-lifecycle.md"
for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_LIFECYCLE" "tests/golden/$profile/.claude/skills/x-dev-lifecycle/SKILL.md"
  cp "$CLAUDE_LIFECYCLE" "tests/golden/$profile/.agents/skills/x-dev-lifecycle/SKILL.md"
  cp "$GITHUB_LIFECYCLE" "tests/golden/$profile/.github/skills/x-dev-lifecycle/SKILL.md"
done
```

Verification:
```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

**Total: 1 new template + up to 48 golden file updates.**

---

### 14. RULE-001 Compliance (Dual Copy Consistency)

| Dimension | Claude Source | GitHub Source |
|-----------|-------------|--------------|
| x-review file | `resources/skills-templates/core/x-review/SKILL.md` | `resources/github-skills-templates/review/x-review.md` |
| x-dev-lifecycle file | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | `resources/github-skills-templates/dev/x-dev-lifecycle.md` |
| Threat model extraction logic | Identical severity rules, STRIDE mapping, incremental update behavior | Identical severity rules, STRIDE mapping, incremental update behavior |
| Path references | `skills/security/references/...` | `.github/skills/security/SKILL.md` |
| Template reference | `resources/templates/_TEMPLATE-THREAT-MODEL.md` | `resources/templates/_TEMPLATE-THREAT-MODEL.md` (same -- templates are shared) |

The threat model content (STRIDE categories, severity-based rules, incremental update logic) MUST be semantically identical in both copies. Only IDE-specific path references differ.

---

### 15. Severity-Based Auto-Add Rules (Decision Table)

| Finding Severity | Auto-Add? | Initial Status | Rationale |
|-----------------|-----------|----------------|-----------|
| Critical | Yes | `Open` | Critical threats must be tracked and addressed immediately |
| High | Yes | `Open` | High threats require prompt mitigation |
| Medium | Yes | `Under Review` | Medium threats need triage before committing to mitigation |
| Low | No | N/A | Low threats are noted in the review but not auto-added to the threat model; they can be added manually if the team decides to track them |

This decision table is embedded in the skill instructions (x-review SKILL.md) so the AI agent applies it consistently.

---

### 16. Incremental Update Algorithm (Pseudocode)

```
function updateThreatModel(existingModel, newFindings, storyRef):
  for each finding in newFindings:
    category = mapToStrideCategory(finding)
    severity = finding.severity
    status = determineStatus(severity)  # Critical/High -> Open, Medium -> Under Review

    existingEntry = findMatchingEntry(existingModel, category, finding.description)
    if existingEntry exists:
      # Update existing entry (e.g., severity escalation, mitigation update)
      existingEntry.update(finding)
    else:
      # Append new entry to the STRIDE category table
      category.table.append(finding, severity, status, storyRef)

  recomputeRiskSummary(existingModel)
  appendChangeHistory(existingModel, date=today, story=storyRef, summary)
```

This logic is described in natural language within the skill instructions. The AI agent interprets and executes it at runtime. No compiled code implements this algorithm.

---

### 17. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| Golden file mismatch after skill edit | HIGH | LOW | Mechanical copy script (Section 13.4) eliminates drift. Run byte-for-byte tests immediately after updates. |
| Inconsistency between Claude and GitHub source templates | HIGH | MEDIUM | After editing both copies, diff them to verify only path references differ. Threat model extraction logic must be semantically identical. |
| story-0004-0006 not yet implemented (dependency) | MEDIUM | MEDIUM | The template can be created independently. Skill modifications that reference architecture plan output can use the output format defined in story-0004-0006's data contract (Section 5) even before implementation. |
| AI misinterpreting severity rules | MEDIUM | LOW | Rules are expressed as an explicit decision table (Section 15) with no ambiguity. Test with sample findings to validate correct application. |
| Template placeholder collision | LOW | LOW | The template uses `{{SERVICE_NAME}}` which is a standard placeholder already used in other templates. No new placeholder patterns are introduced. |
| Incremental update losing existing entries | MEDIUM | LOW | The algorithm (Section 16) explicitly preserves existing entries and only appends/updates. The skill instructions include explicit "NEVER remove existing entries" directive. |
| Threat model becoming too large | LOW | LOW | Each feature adds a bounded number of findings (typically 1-5). The STRIDE categorization distributes entries across 6 tables. Change History is append-only but compact (one row per update). |
| x-dev-lifecycle modification scope creep | MEDIUM | MEDIUM | The lifecycle change is minimal -- add a single reference to threat model update in the documentation phase. If the lifecycle has already been modified by story-0004-0005 or story-0004-0013, coordinate to avoid conflicts. |

---

### 18. Backward Compatibility Assessment

This change is **purely additive** (RULE-003):

- A new template file is created -- no existing templates are modified or removed.
- Skill modifications add new steps/instructions -- no existing steps are removed.
- Projects without a `docs/security/threat-model.md` file are unaffected. The threat model is only generated when the relevant skills are invoked.
- The threat model template uses `{{SERVICE_NAME}}` which is already handled by the template engine's `replacePlaceholders` method (mapped to `project_name`).
- No changes to `ProjectConfig`, pipeline logic, or assembler behavior.
- Golden file changes are confined to skill content (x-review, x-dev-lifecycle) -- no profile-specific variations.

---

### 19. Files to Modify (Complete List)

#### 19.1 New Files (1 file)

| # | File | Description |
|---|------|-------------|
| 1 | `resources/templates/_TEMPLATE-THREAT-MODEL.md` | STRIDE-based threat model template |

#### 19.2 Modified Source Templates (4 files)

| # | File | Change |
|---|------|--------|
| 2 | `resources/skills-templates/core/x-review/SKILL.md` | Add threat model update step with severity-based auto-add rules |
| 3 | `resources/github-skills-templates/review/x-review.md` | Parallel change for GitHub Copilot |
| 4 | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Reference threat model update in documentation phase |
| 5 | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Parallel change for GitHub Copilot |

#### 19.3 Golden Files (up to 48 files)

Listed in Section 13.2 and 13.3.

#### 19.4 Files Explicitly UNCHANGED

| File | Reason |
|------|--------|
| `src/assembler/skills-assembler.ts` | Copy logic unchanged -- no new skills, only content edits to existing skills |
| `src/assembler/github-skills-assembler.ts` | Rendering logic unchanged |
| `src/assembler/codex-skills-assembler.ts` | Mirror logic unchanged |
| `src/assembler/pipeline.ts` | No new assembler step needed |
| `src/models.ts` | No config model changes |
| `src/template-engine.ts` | No new placeholder patterns |
| `src/domain/skill-registry.ts` | No new knowledge packs |
| `resources/skills-templates/knowledge-packs/security/SKILL.md` | Security KP already exists; no changes needed |
| `tests/node/integration/byte-for-byte.test.ts` | Test infrastructure unchanged |

---

### 20. Out of Scope

- Creating a runtime threat model parser/updater in TypeScript (the update logic is AI-interpreted, not compiled).
- Modifying the `ProjectConfig` model or adding a `threatModel` configuration section.
- Creating a new assembler for threat model generation (the template is a static resource).
- Modifying the pipeline to conditionally include/exclude the threat model template.
- Creating a standalone `/x-threat-model` skill (may be a future enhancement).
- Automated threat model validation (linting for completeness, orphaned entries, etc.).
- Integration with external security scanning tools (SAST, DAST, SCA).
