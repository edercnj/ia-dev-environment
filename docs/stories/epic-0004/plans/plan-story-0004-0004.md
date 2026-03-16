# Implementation Plan — story-0004-0004

## x-story-create — Mandatory Mermaid Diagrams

### 1. Summary

This story enhances the `x-story-create` skill's Section 6 (Diagrams) to make Mermaid diagrams mandatory based on story type. The current Section 6 instructs the AI to "create Mermaid sequence diagrams" but provides no enforcement rules, no requirement matrix by story type, no inter-layer sequence template, and no validation checklist. This story adds all four.

**Key insight from context analysis:** The source of truth is `resources/skills-templates/core/x-story-create/SKILL.md` (Claude) and `resources/github-skills-templates/story/x-story-create.md` (GitHub). These templates contain **no `{{placeholders}}`** — the pipeline copies them byte-for-byte. The `.agents` copy is an exact mirror of the `.claude` copy (via `codex-skills-assembler`). All 8 profiles produce identical copies (the skill is profile-independent).

---

### 2. Architecture — How the x-story-create Copy System Works

```
resources/skills-templates/core/x-story-create/SKILL.md   <-- SOURCE OF TRUTH (Claude)
        |
        | copyTemplateTree (no {{placeholders}} in this file)
        | via: src/assembler/skills-assembler.ts :: assembleCore()
        v
{outputDir}/.claude/skills/x-story-create/SKILL.md        <-- pipeline output (.claude)
        |
        | mirror from .claude/skills/
        | via: src/assembler/codex-skills-assembler.ts
        v
{outputDir}/.agents/skills/x-story-create/SKILL.md        <-- pipeline output (.agents)

---

resources/github-skills-templates/story/x-story-create.md  <-- SOURCE OF TRUTH (GitHub)
        |
        | renderSkill (no {{placeholders}} in this file)
        | via: src/assembler/github-skills-assembler.ts
        v
{outputDir}/.github/skills/x-story-create/SKILL.md        <-- pipeline output (.github)
```

Key properties:
- The Claude source template has **no `{{placeholders}}`** — the pipeline copies it byte-for-byte.
- The `.agents` copy is an exact mirror of the `.claude` copy (via `codex-skills-assembler`).
- The GitHub copy is a separate source file with different path references (`.github/` instead of `.claude/`).
- All 8 profiles produce identical copies (the skill is profile-independent).

Additionally, there is a `_TEMPLATE-STORY.md` file at `resources/templates/_TEMPLATE-STORY.md` that defines the output structure of generated stories. This template's Section 6 is currently minimal and **is not changed in this story**; enhancing it with the new diagram requirements will be handled in a future story.

---

### 3. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| Resources (Claude source) | `resources/skills-templates/core/x-story-create/SKILL.md` | **MODIFIED** — enhance Section 6 with diagram requirement matrix, inter-layer sequence template, validation checklist |
| Resources (GitHub source) | `resources/github-skills-templates/story/x-story-create.md` | **MODIFIED** — parallel changes with GitHub-specific path references |
| Resources (Template) | `resources/templates/_TEMPLATE-STORY.md` | **NO CHANGE IN THIS STORY** — remains with minimal Section 6; future story will enhance |
| Golden files (.claude) | `tests/golden/{profile}/.claude/skills/x-story-create/SKILL.md` | **MUST UPDATE** — 8 files |
| Golden files (.agents) | `tests/golden/{profile}/.agents/skills/x-story-create/SKILL.md` | **MUST UPDATE** — 8 files |
| Golden files (.github) | `tests/golden/{profile}/.github/skills/x-story-create/SKILL.md` | **MUST UPDATE** — 8 files |
| Content tests | `tests/node/content/x-story-create-content.test.ts` | **MODIFIED** — add assertions for new content |

**Total: 2 source templates + 24 golden files + 1 test file = 27 files modified.**

---

### 4. New Content to Add to Section 6

#### 4.1 Diagram Requirement Matrix

Add a new subsection to Section 6 with a mandatory table mapping story types to diagram types and their obligation level:

```markdown
##### Diagram Requirement Matrix

| Story Type | Sequence Diagram | Deployment Diagram | Activity Diagram |
|:---|:---|:---|:---|
| Request→Response flow (REST, gRPC, TCP) | **MANDATORY** | — | Recommended if 3+ branches |
| Event-driven flow (producer→broker→consumer) | **MANDATORY** | — | Recommended if 3+ branches |
| Infrastructure / deployment change | — | **MANDATORY** | — |
| Complex business logic (3+ decision branches) | Recommended | — | **MANDATORY** |
| Documentation / configuration only | Not required | Not required | Not required |
| Refactoring (no behavior change) | Recommended | — | — |
```

Rules:
- A story involving data flow between 2+ components MUST include a sequence diagram.
- A story altering infrastructure MUST include a deployment diagram.
- A story with no data flow and no infrastructure change (e.g., pure documentation, configuration) MAY omit diagrams but should note "Diagram not required for this story type."

#### 4.2 Inter-Layer Sequence Diagram Template

Add a reusable Mermaid template showing the canonical inter-layer flow with placeholders:

```markdown
##### Inter-Layer Sequence Diagram Template

Use this template as the starting point for stories involving request→response flows:

    ```mermaid
    sequenceDiagram
        participant Client as Client
        participant Inbound as Inbound Adapter<br/>(REST/gRPC/CLI)
        participant App as Application<br/>(Use Case)
        participant Domain as Domain<br/>(Engine/Model)
        participant Outbound as Outbound Adapter<br/>(DB/API/Queue)

        Client->>Inbound: Request (DTO)
        Inbound->>Inbound: Validate & Map DTO→Command
        Inbound->>App: Execute(Command)
        App->>Domain: Process(Entity)
        Domain->>Domain: Apply business rules
        Domain-->>App: Result
        App->>Outbound: Persist/Send
        Outbound-->>App: Confirmation
        App-->>Inbound: Response
        Inbound-->>Client: Response (DTO)

        alt Validation Error
            Inbound-->>Client: 400 Bad Request
        end

        alt Domain Rule Violation
            Domain-->>App: BusinessException
            App-->>Inbound: Error Result
            Inbound-->>Client: 422 Unprocessable Entity
        end
    ```
```

Participant naming rules:
- Use actual component names from the spec (not generic "Service A").
- Must show at least: trigger → validation → business logic → persistence → response.
- Must include at least 1 error scenario with `alt` block.

#### 4.3 Diagram Validation Checklist

Add a checklist that must be satisfied for every diagram:

```markdown
##### Diagram Validation Checklist

- [ ] Participants use real component names (not "Service A", "Service B")
- [ ] Diagram shows at least 3 architecture layers (e.g., Inbound → Application → Domain)
- [ ] At least 1 error path is shown using `alt` block
- [ ] All data transformations are visible (DTO→Command, Entity→Domain)
- [ ] Async operations (if any) are distinguished from sync calls
- [ ] Response construction path is complete (from domain result back to client)
```

---

### 5. Existing Content to Modify

#### 5.1 Section 6 — Diagramas (Claude and GitHub source templates)

**Current content** (lines 120-132 in Claude source):
```markdown
#### Section 6 — Diagramas

Create Mermaid sequence diagrams showing the complete flow for this story's main operation.
Use the actual component names from the spec (not generic "Service A", "Service B").

Include:
- The trigger (client request, Kafka event, timer)
- Validation steps
- Business logic (decision engine, routing)
- Persistence (DB writes, cache updates)
- Async operations (Kafka publish, logging)
- Response construction
- Error paths (at least one error scenario)
```

**New content:** Replace the existing Section 6 content with an expanded version that retains the original bullet list and adds the three new subsections (matrix, template, checklist) after it. The existing bullet list becomes the "Content Requirements" and the new subsections are appended.

#### 5.2 _TEMPLATE-STORY.md Section 6

**Current content** (lines 70-84):
```markdown
## 6. Diagramas

### 6.1 <Nome do Diagrama>

\`\`\`mermaid
sequenceDiagram
    participant A as <Componente A>
    ...
\`\`\`
```

**New content:** Add after the existing diagram placeholder:
- The Diagram Requirement Matrix (as a reference)
- The validation checklist

---

### 6. New Classes/Interfaces to Create

None. This story modifies only Markdown template files.

---

### 7. Dependency Direction Validation

Not applicable. No TypeScript code changes; only Markdown content templates.

---

### 8. Integration Points

| Integration Point | Description |
|---|---|
| `tests/node/content/x-story-create-content.test.ts` | Existing content validation test. Must add assertions for new content (matrix, template, checklist). |
| `tests/node/integration/byte-for-byte.test.ts` | Existing byte-for-byte test. No changes needed to the test logic itself — the golden files will be updated to match the new source templates, and the byte-for-byte test will verify the match automatically. |

---

### 9. Database Changes

None.

---

### 10. API Changes

None.

---

### 11. Event Changes

None.

---

### 12. Configuration Changes

None.

---

### 13. Files to Modify (Complete List)

#### 13.1 Source of Truth (3 files)

| # | File | Change |
|---|------|--------|
| 1 | `resources/skills-templates/core/x-story-create/SKILL.md` | Enhance Section 6 with diagram requirement matrix, inter-layer sequence template, validation checklist |
| 2 | `resources/github-skills-templates/story/x-story-create.md` | Parallel changes with GitHub-specific path references |
| 3 | `resources/templates/_TEMPLATE-STORY.md` | Enhance Section 6 with diagram validation checklist and requirement notes |

#### 13.2 Test File (1 file)

| # | File | Change |
|---|------|--------|
| 4 | `tests/node/content/x-story-create-content.test.ts` | Add test cases for diagram requirement matrix, inter-layer template, validation checklist |

#### 13.3 Golden Files — Claude (.claude) — 8 files

| # | File |
|---|------|
| 5 | `tests/golden/go-gin/.claude/skills/x-story-create/SKILL.md` |
| 6 | `tests/golden/java-quarkus/.claude/skills/x-story-create/SKILL.md` |
| 7 | `tests/golden/java-spring/.claude/skills/x-story-create/SKILL.md` |
| 8 | `tests/golden/kotlin-ktor/.claude/skills/x-story-create/SKILL.md` |
| 9 | `tests/golden/python-click-cli/.claude/skills/x-story-create/SKILL.md` |
| 10 | `tests/golden/python-fastapi/.claude/skills/x-story-create/SKILL.md` |
| 11 | `tests/golden/rust-axum/.claude/skills/x-story-create/SKILL.md` |
| 12 | `tests/golden/typescript-nestjs/.claude/skills/x-story-create/SKILL.md` |

#### 13.4 Golden Files — Agents (.agents) — 8 files

| # | File |
|---|------|
| 13 | `tests/golden/go-gin/.agents/skills/x-story-create/SKILL.md` |
| 14 | `tests/golden/java-quarkus/.agents/skills/x-story-create/SKILL.md` |
| 15 | `tests/golden/java-spring/.agents/skills/x-story-create/SKILL.md` |
| 16 | `tests/golden/kotlin-ktor/.agents/skills/x-story-create/SKILL.md` |
| 17 | `tests/golden/python-click-cli/.agents/skills/x-story-create/SKILL.md` |
| 18 | `tests/golden/python-fastapi/.agents/skills/x-story-create/SKILL.md` |
| 19 | `tests/golden/rust-axum/.agents/skills/x-story-create/SKILL.md` |
| 20 | `tests/golden/typescript-nestjs/.agents/skills/x-story-create/SKILL.md` |

#### 13.5 Golden Files — GitHub (.github) — 8 files

| # | File |
|---|------|
| 21 | `tests/golden/go-gin/.github/skills/x-story-create/SKILL.md` |
| 22 | `tests/golden/java-quarkus/.github/skills/x-story-create/SKILL.md` |
| 23 | `tests/golden/java-spring/.github/skills/x-story-create/SKILL.md` |
| 24 | `tests/golden/kotlin-ktor/.github/skills/x-story-create/SKILL.md` |
| 25 | `tests/golden/python-click-cli/.github/skills/x-story-create/SKILL.md` |
| 26 | `tests/golden/python-fastapi/.github/skills/x-story-create/SKILL.md` |
| 27 | `tests/golden/rust-axum/.github/skills/x-story-create/SKILL.md` |
| 28 | `tests/golden/typescript-nestjs/.github/skills/x-story-create/SKILL.md` |

**Total: 3 source files + 1 test file + 24 golden files = 28 files.**

#### 13.6 Files Explicitly UNCHANGED

| File | Reason |
|------|--------|
| `src/assembler/skills-assembler.ts` | Copy logic unchanged; no `{{placeholders}}` in this template |
| `src/assembler/github-skills-assembler.ts` | Copy logic unchanged |
| `src/assembler/codex-skills-assembler.ts` | Mirror logic unchanged |
| `tests/node/integration/byte-for-byte.test.ts` | Test infrastructure unchanged; golden file updates are sufficient |
| `.claude/skills/x-story-create/SKILL.md` | Generated output, not source of truth; regenerated by pipeline |
| `.github/skills/x-story-create/SKILL.md` | Does not exist in this project (the project IS the generator) |

---

### 14. New Test Cases for `x-story-create-content.test.ts`

The existing test file validates content in both Claude and GitHub source templates. The following test cases should be added:

#### 14.1 Constants to Add

```typescript
const DIAGRAM_REQUIREMENT_MATRIX_STORY_TYPES = [
  "Request→Response flow",
  "Event-driven flow",
  "Infrastructure",
  "Complex business logic",
  "Documentation / configuration only",
  "Refactoring",
];

const DIAGRAM_VALIDATION_CHECKLIST_ITEMS = [
  "real component names",
  "at least 3 architecture layers",
  "error path",
  "alt",
  "data transformations",
  "Response construction",
];

const INTER_LAYER_TEMPLATE_PARTICIPANTS = [
  "Inbound Adapter",
  "Application",
  "Domain",
  "Outbound Adapter",
];
```

#### 14.2 Test Cases — Claude Source

| Test Name | Assertion |
|-----------|-----------|
| `containsDiagramRequirementMatrix_section6_hasRequirementTable` | Claude source contains "Diagram Requirement Matrix" |
| `containsInterLayerTemplate_section6_hasSequenceDiagramTemplate` | Claude source contains "Inter-Layer Sequence Diagram Template" |
| `containsDiagramValidationChecklist_section6_hasChecklist` | Claude source contains "Diagram Validation Checklist" |
| `containsMandatoryKeyword_section6_hasMandatoryForFlowStories` | Claude source contains "MANDATORY" |
| `containsAltBlock_interLayerTemplate_hasErrorAltBlock` | Claude source contains "alt" within a mermaid block context |
| `containsParticipant_{name}_interLayerTemplate` (parameterized) | Each participant name from `INTER_LAYER_TEMPLATE_PARTICIPANTS` exists in Claude source |
| `containsStoryType_{type}_inRequirementMatrix` (parameterized) | Each story type from `DIAGRAM_REQUIREMENT_MATRIX_STORY_TYPES` exists in Claude source |

#### 14.3 Test Cases — GitHub Source

Mirror of all Claude source tests (same assertions, different source variable).

#### 14.4 Test Cases — Dual Copy Consistency (RULE-001)

| Test Name | Assertion |
|-----------|-----------|
| `bothContainDiagramRequirementMatrix_sameContent` | Both sources contain "Diagram Requirement Matrix" |
| `bothContainInterLayerTemplate_sameContent` | Both sources contain "Inter-Layer Sequence Diagram Template" |
| `bothContainDiagramValidationChecklist_sameContent` | Both sources contain "Diagram Validation Checklist" |
| `bothContainMandatoryKeyword_sameObligation` | Both sources contain "MANDATORY" |

---

### 15. RULE-001 Compliance (Dual Copy Consistency)

| Dimension | Claude Source | GitHub Source |
|-----------|-------------|--------------|
| File path | `resources/skills-templates/core/x-story-create/SKILL.md` | `resources/github-skills-templates/story/x-story-create.md` |
| Section headers (Claude) | `#### Section 6 — Diagramas` | `#### Section 6 — Diagrams` |
| Diagram Requirement Matrix content | Identical table structure and rows | Identical table structure and rows |
| Inter-Layer Sequence Template | Identical Mermaid block | Identical Mermaid block |
| Diagram Validation Checklist | Identical checklist items | Identical checklist items |
| Path references | Not applicable (Section 6 has no path references) | Not applicable |

The Claude and GitHub copies differ only in their section header language (Portuguese vs English), their YAML frontmatter, and path references in other sections. The diagram content added by this story is identical in both copies.

---

### 16. Golden File Update Strategy

```bash
# After editing the source templates:
CLAUDE_SRC="resources/skills-templates/core/x-story-create/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/story/x-story-create.md"
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_SRC" "tests/golden/$profile/.claude/skills/x-story-create/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/$profile/.agents/skills/x-story-create/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/$profile/.github/skills/x-story-create/SKILL.md"
done
```

Verification:
```bash
npx vitest run tests/node/content/x-story-create-content.test.ts
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

---

### 17. Backward Compatibility Assessment

This change is **purely additive**:
- The existing Section 6 content (bullet list of what to include) is preserved.
- Three new subsections are appended: matrix, template, checklist.
- No existing section headings or content are removed.
- Stories previously generated without diagrams remain valid — the requirement matrix explicitly allows diagram-free stories for "Documentation / configuration only" types.
- The `_TEMPLATE-STORY.md` enhancement adds optional reference content but does not break existing story files.

The story explicitly requires backward compatibility: "Stories existentes sem diagrama continuam validas."

---

### 18. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| Golden file mismatch after edit | HIGH | LOW | Mechanical copy script (Section 16) eliminates drift. Run byte-for-byte tests immediately. |
| Inconsistency between Claude and GitHub source templates | HIGH | MEDIUM | After editing both, diff the diagram-related content to verify only section headers differ (Portuguese vs English). |
| Mermaid syntax error in inter-layer template | MEDIUM | LOW | Validate the Mermaid block renders correctly in a Mermaid preview tool before committing. |
| Template too prescriptive (over-constraining AI behavior) | LOW | LOW | The matrix uses "MANDATORY", "Recommended", and "Not required" levels, giving appropriate flexibility. |
| Validation checklist too long or redundant | LOW | LOW | Kept to 6 items, each covering a distinct concern. |
| `_TEMPLATE-STORY.md` changes conflict with other stories | LOW | LOW | Section 6 in the template is currently minimal; no other story in epic-0004 targets this section. |

---

### 19. Implementation Order (TDD)

| Step | Action | Verification |
|------|--------|-------------|
| 1 | Write failing content tests for diagram requirement matrix | Test fails (RED) |
| 2 | Write failing content tests for inter-layer sequence template | Test fails (RED) |
| 3 | Write failing content tests for validation checklist | Test fails (RED) |
| 4 | Write failing dual-copy consistency tests | Test fails (RED) |
| 5 | Edit Claude source: enhance Section 6 with diagram requirement matrix | Content test starts passing |
| 6 | Edit Claude source: add inter-layer sequence diagram template | Content test starts passing |
| 7 | Edit Claude source: add diagram validation checklist | Content test starts passing |
| 8 | Edit GitHub source: apply parallel changes (English headers) | Dual-copy tests pass |
| 9 | Edit `_TEMPLATE-STORY.md`: enhance Section 6 | Manual review |
| 10 | Copy Claude source to 16 golden files (.claude + .agents) | Script execution |
| 11 | Copy GitHub source to 8 golden files (.github) | Script execution |
| 12 | Run content test suite | All new assertions pass (GREEN) |
| 13 | Run byte-for-byte test suite | All assertions pass |
| 14 | Run full test suite | All tests pass, coverage >= 95% line / >= 90% branch |

---

### 20. Difference Map Between Source Templates

For implementer reference, the Claude and GitHub source templates differ in these systematic ways relevant to Section 6:

| Aspect | Claude Template | GitHub Template |
|--------|----------------|-----------------|
| Section 6 heading | `#### Section 6 — Diagramas` | `#### Section 6 — Diagrams` |
| Diagram matrix content | Identical | Identical |
| Inter-layer template content | Identical | Identical |
| Validation checklist content | Identical | Identical |
| Prerequisite paths | `.claude/skills/...` | `.github/skills/...` |
| Template path references | `.claude/templates/_TEMPLATE-STORY.md` | `resources/templates/_TEMPLATE-STORY.md` |

The diagram content (matrix, template, checklist) added by this story must be **identical** in both copies. Only the existing path references in other sections differ.

---

### 21. Out of Scope

- Modifying any TypeScript source code (`src/**/*.ts`)
- Modifying pipeline/assembler logic
- Modifying test infrastructure (`byte-for-byte.test.ts`, `integration-constants.ts`)
- Adding new profiles or config templates
- Modifying other skills (x-story-epic, x-story-map, etc.)
- Enforcing diagram presence at runtime (this story adds guidance to the skill template; runtime enforcement would require TypeScript code changes and is a separate concern)
- Modifying the `x-story-epic` or `x-story-epic-full` skills
