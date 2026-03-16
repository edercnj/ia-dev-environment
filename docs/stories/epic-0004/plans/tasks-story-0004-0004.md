# Task Breakdown — STORY-0004-0004: x-story-create — Mandatory Mermaid Diagrams

**Mode:** Test-Driven (derived from test plan `tests-story-0004-0004.md`)

## Tasks

### TASK-1: RED — Write content tests for Diagram Requirement Matrix
**Tests:** UT-1, UT-2, UT-3, UT-4, UT-5, UT-6, UT-7
**Parallel:** no (foundational)
**Action:** Add new `describe` blocks to `x-story-create-content.test.ts` for matrix heading, row parametrized checks, diagram type checks, obligation level checks, and specific obligation rules.

### TASK-2: GREEN — Add Diagram Requirement Matrix to source templates
**Tests:** UT-1..UT-7 turn GREEN
**Parallel:** no (depends on TASK-1)
**Depends On:** TASK-1
**Action:** Edit `resources/skills-templates/core/x-story-create/SKILL.md` Section 6 to add a "Diagram Requirement Matrix" table with rows for Request-Response, Event-Driven, Infrastructure, Documentation and columns for Sequence, Deployment, Activity with MANDATORY/Recommended/Not required labels.

### TASK-3: RED — Write content tests for Inter-Layer Sequence Diagram Template
**Tests:** UT-8, UT-9, UT-10, UT-11
**Parallel:** yes (independent of TASK-1/TASK-2 tests)
**Action:** Add tests for Mermaid block presence, participant checks (parametrized), alt block, and flow completeness.

### TASK-4: GREEN — Add Inter-Layer Sequence Diagram Template to source templates
**Tests:** UT-8..UT-11 turn GREEN
**Parallel:** no (depends on TASK-3)
**Depends On:** TASK-3
**Action:** Edit source template Section 6 to add Mermaid sequence diagram template with Inbound, Application, Domain, Outbound participants and at least 1 alt error block.

### TASK-5: RED — Write content tests for Diagram Validation Checklist
**Tests:** UT-12, UT-13, UT-14
**Parallel:** yes (independent of TASK-1..TASK-4)
**Action:** Add tests for checklist heading, parametrized checklist item checks, and minimum item count.

### TASK-6: GREEN — Add Diagram Validation Checklist to source templates
**Tests:** UT-12..UT-14 turn GREEN
**Parallel:** no (depends on TASK-5)
**Depends On:** TASK-5
**Action:** Edit source template Section 6 to add a validation checklist with ≥4 items covering real component names, error paths, architecture layers, concrete names.

### TASK-7: RED — Write backward compatibility test
**Tests:** UT-15
**Parallel:** no (depends on TASK-2, TASK-4, TASK-6)
**Depends On:** TASK-2, TASK-4, TASK-6
**Action:** Add test verifying original Section 6 content is preserved.

### TASK-8: GREEN — Verify backward compatibility (should already pass)
**Tests:** UT-15 should be GREEN
**Parallel:** no
**Depends On:** TASK-7
**Action:** Verify existing content is not removed. If test fails, fix template.

### TASK-9: RED — Write dual-copy consistency tests
**Tests:** UT-16, UT-17, UT-18, UT-19, UT-20
**Parallel:** no (depends on TASK-2, TASK-4, TASK-6)
**Depends On:** TASK-2, TASK-4, TASK-6
**Action:** Add dual-copy describe block with tests verifying both copies contain matrix, diagram, checklist, participants, and matrix rows.

### TASK-10: GREEN — Replicate changes to GitHub source template
**Tests:** UT-16..UT-20 turn GREEN
**Parallel:** no
**Depends On:** TASK-9
**Action:** Edit `resources/github-skills-templates/story/x-story-create.md` with same new content as Claude template.

### TASK-11: REFACTOR — Clean up and optimize
**Parallel:** no
**Depends On:** TASK-10
**Action:** Refactor test file if needed, ensure constants are well-organized, remove duplication.

### TASK-12: Regenerate golden files
**Parallel:** no
**Depends On:** TASK-11
**Action:** Run pipeline for all 8 profiles, update golden files. Run `byte-for-byte.test.ts` to validate.

### TASK-13: Final test run with coverage
**Parallel:** no
**Depends On:** TASK-12
**Action:** Run full test suite, verify ≥95% line / ≥90% branch coverage.

## Execution Order

```
TASK-1 (RED) → TASK-2 (GREEN)
TASK-3 (RED) → TASK-4 (GREEN)    [parallel with TASK-1..2]
TASK-5 (RED) → TASK-6 (GREEN)    [parallel with TASK-1..4]
TASK-7 (RED) → TASK-8 (GREEN)    [after TASK-2,4,6]
TASK-9 (RED) → TASK-10 (GREEN)   [after TASK-2,4,6]
TASK-11 (REFACTOR)               [after TASK-8,10]
TASK-12 (golden files)           [after TASK-11]
TASK-13 (final validation)       [after TASK-12]
```
