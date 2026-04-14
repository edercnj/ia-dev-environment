# Test Plan — story-0038-0001

**Mode:** Double-Loop TDD. Outer loop: AT-1 (smoke IT). Inner loop: UT-N in TPP order.

## Acceptance Tests (AT)

### AT-1 — Migrated example is valid (smoke / IT)
- **Type:** Integration
- **File:** `dev.iadev.taskfile.TaskExampleMigrationIT`
- **Given:** `plans/epic-0038/examples/task-TASK-0037-0001-001.md` exists.
- **When:** `TaskFileParser.parse(path)` is invoked.
- **Then:** `result.valid() == true`, `result.testabilityKind() == INDEPENDENT`, `result.violations()` empty (or only WARN-level).
- **Maps to:** TASK-006

## Unit Tests (UT) — TPP Order

| ID | Test | TPP Level | Depends On (Task) |
| :--- | :--- | :--- | :--- |
| UT-1 | `ValidationViolation` record stores ruleId, severity, message | 1 (constant) | TASK-002 |
| UT-2 | `TaskFileValidationResult` `valid` derives from violations.isEmpty() of ERROR severity | 2 (computed) | TASK-002 |
| UT-3 | `TestabilityKind` enum has exactly 3 values | 1 | TASK-002 |
| UT-4 | TF-SCHEMA-001: missing ID -> ERROR; ID matches filename -> pass | 3 (rule logic) | TASK-004 |
| UT-5 | TF-SCHEMA-002: status outside enum -> ERROR; valid status -> pass | 3 | TASK-004 |
| UT-6 | TF-SCHEMA-003: zero checkboxes -> ERROR; two checkboxes -> ERROR; one -> pass | 4 (combinatorial) | TASK-004 |
| UT-7 | TF-SCHEMA-004: empty outputs -> ERROR; non-empty -> pass | 3 | TASK-004 |
| UT-8 | TF-SCHEMA-005: 5 DoD items -> WARN; 6 items -> pass; 10 items -> pass | 4 (boundary) | TASK-004 |
| UT-9 | TF-SCHEMA-006: COALESCED + missing ref -> ERROR; valid ref -> pass | 4 | TASK-004 |
| UT-10 | Parser extracts ID from `**ID:** TASK-0038-0001-001` line | 3 | TASK-003 |
| UT-11 | Parser extracts all 5 sections from a well-formed file | 4 | TASK-003 |
| UT-12 | Parser on empty content -> result.valid()=false with TF-SCHEMA-001 + TF-SCHEMA-003 | 5 (degenerate) | TASK-003 |
| UT-13 | Parser delegates to TaskValidator and aggregates violations | 5 (composition) | TASK-003 |
| UT-14 | `TaskValidator.defaultValidator()` wires all 6 rules | 2 | TASK-004 |

## Coverage Targets

- Line: ≥ 95%
- Branch: ≥ 90%

## Gherkin (from story §7)

The 5 Gherkin scenarios are absorbed:
- "Degenerate — arquivo vazio" -> UT-12
- "Happy path — válido independente" -> AT-1 (and UT-11 + parser composition)
- "Erro — testabilidade múltipla" -> UT-6
- "Boundary — DoD 6 itens" -> UT-8
- "Smoke — exemplo migrado" -> AT-1

## Test Naming Convention

`methodUnderTest_scenario_expectedBehavior` (Rule 05).

Examples: `validate_missingId_returnsError`, `parse_emptyContent_returnsTwoSchemaViolations`.
