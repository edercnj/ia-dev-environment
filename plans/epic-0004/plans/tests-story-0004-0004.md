# Test Plan — STORY-0004-0004: x-story-create — Mandatory Mermaid Diagrams

## Summary
- Total test classes: 1 (extend existing `x-story-create-content.test.ts`)
- Total test methods: ~28 (estimated)
- Categories covered: Unit (content validation), Integration (golden files via byte-for-byte)
- Estimated line coverage: ~99% (template content changes only)

## Test Class: x-story-create-content.test.ts (Extended)

> **Pattern:** Follow existing test structure — `describe` blocks for Claude source, GitHub source, and dual-copy consistency.
> **Source paths:** Same `CLAUDE_SOURCE_PATH` and `GITHUB_SOURCE_PATH` constants already defined.

---

### New Constants

```typescript
const DIAGRAM_REQUIREMENT_MATRIX_ROWS = [
  "Request-Response",
  "Event-Driven",
  "Infrastructure",
  "Documentation",
];

const DIAGRAM_TYPES = [
  "Sequence",
  "Deployment",
  "Activity",
];

const DIAGRAM_OBLIGATION_LEVELS = [
  "MANDATORY",
  "Recommended",
  "Not required",
];

const SEQUENCE_DIAGRAM_PARTICIPANTS = [
  "Inbound",
  "Application",
  "Domain",
  "Outbound",
];

const DIAGRAM_CHECKLIST_ITEMS = [
  "real component names",
  "error path",
  "architecture layers",
  "concrete names",
];
```

---

### Acceptance Tests (AT-N) — Outer Loop

| # | AT ID | Test Name | Description | Depends On |
|---|-------|-----------|-------------|------------|
| 1 | AT-1 | `containsDiagramRequirementMatrix_section6_hasMatrixTable` | Section 6 contains "Diagram Requirement Matrix" heading | — |
| 2 | AT-2 | `containsInterLayerSequenceTemplate_section6_hasMermaidSequenceDiagram` | Section 6 contains a Mermaid sequenceDiagram block | — |
| 3 | AT-3 | `containsDiagramValidationChecklist_section6_hasChecklistItems` | Section 6 contains diagram validation checklist with ≥4 items | — |
| 4 | AT-4 | `dualCopyConsistency_diagramContent_bothCopiesMatch` | Both Claude and GitHub copies contain all new content | AT-1..AT-3 |

---

### Unit Tests (UT-N) — Inner Loop (TPP Order)

#### Group: Diagram Requirement Matrix (degenerate → conditions)

| # | UT ID | Test Name | Description | TPP Level | Parallel | Depends On |
|---|-------|-----------|-------------|-----------|----------|------------|
| 1 | UT-1 | `containsDiagramRequirementMatrix_section6_hasHeading` | Source contains "Diagram Requirement Matrix" heading | L1 degenerate | yes | — |
| 2 | UT-2 | `containsMatrixRow_%s_inRequirementMatrix` (parametrized × 4 rows) | Each story type row exists in matrix | L2 unconditional | yes | UT-1 |
| 3 | UT-3 | `containsDiagramType_%s_inRequirementMatrix` (parametrized × 3 types) | Each diagram type column exists | L2 unconditional | yes | UT-1 |
| 4 | UT-4 | `containsObligationLevel_%s_inRequirementMatrix` (parametrized × 3 levels) | Each obligation level referenced | L2 unconditional | yes | UT-1 |
| 5 | UT-5 | `matrixMandatesSequenceDiagram_requestResponseStory_mandatory` | Request-Response row marks Sequence as MANDATORY | L3 condition | no | UT-2,UT-3 |
| 6 | UT-6 | `matrixMandatesDeploymentDiagram_infrastructureStory_mandatory` | Infrastructure row marks Deployment as MANDATORY | L3 condition | no | UT-2,UT-3 |
| 7 | UT-7 | `matrixAllowsNoDiagram_documentationStory_notRequired` | Documentation row marks all as Not required | L3 condition | no | UT-2,UT-3 |

#### Group: Inter-Layer Sequence Diagram Template (degenerate → edge)

| # | UT ID | Test Name | Description | TPP Level | Parallel | Depends On |
|---|-------|-----------|-------------|-----------|----------|------------|
| 8 | UT-8 | `containsMermaidSequenceDiagram_section6_hasSequenceDiagramBlock` | Source contains ` ```mermaid` + `sequenceDiagram` | L1 degenerate | yes | — |
| 9 | UT-9 | `containsParticipant_%s_inSequenceDiagram` (parametrized × 4) | Each architecture layer participant exists | L2 unconditional | yes | UT-8 |
| 10 | UT-10 | `containsAltBlock_sequenceDiagram_hasErrorScenario` | Diagram includes at least 1 `alt` block for error scenario | L3 condition | no | UT-8 |
| 11 | UT-11 | `sequenceDiagramShowsFlow_triggerToResponse_completeInteraction` | Diagram shows trigger → validation → business logic → persistence → response | L4 compound | no | UT-9 |

#### Group: Diagram Validation Checklist (degenerate → boundary)

| # | UT ID | Test Name | Description | TPP Level | Parallel | Depends On |
|---|-------|-----------|-------------|-----------|----------|------------|
| 12 | UT-12 | `containsDiagramValidationChecklist_section6_hasChecklistHeading` | Source contains "Diagram Validation Checklist" or similar heading | L1 degenerate | yes | — |
| 13 | UT-13 | `containsChecklistItem_%s_inValidationChecklist` (parametrized × 4) | Each validation item exists | L2 unconditional | yes | UT-12 |
| 14 | UT-14 | `checklistHasMinimumItems_validationChecklist_atLeastFourItems` | At least 4 checkbox items present | L3 condition | no | UT-13 |

#### Group: Backward Compatibility (edge cases)

| # | UT ID | Test Name | Description | TPP Level | Parallel | Depends On |
|---|-------|-----------|-------------|-----------|----------|------------|
| 15 | UT-15 | `preservesExistingSection6Content_diagramSection_originalContentIntact` | Original Section 6 content (if any) is preserved or extended, not replaced | L5 edge | no | UT-1,UT-8,UT-12 |

#### Group: Dual-Copy Consistency (RULE-001)

| # | UT ID | Test Name | Description | TPP Level | Parallel | Depends On |
|---|-------|-----------|-------------|-----------|----------|------------|
| 16 | UT-16 | `bothContainDiagramRequirementMatrix_dualCopy_sameContent` | Both copies have Diagram Requirement Matrix | L2 unconditional | no | UT-1 |
| 17 | UT-17 | `bothContainSequenceDiagramTemplate_dualCopy_sameContent` | Both copies have sequenceDiagram block | L2 unconditional | no | UT-8 |
| 18 | UT-18 | `bothContainDiagramChecklist_dualCopy_sameContent` | Both copies have validation checklist | L2 unconditional | no | UT-12 |
| 19 | UT-19 | `bothContainAllParticipants_dualCopy_sameParticipants` (parametrized × 4) | Both copies have same sequence diagram participants | L3 condition | yes | UT-9 |
| 20 | UT-20 | `bothContainAllMatrixRows_dualCopy_sameStoryTypes` (parametrized × 4) | Both copies have same matrix rows | L3 condition | yes | UT-2 |

---

### Integration Tests (IT-N)

| # | IT ID | Test Name | Description | Depends On |
|---|-------|-----------|-------------|------------|
| 1 | IT-1 | Golden file parity (byte-for-byte) | Existing `byte-for-byte.test.ts` validates all 24 golden files match regenerated output | All UT-N |

**Action:** After modifying source templates, regenerate golden files for all 8 profiles and update the golden file directory.

---

## Coverage Estimation

| File | Public Methods | Branches | Est. Tests | Line % | Branch % |
|------|---------------|----------|-----------|--------|----------|
| `resources/skills-templates/core/x-story-create/SKILL.md` | N/A (template) | N/A | ~14 | 99% | 99% |
| `resources/github-skills-templates/story/x-story-create.md` | N/A (template) | N/A | ~14 | 99% | 99% |

> Note: This story modifies markdown templates, not TypeScript source code. Coverage metrics apply to the test file itself, not to the templates. The byte-for-byte integration test ensures generated output matches golden files exactly.

## Risks and Gaps

1. **Golden file regeneration**: 24 golden files must be updated across 8 profiles — risk of missing a profile
2. **Template variable interference**: Mermaid diagram content might contain characters that conflict with `{{PLACEHOLDER}}` syntax — verify no false replacements
3. **Content ordering**: New Section 6 content must be placed correctly relative to existing content — use indexOf ordering assertions
4. **Dual copy drift**: RULE-001 requires both copies to have identical new content — parametrized dual-copy tests mitigate this

## TPP Ordering Summary

```
L1 degenerate  → UT-1, UT-8, UT-12 (headings exist)
L2 unconditional → UT-2..4, UT-9, UT-13, UT-16..20 (content elements present)
L3 condition   → UT-5..7, UT-10, UT-14, UT-19..20 (specific rules enforced)
L4 compound    → UT-11 (complete flow validation)
L5 edge        → UT-15 (backward compatibility)
```
